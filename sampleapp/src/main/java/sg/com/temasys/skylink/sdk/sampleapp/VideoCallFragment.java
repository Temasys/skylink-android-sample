package sg.com.temasys.skylink.sdk.sampleapp;

/**
 * Created by lavanyasudharsanam on 20/1/15.
 */

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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

import java.util.Date;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.ErrorCodes;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;

/**
 * This class is used to demonstrate the VideoCall between two clients in WebRTC
 */
public class VideoCallFragment extends Fragment
        implements LifeCycleListener, MediaListener, RemotePeerListener {
    public static final String ROOM_NAME = Constants.ROOM_NAME_VIDEO;
    public static final String MY_USER_NAME = "videoCallUser";
    //set height width for self-video when in call
    public static final int WIDTH = 350;
    public static final int HEIGHT = 350;
    private static final String TAG = VideoCallFragment.class.getCanonicalName();
    private static final String ARG_SECTION_NUMBER = "section_number";
    // Constants for configuration change
    private static final String BUNDLE_IS_CONNECTED = "isConnected";
    private static final String BUNDLE_PEER_ID = "peerId";
    private static final String BUNDLE_AUDIO_MUTED = "audioMuted";
    private static final String BUNDLE_VIDEO_MUTED = "videoMuted";
    private static GLSurfaceView videoViewSelf;
    private static GLSurfaceView videoViewRemote;
    private static SkylinkConnection skylinkConnection;
    private LinearLayout linearLayout;
    private Button toggleAudioButton;
    private Button toggleVideoButton;
    private Button btnEnterRoom;
    private EditText etRoomName;
    private String roomName;
    private String peerId;
    private boolean audioMuted;
    private boolean videoMuted;
    private boolean connected;
    private boolean orientationChange;
    private Activity parentActivity;
    private AudioRouter audioRouter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Initialize views
        View rootView = inflater.inflate(R.layout.fragment_video_call, container, false);
        linearLayout = (LinearLayout) rootView.findViewById(R.id.ll_video_call);
        btnEnterRoom = (Button) rootView.findViewById(R.id.btn_enter_room);
        etRoomName = (EditText) rootView.findViewById(R.id.et_room_name);

        toggleAudioButton = (Button) rootView.findViewById(R.id.toggle_audio);
        toggleVideoButton = (Button) rootView.findViewById(R.id.toggle_video);

        // Check if it was an orientation change
        if (savedInstanceState != null) {
            connected = savedInstanceState.getBoolean(BUNDLE_IS_CONNECTED);
            // Set the appropriate UI if already connected.
            if (connected) {
                // Set listeners to receive callbacks when events are triggered
                setListeners();
                peerId = savedInstanceState.getString(BUNDLE_PEER_ID, null);
                audioMuted = savedInstanceState.getBoolean(BUNDLE_AUDIO_MUTED);
                videoMuted = savedInstanceState.getBoolean(BUNDLE_VIDEO_MUTED);
                // Set the appropriate UI if already connected.
                onConnectUIChange();
                initializeAudioRouter();
                addSelfView(videoViewSelf);
                addRemoteView(peerId, videoViewRemote);
            }
        }

        // Set UI elements
        setAudioBtnLabel(false);
        setVideoBtnLabel(false);

        btnEnterRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roomName = etRoomName.getText().toString();
                String toast = "";
                // If roomName is not set through the UI, get the default roomName from Constants
                if (roomName.isEmpty()) {
                    roomName = ROOM_NAME;
                    etRoomName.setText(roomName);
                    toast = "No room name provided, entering default video room \"" + roomName
                            + "\".";
                } else {
                    toast = "Entering video room \"" + roomName + "\".";
                }
                Toast.makeText(parentActivity, toast, Toast.LENGTH_SHORT).show();

                btnEnterRoom.setVisibility(View.GONE);
                etRoomName.setEnabled(false);

                String appKey = getString(R.string.app_key);
                String appSecret = getString(R.string.app_secret);

                // Initialize the skylink connection
                initializeSkylinkConnection();

                // Initialize the audio router
                initializeAudioRouter();

                // Obtaining the Skylink connection string done locally
                // In a production environment the connection string should be given
                // by an entity external to the App, such as an App server that holds the Skylink
                // App secret
                // In order to avoid keeping the App secret within the application
                String skylinkConnectionString = Utils.
                        getSkylinkConnectionString(roomName,
                                appKey,
                                appSecret, new Date(),
                                SkylinkConnection
                                        .DEFAULT_DURATION);

                skylinkConnection.connectToRoom(skylinkConnectionString, MY_USER_NAME);
                connected = true;

                // Use the Audio router to switch between headphone and headset
                audioRouter.startAudioRouting(parentActivity.getApplicationContext());
            }
        });

        toggleAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If audio is enabled, mute audio and if audio is enabled, mute it
                audioMuted = !audioMuted;
                skylinkConnection.muteLocalAudio(audioMuted);

                // Set UI and Toast.
                setAudioBtnLabel(true);
            }
        });

        toggleVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If video is enabled, mute video and if video is enabled, mute it
                videoMuted = !videoMuted;
                skylinkConnection.muteLocalVideo(videoMuted);

                // Set UI and Toast.
                setVideoBtnLabel(true);
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Allow volume to be controlled using volume keys
        parentActivity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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
        outState.putString(BUNDLE_PEER_ID, peerId);
        outState.putBoolean(BUNDLE_AUDIO_MUTED, audioMuted);
        outState.putBoolean(BUNDLE_VIDEO_MUTED, videoMuted);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Remove all views from layouts.
        Utils.removeViewFromParent(videoViewSelf);
        Utils.removeViewFromParent(videoViewRemote);
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
        //AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY,
        // AUDIO_AND_VIDEO;
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        config.setMirrorLocalView(true);
        // Allow only 1 remote Peer to join.
        config.setMaxPeers(1);
        return config;
    }

    private void initializeAudioRouter() {
        if (audioRouter == null) {
            audioRouter = AudioRouter.getInstance();
            audioRouter.init(((AudioManager) parentActivity.
                    getSystemService(
                            android.content
                                    .Context
                                    .AUDIO_SERVICE)));
        }
    }

    private void initializeSkylinkConnection() {
        if (skylinkConnection == null) {
            skylinkConnection = SkylinkConnection.getInstance();
            //the app_key and app_secret is obtained from the temasys developer console.
            skylinkConnection.init(getString(R.string.app_key),
                    getSkylinkConfig(), this.parentActivity.getApplicationContext());
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
        btnEnterRoom.setVisibility(View.GONE);
        etRoomName.setEnabled(false);
        toggleAudioButton.setVisibility(View.VISIBLE);
        toggleVideoButton.setVisibility(View.VISIBLE);
    }

    /**
     * Add or update our self VideoView into the app.
     *
     * @param videoView
     */
    private void addSelfView(GLSurfaceView videoView) {
        if (videoView != null) {
            // If previous self video exists,
            // Set new video to size of previous self video
            // And remove old self video.
            View self = linearLayout.findViewWithTag("self");
            if (self != null) {
                videoView.setLayoutParams(self.getLayoutParams());
                // Remove the old self video.
                linearLayout.removeView(self);
            }

            // Tag new video as self and add onClickListener.
            videoView.setTag("self");
            // Allow self view to switch between different cameras (if any) when tapped.
            videoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    skylinkConnection.switchCamera();
                }
            });

            // If peer video exists, remove it first.
            View peer = linearLayout.findViewWithTag("peer");
            if (peer != null) {
                linearLayout.removeView(peer);
            }

            // Show new video on screen
            // Remove video from previous parent, if any.
            Utils.removeViewFromParent(videoView);

            // And new self video.
            linearLayout.addView(videoView);

            // Return the peer video, if it was there before.
            if (peer != null) {
                linearLayout.addView(peer);
            }
        }
    }

    /**
     * Add or update remote Peer's VideoView into the app.
     *
     * @param remotePeerId
     * @param videoView
     */
    private void addRemoteView(String remotePeerId, GLSurfaceView videoView) {
        if (videoView == null) {
            return;
        }

        // Resize self view
        View self = linearLayout.findViewWithTag("self");

        self.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, HEIGHT));
        linearLayout.removeView(self);
        linearLayout.addView(self);

        // Remove previous peer video if it exists
        View viewToRemove = linearLayout.findViewWithTag("peer");
        if (viewToRemove != null) {
            linearLayout.removeView(viewToRemove);
        }

        // Add new peer video
        videoView.setTag("peer");
        // Remove view from previous parent, if any.
        Utils.removeViewFromParent(videoView);
        // Add view to parent
        linearLayout.addView(videoView);

        this.peerId = remotePeerId;
    }

    /**
     * Set the mute audio button label according to the current state of audio.
     *
     * @param doToast If true, Toast about setting audio to current state.
     */
    private void setAudioBtnLabel(boolean doToast) {
        if (audioMuted) {
            toggleAudioButton.setText(getString(R.string.enable_audio));
            if (doToast) {
                Toast.makeText(parentActivity, getString(R.string.muted_audio),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            toggleAudioButton.setText(getString(R.string.mute_audio));
            if (doToast) {
                Toast.makeText(parentActivity, getString(R.string.enabled_audio),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Set the mute video button label according to the current state of video.
     *
     * @param doToast If true, Toast about setting video to current state.
     */
    private void setVideoBtnLabel(boolean doToast) {
        if (videoMuted) {
            toggleVideoButton.setText(getString(R.string.enable_video));
            if (doToast) {
                Toast.makeText(parentActivity, getString(R.string.muted_video),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            toggleVideoButton.setText(getString(R.string.mute_video));
            if (doToast) {
                Toast.makeText(parentActivity, getString(R.string.enabled_video),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's
     * lifecycle
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
            onConnectUIChange();
            Toast.makeText(parentActivity, "Connected to room " + roomName + " as " + MY_USER_NAME,
                    Toast.LENGTH_SHORT).show();
        } else {
            connected = false;
            Log.d(TAG, "Skylink failed to connect!");
            Toast.makeText(parentActivity, "Skylink failed to connect!\nReason : "
                    + message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        skylinkConnection = null;
        String log = message;
        if (errorCode == ErrorCodes.DISCONNECT_FROM_ROOM) {
            log = "[onDisconnect] We have successfully disconnected from the room. Server message: "
                    + message;
        }
        Toast.makeText(parentActivity, "[onDisconnect] " + log, Toast.LENGTH_LONG).show();
        Log.d(TAG, log);
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        Toast.makeText(parentActivity, "Peer " + remotePeerId +
                " has changed Room locked status to " + lockStatus, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReceiveLog(String message) {
        Log.d(TAG, message + " on receive log");
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Log.d(TAG, message + "warning");
        Toast.makeText(parentActivity, "Warning is errorCode" + errorCode, Toast.LENGTH_SHORT)
                .show();
    }

    /**
     * Media Listeners Callbacks - triggered when receiving changes to Media Stream from the
     * remote peer
     */

    /**
     * Triggered after the user's local media is captured.
     *
     * @param videoView
     */
    @Override
    public void onLocalMediaCapture(GLSurfaceView videoView) {
        videoViewSelf = videoView;
        addSelfView(videoView);
    }

    @Override
    public void onVideoSizeChange(String peerId, Point size) {
        Log.d(TAG, "PeerId: " + peerId + " got size " + size.toString());
    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, GLSurfaceView videoView) {
        videoViewRemote = videoView;
        addRemoteView(remotePeerId, videoView);
    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        String message = null;
        if (isMuted) {
            message = "Your peer muted their audio";
        } else {
            message = "Your peer unmuted their audio";
        }

        Toast.makeText(parentActivity, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemotePeerVideoToggle(String peerId, boolean isMuted) {
        String message = null;
        if (isMuted) {
            message = "Your peer muted video";
        } else {
            message = "Your peer unmuted their video";
        }

        Toast.makeText(parentActivity, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        Toast.makeText(parentActivity, "Your peer has just connected", Toast.LENGTH_SHORT).show();
        UserInfo remotePeerUserInfo = skylinkConnection.getUserInfo(remotePeerId);
        Log.d(TAG, "isAudioStereo " + remotePeerUserInfo.isAudioStereo());
        Log.d(TAG, "video height " + remotePeerUserInfo.getVideoHeight());
        Log.d(TAG, "video width " + remotePeerUserInfo.getVideoHeight());
        Log.d(TAG, "video frameRate " + remotePeerUserInfo.getVideoFps());
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        Toast.makeText(parentActivity, "Your peer has left the room", Toast.LENGTH_SHORT).show();
        if (remotePeerId != null && remotePeerId.equals(this.peerId)) {
            this.peerId = null;
            View peerView = linearLayout.findViewWithTag("peer");
            linearLayout.removeView(peerView);
            videoViewRemote = null;

            // Resize self view to better make use of screen.
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            videoViewSelf.setLayoutParams(params);
            addSelfView(videoViewSelf);
        }
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        Log.d(TAG, "onRemotePeerUserDataReceive " + remotePeerId);
    }

    @Override
    public void onOpenDataConnection(String peerId) {
        Log.d(TAG, "onOpenDataConnection");
    }
}
