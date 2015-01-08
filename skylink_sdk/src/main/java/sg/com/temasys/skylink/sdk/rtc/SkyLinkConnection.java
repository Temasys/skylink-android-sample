package sg.com.temasys.skylink.sdk.rtc;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import sg.com.temasys.skylink.sdk.data.DataChannelManager;
import sg.com.temasys.skylink.sdk.rendering.VideoRendererGui;
import sg.com.temasys.skylink.sdk.server.WebServerClient;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

/**
 * Main class to connect to the skyway infrastructure.
 * 
 * @author temasys
 * 
 */
public class SkyLinkConnection {

	/**
	 * Configuration class used to configure the parameters of conversation.
	 * 
	 * @author temasys
	 * 
	 */
	public static class SkyLinkConfig implements Serializable {

		private static final long serialVersionUID = 1L;

		private boolean video;
		private boolean audio;
		private boolean peerMessaging;
		private boolean fileTransfer;
		private int timeout = 60;
		private Map<String, Object> advancedOptions;

		/**
		 * Creates a new SkyLinkConfig object.
		 */
		public SkyLinkConfig() {
			super();
		}

		/**
		 * Creates a new SkyLinkConfig (Copy constructor).
		 * 
		 * @param config
		 *            Configuration object to copy from
		 */
		public SkyLinkConfig(SkyLinkConfig config) {
			super();
			this.video = config.video;
			this.audio = config.audio;
			this.peerMessaging = config.peerMessaging;
			this.fileTransfer = config.fileTransfer;
			this.timeout = config.timeout;
		}

		/**
		 * 
		 * @return Audio config value.
		 */
		public boolean hasAudio() {
			return audio;
		}

		/**
		 * Sets the audio flag for this object to the indicated boolean value.
		 * 
		 * @param audio
		 *            Audio config value
		 */
		public void setHasAudio(boolean audio) {
			this.audio = audio;
			this.video = this.video && this.audio;
		}

		/**
		 * 
		 * @return Video config value.
		 */
		public boolean hasVideo() {
			return video;
		}

		/**
		 * Sets the video flag for this object to the indicated boolean value.
		 * 
		 * @param video
		 *            Video config value
		 */
		public void setHasVideo(boolean video) {
			this.video = video && this.audio;
		}

		/**
		 * 
		 * @return PeerMessaging config value.
		 */
		public boolean hasPeerMessaging() {
			return peerMessaging;
		}

		/**
		 * Sets the peerMessaging flag for this object to the indicated boolean
		 * value.
		 * 
		 * @param peerMessaging
		 *            PeerMessaging config value
		 */
		public void setHasPeerMessaging(boolean peerMessaging) {
			this.peerMessaging = peerMessaging;
		}

		/**
		 * 
		 * @return FileTransfer config value.
		 */
		public boolean hasFileTransfer() {
			return fileTransfer;
		}

		/**
		 * Sets the fileTransfer flag for this object to the indicated boolean
		 * value.
		 * 
		 * @param fileTransfer
		 *            FileTransfer config value
		 */
		public void setHasFileTransfer(boolean fileTransfer) {
			this.fileTransfer = fileTransfer;
		}

		/**
		 * 
		 * @return Timeout config value.
		 */
		public int getTimeout() {
			return timeout;
		}

		/**
		 * Sets the timeout value of this object.
		 * 
		 * @param timeout
		 *            Timeout config value
		 */
		public void setTimeout(int timeout) {
			this.timeout = timeout;
		}

		/**
		 * 
		 * @return Map of Advanced Options.
		 */
		public Map<String, Object> getAdvancedOptions() {
			return advancedOptions;
		}

		/**
		 * Sets advanced options. (For advanced users only).
		 * 
		 * @param advancedOptions
		 *            A map containing optional entries as follows:
		 *            "STUN":"boolean", "TURN":"boolean", "transport":"TCP/UDP"
		 */
		public void setAdvancedOptions(Map<String, Object> advancedOptions) {
			this.advancedOptions = advancedOptions;
		}

		@Override
		public String toString() {
			return "TEMAConnectionConfig [video=" + video + ", audio=" + audio
					+ ", p2PMessage=" + peerMessaging + ", fileTransfer="
					+ fileTransfer + ", timeout=" + timeout + "]";
		}
		
		private boolean isStunDisabled() {
			boolean result = false;
			if (advancedOptions != null) {
				Object object = advancedOptions.get("STUN");
				if (object != null)
					result = ((Boolean) object).booleanValue();
			}
			return result;
		}

		private boolean isTurnDisabled() {
			boolean result = false;
			if (advancedOptions != null) {
				Object object = advancedOptions.get("TURN");
				if (object != null)
					result = ((Boolean) object).booleanValue();
			}
			return result;
		}

		private String getTransport() {
			String result = null;
			if (advancedOptions != null) {
				Object object = advancedOptions.get("transport");
				if (object != null)
					result = (String) object;
			}
			return result;
		}

	}

	/**
	 * Delegate comprises of callbacks related to the life cycle of the
	 * connection.
	 * 
	 * @author temasys
	 * 
	 */
	public interface LifeCycleDelegate {

		/**
		 * This is the first callback to specify whether the connection was
		 * successful.
		 * 
		 * @param isSuccess
		 *            Specify success or failure
		 * @param message
		 *            A message in case of isSuccess is 'false' describing the
		 *            reason of failure
		 */
		public void onConnect(boolean isSuccess, String message);

		/**
		 * This is triggered when the framework successfully captures the camera
		 * input from one's phone if the connection is configured to have a
		 * video call.
		 * 
		 * @param videoView
		 *            Video of oneself
		 * @param size
		 *            Size of the video frame
		 */
		public void onGetUserMedia(GLSurfaceView videoView, Point size);

		/**
		 * This is triggered when the framework issues a warning to the client.
		 * 
		 * @param message
		 *            Warning message
		 */
		public void onWarning(String message);

		/**
		 * This is triggered whenever the connection between the client and the
		 * infrastructure drops.
		 * 
		 * @param message
		 *            Message specifying the reason for disconnection
		 */
		public void onDisconnect(String message);

		/**
		 * Occasionally the framework sends some messages for the client to
		 * intimate about certain happenings.
		 * 
		 * @param message
		 *            Happening message
		 */
		public void onReceiveLog(String message);

	}

	/**
	 * Delegate comprises of callbacks related to the remote peers' activities.
	 * 
	 * @author temasys
	 * 
	 */
	public interface RemotePeerDelegate {

		/**
		 * This is triggered when a new peer joins the room.
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param userData
		 *            User defined data relating to the peer. May be a
		 *            'java.lang.String', 'org.json.JSONObject' or
		 *            'org.json.JSONArray'.
		 */
		public void onPeerJoin(String peerId, Object userData);

		/**
		 * The is triggered upon receiving the video stream of the peer if the
		 * connection is configured to have a video call.
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param videoView
		 *            Video of the peer
		 * @param size
		 *            Size of the peer video frame
		 */
		public void onGetPeerMedia(String peerId, GLSurfaceView videoView,
				Point size);

		/**
		 * This is triggered when an update is received in the user defined data
		 * of a peer.
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param userData
		 *            User defined data relating to the peer. May be a
		 *            'java.lang.String', 'org.json.JSONObject' or
		 *            'org.json.JSONArray'.
		 */
		public void onUserData(String peerId, Object userData);

