package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.app.AlertDialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
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
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static android.view.Gravity.CENTER;

/**
 * Created by muoi.pham on 20/07/18.
 */
public class MultiPartyVideoCallFragment extends Fragment implements MultiPartyVideoCallContract.View {

    private final String TAG = MultiPartyVideoCallFragment.class.getName();

    private Context mContext;

    private MultiPartyVideoCallContract.Presenter mPresenter;

    private FrameLayout selfViewLayout;

    private FrameLayout[] remoteViewLayouts;

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
        for (int peerIndex = 0; peerIndex < remoteViewLayouts.length; ++peerIndex) {
            FrameLayout frameLayout = remoteViewLayouts[peerIndex];
            if (frameLayout == selfViewLayout) {
                // Show room and self info, plus give option to
                // switch self view between different cameras (if any).
                frameLayout.setOnClickListener(v -> {
                    String name = mPresenter.onViewRequestGetRoomPeerIdNick();

                    TextView selfTV = new TextView(mContext);
                    selfTV.setText(name);
                    selfTV.setTextIsSelectable(true);
                    AlertDialog.Builder selfDialogBuilder =
                            new AlertDialog.Builder(mContext);
                    selfDialogBuilder.setView(selfTV);
                    selfDialogBuilder.setPositiveButton("OK", null);
                    // Get the input video resolution.
                    selfDialogBuilder.setPositiveButton("Input video resolution",
                            (dialog, which) -> mPresenter.onViewRequestGetInputVideoResolution());

                    selfDialogBuilder.setNegativeButton("Switch Camera",
                            (dialog, which) -> {
                                mPresenter.onViewRequestSwitchCamera();
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

        mPresenter.onViewRequestResume();
    }

    @Override
    public void onPause() {

        super.onPause();

        // Stop local video source only if not changing orientation
        if (!((MultiPartyVideoCallActivity) mContext).isChangingConfigurations()) {
            mPresenter.onViewRequestPause();
        }
    }

    @Override
    public void onDetach() {

        super.onDetach();
        if (!((MultiPartyVideoCallActivity) mContext).isChangingConfigurations()) {
            mPresenter.onViewRequestExit();
            processEmptyLayout();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        mPresenter.onViewRequestPermissionsResult(requestCode, permissions, grantResults, TAG);
    }

    @Override
    public void onPresenterRequestAddSelfView(SurfaceViewRenderer videoView) {
        addSelfView(videoView);
    }

    @Override
    public void onPresenterRequestAddRemoteView(int peerIndex, SurfaceViewRenderer remoteView) {
        addRemoteView(peerIndex, remoteView);
    }

    @Override
    public void onPresenterRequestRemoveRemotePeer(int viewIndex) {
        removeRemotePeer(viewIndex);
    }

    @Override
    public Fragment onPresenterRequestGetFragmentInstance() {
        return this;
    }

    @Override
    public void onPresenterRequestDisplayVideoLinkInfo(String recordingId, String msg) {
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
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        selfViewLayout = rootView.findViewById(R.id.self_video);
        FrameLayout peer1Layout = rootView.findViewById(R.id.peer_1);
        FrameLayout peer2Layout = rootView.findViewById(R.id.peer_2);
        FrameLayout peer3Layout = rootView.findViewById(R.id.peer_3);

        remoteViewLayouts = new FrameLayout[]{selfViewLayout, peer1Layout, peer2Layout, peer3Layout};
    }

    private void setActionBar() {
        ActionBar actionBar = ((MultiPartyVideoCallActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setTitle(Config.ROOM_NAME_PARTY);
        setHasOptionsMenu(true);
    }

    /**
     * Remove all videoViews from layouts.
     */
    private void processEmptyLayout() {
        int totalInRoom = mPresenter.onViewRequestGetTotalInRoom();

        for (int i = 0; i < totalInRoom; i++) {
            SurfaceViewRenderer videoView = mPresenter.onViewRequestGetVideoViewByIndex(i);

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
                                mPresenter.onViewRequestGetSentVideoResolution(peerIndex);
                                return true;

                            case R.id.vid_res_recv:
                                mPresenter.onViewRequestGetReceivedVideoResolution(peerIndex);
                                return true;

                            case R.id.webrtc_stats:
                                mPresenter.onViewRequestWebrtcStatsToggle(peerIndex);
                                return true;

                            case R.id.transfer_speed:
                                mPresenter.onViewRequestGetTransferSpeeds(peerIndex, Info.MEDIA_DIRECTION_BOTH, Info.MEDIA_ALL);
                                return true;

                            case R.id.recording_start:
                                return mPresenter.onViewRequestStartRecording();

                            case R.id.recording_stop:
                                return mPresenter.onViewRequestStopRecording();

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
            String title = mPresenter.onViewRequestGetRoomPeerIdNick();

            PopupMenu popupMenu = new PopupMenu(mContext, v);
            popupMenu.setOnMenuItemClickListener(clickListener);

            popupMenu.getMenu().add(title);
            // Populate actions of Popup Menu.

            popupMenu.getMenu().add(0, R.id.vid_res_sent, 0, R.string.vid_res_sent);
            popupMenu.getMenu().add(0, R.id.vid_res_recv, 0, R.string.vid_res_recv);

            String statsStr = getString(R.string.webrtc_stats);
            final Boolean gettingStats = mPresenter.onViewRequestGetWebRtcStatsByPeerId(peerIndex);
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

            if (peerIndex < mPresenter.onViewRequestGetTotalInRoom())
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
            mPresenter.onViewRequestConnectedLayout();
        }
    }

    private void addSelfView(SurfaceViewRenderer videoView) {
        if (videoView == null) {
            return;
        }

        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(videoView);

        // Remove self view if its already added
        remoteViewLayouts[0].removeAllViews();

        setLayoutParams(videoView);
        remoteViewLayouts[0].addView(videoView);
    }


    private void refreshConnection(int peerIndex, boolean iceRestart) {
        mPresenter.onViewRequestRefreshConnection(peerIndex, iceRestart);
    }

    /**
     * Add or update remote Peer's VideoView into the app.
     *
     * @param index           index for display videoView
     * @param remoteVideoView videoView of remoteView
     */
    private void addRemoteView(int index, SurfaceViewRenderer remoteVideoView) {

        if (remoteVideoView == null || index < 1 || index > remoteViewLayouts.length)
            return;

        // Remove any existing Peer View at index.
        // This may sometimes be the case, for e.g. in screen sharing.
        removePeerView(index);


        setLayoutParams(remoteVideoView);
        remoteViewLayouts[index].addView(remoteVideoView);
    }

    /**
     * Remove any existing VideoView of a Peer.
     *
     * @param viewIndex
     * @return The index at which Peer was located, or a negative int if Peer Id was not found.
     */
    private void removePeerView(int viewIndex) {

        // Remove view
        if (viewIndex < remoteViewLayouts.length && viewIndex > -1)
            remoteViewLayouts[viewIndex].removeAllViews();
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

        if (removedIndex < 0 || removedIndex >= remoteViewLayouts.length)
            return;

        //shift all video to new positions
        for (int i = removedIndex; i < remoteViewLayouts.length - 1; i++) {

            FrameLayout peerFrameLayout = remoteViewLayouts[i + 1];

            SurfaceViewRenderer view = (SurfaceViewRenderer) peerFrameLayout.getChildAt(0);

            if (view != null) {
                peerFrameLayout.removeAllViews();
                setLayoutParams(view);
                remoteViewLayouts[i].addView(view);
            }
        }
    }
}