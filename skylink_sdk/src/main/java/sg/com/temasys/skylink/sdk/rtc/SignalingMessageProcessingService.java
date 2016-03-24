package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logE;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logI;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logV;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logW;


/**
 * Purpose of this class to handle signaling messages and create message processors to handle
 * different signaling messages
 * <p/>
 * Created by janidu on 27/4/15.
 */
class SignalingMessageProcessingService implements SignalingServerClientListener {

    private static final String TAG = SignalingMessageProcessingService.class.getName();

    private final SkylinkConnection skylinkConnection;
    private SkylinkConnectionService skylinkConnectionService;
    private SignalingServerClient socketIOClient;
    private SignalingServerMessageSender sigMsgSender;
    private SignalingMessageListener signalingMessageListener;
    private final MessageProcessorFactory messageProcessorFactory;

    public SignalingMessageProcessingService(SkylinkConnection skylinkConnection,
                                             SkylinkConnectionService skylinkConnectionService,
                                             MessageProcessorFactory messageProcessorFactory,
                                             SignalingMessageListener signalingMessageListener) {
        this.messageProcessorFactory = messageProcessorFactory;
        this.skylinkConnection = skylinkConnection;
        this.skylinkConnectionService = skylinkConnectionService;
        this.signalingMessageListener = signalingMessageListener;
    }

    public void connect(String signalingIp, int signalingPort, String socketId, String roomId) {

        String info = "Connecting to the room.";
        String debug = info + " I.e. Connecting to the signaling server.";
        logI(TAG, info);
        logD(TAG, debug);

        socketIOClient = new SignalingServerClient(this,
                "https://" + signalingIp, signalingPort);

        // Create SignalingServerMessageSender if is not yet created.
        if (sigMsgSender == null) {
            sigMsgSender = new SignalingServerMessageSender(socketId, roomId, socketIOClient);
        }
    }

    public void sendMessage(JSONObject dictMessage) {
        sigMsgSender.sendMessage(dictMessage);
    }

    // Disconnect from the Signaling Channel.
    public void disconnect() {
        String info = "Disconnecting from the room.";
        String debug = info + " I.e. Disconnecting from the signaling server.";
        logI(TAG, info);
        logD(TAG, debug);
        if (socketIOClient != null) {
            Socket socketIO = socketIOClient.getSocketIO();
            socketIO.off();
            if (socketIO.connected()) {
                socketIO.disconnect();
            }
        }
    }

    /**
     * SignalingServerClientListener implementation
     */

    @Override
    public void onOpen() {
        // Prevent thread from executing with disconnect concurrently.
        synchronized (skylinkConnection.getLockDisconnect()) {
            // If user has indicated intention to disconnect,
            // We should no longer process messages from signalling server.
            if (skylinkConnection.getSkylinkConnectionService().getConnectionState() ==
                    SkylinkConnectionService.ConnectionState.DISCONNECTING) {
                return;
            }
            skylinkConnectionService.setConnectionState(
                    SkylinkConnectionService.ConnectionState.CONNECTED);
            // Inform Listener
            signalingMessageListener.onConnectedToRoom();
        }
    }

