package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;

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
         * Update UI details when changing audio state (muted/on)
         */
        void onPresenterRequestUpdateAudioState(boolean isAudioMuted, boolean isToast);

        /**
         * Update UI details when changing video state (muted/on)
         */
        void onPresenterRequestUpdateVideoState(boolean isVideoMuted, boolean isToast);

        /**
         * Update UI details when changing max range of video width and height seekbar
         */
        void onPresenterRequestUpdateUiResRangeDimInfo(int maxDimRange);

        /**
         * Update UI details when changing max range of video frame rate seekbar
         */
        void onPresenterRequestUpdateUiResRangeFpsInfo(int maxFpsRange);

        /**
         * Update text view and seek bar info  when changing video width and height
         */
        void onPresenterRequestUpdateResDimInfo(int index, int width, int height);

        /**
         * Update text view and seek bar info when changing video frame rate
         */
        void onPresenterRequestUpdateResFpsInfo(int index, int fps);

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

        /**
         * Update UI details when changing screen configuration
         */
        void onPresenterRequestchangeViewLayout();

        /**
         * Update info about the connected room {roomId}
         */
        void onPresenterRequestUpdateRoomInfo(String roomInfo);

        /**
         * Update info about the local peer in action bar
         */
        void onPresenterRequestUpdateLocalPeer(String localUserName);

        /**
         * Update UI details when peers are in room
         */
        void onPresenterRequestRemotePeerLeft(List<SkylinkPeer> peersList);

        /**
         * Update UI details when new remote peer joins at a specific index the room
         */
        void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer, int index);

    }

    interface Presenter {

        /**
         * process data to display on view at initiative connection
         */
        void onViewRequestConnectedLayout();

        /**
         * process runtime permission results
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
         * get remote peer nickname from SDK
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

        /**
         * process get peer info at specific index
         */
        SkylinkPeer onViewRequestGetPeerByIndex(int index);
    }

    interface Service extends BaseService<Presenter> {


    }
}

