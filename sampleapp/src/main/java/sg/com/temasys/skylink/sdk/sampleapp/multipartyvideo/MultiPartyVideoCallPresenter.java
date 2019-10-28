package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkInfo;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.MultiPartyVideoService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for processing multi videos call logic
 */
public class MultiPartyVideoCallPresenter extends BasePresenter implements MultiPartyVideoCallContract.Presenter {

    private final String TAG = MultiPartyVideoCallPresenter.class.getName();

    // view instance
    public MultiPartyVideoCallContract.View multiVideoCallView;

    // Service helps to work with SkylinkSDK
    private MultiPartyVideoService multiVideoCallService;

    //Permission helps to process media runtime permission
    private PermissionUtils permissionUtils;

    private Context context;

    // Map with PeerId as key for boolean state
    // that indicates if currently getting WebRTC stats for Peer.
    private ConcurrentHashMap<String, Boolean> isGettingWebrtcStats =
            new ConcurrentHashMap<String, Boolean>();

    public MultiPartyVideoCallPresenter(Context context) {
        this.context = context;
        this.multiVideoCallService = new MultiPartyVideoService(this.context);
        this.multiVideoCallService.setPresenter(this);
        this.permissionUtils = new PermissionUtils();
    }

    public void setView(MultiPartyVideoCallContract.View view) {
        multiVideoCallView = view;
        multiVideoCallView.setPresenter(this);
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for view to call
    // These methods are responsible for processing requests from view
    //----------------------------------------------------------------------------------------------

    @Override
    public void onViewRequestConnectedLayout() {
        Log.d(TAG, "[onViewRequestConnectedLayout]");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!multiVideoCallService.isConnectingOrConnected()) {

            //reset permission request states.
            permissionUtils.permQReset();

            //connect to room on Skylink connection
            processConnectToRoom();

            //after connected to skylink SDK, UI will be updated later on onServiceRequestConnect

            Log.d(TAG, "Try to connect when entering room");

        }
    }

    @Override
    public void onViewRequestStartLocalMediaIfConfigAllow() {
        String log = "[SA][onViewRequestStartLocalMediaIfConfigAllow] ";
        if (Utils.isDefaultNoneVideoDeviceSetting()) {
            log += " Default video device setting is No device. So do not start any local media automatically! ";
            Log.w(TAG, log);
            return;
        }

        // start local audio
        multiVideoCallService.createLocalAudio();

        // check the default setting for video device and start local video accordingly
        if (Utils.isDefaultCameraDeviceSetting()) {
            multiVideoCallService.createLocalVideo();
            return;
        }

        if (Utils.isDefaultScreenDeviceSetting()) {
            multiVideoCallService.createLocalScreen();
            return;
        }

        // we create a custom video device from back camera of the device, so start custom video device
        // will similarly start back camera
        if (Utils.isDefaultCustomVideoDeviceSetting()) {
            multiVideoCallService.createLocalCustomVideo();
            return;
        }
    }

    @Override
    public void onViewRequestStopScreenSharing() {
        multiVideoCallService.toggleScreen(false);
    }

    @Override
    public void onViewRequestExit() {
        //process disconnect from room
        multiVideoCallService.disconnectFromRoom();
        multiVideoCallService.disposeLocalMedia();

        //after disconnected from skylink SDK, UI will be updated latter on onServiceRequestDisconnect
    }

    @Override
    public void onViewRequestResume() {
        // restart camera to continue capturing when resume
        multiVideoCallService.toggleVideo(true);
    }

    @Override
    public void onViewRequestPause() {
        //stop camera when pausing so that camera will be available for the others to use
        // just in case that user are not sharing screen

        multiVideoCallService.toggleVideo(false);
    }

