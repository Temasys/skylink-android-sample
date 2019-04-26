package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
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
 */

public class VideoCallPresenter extends BasePresenter implements VideoCallContract.Presenter {

    private final String TAG = VideoCallPresenter.class.getName();

    private Context context;

    // The view instance
    public VideoCallContract.View videoCallView;

    // The service instance
    private VideoService videoCallService;

    // The video resolution presenter
    private VideoResolutionPresenter videoResPresenter;

    //utils to process permission
    private PermissionUtils permissionUtils;

    // the current speaker output {speaker/headset}
    private boolean currentVideoSpeaker = Utils.getDefaultVideoSpeaker();

    // the current state of local video {audio, video, camera}
    private VideoLocalState currentVideoLocalState = new VideoLocalState();


    public VideoCallPresenter(Context context) {
        this.context = context;
        this.videoCallService = new VideoService(context);
        this.videoCallService.setPresenter(this);
        this.permissionUtils = new PermissionUtils();
    }

    public void setView(VideoCallContract.View view) {
        videoCallView = view;
        videoCallView.setPresenter(this);
    }

    /**
     * inject the video resolution presenter into this presenter
     * in order to let video resolution presenter handles the video resolution logic
     * both this presenter and video resolution presenter use the video call service
     */
    public void setVideoResPresenter(VideoResolutionPresenter videoResolutionPresenter) {
        this.videoResPresenter = videoResolutionPresenter;
        this.videoResPresenter.setService(this.videoCallService);
        this.videoCallService.setResPresenter(this.videoResPresenter);
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
        if (!videoCallService.isConnectingOrConnected()) {

            //reset permission request states.
            permissionUtils.permQReset();

            //connect to room on Skylink connection
            processConnectToRoom();

            //Start local audio and video.
            videoCallService.startLocalMedia();

            //default setting for video output
            this.currentVideoSpeaker = Utils.getDefaultVideoSpeaker();

            //after connected to skylink SDK, UI will be updated latter on onServiceRequestConnect

            Log.d(TAG, "Try to connect when entering room");
        }

        //get default audio output settings
        videoCallView.onPresenterRequestChangeAudioOuput(this.currentVideoSpeaker);
    }

    @Override
    public void onViewRequestResume() {
        // Do not toggleCamera if currently doing screensharing.
        if (videoCallService.getCurrentVideoDevice() == SkylinkConfig.VideoDevice.SCREEN) {
            return;
        }
        // Toggle camera back to previous state if required.
        // check the current camera state isCameraMute() is true if camera is currently stop
        if (!currentVideoLocalState.isCameraMute()) {
            if (videoCallService.getVideoView(null, null) != null) {
                // change camera state
                videoCallService.toggleCamera(null,false);
                // change UI
                videoCallView.onPresenterRequestChangeCameraUI(false);
            }
        } else {
            // change camera state
            videoCallService.toggleCamera(null, true);
            // change UI
            videoCallView.onPresenterRequestChangeCameraUI(true);
        }
    }

    @Override
    public void onViewRequestPause() {
        // Do not toggleCamera if currently doing screensharing.
        if (videoCallService.getCurrentVideoDevice() == SkylinkConfig.VideoDevice.SCREEN) {
            return;
        }
        //stop camera when pausing so that camera will be available for the others to use
        videoCallService.toggleCamera(null,true);
    }

    @Override
    public void onViewRequestDisconnectFromRoom() {
        videoCallService.disconnectFromRoom();
    }

    @Override
    public void onViewRequestExit() {
        //process disconnect from room
        videoCallService.disconnectFromRoom();
        //after disconnected from skylink SDK, UI will be updated latter on onServiceRequestDisconnect
    }

    @Override
    public void onViewRequestChangeAudioState() {
        boolean isAudioMute = currentVideoLocalState.isAudioMute();

        //change UI and audio state to opposite state
        if (!isAudioMute) {
            videoCallView.onPresenterRequestChangeAudioUI(true);
        } else {
            videoCallView.onPresenterRequestChangeAudioUI(false);
        }

        processAudioStateChanged(!isAudioMute);
    }

