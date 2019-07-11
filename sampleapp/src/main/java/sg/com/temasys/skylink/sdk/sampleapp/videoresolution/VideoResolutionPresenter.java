package sg.com.temasys.skylink.sdk.sampleapp.videoresolution;

import android.content.Context;
import android.util.Log;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.SkylinkCommonService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

/**
 * Created by muoi.pham on 27/02/19.
 * This class is responsible for implementing logic of video resolutions
 */

public class VideoResolutionPresenter extends BasePresenter implements VideoResolutionContract.Presenter {

    private final String TAG = VideoResolutionPresenter.class.getName();

    private Context context;

    // The video view instance
    public VideoResolutionContract.View videoResolutionView;

    // Service instance
    private SkylinkCommonService videoService;

    // the current video resolution
    private VideoResolution currentVideoRes = new VideoResolution();

    // The current camera name.
    private String currentCameraName = null;

    // The selected SkylinkCaptureFormat on UI,
    private SkylinkCaptureFormat currentCaptureFormat = null;

    // The selected frame rate (fps) on UI,
    private int currentFps = -1;

    // Camera is ready only if the current capture format is not null.
    private boolean isCameraReady = false;

    // The array of SkylinkCaptureFormats support by the current camera.
    private SkylinkCaptureFormat[] captureFormats;
    private SkylinkMedia.MediaType currentMainVideoTypeSelected = SkylinkMedia.MediaType.VIDEO_CAMERA;

    public VideoResolutionPresenter(Context context) {
        this.context = context;
    }

    public void setView(VideoResolutionContract.View view) {
        videoResolutionView = view;
        videoResolutionView.setPresenter(this);
    }

    public void setService(SkylinkCommonService videoService) {
        this.videoService = videoService;
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for view to call
    // These methods are responsible for processing requests from view
    //----------------------------------------------------------------------------------------------

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
        int currentFps = this.currentFps;
        int fpsNew = Utils.getFpsForNewCaptureFormat(currentFps, format);

        // If any of the new Dim or Fps values are not valid,
        // or setting new resolution was not successful, reset UI to previous values.
        boolean result1 = videoResolutionView.onPresenterRequestUpdateUiResDimInfo(width, height);
        boolean result2 = processUpdateUiResFps(fpsNew, format);
        boolean result3 = processUpdateInputVideoResolutions(format, fpsNew);

        if (fpsNew < 0 || !result1 || !result2 || !result3) {
            SkylinkCaptureFormat currentFormat = this.currentCaptureFormat;
            processUpdateUiResDim(currentFormat.getWidth(), currentFormat.getHeight(), captureFormats);
            processUpdateUiResFps(currentFps, currentFormat);
            return;
        }
    }

    @Override
    public void onViewRequestFpsSelected(int progress) {
        int fpsNew = processUpdateUiResOnProgressFps(progress);

        SkylinkCaptureFormat currentFormat = this.currentCaptureFormat;
        int currentFps = this.currentFps;

        // Check that new fps is valid for selected CaptureFormat.
        fpsNew = Utils.getFpsForNewCaptureFormat(fpsNew, currentFormat);

        // If any of new Fps or selected CaptureFormat are not valid,
        // or setting new resolution was not successful, reset Fps UI to previous values.
        boolean result1 = processUpdateInputVideoResolutions(currentFormat, fpsNew);

        if (fpsNew < 0 || !result1) {
            processUpdateUiResFps(currentFps, currentFormat);
            return;
        }
    }

    @Override
    public void onViewRequestGetVideoResolutions() {
        // get the remote peer id
        String peerId = videoService.getPeerId(1);
        videoService.getVideoResolutions(this.currentMainVideoTypeSelected, peerId);
    }

    @Override
    public void onViewRequestChooseVideoCamera() {
        this.currentMainVideoTypeSelected = SkylinkMedia.MediaType.VIDEO_CAMERA;
    }

