package sg.com.temasys.skylink.sdk.rtc;

import org.webrtc.PeerConnection;

import java.util.List;

/**
 * Created by janidu on 23/2/15.
 */
class RoomParameters {

    private List<PeerConnection.IceServer> iceServers;
    /* MediaConstraints are now read from SkylinkConfig, and not from App Server.
    private MediaConstraints pcConstraints;
    private MediaConstraints audioConstraints;
    private MediaConstraints videoConstraints;*/
    private String appOwner;
    private String cid;
    private String displayName;
    private String len;
    private String roomCred;
    private String roomId;
    private String sid;
    private String start;
    private String timeStamp;
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

    public String getAppOwner() {
        return appOwner;
    }

    public void setAppOwner(String appOwner) {
        this.appOwner = appOwner;
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
