package sg.com.temasys.skylink.sdk.rtc;

import org.webrtc.MediaStream;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

class HealthChecker {

    final static String OFFERER = "offerer";
    final static String ANSWERER = "answerer";
    final static String ICE_TRICKLE_OFF = "iceTrickleOff";

    final static String ICE_CONNECTED = "iceConnected";
    final static String ICE_COMPLETED = "iceCompleted";
    final static String ICE_FAILED = "iceFailed";
    final static String ICE_DISCONNECTED = "iceDisconnected";

    private final double WAIT_OFFERER = 12.5;
    private final double WAIT_ANSWERER = 10;
    private final double WAIT_ICE_TRICKLE_OFF = 50;

    private String iceState = "";
    // Offerer (sent enter) or Answerer (sent welcome).
    private String iceRole = "";
    // No. of seconds to wait before triggering next restart
    private double waitSeconds = WAIT_OFFERER;

    // Required for restart.
    private String remotePeerId;
    private SkylinkConnection skylinkConnection;
    private WebServerClient webServerClient;
    private MediaStream localMediaStream;
    private SkylinkConfig myConfig;

    // Initialise all required parameters
    void HealthChecker(final String remotePeerId,
                       final SkylinkConnection skylinkConnection,
                       WebServerClient webServerClient,
                       MediaStream localMediaStream,
                       SkylinkConfig myConfig) {
        this.remotePeerId = remotePeerId;
        this.skylinkConnection = skylinkConnection;
        this.webServerClient = webServerClient;
        this.localMediaStream = localMediaStream;
        this.myConfig = myConfig;
    }

    // Initiate a restart loop for the appropriate time span.
    void startRestartTimer(String restartType) {

    }

    // Send restart if it is needed.
    // Return true if needed.
    private boolean tryRestart() {
        return true;
    }

    public String getIceState() {
        return iceState;
    }

    public void setIceState(String iceState) {
        this.iceState = iceState;
    }


    public String getIceRole() {
        return iceRole;
    }

    public void setIceRole(String iceRole) {
        this.iceRole = iceRole;
    }

}