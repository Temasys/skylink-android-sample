package sg.com.temasys.skylink.sdk.rtc;

import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;

import java.util.List;

/**
 * Created by janidu on 23/2/15.
 */
class AppRTCSignalingParameters {

    private List<PeerConnection.IceServer> iceServers;
    private String gaeBaseHref;
    private String channelToken;
    private String postMessageUrl;
    private boolean initiator;
    private MediaConstraints pcConstraints;
    private MediaConstraints audioConstraints;
    private MediaConstraints videoConstraints;
    private String apiOwner;
    private String cid;
    private String displayName;
    private String len;
    private String roomCred;
    private String roomId;
    private String sid;
    private String start;
    private String timeStamp;
    private String tokenTempCreated;
    private String userCred;
    private String userId;
    private String protocol;
    private String ipSigserver;
    private int portSigserver;

    public List<PeerConnection.IceServer> getIceServers() {
        return iceServers;
    }

    public void setIceServers(List<PeerConnection.IceServer> iceServers) {
        this.iceServers = iceServers;
    }

    public String getGaeBaseHref() {
        return gaeBaseHref;
    }

    public void setGaeBaseHref(String gaeBaseHref) {
        this.gaeBaseHref = gaeBaseHref;
    }

    public String getChannelToken() {
        return channelToken;
    }

    public void setChannelToken(String channelToken) {
        this.channelToken = channelToken;
    }

    public String getPostMessageUrl() {
        return postMessageUrl;
    }

    public void setPostMessageUrl(String postMessageUrl) {
        this.postMessageUrl = postMessageUrl;
    }

    public boolean isInitiator() {
        return initiator;
    }

    public void setInitiator(boolean initiator) {
        this.initiator = initiator;
    }

    public MediaConstraints getPcConstraints() {
        return pcConstraints;
    }

    public void setPcConstraints(MediaConstraints pcConstraints) {
        this.pcConstraints = pcConstraints;
    }

    public MediaConstraints getAudioConstraints() {
        return audioConstraints;
    }

    public void setAudioConstraints(MediaConstraints audioConstraints) {
        this.audioConstraints = audioConstraints;
    }

    public MediaConstraints getVideoConstraints() {
        return videoConstraints;
    }

    public void setVideoConstraints(MediaConstraints videoConstraints) {
        this.videoConstraints = videoConstraints;
    }

    public String getApiOwner() {
        return apiOwner;
    }

    public void setApiOwner(String apiOwner) {
        this.apiOwner = apiOwner;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLen() {
        return len;
    }

    public void setLen(String len) {
        this.len = len;
    }

    public String getRoomCred() {
        return roomCred;
    }

    public void setRoomCred(String roomCred) {
        this.roomCred = roomCred;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getTokenTempCreated() {
        return tokenTempCreated;
    }

    public void setTokenTempCreated(String tokenTempCreated) {
        this.tokenTempCreated = tokenTempCreated;
    }

    public String getUserCred() {
        return userCred;
    }

    public void setUserCred(String userCred) {
        this.userCred = userCred;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getIpSigserver() {
        return ipSigserver;
    }

    public void setIpSigserver(String ipSigserver) {
        this.ipSigserver = ipSigserver;
    }

    public int getPortSigserver() {
        return portSigserver;
    }

    public void setPortSigserver(int portSigserver) {
        this.portSigserver = portSigserver;
    }
}