		/**
		 * This is triggered when the underlying data connection is established
		 * between two peers and is ready to send and receive peer messages and
		 * files between them.
		 * 
		 * @param peerId
		 *            The id of the peer
		 */
		public void onOpenDataConnection(String peerId);

		/**
		 * This is triggered when a peer leaves the room.
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param message
		 *            Message specifying the possible reason for leaving the
		 *            room
		 */
		public void onPeerLeave(String peerId, String message);

	}

	/**
	 * Delegate comprises of callbacks related to audio / video manipulation
	 * during the call.
	 * 
	 * @author temasys
	 * 
	 */
	public interface MediaDelegate {

		/**
		 * This is triggered when any of the given video streams' frame size
		 * changes. It includes the self stream also.
		 * 
		 * @param videoView
		 *            The video view for which the frame size is changed
		 * @param size
		 *            Size of the video frame
		 */
		public void onVideoSize(GLSurfaceView videoView, Point size);

		/**
		 * This is triggered when a peer enable / disable its audio.
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param isMuted
		 *            Flag specifying whether the audio is muted or not
		 */
		public void onToggleAudio(String peerId, boolean isMuted);

		/**
		 * This is triggered when a peer enable / disable its video.
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param isMuted
		 *            Flag specifying whether the video is muted or not
		 */
		public void onToggleVideo(String peerId, boolean isMuted);

	}

	/**
	 * Delegate comprises of callbacks upon receiving various messages from
	 * peers.
	 * 
	 * @author temasys
	 * 
	 */
	public interface MessagesDelegate {

		/**
		 * This is triggered when a text message is received from a peer. This
		 * functionality is deprecated and will be removed eventually. One may
		 * continue to use 'onCustomMessage'.
		 * 
		 * @param peerId
		 *            The id of the peer.
		 * @param nick
		 *            The nick of the peer
		 * @param message
		 *            The message itself
		 * @param isPrivate
		 *            Flag to specify whether the message was broadcast to all
		 *            the peers
		 */
		@Deprecated
		public void onChatMessage(String peerId, String nick, String message,
				boolean isPrivate);

		/**
		 * This is triggered when a custom broadcast or private message is
		 * received from a peer via signaling channel.
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param message
		 *            User defined message. May be a 'java.lang.String',
		 *            'org.json.JSONObject' or 'org.json.JSONArray'.
		 * @param isPrivate
		 *            Flag to specify whether the message was broadcast to all
		 *            the peers
		 */
		public void onCustomMessage(String peerId, Object message,
				boolean isPrivate);

		/**
		 * This is triggered when a broadcast or private peer message is
		 * received via data channel.
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param message
		 *            User defined message. May be a 'java.lang.String',
		 *            'org.json.JSONObject' or 'org.json.JSONArray'.
		 * @param isPrivate
		 *            Flag to specify whether the message was broadcast to all
		 *            the peers
		 */
		public void onPeerMessage(String peerId, Object message,
				boolean isPrivate);

	}

	/**
	 * Delegate comprises of callbacks related to file transfer opertion.
	 * 
	 * @author macbookpro
	 * 
	 */
	public interface FileTransferDelegate {

		/**
		 * This is triggered upon receiving a file transfer request from a peer.
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param fileName
		 *            The name of the file
		 */
		public void onRequest(String peerId, String fileName, boolean isPrivate);

		/**
		 * This is triggered upon receiving the response of a file transfer
		 * request from a peer.
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param fileName
		 *            The name of the file
		 * @param isPermitted
		 *            Flag to specify whether the peer has accepted the request
		 */
		public void onPermission(String peerId, String fileName,
				boolean isPermitted);

		/**
		 * This is triggered when an ongoing file transfer drops due to some
		 * reason
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param fileName
		 *            The name of the file
		 * @param message
		 *            Message that possibly tells the reason for dropping
		 */
		public void onDrop(String peerId, String fileName, String message,
				boolean isExplicit);

		/**
		 * This is triggered when a file transfer is completed successfully.
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param fileName
		 *            The name of the file
		 * @param isSending
		 *            Flag to specify whether the completed transfer is for
		 *            sending or receiving a file
		 */
		public void onComplete(String peerId, String fileName, boolean isSending);

		/**
		 * This is triggered timely to report the on going file transfer
		 * progress.
		 * 
		 * @param peerId
		 *            The id of the peer
		 * @param fileName
		 *            The name of the file
		 * @param percentage
		 *            The percentage completed
		 * @param isSending
		 *            Flag to specify whether the completed transfer is for
		 *            sending or receiving a file
		 */
		public void onProgress(String peerId, String fileName,
				double percentage, boolean isSending);

	}

  /**
   * 
   * @return The file transfer delegate object.
   */
  public FileTransferDelegate getFileTransferDelegate() {
    return fileTransferDelegate;
  }

  /**
   * Sets the specified file transfer delegate object.
   * 
   * @param fileTransferDelegate
   *            The file transfer delegate object
   */
  public void setFileTransferDelegate(
      FileTransferDelegate fileTransferDelegate) {
    if (fileTransferDelegate == null)
      this.fileTransferDelegate = new FileTransferAdapter();
    else
      this.fileTransferDelegate = fileTransferDelegate;
  }

  /**
   * 
   * @return The life cycle delegate object.
   */
  public LifeCycleDelegate getLifeCycleDelegate() {
    return lifeCycleDelegate;
  }

  /**
   * Sets the specified life cycle delegate object.
   * 
   * @param lifeCycleDelegate
   *            The life cycle delegate object
   */
  public void setLifeCycleDelegate(LifeCycleDelegate lifeCycleDelegate) {
    if (lifeCycleDelegate == null)
      this.lifeCycleDelegate = new LifeCycleAdapter();
    else
      this.lifeCycleDelegate = lifeCycleDelegate;
  }

  /**
   * 
   * @return The media delegate object.
   */
  public MediaDelegate getMediaDelegate() {
    return mediaDelegate;
  }

  /**
   * Sets the specified media delegate object.
   * 
   * @param mediaDelegate
   *            The media delegate object
   */
  public void setMediaDelegate(MediaDelegate mediaDelegate) {
    if (mediaDelegate == null)
      this.mediaDelegate = new MediaAdapter();
    else
      this.mediaDelegate = mediaDelegate;
  }

  /**
   * 
   * @return The messages delegate object.
   */
  public MessagesDelegate getMessagesDelegate() {
    return messagesDelegate;
  }

  /**
   * Sets the specified messages delegate object.
   * 
   * @param messagesDelegate
   *            The messages delegate object
   */
  public void setMessagesDelegate(MessagesDelegate messagesDelegate) {
    if (messagesDelegate == null)
      this.messagesDelegate = new MessagesAdapter();
    else
      this.messagesDelegate = messagesDelegate;
  }

  /**
   * 
   * @return The remote peer delegate object.
   */
  public RemotePeerDelegate getRemotePeerDelegate() {
    return remotePeerDelegate;
  }

  /**
   * Sets the specified remote peer delegate object.
   * 
   * @param remotePeerDelegate
   *            The remote peer delegate object
   */
  public void setRemotePeerDelegate(RemotePeerDelegate remotePeerDelegate) {
    if (remotePeerDelegate == null)
      this.remotePeerDelegate = new RemotePeerAdapter();
    else
      this.remotePeerDelegate = remotePeerDelegate;
  }

