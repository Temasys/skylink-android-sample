package sg.com.temasys.skylink.sdk.sampleapp.videoresolution;

import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;

public interface VideoResolutionContract {
    interface View extends BaseView<VideoResolutionContract.Presenter> {

        void updateUIChangeMediaType(SkylinkMedia.MediaType mediaType);

        void updateUIOnCameraInputWHValue(int maxWHRange, String minWHValue, String maxWHValue, String currentWHValue, int currentIndex);

        void updateUIOnCameraInputFpsValue(String maxFps, String minFps, String fps);

        void updateUIOnCameraInputFpsValue(String maxFps, String minFps);

        void updateUIOnCameraInputValue(String inputValue);

        void updateUIOnCameraReceivedValue(int width, int height, int fps);

        void updateUIOnCameraSentValue(int width, int height, int fps);

        void updateUIOnCameraInputWHProgressValue(String valueWH);

        void updateUIOnCameraInputFpsProgressValue(String valueFps);

        void updateUIOnScreenInputWHValue(int maxWHRange, String minWHValue, String maxWHValue, String currentWHValue, int currentIndex);

        void updateUIOnScreenInputFpsValue(String maxFps, String minFps, String fps);

        void updateUIOnScreenInputValue(String inputValue);

        void updateUIOnScreenReceivedValue(int width, int height, int fps);

        void updateUIOnScreenSentValue(int width, int height, int fps);

        void updateUIOnScreenInputWHProgressValue(String valueWH);

        void updateUIOnScreenInputFpsProgressValue(String valueFps);
    }

    interface Presenter {

        void processGetVideoResolutions();

        void processChooseVideoCamera();

        void processChooseVideoScreen();

        void processMediaTypeSelected(SkylinkMedia.MediaType videoCamera);

        void processWHProgressChangedCamera(int progress);

        void processWHSelectedCamera(int progress);

        void processFpsProgressChangedCamera(int progress);

        void processFpsSelectedCamera(int progress);

        void processWHProgressChangedScreen(int progress);

        void processWHSelectedScreen(int progress);

        void processFpsProgressChangedScreen(int progress);

        void processFpsSelectedScreen(int progress);
    }

    interface Service extends BaseService<VideoResolutionContract.Presenter> {

    }
}
