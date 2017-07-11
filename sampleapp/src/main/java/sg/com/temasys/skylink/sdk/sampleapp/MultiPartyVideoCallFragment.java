package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.webrtc.SurfaceViewRenderer;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.OsListener;
import sg.com.temasys.skylink.sdk.listener.RecordingListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.listener.StatsListener;
import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.Info;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;

import static sg.com.temasys.skylink.sdk.sampleapp.Utils.getNumRemotePeers;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.getTotalInRoom;

/**
 * Created by janidu on 3/3/15.
 */
public class MultiPartyVideoCallFragment extends Fragment implements
        LifeCycleListener, OsListener, MediaListener, RemotePeerListener, RecordingListener,
        StatsListener {

    public String ROOM_NAME;
    public String MY_USER_NAME;


    public static final String KEY_SELF = "self";
    private static final String TAG = MultiPartyVideoCallFragment.class.getName();
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
    // List that associate FrameLayout position with PeerId.
    // First position is for local Peer.
    private static String[] peerList = new String[4];
    private Activity parentActivity;
    private Context applicationContext;

    // Map with PeerId as key for boolean state
    // that indicates if currently getting WebRTC stats for Peer.
    private ConcurrentHashMap<String, Boolean> isGettingWebrtcStats =
            new ConcurrentHashMap<String, Boolean>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ROOM_NAME = Config.ROOM_NAME_PARTY;
        MY_USER_NAME = Config.USER_NAME_PARTY;

        View rootView = inflater.inflate(R.layout.fragment_video_multiparty, container, false);

        FrameLayout selfLayout = (FrameLayout) rootView.findViewById(R.id.self_video);
        FrameLayout peer1Layout = (FrameLayout) rootView.findViewById(R.id.peer_1);
        FrameLayout peer2Layout = (FrameLayout) rootView.findViewById(R.id.peer_2);
        FrameLayout peer3Layout = (FrameLayout) rootView.findViewById(R.id.peer_3);

        videoViewLayouts = new FrameLayout[]{selfLayout, peer1Layout, peer2Layout, peer3Layout};

        // Set OnClick actions for each Peer's UI.
        for (int peerIndex = 0; peerIndex < videoViewLayouts.length; ++peerIndex) {
            FrameLayout frameLayout = videoViewLayouts[peerIndex];
            if (frameLayout == selfLayout) {
                // Show room and self info, plus give option to
                // switch self view between different cameras (if any).
                frameLayout.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (skylinkConnection != null) {
                            String name = Utils.getRoomPeerIdNick(skylinkConnection, ROOM_NAME,
                                    skylinkConnection.getPeerId());
                            TextView selfTV = new TextView(getContext());
                            selfTV.setText(name);
                            selfTV.setTextIsSelectable(true);
                            AlertDialog.Builder selfDialogBuilder =
                                    new AlertDialog.Builder(getContext());
                            selfDialogBuilder.setView(selfTV);
                            selfDialogBuilder.setPositiveButton("OK", null);
                            selfDialogBuilder.setNegativeButton("Switch Camera",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            skylinkConnection.switchCamera();
                                        }
                                    });
                            selfDialogBuilder.show();
                        }
                    }
                });
            } else {
                // Allow each remote Peer FrameLayout to be able to provide an action menu.
                frameLayout.setOnClickListener(showMenuRemote(peerIndex));
            }
        }

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
            connectToRoom();
        }

        // Set the appropriate UI.
        // Add all existing VideoViews to UI
        addViews();

        return rootView;
    }


    private void connectToRoom() {
        // Initialize the skylink connection
        initializeSkylinkConnection();

        // Create the Skylink connection string.
        // In production, the connection string should be generated by an external entity
        // (such as a secure App server that has the Skylink App Key secret), and sent to the App.
        // This is to avoid keeping the App Key secret within the application, for better security.
        String skylinkConnectionString = Utils.getSkylinkConnectionString(
                ROOM_NAME, new Date(), SkylinkConnection.DEFAULT_DURATION);

        // The skylinkConnectionString should not be logged in production,
        // as it contains potentially sensitive information like the Skylink App Key ID.

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        Utils.onRequestPermissionsResultHandler(
                requestCode, permissions, grantResults, TAG, skylinkConnection);
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

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        // AudioVideo config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setMirrorLocalView(true);

        // Allow only 3 remote Peers to join, due to current UI design.
        config.setMaxPeers(3);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(config);
        config.getAdvancedOptions().put("SdkAdvancedOption)!", new Boolean(true));
        return config;
    }

    private void initializeSkylinkConnection() {
        skylinkConnection = SkylinkConnection.getInstance();
        //the app_key and app_secret is obtained from the temasys developer console.
        skylinkConnection.init(Config.getAppKey(),
                getSkylinkConfig(), this.applicationContext);
        // Set listeners to receive callbacks when events are triggered
        setListeners();
    }

    /**
     * Set listeners to receive callbacks when events are triggered.
     * SkylinkConnection instance must not be null or listeners cannot be set.
     * Do not set before {@link SkylinkConnection#init} as that will remove all existing Listeners.
     *
     * @return false if listeners could not be set.
     */
    private boolean setListeners() {
        if (skylinkConnection != null) {
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setMediaListener(this);
            skylinkConnection.setOsListener(this);
            skylinkConnection.setRecordingListener(this);
            skylinkConnection.setRemotePeerListener(this);
            skylinkConnection.setStatsListener(this);
            return true;
        } else {
            return false;
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


    private void refreshConnection(String peerId, boolean iceRestart) {
        String peer = "all Peers";
        if (peerId != null) {
            peer = "Peer " + Utils.getPeerIdNick(peerId);
        }
        String log = "Refreshing connection for " + peer;
        if (iceRestart) {
            log += " with ICE restart.";
        } else {
            log += ".";
        }
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);

        // Refresh connections and log errors if any.
        String[] failedPeers = skylinkConnection.refreshConnection(peerId, iceRestart);
        if (failedPeers != null) {
            log = "Unable to refresh ";
            if ("".equals(failedPeers[0])) {
                log += "as there is no Peer in the room!";
            } else {
                log += "for Peer(s): " + Arrays.toString(failedPeers) + "!";
            }
            Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
            Log.d(TAG, log);
        }
    }

    private boolean startRecording() {
        boolean success = skylinkConnection.startRecording();
        String log = "[SRS][SA] startRecording=" + success +
                ", isRecording=" + skylinkConnection.isRecording() + ".";
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
        return success;
    }

    private boolean stopRecording() {
        boolean success = skylinkConnection.stopRecording();
        String log = "[SRS][SA] stopRecording=" + success +
                ", isRecording=" + skylinkConnection.isRecording() + ".";
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
        return success;
    }

    /**
     * Toggle WebRTC Stats logging on or off for specific Peer.
     *
     * @param peerId
     */
    private void webrtcStatsToggle(String peerId) {
        Boolean gettingStats = isGettingWebrtcStats.get(peerId);
        if (gettingStats == null) {
            String log = "[SA][wStatsTog] Peer " + peerId +
                    " does not exist. Will not get WebRTC stats.";
            Log.e(TAG, log);
            return;
        }

        // Toggle the state of getting WebRTC stats to the opposite state.
        if (gettingStats) {
            gettingStats = false;
        } else {
            gettingStats = true;
        }
        isGettingWebrtcStats.put(peerId, gettingStats);
        getWStatsAll(peerId);
    }

    /**
     * Get Transfer speed in kbps of all media streams for specific Peer.
     *
     * @param peerId
     */
    private void getTransferSpeedAll(String peerId) {
        // Request to get media transfer speeds.
        skylinkConnection.getTransferSpeeds(peerId, Info.MEDIA_DIRECTION_BOTH, Info.MEDIA_ALL);
    }

    /**
     * Trigger getWebrtcStats for specific Peer in a loop if current state allows.
     * To stop loop, set {@link #isGettingWebrtcStats} to false.
     *
     * @param peerId
     */
    private void getWStatsAll(final String peerId) {
        Boolean gettingStats = isGettingWebrtcStats.get(peerId);
        if (gettingStats == null) {
            String log = "[SA][WStatsAll] Peer " + peerId +
                    " does not exist. Will not get WebRTC stats.";
            Log.e(TAG, log);
            return;
        }

        if (gettingStats) {
            // Request to get WebRTC stats.
            skylinkConnection.getWebrtcStats(peerId, Info.MEDIA_DIRECTION_BOTH, Info.MEDIA_ALL);

            // Wait for waitMs ms before requesting WebRTC stats again.
            final int waitMs = 1000;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(waitMs);
                    } catch (InterruptedException e) {
                        String error =
                                "[SA][WStatsAll] Error while waiting to call for WebRTC stats again: " +
                                        e.getMessage();
                        Log.e(TAG, error);
                    }
                    getWStatsAll(peerId);
                }
            }).start();

        }
        // skylinkConnection.getWebrtcStats(peerId, Info.MEDIA_DIRECTION_BOTH, Info.MEDIA_ALL);
    }


    //----------------------------------------------------------------------------------------------
    // UI helper methods
    //----------------------------------------------------------------------------------------------


    /**
     * Remove all videoViews from layouts.
     */
    private void emptyLayout() {
        int totalInRoom = getTotalInRoom();
        for (int i = 0; i < totalInRoom; i++) {
            Utils.removeViewFromParent(getVideoView(peerList[i]));
        }
    }

    /**
     * Get the Peer index of a Peer, given it's PeerId.
     *
     * @param peerId PeerId of the Peer for whom to retrieve its Peer index
     * @return Peer index of a Peer, which is the it's index in peerList.
     * Return a negative number if PeerId could not be found.
     */
    private int getPeerIndex(String peerId) {
        if (peerId == null) {
            return -1;
        }
        for (int index = 0; index < peerList.length; ++index) {
            if (peerId.equals(peerList[index])) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Remove any existing VideoView of a Peer.
     *
     * @param peerId
     * @return The index at which Peer was located, or a negative int if Peer Id was not found.
     */
    private int removePeerView(String peerId) {
        int indexToRemove = getPeerIndex(peerId);
        // Safety check
        if (indexToRemove < 0 || indexToRemove > peerList.length) {
            return -1;
        }
        // Remove view
        videoViewLayouts[indexToRemove].removeAllViews();
        return indexToRemove;
    }

    /**
     * Add a remote Peer into peerList at the first available remote index, and return it's index.
     * Add to any other Peer maps.
     *
     * @param peerId
     * @return Peer Index added at, or a negative int if Peer could not be added.
     */
    private int addRemotePeer(String peerId) {
        if (peerId == null) {
            return -1;
        }
        for (int peerIndex = 1; peerIndex < peerList.length; ++peerIndex) {
            if (peerList[peerIndex] == null) {
                peerList[peerIndex] = peerId;
                // Add to other Peer maps
                isGettingWebrtcStats.put(peerId, false);
                return peerIndex;
            }
        }
        return -1;
    }

    /**
     * Remove a remote Peer from peerList, other Peer maps, and any video view from UI.
     *
     * @param peerId
     */
    private void removeRemotePeer(String peerId) {
        int index = getPeerIndex(peerId);
        if (index < 1 || index > videoViewLayouts.length) {
            return;
        }
        removePeerView(peerId);
        peerList[index] = null;
        isGettingWebrtcStats.remove(peerId);
        shiftUpRemotePeers();
    }

    /**
     * Shift remote Peers and their views up the peerList and UI, such that there are no empty
     * elements or UI between local Peer and the last remote Peer.
     */
    private void shiftUpRemotePeers() {
        int indexEmpty = 0;
        // Remove view from layout.
        for (int i = 1; i < videoViewLayouts.length; ++i) {
            if (peerList[i] == null) {
                indexEmpty = i;
                continue;
            }
            // Switch Peer to empty spot if there is any.
            if (indexEmpty > 0) {
                // Shift peerList.
                String peerId = peerList[i];
                peerList[i] = null;
                peerList[indexEmpty] = peerId;
                // Shift UI.
                FrameLayout peerFrameLayout = videoViewLayouts[i];
                // Put this view in the layout before it.
                SurfaceViewRenderer view = (SurfaceViewRenderer) peerFrameLayout.getChildAt(0);
                if (view != null) {
                    peerFrameLayout.removeAllViews();
                    videoViewLayouts[indexEmpty].addView(view);
                }
                ++indexEmpty;
            }
        }
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

        // Remove any existing Peer View.
        // This may sometimes be the case, for e.g. in screen sharing.
        removePeerView(remotePeerId);

        int index = getPeerIndex(remotePeerId);
        if (index < 1 || index > videoViewLayouts.length) {
            return;
        }
        videoViewLayouts[index].addView(videoView);
    }

    private void addSelfView(SurfaceViewRenderer videoView) {
        if (videoView == null) {
            return;
        }

        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(videoView);

        // Remove self view if its already added
        videoViewLayouts[0].removeAllViews();

        // Add self PeerId and view.
        String[] peers = skylinkConnection.getPeerIdList();
        if (peers != null) {
            peerList[0] = peers[0];
        }
        videoViewLayouts[0].addView(videoView);
    }

    /**
     * Remove a specific VideoView.
     *
     * @param viewToRemove
     */
    private void removeVideoView(SurfaceViewRenderer viewToRemove) {
        // Remove view from layout.
        for (int peerIndex = 0; peerIndex < videoViewLayouts.length; ++peerIndex) {
            FrameLayout peerFrameLayout = videoViewLayouts[peerIndex];

            if (peerFrameLayout.getChildAt(0) == viewToRemove) {
                // Remove if view found.
                peerFrameLayout.removeView(viewToRemove);
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
        int totalInRoom = getTotalInRoom();
        // Iterate over the remote Peers only (first Peer is self Peer)
        for (int i = 1; i < totalInRoom; i++) {
            addRemoteView(peerList[i]);
        }
    }

    /**
     * Create and return onClickListener that
     * show list of potential actions on clicking space for remote Peers.
     */
    private OnClickListener showMenuRemote(final int peerIndex) {
        // Get peerId
        return new OnClickListener() {
            @Override
            public void onClick(View v) {

                int totalInRoom = getTotalInRoom();
                // Do not allow action if no one is in the room.
                if (totalInRoom == 0) {
                    return;
                }

                String peerIdTemp = null;
                if (peerIndex < totalInRoom) {
                    peerIdTemp = peerList[peerIndex];
                }
                final String peerId = peerIdTemp;
                PopupMenu.OnMenuItemClickListener clickListener =
                        new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {

                                int id = item.getItemId();
                                switch (id) {
                                    case R.id.webrtc_stats:
                                        if (peerId == null) {
                                            return false;
                                        }
                                        webrtcStatsToggle(peerId);
                                        return true;
                                    case R.id.transfer_speed:
                                        if (peerId == null) {
                                            return false;
                                        }
                                        getTransferSpeedAll(peerId);
                                        return true;
                                    case R.id.recording_start:
                                        return startRecording();
                                    case R.id.recording_stop:
                                        return stopRecording();
                                    case R.id.restart:
                                        if (peerId == null) {
                                            return false;
                                        }
                                        refreshConnection(peerId, false);
                                        return true;
                                    case R.id.restart_all:
                                        refreshConnection(null, false);
                                        return true;
                                    case R.id.restart_ice:
                                        if (peerId == null) {
                                            return false;
                                        }
                                        refreshConnection(peerId, true);
                                        return true;
                                    case R.id.restart_all_ice:
                                        refreshConnection(null, true);
                                        return true;
                                    default:
                                        Log.e(TAG, "Unknown menu option: " + id + "!");
                                        return false;
                                }
                            }
                        };
                // Add room name to title
                String title = Utils.getRoomPeerIdNick(skylinkConnection, ROOM_NAME, peerId);

                PopupMenu popupMenu = new PopupMenu(getContext(), v);
                popupMenu.setOnMenuItemClickListener(clickListener);

                popupMenu.getMenu().add(title);
                // Populate actions of Popup Menu.
                if (peerId != null) {
                    String statsStr = getString(R.string.webrtc_stats);
                    if (isGettingWebrtcStats.get(peerId)) {
                        statsStr += " (ON)";
                    } else {
                        statsStr += " (OFF)";
                    }
                    popupMenu.getMenu().add(0, R.id.webrtc_stats, 0, statsStr);
                    popupMenu.getMenu().add(0, R.id.transfer_speed, 0, R.string.transfer_speed);
                }
                popupMenu.getMenu().add(0, R.id.recording_start, 0, R.string.recording_start);
                popupMenu.getMenu().add(0, R.id.recording_stop, 0, R.string.recording_stop);
                if (peerId != null) {
                    popupMenu.getMenu().add(0, R.id.restart, 0, R.string.restart);
                }
                popupMenu.getMenu().add(0, R.id.restart_all, 0, R.string.restart_all);
                if (peerId != null) {
                    popupMenu.getMenu().add(0, R.id.restart_ice, 0, R.string.restart_ice);
                }
                popupMenu.getMenu().add(0, R.id.restart_all_ice, 0, R.string.restart_all_ice);
                popupMenu.show();
            }
        };
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's
     * lifecycle
     */

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        if (isSuccessful) {
            String log = "Connected to room " + ROOM_NAME + " (" + skylinkConnection.getRoomId() +
                    ") as " + skylinkConnection.getPeerId() + " (" + MY_USER_NAME + ").";
            Toast.makeText(parentActivity, log, Toast.LENGTH_LONG).show();
            Log.d(TAG, log);

            peerList[0] = skylinkConnection.getPeerIdList()[0];
        } else {
            String error = "Skylink failed to connect!\nReason : " + message;
            Log.d(TAG, error);
            Toast.makeText(parentActivity, error, Toast.LENGTH_LONG).show();
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
        Utils.handleSkylinkReceiveLog(infoCode, message, parentActivity, TAG);
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Utils.handleSkylinkWarning(errorCode, message, parentActivity, TAG);
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
        String peer = "Peer " + Utils.getPeerIdNick(peerId);
        // If peerId is null, this call is for our local video.
        if (peerId == null) {
            peer = "We've";
        }
        Log.d(TAG, peer + " got video size changed to: " + size.toString() + ".");
    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, SurfaceViewRenderer videoView) {
        addRemoteView(remotePeerId);
        String log = "Received new ";
        if (videoView != null) {
            log += "Video ";
        } else {
            log += "Audio ";
        }
        log += "from Peer " + Utils.getPeerIdNick(remotePeerId) + ".\r\n";

        UserInfo remotePeerUserInfo = skylinkConnection.getUserInfo(remotePeerId);
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
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

    /**
     * OsListener Callbacks - triggered by Android OS related events.
     */
    @Override
    public void onPermissionRequired(
            final String[] permissions, final int requestCode, final int infoCode) {
        Utils.onPermissionRequiredHandler(permissions, requestCode, infoCode, getContext(), this, TAG, skylinkConnection);
    }

    @Override
    public void onPermissionGranted(String[] permissions, int requestCode, int infoCode) {
        Utils.onPermissionGrantedHandler(permissions, infoCode, TAG);
    }

    @Override
    public void onPermissionDenied(String[] permissions, int requestCode, int infoCode) {
        Utils.onPermissionDeniedHandler(infoCode, getContext(), TAG);
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        addRemotePeer(remotePeerId);
        String log = "Your Peer " + Utils.getPeerIdNick(remotePeerId) + " connected.";
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message, UserInfo userInfo) {
        int numRemotePeers = getNumRemotePeers();
        removeRemotePeer(remotePeerId);
        String log = "Your Peer " + Utils.getPeerIdNick(remotePeerId, userInfo) + " left: " +
                message + ". " + numRemotePeers + " remote Peer(s) left in the room.";
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRemotePeerConnectionRefreshed(
            String remotePeerId, Object userData, boolean hasDataChannel, boolean wasIceRestarted) {
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
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        // If Peer has no userData, use an empty string for nick.
        String nick = "";
        if (userData != null) {
            nick = userData.toString();
        }
        String log = "onRemotePeerUserDataReceive for Peer " + Utils.getPeerIdNick(remotePeerId) + ":\n" + nick;
        Log.d(TAG, log);
    }

    @Override
    public void onOpenDataConnection(String remotePeerId) {
        Log.d(TAG, "onOpenDataConnection for Peer " + Utils.getPeerIdNick(remotePeerId) + ".");
    }

    /**
     * Recording Listener Callbacks - triggered during Recording events.
     */

    @Override
    public void onRecordingStart(String recordingId) {
        String log = "[SRS][SA] Recording Started! isRecording=" +
                skylinkConnection.isRecording() + ".";
        Toast.makeText(parentActivity, log, Toast.LENGTH_LONG).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRecordingStop(String recordingId) {
        String log = "[SRS][SA] Recording Stopped! isRecording=" +
                skylinkConnection.isRecording() + ".";
        Toast.makeText(parentActivity, log, Toast.LENGTH_LONG).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRecordingVideoLink(String recordingId, String peerId, String videoLink) {
        String peer = " Mixin";
        if (peerId != null) {
            peer = " Peer " + Utils.getPeerIdNick(peerId) + "'s";
        }
        String msg = "Recording:" + recordingId + peer + " video link:\n" + videoLink;

        // Create a clickable video link.
        final SpannableString videoLinkClickable = new SpannableString(msg);
        Linkify.addLinks(videoLinkClickable, Linkify.WEB_URLS);

        // Create TextView for video link.
        final TextView msgTxtView = new TextView(getContext());
        msgTxtView.setText(videoLinkClickable);
        msgTxtView.setMovementMethod(LinkMovementMethod.getInstance());

        // Create AlertDialog to present video link.
        AlertDialog.Builder videoLinkDialogBuilder = new AlertDialog.Builder(getContext());
        videoLinkDialogBuilder.setTitle("Recording: " + recordingId + " Video link");
        videoLinkDialogBuilder.setView(msgTxtView);
        videoLinkDialogBuilder.setPositiveButton("OK", null);
        videoLinkDialogBuilder.show();
        Log.d(TAG, "[SRS][SA] " + msg);
    }

    @Override
    public void onRecordingError(String recordingId, int errorCode, String description) {
        String error = "[SRS][SA] Received Recording error with errorCode:" + errorCode +
                "! Error: " + description;
        Toast.makeText(parentActivity, error, Toast.LENGTH_LONG).show();
        Log.e(TAG, error);
    }

    /**
     * Stats Listener Callbacks - triggered during statistics measuring events.
     */

    @Override
    public void onWebrtcStatsReceived(final String peerId, int mediaDirection, int mediaType, HashMap<String, String> stats) {
        // Log the WebRTC stats.
        StringBuilder log =
                new StringBuilder("[SA][WStatsRecv] Received for Peer " + peerId + ":\r\n");
        for (Map.Entry<String, String> entry : stats.entrySet()) {
            log.append(entry.getKey()).append(": ").append(entry.getValue()).append(".\r\n");
        }
        Log.d(TAG, log.toString());
    }

    @Override
    public void onTransferSpeedReceived(String peerId, int mediaDirection, int mediaType, double transferSpeed) {
        String direction = "Send";
        if (Info.MEDIA_DIRECTION_RECV == mediaDirection) {
            direction = "Recv";
        }
        // Log the transfer speeds.
        String log = "[SA][TransSpeed] Transfer speed for Peer " + peerId + ": " +
                Info.getInfoString(mediaType) + " " + direction + " = " + transferSpeed + " kbps";
        Log.d(TAG, log);
    }
}