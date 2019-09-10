package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.VideoService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoLocalState;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionPresenter;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for implementing video logic.
 * User can both display video view from camera front/back and video view from screen
 */

public class VideoPresenter extends BasePresenter implements VideoContract.Presenter {

    private final String TAG = VideoPresenter.class.getName();

    private Context context;

    // The view instances for remote peer screen share view and remote peer camera view
    public VideoContract.MainView mainView;

    // The service instance
    private VideoService videoService;

    // The video resolution presenter
    private VideoResolutionPresenter videoResPresenter;

    //utils to process permission
    private PermissionUtils permissionUtils;

    // the current speaker output {speaker/headset}
    private boolean currentVideoSpeaker = Utils.isDefaultSpeakerSettingForVideo();

    // the current state of local video {audio, video, camera}
    private VideoLocalState currentVideoLocalState = new VideoLocalState();

    public VideoPresenter(Context context) {
        this.context = context;
        this.videoService = new VideoService(context);
        this.videoService.setPresenter(this);
        this.permissionUtils = new PermissionUtils();

        // set the init state of currentVideoLocalState, so we can start capturing later
        currentVideoLocalState.setCameraCapturerStop(true);
        currentVideoLocalState.setScreenCapturerStop(true);
    }

    public void setMainView(VideoContract.MainView view) {
        mainView = view;
        mainView.setPresenter(this);
    }

