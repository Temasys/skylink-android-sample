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

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!videoService.isConnectingOrConnected()) {

            //reset permission request states.
            permissionUtils.permQReset();

            //connect to room on Skylink connection
            processConnectToRoom();

            //default setting for video output
            this.currentVideoSpeaker = Utils.isDefaultSpeakerSettingForVideo();

            //after connected to skylink SDK, UI will be updated latter on onServiceRequestConnect

            Log.d(TAG, "Try to connect when entering room");
        }

        //get default audio output settings
        mainView.onPresenterRequestChangeSpeakerOutput(this.currentVideoSpeaker);
    }

    @Override
    public void onViewRequestResume() {
        // Toggle camera back to previous state if required.
        // check the current camera state isCameraCapturerStop() is true if camera is currently stop
        // just process for mediaType as VIDEO_CAMERA, because we do not pause/resume view with mediaType as VIDEO_SCREEN
        if (!currentVideoLocalState.isCameraCapturerStop()) {
            if (videoService.getVideoView(null, SkylinkMedia.MEDIA_TYPE.VIDEO_CAMERA) != null) {
                // change camera state
                videoService.toggleCamera(null, false);
                // change UI
                mainView.onPresenterRequestChangeCameraUI(false, false);
            }
        } else {
            // change camera state
            videoService.toggleCamera(null, true);
            // change UI
            mainView.onPresenterRequestChangeCameraUI(true, false);
        }
    }

    @Override
    public void onViewRequestPause() {
        //stop camera when pausing so that camera will be available for the others to use
        videoService.toggleCamera(null, true);
    }

    @Override
    public void onViewRequestDisconnectFromRoom() {
        videoService.disconnectFromRoom();
    }

    @Override
    public void onViewRequestExit() {
        //process disconnect from room
        videoService.disconnectFromRoom();
        //after disconnected from skylink SDK, UI will be updated latter on onServiceRequestDisconnect
    }

    @Override
    public void onViewRequestChangeAudioState() {
        boolean isAudioMute = currentVideoLocalState.isAudioMute();

        //change UI and audio state to opposite state
        if (!isAudioMute) {
            mainView.onPresenterRequestChangeAudioUI(true);
        } else {
            mainView.onPresenterRequestChangeAudioUI(false);
        }

        processAudioStateChanged(!isAudioMute);
    }

    @Override
    public void onViewRequestChangeVideoState() {
        boolean isVideoMute = currentVideoLocalState.isVideoMute();

        //change UI and video state to opposite state
        if (!isVideoMute) {
            mainView.onPresenterRequestChangeVideoUI(true);
        } else {
            mainView.onPresenterRequestChangeVideoUI(false);
        }

        processVideoStateChanged(!isVideoMute);
    }

    @Override
    public void onViewRequestChangeScreenState() {
        boolean isScreenMute = currentVideoLocalState.isScreenMute();

        //change UI and video state to opposite state
        if (!isScreenMute) {
            mainView.onPresenterRequestChangeVideoUI(true);
        } else {
            mainView.onPresenterRequestChangeVideoUI(false);
        }

        processScreenStateChanged(!isScreenMute);
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

        if (resultCode == Activity.RESULT_OK && data != null) {
            //show the stop screen share button
            requestButtonOverlayPermission();
        }

        // display overlay button if permission is grant
        // or warning dialog if permission is deny
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(context)) {
                mainView.onPresenterRequestShowHideButtonStopScreenSharing();
            } else {
                onViewRequestOverlayPermissionDeny();
            }
        }
    }

    /**
     * display a warning if user deny the file permission
     */
    @Override
    public void onViewRequestOverlayPermissionDeny() {
        permissionUtils.displayOverlayButtonPermissionWarning(context);
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
    public void onViewRequestStartVideo() {
        videoService.startLocalVideo();
    }

    @Override
    public void onViewRequestStartScreen() {
        videoService.startLocalScreen();
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful) {
            processUpdateStateConnected();

            //start audio routing if has audio config
            SkylinkConfig skylinkConfig = videoService.getSkylinkConfig();
            if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
                AudioRouter.setPresenter(this);
                AudioRouter.startAudioRouting(context, Constants.CONFIG_TYPE.VIDEO);
            }

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

        //notify view to change the UI
        mainView.onPresenterRequestLocalAudioCapture(localAudio.getMediaId());
    }

    @Override
    public void onServiceRequestLocalCameraCapture(SkylinkMedia localVideo) {
        String log = "[SA][onServiceRequestLocalCameraCapture] ";
        if (localVideo.getVideoView() == null) {
            log += "VideoView is null!";
            Log.w(TAG, log);

            SurfaceViewRenderer selfVideoView = videoService.getVideoView(null, SkylinkMedia.MEDIA_TYPE.VIDEO_CAMERA);
            processAddSelfView(selfVideoView);
        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
            processAddSelfView(localVideo.getVideoView());
        }

        //notify view to change the UI
        mainView.onPresenterRequestLocalVideoCapture(localVideo.getMediaId());
    }

    @Override
    public void onServiceRequestLocalScreenCapture(SkylinkMedia localVideo) {
        String log = "[SA][onServiceRequestLocalScreenCapture] ";
        if (localVideo.getVideoView() == null) {
            log += "VideoView is null!";
            Log.d(TAG, log);

            SurfaceViewRenderer selfVideoView = videoService.getVideoView(null, SkylinkMedia.MEDIA_TYPE.VIDEO_SCREEN);
            processAddSelfView(selfVideoView);
        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
            processAddSelfScreenView(localVideo.getVideoView());
        }

        //notify view to change the UI
        mainView.onPresenterRequestLocalVideoCapture(localVideo.getMediaId());
    }

    @Override
    public void onServiceRequestRemotePeerJoin(SkylinkPeer remotePeer) {
        // Fill the new peer in button in custom bar
        processAddNewPeer(remotePeer, videoService.getTotalPeersInRoom() - 1);
    }

    @Override
    public void onServiceRequestRemotePeerLeave(SkylinkPeer remotePeer, int removeIndex) {
        // do not process if the left peer is local peer
        if (removeIndex == -1)
            return;

        // Remove the peer in button in custom bar
        processRemoveRemotePeer();
    }

    @Override
    public void onServiceRequestRemotePeerConnectionRefreshed(String log, UserInfo remotePeerUserInfo) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        toastLog(TAG, context, log);
        // do anything properly
    }

    @Override
    public void onServiceRequestRemotePeerAudioReceive(String log, UserInfo remotePeerUserInfo,
                                                       String remotePeerId, String mediaId) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n";
        Log.d(TAG, log);
        toastLog(TAG, context, log);
    }

    @Override
    public void onServiceRequestRemotePeerVideoReceive(String log, UserInfo remotePeerUserInfo,
                                                       String remotePeerId, SkylinkMedia remoteMedia) {
        // add the remote video view in to the view
        processAddRemoteView(remotePeerId, remoteMedia);

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

        //Refresh currentVideoLocalState
        currentVideoLocalState.setAudioMute(false);
        currentVideoLocalState.setVideoMute(false);
        currentVideoLocalState.setCameraCapturerStop(false);
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

        //save audioMuted for other usage
        currentVideoLocalState.setAudioMute(isAudioMuted);

        //set mute audio to sdk
        videoService.muteLocalAudio(isAudioMuted);

        // Set UI and Toast.
        mainView.onPresenterRequestUpdateAudioState(isAudioMuted, true);
    }

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processVideoStateChanged(boolean isVideoMuted) {

        //save audioMuted for other usage
        currentVideoLocalState.setVideoMute(isVideoMuted);

        //set mute audio to sdk
        videoService.muteLocalVideo(isVideoMuted);

        // Set UI and Toast.
        mainView.onPresenterRequestUpdateVideoState(isVideoMuted, true);
    }

    // If screen video is enabled, mute screen video and if screen video is mute, then enable it
    private void processScreenStateChanged(boolean isScreenMuted) {

        //save audioMuted for other usage
        currentVideoLocalState.setScreenMute(isScreenMuted);

        //set mute screen video to sdk
        videoService.muteLocalScreen(isScreenMuted);

        // Set UI and Toast.
        mainView.onPresenterRequestUpdateScreenState(isScreenMuted, true);
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
     * Add local video view into the layout
     */
    private void processAddSelfView(SurfaceViewRenderer videoView) {
        mainView.onPresenterRequestAddCameraSelfView(videoView);
    }

    /**
     * Add local video view into the layout
     */
    private void processAddSelfScreenView(SurfaceViewRenderer videoView) {
        mainView.onPresenterRequestAddScreenSelfView(videoView);
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

        mainView.onPresenterRequestAddCameraRemoteView(videoView);
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

    /**
     * Add new peer on UI when new peer joined in room in specific index
     *
     * @param newPeer the new peer joined in room
     * @param index   the index of the new peer to add
     */
    private void processAddNewPeer(SkylinkPeer newPeer, int index) {
        mainView.onPresenterRequestChangeUiRemotePeerJoin(newPeer, index);
    }

    /**
     * Remove a remote peer by re-fill total remote peer left in the room
     * to make sure the left peers are displayed correctly
     */
    private void processRemoveRemotePeer() {
        // update peer button in action bar
        mainView.onPresenterRequestChangeUiRemotePeerLeft(videoService.getPeersList());

        // remove the remote peer video view
        mainView.onPresenterRequestRemoveRemotePeer();
    }
}
