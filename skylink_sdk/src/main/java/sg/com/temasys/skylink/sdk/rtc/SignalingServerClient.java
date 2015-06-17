package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONObject;

import java.net.URISyntaxException;

class SignalingServerClient {

    private final static String TAG = SignalingServerClient.class.getName();

    private final static int RETRY_MAX = 3;
    private final static int SIG_PORT_DEFAULT = 443;
    private final static int SIG_PORT_FAILOVER = 3443;
    private final static int SOCKET_IO_TIME_OUT_MILLISECONDS = 60000;
    // private final int SIG_PORT_DEFAULT = 9000;
    // private final int SIG_PORT_FAILOVER = 9000;
    // private final int SIG_PORT_DEFAULT = 8018;
    // private final int SIG_PORT_FAILOVER = 8018;

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
        // sigIP = "http://192.168.1.125";
        // sigIP = "http://sgbeta.signaling.temasys.com.sg";
        sigPort = SIG_PORT_DEFAULT;
        try {
            connectSigServer();
        } catch (URISyntaxException e) {
            Log.e(TAG, e.getMessage(), e);
            delegate.onError(0, e.getLocalizedMessage());
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
        socketIO = IO.socket(sigIP + ":" + sigPort, opts);
        // socketIO = IO.socket("http://ec2-52-8-2-158.us-west-1.compute.amazonaws.com:6001", opts);

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
        Log.d(TAG, "Connected to Signaling server.");
        // Reconnection will follow same logic as first connect.
        // Just log reconnection for awareness.
        if (isConnected) {
            Log.d(TAG, "onConnect() called after reconnecting to Signaling server.");
        } else {
            isConnected = true;
        }
        delegate.onOpen();
    }

    void onReconnect() {
        Log.d(TAG, "Reconnected to Signaling server.");
    }

    public void onMessage(JSONObject json) {
        String jsonStr = json.toString();
        Log.d(TAG, "[onMessageJson] Server sent JSON message.");
        delegate.onMessage(jsonStr);
    }

    public void onMessage(String data) {
        Log.d(TAG, "[onMessageString] Server sent String message");
        delegate.onMessage(data);
    }

    void onDisconnect() {
        Log.d(TAG, "[onDisconnect] Disconnected from Signaling server.");
        delegate.onDisconnect();
    }

    void onTimeOut() {
        Log.d(TAG, "[onTimeOut] Connection with Signaling server time out.");
        if (delegate != null) {
            delegate.onClose();
        }
    }

    void onError(Object... args) {
        String strErr = args[0].toString();
        Log.d(TAG, "An Error occured: " + strErr);
        // If it was handshake error, switch to fail over port, and connect
        // again.
        if (!isConnected && retry++ < RETRY_MAX) {
            sigPort = SIG_PORT_FAILOVER;
            try {
                connectSigServer();
            } catch (URISyntaxException e) {
                Log.e(TAG, e.getMessage(), e);
                delegate.onError(0, e.getLocalizedMessage());
            }
            return;
        }
        // Delegate will log message and result in UI disconnect.
        delegate.onError(0, strErr);
    }
}

/**
 * Allows another class to decide what to do on each of these events upon connecting to Signaling
 * server.
 */
interface SignalingServerClientListener {
    void onOpen();

    void onMessage(String data);

    void onDisconnect();

    void onClose();

    void onError(int code, String description);
}
