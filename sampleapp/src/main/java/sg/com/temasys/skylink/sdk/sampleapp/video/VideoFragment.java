package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomTriangleButton;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

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
    private final String SELF_CAM_VIEW = "self_camera_video";
    private final String SELF_SCREEN_VIEW = "self_screen_video";
    private final String REMOTE_CAM_VIEW = "remote_camera_video";
    private final String REMOTE_SCREEN_VIEW = "remote_screen_video";
    private final String MAIN_VIEW = "main";

    // view widgets
    private LinearLayout videoViewLayout;
    private LinearLayout audioLayout, videoLayout, screenLayout;
    private RelativeLayout videoToolLayout;
    private CustomTriangleButton btnVideoRes;
    private ImageButton btnConnectDisconnect, btnAudioSpeaker, btnAudioMute, btnAudioStart, btnAudio,
            btnVideoSwitchCamera, btnVideoMute, btnVideoStart, btnVideo,
            btnScreenMute, btnScreenStart, btnScreen;
    private Button stopScreenshareFloat, btnFullScreen;

    private WindowManager.LayoutParams stopScreenshareLayoutParams;

    // presenter instance to implement video logic
    private VideoContract.Presenter presenter;

    // local variables to keep views' state
    private boolean isShowScreenSharing = false;
    private boolean isFullScreen = false;
    private boolean isShowVideoRes = false;
    private boolean isShareScreen = false;

    private SurfaceViewRenderer localCameraView, localScreenView, remoteCameraView, remoteScreenView;

    // private variables to keep the internal view states
    private boolean toConnectToRoom = true;
    private boolean isAudioOptionsShow = false;
    private boolean isVideoOptionsShow = false;
    private boolean isScreenOptionsShow = false;

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
            case R.id.btn_connect_disconnect:
                processConnectDisconnect();
                break;
            case R.id.btn_audio:
                showAudioOptions();
                break;
            case R.id.btn_audio_start:
                presenter.onViewRequestStartAudio();
                break;
            case R.id.btn_audio_mute:
                presenter.onViewRequestChangeAudioState();
                break;
            case R.id.btn_audio_speaker:
                presenter.onViewRequestChangeSpeakerOutput();
                break;
            case R.id.btn_video:
                showVideoOptions();
                break;
            case R.id.btn_video_start:
                presenter.onViewRequestStartVideo();
                break;
            case R.id.btn_video_mute:
                presenter.onViewRequestChangeVideoState();
                break;
            case R.id.btn_video_switch_camera:
                presenter.onViewRequestSwitchCamera();
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
            case R.id.btn_screen:
                showScreenOptions();
                break;
            case R.id.btn_screen_start:
                presenter.onViewRequestStartScreen();
                break;
            case R.id.btn_screen_mute:
                presenter.onViewRequestChangeScreenState();
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
        // update the room id that has been generated by the SDK
        updateRoomInfo(roomId);

        // update the local peer avatar with the user name configured in default setting
        updateUILocalPeer(Config.USER_NAME_VIDEO);

        // Change the connect button UI to disconnect button
        btnConnectDisconnect.setImageResource(R.drawable.ic_disconnect_white_25dp);
        btnConnectDisconnect.setBackground(getResources().getDrawable(R.drawable.button_circle_call_end));

        // change variable toConnectToRoom
        toConnectToRoom = false;
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
     * @param peerList the list of left peer(s) in the room
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerLeft(List<SkylinkPeer> peerList) {
        processFillPeers(peerList);
    }

    /**
     * Change certain UI elements when disconnecting from room.
     */
    @Override
    public void onPresenterRequestDisconnectUIChange() {
        changeUIDisconnected();
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
        toastLog(TAG, context, "Local audio is on with id = " + mediaId);

        // change UI button
        btnAudioStart.setImageResource(R.drawable.ic_stop_white_20dp);
    }

    /**
     * Add or update our self VideoView into the view layout when local peer connected to room and
     * local video view is ready
     *
     * @param mediaId   id of the media object
     * @param videoView local video view from camera
     */
    @Override
    public void onPresenterRequestAddCameraSelfView(String mediaId, SurfaceViewRenderer videoView) {
        //save localCameraView
        localCameraView = videoView;

        // change UI button
        btnVideoStart.setImageResource(R.drawable.ic_stop_white_20dp);

        // add videoView to main view
        addViewToMain(videoView);

        // move other views to small view, display the local camera view to main view
        bringSmallViewToMainView(Constants.VIDEO_TYPE.LOCAL_CAMERA);

        // notice user about id of the local media
        toastLog(TAG, context, "Local video is on with id = " + mediaId);
    }

    /**
     * Add or update our self VideoView into the view layout when local peer connected to room and
     * local video view is ready
     *
     * @param mediaId    id of the media object
     * @param screenView local video view from screen
     */
    @Override
    public void onPresenterRequestAddScreenSelfView(String mediaId, SurfaceViewRenderer screenView) {
        //save localScreenView
        localScreenView = screenView;

        // change UI button
        btnScreenStart.setImageResource(R.drawable.ic_stop_white_20dp);

        // add local screen view as main view
        addViewToMain(screenView);

        // move other views to small view
        bringSmallViewToMainView(Constants.VIDEO_TYPE.LOCAL_SCREEN);

        // notice user about id of the local media
        toastLog(TAG, context, "Local video is on with id = " + mediaId);
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
     * Update UI (display toast info) about local/remote audio state
     *
     * @param isAudioMuted audio state
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
     * Update UI (display toast info) about local/remote video state
     *
     * @param isVideoMuted video state
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
     * Update UI (display toast info) about local/remote screen state
     *
     * @param isScreenMuted screen video state
     * @param isToast       display toast or not
     */
    @Override
    public void onPresenterRequestUpdateScreenState(boolean isScreenMuted, boolean isToast) {
        if (isScreenMuted) {
            if (isToast) {
                String log = getString(R.string.muted_screen);
                toastLog(TAG, context, log);
            }
        } else {
            if (isToast) {
                String log = getString(R.string.enabled_screen);
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
            btnAudioSpeaker.setImageResource(R.drawable.ic_headset_white_20dp);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnAudioSpeaker.setTooltipText("Enable headset");
            }
        } else {
            btnAudioSpeaker.setImageResource(R.drawable.ic_speaker_white_20dp);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnAudioSpeaker.setTooltipText("Enable speaker");
            }
        }
    }

    /**
     * Update the audio button UI when changing audio state
     */
    @Override
    public void onPresenterRequestChangeAudioUI(boolean isAudioMute) {
        if (isAudioMute) {
            btnAudioMute.setImageResource(R.drawable.ic_audio_active_white_20dp);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnAudioMute.setTooltipText("Enable audio");
            }

        } else {
            btnAudioMute.setImageResource(R.drawable.ic_audio_mute_white_20dp);
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
    public void onPresenterRequestChangeCameraUI(boolean isCameraMute, boolean isToast) {
        if (isCameraMute) {
            btnVideoStart.setImageResource(R.drawable.ic_start_white_20dp);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnVideoStart.setTooltipText("Play video");
            }
            if (isToast) {
                String log = getString(R.string.stop_camera);
                toastLog(TAG, context, log);
            }

        } else {
            btnVideoStart.setImageResource(R.drawable.ic_stop_white_20dp);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnVideoStart.setTooltipText("Stop video");
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

                break;
            case LOCAL_SCREEN:
                if (localScreenView != null) {
                    addViewToMain(localScreenView);
                    if (!isShowScreenSharing) {
                        showHideButton(stopScreenshareFloat, true);
                    }
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
        videoToolLayout = rootView.findViewById(R.id.ll_video_tool);
        btnFullScreen = rootView.findViewById(R.id.btn_full_screen);
        btnVideoRes = rootView.findViewById(R.id.btn_video_res_video);
        btnConnectDisconnect = rootView.findViewById(R.id.btn_connect_disconnect);
        audioLayout = rootView.findViewById(R.id.ll_audio_call);
        videoLayout = rootView.findViewById(R.id.ll_video_call);
        screenLayout = rootView.findViewById(R.id.ll_screen_call);
        btnAudioSpeaker = rootView.findViewById(R.id.btn_audio_speaker);
        btnAudioMute = rootView.findViewById(R.id.btn_audio_mute);
        btnAudioStart = rootView.findViewById(R.id.btn_audio_start);
        btnAudio = rootView.findViewById(R.id.btn_audio);
        btnVideoSwitchCamera = rootView.findViewById(R.id.btn_video_switch_camera);
        btnVideoMute = rootView.findViewById(R.id.btn_video_mute);
        btnVideoStart = rootView.findViewById(R.id.btn_video_start);
        btnVideo = rootView.findViewById(R.id.btn_video);
        btnScreenMute = rootView.findViewById(R.id.btn_screen_mute);
        btnScreenStart = rootView.findViewById(R.id.btn_screen_start);
        btnScreen = rootView.findViewById(R.id.btn_screen);
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

        btnFullScreen.setOnClickListener(this);
        btnVideoRes.setOnClickListener(this);
        btnConnectDisconnect.setOnClickListener(this);
        audioLayout.setOnClickListener(this);
        videoLayout.setOnClickListener(this);
        screenLayout.setOnClickListener(this);
        btnAudioSpeaker.setOnClickListener(this);
        btnAudioMute.setOnClickListener(this);
        btnAudioStart.setOnClickListener(this);
        btnAudio.setOnClickListener(this);
        btnVideoSwitchCamera.setOnClickListener(this);
        btnVideoMute.setOnClickListener(this);
        btnVideoStart.setOnClickListener(this);
        btnVideo.setOnClickListener(this);
        btnScreenMute.setOnClickListener(this);
        btnScreenStart.setOnClickListener(this);
        btnScreen.setOnClickListener(this);

        // init setting value for room name and room id in action bar
        // Update the UI when connecting to room: change the room_id
        updateRoomInfo(getResources().getString(R.string.guide_room_id));
        txtRoomName.setText(Config.ROOM_NAME_VIDEO);
        btnVideoRes.setDirection(CustomTriangleButton.ButtonDirection.TOP_RIGHT);

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

        // add tool tip for buttons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnBack.setTooltipText("Back to main activity");
            btnLocalPeer.setTooltipText("Local peer");
            btnRemotePeer1.setTooltipText("Remote peer");
            btnRemotePeer2.setTooltipText("Remote peer");
            btnRemotePeer3.setTooltipText("Remote peer");
            btnVideoRes.setTooltipText("Video resolution setting");
            btnFullScreen.setTooltipText("Full screen");
            btnConnectDisconnect.setTooltipText("Connect to room");
            btnAudioSpeaker.setTooltipText("Turn on speaker/headset");
            btnAudioMute.setTooltipText("Mute/Active audio");
            btnAudioStart.setTooltipText("Start/Stop audio");
            btnAudio.setTooltipText("Audio setting options");
            btnVideoSwitchCamera.setTooltipText("Switch camera");
            btnVideoMute.setTooltipText("Mute/Active video");
            btnVideoStart.setTooltipText("Start/Stop video");
            btnVideo.setTooltipText("Video setting options");
            btnScreenMute.setTooltipText("Mute/Active screen sharing view");
            btnScreenStart.setTooltipText("Start/Stop screen sharing view");
            btnScreen.setTooltipText("Screen sharing setting optiosn");
        }

        if (Utils.isDefaultSpeakerSettingForVideo()) {
            btnAudioSpeaker.setImageDrawable(getResources().getDrawable(R.drawable.ic_audio_speaker));
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
        View self = videoViewLayout.findViewWithTag(MAIN_VIEW);
        if (self != null) {
            // Remove the old self video.
            videoViewLayout.removeView(self);
        }

        // Tag new video as self and add onClickListener.
        videoView.setTag(MAIN_VIEW);

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

    private void processFullScreen() {
        isFullScreen = !isFullScreen;
        ActionBar actionBar = ((VideoActivity) getActivity()).getSupportActionBar();

        if (isFullScreen) {
            videoToolLayout.setVisibility(GONE);
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
            videoToolLayout.setVisibility(VISIBLE);
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

    private void showHideVideoResolution() {
        isShowVideoRes = !isShowVideoRes;

        ((VideoActivity) getActivity()).onShowHideVideoResFragment(isShowVideoRes);

        if (isShowVideoRes) {
            btnVideoRes.setState(CustomTriangleButton.ButtonState.CLICKED);
        } else {
            btnVideoRes.setState(CustomTriangleButton.ButtonState.NORMAL);
        }
    }

    private void showHideVideoResolution(boolean isShow) {
        isShowVideoRes = isShow;

        ((VideoActivity) getActivity()).onShowHideVideoResFragment(isShow);

        if (isShow) {
            btnVideoRes.setState(CustomTriangleButton.ButtonState.CLICKED);
        } else {
            btnVideoRes.setState(CustomTriangleButton.ButtonState.NORMAL);
        }

    }

    private void moveViewToSmallLocalCameraView(SurfaceViewRenderer view) {
        if (view == null)
            return;

        view.setTag(SELF_CAM_VIEW);

        if (context != null && context instanceof VideoActivity) {
            ((VideoActivity) context).setLocalCameraView(view);
        }
    }

    private void moveViewToSmallLocalScreenView(SurfaceViewRenderer view) {
        if (view == null)
            return;

        view.setTag(SELF_SCREEN_VIEW);

        if (context != null && context instanceof VideoActivity) {
            ((VideoActivity) context).setLocalScreenView(view);
        }

        showHideButton(stopScreenshareFloat, false);
    }

    private void moveViewToSmallRemoteCameraView(SurfaceViewRenderer view) {
        if (view == null)
            return;

        view.setTag(REMOTE_CAM_VIEW);

        if (context != null && context instanceof VideoActivity) {
            ((VideoActivity) context).setRemoteCameraView(view);
        }
    }

    private void moveViewToSmallRemoteScreenView(SurfaceViewRenderer view) {
        if (view == null)
            return;

        view.setTag(REMOTE_SCREEN_VIEW);

        if (context != null && context instanceof VideoActivity) {
            ((VideoActivity) context).setRemoteScreenView(view);
        }
    }

    private void processConnectDisconnect() {
        if (toConnectToRoom) {
            if (presenter != null) {
                presenter.onViewRequestConnectedLayout();

                // Update the UI when connecting to room: change the room_id
                updateRoomInfo(getResources().getString(R.string.initing_room_id));
            }
        } else {
            if (presenter != null) {
                presenter.onViewRequestDisconnectFromRoom();
            }
        }
    }

    /**
     * Update the UI when disconnect from the room
     */
    private void changeUIDisconnected() {
        // remove video views
        View self = videoViewLayout.findViewWithTag(SELF_CAM_VIEW);
        if (self != null) {
            videoViewLayout.removeView(self);
        }

        View peer = videoViewLayout.findViewWithTag(REMOTE_CAM_VIEW);
        if (peer != null) {
            videoViewLayout.removeView(peer);
        }

        View main = videoViewLayout.findViewWithTag(MAIN_VIEW);
        if (main != null) {
            videoViewLayout.removeView(main);
        }

        // change the connect/disconnect button
        btnConnectDisconnect.setImageResource(R.drawable.ic_connect_white_25dp);
        btnConnectDisconnect.setBackground(getResources().getDrawable(R.drawable.button_circle_connect_to_room));

        // reset the tool buttons UI
        btnAudio.setImageResource(R.drawable.ic_audio_green_20dp);
        btnVideo.setImageResource(R.drawable.ic_video_green_20dp);
        btnScreen.setImageResource(R.drawable.ic_screen_share_green_20dp);
        btnAudioStart.setImageResource(R.drawable.ic_start_white_20dp);
        btnVideoStart.setImageResource(R.drawable.ic_start_white_20dp);
        btnScreenStart.setImageResource(R.drawable.ic_start_white_20dp);
        btnAudioMute.setImageResource(R.drawable.ic_audio_mute_white_20dp);
        btnVideoMute.setImageResource(R.drawable.ic_video_mute_white_20dp);
        btnScreenMute.setImageResource(R.drawable.ic_stop_screen_share_white_20dp);
        btnAudioSpeaker.setImageResource(R.drawable.ic_headset_white_20dp);

        // reset the room id info and local peer button
        txtRoomId.setText(R.string.guide_room_id);
        btnLocalPeer.setVisibility(GONE);

        // reset the variable toConnectToRoom
        toConnectToRoom = true;
    }

    /**
     * Do clean up UI when disconnect from the room: remove self video view, remote video view, finish the activity
     */
    private void cleanUIWhenDisconnect() {
        View self = videoViewLayout.findViewWithTag(SELF_CAM_VIEW);
        if (self != null) {
            videoViewLayout.removeView(self);
        }

        View peer = videoViewLayout.findViewWithTag(REMOTE_CAM_VIEW);
        if (peer != null) {
            videoViewLayout.removeView(peer);
        }

//        if (getActivity() != null) {
//            getActivity().finish();
//        }
    }

    /**
     * Show or Hide the audio options
     */
    private void showAudioOptions() {
        isAudioOptionsShow = !isAudioOptionsShow;

        if (isAudioOptionsShow) {
            btnAudio.setImageResource(R.drawable.ic_collapse_green_20dp);
            showHideViewAudio(btnAudioStart, true, false);
            showHideViewAudio(btnAudioMute, true, false);
            showHideViewAudio(btnAudioSpeaker, true, true);
        } else {
            showHideViewAudio(btnAudioStart, false, false);
            showHideViewAudio(btnAudioMute, false, false);
            showHideViewAudio(btnAudioSpeaker, false, true);
            audioLayout.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    /**
     * Show or Hide the video options
     */
    private void showVideoOptions() {
        isVideoOptionsShow = !isVideoOptionsShow;

        if (isVideoOptionsShow) {
            btnVideo.setImageResource(R.drawable.ic_collapse_green_20dp);
            showHideViewVideo(btnVideoStart, true, false);
            showHideViewVideo(btnVideoMute, true, false);
            showHideViewVideo(btnVideoSwitchCamera, true, true);
        } else {
            showHideViewVideo(btnVideoStart, false, false);
            showHideViewVideo(btnVideoMute, false, false);
            showHideViewVideo(btnVideoSwitchCamera, false, true);
            videoLayout.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    /**
     * Show or Hide the screen sharing options
     */
    private void showScreenOptions() {
        isScreenOptionsShow = !isScreenOptionsShow;

        if (isScreenOptionsShow) {
            btnScreen.setImageResource(R.drawable.ic_collapse_green_20dp);
            showHideViewScreen(btnScreenMute, true, false);
            showHideViewScreen(btnScreenStart, true, true);
        } else {
            showHideViewScreen(btnScreenMute, false, false);
            showHideViewScreen(btnScreenStart, false, true);
            screenLayout.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void showHideViewAudio(View view, boolean isShow, boolean isFinalView) {
        if (isShow) {
            view.animate()
                    .translationX(0)
                    .alpha(1.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            view.setVisibility(View.VISIBLE);
                            if (isFinalView) {
                                audioLayout.setBackground(getResources().getDrawable(R.drawable.frame_layout_round_border_tool));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnAudio.setTooltipText("Collapse buttons");
                                }
                            }
                        }
                    });
        } else {
            view.animate()
                    .translationX(btnAudio.getHeight())
                    .alpha(0.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            view.setVisibility(View.GONE);
                            if (isFinalView) {
                                btnAudio.setImageResource(R.drawable.ic_audio_green_20dp);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnAudio.setTooltipText("Audio options");
                                }
                            }
                        }
                    });
        }
    }

    private void showHideViewVideo(View view, boolean isShow, boolean isFinalView) {
        if (isShow) {
            view.animate()
                    .translationX(0)
                    .alpha(1.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            view.setVisibility(View.VISIBLE);
                            if (isFinalView) {
                                videoLayout.setBackground(getResources().getDrawable(R.drawable.frame_layout_round_border_tool));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnVideo.setTooltipText("Collapse buttons");
                                }
                            }
                        }
                    });
        } else {
            view.animate()
                    .translationX(btnVideo.getHeight())
                    .alpha(0.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            view.setVisibility(View.GONE);
                            if (isFinalView) {
                                btnVideo.setImageResource(R.drawable.ic_video_green_20dp);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnVideo.setTooltipText("Video options");
                                }
                            }
                        }
                    });
        }
    }

    private void showHideViewScreen(View view, boolean isShow, boolean isFinalView) {
        if (isShow) {
            view.animate()
                    .translationX(0)
                    .alpha(1.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            view.setVisibility(View.VISIBLE);
                            if (isFinalView) {
                                screenLayout.setBackground(getResources().getDrawable(R.drawable.frame_layout_round_border_tool));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnScreen.setTooltipText("Collapse buttons");
                                }
                            }
                        }
                    });
        } else {
            view.animate()
                    .translationX(btnScreen.getHeight())
                    .alpha(0.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            view.setVisibility(View.GONE);
                            if (isFinalView) {
                                btnScreen.setImageResource(R.drawable.ic_screen_share_green_20dp);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnScreen.setTooltipText("Screen sharing options");
                                }
                            }
                        }
                    });
        }
    }
}
