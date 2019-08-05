package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo.MultiPartyVideoCallContract;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkConfig.VideoDevice.CAMERA_FRONT;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_FHD;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_HDR;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_VGA;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for communicating with SkylinkSDK
 */

public class MultiPartyVideoService extends SkylinkCommonService implements MultiPartyVideoCallContract.Service {

    private final String TAG = MultiPartyVideoService.class.getName();

    public MultiPartyVideoService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(MultiPartyVideoCallContract.Presenter presenter) {
        this.presenter = (BasePresenter) presenter;
    }

    /**
     * Sets the specified listeners for multi videos function
     * Video call needs to implement LifeCycleListener, RemotePeerListener, MediaListener, OsListener,
     * RecordingListener, StatsListener
     */
    @Override
    public void setSkylinkListeners() {
        if (mSkylinkConnection != null) {
            // LifeCycleListener for connect, disconnect,.. with room
            mSkylinkConnection.setLifeCycleListener(this);

            //RemotePeerListener for communicate with remote peer(s)
            mSkylinkConnection.setRemotePeerListener(this);

            // MediaListener for media using like audio, video,..
            mSkylinkConnection.setMediaListener(this);

            // OsListener for permission of media
            mSkylinkConnection.setOsListener(this);

            // RecordingListener for recording audio, video
            mSkylinkConnection.setRecordingListener(this);

            // StatsListener for statistics of media
            mSkylinkConnection.setStatsListener(this);
        }
    }

    /**
     * Get the config for multi videos function
     * User can custom video config by using SkylinkConfig
     */
    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // MultiPartyVideoCall config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setHasPeerMessaging(true);
        skylinkConfig.setHasFileTransfer(true);
        skylinkConfig.setMirrorLocalView(true);

        // Allow only 3 remote Peers to join, due to current UI design.
        skylinkConfig.setMaxPeers(3);

