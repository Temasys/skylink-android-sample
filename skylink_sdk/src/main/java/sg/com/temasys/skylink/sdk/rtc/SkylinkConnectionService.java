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
    private final WebServerClient.IceServersObserver iceServersObserver;

    private final SignalingMessageProcessingService signalingMessageProcessingService;

    private final WebServerClient webServerClient;

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
            dict.put("cid", webServerClient.getCid());
            dict.put("data", message);
            dict.put("mid", webServerClient.getSid());
            dict.put("rid", webServerClient.getRoomId());
            if (remotePeerId != null) {
                dict.put("type", "private");
                dict.put("target", remotePeerId);
            } else {
                dict.put("type", "public");
            }
            webServerClient.sendMessage(dict);
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
            dict.put("mid", webServerClient.getSid());
            dict.put("rid", webServerClient.getRoomId());
            dict.put("userData", userData);
            webServerClient.sendMessage(dict);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Mutes the local user's audio and notifies all the peers in the room.
     *
     * @param isMuted Flag that specifies whether audio should be mute
     */
    void muteLocalAudio(boolean isMuted) {
        if (this.webServerClient == null) {
            return;
        }
        org.webrtc.AudioTrack localAudioTrack = skylinkConnection.getLocalAudioTrack();

        if (skylinkConnection.getMyConfig().hasAudioSend() &&
                (localAudioTrack.enabled() == isMuted)) {

            localAudioTrack.setEnabled(!isMuted);
            JSONObject dict = new JSONObject();
            try {
                dict.put("type", "muteAudioEvent");
                dict.put("mid", webServerClient.getSid());
                dict.put("rid", webServerClient.getRoomId());
                dict.put("muted", new Boolean(isMuted));
                webServerClient.sendMessage(dict);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    /**
     * Mutes the local user's video and notifies all the peers in the room.
     *
     * @param isMuted Flag that specifies whether video should be mute
     */
    void muteLocalVideo(boolean isMuted) {
        if (this.webServerClient == null)
            return;

        org.webrtc.VideoTrack localVideoTrack = skylinkConnection.getLocalVideoTrack();
        if (skylinkConnection.getMyConfig().hasVideoSend() &&
                (localVideoTrack.enabled() == isMuted)) {
            localVideoTrack.setEnabled(!isMuted);
            JSONObject dict = new JSONObject();
            try {
                dict.put("type", "muteVideoEvent");
                dict.put("mid", webServerClient.getSid());
                dict.put("rid", webServerClient.getRoomId());
                dict.put("muted", new Boolean(isMuted));
                webServerClient.sendMessage(dict);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    // Connect to Signaling Server and start signaling process with room.
    void obtainedRoomParameters(String roomId) {
        // Connect to Signaling Server and start signaling process with room.
            /*signalingMessageProcessingService.connect(params.getIpSigserver(),
                    params.getPortSigserver(), params.getSid(), params.getRoomId());*/
        signalingMessageProcessingService.connect(webServerClient.getIpSigServer(),
                webServerClient.getPortSigServer(), webServerClient.getSid(), roomId);

    }

    // Disconnect from the Signaling Channel.
    public void disconnect() {
        if (this.signalingMessageProcessingService != null) {
            signalingMessageProcessingService.disconnect();
        }
    }

    public WebServerClient getWebServerClient() {
        return webServerClient;
    }

    public SignalingMessageProcessingService getSignalingMessageProcessingService() {
        return signalingMessageProcessingService;
    }

    public String getSid() {
        return webServerClient.getSid();
    }

    public void setSid(String sid) {
        webServerClient.setSid(sid);
    }

    public String getRoomId() {
        return webServerClient.getRoomId();
    }

}
