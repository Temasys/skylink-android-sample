package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.content.Intent;

import androidx.fragment.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

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
        Fragment getInstance();

        /**
         * Update UI into connected state
         */
        void updateUIConnected(String roomId);

        /**
         * Update UI details when disconnected from room
         */
        void updateUIDisconnected();

        /**
         * Update UI details when new remote peer joins at a specific index the room
         */
        void updateUIRemotePeerConnected(SkylinkPeer newPeer, int index);

        /**
         * Update UI details when peers are in room
         */
        void updateUIRemotePeerDisconnected(List<SkylinkPeer> peerList);

        /**
         * Update UI details when local audio is on
         */
        void updateUILocalAudioAdded(String mediaId);

        /**
         * Update UI details when adding local video view
         */
        void updateUILocalCameraAdded(String mediaId, SurfaceViewRenderer videoView);

        /**
         * Update UI details when adding local video view
         */
        void updateUILocalScreenAdded(String mediaId, SurfaceViewRenderer videoView);

        /**
         * Update UI details when receiving remote audio
         */
        void updateUIReceiveRemoteAudio(String remotePeerId);

        /**
         * Update UI details when adding remote camera video view
         */
        void updateUIReceiveRemoteVideo(SurfaceViewRenderer remoteVideoView);

        /**
         * Update UI details when adding remote screen video view
         */
        void updateUIReceiveRemoteScreen(SurfaceViewRenderer videoView);

        void updateUIMediaStateChange(SkylinkMedia.MediaType mediaType, SkylinkMedia.MediaState mediaState, boolean isLocal);

        /**
         * Update UI details when removing remote video view
         */
        void updateUIRemoveRemotePeer();

        /**
         * Update UI details when changing speaker state (on/off)
         */
        void updateUIAudioOutputChanged(boolean isSpeakerOff);

        /**
         * Show or hide button stop screen sharing on UI
         */
        void updateUIShowButtonStopScreenShare();

        void updateUIRoomLockStatusChanged(boolean isRoomLocked);
    }

    interface Presenter {

        /**
         * process data to display on view at initiative connection
         */
        void processConnectedLayout();

        /**
         * process change state when disconnect from the room
         */
        void processDisconnectFromRoom();

        /**
         * process results from Activity for results
         */
        void processActivityResult(int requestCode, int resultCode, Intent data);

        /**
         * process runtime permission results
         */
        void processPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

        void processStartLocalMediaIfConfigAllow();

        /**
         * start local audio
         */
        void processStartAudio();

        void processToggleVideo();

        void processToggleScreen();


        void processToggleScreen(boolean start);

        void processRemoveAudio();

        void processRemoveVideo();

        void processRemoveScreen();

        /**
         * process change states when buttons clicked
         */
        void processChangeAudioState();

        void processChangeVideoState();

        void processChangeScreenState();

        /**
         * process change state when view resumed
         */
        void processResumeState();

        /**
         * process change state when view paused
         */
        void processPauseState();

        /**
         * process change state when view exit/closed
         */
        void processExit();

        /**
         * process change audio output between headset and speaker
         */
        void processChangeAudioOutput();

        /**
         * process get peer info at specific index
         */
        SkylinkPeer processGetPeerByIndex(int index);

        /**
         * switch camera between front and back
         */
        void processSwitchCamera();

        void processLockRoom();

        void processUnlockRoom();
    }

    interface Service extends BaseService<Presenter> {

        /**
         * set connection between video service and video resolution presenter
         */
        void setResPresenter(VideoResolutionContract.Presenter videoResPresenter);
    }
}

