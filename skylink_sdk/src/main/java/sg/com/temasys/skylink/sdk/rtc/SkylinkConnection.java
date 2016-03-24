package sg.com.temasys.skylink.sdk.rtc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import sg.com.temasys.skylink.sdk.BuildConfig;
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

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logE;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logI;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logV;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logW;

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

    private static final String TAG = SkylinkConnection.class.getName();
    private static SkylinkConnection instance;
    private PcShared pcShared;
    private String appKey;
    private SkylinkConfig skylinkConfig;

    // Skylink Services
    private SkylinkConnectionService skylinkConnectionService;
    SkylinkPeerService skylinkPeerService;
    private SkylinkMediaService skylinkMediaService;
    private DataChannelManager dataChannelManager;

    // Skylink Listeners
    private FileTransferListener fileTransferListener;
    private LifeCycleListener lifeCycleListener;
    private MediaListener mediaListener;
    private MessagesListener messagesListener;
    private RemotePeerListener remotePeerListener;
    private DataTransferListener dataTransferListener;

    // Room variables
    private boolean isMcuRoom;
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

    public Object getLockDisconnectSdp() {
        return lockDisconnectSdp;
    }

    public Object getLockDisconnectSdpCreate() {
        return lockDisconnectSdpCreate;
    }

    public Object getLockDisconnectSdpSet() {
        return lockDisconnectSdpSet;
    }

    public Object getLockDisconnectSdpDrain() {
        return lockDisconnectSdpDrain;
    }

    public Object getLockDisconnectSdpSend() {
        return lockDisconnectSdpSend;
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
        // Log SDK version.
        String info = "Skylink SDK for Android is at version: " + BuildConfig.VERSION_NAME + ".";
        logI(TAG, info);

        this.appKey = appKey;
        logD(TAG, "[SkylinkConnection.init] appKey: " + appKey);

        this.skylinkConfig = config;
        logD(TAG, "[SkylinkConnection.init] Config:\n" + config);

        // Log SDK logging levels enabled.
        logV(TAG, "Log level enabled: Verbose | isEnableLogs: " + skylinkConfig.isEnableLogs());
        logD(TAG, "Log level enabled: Debug   | isEnableLogs: " + skylinkConfig.isEnableLogs());
        logI(TAG, "Log level enabled: Info    | isEnableLogs: " + skylinkConfig.isEnableLogs());
        logW(TAG, "Log level enabled: Warn    | isEnableLogs: " + skylinkConfig.isEnableLogs());
        logE(TAG, "Log level enabled: Error   | isEnableLogs: " + skylinkConfig.isEnableLogs());

        // Initialise Skylink Services
        pcShared = new PcShared(context);

        if (this.skylinkPeerService == null) {
            this.skylinkPeerService = new SkylinkPeerService(this, pcShared);
        }
        if (this.skylinkConnectionService == null) {
            this.skylinkConnectionService = new SkylinkConnectionService(this);
        }
        if (this.skylinkMediaService == null) {
            this.skylinkMediaService = new SkylinkMediaService(this, pcShared);
        }

        // Set MediaConstraints
        // For local media
        skylinkMediaService.setVideoConstrains(this.skylinkConfig);
        // For PC and SDP
        skylinkMediaService.genMediaConstraints(skylinkConfig);

        // Instantiate DataChannelManager.
        if (this.skylinkConfig.hasPeerMessaging() || this.skylinkConfig.hasFileTransfer()
                || this.skylinkConfig.hasDataTransfer()) {
            this.dataChannelManager = new DataChannelManager(this,
                    this.skylinkConfig.getTimeout(), skylinkConfig.hasPeerMessaging(),
                    skylinkConfig.hasFileTransfer());
            this.dataChannelManager.setConnectionManager(this);
        }
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

        // Fetch the current time from a server
        CurrentTimeService currentTimeService = new CurrentTimeService(new CurrentTimeServiceListener() {
            @Override
            public void onCurrentTimeFetched(Date date) {
                String info = "Got time from a server, " +
                        "connecting to room using server's time: " + date.toString() + ".";
                logI(TAG, info);
                connectToRoomWithDate(date);
            }

            @Override
            public void onCurrentTimeFetchedFailed() {
                Date date = new Date();
                String warn = "[WARN] Failed to get time from a server, " +
                        "connecting to room using device's time: " + date.toString() + "!";
                logW(TAG, warn);
                connectToRoomWithDate(date);
            }

            /**
             * Connect to room with a particular start time.
             * @param startTime Time the room will become valid.
             */
            private void connectToRoomWithDate(Date startTime) {
                String connectionString = Utils.getSkylinkConnectionString(roomName, appKey,
                        secret, startTime, DEFAULT_DURATION);
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

        // Set our UserData
        skylinkPeerService.setUserData(null, userData);

        logD(TAG, "[SkylinkConnection.connectToRoom] userData:\n" + userData);

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

        this.skylinkConnectionService.connectToRoom(skylinkConnectionString);

        // Start local media
        skylinkMediaService.startLocalMedia(lockDisconnectMediaLocal);
        return true;
    }

    /**
     * Locks the room if its not already locked
     */
    public void lockRoom() {
        if (!roomLocked) {
            ProtocolHelper.sendRoomLockStatus(this.skylinkConnectionService, true);
            roomLocked = true;
        }
    }

    /**
     * Unlocks the room if its already locked
     */
    public void unlockRoom() {
        if (roomLocked) {
            ProtocolHelper.sendRoomLockStatus(this.skylinkConnectionService, false);
            roomLocked = false;
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
            if (skylinkPeerService.getPeerNumber() > 0) {
                // Create a new peerId set to prevent concurrent modification of the set
                Set<String> peerIdSet = new HashSet<String>(skylinkPeerService.getPeerIdSet());
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
     * Disconnects from the room we are currently in.
     * <p/>
     * Once disconnect is complete, {@link LifeCycleListener#onDisconnect(int, String)}
     * will be called.
     * <p/>
     * To connect to a room after this,
     * {@link sg.com.temasys.skylink.sdk.rtc.SkylinkConnection#init(String, SkylinkConfig, Context)}
     * and {@link #connectToRoom(String, Object)} will have to be called.
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

                                            String debug = "[SkylinkConnection.disconnectFromRoom] "
                                                    + "Starting method after passing locks and checks.";
                                            logD(TAG, debug);

                                            // Dispose and remove all Peers
                                            skylinkPeerService.removeAllPeers(ProtocolHelper
                                                    .DISCONNECTING, true);

                                            // Dispose and remove local media streams, sources and
                                            // tracks
                                            skylinkMediaService.removeLocalMedia();

                                            //
                                            // To avoid crash on next connection creating PCFactory,
                                            // after dispose and remove PeerConnectionFactory,
                                            // ensure no Handler remains on threads other than this.
                                            pcShared.removePcFactory();

                                            this.skylinkMediaService = null;
                                            this.skylinkPeerService = null;
                                            this.skylinkConnectionService = null;

                                            if (this.lifeCycleListener != null) {
                                                this.lifeCycleListener.onDisconnect(
                                                        Errors.DISCONNECT_FROM_ROOM,
                                                        "User disconnected from the room");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        String debug = "[SkylinkConnection.disconnectFromRoom] "
                + "Completed disconnection and method.";
        logD(TAG, debug);
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

        if (skylinkConfig.hasPeerMessaging()) {
            if (remotePeerId == null) {
                // If MCU in room, it will broadcast so no need to send to everyone.
                if (isMcuRoom) {
                    if (!dataChannelManager.sendDcChat(false, message, null)) {
                        throw new SkylinkException(
                                "Unable to send the message via data channel");
                    }
                } else {

                    for (Peer peer : this.skylinkPeerService.getPeerCollection()) {
                        String tid = peer.getPeerId();
                        PeerInfo peerInfo = peer.getPeerInfo();
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
                }
            } else {
                String tid = remotePeerId;
                Peer peer = skylinkPeerService.getPeer(tid);
                PeerInfo peerInfo = peer.getPeerInfo();
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
     * Sends request(s) to share file with a specific remote peer or to all remote peers in a
     * direct
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

        if (skylinkConfig.hasFileTransfer()) {
            if (remotePeerId == null) {
                // Send to all Peers
                // If MCU in room, it will broadcast so no need to send to everyone.
                if (isMcuRoom) {
                    String tid = remotePeerId;
                    String sendStatus =
                            dataChannelManager.sendFileTransferRequest(tid, fileName, filePath);
                    if (!"".equals(sendStatus)) {
                        String error = "[sendFileTransferPermissionRequest] Unable to send " +
                                "request to share file: " + fileName + ". SendStatus: " + sendStatus;
                        logE(TAG, error);
                        throw new SkylinkException(sendStatus);
                    }
                } else {
                    // Send a WRQ to each Peer.
                    for (Peer peer : this.skylinkPeerService.getPeerCollection()) {
                        String tid = peer.getPeerId();
                        PeerInfo peerInfo = peer.getPeerInfo();
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
                            String sendStatus =
                                    dataChannelManager.sendFileTransferRequest(tid, fileName, filePath);
                            if (!"".equals(sendStatus)) {
                                String error = "Unable to send file share request to Peer " + tid +
                                        "!" + "\nReason: " + sendStatus + "\nHence not sharing file.";
                                throw new SkylinkException(error);
                            }
                        }
                    }
                }
            } else {
                // Send to specific Peer.
                String tid = remotePeerId;
                Peer peer = skylinkPeerService.getPeer(tid);
                PeerInfo peerInfo = peer.getPeerInfo();
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
                    String sendStatus =
                            dataChannelManager.sendFileTransferRequest(tid, fileName, filePath);
                    if (!"".equals(sendStatus)) {
                        String error = "Unable to send file share request to Peer " + tid +
                                "!" + "\nReason: " + sendStatus + "\nHence not sharing file.";
                        throw new SkylinkException(error);
                    }
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
     * The maximum of size of the byte array is 65456 bytes.
     *
     * @param remotePeerId remotePeerID of a specified peer
     * @param data         Array of bytes
     * @throws SkylinkException
     */
    public void sendData(String remotePeerId, byte[] data) throws SkylinkException {
        if (skylinkConfig != null && skylinkConfig.hasDataTransfer()) {
            // Hack for initial MCU release
            if (isMcuRoom) {
                    /*dataChannelManager.sendDataToPeer(null, data);*/
                throw new UnsupportedOperationException(
                        "In this SDK version, we are unable to send binary data " +
                                "when the room has a MCU.");
            }
            if (remotePeerId == null) {
                // If MCU in room, it will broadcast so no need to send to everyone.
                if (isMcuRoom) {
                    dataChannelManager.sendDataToPeer(null, data);
                } else {
                    for (Peer peer : this.skylinkPeerService.getPeerCollection()) {
                        String tid = peer.getPeerId();
                        PeerInfo peerInfo = peer.getPeerInfo();
                        if (peerInfo == null) {
                            throw new SkylinkException(
                                    "Unable to send data to Peer " + tid +
                                            " as the Peer is no longer in room or has missing PeerInfo.");
                        } else {
                            if (!peerInfo.isEnableDataChannel()) {
                                throw new SkylinkException(
                                        "Unable to send data to Peer " + tid +
                                                " as the Peer has not enabled data channel.");
                            }
                            dataChannelManager.sendDataToPeer(tid, data);
                        }
                    }
                }
            } else {
                String tid = remotePeerId;
                Peer peer = skylinkPeerService.getPeer(tid);
                PeerInfo peerInfo = peer.getPeerInfo();
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
        if (skylinkConfig.hasFileTransfer()) {
            dataChannelManager.acceptFileTransfer(remotePeerId, isPermitted, filePath);
        }
    }

    /**
     * Call this method to switch between available camera.
     *
     * @return True or false based on whether the switch was successful or not.
     */
    public boolean switchCamera() {
        return skylinkMediaService.switchCameraAndRender(lifeCycleListener);
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
     * @param remotePeerListener The remote peer listener object that will receive callbacks
     *                           related
     *                           to remote Peers.
     */
    public void setRemotePeerListener(RemotePeerListener remotePeerListener) {
        if (remotePeerListener == null)
            this.remotePeerListener = new RemotePeerAdapter();
        else
            this.remotePeerListener = remotePeerListener;
    }

    /**
     * Retrieves the user defined data object of a peer.
     *
     * @param remotePeerId The id of the remote peer whose UserData is to be retrieved, or NULL for
     *                     self.
     * @return May be a 'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     */
    public Object getUserData(String remotePeerId) {
        return skylinkPeerService.getUserData(remotePeerId);
    }

    /**
     * Sets the userdata for a peer
     *
     * @param remotePeerId The id of the remote peer whose UserData is to be set, or NULL for self.
     * @param userData
     */
    void setUserData(String remotePeerId, Object userData) {
        skylinkPeerService.setUserData(remotePeerId, userData);
    }

    /**
     * Retrieves the UserInfo object of a Peer.
     *
     * @param remotePeerId The id of the remote peer whose UserInfo is to be retrieved, or NULL for
     *                     self.
     * @return UserInfo
     */
    public UserInfo getUserInfo(String remotePeerId) {
        return skylinkPeerService.getUserInfo(remotePeerId);
    }

    // Internal methods
    /*void logD(String tag, String message) {
        logD(TAG, message);
    }*/

    // Getters and Setters

    public Handler getHandler() {
        return handler;
    }

    boolean isMcuRoom() {
        return isMcuRoom;
    }

    void setIsMcuRoom(boolean isMcuRoom) {
        this.isMcuRoom = isMcuRoom;
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

    PcShared getPcShared() {
        return pcShared;
    }

    SkylinkMediaService getSkylinkMediaService() {
        return skylinkMediaService;
    }

    SkylinkPeerService getSkylinkPeerService() {
        return skylinkPeerService;
    }

    void setSkylinkPeerService(SkylinkPeerService skylinkPeerService) {
        this.skylinkPeerService = skylinkPeerService;
    }

    Object getLockDisconnect() {
        return lockDisconnect;
    }

    SkylinkConnectionService getSkylinkConnectionService() {
        return skylinkConnectionService;
    }

    SkylinkConfig getSkylinkConfig() {
        return skylinkConfig;
    }

    DataChannelManager getDataChannelManager() {
        return dataChannelManager;
    }

    void setDataChannelManager(DataChannelManager dataChannelManager) {
        this.dataChannelManager = dataChannelManager;
    }

    boolean isRoomLocked() {
        return roomLocked;
    }

    void setRoomLocked(boolean roomLocked) {
        this.roomLocked = roomLocked;
    }

}
