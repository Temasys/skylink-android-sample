package sg.com.temasys.skylink.sdk.sampleapp.screensharing;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.VIDEO_TYPE;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.VideoResButton;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * A simple {@link CustomActionBar} subclass.
 * This class is responsible for display UI and get user interaction
 */
public class ScreenSharingFragment extends CustomActionBar implements ScreenSharingContract.MainView,
        View.OnClickListener {

    private final String TAG = ScreenSharingFragment.class.getName();

    // view widgets
    private LinearLayout videoViewLayout;
    private FloatingActionButton btnDisconnect, btnOption, btnAudioMute, btnVideoMute, btnCameraMute,
            btnSpeaker, btnScreenShareAction;
    private Button stopScreenshareFloat, btnFullScreen;
    private VideoResButton btnVideoRes;
    private WindowManager.LayoutParams stopScreenshareLayoutParams;

    // presenter instance to implement screen sharing logic
    private ScreenSharingContract.Presenter presenter;

    // local variables to check views' state
    private boolean isShowVideoOption = false;
    private boolean isShowScreenSharing = false;
    private boolean isFullScreen = false;
    private boolean isShowVideoRes = false;
    private SurfaceViewRenderer localCameraView, localScreenView, remoteCameraView,
            remoteScreenView, currentMainView;

    private boolean isShareScreen = false;
    private boolean isShareScreenFirstTime = true;


    public static ScreenSharingFragment newInstance() {
        return new ScreenSharingFragment();
    }

    @Override
    public void setPresenter(ScreenSharingContract.Presenter presenter) {
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
        ((ScreenSharingActivity) context).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[SA][ScreenShare][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_screen_share, container, false);

        // get the UI controls from layout
        getControlWidgets(rootView);

        // setup the action bar
        setActionBar();

        // init the UI controls
        initComponents();

        //request an initiative connection
        requestViewLayout();

        showVideoOption();
//        processShareScreen();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
//        presenter.onViewRequestResume();
    }

    @Override
    public void onPause() {
        super.onPause();
//        presenter.onViewRequestPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        // permission results from app
        if (requestCode == PermissionUtils.REQUEST_BUTTON_OVERLAY_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // show the screen sharing overlay button
                showHideButtonStopScreenSharing();
            } else {
                // permission denied, warning the user
                presenter.onViewRequestPermissionDeny();
            }
        } else { // permission results from SDK
            presenter.onViewRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onClick(View view) {
        //Defining a click event actions for the buttons
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
            case R.id.toggle_speaker_screen_share:
                presenter.onViewRequestChangeAudioOutput();
                break;
            case R.id.toggle_audio_screen_share:
                presenter.onViewRequestChangeAudioState();
                break;
            case R.id.toggle_video_screen_share:
                presenter.onViewRequestChangeVideoState();
                break;
            case R.id.toggle_camera_screen_share:
                presenter.onViewRequestChangeCameraState();
                break;
            case R.id.btn_option_video:
                showVideoOption();
                break;
            case R.id.btn_screen_share_action:
                processShareScreen();
                break;
            case R.id.btn_disconnect_screen_share:
                presenter.onViewRequestDisconnectFromRoom();
                onPresenterRequestDisconnectUIChange();
                break;
            case R.id.btn_full_screen:
                processFullScreen();
                break;
            case R.id.btn_video_res_screen_share:
                showHideVideoResolution();
                break;
            case R.id.ll_screen_share:
                showHideVideoResolution(false);
                break;
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        // only exit/disconnect from room when it is chosen by user
        // not changing configuration
        if (!((ScreenSharingActivity) context).isChangingConfigurations()) {
            presenter.onViewRequestExit();

            if (isShowScreenSharing) {
                WindowManager windowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
                windowManager.removeView(stopScreenshareFloat);
            }
        }


    }

    //----------------------------------------------------------------------------------------------
    // Methods invoked from the Presenter to update UI
    //----------------------------------------------------------------------------------------------

    /**
     * Update UI into connected state when connected to room
     * Show room id and local peer button and display local avatar by the first character of the local username
     *
     * @param roomId the id of the connected room that generated by SDK
     */
    @Override
    public void onPresenterRequestUpdateUIConnected(String roomId) {
        updateRoomInfo(roomId);
        // update the local peer avatar with the user name configured in default setting
        updateUILocalPeer(Config.USER_NAME_VIDEO);
    }

    /**
     * Update information about new remote peer joining the room at a specific index
     *
     * @param newPeer remote peer joining the room
     * @param index   specific index
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer, int index) {
        updateUiRemotePeerJoin(newPeer, index);

        // change the local view
        ((ScreenSharingActivity) getActivity()).onShowHideLocalCameraViewFragment(true, false);

        moveMainViewToSmallLocalCameraView(currentMainView);
    }

    /**
     * Update information about remote peer left the room
     * Re-fill the peers list in order to display correct order of peers in room
     *
     * @param peersList the list of left peer(s) in the room
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerLeft(List<SkylinkPeer> peersList) {
        processFillPeers(peersList);

        // move small view to main view following order: remote peer screen, remote peer camera
        // local peer screen, local peer camera
        addViewToMain(localCameraView);
    }

    /**
     * Change certain UI elements when disconnecting from room.
     */
    @Override
    public void onPresenterRequestDisconnectUIChange() {
        View self = videoViewLayout.findViewWithTag("self");
        if (self != null) {
            videoViewLayout.removeView(self);
        }

        View peer = videoViewLayout.findViewWithTag("peer");
        if (peer != null) {
            videoViewLayout.removeView(peer);
        }

        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    /**
     * Get the instance of the view for implementing runtime permission
     */
    @Override
    public Fragment onPresenterRequestGetFragmentInstance() {
        return this;
    }

    /**
     * Add or update our self VideoView into the view layout when local peer connected to room and
     * local video view is ready
     *
     * @param videoView local video view
     */
    @Override
    public void onPresenterRequestAddCameraSelfView(SurfaceViewRenderer videoView) {
        //save localCameraView
        localCameraView = videoView;

        addViewToMain(videoView);
    }

    /**
     * Add or update our self VideoView into the view layout when local peer connected to room and
     * local video view is ready
     *
     * @param videoView local video view
     */
    @Override
    public void onPresenterRequestAddScreenSelfView(SurfaceViewRenderer videoView) {
        //save localScreenView
        localScreenView = videoView;

        addViewToMain(videoView);

        // get the current view to move to correct small view
        // just for testing
        moveMainViewToSmallLocalCameraView(localCameraView);
    }

    private void addViewToMain(SurfaceViewRenderer videoView) {
        if (videoView == null) {
            String log = "[SA][addViewToMain] Not adding self view as videoView is null!";
            Log.d(TAG, log);
            return;
        }

        // If previous self video exists,
        // Set new video to size of previous self video
        // And remove old self video.
        View self = videoViewLayout.findViewWithTag("main");
        if (self != null) {
            // Remove the old self video.
            videoViewLayout.removeView(self);
        }

        // Tag new video as self and add onClickListener.
        videoView.setTag("main");

        // Show new video on screen
        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(videoView);

        // And new self video.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        params.gravity = Gravity.CENTER;
        params.weight = 1;
        videoView.setLayoutParams(params);

        //alway set self video as vertical orientation
        videoViewLayout.setOrientation(LinearLayout.VERTICAL);

        videoViewLayout.addView(videoView);

        currentMainView = videoView;
    }

    /**
     * Add or update remote Peer's VideoView into the view layout when remote peer joined the room
     *
     * @param remoteVideoView
     */
    @Override
    public void onPresenterRequestAddRemoteView(SurfaceViewRenderer remoteVideoView) {
        // add remote view to main layout

        if (remoteVideoView == null) {
            String log = "[SA][addRemoteView] Not adding remote view as videoView is null!";
            Log.d(TAG, log);
            return;
        }

        // If previous self video exists,
        // Set new video to size of previous self video
        // And remove old self video.
        View main = videoViewLayout.findViewWithTag("main");
        if (main != null) {
            // Remove the old self video.
            videoViewLayout.removeView(main);
        }

        // Tag new video as self and add onClickListener.
        remoteVideoView.setTag("main");

        // Show new video on screen
        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(remoteVideoView);

        // And new self video.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        params.gravity = Gravity.CENTER;
        params.weight = 1;
        remoteVideoView.setLayoutParams(params);

        //alway set self video as vertical orientation
        videoViewLayout.setOrientation(LinearLayout.VERTICAL);

        videoViewLayout.addView(remoteVideoView);

        remoteCameraView = remoteVideoView;

        currentMainView = remoteCameraView;

        // update the localCameraView to display on top of the layout
        if (isShareScreen) {
            moveMainViewToSmallLocalCameraView(localScreenView);
        } else {
            moveMainViewToSmallLocalCameraView(localCameraView);
        }
    }

//    /**
//     * Add or update remote Peer's VideoView into the view layout when remote peer joined the room
//     *
//     * @param remoteScreenView
//     */
//    @Override
//    public void onPresenterRequestAddScreenView(SurfaceViewRenderer remoteScreenView) {
//        // add remote view to main layout
//
//        if (remoteScreenView == null) {
//            String log = "[SA][addRemoteView] Not adding remote view as videoView is null!";
//            Log.d(TAG, log);
//            return;
//        }
//
//        // Tag new video as self and add onClickListener.
//        remoteScreenView.setTag("screen");
//
//        // If peer video exists, remove it first.
//        View peer = videoViewLayout.findViewWithTag("screen");
//        if (peer != null) {
//            videoViewLayout.removeView(peer);
//        }
//
//        // Show new video on screen
//        // Remove video from previous parent, if any.
//        Utils.removeViewFromParent(remoteScreenView);
//
//        // And new self video.
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
//        params.gravity = Gravity.CENTER;
//        params.weight = 1;
//        remoteScreenView.setLayoutParams(params);
//
//        //alway set self video as vertical orientation
//        videoViewLayout.setOrientation(LinearLayout.VERTICAL);
//
//        videoViewLayout.addView(remoteScreenView);
//
//        this.remoteScreenView = remoteScreenView;
//
//        currentMainView = remoteScreenView;
//
//        // update the localCameraView to display on top of the layout
//        moveMainViewToSmallView2(remoteCameraView);
//    }

    /**
     * Remove remote video view when remote peer left or local peer exit the room
     * <p>
     * Change layout orientation to Vertical when there is only 1 local video view
     */
    @Override
    public void onPresenterRequestRemoveRemotePeer() {
        View peerView = videoViewLayout.findViewWithTag("peer");
        videoViewLayout.removeView(peerView);

        //change orientation to vertical
        videoViewLayout.setOrientation(LinearLayout.VERTICAL);
    }

    /**
     * Update UI (display toast info) about remote audio state
     *
     * @param isAudioMuted remote audio state
     * @param isToast      display toast or not
     */
    @Override
    public void onPresenterRequestUpdateAudioState(boolean isAudioMuted, boolean isToast) {
        if (isAudioMuted) {
            if (isToast) {
                String log = getString(R.string.muted_audio);
                toastLog(TAG, context, log);
            }
        } else {
            if (isToast) {
                String log = getString(R.string.enabled_audio);
                toastLog(TAG, context, log);
            }
        }
    }

    /**
     * Update UI (display toast info) about remote video state
     *
     * @param isVideoMuted remote video state
     * @param isToast      display toast or not
     */
    @Override
    public void onPresenterRequestUpdateVideoState(boolean isVideoMuted, boolean isToast) {
        if (isVideoMuted) {
            if (isToast) {
                String log = getString(R.string.muted_video);
                toastLog(TAG, context, log);
            }
        } else {
            if (isToast) {
                String log = getString(R.string.enabled_video);
                toastLog(TAG, context, log);
            }
        }
    }

    /**
     * Update the audio output/speaker button when being requested from presenter
     * For example: when the phone connects to a bluetooth headset, the speaker is automatically off
     */
    @Override
    public void onPresenterRequestChangeAudioOuput(boolean isSpeakerOn) {
        if (isSpeakerOn) {
            btnSpeaker.setImageResource(R.drawable.ic_audio_speaker);
        } else {
            btnSpeaker.setImageResource(R.drawable.icon_speaker_mute);
        }
    }

    /**
     * Update the audio button UI when changing audio state
     */
    @Override
    public void onPresenterRequestChangeAudioUI(boolean isAudioMute) {
        if (isAudioMute) {
            btnAudioMute.setImageResource(R.drawable.icon_audio_mute);

        } else {
            btnAudioMute.setImageResource(R.drawable.icon_audio_active);
        }
    }

    /**
     * Update the video button UI when changing video state
     */
    @Override
    public void onPresenterRequestChangeVideoUI(boolean isVideoMute) {
        if (isVideoMute) {
            btnVideoMute.setImageResource(R.drawable.icon_video_mute);

        } else {
            btnVideoMute.setImageResource(R.drawable.icon_video_active);
        }
    }

    /**
     * Update the camera button UI when changing camera state
     */
    @Override
    public void onPresenterRequestChangeCameraUI(boolean isCameraMute, boolean isToast) {
        if (isCameraMute) {
            btnCameraMute.setImageResource(R.drawable.icon_camera_mute);
            if (isToast) {
                String log = getString(R.string.stop_camera);
                toastLog(TAG, context, log);
            }

        } else {
            btnCameraMute.setImageResource(R.drawable.icon_camera_active);
            if (isToast) {
                String log = getString(R.string.restart_camera);
                toastLog(TAG, context, log);
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        videoViewLayout = rootView.findViewById(R.id.ll_screen_share);
        btnSpeaker = rootView.findViewById(R.id.toggle_speaker_screen_share);
        btnAudioMute = rootView.findViewById(R.id.toggle_audio_screen_share);
        btnVideoMute = rootView.findViewById(R.id.toggle_video_screen_share);
        btnCameraMute = rootView.findViewById(R.id.toggle_camera_screen_share);
        btnDisconnect = rootView.findViewById(R.id.btn_disconnect_screen_share);
        btnOption = rootView.findViewById(R.id.btn_option_video);
        btnScreenShareAction = rootView.findViewById(R.id.btn_screen_share_action);
        btnFullScreen = rootView.findViewById(R.id.btn_full_screen);
        btnVideoRes = rootView.findViewById(R.id.btn_video_res_screen_share);
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private void setActionBar() {
        ActionBar actionBar = ((ScreenSharingActivity) getActivity()).getSupportActionBar();
        super.setActionBar(actionBar);
    }

    /**
     * Init value for view components
     */
    private void initComponents() {
        // set onClick event for buttons in layout
        btnBack.setOnClickListener(this);
        btnLocalPeer.setOnClickListener(this);
        btnRemotePeer1.setOnClickListener(this);
        btnRemotePeer2.setOnClickListener(this);
        btnRemotePeer3.setOnClickListener(this);
        btnSpeaker.setOnClickListener(this);
        btnAudioMute.setOnClickListener(this);
        btnVideoMute.setOnClickListener(this);
        btnCameraMute.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);
        btnOption.setOnClickListener(this);
        videoViewLayout.setOnClickListener(this);
        btnScreenShareAction.setOnClickListener(this);
        btnFullScreen.setOnClickListener(this);
        btnVideoRes.setOnClickListener(this);

        // init setting value for room name in action bar
        txtRoomName.setText(Config.ROOM_NAME_SCREEN);
        btnVideoRes.setDirection(VideoResButton.ButtonDirection.TOP_RIGHT);

        btnSpeaker.setVisibility(GONE);
        btnAudioMute.setVisibility(GONE);
        btnVideoMute.setVisibility(GONE);
        btnCameraMute.setVisibility(GONE);
        btnScreenShareAction.setVisibility(GONE);

        // Set init audio/video state
        setAudioBtnLabel(false, false);
        setVideoBtnLabel(false, false);

        // Add an system overlay button for stop screen share
        stopScreenshareFloat = new Button(getActivity());
        stopScreenshareFloat.setText(getActivity().getResources().getText(R.string.stop_screenShare));
        stopScreenshareFloat.setTextColor(getActivity().getResources().getColor(R.color.color_white));
        stopScreenshareFloat.setTextSize(getActivity().getResources().getDimension(R.dimen.sp_4sp));
        stopScreenshareFloat.setBackground(getActivity().getResources().getDrawable(R.drawable.button_stop_screen_sharing));

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

        stopScreenshareFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isShowScreenSharing = true;
                showHideButtonStopScreenSharing();
                stopScreenCapture();
            }
        });
    }

    /**
     * request info to display on view from presenter
     * try to connect to room if not connected
     */
    private void requestViewLayout() {
        if (presenter != null) {
            presenter.onViewRequestConnectedLayout();
//            startScreenCapture();
        }
    }

    /**
     * Set the mute audio button label according to the current state of audio.
     *
     * @param doToast If true, Toast about setting audio to current state.
     */
    private void setAudioBtnLabel(boolean isAudioMuted, boolean doToast) {
        if (isAudioMuted) {
            if (doToast) {
                String log = getString(R.string.muted_audio);
                toastLog(TAG, context, log);
            }
        } else {
            if (doToast) {
                String log = getString(R.string.enabled_audio);
                toastLog(TAG, context, log);
            }
        }
    }

    /**
     * Set the mute video button label according to the current state of video.
     *
     * @param doToast If true, Toast about setting video to current state.
     */
    private void setVideoBtnLabel(boolean isVideoMuted, boolean doToast) {
        if (isVideoMuted) {
            if (doToast) {
                String log = getString(R.string.muted_video);
                toastLog(TAG, context, log);
            }
        } else {
            if (doToast) {
                String log = getString(R.string.enabled_video);
                toastLog(TAG, context, log);
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

    private void showVideoOption() {
        isShowVideoOption = !isShowVideoOption;

        if (isShowVideoOption) {
            if (!isShareScreen) {
                showHideView(btnCameraMute, isShowVideoOption, false);
                showHideView(btnVideoMute, isShowVideoOption, false);
            }
            showHideView(btnAudioMute, isShowVideoOption, false);
            showHideView(btnSpeaker, isShowVideoOption, false);
            showHideView(btnScreenShareAction, isShowVideoOption, true);

        } else {
            showHideView(btnScreenShareAction, isShowVideoOption, true);
            showHideView(btnSpeaker, isShowVideoOption, false);
            showHideView(btnAudioMute, isShowVideoOption, false);
            if (!isShareScreen) {
                showHideView(btnVideoMute, isShowVideoOption, false);
                showHideView(btnCameraMute, isShowVideoOption, false);
            }
        }
    }

    private void showHideView(View view, boolean isShow, boolean isFinalView) {
        if (isShow) {
            view.animate()
                    .translationY(0)
                    .alpha(1.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            view.setVisibility(View.VISIBLE);
                            if (isFinalView) {
                                btnOption.setImageResource(R.drawable.ic_collapse_option);
                            }
                        }
                    });
        } else {
            view.animate()
                    .translationY(btnOption.getHeight())
                    .alpha(0.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            view.setVisibility(View.GONE);
                            if (isFinalView) {
                                btnOption.setImageResource(R.drawable.ic_expand_option);
                            }
                        }
                    });
        }
    }


    private void processFullScreen() {
        isFullScreen = !isFullScreen;
        ActionBar actionBar = ((ScreenSharingActivity) getActivity()).getSupportActionBar();

        if (isFullScreen) {
            showHideVideoTool(false);

            btnVideoRes.setVisibility(GONE);
            ((ScreenSharingActivity) getActivity()).onShowHideLocalCameraViewFragment(false, true);
            ((ScreenSharingActivity) getActivity()).onShowHideVideoResFragment(false);
            btnFullScreen.setBackground(getActivity().getResources().getDrawable(R.drawable.ic_full_screen_exit));
            actionBar.hide();
        } else {
            showHideVideoTool(true);
            btnOption.setVisibility(View.VISIBLE);
            btnDisconnect.setVisibility(View.VISIBLE);
            btnVideoRes.setVisibility(View.VISIBLE);
            ((ScreenSharingActivity) getActivity()).onShowHideLocalCameraViewFragment(true, true);
            if (isShowVideoRes) {
                ((ScreenSharingActivity) getActivity()).onShowHideVideoResFragment(true);
            }
            btnFullScreen.setBackground(getActivity().getResources().getDrawable(R.drawable.ic_full_screen));
            actionBar.show();
        }
    }

    private void showHideVideoTool(boolean isShow) {
        if (isShow) {
            if (isShowVideoOption) {
                btnScreenShareAction.setVisibility(VISIBLE);
                btnSpeaker.setVisibility(VISIBLE);
                btnAudioMute.setVisibility(VISIBLE);
//                if (!isShareScreen) {
                btnVideoMute.setVisibility(VISIBLE);
                btnCameraMute.setVisibility(VISIBLE);
//                }
            }

            btnOption.setVisibility(VISIBLE);
            btnDisconnect.setVisibility(VISIBLE);
        } else {
            btnScreenShareAction.setVisibility(GONE);
            btnSpeaker.setVisibility(GONE);
            btnAudioMute.setVisibility(GONE);
            btnVideoMute.setVisibility(GONE);
            btnCameraMute.setVisibility(GONE);
            btnOption.setVisibility(GONE);
            btnDisconnect.setVisibility(GONE);
        }
    }

    private void showHideVideoResolution() {
        isShowVideoRes = !isShowVideoRes;

        ((ScreenSharingActivity) getActivity()).onShowHideVideoResFragment(isShowVideoRes);

        if (isShowVideoRes) {
            btnVideoRes.setState(VideoResButton.ButtonState.CLICKED);
        } else {
            btnVideoRes.setState(VideoResButton.ButtonState.NORMAL);
        }
    }

    private void showHideVideoResolution(boolean isShow) {
        isShowVideoRes = isShow;

        ((ScreenSharingActivity) getActivity()).onShowHideVideoResFragment(isShow);

        if (isShow) {
            btnVideoRes.setState(VideoResButton.ButtonState.CLICKED);
        } else {
            btnVideoRes.setState(VideoResButton.ButtonState.NORMAL);
        }

    }

    private void moveMainViewToSmallLocalCameraView(SurfaceViewRenderer view) {
        if (context != null && context instanceof ScreenSharingActivity) {
            ((ScreenSharingActivity) context).setLocalCameraView(view);
        }
    }

    private void moveMainViewToSmallLocalScreenView(SurfaceViewRenderer view) {
        if (context != null && context instanceof ScreenSharingActivity) {
            ((ScreenSharingActivity) context).setLocalScreenView(view);
        }
    }

    private void moveMainViewToSmallView2(SurfaceViewRenderer view) {
        if (context != null && context instanceof ScreenSharingActivity) {
            ((ScreenSharingActivity) context).setRemoteCameraView(view);
        }
    }

    public void bringSmallViewToMainView(VIDEO_TYPE video_type) {
        if (video_type == VIDEO_TYPE.LOCAL_CAMERA) {
//            if (localCameraView == currentMainView) {
//                addViewToMain(remoteCameraView);
////                moveMainViewToSmallLocalCameraView(localCameraView);
//            } else if (remoteCameraView == currentMainView) {
//                addViewToMain(localCameraView);
////                moveMainViewToSmallLocalCameraView(remoteCameraView);
//            }
            addViewToMain(localCameraView);
            moveMainViewToSmallLocalScreenView(localScreenView);
            // hide local camera view
//            if (context != null && context instanceof ScreenSharingActivity) {
//                ((ScreenSharingActivity) context).onShowHideLocalCameraViewFragment(false, false);
//            }
        } else if (video_type == VIDEO_TYPE.LOCAL_SCREEN) {
//            if (localScreenView == currentMainView) {
//                addViewToMain(remoteCameraView);
////                moveMainViewToSmallLocalCameraView(localScreenView);
//            } else if (remoteCameraView == currentMainView) {
//                addViewToMain(localScreenView);
////                moveMainViewToSmallLocalCameraView(remoteCameraView);
//            }
            addViewToMain(localScreenView);
            moveMainViewToSmallLocalCameraView(localCameraView);
            // hide local screen view
            if (context != null && context instanceof ScreenSharingActivity) {
                ((ScreenSharingActivity) context).onShowHideLocalScreenViewFragment(false, false);
            }
        }
    }

    private void showHideButtonStopScreenSharing() {
        isShowScreenSharing = !isShowScreenSharing;
        WindowManager windowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        if (isShowScreenSharing) {
            windowManager.addView(stopScreenshareFloat, stopScreenshareLayoutParams);
            btnScreenShareAction.setImageResource(R.drawable.ic_start_screen_share);
        } else {
            windowManager.removeView(stopScreenshareFloat);
            btnScreenShareAction.setImageResource(R.drawable.ic_stop_screen_share);
        }
    }

    private void processShareScreen() {
        //show the stop screen share button
        if (presenter.onViewRequestButtonOverlayPermission()) {
            // Permission has already been granted
            showHideButtonStopScreenSharing();
        }

        isShareScreen = !isShareScreen;

        // call SDK to create screen share renderer view and share to remote
        if (isShareScreen) {
            startScreenCapture();
        } else {
            stopScreenCapture();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PermissionUtils.REQUEST_BUTTON_OVERLAY_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(context)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + context.getPackageName()));
                    presenter.onViewRequestPermissionDeny();
                } else {
                    showHideButtonStopScreenSharing();
                }
            }
        } else if (requestCode == PermissionUtils.CAPTURE_PERMISSION_REQUEST_CODE) {
            presenter.onViewRequestShareScreen();
            isShareScreenFirstTime = false;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    @TargetApi(21)
    private void startScreenCapture() {
        if (isShareScreenFirstTime) {
            MediaProjectionManager mediaProjectionManager =
                    (MediaProjectionManager) getActivity().getApplication().getSystemService(
                            Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(
                    mediaProjectionManager.createScreenCaptureIntent(), PermissionUtils.CAPTURE_PERMISSION_REQUEST_CODE);
        } else {
            addViewToMain(localScreenView);
            moveMainViewToSmallLocalCameraView(localCameraView);


            if (context != null && context instanceof ScreenSharingActivity) {
                ((ScreenSharingActivity) context).onShowHideLocalCameraViewFragment(true, false);
            }
        }

        //hide button
        btnVideoMute.setVisibility(GONE);
        btnCameraMute.setVisibility(GONE);

        if (context != null && context instanceof ScreenSharingActivity) {
            ((ScreenSharingActivity) context).onShowHideLocalScreenViewFragment(false, false);
        }
    }

    private void stopScreenCapture() {
        isShareScreen = false;

        addViewToMain(localCameraView);
        moveMainViewToSmallLocalScreenView(localScreenView);

        //show button
        btnVideoMute.setVisibility(VISIBLE);
        btnCameraMute.setVisibility(VISIBLE);

        // hide local camera view
//        if (context != null && context instanceof ScreenSharingActivity) {
//            ((ScreenSharingActivity) context).onShowHideLocalCameraViewFragment(false, false);
//        }
    }
}
