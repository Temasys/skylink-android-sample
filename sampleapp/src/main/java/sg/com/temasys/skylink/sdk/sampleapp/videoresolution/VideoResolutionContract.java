package sg.com.temasys.skylink.sdk.sampleapp.videoresolution;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;

public interface VideoResolutionContract {
    interface View extends BaseView<VideoResolutionContract.Presenter> {
        /**
         * Update UI details when getting local input video resolution
         */
        void onPresenterRequestUpdateUiResInput(VideoResolution videoInput);

        /**
         * Update UI details when getting local sent video resolution
         */
        void onPresenterRequestUpdateUiResSent(VideoResolution videoSent);

        /**
         * Update UI details when getting remote received video resolution
         */
        void onPresenterRequestUpdateUiResReceive(VideoResolution videoReceive);

        /**
         * Update text view info when changing video width and height
         */
        boolean onPresenterRequestUpdateUiResDimInfo(int width, int height);

        /**
         * Update text view info details when changing video frame rate
         */
        void onPresenterRequestUpdateUiResFpsInfo(int fps);

        /**
         * Update UI details when changing max range of video width and height seekbar
         */
        void onPresenterRequestUpdateUiResRangeDimInfo(int maxDimRange, String minDimValue, String maxDimValue);

        /**
         * Update UI details when changing max range of video frame rate seekbar
         */
        void onPresenterRequestUpdateUiResRangeFpsInfo(int maxFpsRange, int minFpsValue, int maxFpsValue);

        /**
         * Update text view and seek bar info  when changing video width and height
         */
        void onPresenterRequestUpdateResDimInfo(int index, int width, int height);

        /**
         * Update text view and seek bar info when changing video frame rate
         */
        void onPresenterRequestUpdateResFpsInfo(int index, int fps);

    }

    interface Presenter {
        /**
         * process change state when video dimension or video fps changed
         */
        void onViewRequestDimProgressChanged(int progress);

        void onViewRequestFpsProgressChanged(int progress);

        void onViewRequestDimSelected(int progress);

        void onViewRequestFpsSelected(int progress);

        void onViewRequestGetVideoResolutions();

    }

    interface Service extends BaseService<VideoResolutionContract.Presenter> {

    }
}
