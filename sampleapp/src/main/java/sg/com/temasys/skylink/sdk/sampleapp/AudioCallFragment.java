package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.Activity;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.webrtc.SurfaceViewRenderer;

import java.util.Date;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigFragment;

import static sg.com.temasys.skylink.sdk.sampleapp.Utils.getNumRemotePeers;

/**
 * This class is used to demonstrate the AudioCall between two clients in WebRTC Created by
 * lavanyasudharsanam on 20/1/15.
 */
public class AudioCallFragment extends Fragment
        implements LifeCycleListener, MediaListener, RemotePeerListener {
    // Inflate the layout for this fragment

    private String ROOM_NAME;
    private String MY_USER_NAME;

    private static final String TAG = AudioCallFragment.class.getCanonicalName();
    private static final String ARG_SECTION_NUMBER = "section_number";

    // Constants for configuration change
    private static final String BUNDLE_IS_PEER_JOINED = "peerJoined";
    private static final String BUNDLE_PEER_ID = "peerId";
    private static final String BUNDLE_PEER_NAME = "remotePeerName";
    private static SkylinkConnection skylinkConnection;
    private TextView tvRoomDetails;
    private Button btnAudioCall;
    private String remotePeerId;
    private String remotePeerName;
    private boolean peerJoined;
    private Activity parentActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ROOM_NAME = Config.ROOM_NAME_AUDIO;
        MY_USER_NAME = Config.USER_NAME_AUDIO;

        View rootView = inflater.inflate(R.layout.fragment_audio_call, container, false);
        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_room_details);
        btnAudioCall = (Button) rootView.findViewById(R.id.btn_audio_call);

        // Check if it was an orientation change
        if (savedInstanceState != null) {
            // Set the appropriate UI if already isConnected().
            if (isConnected()) {
                // Set listeners to receive callbacks when events are triggered
                setListeners();
                peerJoined = savedInstanceState.getBoolean(BUNDLE_IS_PEER_JOINED);
                remotePeerId = savedInstanceState.getString(BUNDLE_PEER_ID, null);
                remotePeerName = savedInstanceState.getString(BUNDLE_PEER_NAME, null);
                // Set the appropriate UI if already isConnected().
                onConnectUIChange();
            } else {
                onDisconnectUIChange();
            }
        }

        btnAudioCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToRoom();
                onConnectUIChange();
            }
        });
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Allow volume to be controlled using volume keys
        getActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //update actionbar title
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
        parentActivity = getActivity();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save states for fragment restart
        outState.putBoolean(BUNDLE_IS_PEER_JOINED, peerJoined);
        outState.putString(BUNDLE_PEER_ID, remotePeerId);
        outState.putString(BUNDLE_PEER_NAME, remotePeerName);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        disconnectFromRoom();
    }

    private void disconnectFromRoom() {
        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        if (!parentActivity.isChangingConfigurations() && skylinkConnection != null
                && isConnected()) {
            skylinkConnection.disconnectFromRoom();
            AudioRouter.stopAudioRouting(parentActivity.getApplicationContext());
        }
    }

    /***
     * Skylink Helper methods
     */

    /**
     * Check if we are currently connected to the room.
     *
     * @return True if we are connected and false otherwise.
     */
    private boolean isConnected() {
        if (skylinkConnection != null) {
            return skylinkConnection.isConnected();
        }
        return false;
    }

    private void connectToRoom() {
        // Initialize the skylink connection
        initializeSkylinkConnection();

        // Obtaining the Skylink connection string done locally
        // In a production environment the connection string should be given
        // by an entity external to the App, such as an App server that holds the Skylink
        // App secret
        // In order to avoid keeping the App secret within the application
        String skylinkConnectionString = Utils.getSkylinkConnectionString(
                ROOM_NAME, new Date(), SkylinkConnection.DEFAULT_DURATION);

        boolean connectFailed;
        connectFailed = !skylinkConnection.connectToRoom(skylinkConnectionString, MY_USER_NAME);
        if (connectFailed) {
            Toast.makeText(parentActivity, "Unable to connect to room!", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(parentActivity, "Connecting....",
                    Toast.LENGTH_SHORT).show();
        }

        // Initialize and use the Audio router to switch between headphone and headset
        AudioRouter.startAudioRouting(parentActivity);

    }

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        // AudioVideo config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        config.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
/*
        // To limit audio bandwidth:
        config.setMaxAudioBitrate(20);  // Default is not limited.
*/

        config.setTimeout(ConfigFragment.TIME_OUT);
        // To enable logs from Skylink SDK (e.g. during debugging),
        // Uncomment the following. Do not enable logs for production apps!
        // config.setEnableLogs(true);

        // Allow only 1 remote Peer to join.
        config.setMaxPeers(1); // Default is 4 remote Peers.

        return config;
    }

    private void initializeSkylinkConnection() {
        skylinkConnection = SkylinkConnection.getInstance();
        skylinkConnection.init(Config.getAppKey(), getSkylinkConfig(),
                this.parentActivity.getApplicationContext());

        // Set listeners to receive callbacks when events are triggered
        setListeners();
    }

    /**
     * Set listeners to receive callbacks when events are triggered.
     * SkylinkConnection instance must not be null or listeners cannot be set.
     *
     * @return false if listeners could not be set.
     */
    private boolean setListeners() {
        if (skylinkConnection != null) {
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setMediaListener(this);
            skylinkConnection.setRemotePeerListener(this);
            return true;
        } else {
            return false;
        }
    }


    /***
     * UI helper methods
     */

    /**
     * Change certain UI elements once isConnected() to room or when Peer(s) join or leave.
     */
    private void onConnectUIChange() {
        btnAudioCall.setEnabled(false);
        Utils.setRoomDetails(isConnected(), peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME,
                MY_USER_NAME);
    }

    /**
     * Change certain UI elements when disconnecting from room.
     */
    private void onDisconnectUIChange() {
        btnAudioCall.setEnabled(true);
        Utils.setRoomDetails(isConnected(), peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME,
                MY_USER_NAME);
    }

    /***
     * Lifecycle Listener
     */

    /**
     * Triggered when connection is successful
     *
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        if (isSuccess) {
            Log.d(TAG, "Skylink Connected");
            Utils.setRoomDetails(isConnected(), peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME,
                    MY_USER_NAME);
        } else {
            Log.d(TAG, "Skylink failed to connect!");
            Toast.makeText(parentActivity, "Skylink failed to connect!\nReason : "
                    + message, Toast.LENGTH_SHORT).show();
            onDisconnectUIChange();
        }
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Log.d(TAG, message + "warning");
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        onDisconnectUIChange();

        String log = "";
        if (errorCode == Errors.DISCONNECT_FROM_ROOM) {
            log += "We have successfully disconnected from the room.";
        } else if (errorCode == Errors.DISCONNECT_UNEXPECTED_ERROR) {
            log += "WARNING! We have been unexpectedly disconnected from the room!";
        }
        log += " Server message: " + message;

        Toast.makeText(parentActivity, log, Toast.LENGTH_LONG).show();
        log = "[onDisconnect] " + log;
        Log.d(TAG, log);
    }

    @Override
    public void onReceiveLog(int infoCode, String message) {
        Log.d(TAG, message + "onReceiveLog");
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        Toast.makeText(parentActivity, "Peer " + remotePeerId +
                " has changed Room locked status to " + lockStatus, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocalMediaCapture(SurfaceViewRenderer surfaceView) {
        Log.d(TAG, "onLocalMediaCapture");
    }

    @Override
    public void onVideoSizeChange(String peerId, Point size) {
        String peer = "Peer " + peerId;
        // If peerId is null, this call is for our local video.
        if (peerId == null) {
            peer = "We've";
        }
        Log.d(TAG, peer + " got video size changed to: " + size.toString() + ".");
    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, SurfaceViewRenderer videoView) {
        String log = "Received new ";
        if (videoView != null) {
            log += "Video ";
        } else {
            log += "Audio ";
        }
        log += "from Peer " + Utils.getPeerIdNick(remotePeerId) + ".\r\n";

        UserInfo remotePeerUserInfo = skylinkConnection.getUserInfo(remotePeerId);
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".";
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        String log = "Peer " + Utils.getPeerIdNick(remotePeerId) +
                " Audio mute status via:\r\nCallback: " + isMuted + ".";

        // It is also possible to get the mute status via the UserInfo.
        UserInfo userInfo = skylinkConnection.getUserInfo(remotePeerId);
        if (userInfo != null) {
            log += "\r\nUserInfo: " + userInfo.isAudioMuted() + ".";
        }
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRemotePeerVideoToggle(String remotePeerId, boolean isMuted) {
        String log = "Peer " + Utils.getPeerIdNick(remotePeerId) +
                " Video mute status via:\r\nCallback: " + isMuted + ".";

        // It is also possible to get the mute status via the UserInfo.
        UserInfo userInfo = skylinkConnection.getUserInfo(remotePeerId);
        if (userInfo != null) {
            log += "\r\nUserInfo: " + userInfo.isVideoMuted() + ".";
        }
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        // When remote peer joins room, keep track of user and update text-view to display details
        this.remotePeerId = remotePeerId;
        peerJoined = true;
        if (userData instanceof String) {
            remotePeerName = (String) userData;
        }
        Utils.setRoomDetails(isConnected(), peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME,
                MY_USER_NAME);

        String log = "Your Peer " + Utils.getPeerIdNick(remotePeerId) + " connected.";
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message, UserInfo userInfo) {
        //reset peerId
        peerJoined = false;
        this.remotePeerId = null;
        remotePeerName = null;
        //update textview to show room status
        Utils.setRoomDetails(isConnected(), peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME,
                MY_USER_NAME);

        int numRemotePeers = getNumRemotePeers();
        String log = "Your Peer " + Utils.getPeerIdNick(remotePeerId, userInfo) + " left: " +
                message + ". " + numRemotePeers + " remote Peer(s) left in the room.";
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRemotePeerConnectionRefreshed(String remotePeerId, Object userData,
                                                boolean hasDataChannel, boolean wasIceRestarted) {
        String peer = "Skylink Media Relay server";
        if (remotePeerId != null) {
            peer = "Peer " + Utils.getPeerIdNick(remotePeerId);
        }
        String log = "Your connection with " + peer + " has just been refreshed";
        if (wasIceRestarted) {
            log += ", with ICE restarted.";
        } else {
            log += ".\r\n";
        }

        UserInfo remotePeerUserInfo = skylinkConnection.getUserInfo(remotePeerId);
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".";
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRemotePeerUserDataReceive(String s, Object o) {
        Log.d(TAG, "onRemotePeerUserDataReceive");
    }

    @Override
    public void onOpenDataConnection(String s) {
        Log.d(TAG, "onOpenDataConnection");

    }

}
