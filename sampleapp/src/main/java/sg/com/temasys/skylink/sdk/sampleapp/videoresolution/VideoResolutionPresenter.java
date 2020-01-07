package sg.com.temasys.skylink.sdk.sampleapp.videoresolution;

import android.content.Context;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.SkylinkCommonService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;

/**
 * Created by muoi.pham on 27/02/19.
 * This class is responsible for implementing logic of video resolutions
 */
public class VideoResolutionPresenter extends BasePresenter implements VideoResolutionContract.Presenter {

    private Context context;

    // The video view instance
    public VideoResolutionContract.View videoResolutionView;

    // Service instance
    private SkylinkCommonService videoService;

    // the current video resolutions
    private VideoResolution currentVideoResCam = new VideoResolution();

    private VideoResolution currentVideoResScreen = new VideoResolution();

    private SkylinkMedia.MediaType currentMainVideoTypeSelected = null;

    // flags for first time informed the input video resolution obtained to update the range, min, max value for UI
    private boolean hasInformedWHFirstValueScreen = false;
    private boolean hasInformedFpsFirstValueScreen = false;
    private boolean hasInformedWHFirstValueCam = false;
    private boolean hasInformedFpsFirstValueCam = false;

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
    public void processGetVideoResolutions() {
        videoService.getVideoResolutions(this.currentMainVideoTypeSelected, 1);
    }

    @Override
    public void processChooseVideoCamera() {
        this.currentMainVideoTypeSelected = SkylinkMedia.MediaType.VIDEO_CAMERA;

        processGetVideoResolutions();
    }

    @Override
    public void processChooseVideoScreen() {
        this.currentMainVideoTypeSelected = SkylinkMedia.MediaType.VIDEO_SCREEN;

        processGetVideoResolutions();
    }

    // camera video resolution processing

    @Override
    public void processWHProgressChangedCamera(int progress) {
        if (progress < 0 || progress > currentVideoResCam.getCaptureFormats().length)
            return;

        SkylinkCaptureFormat format = currentVideoResCam.getCaptureFormats()[progress];

        String inputValue = "N/A";
        if (format.getWidth() > 0 && format.getHeight() > 0) {
            inputValue = format.getWidth() + "x" + format.getHeight();
        }

        videoResolutionView.updateUIOnCameraInputWHProgressValue(inputValue);
    }

    @Override
    public void processWHSelectedCamera(int progress) {
        if (progress < 0 || progress > currentVideoResCam.getCaptureFormats().length)
            return;

        SkylinkCaptureFormat format = currentVideoResCam.getCaptureFormats()[progress];

        videoService.setInputVideoResolution(SkylinkMedia.MediaType.VIDEO_CAMERA, format.getWidth(), format.getHeight(), currentVideoResCam.getFps());
    }

    @Override
    public void processFpsProgressChangedCamera(int progress) {
        if (progress < 0 || progress > 60)
            return;

        videoResolutionView.updateUIOnCameraInputFpsProgressValue(String.valueOf(progress));

    }

    @Override
    public void processFpsSelectedCamera(int progress) {
        if (progress < 0 || progress > 60)
            return;

        videoService.setInputVideoResolution(SkylinkMedia.MediaType.VIDEO_CAMERA, currentVideoResCam.getWidth(), currentVideoResCam.getHeight(), progress);
    }

    // screen video resolution processing

    @Override
    public void processWHProgressChangedScreen(int progress) {
        if (progress < 0 || progress > currentVideoResScreen.getCaptureFormats().length)
            return;

        SkylinkCaptureFormat format = currentVideoResScreen.getCaptureFormats()[progress];

        String inputValue = "N/A";
        if (format.getWidth() > 0 && format.getHeight() > 0 && progress >= 0) {
            inputValue = format.getWidth() + "x" + format.getHeight();
        }

        videoResolutionView.updateUIOnScreenInputWHProgressValue(inputValue);
    }

    @Override
    public void processWHSelectedScreen(int progress) {
        if (progress < 0 || progress > currentVideoResScreen.getCaptureFormats().length)
            return;

        SkylinkCaptureFormat format = currentVideoResScreen.getCaptureFormats()[progress];

        videoService.setInputVideoResolution(SkylinkMedia.MediaType.VIDEO_SCREEN, format.getWidth(), format.getHeight(), currentVideoResScreen.getFps());
    }

    @Override
    public void processFpsProgressChangedScreen(int progress) {
        if (progress < 0 || progress > 60)
            return;

        videoResolutionView.updateUIOnScreenInputFpsProgressValue(String.valueOf(progress));

    }

