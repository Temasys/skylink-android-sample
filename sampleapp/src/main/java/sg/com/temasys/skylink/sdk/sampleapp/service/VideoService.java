package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoContract;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionContract;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_FHD;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_HDR;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_VGA;

/**
 * Created by muoi.pham on 20/07/18.
 * The service class is responsible for communicating with the SkylinkSDK API by using SkylinkConnection instance
 */

public class VideoService extends SkylinkCommonService implements VideoContract.Service {

    public VideoService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(VideoContract.Presenter presenter) {
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
     */
    public void toggleVideo(String mediaId, boolean isToggle) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.toggleVideo(mediaId, !isToggle);
    }

    public void toggleVideo(boolean isRestart) {
        if (mSkylinkConnection != null && localVideoId != null) {
            mSkylinkConnection.toggleVideo(localVideoId, isRestart);
        } else if (isRestart) {
            startLocalVideo();
        }
    }

    public void toggleVideo() {
        if (mSkylinkConnection != null && localVideoId != null) {
            mSkylinkConnection.toggleVideo(localVideoId);
        } else {
            startLocalVideo();
        }
    }

    public void toggleScreen() {
        if (mSkylinkConnection != null && localScreenSharingId != null)
            mSkylinkConnection.toggleVideo(localScreenSharingId);
        else
            startLocalScreen();
    }

    public void toggleScreen(boolean start) {
        if (mSkylinkConnection != null && localScreenSharingId != null)
            mSkylinkConnection.toggleVideo(localScreenSharingId, start);
        else
            startLocalScreen();
    }

    /**
     * Mutes the local user's audio and notifies all the peers in the room.
     *
     * @param audioMuted Flag that specifies whether audio should be mute
     */
    public void muteLocalAudio(boolean audioMuted) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.muteLocalAudio(localAudioId, audioMuted);
    }

    /**
     * Mutes the local user's video and notifies all the peers in the room.
     * Note that black video frames (consuming bandwidth) are still being sent to remote Peer(s).
     *
     * @param videoMuted Flag that specifies whether video should be mute
     */
    public void muteLocalVideo(boolean videoMuted) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.muteLocalVideo(localVideoId, videoMuted);
    }

    /**
     * Mutes the local user's screen video and notifies all the peers in the room.
     * Note that black video frames (consuming bandwidth) are still being sent to remote Peer(s).
     *
     * @param screenMuted Flag that specifies whether screen video should be mute
     */
    public void muteLocalScreen(boolean screenMuted) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.muteLocalVideo(localScreenSharingId, screenMuted);
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
        if (mSkylinkConnection != null) {
            SkylinkMedia media = mSkylinkConnection.getSkylinkMedia(peerId, mediaId);
            if (media != null) {
                return media.getVideoView();
            }
        }

        return null;
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

        // Set the room size
        skylinkConfig.setRoomSize(SkylinkConfig.RoomSize.SMALL);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);

        skylinkConfig.setDefaultVideoDevice(Utils.getDefaultVideoDevice());

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

    public void switchCamera() {
        mSkylinkConnection.switchCamera();
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

    public void stopLocalVideo() {
        mSkylinkConnection.toggleVideo(localVideoId);
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
        if (mSkylinkConnection == null) {
            initializeSkylinkConnection(Constants.CONFIG_TYPE.VIDEO);
        }

        // change default video device to CAMERA_BACK to avoid conflict with default CAMERA_FRONT of starting camera
        getSkylinkConfig().setDefaultVideoDevice(SkylinkConfig.VideoDevice.CAMERA_BACK);
        presenter.onServiceRequestChangeDefaultVideoDevice(SkylinkConfig.VideoDevice.CAMERA_BACK);

        // create a new custom video capturer to input for the method
        VideoCapturer customVideoCapturer = Utils.createCustomVideoCapturerFromCamera();
        if (customVideoCapturer != null) {
            mSkylinkConnection.startLocalMedia(SkylinkConfig.VideoDevice.CAMERA_BACK, customVideoCapturer);
        }
    }

    public void disposeLocalMedia() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.disposeLocalMedia();
        }

        clearInstance();
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
