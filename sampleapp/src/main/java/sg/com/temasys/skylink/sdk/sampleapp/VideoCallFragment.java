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
import android.text.TextUtils;
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
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;

/**
 * This class is used to demonstrate the VideoCall between two clients in WebRTC
 */
public class VideoCallFragment extends Fragment implements LifeCycleListener, MediaListener, RemotePeerListener {
    private static final String TAG = VideoCallFragment.class.getCanonicalName();
    public static final String ROOM_NAME = Constants.ROOM_NAME_VIDEO;
    private static String BUNDLE_IS_CONNECTED = "isConnected";
    private static final String BUNDLE_PEER_ID = "peerId";
    public static final String MY_USER_NAME = "videoCallUser";
    private static final String ARG_SECTION_NUMBER = "section_number";

    private static GLSurfaceView videoViewSelf;
    private static GLSurfaceView videoViewRemote;

    //set height width for self-video when in call
    public static final int WIDTH = 350;
    public static final int HEIGHT = 350;
    private LinearLayout linearLayout;
    private Button toggleAudioButton;
    private Button toggleVideoButton;
    private Button btnEnterRoom;
    private EditText etRoomName;
    private static SkylinkConnection skylinkConnection;
    private String roomName;
    private String peerId;
    private ViewGroup.LayoutParams selfLayoutParams;
    private boolean audioMuted;
    private boolean videoMuted;
    private boolean connected;
    private AudioRouter audioRouter;
    private boolean orientationChange;

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
        if(savedInstanceState != null){
            connected = savedInstanceState.getBoolean(BUNDLE_IS_CONNECTED);
            // Set the appropriate UI if already connected.
            if(connected) {
                onConnectUIChange();
                peerId = savedInstanceState.getString(BUNDLE_PEER_ID, null);
                addSelfView(videoViewSelf);
                addRemoteView(peerId, videoViewRemote);
            }
        }

        btnEnterRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roomName = etRoomName.getText().toString();
                String toast = "";
                if (roomName.isEmpty()) {
                    roomName = ROOM_NAME;
                    toast = "No room name provided, entering default video room \"" + roomName
                            + "\".";
                } else {
                    toast = "Entering video room \"" + roomName + "\".";
                }
                Toast.makeText(getActivity(), toast, Toast.LENGTH_SHORT).show();

                btnEnterRoom.setVisibility(View.GONE);

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
                        getSkylinkConnectionString(roomName, appKey,
                                appSecret, new Date(), SkylinkConnection.DEFAULT_DURATION);

                skylinkConnection.connectToRoom(skylinkConnectionString,
                        MY_USER_NAME);

