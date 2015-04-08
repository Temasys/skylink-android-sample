package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

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

    private PeerConnection.IceConnectionState iceState;
    // Offerer (sent enter) or Answerer (sent welcome).
    private String iceRole = "";
    // No. of seconds to wait before triggering next restart
    private long waitMs = WAIT_OFFERER;
    // No. of times restarting.
    private int restartNumber = 0;

    // Required for restart.
    private String remotePeerId;
    private SkylinkConnection skylinkConnection;
    private WebServerClient webServerClient;
    private MediaStream localMediaStream;
    private SkylinkConfig myConfig;
    private PeerConnection pc;

    // Initialise all required parameters
    HealthChecker(final String remotePeerId,
                  final SkylinkConnection skylinkConnection,
                  WebServerClient webServerClient,
                  MediaStream localMediaStream,
                  SkylinkConfig myConfig,
                  PeerConnection pc) {
        this.remotePeerId = remotePeerId;
        this.skylinkConnection = skylinkConnection;
        this.webServerClient = webServerClient;
        this.localMediaStream = localMediaStream;
        this.myConfig = myConfig;
        this.pc = pc;
    }

    // Initiate a restart loop for the appropriate time span.
    void startRestartTimer() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(
                new Runnable() {
                    public void run() {
                        try {
                            if (tryRestart()) {
                                startRestartTimer();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }, waitMs, TimeUnit.MILLISECONDS);
    }

    // Send restart if it is needed.
    // Return true if needed.
    private boolean tryRestart() {
        // Check actual IceConnectionState
        /*PeerConnection.IceConnectionState currentIceState = pc.iceConnectionState();
        Log.d(TAG, "Peer " + remotePeerId + " : iceState : " + iceState + ".");
        Log.d(TAG, "Peer " + remotePeerId + " : currentIceState : " + currentIceState + ".");
        Log.d(TAG, "Peer " + remotePeerId + " : setting iceState to currentIceState.");
        iceState = currentIceState;*/

        switch (iceState) {
            case NEW:
            case CHECKING:
            case CONNECTED:
            case COMPLETED:
            case DISCONNECTED:
            case CLOSED:
                restartNumber = 0;
                return false;
            case FAILED:
            default:
                ++restartNumber;
                sendRestart();
                return true;
        }
    }

    // Set the right waitMs based on iceRole
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

    // Send the restart call
    private void sendRestart() {
        try {
          Log.d(TAG, "Peer " + remotePeerId + " : IceConnectionState : " + iceState + 
            " - Restarting (" + restartNumber + ").");
            PeerInfo peerInfo = skylinkConnection.getPeerInfoMap().get(remotePeerId);
            if(peerInfo != null && peerInfo.getAgent().equals("Android")) {
              // If it is Android, send restart.
                ProtocolHelper.sendRestart(remotePeerId, skylinkConnection, webServerClient,
                        localMediaStream, myConfig);
            } else {
              // If web or others, send directed enter
                ProtocolHelper.sendEnter(remotePeerId, skylinkConnection, webServerClient);
            }
        } catch (JSONException e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    PeerConnection.IceConnectionState getIceState() {
        return iceState;
    }

    // Set the iceState and also waitMs
    void setIceState(PeerConnection.IceConnectionState iceState) {
        this.iceState = iceState;
        setWaitMs();
    }


    String getIceRole() {
        return iceRole;
    }

    void setIceRole(String iceRole) {
        this.iceRole = iceRole;
    }

}