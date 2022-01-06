package sg.com.temasys.skylink.sdk.sampleapp.multivideos;

import android.content.Intent;

import androidx.fragment.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 * This interface is responsible for specify behaviors of View, Presenter, Service
 */

public interface MultiVideosContract {

    interface View extends BaseView<Presenter> {

        /**
         * Get instance of the fragment for processing runtime permission
         */
        Fragment getInstance();

        /**
         * Update UI into connected state
         */
        void updateUIConnected(String roomId);

        /**
         * Update UI into disconnected state
         */
        void updateUIDisconnected();

        /**
         * Update UI when remote peer join the room
         */
        void updateUIRemotePeerConnected(SkylinkPeer newPeer, int index);

        /**
         * Update UI details when peers are in room
         */
        void updateUIRemotePeerDisconnected(int peerIndex, List<SkylinkPeer> peersList);

        /**
         * Update UI details when need to add local video view
         */
        void updateUIAddLocalMediaView(SurfaceViewRenderer videoView, SkylinkMedia.MediaType mediaType);

        /**
         * Update UI details when need to add remote video view
         */
        void updateUIAddRemoteMediaView(int peerIndex, SkylinkMedia.MediaType mediaType, SurfaceViewRenderer remoteView);

        /**
         * Update UI details when need to remove remote video view
         */
        void updateUIRemoveRemotePeer(int viewIndex);

        /**
         * Show or hide button stop screen sharing on UI
         */
        void updateUIShowButtonStopScreenSharing();
    }

    interface Presenter {

        /**
         * process runtime audio/camera permission results
         */
        void processPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

        /**
         * process the permission for screen sharing
         */
        void processActivityResult(int requestCode, int resultCode, Intent data);

        /**
         * Start local audio
         */
        void processStartAudio();

        /**
         * Start local video base on the default video device setting
         */
        void processStartVideo();

        /**
         * Start local screen video view
         */
        void processStartScreenShare();

        /**
         * Start second video view
         */
        void processStartSecondVideoView();

        void processStartLocalMediaIfConfigAllow();

        void processStopScreenShare();

        /**
         * process data to display on view at initiative connection
         */
        void processConnectedLayout();

        /**
         * process resuming the app state when view resumed
         */
        void processResumeState();

        /**
         * process pausing the app state when view paused
         */
        void processPauseState();

        /**
         * process disconnecting/closing the app when view exited
         */
        void processExit();

        /**
         * process switching camera to front/back camera
         */
        void processSwitchCamera();

        /**
         * process starting recording function
         */
        void processStartRecording();

        /**
         * process stopping recording function
         */
        void processStopRecording();

        /**
         * process get info about local input video resolution from SDK
         */
        void processGetInputVideoResolution();

        /**
         * process get info about local sent video resolution from SDK
         */
        void processGetSentVideoResolution(int peerIndex);

        /**
         * process get info about remote received video resolution from SDK
         */
        void processGetReceivedVideoResolution(int peerIndex);

        /**
         * process toggling stats from WebRTC lib
         */
        void processToggleWebrtcStats(int peerIndex);

        /**
         * process getting info about transfer speed through network
         */
        void processGetTransferSpeeds(int peerIndex, SkylinkMedia.MediaType mediaType, boolean forSending);

        /**
         * process refresh the connection to remote peer
         */
        void processRefreshConnection(int peerIndex, boolean iceRestart);

        /**
         * process getting total number of peers in the current room
         */
        int processGetTotalInRoom();

        /**
         * process getting remote video view from specific peer
         */
        List<SurfaceViewRenderer> processGetVideoViewByIndex(int index);

        /**
         * Get specific peer at index
         */
        SkylinkPeer processGetPeerByIndex(int index);

        /**
         * Get the stats state of specific remote video view
         */
        Boolean processGetWebRtcStatsState(int peerIndex);

    }

    interface Service extends BaseService<Presenter> {


    }
}