  /**
   * 
   * @return The associated activity.
   */
  public Context getContext() {
    return myActivity;
  }

	private static final String TAG = "TEMAConnectionManager";
	private static final int MAX_PEER_CONNECTIONS = 4;
	private static final String MY_SELF = "me";

	private static boolean factoryStaticInitialized;

	private Activity myActivity;
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
	private Map<String, PCObserver> pcObserverPool;
	private Map<String, PeerConnection> peerConnectionPool;
	private Map<String, SDPObserver> sdpObserverPool;
	private MediaConstraints pcConstraints;
	private MediaConstraints sdpMediaConstraints;
	private MediaStream localMediaStream;
	private Object myUserData;
	private PeerConnectionFactory peerConnectionFactory;
	private String apiKey;
	private String apiSecret;
	private SkyLinkConfig myConfig;
	private VideoCapturer localVideoCapturer;
	private VideoSource localVideoSource;
	private VideoTrack localVideoTrack;
	private WebServerClient webServerClient;

	private WebServerClient.IceServersObserver iceServersObserver = new MyIceServersObserver();
	private WebServerClient.MessageHandler messageHandler = new MyMessageHandler();
	private VideoRendererGui.VideoRendererGuiDelegate videoRendererGuiDelegate = new MyVideoRendererGuiDelegate();

	private FileTransferDelegate fileTransferDelegate;
	private LifeCycleDelegate lifeCycleDelegate;
	private MediaDelegate mediaDelegate;
	private MessagesDelegate messagesDelegate;
	private RemotePeerDelegate remotePeerDelegate;

  // List of Connection state types
  public enum ConnectionState {
    CONNECT, DISCONNECT
  }
  private ConnectionState connectionState;

  // Lock objects to prevent threads from executing the following methods concurrently:
    // WebServerClient.MessageHandler.onMessage
    // SkyLinkConnection.disconnect
    // WebServerClient.IceServersObserver.onIceServers
    // WebServerClient.IceServersObserver.onError
  private Object lockDisconnect = new Object();
  private Object lockDisconnectMsg = new Object();
  private Object lockDisconnectMedia = new Object();
  private Object lockDisconnectSdp = new Object();

