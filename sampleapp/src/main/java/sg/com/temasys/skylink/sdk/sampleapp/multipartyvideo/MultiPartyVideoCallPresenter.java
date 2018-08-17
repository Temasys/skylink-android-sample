package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sg.com.temasys.skylink.sdk.rtc.Info;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.service.MultiPartyVideoService;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */
public class MultiPartyVideoCallPresenter implements MultiPartyVideoCallContract.Presenter {

    private final String TAG = MultiPartyVideoCallPresenter.class.getName();

    public MultiPartyVideoCallContract.View mMultiVideoCallView;
    private MultiPartyVideoService mMultiVideoCallService;

    //utils to process permission
    private PermissionUtils mPermissionUtils;

    private Context mContext;

    // Map with PeerId as key for boolean state
    // that indicates if currently getting WebRTC stats for Peer.
    private static ConcurrentHashMap<String, Boolean> isGettingWebrtcStats =
            new ConcurrentHashMap<String, Boolean>();

    //index of removed peer when remote peer leave the room
    private static int removeIndex = -1;

    public MultiPartyVideoCallPresenter(MultiPartyVideoCallContract.View videoCallView, Context context) {
        this.mMultiVideoCallView = videoCallView;
        this.mContext = context;

        mMultiVideoCallService = new MultiPartyVideoService(mContext);

        this.mMultiVideoCallView.setPresenter(this);
        this.mMultiVideoCallService.setPresenter(this);

        this.mMultiVideoCallService.setTypeCall();

        mPermissionUtils = new PermissionUtils();
    }

