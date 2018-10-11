package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface VideoCallContract {
    interface View extends BaseView<Presenter> {

        /**
         * Get instance of the fragment for processing permission
         */
        Fragment onPresenterRequestGetFragmentInstance();

        /**
         * Update UI details when changing state
         */
        void onPresenterRequestConnectingUIChange();

        void onPresenterRequestConnectedUIChange();

        void onPresenterRequestDisconnectUIChange();

        void onPresenterRequestUpdateUiResInput(VideoResolution videoInput);

        void onPresenterRequestUpdateUiResSent(VideoResolution videoSent);

        void onPresenterRequestUpdateUiResReceive(VideoResolution videoReceive);

        boolean onPresenterRequestUpdateUiResDimInfo(int width, int height);

        void onPresenterRequestUpdateUiResFpsInfo(int fps);

        void onPresenterRequestAddSelfView(SurfaceViewRenderer videoView);

        void onPresenterRequestAddRemoteView(SurfaceViewRenderer remoteVideoView);

        void onPresenterRequestRemoveRemotePeer();

        void onPresenterRequestUpdateAudioState(boolean isAudioMuted, boolean isToast);

        void onPresenterRequestUpdateVideoState(boolean isVideoMuted, boolean isToast);

        void onPresenterRequestUpdateUiResRangeDimInfo(int maxDimRange);

        void onPresenterRequestUpdateUiResRangeFpsInfo(int maxFpsRange);

        void onPresenterRequestUpdateResDimInfo(int index, int width, int height);

        void onPresenterRequestUpdateResFpsInfo(int index, int fps);

        void onPresenterRequestChangeSpeakerOuput(boolean isSpeakerOff);

        void onPresenterRequestChangeAudioUI(boolean isAudioMute);

        void onPresenterRequestChangeVideoUI(boolean isVideoMute);

        void onPresenterRequestChangeCameraUI(boolean isCameraMute);

        void onPresenterRequestchangeViewLayout();

    }

    interface Presenter {

        /**
         * process data to display on view
         */
        void onViewRequestLayout();

        /**
         * process permission result
         */
        void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag);

        /**
         * process change audio output between headset and speaker
         */
        void onViewRequestChangeAudioOutput();

        /**
         * process change state when button clicked
         */
        void onViewRequestChangeAudioState();

        void onViewRequestChangeVideoState();

        void onViewRequestChangeCameraState();

        /**
         * process change state when video dimension or video fps changed
         */
        void onViewRequestDimProgressChanged(int progress);

        void onViewRequestFpsProgressChanged(int progress);

        void onViewRequestDimSelected(int progress);

        void onViewRequestFpsSelected(int progress);

        /**
         * process change state when view resumed
         */
        void onViewRequestResume();

        /**
         * process change state when view paused
         */
        void onViewRequestPause();

        /**
         * switch camera between front and back
         */
        void onViewRequestSwitchCamera();

        /**
         * get remote peer nick
         */
        String onViewRequestGetRoomPeerIdNick();

        /**
         * get current video resolution info
         */
        void onViewRequestGetVideoResolutions();

        /**
         * process change state when view exit/closed
         */
        void onViewRequestExit();

        void onViewRequestDisconnectFromRoom();
    }

    interface Service extends BaseService<Presenter> {


    }
}

