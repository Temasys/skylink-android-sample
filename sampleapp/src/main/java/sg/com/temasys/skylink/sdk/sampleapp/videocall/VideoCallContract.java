package sg.com.temasys.skylink.sdk.sampleapp.videocall;

import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.VideoResolution;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface VideoCallContract {
    interface View extends BaseView<Presenter> {

        void onConnectingUIChangeViewHandler();

        void onConnectedUIChangeViewHandler();

        void onDisconnectUIChangeViewHandler();

        void onSetUiResTvStatsSentViewHandler(VideoResolution videoSent);

        void onSetUiResTvStatsReceiveViewHandler(VideoResolution videoReceive);

        void onAddSelfViewViewHandler(SurfaceViewRenderer videoView);

        void onAddRemoteViewViewHandler(SurfaceViewRenderer remoteVideoView);

        void onRemotePeerLeaveUIChangeViewHandler();

        Fragment onGetFragmentViewHandler();

        void onSetAudioBtnLabelViewHandler(boolean isAudioMuted, boolean isToast);

        void onSetVideoBtnLabelViewHandler(boolean isVideoMuted, boolean isToast);

        void onSetTvResInputStatsViewHandler(VideoResolution videoInput);

        boolean onSetUiResTvDimViewHandler(int width, int height);

        void onSetUiResSeekBarRangeDimViewHandler(int maxSeekBarDimRange);

        void onSetSeekBarResDimViewHandler(int index, int width, int height);

        void onSetUiResSeekBarRangeFpsViewHandler(int seekBarResFpsMax);

        void onSetSeekBarResFpsViewHandler(int index, int fps);

        void onSetUiResTvFpsViewHandler(int fps);

    }

    interface Presenter extends BasePresenter {

        void disconnectFromRoomPresenterHandler();

        String getPeerIdPresenterHandler(int index);

        String getRoomPeerIdNickPresenterHandler();

        void getVideoResolutionsPresenterHandler(String peerIdPresenterHandler);

        void switchCameraPresenterHandler();

        void processBtnAudioMutePresenterHandler();

        void processBtnVideoMutePresenterHandler();

        void processBtnCameraTogglePresenterHandler();

        void onViewPausePresenterHandler();

        void onRequestPermissionsResultPresenterHandler(int requestCode, String[] permissions, int[] grantResults, String tag);

        void onDimProgressChangedPresenterHandler(int progress);

        void onFpsProgressChangedPresenterHandler(int progress);

        void onDimStopTrackingTouchPresenterHandler(int progress);

        void onFpsStopTrackingTouchPresenterHandler(int progress);


        void onViewResumePresenterHandler();
    }

    interface Service extends BaseService<Presenter> {


    }
}