                // Use the Audio router to switch between headphone and headset
                audioRouter.startAudioRouting(getActivity().getApplicationContext());
            }
        });

        toggleAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // If audio is enabled, mute audio and if audio is enabled, mute it
                audioMuted = !audioMuted;

                if (audioMuted) {
                    Toast.makeText(getActivity(), getString(R.string.muted_audio),
                            Toast.LENGTH_SHORT).show();
                    toggleAudioButton.setText(getString(R.string.enable_audio));
                } else {
                    Toast.makeText(getActivity(), getString(R.string.enabled_audio),
                            Toast.LENGTH_SHORT).show();
                    toggleAudioButton.setText(getString(R.string.mute_audio));
                }

                skylinkConnection.muteLocalAudio(audioMuted);
            }
        });

        toggleVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // If video is enabled, mute video and if video is enabled, mute it
                videoMuted = !videoMuted;

                if (videoMuted) {
                    Toast.makeText(getActivity(), getString(R.string.muted_video),
                            Toast.LENGTH_SHORT).show();
                    toggleVideoButton.setText(getString(R.string.enable_video));
                } else {
                    Toast.makeText(getActivity(), getString(R.string.enabled_video),
                            Toast.LENGTH_SHORT).show();
                    toggleVideoButton.setText(getString(R.string.mute_video));
                }

                skylinkConnection.muteLocalVideo(videoMuted);
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

    private void initializeAudioRouter() {
        if (audioRouter == null) {
            audioRouter = AudioRouter.getInstance();
            audioRouter.init(((AudioManager) getActivity().
                    getSystemService(android.content.Context.AUDIO_SERVICE)));
        }
    }

    private void initializeSkylinkConnection() {
        if (skylinkConnection == null) {
            skylinkConnection = SkylinkConnection.getInstance();
            //the app_key and app_secret is obtained from the temasys developer console.
            skylinkConnection.init(getString(R.string.app_key),
                    getSkylinkConfig(), this.getActivity().getApplicationContext());
            //set listeners to receive callbacks when events are triggered
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setMediaListener(this);
            skylinkConnection.setRemotePeerListener(this);
        }
    }

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        //AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY, AUDIO_AND_VIDEO;
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        config.setMirrorLocalView(true);
        return config;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Note that orientation change is happening.
        orientationChange = true;
        // Save states for fragment restart
        outState.putBoolean(BUNDLE_IS_CONNECTED, connected);
        outState.putString(BUNDLE_PEER_ID, peerId);
    }

    @Override
    public void onDetach() {
        //close the connection when the fragment is detached, so the streams are not open.
        super.onDetach();
        // Disconnect from room only if already connected and not changing orientation.
        if (!orientationChange && skylinkConnection != null && connected) {
            skylinkConnection.disconnectFromRoom();
            skylinkConnection.setLifeCycleListener(null);
            skylinkConnection.setMediaListener(null);
            skylinkConnection.setRemotePeerListener(null);
            connected = false;
            audioRouter.stopAudioRouting(getActivity().getApplicationContext());
        }
    }

    /**
     * Change certain UI elements once connected to room.
     */
    private void onConnectUIChange() {
        etRoomName.setEnabled(false);
        btnEnterRoom.setVisibility(View.GONE);
        toggleAudioButton.setVisibility(View.VISIBLE);
        toggleVideoButton.setVisibility(View.VISIBLE);
    }

    /**
     * Add or update our self VideoView into the app.
     * @param videoView
     */
    private void addSelfView(GLSurfaceView videoView) {
        if (videoView != null) {
            View self = linearLayout.findViewWithTag("self");
            videoView.setTag("self");
            // Allow self view to switch between different cameras (if any) when tapped.
            videoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    skylinkConnection.switchCamera();
                }
            });

            if (self == null) {
                //show media on screen
                // Remove view from previous parent, if any.
                LinearLayout parentFragmentOld = (LinearLayout) videoView.getParent();
                if(parentFragmentOld != null ) {
                    parentFragmentOld.removeView(videoView);
                }
                // Add view to parent
                linearLayout.addView(videoView);
            } else {
                videoView.setLayoutParams(self.getLayoutParams());

                // If peer video exists, remove it first.
                View peer = linearLayout.findViewWithTag("peer");
                if (peer != null) {
                    linearLayout.removeView(peer);
                }

                // Remove the old self video and add the new one.
                linearLayout.removeView(self);
                linearLayout.addView(videoView);

                // Return the peer video, if it was there before.
                if (peer != null) {
                    linearLayout.addView(peer);
                }
            }

        }
    }

    /**
     * Add or update remote Peer's VideoView into the app.
     * @param remotePeerId
     * @param videoView
     */
    private void addRemoteView(String remotePeerId, GLSurfaceView videoView) {
        if (videoView == null) {
            return;
        }

        // Resize self view
        View self = linearLayout.findViewWithTag("self");
        if (this.selfLayoutParams != null) {
            // Record the original size of the layout
            this.selfLayoutParams = self.getLayoutParams();
        }

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
        LinearLayout parentFragmentOld = (LinearLayout) videoView.getParent();
        if(parentFragmentOld != null ) {
            parentFragmentOld.removeView(videoView);
        }
        // Add view to parent
        linearLayout.addView(videoView);

        this.peerId = remotePeerId;
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's lifecycle
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
            connected = true;
            onConnectUIChange();
            Toast.makeText(getActivity(), "Connected to room " + roomName + " as " + MY_USER_NAME, Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Skylink Failed " + message);
            Toast.makeText(getActivity(), "Skylink Connection Failed\nReason : "
                    + message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        Toast.makeText(getActivity(), "Peer " + remotePeerId +
                " has changed Room locked status to " + lockStatus, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Log.d(TAG, message + "warning");
        Toast.makeText(getActivity(), "Warning is errorCode" + errorCode, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        Log.d(TAG, message + " disconnected");
        Toast.makeText(getActivity(), "onDisconnect " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onReceiveLog(String message) {
        Log.d(TAG, message + " on receive log");
    }

    /**
     * Media Listeners Callbacks - triggered when receiving changes to Media Stream from the remote peer
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
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        String message = null;
        if (isMuted) {
            message = "Your peer muted their audio";
        } else {
            message = "Your peer unmuted their audio";
        }

        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemotePeerVideoToggle(String peerId, boolean isMuted) {
        String message = null;
        if (isMuted)
            message = "Your peer muted video";
        else
            message = "Your peer unmuted their video";

        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        Toast.makeText(getActivity(), "Your peer has just connected", Toast.LENGTH_SHORT).show();
        UserInfo remotePeerUserInfo = skylinkConnection.getUserInfo(remotePeerId);
        Log.d(TAG, "isAudioStereo " + remotePeerUserInfo.isAudioStereo());
        Log.d(TAG, "video height " + remotePeerUserInfo.getVideoHeight());
        Log.d(TAG, "video width " + remotePeerUserInfo.getVideoHeight());
        Log.d(TAG, "video frameRate " + remotePeerUserInfo.getVideoFps());
    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, GLSurfaceView videoView) {
        if (!TextUtils.isEmpty(this.peerId) && !remotePeerId.equals(this.peerId)) {
            Toast.makeText(getActivity(), " You are already in connection with two peers",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        videoViewRemote = videoView;
        addRemoteView(remotePeerId, videoView);
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        Toast.makeText(getActivity(), "Your peer has left the room", Toast.LENGTH_SHORT).show();
        if (remotePeerId != null && remotePeerId.equals(this.peerId)) {
            this.peerId = null;
            View peerView = linearLayout.findViewWithTag("peer");
            linearLayout.removeView(peerView);
            videoViewRemote = null;

            // Resize self view to original size
            if (this.selfLayoutParams != null) {
                View self = linearLayout.findViewWithTag("self");
                self.setLayoutParams(selfLayoutParams);
            }
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
