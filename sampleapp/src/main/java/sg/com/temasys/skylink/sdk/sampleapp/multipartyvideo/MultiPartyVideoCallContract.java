package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.graphics.Point;
import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import java.util.HashMap;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface MultiPartyVideoCallContract {
    interface View extends BaseView<Presenter> {

        void onAddSelfView(SurfaceViewRenderer videoView);

        void onAddRemoteView(int peerIndex, SurfaceViewRenderer remoteView);

        void onRemoveRemotePeer(int viewIndex);

        Fragment onGetFragment();

        void onDisplayAlerDlg(String recordingId, String msg);
    }

    interface Presenter extends BasePresenter {

        void onPermissionRequired(PermRequesterInfo info);

        void onPermissionGranted(PermRequesterInfo info);

        void onPermissionDenied(PermRequesterInfo info);

        void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag);

        void onViewResume();

        void onViewPause();

        void onViewExit();

        void onSwitchCamera();

        boolean onStartRecording();

        boolean onStopRecording();

        void onRemotePeerConnectionRefreshed(String log, UserInfo remotePeerUserInfo);

        void onLocalMediaCapture(SurfaceViewRenderer videoView);

        void onRemotePeerMediaReceive(String log, UserInfo remotePeerUserInfo, String remotePeerId);

        void onInputVideoResolutionObtained(int width, int height, int fps, SkylinkCaptureFormat captureFormat);

        void onReceivedVideoResolutionObtained(String peerId, int width, int height, int fps);

        void onSentVideoResolutionObtained(String peerId, int width, int height, int fps);

        void onVideoSizeChange(String peerId, Point size);

        void onRecordingStart(boolean recording);

        void onRecordingStop(boolean recording);

        void onRecordingVideoLink(String recordingId, String peerId, String videoLink);

        void onRecordingError(String recordingId, int errorCode, String description);

        void onTransferSpeedReceived(String peerId, int mediaDirection, int mediaType, double transferSpeed);

        void onWebrtcStatsReceived(String peerId, int mediaDirection, int mediaType, HashMap<String, String> stats);

        String onGetRoomPeerIdNick();

        void onGetInputVideoResolution();

        void onGetSentVideoResolution(int peerIndex);

        void onGetReceivedVideoResolution(int peerIndex);

        void onWebrtcStatsToggle(int peerIndex);

        void onGetTransferSpeeds(int peerIndex, int mediaDirection, int mediaType);

        void onRefreshConnection(int peerIndex, boolean iceRestart);

        Boolean onGetWebRtcStatsByPeerId(int peerIndex);

        int onGetTotalInRoom();

        SurfaceViewRenderer onGetVideoViewByIndex(int i);

        void onSetRemovedPeerIndex(int removeIndex);
    }

    interface Service extends BaseService<Presenter> {


    }
}

