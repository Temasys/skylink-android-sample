package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

import sg.com.temasys.skylink.sdk.rtc.Info;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.DoubleClickListener;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static android.view.Gravity.FILL;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for display UI and get user interaction
 */
public class MultiPartyVideoCallFragment extends CustomActionBar implements MultiPartyVideoCallContract.View,
        OnClickListener, PopupMenu.OnMenuItemClickListener {

    private final String TAG = MultiPartyVideoCallFragment.class.getName();

    // presenter instance to implement app logic
    private MultiPartyVideoCallContract.Presenter presenter;

    // layout for displaying local video view
    private FrameLayout selfViewLayout;

    // layouts for displaying remote video views (3 remotes)
    private FrameLayout[] remoteViewLayouts;

    // menu option buttons for peers
    private Button btnOptionLocal, btnOptionPeer1, btnOptionPeer2, btnOptionPeer3, btnStartAudioMulti,
            btnStartVideoMulti, stopScreenshareFloat;

    private SurfaceViewRenderer localCameraView, localScreenView;

    private WindowManager.LayoutParams stopScreenshareLayoutParams;

    // the index of current selected peer
    private int currentSelectIndex = 0;

    private boolean isStopScreenShareBtnShowing;

    public static MultiPartyVideoCallFragment newInstance() {
        return new MultiPartyVideoCallFragment();
    }

    @Override
    public void setPresenter(MultiPartyVideoCallContract.Presenter presenter) {
        this.presenter = presenter;
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
        presenter.onViewRequestResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop local video source only if not changing orientation
        if (!((MultiPartyVideoCallActivity) context).isChangingConfigurations()) {
            presenter.onViewRequestPause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        // delegate to PermissionUtils to process the permissions
        presenter.onViewRequestPermissionsResult(requestCode, permissions, grantResults);
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
            case R.id.btnStartAudioMulti:
                presenter.onViewRequestStartAudio();
                break;
            case R.id.btnStartVideoMulti:
                presenter.onViewRequestStartVideo();
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
                presenter.onViewRequestSwitchCamera();
                break;
            case R.id.inputVideoRes:
                presenter.onViewRequestGetInputVideoResolution();
                break;
            case R.id.sentVideoRes:
                presenter.onViewRequestGetSentVideoResolution(currentSelectIndex);
                break;
            case R.id.receiveVideoRes:
                presenter.onViewRequestGetReceivedVideoResolution(currentSelectIndex);
                break;
            case R.id.webRtcStats:
                presenter.onViewRequestWebrtcStatsToggle(currentSelectIndex);
                break;
            case R.id.transferSpeed:
                presenter.onViewRequestGetTransferSpeeds(currentSelectIndex, Info.MEDIA_DIRECTION_BOTH, Info.MEDIA_ALL);
                break;
            case R.id.recordingStart:
                return presenter.onViewRequestStartRecording();
            case R.id.recordingStop:
                return presenter.onViewRequestStopRecording();
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
            presenter.onViewRequestExit();

            //remove all views
            processEmptyLayout();


            if (isStopScreenShareBtnShowing) {
                showHideButton(stopScreenshareFloat, false);
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Methods called from the Presenter to update UI
    //----------------------------------------------------------------------------------------------

    /**
     * Update GUI into connected state when connected to room
     * Show room id and local peer button and display local avatar by the first character of the local username
     *
     * @param roomId the id of the connected room that generated by SDK
     */
    @Override
    public void onPresenterRequestUpdateUIConnected(String roomId) {
        updateRoomInfo(roomId);
        // update the local peer button in the action bar
        updateUILocalPeer(Config.USER_NAME_PARTY);

        // Remove any existing Peer View at index.
        // This may sometimes be the case, for e.g. in screen sharing.
        removePeerView(0);

        //Create an empty view with black background color
        selfViewLayout = new FrameLayout(context);
        selfViewLayout.setBackgroundColor(getResources().getColor(R.color.color_black));
        setLayoutParams(selfViewLayout);
        remoteViewLayouts[0].addView(selfViewLayout);
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

        // add emty black view to layout
        if (index < 1 || index > remoteViewLayouts.length)
            return;

        // Remove any existing Peer View at index.
        // This may sometimes be the case, for e.g. in screen sharing.
        removePeerView(index);

        //Create an empty view with black background color
        View remoteVideoView = new FrameLayout(context);
        remoteVideoView.setBackgroundColor(getResources().getColor(R.color.color_black));

        // Add new remote videoView to frame at specific position
        setLayoutParams(remoteVideoView);
        remoteViewLayouts[index].addView(remoteVideoView);
    }

    /**
     * Update information about remote peer left the room
     * Re-fill the peers list in order to display correct order of peers in room
     *
     * @param peersList
     */
    @Override
    public void onPresenterRequestChangeUIRemotePeerLeft(int peerIndex, List<SkylinkPeer> peersList) {
        // re fill the peers buttons in the action bar to show the peer correctly order
        processFillPeers(peersList);
    }

    /**
     * Add or update local video view
     * The local view is always in the first position
     *
     * @param videoView videoView of remoteView
     */
    @Override
    public void onPresenterRequestAddSelfView(SurfaceViewRenderer videoView, SkylinkMedia.MEDIA_TYPE mediaType) {
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

        btnStartAudioMulti.setVisibility(View.GONE);
        btnStartVideoMulti.setVisibility(View.GONE);

        if (mediaType == SkylinkMedia.MEDIA_TYPE.VIDEO_CAMERA) {
            localCameraView = videoView;
        } else if (mediaType == SkylinkMedia.MEDIA_TYPE.VIDEO_SCREEN) {
            localScreenView = videoView;
        }
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

    @Override
    public void onPresenterRequestShowHideButtonStopScreenSharing(boolean isShow) {
        isStopScreenShareBtnShowing = isShow;

        WindowManager windowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        if (isShow) {
            windowManager.addView(stopScreenshareFloat, stopScreenshareLayoutParams);
        } else {
            windowManager.removeView(stopScreenshareFloat);
        }
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

        btnStartAudioMulti = rootView.findViewById(R.id.btnStartAudioMulti);
        btnStartVideoMulti = rootView.findViewById(R.id.btnStartVideoMulti);
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private void setActionBar() {
        ActionBar actionBar = ((MultiPartyVideoCallActivity) getActivity()).getSupportActionBar();
        super.setActionBar(actionBar);
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

        btnStartAudioMulti.setOnClickListener(this);
        btnStartVideoMulti.setOnClickListener(this);

        // display context menu button for each peer in correct position
        locateMenuButtons();

        isStopScreenShareBtnShowing = false;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            stopScreenshareLayoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        } else {
            stopScreenshareLayoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }

        stopScreenshareLayoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

        stopScreenshareFloat = new Button(getActivity());
        stopScreenshareFloat.setText(getActivity().getResources().getText(R.string.stop_screenShare));
        stopScreenshareFloat.setTextColor(getActivity().getResources().getColor(R.color.color_white));
        stopScreenshareFloat.setTextSize(getActivity().getResources().getDimension(R.dimen.sp_4sp));
        stopScreenshareFloat.setBackground(getActivity().getResources().getDrawable(R.drawable.button_stop_screen_sharing));

        stopScreenshareFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isStopScreenShareBtnShowing = false;
                showHideButton(stopScreenshareFloat, false);
                onPresenterRequestAddSelfView(localCameraView, SkylinkMedia.MEDIA_TYPE.VIDEO_CAMERA);
            }
        });

        // setup double click event for selfViewLayout
        // double click on selfViewLayout to start second local video view
        selfViewLayout.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onDoubleClick() {
                startSecondVideoView();
            }
        });
    }

    /**
     * Start the second video view
     * if current video view is from camera, then start screen video view
     * if current video view is from screen view, then start camera video view
     */
    private void startSecondVideoView() {
        presenter.onViewRequestStartSecondVideoView();
    }

    /**
     * Remove all videoViews from layouts.
     */
    private void processEmptyLayout() {
        int totalInRoom = presenter.onViewRequestGetTotalInRoom();

        for (int i = 0; i < totalInRoom; i++) {
            SurfaceViewRenderer videoView = presenter.onViewRequestGetVideoViewByIndex(i);

            if (videoView != null)
                Utils.removeViewFromParent(videoView);
        }
    }

    /**
     * Set LayoutParams for a VideoView to fit within it's containing FrameLayout, in the center.
     *
     * @param videoView
     */
    private void setLayoutParams(View videoView) {
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
        if (presenter != null) {
            presenter.onViewRequestConnectedLayout();
        }
    }

    /**
     * refresh the connection with the specific peer index
     *
     * @param peerIndex  the index of the peer to refresh with
     * @param iceRestart the option to allow refresh the ICE connection of the peer
     */
    private void refreshConnection(int peerIndex, boolean iceRestart) {
        if (presenter != null) {
            presenter.onViewRequestRefreshConnection(peerIndex, iceRestart);
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
        SkylinkPeer peer = presenter.onViewRequestGetPeerByIndex(index);
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
        Boolean startOn = presenter.onViewRequestGetWebRtcStatsState(peerIndex);
        if (startOn != null && startOn) {
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

    private void showHideButton(View view, boolean isShow) {
        if (view == null)
            return;

        if (view == stopScreenshareFloat) {
            WindowManager windowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
            if (isShow) {
                windowManager.addView(stopScreenshareFloat, stopScreenshareLayoutParams);
            } else {
                windowManager.removeView(stopScreenshareFloat);
            }
        } else {
            if (isShow) {
                view.setVisibility(VISIBLE);
            } else {
                view.setVisibility(GONE);
            }
        }
    }
}