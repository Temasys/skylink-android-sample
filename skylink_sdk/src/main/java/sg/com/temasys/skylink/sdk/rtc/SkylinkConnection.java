package sg.com.temasys.skylink.sdk.rtc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaCodecVideoEncoder;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import sg.com.temasys.skylink.sdk.BuildConfig;
import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.DataTransferListener;
import sg.com.temasys.skylink.sdk.listener.FileTransferListener;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.MessagesListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;

/**
 * Main class to connect to the skylink infrastructure.
 *
 * @author Temasys Communications Pte Ltd
 */
public class SkylinkConnection {

    /**
     * Duration in hours after the start time when the room will be closed by the signalling
     * server.
     */
    public static final int DEFAULT_DURATION = 24;
    public static final String API_SERVER = "http://api.temasys.com.sg/api/";

    private static final String TAG = "SkylinkConnection";
    private static final int MAX_PEER_CONNECTIONS = 4;
    private static final String MY_SELF = "me";

    private static final String AUDIO_CODEC_OPUS = "opus";
    private static final String AUDIO_CODEC_ISAC = "ISAC";

    private static boolean factoryStaticInitialized;

    private Context applicationContext;
    private AudioSource localAudioSource;
    private AudioTrack localAudioTrack;
    private boolean isMCUConnection;
    private boolean videoSourceStopped;
    private ConstConnectionConfig settingsObject;
    private DataChannelManager dataChannelManager;
    private GLSurfaceView localVideoView;
    private List<PeerConnection.IceServer> iceServerArray;
    private Map<GLSurfaceView, String> surfaceOnHoldPool;
    private Map<String, Object> displayNameMap;
    private Map<String, sg.com.temasys.skylink.sdk.rtc.PeerInfo> peerInfoMap;
    private Map<String, PCObserver> pcObserverPool;
    private Map<String, PeerConnection> peerConnectionPool;
    private Map<String, SDPObserver> sdpObserverPool;
    private MediaConstraints pcConstraints;
    private MediaConstraints sdpMediaConstraints;
    private MediaStream localMediaStream;
    private Object myUserData;
    private PeerConnectionFactory peerConnectionFactory;
    private String apiKey;
    private SkylinkConfig myConfig;
    private VideoCapturerAndroid localVideoCapturer;
    private VideoSource localVideoSource;
    private VideoTrack localVideoTrack;
    private WebServerClient webServerClient;

    private WebServerClient.IceServersObserver iceServersObserver = new MyIceServersObserver();
    private MessageHandler messageHandler = new MyMessageHandler();
    private VideoRendererGuiListener videoRendererGuiListener = new MyVideoRendererGuiListener();

    private FileTransferListener fileTransferListener;
    private LifeCycleListener lifeCycleListener;
    private MediaListener mediaListener;
    private MessagesListener messagesListener;
    private RemotePeerListener remotePeerListener;
    private DataTransferListener dataTransferListener;

    private boolean roomLocked;
    private VideoRendererGui localVideoRendererGui;

    /**
     * List of Connection state types
     */
    public enum ConnectionState {
        CONNECT, DISCONNECT
    }

    private ConnectionState connectionState;

    // Lock objects to prevent threads from executing the following methods concurrently:
    // WebServerClient.MessageHandler.onMessage
    // SkylinkConnection.disconnect
    // WebServerClient.IceServersObserver.onIceServers
    // WebServerClient.IceServersObserver.onError
    private Object lockDisconnect = new Object();
    private Object lockDisconnectMsg = new Object();
    private Object lockDisconnectMedia = new Object();
    private Object lockDisconnectSdp = new Object();
    private Object lockDisconnectSdpCreate = new Object();
    private Object lockDisconnectSdpSet = new Object();
    private Object lockDisconnectSdpDrain = new Object();
    private Object lockDisconnectSdpSend = new Object();

    private static SkylinkConnection instance;
    private Handler handler;