    @Override
    public void processFpsSelectedScreen(int progress) {
        if (progress < 0 || progress > 60)
            return;

        videoService.setInputVideoResolution(SkylinkMedia.MediaType.VIDEO_SCREEN, currentVideoResScreen.getWidth(), currentVideoResScreen.getHeight(), progress);
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void processMediaTypeSelected(SkylinkMedia.MediaType mediaType) {
        this.currentMainVideoTypeSelected = mediaType;

        videoResolutionView.updateUIChangeMediaType(mediaType);
    }

    @Override
    public void processInputVideoResolutionObtained(SkylinkMedia.MediaType mediaType, int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            // update currentVideoResCam to the lastest resolution value
            SkylinkCaptureFormat[] captureFormats = videoService.getCaptureFormats(null);
            currentVideoResCam.setCaptureFormats(captureFormats);
            currentVideoResCam.setCurrentCaptureFormat(captureFormat);
            currentVideoResCam.setWidth(width);
            currentVideoResCam.setHeight(height);
            currentVideoResCam.setFps(fps);

            // update input video res value on UI
            String inputValue = "N/A";
            if (width > 0 && height > 0 && fps > -1) {
                inputValue = width + "x" + height + ",\n" + fps + " Fps";
            }

            videoResolutionView.updateUIOnCameraInputValue(inputValue);

            // update input video res on widthxheight in UI
            if (captureFormats != null && captureFormats.length > 0) {
                int len = captureFormats.length - 1;

                int currentIndexWH = 0;
                for (int i = 0; i < captureFormats.length; i++) {
                    if (captureFormats[i].getWidth() == width && captureFormats[i].getHeight() == height) {
                        currentIndexWH = i;
                        break;
                    }
                }

                if (!hasInformedWHFirstValueCam) {
                    String maxWHValue = captureFormats[0].getWidth() + "x" + captureFormats[0].getHeight();
                    String minWHValue = captureFormats[len].getWidth() + "x" + captureFormats[0].getHeight();
                    String WHValue = width + "x" + height;
                    videoResolutionView.updateUIOnCameraInputWHValue(len, maxWHValue, minWHValue, WHValue, currentIndexWH);
                    hasInformedWHFirstValueCam = true;
                }
            }

            // update input video res on Fps in UI
            if (captureFormat == null) {
                return;
            }

            // update the range of fps seekbar
            videoResolutionView.updateUIOnCameraInputFpsValue(String.valueOf(captureFormat.getFpsMax()),
                    String.valueOf(captureFormat.getFpsMin()));

            // update the current value of the fps seekbar
            if (!hasInformedFpsFirstValueCam && fps > 0) {
                videoResolutionView.updateUIOnCameraInputFpsValue(String.valueOf(captureFormat.getFpsMax()),
                        String.valueOf(captureFormat.getFpsMin()), String.valueOf(fps));
                hasInformedFpsFirstValueCam = true;
            }
        } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            // manual create SkylinkCaptureFormat for screen
            // because the mobile screen height is normally double screen width, so create some demo resolutions
            int maxWidth = 800;
            int maxHeight = 1600;

            int minFps = 0;
            int maxFps = 60;

            // create 20 captureFormats as demo for screen
            int RANGE = 20;
            SkylinkCaptureFormat[] captureFormats = new SkylinkCaptureFormat[RANGE];

            for (int i = 0; i < RANGE; i++) {
                int widthFormat = maxWidth;
                int heightFormat = maxHeight;

                SkylinkCaptureFormat format = new SkylinkCaptureFormat(widthFormat, heightFormat, minFps, maxFps);
                captureFormats[i] = format;

                maxWidth -= 30;
                maxHeight -= 60;
            }

            // save value to currentVideoResScreen
            currentVideoResScreen.setCaptureFormats(captureFormats);
            currentVideoResScreen.setCurrentCaptureFormat(new SkylinkCaptureFormat(width, height, minFps, maxFps));
            currentVideoResScreen.setWidth(width);
            currentVideoResScreen.setHeight(height);
            currentVideoResScreen.setFps(fps);

            // update input video res value on UI
            String inputValue = "N/A";
            if (width > 0 && height > 0 && fps > -1) {
                inputValue = width + "x" + height + ",\n" + fps + " Fps";
            }

            videoResolutionView.updateUIOnScreenInputValue(inputValue);

            // update input video res on widthxheight on UI
            int len = captureFormats.length - 1;

            int currentIndexWH = 0;
            for (int i = 0; i < captureFormats.length; i++) {
                if (captureFormats[i].getWidth() == width && captureFormats[i].getHeight() == height) {
                    currentIndexWH = i;
                    break;
                }
            }

            if (!hasInformedWHFirstValueScreen) {
                String maxWHValue = captureFormats[0].getWidth() + "x" + captureFormats[0].getHeight();
                String minWHValue = captureFormats[len].getWidth() + "x" + captureFormats[0].getHeight();
                String WHValue = width + "x" + height;
                videoResolutionView.updateUIOnScreenInputWHValue(len, maxWHValue, minWHValue, WHValue, currentIndexWH);
                hasInformedWHFirstValueScreen = true;
            }

            // update input video res on Fps on UI
            if (!hasInformedFpsFirstValueScreen) {
                videoResolutionView.updateUIOnScreenInputFpsValue(String.valueOf(maxFps), String.valueOf(minFps), String.valueOf(fps));
                hasInformedFpsFirstValueScreen = true;
            }
        }
    }

    @Override
    public void processReceivedVideoResolutionObtained(String peerId, SkylinkMedia.MediaType mediaType, int width, int height, int fps) {
        if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            // Update UI about received resolution from remote peer
            videoResolutionView.updateUIOnCameraReceivedValue(width, height, fps);
        } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            videoResolutionView.updateUIOnScreenReceivedValue(width, height, fps);
        }
    }

    @Override
    public void processSentVideoResolutionObtained(String peerId, SkylinkMedia.MediaType mediaType, int width, int height, int fps) {
        if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            // Update UI about sent resolution to remote peer
            videoResolutionView.updateUIOnCameraSentValue(width, height, fps);
        } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            videoResolutionView.updateUIOnScreenSentValue(width, height, fps);
        }
    }
}
