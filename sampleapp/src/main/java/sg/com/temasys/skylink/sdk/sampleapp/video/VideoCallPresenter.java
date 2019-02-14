package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.content.Context;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.VideoService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

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

    //utils to process permission
    private PermissionUtils permissionUtils;

    // The array of SkylinkCaptureFormats support by the current camera.
    private SkylinkCaptureFormat[] captureFormats;

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

            //default setting for video output
            videoCallService.setCurrentVideoSpeaker(Utils.getDefaultVideoSpeaker());

            //after connected to skylink SDK, UI will be updated latter on onServiceRequestConnect

            Log.d(TAG, "Try to connect when entering room");
        }

        //get default audio output settings
        boolean isSpeakerOn = videoCallService.getCurrentVideoSpeaker();
        videoCallView.onPresenterRequestChangeAudioOuput(isSpeakerOn);
    }

    @Override
    public void onViewRequestResume() {
        // Toggle camera back to previous state if required.
        // check the current camera state isCameraMute() is true if camera is currently stop
        if (!videoCallService.isCameraMute()) {
            if (videoCallService.getVideoView(null) != null) {
                // change camera state
                videoCallService.toggleCamera(false);
                // change UI
                videoCallView.onPresenterRequestChangeCameraUI(false);
            }
        } else {
            // change camera state
            videoCallService.toggleCamera(true);
            // change UI
            videoCallView.onPresenterRequestChangeCameraUI(true);
        }
    }

    @Override
    public void onViewRequestPause() {
        //stop camera when pausing so that camera will be available for the other to use
        videoCallService.toggleCamera(true);
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
        // get current audio state
        boolean isAudioMute = videoCallService.isAudioMute();

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
        // get current video state
        boolean isVideoMute = videoCallService.isVideoMute();

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
        boolean isCamMute = videoCallService.isCameraMute();

        //change state
        videoCallService.setCamMute(!isCamMute);

        //change camera state in service layer
        videoCallService.toggleCamera(!isCamMute);

        // change UI
        videoCallView.onPresenterRequestChangeCameraUI(!isCamMute);
    }

    @Override
    public void onViewRequestChangeAudioOutput() {
        //change current speakerOn
        boolean isSpeakerOn = videoCallService.getCurrentVideoSpeaker();

        // use service layer to change the audio output
        videoCallService.changeSpeakerOutput(!isSpeakerOn);
    }

    @Override
    public void onViewRequestSwitchCamera() {
        videoCallService.switchCamera();
    }

    @Override
    public void onViewRequestDimProgressChanged(int progress) {
        processUpdateUiResOnProgressDim(progress);
    }

    @Override
    public void onViewRequestFpsProgressChanged(int progress) {
        processUpdateUiResOnProgressFps(progress);
    }

    @Override
    public void onViewRequestDimSelected(int progress) {
        SkylinkCaptureFormat format = processUpdateUiResOnProgressDim(progress);
        // Check that new CaptureFormat is valid.
        if (!Utils.isCaptureFormatValid(format)) {
            return;
        }

        int width = format.getWidth();
        int height = format.getHeight();
        int currentFps = videoCallService.getCurrentFps();
        int fpsNew = Utils.getFpsForNewCaptureFormat(currentFps, format);

        // If any of the new Dim or Fps values are not valid,
        // or setting new resolution was not successful, reset UI to previous values.
        boolean result1 = videoCallView.onPresenterRequestUpdateUiResDimInfo(width, height);
        boolean result2 = processUpdateUiResFps(fpsNew, format);
        boolean result3 = processUpdateInputVideoResolutions(format, fpsNew);

        if (fpsNew < 0 || !result1 || !result2 || result3) {
            SkylinkCaptureFormat currentFormat = videoCallService.getCurrentCaptureFormat();
            processUpdateUiResDim(currentFormat.getWidth(), currentFormat.getHeight(), captureFormats);
            processUpdateUiResFps(currentFps, currentFormat);
            return;
        }
    }

    @Override
    public void onViewRequestFpsSelected(int progress) {
        int fpsNew = processUpdateUiResOnProgressFps(progress);

        SkylinkCaptureFormat currentFormat = videoCallService.getCurrentCaptureFormat();
        int currentFps = videoCallService.getCurrentFps();

        // Check that new fps is valid for selected CaptureFormat.
        fpsNew = Utils.getFpsForNewCaptureFormat(fpsNew, currentFormat);

        // If any of new Fps or selected CaptureFormat are not valid,
        // or setting new resolution was not successful, reset Fps UI to previous values.
        boolean result1 = processUpdateInputVideoResolutions(currentFormat, fpsNew);

        if (fpsNew < 0 || result1) {
            processUpdateUiResFps(currentFps, currentFormat);
            return;
        }
    }

    @Override
    public String onViewRequestGetRoomPeerIdNick() {
        return videoCallService.getRoomIdAndNickname(Constants.CONFIG_TYPE.VIDEO);
    }

    @Override
    public void onViewRequestGetVideoResolutions() {
        String peerId = videoCallService.getPeerId(1);
        videoCallService.getVideoResolutions(peerId);
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
    }

    @Override
    public void onServiceRequestRemotePeerMediaReceive(String log, UserInfo remotePeerUserInfo, String remotePeerId) {
        processAddRemoteView();

        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        Log.d(TAG, log);
    }

    @Override
    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onPermissionRequiredHandler(info, TAG, context, videoCallView.onPresenterRequestGetFragmentInstance());
    }

    @Override
    public void onServiceRequestLocalMediaCapture(SurfaceViewRenderer videoView) {
        String log = "[SA][onLocalMediaCapture] ";
        if (videoView == null) {
            log += "VideoView is null!";
            Log.d(TAG, log);

            SurfaceViewRenderer selfVideoView = videoCallService.getVideoView(null);
            processAddSelfView(selfVideoView);
        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
            processAddSelfView(videoView);
        }

        //change video layout to horizontal for landscape mode
        //and vertical for portrait mode
        videoCallView.onPresenterRequestchangeViewLayout();

    }

    @Override
    public void onServiceRequestInputVideoResolutionObtained(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        processInputVideoResolutions(width, height, fps, captureFormat);

        String log = "[SA][VideoResInput] The current video input has width x height, fps: " +
                width + " x " + height + ", " + fps + " fps.\r\n";
        toastLog(TAG, context, log);
    }

    @Override
    public void onServiceRequestReceivedVideoResolutionObtained(String peerId, int width, int height, int fps) {
        processReceivedVideoResolutions(width, height, fps);

        String log = "[SA][VideoResRecv] The current video received from Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }

    @Override
    public void onServiceRequestSentVideoResolutionObtained(String peerId, int width, int height, int fps) {
        processSentVideoResolutions(width, height, fps);

        String log = "[SA][VideoResSent] The current video sent to Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }

    @Override
    public void onServiceRequestAudioOutputChanged(boolean isSpeakerOn) {
        videoCallView.onPresenterRequestChangeAudioOuput(isSpeakerOn);
        videoCallService.setCurrentVideoSpeaker(isSpeakerOn);

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

        //Refresh VideoLocalState
        videoCallService.setAudioMute(false);
        videoCallService.setVideoMute(false);
        videoCallService.setCamMute(false);
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
        videoCallService.setAudioMute(isAudioMuted);

        //set mute audio to sdk
        videoCallService.muteLocalAudio(isAudioMuted);

        // Set UI and Toast.
        videoCallView.onPresenterRequestUpdateAudioState(isAudioMuted, true);
    }

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processVideoStateChanged(boolean isVideoMuted) {

        //save audioMuted for other usage
        videoCallService.setVideoMute(isVideoMuted);

        //set mute audio to sdk
        videoCallService.muteLocalVideo(isVideoMuted);

        // Set UI and Toast.
        videoCallView.onPresenterRequestUpdateVideoState(isVideoMuted, true);
    }

    /**
     * Record the current local input video width, height, fps and SkylinkCaptureFormat.
     * Get the range of {@link SkylinkCaptureFormat} supported by the current camera,
     * and write them to {@link #captureFormats} if the camera has changed.
     * If the current VideoDevice is not a camera, this will set captureFormats to null.
     *
     * @param width
     * @param height
     * @param fps
     * @param captureFormat
     */
    private void processInputVideoResolutions(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        // Update UI about input resolution of local video
        VideoResolution videoInputRes = new VideoResolution(width, height, fps);
        videoCallView.onPresenterRequestUpdateUiResInput(videoInputRes);

        SkylinkConfig.VideoDevice currentVideoDevice = videoCallService.getCurrentVideoDevice();
        String previousCameraName = videoCallService.getCurrentCamera();

        String currentCamera = videoCallService.getCurrentCameraName();

        String captureFormatString = "Current capture formats have not changed.";

        // Check if a new camera in now active.
        boolean newCamera = false;
        if (currentCamera != null) {
            if (!currentCamera.equals(previousCameraName)) {
                newCamera = true;
            }
        } else if (previousCameraName != null) {
            if (!previousCameraName.equals(currentCamera)) {
                newCamera = true;
            }
        }

        // Update the UI for setting new video resolution if a new camera has been obtained.
        if (newCamera) {
            // Set the range of supported SkylinkCaptureFormats.
            // Record current range of supported SkylinkCaptureFormats.
            captureFormats = videoCallService.getCaptureFormats(null);
            int currentFps = videoCallService.getCurrentFps();

            //get mCaptureFormats String info
            captureFormatString = videoCallService.getCaptureFormatsString(captureFormats);
            // Try to continue to with last selected Fps if possible.
            int fpsNew = Utils.getFpsForNewCaptureFormat(currentFps, captureFormat);

            if (captureFormat != null) {
                // Set new selected CaptureFormat and frame rate.
                videoCallService.setCurrentCaptureFormat(captureFormat);
                videoCallService.setCurrentFps(fpsNew);
                videoCallService.setCurrentCamera(currentCamera);
            }

            // Set UI values.
            processUpdateUiResDim(width, height, captureFormats);

            processUpdateUiResFps(fpsNew, captureFormat);

        }

        String log = "The current local video by VideoDevice " + currentVideoDevice +
                ", with camera name \"" + currentCamera +
                "\", has width, height, fps: " + width + ", " + height + ", " + fps +
                ".\r\n" + captureFormatString;
        Log.d(TAG, log);

    }

    /**
     * Set the received video resolution from remote peer
     * and Update UI accordingly
     */
    private void processReceivedVideoResolutions(int width, int height, int fps) {
        // Update UI about received resolution from remote peer
        VideoResolution videoReceivedRes = new VideoResolution(width, height, fps);

        videoCallView.onPresenterRequestUpdateUiResReceive(videoReceivedRes);
    }

    /**
     * Set the sent video resolution from local peer to remote peer
     * and Update UI accordingly
     */
    private void processSentVideoResolutions(int width, int height, int fps) {
        // Update UI about sent resolution to remote peer
        VideoResolution videoSentRes = new VideoResolution(width, height, fps);
        videoCallView.onPresenterRequestUpdateUiResSent(videoSentRes);
    }

    /**
     * Get video view by remote peer id
     */
    private SurfaceViewRenderer processGetVideoView(String remotePeerId) {
        return videoCallService.getVideoView(remotePeerId);
    }

    /**
     * Get the remote video view from peer id
     * the local peer is always at index 0 and
     * the remote peer is at index 1 because we just have 2 peers in the room
     */
    private SurfaceViewRenderer processGetRemoteView() {
        SurfaceViewRenderer videoView;
        String remotePeerId = videoCallService.getPeerId(1);
        // Proceed only if the first (& only) remote Peer has joined.
        if (remotePeerId == null) {
            return null;
        } else {
            videoView = processGetVideoView(remotePeerId);
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
    private void processAddRemoteView() {

        SurfaceViewRenderer videoView = processGetRemoteView();

        videoCallView.onPresenterRequestAddRemoteView(videoView);

    }

    /**
     * Get the selected value of width x height seek bar
     */
    private SkylinkCaptureFormat processGetSelectedValueDim(int progress, SkylinkCaptureFormat[] captureFormats) {
        if (!Utils.isCaptureFormatsValid(captureFormats) || progress >= captureFormats.length) {
            return null;
        }
        SkylinkCaptureFormat format = captureFormats[progress];
        return format;
    }

    /**
     * Get the selected value of frame rate seek bar
     */
    private int processGetSelectedValueFps(int progress, SkylinkCaptureFormat format) {
        if (!Utils.isCaptureFormatValid(format)) {
            return -1;
        }
        int fpsMin = format.getFpsMin();
        int fps = fpsMin + progress;
        // Fps value must be within format's fps range.
        if (fps < fpsMin || fps > format.getFpsMax()) {
            return -1;
        }
        return fps;
    }

    /**
     * Return the index of the given SkylinkCaptureFormat from within the list of
     * current supported SkylinkCaptureFormat[].
     *
     * @param captureFormats
     * @param width
     * @param height
     * @return Negative number if index could not be found, or if mCaptureFormats is invalid.
     */
    private int processGetSelectedIndexDim(SkylinkCaptureFormat[] captureFormats, int width, int height) {
        if (!processCheckCaptureFormatsValid(captureFormats)) {
            return -1;
        }

        int index = -1;
        SkylinkCaptureFormat format;
        int length = captureFormats.length;
        for (int i = 0; i < length; ++i) {
            format = captureFormats[i];
            if (width == format.getWidth() && height == format.getHeight()) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Return the index of the given fps from within the range of current supported fps.
     *
     * @param captureFormat
     * @param fps
     * @return Negative number if index could not be found, or if captureFormat is invalid.
     */
    private int processGetSelectedIndexFps(SkylinkCaptureFormat captureFormat, int fps) {
        if (!Utils.isCaptureFormatValid(captureFormat)) {
            return -1;
        }

        int fpsMin = captureFormat.getFpsMin();
        // fps must be within captureFormat's fps range.
        if (fps < fpsMin || fps > captureFormat.getFpsMax()) {
            return -1;
        }

        int index = fps - fpsMin;
        return index;
    }

    /**
     * Once new video resolution dimensions are selected on the Seekbar,
     * change the selected video resolution dimensions TextView to match.
     *
     * @param progress
     * @return {@link SkylinkCaptureFormat} selected.
     */
    private SkylinkCaptureFormat processUpdateUiResOnProgressDim(int progress) {
        SkylinkCaptureFormat format = processGetSelectedValueDim(progress, captureFormats);
        if (format == null) {
            return null;
        }

        // Set textView to match
        int width = format.getWidth();
        int height = format.getHeight();

        videoCallService.setCurrentCaptureFormat(format);
        videoCallView.onPresenterRequestUpdateUiResDimInfo(width, height);

        return format;
    }

    /**
     * Update the UI when video resolution frame rate is changed
     */
    private int processUpdateUiResOnProgressFps(int progress) {
        SkylinkCaptureFormat currentFormat = videoCallService.getCurrentCaptureFormat();
        int fps = processGetSelectedValueFps(progress, currentFormat);

        videoCallService.setCurrentFps(fps);
        videoCallView.onPresenterRequestUpdateUiResFpsInfo(fps);

        return fps;
    }

    /**
     * Set selected resolution dimensions (i.e. width and height) on the UI based on the
     * given range of {@link SkylinkCaptureFormat} array.
     * Both the SeekBar and the TextView would be set, or reset if provided values were not valid.
     *
     * @param width          The width to be set.
     * @param height         The height to be set.
     * @param captureFormats The {@link SkylinkCaptureFormat} array that provides range of seekbar.
     * @return True if provided values were valid and false otherwise.
     */
    private boolean processUpdateUiResDim(int width, int height, SkylinkCaptureFormat[] captureFormats) {
        boolean valid;
        int index = -1;
        // Set the seekbar range.
        valid = processUpdateUiResRangeDim(captureFormats);
        // Get the captureFormat that matches for width and height.
        if (valid) {
            index = processGetSelectedIndexDim(captureFormats, width, height);
            if (index < 0) {
                valid = false;
            }
        }

        if (valid) {
            videoCallView.onPresenterRequestUpdateResDimInfo(index, width, height);
        } else {
            videoCallView.onPresenterRequestUpdateResDimInfo(0, -1, -1);
        }
        return valid;
    }

    /**
     * Set selected resolution frame rate on the UI.
     * Both the SeekBar and the TextView would be set, or reset if provided values were not valid.
     *
     * @param fps    The frame rate in fps (frames per second) to be set.
     * @param format A {@link SkylinkCaptureFormat} that provides the fps range to be set.
     * @return True if provided values were valid and false otherwise.
     */
    private boolean processUpdateUiResFps(int fps, SkylinkCaptureFormat format) {
        boolean valid;
        int index = -1;
        // Set the seekbar range.
        valid = processUpdateUiResRangeFps(format);

        // Get the fps index in format.
        if (valid) {
            index = processGetSelectedIndexFps(format, fps);
            if (index < 0) {
                valid = false;
            }
        }

        if (valid) {
            videoCallView.onPresenterRequestUpdateResFpsInfo(index, fps);
        } else {
            videoCallView.onPresenterRequestUpdateResFpsInfo(0, -1);
        }
        return valid;
    }

    /**
     * Set the ranges of values for seekBarResDim.
     * Set to zero if range of values invalid.
     *
     * @param captureFormats SkylinkCaptureFormat array for setting range of seekBarResDim.
     * @return True if mCaptureFormats was valid and false otherwise.
     */
    private boolean processUpdateUiResRangeDim(SkylinkCaptureFormat[] captureFormats) {
        // If there is no valid mCaptureFormats set, set seekBar range to zero.

        int rangeResDimMax = 0;
        boolean isValid = false;

        if (processCheckCaptureFormatsValid(captureFormats)) {
            rangeResDimMax = captureFormats.length - 1;
            isValid = true;
        }

        // Set dimension range based on size of current CaptureFormat list.
        videoCallView.onPresenterRequestUpdateUiResRangeDimInfo(rangeResDimMax);

        return isValid;
    }

    /**
     * Set the ranges of values for seekBarResFps.
     * Set to zero if range of values invalid.
     *
     * @return True if captureFormat was valid and false otherwise.
     */
    private boolean processUpdateUiResRangeFps(SkylinkCaptureFormat captureFormat) {
        // If there is no valid mCaptureFormats set, set seekBar range to zero.

        int maxFpsRange = 0;
        boolean isValid = false;

        if (Utils.isCaptureFormatValid(captureFormat)) {
            maxFpsRange = captureFormat.getFpsMax() - captureFormat.getFpsMin();
            isValid = true;
        }

        // Set dimension range based on size of current CaptureFormat list.
        videoCallView.onPresenterRequestUpdateUiResRangeFpsInfo(maxFpsRange);

        return isValid;
    }

    /**
     * Change the video resolution state and UI when local input video resolution changed
     */
    private boolean processUpdateInputVideoResolutions(SkylinkCaptureFormat format, int fpsNew) {
        if (format == null) {
            return false;
        }
        int width = format.getWidth();
        int height = format.getHeight();
        if (width < 0 || height < 0 || fpsNew < 0) {
            return false;
        }

        // Set new selected CaptureFormat and frame rate.
        videoCallService.setCurrentCaptureFormat(format);
        videoCallService.setCurrentFps(fpsNew);
        videoCallService.setInputVideoResolution(width, height, fpsNew);

        return true;
    }

    /**
     * Checks if given {@link SkylinkCaptureFormat SkylinkCaptureFormat[]} is valid for using.
     * To be valid, it cannot be null or empty.
     *
     * @param captureFormats
     * @return
     */
    private boolean processCheckCaptureFormatsValid(SkylinkCaptureFormat[] captureFormats) {
        if (captureFormats == null || captureFormats.length == 0) {
            return false;
        }
        return true;
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