    private SkylinkConnection() {
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * @return Existing instance of SkylinkConnection Object if it exists or a new instance if it
     * doesn't exist.
     */
    public static synchronized SkylinkConnection getInstance() {
        if (instance == null) {
            instance = new SkylinkConnection();
        }
        return instance;
    }

    /**
     * Connects to a room with the default duration of 24 hours and with the current time It is
     * encouraged to use the method connectToRoom(String connectionString, Object userData)
     *
     * @param secret   The secret associated with the key as registered with the Skylink Developer
     *                 Console
     * @param roomName The name of the room
     * @param userData User defined data relating to oneself. May be a 'java.lang.String',
     *                 'org.json.JSONObject' or 'org.json.JSONArray'.
     * @return 'false' if the connection is already established
     */
    public boolean connectToRoom(final String secret,
                                 final String roomName, final Object userData) {

        if (isAlreadyConnected()) {
            return false;
        }

        this.myUserData = userData;

        // Fetch the current time from a server
        CurrentTimeService currentTimeService = new CurrentTimeService(new CurrentTimeServiceListener() {
            @Override
            public void onCurrentTimeFetched(Date date) {
                Log.d(TAG, "onCurrentTimeFetched" + date);
                String connectionString = Utils.getSkylinkConnectionString(roomName, apiKey,
                        secret, date, DEFAULT_DURATION);
                connectToRoom(connectionString, userData);
            }

            @Override
            public void onCurrentTimeFetchedFailed() {
                Log.d(TAG, "onCurrentTimeFetchedFailed, using device time");
                String connectionString = Utils.getSkylinkConnectionString(roomName, apiKey,
                        secret, new Date(), DEFAULT_DURATION);
                connectToRoom(connectionString, userData);
            }
        });

        currentTimeService.execute();

        return true;
    }

    /**
     * Connects to a room with SkylinkConnectionString
     *
     * @param skylinkConnectionString SkylinkConnectionString Generated with room name, apiKey,
     *                                secret, startTime and duration
     * @param userData                User defined data relating to oneself. May be a
     *                                'java.lang.String', 'org.json.JSONObject' or
     *                                'org.json.JSONArray'.
     * @return 'false' if the connection is already established
     */
    public boolean connectToRoom(String skylinkConnectionString, Object userData) {

        this.myUserData = userData;

        logMessage("SkylinkConnection::connectingRoom userData=>" + userData);

        if (isAlreadyConnected()) {
            return false;
        }

        // Record user intention for connection to room state
        connectionState = ConnectionState.CONNECT;

        if (this.fileTransferListener == null)
            this.fileTransferListener = new FileTransferAdapter();
        if (this.lifeCycleListener == null)
            this.lifeCycleListener = new LifeCycleAdapter();
        if (this.mediaListener == null)
            this.mediaListener = new MediaAdapter();
        if (this.messagesListener == null)
            this.messagesListener = new MessagesAdapter();
        if (this.remotePeerListener == null)
            this.remotePeerListener = new RemotePeerAdapter();

        if (this.dataTransferListener == null) {
            this.dataTransferListener = new DataTransferAdapter();
        }

        this.webServerClient = new WebServerClient(messageHandler,
                iceServersObserver);

        String url = API_SERVER + skylinkConnectionString;
        try {
            this.webServerClient.connectToRoom(url);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        logMessage("SkylinkConnection::connection url=>" + url);
        return true;
    }

    /**
     * Locks the room if its not already locked
     */
    public void lockRoom() {
        if (!roomLocked) {
            try {
                ProtocolHelper.sendRoomLockStatus(this.webServerClient, true);
                roomLocked = true;
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    /**
     * Unlocks the room if its already locked
     */
    public void unlockRoom() {
        if (roomLocked) {
            try {
                ProtocolHelper.sendRoomLockStatus(this.webServerClient, false);
                roomLocked = false;
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    /**
     * Restarts a connection with a specific peer or all connections if remotePeerId is null
     *
     * @param remotePeerId Id of the remote peer to whom we will restart a message. Use 'null' if
     *                     the message is to be broadcast to all remote peers in the room.
     */
    public void restartConnection(String remotePeerId) {
        if (TextUtils.isEmpty(remotePeerId)) {
            // If remoteId is provided restart the specific peerConnection
            // Else restart all the peer connections
            if (pcObserverPool != null) {
                // Create a new peerId set to prevent concurrent modification of the set
                Set<String> peerIdSet = new HashSet<String>(pcObserverPool.keySet());
                for (String peerId : peerIdSet) {
                    restartConnectionInternal(peerId);
                }
            }
        } else {
            restartConnectionInternal(remotePeerId);
        }
    }

    private void restartConnectionInternal(String remotePeerId) {
        if (connectionState == ConnectionState.DISCONNECT) {
            return;
        }
        synchronized (lockDisconnect) {
            try {
                ProtocolHelper.sendRestart(remotePeerId, this, webServerClient, localMediaStream,
                        myConfig);
            } catch (JSONException e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    }

    /**
     * Creates a new SkylinkConnection object with the specified parameters.
     *
     * @param apiKey  The api key from the Skylink Developer Console
     * @param config  The SkylinkConfig object to configure the type of call.
     * @param context The application context
     */
    public void init(String apiKey,
                     SkylinkConfig config, Context context) {
        logMessage("SkylinkConnection::config=>" + config);

        this.myConfig = new SkylinkConfig(config);
        this.settingsObject = new ConstConnectionConfig();

        logMessage("SkylinkConnection::apiKey=>" + apiKey);
        this.apiKey = apiKey;

        if (!factoryStaticInitialized) {

            boolean hardwareAccelerated = false;
            EGLContext eglContext = null;

            // Enable hardware acceleration if supported
            if (MediaCodecVideoEncoder.isVp8HwSupported()) {
                hardwareAccelerated = true;
                eglContext = VideoRendererGui.getEGLContext();
                Log.d(TAG, "Enabled hardware acceleration");
            }

            abortUnless(PeerConnectionFactory.initializeAndroidGlobals(context,
                    true, true, hardwareAccelerated, eglContext
            ), "Failed to initializeAndroidGlobals");

            factoryStaticInitialized = true;
        }

        this.sdpMediaConstraints = new MediaConstraints();
        this.sdpMediaConstraints.mandatory
                .add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio",
                        String.valueOf(this.myConfig.hasAudioReceive())));
        this.sdpMediaConstraints.mandatory
                .add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo",
                        String.valueOf(this.myConfig.hasVideoReceive())));

        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory
                .add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio",
                        String.valueOf(this.myConfig.hasAudioReceive())));
        constraints.mandatory
                .add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo",
                        String.valueOf(this.myConfig.hasVideoReceive())));
        constraints.optional.add(new MediaConstraints.KeyValuePair(
                "internalSctpDataChannels", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair(
                "DtlsSrtpKeyAgreement", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("googDscp",
                "true"));
        this.pcConstraints = constraints;

        this.applicationContext = context;

        // Instantiate DataChannelManager.
        if (this.myConfig.hasPeerMessaging() || this.myConfig.hasFileTransfer()
                || this.myConfig.hasDataTransfer()) {
            this.dataChannelManager = new DataChannelManager(this,
                    this.myConfig.getTimeout(), myConfig.hasPeerMessaging(),
                    myConfig.hasFileTransfer());
            this.dataChannelManager.setConnectionManager(this);
        }

        // Instantiate other variables
        peerInfoMap = new Hashtable<String, PeerInfo>();
    }

    /**
     * Runs the specified action on the UI thread
     *
     * @param action the action to run on the UI thread
     */
    void runOnUiThread(Runnable action) {
        handler.post(action);
    }

    /**
     * Gets PeerInfo object of a specific peer.
     *
     * @param peerId PeerId of specific peer for which PeerInfo is desired.
     */
    PeerInfo getPeerInfo(String peerId) {
        return peerInfoMap.get(peerId);
    }

    private boolean isAlreadyConnected() {
        return this.webServerClient != null;
    }

    /**
     * Disconnects from the room we are currently in.
     */
    public void disconnectFromRoom() {
        // Prevent thread from executing with WebServerClient methods concurrently.
        synchronized (lockDisconnectMsg) {
            synchronized (lockDisconnectMedia) {
                synchronized (lockDisconnectSdpDrain) {
                    synchronized (lockDisconnectSdpSend) {
                        synchronized (lockDisconnectSdpSet) {
                            synchronized (lockDisconnectSdpCreate) {
                                synchronized (lockDisconnectSdp) {
                                    synchronized (lockDisconnect) {

                                        // Disconnect only if connected
                                        if (connectionState != ConnectionState.CONNECT) {
                                            return;
                                        }

                                        // Record user intention for connection to room state
                                        connectionState = ConnectionState.DISCONNECT;

                                        if (this.webServerClient != null) {
                                            this.webServerClient.disconnect();
                                        }

                                        logMessage("Inside SkylinkConnection.disconnectFromRoom");

                                        // Dispose all DC.
                                        String allPeers = null;
                                        if (dataChannelManager != null) {
                                            dataChannelManager.disposeDC(allPeers);
                                        }

                                        if (this.peerConnectionPool != null) {
                                            for (PeerConnection peerConnection : this.peerConnectionPool
                                                    .values()) {
                                                // Remove stream before disposing in order to prevent
                                                // localMediaStream from being disposed
                                                peerConnection.removeStream(localMediaStream);
                                                peerConnection.dispose();
                                            }

                                            this.peerConnectionPool.clear();
                                        }

                                        this.peerConnectionPool = null;

                                        if (this.pcObserverPool != null) {
                                            this.pcObserverPool.clear();
                                        }

                                        this.pcObserverPool = null;

                                        if (this.sdpObserverPool != null) {
                                            this.sdpObserverPool.clear();
                                        }
                                        this.sdpObserverPool = null;

                                        if (this.displayNameMap != null) {
                                            this.displayNameMap.clear();
                                        }

                                        this.displayNameMap = null;

                                        //Dispose local media streams, sources and tracks
                                        this.localMediaStream = null;
                                        this.localAudioTrack = null;
                                        this.localVideoTrack = null;

                                        if (this.localVideoSource != null) {
                                            // Stop the video source
                                            this.localVideoSource.stop();
                                            Log.d(TAG, "Stopped local Video Source");
                                        }

                                        this.localVideoSource = null;
                                        this.localAudioSource = null;

                                        // Dispose video capturer
                                        if (this.localVideoCapturer != null) {
                                            this.localVideoCapturer.dispose();
                                        }

                                        this.localVideoCapturer = null;

                                        if (this.peerConnectionFactory != null) {
                                            Log.d(TAG, "Disposing Peer Connection Factory");
                                            this.peerConnectionFactory.dispose();
                                            Log.d(TAG, "Disposed Peer Connection Factory");
                                        }

                                        this.peerConnectionFactory = null;
                                        this.webServerClient = null;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Completed disconnection");
    }

    /**
     * Sends a user defined message to a specific remote peer or to all remote peers via a server.
     *
     * @param remotePeerId Id of the remote peer to whom we will send a message. Use 'null' if the
     *                     message is to be broadcast to all remote peers in the room.
     * @param message      User defined data. May be a 'java.lang.String', 'org.json.JSONObject' or
     *                     'org.json.JSONArray'.
     */
    public void sendServerMessage(String remotePeerId, Object message) {
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
     * Sends a user defined message to a specific remote peer or to all remote peers in a direct
     * peer to peer manner.
     *
     * @param remotePeerId Id of the remote peer to whom we will send a message. Use 'null' if the
     *                     message is to be broadcast to all remote peers in the room.
     * @param message      User defined data. May be a 'java.lang.String', 'org.json.JSONObject' or
     *                     'org.json.JSONArray'.
     * @throws SkylinkException if the system was unable to send the message.
     */
    public void sendP2PMessage(String remotePeerId, Object message)
            throws SkylinkException {
        if (this.webServerClient == null)
            return;

        if (myConfig.hasPeerMessaging()) {
            if (remotePeerId == null) {
                Iterator<String> iPeerId = this.displayNameMap.keySet()
                        .iterator();
                while (iPeerId.hasNext()) {
                    String tid = iPeerId.next();
                    PeerInfo peerInfo = peerInfoMap.get(tid);
                    if (peerInfo == null) {
                        throw new SkylinkException(
                                "Unable to send the message to Peer " + tid +
                                        " as the Peer is no longer in room or has missing PeerInfo.");
                    } else {
                        if (!peerInfo.isEnableDataChannel())
                            throw new SkylinkException(
                                    "Unable to send the message via data channel to Peer " + tid +
                                            " as the Peer has not enabled data channel.");
                    }
                    if (!dataChannelManager.sendDcChat(false, message, tid))
                        throw new SkylinkException(
                                "Unable to send the message via data channel");
                }
            } else {
                String tid = remotePeerId;
                PeerInfo peerInfo = peerInfoMap.get(tid);
                if (peerInfo == null) {
                    throw new SkylinkException(
                            "Unable to send the message to Peer " + tid +
                                    " as the Peer is no longer in room or has missing PeerInfo.");
                } else {
                    if (!peerInfo.isEnableDataChannel())
                        throw new SkylinkException(
                                "Unable to send the message via data channel to Peer " + tid +
                                        " as the Peer has not enabled data channel.");
                }
                if (!dataChannelManager.sendDcChat(true, message, remotePeerId))
                    throw new SkylinkException(
                            "Unable to send the message via data channel");
            }
        } else {
            final String str = "Cannot send P2P message as it was not enabled in the configuration.\nUse "
                    + "setHasPeerMessaging( true ) on SkylinkConfig before creating SkylinkConnection.";
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from DC.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        lifeCycleListener.onReceiveLog(str);
                    }
                }
            });
        }
    }

    /**
     * Sends local user data related to oneself, to all remote peers in our room.
     *
     * @param userData User defined data relating to the peer. May be a 'java.lang.String',
     *                 'org.json.JSONObject' or 'org.json.JSONArray'.
     */
    public void sendLocalUserData(Object userData) {
        if (this.webServerClient == null) {
            return;
        }

        this.myUserData = userData;
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
    public void muteLocalAudio(boolean isMuted) {
        if (this.webServerClient == null) {
            return;
        }

        if (myConfig.hasAudioSend() && (localAudioTrack.enabled() == isMuted)) {
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
    public void muteLocalVideo(boolean isMuted) {
        if (this.webServerClient == null)
            return;

        if (myConfig.hasVideoSend() && (localVideoTrack.enabled() == isMuted)) {
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

    /**
     * Sends request(s) to share file with a specific remote peer or to all remote peers in a direct
     * peer to peer manner in the same room.
     *
     * @param remotePeerId The id of the remote peer to send the file to. Use 'null' if the file is
     *                     to be broadcast to all remote peers in the room.
     * @param fileName     The name of the file that is to be shared.
     * @param filePath     The absolute path of the file in the filesystem
     */
    public void sendFileTransferPermissionRequest(String remotePeerId, String fileName,
                                                  String filePath) throws SkylinkException {
        if (this.webServerClient == null)
            return;

        if (myConfig.hasFileTransfer()) {
            if (remotePeerId == null) {
                for (String iPeerId : displayNameMap.keySet()) {
                    String tid = iPeerId;
                    PeerInfo peerInfo = peerInfoMap.get(tid);
                    if (peerInfo == null) {
                        throw new SkylinkException(
                                "Unable to share the file with Peer " + tid +
                                        " as the Peer is no longer in room or has missing PeerInfo.");
                    } else {
                        if (!peerInfo.isEnableDataChannel()) {
                            throw new SkylinkException(
                                    "Unable to share the file with Peer " + tid +
                                            " as the Peer has not enabled data channel.");
                        }
                        dataChannelManager.sendFileTransferRequest(tid, fileName, filePath);
                    }
                }
            } else {
                String tid = remotePeerId;
                PeerInfo peerInfo = peerInfoMap.get(tid);
                if (peerInfo == null) {
                    throw new SkylinkException(
                            "Unable to share the file with Peer " + tid +
                                    " as the Peer is no longer in room or has missing PeerInfo.");
                } else {
                    if (!peerInfo.isEnableDataChannel()) {
                        throw new SkylinkException(
                                "Unable to share the file with Peer " + tid +
                                        " as the Peer has not enabled data channel.");
                    }
                    dataChannelManager.sendFileTransferRequest(tid, fileName, filePath);
                }
            }
        } else {
            final String str = "Cannot do file transfer as it was not enabled in the configuration.\nUse "
                    + "setHasFileTransfer( true ) on SkylinkConfig before creating SkylinkConnection.";
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from DC.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        lifeCycleListener.onReceiveLog(str);
                    }
                }
            });
        }
    }

    /**
     * Sends a byte array to a specified remotePeer or to all participants of the room if the
     * remotePeerId is null
     *
     * @param remotePeerId remotePeerID of a specified peer
     * @param data         Array of bytes
     * @throws SkylinkException
     */
    public void sendData(String remotePeerId, byte[] data) throws SkylinkException {
        if (myConfig != null && myConfig.hasDataTransfer()) {
            if (remotePeerId == null) {
                Iterator<String> iPeerId = this.displayNameMap.keySet()
                        .iterator();
                while (iPeerId.hasNext()) {
                    String tid = iPeerId.next();
                    PeerInfo peerInfo = peerInfoMap.get(tid);
                    if (peerInfo == null) {
                        throw new SkylinkException(
                                "Unable to send data to Peer " + tid +
                                        " as the Peer is no longer in room or has missing PeerInfo.");
                    } else {
                        if (!peerInfo.isEnableDataChannel())
                            throw new SkylinkException(
                                    "Unable to send data to Peer " + tid +
                                            " as the Peer has not enabled data channel.");
                        dataChannelManager.sendDataToPeer(tid, data);
                    }
                }
            } else {
                String tid = remotePeerId;
                PeerInfo peerInfo = peerInfoMap.get(tid);
                if (peerInfo == null) {
                    throw new SkylinkException(
                            "Unable to send data to Peer " + tid +
                                    " as the Peer is no longer in room or has missing PeerInfo.");
                } else {
                    if (!peerInfo.isEnableDataChannel())
                        throw new SkylinkException(
                                "Unable to send data to Peer " + tid +
                                        " as the Peer has not enabled data channel.");
                    dataChannelManager.sendDataToPeer(tid, data);
                }
            }
        } else {
            final String str = "Cannot do data transfer as it was not enabled in the configuration.\nUse "
                    + "setHasDataTransfer( true ) on SkylinkConfig before creating SkylinkConnection.";
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from DC.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        lifeCycleListener.onReceiveLog(str);
                    }
                }
            });
        }
    }

    /**
     * Call this method to accept or reject the file share request from a remote peer.
     *
     * @param remotePeerId The id of the remote peer that requested to share with us a file.
     * @param filePath     The absolute path of the file where we want it to be saved.
     * @param isPermitted  Whether permission was granted for the file share to proceed.
     */
    public void sendFileTransferPermissionResponse(String remotePeerId,
                                                   String filePath, boolean isPermitted) {
        if (this.webServerClient == null) {
            return;
        }
        if (myConfig.hasFileTransfer()) {
            dataChannelManager.acceptFileTransfer(remotePeerId, isPermitted, filePath);
        }
    }

    /**
     * @return The file transfer listener object.
     */
    public FileTransferListener getFileTransferListener() {
        return fileTransferListener;
    }

    /**
     * Sets the specified file transfer listener object.
     *
     * @param fileTransferListener The file transfer listener object that will receive callbacks
     *                             related to FileTransfer
     */
    public void setFileTransferListener(
            FileTransferListener fileTransferListener) {
        if (fileTransferListener == null)
            this.fileTransferListener = new FileTransferAdapter();
        else
            this.fileTransferListener = fileTransferListener;
    }

    /**
     * @return The life cycle listener object.
     */
    public LifeCycleListener getLifeCycleListener() {
        return lifeCycleListener;
    }

    /**
     * Sets the specified life cycle listener object.
     *
     * @param lifeCycleListener The life cycle listener object that will receive callbacks related
     *                          to the SDK's Lifecycle.
     */
    public void setLifeCycleListener(LifeCycleListener lifeCycleListener) {
        if (lifeCycleListener == null)
            this.lifeCycleListener = new LifeCycleAdapter();
        else
            this.lifeCycleListener = lifeCycleListener;
    }

    /**
     * @return The media listener object
     */
    public MediaListener getMediaListener() {
        return mediaListener;
    }

    /**
     * Sets the specified media listener object that will receive callbacks related to Media
     * Stream.
     * <p/>
     * Callbacks include those for the local user and remote peers.
     *
     * @param mediaListener The media listener object
     */
    public void setMediaListener(MediaListener mediaListener) {
        if (mediaListener == null)
            this.mediaListener = new MediaAdapter();
        else
            this.mediaListener = mediaListener;
    }

    /**
     * @return The messages listener object.
     */
    public MessagesListener getMessagesListener() {
        return messagesListener;
    }

    /**
     * Sets the specified messages listener object.
     *
     * @param messagesListener The messages listener object that will receive callbacks related to
     *                         Message Transmissions.
     */
    public void setMessagesListener(MessagesListener messagesListener) {
        if (messagesListener == null)
            this.messagesListener = new MessagesAdapter();
        else
            this.messagesListener = messagesListener;
    }

    /**
     * @return The remote peer listener object.
     */
    public RemotePeerListener getRemotePeerListener() {
        return remotePeerListener;
    }

    /**
     * Sets the specified remote peer listener object.
     *
     * @param remotePeerListener The remote peer listener object that will receive callbacks related
     *                           to the RemotePeer.
     */
    public void setRemotePeerListener(RemotePeerListener remotePeerListener) {
        if (remotePeerListener == null)
            this.remotePeerListener = new RemotePeerAdapter();
        else
            this.remotePeerListener = remotePeerListener;
    }


    /**
     * Retrieves the user defined data object associated with a remote peer.
     *
     * @param remotePeerId The id of the remote peer whose data is to be retrieved.
     * @return May be a 'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     */
    public Object getUserData(String remotePeerId) {
        if (remotePeerId == null)
            return this.myUserData;
        else
            return this.displayNameMap.get(remotePeerId);
    }

    private void logMessage(String message) {
        Log.d(TAG, message);
    }

    // Poor-man's assert(): die with |msg| unless |condition| is true.
    private static void abortUnless(boolean condition, String msg) {
        if (!condition) {
            throw new RuntimeException(msg);
        }
    }

    private void setDisplayMap(Object userData, String key) {
        if (this.displayNameMap == null)
            this.displayNameMap = new Hashtable<String, Object>();
        this.displayNameMap.put(key, userData);
    }

    /**
     * Cycle through likely device names for the camera and return the first capturer that works, or
     * crash if none do.
     *
     * @return
     */
    private VideoCapturerAndroid getVideoCapturer() {
        String frontCameraDeviceName =
                VideoCapturerAndroid.getNameOfFrontFacingDevice();
        Log.d(TAG, "Opening camera: " + frontCameraDeviceName);
        return VideoCapturerAndroid.create(frontCameraDeviceName);
    }

    private List<Object> getWeightedPeerConnection(String key, double weight) {
        if (this.peerConnectionPool == null) {
            this.peerConnectionPool = new Hashtable<String, PeerConnection>();
            this.isMCUConnection = isPeerIdMCU(key);
            if (dataChannelManager != null) {
                dataChannelManager.setIsMcuRoom(isMCUConnection);
            }
        }
        if (this.pcObserverPool == null)
            this.pcObserverPool = new Hashtable<String, PCObserver>();

        List<Object> resultList = new ArrayList<Object>();
        if (weight > 0) {
            PCObserver pc = this.pcObserverPool.get(key);
            if (pc != null) {
                if (pc.getMyWeight() > weight) {
                    resultList.add(new Boolean(true));
                    resultList.add(getPeerConnection(key));
                } else {
                    resultList.add(new Boolean(false));
                    resultList.add(new Boolean(false));
                }
            } else {
                resultList.add(new Boolean(true));
                resultList.add(getPeerConnection(key));
            }
        } else {
            resultList.add(new Boolean(true));
            resultList.add(getPeerConnection(key));
        }
        return resultList;
    }

    PeerConnection getPeerConnection(String key) {
        if (this.peerConnectionPool == null) {
            this.peerConnectionPool = new Hashtable<String, PeerConnection>();
            this.isMCUConnection = isPeerIdMCU(key);
            if (dataChannelManager != null) {
                dataChannelManager.setIsMcuRoom(isMCUConnection);
            }
        }
        if (this.pcObserverPool == null)
            this.pcObserverPool = new Hashtable<String, PCObserver>();

        PeerConnection pc = this.peerConnectionPool.get(key);
        if (pc == null) {
            if (this.peerConnectionPool.size() >= MAX_PEER_CONNECTIONS
                    && !isPeerIdMCU(key))
                return null;

            logMessage("Creating a new peer connection ...");
            PCObserver pcObserver = new SkylinkConnection.PCObserver();
            pcObserver.setMyId(key);
            // Prevent thread from executing with disconnect concurrently.
            synchronized (lockDisconnect) {
                pc = this.peerConnectionFactory.createPeerConnection(
                        this.iceServerArray, this.pcConstraints, pcObserver);
                logMessage("Created a new peer connection");
            }
            /*if (this.myConfig.hasAudio())
                pc.addStream(this.localMediaStream, this.pcConstraints);*/

            this.peerConnectionPool.put(key, pc);
            this.pcObserverPool.put(key, pcObserver);
        }

        return pc;
    }

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /**
     * Computes RFC 2104-compliant HMAC signature. * @param data The data to be signed.
     *
     * @param key The signing key.
     * @return The Base64-encoded RFC 2104-compliant HMAC signature.
     * @throws java.security.SignatureException when signature generation fails
     */
    private static String calculateRFC2104HMAC(String data, String key)
            throws SignatureException {
        String result;
        try {

            // get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(),
                    HMAC_SHA1_ALGORITHM);

            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());

            // base64-encode the hmac
            result = Base64
                    .encodeToString(rawHmac, android.util.Base64.DEFAULT);

        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : "
                    + e.getMessage());
        }
        return result.substring(0, result.length() - 1);
    }

    // Mangle SDP to prefer ISAC/16000 over any other audio codec.
    private static String preferCodec(
            String sdpDescription, String codec, boolean isAudio) {
        String[] lines = sdpDescription.split("\r\n");
        int mLineIndex = -1;
        String codecRtpMap = null;
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codecPattern = Pattern.compile(regex);
        String mediaDescription = "m=video ";
        if (isAudio) {
            mediaDescription = "m=audio ";
        }
        for (int i = 0; (i < lines.length) &&
                (mLineIndex == -1 || codecRtpMap == null); i++) {
            if (lines[i].startsWith(mediaDescription)) {
                mLineIndex = i;
                continue;
            }
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                codecRtpMap = codecMatcher.group(1);
                continue;
            }
        }
        if (mLineIndex == -1) {
            Log.w(TAG, "No " + mediaDescription + " line, so can't prefer " + codec);
            return sdpDescription;
        }
        if (codecRtpMap == null) {
            Log.w(TAG, "No rtpmap for " + codec);
            return sdpDescription;
        }
        Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + ", prefer at " +
                lines[mLineIndex]);
        String[] origMLineParts = lines[mLineIndex].split(" ");
        StringBuilder newMLine = new StringBuilder();
        int origPartIndex = 0;
        // Format is: m=<media> <port> <proto> <fmt> ...
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(codecRtpMap);
        for (; origPartIndex < origMLineParts.length; origPartIndex++) {
            if (!origMLineParts[origPartIndex].equals(codecRtpMap)) {
                newMLine.append(" ").append(origMLineParts[origPartIndex]);
            }
        }
        lines[mLineIndex] = newMLine.toString();
        Log.d(TAG, "Change media description: " + lines[mLineIndex]);
        StringBuilder newSdpDescription = new StringBuilder();
        for (String line : lines) {
            newSdpDescription.append(line).append("\r\n");
        }
        return newSdpDescription.toString();
    }

    void setUserInfo(JSONObject jsonObject) throws JSONException {
        JSONObject dictAudio = null;
        if (myConfig.hasAudioSend()) {
            dictAudio = new JSONObject();
            dictAudio.put("stereo", settingsObject.audio_stereo);
        }

        JSONObject dictVideo = null;
        if (myConfig.hasVideoSend()) {
            dictVideo = new JSONObject();
            dictVideo.put("frameRate", settingsObject.video_frameRate);
            JSONObject resolution = new JSONObject();
            resolution.put("height", settingsObject.video_height);
            resolution.put("width", settingsObject.video_width);
            dictVideo.put("resolution", resolution);
        }

        JSONObject dictSettings = new JSONObject();
        if (dictAudio != null)
            dictSettings.put("audio", dictAudio);
        if (dictVideo != null)
            dictSettings.put("video", dictVideo);

        JSONObject dictMediaStatus = new JSONObject();
        dictMediaStatus.put("audioMuted", false);
        dictMediaStatus.put("videoMuted", false);

        JSONObject dictUserInfo = new JSONObject();
        dictUserInfo.put("settings", dictSettings);
        dictUserInfo.put("mediaStatus", dictMediaStatus);
        dictUserInfo.put("userData", this.myUserData == null ? ""
                : this.myUserData);

        jsonObject.put("userInfo", dictUserInfo);

        // NOTE XR: dictBandwidth object is not being used.
        // Commented out for now.
        // Consider removing code.
        /*JSONObject dictBandwidth = new JSONObject();
        if (myConfig.hasAudioSend())
            dictBandwidth.put("audio", settingsObject.audio_bandwidth);
        if (myConfig.hasVideoSend())
            dictBandwidth.put("video", settingsObject.video_bandwidth);
        if (myConfig.hasPeerMessaging() || myConfig.hasFileTransfer())
            dictBandwidth.put("data", settingsObject.data_bandwidth);*/
    }

    private boolean isPeerIdMCU(String peerId) {
        return peerId.startsWith("MCU");
    }

    private class ConstConnectionConfig {
        public int data_bandwidth = 14460;
        public int audio_bandwidth = 56;
        public boolean audio_stereo = false;
        public int video_bandwidth = 256;
        public int video_frameRate = 30;
        public int video_height = 480;
        public int video_width = 320;
    }

    /*
     * AppRTCClient.IceServersObserver
     */
    private class MyIceServersObserver implements
            WebServerClient.IceServersObserver {

        private SkylinkConnection connectionManager = SkylinkConnection.this;

        @SuppressLint("NewApi")
        @Override
        public void onIceServers(List<IceServer> iceServers) {
            MediaStream lms;
            if (iceServers == null) {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (lockDisconnect) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (connectionState == ConnectionState.DISCONNECT) return;

                    if (connectionManager.peerConnectionFactory == null) {

                        connectionManager.peerConnectionFactory = new PeerConnectionFactory();

                        connectionManager.logMessage("[SDK] Local video source: Creating...");
                        lms = connectionManager.peerConnectionFactory
                                .createLocalMediaStream("ARDAMS");
                        connectionManager.localMediaStream = lms;

                        if (myConfig.hasVideoSend()) {

                            connectionManager.localVideoCapturer = getVideoCapturer();

                            if (connectionManager.localVideoCapturer == null) {
                                throw new RuntimeException("Failed to open capturer");
                            }

                            connectionManager.localVideoSource = connectionManager.peerConnectionFactory
                                    .createVideoSource(connectionManager.localVideoCapturer,
                                            connectionManager.webServerClient
                                                    .videoConstraints());
                            final VideoTrack localVideoTrack = connectionManager.peerConnectionFactory
                                    .createVideoTrack("ARDAMSv0",
                                            connectionManager.localVideoSource);
                            if (localVideoTrack != null) {
                                lms.addTrack(localVideoTrack);
                                connectionManager.localVideoTrack = localVideoTrack;
                            }
                        }


                        runOnUiThread(new Runnable() {
                            public void run() {
                                // Prevent thread from executing with disconnect concurrently.
                                synchronized (lockDisconnectMedia) {
                                    // If user has indicated intention to disconnect,
                                    // We should no longer process messages from signalling server.
                                    if (connectionState == ConnectionState.DISCONNECT) return;
                                    if (myConfig.hasVideoSend()) {
                                        localVideoView = new GLSurfaceView(applicationContext);
                                        localVideoRendererGui = new VideoRendererGui(localVideoView);
                                        localVideoRendererGui.setListener(connectionManager.videoRendererGuiListener);
                                        VideoRenderer.Callbacks localRender = localVideoRendererGui.create(0,
                                                0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
                                        localVideoTrack.addRenderer(new VideoRenderer(
                                                localRender));
                                    }

                                    if (connectionManager.surfaceOnHoldPool == null)
                                        connectionManager.surfaceOnHoldPool = new Hashtable<GLSurfaceView, String>();
                                    connectionManager.logMessage("[SDK] Local video source: Created.");
                                    // connectionManager.surfaceOnHoldPool.put(localVideoView, MY_SELF);
                                    mediaListener.onLocalMediaCapture(localVideoView);
                                    connectionManager.logMessage("[SDK] Local video source: Sent to App.");
                                }
                            }
                        });


                        synchronized (lockDisconnect) {
                            // If user has indicated intention to disconnect,
                            // We should no longer process messages from signalling server.
                            if (connectionState == ConnectionState.DISCONNECT) return;
                            if (myConfig.hasAudioSend()) {
                                connectionManager.logMessage("[SDK] Local audio source: Creating...");
                                connectionManager.localAudioSource = connectionManager.peerConnectionFactory
                                        .createAudioSource(new MediaConstraints());
                                connectionManager.localAudioTrack = connectionManager.peerConnectionFactory
                                        .createAudioTrack("ARDAMSa0",
                                                connectionManager.localAudioSource);
                                lms.addTrack(connectionManager.localAudioTrack);
                                connectionManager.logMessage("[SDK] Local audio source: Created.");
                            }
                        }
                    }

                    try {
                        JSONObject msgJoinRoom = new JSONObject();
                        msgJoinRoom.put("type", "joinRoom");
                        msgJoinRoom.put("rid",
                                connectionManager.webServerClient.getRoomId());
                        msgJoinRoom.put("uid",
                                connectionManager.webServerClient.getUserId());
                        msgJoinRoom.put("roomCred",
                                connectionManager.webServerClient.getRoomCred());
                        msgJoinRoom.put("cid",
                                connectionManager.webServerClient.getCid());
                        msgJoinRoom.put("userCred",
                                connectionManager.webServerClient.getUserCred());
                        msgJoinRoom.put("timeStamp",
                                connectionManager.webServerClient.getTimeStamp());
                        msgJoinRoom.put("apiOwner",
                                connectionManager.webServerClient.getApiOwner());
                        msgJoinRoom.put("len",
                                connectionManager.webServerClient.getLen());
                        msgJoinRoom.put("start",
                                connectionManager.webServerClient.getStart());
                        connectionManager.webServerClient.sendMessage(msgJoinRoom);
                        connectionManager.logMessage("[SDK] Join Room msg: Sending...");
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            } else {
                connectionManager.iceServerArray = iceServers;
            }
        }

        @Override
        public void onError(final String message) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        lifeCycleListener.onConnect(false, message);
                    }
                }
            });
        }

        @Override
        public void onShouldConnectToRoom() {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (connectionState == ConnectionState.DISCONNECT) {
                            return;
                        }
                        lifeCycleListener.onConnect(true, null);
                    }
                }
            });
        }
    }

    /*
     * GAEChannelClient.MessageHandler
     */
    private class MyMessageHandler implements MessageHandler {

        private SkylinkConnection connectionManager = SkylinkConnection.this;

        @Override
        public void onOpen() {
            // Prevent thread from executing with disconnect concurrently.
            synchronized (lockDisconnect) {
                // If user has indicated intention to disconnect,
                // We should no longer process messages from signalling server.
                if (connectionState == ConnectionState.DISCONNECT) return;
                connectionManager.iceServersObserver.onIceServers(null);
            }
        }

        @Override
        public void onMessage(String data) {
            // Prevent thread from executing with disconnect concurrently.
            synchronized (lockDisconnectMsg) {
                // If user has indicated intention to disconnect,
                // We should no longer process messages from signalling server.
                if (connectionState == ConnectionState.DISCONNECT) return;
                try {
                    messageProcessor(data);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }

        private void messageProcessor(String data) throws JSONException {
            String message = data;
            final JSONObject objects = new JSONObject(data);

            final String value = objects.getString("type");
            connectionManager.logMessage("[SDK] onMessage type - " + value);

            if (value.compareTo("inRoom") == 0) {
                String mid = objects.getString("sid");
                connectionManager.webServerClient.setSid(mid);

                // Set the peerID of the local video renderer
                localVideoRendererGui.setPeerId(mid);

                JSONObject pcConfigJSON = objects.getJSONObject("pc_config");
                String username = "";// pcConfigJSON.getString("username");
                username = username != null ? username : "";
                List<PeerConnection.IceServer> result = new ArrayList<PeerConnection.IceServer>();
                JSONArray iceServers = pcConfigJSON.getJSONArray("iceServers");
                for (int i = 0; i < iceServers.length(); i++) {
                    JSONObject iceServer = iceServers.getJSONObject(i);
                    String url = iceServer.getString("url");
                    if (myConfig.isStunDisabled() && url.startsWith("stun:")) {
                        connectionManager.logMessage(
                                "[SDK] Not adding stun server as stun disabled in config.");
                        continue;
                    }
                    if (myConfig.isTurnDisabled() && url.startsWith("turn:")) {
                        connectionManager.logMessage(
                                "[SDK] Not adding turn server as turn disabled in config.");
                        continue;
                    }
                    if (myConfig.getTransport() != null)
                        url = url + "?transport=" + myConfig.getTransport();
                    String credential = "";
                    try {
                        credential = iceServer.getString("credential");
                    } catch (JSONException e) {

                    }
                    credential = credential != null ? credential : "";
                    connectionManager.logMessage("[SDK] url [" + url
                            + "] - credential [" + credential + "]");
                    PeerConnection.IceServer server = new PeerConnection.IceServer(
                            url, username, credential);
                    result.add(server);
                }

                connectionManager.iceServersObserver.onIceServers(result);

                // Set mid and displayName in DataChannelManager
                if (connectionManager.dataChannelManager != null) {
                    connectionManager.dataChannelManager.setMid(mid);
                    connectionManager.dataChannelManager
                            .setDisplayName(connectionManager.myUserData
                                    .toString());
                }

                connectionManager.logMessage("*** SendEnter");
                JSONObject enterObject = new JSONObject();
                enterObject.put("type", "enter");
                enterObject.put("mid",
                        connectionManager.webServerClient.getSid());
                enterObject.put("rid",
                        connectionManager.webServerClient.getRoomId());
                enterObject.put("receiveOnly", false);
                enterObject.put("agent", "Android");
                enterObject.put("version", BuildConfig.VERSION_NAME);
                setUserInfo(enterObject);
                connectionManager.webServerClient.sendMessage(enterObject);

            } else if (value.compareTo("enter") == 0) {

                String mid = objects.getString("mid");
                Object userData = "";
                try {
                    userData = ((JSONObject) objects.get("userInfo"))
                            .get("userData");
                } catch (JSONException e) {
                }
                PeerConnection peerConnection = connectionManager
                        .getPeerConnection(mid);

                PeerInfo peerInfo = new PeerInfo();
                try {
                    peerInfo.setReceiveOnly(objects.getBoolean("receiveOnly"));
                    peerInfo.setAgent(objects.getString("agent"));
                    // SM0.1.0 - Browser version for web, SDK version for others.
                    peerInfo.setVersion(objects.getString("version"));
                } catch (JSONException e) {
                }

                peerInfoMap.put(mid, peerInfo);

                // Add our local media stream to this PC, or not.
                if ((myConfig.hasAudioSend() || myConfig.hasVideoSend())) {
                    peerConnection.addStream(connectionManager.localMediaStream);
                    Log.d(TAG, "Added localMedia Stream");
                }

                if (peerConnection != null) {
                    setDisplayMap(userData, mid);

                    try {
                        ProtocolHelper.sendWelcome(mid, connectionManager, webServerClient, myConfig, false);
                    } catch (JSONException e) {
                        Log.d(TAG, e.getMessage(), e);
                    }

                } else {
                    connectionManager
                            .logMessage("I only support "
                                    + MAX_PEER_CONNECTIONS
                                    + " connections are in this app. I am discarding this 'welcome'.");
                }

            } else if (value.compareTo("welcome") == 0) {
                processWelcome(objects);
            } else if (value.compareTo("restart") == 0) {
                String mid = objects.getString("mid");
                if (ProtocolHelper.processRestart(mid, localMediaStream, connectionManager)) {
                    processWelcome(objects);
                }
            } else if (value.compareTo("answer") == 0
                    || value.compareTo("offer") == 0) {

                String target = objects.getString("target");
                if (target
                        .compareTo(connectionManager.webServerClient.getSid()) != 0)
                    return;

                String mid = objects.getString("mid");
                PeerConnection peerConnection = connectionManager
                        .getPeerConnection(mid);

                String sdpString = objects.getString("sdp");

                // Set the preferred audio codec
                sdpString = preferCodec(sdpString, AUDIO_CODEC_OPUS, true);

                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(value),
                        sdpString);

                if (connectionManager.sdpObserverPool == null)
                    connectionManager.sdpObserverPool = new Hashtable<String, SDPObserver>();
                SDPObserver sdpObserver = connectionManager.sdpObserverPool
                        .get(mid);
                if (sdpObserver == null) {
                    sdpObserver = new SkylinkConnection.SDPObserver();
                    sdpObserver.setMyId(mid);
                    connectionManager.sdpObserverPool.put(mid, sdpObserver);
                }
                peerConnection.setRemoteDescription(sdpObserver, sdp);
                connectionManager
                        .logMessage("PC - setRemoteDescription. Sending "
                                + sdp.type + " to " + mid);

            } else if (value.compareTo("group") == 0) {
                // Split up group message
                // Format:
                // { type: "group", lists: [<group msg>...], mid: "xxx", rid: "xxx" }
                JSONArray msgArr = objects.getJSONArray("lists");
                for (int i = 0; i < msgArr.length(); ++i) {
                    String msg = (String) msgArr.get(i);
                    // Send each message to be processed like a non-group message.
                    if (msg != null) messageProcessor(msg);
                }
            } else if (value.compareTo("chat") == 0) {

                final String mid = objects.getString("mid");
                final String nick = objects.getString("nick");
                final String text = objects.getString("data");
                String tempTarget = null;
                try {
                    tempTarget = objects.getString("target");
                } catch (JSONException e) {

                }
                final String target = tempTarget;
                connectionManager.logMessage("event:" + value + ", nick->"
                        + nick + ", text->" + text + ", target->" + target);
                if (!connectionManager.isPeerIdMCU(mid)) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // Prevent thread from executing with disconnect concurrently.
                            synchronized (lockDisconnect) {
                                // If user has indicated intention to disconnect,
                                // We should no longer process messages from signalling server.
                                if (connectionState == ConnectionState.DISCONNECT) return;
                            }
                        }
                    });
                }
            } else if (value.compareTo("bye") == 0) {

                // Ignoring targetted bye
                String target = null;
                try {
                    target = objects.getString("target");
                } catch (JSONException e) {

                }
                if (target != null)
                    return;

                final String mid = objects.getString("mid");
                if (!connectionManager.isPeerIdMCU(mid)) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // Prevent thread from executing with disconnect concurrently.
                            synchronized (lockDisconnect) {
                                // If user has indicated intention to disconnect,
                                // We should no longer process messages from signalling server.
                                if (connectionState == ConnectionState.DISCONNECT) return;
                                remotePeerListener.onRemotePeerLeave(mid, "The peer has left the room");
                            }
                        }
                    });
                }
                PeerConnection peerConnection = connectionManager
                        .getPeerConnection(mid);

                // Dispose DataChannel.
                if (dataChannelManager != null) {
                    connectionManager.dataChannelManager.disposeDC(mid);
                }

                // Remove Stream so that it will not be disposed when the PeerConnection is disposed
                peerConnection.removeStream(localMediaStream);
                peerConnection.dispose();

                connectionManager.peerConnectionPool.remove(mid);
                connectionManager.pcObserverPool.remove(mid);
                connectionManager.sdpObserverPool.remove(mid);
                connectionManager.displayNameMap.remove(mid);
                connectionManager.peerInfoMap.remove(mid);

            } else if (value.compareTo("candidate") == 0) {

                String target = objects.getString("target");
                if (target
                        .compareTo(connectionManager.webServerClient.getSid()) != 0)
                    return;

                String mid = objects.getString("mid");
                String ID = objects.getString("id");
                int sdpLineIndex = objects.getInt("label");
                String sdp = objects.getString("candidate");

                IceCandidate candidate = new IceCandidate(ID, sdpLineIndex, sdp);

                PeerConnection peerConnection = connectionManager
                        .getPeerConnection(mid);
                if (peerConnection != null)
                    peerConnection.addIceCandidate(candidate);

            } else if (value.compareTo("ack_candidate") == 0) {

                connectionManager.logMessage("[SDK] onMessage - ack_candidate");

            } else if (value.compareTo("ping") == 0) {

                String target = objects.getString("target");
                if (target
                        .compareTo(connectionManager.webServerClient.getSid()) != 0)
                    return;

                String mid = objects.getString("mid");
                JSONObject pingObject = new JSONObject();
                pingObject.put("type", "ping");
                pingObject.put("mid",
                        connectionManager.webServerClient.getSid());
                pingObject.put("target", mid);
                pingObject.put("rid",
                        connectionManager.webServerClient.getRoomId());
                connectionManager.webServerClient.sendMessage(pingObject);

            } else if (value.compareTo("redirect") == 0) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        boolean shouldDisconnect = false;
                        // Prevent thread from executing with disconnect concurrently.
                        synchronized (lockDisconnect) {
                            // If user has indicated intention to disconnect,
                            // We should no longer process messages from signalling server.
                            if (connectionState == ConnectionState.DISCONNECT) return;
                            try {
                                shouldDisconnect = ProtocolHelper.processRedirect(objects, lifeCycleListener);
                            } catch (JSONException e) {
                                Log.e(TAG, e.getMessage(), e);
                            }
                        }

                        if (shouldDisconnect) {
                            disconnectFromRoom();
                        }
                    }
                });

            } else if (value.compareTo("private") == 0
                    || value.compareTo("public") == 0) {

                final Object objData = objects.get("data");
                final String mid = objects.getString("mid");
                if (!connectionManager.isPeerIdMCU(mid)) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // Prevent thread from executing with disconnect concurrently.
                            synchronized (lockDisconnect) {
                                // If user has indicated intention to disconnect,
                                // We should no longer process messages from signalling server.
                                if (connectionState == ConnectionState.DISCONNECT) return;
                                messagesListener.onServerMessageReceive(mid, objData, value.compareTo("private") == 0);
                            }
                        }
                    });
                }
            } else if (value.compareTo("updateUserEvent") == 0) {

                final String mid = objects.getString("mid");
                final Object userData = objects.get("userData");
                if (!connectionManager.isPeerIdMCU(mid)) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // Prevent thread from executing with disconnect concurrently.
                            synchronized (lockDisconnect) {
                                // If user has indicated intention to disconnect,
                                // We should no longer process messages from signalling server.
                                if (connectionState == ConnectionState.DISCONNECT) return;
                                remotePeerListener.onRemotePeerUserDataReceive(mid, userData);
                            }
                        }
                    });
                }
            } else if (value.compareTo("roomLockEvent") == 0) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        // Prevent thread from executing with disconnect concurrently.
                        synchronized (lockDisconnect) {
                            // If user has indicated intention to disconnect,
                            // We should no longer process messages from signalling server.
                            if (connectionState == ConnectionState.DISCONNECT) return;
                            try {
                                roomLocked = ProtocolHelper.processRoomLockStatus(roomLocked,
                                        objects, lifeCycleListener);
                            } catch (JSONException e) {
                                Log.e(TAG, e.getMessage(), e);
                            }
                        }
                    }
                });

            } else if (value.compareTo("muteAudioEvent") == 0) {

                if (myConfig.hasAudioReceive()) {
                    final String mid = objects.getString("mid");
                    final boolean muted = objects.getBoolean("muted");
                    if (!connectionManager.isPeerIdMCU(mid)) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                // Prevent thread from executing with disconnect concurrently.
                                synchronized (lockDisconnect) {
                                    // If user has indicated intention to disconnect,
                                    // We should no longer process messages from signalling server.
                                    if (connectionState == ConnectionState.DISCONNECT) return;
                                    mediaListener.onRemotePeerAudioToggle(mid, muted);
                                }
                            }
                        });
                    }
                }

            } else if (value.compareTo("muteVideoEvent") == 0) {

                if (myConfig.hasVideoReceive()) {
                    final String mid = objects.getString("mid");
                    final boolean muted = objects.getBoolean("muted");
                    if (!connectionManager.isPeerIdMCU(mid)) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                // Prevent thread from executing with disconnect concurrently.
                                synchronized (lockDisconnect) {
                                    // If user has indicated intention to disconnect,
                                    // We should no longer process messages from signalling server.
                                    if (connectionState == ConnectionState.DISCONNECT) return;
                                    mediaListener.onRemotePeerVideoToggle(mid, muted);
                                }
                            }
                        });
                    }
                }

            } else {

                connectionManager.logMessage("The message '" + message
                        + "' is not handled yet");

            }

        }

        @Override
        public void onClose() {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        connectionManager.logMessage("[SDK] onClose.");

                        lifeCycleListener.onDisconnect(ErrorCodes.DISCONNECT_UNEXPECTED_ERROR,
                                "Connection with the skylink server is closed");
                    }
                    // Disconnect from room
                    disconnectFromRoom();
                }
            });
        }

        @Override
        public void onError(final int code, final String description) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        final String message = "[SDK] onError: " + code + ", " + description;
                        connectionManager.logMessage(message);
                        lifeCycleListener.onDisconnect(ErrorCodes.DISCONNECT_UNEXPECTED_ERROR, message);
                    }

                    // Disconnect from room
                    disconnectFromRoom();
                }
            });
        }

    }

    void processWelcome(JSONObject objects) throws JSONException {
        String target = objects.getString("target");
        if (target
                .compareTo(webServerClient.getSid()) != 0)
            return;

        PeerInfo peerInfo = new PeerInfo();
        String mid = objects.getString("mid");
        try {
            peerInfo.setReceiveOnly(objects.getBoolean("receiveOnly"));
        } catch (JSONException e) {
        }

        try {
            peerInfo.setAgent(objects.getString("agent"));
            // SM0.1.0 - Browser version for web, SDK version for others.
            peerInfo.setVersion(objects.getString("version"));
            if (objects.has("enableIceTrickle")) {
                peerInfo.setEnableIceTrickle(objects.getBoolean("enableIceTrickle"));
            } else {
                // Work around for JS and/or other clients that do not yet implement this flag.
                peerInfo.setEnableIceTrickle(true);
            }
            if (objects.has("enableDataChannel")) {
                peerInfo.setEnableDataChannel(objects.getBoolean("enableDataChannel"));
            } else {
                // Work around for JS and/or other clients that do not yet implement this flag.
                peerInfo.setEnableDataChannel(true);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        peerInfoMap.put(mid, peerInfo);

        Object userData = "";

        try {
            userData = ((JSONObject) objects.get("userInfo"))
                    .get("userData");
        } catch (JSONException e) {
        }

        double weight = 0.0;
        try {
            weight = objects.getDouble("weight");
        } catch (JSONException e) {
        }

        PeerConnection peerConnection = null;
        List<Object> weightedConnection = getWeightedPeerConnection(mid, weight);
        if (!(Boolean) weightedConnection.get(0)) {
            Log.d(TAG, "Ignoring this welcome");
            return;
        }

        Object secondObject = weightedConnection.get(1);
        if (secondObject instanceof PeerConnection)
            peerConnection = (PeerConnection) secondObject;

        if (peerConnection == null) {
            logMessage("I only support "
                    + MAX_PEER_CONNECTIONS
                    + " connections are in this app. I am discarding this 'welcome'.");
            return;
        }

        setDisplayMap(userData, mid);

        boolean receiveOnly = false;
        try {
            receiveOnly = objects.getBoolean("receiveOnly");
        } catch (JSONException e) {
        }

        // Add our local media stream to this PC, or not.
        if ((myConfig.hasAudioSend() || myConfig.hasVideoSend()) && !receiveOnly) {
            peerConnection.addStream(localMediaStream);
            Log.d(TAG, "Added localMedia Stream");
        }

        logMessage("[SDK] onMessage - create offer.");
        // Create DataChannel if both Peer and ourself desires it.
        if (peerInfo.isEnableDataChannel() &&
                (myConfig.hasPeerMessaging() || myConfig.hasFileTransfer()
                        || myConfig.hasDataTransfer()))

        {
            // It is stored by dataChannelManager.
            dataChannelManager.createDataChannel(
                    peerConnection, target, mid, "", null, mid);
        }

        if (sdpObserverPool == null)
            sdpObserverPool = new Hashtable<String, SDPObserver>();
        SDPObserver sdpObserver = sdpObserverPool
                .get(mid);
        if (sdpObserver == null) {
            sdpObserver = new SkylinkConnection.SDPObserver();
            sdpObserver.setMyId(mid);
            sdpObserverPool.put(mid, sdpObserver);
        }

        peerConnection.createOffer(sdpObserver,
                sdpMediaConstraints);

        logMessage("PC - createOffer for " + mid);
    }


    /*
     * VideoRendererGui.VideoRendererGuiListener
     */
    private class MyVideoRendererGuiListener implements
            VideoRendererGuiListener {

        @Override
        public void updateDisplaySize(final GLSurfaceView surface,
                                      final Point screenDimensions, final String peerId) {
            runOnUiThread(new Runnable() {
                @SuppressWarnings("unused")
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnectMedia) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        mediaListener.onVideoSizeChange(peerId, screenDimensions);
                    }
                }
            });
        }

    }

    // Implementation detail: observe ICE & stream changes and react
