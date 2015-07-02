package sg.com.temasys.skylink.sdk.rtc;

/**
 * Created by xiangrong on 2/7/15.
 */

import org.webrtc.DataChannel;
import org.webrtc.PeerConnection;

/**
 * This class contains all information of a Skylink Peer object.
 */
public class Peer {

    private String peerId;
    private PeerConnection pc;
    private SkylinkPcObserver pcObserver;
    private SkylinkSdpObserver sdpObserver;
    private HealthChecker healthChecker;
    private PeerInfo peerInfo;
    private DataChannel dc;

    void initialiseHealthChecker() {

    }
}
