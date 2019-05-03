package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
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
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.VideoResButton;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * A simple {@link CustomActionBar} subclass.
 * This class is responsible for display UI and get user interaction
 */
public class VideoFragment extends CustomActionBar implements VideoContract.MainView,
        View.OnClickListener {

    private final String TAG = VideoFragment.class.getName();

    // view widgets
    private LinearLayout videoViewLayout;
    private FloatingActionButton btnDisconnect, btnOption, btnAudioMute, btnVideoMute, btnVideoStop,
            btnSpeaker, btnStartSecondVideo, btnSwitchCamera;
    private Button stopScreenshareFloat, btnFullScreen, btnStartVideo, btnStartAudio;
    private VideoResButton btnVideoRes;
    private WindowManager.LayoutParams stopScreenshareLayoutParams;

    // presenter instance to implement video logic
    private VideoContract.Presenter presenter;

    // local variables to check views' state
    private boolean isShowVideoOption = false;
    private boolean isShowScreenSharing = false;
    private boolean isFullScreen = false;
    private boolean isShowVideoRes = false;
    private boolean isShareScreen = false;

    private SurfaceViewRenderer localCameraView, localScreenView, remoteCameraView,
            remoteScreenView, currentMainView;

    public static VideoFragment newInstance() {
        return new VideoFragment();
    }

    @Override
    public void setPresenter(VideoContract.Presenter presenter) {
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
        ((VideoActivity) context).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[SA][Video][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_video, container, false);

        // get the UI controls from layout
        getControlWidgets(rootView);

        // setup the action bar
        setActionBar();

        // init the UI controls
        initComponents();

        //request an initiative connection
        requestViewLayout();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // just process resume for video camera, not apply for video screen
        if (!isShareScreen) {
            presenter.onViewRequestResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // just process pause for video camera, not apply for video screen
        if (!isShareScreen) {
            presenter.onViewRequestPause();
        }
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
            case R.id.toggle_speaker_video:
                presenter.onViewRequestChangeSpeakerOutput();
                break;
            case R.id.toggle_audio_video:
                presenter.onViewRequestChangeAudioState();
                break;
            case R.id.toggle_video_video:
                presenter.onViewRequestChangeVideoState();
                break;
            case R.id.toggle_video_device:
                presenter.onViewRequestChangeVideoSourceState();
                break;
            case R.id.btn_option_video:
                showVideoOption();
                break;
            case R.id.btn_start_second_video:
                processStartSecondVideo();
                break;
            case R.id.btn_switch_camera_video:
                processSwitchCamera();
                break;
            case R.id.btn_disconnect_video:
                processDisconnect();
                break;
            case R.id.btn_full_screen:
                processFullScreen();
                break;
            case R.id.btn_video_res_video:
                showHideVideoResolution();
                break;
            case R.id.ll_videos:
                showHideVideoResolution(false);
                break;
            case R.id.btnStartAudio:
                startLocalAudio();
                break;
            case R.id.btnStartVideo:
                startLocalVideo();
                break;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // only exit/disconnect from room when it is chosen by user
        // not changing configuration
        if (!((VideoActivity) context).isChangingConfigurations()) {
            presenter.onViewRequestExit();

            if (isShowScreenSharing) {
                showHideButton(stopScreenshareFloat, false);
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

    @Override
    public void onPresenterRequestShowHideButtonStopScreenSharing() {
        showHideButtonStopScreenSharing();
    }

    @Override
    public void onPresenterRequestLocalAudioCapture(String mediaId) {
        btnStartAudio.setVisibility(GONE);

        toastLog(TAG, context, "Local audio is on with id = " + mediaId);
    }

    @Override
    public void onPresenterRequestLocalVideoCapture(String mediaId) {
        btnStartVideo.setVisibility(GONE);

        toastLog(TAG, context, "Local video is on with id = " + mediaId);
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

        // move other views to small view
        bringSmallViewToMainView(Constants.VIDEO_TYPE.LOCAL_CAMERA);
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

        // add local screen view as main view
        addViewToMain(videoView);

        // move other views to small view
        bringSmallViewToMainView(Constants.VIDEO_TYPE.LOCAL_SCREEN);
    }

    /**
     * Add or update remote Peer's VideoView into the view layout when receiving remote camera video view
     *
     * @param remoteVideoView
     */
    @Override
    public void onPresenterRequestAddCameraRemoteView(SurfaceViewRenderer remoteVideoView) {
        //save localScreenView
        remoteCameraView = remoteVideoView;

        // add local screen view as main view
        addViewToMain(remoteVideoView);

        // move other views to small view
        bringSmallViewToMainView(Constants.VIDEO_TYPE.REMOTE_CAMERA);
    }

    /**
     * Add remote screen video view to the main view when receiving remote screen video view
     *
     * @param remoteScreenView remote screen video view
     */
    @Override
    public void onPresenterRequestAddScreenRemoteView(SurfaceViewRenderer remoteScreenView) {
        //save localScreenView
        this.remoteScreenView = remoteScreenView;

        // add local screen view as main view
        addViewToMain(remoteScreenView);

        // move other views to small view
        bringSmallViewToMainView(Constants.VIDEO_TYPE.REMOTE_SCREEN);
    }

    /**
     * Remove remote video view when remote peer left or local peer exit the room
     * <p>
     * Change layout orientation to Vertical when there is only 1 local video view
     */
    @Override
    public void onPresenterRequestRemoveRemotePeer() {
        remoteCameraView = null;
        remoteScreenView = null;

        // bring local camera view to main big view
        bringSmallViewToMainView(Constants.VIDEO_TYPE.LOCAL_CAMERA);
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
    public void onPresenterRequestChangeSpeakerOutput(boolean isSpeakerOn) {
        if (isSpeakerOn) {
            btnSpeaker.setImageResource(R.drawable.ic_headset);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnSpeaker.setTooltipText("Enable headset");
            }
        } else {
            btnSpeaker.setImageResource(R.drawable.ic_audio_speaker);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnSpeaker.setTooltipText("Enable speaker");
            }
        }
    }

    /**
     * Update the audio button UI when changing audio state
     */
    @Override
    public void onPresenterRequestChangeAudioUI(boolean isAudioMute) {
        if (isAudioMute) {
            btnAudioMute.setImageResource(R.drawable.icon_audio_active);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnAudioMute.setTooltipText("Enable audio");
            }

        } else {
            btnAudioMute.setImageResource(R.drawable.icon_audio_mute);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnAudioMute.setTooltipText("Mute audio");
            }
        }
    }

    /**
     * Update the video button UI when changing video state
     */
    @Override
    public void onPresenterRequestChangeVideoUI(boolean isVideoMute) {
        if (isVideoMute) {
            btnVideoMute.setImageResource(R.drawable.icon_video_active);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnVideoMute.setTooltipText("Enable video");
            }
        } else {
            btnVideoMute.setImageResource(R.drawable.icon_video_mute);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnVideoMute.setTooltipText("Mute video");
            }
        }
    }

    /**
     * Update the video stop button UI when changing video state {stop, start}
     */
    @Override
    public void onPresenterRequestChangeVideoSourceUI(boolean isCameraMute, boolean isToast) {
        if (isCameraMute) {
            btnVideoStop.setImageResource(R.drawable.ic_play_video);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnVideoStop.setTooltipText("Play video");
            }
            if (isToast) {
                String log = getString(R.string.stop_camera);
                toastLog(TAG, context, log);
            }

        } else {
            btnVideoStop.setImageResource(R.drawable.ic_stop_video);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnVideoStop.setTooltipText("Stop video");
            }
            if (isToast) {
                String log = getString(R.string.restart_camera);
                toastLog(TAG, context, log);
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // public methods
    //----------------------------------------------------------------------------------------------

    /**
     * Bring the small available on small view to main big view
     * and move all other views to small views
     *
     * @param video_type the type of small view to move to main big view
     */
    public void bringSmallViewToMainView(Constants.VIDEO_TYPE video_type) {
        switch (video_type) {
            case LOCAL_CAMERA:
                if (localCameraView != null) {
                    addViewToMain(localCameraView);
                }
                moveViewToSmallLocalScreenView(localScreenView);
                moveViewToSmallRemoteCameraView(remoteCameraView);
                moveViewToSmallRemoteScreenView(remoteScreenView);
                if (isShowVideoOption) {
                    showHideButton(btnSwitchCamera, true);
                }
                break;
            case LOCAL_SCREEN:
                if (localScreenView != null) {
                    addViewToMain(localScreenView);
                    if (!isShowScreenSharing) {
                        showHideButton(stopScreenshareFloat, true);
                    }
                    showHideButton(btnSwitchCamera, false);
                }
                moveViewToSmallLocalCameraView(localCameraView);
                moveViewToSmallRemoteCameraView(remoteCameraView);
                moveViewToSmallRemoteScreenView(remoteScreenView);
                break;
            case REMOTE_CAMERA:
                if (remoteCameraView != null) {
                    addViewToMain(remoteCameraView);
                }
                moveViewToSmallLocalCameraView(localCameraView);
                moveViewToSmallLocalScreenView(localScreenView);
                moveViewToSmallRemoteScreenView(remoteScreenView);
                break;
            case REMOTE_SCREEN:
                if (remoteScreenView != null) {
                    addViewToMain(remoteScreenView);
                }
                moveViewToSmallLocalCameraView(localCameraView);
                moveViewToSmallLocalScreenView(localScreenView);
                moveViewToSmallRemoteCameraView(remoteCameraView);
                break;
        }
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        videoViewLayout = rootView.findViewById(R.id.ll_videos);
        btnSpeaker = rootView.findViewById(R.id.toggle_speaker_video);
        btnAudioMute = rootView.findViewById(R.id.toggle_audio_video);
        btnVideoMute = rootView.findViewById(R.id.toggle_video_video);
        btnVideoStop = rootView.findViewById(R.id.toggle_video_device);
        btnDisconnect = rootView.findViewById(R.id.btn_disconnect_video);
        btnOption = rootView.findViewById(R.id.btn_option_video);
        btnStartSecondVideo = rootView.findViewById(R.id.btn_start_second_video);
        btnSwitchCamera = rootView.findViewById(R.id.btn_switch_camera_video);
        btnFullScreen = rootView.findViewById(R.id.btn_full_screen);
        btnVideoRes = rootView.findViewById(R.id.btn_video_res_video);
        btnStartAudio = rootView.findViewById(R.id.btnStartAudio);
        btnStartVideo = rootView.findViewById(R.id.btnStartVideo);
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private void setActionBar() {
        ActionBar actionBar = ((VideoActivity) getActivity()).getSupportActionBar();
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
        btnVideoStop.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);
        btnOption.setOnClickListener(this);
        videoViewLayout.setOnClickListener(this);
        btnStartSecondVideo.setOnClickListener(this);
        btnSwitchCamera.setOnClickListener(this);
        btnFullScreen.setOnClickListener(this);
        btnVideoRes.setOnClickListener(this);
        btnStartAudio.setOnClickListener(this);
        btnStartVideo.setOnClickListener(this);

        // init setting value for room name in action bar
        txtRoomName.setText(Config.ROOM_NAME_VIDEO);
        btnVideoRes.setDirection(VideoResButton.ButtonDirection.TOP_RIGHT);

        btnSpeaker.setVisibility(GONE);
        btnAudioMute.setVisibility(GONE);
        btnVideoMute.setVisibility(GONE);
        btnVideoStop.setVisibility(GONE);
        btnStartSecondVideo.setVisibility(GONE);
        btnSwitchCamera.setVisibility(GONE);

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
                bringSmallViewToMainView(Constants.VIDEO_TYPE.LOCAL_CAMERA);
            }
        });

        if (Utils.isDefaultScreenDeviceSetting()) {
            btnStartSecondVideo.setImageResource(R.drawable.icon_camera_active);
            isShareScreen = true;
            showHideButton(btnSwitchCamera, false);
        } else {
            btnStartSecondVideo.setImageResource(R.drawable.ic_start_second_video);
            isShareScreen = false;
        }

        // add tool tip for buttons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnBack.setTooltipText("Back to main activity");
            btnLocalPeer.setTooltipText("Local peer");
            btnRemotePeer1.setTooltipText("Remote peer");
            btnRemotePeer2.setTooltipText("Remote peer");
            btnRemotePeer3.setTooltipText("Remote peer");
            btnVideoRes.setTooltipText("Video resolution setting");
            btnSwitchCamera.setTooltipText("Switch camera");
            if (isShareScreen) {
                btnStartSecondVideo.setTooltipText("Start screen video");
            } else {
                btnStartSecondVideo.setTooltipText("Start camera video");
            }
            btnSpeaker.setTooltipText("Enable speaker");
            btnAudioMute.setTooltipText("Mute audio");
            btnVideoMute.setTooltipText("Mute video");
            btnVideoStop.setTooltipText("Stop video");
            btnOption.setTooltipText("Extend buttons");
            btnDisconnect.setTooltipText("Disconnect");
            btnFullScreen.setTooltipText("Full screen");
            btnStartAudio.setTooltipText("Start local audio");
            btnStartVideo.setTooltipText("Start local video");
        }
    }

    /**
     * request info to display on view from presenter
     * try to connect to room if not connected
     */
    private void requestViewLayout() {
        if (presenter != null) {
            presenter.onViewRequestConnectedLayout();
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

    private void addViewToMain(SurfaceViewRenderer videoView) {
        if (videoView == null) {
            String log = "[SA][addViewToMain] Not adding view to main view as videoView is null!";
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

    private void showHideButtonStopScreenSharing() {
        isShowScreenSharing = !isShowScreenSharing;
        WindowManager windowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        if (isShowScreenSharing) {
            windowManager.addView(stopScreenshareFloat, stopScreenshareLayoutParams);
        } else {
            windowManager.removeView(stopScreenshareFloat);
        }
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

    private void showVideoOption() {
        isShowVideoOption = !isShowVideoOption;

        if (isShowVideoOption) {
            showHideView(btnVideoStop, isShowVideoOption, false);
            showHideView(btnVideoMute, isShowVideoOption, false);
            showHideView(btnAudioMute, isShowVideoOption, false);
            showHideView(btnSpeaker, isShowVideoOption, false);
            if (!isShareScreen) {
                showHideView(btnSwitchCamera, isShowVideoOption, false);
            }
            showHideView(btnStartSecondVideo, isShowVideoOption, true);
        } else {
            showHideView(btnStartSecondVideo, isShowVideoOption, false);
            showHideView(btnSwitchCamera, isShowVideoOption, false);
            showHideView(btnSpeaker, isShowVideoOption, false);
            showHideView(btnAudioMute, isShowVideoOption, false);
            showHideView(btnVideoMute, isShowVideoOption, false);
            showHideView(btnVideoStop, isShowVideoOption, true);
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
                                // change tool tip for button
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnOption.setTooltipText("Collapse buttons");
                                }
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
                                // change tool tip for button
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnOption.setTooltipText("Collapse buttons");
                                }
                            }
                        }
                    });
        }
    }

    private void processFullScreen() {
        isFullScreen = !isFullScreen;
        ActionBar actionBar = ((VideoActivity) getActivity()).getSupportActionBar();

        if (isFullScreen) {
            showHideVideoTool(false);

            btnVideoRes.setVisibility(GONE);
            ((VideoActivity) getActivity()).onShowHideLocalCameraViewFragment(false, true);
            ((VideoActivity) getActivity()).onShowHideVideoResFragment(false);
            btnFullScreen.setBackground(getActivity().getResources().getDrawable(R.drawable.ic_full_screen_exit));
            actionBar.hide();

            // change tool tip for button
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                btnFullScreen.setTooltipText("Exit full screen");
            }

        } else {
            showHideVideoTool(true);
            btnOption.setVisibility(View.VISIBLE);
            btnDisconnect.setVisibility(View.VISIBLE);
            btnVideoRes.setVisibility(View.VISIBLE);
            ((VideoActivity) getActivity()).onShowHideLocalCameraViewFragment(true, true);
            if (isShowVideoRes) {
                ((VideoActivity) getActivity()).onShowHideVideoResFragment(true);
            }
            btnFullScreen.setBackground(getActivity().getResources().getDrawable(R.drawable.ic_full_screen));
            actionBar.show();

            // change tool tip for button
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                btnFullScreen.setTooltipText("Full screen");
            }
        }
    }

    private void showHideVideoTool(boolean isShow) {
        if (isShow) {
            if (isShowVideoOption) {
                btnStartSecondVideo.setVisibility(VISIBLE);
                btnSpeaker.setVisibility(VISIBLE);
                btnAudioMute.setVisibility(VISIBLE);
                if (!isShareScreen) {
                    btnSwitchCamera.setVisibility(VISIBLE);
                }
                btnVideoMute.setVisibility(VISIBLE);
                btnVideoStop.setVisibility(VISIBLE);
            }

            btnOption.setVisibility(VISIBLE);
            btnDisconnect.setVisibility(VISIBLE);
        } else {
            btnStartSecondVideo.setVisibility(GONE);
            btnSpeaker.setVisibility(GONE);
            btnAudioMute.setVisibility(GONE);
            btnSwitchCamera.setVisibility(GONE);
            btnVideoMute.setVisibility(GONE);
            btnVideoStop.setVisibility(GONE);
            btnOption.setVisibility(GONE);
            btnDisconnect.setVisibility(GONE);
        }
    }

    private void showHideVideoResolution() {
        isShowVideoRes = !isShowVideoRes;

        ((VideoActivity) getActivity()).onShowHideVideoResFragment(isShowVideoRes);

        if (isShowVideoRes) {
            btnVideoRes.setState(VideoResButton.ButtonState.CLICKED);
        } else {
            btnVideoRes.setState(VideoResButton.ButtonState.NORMAL);
        }
    }

    private void showHideVideoResolution(boolean isShow) {
        isShowVideoRes = isShow;

        ((VideoActivity) getActivity()).onShowHideVideoResFragment(isShow);

        if (isShow) {
            btnVideoRes.setState(VideoResButton.ButtonState.CLICKED);
        } else {
            btnVideoRes.setState(VideoResButton.ButtonState.NORMAL);
        }

    }

    private void moveViewToSmallLocalCameraView(SurfaceViewRenderer view) {
        if (view == null)
            return;

        if (context != null && context instanceof VideoActivity) {
            ((VideoActivity) context).setLocalCameraView(view);
        }
    }

    private void moveViewToSmallLocalScreenView(SurfaceViewRenderer view) {
        if (view == null)
            return;

        if (context != null && context instanceof VideoActivity) {
            ((VideoActivity) context).setLocalScreenView(view);
        }

        showHideButton(stopScreenshareFloat, false);
        if (isShowVideoOption) {
            showHideButton(btnSwitchCamera, true);
        }
    }

    private void moveViewToSmallRemoteCameraView(SurfaceViewRenderer view) {
        if (view == null)
            return;

        if (context != null && context instanceof VideoActivity) {
            ((VideoActivity) context).setRemoteCameraView(view);
        }
    }

    private void moveViewToSmallRemoteScreenView(SurfaceViewRenderer view) {
        if (view == null)
            return;

        if (context != null && context instanceof VideoActivity) {
            ((VideoActivity) context).setRemoteScreenView(view);
        }
    }

    /**
     * Start the second video.
     * If current video is from front/back/custom camera, then start screen video
     * If current video is from screen video, then start video from front camera as default
     */
    private void processStartSecondVideo() {
        if (!isShareScreen) {
            if (localScreenView == null) {
                presenter.onViewRequestStartScreen();
            }

            bringSmallViewToMainView(Constants.VIDEO_TYPE.LOCAL_SCREEN);
        } else {
            presenter.onViewRequestStartFrontCamera();
            bringSmallViewToMainView(Constants.VIDEO_TYPE.LOCAL_CAMERA);
        }
    }

    private void processSwitchCamera() {
        presenter.onViewRequestSwitchCamera();
    }

    private void startLocalAudio() {
        // start local audio
        presenter.onViewRequestStartAudio();
    }

    private void startLocalVideo() {
        //start local video
        presenter.onViewRequestStartVideo();
    }

    private void processDisconnect() {
        presenter.onViewRequestDisconnectFromRoom();
        onPresenterRequestDisconnectUIChange();
    }
}
