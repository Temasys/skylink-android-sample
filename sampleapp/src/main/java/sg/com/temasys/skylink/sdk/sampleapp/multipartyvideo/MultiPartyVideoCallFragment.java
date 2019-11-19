package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
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

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
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
    private Button btnOptionLocal, btnOptionPeer1, btnOptionPeer2, btnOptionPeer3, stopScreenshareFloat;

    //    private SurfaceViewRenderer localCameraView, localScreenView, localMainView;
    // The array of local video views (local camera, local screen sharing)
    private SurfaceViewRenderer[] localViews;

    // The array of remote video views (remote camera, remote screen sharing)
    // Up to 3 remote peers which can have 6 remote views
    private SurfaceViewRenderer[][] remoteViews;

    private boolean isLocalCameraDisplay = false;
    private boolean[] isRemoteCameraDisplay;

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

        // just in case that user are not sharing screen, then stop the camera
        if (localViews[1] == null || !isStopScreenShareBtnShowing) {
            presenter.onViewRequestResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // just in case that user are not sharing screen, then stop the camera
        if (localViews[1] == null || !isStopScreenShareBtnShowing) {
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
                processReturn();
                break;
            case R.id.btnLocalPeer:
                changeLocalPeerUI(true);
                displayPeerInfo(0);
                break;
            case R.id.btnRemotePeer1:
                changeRemotePeerUI(1, true);
                displayPeerInfo(1);
                break;
            case R.id.btnRemotePeer2:
                changeRemotePeerUI(2, true);
                displayPeerInfo(2);
                break;
            case R.id.btnRemotePeer3:
                changeRemotePeerUI(3, true);
                displayPeerInfo(3);
                break;
            case R.id.btnRemotePeer4:
                changeRemotePeerUI(4, true);
                displayPeerInfo(4);
                break;
            case R.id.btnRemotePeer5:
                changeRemotePeerUI(5, true);
                displayPeerInfo(5);
                break;
            case R.id.btnRemotePeer6:
                changeRemotePeerUI(6, true);
                displayPeerInfo(6);
                break;
            case R.id.btnRemotePeer7:
                changeRemotePeerUI(7, true);
                displayPeerInfo(7);
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
            case R.id.startAudio:
                presenter.onViewRequestStartAudio();
                break;
            case R.id.startVideo:
                presenter.onViewRequestStartVideo();
                break;
            case R.id.startScreen:
                presenter.onViewRequestStartVideoScreen();
                break;
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
            case R.id.sendingTransferSpeed:
                // TODO @Muoi need to update when SDK finished get stats by specific media track
                // currently the SDK is just able to get full stats for receiving track, no all mediaTypes or
                // media tracks will get the same stats
                presenter.onViewRequestGetTransferSpeeds(currentSelectIndex, SkylinkMedia.MediaType.VIDEO_CAMERA, true);
                break;
            case R.id.receivingTransferSpeed:
                // TODO @Muoi need to update when SDK finished get stats by specific media track
                // currently the SDK is just able to get full stats for receiving track, no all mediaTypes or
                // media tracks will get the same stats
                presenter.onViewRequestGetTransferSpeeds(currentSelectIndex, SkylinkMedia.MediaType.VIDEO_CAMERA, false);
                break;
            case R.id.recordingStart:
                presenter.onViewRequestStartRecording();
                break;
            case R.id.recordingStop:
                presenter.onViewRequestStopRecording();
                break;
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

        //Create an empty view with black background color
        if (Utils.isDefaultNoneVideoDeviceSetting()) {
            removePeerView(0);
//            selfViewLayout = new FrameLayout(context);
//            selfViewLayout.setBackgroundColor(getResources().getColor(R.color.color_black));
//            setLayoutParams(selfViewLayout);
//            selfViewLayout.addView(selfViewLayout);
            // display the context menu for local view
            displayPeerMenuOption(0);
        }
    }

    /**
     * Update the UI into disconnected state
     */
    @Override
    public void onPresenterRequestUpdateUIDisconnected() {
        updateRoomInfo(getResources().getString(R.string.guide_room_id));

        btnLocalPeer.setVisibility(GONE);
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
        if (index < 0 || index >= remoteViewLayouts.length)
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
     * @param peerList
     */
    @Override
    public void onPresenterRequestChangeUIRemotePeerLeft(int peerIndex, List<SkylinkPeer> peerList) {
        // re fill the peers buttons in the action bar to show the peer correctly order
        processFillPeers(peerList);
    }

    /**
     * Add or update local video view
     * The local view is always in the first position
     *
     * @param videoView videoView of remoteView
     */
    @Override
    public void onPresenterRequestAddSelfView(SurfaceViewRenderer videoView, SkylinkMedia.MediaType mediaType) {

        displayPeerMenuOption(0);

        if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            localViews[0] = videoView;
            isLocalCameraDisplay = true;
        } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            localViews[1] = videoView;
            isLocalCameraDisplay = false;
        }

        displayLocalView(videoView);
    }

    /**
     * Add or update remote Peer's VideoView
     *
     * @param peerIndex  index for display videoView
     * @param remoteView videoView of remoteView
     */
    @Override
    public void onPresenterRequestAddRemoteView(int peerIndex, SkylinkMedia.MediaType mediaType, SurfaceViewRenderer remoteView) {
        // display menu option button accordingly to peerIndex
        displayPeerMenuOption(peerIndex + 1);

        if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            remoteViews[peerIndex][0] = remoteView;
            isRemoteCameraDisplay[peerIndex] = true;
        } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            remoteViews[peerIndex][1] = remoteView;
            isRemoteCameraDisplay[peerIndex] = false;
        }

        displayRemoteView(peerIndex, remoteView);
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

        // hide the menu option button for peer also
        hidePeerMenuOption(viewIndex);

        // Shift all other views from right to left
        shiftUpRemotePeers(viewIndex);
    }

    /**
     * Get the instance of the view for implementing runtime permission
     */
    @Override
    public Fragment onPresenterRequestGetFragmentInstance() {
        return this;
    }

    @Override
    public void onPresenterRequestShowButtonStopScreenSharing() {
        if (isStopScreenShareBtnShowing) {
            return;
        }

        isStopScreenShareBtnShowing = true;

        WindowManager windowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        if (true) {
            windowManager.addView(stopScreenshareFloat, stopScreenshareLayoutParams);
        } else {
            windowManager.removeView(stopScreenshareFloat);
        }
    }

    @Override
    public void onPresenterRequestChangeDefaultVideoDevice(SkylinkConfig.VideoDevice videoDevice) {
        Config.setPrefString(Config.DEFAULT_VIDEO_DEVICE, videoDevice.getDeviceName(), (MultiPartyVideoCallActivity) context);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        selfViewLayout = rootView.findViewById(R.id.self_video);
        FrameLayout peer1Layout = rootView.findViewById(R.id.peer_1);
        FrameLayout peer2Layout = rootView.findViewById(R.id.peer_2);
        FrameLayout peer3Layout = rootView.findViewById(R.id.peer_3);

        remoteViewLayouts = new FrameLayout[]{peer1Layout, peer2Layout, peer3Layout};

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
        btnRemotePeer4.setOnClickListener(this);
        btnRemotePeer5.setOnClickListener(this);
        btnRemotePeer6.setOnClickListener(this);
        btnRemotePeer7.setOnClickListener(this);

        btnOptionLocal.setOnClickListener(this);
        btnOptionPeer1.setOnClickListener(this);
        btnOptionPeer2.setOnClickListener(this);
        btnOptionPeer3.setOnClickListener(this);

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

        stopScreenshareFloat.setOnClickListener(view -> {
            isStopScreenShareBtnShowing = false;
            showHideButton(stopScreenshareFloat, false);
            onPresenterRequestAddSelfView(localViews[0], SkylinkMedia.MediaType.VIDEO_CAMERA);

            presenter.onViewRequestStopScreenSharing();
        });

        // init views array
        localViews = new SurfaceViewRenderer[2];
        remoteViews = new SurfaceViewRenderer[3][2];
        isRemoteCameraDisplay = new boolean[3];

        // click on the view to switch between camera view and screen sharing view
        selfViewLayout.setOnClickListener(view -> switchLocalView());

        // setup click event for remoteViewLayouts
        for (int index = 0; index < remoteViewLayouts.length; index++) {
            int finalIndex = index;
            remoteViewLayouts[finalIndex].setOnClickListener(view -> switchRemoteView(finalIndex));
        }
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
            List<SurfaceViewRenderer> videoViews = presenter.onViewRequestGetVideoViewByIndex(i);

            if (videoViews != null && videoViews.size() > 0) {
                for (SurfaceViewRenderer videoView : videoViews) {
                    Utils.removeViewFromParent(videoView);
                }
            }
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
            // start a local video base on default device setting
            presenter.onViewRequestStartLocalMediaIfConfigAllow();
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

            hidePeerMenuOption(i + 1);

            SurfaceViewRenderer view = (SurfaceViewRenderer) peerFrameLayout.getChildAt(0);

            if (view != null) {
                peerFrameLayout.removeAllViews();
                setLayoutParams(view);
                remoteViewLayouts[i].addView(view);

                showPeerMenuOption(i);
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
                btnOptionPeer1.setVisibility(View.GONE);
                break;
            case 1:
                btnOptionPeer2.setVisibility(View.GONE);
                break;
            case 2:
                btnOptionPeer3.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Show menu option button for peer in specific index
     */
    private void showPeerMenuOption(int index) {
        switch (index) {
            case 0:
                btnOptionPeer1.setVisibility(VISIBLE);
                break;
            case 1:
                btnOptionPeer2.setVisibility(VISIBLE);
                break;
            case 2:
                btnOptionPeer3.setVisibility(VISIBLE);
                break;
        }
    }

    /**
     * Display local peer menu option
     * Base on the default setting for video device, we inflate the different menu layout accordingly
     */
    private void onMenuOptionLocalPeer(View view) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.setOnMenuItemClickListener(this);

        popup.inflate(R.menu.local_view_option_menu_1);

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

    /**
     * Display local video view, using both in local camera and local screen sharing
     */
    private void displayLocalView(SurfaceViewRenderer videoView) {
        if (videoView == null) {
            return;
        }

        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(videoView);

        selfViewLayout.removeAllViews();

        setLayoutParams(videoView);

        selfViewLayout.addView(videoView);
    }

    /**
     * Display remote video view, using both in remote camera and remote screen sharing at a specific index
     */
    private void displayRemoteView(int peerIndex, SurfaceViewRenderer remoteView) {
        if (remoteView == null || peerIndex < 0 || peerIndex >= remoteViewLayouts.length)
            return;

        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(remoteView);

        remoteViewLayouts[peerIndex].removeAllViews();

        setLayoutParams(remoteView);

        remoteViewLayouts[peerIndex].addView(remoteView);
    }

    /**
     * Switch between local camera view and local screen sharing
     */
    private void switchLocalView() {
        if (localViews == null || localViews[0] == null || localViews[1] == null) {
            return;
        }

        if (isLocalCameraDisplay && localViews[1] != null) {
            displayLocalView(localViews[1]);
        } else if (!isLocalCameraDisplay && localViews[0] != null) {
            displayLocalView(localViews[0]);
        }

        isLocalCameraDisplay = !isLocalCameraDisplay;

    }

    /**
     * switch between remote camera view and remote screen sharing at a specific index
     */
    private void switchRemoteView(int index) {
        if (remoteViews == null || remoteViews[index][0] == null || remoteViews[index][1] == null)
            return;

        if (isRemoteCameraDisplay[index] && remoteViews[index][1] != null) {
            displayRemoteView(index, remoteViews[index][1]);
        } else if (!isRemoteCameraDisplay[index] && remoteViews[index][0] != null) {
            displayRemoteView(index, remoteViews[index][0]);
        }

        isRemoteCameraDisplay[index] = !isRemoteCameraDisplay[index];
    }

    /**
     * process exit the demo when people press on back button in the menu
     */
    private void processReturn() {
        presenter.onViewRequestExit();
        processBack();
    }
}