    /**
     * inject the video resolution presenter into this presenter
     * in order to let video resolution presenter handles the video resolution logic
     * both this presenter and video resolution presenter use the video call service
     */
    public void setVideoResPresenter(VideoResolutionPresenter videoResolutionPresenter) {
        this.videoResPresenter = videoResolutionPresenter;
        this.videoResPresenter.setService(this.videoService);
        this.videoService.setResPresenter(this.videoResPresenter);
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for view to call
    // These methods are responsible for processing requests from view
    //----------------------------------------------------------------------------------------------

    @Override
    public void onViewRequestConnectedLayout() {
        Log.d(TAG, "onViewLayoutRequested");

        //connect to room on Skylink connection
        processConnectToRoom();

        //default setting for video output
        this.currentVideoSpeaker = Utils.isDefaultSpeakerSettingForVideo();

        //get default audio output settings
        mainView.onPresenterRequestChangeSpeakerOutput(this.currentVideoSpeaker);

        //after connected to skylink SDK, UI will be updated latter on onServiceRequestConnect
    }

    @Override
    public void onViewRequestResume() {

        // do not process if user has not started video from camera
        if (videoService.getLocalVideoId() == null) {
            return;
        }

        // turn back camera to previous state
        boolean localCameraStop = currentVideoLocalState.isCameraCapturerStop();

        videoService.toggleVideo(!localCameraStop);

    }

    @Override
    public void onViewRequestPause() {
        //stop camera when pausing so that camera will be available for the others to use
        videoService.toggleVideo(false);
    }

    @Override
    public void onViewRequestDisconnectFromRoom() {
        videoService.disconnectFromRoom();
    }

    @Override
    public void onViewRequestExit() {
        //process disconnect from room if connecting
        //after disconnected from skylink SDK, UI will be updated latter on onServiceRequestDisconnect
        if (!videoService.disconnectFromRoom()) {
            // if not connecting to the room, dispose the local media
            videoService.disposeLocalMedia();
        }
    }

    @Override
    public void onViewRequestChangeAudioState() {
        boolean isAudioMute = currentVideoLocalState.isAudioMute();

        processAudioStateChanged(!isAudioMute);

        currentVideoLocalState.setAudioMute(!isAudioMute);
    }

    @Override
    public void onViewRequestChangeVideoState() {
        boolean isVideoMute = currentVideoLocalState.isVideoMute();

        processVideoStateChanged(!isVideoMute);

        currentVideoLocalState.setVideoMute(!isVideoMute);
    }

    @Override
    public void onViewRequestChangeScreenState() {
        boolean isScreenMute = currentVideoLocalState.isScreenMute();

        processScreenStateChanged(!isScreenMute);

        currentVideoLocalState.setScreenMute(!isScreenMute);
    }

    @Override
    public void onViewRequestChangeSpeakerOutput() {
        //change current speakerOn
        this.currentVideoSpeaker = !this.currentVideoSpeaker;

        // use service layer to change the audio output, update UI will be called later in onServiceRequestAudioOutputChanged
        videoService.changeSpeakerOutput(this.currentVideoSpeaker);
    }

    @Override
    public void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, TAG);
    }

    @Override
    public void onViewRequestActivityResult(int requestCode, int resultCode, Intent data) {
        permissionUtils.onRequestActivityResultHandler(requestCode, resultCode, data);

        // for displaying overlay button permission
        if (resultCode == Activity.RESULT_OK && data != null) {
            // send request permission for displaying overlay button
            if (requestButtonOverlayPermission()) {
                mainView.onPresenterRequestShowButtonStopScreenSharing();
            }
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(context)) {
                mainView.onPresenterRequestShowButtonStopScreenSharing();
            } else {
                if (permissionUtils.isSendOverlayAlready()) {
                    permissionUtils.displayOverlayButtonPermissionWarning(context);
                } else {
                    requestButtonOverlayPermission();
                }
            }
        }
    }

    /**
     * Get the specific peer object according to the index
     */
    @Override
    public SkylinkPeer onViewRequestGetPeerByIndex(int index) {
        return videoService.getPeerByIndex(index);
    }

    @Override
    public void onViewRequestSwitchCamera() {
        videoService.switchCamera();
    }

    @Override
    public void onViewRequestStartAudio() {
        videoService.startLocalAudio();
    }

    @Override
    public void onViewRequestToggleVideo() {
        // implement start or stop video base on the state of the current video from camera
        boolean isCameraStart = currentVideoLocalState.isCameraCapturerStop();

        videoService.toggleVideo(isCameraStart);

        currentVideoLocalState.setCameraCapturerStop(!isCameraStart);
    }

    @Override
    public void onViewRequestToggleScreen() {
        boolean isScreenStart = currentVideoLocalState.isScreenCapturerStop();

        videoService.toggleScreen(isScreenStart);

        currentVideoLocalState.setScreenCapturerStop(!isScreenStart);
    }

    @Override
    public void onViewRequestToggleScreen(boolean start) {
        // implement start or stop video base on the state of the current screen video
        videoService.toggleScreen(start);

        currentVideoLocalState.setScreenCapturerStop(!start);
    }

    @Override
    public void onViewRequestRemoveAudio() {
        videoService.removeLocalAudio();
    }

    @Override
    public void onViewRequestRemoveVideo() {
        videoService.removeLocalVideo();
    }

    @Override
    public void onViewRequestRemoveScreen() {
        videoService.removeLocalScreen();
    }

    @Override
    public void onViewRequestStartLocalMediaIfConfigAllow() {
        String log = "[SA][onViewRequestStartLocalMediaIfConfigAllow] ";
        if (Utils.isDefaultNoneVideoDeviceSetting()) {
            log += " Default video device setting is No device. So do not start any local media automatically! ";
            Log.w(TAG, log);
            return;
        }

        // start local audio
        videoService.startLocalAudio();

        // check the default setting for video device and start local video accordingly
        if (Utils.isDefaultCameraDeviceSetting()) {
            videoService.startLocalVideo();
            return;
        }

        if (Utils.isDefaultScreenDeviceSetting()) {
            videoService.startLocalScreen();
            return;
        }

        // we create a custom video device from back camera of the device, so start custom video device
        // will similarly start back camera
        if (Utils.isDefaultCustomVideoDeviceSetting()) {
            videoService.startLocalCustomVideo();
            return;
        }
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful) {
            processUpdateStateConnected();
        } else {
            processDisconnectUIChange();
        }
    }

    @Override
    public void onServiceRequestDisconnect() {
        processDisconnectUIChange();

        //stop audio routing
        SkylinkConfig skylinkConfig = videoService.getSkylinkConfig();
        if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
            AudioRouter.stopAudioRouting(context);
        }

        // reset class variables
        currentVideoLocalState = new VideoLocalState();

