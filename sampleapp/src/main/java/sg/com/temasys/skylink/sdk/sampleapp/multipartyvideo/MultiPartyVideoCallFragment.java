package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.support.v7.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.Info;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static android.view.Gravity.CENTER;

/**
 * Created by muoi.pham on 20/07/18.
 */
public class MultiPartyVideoCallFragment extends Fragment implements MultiPartyVideoCallContract.View {

    private final String TAG = MultiPartyVideoCallFragment.class.getName();

    private Context mContext;

    private MultiPartyVideoCallContract.Presenter mPresenter;

    private FrameLayout selfLayout;

    private FrameLayout[] videoViewLayouts;

    public static MultiPartyVideoCallFragment newInstance() {
        return new MultiPartyVideoCallFragment();
    }

    @Override
    public void setPresenter(MultiPartyVideoCallContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Allow volume to be controlled using volume keys
        ((MultiPartyVideoCallActivity) mContext).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "[SA][MultiPartyVideo][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_video_multiparty, container, false);

        getControlWidgets(rootView);

        setActionBar();

        requestViewLayout();

        // Set OnClick actions for each Peer's UI.
        for (int peerIndex = 0; peerIndex < videoViewLayouts.length; ++peerIndex) {
            FrameLayout frameLayout = videoViewLayouts[peerIndex];
            if (frameLayout == selfLayout) {
                // Show room and self info, plus give option to
                // switch self view between different cameras (if any).
                frameLayout.setOnClickListener(v -> {
                    String name = mPresenter.onGetRoomPeerIdNick();

                    TextView selfTV = new TextView(mContext);
                    selfTV.setText(name);
                    selfTV.setTextIsSelectable(true);
                    AlertDialog.Builder selfDialogBuilder =
                            new AlertDialog.Builder(mContext);
                    selfDialogBuilder.setView(selfTV);
                    selfDialogBuilder.setPositiveButton("OK", null);
                    // Get the input video resolution.
                    selfDialogBuilder.setPositiveButton("Input video resolution",
                            (dialog, which) -> mPresenter.onGetInputVideoResolution());

                    selfDialogBuilder.setNegativeButton("Switch Camera",
                            (dialog, which) -> {
                                mPresenter.onSwitchCamera();
                            });
                    selfDialogBuilder.show();
                });
            } else {
                // Allow each remote Peer FrameLayout to be able to provide an action menu.
                frameLayout.setOnClickListener(showMenuRemote(peerIndex));
            }
        }

        return rootView;
    }


    @Override
    public void onResume() {

        super.onResume();

        mPresenter.onViewResume();
    }

    @Override
    public void onPause() {

        super.onPause();

        // Stop local video source only if not changing orientation
        if (!((MultiPartyVideoCallActivity) mContext).isChangingConfigurations()) {
            mPresenter.onViewPause();
        }
    }

