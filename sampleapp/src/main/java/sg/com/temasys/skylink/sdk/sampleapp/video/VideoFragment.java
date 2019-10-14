package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomImageButton;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomTriangleButton;
import sg.com.temasys.skylink.sdk.sampleapp.utils.SmallVideoViewFragment;
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
    private final String SELF_CAM_VIEW = "selfCameraVideo";
    private final String SELF_SCREEN_VIEW = "selfScreenVideo";
    private final String REMOTE_CAM_VIEW = "remoteCameraVideo";
    private final String REMOTE_SCREEN_VIEW = "remoteScreenVideo";
    private final String MAIN_VIEW = "main";

    // view widgets
    private LinearLayout videoViewLayout;
    private LinearLayout audioLayout, videoLayout, screenLayout;
    private RelativeLayout videoToolLayout;
    private CustomTriangleButton btnVideoRes;
    private ImageButton btnConnectDisconnect, btnAudio, btnVideo, btnScreen;
    private CustomImageButton btnAudioSpeaker, btnAudioMute, btnAudioRemove, btnAudioStart,
            btnVideoSwitchCamera, btnVideoMute, btnVideoRemove, btnVideoStart,
            btnScreenMute, btnScreenRemove, btnScreenStart;
    private Button stopScreenshareFloat, btnFullScreen;

    private WindowManager.LayoutParams stopScreenshareLayoutParams;

    // presenter instance to implement video logic
    private VideoContract.Presenter presenter;

    // local variables to keep views' state
    private boolean isShowScreenSharing = false;
    private boolean isFullScreen = false;
    private boolean isShowVideoRes = false;

    private SurfaceViewRenderer localCameraView, localScreenView, remoteCameraView, remoteScreenView;

    // private variables to keep the internal view states
    private boolean toConnectToRoom = true;
    private boolean isAudioOptionsShow = false;
    private boolean isVideoOptionsShow = false;
    private boolean isScreenOptionsShow = false;

    private Constants.VIDEO_TYPE currentMainVideoType = null;

    private boolean exitFromRoomByUser = false;

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

        // just in case that user are not sharing screen, then stop the camera
        presenter.onViewRequestResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        // just in case that user are not sharing screen, then stop the camera
        presenter.onViewRequestPause();
    }

    @Override
    public void onClick(View view) {
        //Defining a click event actions for the buttons
        switch (view.getId()) {
            case R.id.btnBack:
                processBack();
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
            case R.id.btn_connect_disconnect:
                processConnectDisconnect();
                break;
            case R.id.btn_audio:
                showAudioOptions();
                break;
            case R.id.btn_audio_start:
                presenter.onViewRequestStartAudio();
                break;
            case R.id.btn_audio_remove:
                presenter.onViewRequestRemoveAudio();
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
                presenter.onViewRequestToggleVideo();
                break;
            case R.id.btn_video_remove:
                presenter.onViewRequestRemoveVideo();
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
                presenter.onViewRequestToggleScreen();
                break;
            case R.id.btn_screen_remove:
                presenter.onViewRequestRemoveScreen();
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

            exitFromRoomByUser = true;

            if (isShowScreenSharing) {
                showHideButton(stopScreenshareFloat, false);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // delegate presenter to implement the permission results
        presenter.onViewRequestPermissionsResult(requestCode, permissions, grantResults);
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

        // reset the video resolution
        if (context != null && (context instanceof VideoActivity)) {
            ((VideoActivity) context).removeView(Constants.VIDEO_TYPE.REMOTE_CAMERA);
            ((VideoActivity) context).removeView(Constants.VIDEO_TYPE.REMOTE_SCREEN);
            ((VideoActivity) context).onShowHideRemoteCameraViewFragment(false, false);
            ((VideoActivity) context).onShowHideRemoteScreenViewFragment(false, false);
        }
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
    public void onPresenterRequestShowButtonStopScreenSharing() {
        if (!isShowScreenSharing) {
            showHideButton(stopScreenshareFloat, true);
        }
    }

    @Override
    public void onPresenterRequestLocalAudioCapture(String mediaId) {
        btnAudioSpeaker.setEnabled(true);
        if (Utils.isDefaultSpeakerSettingForVideo()) {
            btnAudioSpeaker.setMuted(false);
        } else {
            btnAudioSpeaker.setMuted(true);
        }

        btnAudioMute.setEnabled(true);
        btnAudioRemove.setEnabled(true);

        // set disable for btnAudioStart because we do not support stop audio atm
        btnAudioStart.setEnabled(false);

        // change UI button
        btnAudioStart.setStart(false);
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
        currentMainVideoType = Constants.VIDEO_TYPE.LOCAL_CAMERA;

        btnVideoSwitchCamera.setEnabled(true);
        btnVideoMute.setEnabled(true);
        btnVideoRemove.setEnabled(true);
        btnVideoStart.setStart(false);
        bringSmallViewToMainView(Constants.VIDEO_TYPE.LOCAL_CAMERA);
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
        currentMainVideoType = Constants.VIDEO_TYPE.LOCAL_SCREEN;

        btnScreenRemove.setEnabled(true);
        btnScreenMute.setEnabled(true);
        btnScreenMute.setMuted(false);
        btnScreenStart.setStart(false);

        bringSmallViewToMainView(Constants.VIDEO_TYPE.LOCAL_SCREEN);

        if (!isShowScreenSharing) {
            showHideButton(stopScreenshareFloat, true);
        }
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
        currentMainVideoType = Constants.VIDEO_TYPE.REMOTE_CAMERA;

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
        currentMainVideoType = Constants.VIDEO_TYPE.REMOTE_SCREEN;

        // move other views to small view
        bringSmallViewToMainView(Constants.VIDEO_TYPE.REMOTE_SCREEN);
    }

    @Override
    public void onPresenterRequestMediaStateChange(SkylinkMedia.MediaType mediaType, SkylinkMedia.MediaState mediaState, boolean isLocal) {
        if (isLocal) {
            switch (mediaState) {
                case ACTIVE:
                    if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
                        btnVideoMute.setEnabled(true);
                        btnVideoMute.setMuted(false);
                        btnVideoStart.setStart(false);
                        btnVideoRemove.setEnabled(true);
                        btnVideoSwitchCamera.setEnabled(true);
                    } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
                        btnScreenMute.setMuted(false);
                        btnScreenStart.setStart(false);
                        btnScreenMute.setEnabled(true);
                        btnScreenRemove.setEnabled(true);
                        if (!isShowScreenSharing) {
                            showHideButton(stopScreenshareFloat, true);
                        }
                    } else if (mediaType == SkylinkMedia.MediaType.AUDIO_MIC) {
                        btnAudioMute.setEnabled(true);
                        btnAudioRemove.setEnabled(true);
                        btnAudioSpeaker.setEnabled(true);

                        if (Utils.isDefaultSpeakerSettingForVideo()) {
                            btnAudioSpeaker.setMuted(false);
                        } else {
                            btnAudioSpeaker.setMuted(true);
                        }
                    }
                    break;
                case MUTED:
                    if (mediaType == SkylinkMedia.MediaType.AUDIO_MIC) {
                        btnAudioMute.setMuted(true);
                    } else if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
                        btnVideoStart.setStart(false);
                        btnVideoMute.setEnabled(true);
                        btnVideoSwitchCamera.setEnabled(true);
                        btnVideoMute.setMuted(true);
                    } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
                        btnScreenStart.setStart(false);
                        btnScreenMute.setEnabled(true);
                        btnScreenMute.setMuted(true);
                    }
                    break;
                case STOPPED:
                    if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
                        btnVideoStart.setStart(true);
                        btnVideoMute.setEnabled(true);
                        btnVideoSwitchCamera.setEnabled(false);
                    } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
                        btnScreenStart.setStart(true);
                        btnScreenMute.setEnabled(true);
                        if (isShowScreenSharing) {
                            showHideButton(stopScreenshareFloat, false);
                        }
                    }
                    break;
                case UNAVAILABLE:
                    if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
                        btnVideoStart.setEnabled(true);
                        btnVideoStart.setStart(true);
                        btnVideoMute.setEnabled(false);
                        btnVideoSwitchCamera.setEnabled(false);
                        btnVideoRemove.setEnabled(false);
                        removeView(SkylinkMedia.MediaType.VIDEO_CAMERA, true);
                    } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
                        btnScreenStart.setEnabled(true);
                        btnScreenStart.setStart(true);
                        btnScreenMute.setEnabled(false);
                        if (isShowScreenSharing) {
                            showHideButton(stopScreenshareFloat, false);
                        }
                        btnScreenRemove.setEnabled(false);
                        removeView(SkylinkMedia.MediaType.VIDEO_SCREEN, true);
                    } else if (mediaType == SkylinkMedia.MediaType.AUDIO_MIC) {
                        btnAudioSpeaker.setEnabled(false);
                        btnAudioMute.setEnabled(false);
                        btnAudioStart.setEnabled(true);
                        btnAudioStart.setStart(true);
                        btnAudioRemove.setEnabled(false);
                    }
                    break;
            }
        } else {
            if (mediaState == SkylinkMedia.MediaState.UNAVAILABLE) {
                removeView(mediaType, false);
            }
        }
    }

    /**
     * Remove a specific view, both main view and small views
     */
    private void removeView(SkylinkMedia.MediaType videoType, boolean isLocal) {
        // remove the small video views and main view
        switch (videoType) {
            case VIDEO_CAMERA:
                showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, isLocal, false);
                if (currentMainVideoType == Constants.VIDEO_TYPE.LOCAL_CAMERA) {
                    localCameraView = null;
                    ((VideoActivity) context).removeView(Constants.VIDEO_TYPE.LOCAL_CAMERA);
                }
                if (currentMainVideoType == Constants.VIDEO_TYPE.REMOTE_CAMERA) {
                    localScreenView = null;
                    ((VideoActivity) context).removeView(Constants.VIDEO_TYPE.LOCAL_SCREEN);
                }
                break;
            case VIDEO_SCREEN:
                showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, isLocal, false);
                if (currentMainVideoType == Constants.VIDEO_TYPE.LOCAL_SCREEN) {
                    remoteCameraView = null;
                    ((VideoActivity) context).removeView(Constants.VIDEO_TYPE.REMOTE_CAMERA);
                }
                if (currentMainVideoType == Constants.VIDEO_TYPE.REMOTE_SCREEN) {
                    remoteScreenView = null;
                    ((VideoActivity) context).removeView(Constants.VIDEO_TYPE.REMOTE_SCREEN);
                }
                break;
        }
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
     * Update the audio output/speaker button when being requested from presenter
     * For example: when the phone connects to a bluetooth headset, the speaker is automatically off
     */
    @Override
    public void onPresenterRequestChangeSpeakerOutput(boolean isSpeakerOn) {
        btnAudioSpeaker.setMuted(!isSpeakerOn);

        if (isSpeakerOn) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnAudioSpeaker.setTooltipText("Enable headset");
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                btnAudioSpeaker.setTooltipText("Enable speaker");
            }
        }
    }

    @Override
    public void onPresenterRequestChangeDefaultVideoDevice(SkylinkConfig.VideoDevice videoDevice) {
        Config.setPrefString(Config.DEFAULT_VIDEO_DEVICE, videoDevice.getDeviceName(), (VideoActivity) context);
    }

    @Override
    public void onPresenterRequestShowHideSmallView(SkylinkMedia.MediaType mediaType, boolean isShow) {
        //show/hide all small views if mediaType is null
        if (mediaType == null) {
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, true, isShow);
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, true, isShow);
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, false, isShow);
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, false, isShow);
        } else {
            showHideSmallFragment(mediaType, true, isShow);
            showHideSmallFragment(mediaType, false, isShow);
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
        btnAudioRemove = rootView.findViewById(R.id.btn_audio_remove);
        btnAudioStart = rootView.findViewById(R.id.btn_audio_start);
        btnAudio = rootView.findViewById(R.id.btn_audio);
        btnVideoSwitchCamera = rootView.findViewById(R.id.btn_video_switch_camera);
        btnVideoMute = rootView.findViewById(R.id.btn_video_mute);
        btnVideoRemove = rootView.findViewById(R.id.btn_video_remove);
        btnVideoStart = rootView.findViewById(R.id.btn_video_start);
        btnVideo = rootView.findViewById(R.id.btn_video);
        btnScreenMute = rootView.findViewById(R.id.btn_screen_mute);
        btnScreenRemove = rootView.findViewById(R.id.btn_screen_remove);
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
        btnAudioSpeaker.setDrawableActive(getResources().getDrawable(R.drawable.ic_speaker_white_20dp));
        btnAudioSpeaker.setDrawableDisable(getResources().getDrawable(R.drawable.ic_speaker_disable_20dp));
        btnAudioSpeaker.setDrawableMuted(getResources().getDrawable(R.drawable.ic_headset_white_20dp));
        btnAudioSpeaker.setDrawableMutedDisable(getResources().getDrawable(R.drawable.ic_headset_disable_20dp));

        btnAudioMute.setDrawableActive(getResources().getDrawable(R.drawable.ic_audio_mute_white_20dp));
        btnAudioMute.setDrawableDisable(getResources().getDrawable(R.drawable.ic_mic_off_disable_20dp));
        btnAudioMute.setDrawableMuted(getResources().getDrawable(R.drawable.ic_mic_active_white_20dp));
        btnAudioMute.setDrawableMutedDisable(getResources().getDrawable(R.drawable.ic_mic_disable_20dp));

        btnAudioRemove.setDrawableActive(getResources().getDrawable(R.drawable.ic_remove_white_20dp));
        btnAudioRemove.setDrawableDisable(getResources().getDrawable(R.drawable.ic_remove_disable_20dp));

        btnAudioStart.setDrawableActive(getResources().getDrawable(R.drawable.ic_start_white_20dp));
        btnAudioStart.setDrawableDisable(getResources().getDrawable(R.drawable.ic_start_disable_20dp));
        btnAudioStart.setDrawableStart(getResources().getDrawable(R.drawable.ic_start_white_20dp));
        btnAudioStart.setDrawableStop(getResources().getDrawable(R.drawable.ic_stop_white_20dp));

        btnVideoSwitchCamera.setDrawableActive(getResources().getDrawable(R.drawable.ic_switch_camera_white_20dp));
        btnVideoSwitchCamera.setDrawableDisable(getResources().getDrawable(R.drawable.ic_switch_camera_disable_20dp));

        btnVideoMute.setDrawableActive(getResources().getDrawable(R.drawable.ic_videocam_mute_white_20dp));
        btnVideoMute.setDrawableDisable(getResources().getDrawable(R.drawable.ic_videocam_mute_disable_20dp));
        btnVideoMute.setDrawableMuted(getResources().getDrawable(R.drawable.ic_videocam_active_white_20dp));
        btnVideoMute.setDrawableMutedDisable(getResources().getDrawable(R.drawable.ic_videocam_active_disable_20dp));

        btnVideoRemove.setDrawableActive(getResources().getDrawable(R.drawable.ic_remove_white_20dp));
        btnVideoRemove.setDrawableDisable(getResources().getDrawable(R.drawable.ic_remove_disable_20dp));

        btnVideoStart.setDrawableActive(getResources().getDrawable(R.drawable.ic_start_white_20dp));
        btnVideoStart.setDrawableDisable(getResources().getDrawable(R.drawable.ic_start_disable_20dp));
        btnVideoStart.setDrawableStart(getResources().getDrawable(R.drawable.ic_start_white_20dp));
        btnVideoStart.setDrawableStop(getResources().getDrawable(R.drawable.ic_stop_white_20dp));

        btnScreenMute.setDrawableActive(getResources().getDrawable(R.drawable.ic_stop_screen_share_white_20dp));
        btnScreenMute.setDrawableDisable(getResources().getDrawable(R.drawable.ic_stop_screen_share_disable_20dp));
        btnScreenMute.setDrawableMuted(getResources().getDrawable(R.drawable.ic_start_screen_share_20dp));
        btnScreenMute.setDrawableMutedDisable(getResources().getDrawable(R.drawable.ic_start_screen_share_disable_20dp));

        btnScreenRemove.setDrawableActive(getResources().getDrawable(R.drawable.ic_remove_white_20dp));
        btnScreenRemove.setDrawableDisable(getResources().getDrawable(R.drawable.ic_remove_disable_20dp));

        btnScreenStart.setDrawableActive(getResources().getDrawable(R.drawable.ic_start_white_20dp));
        btnScreenStart.setDrawableDisable(getResources().getDrawable(R.drawable.ic_start_disable_20dp));
        btnScreenStart.setDrawableStart(getResources().getDrawable(R.drawable.ic_start_white_20dp));
        btnScreenStart.setDrawableStop(getResources().getDrawable(R.drawable.ic_stop_white_20dp));

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
        btnAudioRemove.setOnClickListener(this);
        btnAudioStart.setOnClickListener(this);
        btnAudio.setOnClickListener(this);
        btnVideoSwitchCamera.setOnClickListener(this);
        btnVideoMute.setOnClickListener(this);
        btnVideoRemove.setOnClickListener(this);
        btnVideoStart.setOnClickListener(this);
        btnVideo.setOnClickListener(this);
        btnScreenMute.setOnClickListener(this);
        btnScreenRemove.setOnClickListener(this);
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

        // set disable for some buttons
        btnAudioSpeaker.setEnabled(false);
        btnAudioMute.setEnabled(false);
        btnAudioRemove.setEnabled(false);
        btnVideoSwitchCamera.setEnabled(false);
        btnVideoMute.setEnabled(false);
        btnVideoRemove.setEnabled(false);
        btnScreenMute.setEnabled(false);
        btnScreenRemove.setEnabled(false);

        // Add an system overlay button for stop screen share
        stopScreenshareFloat = new Button(getActivity());
        stopScreenshareFloat.setText(getActivity().getResources().getText(R.string.stop_screenShare));
        stopScreenshareFloat.setTextColor(getActivity().getResources().getColor(R.color.color_white));
        stopScreenshareFloat.setTextSize(getActivity().getResources().getDimension(R.dimen.stop_screen_text_size));
        stopScreenshareFloat.setBackground(getActivity().getResources().getDrawable(R.drawable.button_stop_screen_sharing));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            stopScreenshareLayoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }
