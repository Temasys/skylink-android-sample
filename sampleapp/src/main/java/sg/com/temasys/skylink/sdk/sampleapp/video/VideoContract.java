package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.content.Intent;
import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionContract;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface VideoContract {
    interface MainView extends BaseView<Presenter> {

        /**
         * Get instance of the fragment for processing audio/camera runtime permission
         */
        Fragment onPresenterRequestGetFragmentInstance();

        /**
         * Update UI details when disconnected from room
         */
        void onPresenterRequestDisconnectUIChange();

        /**
         * Update UI details when adding local video view
         */
        void onPresenterRequestAddCameraSelfView(String mediaId, SurfaceViewRenderer videoView);

        /**
         * Update UI details when adding local video view
         */
        void onPresenterRequestAddScreenSelfView(String mediaId, SurfaceViewRenderer videoView);

        /**
         * Update UI details when receiving remote audio
         */
        void onPresenterRequestReceiveRemoteAudio(String remotePeerId);

        /**
         * Update UI details when adding remote camera video view
         */
        void onPresenterRequestAddCameraRemoteView(SurfaceViewRenderer remoteVideoView);

        /**
         * Update UI details when adding remote screen video view
         */
        void onPresenterRequestAddScreenRemoteView(SurfaceViewRenderer videoView);

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
        void onPresenterRequestChangeUiRemotePeerLeft(List<SkylinkPeer> peerList);

        /**
         * Update UI details when changing speaker state (on/off)
         */
        void onPresenterRequestChangeSpeakerOutput(boolean isSpeakerOff);

        /**
         * Show or hide button stop screen sharing on UI
         */
        void onPresenterRequestShowButtonStopScreenSharing();

        /**
         * Update UI details when local audio is on
         */
        void onPresenterRequestLocalAudioCapture(String mediaId);

        void onPresenterRequestMediaStateChange(SkylinkMedia.MediaType mediaType, SkylinkMedia.MediaState mediaState, boolean isLocal);

        void onPresenterRequestChangeRoomLockStatus(boolean isRoomLocked);
    }

    interface Presenter {

        /**
         * process data to display on view at initiative connection
         */
        void onViewRequestConnectedLayout();

        /**
         * process results from Activity for results
         */
        void onViewRequestActivityResult(int requestCode, int resultCode, Intent data);

        /**
         * process runtime permission results
         */
        void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

        /**
         * process change audio output between headset and speaker
         */
        void onViewRequestChangeSpeakerOutput();

        /**
         * process change states when buttons clicked
         */
        void onViewRequestChangeAudioState();

        void onViewRequestChangeVideoState();

        void onViewRequestChangeScreenState();

        /**
         * process change state when view resumed
         */
        void onViewRequestResume();

        /**
         * process change state when view paused
         */
        void onViewRequestPause();

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

        /**
         * switch camera between front and back
         */
        void onViewRequestSwitchCamera();

        /**
         * start local audio
         */
        void onViewRequestStartAudio();

        void onViewRequestStartLocalMediaIfConfigAllow();

        void onViewRequestToggleVideo();

        void onViewRequestToggleScreen();


        void onViewRequestToggleScreen(boolean start);

        void onViewRequestRemoveAudio();

        void onViewRequestRemoveVideo();

        void onViewRequestRemoveScreen();

        void onViewRequestLockRoom();

        void onViewRequestUnlockRoom();
    }

    interface Service extends BaseService<Presenter> {

        /**
         * set connection between service and presenter
         */
        void setResPresenter(VideoResolutionContract.Presenter videoResPresenter);
    }
}