	/**
	 * Creates a new SkyLinkConnection object with the specified parameters.
	 * 
	 * @param apiKey
	 *            The api key
	 * @param secret
	 *            The secret associated with the key
	 * @param config
	 *            The config object to configure the call itself
	 * @param parentActivity
	 *            The activity to which this connection object belongs
	 */
	public SkyLinkConnection( String apiKey, String secret,
			SkyLinkConfig config, Context context ) {
		logMessage("TEMAConnectionManager::config=>" + config);

		this.myConfig = new SkyLinkConfig(config);
		this.settingsObject = new ConstConnectionConfig();

		logMessage("TEMAConnectionManager::apiKey=>" + apiKey);
		this.apiKey = apiKey;
		logMessage("TEMAConnectionManager::secret=>" + secret);
		this.apiSecret = secret;

		if (!factoryStaticInitialized) {
			abortUnless(PeerConnectionFactory.initializeAndroidGlobals(context,
					true, true), "Failed to initializeAndroidGlobals");
			factoryStaticInitialized = true;
		}

		this.sdpMediaConstraints = new MediaConstraints();
		this.sdpMediaConstraints.mandatory
				.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio",
						String.valueOf(this.myConfig.hasAudio())));
		this.sdpMediaConstraints.mandatory
				.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo",
						String.valueOf(this.myConfig.hasVideo())));

		MediaConstraints constraints = new MediaConstraints();
		constraints.mandatory
				.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio",
						String.valueOf(this.myConfig.hasAudio())));
		constraints.mandatory
				.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo",
						String.valueOf(this.myConfig.hasVideo())));
		constraints.optional.add(new MediaConstraints.KeyValuePair(
				"internalSctpDataChannels", "true"));
		constraints.optional.add(new MediaConstraints.KeyValuePair(
				"DtlsSrtpKeyAgreement", "true"));
		constraints.optional.add(new MediaConstraints.KeyValuePair("googDscp",
				"true"));
		this.pcConstraints = constraints;

		this.myActivity = (Activity) context;

		// Instantiate DataChannelManager.
		if (this.myConfig.hasPeerMessaging() || this.myConfig.hasFileTransfer()) {
			this.dataChannelManager = new DataChannelManager(this,
					this.myConfig.getTimeout(), myConfig.hasPeerMessaging(),
					myConfig.hasFileTransfer());
			this.dataChannelManager.setConnectionManager(this);
		}
	}

	/**
	 * Connects to a room.
	 * 
	 * @param roomName
	 *            The name of the room
	 * @param userData
	 *            User defined data relating to oneself. May be a
	 *            'java.lang.String', 'org.json.JSONObject' or
	 *            'org.json.JSONArray'.
	 * @param startTime
	 *            The time to start a call
	 * @param duration
	 *            The expected duration of a call
	 * @return 'false' if the connection is already established
	 * @throws SignatureException
	 * @throws IOException
	 * @throws JSONException
	 */
	public boolean connectToRoom(String roomName, Object userData,
			Date startTime, float duration) throws SignatureException,
			IOException, JSONException {
		if (this.webServerClient != null)
			return false;
    // Record user intention for connection to room state
    connectionState = ConnectionState.CONNECT;

		if (this.fileTransferDelegate == null)
			this.fileTransferDelegate = new FileTransferAdapter();
		if (this.lifeCycleDelegate == null)
			this.lifeCycleDelegate = new LifeCycleAdapter();
		if (this.mediaDelegate == null)
			this.mediaDelegate = new MediaAdapter();
		if (this.messagesDelegate == null)
			this.messagesDelegate = new MessagesAdapter();
		if (this.remotePeerDelegate == null)
			this.remotePeerDelegate = new RemotePeerAdapter();

		this.webServerClient = new WebServerClient(myActivity, messageHandler,
				iceServersObserver);

		logMessage("TEMAConnectionManager::room name=>" + roomName);
		logMessage("TEMAConnectionManager::call duration=>" + duration);
		String dateString = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:00.0'Z'")
				.format(startTime);
		logMessage("TEMAConnectionManager::iso start time=>" + dateString);
		String cred = calculateRFC2104HMAC(roomName + "_" + duration + "_"
				+ dateString, this.apiSecret);
		cred = URLEncoder.encode(cred, "UTF-8");
		String url = "http://api.temasys.com.sg/api/" + this.apiKey + "/"
				+ roomName + "/" + dateString + "/" + duration + "?cred="
				+ cred;
		this.myUserData = userData;

		logMessage("Connecting to room ...");
		this.webServerClient.connectToRoom(url);
		return true;
	}

	/**
	 * Must be called to reset the context if the context is destroyed or
	 * updated.
	 * 
	 * @param context
	 *            The context
	 */
	public void resetContext(Context context) {
		this.myActivity = (Activity) context;
	}
	
	/**
	 * Call this method from within the onPause of the parent activity.
	 */
	public void onPause() {
		/*if (this.localVideoSource != null
				&& this.localVideoSource.state() != MediaSource.State.ENDED) {
			this.localVideoSource.stop();
			videoSourceStopped = true;
		}*/
	}

	/**
	 * Call this method from within the onResume of the parent activity.
	 */
	public void onResume() {
		if (this.localVideoSource != null && videoSourceStopped) {
			this.localVideoSource.restart();
		}
	}

	/**
	 * Disconnects from the room.
	 */
	public void disconnect() {
    // Prevent thread from executing with WebServerClient methods concurrently.
    synchronized( lockDisconnectMsg ) {
    synchronized( lockDisconnectMedia ) {
    synchronized( lockDisconnectSdp ) {
    synchronized( lockDisconnect ) {
      // Record user intention for connection to room state
      connectionState = ConnectionState.DISCONNECT;

  		/*if (this.webServerClient == null)
  			return;*/
      if (this.webServerClient != null) this.webServerClient.disconnect();

      logMessage("Inside TEMAConnectionManager.disconnect");

      // Dispose all DC.
      String allPeers = null;
      dataChannelManager.disposeDC( allPeers );

      if (this.peerConnectionPool != null) {
        for (PeerConnection peerConnection : this.peerConnectionPool
            .values())
          peerConnection.dispose();
        this.peerConnectionPool.clear();
      }

      this.peerConnectionPool = null;
      if (this.pcObserverPool != null)
        this.pcObserverPool.clear();
      this.pcObserverPool = null;
      if (this.sdpObserverPool != null)
        this.sdpObserverPool.clear();
      this.sdpObserverPool = null;
      if (this.displayNameMap != null)
        this.displayNameMap.clear();
      this.displayNameMap = null;

      if (this.localMediaStream != null)
        this.localMediaStream.dispose();
      this.localMediaStream = null;
      this.localAudioTrack = null;
      if (this.localAudioSource != null)
        this.localAudioSource.dispose();
      this.localAudioSource = null;

      this.localVideoTrack = null;
      if (this.localVideoSource != null)
        this.localVideoSource.dispose();
      this.localVideoSource = null;

      if (this.localVideoCapturer != null)
        this.localVideoCapturer.dispose();
      this.localVideoCapturer = null;

      if (this.peerConnectionFactory != null)
        this.peerConnectionFactory.dispose();
      this.peerConnectionFactory = null;

      this.webServerClient = null;
    }}}}
	}

	/**
	 * Sends a text message to a peer or to all peers. This functionality is
	 * deprecated and will be removed eventually. One is appreciated to use
	 * 'sendCustomMessage' instead.
	 * 
	 * @param peerId
	 *            The id of the peer. Send 'null' if the message is intended to
	 *            broadcast to all of the connected peers in the room.
	 * @param message
	 *            The message to be sent to the peer
	 */
	@Deprecated
	public void sendChatMessage(String peerId, String message) {
		if (this.webServerClient == null)
			return;
		Log.d(TAG,
				"TEMAConnectionManager.sendChatMessage::Sending chat::remotePeerId->"
						+ peerId + ", message->" + message);
		JSONObject dict = new JSONObject();
		try {
			dict.put("type", "chat");
			dict.put("data", message);
			if (peerId != null)
				dict.put("target", peerId);
			dict.put("nick", webServerClient.getDisplayName());
			dict.put("mid", webServerClient.getSid());
			dict.put("rid", webServerClient.getRoomId());
			dict.put("cid", webServerClient.getCid());
			webServerClient.sendMessage(dict);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	/**
	 * Sends a user defined message to a peer or to all peers via signalling
	 * channel.
	 * 
	 * @param peerId
	 *            The id of the peer. Send 'null' if the message is intended to
	 *            broadcast to all of the connected peers in the room.
	 * @param message
	 *            User defined data. May be a 'java.lang.String',
	 *            'org.json.JSONObject' or 'org.json.JSONArray'.
	 */
	public void sendCustomMessage(String peerId, Object message) {
		if (this.webServerClient == null)
			return;

		JSONObject dict = new JSONObject();
		try {
			dict.put("cid", webServerClient.getCid());
			dict.put("data", message);
			dict.put("mid", webServerClient.getSid());
			dict.put("nick", webServerClient.getDisplayName());
			dict.put("rid", webServerClient.getRoomId());
			if (peerId != null) {
				dict.put("type", "private");
				dict.put("target", peerId);
			} else {
				dict.put("type", "public");
			}
			webServerClient.sendMessage(dict);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}
	
	/**
	 * Sends a user defined message to a peer or to all peers via data channel.
	 * 
	 * @param peerId
	 *            The id of the peer. Send 'null' if the message is intended to
	 *            broadcast to all of the connected peers in the room.
	 * @param message
	 *            User defined data. May be a 'java.lang.String',
	 *            'org.json.JSONObject' or 'org.json.JSONArray'.
	 * @throws SkyLinkException
	 *             if the system was unable to send the message.
	 */
	public void sendPeerMessage(String peerId, Object message)
			throws SkyLinkException {
		if (this.webServerClient == null)
			return;

		if (myConfig.hasPeerMessaging()) {
			if (peerId == null) {
				Iterator<String> iPeerId = this.displayNameMap.keySet()
						.iterator();
				while (iPeerId.hasNext())
					if (!dataChannelManager.sendDcChat(false, message,
							iPeerId.next()))
						throw new SkyLinkException(
								"Unable to send the message via data channel");
			} else {
				if (!dataChannelManager.sendDcChat(true, message, peerId))
					throw new SkyLinkException(
							"Unable to send the message via data channel");
			}
		} else {
			final String str = "Cannot send P2P message as it was not enabled in the configuration.\nUse "
					+ "hasP2PMessage( true ) on TEMAConnectionConfig before creating TEMAConnectionManager.";
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from DC.
            if( connectionState == ConnectionState.DISCONNECT ) return;
  					lifeCycleDelegate.onReceiveLog(str);
          }
				}
			});
		}
	}

	/**
	 * Sends the user data related to oneself.
	 * 
	 * @param userData
	 *            User defined data relating to the peer. May be a
	 *            'java.lang.String', 'org.json.JSONObject' or
	 *            'org.json.JSONArray'.
	 */
	public void sendUserData(Object userData) {
		if (this.webServerClient == null)
			return;

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
	 * Mutes the client audio and intimates all the peers in the room.
	 * 
	 * @param isMuted
	 *            Flag that specify whether to mute / unmute the audio
	 */
	public void muteAudio(boolean isMuted) {
		if (this.webServerClient == null)
			return;

		if (myConfig.hasAudio() && (localAudioTrack.enabled() == isMuted)) {
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
	 * Mutes the client video and intimates all the peers in the room.
	 * 
	 * @param isMuted
	 *            Flag that specify whether to mute / unmute the audio
	 */
	public void muteVideo(boolean isMuted) {
		if (this.webServerClient == null)
			return;

		if (myConfig.hasVideo() && (localVideoTrack.enabled() == isMuted)) {
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
	 * Sends file transfer request to a specified peer.
	 * 
	 * @param peerId
	 *            The id of the peer
	 * @param fileName
	 *            The name of the file
	 * @param filePath
	 *            The path of the file in the filesystem
	 */
	public void sendFileTransferRequest(String peerId, String fileName,
			String filePath) {
		if (this.webServerClient == null)
			return;

		if (myConfig.hasFileTransfer()) {
			dataChannelManager.sendFileTransferRequest(peerId, fileName,
					filePath);
		} else {
			final String str = "Cannot do file transfer as it was not enabled in the configuration.\nUse "
					+ "hasFileTransfer( true ) on TEMAConnectionConfig before creating TEMAConnectionManager.";
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from DC.
            if( connectionState == ConnectionState.DISCONNECT ) return;
  					lifeCycleDelegate.onReceiveLog(str);
          }
				}
			});
		}
	}

	/**
	 * Call this method to accept or reject the file transfer request from a
	 * peer.
	 * 
	 * @param peerId
	 *            The id of the peer
	 * @param accept
	 *            Flag to indicate whether the request is accepted or not
	 * @param filePath
	 *            The path of the file
	 */
	public void acceptFileTransferRequest(String peerId, boolean accept,
			String filePath) {
		if (this.webServerClient == null)
			return;

		if (myConfig.hasFileTransfer())
			dataChannelManager.acceptFileTransfer(peerId, accept, filePath);
	}

	/**
	 * Retrives the user defined data object associated with a peer.
	 * 
	 * @param peerId
	 *            The id of the peer
	 * @return May be a 'java.lang.String', 'org.json.JSONObject' or
	 *         'org.json.JSONArray'.
	 */
	public Object getUserData(String peerId) {
		if (peerId == null)
			return this.myUserData;
		else
			return this.displayNameMap.get(peerId);
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

	// Cycle through likely device names for the camera and return the first
	// capturer that works, or crash if none do.
	private VideoCapturer getVideoCapturer() {
		String[] cameraFacing = { "front", "back" };
		int[] cameraIndex = { 0, 1 };
		int[] cameraOrientation = { 0, 90, 180, 270 };
		for (String facing : cameraFacing) {
			for (int index : cameraIndex) {
				for (int orientation : cameraOrientation) {
					String name = "Camera " + index + ", Facing " + facing
							+ ", Orientation " + orientation;
					VideoCapturer capturer = VideoCapturer.create(name);
					if (capturer != null) {
						logMessage("Using camera: " + name);
						return capturer;
					}
				}
			}
		}
		throw new RuntimeException("Failed to open capturer");
	}

	private List<Object> getWeightedPeerConnection(String key, double weight) {
		if (this.peerConnectionPool == null) {
			this.peerConnectionPool = new Hashtable<String, PeerConnection>();
			this.isMCUConnection = isPeerIdMCU(key);
      dataChannelManager.setIsMcuRoom( isMCUConnection );
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
	
	private PeerConnection getPeerConnection(String key) {
		if (this.peerConnectionPool == null) {
			this.peerConnectionPool = new Hashtable<String, PeerConnection>();
			this.isMCUConnection = isPeerIdMCU(key);
      dataChannelManager.setIsMcuRoom( isMCUConnection );
		}
		if (this.pcObserverPool == null)
			this.pcObserverPool = new Hashtable<String, PCObserver>();

		PeerConnection pc = this.peerConnectionPool.get(key);
		if (pc == null) {
			if (this.peerConnectionPool.size() >= MAX_PEER_CONNECTIONS
					&& !isPeerIdMCU(key))
				return null;

			logMessage("Creating a new peer connection ...");
			PCObserver pcObserver = new SkyLinkConnection.PCObserver();
			pcObserver.setMyId(key);
      // Prevent thread from executing with disconnect concurrently.
      synchronized( lockDisconnect ) {
  			pc = this.peerConnectionFactory.createPeerConnection(
  					this.iceServerArray, this.pcConstraints, pcObserver);
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
	 * Computes RFC 2104-compliant HMAC signature. * @param data The data to be
	 * signed.
	 * 
	 * @param key
	 *            The signing key.
	 * @return The Base64-encoded RFC 2104-compliant HMAC signature.
	 * @throws java.security.SignatureException
	 *             when signature generation fails
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
	private static String preferISAC(String sdpDescription) {
		String[] lines = sdpDescription.split("\r\n");
		int mLineIndex = -1;
		String isac16kRtpMap = null;
		Pattern isac16kPattern = Pattern
				.compile("^a=rtpmap:(\\d+) ISAC/16000[\r]?$");
		for (int i = 0; (i < lines.length)
				&& (mLineIndex == -1 || isac16kRtpMap == null); ++i) {
			if (lines[i].startsWith("m=audio ")) {
				mLineIndex = i;
				continue;
			}
			Matcher isac16kMatcher = isac16kPattern.matcher(lines[i]);
			if (isac16kMatcher.matches()) {
				isac16kRtpMap = isac16kMatcher.group(1);
				continue;
			}
		}
		if (mLineIndex == -1) {
			Log.d(TAG, "No m=audio line, so can't prefer iSAC");
			return sdpDescription;
		}
		if (isac16kRtpMap == null) {
			Log.d(TAG, "No ISAC/16000 line, so can't prefer iSAC");
			return sdpDescription;
		}
		String[] origMLineParts = lines[mLineIndex].split(" ");
		StringBuilder newMLine = new StringBuilder();
		int origPartIndex = 0;
		// Format is: m=<media> <port> <proto> <fmt> ...
		newMLine.append(origMLineParts[origPartIndex++]).append(" ");
		newMLine.append(origMLineParts[origPartIndex++]).append(" ");
		newMLine.append(origMLineParts[origPartIndex++]).append(" ");
		newMLine.append(isac16kRtpMap);
		for (; origPartIndex < origMLineParts.length; ++origPartIndex) {
			if (!origMLineParts[origPartIndex].equals(isac16kRtpMap)) {
				newMLine.append(" ").append(origMLineParts[origPartIndex]);
			}
		}
		lines[mLineIndex] = newMLine.toString();
		StringBuilder newSdpDescription = new StringBuilder();
		for (String line : lines) {
			newSdpDescription.append(line).append("\r\n");
		}
		return newSdpDescription.toString();
	}

	private void setUserInfo(JSONObject jsonObject) throws JSONException {
		JSONObject dictAudio = null;
		if (myConfig.hasAudio()) {
			dictAudio = new JSONObject();
			dictAudio.put("stereo", settingsObject.audio_stereo);
		}

		JSONObject dictVideo = null;
		if (myConfig.hasVideo()) {
			dictVideo = new JSONObject();
			dictVideo.put("frameRate", settingsObject.video_frameRate);
			JSONObject resolution = new JSONObject();
			resolution.put("height", settingsObject.video_height);
			resolution.put("width", settingsObject.video_width);
			dictVideo.put("resolution", resolution);
		}

		JSONObject dictBandwidth = new JSONObject();
		if (myConfig.hasAudio())
			dictBandwidth.put("audio", settingsObject.audio_bandwidth);
		if (myConfig.hasVideo())
			dictBandwidth.put("video", settingsObject.video_bandwidth);
		if (myConfig.hasPeerMessaging() || myConfig.hasFileTransfer())
			dictBandwidth.put("data", settingsObject.data_bandwidth);

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

		private SkyLinkConnection connectionManager = SkyLinkConnection.this;

		@SuppressLint("NewApi")
		@Override
		public void onIceServers(List<IceServer> iceServers) {
      MediaStream lms;
			if (iceServers == null) {
        // Prevent thread from executing with disconnect concurrently.
        synchronized( lockDisconnect ) {
          // If user has indicated intention to disconnect,
            // We should no longer process messages from signalling server.
          if( connectionState == ConnectionState.DISCONNECT ) return;
  				connectionManager.peerConnectionFactory = new PeerConnectionFactory();

  				connectionManager.logMessage("[SDK] Local video source: Creating...");
  				lms = connectionManager.peerConnectionFactory
  						.createLocalMediaStream("ARDAMS");
  				connectionManager.localMediaStream = lms;

  				if (myConfig.hasVideo()) {
  					VideoCapturer capturer = getVideoCapturer();
  					connectionManager.localVideoCapturer = capturer;
  					connectionManager.localVideoSource = connectionManager.peerConnectionFactory
                .createVideoSource(capturer,
                    connectionManager.webServerClient
                        .videoConstraints());
            final VideoTrack localVideoTrack = connectionManager.peerConnectionFactory
                .createVideoTrack("ARDAMSv0",
                    connectionManager.localVideoSource);
            if (localVideoTrack != null) {
              lms.addTrack(localVideoTrack);
              connectionManager.localVideoTrack = localVideoTrack;
            }

            final Point displaySize = new Point();
            myActivity.getWindowManager().getDefaultDisplay()
                .getRealSize(displaySize);
          }
        }

        myActivity.runOnUiThread(new Runnable() {
          public void run() {
            // Prevent thread from executing with disconnect concurrently.
            synchronized( lockDisconnectMedia ) {
              // If user has indicated intention to disconnect,
                // We should no longer process messages from signalling server.
              if( connectionState == ConnectionState.DISCONNECT ) return;
              localVideoView = new GLSurfaceView(myActivity
                  .getApplicationContext());
              VideoRendererGui gui = new VideoRendererGui(
                  localVideoView);
              gui.setDelegate(connectionManager.videoRendererGuiDelegate);
              VideoRenderer.Callbacks localRender = gui.create(0,
                  0, 100, 100);
              localVideoTrack.addRenderer(new VideoRenderer(
                  localRender));

              if (connectionManager.surfaceOnHoldPool == null)
                connectionManager.surfaceOnHoldPool = new Hashtable<GLSurfaceView, String>();
              connectionManager.logMessage("[SDK] Local video source: Created.");
              // connectionManager.surfaceOnHoldPool.put(localVideoView, MY_SELF);
              lifeCycleDelegate.onGetUserMedia(localVideoView, null);
              connectionManager.logMessage("[SDK] Local video source: Sent to App.");
            }
          }
        });

        synchronized( lockDisconnect ) {
          // If user has indicated intention to disconnect,
            // We should no longer process messages from signalling server.
          if( connectionState == ConnectionState.DISCONNECT ) return;
          if (myConfig.hasAudio()) {
            connectionManager.logMessage("[SDK] Local audio source: Creating...");
            connectionManager.localAudioSource = connectionManager.peerConnectionFactory
                .createAudioSource(new MediaConstraints());
            connectionManager.localAudioTrack = connectionManager.peerConnectionFactory
                .createAudioTrack("ARDAMSa0",
                    connectionManager.localAudioSource);
            lms.addTrack(connectionManager.localAudioTrack);
            connectionManager.logMessage("[SDK] Local audio source: Created.");
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
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from signalling server.
            if( connectionState == ConnectionState.DISCONNECT ) return;
  					lifeCycleDelegate.onConnect( message == null, message );
          }
				}
			});
		}

	}

	/*
	 * GAEChannelClient.MessageHandler
	 */
	private class MyMessageHandler implements WebServerClient.MessageHandler {

		private SkyLinkConnection connectionManager = SkyLinkConnection.this;

		@Override
		public void onOpen() {
      // Prevent thread from executing with disconnect concurrently.
      synchronized( lockDisconnect ) {
        // If user has indicated intention to disconnect,
          // We should no longer process messages from signalling server.
        if( connectionState == ConnectionState.DISCONNECT ) return;
  			connectionManager.iceServersObserver.onIceServers(null);
      }
		}

		@Override
		public void onMessage(String data) {
      // Prevent thread from executing with disconnect concurrently.
      synchronized( lockDisconnectMsg ) {
        // If user has indicated intention to disconnect,
          // We should no longer process messages from signalling server.
        if( connectionState == ConnectionState.DISCONNECT ) return;
  			try {
  				messageProcessor(data);
  			} catch (JSONException e) {
  				Log.e(TAG, e.getMessage(), e);
  			}
      }
		}

		private void messageProcessor(String data) throws JSONException {
			String message = data;
			JSONObject objects = new JSONObject(data);

			final String value = objects.getString("type");
			connectionManager.logMessage("[SDK] onMessage type - " + value);

			if (value.compareTo("inRoom") == 0) {
				String mid = objects.getString("sid");
				connectionManager.webServerClient.setSid(mid);
				JSONObject pcConfigJSON = objects.getJSONObject("pc_config");
				String username = "";// pcConfigJSON.getString("username");
				username = username != null ? username : "";
				List<PeerConnection.IceServer> result = new ArrayList<PeerConnection.IceServer>();
				JSONArray iceServers = pcConfigJSON.getJSONArray("iceServers");
				for (int i = 0; i < iceServers.length(); i++) {
					JSONObject iceServer = iceServers.getJSONObject(i);
					String url = iceServer.getString("url");
					if( myConfig.isStunDisabled() && url.startsWith("stun:") ) {
            connectionManager.logMessage(
              "[SDK] Not adding stun server as stun disabled in config.");
            continue;
          }
					if( myConfig.isTurnDisabled() && url.startsWith("turn:") ){
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
				enterObject.put("agent", "Android");
				enterObject.put("version", Build.VERSION.SDK_INT);
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

				boolean receiveOnly = false;
				try {
					receiveOnly = objects.getBoolean("receiveOnly");
				} catch (JSONException e) {
				}
				if (myConfig.hasAudio() && !receiveOnly)
					peerConnection.addStream(
							connectionManager.localMediaStream,
							connectionManager.pcConstraints);
				
				if (peerConnection != null) {
					setDisplayMap(userData, mid);
					connectionManager
							.logMessage("[SDK] onMessage - Sending 'welcome'.");

					JSONObject welcomeObject = new JSONObject();
					welcomeObject.put("type", "welcome");
					welcomeObject.put("weight",
							connectionManager.pcObserverPool.get(mid)
									.getMyWeight());
					welcomeObject.put("mid",
							connectionManager.webServerClient.getSid());
					welcomeObject.put("target", mid);
					welcomeObject.put("rid",
							connectionManager.webServerClient.getRoomId());
					welcomeObject.put("agent", "Android");
					welcomeObject.put("version", Build.VERSION.SDK_INT);
					setUserInfo(welcomeObject);
					connectionManager.webServerClient
							.sendMessage(welcomeObject);
				} else {
					connectionManager
							.logMessage("I only support "
									+ MAX_PEER_CONNECTIONS
									+ " connections are in this app. I am discarding this 'welcome'.");
				}

			} else if (value.compareTo("welcome") == 0) {

				String target = objects.getString("target");
				if (target
						.compareTo(connectionManager.webServerClient.getSid()) != 0)
					return;

				String mid = objects.getString("mid");
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
				List<Object> weightedConnection = connectionManager
						.getWeightedPeerConnection(mid, weight);
				if (!(Boolean) weightedConnection.get(0)) {
					Log.d(TAG, "Ignoring this welcome");
					return;
				}
				Object secondObject = weightedConnection.get(1);
				if (secondObject instanceof PeerConnection)
					peerConnection = (PeerConnection) secondObject;
				if (peerConnection == null) {
					connectionManager
							.logMessage("I only support "
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
				if (myConfig.hasAudio() && !receiveOnly)
					peerConnection.addStream(
							connectionManager.localMediaStream,
							connectionManager.pcConstraints);

				connectionManager.logMessage("[SDK] onMessage - create offer.");
				if (myConfig.hasPeerMessaging() || myConfig.hasFileTransfer()) {
					// Create DataChannel
					// It is stored by dataChannelManager.
					connectionManager.dataChannelManager.createDataChannel(
							peerConnection, target, mid, "", null, mid );
				}

				if (connectionManager.sdpObserverPool == null)
					connectionManager.sdpObserverPool = new Hashtable<String, SDPObserver>();
				SDPObserver sdpObserver = connectionManager.sdpObserverPool
						.get(mid);
				if (sdpObserver == null) {
					connectionManager.sdpObserverPool = new Hashtable<String, SDPObserver>();
					sdpObserver = new SkyLinkConnection.SDPObserver();
					sdpObserver.setMyId(mid);
					connectionManager.sdpObserverPool.put(mid, sdpObserver);
				}
				peerConnection.createOffer(sdpObserver,
						connectionManager.sdpMediaConstraints);
				connectionManager.logMessage("PC - createOffer for " + mid);

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
				sdpString = sdpString.replaceAll("\r\nb=AS:50\r\n", "\n");
				SessionDescription sdp = new SessionDescription(
						SessionDescription.Type.fromCanonicalForm(value),
						preferISAC(sdpString));

				if (connectionManager.sdpObserverPool == null)
					connectionManager.sdpObserverPool = new Hashtable<String, SDPObserver>();
				SDPObserver sdpObserver = connectionManager.sdpObserverPool
						.get(mid);
				if (sdpObserver == null) {
					connectionManager.sdpObserverPool = new Hashtable<String, SDPObserver>();
					sdpObserver = new SkyLinkConnection.SDPObserver();
					sdpObserver.setMyId(mid);
					connectionManager.sdpObserverPool.put(mid, sdpObserver);
				}
				peerConnection.setRemoteDescription(sdpObserver, sdp);
				connectionManager
						.logMessage("PC - setRemoteDescription. Sending "
								+ sdp.type + " to " + mid);

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
					myActivity.runOnUiThread(new Runnable() {
						public void run() {
              // Prevent thread from executing with disconnect concurrently.
              synchronized( lockDisconnect ) {
                // If user has indicated intention to disconnect,
                  // We should no longer process messages from signalling server.
                if( connectionState == ConnectionState.DISCONNECT ) return;
  							messagesDelegate.onChatMessage(mid, nick, text, target != null);
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
					myActivity.runOnUiThread(new Runnable() {
						public void run() {
              // Prevent thread from executing with disconnect concurrently.
              synchronized( lockDisconnect ) {
                // If user has indicated intention to disconnect,
                  // We should no longer process messages from signalling server.
                if( connectionState == ConnectionState.DISCONNECT ) return;
  							remotePeerDelegate.onPeerLeave( mid, "The peer has left the room" );
              }
						}
					});
        }
				PeerConnection peerConnection = connectionManager
						.getPeerConnection(mid);

        // Dispose DataChannel.
        connectionManager.dataChannelManager.disposeDC( mid );

				peerConnection.dispose();

				connectionManager.peerConnectionPool.remove(mid);
				connectionManager.pcObserverPool.remove(mid);
				connectionManager.sdpObserverPool.remove(mid);
				connectionManager.displayNameMap.remove(mid);

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

				final String info = objects.getString("info");
				final String action = objects.getString("action");
				myActivity.runOnUiThread(new Runnable() {
					public void run() {
            // Prevent thread from executing with disconnect concurrently.
            synchronized( lockDisconnect ) {
              // If user has indicated intention to disconnect,
                // We should no longer process messages from signalling server.
              if( connectionState == ConnectionState.DISCONNECT ) return;
  						if (action.compareTo("warning") == 0)
  							lifeCycleDelegate.onWarning(info);
  						else
  							lifeCycleDelegate.onDisconnect(info);
            }
					}
				});

			} else if (value.compareTo("private") == 0
					|| value.compareTo("public") == 0) {

				final Object objData = objects.get("data");
				final String mid = objects.getString("mid");
				if (!connectionManager.isPeerIdMCU(mid)) {
					myActivity.runOnUiThread(new Runnable() {
						public void run() {
              // Prevent thread from executing with disconnect concurrently.
              synchronized( lockDisconnect ) {
                // If user has indicated intention to disconnect,
                  // We should no longer process messages from signalling server.
                if( connectionState == ConnectionState.DISCONNECT ) return;
  							messagesDelegate.onCustomMessage( mid, objData, value.compareTo( "private" ) == 0 );
              }
						}
					});
        }
			} else if (value.compareTo("updateUserEvent") == 0) {

				final String mid = objects.getString("mid");
				final Object userData = objects.get("userData");
				if (!connectionManager.isPeerIdMCU(mid)) {
					myActivity.runOnUiThread(new Runnable() {
						public void run() {
              // Prevent thread from executing with disconnect concurrently.
              synchronized( lockDisconnect ) {
                // If user has indicated intention to disconnect,
                  // We should no longer process messages from signalling server.
                if( connectionState == ConnectionState.DISCONNECT ) return;
  							remotePeerDelegate.onUserData(mid, userData);
              }
						}
					});
        }
			} else if (value.compareTo("muteAudioEvent") == 0) {

				if (myConfig.hasAudio()) {
					final String mid = objects.getString("mid");
					final boolean muted = objects.getBoolean("muted");
					if (!connectionManager.isPeerIdMCU(mid)) {
						myActivity.runOnUiThread(new Runnable() {
							public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized( lockDisconnect ) {
                  // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                  if( connectionState == ConnectionState.DISCONNECT ) return;
  								mediaDelegate.onToggleAudio(mid, muted);
                }
							}
						});
          }
				}

			} else if (value.compareTo("muteVideoEvent") == 0) {

				if (myConfig.hasVideo()) {
					final String mid = objects.getString("mid");
					final boolean muted = objects.getBoolean("muted");
					if (!connectionManager.isPeerIdMCU(mid)) {
						myActivity.runOnUiThread(new Runnable() {
							public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized( lockDisconnect ) {
                  // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                  if( connectionState == ConnectionState.DISCONNECT ) return;
  								mediaDelegate.onToggleVideo(mid, muted);
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
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from signalling server.
            if( connectionState == ConnectionState.DISCONNECT ) return;
            connectionManager.logMessage("[SDK] onClose.");
  					lifeCycleDelegate.onDisconnect("Connection with the skylink server is closed");
          }
        }
      });
		}

		@Override
		public void onError( final int code, final String description) {
      myActivity.runOnUiThread(new Runnable() {
        public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from signalling server.
            if( connectionState == ConnectionState.DISCONNECT ) return;
      			final String message = "[SDK] onError: " + code + ", " + description;
      			connectionManager.logMessage(message);
  					lifeCycleDelegate.onDisconnect(message);
          }
				}
			});
		}

	}

	/*
	 * VideoRendererGui.VideoRendererGuiDelegate
	 */
	private class MyVideoRendererGuiDelegate implements
			VideoRendererGui.VideoRendererGuiDelegate {

		@Override
		public void updateDisplaySize(final GLSurfaceView surface,
				final Point screenDimensions) {
			myActivity.runOnUiThread(new Runnable() {
				@SuppressWarnings("unused")
				public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnectMedia ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from signalling server.
            if( connectionState == ConnectionState.DISCONNECT ) return;
  					if (true/*SkyLinkConnection.this.surfaceOnHoldPool.get(surface) == null*/) {
  						mediaDelegate.onVideoSize(surface, screenDimensions);
  					} else {
  						String peerId = SkyLinkConnection.this.surfaceOnHoldPool
  								.get(surface);
  						SkyLinkConnection.this.surfaceOnHoldPool
  								.remove(surface);
  						if (peerId.compareToIgnoreCase(MY_SELF) == 0) {
  							lifeCycleDelegate.onGetUserMedia(surface,
  									screenDimensions);
  						} else {
  							remotePeerDelegate.onGetPeerMedia(peerId, surface,
  									screenDimensions);
  						}
  					}
          }
				}
			});
		}

	}

	// Implementation detail: observe ICE & stream changes and react
	// accordingly.
	private class PCObserver implements PeerConnection.Observer {

		private SkyLinkConnection connectionManager = SkyLinkConnection.this;

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
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from signalling server.
            if( connectionState == ConnectionState.DISCONNECT ) return;
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
		public void onError() {
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from signalling server.
            if( connectionState == ConnectionState.DISCONNECT ) return;
  					throw new RuntimeException("PeerConnection error!");
          }
				}
			});
		}

		@Override
		public void onSignalingChange(PeerConnection.SignalingState newState) {
		}

		@Override
		public void onIceConnectionChange( PeerConnection.IceConnectionState newState ) {
		}

		@Override
		public void onIceGatheringChange(
				PeerConnection.IceGatheringState newState) {
			if (newState == PeerConnection.IceGatheringState.COMPLETE
					&& connectionManager.isMCUConnection)
				myActivity.runOnUiThread(new Runnable() {
					public void run() {
            // Prevent thread from executing with disconnect concurrently.
            synchronized( lockDisconnect ) {
              // If user has indicated intention to disconnect,
                // We should no longer process messages from signalling server.
              if( connectionState == ConnectionState.DISCONNECT ) return;
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
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from signalling server.
            if( connectionState == ConnectionState.DISCONNECT ) return;
  					abortUnless(stream.audioTracks.size() <= 1
  							&& stream.videoTracks.size() <= 1,
  							"Weird-looking stream: " + stream);
  					GLSurfaceView remoteVideoView = null;
  					if (stream.videoTracks.size() == 1 && myConfig.hasVideo()) {
  						Point displaySize = new Point();
  						myActivity.getWindowManager().getDefaultDisplay()
  								.getRealSize(displaySize);
  						remoteVideoView = new GLSurfaceView(myActivity
  								.getApplicationContext());
  						VideoRendererGui gui = new VideoRendererGui(
  								remoteVideoView);
  						gui.setDelegate(connectionManager.videoRendererGuiDelegate);
  						VideoRenderer.Callbacks remoteRender = gui.create(0, 0,
  								100, 100);
  						stream.videoTracks.get(0).addRenderer(
  								new VideoRenderer(remoteRender));

  						final GLSurfaceView rVideoView = remoteVideoView;
  						// connectionManager.surfaceOnHoldPool.put(rVideoView, myId);
  						if (!connectionManager.isPeerIdMCU(myId))
  							remotePeerDelegate.onGetPeerMedia( myId, rVideoView, null );
  					}
          }
				}
			});
		}

		@Override
		public void onRemoveStream(final MediaStream stream) {
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from signalling server.
            if( connectionState == ConnectionState.DISCONNECT ) return;
  					stream.videoTracks.get(0).dispose();
          }
				}
			});
		}

		@Override
		public void onDataChannel( final DataChannel dc ) {
      // Prevent thread from executing with disconnect concurrently.
      synchronized( lockDisconnect ) {
        // If user has indicated intention to disconnect,
          // We should no longer process messages from signalling server.
        if( connectionState == ConnectionState.DISCONNECT ) return;
  			if (myConfig.hasPeerMessaging() || myConfig.hasFileTransfer()) {
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

		private SkyLinkConnection connectionManager = SkyLinkConnection.this;

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
      synchronized( lockDisconnect ) {
        // If user has indicated intention to disconnect,
          // We should no longer process messages from signalling server.
        if( connectionState == ConnectionState.DISCONNECT ) return;
  			abortUnless(this.localSdp == null, "multiple SDP create?!?");
  			sdp = new SessionDescription( origSdp.type, preferISAC( origSdp.description ) );
  			this.localSdp = sdp;
  			pc = connectionManager.peerConnectionPool
  					.get(this.myId);
      }
      myActivity.runOnUiThread(new Runnable() {
        public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from signalling server.
            if( connectionState == ConnectionState.DISCONNECT ) return;

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
      synchronized( lockDisconnectSdp ) {
        // If user has indicated intention to disconnect,
          // We should no longer process messages from signalling server.
        if( connectionState == ConnectionState.DISCONNECT ) return;
  			pc = connectionManager.peerConnectionPool.get( this.myId );
      }
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from signalling server.
            if( connectionState == ConnectionState.DISCONNECT ) return;
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
  							if( !connectionManager.isPeerIdMCU( myId ) ) {
  								String tid = SDPObserver.this.myId;
  								remotePeerDelegate.onPeerJoin( tid, connectionManager.displayNameMap.get( tid ) );
                }
  						}
  					}
  				}
        }
			});
		}

		@Override
		public void onCreateFailure(final String error) {
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from signalling server.
            if( connectionState == ConnectionState.DISCONNECT ) return;
  					throw new RuntimeException("createSDP error: " + error);
          }
				}
			});
		}

		@Override
		public void onSetFailure(final String error) {
      myActivity.runOnUiThread(new Runnable() {
        public void run() {
          // Prevent thread from executing with disconnect concurrently.
          synchronized( lockDisconnect ) {
            // If user has indicated intention to disconnect,
              // We should no longer process messages from signalling server.
            if( connectionState == ConnectionState.DISCONNECT ) return;
  					throw new RuntimeException("setSDP error: " + error);
          }
				}
			});
		}

		private void sendLocalDescription(SessionDescription sdp) {
      // Prevent thread from executing with disconnect concurrently.
      synchronized( lockDisconnect ) {
        // If user has indicated intention to disconnect,
          // We should no longer process messages from signalling server.
        if( connectionState == ConnectionState.DISCONNECT ) return;
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
      synchronized( lockDisconnect ) {
        // If user has indicated intention to disconnect,
          // We should no longer process messages from signalling server.
        if( connectionState == ConnectionState.DISCONNECT ) return;
  			connectionManager.logMessage("Inside SDPObserver.drainRemoteCandidates()");
      }
		}
	}

}
