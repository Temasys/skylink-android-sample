package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.content.Context;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

import java.util.concurrent.ConcurrentHashMap;

import sg.com.temasys.skylink.sdk.rtc.Info;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.service.MultiPartyVideoService;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */
public class MultiPartyVideoCallPresenter extends BasePresenter implements MultiPartyVideoCallContract.Presenter {

    private final String TAG = MultiPartyVideoCallPresenter.class.getName();

    public MultiPartyVideoCallContract.View mMultiVideoCallView;
    private MultiPartyVideoService mMultiVideoCallService;
    private PermissionUtils mPermissionUtils;

    private Context mContext;

    // Map with PeerId as key for boolean state
    // that indicates if currently getting WebRTC stats for Peer.
    private static ConcurrentHashMap<String, Boolean> isGettingWebrtcStats =
            new ConcurrentHashMap<String, Boolean>();

    public MultiPartyVideoCallPresenter(Context context) {
        this.mContext = context;
        this.mMultiVideoCallService = new MultiPartyVideoService(mContext);
        this.mMultiVideoCallService.setPresenter(this);
        this.mPermissionUtils = new PermissionUtils();
    }

    public void setView(MultiPartyVideoCallContract.View view) {
        mMultiVideoCallView = view;
        mMultiVideoCallView.setPresenter(this);
    }

