package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkInfo;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.VideoService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
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

    // The video resolution presenter to implement video resolutions
    private VideoResolutionPresenter videoResPresenter;

    // Utils to process permission
    private PermissionUtils permissionUtils;

    // the current speaker output {speaker/headset}
    private boolean currentVideoSpeaker = Utils.isDefaultSpeakerSettingForVideo();

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

        //connect to room on Skylink connection
        processConnectToRoom();

        //get default audio output settings
        mainView.onPresenterRequestChangeSpeakerOutput(this.currentVideoSpeaker);

        //after connected to skylink SDK, UI will be updated latter on onServiceRequestConnect
    }

    @Override
    public void onViewRequestResume() {

        // do not process if user has not started video from camera
        if (videoService.getLocalVideo() == null) {
            return;
        }

        // turn back camera to active state
        // we just active local video camera if there is only 1 active camera at current time
        // if there is an active screen, do not ative local camera as it may be intention of the user
        SkylinkMedia localVideo = videoService.getLocalVideo();
        SkylinkMedia localScreen = videoService.getLocalScreen();
        if (localVideo != null && localVideo.getMediaState() == SkylinkMedia.MediaState.STOPPED
                && (localScreen == null || (localScreen != null && localScreen.getMediaState() == SkylinkMedia.MediaState.UNAVAILABLE))) {
            videoService.toggleVideo(true);
        }

    }

    @Override
    public void onViewRequestPause() {
        // stop camera when pausing so that camera will be available for the others to use
        // we just stop local video camera if there is only 1 active camera at current time
        // if there is an active screen, do not stop local camera as it may be intention of the user
        SkylinkMedia localVideo = videoService.getLocalVideo();
        SkylinkMedia localScreen = videoService.getLocalScreen();
        if (localVideo != null && (localVideo.getMediaState() == SkylinkMedia.MediaState.ACTIVE || localVideo.getMediaState() == SkylinkMedia.MediaState.MUTED)
                && (localScreen == null || (localScreen != null && localScreen.getMediaState() == SkylinkMedia.MediaState.UNAVAILABLE))) {
            videoService.toggleVideo(false);
        }
    }

    @Override
    public void onViewRequestDisconnectFromRoom() {
        videoService.disconnectFromRoom();
    }

    @Override
    public void onViewRequestExit() {
        //process disconnect from room if connecting
        //after disconnected from skylink SDK, UI will be updated latter on onServiceRequestDisconnect
        videoService.disconnectFromRoom();

        // need to call disposeLocalMedia to clear all local media objects as disconnectFromRoom no longer dispose local media
        videoService.disposeLocalMedia();
    }

    @Override
    public void onViewRequestChangeAudioState() {
        SkylinkMedia localAudio = videoService.getLocalAudio();

        // do not process if user has not started audio
        if (localAudio == null)
            return;

        // mute local audio if its current state is ACTIVE or STOPPED
        // active it if its current state is MUTED
        if (localAudio.getMediaState() == SkylinkMedia.MediaState.ACTIVE || localAudio.getMediaState() == SkylinkMedia.MediaState.STOPPED) {
            videoService.muteLocalAudio(true);
        } else if (localAudio.getMediaState() == SkylinkMedia.MediaState.MUTED) {
            videoService.muteLocalAudio(false);
        }
    }

    @Override
    public void onViewRequestChangeVideoState() {
        SkylinkMedia localVideo = videoService.getLocalVideo();

        // do not process if user has not started video from camera
        if (localVideo == null)
            return;

        // mute local video if its current state is ACTIVE or STOPPED
        // active it if its current state is MUTED
        if (localVideo.getMediaState() == SkylinkMedia.MediaState.ACTIVE || localVideo.getMediaState() == SkylinkMedia.MediaState.STOPPED) {
            videoService.muteLocalVideo(true);
        } else if (localVideo.getMediaState() == SkylinkMedia.MediaState.MUTED) {
            videoService.muteLocalVideo(false);
        }
    }

    @Override
    public void onViewRequestChangeScreenState() {
        // do not process if user has not started screen sharing
        SkylinkMedia localScreen = videoService.getLocalScreen();

        if (localScreen == null)
            return;

        // mute local screen if its current state is ACTIVE or STOPPED
        // active it if its current state is MUTED
        if (localScreen.getMediaState() == SkylinkMedia.MediaState.ACTIVE || localScreen.getMediaState() == SkylinkMedia.MediaState.STOPPED) {
            videoService.muteLocalScreen(true);
        } else if (localScreen.getMediaState() == SkylinkMedia.MediaState.MUTED) {
            videoService.muteLocalScreen(false);
        }
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
        videoService.createLocalAudio();
    }

    @Override
    public void onViewRequestToggleVideo() {
        // implement start or stop video base on the state of the current video from camera
        SkylinkMedia localVideo = videoService.getLocalVideo();

        // change local video state to STOPPED if its current state is active or muted
        if (localVideo != null && (localVideo.getMediaState() == SkylinkMedia.MediaState.ACTIVE || localVideo.getMediaState() == SkylinkMedia.MediaState.MUTED)) {
            videoService.toggleVideo(false);
        } else if (localVideo == null || localVideo.getMediaState() == SkylinkMedia.MediaState.STOPPED) {
            videoService.toggleVideo(true);
        }
    }

    @Override
    public void onViewRequestToggleScreen() {
        // implement start or stop video base on the state of the current video from camera
        SkylinkMedia localScreen = videoService.getLocalScreen();

        // change local video state to STOPPED if its current state is active or muted
        if (localScreen != null && (localScreen.getMediaState() == SkylinkMedia.MediaState.ACTIVE || localScreen.getMediaState() == SkylinkMedia.MediaState.MUTED)) {
            videoService.toggleScreen(false);
        } else if (localScreen == null || localScreen.getMediaState() == SkylinkMedia.MediaState.STOPPED) {
            videoService.toggleScreen(true);
        }
    }

    @Override
    public void onViewRequestToggleScreen(boolean toActive) {
        videoService.toggleScreen(toActive);
    }

    @Override
    public void onViewRequestRemoveAudio() {
        videoService.destroyLocalAudio();
    }

    @Override
    public void onViewRequestRemoveVideo() {
        videoService.destroyLocalVideo();
    }

    @Override
    public void onViewRequestRemoveScreen() {
        videoService.destroyLocalScreen();
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
        videoService.createLocalAudio();

        // check the default setting for video device and start local video accordingly
        if (Utils.isDefaultCameraDeviceSetting()) {
            videoService.createLocalVideo();
            return;
        }

        if (Utils.isDefaultScreenDeviceSetting()) {
            videoService.createLocalScreen();
            return;
        }

        // we create a custom video device from back camera of the device, so start custom video device
        // will similarly start back camera
        if (Utils.isDefaultCustomVideoDeviceSetting()) {
            videoService.createLocalCustomVideo();
            return;
        }
    }

    @Override
    public void onViewRequestLockRoom() {
        videoService.lockRoom();
    }

    @Override
    public void onViewRequestUnlockRoom() {
        videoService.unlockRoom();
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
    }

    @Override
    public void onServiceRequestIntentRequired(Intent intent, int requestCode, SkylinkInfo skylinkInfo) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onIntentRequiredHandler(intent, requestCode, skylinkInfo, (Activity) context);
    }

    @Override
    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onPermissionRequiredHandler(info, TAG, context, mainView.onPresenterRequestGetFragmentInstance());
    }

    @Override
    public void onServiceRequestLocalAudioCapture(SkylinkMedia localAudio) {
        toastLog("[SA][onServiceRequestLocalAudioCapture]", context, "Local audio is on with id = " + localAudio.getMediaId());

        //notify view to change the UI
        mainView.onPresenterRequestLocalAudioCapture(localAudio.getMediaId());
    }

    @Override
    public void onServiceRequestLocalCameraCapture(SkylinkMedia localVideo) {
        String log = "[SA][onServiceRequestLocalCameraCapture] ";
        toastLog(log, context, "Local video camera is on with id = " + localVideo.getMediaId());

        SurfaceViewRenderer selfVideoView = localVideo.getVideoView();

        if (selfVideoView == null) {
            log += "VideoView is null! Try to get video from SDK";
            Log.w(TAG, log);

            selfVideoView = videoService.getVideoView(localVideo.getMediaId());
        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
        }

        //notify view to change the UI
        mainView.onPresenterRequestAddCameraSelfView(localVideo.getMediaId(), selfVideoView);
    }

    @Override
    public void onServiceRequestLocalScreenCapture(SkylinkMedia localScreen) {
        String log = "[SA][onServiceRequestLocalScreenCapture] ";
        toastLog(log, context, "Local video screen is on with id = " + localScreen.getMediaId());

        SurfaceViewRenderer selfVideoView = localScreen.getVideoView();

        if (selfVideoView == null) {
            log += "VideoView is null! Try to get video from SDK";
            Log.w(TAG, log);

            selfVideoView = videoService.getVideoView(localScreen.getMediaId());
        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
        }

        //notify view to change the UI
        mainView.onPresenterRequestAddScreenSelfView(localScreen.getMediaId(), selfVideoView);
    }

    @Override
    public void onServiceRequestChangeDefaultVideoDevice(SkylinkConfig.VideoDevice videoDevice) {
        mainView.onPresenterRequestChangeDefaultVideoDevice(videoDevice);
    }

    @Override
    public void onServiceRequestMediaStateChange(SkylinkMedia media, boolean isLocal) {
        // change the UI
        mainView.onPresenterRequestMediaStateChange(media.getMediaType(), media.getMediaState(), isLocal);

        // stop audio routing when remote audio is unavailable
        if (!isLocal && !media.isVideo() && media.getMediaState() == SkylinkMedia.MediaState.UNAVAILABLE) {
            AudioRouter.stopAudioRouting(context);
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
        if (removeIndex == -1 || remotePeer == null)
            return;

        // Remove the peer in button in custom bar
        mainView.onPresenterRequestChangeUiRemotePeerLeft(videoService.getPeersList());

        // remove the remote peer video view
        mainView.onPresenterRequestRemoveRemotePeer();

        videoResPresenter.onServiceRequestSentVideoResolutionObtained(remotePeer.getPeerId(), SkylinkMedia.MediaType.VIDEO_CAMERA, -1, -1, -1);
        videoResPresenter.onServiceRequestSentVideoResolutionObtained(remotePeer.getPeerId(), SkylinkMedia.MediaType.VIDEO_SCREEN, -1, -1, -1);
        videoResPresenter.onServiceRequestReceivedVideoResolutionObtained(remotePeer.getPeerId(), SkylinkMedia.MediaType.VIDEO_CAMERA, -1, -1, -1);
        videoResPresenter.onServiceRequestReceivedVideoResolutionObtained(remotePeer.getPeerId(), SkylinkMedia.MediaType.VIDEO_SCREEN, -1, -1, -1);
    }

    @Override
    public void onServiceRequestRemotePeerAudioReceive(String remotePeerId) {
        AudioRouter.setPresenter(this);
        AudioRouter.startAudioRouting(context, Constants.CONFIG_TYPE.VIDEO);

        // use service layer to change the audio output, update UI will be called later in onServiceRequestAudioOutputChanged
        videoService.changeSpeakerOutput(this.currentVideoSpeaker);

        mainView.onPresenterRequestReceiveRemoteAudio(remotePeerId);
    }

    @Override
    public void onServiceRequestRemotePeerVideoReceive(String remotePeerId, SkylinkMedia remoteVideo) {
        processAddRemoteView(remotePeerId, remoteVideo);
    }

    @Override
    public void onServiceRequestAudioOutputChanged(boolean isSpeakerOn) {
        mainView.onPresenterRequestChangeSpeakerOutput(isSpeakerOn);
        this.currentVideoSpeaker = isSpeakerOn;

        if (isSpeakerOn) {
            String log = context.getString(R.string.enable_speaker);
//            toastLog(TAG, context, log);
        } else {
            String log = context.getString(R.string.enable_headset);
//            toastLog(TAG, context, log);
        }
    }

    @Override
    public void onServiceRequestChangeRoomLockStatus(boolean roomLockStatus) {
        mainView.onPresenterRequestChangeRoomLockStatus(roomLockStatus);
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

    // If screen video is enabled, mute screen video and if screen video is mute, then enable it
    private void processScreenStateChanged(boolean toMuted) {
        //set mute screen video to sdk
        videoService.muteLocalScreen(toMuted);
    }

    /**
     * Get video view by remote peer id
     */
    private SurfaceViewRenderer processGetVideoView(String mediaId) {
        return videoService.getVideoView(mediaId);
    }

    /**
     * Get the remote video view from peer id
     */
    private SurfaceViewRenderer processGetRemoteView(String mediaId) {
        return processGetVideoView(mediaId);
    }

    /**
     * Get the remote video view from peer id
     */
    private List<SurfaceViewRenderer> processGetRemoteViews(String remotePeerId, SkylinkMedia.MediaType mediaType) {
        return videoService.getVideoViews(remotePeerId, mediaType);
    }

    /**
     * Add remote video view into the layout
     */
    private void processAddRemoteView(String remotePeerId, SkylinkMedia remoteMedia) {

        SurfaceViewRenderer videoView = remoteMedia.getVideoView();

        if (videoView == null) {
            List<SurfaceViewRenderer> videoViews = processGetRemoteViews(remotePeerId, remoteMedia.getMediaType());
            if (videoViews != null && videoViews.size() > 0) {
                // get the first video view of the media type
                videoView = videoViews.get(0);
            }
        }

        if (videoView == null)
            return;

        // for testing getSkylinkMediaList API
        List<SurfaceViewRenderer> videoViews = processGetRemoteViews(remotePeerId, remoteMedia.getMediaType());
        if (videoViews != null && videoViews.size() > 0) {
            // get the first video view of the media type
            videoView = videoViews.get(0);
        }

        // setTag for the remote video view
        videoView.setTag(remoteMedia.getMediaId());

        if (remoteMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_CAMERA ||
                remoteMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO) {
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