    @Override
    public void onMessage(String data) {
        // Prevent thread from executing with disconnect concurrently.
        synchronized (skylinkConnection.getLockDisconnectMsg()) {
            // If user has indicated intention to disconnect,
            // We should no longer process messages from signalling server.
            if (skylinkConnection.getSkylinkConnectionService().getConnectionState() ==
                    SkylinkConnectionService.ConnectionState.DISCONNECTING) {
                return;
            }
            logD(TAG, "[onMessage] raw data:\n" + data);
            try {
                // Instantiate the relevant message processor
                JSONObject object = new JSONObject(data);

                String type = object.getString("type");
                String target = object.has("target") ? object.getString("target") : "";

                // If the target exist it should be the same for this client
                if (object.has("target") && !target.equals(skylinkConnectionService.getSid())) {
                    String warn = "[ERROR:" + Errors.SIG_MSG_TARGETED_FOR_OTHERS + "]";
                    String debug = warn + "Ignoring the message: \"" + data +
                            "\".\n" +
                            "Due to target mismatch." +
                            "\"target\": " + target + ". \"type\": " + type;
                    logW(TAG, warn);
                    logD(TAG, debug);
                    return;
                }

                MessageProcessor messageProcessor = messageProcessorFactory.
                        getMessageProcessor(type);

                if (messageProcessor != null) {

                    messageProcessor.setSkylinkConnection(skylinkConnection);
                    logV(TAG, "Processing message type " + type + " with "
                            + messageProcessor.getClass().getName());

                    messageProcessor.process(object);

                    logD(TAG, "Processed message type " + type + " with "
                            + messageProcessor.getClass().getName());
                } else {
                    String error = "[ERROR:" + Errors.SIG_MSG_UNKNOWN_TYPE + "] Ignoring some " +
                            "unknown message type from the Server!";
                    String debug = error + "\nIgnoring the message: \"" + data +
                            "\".\n" +
                            "Due to unknown signaling message \"type\": " + type +
                            ", target\": " + target + ".";
                    logE(TAG, error);
                    logD(TAG, debug);
                }
            } catch (JSONException e) {
                String error = "[ERROR:" + Errors.SIG_MSG_UNABLE_TO_READ_JSON +
                        "] Unable to read some message from the Server!";
                String debug = error + "\nDetails: SIG_MSG_UNABLE_TO_READ_JSON.\n" +
                        "Message: " + data + "\nException: " + e.getMessage();
                logE(TAG, error);
                logD(TAG, debug);
            }
        }
    }

    @Override
    public void onDisconnect() {
        // Remove all existing PeerConnections and related info.
        // If socket reconnects,
        // Will join room again like first time.
        // New PeerId will be issued.
        skylinkConnection.getSkylinkPeerService().
                removeAllPeers(ProtocolHelper.CONNECTION_LOST, true);
    }

    @Override
    public void onClose() {
        skylinkConnection.runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (skylinkConnection.getLockDisconnect()) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (skylinkConnection.getSkylinkConnectionService().getConnectionState() ==
                            SkylinkConnectionService.ConnectionState.DISCONNECTING) {
                        return;
                    }

                    String error = "[ERROR:" + Errors.DISCONNECT_UNEXPECTED_ERROR +
                            "] Connection with the skylink server is closed";
                    String debug = error + "\nDetails: Socket.io connection time-out.";
                    logE(TAG, error);
                    logD(TAG, debug);

                    skylinkConnection.getLifeCycleListener()
                            .onDisconnect(Errors.DISCONNECT_UNEXPECTED_ERROR,
                                    error);
                }
                // Disconnect from room
                skylinkConnection.disconnectFromRoom();
            }
        });
    }

    @Override
    public void onError(final int code, final String description) {
        skylinkConnection.runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (skylinkConnection.getLockDisconnect()) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (skylinkConnection.getSkylinkConnectionService().getConnectionState() ==
                            SkylinkConnectionService.ConnectionState.DISCONNECTING) {
                        return;
                    }

                    final String error;
                    String debug;
                    switch (code) {
                        case SignalingServerClientListener.ERROR_SIG_SERVER_SOCKET:
                            error = "[ERROR:" + Errors.CONNECT_SOCKET_ERROR_SIG_SERVER + "]";
                            break;
                        case SignalingServerClientListener.ERROR_SIG_SERVER_URI_SYNTAX:
                            error = "[ERROR:" + Errors.CONNECT_UNABLE_SIG_SERVER + "]" +
                                    " Unable to connect to the Server! Please check your URL syntax.";
                            break;
                        default:
                            error = "[ERROR] Unrecognised error from the Server!";
                            break;
                    }
                    debug = error + "\n" + description;
                    skylinkConnection.getLifeCycleListener()
                            .onWarning(Errors.SIGNALING_CONNECTION_ERROR, error);
                    logE(TAG, error);
                    logD(TAG, debug);
                }
            }
        });
    }
}

interface SignalingMessageListener {
    public void onConnectedToRoom();
}