    @Override
    public void onViewRequestChangeVideoState() {
        boolean isVideoMute = currentVideoLocalState.isVideoMute();

        //change UI and video state to opposite state
        if (!isVideoMute) {
            videoCallView.onPresenterRequestChangeVideoUI(true);
        } else {
            videoCallView.onPresenterRequestChangeVideoUI(false);
        }

        processVideoStateChanged(!isVideoMute);
    }

    @Override
    public void onViewRequestChangeCameraState() {
        //get current camera state: true is muted, false is active
        boolean isCamMute = currentVideoLocalState.isCameraMute();

        //change state
        currentVideoLocalState.setCameraMute(!isCamMute);

        //change camera state in service layer
        videoCallService.toggleCamera(null, !isCamMute);

        // change UI
        videoCallView.onPresenterRequestChangeCameraUI(!isCamMute);
    }

    @Override
    public void onViewRequestChangeAudioOutput() {
        //change current speakerOn
        boolean isSpeakerOn = this.currentVideoSpeaker;

        // use service layer to change the audio output
        videoCallService.changeSpeakerOutput(!isSpeakerOn);

        this.currentVideoSpeaker = !isSpeakerOn;
    }

    @Override
    public void onViewRequestSwitchCamera() {
        videoCallService.switchCamera();
    }

    @Override
    public void onViewRequestGetVideoResolutions() {
        // get the remote peer id
        String peerId = videoCallService.getPeerId(1);
        videoCallService.getVideoResolutions(peerId);
    }

    @Override
    public void onViewRequestActivityResult(int requestCode, int resultCode, Intent data) {
        permissionUtils.onRequestActivityResultHandler(requestCode, resultCode, data);
    }

