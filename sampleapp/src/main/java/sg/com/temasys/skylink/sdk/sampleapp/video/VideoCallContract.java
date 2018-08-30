package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.graphics.Point;
import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface VideoCallContract {
    interface View extends BaseView<Presenter> {

        void onConnectingUIChange();

        void onConnectedUIChange();

        void onDisconnectUIChange();

        void onSetUiResTvStatsInput(VideoResolution videoInput);

        void onSetUiResTvStatsSent(VideoResolution videoSent);

        void onSetUiResTvStatsReceive(VideoResolution videoReceive);

        boolean onSetUiResTvDim(int width, int height);

        void onSetUiResTvFps(int fps);

        void onAddSelfView(SurfaceViewRenderer videoView);

        void onAddRemoteView(SurfaceViewRenderer remoteVideoView);

        void onRemoveRemotePeer();

        Fragment onGetFragment();

        void onSetAudioBtnLabel(boolean isAudioMuted, boolean isToast);

        void onSetVideoBtnLabel(boolean isVideoMuted, boolean isToast);

        void onSetUiResSeekBarRangeDim(int maxSeekBarDimRange);

        void onSetUiResSeekBarRangeFps(int maxSeekBarFpsRange);

        void onSetSeekBarResDim(int index, int width, int height);

        void onSetSeekBarResFps(int index, int fps);

        void onChangeBtnSpeakerUI(boolean isSpeakerOff);

        void onChangeBtnAudioMuteUI(boolean isAudioMute);

        void onChangeBtnVideoMuteUI(boolean isVideoMute);

        void onChangeBtnCameraMuteUI(boolean isCameraMute);

    }

    interface Presenter extends BasePresenter {

        void onDisconnectFromRoom();

        void onPermissionRequired(PermRequesterInfo info);

        void onPermissionGranted(PermRequesterInfo info);

        void onPermissionDenied(PermRequesterInfo info);

        void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag);

        void onRemotePeerConnectionRefreshed(String log, UserInfo remotePeerUserInfo);

        void onRemotePeerMediaReceive(String log, UserInfo remotePeerUserInfo);

        void onLocalMediaCapture(SurfaceViewRenderer videoView);

        void onInputVideoResolutionObtained(int width, int height, int fps, SkylinkCaptureFormat captureFormat);

        void onReceivedVideoResolutionObtained(String peerId, int width, int height, int fps);

        void onSentVideoResolutionObtained(String peerId, int width, int height, int fps);

        void onVideoSizeChange(String peerId, Point size);

        void onProcessBtnSpeakerOn();

        void onProcessBtnAudioMute();

        void onProcessBtnVideoMute();

        void onProcessBtnCameraToggle();

        void onViewResume();

        void onViewPause();

        void onSwitchCamera();

        void onDimProgressChanged(int progress);

        void onFpsProgressChanged(int progress);

        void onDimStopTrackingTouch(int progress);

        void onFpsStopTrackingTouch(int progress);

        String onGetRoomPeerIdNick();

        void onGetVideoResolutions();

        void onAudioChangedToSpeaker(boolean isSpeakerOn);

    }

    interface Service extends BaseService<Presenter> {


    }
}

