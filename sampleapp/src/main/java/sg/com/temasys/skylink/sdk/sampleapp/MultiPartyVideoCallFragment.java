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
import java.util.HashMap;
import java.util.Map;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;

/**
 * Created by janidu on 3/3/15.
 */
public class MultiPartyVideoCallFragment extends Fragment implements
        MediaListener, RemotePeerListener, LifeCycleListener {

    private static final String TAG = MultiPartyVideoCallFragment.class.getName();
    private static final String ROOM_NAME = "Hangout";
    private static final String MY_USER_NAME = "videoCallUser";
    public static final String KEY_SELF = "self";
    private static final String ARG_SECTION_NUMBER = "section_number";

    private Map<String, GLSurfaceView> surfaceViews;
    private SkylinkConnection skylinkConnection;
    private FrameLayout[] peerLayouts;
    private boolean connected;
    private AudioRouter audioRouter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_video_multiparty, container, false);

        FrameLayout selfLayout = (FrameLayout) rootView.findViewById(R.id.self_video);
        FrameLayout peer1Layout = (FrameLayout) rootView.findViewById(R.id.peer_1);
        FrameLayout peer2Layout = (FrameLayout) rootView.findViewById(R.id.peer_2);
        FrameLayout peer3Layout = (FrameLayout) rootView.findViewById(R.id.peer_3);

        peerLayouts = new FrameLayout[]{selfLayout, peer1Layout, peer2Layout, peer3Layout};
        surfaceViews = new HashMap<String, GLSurfaceView>();
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String apiKey = getString(R.string.app_key);
        String apiSecret = getString(R.string.app_secret);

        // Initialize the skylink connection
        initializeSkylinkConnection();

        // Initialize the audio router
        initializeAudioRouter();

        // Obtaining the Skylink connection string done locally
        // In a production environment the connection string should be given
        // by an entity external to the App, such as an App server that holds the Skylink API secret
        // In order to avoid keeping the API secret within the application
        String skylinkConnectionString = Utils.
                getSkylinkConnectionString(ROOM_NAME, apiKey,
                        apiSecret, new Date(), SkylinkConnection.DEFAULT_DURATION);

        Log.d(TAG, "Connection String" + skylinkConnectionString);
        skylinkConnection.connectToRoom(skylinkConnectionString,
                MY_USER_NAME);
        connected = true;

        // Use the Audio router to switch between headphone and headset
        audioRouter.startAudioRouting(getActivity().getApplicationContext());

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

    @Override
    public void onDetach() {
        //close the connection when the fragment is detached, so the streams are not open.
        if (skylinkConnection != null && connected) {
            skylinkConnection.disconnectFromRoom();
            skylinkConnection.setMediaListener(null);
            skylinkConnection.setRemotePeerListener(null);
            skylinkConnection.setLifeCycleListener(null);
            connected = false;
            audioRouter.stopAudioRouting(getActivity().getApplicationContext());
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Manage the lifecycle of the surface view
        if (this.surfaceViews != null && !this.surfaceViews.isEmpty()) {
            for (GLSurfaceView peerSurfaceView : this.surfaceViews.values()) {
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
        if (this.surfaceViews != null && !this.surfaceViews.isEmpty()) {
            for (GLSurfaceView peerSurfaceView : this.surfaceViews.values()) {
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
    }

    private void initializeSkylinkConnection() {
        if (skylinkConnection == null) {
            skylinkConnection = SkylinkConnection.getInstance();
            //the app_key and app_secret is obtained from the temasys developer console.
            skylinkConnection.init(getString(R.string.app_key),
                    getSkylinkConfig(), this.getActivity().getApplicationContext());
            //set listeners to receive callbacks when events are triggered
            skylinkConnection.setMediaListener(this);
            skylinkConnection.setRemotePeerListener(this);
            skylinkConnection.setLifeCycleListener(this);
        }
    }

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        // AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY, AUDIO_AND_VIDEO;
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        return config;
    }

    @Override
    public void onLocalMediaCapture(GLSurfaceView videoView, Point size) {
        if (!surfaceViews.containsKey(KEY_SELF)) {
            // Add self view if its not already added
            peerLayouts[0].addView(videoView);
            surfaceViews.put(KEY_SELF, videoView);
        }
    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, GLSurfaceView videoView, Point size) {
        addRemotePeerViews(remotePeerId, videoView);
    }

    private void addRemotePeerViews(String remotePeerId, GLSurfaceView videoView) {
        if (!surfaceViews.containsKey(remotePeerId)) {
            // Add peer view if its not already added
            // Find the frame layout that's empty
            for (FrameLayout peerFrameLayout : peerLayouts) {
                if (peerFrameLayout.getChildCount() == 0) {
                    peerFrameLayout.addView(videoView);
                    surfaceViews.put(remotePeerId, videoView);
                    break;
                }
            }
        }
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        removePeerViews(remotePeerId);
    }

    private void removePeerViews(String remotePeerId) {
        if (surfaceViews.containsKey(remotePeerId)) {
            GLSurfaceView viewToRemove = surfaceViews.get(remotePeerId);
            // Remove peer view from layout
            for (FrameLayout peerFrameLayout : peerLayouts) {
                peerFrameLayout.removeView(viewToRemove);
            }
            surfaceViews.remove(remotePeerId);
        }
    }

    @Override
    public void onVideoSizeChange(GLSurfaceView videoView, Point size) {
        Log.d(TAG, "onVideoSizeChange");
    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        Log.d(TAG, "onRemotePeerAudioToggle");
    }

    @Override
    public void onRemotePeerVideoToggle(String remotePeerId, boolean isMuted) {
        Log.d(TAG, "onRemotePeerAudioToggle");
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

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        if (isSuccessful) {
            Toast.makeText(getActivity(), String.format(getString(R.string.data_transfer_waiting),
                    ROOM_NAME), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), "Skylink Connection Failed\nReason :" +
                    " " + message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWarning(int errorCode, String message) {

    }

    @Override
    public void onDisconnect(int errorCode, String message) {

    }

    @Override
    public void onReceiveLog(String message) {

    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {

    }
}