    @Override
    public void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, TAG);
    }

    /**
     * Get the specific peer object according to the index
     */
    @Override
    public SkylinkPeer onViewRequestGetPeerByIndex(int index) {
        return videoCallService.getPeerByIndex(index);
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful) {
            videoCallView.onPresenterRequestConnectedUIChange();

            processUpdateStateConnected();

            //start audio routing if has audio config
            SkylinkConfig skylinkConfig = videoCallService.getSkylinkConfig();
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
        SkylinkConfig skylinkConfig = videoCallService.getSkylinkConfig();
        if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
            AudioRouter.stopAudioRouting(context);
        }
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
    public void onServiceRequestRemotePeerAudioReceive(String log, UserInfo remotePeerUserInfo, String remotePeerId, String mediaId) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n";
        Log.d(TAG, log);
        toastLog(TAG, context, log);
    }

    @Override
    public void onServiceRequestRemotePeerVideoReceive(String log, UserInfo remotePeerUserInfo, String remotePeerId, String mediaId) {
        // add the remote video view in to the view
        processAddRemoteView(remotePeerId, mediaId);

        log += "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        Log.d(TAG, log);
        toastLog(TAG, context, log);
    }

    @Override
    public void onServiceRequestIntentRequired(Intent intent, int requestCode, int infoCode) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onIntentRequiredHandler(intent, requestCode, infoCode, (Activity) context);
    }

    @Override
    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onPermissionRequiredHandler(info, TAG, context, videoCallView.onPresenterRequestGetFragmentInstance());
    }

    @Override
    public void onServiceRequestLocalAudioCapture(String mediaId) {
        String log = "[SA][onServiceRequestLocalAudioCapture] ";

        toastLog(TAG, context, "Local audio is on with id = " + mediaId);
    }

    @Override
    public void onServiceRequestLocalVideoCapture(SurfaceViewRenderer videoView) {
        String log = "[SA][onServiceRequestLocalVideoCapture] ";
        if (videoView == null) {
            log += "VideoView is null!";
            Log.d(TAG, log);

            SurfaceViewRenderer selfVideoView = videoCallService.getVideoView(null, null);
            processAddSelfView(selfVideoView);
        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
            processAddSelfView(videoView);
        }
    }

    @Override
    public void onServiceRequestAudioOutputChanged(boolean isSpeakerOn) {
        videoCallView.onPresenterRequestChangeAudioOuput(isSpeakerOn);
        this.currentVideoSpeaker = isSpeakerOn;

        if (isSpeakerOn) {
            String log = context.getString(R.string.enable_speaker);
            toastLog(TAG, context, log);
        } else {
            String log = context.getString(R.string.enable_headset);
            toastLog(TAG, context, log);
        }
    }

    @Override
    public void onServiceRequestRemotePeerJoin(SkylinkPeer remotePeer) {
        // Fill the new peer in button in custom bar
        processAddNewPeer(remotePeer, videoCallService.getTotalPeersInRoom() - 1);
    }

    @Override
    public void onServiceRequestRemotePeerLeave(SkylinkPeer remotePeer, int removeIndex) {
        // do not process if the left peer is local peer
        if (removeIndex == -1)
            return;

        // Remove the peer in button in custom bar
        processRemoveRemotePeer();
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Process connect to room on service layer and update UI accordingly
     */
    private void processConnectToRoom() {

        //connect to SDK
        videoCallService.connectToRoom(Constants.CONFIG_TYPE.VIDEO);

        //update UI and make toast
        videoCallView.onPresenterRequestConnectingUIChange();

        //Refresh currentVideoLocalState
        currentVideoLocalState.setAudioMute(false);
        currentVideoLocalState.setVideoMute(false);
        currentVideoLocalState.setCameraMute(false);
    }

    /**
     * Update UI into disconnected state
     */
    private void processDisconnectUIChange() {
        // update UI
        videoCallView.onPresenterRequestDisconnectUIChange();
    }

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processAudioStateChanged(boolean isAudioMuted) {

        //save audioMuted for other usage
        currentVideoLocalState.setAudioMute(isAudioMuted);

        //set mute audio to sdk
        videoCallService.muteLocalAudio(isAudioMuted);

        // Set UI and Toast.
        videoCallView.onPresenterRequestUpdateAudioState(isAudioMuted, true);
    }

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processVideoStateChanged(boolean isVideoMuted) {

        //save audioMuted for other usage
        currentVideoLocalState.setVideoMute(isVideoMuted);

        //set mute audio to sdk
        videoCallService.muteLocalVideo(isVideoMuted);

        // Set UI and Toast.
        videoCallView.onPresenterRequestUpdateVideoState(isVideoMuted, true);
    }

    /**
     * Get video view by remote peer id
     */
    private SurfaceViewRenderer processGetVideoView(String remotePeerId, String mediaId) {
        return videoCallService.getVideoView(remotePeerId, mediaId);
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
        videoCallView.onPresenterRequestAddSelfView(videoView);
    }

    /**
     * Add remote video view into the layout
     */
    private void processAddRemoteView(String remotePeerId, String mediaId) {

        SurfaceViewRenderer videoView = processGetRemoteView(remotePeerId, mediaId);
        // setTag for the remote video view
        videoView.setTag(mediaId);

        videoCallView.onPresenterRequestAddRemoteView(videoView);
    }

    /**
     * Update UI when connected to room
     */
    private void processUpdateStateConnected() {
        videoCallView.onPresenterRequestUpdateUIConnected(processGetRoomId());
    }

    /**
     * Get the room id info
     */
    private String processGetRoomId() {
        return videoCallService.getRoomId();
    }

    /**
     * Add new peer on UI when new peer joined in room in specific index
     *
     * @param newPeer the new peer joined in room
     * @param index   the index of the new peer to add
     */
    private void processAddNewPeer(SkylinkPeer newPeer, int index) {
        videoCallView.onPresenterRequestChangeUiRemotePeerJoin(newPeer, index);
    }

    /**
     * Remove a remote peer by re-fill total remote peer left in the room
     * to make sure the left peers are displayed correctly
     */
    private void processRemoveRemotePeer() {
        // update peer button in action bar
        videoCallView.onPresenterRequestChangeUiRemotePeerLeft(videoCallService.getPeersList());

        // remove the remote peer video view
        videoCallView.onPresenterRequestRemoveRemotePeer();
    }
}
