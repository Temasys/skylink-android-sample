package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

class HealthChecker {

    private final static String TAG = HealthChecker.class.getName();

    final static String ICE_ROLE_OFFERER = "offerer";
    final static String ICE_ROLE_ANSWERER = "answerer";
    final static String ICE_ROLE_TRICKLE_OFF = "iceTrickleOff";

    final static String ICE_CONNECTED = "iceConnected";
    final static String ICE_COMPLETED = "iceCompleted";
    final static String ICE_FAILED = "iceFailed";
    final static String ICE_DISCONNECTED = "iceDisconnected";

    private final long WAIT_OFFERER = 12500;
    private final long WAIT_ANSWERER = 10000;
    private final long WAIT_ICE_TRICKLE_OFF = 50000;

    /**
     * Max number of times to try for restart.
     */
    private final int MAX_RESTART_ATTEMPTS = 100;

    // Has a default value to avoid null exceptions when iceState is not yet updated.
    private PeerConnection.IceConnectionState iceState = PeerConnection.IceConnectionState.CHECKING;
    // Our ICE role in this PeerConnection
    // Offerer (sent enter) or Answerer (sent welcome).
    // Default is the Offerer (longer wait duration).
    private String iceRole = ICE_ROLE_OFFERER;
    // No. of seconds to wait before triggering next restart
    private long waitMs = WAIT_OFFERER;
    // No. of times restarting.
    private int restartNumber = 0;

    // Required for restart.
    private String remotePeerId;
    private SkylinkConnection skylinkConnection;
    private SkylinkConnectionService skylinkConnectionService;
    private WebServerClient webServerClient;
    private MediaStream localMediaStream;
    private SkylinkConfig myConfig;
    private PeerConnection pc;

    /**
     * Initialise all required parameters
     *
     * @param remotePeerId
     * @param skylinkConnection
     * @param skylinkConnectionService
     * @param localMediaStream
     * @param myConfig
     * @param pc
     */
    HealthChecker(final String remotePeerId,
                  final SkylinkConnection skylinkConnection,
                  SkylinkConnectionService skylinkConnectionService,
                  MediaStream localMediaStream,
                  SkylinkConfig myConfig,
                  PeerConnection pc) {
        this.remotePeerId = remotePeerId;
        this.skylinkConnection = skylinkConnection;
        this.skylinkConnectionService = skylinkConnectionService;
        this.localMediaStream = localMediaStream;
        this.myConfig = myConfig;
        this.pc = pc;
    }

    /**
     * Initiate a restart loop for the appropriate time span.
     */
    void startRestartTimer() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(
                new Runnable() {
                    public void run() {
                        try {
                            if (tryRestart()) {
                                startRestartTimer();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }
                }, waitMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Send restart if it is needed.
     *
     * @return true if restart needed, false otherwise.
     */
    private boolean tryRestart() {
        // Stop trying to restart after certain number of attempts.
        if (restartNumber >= MAX_RESTART_ATTEMPTS) {
            Log.e(TAG, "Stop trying to restarting as already tried " + restartNumber + " times.");
            return false;
        }
        switch (iceState) {
            // Continue to monitor but do not restart
            case NEW:
            case CHECKING:
                return true;
            // Stop monitoring
            case CONNECTED:
            case COMPLETED:
            case DISCONNECTED:
            case CLOSED:
                restartNumber = 0;
                return false;
            // Restart and continue monitoring
            case FAILED:
                ++restartNumber;
                sendRestart();
                return true;
            default:
                return true;
        }
    }

    /**
     * Set the right waitMs based on iceRole
     */
    private void setWaitMs() {
        switch (iceRole) {
            case ICE_ROLE_OFFERER:
                waitMs = WAIT_OFFERER;
                break;
            case ICE_ROLE_ANSWERER:
                waitMs = WAIT_ANSWERER;
                break;
            case ICE_ROLE_TRICKLE_OFF:
                waitMs = WAIT_ICE_TRICKLE_OFF;
                break;
            default:
                break;
        }
    }

    /**
     * Send the restart call
     */
    private void sendRestart() {
        try {
            Log.d(TAG, "[HealthChecker] Peer " + remotePeerId + " : IceConnectionState : " + iceState +
                    " - Restarting (" + restartNumber + ").");
            ProtocolHelper.sendRestart(remotePeerId, skylinkConnection, skylinkConnectionService,
                    localMediaStream, myConfig);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * @return iceState
     */
    PeerConnection.IceConnectionState getIceState() {
        return iceState;
    }

    /**
     * Set the iceState and also sets waitMs
     *
     * @param iceState
     */
    void setIceState(PeerConnection.IceConnectionState iceState) {
        this.iceState = iceState;
        setWaitMs();
    }

    /**
     * @return iceRole
     */
    String getIceRole() {
        return iceRole;
    }

    /**
     * Set iceRole based on role given and whether ICE trickle has been enabled.
     *
     * @param iceRole
     */
    void setIceRole(String iceRole) {
        this.iceRole = iceRole;
        // Check if ICE trickle is enable.
        Map<String, PeerInfo> peerInfoMap = skylinkConnection.getPeerInfoMap();
        if (peerInfoMap != null) {
            PeerInfo peerInfo = peerInfoMap.get(remotePeerId);
            if (peerInfo != null) {
                boolean enableIceTrickle = peerInfo.isEnableIceTrickle();
                if (!enableIceTrickle) {
                    iceRole = ICE_ROLE_TRICKLE_OFF;
                    Log.d(TAG, "[HealthChecker] Peer " + remotePeerId + " : has NOT enabled ICE trickle.");
                } else {
                    Log.d(TAG, "[HealthChecker] Peer " + remotePeerId + " : has enabled ICE trickle.");
                }
            }

        }
        Log.d(TAG, "[HealthChecker] Peer " + remotePeerId + " : iceRole set to " + iceRole + ".");
    }
}