package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by xiangrong on 20/5/15.
 */
class SkylinkConnectionService {
    private static final String TAG = SkylinkConnectionService.class.getName();
    private final SkylinkConnection skylinkConnection;
    private final SignalingMessageProcessingService signalingMessageProcessingService;
    private final WebServerClient webServerClient;
    private final WebServerClient.IceServersObserver iceServersObserver;

    private AppRTCSignalingParameters appRTCSignalingParameters;

    public SkylinkConnectionService(SkylinkConnection skylinkConnection,
                                    WebServerClient.IceServersObserver iceServersObserver) {
        this.skylinkConnection = skylinkConnection;
        this.iceServersObserver = iceServersObserver;
        this.webServerClient = new WebServerClient(this, iceServersObserver);
        this.signalingMessageProcessingService = new SignalingMessageProcessingService(
                skylinkConnection, new MessageProcessorFactory());
    }

    /**
     * Asynchronously connect to an AppRTC room URL, e.g. https://apprtc.appspot.com/?r=NNN and
     * register message-handling callbacks on its GAE Channel.
     *
     * @throws IOException
     * @throws JSONException
     * @throws Exception
     */
    public void connectToRoom(String url) throws IOException, JSONException {
        this.webServerClient.connectToRoom(url);
    }

    /**
     * Sends a user defined message to a specific remote peer or to all remote peers via a server.
     *
     * @param remotePeerId Id of the remote peer to whom we will send a message. Use 'null' if the
     *                     message is to be broadcast to all remote peers in the room.
     * @param message      User defined data. May be a 'java.lang.String', 'org.json.JSONObject' or
     *                     'org.json.JSONArray'.
     */
    void sendServerMessage(String remotePeerId, Object message) {
        if (this.webServerClient == null)
            return;

        JSONObject dict = new JSONObject();
        try {
            dict.put("cid", getCid());
            dict.put("data", message);
            dict.put("mid", getSid());
            dict.put("rid", getRoomId());
            if (remotePeerId != null) {
                dict.put("type", "private");
                dict.put("target", remotePeerId);
            } else {
                dict.put("type", "public");
            }
            sendMessage(dict);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Sends local user data related to oneself, to all remote peers in our room.
     *
     * @param userData User defined data relating to the peer. May be a 'java.lang.String',
     *                 'org.json.JSONObject' or 'org.json.JSONArray'.
     */
    void sendLocalUserData(Object userData) {
        if (this.webServerClient == null) {
            return;
        }

        skylinkConnection.setUserData(userData);
        JSONObject dict = new JSONObject();
        try {
            dict.put("type", "updateUserEvent");
            dict.put("mid", getSid());
            dict.put("rid", getRoomId());
            dict.put("userData", userData);
            sendMessage(dict);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    // Disconnect from the Signaling Channel.
    public void disconnect() {
        if (this.signalingMessageProcessingService != null) {
            signalingMessageProcessingService.disconnect();
        }
    }

    // Connect to Signaling Server and start signaling process with room.
    void obtainedRoomParameters(AppRTCSignalingParameters params) {
        setAppRTCSignalingParameters(params);
        // Connect to Signaling Server and start signaling process with room.
        signalingMessageProcessingService.connect(getIpSigServer(),
                getPortSigServer(), getSid(), getRoomId());
    }

    public void sendMessage(JSONObject dictMessage) {
        if (this.signalingMessageProcessingService == null) {
            return;
        }
        signalingMessageProcessingService.sendMessage(dictMessage);
    }

    /**
     * Notify all the peers in the room on our changed audio status.
     *
     * @param isMuted Flag that specifies whether audio is now mute
     */
    void sendMuteAudio(boolean isMuted) {
        ProtocolHelper.sendMuteAudio(isMuted, this);
    }

    /**
     * Notify all the peers in the room on our changed video status.
     *
     * @param isMuted Flag that specifies whether video is now mute
     */
    void sendMuteVideo(boolean isMuted) {
        ProtocolHelper.sendMuteVideo(isMuted, this);
    }

    WebServerClient.IceServersObserver getIceServersObserver() {
        return iceServersObserver;
    }

    public WebServerClient getWebServerClient() {
        return webServerClient;
    }

    public SignalingMessageProcessingService getSignalingMessageProcessingService() {
        return signalingMessageProcessingService;
    }

    public AppRTCSignalingParameters getAppRTCSignalingParameters() {
        return appRTCSignalingParameters;
    }

    public void setAppRTCSignalingParameters(AppRTCSignalingParameters appRTCSignalingParameters) {
        this.appRTCSignalingParameters = appRTCSignalingParameters;
    }

    public String getAppOwner() {
        return appRTCSignalingParameters.getAppOwner();
    }

    public String getIpSigServer() {
        return this.appRTCSignalingParameters.getIpSigserver();
    }

    public int getPortSigServer() {
        return this.appRTCSignalingParameters.getPortSigserver();
    }

    public String getCid() {
        return appRTCSignalingParameters.getCid();
    }

    public String getLen() {
        return appRTCSignalingParameters.getLen();
    }

    public String getRoomCred() {
        return appRTCSignalingParameters.getRoomCred();
    }

    public String getRoomId() {
        return appRTCSignalingParameters.getRoomId();
    }

    public String getSid() {
        return appRTCSignalingParameters.getSid();
    }

    public void setSid(String sid) {
        this.appRTCSignalingParameters.setSid(sid);
    }

    public String getStart() {
        return appRTCSignalingParameters.getStart();
    }

    public void setStart(String start) {
        this.appRTCSignalingParameters.setStart(start);
    }

    public String getTimeStamp() {
        return appRTCSignalingParameters.getTimeStamp();
    }

    public String getUserCred() {
        return appRTCSignalingParameters.getUserCred();
    }

    public String getUserId() {
        return appRTCSignalingParameters.getUserId();
    }
}