        // Set the room size
        skylinkConfig.setRoomSize(SkylinkConfig.RoomSize.MEDIUM);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);

        // Set default camera setting
        SkylinkConfig.VideoDevice videoDevice = Utils.getDefaultVideoDevice();
        if (videoDevice != null) {
            switch (videoDevice) {
                case CAMERA_FRONT:
                    skylinkConfig.setDefaultVideoDevice(SkylinkConfig.VideoDevice.CAMERA_FRONT);
                    break;
                case CAMERA_BACK:
                    skylinkConfig.setDefaultVideoDevice(SkylinkConfig.VideoDevice.CAMERA_BACK);
                    break;
                case SCREEN:
                    skylinkConfig.setDefaultVideoDevice(SkylinkConfig.VideoDevice.SCREEN);
                    break;
            }
        }

        //Set default video resolution setting
        String videoResolution = Utils.getDefaultVideoResolution();
        if (videoResolution.equals(VIDEO_RESOLUTION_VGA)) {
            skylinkConfig.setVideoWidth(SkylinkConfig.VIDEO_WIDTH_VGA);
            skylinkConfig.setVideoHeight(SkylinkConfig.VIDEO_HEIGHT_VGA);
        } else if (videoResolution.equals(VIDEO_RESOLUTION_HDR)) {
            skylinkConfig.setVideoWidth(SkylinkConfig.VIDEO_WIDTH_HDR);
            skylinkConfig.setVideoHeight(SkylinkConfig.VIDEO_HEIGHT_HDR);
        } else if (videoResolution.equals(VIDEO_RESOLUTION_FHD)) {
            skylinkConfig.setVideoWidth(SkylinkConfig.VIDEO_WIDTH_FHD);
            skylinkConfig.setVideoHeight(SkylinkConfig.VIDEO_HEIGHT_FHD);
        }

        return skylinkConfig;
    }

    /**
     * Stop or restart the local camera given that the local video source is available,
     * i.e., had been started and not removed.
     * When camera is toggled to stopped, it is accessible by other apps, for e.g.
     * it can be used to take pictures.
     * Trigger {@link SkylinkCommonService#onWarning(int, String)} if an error occurs, for e.g. with:
     * errorCode {@link sg.com.temasys.skylink.sdk.rtc.Errors#VIDEO_UNABLE_TO_SWITCH_CAMERA_ERROR}
     * if local video source is not available.
     */
    public void toggleCamera(String mediaId) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.toggleVideo(mediaId);
    }

    /**
     * Stop or restart the local camera based on the parameter |toRestart|,
     * <p>
     * Trigger {@link SkylinkCommonService#onWarning(int, String)} if an error occurs, for e.g. with:
     * errorCode {@link sg.com.temasys.skylink.sdk.rtc.Errors#VIDEO_UNABLE_TO_SWITCH_CAMERA_ERROR}
     * if local video source is not available.
     *
     * @param toRestart true if restart camera, false if stop camera
     * @return True if camera state had changed, false if not.
     */
    public void toggleCamera(String mediaId, boolean toRestart) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.toggleVideo(mediaId, toRestart);
    }

    /**
     * Call this method to switch between available camera.
     * Outcome of operation delivered via callback at
     * {@link SkylinkCommonService#onReceiveLog(int, String)}
     * with 2 possible Info:
     * -- Info.CAM_SWITCH_FRONT (successfully switched to the front camera)
     * -- Info.CAM_SWITCH_NON_FRONT (successfully switched to a back camera)
     */
    public void switchCamera() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.switchCamera();
        }
    }

    public void startLocalAudio() {
        if (mSkylinkConnection == null) {
            initializeSkylinkConnection(Constants.CONFIG_TYPE.AUDIO);
        }

        //Start audio.
        if (mSkylinkConnection != null) {
            mSkylinkConnection.startLocalMedia(SkylinkConfig.AudioDevice.MICROPHONE, null);
        }
    }

    public void startLocalVideo() {
        if (mSkylinkConnection == null) {
            initializeSkylinkConnection(Constants.CONFIG_TYPE.VIDEO);
        }

        // start custom camera if default video device setting is custom device
        if (Utils.isDefaultCustomVideoDeviceSetting()) {
            startLocalCustomVideo();
            return;
        }

        //Start audio.
        if (mSkylinkConnection != null) {

            // Get default setting for videoDevice
            SkylinkConfig.VideoDevice videoDevice = Utils.getDefaultVideoDevice();

            // If user select back camera as default video device, start back camera
            // else start front camera as default
            if (videoDevice == SkylinkConfig.VideoDevice.CAMERA_BACK) {
                mSkylinkConnection.startLocalMedia(SkylinkConfig.VideoDevice.CAMERA_BACK, null);
            } else {
                mSkylinkConnection.startLocalMedia(SkylinkConfig.VideoDevice.CAMERA_FRONT, null);
            }
        }
    }

    public void toggleVideo(boolean isRestart) {
        if (mSkylinkConnection != null && localVideoId != null) {
            mSkylinkConnection.toggleVideo(localVideoId, isRestart);
        }
    }

    public void startLocalScreen() {
        if (mSkylinkConnection == null) {
            initializeSkylinkConnection(Constants.CONFIG_TYPE.SCREEN_SHARE);
        }

        //Start audio.
        if (mSkylinkConnection != null) {

            SkylinkConfig.VideoDevice videoDevice = SkylinkConfig.VideoDevice.SCREEN;
            //Start video.
            mSkylinkConnection.startLocalMedia(videoDevice, null);
        }
    }

    public void startLocalCustomVideo() {
        // create a new custom video capturer to input for the method
        VideoCapturer customVideoCapturer = Utils.createCustomVideoCapturerFromCamera(
                CAMERA_FRONT, mSkylinkConnection);
        if (customVideoCapturer != null) {
            mSkylinkConnection.startLocalMedia(CAMERA_FRONT, customVideoCapturer);
        }
    }

    public void toggleScreen() {
        if (mSkylinkConnection != null && localScreenSharingId != null)
            mSkylinkConnection.toggleVideo(localScreenSharingId);
        else
            startLocalScreen();
    }

    public void disposeLocalMedia() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.disposeLocalMedia();
        }

        clearInstance();
    }

    /**
     * Start recording with conditions:
     * - We must be using Skylink Media Relay (SMR key)
     * - Recording should not be already started.
     * - We should not have just tried to start recording.
     * Actual start of recording will be notified via relevant callback on
     * {@link BasePresenter#onServiceRequestRecordingStart(Context, boolean)}
     *
     * @return True if recording is successful, false otherwise.
     */
    public boolean startRecording() {
        if (mSkylinkConnection != null) {
            boolean success = mSkylinkConnection.startRecording();

            String log = "[SRS][SA] startRecording=" + success +
                    ", isRecording=" + mSkylinkConnection.isRecording() + ".";
            toastLog(TAG, context, log);
            return success;
        }

        return false;
    }

    /**
     * Stop recording with conditions:
     * - We must be already be recording.
     * - We should not have just tried to stop recording.
     * Actual stop of recording will be notified via relevant callback on
     * {@link BasePresenter#onServiceRequestRecordingStop(Context, boolean)}
     *
     * @return True if recording is successful, false otherwise.
     */
    public boolean stopRecording() {
        if (mSkylinkConnection != null) {
            boolean success = mSkylinkConnection.stopRecording();

            String log = "[SRS][SA] stopRecording=" + success +
                    ", isRecording=" + mSkylinkConnection.isRecording() + ".";
            toastLog(TAG, context, log);
            return success;
        }

        return false;
    }

    /**
     * Get the current resolution of the input video being captured by the local camera
     * and the SkylinkCaptureFormat used.
     * If resolution is available, it will be returned in
     * {@link BasePresenter#onServiceRequestInputVideoResolutionObtained(SkylinkMedia.MediaType mediaType, int, int, int, SkylinkCaptureFormat)}.
     * Note:
     * - Resolution may not always be available, e.g. if no video is captured.
     * - This might be different from the resolution of the video actually sent to Peers as
     * SkylinkSDK may adjust resolution dynamically to try to match its bandwidth criteria.
     */
    public void getInputVideoResolution() {
        if (mSkylinkConnection != null && localVideoId != null) {
            mSkylinkConnection.getInputVideoResolutionByVideoId(localVideoId);
        }
    }

    /**
     * Get the current resolution of the video being sent to a specific Peer.
     * If resolution is available, it will be returned in
     * {@link BasePresenter#onServiceRequestSentVideoResolutionObtained(String, SkylinkMedia.MediaType mediaType, int, int, int)}
     *
     * @param peerIndex Index of the remote Peer in frame from whom we want to get sent video resolution.
     *                  Use -1 to get sent video resolutions of all connected remote Peers.
     * @param videoType Type of the SkylinkMedia video object that to get video resolution
     */
    public void getSentVideoResolution(int peerIndex, SkylinkMedia.MediaType videoType) {
        if (mSkylinkConnection != null) {
            if (peerIndex == -1) {
                mSkylinkConnection.getSentVideoResolutionByVideoType(null, videoType);
            } else {
                mSkylinkConnection.getSentVideoResolutionByVideoType(mPeersList.get(peerIndex).getPeerId(), videoType);
            }
        }
    }

    /**
     * Get the current resolution of the video received from a specific Peer's index.
     * If resolution is available, it will be returned in
     * {@link BasePresenter#onServiceRequestReceivedVideoResolutionObtained(String, SkylinkMedia.MediaType, int, int, int)}
     *
     * @param peerIndex Index of the remote Peer in frame from whom we want to get received video resolution.
     *                  Use -1 to get received video resolutions of all connected remote Peers.
     * @param videoType type of the SkylinkMedia video object to get received video resolution
     */
    public void getReceivedVideoResolution(int peerIndex, SkylinkMedia.MediaType videoType) {
        if (mSkylinkConnection != null) {
            if (peerIndex == -1) {
                mSkylinkConnection.getReceivedVideoResolutionByVideoType(null, videoType);
            } else {
                mSkylinkConnection.getReceivedVideoResolutionByVideoType(mPeersList.get(peerIndex).getPeerId(), videoType);
            }
        }
    }

    /**
     * Request for WebRTC statistics of the specified media stream.
     * Results will be reported via
     * {@link BasePresenter#onServiceRequestWebrtcStatsReceived(String, int, int, String, HashMap)}
     *
     * @param peerId         PeerId of the remote Peer for which we are getting stats on.
     * @param mediaId        id of the local media object to get resolution
     *                       input null as current local media if there is only 1 local media at the moment
     *                       OR the camera video as default video if there are more than 1 local video at the moment.
     *                       OR the default audio if there are more than 1 local audio at the moment
     * @param mediaDirection Integer that defines the direction of media stream(s) reported on, such as
     *                       {@link sg.com.temasys.skylink.sdk.rtc.Info#MEDIA_DIRECTION_SEND sending},
     *                       {@link sg.com.temasys.skylink.sdk.rtc.Info#MEDIA_DIRECTION_RECV receiving} or
     *                       {@link sg.com.temasys.skylink.sdk.rtc.Info#MEDIA_DIRECTION_BOTH both}.
     * @param mediaType      Integer that defines the type(s) of media reported on such as
     *                       {@link sg.com.temasys.skylink.sdk.rtc.Info#MEDIA_AUDIO audio},
     *                       {@link sg.com.temasys.skylink.sdk.rtc.Info#MEDIA_VIDEO video} or
     *                       {@link sg.com.temasys.skylink.sdk.rtc.Info#MEDIA_ALL all}.
     */
    public void getWebrtcStats(String peerId, String mediaId, int mediaDirection, int mediaType) {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.getWebrtcStats(peerId, mediaId, mediaDirection, mediaType);
        }
    }

    /**
     * Request for the instantaneous transfer speed(s) of media stream(s), at the moment of request.
     * Results will be reported via
     * {@link BasePresenter#onServiceRequestTransferSpeedReceived(String, String, int, int, double)}
     *
     * @param peerIndex      Index of the remote Peer in frame for which we are getting transfer speed on.
     * @param mediaId        id of the local media object to get resolution
     *                       input null as current local media if there is only 1 local media at the moment
     *                       OR the camera video as default video if there are more than 1 local video at the moment.
     *                       OR the default audio if there are more than 1 local audio at the moment
     * @param mediaDirection Integer that defines the direction of media stream(s) reported on, such as
     *                       {@link sg.com.temasys.skylink.sdk.rtc.Info#MEDIA_DIRECTION_SEND sending},
     *                       {@link sg.com.temasys.skylink.sdk.rtc.Info#MEDIA_DIRECTION_RECV receiving} or
     *                       {@link sg.com.temasys.skylink.sdk.rtc.Info#MEDIA_DIRECTION_BOTH both}.
     * @param mediaType      Integer that defines the type(s) of media reported on such as
     *                       {@link sg.com.temasys.skylink.sdk.rtc.Info#MEDIA_AUDIO audio},
     *                       {@link sg.com.temasys.skylink.sdk.rtc.Info#MEDIA_VIDEO video} or
     *                       {@link sg.com.temasys.skylink.sdk.rtc.Info#MEDIA_ALL all}.
     */
    public void getTransferSpeeds(int peerIndex, String mediaId, int mediaDirection, int mediaType) {

        String peerId = mPeersList.get(peerIndex).getPeerId();

        if (peerId == null)
            return;

        if (mSkylinkConnection != null) {
            mSkylinkConnection.getTransferSpeeds(peerId, mediaId, mediaDirection, mediaType);
        }
    }

    /**
     * Refreshes a connection with a specific peer or all peer(s) connections
     * Able to indicate preference for ICE restart.
     *
     * @param peerIndex  Index of the remote peer in frame to whom we will refresh connection. Use -1 if
     *                   refresh is to be done with all our remote peers in the room.
     * @param iceRestart Specify if ICE restart should be performed. ICE restart is recommended
     *                   if network conditions had changed, for e.g. a different network is used.
     */
    public void refreshConnection(int peerIndex, boolean iceRestart) {

        if (mSkylinkConnection == null)
            return;

        String peerStr = "All peers ";

        //list of peers that are failed for refreshing
        String[] failedPeers = null;

        if (peerIndex == -1) {
            failedPeers = mSkylinkConnection.refreshConnection(null, iceRestart);
        } else {
            SkylinkPeer peer = mPeersList.get(peerIndex);
            failedPeers = mSkylinkConnection.refreshConnection(peer.getPeerId(), iceRestart);
            peerStr = "Peer " + peer.getPeerName();
        }

        String log = "Refreshing connection for " + peerStr;
        if (iceRestart) {
            log += " with ICE restart.";
        } else {
            log += ".";
        }
        toastLog(TAG, context, log);

        // Log errors if any.
        if (failedPeers != null) {
            log = "Unable to refresh ";
            if ("".equals(failedPeers[0])) {
                log += "as there is no Peer in the room!";
            } else {
                log += "for Peer(s): " + Arrays.toString(failedPeers) + "!";
            }
            toastLog(TAG, context, log);
        }
    }

    /**
     * Return the video view of Peer whose PeerId was provided.
     * If peerId is null, local video view will be returned.
     *
     * @param remotePeerId PeerId of the Peer whose videoView to be returned.
     * @return Video View of Peer or null if none present.
     */
    public SurfaceViewRenderer getVideoView(String remotePeerId, String mediaId) {
        if (mSkylinkConnection != null && mediaId != null) {
            SkylinkMedia media = mSkylinkConnection.getSkylinkMedia(remotePeerId, mediaId);
            if (media != null) {
                return media.getVideoView();
            }
        }

        return null;
    }

    /**
     * Return the video view of Peer whose PeerId was provided.
     * If peerId is null, local video view will be returned.
     *
     * @param remotePeerId PeerId of the Peer whose videoView to be returned.
     * @return Video View of Peer or null if none present.
     */
    public SurfaceViewRenderer getVideoView(String remotePeerId, SkylinkMedia.MediaType
            mediaType, boolean isLocal) {
        if (mSkylinkConnection == null) {
            return null;
        }

        List<SkylinkMedia> mediaList = null;

        if (isLocal)
            mediaList = mSkylinkConnection.getSkylinkMediaListLocal(mediaType);
        else
            mediaList = mSkylinkConnection.getSkylinkMediaListRemote(remotePeerId, mediaType);

        return mediaList.get(0).getVideoView();
    }

    /**
     * Return the video view of Peer whose peerIndex was provided.
     * If peerIndex is -1, local video view will be returned.
     * Return null if:
     * - peerIndex is not in peerList.
     * - No video view exists for given PeerIndex.
     *
     * @param peerIndex index of the Peer whose videoView to be returned.
     * @return Video View of Peer or null if none present.
     */
    public SurfaceViewRenderer getVideoViewByIndex(int peerIndex) {
        if (peerIndex == -1) {
            return getVideoView(null, SkylinkMedia.MediaType.VIDEO, true);
        }

        if (mSkylinkConnection != null && peerIndex < mPeersList.size()) {
            SkylinkPeer skylinkPeer = mPeersList.get(peerIndex);
            if (skylinkPeer.getMediaIds() != null) {
                for (int i = 0; i < skylinkPeer.getMediaIds().size(); i++) {
                    // return the first video view of the remote peer
                    Map<String, SkylinkMedia.MediaType> mediaIds = skylinkPeer.getMediaIds();

                    if (mediaIds == null || mediaIds.size() == 0) {
                        return null;
                    }

                    String trackId = null;
                    for (String key : mediaIds.keySet()) {
                        if (mediaIds.get(key) == SkylinkMedia.MediaType.VIDEO) {
                            trackId = key;
                        }
                    }

                    return getVideoView(skylinkPeer.getPeerId(), trackId);
                }
            }
        }

        return null;
    }

    /**
     * Get total number of peers in room including local peer
     *
     * @return number of peer(s) or 0 if nothing in room.
     */
    public int getTotalInRoom() {
        if (mPeersList != null)
            return mPeersList.size();

        return 0;
    }

    /**
     * Get list of remote peer id in room using SkylinkConnection API.
     *
     * @return list of peerId or null if not available.
     */
    public String[] getPeerIdList() {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getPeerIdList();
        }

        return null;
    }

    /**
     * Get index of the peer whose peerId is provided
     * Return -1 if:
     * - No peer exists for given id.
     *
     * @return index in frame of the peer
     */
    public int getPeerIndexByPeerId(String peerId) {
        for (int i = 0; i < mPeersList.size(); i++) {
            SkylinkPeer peer = mPeersList.get(i);

            if (peer.getPeerId().equals(peerId)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Get id of the peer in specific index
     * Return null if:
     * - peerIndex is not in peerList.
     * - No peer exists for given index.
     *
     * @return id of peer
     */
    public String getPeerIdByIndex(int peerIndex) {
        if (mSkylinkConnection != null && peerIndex < mPeersList.size()) {
            return mPeersList.get(peerIndex).getPeerId();
        }

        return null;
    }

    /**
     * Get the info of a peer in specific index
     */
    public SkylinkPeer getPeerByIndex(int index) {
        return mPeersList.get(index);
    }

    public String localAudioId() {
        return localAudioId;
    }

    public String getLocalVideoId() {
        return localVideoId;
    }

    public String getLocalScreenId() {
        return localScreenSharingId;
    }

}