//        else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//            stopScreenshareLayoutParams = new WindowManager.LayoutParams(
//                    WindowManager.LayoutParams.WRAP_CONTENT,
//                    WindowManager.LayoutParams.WRAP_CONTENT,
//                    WindowManager.LayoutParams.TYPE_TOAST,
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                    PixelFormat.TRANSLUCENT);
//        } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
//            stopScreenshareLayoutParams = new WindowManager.LayoutParams(
//                    WindowManager.LayoutParams.WRAP_CONTENT,
//                    WindowManager.LayoutParams.WRAP_CONTENT,
//                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                    PixelFormat.TRANSLUCENT);
//        }
        else {
            stopScreenshareLayoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }

        stopScreenshareLayoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

        stopScreenshareFloat.setOnClickListener(view -> {
            // stop capturing screen
            presenter.onViewRequestToggleScreen(false);
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
            btnAudioRemove.setTooltipText("Remove audio");
            btnAudioStart.setTooltipText("Start/Stop audio");
            btnAudio.setTooltipText("Audio setting options");
            btnVideoSwitchCamera.setTooltipText("Switch camera");
            btnVideoMute.setTooltipText("Mute/Active video");
            btnVideoRemove.setTooltipText("Remove video");
            btnVideoStart.setTooltipText("Start/Stop video");
            btnVideo.setTooltipText("Video setting options");
            btnScreenMute.setTooltipText("Mute/Active screen sharing");
            btnScreenRemove.setTooltipText("Remove screen sharing");
            btnScreenStart.setTooltipText("Start/Stop screen sharing");
            btnScreen.setTooltipText("Screen sharing setting optiosn");
        }

        if (Utils.isDefaultSpeakerSettingForVideo()) {
            btnAudioSpeaker.setMuted(false);
        } else {
            btnAudioSpeaker.setMuted(true);
        }

        // start a local video base on default device setting
        presenter.onViewRequestStartLocalMediaIfConfigAllow();
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

    private void showHideButton(View view, boolean isShow) {
        if (view == null)
            return;

        if (view == stopScreenshareFloat) {
            if (getActivity() == null)
                return;

            WindowManager windowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
            if (isShow && !isShowScreenSharing) {
                windowManager.addView(stopScreenshareFloat, stopScreenshareLayoutParams);
                isShowScreenSharing = true;
            } else if (!isShow && isShowScreenSharing) {
                windowManager.removeView(stopScreenshareFloat);
                isShowScreenSharing = false;
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

            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, true, false);
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, true, false);
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, false, false);
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, false, false);
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
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, true, true);
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, true, true);
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, false, true);
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, false, true);
            if (isShowVideoRes) {
                ((VideoActivity) getActivity()).onShowHideVideoResFragment(true);
            }
            btnFullScreen.setBackground(getActivity().getResources().getDrawable(R.drawable.ic_full_screen));
            actionBar.show();

            bringSmallViewToMainView(currentMainVideoType);

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

    /**
     * Bring the small available on small view to main big view
     * and move all other views to small views
     *
     * @param videoType the type of small view to move to main big view
     */
    public void bringSmallViewToMainView(Constants.VIDEO_TYPE videoType) {
        if (videoType == null)
            return;

        currentMainVideoType = videoType;

        switch (videoType) {
            case LOCAL_CAMERA:
                if (localCameraView != null) {
                    addViewToMain(localCameraView);
                    showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, true, false);
                }
                moveViewToSmallLocalScreenView(localScreenView);
                moveViewToSmallRemoteCameraView(remoteCameraView);
                moveViewToSmallRemoteScreenView(remoteScreenView);

                break;
            case LOCAL_SCREEN:
                if (localScreenView != null) {
                    addViewToMain(localScreenView);
                    showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, true, false);
                }
                moveViewToSmallLocalCameraView(localCameraView);
                moveViewToSmallRemoteCameraView(remoteCameraView);
                moveViewToSmallRemoteScreenView(remoteScreenView);
                break;
            case REMOTE_CAMERA:
                if (remoteCameraView != null) {
                    addViewToMain(remoteCameraView);
                    showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, false, false);
                }
                moveViewToSmallLocalCameraView(localCameraView);
                moveViewToSmallLocalScreenView(localScreenView);
                moveViewToSmallRemoteScreenView(remoteScreenView);
                break;
            case REMOTE_SCREEN:
                if (remoteScreenView != null) {
                    addViewToMain(remoteScreenView);
                    showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, false, false);
                }
                moveViewToSmallLocalCameraView(localCameraView);
                moveViewToSmallLocalScreenView(localScreenView);
                moveViewToSmallRemoteCameraView(remoteCameraView);
                break;
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
        // if user exit from the room by intentionally press back button or navigate up button,
        // no need to change the UI to avoid crashing the app
        if (exitFromRoomByUser) {
            return;
        }

        // remove video views
        View selfCamera = videoViewLayout.findViewWithTag(SELF_CAM_VIEW);
        if (selfCamera != null) {
            videoViewLayout.removeView(selfCamera);
        }

        View selfScreen = videoViewLayout.findViewWithTag(SELF_SCREEN_VIEW);
        if (selfScreen != null) {
            videoViewLayout.removeView(selfScreen);
        }

        View peerCamera = videoViewLayout.findViewWithTag(REMOTE_CAM_VIEW);
        if (peerCamera != null) {
            videoViewLayout.removeView(peerCamera);
        }

        View peerScreen = videoViewLayout.findViewWithTag(REMOTE_SCREEN_VIEW);
        if (peerScreen != null) {
            videoViewLayout.removeView(peerScreen);
        }

        View main = videoViewLayout.findViewWithTag(MAIN_VIEW);
        if (main != null) {
            videoViewLayout.removeView(main);
        }

        if (isShowScreenSharing) {
            showHideButton(stopScreenshareFloat, false);
        }

        // change the connect/disconnect button
        btnConnectDisconnect.setImageResource(R.drawable.ic_connect_white_25dp);
        btnConnectDisconnect.setBackground(getResources().getDrawable(R.drawable.button_circle_connect_to_room));

        btnAudioSpeaker.setEnabled(false);
        btnAudioMute.setEnabled(false);
        btnAudioRemove.setEnabled(false);
        btnAudioStart.setStart(true);
        btnAudioStart.setEnabled(true);

        btnVideoSwitchCamera.setEnabled(false);
        btnVideoMute.setEnabled(false);
        btnVideoRemove.setEnabled(false);
        btnVideoStart.setStart(true);
        btnVideoStart.setEnabled(true);

        btnScreenMute.setEnabled(false);
        btnScreenRemove.setEnabled(false);
        btnScreenStart.setStart(true);
        btnScreenStart.setEnabled(true);

        // reset the room id info and local peer button
        txtRoomId.setText(R.string.guide_room_id);
        btnLocalPeer.setVisibility(GONE);
        btnRemotePeer1.setVisibility(GONE);
        btnRemotePeer2.setVisibility(GONE);
        btnRemotePeer3.setVisibility(GONE);

        // reset the variable toConnectToRoom
        toConnectToRoom = true;

        // reset the video resolution
        if (context != null && (context instanceof VideoActivity)) {
            ((VideoActivity) context).resetResolution();
            ((VideoActivity) context).resetSmallViews();
        }
    }

    /**
     * Show or Hide the audio options
     */
    private void showAudioOptions() {
        isAudioOptionsShow = !isAudioOptionsShow;

        if (isAudioOptionsShow) {
            btnAudio.setImageResource(R.drawable.ic_collapse_green_20dp);
            showHideViewAudioOption(btnAudioStart, true, false);
            showHideViewAudioOption(btnAudioRemove, true, false);
            showHideViewAudioOption(btnAudioMute, true, false);
            showHideViewAudioOption(btnAudioSpeaker, true, true);
        } else {
            showHideViewAudioOption(btnAudioStart, false, false);
            showHideViewAudioOption(btnAudioRemove, false, false);
            showHideViewAudioOption(btnAudioMute, false, false);
            showHideViewAudioOption(btnAudioSpeaker, false, true);
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
            showHideViewVideoOption(btnVideoStart, true, false);
            showHideViewVideoOption(btnVideoRemove, true, false);
            showHideViewVideoOption(btnVideoMute, true, false);
            showHideViewVideoOption(btnVideoSwitchCamera, true, true);
        } else {
            showHideViewVideoOption(btnVideoStart, false, false);
            showHideViewVideoOption(btnVideoRemove, false, false);
            showHideViewVideoOption(btnVideoMute, false, false);
            showHideViewVideoOption(btnVideoSwitchCamera, false, true);
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
            showHideViewScreenOption(btnScreenMute, true, false);
            showHideViewScreenOption(btnScreenRemove, true, false);
            showHideViewScreenOption(btnScreenStart, true, true);
        } else {
            showHideViewScreenOption(btnScreenMute, false, false);
            showHideViewScreenOption(btnScreenRemove, false, false);
            showHideViewScreenOption(btnScreenStart, false, true);
            screenLayout.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void showHideViewAudioOption(View view, boolean isShow, boolean isFinalView) {
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

    private void showHideViewVideoOption(View view, boolean isShow, boolean isFinalView) {
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

    private void showHideViewScreenOption(View view, boolean isShow, boolean isFinalView) {
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

    private void showHideSmallFragment(SkylinkMedia.MediaType mediaType, boolean isLocal, boolean isShow) {
        if (context == null || !(context instanceof VideoActivity)) {
            return;
        }

        SmallVideoViewFragment localVideoFragment = null;

        switch (mediaType) {
            case VIDEO_CAMERA:
                if (isLocal)
                    localVideoFragment = ((VideoActivity) context).getLocalVideoCameraFragment();
                else
                    localVideoFragment = ((VideoActivity) context).getRemoteVideoCameraFragment();
                break;
            case VIDEO_SCREEN:
                if (isLocal)
                    localVideoFragment = ((VideoActivity) context).getLocalVideoScreenFragment();
                else
                    localVideoFragment = ((VideoActivity) context).getRemoteVideoScreenFragment();
                break;
        }

        if (localVideoFragment == null) {
            return;
        }

        if (isShow) {
            ((VideoActivity) context).attachSmallView(localVideoFragment);
        } else {
            ((VideoActivity) context).detachSmallView(localVideoFragment);
        }
    }
}