    @Override
    public void onViewLayoutRequested() {
        Log.d(TAG, "onViewLayoutRequested");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mMultiVideoCallService.isConnectingOrConnected()) {

            //reset permission request states.
            mPermissionUtils.permQReset();

            //connect to room on Skylink connection
            connectToRoom();

            //after connected to skylink SDK, UI will be updated latter on AudioService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //if it already connected to room, then resume permission
            mPermissionUtils.permQResume(mContext, mMultiVideoCallView.onGetFragment());

            //update UI into connected
            updateConnectedUI();

            Log.d(TAG, "Try to update UI when changing configuration");
        }
    }

    @Override
    public void onConnect(boolean isSuccessful) {

        //no need to update UI
    }

    @Override
    public void onDisconnect() {

        //no need to update UI

    }

    @Override
    public void onViewExit() {
        //process disconnect from room
        mMultiVideoCallService.disconnectFromRoom();
    }

    @Override
    public void onSwitchCamera() {
        mMultiVideoCallService.switchCamera();
    }

    @Override
    public boolean onStartRecording() {
        return mMultiVideoCallService.startRecording();
    }

    @Override
    public boolean onStopRecording() {
        return mMultiVideoCallService.stopRecording();
    }

    @Override
    public String onGetRoomPeerIdNick() {
        return mMultiVideoCallService.getRoomPeerIdNick();
    }

    @Override
    public void onGetInputVideoResolution() {
        mMultiVideoCallService.getInputVideoResolution();
    }

    @Override
    public void onGetSentVideoResolution(int peerIndex) {
        mMultiVideoCallService.getSentVideoResolution(peerIndex);
    }

    @Override
    public void onGetReceivedVideoResolution(int peerIndex) {
        mMultiVideoCallService.getReceivedVideoResolution(peerIndex);
    }

    @Override
    public void onWebrtcStatsToggle(int peerIndex) {

        String peerId = mMultiVideoCallService.getPeerIdByIndex(peerIndex);

        if (peerId == null)
            return;

        Boolean gettingStats = isGettingWebrtcStats.get(peerId);
        if (gettingStats == null) {
            String log = "[SA][wStatsTog] Peer " + peerId +
                    " does not exist. Will not get WebRTC stats.";
            Log.e(TAG, log);
            return;
        }

        // Toggle the state of getting WebRTC stats to the opposite state.
        if (gettingStats) {
            gettingStats = false;
        } else {
            gettingStats = true;
        }
        isGettingWebrtcStats.put(peerId, gettingStats);
        getWStatsAll(peerId);
    }

    @Override
    public void onGetTransferSpeeds(int peerIndex, int mediaDirection, int mediaType) {
        mMultiVideoCallService.getTransferSpeeds(peerIndex, mediaDirection, mediaType);
    }

    @Override
    public void onRefreshConnection(int peerIndex, boolean iceRestart) {

        mMultiVideoCallService.refreshConnection(peerIndex, iceRestart);
    }

    @Override
    public Boolean onGetWebRtcStatsByPeerId(int peerIndex) {

        String peerId = mMultiVideoCallService.getPeerIdByIndex(peerIndex);

        return isGettingWebrtcStats.get(peerId);
    }

    @Override
    public int onGetTotalInRoom() {
        return mMultiVideoCallService.getTotalInRoom();
    }

    @Override
    public SurfaceViewRenderer onGetVideoViewByIndex(int index) {
        return mMultiVideoCallService.getVideoViewByIndex(index);
    }

    @Override
    public void onLocalMediaCapture(SurfaceViewRenderer videoView) {
        String log = "[SA][onLocalMediaCapture] ";

        if (videoView == null) {
            log += "VideoView is null!";
            Log.d(TAG, log);

            SurfaceViewRenderer selfVideoView = mMultiVideoCallService.getVideoView(null);
            mMultiVideoCallView.onAddSelfView(selfVideoView);

        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
            mMultiVideoCallView.onAddSelfView(videoView);
        }
    }

    @Override
    public void onInputVideoResolutionObtained(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        String log = "[SA][VideoResInput] The current video input has width x height, fps: " +
                width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
        toastLogLong(TAG, mContext, log);
    }

    @Override
    public void onReceivedVideoResolutionObtained(String peerId, int width, int height, int fps) {
        String log = "[SA][VideoResRecv] The current video received from Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
        toastLogLong(TAG, mContext, log);
    }

    @Override
    public void onSentVideoResolutionObtained(String peerId, int width, int height, int fps) {
        String log = "[SA][VideoResSent] The current video sent to Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
        toastLogLong(TAG, mContext, log);
    }

    @Override
    public void onVideoSizeChange(String peerId, Point size) {
        String peer = "Peer " + peerId;
        // If peerId is null, this call is for our local video.
        if (peerId == null) {
            peer = "We've";
        }
        Log.d(TAG, peer + " got video size changed to: " + size.toString() + ".");
    }

    @Override
    public void onRemotePeerJoin(SkylinkPeer skylinkPeer) {

        isGettingWebrtcStats.put(skylinkPeer.getPeerId(), false);

        addRemoteView(skylinkPeer.getPeerId());

    }

    @Override
    public void onRemotePeerLeave(String remotePeerId) {

        isGettingWebrtcStats.remove(remotePeerId);

        mMultiVideoCallView.onRemoveRemotePeer(removeIndex);
    }

    @Override
    public void onSetRemovedPeerIndex(int removeIndex) {
        this.removeIndex = removeIndex;
    }

    @Override
    public void onRemotePeerConnectionRefreshed(String log, UserInfo remotePeerUserInfo) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onRemotePeerMediaReceive(String log, UserInfo remotePeerUserInfo, String remotePeerId) {

        addRemoteView(remotePeerId);

        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        Log.d(TAG, log);
    }

    @Override
    public void onRecordingStart(boolean recording) {
        String log = "[SRS][SA] Recording Started! isRecording=" +
                recording + ".";
        toastLogLong(TAG, mContext, log);
    }

    @Override
    public void onRecordingStop(boolean recording) {
        String log = "[SRS][SA] Recording Stopped! isRecording=" +
                recording + ".";
        toastLogLong(TAG, mContext, log);
    }

    @Override
    public void onRecordingVideoLink(String recordingId, String peerId, String videoLink) {
        String peer = " Mixin";
        if (peerId != null) {
            peer = " Peer " + mMultiVideoCallService.getPeerIdNick(peerId) + "'s";
        }
        String msg = "Recording:" + recordingId + peer + " video link:\n" + videoLink;

        mMultiVideoCallView.onDisplayAlerDlg(recordingId, msg);

    }

    @Override
    public void onRecordingError(String recordingId, int errorCode, String description) {
        String log = "[SRS][SA] Received Recording error with errorCode:" + errorCode +
                "! Error: " + description;
        toastLogLong(TAG, mContext, log);
        Log.e(TAG, log);
    }

    @Override
    public void onTransferSpeedReceived(String peerId, int mediaDirection, int mediaType, double transferSpeed) {
        String direction = "Send";
        if (Info.MEDIA_DIRECTION_RECV == mediaDirection) {
            direction = "Recv";
        }
        // Log the transfer speeds.
        String log = "[SA][TransSpeed] Transfer speed for Peer " + peerId + ": " +
                Info.getInfoString(mediaType) + " " + direction + " = " + transferSpeed + " kbps";
        Log.d(TAG, log);
    }

    @Override
    public void onWebrtcStatsReceived(String peerId, int mediaDirection, int mediaType, HashMap<String, String> stats) {
        // Log the WebRTC stats.
        StringBuilder log =
                new StringBuilder("[SA][WStatsRecv] Received for Peer " + peerId + ":\r\n");
        for (Map.Entry<String, String> entry : stats.entrySet()) {
            log.append(entry.getKey()).append(": ").append(entry.getValue()).append(".\r\n");
        }
        Log.d(TAG, log.toString());
    }

    @Override
    public void onPermissionRequired(PermRequesterInfo info) {
        mPermissionUtils.onPermissionRequiredHandler(info, TAG, mContext, mMultiVideoCallView.onGetFragment());
    }

    @Override
    public void onPermissionGranted(PermRequesterInfo info) {
        mPermissionUtils.onPermissionGrantedHandler(info, TAG);
    }

    @Override
    public void onPermissionDenied(PermRequesterInfo info) {
        mPermissionUtils.onPermissionDeniedHandler(info, mContext, TAG);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag) {
        mPermissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, tag);
    }

    @Override
    public void onViewResume() {
        // Toggle camera back to previous state if required.
        if (mMultiVideoCallService.isCameraToggle()) {

            if (mMultiVideoCallService.getVideoView(null) != null) {

                mMultiVideoCallService.toggleCamera();

                mMultiVideoCallService.setCamToggle(false);
            }
        }
    }

    @Override
    public void onViewPause() {

        if (mMultiVideoCallService.getVideoView(null) != null) {
            boolean toggleCamera = mMultiVideoCallService.toggleCamera(false);

            mMultiVideoCallService.setCamToggle(toggleCamera);
        }
    }

    private void connectToRoom() {
        //connect to SDK
        if(mMultiVideoCallService.connectToRoom()){
            //get roomName from setting
            String log = "Entering multi party videos room : \"" + Config.ROOM_NAME_PARTY + "\".";
            toastLog(TAG, mContext, log);
        }
    }

    private void updateConnectedUI() {

        // Toggle camera back to previous state if required.
        if (mMultiVideoCallService.isCameraToggle()) {
            processCameraToggle();
        }

        //update UI
        mMultiVideoCallView.onAddSelfView(getVideoView(null));

        String[] remotePeerIds = mMultiVideoCallService.getPeerIdList();

        for (int i = 0; i < remotePeerIds.length; i++) {
            addRemoteView(remotePeerIds[i]);
        }

    }

    private SurfaceViewRenderer getVideoView(String remotePeerId) {
        return mMultiVideoCallService.getVideoView(remotePeerId);
    }

    // If video is enable, toggle video and if video is toggle, then enable it
    private void processCameraToggle() {

        //display instruction log
        String log12 = "Toggled camera ";
        if (getVideoView(null) != null) {
            if (mMultiVideoCallService.toggleCamera()) {
                log12 += "to restarted!";

                //change state of camera toggle
                mMultiVideoCallService.setCamToggle(false);
            } else {
                log12 += "to stopped!";

                mMultiVideoCallService.setCamToggle(true);
            }
        } else {
            log12 += "but failed as local video is not available!";
        }
        toastLog(TAG, mContext, log12);

        //this button don't need to change text
    }

    /**
     * Trigger getWebrtcStats for specific Peer in a loop if current state allows.
     * To stop loop, set {@link #isGettingWebrtcStats} to false.
     *
     * @param peerId
     */
    private void getWStatsAll(final String peerId) {
        Boolean gettingStats = isGettingWebrtcStats.get(peerId);
        if (gettingStats == null) {
            String log = "[SA][WStatsAll] Peer " + peerId +
                    " does not exist. Will not get WebRTC stats.";
            Log.e(TAG, log);
            return;
        }

        if (gettingStats) {
            // Request to get WebRTC stats.
            getWebrtcStats(peerId, Info.MEDIA_DIRECTION_BOTH, Info.MEDIA_ALL);

            // Wait for waitMs ms before requesting WebRTC stats again.
            final int waitMs = 1000;
            new Thread(() -> {
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException e) {
                    String error =
                            "[SA][WStatsAll] Error while waiting to call for WebRTC stats again: " +
                                    e.getMessage();
                    Log.e(TAG, error);
                }
                getWStatsAll(peerId);
            }).start();

        }
    }

    private void getWebrtcStats(String peerId, int mediaDirection, int mediaType) {
        mMultiVideoCallService.getWebrtcStats(peerId, mediaDirection, mediaType);
    }

    private void addRemoteView(String remotePeerId) {

        int index = mMultiVideoCallService.getPeerIndexByPeerId(remotePeerId);

        SurfaceViewRenderer videoView = mMultiVideoCallService.getVideoView(remotePeerId);

        mMultiVideoCallView.onAddRemoteView(index, videoView);
    }
}