    @Override
    public void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, TAG);
    }

    @Override
    public void onViewRequestSwitchCamera() {
        //change to back/front camera by SkylinkSDK
        multiVideoCallService.switchCamera();
    }

    @Override
    public boolean onViewRequestStartRecording() {
        // start recording the view call by SkylinkSDK (with SMR key)
        return multiVideoCallService.startRecording();
    }

    @Override
    public boolean onViewRequestStopRecording() {
        // stop recording the view call by SkylinkSDK (with SMR key)
        return multiVideoCallService.stopRecording();
    }

    @Override
    public void onViewRequestGetInputVideoResolution() {
        // get local video resolution by SkylinkSDK
        multiVideoCallService.getInputVideoResolution();
    }

    @Override
    public void onViewRequestGetSentVideoResolution(int peerIndex) {
        // get video resolution sent to remote peer(s)
        multiVideoCallService.getSentVideoResolution(peerIndex, SkylinkMedia.MediaType.VIDEO_CAMERA);
    }

    @Override
    public void onViewRequestGetReceivedVideoResolution(int peerIndex) {
        //get received video resolution from remote peer(s)
        multiVideoCallService.getReceivedVideoResolution(peerIndex, SkylinkMedia.MediaType.VIDEO_CAMERA);
    }

    @Override
    public void onViewRequestWebrtcStatsToggle(int peerIndex) {

        String peerId = multiVideoCallService.getPeerIdByIndex(peerIndex);

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
        processGetWStatsAll(peerIndex);
    }

    @Override
    public void onViewRequestGetTransferSpeeds(int peerIndex, SkylinkMedia.MediaType mediaType, boolean forSending) {
        multiVideoCallService.getTransferSpeeds(peerIndex, mediaType, forSending);
    }

    @Override
    public void onViewRequestRefreshConnection(int peerIndex, boolean iceRestart) {
        multiVideoCallService.refreshConnection(peerIndex, iceRestart);
    }

    @Override
    public String onViewRequestGetRoomIdAndNickname() {
        //get id and nickname of the room by SkylinkSDK
        return multiVideoCallService.getRoomIdAndNickname(Constants.CONFIG_TYPE.MULTI_PARTY_VIDEO);
    }

    @Override
    public int onViewRequestGetTotalInRoom() {
        //get total peers in room (include local peer)
        return multiVideoCallService.getTotalInRoom();
    }

    @Override
    public List<SurfaceViewRenderer> onViewRequestGetVideoViewByIndex(int index) {
        return multiVideoCallService.getVideoViewByIndex(index);
    }

    /**
     * Get the specific peer object according to the index
     */
    @Override
    public SkylinkPeer onViewRequestGetPeerByIndex(int index) {
        return multiVideoCallService.getPeerByIndex(index);
    }

    @Override
    public Boolean onViewRequestGetWebRtcStatsState(int peerIndex) {
        SkylinkPeer peer = onViewRequestGetPeerByIndex(peerIndex);
        if (peer != null)
            return isGettingWebrtcStats.get(peer.getPeerId());
        return null;
    }

    @Override
    public void onViewRequestStartAudio() {
        multiVideoCallService.createLocalAudio();
    }

    @Override
    public void onViewRequestStartVideo() {
        multiVideoCallService.createLocalVideo();
    }

    @Override
    public void onViewRequestStartVideoCustom() {
        multiVideoCallService.createLocalCustomVideo();
    }

    @Override
    public void onViewRequestStartVideoCamera() {
        multiVideoCallService.createLocalVideo();
    }

    @Override
    public void onViewRequestStartVideoScreen() {
        multiVideoCallService.createLocalScreen();
    }

    @Override
    public void onViewRequestStartSecondVideoView() {
        if (!Utils.isDefaultScreenDeviceSetting()) {
            // start screen sharing
            multiVideoCallService.createLocalScreen();
        } else {
            // start front camera
            multiVideoCallService.createLocalVideo();
        }
    }

    @Override
    public void onViewRequestActivityResult(int requestCode, int resultCode, Intent data) {
        permissionUtils.onRequestActivityResultHandler(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            //show the stop screen share button
            permissionUtils.requestButtonOverlayPermission(context,
                    multiVideoCallView.onPresenterRequestGetFragmentInstance());
        }

        // display overlay button if permission is grant
        // or warning dialog if permission is deny
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(context)) {
                multiVideoCallView.onPresenterRequestShowButtonStopScreenSharing();
            } else {
                permissionUtils.displayOverlayButtonPermissionWarning(context);
            }
        }
    }

    @Override
    public void onServiceRequestIntentRequired(Intent intent, int requestCode, SkylinkInfo skylinkInfo) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onIntentRequiredHandler(intent, requestCode, skylinkInfo, (Activity) context);
    }


    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful) {
            multiVideoCallView.onPresenterRequestUpdateUIConnected(processGetRoomId());
        }
    }

    @Override
    public void onServiceRequestDisconnect() {
        //stop audio routing
        SkylinkConfig skylinkConfig = multiVideoCallService.getSkylinkConfig();
        if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
            AudioRouter.stopAudioRouting(context);
        }
    }

    @Override
    public void onServiceRequestLocalAudioCapture(SkylinkMedia localAudio) {
        toastLog("[SA][onServiceRequestLocalAudioCapture]", context, "Local audio is on with id = " + localAudio.getMediaId());

        //start audio routing if has audio config
        SkylinkConfig skylinkConfig = multiVideoCallService.getSkylinkConfig();
        if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
            AudioRouter.setPresenter(this);
            AudioRouter.startAudioRouting(context, Constants.CONFIG_TYPE.VIDEO);

            // Turn on speaker by the default
            AudioRouter.turnOnSpeaker();
        }
    }

    @Override
    public void onServiceRequestLocalCameraCapture(SkylinkMedia localVideo) {
        String log = "[SA][onServiceRequestLocalCameraCapture] ";

        if (localVideo.getVideoView() == null) {
            log += "VideoView is null! Try to get video view from the SDK";
            Log.d(TAG, log);

            // Able to get video view from SDK
            SurfaceViewRenderer selfVideoView = multiVideoCallService.getVideoViewById(localVideo.getMediaId());
            multiVideoCallView.onPresenterRequestAddSelfView(selfVideoView, localVideo.getMediaType());

        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
            multiVideoCallView.onPresenterRequestAddSelfView(localVideo.getVideoView(), localVideo.getMediaType());
        }
    }

    @Override
    public void onServiceRequestLocalScreenCapture(SkylinkMedia localVideo) {
        String log = "[SA][onServiceRequestLocalScreenCapture] ";

        if (localVideo.getVideoView() == null) {
            log += "VideoView is null!";
            Log.d(TAG, log);

            SurfaceViewRenderer selfVideoView = multiVideoCallService.getVideoViewById(localVideo.getMediaId());
            multiVideoCallView.onPresenterRequestAddSelfView(selfVideoView, localVideo.getMediaType());

        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
            multiVideoCallView.onPresenterRequestAddSelfView(localVideo.getVideoView(), localVideo.getMediaType());
        }
    }

    @Override
    public void onServiceRequestRemotePeerJoin(SkylinkPeer skylinkPeer) {
        // add new peer button in action bar
        multiVideoCallView.onPresenterRequestChangeUiRemotePeerJoin(skylinkPeer, multiVideoCallService.getTotalPeersInRoom() - 2);

        // add new webRTCStats for peer
        isGettingWebrtcStats.put(skylinkPeer.getPeerId(), false);
    }

    @Override
    public void onServiceRequestRemotePeerLeave(SkylinkPeer remotePeer, int removeIndex) {
        // do not process if the left peer is local peer
        if (removeIndex == -1 || remotePeer == null)
            return;

        // Remove the peer in button in custom bar
        multiVideoCallView.onPresenterRequestChangeUIRemotePeerLeft(removeIndex, multiVideoCallService.getPeersList());

        // remove the   webRtStats of the peer
        isGettingWebrtcStats.remove(remotePeer.getPeerId());

        // remote the remote peer video view
        multiVideoCallView.onPresenterRequestRemoveRemotePeer(removeIndex);
    }

    @Override
    public void onServiceRequestRemotePeerVideoReceive(String remotePeerId, SkylinkMedia remoteMedia) {
        processAddRemoteView(remotePeerId, remoteMedia.getMediaType(), remoteMedia.getVideoView());
    }

    @Override
    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
        permissionUtils.onPermissionRequiredHandler(info, TAG, context, multiVideoCallView.onPresenterRequestGetFragmentInstance());
    }

    @Override
    public void onServiceRequestChangeDefaultVideoDevice(SkylinkConfig.VideoDevice videoDevice) {
        multiVideoCallView.onPresenterRequestChangeDefaultVideoDevice(videoDevice);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Process connect to room on service layer and update UI accordingly
     */
    private void processConnectToRoom() {
        //get roomName from setting
        String log = "Entering multi party videos room : \"" + Config.ROOM_NAME_PARTY + "\".";
        toastLog(TAG, context, log);

        //connect to SkylinkSDK
        multiVideoCallService.connectToRoom(Constants.CONFIG_TYPE.MULTI_PARTY_VIDEO);
    }

    /**
     * Trigger processGetWebrtcStats for specific Peer in a loop if current state allows.
     * To stop loop, set {@link #isGettingWebrtcStats} to false.
     *
     * @param peerIndex the index of the remote peer to get the stats
     */
    private void processGetWStatsAll(final int peerIndex) {
        String peerId = multiVideoCallService.getPeerId(peerIndex);
        Boolean gettingStats = isGettingWebrtcStats.get(peerId);
        if (gettingStats == null) {
            String log = "[SA][WStatsAll] Peer " + peerId +
                    " does not exist. Will not get WebRTC stats.";
            Log.e(TAG, log);
            return;
        }

        if (gettingStats) {
            // Request to get WebRTC stats.
            processGetWebrtcStats(peerIndex);

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
                processGetWStatsAll(peerIndex);
            }).start();
        }
    }

    private void processGetWebrtcStats(int peerIndex) {
        multiVideoCallService.getWebrtcStats(peerIndex);
    }

    private void processAddRemoteView(String remotePeerId, SkylinkMedia.MediaType mediaType, SurfaceViewRenderer videoView) {

        int index = multiVideoCallService.getPeerIndexByPeerId(remotePeerId) - 1;

        multiVideoCallView.onPresenterRequestAddRemoteView(index, mediaType, videoView);
    }

    /**
     * Get the room id info
     */
    private String processGetRoomId() {
        return multiVideoCallService.getRoomId();
    }
}