    @Override
    public void onDetach() {

        super.onDetach();
        if (!((MultiPartyVideoCallActivity) mContext).isChangingConfigurations()) {
            mPresenter.onViewExit();
            emptyLayout();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        mPresenter.onRequestPermissionsResult(requestCode, permissions, grantResults, TAG);
    }

    @Override
    public void onAddSelfView(SurfaceViewRenderer videoView) {
        addSelfView(videoView);
    }

    @Override
    public void onAddRemoteView(int peerIndex, SurfaceViewRenderer remoteView) {
        addRemoteView(peerIndex, remoteView);
    }

    @Override
    public void onRemoveRemotePeer(int viewIndex) {
        removeRemotePeer(viewIndex);
    }

    @Override
    public Fragment onGetFragment() {
        return this;
    }

    @Override
    public void onDisplayAlerDlg(String recordingId, String msg) {
        // Create a clickable video link.
        final SpannableString videoLinkClickable = new SpannableString(msg);
        Linkify.addLinks(videoLinkClickable, Linkify.WEB_URLS);

        // Create TextView for video link.
        final TextView msgTxtView = new TextView(mContext);
        msgTxtView.setText(videoLinkClickable);
        msgTxtView.setMovementMethod(LinkMovementMethod.getInstance());

        // Create AlertDialog to present video link.
        AlertDialog.Builder videoLinkDialogBuilder = new AlertDialog.Builder(mContext);
        videoLinkDialogBuilder.setTitle("Recording: " + recordingId + " Video link");
        videoLinkDialogBuilder.setView(msgTxtView);
        videoLinkDialogBuilder.setPositiveButton("OK", null);
        videoLinkDialogBuilder.show();
        Log.d(TAG, "[SRS][SA] " + msg);
    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        selfLayout = rootView.findViewById(R.id.self_video);
        FrameLayout peer1Layout = rootView.findViewById(R.id.peer_1);
        FrameLayout peer2Layout = rootView.findViewById(R.id.peer_2);
        FrameLayout peer3Layout = rootView.findViewById(R.id.peer_3);

        videoViewLayouts = new FrameLayout[]{selfLayout, peer1Layout, peer2Layout, peer3Layout};
    }

    private void setActionBar() {
        ActionBar actionBar = ((MultiPartyVideoCallActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        setHasOptionsMenu(true);
    }

    /**
     * Get Transfer speed in kbps of all media streams for specific Peer.
     *
     * @param peerIndex index of the peer
     */
    private void getTransferSpeedAll(int peerIndex) {
        // Request to get media transfer speeds.
        mPresenter.onGetTransferSpeeds(peerIndex, Info.MEDIA_DIRECTION_BOTH, Info.MEDIA_ALL);
    }

    /**
     * Remove all videoViews from layouts.
     */
    private void emptyLayout() {
        int totalInRoom = mPresenter.onGetTotalInRoom();

        for (int i = 0; i < totalInRoom; i++) {
            SurfaceViewRenderer videoView = mPresenter.onGetVideoViewByIndex(i);

            if (videoView != null)
                Utils.removeViewFromParent(videoView);
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
     * Create and return onClickListener that
     * show list of potential actions on clicking space for remote Peers.
     */
    private OnClickListener showMenuRemote(final int peerIndex) {

        //check peerIndex is available or not
        if(mPresenter.onGetTotalInRoom() <= peerIndex)
            return null;

        // Get peerId
        return v -> {

            PopupMenu.OnMenuItemClickListener clickListener =
                    item -> {

                        if (peerIndex == -1) {
                            return false;
                        }

                        int id = item.getItemId();
                        switch (id) {
                            case R.id.vid_res_sent:
                                mPresenter.onGetSentVideoResolution(peerIndex);
                                return true;

                            case R.id.vid_res_recv:
                                mPresenter.onGetReceivedVideoResolution(peerIndex);
                                return true;

                            case R.id.webrtc_stats:
                                mPresenter.onWebrtcStatsToggle(peerIndex);
                                return true;

                            case R.id.transfer_speed:
                                getTransferSpeedAll(peerIndex);
                                return true;

                            case R.id.recording_start:
                                return startRecording();

                            case R.id.recording_stop:
                                return stopRecording();

                            case R.id.restart:
                                refreshConnection(peerIndex, false);
                                return true;

                            case R.id.restart_all:
                                refreshConnection(-1, false);
                                return true;

                            case R.id.restart_ice:
                                refreshConnection(peerIndex, true);
                                return true;

                            case R.id.restart_all_ice:
                                refreshConnection(-1, true);
                                return true;

                            default:
                                Log.e(TAG, "Unknown menu option: " + id + "!");
                                return false;
                        }
                    };
            // Add room name to title
            String title = mPresenter.onGetRoomPeerIdNick();

            PopupMenu popupMenu = new PopupMenu(mContext, v);
            popupMenu.setOnMenuItemClickListener(clickListener);

            popupMenu.getMenu().add(title);
            // Populate actions of Popup Menu.

            popupMenu.getMenu().add(0, R.id.vid_res_sent, 0, R.string.vid_res_sent);
            popupMenu.getMenu().add(0, R.id.vid_res_recv, 0, R.string.vid_res_recv);

            String statsStr = getString(R.string.webrtc_stats);
            final Boolean gettingStats = mPresenter.onGetWebRtcStatsByPeerId(peerIndex);
            if ((gettingStats != null) && gettingStats) {
                statsStr += " (ON)";
            } else {
                statsStr += " (OFF)";
            }
            popupMenu.getMenu().add(0, R.id.webrtc_stats, 0, statsStr);
            popupMenu.getMenu().add(0, R.id.transfer_speed, 0, R.string.transfer_speed);
            popupMenu.getMenu().add(0, R.id.recording_start, 0, R.string.recording_start);
            popupMenu.getMenu().add(0, R.id.recording_stop, 0, R.string.recording_stop);
            popupMenu.getMenu().add(0, R.id.restart, 0, R.string.restart);
            popupMenu.getMenu().add(0, R.id.restart_all, 0, R.string.restart_all);
            popupMenu.getMenu().add(0, R.id.restart_ice, 0, R.string.restart_ice);
            popupMenu.getMenu().add(0, R.id.restart_all_ice, 0, R.string.restart_all_ice);
            popupMenu.show();
        };
    }

    /**
     * request info to display from presenter
     * try to connect to room if not connected
     * try to update UI if connnected
     */
    private void requestViewLayout() {
        if (mPresenter != null) {
            mPresenter.onViewLayoutRequested();
        }
    }

    private void addSelfView(SurfaceViewRenderer videoView) {
        if (videoView == null) {
            return;
        }

        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(videoView);

        // Remove self view if its already added
        videoViewLayouts[0].removeAllViews();

        setLayoutParams(videoView);
        videoViewLayouts[0].addView(videoView);
    }


    private void refreshConnection(int peerIndex, boolean iceRestart) {
        mPresenter.onRefreshConnection(peerIndex, iceRestart);
    }

    private boolean startRecording() {
        return mPresenter.onStartRecording();
    }

    private boolean stopRecording() {
        return mPresenter.onStopRecording();
    }

    /**
     * Add or update remote Peer's VideoView into the app.
     *
     * @param index           index for display videoView
     * @param remoteVideoView videoView of remoteView
     */
    private void addRemoteView(int index, SurfaceViewRenderer remoteVideoView) {

        if (remoteVideoView == null || index < 1 || index > videoViewLayouts.length)
            return;

        // Remove any existing Peer View at index.
        // This may sometimes be the case, for e.g. in screen sharing.
        removePeerView(index);


        setLayoutParams(remoteVideoView);
        videoViewLayouts[index].addView(remoteVideoView);
    }

    /**
     * Remove any existing VideoView of a Peer.
     *
     * @param viewIndex
     * @return The index at which Peer was located, or a negative int if Peer Id was not found.
     */
    private void removePeerView(int viewIndex) {

        // Remove view
        if (viewIndex < videoViewLayouts.length && viewIndex > -1)
            videoViewLayouts[viewIndex].removeAllViews();
    }

    /**
     * Remove a remote video view from UI and shift the other videos to new positions
     *
     * @param removedIndex index of removed peer
     */
    private void removeRemotePeer(int removedIndex) {

        removePeerView(removedIndex);

        shiftUpRemotePeers(removedIndex);
    }

    /**
     * Shift remote Peers and their views, such that there are no empty
     * elements or UI between local Peer and the last remote Peer.
     */
    private void shiftUpRemotePeers(int removedIndex) {

        //shift all video to new positions
        for(int i=removedIndex; i<videoViewLayouts.length-1; i++){

            FrameLayout peerFrameLayout = videoViewLayouts[i+1];

            SurfaceViewRenderer view = (SurfaceViewRenderer) peerFrameLayout.getChildAt(0);

            if (view != null) {
                peerFrameLayout.removeAllViews();
                setLayoutParams(view);
                videoViewLayouts[i].addView(view);
            }
        }
    }
}