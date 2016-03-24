package sg.com.temasys.skylink.sdk.rtc;

import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logE;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logW;

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
    private MediaStream localMediaStream;
    private SkylinkConfig myConfig;

    /**
     * Initialise all required parameters
     *
     * @param remotePeerId
     * @param skylinkConnection
     */
    HealthChecker(final String remotePeerId,
                  final SkylinkConnection skylinkConnection) {
        this.remotePeerId = remotePeerId;
        this.skylinkConnection = skylinkConnection;
        this.localMediaStream = skylinkConnection.getSkylinkMediaService().getLocalMediaStream();
        this.myConfig = skylinkConnection.getSkylinkConfig();
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
                            String warn = "[WARN:" + Errors.HANDSHAKE_RESTART_TIMER_FAILED +
                                    "] Connection with Peer " + remotePeerId +
                                    " might face some difficulties in completion";
                            String debug = warn + "\nHANDSHAKE_RESTART_TIMER_FAILED " +
                                    "Exception:\n" + e.getMessage();
                            logW(TAG, warn);
                            logD(TAG, debug);
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
            String error = "[ERROR:" + Errors.PC_MAX_RESTART_ATTEMPTS_REACHED + "]" +
                    " Unable to connect to Peer " + remotePeerId + ".";
            String debug = error + "\nStop trying to restart as already tried " + restartNumber +
                    " times.";
            logE(TAG, error);
            logD(TAG, debug);
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
        logD(TAG, "Peer " + remotePeerId + " : IceConnectionState : " + iceState +
                " - Restarting (" + restartNumber + ").");
        ProtocolHelper.sendRestart(remotePeerId, skylinkConnection,
                localMediaStream, myConfig);
    }

    /**
     * @return iceState
     */
    PeerConnection.IceConnectionState getIceState() {
        return iceState;
    }

    /**
     * Set the iceState
     *
     * @param iceState
     */
    void setIceState(PeerConnection.IceConnectionState iceState) {
        this.iceState = iceState;
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
        Peer peer = skylinkConnection.getSkylinkPeerService().getPeer(remotePeerId);
        if (peer != null) {
            PeerInfo peerInfo = peer.getPeerInfo();
            if (peerInfo != null) {
                boolean enableIceTrickle = peerInfo.isEnableIceTrickle();
                if (!enableIceTrickle) {
                    iceRole = ICE_ROLE_TRICKLE_OFF;
                    logD(TAG, "Peer " + remotePeerId + " : has NOT enabled ICE trickle.");
                } else {
                    logD(TAG, "Peer " + remotePeerId + " : has enabled ICE trickle.");
                }
            }

        }
        // Set waitMs based on iceRole
        setWaitMs();
        logD(TAG, "Peer " + remotePeerId + " : iceRole set to " + iceRole + ".");
    }
}