    @Override
    public void onViewRequestLayout() {
        Log.d(TAG, "onViewLayoutRequested");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mMultiVideoCallService.isConnectingOrConnected()) {

            //reset permission request states.
            mPermissionUtils.permQReset();

            //connect to room on Skylink connection
            processConnectToRoom();

            //after connected to skylink SDK, UI will be updated latter on AudioService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //if it already connected to room, then resume permission
            mPermissionUtils.permQResume(mContext, mMultiVideoCallView.onPresenterRequestGetFragmentInstance());

            //update UI into connected
            processUpdateConnectedUI();

            Log.d(TAG, "Try to update UI when changing configuration");
        }
    }

    @Override
    public void onViewRequestExit() {
        //process disconnect from room
        mMultiVideoCallService.disconnectFromRoom();
    }

    @Override
    public void onViewRequestSwitchCamera() {
        mMultiVideoCallService.switchCamera();
    }

    @Override
    public boolean onViewRequestStartRecording() {
        return mMultiVideoCallService.startRecording();
    }

    @Override
    public boolean onViewRequestStopRecording() {
        return mMultiVideoCallService.stopRecording();
    }

    @Override
    public String onViewRequestGetRoomPeerIdNick() {
        return mMultiVideoCallService.getRoomPeerIdNick(Constants.CONFIG_TYPE.VIDEO);
    }

    @Override
    public void onViewRequestGetInputVideoResolution() {
        mMultiVideoCallService.getInputVideoResolution();
    }

    @Override
    public void onViewRequestGetSentVideoResolution(int peerIndex) {
        mMultiVideoCallService.getSentVideoResolution(peerIndex);
    }

    @Override
    public void onViewRequestGetReceivedVideoResolution(int peerIndex) {
        mMultiVideoCallService.getReceivedVideoResolution(peerIndex);
    }

    @Override
    public void onViewRequestWebrtcStatsToggle(int peerIndex) {

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
        processGetWStatsAll(peerId);
    }

    @Override
    public void onViewRequestGetTransferSpeeds(int peerIndex, int mediaDirection, int mediaType) {
        mMultiVideoCallService.getTransferSpeeds(peerIndex, mediaDirection, mediaType);
    }

    @Override
    public void onViewRequestRefreshConnection(int peerIndex, boolean iceRestart) {

        mMultiVideoCallService.refreshConnection(peerIndex, iceRestart);
    }

    @Override
    public Boolean onViewRequestGetWebRtcStatsByPeerId(int peerIndex) {

        if(peerIndex >= onViewRequestGetTotalInRoom())
            return false;

        String peerId = mMultiVideoCallService.getPeerIdByIndex(peerIndex);

        return isGettingWebrtcStats.get(peerId);
    }

    @Override
    public int onViewRequestGetTotalInRoom() {
        return mMultiVideoCallService.getTotalInRoom();
    }

    @Override
    public SurfaceViewRenderer onViewRequestGetVideoViewByIndex(int index) {
        return mMultiVideoCallService.getVideoViewByIndex(index);
    }

    @Override
    public void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag) {
        mPermissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, tag);
    }

    @Override
    public void onViewRequestResume() {
        // Toggle camera back to previous state if required.
        if (mMultiVideoCallService.isCameraToggle()) {

            if (mMultiVideoCallService.getVideoView(null) != null) {

                mMultiVideoCallService.toggleCamera();

                mMultiVideoCallService.setCamToggle(false);
            }
        }
    }

    @Override
    public void onViewRequestPause() {

        if (mMultiVideoCallService.getVideoView(null) != null) {
            boolean toggleCamera = mMultiVideoCallService.toggleCamera(false);

            mMultiVideoCallService.setCamToggle(toggleCamera);
        }
    }

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {

        //start audio routing
        SkylinkConfig skylinkConfig = mMultiVideoCallService.getSkylinkConfig();
        if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
            AudioRouter.setPresenter(this);
            AudioRouter.startAudioRouting(mContext, Constants.CONFIG_TYPE.VIDEO);
        }
    }

    @Override
    public void onServiceRequestDisconnect() {

        //stop audio routing
        SkylinkConfig skylinkConfig = mMultiVideoCallService.getSkylinkConfig();
        if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
            AudioRouter.stopAudioRouting(mContext);
        }
    }

    @Override
    public void onServiceRequestLocalMediaCapture(SurfaceViewRenderer videoView) {
        String log = "[SA][onLocalMediaCapture] ";

        if (videoView == null) {
            log += "VideoView is null!";
            Log.d(TAG, log);

            SurfaceViewRenderer selfVideoView = mMultiVideoCallService.getVideoView(null);
            mMultiVideoCallView.onPresenterRequestAddSelfView(selfVideoView);

        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
            mMultiVideoCallView.onPresenterRequestAddSelfView(videoView);
        }
    }

    @Override
    public void onServiceRequestInputVideoResolutionObtained(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        String log = "[SA][VideoResInput] The current video input has width x height, fps: " +
                width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
        toastLogLong(TAG, mContext, log);
    }

    @Override
    public void onServiceRequestReceivedVideoResolutionObtained(String peerId, int width, int height, int fps) {
        String log = "[SA][VideoResRecv] The current video received from Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
        toastLogLong(TAG, mContext, log);
    }

    @Override
    public void onServiceRequestSentVideoResolutionObtained(String peerId, int width, int height, int fps) {
        String log = "[SA][VideoResSent] The current video sent to Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
        toastLogLong(TAG, mContext, log);
    }

    @Override
    public void onServiceRequestRemotePeerJoin(SkylinkPeer skylinkPeer) {

        isGettingWebrtcStats.put(skylinkPeer.getPeerId(), false);

        processAddRemoteView(skylinkPeer.getPeerId());

    }

    @Override
    public void onServiceRequestRemotePeerLeave(String remotePeerId, int removeIndex) {

        isGettingWebrtcStats.remove(remotePeerId);

        mMultiVideoCallView.onPresenterRequestRemoveRemotePeer(removeIndex);
    }

    @Override
    public void onServiceRequestRemotePeerConnectionRefreshed(String log, UserInfo remotePeerUserInfo) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onServiceRequestRemotePeerMediaReceive(String log, UserInfo remotePeerUserInfo, String remotePeerId) {

        processAddRemoteView(remotePeerId);

        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        Log.d(TAG, log);
    }

    @Override
    public void onServiceRequestRecordingVideoLink(String recordingId, String peerId, String videoLink) {
        String peer = " Mixin";
        if (peerId != null) {
            peer = " Peer " + mMultiVideoCallService.getPeerIdNick(peerId) + "'s";
        }
        String msg = "Recording:" + recordingId + peer + " video link:\n" + videoLink;

        mMultiVideoCallView.onPresenterRequestDisplayVideoLinkInfo(recordingId, msg);

    }

    @Override
    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
        mPermissionUtils.onPermissionRequiredHandler(info, TAG, mContext, mMultiVideoCallView.onPresenterRequestGetFragmentInstance());
    }

    private void processConnectToRoom() {

        //connect to SDK
        mMultiVideoCallService.connectToRoom(Constants.CONFIG_TYPE.MULTI_PARTY_VIDEO);

        //get roomName from setting
        String log = "Entering multi party videos room : \"" + Config.ROOM_NAME_PARTY + "\".";
        toastLog(TAG, mContext, log);
    }

    private void processUpdateConnectedUI() {

        // Toggle camera back to previous state if required.
        if (mMultiVideoCallService.isCameraToggle()) {
            processCameraToggle();
        }

        //update UI
        mMultiVideoCallView.onPresenterRequestAddSelfView(processGetVideoView(null));

        String[] remotePeerIds = mMultiVideoCallService.getPeerIdList();

        for (int i = 0; i < remotePeerIds.length; i++) {
            processAddRemoteView(remotePeerIds[i]);
        }

    }

    private SurfaceViewRenderer processGetVideoView(String remotePeerId) {
        return mMultiVideoCallService.getVideoView(remotePeerId);
    }

    // If video is enable, toggle video and if video is toggle, then enable it
    private void processCameraToggle() {

        //display instruction log
        String log12 = "Toggled camera ";
        if (processGetVideoView(null) != null) {
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
    }

    /**
     * Trigger processGetWebrtcStats for specific Peer in a loop if current state allows.
     * To stop loop, set {@link #isGettingWebrtcStats} to false.
     *
     * @param peerId
     */
    private void processGetWStatsAll(final String peerId) {
        Boolean gettingStats = isGettingWebrtcStats.get(peerId);
        if (gettingStats == null) {
            String log = "[SA][WStatsAll] Peer " + peerId +
                    " does not exist. Will not get WebRTC stats.";
            Log.e(TAG, log);
            return;
        }

        if (gettingStats) {
            // Request to get WebRTC stats.
            processGetWebrtcStats(peerId, Info.MEDIA_DIRECTION_BOTH, Info.MEDIA_ALL);

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
                processGetWStatsAll(peerId);
            }).start();

        }
    }

    private void processGetWebrtcStats(String peerId, int mediaDirection, int mediaType) {
        mMultiVideoCallService.getWebrtcStats(peerId, mediaDirection, mediaType);
    }

    private void processAddRemoteView(String remotePeerId) {

        int index = mMultiVideoCallService.getPeerIndexByPeerId(remotePeerId);

        SurfaceViewRenderer videoView = mMultiVideoCallService.getVideoView(remotePeerId);

        mMultiVideoCallView.onPresenterRequestAddRemoteView(index, videoView);
    }
}
