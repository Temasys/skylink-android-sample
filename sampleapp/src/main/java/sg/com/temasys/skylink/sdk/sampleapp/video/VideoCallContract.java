package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.graphics.Point;
import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.VideoResolution;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface VideoCallContract {
    interface View extends BaseView<Presenter> {

        void onConnectingUIChangeViewHandler();

        void onConnectedUIChangeViewHandler();

        void onDisconnectUIChangeViewHandler();

        void onSetUiResTvStatsInputViewHandler(VideoResolution videoInput);

        void onSetUiResTvStatsSentViewHandler(VideoResolution videoSent);

        void onSetUiResTvStatsReceiveViewHandler(VideoResolution videoReceive);

        boolean onSetUiResTvDimViewHandler(int width, int height);

        void onSetUiResTvFpsViewHandler(int fps);

        void onAddSelfViewViewHandler(SurfaceViewRenderer videoView);

        void onAddRemoteViewViewHandler(SurfaceViewRenderer remoteVideoView);

        void onRemoveRemotePeerViewHandler();

        Fragment onGetFragmentViewHandler();

        void onSetAudioBtnLabelViewHandler(boolean isAudioMuted, boolean isToast);

        void onSetVideoBtnLabelViewHandler(boolean isVideoMuted, boolean isToast);

        void onSetUiResSeekBarRangeDimViewHandler(int maxSeekBarDimRange);

        void onSetUiResSeekBarRangeFpsViewHandler(int maxSeekBarFpsRange);

        void onSetSeekBarResDimViewHandler(int index, int width, int height);

        void onSetSeekBarResFpsViewHandler(int index, int fps);

    }

    interface Presenter extends BasePresenter {

        void onDisconnectFromRoomPresenterHandler();

        void onPermissionRequiredPresenterHandler(PermRequesterInfo info);

        void onPermissionGrantedPresenterHandler(PermRequesterInfo info);

        void onPermissionDeniedPresenterHandler(PermRequesterInfo info);

        void onRequestPermissionsResultPresenterHandler(int requestCode, String[] permissions, int[] grantResults, String tag);

        /**
         * process update view when remote peer refresh the connection
         * @param log info to display
         * @param remotePeerUserInfo
         */
        void onRemotePeerConnectionRefreshedPresenterHandler(String log, UserInfo remotePeerUserInfo);

        /**
         * process update view when remote peer has receive media info
         * @param log info to display
         * @param remotePeerUserInfo
         */
        void onRemotePeerMediaReceivePresenterHandler(String log, UserInfo remotePeerUserInfo);

        void onLocalMediaCapturePresenterHandler(SurfaceViewRenderer videoView);

        void onInputVideoResolutionObtainedPresenterHandler(int width, int height, int fps, SkylinkCaptureFormat captureFormat);

        void onReceivedVideoResolutionObtainedPresenterHandler(String peerId, int width, int height, int fps);

        void onSentVideoResolutionObtainedPresenterHandler(String peerId, int width, int height, int fps);

        void onVideoSizeChangePresenterHandler(String peerId, Point size);

        void onProcessBtnAudioMutePresenterHandler();

        void onProcessBtnVideoMutePresenterHandler();

        void onProcessBtnCameraTogglePresenterHandler();

        void onViewResumePresenterHandler();

        void onViewPausePresenterHandler();

        void onSwitchCameraPresenterHandler();

        void onDimProgressChangedPresenterHandler(int progress);

        void onFpsProgressChangedPresenterHandler(int progress);

        void onDimStopTrackingTouchPresenterHandler(int progress);

        void onFpsStopTrackingTouchPresenterHandler(int progress);

        String onGetRoomPeerIdNickPresenterHandler();

        void onGetVideoResolutionsPresenterHandler();

    }

    interface Service extends BaseService<Presenter> {


    }
}

