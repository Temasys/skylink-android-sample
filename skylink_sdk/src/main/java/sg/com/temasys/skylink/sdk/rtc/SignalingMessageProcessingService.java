package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose of this class to handle signaling messages and create message processors to handle
 * different signaling messages
 * <p/>
 * Created by janidu on 27/4/15.
 */
class SignalingMessageProcessingService implements SignalingServerClientListener {

    private static final String TAG = SignalingMessageProcessingService.class.getSimpleName();

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

        Log.d(TAG, "Connecting to the signaling server");

        socketIOClient = new SignalingServerClient(this,
                "https://" + signalingIp, signalingPort);

        // Create SignalingServerMessageSender if is not yet created.
        if (sigMsgSender == null) {
            sigMsgSender = new SignalingServerMessageSender(socketId, roomId);
        }
    }

    public void sendMessage(JSONObject dictMessage) {
        Log.d(TAG, "[sendMessage] " + dictMessage);
        sigMsgSender.sendMessage(socketIOClient, dictMessage);
    }

    // Disconnect from the Signaling Channel.
    public void disconnect() {
        Log.d(TAG, "Disconnecting from the signaling server");
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
            Log.d(TAG, "[onMessage] " + data);
            try {
                // Instantiate the relevant message processor
                JSONObject object = new JSONObject(data);

                String type = object.getString("type");
                String target = object.has("target") ? object.getString("target") : "";

                // If the target exist it should be the same for this client
                if (object.has("target") && !target.equals(skylinkConnectionService.getSid())) {
                    Log.e(TAG, "Ignoring the message" +
                            " due target mismatch , target :" + target + " type: " + type);
                    return;
                }

                MessageProcessor messageProcessor = messageProcessorFactory.
                        getMessageProcessor(type);

                if (messageProcessor != null) {

                    messageProcessor.setSkylinkConnection(skylinkConnection);
                    Log.d(TAG, "Processing message type " + type + " with "
                            + messageProcessor.getClass().getSimpleName());

                    messageProcessor.process(object);

                    Log.d(TAG, "Processed message type " + type + " with "
                            + messageProcessor.getClass().getSimpleName());
                } else {
                    Log.e(TAG, "Invalid signaling message type");
                }
            } catch (JSONException e) {
                onSignalingMessageException(e);
            }
        }
    }


    // A place to collect all Signaling message exceptions.
    // As some objects, like runnables, have to catch exceptions.
    void onSignalingMessageException(JSONException e) {
        String strErr = "[onMessage] error: " + e.getMessage();
        Log.e(TAG, strErr, e);
    }

    @Override
    public void onDisconnect() {
        // Remove all existing PeerConnections and related info.
        // If socket reconnects,
        // Will join room again like first time.
        // New PeerId will be issued.
        ProtocolHelper.removeAllPeers(skylinkConnection, ProtocolHelper.CONNECTION_LOST);
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

                    Log.d(TAG, "[SDK] onClose.");

                    skylinkConnection.getLifeCycleListener()
                            .onDisconnect(ErrorCodes.DISCONNECT_UNEXPECTED_ERROR,
                                    "Connection with the skylink server is closed");
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

                    final String message = "[SDK] onErrorAppServer: " + code + ", " + description;
                    Log.d(TAG, message);
                    skylinkConnection.getLifeCycleListener()
                            .onWarning(ErrorCodes.SIGNALING_CONNECTION_ERROR, message);
                }
            }
        });
    }
}

interface SignalingMessageListener {
    public void onConnectedToRoom();
}
