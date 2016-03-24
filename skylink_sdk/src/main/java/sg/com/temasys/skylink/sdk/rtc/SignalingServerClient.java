package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logI;


class SignalingServerClient {

    private final static String TAG = SignalingServerClient.class.getName();

    private final static int RETRY_MAX = 3;
    private final static int SIG_PORT_DEFAULT = 443;
    private final static int SIG_PORT_FAILOVER = 3443;
    private final static int SOCKET_IO_TIME_OUT_MILLISECONDS = 60000;
    // private final static int SIG_PORT_DEFAULT = 80;
    // private final int SIG_PORT_DEFAULT = 9000;
    // private final int SIG_PORT_FAILOVER = 9000;
    // private final int SIG_PORT_DEFAULT = 8018;
    // private final int SIG_PORT_FAILOVER = 8018;
    public static final String SIG_SERVER_STAGING = "http://staging-signaling.temasys.com.sg";

    private boolean isConnected;
    private int retry = 0;
    private int sigPort;
    private final String sigIP;

    private Socket socketIO;
    private SignalingServerClientListener delegate;

    public SignalingServerClient(SignalingServerClientListener delegate,
                                 String signalingIp, int signalingPort) {
        this.delegate = delegate;
        sigIP = signalingIp;
        sigPort = SIG_PORT_DEFAULT;
        try {
            connectSigServer();
        } catch (URISyntaxException e) {
            String debug = "Attempted to connect to Signalling Server but " +
                    "obtained URI Syntax error. Exception:\n" + e.getMessage();
            delegate.onError(SignalingServerClientListener.ERROR_SIG_SERVER_URI_SYNTAX, debug);
            delegate.onError(0, e.getMessage());
        }
    }

    public Socket getSocketIO() {
        return socketIO;
    }

    public void setDelegate(SignalingServerClientListener delegate) {
        this.delegate = delegate;
    }

    private void connectSigServer() throws URISyntaxException {

        IO.Options opts = new IO.Options();
        opts.secure = true;
        opts.forceNew = true;
        opts.reconnection = true;
        opts.timeout = SOCKET_IO_TIME_OUT_MILLISECONDS;

        // Initialize SocketIO
        String sigUrl = sigIP + ":" + sigPort;
        // sigUrl = SIG_SERVER_STAGING + ":" + sigPort;
        // sigUrl = "http://192.168.1.125:6001";
        // sigUrl = "http://sgbeta.signaling.temasys.com.sg:6001";
        // sigUrl = "http://ec2-52-8-93-170.us-west-1.compute.amazonaws.com:6001";
        // sigUrl = "http://192.168.1.54:6001";
        logD(TAG, "Connecting to the Signaling server at: " + sigUrl);
        socketIO = IO.socket(sigUrl, opts);

        socketIO.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onConnect();
            }
        }).on(Socket.EVENT_RECONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onReconnect();
            }
        }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                Object message = args[0];
                if (message instanceof String) {
                    onMessage((String) message);
                } else if (message instanceof JSONObject) {
                    onMessage((JSONObject) message);
                }
            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                onDisconnect();
            }

        }).on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                onTimeOut();
            }

        }).on(Socket.EVENT_ERROR, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                onError(args);
            }
        });

        socketIO.connect();
    }

    void onConnect() {
        String info = "Connected to the room!";
        String debug = info + " I.e. to Signaling server.";
        // Reconnection will follow same logic as first connect.
        // Just log reconnection for awareness.
        if (isConnected) {
            debug = "onConnect() called after reconnecting to Signaling server.";
        } else {
            isConnected = true;
            logI(TAG, info);
        }
        logD(TAG, debug);
        delegate.onOpen();
    }

    void onReconnect() {
        String info = "Reconnected to the room!";
        String debug = info + " I.e. to Signaling server.";
        logI(TAG, info);
        logD(TAG, debug);
    }

    public void onMessage(JSONObject json) {
        String jsonStr = json.toString();
        logD(TAG, "[onMessageJson] Server sent JSON message.");
        delegate.onMessage(jsonStr);
    }

    public void onMessage(String data) {
        logD(TAG, "[onMessageString] Server sent String message");
        delegate.onMessage(data);
    }

    void onDisconnect() {
        String info = "We have been just disconnected from the room.";
        String debug = "[onDisconnect] " + info + " I.e. From the Signaling server.";
        logI(TAG, info);
        logD(TAG, debug);
        delegate.onDisconnect();
    }

    void onTimeOut() {
        logD(TAG, "[onTimeOut] Connection with Signaling server time out.");
        if (delegate != null) {
            delegate.onClose();
        }
    }

    void onError(Object... args) {
        // Logging of errors will be done at delegate's onError, so no need to do it here.
        String strErr = "Received error (" + args[0].toString() + ").\n";
        // If it was handshake error, switch to fail over port, and connect
        // again.
        if (!isConnected && retry++ < RETRY_MAX) {
            sigPort = SIG_PORT_FAILOVER;
            try {
                connectSigServer();
            } catch (URISyntaxException e) {
                String debug = strErr +
                        "Attempted to reconnect to Signalling Server but " +
                        "obtained URI Syntax error. Exception:\n" +
                        e.getMessage();
                delegate.onError(SignalingServerClientListener.ERROR_SIG_SERVER_URI_SYNTAX, debug);
            }
            return;
        }
        // Delegate will log message and result in UI disconnect.
        String debug = strErr;
        if (isConnected) {
            debug += "Still connected to Signaling Server, so not attempting to reconnect.";
        } else {
            debug += "Exceeded Max reconnect attempt(s) (" + RETRY_MAX +
                    "), so not attempting to reconnect.";
        }
        ;
        delegate.onError(SignalingServerClientListener.ERROR_SIG_SERVER_SOCKET, debug);
    }
}

/**
 * Allows another class to decide what to do on each of these events upon connecting to Signaling
 * server.
 */
interface SignalingServerClientListener {
    static final int ERROR_SIG_SERVER_SOCKET = 0;
    static final int ERROR_SIG_SERVER_URI_SYNTAX = 1;

    void onOpen();

    void onMessage(String data);

    void onDisconnect();

    void onClose();

    void onError(int code, String description);
}
