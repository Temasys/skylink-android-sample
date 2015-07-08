package sg.com.temasys.skylink.sdk.rtc;

/**
 * Created by xiangrong on 2/7/15.
 */

import org.webrtc.DataChannel;
import org.webrtc.PeerConnection;

import java.util.Date;

/**
 * This class contains all information of a Skylink Peer object.
 */
public class Peer {

    private String peerId;
    private PeerConnection pc;
    private SkylinkPcObserver pcObserver;
    private SkylinkSdpObserver sdpObserver;
    private PeerInfo peerInfo;
    private UserInfo userInfo;
    private DataChannel dc;
    private HealthChecker healthChecker;

    private double weight;
    private SkylinkConnection skylinkConnection;

    public Peer() {
        this.weight = new Date().getTime();
        /*this.weight = new Random(new Date().getTime()).nextDouble()
                * (double) 1000000;*/
    }

    public Peer(String peerId, SkylinkConnection skylinkConnection) {
        this();
        this.peerId = peerId;
        this.skylinkConnection = skylinkConnection;
    }

    public Peer(String peerId, PeerConnection pc, SkylinkPcObserver pcObserver, SkylinkSdpObserver sdpObserver) {
        this();
        this.peerId = peerId;
        this.pc = pc;
        this.pcObserver = pcObserver;
        this.sdpObserver = sdpObserver;
    }

    void initialiseHealthChecker(String iceRole) {
        healthChecker = new HealthChecker(peerId, skylinkConnection);
        healthChecker.setIceRole(iceRole);
        healthChecker.startRestartTimer();
    }

    /**
     * Set the iceState
     *
     * @param iceState
     */
    void setHealthCheckerIceState(PeerConnection.IceConnectionState iceState) {
        healthChecker.setIceState(iceState);
    }

        // Getters and Setters
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

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public DataChannel getDc() {
        return dc;
    }

    public void setDc(DataChannel dc) {
        this.dc = dc;
    }

}
