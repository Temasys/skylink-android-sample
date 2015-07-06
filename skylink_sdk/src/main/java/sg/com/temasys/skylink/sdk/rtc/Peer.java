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

    private UserInfo userInfo;
    private DataChannel dc;

    public Peer() {
    }

    public Peer(String peerId, PeerConnection pc, SkylinkPcObserver pcObserver, SkylinkSdpObserver sdpObserver) {
        this.peerId = peerId;
        this.pc = pc;
        this.pcObserver = pcObserver;
        this.sdpObserver = sdpObserver;
    }

    void initialiseHealthChecker() {

    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public PeerConnection getPc() {
        return pc;
    }

    public void setPc(PeerConnection pc) {
        this.pc = pc;
    }

    public SkylinkPcObserver getPcObserver() {
        return pcObserver;
    }

    public void setPcObserver(SkylinkPcObserver pcObserver) {
        this.pcObserver = pcObserver;
    }

    public SkylinkSdpObserver getSdpObserver() {
        return sdpObserver;
    }

    public void setSdpObserver(SkylinkSdpObserver sdpObserver) {
        this.sdpObserver = sdpObserver;
    }

    public HealthChecker getHealthChecker() {
        return healthChecker;
    }

    public void setHealthChecker(HealthChecker healthChecker) {
        this.healthChecker = healthChecker;
    }

    public PeerInfo getPeerInfo() {
        return peerInfo;
    }

    public void setPeerInfo(PeerInfo peerInfo) {
        this.peerInfo = peerInfo;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public DataChannel getDc() {
        return dc;
    }

    public void setDc(DataChannel dc) {
        this.dc = dc;
    }

}
