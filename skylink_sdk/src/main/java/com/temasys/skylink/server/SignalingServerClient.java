package com.temasys.skylink.server;

import android.util.Log;

import org.json.JSONObject;

import java.net.MalformedURLException;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

public class SignalingServerClient implements IOCallback {

    private final static String TAG = "SocketTesterClient";

    private final int SIG_PORT_DEFAULT = 80;
    private final int SIG_PORT_FAILOVER = 3000;
    private final int RETRY_MAX = 3;

    private String sigIP;
    private int sigPort;
    private boolean isConnected;
    private int retry = 0;

    private SocketIO socketIO = null;
    private WebServerClient.MessageHandler delegate;

    public SignalingServerClient(WebServerClient.MessageHandler delegate,
                                 String signalingIp, int signalingPort) {
        this.delegate = delegate;
        sigPort = signalingPort;
        // sigPort = 81;
        // sigPort = SIG_PORT_DEFAULT;
        sigIP = signalingIp;
        connectSigServer();
    }

    public SocketIO getSocketIO() {
        return socketIO;
    }

    public void setDelegate(WebServerClient.MessageHandler delegate) {
        this.delegate = delegate;
    }

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
    }

    @Override
    public void onConnect() {
        Log.d(TAG, "Connection established");
        Log.d(TAG, "Connected to Signaling server.");
        isConnected = true;
        delegate.onOpen();
    }

    @Override
    public void onDisconnect() {
        Log.d(TAG, "Connection terminated.");
        Log.d(TAG, "Disconnected from Signaling server.");
        if (delegate != null) delegate.onClose();
        delegate = null;
    }

    @Override
    public void onError(SocketIOException socketIOException) {
        Log.d(TAG, "an Error occured");
        socketIOException.printStackTrace();
        String str = socketIOException.toString();
        Log.d(TAG, "Server error: " + str + "\n");
        String errStr = socketIOException.getMessage();
        // If it was handshake error, switch to fail over port, and connect again.
        if (errStr.equals("Error while handshaking")) {
            if (!isConnected && retry < RETRY_MAX) {
                sigPort = SIG_PORT_FAILOVER;
                retry += 1;
                connectSigServer();
                return;
            }
        }
        // Delegate will log message and result in UI disconnect.
        delegate.onError(0, socketIOException.getMessage());
    }

    @Override
    public void onMessage(String data, IOAcknowledge ack) {
        Log.d(TAG, "Server said: " + data);
        Log.d(TAG, "Server message String: " + data + "\n");
        delegate.onMessage(data);
    }

    @Override
    public void onMessage(JSONObject json, IOAcknowledge ack) {
        String jsonStr = json.toString();
        Log.d(TAG, "Server said:" + jsonStr);
        Log.d(TAG, "Server message Json: " + jsonStr + "\n");
        delegate.onMessage(jsonStr);
    }

    private void connectSigServer() {
        try {
            socketIO = new SocketIO(sigIP + ":" + sigPort);
            socketIO.connect(this);
        } catch (MalformedURLException e) {
            Log.d(TAG, e.getLocalizedMessage(), e);
        }
    }

}
