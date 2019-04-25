package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoCallContract;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionContract;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_FHD;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_HDR;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_VGA;

/**
 * Created by muoi.pham on 20/07/18.
 * The service class is responsible for communicating with the SkylinkSDK API by using SkylinkConnection instance
 */

public class VideoService extends SkylinkCommonService implements VideoCallContract.Service {

    public VideoService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(VideoCallContract.Presenter presenter) {
        this.presenter = (BasePresenter) presenter;
    }

    @Override
    public void setResPresenter(VideoResolutionContract.Presenter videoResPresenter) {
        this.videoResPresenter = (BasePresenter) videoResPresenter;
    }

    /**
     * Stop or restart the local camera based on the parameter |isToggle|,
     * given that the local video source is available, i.e., had been started and not removed.
     * However, if the intended state of the camera (started or stopped) is already the current
     * state, then no change will be effected.
     * Trigger LifeCycleListener.onWarning if an error occurs, for example:
     * if local video source is not available.
     *
     * @return True if camera state had changed, false if not.
     */
    public boolean toggleCamera(String mediaId, boolean isToggle) {
        if (mSkylinkConnection != null)
            return mSkylinkConnection.toggleCamera(mediaId, !isToggle);
        return false;
    }

    /**
     * Mutes the local user's audio and notifies all the peers in the room.
     *
     * @param audioMuted Flag that specifies whether audio should be mute
     */
    public void muteLocalAudio(boolean audioMuted) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.muteLocalAudio(null, audioMuted);
    }

    /**
     * Mutes the local user's video and notifies all the peers in the room.
     * Note that black video frames (consuming bandwidth) are still being sent to remote Peer(s).
     *
     * @param videoMuted Flag that specifies whether video should be mute
     */
    public void muteLocalVideo(boolean videoMuted) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.muteLocalVideo(null, videoMuted);
    }

    /**
     * Return the video view of Peer whose PeerId was provided.
     * If peerId is null, local video view will be returned.
     * Return null if:
     * - No video view exists for given PeerId.
     * - Including if given PeerId does not exist.
     *
     * @param peerId Id of the Peer whose videoView to be returned.
     * @return Video View of Peer or null if none present.
     */
    public SurfaceViewRenderer getVideoView(String peerId, String mediaId) {
        if (mSkylinkConnection != null)
            return mSkylinkConnection.getVideoView(peerId, mediaId);

        return null;
    }

    /**
     * Get the input/sent/received video resolution of a specified peer
     * Note:
     * - Resolution may not always be available, e.g. if no video is captured.
     * - If resolution are available, they will be returned in
     * {@link SkylinkCommonService#onInputVideoResolutionObtained} for input video resolution
     * {@link SkylinkCommonService#onReceivedVideoResolutionObtained} for received video resolution
     * {@link SkylinkCommonService#onSentVideoResolutionObtained} for sent video resolution
     */
    public void getVideoResolutions(String peerId) {
        if (mSkylinkConnection == null) {
            return;
        }

        mSkylinkConnection.getInputVideoResolution();

        if (peerId != null) {
            mSkylinkConnection.getSentVideoResolution(peerId);
            mSkylinkConnection.getReceivedVideoResolution(peerId);
        }
    }

    /**
     * Call this method to switch between available camera.
     * Outcome of operation delivered via callback at SkylinkCommonService.onReceiveLog,
     * with 3 possible Info:
     * -- Info.CAM_SWITCH_FRONT (successfully switched to the front camera)
     * -- Info.CAM_SWITCH_NON_FRONT (successfully switched to a non front camera/back camera)
     * -- Info.CAM_SWITCH_NO (camera could not be switched)
     */
    public void switchCamera() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.switchCamera();
        }
    }

    /**
     * Change the speaker output to on/off
     * The speaker is automatically turned off when audio bluetooth is connected.
     */
    public void changeSpeakerOutput(boolean isSpeakerOn) {
        AudioRouter.changeAudioOutput(context, isSpeakerOn);
    }

    /**
     * Sets the specified listeners for video function
     * Video call needs to implement LifeCycleListener, RemotePeerListener, MediaListener, OsListener
     */
    @Override
    public void setSkylinkListeners() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.setLifeCycleListener(this);
            mSkylinkConnection.setRemotePeerListener(this);
            mSkylinkConnection.setMediaListener(this);
            mSkylinkConnection.setOsListener(this);
        }
    }

    /**
     * Get the config for video function
     * User can custom video config by using SkylinkConfig
     */
    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // VideoCall config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setHasPeerMessaging(true);
        skylinkConfig.setHasFileTransfer(true);
        skylinkConfig.setMirrorLocalView(true);
        skylinkConfig.setReportVideoResolutionUntilStable(true);

        // Allow only 1 remote Peer to join.
        skylinkConfig.setMaxPeers(1); // Default is 4 remote Peers.

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);

        // Set default camera setting
        SkylinkConfig.VideoDevice videoDevice = Utils.getDefaultVideoDevice();
        switch (videoDevice) {
            case CAMERA_FRONT:
                skylinkConfig.setDefaultVideoDevice(SkylinkConfig.VideoDevice.CAMERA_FRONT);
                break;
            case CAMERA_BACK:
                skylinkConfig.setDefaultVideoDevice(SkylinkConfig.VideoDevice.CAMERA_BACK);
                break;
            case CUSTOM_CAPTURER:
                skylinkConfig.setDefaultVideoDevice(SkylinkConfig.VideoDevice.CUSTOM_CAPTURER);
                break;
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
     * Get the info of a peer in specific index
     */
    public SkylinkPeer getPeerByIndex(int index) {
        return mPeersList.get(index);
    }
}