// accordingly.
    class PCObserver implements PeerConnection.Observer {

        private SkylinkConnection connectionManager = SkylinkConnection.this;

        private double myWeight;
        private String myId;

        public double getMyWeight() {
            return myWeight;
        }

        @SuppressWarnings("unused")
        public void setMyWeight(double myWeight) {
            this.myWeight = myWeight;
        }

        @SuppressWarnings("unused")
        public String getMyId() {
            return myId;
        }

        public void setMyId(String myId) {
            this.myId = myId;
        }

        public PCObserver() {
            super();
            this.myWeight = new Random(new Date().getTime()).nextDouble()
                    * (double) 1000000;
        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        JSONObject json = new JSONObject();
                        try {
                            json.put("type", "candidate");
                            json.put("label", candidate.sdpMLineIndex);
                            json.put("id", candidate.sdpMid);
                            json.put("candidate", candidate.sdp);
                            json.put("mid",
                                    connectionManager.webServerClient.getSid());
                            json.put("rid",
                                    connectionManager.webServerClient.getRoomId());
                            json.put("target", PCObserver.this.myId);
                            connectionManager.webServerClient.sendMessage(json);
                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }
                }
            });
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState newState) {
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
            switch (newState) {
                case FAILED:
                    Log.d(TAG, "onIceConnectionChange : Failed - Restarting");
                    restartConnectionInternal(PCObserver.this.myId);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onIceGatheringChange(
                PeerConnection.IceGatheringState newState) {
            if (newState == PeerConnection.IceGatheringState.COMPLETE
                    && connectionManager.isMCUConnection)
                runOnUiThread(new Runnable() {
                    public void run() {
                        // Prevent thread from executing with disconnect concurrently.
                        synchronized (lockDisconnect) {
                            // If user has indicated intention to disconnect,
                            // We should no longer process messages from signalling server.
                            if (connectionState == ConnectionState.DISCONNECT) return;
                            SessionDescription sdp = connectionManager.peerConnectionPool
                                    .get(myId).getLocalDescription();
                            JSONObject json = new JSONObject();
                            try {
                                json.put("type", sdp.type.canonicalForm());
                                json.put("sdp", sdp.description);
                                json.put("mid",
                                        connectionManager.webServerClient.getSid());
                                json.put("target", myId);
                                json.put("rid", connectionManager.webServerClient
                                        .getRoomId());
                                connectionManager.webServerClient.sendMessage(json);
                            } catch (JSONException e) {
                                Log.e(TAG, e.getMessage(), e);
                            }
                        }
                    }
                });
        }

        @SuppressLint("NewApi")
        @Override
        public void onAddStream(final MediaStream stream) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        if (myConfig.hasVideoReceive() || myConfig.hasAudioReceive()) {
                            abortUnless(stream.audioTracks.size() <= 1
                                            && stream.videoTracks.size() <= 1,
                                    "Weird-looking stream: " + stream);
                            GLSurfaceView remoteVideoView = null;
                            if (stream.videoTracks.size() >= 1) {
                                remoteVideoView = new GLSurfaceView(applicationContext);

                                VideoRendererGui gui = new VideoRendererGui(remoteVideoView);
                                gui.setListener(connectionManager.videoRendererGuiListener);
                                // Set the peerID of the local video renderer
                                gui.setPeerId(myId);

                                VideoRenderer.Callbacks remoteRender = gui.create(0, 0,
                                        100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
                                stream.videoTracks.get(0).addRenderer(
                                        new VideoRenderer(remoteRender));

                                final GLSurfaceView rVideoView = remoteVideoView;
                                // connectionManager.surfaceOnHoldPool.put(rVideoView, myId);
                                if (!connectionManager.isPeerIdMCU(myId))
                                    mediaListener.onRemotePeerMediaReceive(myId, rVideoView);
                            } else {
                                // If this is an audio only stream, audio will be added automatically.
                                // Still, send a null videoView to alert user stream is received.
                                if (!connectionManager.isPeerIdMCU(myId))
                                    mediaListener.onRemotePeerMediaReceive(myId, null);
                            }
                        } else {
                            // If this is a no audio no video stream,
                            // still send a null videoView to alert user stream is received.
                            if (!connectionManager.isPeerIdMCU(myId))
                                mediaListener.onRemotePeerMediaReceive(myId, null);
                        }
                    }
                }
            });
        }

        @Override
        public void onRemoveStream(final MediaStream stream) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        stream.videoTracks.get(0).dispose();
                    }
                }
            });
        }

        @Override
        public void onDataChannel(final DataChannel dc) {
            PeerInfo peerInfo = peerInfoMap.get(this.myId);
            peerInfo.setEnableDataChannel(true);
            // Prevent thread from executing with disconnect concurrently.
            synchronized (lockDisconnect) {
                // If user has indicated intention to disconnect,
                // We should no longer process messages from signalling server.
                if (connectionState == ConnectionState.DISCONNECT) return;
                if (myConfig.hasPeerMessaging() || myConfig.hasFileTransfer()
                        || myConfig.hasDataTransfer()) {
                    // Create our DataChannel based on given dc.
                    // It is stored by dataChannelManager.
                    // Get PeerConnection.
                    PeerConnection pc = connectionManager.peerConnectionPool.get(this.myId);
                    String mid = connectionManager.webServerClient.getSid();
                    connectionManager.dataChannelManager.createDataChannel(pc,
                            this.myId, mid, "", dc, this.myId);
                }
            }
        }

        @Override
        public void onRenegotiationNeeded() {
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
        }
    }

    // Implementation detail: handle offer creation/signaling and answer
