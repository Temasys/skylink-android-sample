package sg.com.temasys.skylink.sdk.sampleapp.videocall;

import android.content.Context;
import android.graphics.Point;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.VideoView;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.VideoResolution;
import sg.com.temasys.skylink.sdk.sampleapp.data.service.VideoService;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class VideoCallPresenter implements VideoCallContract.Presenter {

    private final String TAG = VideoCallPresenter.class.getName();

    private Context mContext;

    public VideoCallContract.View mVideoCallView;
    private VideoService mVideoCallService;

    //utils to process permission
    private PermissionUtils mPermissionUtils;

    // Video resolution from camera input.
    private VideoResolution videoInput;

    // Video resolution from camera sent out to Peer.
    private VideoResolution videoSent;

    // Video resolution received from Peer.
    private VideoResolution videoReceive;

    // The current VideoDevice.
    private SkylinkConfig.VideoDevice currentVideoDevice = null;
    // The current camera name.
    private String currentCameraName = null;
    // The array of SkylinkCaptureFormats support by the current camera.
    private static SkylinkCaptureFormat[] captureFormats;
    // The selected SkylinkCaptureFormat on UI,
    // not necessarily the currently used SkylinkCaptureFormat.
    private static SkylinkCaptureFormat captureFormatSel = null;
    // The last selected frame rate (fps) on UI,
    // not necessarily the currently used frame rate.
    private static int fpsSel = -1;


    public VideoCallPresenter(VideoCallContract.View videoCallView, Context context) {
        this.mContext = context;

        this.mVideoCallView = videoCallView;

        mVideoCallService = new VideoService(context);

        mPermissionUtils = new PermissionUtils(context);

        initialize();

    }

    private void initialize() {
        this.mVideoCallView.setPresenter(this);
        this.mVideoCallService.setPresenter(this);

        this.mVideoCallService.setTypeCall();

        videoInput = new VideoResolution();
        videoSent = new VideoResolution();
        videoReceive = new VideoResolution();
    }

    private void onDisconnectUIChangePresenterHandler() {
        videoInput = new VideoResolution();
        videoSent = new VideoResolution();
        videoReceive = new VideoResolution();

        mVideoCallView.onDisconnectUIChangeViewHandler();
    }

    private void addSelfViewPresenterHandler(SurfaceViewRenderer videoView) {
        mVideoCallView.onAddSelfViewViewHandler(videoView);
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

    private void noteInputVideoResolutionsPresenterHandler(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {

        videoInput.setWidth(width);
        videoInput.setHeight(height);
        videoInput.setFps(fps);

        mVideoCallView.onSetTvResInputStatsViewHandler(videoInput);

        currentVideoDevice = mVideoCallService.getCurrentVideoDeviceServiceHandler();
        String previousCameraName = currentCameraName;
        currentCameraName = mVideoCallService.getCurrentCameraNameServiceHandler();

        String captureFormatString = "Current capture formats have not changed.";

        // Check if a new camera in now active.
        boolean newCamera = false;
        if (currentCameraName != null) {
            if (!currentCameraName.equals(previousCameraName)) {
                newCamera = true;
            }
        } else if (previousCameraName != null) {
            if (!previousCameraName.equals(currentCameraName)) {
                newCamera = true;
            }
        }

        // Update the UI for setting new video resolution if a new camera has been obtained.
        if (newCamera) {
            // Set the range of supported SkylinkCaptureFormats.
            // Record current range of supported SkylinkCaptureFormats.
            captureFormats = mVideoCallService.getCaptureFormatsServiceHandler(null);

            //get captureFormats String info
            captureFormatString = mVideoCallService.getCaptureFormatsStringServiceHandler(captureFormats);
            // Try to continue to with last selected Fps if possible.
            int fpsNew = Utils.getFpsForNewCaptureFormat(fpsSel, captureFormat);

            if (captureFormat != null) {
                // Set new selected CaptureFormat and frame rate.
                captureFormatSel = captureFormat;
                fpsSel = fpsNew;
            }

            // Set UI values.
            setUiResDimPresenterHandler(width, height, captureFormats);

            setUiResFpsPresenterHandler(fpsNew, captureFormat);

        }

        String log = "The current local video by VideoDevice " + currentVideoDevice +
                ", with camera name \"" + currentCameraName +
                "\", has width, height, fps: " + width + ", " + height + ", " + fps +
                ".\r\n" + captureFormatString;
        Log.d(TAG, log);

    }

    private void setUiResTvStatsReceivedPresenterHandler(int width, int height, int fps) {
        videoReceive.setWidth(width);
        videoReceive.setHeight(height);
        videoReceive.setFps(fps);

        mVideoCallView.onSetUiResTvStatsReceiveViewHandler(videoReceive);
    }

    private void setUiResTvStatsSentPresenterHandler(int width, int height, int fps) {
        videoSent.setWidth(width);
        videoSent.setHeight(height);
        videoSent.setFps(fps);

        mVideoCallView.onSetUiResTvStatsSentViewHandler(videoSent);
    }

    private SurfaceViewRenderer getRemoteViewPresenterHandler() {
        SurfaceViewRenderer videoView;
        String remotePeerId = mVideoCallService.getPeerIdServiceHandler(1);
        // Proceed only if the first (& only) remote Peer has joined.
        if (remotePeerId == null) {
            return null;
        } else {
            videoView = getVideoViewPresenterHandler(remotePeerId);
        }

        return videoView;
    }

    private void addRemoteViewPresenterHandler() {

        SurfaceViewRenderer videoView = getRemoteViewPresenterHandler();

        mVideoCallView.onAddRemoteViewViewHandler(videoView);

    }

    @Override
    public void disconnectFromRoomPresenterHandler() {
        mVideoCallService.disconnectFromRoomServiceHandler();
    }

    @Override
    public String getPeerIdPresenterHandler(int index) {
        return mVideoCallService.getPeerIdServiceHandler(index);
    }

    private SurfaceViewRenderer getVideoViewPresenterHandler(String remotePeerId) {
        return mVideoCallService.getVideoViewServiceHandler(remotePeerId);
    }

    @Override
    public String getRoomPeerIdNickPresenterHandler() {
        return mVideoCallService.getRoomPeerIdNickServiceHandler();
    }

    @Override
    public void getVideoResolutionsPresenterHandler(String peerIdPresenterHandler) {
        mVideoCallService.getVideoResolutionsServiceHandler(peerIdPresenterHandler);
    }

    @Override
    public void switchCameraPresenterHandler() {
        mVideoCallService.switchCameraServiceHandler();
    }

    private SkylinkCaptureFormat getSeekBarValueDimPresenterHandler(int progress, SkylinkCaptureFormat[] captureFormats) {
        if (!Utils.isCaptureFormatsValid(captureFormats) || progress >= captureFormats.length) {
            return null;
        }
        SkylinkCaptureFormat format = captureFormats[progress];
        return format;
    }

    @Override
    public void onViewLayoutRequestedPresenterHandler() {
        Log.d(TAG, "onViewLayoutRequestedPresenterHandler");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mVideoCallService.isConnectingOrConnectedServiceHandler()) {

            //reset permission request states.
            mPermissionUtils.permQReset();

            //connect to room on Skylink connection
            connectToRoomPresenterHandler();

            //after connected to skylink SDK, UI will be updated latter on AudioService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //if it already connected to room, then resume permission
            mPermissionUtils.permQResume(mContext, mVideoCallView.onGetFragmentViewHandler());

            //update UI into connected
            updateConnectedUIPresenterHandler();

            Log.d(TAG, "Try to update UI when changing configuration");
        }

    }

    private void connectToRoomPresenterHandler() {

        //connect to SDK
        mVideoCallService.connectToRoomServiceHandler();

        //update UI and make toast
        mVideoCallView.onConnectingUIChangeViewHandler();

        //get roomName from setting
        String log = "Entering video room \"" + Config.ROOM_NAME_VIDEO + "\".";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onViewExitPresenterHandler() {
        //process disconnect from room
        mVideoCallService.disconnectFromRoomServiceHandler();

        //after disconnected from skylink SDK, UI will be updated latter on AudioService.onDisconnect
    }

    @Override
    public void onViewResumePresenterHandler(){

        // Toggle camera back to previous state if required.
        if(mVideoCallService.isCameraToggleServiceHandler()){

            if(mVideoCallService.getVideoViewServiceHandler(null) != null){

                mVideoCallService.toggleCameraServiceHandler();

                mVideoCallService.setCamToggleServiceHandler(false);

            }
        }
    }

    @Override
    public void onConnectPresenterHandler(boolean isSuccessful) {
        if (isSuccessful) {

            mVideoCallView.onConnectedUIChangeViewHandler();

        } else {
            onDisconnectUIChangePresenterHandler();
        }
    }

    @Override
    public void onDisconnectPresenterHandler() {
        onDisconnectUIChangePresenterHandler();
    }

    @Override
    public void onRemotePeerJoinPresenterHandler(String remotePeerId, String nick) {
        //do nothing
    }

    @Override
    public void onRemotePeerLeavePresenterHandler(String remotePeerId) {
        mVideoCallView.onRemotePeerLeaveUIChangeViewHandler();
    }

    private void updateConnectedUIPresenterHandler() {

        if (mVideoCallService.isAudioMuteServiceHandler()) {
            processBtnAudioMutePresenterHandler(true);
        }

        if (mVideoCallService.isVideoMuteServiceHandler()) {
            processBtnVideoMutePresenterHandler(true);
        }

        // Toggle camera back to previous state if required.
        if (mVideoCallService.isCameraToggleServiceHandler() && getVideoViewPresenterHandler(null) != null) {
            processBtnCameraTogglePresenterHandler(true);
        }

        //update UI
        mVideoCallView.onConnectedUIChangeViewHandler();

        mVideoCallView.onAddSelfViewViewHandler(getVideoViewPresenterHandler(null));

        SurfaceViewRenderer remoteView = getRemoteViewPresenterHandler();

        if(remoteView != null) {
            mVideoCallView.onAddRemoteViewViewHandler(remoteView);
        }

    }

    @Override
    public void processBtnAudioMutePresenterHandler() {
        boolean isAudioMute = mVideoCallService.isAudioMuteServiceHandler();

        processBtnAudioMutePresenterHandler(!isAudioMute);
    }

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processBtnAudioMutePresenterHandler(boolean isAudioMuted) {

        //save audioMuted for other usage
        mVideoCallService.setAudioMuteServiceHandler(isAudioMuted);

        //set mute audio to sdk
        mVideoCallService.muteLocalAudioServiceHandler(isAudioMuted);

        // Set UI and Toast.
        mVideoCallView.onSetAudioBtnLabelViewHandler(isAudioMuted, true);
    }

    @Override
    public void processBtnVideoMutePresenterHandler() {
        boolean isVideoMute = mVideoCallService.isVideoMuteServiceHandler();

        processBtnVideoMutePresenterHandler(!isVideoMute);
    }

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processBtnVideoMutePresenterHandler(boolean isVideoMuted) {

        //save audioMuted for other usage
        mVideoCallService.setVideoMuteServiceHandler(isVideoMuted);

        //set mute audio to sdk
        mVideoCallService.muteLocalVideoServiceHandler(isVideoMuted);

        // Set UI and Toast.
        mVideoCallView.onSetVideoBtnLabelViewHandler(isVideoMuted, true);
    }

    @Override
    public void processBtnCameraTogglePresenterHandler() {
        boolean isCamToggle = mVideoCallService.isCameraToggleServiceHandler();

        processBtnCameraTogglePresenterHandler(!isCamToggle);
    }

    // If video is enable, toggle video and if video is toggle, then enable it
    private void processBtnCameraTogglePresenterHandler(boolean isCameraToggle) {

        //change state of camera toggle
        mVideoCallService.setCamToggleServiceHandler(isCameraToggle);

        //display instruction log
        String log12 = "Toggled camera ";
        if (getVideoViewPresenterHandler(null) != null) {
            if (mVideoCallService.toggleCameraServiceHandler()) {
                log12 += "to restarted!";
            } else {
                log12 += "to stopped!";
            }
        } else {
            log12 += "but failed as local video is not available!";
        }
        toastLog(TAG, mContext, log12);

        //this button don't need to change text
    }

    @Override
    public void onViewPausePresenterHandler() {
        boolean toggleCamera = mVideoCallService.toggleCameraServiceHandler(false);

        mVideoCallService.setCamToggleServiceHandler(toggleCamera);
    }

    @Override
    public void onPermissionRequiredPresenterHandler(PermRequesterInfo info) {
        mPermissionUtils.onPermissionRequiredHandler(info, TAG, mContext, mVideoCallView.onGetFragmentViewHandler());
    }

    @Override
    public void onRequestPermissionsResultPresenterHandler(int requestCode, String[] permissions, int[] grantResults, String tag) {
        mPermissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, tag);
    }

    @Override
    public void onPermissionGrantedPresenterHandler(String[] permissions, int infoCode) {
        mPermissionUtils.onPermissionGrantedHandler(permissions, infoCode, TAG);
    }

    @Override
    public void onPermissionDeniedPresenterHandler(int infoCode) {
        mPermissionUtils.onPermissionDeniedHandler(infoCode, mContext, TAG);
    }

    @Override
    public void onRemotePeerConnectionRefreshedPresenterHandler(String log, UserInfo remotePeerUserInfo) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onLocalMediaCapturePresenterHandler(SurfaceViewRenderer videoView) {
        String log = "[SA][onLocalMediaCapture] ";
        if (videoView == null) {
            log += "VideoView is null!";
            Log.d(TAG, log);

            SurfaceViewRenderer selfVideoView = mVideoCallService.getVideoViewServiceHandler(null);
            addSelfViewPresenterHandler(selfVideoView);
        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
            addSelfViewPresenterHandler(videoView);
        }
    }

    @Override
    public void onInputVideoResolutionObtainedPresenterHandler(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        noteInputVideoResolutionsPresenterHandler(width, height, fps, captureFormat);

        String log = "[SA][VideoResInput] The current video input has width x height, fps: " +
                width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }

    @Override
    public void onReceivedVideoResolutionObtainedPresenterHandler(String peerId, int width, int height, int fps) {
        setUiResTvStatsReceivedPresenterHandler(width, height, fps);

        String log = "[SA][VideoResRecv] The current video received from Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }

    @Override
    public void onSentVideoResolutionObtainedPresenterHandler(String peerId, int width, int height, int fps) {
        setUiResTvStatsSentPresenterHandler(width, height, fps);

        String log = "[SA][VideoResSent] The current video sent to Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }

    @Override
    public void onVideoSizeChangePresenterHandler(String peerId, Point size) {
        String peer = "Peer " + peerId;
        // If peerId is null, this call is for our local video.
        if (peerId == null) {
            peer = "We've";
        }
        Log.d(TAG, peer + " got video size changed to: " + size.toString() + ".");
    }

    @Override
    public void onRemotePeerMediaReceivePresenterHandler(String log, UserInfo remotePeerUserInfo) {
        addRemoteViewPresenterHandler();

        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        Log.d(TAG, log);
    }

    @Override
    public void onDimProgressChangedPresenterHandler(int progress) {
        setUiResTvOnSeekBarProgressDimPresenterHandler(progress);
    }

    @Override
    public void onFpsProgressChangedPresenterHandler(int progress) {
        setUiResTvOnSeekBarProgressFpsPresenterHandler(progress);
    }

    private int setUiResTvOnSeekBarProgressFpsPresenterHandler(int progress) {
        int fps = getSeekBarValueFpsPresenterHandler(progress, captureFormatSel);

        mVideoCallView.onSetUiResTvFpsViewHandler(fps);

        return fps;
    }

    /**
     * Once new video resolution dimensions are selected on the Seekbar,
     * change the selected video resolution dimensions TextView to match.
     *
     * @param progress
     * @return {@link SkylinkCaptureFormat} selected.
     */
    private SkylinkCaptureFormat setUiResTvOnSeekBarProgressDimPresenterHandler(int progress) {
        SkylinkCaptureFormat format = getSeekBarValueDimPresenterHandler(progress, captureFormats);
        if (format == null) {
            return null;
        }

        // Set textView to match
        int width = format.getWidth();
        int height = format.getHeight();

        mVideoCallView.onSetUiResTvDimViewHandler(width, height);

        return format;
    }

    @Override
    public void onDimStopTrackingTouchPresenterHandler(int progress) {
        SkylinkCaptureFormat format = setUiResTvOnSeekBarProgressDimPresenterHandler(progress);
        // Check that new CaptureFormat is valid.
        if (!Utils.isCaptureFormatValid(format)) {
            return;
        }

        int width = format.getWidth();
        int height = format.getHeight();
        int fpsNew = Utils.getFpsForNewCaptureFormat(fpsSel, format);

        // If any of the new Dim or Fps values are not valid,
        // or setting new resolution was not successful, reset UI to previous values.

        boolean result1 = mVideoCallView.onSetUiResTvDimViewHandler(width, height);
        boolean result2 = setUiResFpsPresenterHandler(fpsNew, format);
        boolean result3 = setInputVideoResolutionsPresenterHandler(format, fpsNew);

        if(fpsNew < 0 || !result1 || !result2 || result3){
            setUiResDimPresenterHandler(captureFormatSel.getWidth(), captureFormatSel.getHeight(),
                    captureFormats);
            setUiResFpsPresenterHandler(fpsSel, captureFormatSel);
            return;
        }
    }

    @Override
    public void onFpsStopTrackingTouchPresenterHandler(int progress){
        int fpsNew = setUiResTvOnSeekBarProgressFps(progress);

        // Check that new fps is valid for selected CaptureFormat.
        fpsNew = getFpsForNewCaptureFormatPresenterHandler(fpsNew, captureFormatSel);

        // If any of new Fps or selected CaptureFormat are not valid,
        // or setting new resolution was not successful, reset Fps UI to previous values.

        boolean result1 = setInputVideoResolutionsPresenterHandler(captureFormatSel, fpsNew);

        if (fpsNew < 0 || result1) {
            setUiResFpsPresenterHandler(fpsSel, captureFormatSel);
            return;
        }
    }

    private int getFpsForNewCaptureFormatPresenterHandler(int fps, SkylinkCaptureFormat format) {
        // Check if given CaptureFormat is valid.
        if (!isCaptureFormatValid(format)) {
            return -1;
        }

        int fpsMinNew = format.getFpsMin();
        int fpsMaxNew = format.getFpsMax();

        // Set new fps UI max value if the current one is out of the new range.
        if (fps < fpsMinNew || fps > fpsMaxNew) {
            return fpsMaxNew;
        }
        return fps;
    }

    /**
     * Set selected resolution frame rate on the UI.
     * Both the SeekBar and the TextView would be set, or reset if provided values were not valid.
     *
     * @param fps    The frame rate in fps (frames per second) to be set.
     * @param format A {@link SkylinkCaptureFormat} that provides the fps range to be set.
     * @return True if provided values were valid and false otherwise.
     */
    private boolean setUiResFpsPresenterHandler(int fps, SkylinkCaptureFormat format) {
        boolean valid;
        int index = -1;
        // Set the seekbar range.
        valid = setUiResSeekBarRangeFpsPresenterHandler(format);

        // Get the fps index in format.
        if (valid) {
            index = getSeekBarIndexFpsPresenterHandler(format, fps);
            if (index < 0) {
                valid = false;
            }
        }

        if (valid) {

            mVideoCallView.onSetSeekBarResFpsViewHandler(index, fps);

        } else {

            mVideoCallView.onSetSeekBarResFpsViewHandler(0, -1);
        }
        return valid;
    }

    /**
     * Return the index of the given fps from within the range of current supported fps.
     *
     * @param captureFormat
     * @param fps
     * @return Negative number if index could not be found, or if captureFormat is invalid.
     */
    private int getSeekBarIndexFpsPresenterHandler(SkylinkCaptureFormat captureFormat, int fps) {
        if (!isCaptureFormatValid(captureFormat)) {
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
     * Set selected resolution dimensions (i.e. width and height) on the UI based on the
     * given range of {@link SkylinkCaptureFormat} array.
     * Both the SeekBar and the TextView would be set, or reset if provided values were not valid.
     * @param width          The width to be set.
     * @param height         The height to be set.
     * @param captureFormats The {@link SkylinkCaptureFormat} array that provides range of seekbar.
     * @return True if provided values were valid and false otherwise.
     */
    private boolean setUiResDimPresenterHandler(int width, int height, SkylinkCaptureFormat[] captureFormats) {
        boolean valid;
        int index = -1;
        // Set the seekbar range.
        valid = setUiResSeekBarRangeDimPresenterHandler(captureFormats);
        // Get the captureFormat that matches for width and height.
        if (valid) {
            index = getSeekBarIndexDimPresenterHandler(captureFormats, width, height);
            if (index < 0) {
                valid = false;
            }
        }

        if (valid) {

            mVideoCallView.onSetSeekBarResDimViewHandler(index, width, height);

        } else {

            mVideoCallView.onSetSeekBarResDimViewHandler(0, -1, -1);
        }
        return valid;
    }

    /**
     * Return the index of the given SkylinkCaptureFormat from within the list of
     * current supported SkylinkCaptureFormat[].
     *
     * @param captureFormats
     * @param width
     * @param height
     * @return Negative number if index could not be found, or if captureFormats is invalid.
     */
    private int getSeekBarIndexDimPresenterHandler(SkylinkCaptureFormat[] captureFormats, int width, int height) {
        if (!isCaptureFormatsValid(captureFormats)) {
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
     * Set the ranges of values for seekBarResDim.
     * Set to zero if range of values invalid.
     *
     * @param captureFormats SkylinkCaptureFormat array for setting range of seekBarResDim.
     * @return True if captureFormats was valid and false otherwise.
     */
    private boolean setUiResSeekBarRangeDimPresenterHandler(SkylinkCaptureFormat[] captureFormats) {
        // If there is no valid captureFormats set, set seekBar range to zero.

        int seekBarResDimMax = 0;
        boolean isValid = false;

        if (isCaptureFormatsValid(captureFormats)) {
            seekBarResDimMax = captureFormats.length - 1;
            isValid = true;
        }

        // Set dimension range based on size of current CaptureFormat list.
        mVideoCallView.onSetUiResSeekBarRangeDimViewHandler(seekBarResDimMax);

        return isValid;
    }

    /**
     * Set the ranges of values for seekBarResFps.
     * Set to zero if range of values invalid.
     *
     * @return True if captureFormat was valid and false otherwise.
     */
    private boolean setUiResSeekBarRangeFpsPresenterHandler(SkylinkCaptureFormat captureFormat) {
        // If there is no valid captureFormats set, set seekBar range to zero.

        int seekBarResFpsMax = 0;
        boolean isValid = false;

        if (isCaptureFormatValid(captureFormat)) {
            seekBarResFpsMax = captureFormat.getFpsMax() - captureFormat.getFpsMin();
            isValid = true;
        }

        // Set dimension range based on size of current CaptureFormat list.
        mVideoCallView.onSetUiResSeekBarRangeFpsViewHandler(seekBarResFpsMax);

        return isValid;
    }

    private boolean setInputVideoResolutionsPresenterHandler(SkylinkCaptureFormat format, int fpsNew) {
        int width = format.getWidth();
        int height = format.getHeight();
        if (width < 0 || height < 0 || fpsNew < 0) {
            return false;
        }

        // Set new selected CaptureFormat and frame rate.
        captureFormatSel = format;
        fpsSel = fpsNew;

        // If already at new resolution, no need to call Skylink API.
        if (videoInput.getWidth() == width && videoInput.getHeight() == height && videoInput.getFps() == fpsNew) {
            return true;
        }

        mVideoCallService.setInputVideoResolutionServiceHandler(width, height, fpsNew);

        return true;
    }

    /**
     * Checks if given {@link SkylinkCaptureFormat SkylinkCaptureFormat[]} is valid for using.
     * To be valid, it cannot be null or empty.
     *
     * @param captureFormats
     * @return
     */
    private boolean isCaptureFormatsValid(SkylinkCaptureFormat[] captureFormats) {
        if (captureFormats == null || captureFormats.length == 0) {
            return false;
        }
        return true;
    }

    /**
     * Checks if given {@link SkylinkCaptureFormat} is valid for using.
     * To be valid it cannot be null, and the fps range cannot be negative.
     *
     * @param format
     * @return True if valid and false if not.
     */
    private boolean isCaptureFormatValid(SkylinkCaptureFormat format) {
        if (format == null) {
            return false;
        }

        // Check fps range based on min and max fps of this CaptureFormat.
        int range = format.getFpsMax() - format.getFpsMin();
        if (range < 0 || format.getFpsMin() < 0) {
            return false;
        }
        return true;
    }

    private int setUiResTvOnSeekBarProgressFps(int progress) {
        int fps = getSeekBarValueFpsPresenterHandler(progress, captureFormatSel);

        mVideoCallView.onSetUiResTvFpsViewHandler(fps);

        return fps;
    }

    private int getSeekBarValueFpsPresenterHandler(int progress, SkylinkCaptureFormat format) {
        if (!isCaptureFormatValid(format)) {
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

}
