package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logE;

/**
 * Created by xiangrong on 20/5/15.
 */
class SkylinkConnectionService implements AppServerClientListener, SignalingMessageListener {
    private static final String TAG = SkylinkConnectionService.class.getName();
    public static final String APP_SERVER = "http://api.temasys.com.sg/api/";
    public static final String APP_SERVER_STAGING = "http://staging-api.temasys.com.sg/api/";

    /**
     * List of Connection state types
     */
    public enum ConnectionState {
        CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED
    }

    private final SkylinkConnection skylinkConnection;
    private final AppServerClient appServerClient;
    private final SignalingMessageProcessingService signalingMessageProcessingService;

    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private RoomParameters roomParameters;

    public SkylinkConnectionService(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
        this.appServerClient = new AppServerClient(this, new SkylinkRoomParameterProcessor());
        this.signalingMessageProcessingService = new SignalingMessageProcessingService(
                skylinkConnection, this, new MessageProcessorFactory(), this);
    }

    /**
     * AppServerClientListener implementation When error occurs while getting room parameters.
     */
    @Override
    public void onErrorAppServer(final String message) {
        skylinkConnection.runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (skylinkConnection.getLockDisconnect()) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (isDisconnected()) {
                        return;
                    }
                    skylinkConnection.getLifeCycleListener().onConnect(false, message);
                }
            }
        });
    }

    /**
     * AppServerClientListener implementation Connect to Signaling Server and start signaling
     * process with room.
     *
     * @param params Parameters obtained from App server.
     */
    @Override
    public void onObtainedRoomParameters(final RoomParameters params) {
        skylinkConnection.runOnUiThread(new Runnable() {
            public void run() {
                setRoomParameters(params);
                // Connect to Signaling Server and start signaling process with room.
                signalingMessageProcessingService.connect(getIpSigServer(),
                        getPortSigServer(), getSid(), getRoomId());
            }
        });
    }

    /**
     * SignalingMessageListener implementation Established socket.io connection with Signaling
     * server Should now request to join the room.
     */
    @Override
    public void onConnectedToRoom() {
        // Send joinRoom.
        ProtocolHelper.sendJoinRoom(this);

        skylinkConnection.runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (skylinkConnection.getLockDisconnect()) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (isDisconnected()) {
                        return;
                    }
                    skylinkConnection.getLifeCycleListener().onConnect(true, null);
                }
            }
        });
    }

    /**
     * Check if already connected to Room, i.e., to Signaling server.
     *
     * @return
     */

    boolean isAlreadyConnected() {
        boolean connected = (connectionState == ConnectionState.CONNECTED);
        return connected;
    }

    /**
     * Check if disconnected or disconnecting from Room, i.e., to Signaling server.
     *
     * @return
     */
    boolean isDisconnected() {
        boolean disconnected = (connectionState == ConnectionState.DISCONNECTED ||
                connectionState == ConnectionState.DISCONNECTING);
        return disconnected;
    }

    /**
     * Asynchronously connect to the App server, which will provide room parameters required to
     * connect to room. Connection to room will trigger after obtaining room parameters.
     *
     * @param skylinkConnectionString
     * @throws IOException
     * @throws JSONException
     */
    public void connectToRoom(String skylinkConnectionString) {
        // Record user intention for connection to room state
        connectionState = ConnectionState.CONNECTING;
        String url = APP_SERVER + skylinkConnectionString;
        // url = APP_SERVER_STAGING + skylinkConnectionString;
        // Append json query
        String jsonURL = url + "&t=json";

        logD(TAG, "[SkylinkConnectionService.connectToRoom] url:\n" + jsonURL);
        this.appServerClient.connectToRoom(jsonURL);
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
        if (this.appServerClient == null)
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
            String error = "[ERROR] Unable to send Server message! Message:\n" + message.toString();
            String debug = error + "\nException: " + e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);
        }
    }

    /**
     * Sends local user data related to oneself, to all remote peers in our room.
     *
     * @param userData User defined data relating to the peer. May be a 'java.lang.String',
     *                 'org.json.JSONObject' or 'org.json.JSONArray'.
     */
    void sendLocalUserData(Object userData) {
        if (this.appServerClient == null) {
            return;
        }

        getSkylinkPeerService().setUserData(null, userData);
        JSONObject dict = new JSONObject();
        try {
            dict.put("type", "updateUserEvent");
            dict.put("mid", getSid());
            dict.put("rid", getRoomId());
            dict.put("userData", userData);
            sendMessage(dict);
        } catch (JSONException e) {
            String error = "[ERROR] Unable to send user data!";
            String debug = error + "Details: Could not form updateUserEvent JSON. Exception:\n" +
                    e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);
        }
    }

    /**
     * Disconnect from the Signaling Channel.
     *
     * @return False if unable to disconnect.
     */
    public boolean disconnect() {
        // Record user intention for disconnecting to room
        connectionState = ConnectionState.DISCONNECTING;

        if (this.signalingMessageProcessingService != null) {
            signalingMessageProcessingService.disconnect();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Restart all connections when rejoining room.
     *
     * @param skylinkConnection SkylinkConnection instance.
     */
    void rejoinRestart(SkylinkConnection skylinkConnection) {
        if (getSkylinkPeerService().getPeerNumber() > 0) {
            // Create a new peerId set to prevent concurrent modification of the set
            Set<String> peerIdSet =
                    new HashSet<String>(getSkylinkPeerService().getPeerIdSet());
            for (String peerId : peerIdSet) {
                rejoinRestart(peerId, skylinkConnection);
            }
        }
    }

    /**
     * Restart specific connection when rejoining room. Sends targeted "enter" for non-Android
     * peers. This is a hack to accomodate the non-Android clients until the update to SM 0.1.1 This
     * is esp. so for the JS clients which do not allow restarts for PeerIds without
     * PeerConnection.
     *
     * @param remotePeerId      PeerId of the remote Peer with whom we should restart with.
     * @param skylinkConnection SkylinkConnection instance.
     */
    void rejoinRestart(String remotePeerId, SkylinkConnection skylinkConnection) {
        if (skylinkConnection.getSkylinkConnectionService().getConnectionState() == ConnectionState.DISCONNECTING) {
            return;
        }
        synchronized (skylinkConnection.getLockDisconnect()) {
            try {
                String debug = "[rejoinRestart] Peer " + remotePeerId + " ";
                Peer peer = getSkylinkPeerService().getPeer(remotePeerId);
                PeerInfo peerInfo = peer.getPeerInfo();
                if (peerInfo != null && peerInfo.getAgent().equals("Android")) {
                    // If it is Android, send restart.
                    debug += "is Android.";
                    ProtocolHelper.sendRestart(remotePeerId, skylinkConnection,
                            getSkylinkMediaService().getLocalMediaStream(), skylinkConnection
                                    .getSkylinkConfig());
                } else {
                    // If web or others, send directed enter
                    // TODO XR: Remove after JS client update to compatible restart protocol.
                    debug += "is non-Android or has no PeerInfo.";
                    ProtocolHelper.sendEnter(remotePeerId, skylinkConnection, this);
                }
                logD(TAG, debug);
            } catch (SkylinkException e) {
                String error = "[ERROR:" + Errors.CONNECT_UNABLE_RESTART_ON_REJOIN +
                        "] Unable to connect to Peer(s) on rejoining after disconnect!";
                String debug = error + "\nCONNECT_UNABLE_RESTART_ON_REJOIN Exception:\n" +
                        e.getMessage();
                logE(TAG, error);
                logD(TAG, debug);
            }
        }
    }

    void restartConnectionInternal(String remotePeerId, SkylinkConnection skylinkConnection) {
        if (skylinkConnection.getSkylinkConnectionService().getConnectionState() == ConnectionState.DISCONNECTING) {
            return;
        }
        synchronized (skylinkConnection.getLockDisconnect()) {
            ProtocolHelper.sendRestart(remotePeerId, skylinkConnection, getSkylinkMediaService()
                    .getLocalMediaStream(), skylinkConnection.getSkylinkConfig());
        }
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

    // Internal methods
    private SkylinkPeerService getSkylinkPeerService() {
        return skylinkConnection.getSkylinkPeerService();
    }

    private SkylinkMediaService getSkylinkMediaService() {
        return skylinkConnection.getSkylinkMediaService();
    }

    // Getters and Setters
    public SignalingMessageProcessingService getSignalingMessageProcessingService() {
        return signalingMessageProcessingService;
    }

    public String getAppOwner() {
        return roomParameters.getAppOwner();
    }

    public String getCid() {
        return roomParameters.getCid();
    }

    void setConnectionState(ConnectionState connectionState) {
        this.connectionState = connectionState;
    }

    ConnectionState getConnectionState() {
        return connectionState;
    }

    public String getIpSigServer() {
        return this.roomParameters.getIpSigserver();
    }

    public int getPortSigServer() {
        return this.roomParameters.getPortSigserver();
    }

    public List<PeerConnection.IceServer> getIceServers() {
        return this.roomParameters.getIceServers();
    }

    public void setIceServers(List<PeerConnection.IceServer> iceServers) {
        this.roomParameters.setIceServers(iceServers);
    }

    public String getLen() {
        return roomParameters.getLen();
    }

    public String getRoomCred() {
        return roomParameters.getRoomCred();
    }

    public String getRoomId() {
        return roomParameters.getRoomId();
    }

    public RoomParameters getRoomParameters() {
        return roomParameters;
    }

    public void setRoomParameters(RoomParameters roomParameters) {
        this.roomParameters = roomParameters;
    }

    public String getSid() {
        return roomParameters.getSid();
    }

    public void setSid(String sid) {
        this.roomParameters.setSid(sid);
    }

    public String getStart() {
        return roomParameters.getStart();
    }

    public void setStart(String start) {
        this.roomParameters.setStart(start);
    }

    public String getTimeStamp() {
        return roomParameters.getTimeStamp();
    }

    public String getUserCred() {
        return roomParameters.getUserCred();
    }

    public String getUserId() {
        return roomParameters.getUserId();
    }

}
