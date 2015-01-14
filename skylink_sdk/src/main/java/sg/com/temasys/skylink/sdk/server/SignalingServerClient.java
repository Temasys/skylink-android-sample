package sg.com.temasys.skylink.sdk.server;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.json.JSONObject;

import android.util.Log;

public class SignalingServerClient implements IOCallback {

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
	
	private SocketIO socketIO = null;
	private WebServerClient.MessageHandler delegate;

	public SignalingServerClient(WebServerClient.MessageHandler delegate,
			String signalingIp, int signalingPort) {
		this.delegate = delegate;
		sigIP = signalingIp;
		// sigIP = "http://192.168.1.125";
		// sigIP = "http://sgbeta.signaling.temasys.com.sg";
		sigPort = SIG_PORT_DEFAULT;
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, null, null);
			SocketIO.setDefaultSSLSocketFactory(sslContext);
			connectSigServer();
		} catch (MalformedURLException e) {
			delegate.onError(0, e.getLocalizedMessage());
		} catch (NoSuchAlgorithmException e) {
			delegate.onError(0, e.getLocalizedMessage());
		} catch (KeyManagementException e) {
			delegate.onError(0, e.getLocalizedMessage());
		}
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
		Log.d(TAG, "an Error occured", socketIOException);
		// If it was handshake error, switch to fail over port, and connect
		// again.
		if (!isConnected && retry++ < RETRY_MAX) {
			sigPort = SIG_PORT_FAILOVER;
			try {
				connectSigServer();
			} catch (MalformedURLException e) {
				delegate.onError(0, e.getLocalizedMessage());
			}
			return;
		}
		// Delegate will log message and result in UI disconnect.
		delegate.onError(0, socketIOException.getLocalizedMessage());
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

	private void connectSigServer() throws MalformedURLException {
		socketIO = new SocketIO(sigIP + ":" + sigPort);
		socketIO.connect(this);
	}

}
