package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;
import sg.com.temasys.skylink.sdk.sampleapp.service.VideoService;
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
    // not necessarily the currently used SkylinkCaptureFormat.
    private static SkylinkCaptureFormat mCurrentCaptureFormat = null;

    // The last selected frame rate (fps) on UI,
    // not necessarily the currently used frame rate.
    private static int mCurrentFps = -1;


    public VideoCallPresenter(VideoCallContract.View videoCallView, Context context) {
        this.mContext = context;

        this.mVideoCallView = videoCallView;

        mVideoCallService = new VideoService(context);

        mPermissionUtils = new PermissionUtils();

        initialize();

    }

    private void initialize() {
        this.mVideoCallView.setPresenter(this);
        this.mVideoCallService.setPresenter(this);

        this.mVideoCallService.setTypeCall();

        mVideoInput = new VideoResolution();
        mVideoSent = new VideoResolution();
        mVideoReceive = new VideoResolution();
    }

    //----------------------------------------------------------------------------------------------
    // listener implement methods from View and Service
    //----------------------------------------------------------------------------------------------

    @Override
    public void onViewLayoutRequested() {
        Log.d(TAG, "onViewLayoutRequested");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mVideoCallService.isConnectingOrConnected()) {

            //reset permission request states.
            mPermissionUtils.permQReset();

            //connect to room on Skylink connection
            connectToRoom();

            //after connected to skylink SDK, UI will be updated latter on AudioService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //if it already connected to room, then resume permission
            mPermissionUtils.permQResume(mContext, mVideoCallView.onGetFragment());

            //update UI into connected
            updateConnectedUI();

            Log.d(TAG, "Try to update UI when changing configuration");
        }

    }

    @Override
    public void onConnect(boolean isSuccessful) {
        if (isSuccessful) {

            mVideoCallView.onConnectedUIChange();

        } else {
            onDisconnectUIChange();
        }
    }

    @Override
    public void onDisconnect() {
        onDisconnectUIChange();
    }

    @Override
    public void onDisconnectFromRoom() {
        mVideoCallService.disconnectFromRoom();
    }

    @Override
    public void onViewExit() {
        //process disconnect from room
        mVideoCallService.disconnectFromRoom();

        //after disconnected from skylink SDK, UI will be updated latter on AudioService.onDisconnect
    }

    @Override
    public void onRemotePeerJoin(SkylinkPeer skylinkPeer) {
        //do nothing
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId) {
        mVideoCallView.onRemoveRemotePeer();
    }

    @Override
    public void onRemotePeerConnectionRefreshed(String log, UserInfo remotePeerUserInfo) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onRemotePeerMediaReceive(String log, UserInfo remotePeerUserInfo) {
        addRemoteView();

        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        Log.d(TAG, log);
    }

    @Override
    public void onPermissionRequired(PermRequesterInfo info) {
        mPermissionUtils.onPermissionRequiredHandler(info, TAG, mContext, mVideoCallView.onGetFragment());
    }

    @Override
    public void onPermissionGranted(PermRequesterInfo info) {
        mPermissionUtils.onPermissionGrantedHandler(info, TAG);
    }

    @Override
    public void onPermissionDenied(PermRequesterInfo info) {
        mPermissionUtils.onPermissionDeniedHandler(info, mContext, TAG);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag) {
        mPermissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, tag);
    }

    @Override
    public void onLocalMediaCapture(SurfaceViewRenderer videoView) {
        String log = "[SA][onLocalMediaCapture] ";
        if (videoView == null) {
            log += "VideoView is null!";
            Log.d(TAG, log);

            SurfaceViewRenderer selfVideoView = mVideoCallService.getVideoView(null);
            addSelfView(selfVideoView);
        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
            addSelfView(videoView);
        }
    }

    @Override
    public void onInputVideoResolutionObtained(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        noteInputVideoResolutions(width, height, fps, captureFormat);

        String log = "[SA][VideoResInput] The current video input has width x height, fps: " +
                width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }

    @Override
    public void onReceivedVideoResolutionObtained(String peerId, int width, int height, int fps) {
        setUiResTvStatsReceived(width, height, fps);

        String log = "[SA][VideoResRecv] The current video received from Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }

    @Override
    public void onSentVideoResolutionObtained(String peerId, int width, int height, int fps) {
        setUiResTvStatsSent(width, height, fps);

        String log = "[SA][VideoResSent] The current video sent to Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }

    @Override
    public void onVideoSizeChange(String peerId, Point size) {
        String peer = "Peer " + peerId;
        // If peerId is null, this call is for our local video.
        if (peerId == null) {
            peer = "We've";
        }
        Log.d(TAG, peer + " got video size changed to: " + size.toString() + ".");
    }

    @Override
    public void onProcessBtnAudioMute() {
        boolean isAudioMute = mVideoCallService.isAudioMute();

        processBtnAudioMute(!isAudioMute);
    }

    @Override
    public void onProcessBtnVideoMute() {
        boolean isVideoMute = mVideoCallService.isVideoMute();

        processBtnVideoMute(!isVideoMute);
    }

    @Override
    public void onProcessBtnCameraToggle() {
        boolean isCamToggle = mVideoCallService.isCameraToggle();

        processBtnCameraToggle(!isCamToggle);
    }

    @Override
    public void onViewResume() {

        // Toggle camera back to previous state if required.
        if (mVideoCallService.isCameraToggle()) {

            if (mVideoCallService.getVideoView(null) != null) {

                mVideoCallService.toggleCamera();

                mVideoCallService.setCamToggle(false);
            }
        }
    }

    @Override
    public void onViewPause() {
        boolean toggleCamera = mVideoCallService.toggleCamera(false);

        mVideoCallService.setCamToggle(toggleCamera);
    }

    @Override
    public void onSwitchCamera() {
        mVideoCallService.switchCamera();
    }

    @Override
    public void onDimProgressChanged(int progress) {
        setUiResTvOnSeekBarProgressDim(progress);
    }

    @Override
    public void onFpsProgressChanged(int progress) {
        setUiResTvOnSeekBarProgressFps(progress);
    }

    @Override
    public void onDimStopTrackingTouch(int progress) {
        SkylinkCaptureFormat format = setUiResTvOnSeekBarProgressDim(progress);
        // Check that new CaptureFormat is valid.
        if (!Utils.isCaptureFormatValid(format)) {
            return;
        }

        int width = format.getWidth();
        int height = format.getHeight();
        int fpsNew = Utils.getFpsForNewCaptureFormat(mCurrentFps, format);

        // If any of the new Dim or Fps values are not valid,
        // or setting new resolution was not successful, reset UI to previous values.

        boolean result1 = mVideoCallView.onSetUiResTvDim(width, height);
        boolean result2 = setUiResFps(fpsNew, format);
        boolean result3 = setInputVideoResolutions(format, fpsNew);

        if (fpsNew < 0 || !result1 || !result2 || result3) {
            setUiResDim(mCurrentCaptureFormat.getWidth(), mCurrentCaptureFormat.getHeight(),
                    mCaptureFormats);
            setUiResFps(mCurrentFps, mCurrentCaptureFormat);
            return;
        }
    }

    @Override
    public void onFpsStopTrackingTouch(int progress) {
        int fpsNew = setUiResTvOnSeekBarProgressFps(progress);

        // Check that new fps is valid for selected CaptureFormat.
        fpsNew = Utils.getFpsForNewCaptureFormat(fpsNew, mCurrentCaptureFormat);

        // If any of new Fps or selected CaptureFormat are not valid,
        // or setting new resolution was not successful, reset Fps UI to previous values.

        boolean result1 = setInputVideoResolutions(mCurrentCaptureFormat, fpsNew);

        if (fpsNew < 0 || result1) {
            setUiResFps(mCurrentFps, mCurrentCaptureFormat);
            return;
        }
    }

    @Override
    public String onGetRoomPeerIdNick() {
        return mVideoCallService.getRoomPeerIdNick();
    }

    @Override
    public void onGetVideoResolutions() {

        String peerId = mVideoCallService.getPeerId(1);

        mVideoCallService.getVideoResolutions(peerId);
    }

    //----------------------------------------------------------------------------------------------
    // private methods to process logic
    //----------------------------------------------------------------------------------------------



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

    private void noteInputVideoResolutions(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {

        mVideoInput.setWidth(width);
        mVideoInput.setHeight(height);
        mVideoInput.setFps(fps);

        mVideoCallView.onSetUiResTvStatsInput(mVideoInput);

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
            setUiResDim(width, height, mCaptureFormats);

            setUiResFps(fpsNew, captureFormat);

        }

        String log = "The current local video by VideoDevice " + mCurrentVideoDevice +
                ", with camera name \"" + mCurrentCameraName +
                "\", has width, height, fps: " + width + ", " + height + ", " + fps +
                ".\r\n" + captureFormatString;
        Log.d(TAG, log);

    }

    private void setUiResTvStatsReceived(int width, int height, int fps) {
        mVideoReceive.setWidth(width);
        mVideoReceive.setHeight(height);
        mVideoReceive.setFps(fps);

        mVideoCallView.onSetUiResTvStatsReceive(mVideoReceive);
    }

    private void setUiResTvStatsSent(int width, int height, int fps) {
        mVideoSent.setWidth(width);
        mVideoSent.setHeight(height);
        mVideoSent.setFps(fps);

        mVideoCallView.onSetUiResTvStatsSent(mVideoSent);
    }

    private SurfaceViewRenderer getRemoteView() {
        SurfaceViewRenderer videoView;
        String remotePeerId = mVideoCallService.getPeerId(1);
        // Proceed only if the first (& only) remote Peer has joined.
        if (remotePeerId == null) {
            return null;
        } else {
            videoView = getVideoView(remotePeerId);
        }

        return videoView;
    }

    private void onDisconnectUIChange() {
        mVideoInput = null;
        mVideoSent = null;
        mVideoReceive = null;

        mVideoCallView.onDisconnectUIChange();
    }

    private void addSelfView(SurfaceViewRenderer videoView) {
        mVideoCallView.onAddSelfView(videoView);
    }

    private void addRemoteView() {

        SurfaceViewRenderer videoView = getRemoteView();

        mVideoCallView.onAddRemoteView(videoView);

    }

    private SurfaceViewRenderer getVideoView(String remotePeerId) {
        return mVideoCallService.getVideoView(remotePeerId);
    }

    private SkylinkCaptureFormat getSeekBarValueDim(int progress, SkylinkCaptureFormat[] captureFormats) {
        if (!Utils.isCaptureFormatsValid(captureFormats) || progress >= captureFormats.length) {
            return null;
        }
        SkylinkCaptureFormat format = captureFormats[progress];
        return format;
    }

    private void connectToRoom() {

        //connect to SDK
        mVideoCallService.connectToRoom();

        //update UI and make toast
        mVideoCallView.onConnectingUIChange();

        //get roomName from setting
        String log = "Entering video room \"" + Config.ROOM_NAME_VIDEO + "\".";
        toastLog(TAG, mContext, log);
    }

    private void updateConnectedUI() {

        if (mVideoCallService.isAudioMute()) {
            processBtnAudioMute(true);
        }

        if (mVideoCallService.isVideoMute()) {
            processBtnVideoMute(true);
        }

        // Toggle camera back to previous state if required.
        if (mVideoCallService.isCameraToggle() && getVideoView(null) != null) {
            processBtnCameraToggle(true);
        }

        //update UI
        mVideoCallView.onConnectedUIChange();

        mVideoCallView.onAddSelfView(getVideoView(null));

        SurfaceViewRenderer remoteView = getRemoteView();

        if (remoteView != null) {
            mVideoCallView.onAddRemoteView(remoteView);
        }

    }

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processBtnAudioMute(boolean isAudioMuted) {

        //save audioMuted for other usage
        mVideoCallService.setAudioMute(isAudioMuted);

        //set mute audio to sdk
        mVideoCallService.muteLocalAudio(isAudioMuted);

        // Set UI and Toast.
        mVideoCallView.onSetAudioBtnLabel(isAudioMuted, true);
    }

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processBtnVideoMute(boolean isVideoMuted) {

        //save audioMuted for other usage
        mVideoCallService.setVideoMute(isVideoMuted);

        //set mute audio to sdk
        mVideoCallService.muteLocalVideo(isVideoMuted);

        // Set UI and Toast.
        mVideoCallView.onSetVideoBtnLabel(isVideoMuted, true);
    }

    // If video is enable, toggle video and if video is toggle, then enable it
    private void processBtnCameraToggle(boolean isCameraToggle) {

        //display instruction log
        String log12 = "Toggled camera ";
        if (getVideoView(null) != null) {
            if (mVideoCallService.toggleCamera()) {
                log12 += "to restarted!";

                //change state of camera toggle
                mVideoCallService.setCamToggle(false);
            } else {
                log12 += "to stopped!";

                mVideoCallService.setCamToggle(true);
            }
        } else {
            log12 += "but failed as local video is not available!";
        }
        toastLog(TAG, mContext, log12);

        //this button don't need to change text
    }

    /**
     * Once new video resolution dimensions are selected on the Seekbar,
     * change the selected video resolution dimensions TextView to match.
     *
     * @param progress
     * @return {@link SkylinkCaptureFormat} selected.
     */
    private SkylinkCaptureFormat setUiResTvOnSeekBarProgressDim(int progress) {
        SkylinkCaptureFormat format = getSeekBarValueDim(progress, mCaptureFormats);
        if (format == null) {
            return null;
        }

        // Set textView to match
        int width = format.getWidth();
        int height = format.getHeight();

        mVideoCallView.onSetUiResTvDim(width, height);

        return format;
    }

    /**
     * Set selected resolution frame rate on the UI.
     * Both the SeekBar and the TextView would be set, or reset if provided values were not valid.
     *
     * @param fps    The frame rate in fps (frames per second) to be set.
     * @param format A {@link SkylinkCaptureFormat} that provides the fps range to be set.
     * @return True if provided values were valid and false otherwise.
     */
    private boolean setUiResFps(int fps, SkylinkCaptureFormat format) {
        boolean valid;
        int index = -1;
        // Set the seekbar range.
        valid = setUiResSeekBarRangeFps(format);

        // Get the fps index in format.
        if (valid) {
            index = getSeekBarIndexFps(format, fps);
            if (index < 0) {
                valid = false;
            }
        }

        if (valid) {

            mVideoCallView.onSetSeekBarResFps(index, fps);

        } else {

            mVideoCallView.onSetSeekBarResFps(0, -1);
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
    private int getSeekBarIndexFps(SkylinkCaptureFormat captureFormat, int fps) {
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
     * Set selected resolution dimensions (i.e. width and height) on the UI based on the
     * given range of {@link SkylinkCaptureFormat} array.
     * Both the SeekBar and the TextView would be set, or reset if provided values were not valid.
     *
     * @param width          The width to be set.
     * @param height         The height to be set.
     * @param captureFormats The {@link SkylinkCaptureFormat} array that provides range of seekbar.
     * @return True if provided values were valid and false otherwise.
     */
    private boolean setUiResDim(int width, int height, SkylinkCaptureFormat[] captureFormats) {
        boolean valid;
        int index = -1;
        // Set the seekbar range.
        valid = setUiResSeekBarRangeDim(captureFormats);
        // Get the captureFormat that matches for width and height.
        if (valid) {
            index = getSeekBarIndexDim(captureFormats, width, height);
            if (index < 0) {
                valid = false;
            }
        }

        if (valid) {

            mVideoCallView.onSetSeekBarResDim(index, width, height);

        } else {

            mVideoCallView.onSetSeekBarResDim(0, -1, -1);
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
     * @return Negative number if index could not be found, or if mCaptureFormats is invalid.
     */
    private int getSeekBarIndexDim(SkylinkCaptureFormat[] captureFormats, int width, int height) {
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
     * @return True if mCaptureFormats was valid and false otherwise.
     */
    private boolean setUiResSeekBarRangeDim(SkylinkCaptureFormat[] captureFormats) {
        // If there is no valid mCaptureFormats set, set seekBar range to zero.

        int seekBarResDimMax = 0;
        boolean isValid = false;

        if (isCaptureFormatsValid(captureFormats)) {
            seekBarResDimMax = captureFormats.length - 1;
            isValid = true;
        }

        // Set dimension range based on size of current CaptureFormat list.
        mVideoCallView.onSetUiResSeekBarRangeDim(seekBarResDimMax);

        return isValid;
    }

    /**
     * Set the ranges of values for seekBarResFps.
     * Set to zero if range of values invalid.
     *
     * @return True if captureFormat was valid and false otherwise.
     */
    private boolean setUiResSeekBarRangeFps(SkylinkCaptureFormat captureFormat) {
        // If there is no valid mCaptureFormats set, set seekBar range to zero.

        int maxSeekBarFpsRange = 0;
        boolean isValid = false;

        if (Utils.isCaptureFormatValid(captureFormat)) {
            maxSeekBarFpsRange = captureFormat.getFpsMax() - captureFormat.getFpsMin();
            isValid = true;
        }

        // Set dimension range based on size of current CaptureFormat list.
        mVideoCallView.onSetUiResSeekBarRangeFps(maxSeekBarFpsRange);

        return isValid;
    }

    private boolean setInputVideoResolutions(SkylinkCaptureFormat format, int fpsNew) {
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
    private boolean isCaptureFormatsValid(SkylinkCaptureFormat[] captureFormats) {
        if (captureFormats == null || captureFormats.length == 0) {
            return false;
        }
        return true;
    }

    private int setUiResTvOnSeekBarProgressFps(int progress) {
        int fps = getSeekBarValueFps(progress, mCurrentCaptureFormat);

        mVideoCallView.onSetUiResTvFps(fps);

        return fps;
    }

    private int getSeekBarValueFps(int progress, SkylinkCaptureFormat format) {
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

}
