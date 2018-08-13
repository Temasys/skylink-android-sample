package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.support.v7.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.webrtc.SurfaceViewRenderer;

import java.util.concurrent.ConcurrentHashMap;

import sg.com.temasys.skylink.sdk.rtc.Info;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;

import static android.view.Gravity.CENTER;

/**
 * Created by muoi.pham on 20/07/18.
 */
public class MultiPartyVideoCallFragment extends Fragment implements MultiPartyVideoCallContract.View {

    public String ROOM_NAME;
    public String MY_USER_NAME;

    private static final String TAG = MultiPartyVideoCallFragment.class.getName();

    //this variable need to be static for configuration change
    private static MultiPartyVideoCallContract.Presenter mPresenter;

    private PermissionUtils permissionUtils;

    private FrameLayout selfLayout;

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
    private Context mContext;

    // Map with PeerId as key for boolean state
    // that indicates if currently getting WebRTC stats for Peer.
    private static ConcurrentHashMap<String, Boolean> isGettingWebrtcStats =
            new ConcurrentHashMap<String, Boolean>();

    public MultiPartyVideoCallFragment() {
        // Required empty public constructor
    }

    public static MultiPartyVideoCallFragment newInstance() {
        return new MultiPartyVideoCallFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ROOM_NAME = Config.ROOM_NAME_PARTY;
        MY_USER_NAME = Config.USER_NAME_PARTY;

        View rootView = inflater.inflate(R.layout.fragment_video_multiparty, container, false);

        getControlWidgets(rootView);

        setActionBar();

        initComponents();

        // Set OnClick actions for each Peer's UI.
        for (int peerIndex = 0; peerIndex < videoViewLayouts.length; ++peerIndex) {
            FrameLayout frameLayout = videoViewLayouts[peerIndex];
            if (frameLayout == selfLayout) {
                // Show room and self info, plus give option to
                // switch self view between different cameras (if any).
                frameLayout.setOnClickListener(v -> {
                    String name = mPresenter.getRoomPeerIdNickPresenterHandler();

                    TextView selfTV = new TextView(mContext);
                    selfTV.setText(name);
                    selfTV.setTextIsSelectable(true);
                    AlertDialog.Builder selfDialogBuilder =
                            new AlertDialog.Builder(mContext);
                    selfDialogBuilder.setView(selfTV);
                    selfDialogBuilder.setPositiveButton("OK", null);
                    // Get the input video resolution.
                    selfDialogBuilder.setPositiveButton("Input video resolution",
                            (dialog, which) -> mPresenter.getInputVideoResolutionPresenterHandler());

                    selfDialogBuilder.setNegativeButton("Switch Camera",
                            (dialog, which) -> {
                                mPresenter.switchCameraPresenterHandler();
                            });
                    selfDialogBuilder.show();
                });
            } else {
                // Allow each remote Peer FrameLayout to be able to provide an action menu.
                frameLayout.setOnClickListener(showMenuRemote(peerIndex));
            }
        }

        // Check if it was an orientation change
        if (savedInstanceState != null) {
            // Resume previous permission request, if any.
            permissionUtils.permQResume(mContext, this);

            // Toggle camera back to previous state if required.
            if (toggleCamera) {
                if (mPresenter.getVideoViewPresenterHandler(null) != null) {
                    mPresenter.toggleCameraPresenterHandler();
                    toggleCamera = false;
                }
            }

        } else {
            // This is the start of this sample, reset permission request states.
            permissionUtils.permQReset();

            // Set toggleCamera back to default state.
            toggleCamera = false;
        }

        // Try to connect to room if we have not tried connecting.
        if (!mPresenter.isConnectingOrConnectedPresenterHandler()) {
            connectToRoom();
        }

        // Set the appropriate UI.
        // Add all existing VideoViews to UI
        addViews();

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Allow volume to be controlled using volume keys
        ((MultiPartyVideoCallActivity) mContext).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Toggle camera back to previous state if required.
        if (toggleCamera) {
            if (mPresenter.getVideoViewPresenterHandler(null) != null) {
                mPresenter.toggleCameraPresenterHandler();
                toggleCamera = false;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop local video source only if not changing orientation
        if (!((MultiPartyVideoCallActivity) mContext).isChangingConfigurations()) {
            if (mPresenter.getVideoViewPresenterHandler(null) != null) {
                // Stop local video source if it's on.
                // Record if need to toggleCamera when resuming.
                toggleCamera = mPresenter.toggleCameraPresenterHandler(false);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //update actionbar title
        this.mContext = context;
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
        if (!((MultiPartyVideoCallActivity) mContext).isChangingConfigurations()) {
            mPresenter.disconnectFromRoomPresenterHandler();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        permissionUtils.onRequestPermissionsResultHandler(
                requestCode, permissions, grantResults, TAG);
    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView){
        selfLayout = (FrameLayout) rootView.findViewById(R.id.self_video);
        FrameLayout peer1Layout = (FrameLayout) rootView.findViewById(R.id.peer_1);
        FrameLayout peer2Layout = (FrameLayout) rootView.findViewById(R.id.peer_2);
        FrameLayout peer3Layout = (FrameLayout) rootView.findViewById(R.id.peer_3);

        videoViewLayouts = new FrameLayout[]{selfLayout, peer1Layout, peer2Layout, peer3Layout};
    }

    private void setActionBar(){
        ActionBar actionBar = ((MultiPartyVideoCallActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        setHasOptionsMenu(true);
    }

    private void initComponents(){
        permissionUtils = new PermissionUtils();
    }

    private void connectToRoom() {
        mPresenter.connectToRoomPresenterHandler(ROOM_NAME);

    }

    private void refreshConnection(String peerId, boolean iceRestart) {
        mPresenter.refreshConnectionPresenterHandler(peerId, iceRestart);
    }

    private boolean startRecording() {
        return mPresenter.startRecordingPresenterHandler();
    }

    private boolean stopRecording() {
        return mPresenter.stopRecordingPresenterHandler();
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
        mPresenter.getTransferSpeedsPresenterHandler(peerId, Info.MEDIA_DIRECTION_BOTH, Info.MEDIA_ALL);
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
            mPresenter.getWebrtcStatsPresenterHandler(peerId, Info.MEDIA_DIRECTION_BOTH, Info.MEDIA_ALL);

            // Wait for waitMs ms before requesting WebRTC stats again.
            final int waitMs = 1000;
            new Thread(() -> {
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException e) {
                    String error =
                            "[SA][WStatsAll] Error while waiting to call for WebRTC stats again: " +
                                    e.getMessage();
                    Log.e(TAG, error);
                }
                getWStatsAll(peerId);
            }).start();

        }
    }

    /**
     * Remove all videoViews from layouts.
     */
    private void emptyLayout() {
        int totalInRoom = mPresenter.getTotalInRoomPresenterHandler();
        for (int i = 0; i < totalInRoom; i++) {
            Utils.removeViewFromParent(mPresenter.getVideoViewPresenterHandler(peerList[i]));
        }
    }

    /**
     * Set LayoutParams for a VideoView to fit within it's containing FrameLayout, in the center.
     *
     * @param videoView
     */
    private void setLayoutParams(SurfaceViewRenderer videoView) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, CENTER);
        videoView.setLayoutParams(params);
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
                    setLayoutParams(view);
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
        SurfaceViewRenderer videoView = mPresenter.getVideoViewPresenterHandler(remotePeerId);
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
        setLayoutParams(videoView);
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
        String[] peers = mPresenter.getPeerIdListPresenterHandler();


        if (peers != null) {
            peerList[0] = peers[0];
        }
        setLayoutParams(videoView);
        videoViewLayouts[0].addView(videoView);
    }

    /**
     * Add all videoViews onto layouts.
     */
    private void addViews() {
        // Add self VideoView
        addSelfView(mPresenter.getVideoViewPresenterHandler(null));

        // Add remote VideoView(s)
        int totalInRoom = mPresenter.getTotalInRoomPresenterHandler();
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
        return v -> {

            int totalInRoom = mPresenter.getTotalInRoomPresenterHandler();
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
                    item -> {

                        int id = item.getItemId();
                        switch (id) {
                            case R.id.vid_res_sent:
                                if (peerId == null) {
                                    return false;
                                }
                                mPresenter.getSentVideoResolutionPresenterHandler(peerId);

                                return true;
                            case R.id.vid_res_recv:
                                if (peerId == null) {
                                    return false;
                                }
                                mPresenter.getReceivedVideoResolutionPresenterHandler(peerId);
                                return true;
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
                    };
            // Add room name to title
            String title = mPresenter.getRoomPeerIdNickPresenterHandler(ROOM_NAME, peerId);

            PopupMenu popupMenu = new PopupMenu(mContext, v);
            popupMenu.setOnMenuItemClickListener(clickListener);

            popupMenu.getMenu().add(title);
            // Populate actions of Popup Menu.
            if (peerId != null) {
                popupMenu.getMenu().add(0, R.id.vid_res_sent, 0, R.string.vid_res_sent);
                popupMenu.getMenu().add(0, R.id.vid_res_recv, 0, R.string.vid_res_recv);
            }
            if (peerId != null) {
                String statsStr = getString(R.string.webrtc_stats);
                final Boolean gettingStats = isGettingWebrtcStats.get(peerId);
                if ((gettingStats != null) && gettingStats) {
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
        };
    }

    //----------------------------------------------------------------------------------------------
    // View Listeners to update GUI from presenter
    //----------------------------------------------------------------------------------------------

    @Override
    public void setPresenter(MultiPartyVideoCallContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public void addSelfViewPresenterHandler(SurfaceViewRenderer videoView) {
        addSelfView(videoView);
    }

    @Override
    public Fragment getFragmentViewHandler() {
        return this;
    }

    @Override
    public void addRemoteViewViewHandler(String remotePeerId) {
        addRemoteView(remotePeerId);
    }

    @Override
    public void addRemotePeerViewHandler(String remotePeerId) {
        addRemotePeer(remotePeerId);
    }

    @Override
    public void removeRemotePeerHandler(String remotePeerId) {
        removeRemotePeer(remotePeerId);
    }

    @Override
    public void setPeerListViewHandler(String peer) {
        peerList[0] = peer;
    }

}