//        mainView.onPresenterRequestShowHideSmallView(null, false);
    }

    @Override
    public void onServiceRequestIntentRequired(Intent intent, int requestCode, int infoCode) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onIntentRequiredHandler(intent, requestCode, infoCode, (Activity) context);
    }

    @Override
    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onPermissionRequiredHandler(info, TAG, context, mainView.onPresenterRequestGetFragmentInstance());
    }

    @Override
    public void onServiceRequestLocalAudioCapture(SkylinkMedia localAudio) {
        String log = "[SA][onServiceRequestLocalAudioCapture] ";

        //start audio routing if has audio config
        SkylinkConfig skylinkConfig = videoService.getSkylinkConfig();
        if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
            AudioRouter.setPresenter(this);
            AudioRouter.startAudioRouting(context, Constants.CONFIG_TYPE.VIDEO);

            // use service layer to change the audio output, update UI will be called later in onServiceRequestAudioOutputChanged
            videoService.changeSpeakerOutput(this.currentVideoSpeaker);
        }

        //notify view to change the UI
        mainView.onPresenterRequestLocalAudioCapture(localAudio.getMediaId());
    }

    @Override
    public void onServiceRequestLocalCameraCapture(SkylinkMedia localVideo) {
        String log = "[SA][onServiceRequestLocalCameraCapture] ";
        SurfaceViewRenderer selfVideoView = localVideo.getVideoView();

        if (selfVideoView == null) {
            log += "VideoView is null! Try to get video from SDK";
            Log.w(TAG, log);

            selfVideoView = videoService.getVideoView(null, localVideo.getMediaId());
        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
        }

        //notify view to change the UI
        mainView.onPresenterRequestAddCameraSelfView(localVideo.getMediaId(), selfVideoView);

        // change state of currentVideoLocalState
        currentVideoLocalState.setCameraCapturerStop(false);
    }

    @Override
    public void onServiceRequestLocalScreenCapture(SkylinkMedia localScreen) {
        String log = "[SA][onServiceRequestLocalScreenCapture] ";
        SurfaceViewRenderer selfVideoView = localScreen.getVideoView();

        if (selfVideoView == null) {
            log += "VideoView is null! Try to get video from SDK";
            Log.w(TAG, log);

            selfVideoView = videoService.getVideoView(null, localScreen.getMediaId());
        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
        }

        //notify view to change the UI
        mainView.onPresenterRequestAddScreenSelfView(localScreen.getMediaId(), selfVideoView);

        // change state of currentVideoLocalState
        currentVideoLocalState.setScreenCapturerStop(false);
    }

    @Override
    public void onServiceRequestChangeDefaultVideoDevice(SkylinkConfig.VideoDevice videoDevice) {
        mainView.onPresenterRequestChangeDefaultVideoDevice(videoDevice);
    }

    @Override
    public void onServiceRequestMediaStateChange(SkylinkMedia media, boolean isLocal) {
        // change the UI
        mainView.onPresenterRequestMediaStateChange(media.getMediaType(), media.getMediaState(), isLocal);

        // change the currentVideoLocalState
        if (media.getMediaType() == SkylinkMedia.MediaType.VIDEO_CAMERA && isLocal) {
            if (media.getMediaState() == SkylinkMedia.MediaState.MUTED) {
                currentVideoLocalState.setVideoMute(true);
            } else if (media.getMediaState() == SkylinkMedia.MediaState.STOPPED) {
                currentVideoLocalState.setCameraCapturerStop(true);
            } else if (media.getMediaState() == SkylinkMedia.MediaState.ACTIVE) {
                currentVideoLocalState.setVideoMute(false);
                currentVideoLocalState.setCameraCapturerStop(false);
            } else if (media.getMediaState() == SkylinkMedia.MediaState.UNAVAILABLE) {
                currentVideoLocalState.setCameraCapturerStop(true);
            }
        } else if (media.getMediaType() == SkylinkMedia.MediaType.VIDEO_SCREEN && isLocal) {
            if (media.getMediaState() == SkylinkMedia.MediaState.MUTED) {
                currentVideoLocalState.setScreenMute(true);
            } else if (media.getMediaState() == SkylinkMedia.MediaState.STOPPED) {
                currentVideoLocalState.setScreenCapturerStop(true);
            } else if (media.getMediaState() == SkylinkMedia.MediaState.ACTIVE) {
                currentVideoLocalState.setScreenMute(false);
                currentVideoLocalState.setScreenCapturerStop(false);
            } else if (media.getMediaState() == SkylinkMedia.MediaState.UNAVAILABLE) {
                currentVideoLocalState.setScreenCapturerStop(true);
            }
        } else if (media.getMediaType() == SkylinkMedia.MediaType.AUDIO_MIC && isLocal) {
            if (media.getMediaState() == SkylinkMedia.MediaState.MUTED) {
                currentVideoLocalState.setAudioMute(true);
            } else if (media.getMediaState() == SkylinkMedia.MediaState.ACTIVE) {
                currentVideoLocalState.setAudioMute(false);
            }
        }
    }

    @Override
    public void onServiceRequestRemotePeerJoin(SkylinkPeer remotePeer) {
        // Fill the new peer in button in custom bar
        mainView.onPresenterRequestChangeUiRemotePeerJoin(remotePeer, videoService.getTotalPeersInRoom() - 2);
    }

    @Override
    public void onServiceRequestRemotePeerLeave(SkylinkPeer remotePeer, int removeIndex) {
        // do not process if the left peer is local peer
        if (removeIndex == -1)
            return;

        // Remove the peer in button in custom bar
        mainView.onPresenterRequestChangeUiRemotePeerLeft(videoService.getPeersList());

        // remove the remote peer video view
        mainView.onPresenterRequestRemoveRemotePeer();
    }

    @Override
    public void onServiceRequestRemotePeerConnectionRefreshed(String log, UserInfo
            remotePeerUserInfo) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        toastLog(TAG, context, log);
    }

    @Override
    public void onServiceRequestRemotePeerAudioReceive(String log, UserInfo remotePeerUserInfo,
                                                       String remotePeerId, SkylinkMedia remoteAudio) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n";
        Log.d(TAG, log);
        toastLog(TAG, context, log);
    }

    @Override
    public void onServiceRequestRemotePeerVideoReceive(String log, UserInfo remotePeerUserInfo,
                                                       String remotePeerId, SkylinkMedia remoteVideo) {
        // add the remote video view in to the view
        processAddRemoteView(remotePeerId, remoteVideo);

        log += "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        Log.d(TAG, log);
        toastLog(TAG, context, log);
    }

    @Override
    public void onServiceRequestAudioOutputChanged(boolean isSpeakerOn) {
        mainView.onPresenterRequestChangeSpeakerOutput(isSpeakerOn);
        this.currentVideoSpeaker = isSpeakerOn;

        if (isSpeakerOn) {
            String log = context.getString(R.string.enable_speaker);
            toastLog(TAG, context, log);
        } else {
            String log = context.getString(R.string.enable_headset);
            toastLog(TAG, context, log);
        }
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Process connect to room on service layer and update UI accordingly
     */
    private void processConnectToRoom() {

        //connect to SDK
        videoService.connectToRoom(Constants.CONFIG_TYPE.VIDEO);
    }

    /**
     * Update UI into disconnected state
     */
    private void processDisconnectUIChange() {
        // update UI
        mainView.onPresenterRequestDisconnectUIChange();
    }

    /**
     * process file permission that comes from the app
     * when user first choose browsing file from device, permission request dialog will be display
     */
    private boolean requestButtonOverlayPermission() {
        return permissionUtils.requestButtonOverlayPermission(context, mainView.onPresenterRequestGetFragmentInstance());
    }

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processAudioStateChanged(boolean isAudioMuted) {
        //set mute audio to sdk
        videoService.muteLocalAudio(isAudioMuted);
    }

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processVideoStateChanged(boolean isVideoMuted) {
        //set mute audio to sdk
        videoService.muteLocalVideo(isVideoMuted);
    }

    // If screen video is enabled, mute screen video and if screen video is mute, then enable it
    private void processScreenStateChanged(boolean isScreenMuted) {
        //set mute screen video to sdk
        videoService.muteLocalScreen(isScreenMuted);
    }

    /**
     * Get video view by remote peer id
     */
    private SurfaceViewRenderer processGetVideoView(String remotePeerId, String mediaId) {
        return videoService.getVideoView(remotePeerId, mediaId);
    }

    /**
     * Get the remote video view from peer id
     */
    private SurfaceViewRenderer processGetRemoteView(String remotePeerId, String mediaId) {
        SurfaceViewRenderer videoView;
        // Proceed only if the first (& only) remote Peer has joined.
        if (remotePeerId == null) {
            return null;
        } else {
            videoView = processGetVideoView(remotePeerId, mediaId);
        }

        return videoView;
    }

    /**
     * Add remote video view into the layout
     */
    private void processAddRemoteView(String remotePeerId, SkylinkMedia remoteMedia) {

        SurfaceViewRenderer videoView = remoteMedia.getVideoView();

        if (videoView == null) {
            videoView = processGetRemoteView(remotePeerId, remoteMedia.getMediaId());
        }

        if (videoView == null)
            return;

        // setTag for the remote video view
        videoView.setTag(remoteMedia.getMediaId());

        if (remoteMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_CAMERA || remoteMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO) {
            mainView.onPresenterRequestAddCameraRemoteView(videoView);
        } else if (remoteMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            mainView.onPresenterRequestAddScreenRemoteView(videoView);
        }
    }

    /**
     * Update UI when connected to room
     */
    private void processUpdateStateConnected() {
        mainView.onPresenterRequestUpdateUIConnected(processGetRoomId());
    }

    /**
     * Get the room id info
     */
    private String processGetRoomId() {
        return videoService.getRoomId();
    }
}