    @Override
    public void onViewRequestChooseVideoScreen() {
        this.currentMainVideoTypeSelected = SkylinkMedia.MediaType.VIDEO_SCREEN;
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void onServiceRequestInputVideoResolutionObtained(SkylinkMedia.MediaType mediaType, int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        processInputVideoResolutions(width, height, fps, captureFormat);

        String log = "[SA][VideoResInput] The current video input has width x height, fps: " +
                width + " x " + height + ", " + fps + " fps.\r\n" + " CaptureFormat:";
        if (captureFormat != null) {
            log += captureFormat.toStringCompact() + ".";
        } else {
            log += " NULL.";
        }
        ;
//        toastLog(TAG, context, log);
    }

    @Override
    public void onServiceRequestReceivedVideoResolutionObtained(String peerId, SkylinkMedia.MediaType mediaType, int width, int height, int fps) {
        processReceivedVideoResolutions(width, height, fps);

        String log = "[SA][VideoResRecv] The video (" + mediaType.toString() + ") received from Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }

    @Override
    public void onServiceRequestSentVideoResolutionObtained(String peerId, SkylinkMedia.MediaType mediaType, int width, int height, int fps) {
        processSentVideoResolutions(width, height, fps);

        String log = "[SA][VideoResSent] The video (" + mediaType + ") sent to Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }


    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

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

        this.currentCaptureFormat = format;
        videoResolutionView.onPresenterRequestUpdateUiResDimInfo(width, height);

        return format;
    }

    /**
     * Get the selected value of width x height seek bar
     */
    private SkylinkCaptureFormat processGetSelectedValueDim(
            int progress, SkylinkCaptureFormat[] captureFormats) {
        if (!Utils.isCaptureFormatsValid(captureFormats) || progress >= captureFormats.length) {
            return null;
        }
        SkylinkCaptureFormat format = captureFormats[progress];
        return format;
    }

    /**
     * Update the UI when video resolution frame rate is changed
     */
    private int processUpdateUiResOnProgressFps(int progress) {
        SkylinkCaptureFormat currentFormat = this.currentCaptureFormat;
        int fps = processGetSelectedValueFps(progress, currentFormat);

        this.currentFps = fps;
        videoResolutionView.onPresenterRequestUpdateUiResFpsInfo(fps);

        return fps;
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
            videoResolutionView.onPresenterRequestUpdateResFpsInfo(index, fps);
        } else {
            videoResolutionView.onPresenterRequestUpdateResFpsInfo(0, -1);
        }
        return valid;
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
        this.currentCaptureFormat = format;
        this.currentFps = fpsNew;

        // no need to update video resolution if nothing changed
        if (currentVideoRes.getWidth() == width && currentVideoRes.getHeight() == height &&
                currentVideoRes.getFps() == fpsNew) {
            return false;
        }

        videoService.setInputVideoResolution(currentMainVideoTypeSelected, width, height, fpsNew);

        // save new res as current video resolution
        currentVideoRes.setWidth(width);
        currentVideoRes.setHeight(height);
        currentVideoRes.setFps(fpsNew);

        return true;
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

        // just for hot fix min-max capture format returned value
        if (captureFormat != null && captureFormat.getFpsMin() != 0) {
            captureFormat.setFpsMin(0);
        }

        if (Utils.isCaptureFormatValid(captureFormat)) {
            maxFpsRange = captureFormat.getFpsMax() - captureFormat.getFpsMin();
            isValid = true;
        }

        if (isValid) {
            // Set dimension range based on size of current CaptureFormat list.
            videoResolutionView.onPresenterRequestUpdateUiResRangeFpsInfo(maxFpsRange,
                    captureFormat.getFpsMin(), captureFormat.getFpsMax());
        }

        return isValid;
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
     * Set selected resolution dimensions (i.e. width and height) on the UI based on the
     * given range of {@link SkylinkCaptureFormat} array.
     * Both the SeekBar and the TextView would be set, or reset if provided values were not valid.
     *
     * @param width          The width to be set.
     * @param height         The height to be set.
     * @param captureFormats The {@link SkylinkCaptureFormat} array that provides range of seekbar.
     * @return True if provided values were valid and false otherwise.
     */
    private boolean processUpdateUiResDim(int width, int height, SkylinkCaptureFormat[]
            captureFormats) {
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
            videoResolutionView.onPresenterRequestUpdateResDimInfo(index, width, height);
        } else {
            videoResolutionView.onPresenterRequestUpdateResDimInfo(0, -1, -1);
        }
        return valid;
    }

    /**
     * Set the ranges of values for seekBarResDim. The value ranges is number of
     * supported SkylinkCaptureFormats in current camera.
     * Set to zero if range of values invalid.
     *
     * @param captureFormats SkylinkCaptureFormat array for setting range of seekBarResDim.
     * @return True if mCaptureFormats was valid and false otherwise.
     */
    private boolean processUpdateUiResRangeDim(SkylinkCaptureFormat[] captureFormats) {
        // If there is no valid mCaptureFormats set, set seekBar range to zero.

        int maxRangeIndex = 0;
        boolean isValid = false;

        if (processCheckCaptureFormatsValid(captureFormats)) {
            maxRangeIndex = captureFormats.length - 1;
            isValid = true;
        }

        if (isValid) {
            String minRangeValue = captureFormats[0].getWidth() + " x "
                    + captureFormats[0].getHeight();

            String maxRangeValue = captureFormats[maxRangeIndex].getWidth() + " x "
                    + captureFormats[maxRangeIndex].getHeight();

            // Set dimension range based on size of current CaptureFormat list.
            videoResolutionView.onPresenterRequestUpdateUiResRangeDimInfo(maxRangeIndex, minRangeValue, maxRangeValue);
        }

        return isValid;
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
    private int processGetSelectedIndexDim(SkylinkCaptureFormat[] captureFormats, int width,
                                           int height) {
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
    private void processInputVideoResolutions(int width, int height, int fps,
                                              SkylinkCaptureFormat captureFormat) {
        // Update UI about input resolution of local video
        VideoResolution videoInputRes = new VideoResolution(width, height, fps);
        videoResolutionView.onPresenterRequestUpdateUiResInput(videoInputRes);

        // Update UI for new captureFormats if there was a camera change:
        // Closed <-> Opened
        // One camera -> Another camera
        // Camera not ready -> Camera ready
        boolean cameraChanged = false;
        if (captureFormat == null) {
            isCameraReady = false;
        }

        String previousCameraName = this.currentCameraName;
        String currentCamera = videoService.getCurrentCameraName();

        // Check if a new camera in now active.
        if (currentCamera != null) {
            // Check if camera just started from no/different camera.
            if (!currentCamera.equals(previousCameraName)/* || !isCameraReady*/) {
                cameraChanged = true;
            }
            // Check if camera was already started, but only just became ready.
            else if (captureFormat != null && !isCameraReady) {
                isCameraReady = true;
                cameraChanged = true;
            }
        }
        // Check if camera had just closed.
        else if (previousCameraName != null) {
            cameraChanged = true;
        }

        String captureFormatString = "Current capture formats have not changed.";
        // Update the UI for setting new video resolution if a new camera has been obtained.
        if (cameraChanged) {
            // When a new camera is first obtained, actual fps is often unavailable or zero.
            // Hence, set to current fps.
            int selectedFps = this.currentFps;
            if (selectedFps <= 0) {
                // If current fps is invalid or zero.
                // Else set to max of new captureFormat if available.
                if (captureFormat != null) {
                    selectedFps = captureFormat.getFpsMax();
                } else {
                    // Or -1 (no valid format).
                    selectedFps = -1;
                }
            }
            captureFormatString = processUpdateUiResOnNewCamera(
                    width, height, selectedFps, captureFormat, currentCamera);
        }

        SkylinkConfig.VideoDevice currentVideoDevice = videoService.getCurrentVideoDevice();
        String log = "The current local video by VideoDevice " + currentVideoDevice +
                ", with camera name \"" + currentCamera +
                "\", has width, height, fps: " + width + ", " + height + ", " + fps +
                ".\r\n" + captureFormatString;
        Log.d(TAG, log);
    }

    /**
     * Update the UI for video resolution when the camera changes.
     * Use -1 for width, height and fps if none is available.
     *
     * @param width         The new width to set in video resolution UI.
     * @param height        The new height to set in video resolution UI.
     * @param fps           The new fps to set in video resolution UI.
     * @param captureFormat
     * @return
     */
    private String processUpdateUiResOnNewCamera(int width, int height, int fps, SkylinkCaptureFormat
            captureFormat, String newCamera) {
        String captureFormatString;
        // Set the range of supported SkylinkCaptureFormats.
        // Record current range of supported SkylinkCaptureFormats.
        captureFormats = videoService.getCaptureFormats(null);

        //get captureFormats String info
        captureFormatString = videoService.getCaptureFormatsString(captureFormats);
        // Try to continue to with last selected Fps if possible.
        int fpsNew = Utils.getFpsForNewCaptureFormat(fps, captureFormat);

        if (captureFormat != null) {
            // Set new selected CaptureFormat and frame rate.
            this.currentCaptureFormat = captureFormat;
            this.currentFps = fpsNew;

            this.currentCameraName = newCamera;
        }

        // Set UI values.
        processUpdateUiResDim(width, height, captureFormats);

        processUpdateUiResFps(fpsNew, captureFormat);
        return captureFormatString;
    }

    /**
     * Set the sent video resolution from local peer to remote peer
     * and Update UI accordingly
     */
    private void processSentVideoResolutions(int width, int height, int fps) {
        // Update UI about sent resolution to remote peer
        VideoResolution videoSentRes = new VideoResolution(width, height, fps);
        videoResolutionView.onPresenterRequestUpdateUiResSent(videoSentRes);
    }

    /**
     * Set the received video resolution from remote peer
     * and Update UI accordingly
     */
    private void processReceivedVideoResolutions(int width, int height, int fps) {
        // Update UI about received resolution from remote peer
        VideoResolution videoReceivedRes = new VideoResolution(width, height, fps);

        videoResolutionView.onPresenterRequestUpdateUiResReceive(videoReceivedRes);
    }
}
