package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.webrtc.SurfaceViewRenderer;

import java.util.Date;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;

import static sg.com.temasys.skylink.sdk.rtc.Info.CAM_SWITCH_FRONT;
import static sg.com.temasys.skylink.sdk.rtc.Info.CAM_SWITCH_NO;
import static sg.com.temasys.skylink.sdk.rtc.Info.CAM_SWITCH_NON_FRONT;

/**
 * Created by janidu on 3/3/15.
 */
public class MultiPartyVideoCallFragment extends Fragment implements
        MediaListener, RemotePeerListener,
        LifeCycleListener {

    private static final String TAG = MultiPartyVideoCallFragment.class.getName();
    private static final String ROOM_NAME = Constants.ROOM_NAME_MULTI;
    private static final String MY_USER_NAME = "videoCallUser";
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * List of Framelayouts for VideoViews
     */
    private static SkylinkConnection skylinkConnection;
    // Indicates if camera should be toggled after returning to app.
    // Generally, it should match whether it was toggled when moving away from app.
    // For e.g., if camera was already off, then it would not be toggled when moving away from app,
    // So toggleCamera would be set to false at onPause(), and at onCreateView,
    // it would not be toggled.
    private static boolean toggleCamera;
    private FrameLayout[] videoViewLayouts;
    private Activity parentActivity;
    private Context applicationContext;

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

        // Check if it was an orientation change
        if (savedInstanceState != null) {

            // Toggle camera back to previous state if required.
            if (toggleCamera) {
                if (getVideoView(null) != null) {
                    try {
                        skylinkConnection.toggleCamera();
                        toggleCamera = false;
                    } catch (SkylinkException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }

            // Set again the listeners to receive callbacks when events are triggered
            setListeners();
        } else {
            // Set toggleCamera back to default state.
            toggleCamera = false;
        }

        // Try to connect to room if not yet connected.
        if (!isConnected()) {
            connectToRoom(appKey, appSecret);
        }

        // Set the appropriate UI.
        // Add all existing VideoViews to UI
        addViews();

        return rootView;
    }

    private void connectToRoom(String appKey, String appSecret) {
        // Initialize the skylink connection
        initializeSkylinkConnection();

        // Obtaining the Skylink connection string locally.
        // In a production environment the connection string should be given
        // by an entity external to the App,
        // such as an App server that holds the Skylink App secret.
        // This is to avoid keeping the App secret within the application
        String skylinkConnectionString = Utils.
                getSkylinkConnectionString(ROOM_NAME,
                        appKey,
                        appSecret, new Date(),
                        SkylinkConnection
                                .DEFAULT_DURATION);

        // The skylinkConnectionString should not be logged in production,
        // as it contains potentially sensitive information like the Skylink App key.

        boolean connectFailed;
        connectFailed = !skylinkConnection.connectToRoom(skylinkConnectionString, MY_USER_NAME);
        if (connectFailed) {
            String error = "Unable to connect to Room! Rotate device to try again later.";
            Toast.makeText(parentActivity, error, Toast.LENGTH_LONG).show();
            Log.e(TAG, error);
            return;
        }

        // Initialize and use the Audio router to switch between headphone and headset
        AudioRouter.startAudioRouting(parentActivity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Allow volume to be controlled using volume keys
        parentActivity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Toggle camera back to previous state if required.
        if (toggleCamera) {
            if (getVideoView(null) != null) {
                try {
                    skylinkConnection.toggleCamera();
                    toggleCamera = false;
                } catch (SkylinkException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop local video source only if not changing orientation
        if (!parentActivity.isChangingConfigurations()) {
            if (getVideoView(null) != null) {
                // Stop local video source if it's on.
                try {
                    // Record if need to toggleCamera when resuming.
                    toggleCamera = skylinkConnection.toggleCamera(false);
                } catch (SkylinkException e) {
                    Log.e(TAG, e.getMessage());
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
        applicationContext = parentActivity.getApplicationContext();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Remove all views from layouts.
        emptyLayout();
        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        if (!parentActivity.isChangingConfigurations() && skylinkConnection != null
                && isConnected()) {
            skylinkConnection.disconnectFromRoom();
            AudioRouter.stopAudioRouting(parentActivity.getApplicationContext());
        }
    }

    /***
     * Helper methods
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

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        // AudioVideo config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setMirrorLocalView(true);
        config.setTimeout(Constants.TIME_OUT);

        // To limit audio and/or video bandwidth:
        // config.setMaxAudioBitrate(20);
        // config.setMaxVideoBitrate(256);

        // To enable logs from Skylink SDK (e.g. during debugging),
        // Uncomment the following. Do not enable logs for production apps!
        // config.setEnableLogs(true);

        // Allow only 3 remote Peers to join, due to current UI design.
        config.setMaxPeers(3);

        return config;
    }

    private void initializeSkylinkConnection() {
        skylinkConnection = SkylinkConnection.getInstance();
        //the app_key and app_secret is obtained from the temasys developer console.
        skylinkConnection.init(getString(R.string.app_key),
                getSkylinkConfig(), this.applicationContext);
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

    /**
     * Get the number of remote Peers connected to us.
     *
     * @return
     */
    private int getNumPeers() {
        String[] peerIdList = skylinkConnection.getPeerIdList();
        if (peerIdList == null) {
            return 0;
        }
        // The first Peer is the local Peer.
        return peerIdList.length - 1;
    }

    /**
     * Get peerId of a Peer using SkylinkConnection API.
     *
     * @param index 0 for self Peer, 1 onwards for remote Peer(s).
     * @return Desired peerId or null if not available.
     */
    private String getPeerId(int index) {
        if (skylinkConnection == null) {
            return null;
        }
        String[] peerIdList = skylinkConnection.getPeerIdList();
        // Ensure index does not exceed max index on peerIdList.
        if (index <= peerIdList.length - 1) {
            return peerIdList[index];
        } else {
            return null;
        }
    }

    /**
     * Get Video View of a given Peer using SkylinkConnection API.
     *
     * @param peerId null for self Peer.
     * @return Desired Video View or null if not present.
     */
    private SurfaceViewRenderer getVideoView(String peerId) {
        if (skylinkConnection == null) {
            return null;
        }
        return skylinkConnection.getVideoView(peerId);
    }

    /***
     * UI helper methods
     */

    private void addSelfView(SurfaceViewRenderer videoView) {
        if (videoView == null) {
            return;
        }

        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(videoView);

        // Remove self view if its already added
        videoViewLayouts[0].removeAllViews();
        videoViewLayouts[0].addView(videoView);

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
     */
    private void addRemoteView(String remotePeerId) {
        SurfaceViewRenderer videoView = getVideoView(remotePeerId);
        if (videoView == null) {
            return;
        }

        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(videoView);

        // Remove any existing VideoView of this remote Peer.
        removePeerView(remotePeerId);
        // Add peer view into 1st remote Peer frame layout that's empty (if any)
        for (FrameLayout peerFrameLayout : videoViewLayouts) {
            if (peerFrameLayout.getChildCount() == 0) {
                peerFrameLayout.addView(videoView);
                break;
            }
        }
    }

    /**
     * Add all videoViews onto layouts.
     */
    private void addViews() {
        // Add self VideoView
        addSelfView(getVideoView(null));

        // Add remote VideoView(s)
        int maxIndex = getNumPeers();
        for (int i = 0; i < maxIndex; i++) {
            // Iterate over the remote Peers only (first Peer is self Peer)
            addRemoteView(getPeerId(i + 1));
        }
    }

    /**
     * Remove all videoViews from layouts.
     */
    private void emptyLayout() {
        int maxIndex = getNumPeers();
        for (int i = 0; i < maxIndex; i++) {
            // Iterate over the remote Peers only (first Peer is self Peer)
            Utils.removeViewFromParent(getVideoView(getPeerId(i + 1)));
        }
    }

    /**
     * Remove any existing VideoView of a remote Peer.
     *
     * @param remotePeerId
     */
    private void removePeerView(String remotePeerId) {
        SurfaceViewRenderer viewToRemove = getVideoView(remotePeerId);
        // Remove Peer view from layout.
        for (FrameLayout peerFrameLayout : videoViewLayouts) {
            peerFrameLayout.removeView(viewToRemove);
        }
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's
     * lifecycle
     */

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        if (isSuccessful) {
            Toast.makeText(applicationContext,
                    String.format(getString(R.string.data_transfer_waiting),
                            ROOM_NAME), Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "Skylink failed to connect!");
            Toast.makeText(parentActivity, "Skylink failed to connect!\nReason : "
                    + message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
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
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {

    }

    @Override
    public void onReceiveLog(int infoCode, String message) {
        switch (infoCode) {
            case CAM_SWITCH_FRONT:
            case CAM_SWITCH_NON_FRONT:
                Toast.makeText(parentActivity, message, Toast.LENGTH_SHORT).show();
                break;
            case CAM_SWITCH_NO:
                Toast.makeText(parentActivity, message, Toast.LENGTH_LONG).show();
                break;
            default:
                Log.d(TAG, "Received SDK log: " + message);
                break;
        }
    }

    @Override
    public void onWarning(int errorCode, String message) {

    }

    /**
     * Media Listeners Callbacks - triggered when receiving changes to Media Stream from the
     * remote peer
     */

    @Override
    public void onLocalMediaCapture(SurfaceViewRenderer videoView) {
        if (videoView == null) {
            return;
        }
        addSelfView(videoView);
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
        addRemoteView(remotePeerId);
    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        Log.d(TAG, "onRemotePeerAudioToggle");
    }

    @Override
    public void onRemotePeerVideoToggle(String remotePeerId, boolean isMuted) {
        Log.d(TAG, "onRemotePeerAudioToggle for Peer " + remotePeerId + ".");
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
        Log.d(TAG, "onRemotePeerJoin for Peer " + remotePeerId + ".");

    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        // If Peer has no userData, use an empty string for nick.
        String nick = "";
        if (userData != null) {
            nick = userData.toString();
        }
        String log = "onRemotePeerUserDataReceive for Peer " + remotePeerId + ":\n" + nick;
        Log.d(TAG, log);
    }

    @Override
    public void onOpenDataConnection(String remotePeerId) {
        Log.d(TAG, "onOpenDataConnection for Peer " + remotePeerId + ".");
    }

}