package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.content.Context;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.VideoService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
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

    private Context mContext;

    // The view instance
    public VideoCallContract.View mVideoCallView;

    // The service instance
    private VideoService mVideoCallService;

    //utils to process permission
    private PermissionUtils mPermissionUtils;

    // Video resolution from local camera input.
    private static VideoResolution mVideoInput;

    // Video resolution from camera sent out to Peer.
    private static VideoResolution mVideoSent;

    // Video resolution received from Peer.
    private static VideoResolution mVideoReceive;

    // The current VideoDevice.
    private SkylinkConfig.VideoDevice mCurrentVideoDevice = null;

    // The current camera name.
    private String mCurrentCameraName = null;

    // The array of SkylinkCaptureFormats support by the current camera.
    private static SkylinkCaptureFormat[] mCaptureFormats;

    // The selected SkylinkCaptureFormat on UI,
    private static SkylinkCaptureFormat mCurrentCaptureFormat = null;

    // The selected frame rate (fps) on UI,
    private static int mCurrentFps = -1;

    //current speaker output (on/off)
    private boolean isSpeakerOn;

    public VideoCallPresenter(Context context) {
        this.mContext = context;
        initVideoResolutions();

        this.mVideoCallService = new VideoService(context);
        this.mVideoCallService.setPresenter(this);
        this.mPermissionUtils = new PermissionUtils();
    }

    public void setView(VideoCallContract.View view) {
        mVideoCallView = view;
        mVideoCallView.setPresenter(this);
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
        if (!mVideoCallService.isConnectingOrConnected()) {

            //reset permission request states.
            mPermissionUtils.permQReset();

            //connect to room on Skylink connection
            processConnectToRoom();

            //default setting for video output
            mVideoCallService.setCurrentVideoSpeaker(Utils.getDefaultVideoSpeaker());

            //after connected to skylink SDK, UI will be updated latter on AudioService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //if it already connected to room, then resume permission
            mPermissionUtils.permQResume(mContext, mVideoCallView.onPresenterRequestGetFragmentInstance());

            //update UI into connected
            processUpdateConnectedUI();

            Log.d(TAG, "Try to update UI when changing configuration");
        }

        //get default audio output settings
        isSpeakerOn = mVideoCallService.getCurrentVideoSpeaker();
        mVideoCallView.onPresenterRequestChangeAudioOuput(isSpeakerOn);

    }

    @Override
    public void onViewRequestDisconnectFromRoom() {
        mVideoCallService.disconnectFromRoom();
    }

    @Override
    public void onViewRequestExit() {
        //process disconnect from room
        mVideoCallService.disconnectFromRoom();

        //reset default audio speaker
        mVideoCallService.setCurrentVideoSpeaker(Utils.getDefaultVideoSpeaker());

        //after disconnected from skylink SDK, UI will be updated latter on VideoService.onDisconnect
    }

    @Override
    public void onViewRequestResume() {
        // Toggle camera back to previous state if required.
        if (mVideoCallService.isCameraToggle()) {
            if (mVideoCallService.getVideoView(null) != null) {
                // change camera state
                mVideoCallService.toggleCamera(true);
                // change UI
                mVideoCallView.onPresenterRequestChangeCameraUI(false);
            }
        } else {
            // change camera state
            mVideoCallService.toggleCamera(false);
            // change UI
            mVideoCallView.onPresenterRequestChangeCameraUI(true);
        }
    }

    @Override
    public void onViewRequestPause() {
        //stop camera when pause
        mVideoCallService.toggleCamera(false);
    }

    @Override
    public void onViewRequestChangeAudioState() {
        // get current audio state
        boolean isAudioMute = mVideoCallService.isAudioMute();

        //change UI and audio state to opposite state
        if (!isAudioMute) {
            mVideoCallView.onPresenterRequestChangeAudioUI(true);
        } else {
            mVideoCallView.onPresenterRequestChangeAudioUI(false);
        }

        processAudioStateChanged(!isAudioMute);
    }

    @Override
    public void onViewRequestChangeVideoState() {
        // get current video state
        boolean isVideoMute = mVideoCallService.isVideoMute();

        //change UI and video state to opposite state
        if (!isVideoMute) {
            mVideoCallView.onPresenterRequestChangeVideoUI(true);
        } else {
            mVideoCallView.onPresenterRequestChangeVideoUI(false);
        }

        processVideoStateChanged(!isVideoMute);
    }

    @Override
    public void onViewRequestChangeCameraState() {
        //get current camera state: true is active, false is stop
        boolean isCamActive = mVideoCallService.isCameraToggle();

        //change state
        mVideoCallService.setCamToggle(!isCamActive);

        //change UI
        if (mVideoCallService.isCameraToggle()) {
            mVideoCallView.onPresenterRequestChangeCameraUI(false);
        } else {
            mVideoCallView.onPresenterRequestChangeCameraUI(true);
        }

        //change camera state in service layer
        mVideoCallService.toggleCamera(mVideoCallService.isCameraToggle());
    }

    @Override
    public void onViewRequestChangeAudioOutput() {
        //change current speakerOn
        isSpeakerOn = !isSpeakerOn;

        // use service layer to change the audio output
        mVideoCallService.changeSpeakerOutput(isSpeakerOn);
    }

    @Override
    public void onViewRequestSwitchCamera() {
        mVideoCallService.switchCamera();
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
        int fpsNew = Utils.getFpsForNewCaptureFormat(mCurrentFps, format);

        // If any of the new Dim or Fps values are not valid,
        // or setting new resolution was not successful, reset UI to previous values.
        boolean result1 = mVideoCallView.onPresenterRequestUpdateUiResDimInfo(width, height);
        boolean result2 = processUpdateUiResFps(fpsNew, format);
        boolean result3 = processUpdateInputVideoResolutions(format, fpsNew);

        if (fpsNew < 0 || !result1 || !result2 || result3) {
            processUpdateUiResDim(mCurrentCaptureFormat.getWidth(), mCurrentCaptureFormat.getHeight(),
                    mCaptureFormats);
            processUpdateUiResFps(mCurrentFps, mCurrentCaptureFormat);
            return;
        }
    }

    @Override
    public void onViewRequestFpsSelected(int progress) {
        int fpsNew = processUpdateUiResOnProgressFps(progress);

        // Check that new fps is valid for selected CaptureFormat.
        fpsNew = Utils.getFpsForNewCaptureFormat(fpsNew, mCurrentCaptureFormat);

        // If any of new Fps or selected CaptureFormat are not valid,
        // or setting new resolution was not successful, reset Fps UI to previous values.
        boolean result1 = processUpdateInputVideoResolutions(mCurrentCaptureFormat, fpsNew);

        if (fpsNew < 0 || result1) {
            processUpdateUiResFps(mCurrentFps, mCurrentCaptureFormat);
            return;
        }
    }

    @Override
    public String onViewRequestGetRoomPeerIdNick() {
        return mVideoCallService.getRoomIdAndNickname(Constants.CONFIG_TYPE.VIDEO);
    }

    @Override
    public void onViewRequestGetVideoResolutions() {
        String peerId = mVideoCallService.getPeerId(1);
        mVideoCallService.getVideoResolutions(peerId);
    }

    @Override
    public void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag) {
        // delegate to PermissionUtils to process the permissions
        mPermissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, tag);
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful) {
            mVideoCallView.onPresenterRequestConnectedUIChange();

            //start audio routing if has audio config
            SkylinkConfig skylinkConfig = mVideoCallService.getSkylinkConfig();
            if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
                AudioRouter.setPresenter(this);
                AudioRouter.startAudioRouting(mContext, Constants.CONFIG_TYPE.VIDEO);
            }

        } else {
            processDisconnectUIChange();
        }
    }

    @Override
    public void onServiceRequestDisconnect() {
        processDisconnectUIChange();

        //stop audio routing
        SkylinkConfig skylinkConfig = mVideoCallService.getSkylinkConfig();
        if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
            AudioRouter.stopAudioRouting(mContext);
        }
    }

    @Override
    public void onServiceRequestRemotePeerLeave(String remotePeerId, int removeIndex) {
        mVideoCallView.onPresenterRequestRemoveRemotePeer();
    }

    @Override
    public void onServiceRequestRemotePeerConnectionRefreshed(String log, UserInfo remotePeerUserInfo) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        toastLog(TAG, mContext, log);
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
        mPermissionUtils.onPermissionRequiredHandler(info, TAG, mContext, mVideoCallView.onPresenterRequestGetFragmentInstance());
    }

    @Override
    public void onServiceRequestLocalMediaCapture(SurfaceViewRenderer videoView) {
        String log = "[SA][onLocalMediaCapture] ";
        if (videoView == null) {
            log += "VideoView is null!";
            Log.d(TAG, log);

            SurfaceViewRenderer selfVideoView = mVideoCallService.getVideoView(null);
            processAddSelfView(selfVideoView);
        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
            processAddSelfView(videoView);
        }

        //change video layout to horizontal for landscape mode
        //and vertical for portrait mode
        mVideoCallView.onPresenterRequestchangeViewLayout();

    }

    @Override
    public void onServiceRequestInputVideoResolutionObtained(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        processInputVideoResolutions(width, height, fps, captureFormat);

        String log = "[SA][VideoResInput] The current video input has width x height, fps: " +
                width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
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
        this.isSpeakerOn = isSpeakerOn;
        mVideoCallView.onPresenterRequestChangeAudioOuput(isSpeakerOn);
        mVideoCallService.setCurrentVideoSpeaker(isSpeakerOn);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Init the video input, video sent and video received state
     * */
    private void initVideoResolutions() {
        if (mVideoInput == null || mVideoSent == null || mVideoReceive == null) {
            mVideoInput = new VideoResolution();
            mVideoSent = new VideoResolution();
            mVideoReceive = new VideoResolution();
        }
    }

    /**
     * Process connect to room on service layer and update UI accordingly
     * */
    private void processConnectToRoom() {

        //connect to SDK
        mVideoCallService.connectToRoom(Constants.CONFIG_TYPE.VIDEO);

        //update UI and make toast
        mVideoCallView.onPresenterRequestConnectingUIChange();

        //Refresh VideoLocalState
        mVideoCallService.setAudioMute(false);
        mVideoCallService.setVideoMute(false);
        mVideoCallService.setCamToggle(true);

        //get roomName from setting
        String log = "Entering video room \"" + Config.ROOM_NAME_VIDEO + "\".";
        toastLog(TAG, mContext, log);
    }

    /**
     * Update UI into connected state
     * */
    private void processUpdateConnectedUI() {
        // update the audio state
        if (mVideoCallService.isAudioMute()) {
            processAudioStateChanged(true);
        }
        //update the video state
        if (mVideoCallService.isVideoMute()) {
            processVideoStateChanged(true);
        }

        // Toggle camera back to previous state if required.
        if (mVideoCallService.isCameraToggle() && processGetVideoView(null) != null) {
            processCameraStateChanged(false);
        } else {
            processCameraStateChanged(true);
        }

        //update UI into connected state
        mVideoCallView.onPresenterRequestConnectedUIChange();

        // add local video view
        mVideoCallView.onPresenterRequestAddSelfView(processGetVideoView(null));

        // add remote video view if existed
        SurfaceViewRenderer remoteView = processGetRemoteView();
        if (remoteView != null) {
            mVideoCallView.onPresenterRequestAddRemoteView(remoteView);
        }
    }

    /**
     * Update UI into disconnected state
     * */
    private void processDisconnectUIChange() {
        // reset the video resolution state
        mVideoInput = null;
        mVideoSent = null;
        mVideoReceive = null;
        // update UI
        mVideoCallView.onPresenterRequestDisconnectUIChange();
    }

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processAudioStateChanged(boolean isAudioMuted) {

        //save audioMuted for other usage
        mVideoCallService.setAudioMute(isAudioMuted);

        //set mute audio to sdk
        mVideoCallService.muteLocalAudio(isAudioMuted);

        // Set UI and Toast.
        mVideoCallView.onPresenterRequestUpdateAudioState(isAudioMuted, true);
    }

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processVideoStateChanged(boolean isVideoMuted) {

        //save audioMuted for other usage
        mVideoCallService.setVideoMute(isVideoMuted);

        //set mute audio to sdk
        mVideoCallService.muteLocalVideo(isVideoMuted);

        // Set UI and Toast.
        mVideoCallView.onPresenterRequestUpdateVideoState(isVideoMuted, true);
    }

    // If video is enable, toggle video to stopped and if video is stopped, then enable it
    private void processCameraStateChanged(boolean isCameraStop) {
        if (processGetVideoView(null) != null) {
            if (isCameraStop) {
                //change UI
                mVideoCallView.onPresenterRequestChangeCameraUI(true);
            } else {
                mVideoCallView.onPresenterRequestChangeCameraUI(false);
            }
        }
    }

    /**
     * Record the current local input video width, height, fps and SkylinkCaptureFormat.
     * Get the range of {@link SkylinkCaptureFormat} supported by the current camera,
     * and write them to {@link #mCaptureFormats} if the camera has changed.
     * If the current VideoDevice is not a camera, this will set mCaptureFormats to null.
     *
     * @param width
     * @param height
     * @param fps
     * @param captureFormat
     */
    private void processInputVideoResolutions(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {

        if (mVideoInput == null) {
            return;
        }
        mVideoInput.setWidth(width);
        mVideoInput.setHeight(height);
        mVideoInput.setFps(fps);

        mVideoCallView.onPresenterRequestUpdateUiResInput(mVideoInput);

        mCurrentVideoDevice = mVideoCallService.getCurrentVideoDevice();
        String previousCameraName = mCurrentCameraName;
        mCurrentCameraName = mVideoCallService.getCurrentCameraName();

        String captureFormatString = "Current capture formats have not changed.";

        // Check if a new camera in now active.
        boolean newCamera = false;
        if (mCurrentCameraName != null) {
            if (!mCurrentCameraName.equals(previousCameraName)) {
                newCamera = true;
            }
        } else if (previousCameraName != null) {
            if (!previousCameraName.equals(mCurrentCameraName)) {
                newCamera = true;
            }
        }

        // Update the UI for setting new video resolution if a new camera has been obtained.
        if (newCamera) {
            // Set the range of supported SkylinkCaptureFormats.
            // Record current range of supported SkylinkCaptureFormats.
            mCaptureFormats = mVideoCallService.getCaptureFormats(null);

            //get mCaptureFormats String info
            captureFormatString = mVideoCallService.getCaptureFormatsString(mCaptureFormats);
            // Try to continue to with last selected Fps if possible.
            int fpsNew = Utils.getFpsForNewCaptureFormat(mCurrentFps, captureFormat);

            if (captureFormat != null) {
                // Set new selected CaptureFormat and frame rate.
                mCurrentCaptureFormat = captureFormat;
                mCurrentFps = fpsNew;
            }

            // Set UI values.
            processUpdateUiResDim(width, height, mCaptureFormats);

            processUpdateUiResFps(fpsNew, captureFormat);

        }

        String log = "The current local video by VideoDevice " + mCurrentVideoDevice +
                ", with camera name \"" + mCurrentCameraName +
                "\", has width, height, fps: " + width + ", " + height + ", " + fps +
                ".\r\n" + captureFormatString;
        Log.d(TAG, log);

    }

    /**
     * Set the received video resolution from remote peer
     * and Update UI accordingly
     * */
    private void processReceivedVideoResolutions(int width, int height, int fps) {
        if (mVideoReceive == null) {
            return;
        }
        mVideoReceive.setWidth(width);
        mVideoReceive.setHeight(height);
        mVideoReceive.setFps(fps);

        mVideoCallView.onPresenterRequestUpdateUiResReceive(mVideoReceive);
    }

    /**
     * Set the sent video resolution from local peer to remote peer
     * and Update UI accordingly
     * */
    private void processSentVideoResolutions(int width, int height, int fps) {
        if (mVideoSent == null) {
            return;
        }
        mVideoSent.setWidth(width);
        mVideoSent.setHeight(height);
        mVideoSent.setFps(fps);

        mVideoCallView.onPresenterRequestUpdateUiResSent(mVideoSent);
    }

    /**
     * Get video view by remote peer id
     * */
    private SurfaceViewRenderer processGetVideoView(String remotePeerId) {
        return mVideoCallService.getVideoView(remotePeerId);
    }

    /**
     * Get the remote video view from peer id
     * the local peer is always at index 0 and
     * the remote peer is at index 1 because we just have 2 peers in the room
     * */
    private SurfaceViewRenderer processGetRemoteView() {
        SurfaceViewRenderer videoView;
        String remotePeerId = mVideoCallService.getPeerId(1);
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
     * */
    private void processAddSelfView(SurfaceViewRenderer videoView) {
        mVideoCallView.onPresenterRequestAddSelfView(videoView);
    }

    /**
     * Add remote video view into the layout
     * */
    private void processAddRemoteView() {

        SurfaceViewRenderer videoView = processGetRemoteView();

        mVideoCallView.onPresenterRequestAddRemoteView(videoView);

    }

    /**
     * Get the selected value of width x height seek bar
     * */
    private SkylinkCaptureFormat processGetSelectedValueDim(int progress, SkylinkCaptureFormat[] captureFormats) {
        if (!Utils.isCaptureFormatsValid(captureFormats) || progress >= captureFormats.length) {
            return null;
        }
        SkylinkCaptureFormat format = captureFormats[progress];
        return format;
    }

    /**
     * Get the selected value of frame rate seek bar
     * */
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
        SkylinkCaptureFormat format = processGetSelectedValueDim(progress, mCaptureFormats);
        if (format == null) {
            return null;
        }

        // Set textView to match
        int width = format.getWidth();
        int height = format.getHeight();

        mVideoCallView.onPresenterRequestUpdateUiResDimInfo(width, height);

        return format;
    }

    /**
     * Update the UI when video resolution frame rate is changed
     */
    private int processUpdateUiResOnProgressFps(int progress) {
        int fps = processGetSelectedValueFps(progress, mCurrentCaptureFormat);

        mVideoCallView.onPresenterRequestUpdateUiResFpsInfo(fps);

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
            mVideoCallView.onPresenterRequestUpdateResDimInfo(index, width, height);
        } else {
            mVideoCallView.onPresenterRequestUpdateResDimInfo(0, -1, -1);
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
            mVideoCallView.onPresenterRequestUpdateResFpsInfo(index, fps);
        } else {
            mVideoCallView.onPresenterRequestUpdateResFpsInfo(0, -1);
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
        mVideoCallView.onPresenterRequestUpdateUiResRangeDimInfo(rangeResDimMax);

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
        mVideoCallView.onPresenterRequestUpdateUiResRangeFpsInfo(maxFpsRange);

        return isValid;
    }

    /**
     * Change the video resolution state and UI when local input video resolution changed
     * */
    private boolean processUpdateInputVideoResolutions(SkylinkCaptureFormat format, int fpsNew) {
        int width = format.getWidth();
        int height = format.getHeight();
        if (width < 0 || height < 0 || fpsNew < 0) {
            return false;
        }

        // Set new selected CaptureFormat and frame rate.
        mCurrentCaptureFormat = format;
        mCurrentFps = fpsNew;

        // If already at new resolution, no need to call Skylink API.
        if (mVideoInput.getWidth() == width && mVideoInput.getHeight() == height && mVideoInput.getFps() == fpsNew) {
            return true;
        }

        mVideoCallService.setInputVideoResolution(width, height, fpsNew);

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
}
