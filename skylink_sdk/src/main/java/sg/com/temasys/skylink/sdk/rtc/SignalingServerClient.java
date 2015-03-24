package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

class SignalingServerClient {

    private final static String TAG = "SocketTesterClient";

    private final int RETRY_MAX = 3;
    private final int SIG_PORT_DEFAULT = 443;
    private final int SIG_PORT_FAILOVER = 3443;
    // private final int SIG_PORT_DEFAULT = 9000;
    // private final int SIG_PORT_FAILOVER = 9000;
    // private final int SIG_PORT_DEFAULT = 8018;
    // private final int SIG_PORT_FAILOVER = 8018;

    private boolean isConnected;
    private int retry = 0;
    private int sigPort;
    private String sigIP;

    private Socket socketIO = null;
    private MessageHandler delegate;

    public SignalingServerClient(MessageHandler delegate,
                                 String signalingIp, int signalingPort) {
        this.delegate = delegate;
        sigIP = signalingIp;
        // sigIP = "http://192.168.1.125";
        // sigIP = "http://sgbeta.signaling.temasys.com.sg";
        sigPort = SIG_PORT_DEFAULT;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            IO.setDefaultSSLContext(sslContext);
            connectSigServer();
        } catch (URISyntaxException e) {
            delegate.onError(0, e.getLocalizedMessage());
        } catch (NoSuchAlgorithmException e) {
            delegate.onError(0, e.getLocalizedMessage());
        } catch (KeyManagementException e) {
            delegate.onError(0, e.getLocalizedMessage());
        }
    }

    public Socket getSocketIO() {
        return socketIO;
    }

    public void setDelegate(MessageHandler delegate) {
        this.delegate = delegate;
    }


    private void connectSigServer() throws URISyntaxException {

        socketIO = IO.socket(sigIP + ":" + sigPort);

        socketIO.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onConnect();
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

        }).on(Socket.EVENT_ERROR, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                onError(args);
            }
        });

        socketIO.connect();
    }

    /*
    @Override
    public void on(String event, IOAcknowledge ack, Object... args) {
        Log.d(TAG, "Server triggered event '" + event + "'");
        Log.d(TAG, "Server Event: " + event + "\n");
        if (event.equals("message")) {
            try {
                String jsonStr = (String) args[0];
                delegate.onMessage(jsonStr);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }*/

    void onConnect() {
        Log.d(TAG, "Connection established");
        Log.d(TAG, "Connected to Signaling server.");
        isConnected = true;
        delegate.onOpen();
    }

    public void onMessage(JSONObject json) {
        String jsonStr = json.toString();
        Log.d(TAG, "Server said:" + jsonStr);
        Log.d(TAG, "Server message Json: " + jsonStr + "\n");
        delegate.onMessage(jsonStr);
    }

    public void onMessage(String data) {
        Log.d(TAG, "Server said: " + data);
        Log.d(TAG, "Server message String: " + data + "\n");
        delegate.onMessage(data);
    }

    void onDisconnect() {
        Log.d(TAG, "Connection terminated.");
        Log.d(TAG, "Disconnected from Signaling server.");
        if (delegate != null) delegate.onClose();
        //delegate = null;
    }

    void onError(Object... args) {
        Log.d(TAG, "an Error occured");
        // If it was handshake error, switch to fail over port, and connect
        // again.
        if (!isConnected && retry++ < RETRY_MAX) {
            sigPort = SIG_PORT_FAILOVER;
            try {
                connectSigServer();
            } catch (URISyntaxException e) {
                delegate.onError(0, e.getLocalizedMessage());
            }
            return;
        }
        // Delegate will log message and result in UI disconnect.
        delegate.onError(0, args[0].toString());
    }
}