// setting,
// as well as adding remote ICE candidates once the answer SDP is set.
    private class SDPObserver implements SdpObserver {

        private SkylinkConnection connectionManager = SkylinkConnection.this;

        private SessionDescription localSdp;

        private String myId;

        @SuppressWarnings("unused")
        public String getMyId() {
            return myId;
        }

        public void setMyId(String myId) {
            this.myId = myId;
        }

        @Override
        public void onCreateSuccess(final SessionDescription origSdp) {
            final PeerConnection pc;
            final SessionDescription sdp;
            // Prevent thread from executing with disconnect concurrently.
            synchronized (lockDisconnectSdpCreate) {
                // If user has indicated intention to disconnect,
                // We should no longer process messages from signalling server.
                if (connectionState == ConnectionState.DISCONNECT) return;
                abortUnless(this.localSdp == null, "multiple SDP create?!?");

                // Set the preferred audio codec
                String sdpString = preferCodec(origSdp.description, AUDIO_CODEC_OPUS, true);
                sdp = new SessionDescription(origSdp.type, sdpString);
                this.localSdp = sdp;
                pc = connectionManager.peerConnectionPool
                        .get(this.myId);
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (connectionState == ConnectionState.DISCONNECT) return;

                        pc.setLocalDescription(SDPObserver.this, sdp);
                        if (!connectionManager.isMCUConnection)
                            sendLocalDescription(sdp);
                    }
                }
            });
        }

        @Override
        public void onSetSuccess() {
            final PeerConnection pc;
            // Prevent thread from executing with disconnect concurrently.
            synchronized (lockDisconnectSdp) {
                // If user has indicated intention to disconnect,
                // We should no longer process messages from signalling server.
                if (connectionState == ConnectionState.DISCONNECT) return;
                pc = connectionManager.peerConnectionPool.get(this.myId);
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnectSdpSet) {

                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        if (pc.signalingState() == PeerConnection.SignalingState.HAVE_REMOTE_OFFER) {
                            if (pc.getRemoteDescription() != null
                                    && pc.getLocalDescription() == null) {
                                connectionManager
                                        .logMessage("Callee, setRemoteDescription succeeded");
                                pc.createAnswer(SDPObserver.this,
                                        sdpMediaConstraints);
                                connectionManager.logMessage("PC - createAnswer.");
                            } else {
                                drainRemoteCandidates();
                            }
                        } else {
                            if (pc.getRemoteDescription() != null) {
                                connectionManager.logMessage("SDP onSuccess - drain candidates");
                                drainRemoteCandidates();
                                if (!connectionManager.isPeerIdMCU(myId)) {
                                    String tid = SDPObserver.this.myId;
                                    PeerInfo peerInfo = peerInfoMap.get(SDPObserver.this.myId);
                                    boolean eDC = false;
                                    if (peerInfo != null) eDC = peerInfo.isEnableDataChannel();
                                    remotePeerListener.onRemotePeerJoin(tid, connectionManager.displayNameMap.get(tid), eDC);
                                }
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onCreateFailure(final String error) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        throw new RuntimeException("createSDP error: " + error);
                    }
                }
            });
        }

        @Override
        public void onSetFailure(final String error) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (connectionState == ConnectionState.DISCONNECT) return;
                        throw new RuntimeException("setSDP error: " + error);
                    }
                }
            });
        }

        private void sendLocalDescription(SessionDescription sdp) {
            // Prevent thread from executing with disconnect concurrently.
            synchronized (lockDisconnectSdpSend) {
                // If user has indicated intention to disconnect,
                // We should no longer process messages from signalling server.
                if (connectionState == ConnectionState.DISCONNECT) return;
                JSONObject json = new JSONObject();
                try {
                    json.put("type", sdp.type.canonicalForm());
                    json.put("sdp", sdp.description);
                    json.put("mid", connectionManager.webServerClient.getSid());
                    json.put("target", this.myId);
                    json.put("rid", connectionManager.webServerClient.getRoomId());
                    connectionManager.webServerClient.sendMessage(json);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }

        private void drainRemoteCandidates() {
            // Prevent thread from executing with disconnect concurrently.
            synchronized (lockDisconnectSdpDrain) {
                // If user has indicated intention to disconnect,
                // We should no longer process messages from signalling server.
                if (connectionState == ConnectionState.DISCONNECT) return;
                connectionManager.logMessage("Inside SDPObserver.drainRemoteCandidates()");
            }
        }

    }

    public DataTransferListener getDataTransferListener() {
        return dataTransferListener;
    }

    public void setDataTransferListener(DataTransferListener dataTransferListener) {
        this.dataTransferListener = dataTransferListener;
    }

    protected void setDataChannelManager(DataChannelManager dataChannelManager) {
        this.dataChannelManager = dataChannelManager;
    }

    protected Map<String, Object> getDisplayNameMap() {
        return displayNameMap;
    }

    Map<String, SDPObserver> getSdpObserverPool() {
        return sdpObserverPool;
    }

    Map<String, PCObserver> getPcObserverPool() {
        return pcObserverPool;
    }

    Map<String, PeerConnection> getPeerConnectionPool() {
        return peerConnectionPool;
    }

}
