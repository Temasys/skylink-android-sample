package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface MultiPartyVideoCallContract {
    interface View extends BaseView<Presenter> {

        void onPresenterRequestAddSelfView(SurfaceViewRenderer videoView);

        void onPresenterRequestAddRemoteView(int peerIndex, SurfaceViewRenderer remoteView);

        void onPresenterRequestRemoveRemotePeer(int viewIndex);

        Fragment onPresenterRequestGetFragmentInstance();

        void onPresenterRequestDisplayVideoLinkInfo(String recordingId, String msg);
    }

    interface Presenter{

        void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag);

        void onViewRequestResume();

        void onViewRequestPause();

        void onViewRequestExit();

        void onViewRequestSwitchCamera();

        boolean onViewRequestStartRecording();

        boolean onViewRequestStopRecording();

        String onViewRequestGetRoomPeerIdNick();

        void onViewRequestGetInputVideoResolution();

        void onViewRequestGetSentVideoResolution(int peerIndex);

        void onViewRequestGetReceivedVideoResolution(int peerIndex);

        void onViewRequestWebrtcStatsToggle(int peerIndex);

        void onViewRequestGetTransferSpeeds(int peerIndex, int mediaDirection, int mediaType);

        void onViewRequestRefreshConnection(int peerIndex, boolean iceRestart);

        Boolean onViewRequestGetWebRtcStatsByPeerId(int peerIndex);

        int onViewRequestGetTotalInRoom();

        SurfaceViewRenderer onViewRequestGetVideoViewByIndex(int i);

        void onViewRequestLayout();
    }

    interface Service extends BaseService<Presenter> {


    }
}

