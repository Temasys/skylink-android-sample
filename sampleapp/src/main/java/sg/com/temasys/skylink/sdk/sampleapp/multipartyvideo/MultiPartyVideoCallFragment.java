package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.webrtc.SurfaceViewRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sg.com.temasys.skylink.sdk.rtc.Info;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static android.view.Gravity.FILL;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for display UI and get user interaction
 */
public class MultiPartyVideoCallFragment extends CustomActionBar implements MultiPartyVideoCallContract.View,
        OnClickListener, PopupMenu.OnMenuItemClickListener {

    private final String TAG = MultiPartyVideoCallFragment.class.getName();

    // presenter instance to implement app logic
    private MultiPartyVideoCallContract.Presenter mPresenter;

    // layout for displaying local video view
    private FrameLayout selfViewLayout;

    // layouts for displaying remote video views (3 remotes)
    private FrameLayout[] remoteViewLayouts;

    // menu option buttons for peers
    private Button btnOptionLocal, btnOptionPeer1, btnOptionPeer2, btnOptionPeer3;

    // the index of current selected peer
    private int currentSelectIndex = 0;

    // the flag to check WebRTC Stats is currently on or off
    // then we will display corresponding remote menu items
    private Map<Integer, Boolean> gettingStats = null;

    public static MultiPartyVideoCallFragment newInstance() {
        return new MultiPartyVideoCallFragment();
    }

    @Override
    public void setPresenter(MultiPartyVideoCallContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    //----------------------------------------------------------------------------------------------
    // Fragment life cycle methods
    //----------------------------------------------------------------------------------------------

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        super.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Allow volume to be controlled using volume keys
        ((MultiPartyVideoCallActivity) context).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[SA][MultiPartyVideoCallFragment][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_video_multiparty, container, false);

        // get the UI controls from layout
        getControlWidgets(rootView);

        // setup the action bar
        setActionBar();

        // setup init value for view controls
        initControls();

        //request an initiative connection
        requestViewLayout();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Toggle camera back to previous state if required.
        mPresenter.onViewRequestResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop local video source only if not changing orientation
        if (!((MultiPartyVideoCallActivity) context).isChangingConfigurations()) {
            mPresenter.onViewRequestPause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        // delegate to PermissionUtils to process the permissions
        mPresenter.onViewRequestPermissionsResult(requestCode, permissions, grantResults, TAG);
    }

    /**
     * Define the actions for buttons click events
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnBack:
                processBack();
                break;
            case R.id.btnLocalPeer:
                displayPeerInfo(0);
                break;
            case R.id.btnRemotePeer1:
                displayPeerInfo(1);
                break;
            case R.id.btnRemotePeer2:
                displayPeerInfo(2);
                break;
            case R.id.btnRemotePeer3:
                displayPeerInfo(3);
                break;
            case R.id.btnLocalPeerMenu:
                onMenuOptionLocalPeer(btnOptionLocal);
                break;
            case R.id.btnPeer1Menu:
                onMenuOptionRemotePeer(btnOptionPeer1, 1);
                break;
            case R.id.btnPeer2Menu:
                onMenuOptionRemotePeer(btnOptionPeer2, 2);
                break;
            case R.id.btnPeer3Menu:
                onMenuOptionRemotePeer(btnOptionPeer3, 3);
                break;
        }
    }

    /**
     * define the action for each menu items for each peer
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.switchCamera:
                mPresenter.onViewRequestSwitchCamera();
                break;
            case R.id.inputVideoRes:
                mPresenter.onViewRequestGetInputVideoResolution();
                break;
            case R.id.sentVideoRes:
                mPresenter.onViewRequestGetSentVideoResolution(currentSelectIndex);
                break;
            case R.id.receiveVideoRes:
                mPresenter.onViewRequestGetReceivedVideoResolution(currentSelectIndex);
                break;
            case R.id.webRtcStats:
                mPresenter.onViewRequestWebrtcStatsToggle(currentSelectIndex);
                gettingStats.put(currentSelectIndex, mPresenter.onViewRequestGetWebRtcStatsByPeerId(currentSelectIndex));
                break;
            case R.id.transferSpeed:
                mPresenter.onViewRequestGetTransferSpeeds(currentSelectIndex, Info.MEDIA_DIRECTION_BOTH, Info.MEDIA_ALL);
                break;
            case R.id.recordingStart:
                return mPresenter.onViewRequestStartRecording();
            case R.id.recordingStop:
                return mPresenter.onViewRequestStopRecording();
            case R.id.restart:
                refreshConnection(currentSelectIndex, false);
                break;
            case R.id.restartAll:
                refreshConnection(-1, false);
                break;
            case R.id.restartICE:
                refreshConnection(currentSelectIndex, true);
                break;
            case R.id.restartAllICE:
                refreshConnection(-1, true);
                break;
            default:
                Log.e(TAG, "Unknown menu option: " + menuItem.getItemId() + "!");
                return false;
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // change the context menu buttons of each peer
        locateMenuButtons();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Exit from room only if not changing orientation
        if (!((MultiPartyVideoCallActivity) context).isChangingConfigurations()) {
            //exit from room
            mPresenter.onViewRequestExit();

            //remove all views
            processEmptyLayout();
        }
    }

    //----------------------------------------------------------------------------------------------
    // Methods called from the Presenter to update UI
    //----------------------------------------------------------------------------------------------

    /**
     * Update information about room id on the action bar
     *
     * @param roomId
     */
    public void onPresenterRequestUpdateRoomInfo(String roomId) {
        updateRoomInfo(roomId);
    }

    /**
     * Show local peer button and display local avatar by the first character of the local username
     */
    @Override
    public void onPresenterRequestUpdateUIConnected(String localUserName) {
        // update the local peer button in the action bar
        updateUILocalPeer(localUserName);
    }

    /**
     * Update information about new remote peer joining the room at a specific index
     *
     * @param newPeer remote peer joining the room
     * @param index   specific index
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer, int index) {
        //add new remote peer button in the action bar
        updateUiRemotePeerJoin(newPeer, index);

        gettingStats.put(index, false);
    }

    /**
     * Display information about list of peers in room on the action bar
     *
     * @param peersList
     */
    @Override
    public void onPresenterRequestChangeUIRemotePeerLeft(int peerIndex, List<SkylinkPeer> peersList) {
        // re fill the peers buttons in the action bar to show the peer correctly order
        processFillPeers(peersList);

        gettingStats.put(peerIndex, false);
    }

    /**
     * Add or update local video view
     * The local view is always in the first position
     *
     * @param videoView videoView of remoteView
     */
    @Override
    public void onPresenterRequestAddSelfView(SurfaceViewRenderer videoView) {
        if (videoView == null) {
            return;
        }

        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(videoView);

        // Remove self view if its already added
        remoteViewLayouts[0].removeAllViews();

        // Add new local videoView to frame
        setLayoutParams(videoView);
        remoteViewLayouts[0].addView(videoView);

        displayPeerMenuOption(0);
    }

    /**
     * Add or update remote Peer's VideoView
     *
     * @param peerIndex  index for display videoView
     * @param remoteView videoView of remoteView
     */
    @Override
    public void onPresenterRequestAddRemoteView(int peerIndex, SurfaceViewRenderer remoteView) {
        if (remoteView == null || peerIndex < 1 || peerIndex > remoteViewLayouts.length)
            return;

        // Remove any existing Peer View at index.
        // This may sometimes be the case, for e.g. in screen sharing.
        removePeerView(peerIndex);

        // Add new remote videoView to frame at specific position
        setLayoutParams(remoteView);
        remoteViewLayouts[peerIndex].addView(remoteView);

        // display menu option button accordingly to peerIndex
        displayPeerMenuOption(peerIndex);
    }

    /**
     * Remove a remote video view from UI and shift the other videos to new positions
     *
     * @param viewIndex index of removed peer
     */
    @Override
    public void onPresenterRequestRemoveRemotePeer(int viewIndex) {
        // Remove video view at viewIndex
        removePeerView(viewIndex);

        // Shift all other views from right to left
        shiftUpRemotePeers(viewIndex);

        // hide the menu option button for peer also
        hidePeerMenuOption(viewIndex);
    }

    /**
     * Get the instance of the view for implementing runtime permission
     */
    @Override
    public Fragment onPresenterRequestGetFragmentInstance() {
        return this;
    }

    /**
     * Display the video link after recording the video
     *
     * @param recordingId
     * @param msg
     */
    @Override
    public void onPresenterRequestDisplayVideoLinkInfo(String recordingId, String msg) {
        // Create a clickable video link.
        final SpannableString videoLinkClickable = new SpannableString(msg);
        Linkify.addLinks(videoLinkClickable, Linkify.WEB_URLS);

        // Create TextView for video link.
        final TextView msgTxtView = new TextView(context);
        msgTxtView.setText(videoLinkClickable);
        msgTxtView.setMovementMethod(LinkMovementMethod.getInstance());

        // Create AlertDialog to present video link.
        AlertDialog.Builder videoLinkDialogBuilder = new AlertDialog.Builder(context);
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

        btnOptionLocal = rootView.findViewById(R.id.btnLocalPeerMenu);
        btnOptionPeer1 = rootView.findViewById(R.id.btnPeer1Menu);
        btnOptionPeer2 = rootView.findViewById(R.id.btnPeer2Menu);
        btnOptionPeer3 = rootView.findViewById(R.id.btnPeer3Menu);
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private void setActionBar() {
        ActionBar actionBar = ((MultiPartyVideoCallActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.custom_action_bar);
        View customBar = actionBar.getCustomView();

        // get the view controls in custom action bar by id
        btnBack = customBar.findViewById(R.id.btnBack);
        txtRoomName = customBar.findViewById(R.id.txtRoomName);
        txtRoomId = customBar.findViewById(R.id.txtRoomId);
        btnLocalPeer = customBar.findViewById(R.id.btnLocalPeer);
        btnRemotePeer1 = customBar.findViewById(R.id.btnRemotePeer1);
        btnRemotePeer2 = customBar.findViewById(R.id.btnRemotePeer2);
        btnRemotePeer3 = customBar.findViewById(R.id.btnRemotePeer3);
    }

    /**
     * Init the view widgets for the fragment
     */
    private void initControls() {
        // init setting value for room name in action bar
        txtRoomName.setText(Config.ROOM_NAME_PARTY);

        // set onClick event for buttons in layout
        btnBack.setOnClickListener(this);
        btnLocalPeer.setOnClickListener(this);
        btnRemotePeer1.setOnClickListener(this);
        btnRemotePeer2.setOnClickListener(this);
        btnRemotePeer3.setOnClickListener(this);

        btnOptionLocal.setOnClickListener(this);
        btnOptionPeer1.setOnClickListener(this);
        btnOptionPeer2.setOnClickListener(this);
        btnOptionPeer3.setOnClickListener(this);

        // display context menu button for each peer in correct position
        locateMenuButtons();

        // init the WebRTC Stats options of total 4 peers
        gettingStats = new HashMap<Integer, Boolean>(4);
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
                FrameLayout.LayoutParams.FILL_PARENT,
                FrameLayout.LayoutParams.FILL_PARENT, FILL);
        videoView.setLayoutParams(params);
    }

    /**
     * request info to display from presenter
     * try to connect to room if not connected
     * try to update UI if connected
     */
    private void requestViewLayout() {
        if (mPresenter != null) {
            mPresenter.onViewRequestConnectedLayout();
        }
    }

    /**
     * refresh the connection with the specific peer index
     *
     * @param peerIndex  the index of the peer to refresh with
     * @param iceRestart the option to allow refresh the ICE connection of the peer
     */
    private void refreshConnection(int peerIndex, boolean iceRestart) {
        if (mPresenter != null) {
            mPresenter.onViewRequestRefreshConnection(peerIndex, iceRestart);
        }
    }

    /**
     * Remove any existing VideoView of a Peer.
     *
     * @param viewIndex
     */
    private void removePeerView(int viewIndex) {
        // Remove all views at the viewIndex
        if (viewIndex < remoteViewLayouts.length && viewIndex > -1)
            remoteViewLayouts[viewIndex].removeAllViews();
    }

    /**
     * Shift remote Peers and their views, such that there are no empty
     * elements or UI between local Peer and the last remote Peer.
     */
    private void shiftUpRemotePeers(int removedIndex) {

        if (removedIndex < 0 || removedIndex >= remoteViewLayouts.length)
            return;

        //shift all video to new positions from right to left
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


    /**
     * Display the dialog of peer info including peer username and peer id
     * when the user click into the peer button in action bar
     */
    private void displayPeerInfo(int index) {
        SkylinkPeer peer = mPresenter.onViewRequestGetPeerByIndex(index);
        if (index == 0) {
            processDisplayLocalPeer(peer);
        } else {
            processDisplayRemotePeer(peer);
        }
    }

    /**
     * Display menu option button for peer in specific index
     */
    private void displayPeerMenuOption(int index) {
        switch (index) {
            case 0:
                btnOptionLocal.setVisibility(View.VISIBLE);
                break;
            case 1:
                btnOptionPeer1.setVisibility(View.VISIBLE);
                break;
            case 2:
                btnOptionPeer2.setVisibility(View.VISIBLE);
                break;
            case 3:
                btnOptionPeer3.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Hide menu option button for peer in specific index
     */
    private void hidePeerMenuOption(int index) {
        switch (index) {
            case 0:
                btnOptionLocal.setVisibility(View.GONE);
                break;
            case 1:
                btnOptionPeer1.setVisibility(View.GONE);
                break;
            case 2:
                btnOptionPeer2.setVisibility(View.GONE);
                break;
            case 3:
                btnOptionPeer3.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Display local peer menu option
     */
    private void onMenuOptionLocalPeer(View view) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.local_option_menu);
        popup.show();
    }

    /**
     * Display remote peer menu option
     */
    private void onMenuOptionRemotePeer(Button view, int peerIndex) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.setOnMenuItemClickListener(this);

        // need to check the current WebRTC Stats to display correct title for WebRTC Stats menu item
        // using different menu layouts for menu option WebRTC Stats
        if (gettingStats.get(peerIndex)) {
            popup.inflate(R.menu.remote_option_menu_on);
        } else {
            popup.inflate(R.menu.remote_option_menu_off);
        }
        popup.show();

        currentSelectIndex = peerIndex;
    }

    /**
     * put the context menu buttons in the correct positions
     */
    private void locateMenuButtons() {
        // get the screen width and height in pixels
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        // change button local peer option to the right top of local video view
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) btnOptionLocal.getLayoutParams();
        int buttonSize = (int) context.getResources().getDimension(R.dimen.dp_40dp);
        params.leftMargin = width / 2 - buttonSize;
        btnOptionLocal.setLayoutParams(params);

        // change button peer 2 option to the right top of remote peer 2 video view
        RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) btnOptionPeer2.getLayoutParams();
        params2.leftMargin = width / 2 - buttonSize;
        params2.bottomMargin = height / 2 - (buttonSize * 2);
        btnOptionPeer2.setLayoutParams(params2);

        // change button peer 3 option to the right top of remote peer 2 video view
        RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) btnOptionPeer3.getLayoutParams();
        params3.bottomMargin = height / 2 - (buttonSize * 2);
        btnOptionPeer3.setLayoutParams(params3);
    }
}