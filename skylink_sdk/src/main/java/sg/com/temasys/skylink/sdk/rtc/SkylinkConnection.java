package sg.com.temasys.skylink.sdk.rtc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaCodecVideoEncoder;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import sg.com.temasys.skylink.sdk.adapter.DataTransferAdapter;
import sg.com.temasys.skylink.sdk.adapter.FileTransferAdapter;
import sg.com.temasys.skylink.sdk.adapter.LifeCycleAdapter;
import sg.com.temasys.skylink.sdk.adapter.MediaAdapter;
import sg.com.temasys.skylink.sdk.adapter.MessagesAdapter;
import sg.com.temasys.skylink.sdk.adapter.RemotePeerAdapter;
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
    public static final String APP_SERVER = "http://api.temasys.com.sg/api/";

    private static final String TAG = "SkylinkConnection";

    private static final int MAX_PEER_CONNECTIONS = 4;
    private static final String MY_SELF = "me";

    private static boolean factoryStaticInitialized;

    public Context getApplicationContext() {
        return applicationContext;
    }

    private Context applicationContext;
    private boolean isMCUConnection;
    private DataChannelManager dataChannelManager;
    private Map<String, UserInfo> userInfoMap;
    private Map<String, sg.com.temasys.skylink.sdk.rtc.PeerInfo> peerInfoMap;
    private Map<String, PCObserver> pcObserverPool;
    private Map<String, PeerConnection> peerConnectionPool;
    private Map<String, SDPObserver> sdpObserverPool;

    private MediaStream localMediaStream;

    private Object myUserData;
    private UserInfo myUserInfo;
    private PeerConnectionFactory peerConnectionFactory;
    private String appKey;
    private SkylinkConfig myConfig;
    private VideoCapturerAndroid localVideoCapturer;
    private AudioSource localAudioSource;
    private AudioTrack localAudioTrack;
    private VideoSource localVideoSource;
    private VideoTrack localVideoTrack;

    // Skylink Services
    private SkylinkConnectionService skylinkConnectionService;
    private SkylinkPeerService skylinkPeerService;
    private SkylinkMediaService skylinkMediaService;

    private FileTransferListener fileTransferListener;
    private LifeCycleListener lifeCycleListener;
    private MediaListener mediaListener;
    private MessagesListener messagesListener;
    private RemotePeerListener remotePeerListener;
    private DataTransferListener dataTransferListener;

    private boolean roomLocked;


    // Lock objects to prevent threads from executing the following methods concurrently:
    // signalingServerClient.MessageHandler.onMessage
    // SkylinkConnection.disconnect
    // roomParameterServiceListener.onRoomParameterSuccessful(params);
    // appServerClientListener.onErrorAppServer(message)
    private Object lockDisconnect = new Object();
    private Object lockDisconnectMsg = new Object();
    private Object lockDisconnectMediaLocal = new Object();
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
     * Creates a new SkylinkConnection object with the specified parameters.
     *
     * @param appKey  The App key from the Skylink Developer Console
     * @param config  The SkylinkConfig object to configure the type of call.
     * @param context The application context
     */
    public void init(String appKey,
                     SkylinkConfig config, Context context) {
        logMessage("SkylinkConnection::config=>" + config);

        this.myConfig = new SkylinkConfig(config);

        logMessage("SkylinkConnection::appKey=>" + appKey);
        this.appKey = appKey;

        if (!factoryStaticInitialized) {

            boolean hardwareAccelerated = false;
            EGLContext eglContext = null;

            // Enable hardware acceleration if supported
            if (MediaCodecVideoEncoder.isVp8HwSupported()) {
                hardwareAccelerated = true;
                eglContext = VideoRendererGui.getEGLContext();
                Log.d(TAG, "Enabled hardware acceleration");
            }

            /*
            Note XR:
             PeerConnectionFactory.initializeAndroidGlobals to always use true for initializeAudio and initializeVideo, as otherwise, new PeerConnectionFactory() crashes.
            */
            abortUnless(PeerConnectionFactory.initializeAndroidGlobals(context,
                    true, true, hardwareAccelerated, eglContext
            ), "Failed to initializeAndroidGlobals");

            factoryStaticInitialized = true;
        }

        // Initialise Skylink Services
        if (this.skylinkPeerService == null) {
            this.skylinkPeerService = new SkylinkPeerService(this);
        }
        if (this.skylinkConnectionService == null) {
            this.skylinkConnectionService = new SkylinkConnectionService(this);
        }
        if (this.skylinkMediaService == null) {
            this.skylinkMediaService = new SkylinkMediaService(this, skylinkConnectionService);
            skylinkPeerService.setSkylinkMediaService(skylinkMediaService);
            skylinkPeerService.setSkylinkConnectionService(skylinkConnectionService);
        }

        skylinkMediaService.setVideoConstrains(this.myConfig);

        skylinkMediaService.genMediaConstraints(myConfig);

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

        if (skylinkConnectionService.isAlreadyConnected()) {
            return false;
        }

        this.myUserData = userData;
        this.myUserInfo = new UserInfo(myConfig, this.myUserData);

        // Fetch the current time from a server
        CurrentTimeService currentTimeService = new CurrentTimeService(new CurrentTimeServiceListener() {
            @Override
            public void onCurrentTimeFetched(Date date) {
                Log.d(TAG, "onCurrentTimeFetched" + date);
                String connectionString = Utils.getSkylinkConnectionString(roomName, appKey,
                        secret, date, DEFAULT_DURATION);
                connectToRoom(connectionString, userData);
            }

            @Override
            public void onCurrentTimeFetchedFailed() {
                Log.d(TAG, "onCurrentTimeFetchedFailed, using device time");
                String connectionString = Utils.getSkylinkConnectionString(roomName, appKey,
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
     * @param skylinkConnectionString SkylinkConnectionString Generated with room name, appKey,
     *                                secret, startTime and duration
     * @param userData                User defined data relating to oneself. May be a
     *                                'java.lang.String', 'org.json.JSONObject' or
     *                                'org.json.JSONArray'.
     * @return 'false' if the connection is already established
     */
    public boolean connectToRoom(String skylinkConnectionString, Object userData) {

        this.myUserData = userData;
        this.myUserInfo = new UserInfo(myConfig, this.myUserData);

        logMessage("SkylinkConnection::connectingRoom userData=>" + userData);

        if (skylinkConnectionService.isAlreadyConnected()) {
            return false;
        }

        // Initialise null Listeners to default values.
        if (this.fileTransferListener == null) {
            this.fileTransferListener = new FileTransferAdapter();
        }
        if (this.lifeCycleListener == null) {
            this.lifeCycleListener = new LifeCycleAdapter();
        }
        if (this.mediaListener == null) {
            this.mediaListener = new MediaAdapter();
        }
        if (this.messagesListener == null) {
            this.messagesListener = new MessagesAdapter();
        }
        if (this.remotePeerListener == null) {
            this.remotePeerListener = new RemotePeerAdapter();
        }
        if (this.dataTransferListener == null) {
            this.dataTransferListener = new DataTransferAdapter();
        }

        String url = APP_SERVER + skylinkConnectionString;
        try {
            this.skylinkConnectionService.connectToRoom(url);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        logMessage("SkylinkConnection::connection url=>" + url);

        // Start local media
        skylinkMediaService.startLocalMedia(lockDisconnectMediaLocal);

        return true;
    }

    /**
     * Locks the room if its not already locked
     */
    public void lockRoom() {
        if (!roomLocked) {
            try {
                ProtocolHelper.sendRoomLockStatus(this.skylinkConnectionService, true);
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
                ProtocolHelper.sendRoomLockStatus(this.skylinkConnectionService, false);
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
     *                     the message is to be sent to all our remote peers in the room.
     */
    public void restartConnection(String remotePeerId) {
        if (TextUtils.isEmpty(remotePeerId)) {
            // If remoteId is provided restart the specific peerConnection
            // Else restart all the peer connections
            if (pcObserverPool != null) {
                // Create a new peerId set to prevent concurrent modification of the set
                Set<String> peerIdSet = new HashSet<String>(pcObserverPool.keySet());
                for (String peerId : peerIdSet) {
                    skylinkConnectionService.restartConnectionInternal(peerId, this);
                }
            }
        } else {
            skylinkConnectionService.restartConnectionInternal(remotePeerId, this);
        }
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

    /**
     * Disconnects from the room we are currently in.
     */
    public void disconnectFromRoom() {
        // Prevent thread from executing with WebServerClient methods concurrently.
        synchronized (lockDisconnectMsg) {
            synchronized (lockDisconnectMediaLocal) {
                synchronized (lockDisconnectMedia) {
                    synchronized (lockDisconnectSdpDrain) {
                        synchronized (lockDisconnectSdpSend) {
                            synchronized (lockDisconnectSdpSet) {
                                synchronized (lockDisconnectSdpCreate) {
                                    synchronized (lockDisconnectSdp) {
                                        synchronized (lockDisconnect) {

                                            // Disconnect from the Signaling Channel only if connected.
                                            boolean canDisconnect = false;
                                            if (this.skylinkConnectionService != null) {
                                                canDisconnect = skylinkConnectionService.disconnect();
                                            }
                                            if (!canDisconnect) {
                                                return;
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

                                            if (this.userInfoMap != null) {
                                                this.userInfoMap.clear();
                                            }

                                            this.userInfoMap = null;

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
                                            this.skylinkConnectionService = null;
                                        }
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
        skylinkConnectionService.sendServerMessage(remotePeerId, message);
    }

    /**
     * Sends a user defined message to a specific remote peer or to all remote peers in a direct
     * peer to peer manner.
     *
     * @param remotePeerId Id of the remote peer to whom we will send a message. Use 'null' if the
     *                     message is to be sent to all our remote peers in the room.
     * @param message      User defined data. May be a 'java.lang.String', 'org.json.JSONObject' or
     *                     'org.json.JSONArray'.
     * @throws SkylinkException if the system was unable to send the message.
     */
    public void sendP2PMessage(String remotePeerId, Object message)
            throws SkylinkException {
        if (this.skylinkConnectionService == null)
            return;

        if (myConfig.hasPeerMessaging()) {
            if (remotePeerId == null) {
                Iterator<String> iPeerId = this.userInfoMap.keySet()
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
                        if (skylinkConnectionService.isDisconnected()) {
                            return;
                        }

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
        skylinkConnectionService.sendLocalUserData(userData);
    }

    /**
     * Mutes the local user's audio and notifies all the peers in the room.
     *
     * @param isMuted Flag that specifies whether audio should be mute
     */
    public void muteLocalAudio(boolean isMuted) {
        skylinkMediaService.muteLocalAudio(isMuted);
    }

    /**
     * Mutes the local user's video and notifies all the peers in the room.
     *
     * @param isMuted Flag that specifies whether video should be mute
     */
    public void muteLocalVideo(boolean isMuted) {
        skylinkMediaService.muteLocalVideo(isMuted);
    }

    /**
     * Sends request(s) to share file with a specific remote peer or to all remote peers in a direct
     * peer to peer manner in the same room.
     *
     * @param remotePeerId The id of the remote peer to send the file to. Use 'null' if the file is
     *                     to be sent to all our remote peers in the room.
     * @param fileName     The name of the file that is to be shared.
     * @param filePath     The absolute path of the file in the filesystem
     */
    public void sendFileTransferPermissionRequest(String remotePeerId, String fileName,
                                                  String filePath) throws SkylinkException {
        if (this.skylinkConnectionService == null)
            return;

        if (myConfig.hasFileTransfer()) {
            if (remotePeerId == null) {
                for (String iPeerId : userInfoMap.keySet()) {
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
                        if (skylinkConnectionService.isDisconnected()) {
                            return;
                        }

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
                Iterator<String> iPeerId = this.userInfoMap.keySet()
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
                        if (skylinkConnectionService.isDisconnected()) {
                            return;
                        }

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
        if (this.skylinkConnectionService == null) {
            return;
        }
        if (myConfig.hasFileTransfer()) {
            dataChannelManager.acceptFileTransfer(remotePeerId, isPermitted, filePath);
        }
    }

    /**
     * Call this method to switch between available camera.
     *
     * @return True or false based on whether the switch was successful or not.
     */
    public boolean switchCamera() {
        return skylinkMediaService.switchCamera(lifeCycleListener);
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
     * @return The data transfer listener object.
     */
    public DataTransferListener getDataTransferListener() {
        return dataTransferListener;
    }

    /**
     * Sets the specified data transfer listener object.
     *
     * @param dataTransferListener The data transfer listener object that will receive callbacks
     *                             related to DataTransfer
     */
    public void setDataTransferListener(DataTransferListener dataTransferListener) {
        this.dataTransferListener = dataTransferListener;
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
     *                           to remote Peers.
     */
    public void setRemotePeerListener(RemotePeerListener remotePeerListener) {
        if (remotePeerListener == null)
            this.remotePeerListener = new RemotePeerAdapter();
        else
            this.remotePeerListener = remotePeerListener;
    }


    /**
     * Retrieves the user defined data object associated with a peer.
     *
     * @param remotePeerId The id of the remote peer whose data is to be retrieved, or NULL for
     *                     self.
     * @return May be a 'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     */
    public Object getUserData(String remotePeerId) {
        Object userData = null;
        if (remotePeerId == null) {
            userData = this.myUserData;
        } else {
            if (this.userInfoMap != null) {
                UserInfo userInfo = getUserInfo(remotePeerId);
                userData = userInfo.getUserData();
            }
        }
        return userData;
    }

    /**
     * Sets the userdata to the relevant peer
     *
     * @param remotePeerId
     * @param userData
     */
    void setUserData(String remotePeerId, Object userData) {
        if (remotePeerId == null) {
            this.myUserData = userData;
        } else {
            if (this.userInfoMap != null) {
                UserInfo userInfo = getUserInfo(remotePeerId);
                if (userInfo != null) {
                    userInfo.setUserData(userData);
                }
            }
        }
    }

    /**
     * Retrieves the UserInfo object associated with a remote peer.
     *
     * @param remotePeerId The id of the remote peer whose userInfo is to be retrieved.
     * @return UserInfo
     */
    public UserInfo getUserInfo(String remotePeerId) {
        if (remotePeerId == null) {
            return this.myUserInfo;
        } else {
            return this.userInfoMap.get(remotePeerId);
        }
    }

    void setUserInfo(String peerId, UserInfo userInfo) {
        if (this.userInfoMap == null) {
            this.userInfoMap = new Hashtable<String, UserInfo>();
        }
        this.userInfoMap.put(peerId, userInfo);
    }

    void logMessage(String message) {
        Log.d(TAG, message);
    }

    // Poor-man's assert(): die with |msg| unless |condition| is true.
    static void abortUnless(boolean condition, String msg) {
        if (!condition) {
            throw new RuntimeException(msg);
        }
    }

    List<Object> getWeightedPeerConnection(String key, double weight) {
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
                    // Use this welcome (ours will be discarded on peer's side).
                    resultList.add(new Boolean(true));
                    resultList.add(getPeerConnection(key, HealthChecker.ICE_ROLE_OFFERER));
                } else {
                    // Discard this welcome (ours will be used on peer's side).
                    resultList.add(new Boolean(false));
                    resultList.add(new Boolean(false));
                }
            } else {
                // Use this welcome (we did not send one to the peer).
                resultList.add(new Boolean(true));
                resultList.add(getPeerConnection(key, HealthChecker.ICE_ROLE_OFFERER));
            }
        } else {
            // Peer did not send a weight, use peer's welcome.
            resultList.add(new Boolean(true));
            resultList.add(getPeerConnection(key, HealthChecker.ICE_ROLE_OFFERER));
        }
        return resultList;
    }

    PeerConnection getPeerConnection(String key) {
        return getPeerConnection(key, "");
    }

    PeerConnection getPeerConnection(String key, String iceRole) {
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
            if ("".equals(iceRole)) {
                try {
                    throw new SkylinkException(
                            "Trying to get an existing PeerConnection for " + key +
                                    ", but which does not exist!");
                } catch (SkylinkException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
            PCObserver pcObserver = new SkylinkConnection.PCObserver();
            pcObserver.setMyId(key);

            // Prevent thread from executing with disconnect concurrently.
            synchronized (lockDisconnect) {
                pc = this.peerConnectionFactory.createPeerConnection(
                        skylinkConnectionService.getIceServers(),
                        skylinkMediaService.getPcConstraints(), pcObserver);
                logMessage("Created a new peer connection");
            }
            /*if (this.myConfig.hasAudio())
                pc.addStream(this.localMediaStream, this.pcConstraints);*/

            pcObserver.setPc(pc);
            // Initialise and start Health Checker.
            pcObserver.initialiseHealthChecker(iceRole);

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

    boolean isPeerIdMCU(String peerId) {
        return peerId.startsWith("MCU");
    }


    // Implementation detail: observe ICE & stream changes and react
// accordingly.
    class PCObserver implements PeerConnection.Observer {

        private SkylinkConnection connectionManager = SkylinkConnection.this;

        private PeerConnection pc;
        private double myWeight;
        private String myId;
        private HealthChecker healthChecker;

        public PeerConnection getPc() {
            return pc;
        }

        public void setPc(PeerConnection pc) {
            this.pc = pc;
        }

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

        void initialiseHealthChecker(String iceRole) {
            healthChecker = new HealthChecker(myId, connectionManager, connectionManager.skylinkConnectionService, connectionManager.localMediaStream, connectionManager.myConfig, pc);
            healthChecker.setIceRole(iceRole);
            healthChecker.startRestartTimer();
        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (skylinkConnectionService.isDisconnected()) {
                            return;
                        }

                        ProtocolHelper.sendCandidate(connectionManager.skylinkConnectionService, candidate,
                                PCObserver.this.myId);
                    }
                }
            });
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState newState) {
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
            healthChecker.setIceState(newState);
            Log.d(TAG, "Peer " + myId + " : onIceConnectionChange : iceState : " + newState + ".");
            switch (newState) {
                case NEW:
                    break;
                case CHECKING:
                    break;
                case CONNECTED:
                    break;
                case COMPLETED:
                    break;
                case DISCONNECTED:
                    break;
                case CLOSED:
                    break;
                case FAILED:
                    // restartConnectionInternal(PCObserver.this.myId);
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
                        synchronized (lockDisconnectSdpSend) {
                            // If user has indicated intention to disconnect,
                            // We should no longer process messages from signalling server.
                            if (skylinkConnectionService.isDisconnected()) {
                                return;
                            }

                            SessionDescription sdp = connectionManager.peerConnectionPool
                                    .get(myId).getLocalDescription();
                            ProtocolHelper.sendSdp(connectionManager.skylinkConnectionService,
                                    sdp,
                                    PCObserver.this.myId);
                        }
                    }
                });
        }

        @SuppressLint("NewApi")
        @Override
        public void onAddStream(final MediaStream stream) {
            skylinkMediaService.addMediaStream(stream, this.myId, lockDisconnectMediaLocal);
        }

        @Override
        public void onRemoveStream(final MediaStream stream) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnect) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (skylinkConnectionService.isDisconnected()) {
                            return;
                        }

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
                if (skylinkConnectionService.isDisconnected()) {
                    return;
                }

                if (myConfig.hasPeerMessaging() || myConfig.hasFileTransfer()
                        || myConfig.hasDataTransfer()) {
                    // Create our DataChannel based on given dc.
                    // It is stored by dataChannelManager.
                    // Get PeerConnection.
                    PeerConnection pc = connectionManager.peerConnectionPool.get(this.myId);
                    String mid = connectionManager.skylinkConnectionService.getSid();
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
    class SDPObserver implements SdpObserver {

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
                if (skylinkConnectionService.isDisconnected()) {
                    return;
                }

                abortUnless(this.localSdp == null, "multiple SDP create?!?");

                String sdpType = origSdp.type.canonicalForm();

                // Set the preferred audio codec
                String sdpString = Utils.preferCodec(origSdp.description,
                        myConfig.getPreferredAudioCodec().toString(), true);

                // Modify stereo audio in the SDP if required
                sdpString = Utils.modifyStereoAudio(sdpString, myConfig);

                // If answer, may need to mangle to respect our own MediaConstraints:
                /* Note XR:
                The webrtc designed behaviour seems to be that if an offerer SDP indicates to send media,
                the answerer will generate an SDP to accept it, even if the answerer had put in its
                MediaConstraints not to accept that media (provided it sends that media):
                https://code.google.com/p/webrtc/issues/detail?id=2404
                Hence, for our answerer to respect its own MediaConstraints, the answer SDP will be
                mangled (if required) to respect the MediaConstraints (sdpMediaConstraints).
                */
                if ("answer".equals(sdpType)) {
                    if (!myConfig.hasAudioReceive() && myConfig.hasAudioSend()) {
                        sdpString = Utils.sdpAudioSendOnly(sdpString);
                    }
                    if (!myConfig.hasVideoReceive() && myConfig.hasVideoSend()) {
                        sdpString = Utils.sdpVideoSendOnly(sdpString);
                    }
                }

                sdp = new SessionDescription(origSdp.type, sdpString);

                this.localSdp = sdp;
                pc = connectionManager.peerConnectionPool
                        .get(this.myId);
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnectSdpSend) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (skylinkConnectionService.isDisconnected()) {
                            return;
                        }


                        pc.setLocalDescription(SDPObserver.this, sdp);
                        if (!connectionManager.isMCUConnection)
                            ProtocolHelper.sendSdp(connectionManager.skylinkConnectionService,
                                    sdp, SDPObserver.this.myId);
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
                if (skylinkConnectionService.isDisconnected()) {
                    return;
                }

                pc = connectionManager.peerConnectionPool.get(this.myId);
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (lockDisconnectSdpSet) {

                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (skylinkConnectionService.isDisconnected()) {
                            return;
                        }

                        if (pc.signalingState() == PeerConnection.SignalingState.HAVE_REMOTE_OFFER) {
                            if (pc.getRemoteDescription() != null
                                    && pc.getLocalDescription() == null) {
                                connectionManager
                                        .logMessage("Callee, setRemoteDescription succeeded");
                                pc.createAnswer(SDPObserver.this,
                                        skylinkMediaService.getSdpMediaConstraints());
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
                                    remotePeerListener.onRemotePeerJoin(tid, connectionManager.getUserData(tid), eDC);
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
                        if (skylinkConnectionService.isDisconnected()) {
                            return;
                        }

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
                        if (skylinkConnectionService.isDisconnected()) {
                            return;
                        }

                        throw new RuntimeException("setSDP error: " + error);
                    }
                }
            });
        }

        private void drainRemoteCandidates() {
            // Prevent thread from executing with disconnect concurrently.
            synchronized (lockDisconnectSdpDrain) {
                // If user has indicated intention to disconnect,
                // We should no longer process messages from signalling server.
                if (skylinkConnectionService.isDisconnected()) {
                    return;
                }

                connectionManager.logMessage("Inside SDPObserver.drainRemoteCandidates()");
            }
        }

    }

    // Getters and Setters

    SkylinkPeerService getSkylinkPeerService() {
        return skylinkPeerService;
    }

    MediaStream getLocalMediaStream() {
        return localMediaStream;
    }

    void setLocalMediaStream(MediaStream localMediaStream) {
        this.localMediaStream = localMediaStream;
    }

    VideoCapturerAndroid getLocalVideoCapturer() {
        return localVideoCapturer;
    }

    void setLocalVideoCapturer(VideoCapturerAndroid localVideoCapturer) {
        this.localVideoCapturer = localVideoCapturer;
    }

    static int getMaxPeerConnections() {
        return MAX_PEER_CONNECTIONS;
    }

    void setUserData(Object myUserData) {
        this.myUserData = myUserData;
    }

    AudioSource getLocalAudioSource() {
        return localAudioSource;
    }

    void setLocalAudioSource(AudioSource localAudioSource) {
        this.localAudioSource = localAudioSource;
    }

    VideoSource getLocalVideoSource() {
        return localVideoSource;
    }

    void setLocalVideoSource(VideoSource localVideoSource) {
        this.localVideoSource = localVideoSource;
    }

    AudioTrack getLocalAudioTrack() {
        return localAudioTrack;
    }

    void setLocalAudioTrack(AudioTrack localAudioTrack) {
        this.localAudioTrack = localAudioTrack;
    }

    VideoTrack getLocalVideoTrack() {
        return localVideoTrack;
    }

    void setLocalVideoTrack(VideoTrack localVideoTrack) {
        this.localVideoTrack = localVideoTrack;
    }

    Object getLockDisconnectMediaLocal() {
        return lockDisconnectMediaLocal;
    }

    Object getLockDisconnectMedia() {
        return lockDisconnectMedia;
    }

    Object getLockDisconnectMsg() {
        return lockDisconnectMsg;
    }

    PeerConnectionFactory getPeerConnectionFactory() {
        return peerConnectionFactory;
    }

    void setPeerConnectionFactory(PeerConnectionFactory peerConnectionFactory) {
        this.peerConnectionFactory = peerConnectionFactory;
    }

    void setDataChannelManager(DataChannelManager dataChannelManager) {
        this.dataChannelManager = dataChannelManager;
    }

    SDPObserver getSdpObserver(String mid) {

        if (sdpObserverPool == null) {
            sdpObserverPool = new Hashtable<String, SDPObserver>();
        }

        SDPObserver sdpObserver = sdpObserverPool.get(mid);
        if (sdpObserver == null) {
            sdpObserver = new SkylinkConnection.SDPObserver();
            sdpObserver.setMyId(mid);
            sdpObserverPool.put(mid, sdpObserver);
        }

        return sdpObserver;
    }

    // Initialize all PC related maps.
    void initializePcRelatedMaps() {
        peerConnectionPool = new Hashtable<String, PeerConnection>();
        pcObserverPool = new Hashtable<String, PCObserver>();
        sdpObserverPool = new Hashtable<String, SDPObserver>();
        userInfoMap = new Hashtable<String, UserInfo>();
        peerInfoMap = new Hashtable<String, PeerInfo>();
    }

    Map<String, UserInfo> getUserInfoMap() {
        return userInfoMap;
    }

    Map<String, SDPObserver> getSdpObserverPool() {
        return sdpObserverPool;
    }

    void setSdpObserverPool(Map<String, SDPObserver> sdpObserverPool) {
        this.sdpObserverPool = sdpObserverPool;
    }

    Map<String, PCObserver> getPcObserverPool() {
        return pcObserverPool;
    }

    Map<String, PeerConnection> getPeerConnectionPool() {
        return peerConnectionPool;
    }

    Map<String, PeerInfo> getPeerInfoMap() {
        return peerInfoMap;
    }

    Object getLockDisconnect() {
        return lockDisconnect;
    }

    SkylinkConnectionService getSkylinkConnectionService() {
        return skylinkConnectionService;
    }

    SkylinkConfig getMyConfig() {
        return myConfig;
    }

    DataChannelManager getDataChannelManager() {
        return dataChannelManager;
    }

    Object getMyUserData() {
        return myUserData;
    }

    void setRoomLocked(boolean roomLocked) {
        this.roomLocked = roomLocked;
    }

    boolean isRoomLocked() {
        return roomLocked;
    }

}
