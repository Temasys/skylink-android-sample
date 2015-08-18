package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.Activity;
import android.graphics.Point;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

import java.util.Date;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.ErrorCodes;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;

/**
 * This class is used to demonstrate the AudioCall between two clients in WebRTC Created by
 * lavanyasudharsanam on 20/1/15.
 */
public class AudioCallFragment extends Fragment implements LifeCycleListener, MediaListener, RemotePeerListener {
    private static final String TAG = AudioCallFragment.class.getCanonicalName();
    public static final String ROOM_NAME = Constants.ROOM_NAME_AUDIO;
    public static final String MY_USER_NAME = "audioCallUser";
    private static final String ARG_SECTION_NUMBER = "section_number";

    // Constants for configuration change
    private static final String BUNDLE_IS_CONNECTED = "isConnected";
    private static final String BUNDLE_IS_PEER_JOINED = "peerJoined";
    private static final String BUNDLE_PEER_ID = "peerId";
    private static final String BUNDLE_PEER_NAME = "remotePeerName";

    private TextView tvRoomDetails;
    private Button btnAudioCall;

    private static SkylinkConnection skylinkConnection;
    private String remotePeerId;
    private String remotePeerName;
    private boolean connected;
    private boolean peerJoined;
    private boolean orientationChange;
    private Activity parentActivity;
    private AudioRouter audioRouter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_audio_call, container, false);

        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_room_details);
        btnAudioCall = (Button) rootView.findViewById(R.id.btn_audio_call);

        // Check if it was an orientation change
        if (savedInstanceState != null) {
            connected = savedInstanceState.getBoolean(BUNDLE_IS_CONNECTED);
            // Set the appropriate UI if already connected.
            if (connected) {
                // Set listeners to receive callbacks when events are triggered
                setListeners();
                peerJoined = savedInstanceState.getBoolean(BUNDLE_IS_PEER_JOINED);
                remotePeerId = savedInstanceState.getString(BUNDLE_PEER_ID, null);
                remotePeerName = savedInstanceState.getString(BUNDLE_PEER_NAME, null);
                // Set the appropriate UI if already connected.
                onConnectUIChange();
                initializeAudioRouter();
            }
        }

        btnAudioCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String appKey = getString(R.string.app_key);
                String appSecret = getString(R.string.app_secret);

                // Initialize the skylink connection
                initializeSkylinkConnection();

                // Initialize the audio router
                initializeAudioRouter();

                // Obtaining the Skylink connection string done locally
                // In a production environment the connection string should be given
                // by an entity external to the App, such as an App server that holds the Skylink App secret
                // In order to avoid keeping the App secret within the application
                String skylinkConnectionString = Utils.
                        getSkylinkConnectionString(ROOM_NAME, appKey,
                                appSecret, new Date(), SkylinkConnection.DEFAULT_DURATION);

                skylinkConnection.connectToRoom(skylinkConnectionString, MY_USER_NAME);

                // Use the Audio router to switch between headphone and headset
                audioRouter.startAudioRouting(parentActivity.getApplicationContext());

                onConnectUIChange();

                Toast.makeText(parentActivity, "Connecting....",
                        Toast.LENGTH_SHORT).show();
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
        // Note that orientation change is happening.
        orientationChange = true;
        // Save states for fragment restart
        outState.putBoolean(BUNDLE_IS_CONNECTED, connected);
        outState.putBoolean(BUNDLE_IS_PEER_JOINED, peerJoined);
        outState.putString(BUNDLE_PEER_ID, remotePeerId);
        outState.putString(BUNDLE_PEER_NAME, remotePeerName);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already connected and not changing orientation.
        if (!orientationChange && skylinkConnection != null && connected) {
            skylinkConnection.disconnectFromRoom();
            connected = false;
            if (audioRouter != null && parentActivity != null) {
                audioRouter.stopAudioRouting(parentActivity.getApplicationContext());
            }
        }
    }

    /***
     * Helper methods
     */

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        config.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        return config;
    }

    private void initializeAudioRouter() {
        if (audioRouter == null) {
            audioRouter = AudioRouter.getInstance();
            audioRouter.init(((AudioManager) parentActivity.
                    getSystemService(android.content.Context.AUDIO_SERVICE)));
        }
    }

    private void initializeSkylinkConnection() {
        if (skylinkConnection == null) {
            skylinkConnection = SkylinkConnection.getInstance();
            skylinkConnection.init(getString(R.string.app_key), getSkylinkConfig(),
                    this.parentActivity.getApplicationContext());

            // Set listeners to receive callbacks when events are triggered
            setListeners();
        }
    }

    /**
     * Set listeners to receive callbacks when events are triggered
     */
    private void setListeners() {
        skylinkConnection.setLifeCycleListener(this);
        skylinkConnection.setMediaListener(this);
        skylinkConnection.setRemotePeerListener(this);
    }

    /***
     * UI helper methods
     */

    /**
     * Change certain UI elements once connected to room or when Peer(s) join or leave.
     */
    private void onConnectUIChange() {
        btnAudioCall.setEnabled(false);
        Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME, MY_USER_NAME);
    }

    /***
     * Lifecycle Listener
     */

    /**
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        if (isSuccess) {
            connected = true;
            Log.d(TAG, "Skylink Connected");
            Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME, MY_USER_NAME);
        } else {
            Toast.makeText(parentActivity, "Skylink Connection Failed\nReason : "
                    + message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Log.d(TAG, message + "warning");
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        skylinkConnection = null;
        Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME, MY_USER_NAME);

        String log = message;
        if (errorCode == ErrorCodes.DISCONNECT_FROM_ROOM) {
            log = "[onDisconnect] We have successfully disconnected from the room. Server message: "
                    + message;
        }
        Toast.makeText(parentActivity, "[onDisconnect] " + log, Toast.LENGTH_LONG).show();
        Log.d(TAG, log);
    }

    @Override
    public void onReceiveLog(String message) {
        Log.d(TAG, message + "onReceiveLog");
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        Toast.makeText(parentActivity, "Peer " + remotePeerId +
                " has changed Room locked status to " + lockStatus, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocalMediaCapture(GLSurfaceView glSurfaceView) {
        Log.d(TAG, "onLocalMediaCapture");
    }

    @Override
    public void onVideoSizeChange(String remotePeerId, Point point) {
        Log.d(TAG, point.toString() + "got size");
    }

    @Override
    public void onRemotePeerAudioToggle(String s, boolean b) {
        Log.d(TAG, "onRemotePeerAudioToggle");
    }

    @Override
    public void onRemotePeerVideoToggle(String s, boolean b) {
        Log.d(TAG, "onRemotePeerVideoToggle");
    }

    @Override
    public void onRemotePeerMediaReceive(String s, GLSurfaceView glSurfaceView) {
        Log.d(TAG, "onRemotePeerVideoToggle");
    }

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        // If there is an existing peer, prevent new remotePeer from joining call.
        if (this.remotePeerId != null) {
            Toast.makeText(parentActivity, "Rejected third peer from joining conversation",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        //if first remote peer to join room, keep track of user and update text-view to display details
        this.remotePeerId = remotePeerId;
        peerJoined = true;
        if (userData instanceof String) {
            remotePeerName = (String) userData;
        }
        Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME, MY_USER_NAME);
    }

    @Override
    public void onRemotePeerUserDataReceive(String s, Object o) {
        Log.d(TAG, "onRemotePeerUserDataReceive");
    }

    @Override
    public void onOpenDataConnection(String s) {
        Log.d(TAG, "onOpenDataConnection");

    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        Toast.makeText(parentActivity, "Your peer has left the room", Toast.LENGTH_SHORT).show();
        //reset peerId
        peerJoined = false;
        this.remotePeerId = null;
        remotePeerName = null;
        //update textview to show room status
        Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME, MY_USER_NAME);
    }
}
