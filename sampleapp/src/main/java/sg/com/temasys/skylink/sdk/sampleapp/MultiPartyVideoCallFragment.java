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
import android.widget.FrameLayout;
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.ErrorCodes;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;

/**
 * Created by janidu on 3/3/15.
 */
public class MultiPartyVideoCallFragment extends Fragment implements
        MediaListener, RemotePeerListener, LifeCycleListener {

    private static final String TAG = MultiPartyVideoCallFragment.class.getName();
    private static final String ROOM_NAME = Constants.ROOM_NAME_MULTI;
    private static final String MY_USER_NAME = "videoCallUser";
    public static final String KEY_SELF = "self";
    private static final String ARG_SECTION_NUMBER = "section_number";

    // Constants for configuration change
    private static final String BUNDLE_IS_CONNECTED = "isConnected";

    private static GLSurfaceView videoViewSelf;
    /**
     * List of remote VideoViews
     */
    private static Map<String, GLSurfaceView> videoViewRemoteMap;
    /**
     * List of Framelayouts for VideoViews
     */
    private static SkylinkConnection skylinkConnection;
    private FrameLayout[] videoViewLayouts;
    private boolean connected;
    private boolean orientationChange;
    private Activity parentActivity;
    private AudioRouter audioRouter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_video_multiparty, container, false);

        FrameLayout selfLayout = (FrameLayout) rootView.findViewById(R.id.self_video);
        FrameLayout peer1Layout = (FrameLayout) rootView.findViewById(R.id.peer_1);
        FrameLayout peer2Layout = (FrameLayout) rootView.findViewById(R.id.peer_2);
        FrameLayout peer3Layout = (FrameLayout) rootView.findViewById(R.id.peer_3);

        videoViewLayouts = new FrameLayout[]{selfLayout, peer1Layout, peer2Layout, peer3Layout};

        String appKey = getString(R.string.app_key);
        String appSecret = getString(R.string.app_secret);

        // Initialize the audio router
        initializeAudioRouter();

        // Check if it was an orientation change
        if (savedInstanceState != null) {
            connected = savedInstanceState.getBoolean(BUNDLE_IS_CONNECTED);
            // Set the appropriate UI if already connected.
            if (connected) {
                // Set listeners to receive callbacks when events are triggered
                setListeners();
                // Add all existing VideoViews to UI
                addViews();
            }
        } else {
            videoViewRemoteMap = new ConcurrentHashMap<String, GLSurfaceView>();
            // Initialize the skylink connection
            initializeSkylinkConnection();

            // Obtaining the Skylink connection string done locally
            // In a production environment the connection string should be given
            // by an entity external to the App, such as an App server that holds the Skylink App secret
            // In order to avoid keeping the App secret within the application
            String skylinkConnectionString = Utils.
                    getSkylinkConnectionString(ROOM_NAME, appKey,
                            appSecret, new Date(), SkylinkConnection.DEFAULT_DURATION);

            Log.d(TAG, "Connection String" + skylinkConnectionString);
            skylinkConnection.connectToRoom(skylinkConnectionString,
                    MY_USER_NAME);

            // Use the Audio router to switch between headphone and headset
            audioRouter.startAudioRouting(getActivity().getApplicationContext());
        }

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Allow volume to be controlled using volume keys
        getActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Manage the lifecycle of the surface view
        if (this.videoViewRemoteMap != null && !this.videoViewRemoteMap.isEmpty()) {
            for (GLSurfaceView peerSurfaceView : this.videoViewRemoteMap.values()) {
                if (peerSurfaceView != null) {
                    peerSurfaceView.onResume();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Manage the lifecycle of the surface view
        if (this.videoViewRemoteMap != null && !this.videoViewRemoteMap.isEmpty()) {
            for (GLSurfaceView peerSurfaceView : this.videoViewRemoteMap.values()) {
                if (peerSurfaceView != null) {
                    peerSurfaceView.onPause();
                }
            }
        }
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Remove all views from layouts.
        emptyLayout();
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
        // AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY, AUDIO_AND_VIDEO;
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        config.setMirrorLocalView(true);
        return config;
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

    private void addSelfView(GLSurfaceView videoView) {
        if (videoView == null) return;

        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(videoView);

        // Remove self view if its already added
        videoViewLayouts[0].removeAllViews();
        videoViewLayouts[0].addView(videoView);
        // videoViewRemoteMap.put(KEY_SELF, videoView);
        // Allow self view to switch between different cameras (if any) when tapped.
        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skylinkConnection.switchCamera();
            }
        });
    }

    /**
     * Add or update remote Peer's VideoView into the app.
     *
     * @param remotePeerId
     * @param videoView
     */
    private void addRemoteView(String remotePeerId, GLSurfaceView videoView) {
        if (videoView == null) return;

        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(videoView);

        // Remove any existing VideoView of this remote Peer.
        removePeerView(remotePeerId);
        // Add peer view into 1st remote Peer frame layout that's empty (if any)
        for (FrameLayout peerFrameLayout : videoViewLayouts) {
            if (peerFrameLayout.getChildCount() == 0) {
                peerFrameLayout.addView(videoView);
                videoViewRemoteMap.put(remotePeerId, videoView);
                break;
            }
        }
    }

    /**
     * Add all videoViews onto layouts.
     */
    private void addViews() {
        // Add self VideoView
        addSelfView(videoViewSelf);

        // Add remote VideoView(s)
        for (Map.Entry<String, GLSurfaceView> entry : videoViewRemoteMap.entrySet()) {
            String peerId = entry.getKey();
            GLSurfaceView videoView = entry.getValue();
            addRemoteView(peerId, videoView);
        }
    }

    /**
     * Remove all videoViews from layouts.
     */
    private void emptyLayout() {
        for (Map.Entry<String, GLSurfaceView> entry : videoViewRemoteMap.entrySet()) {
            GLSurfaceView videoView = entry.getValue();
            Utils.removeViewFromParent(videoView);
        }
    }

    /**
     * Remove any existing VideoView of a remote Peer and remove remote Peer from record.
     *
     * @param remotePeerId
     */
    private void removePeerView(String remotePeerId) {
        if (videoViewRemoteMap != null && videoViewRemoteMap.containsKey(remotePeerId)) {
            GLSurfaceView viewToRemove = videoViewRemoteMap.get(remotePeerId);
            // Remove Peer view from layout.
            for (FrameLayout peerFrameLayout : videoViewLayouts) {
                peerFrameLayout.removeView(viewToRemove);
            }
            // Remove Peer from record.
            videoViewRemoteMap.remove(remotePeerId);
        }
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's lifecycle
     */

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        if (isSuccessful) {
            connected = true;
            Toast.makeText(getActivity(), String.format(getString(R.string.data_transfer_waiting),
                    ROOM_NAME), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), "Skylink Connection Failed\nReason :" +
                    " " + message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        skylinkConnection = null;
        String log = message;
        if (errorCode == ErrorCodes.DISCONNECT_FROM_ROOM) {
            log = "[onDisconnect] We have successfully disconnected from the room. Server message: "
                    + message;
            Log.d(TAG, log);
        }
        Toast.makeText(parentActivity, log, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {

    }

    @Override
    public void onReceiveLog(String message) {

    }

    @Override
    public void onWarning(int errorCode, String message) {

    }

    /**
     * Media Listeners Callbacks - triggered when receiving changes to Media Stream from the remote peer
     */

    @Override
    public void onLocalMediaCapture(GLSurfaceView videoView) {
        videoViewSelf = videoView;
        addSelfView(videoView);
    }

    @Override
    public void onVideoSizeChange(String peerId, Point size) {
        Log.d(TAG, "[onVideoSizeChange] Peer:" + peerId + ", size:" + size.x + "," + size.y + ".");
    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, GLSurfaceView videoView) {
        addRemoteView(remotePeerId, videoView);
    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        Log.d(TAG, "onRemotePeerAudioToggle");
    }

    @Override
    public void onRemotePeerVideoToggle(String remotePeerId, boolean isMuted) {
        Log.d(TAG, "onRemotePeerAudioToggle");
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        removePeerView(remotePeerId);
    }

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        Log.d(TAG, "onRemotePeerJoin");

    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        Log.d(TAG, "onRemotePeerUserDataReceive");
    }

    @Override
    public void onOpenDataConnection(String remotePeerId) {
        Log.d(TAG, "onOpenDataConnection");
    }

}