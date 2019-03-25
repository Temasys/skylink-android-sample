package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionContract;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface VideoCallContract {
    interface View extends BaseView<Presenter> {

        /**
         * Get instance of the fragment for processing audio/camera runtime permission
         */
        Fragment onPresenterRequestGetFragmentInstance();

        /**
         * Update UI details when connecting to room
         */
        void onPresenterRequestConnectingUIChange();

        /**
         * Update UI details when connected to room
         */
        void onPresenterRequestConnectedUIChange();

        /**
         * Update UI details when disconnected from room
         */
        void onPresenterRequestDisconnectUIChange();

        /**
         * Update UI details when adding local video view
         */
        void onPresenterRequestAddSelfView(SurfaceViewRenderer videoView);

        /**
         * Update UI details when adding remote video view
         */
        void onPresenterRequestAddRemoteView(SurfaceViewRenderer remoteVideoView);

        /**
         * Update UI details when removing remote video view
         */
        void onPresenterRequestRemoveRemotePeer();

        /**
         * Update UI into connected state
         */
        void onPresenterRequestUpdateUIConnected(String roomId);

        /**
         * Update UI details when new remote peer joins at a specific index the room
         */
        void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer, int index);

        /**
         * Update UI details when peers are in room
         */
        void onPresenterRequestChangeUiRemotePeerLeft(List<SkylinkPeer> peersList);

        /**
         * Update audio state (muted/on)
         */
        void onPresenterRequestUpdateAudioState(boolean isAudioMuted, boolean isToast);

        /**
         * Update video state (muted/on)
         */
        void onPresenterRequestUpdateVideoState(boolean isVideoMuted, boolean isToast);

        /**
         * Update UI details when changing speaker state (on/off)
         */
        void onPresenterRequestChangeAudioOuput(boolean isSpeakerOff);

        /**
         * Update UI details when changing audio state (muted/on)
         */
        void onPresenterRequestChangeAudioUI(boolean isAudioMute);

        /**
         * Update UI details when changing video state (muted/on)
         */
        void onPresenterRequestChangeVideoUI(boolean isVideoMute);

        /**
         * Update UI details when changing camera state (muted/on)
         */
        void onPresenterRequestChangeCameraUI(boolean isCameraMute);
    }

    interface Presenter {

        /**
         * process data to display on view at initiative connection
         */
        void onViewRequestConnectedLayout();

        /**
         * process runtime permission results
         */
        void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

        /**
         * process change audio output between headset and speaker
         */
        void onViewRequestChangeAudioOutput();

        /**
         * process change state when buttons clicked
         */
        void onViewRequestChangeAudioState();

        void onViewRequestChangeVideoState();

        void onViewRequestChangeCameraState();

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
         * get current video resolution info
         */
        void onViewRequestGetVideoResolutions();

        /**
         * process change state when view exit/closed
         */
        void onViewRequestExit();

        /**
         * process change state when disconnect from the room
         */
        void onViewRequestDisconnectFromRoom();

        /**
         * process get peer info at specific index
         */
        SkylinkPeer onViewRequestGetPeerByIndex(int index);
    }

    interface Service extends BaseService<Presenter> {

        /**
         * set connection between service and presenter
         */
        void setResPresenter(VideoResolutionContract.Presenter videoResPresenter);
    }
}

