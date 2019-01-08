package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoLocalState;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoCallContract;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_FHD;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_HDR;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_VGA;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class VideoService extends SkylinkCommonService implements VideoCallContract.Service {

    //this variable need to be static for configuration changed
    // the state of local video {audio, video, camera}
    private static VideoLocalState videoLocalState = new VideoLocalState();

    // the current speaker output {speaker/headset}
    private static boolean currentVideoSpeaker = Utils.getDefaultVideoSpeaker();

    public VideoService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(VideoCallContract.Presenter presenter) {
        mPresenter = (BasePresenter) presenter;
    }

    /**
     * Get the current state of audio
     */
    public boolean isAudioMute() {
        return videoLocalState.isAudioMute();
    }

    /**
     * Set current state of audio
     */
    public void setAudioMute(boolean isAudioMuted) {
        videoLocalState.setAudioMute(isAudioMuted);
    }

    /**
     * Get the current state of video
     */
    public boolean isVideoMute() {
        if (videoLocalState != null)
            return videoLocalState.isVideoMute();

        return false;
    }

    /**
     * Set current state of video
     */
    public void setVideoMute(boolean isVideoMuted) {
        videoLocalState.setVideoMute(isVideoMuted);
    }

    /**
     * Get current state of camera
     */
    public boolean isCameraToggle() {
        return videoLocalState.isCameraToggle();
    }

    /**
     * Set current state of camera
     */
    public void setCamToggle(boolean isCamToggle) {

        //isCamToggle = true, then camera is active
        //isCamToggle = false, then camera is stop
        videoLocalState.setCameraToggle(isCamToggle);
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
    public boolean toggleCamera(boolean isToggle) {
        if (mSkylinkConnection != null)
            return mSkylinkConnection.toggleCamera(isToggle);
        return false;
    }

    /**
     * Mutes the local user's audio and notifies all the peers in the room.
     *
     * @param audioMuted Flag that specifies whether audio should be mute
     */
    public void muteLocalAudio(boolean audioMuted) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.muteLocalAudio(audioMuted);
    }

    /**
     * Mutes the local user's video and notifies all the peers in the room.
     * Note that black video frames (consuming bandwidth) are still being sent to remote Peer(s).
     *
     * @param videoMuted Flag that specifies whether video should be mute
     */
    public void muteLocalVideo(boolean videoMuted) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.muteLocalVideo(videoMuted);
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
    public SurfaceViewRenderer getVideoView(String peerId) {
        if (mSkylinkConnection != null)
            return mSkylinkConnection.getVideoView(peerId);

        return null;
    }

    /**
     * Get the possible capture format(s) of the specified camera device in an array.
     * Return null if current {@link SkylinkConfig.VideoDevice VideoDevice} is not a defined camera,
     * or if it was not possible to get the capture formats.
     *
     * @param videoDevice Use null to specific the current VideoDevice.
     * @return
     */
    public SkylinkCaptureFormat[] getCaptureFormats(SkylinkConfig.VideoDevice videoDevice) {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getCaptureFormats(videoDevice);
        }

        return null;
    }

    /**
     * Return the info of the SkylinkCaptureFormat that is currently being used by the camera.
     * Note that the current CaptureFormat may change whenever the
     * video resolution dimensions change.
     *
     * @return null if there is no CaptureFormat in use now, e.g. if video is not capturing.
     */
    public String getCaptureFormatsString(SkylinkCaptureFormat[] captureFormats) {
        String strFormat = "No CaptureFormat currently registered.";
        String strFormats = "No CaptureFormats currently registered.";

        if (Utils.isCaptureFormatsValid(captureFormats)) {
            strFormats = Utils.captureFormatsToString(captureFormats);
        }

        // Get the current CaptureFormat, if there is one.
        String captureFormatString = null;
        if (mSkylinkConnection != null) {
            SkylinkCaptureFormat captureFormat = mSkylinkConnection.getCaptureFormat();


            if (captureFormat != null) {
                strFormat = captureFormat.toString();
            }

            captureFormatString = "Current capture format: " + strFormat + ".\r\n" +
                    "Supported capture formats: " + strFormats + ".";
        }
        return captureFormatString;
    }

    /**
     * Get the name of the current camera being used.
     * If no camera or if a custom VideoCapturer is being used, return null.
     *
     * @return
     */
    public String getCurrentCameraName() {

        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getCurrentCameraName();
        }
        return null;
    }

    /**
     * Get the current {@link SkylinkConfig.VideoDevice VideoDevice} being used.
     * If none are active, return null.
     *
     * @return
     */
    public SkylinkConfig.VideoDevice getCurrentVideoDevice() {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getCurrentVideoDevice();
        }
        return null;
    }

    /**
     * If the current local input video device is a camera,
     * change the current captured video stream to the specified resolution,
     * and the specified resolution will be set into SkylinkConfig.
     * Non-camera supported resolution can be accepted,
     * but a camera supported resolution will be used when opening camera.
     * There is no guarantee that a specific camera resolution will be maintained
     * as WebRTC may adjust the resolution dynamically to match its bandwidth criteria.
     *
     * @param width
     * @param height
     * @param fps
     */
    public void setInputVideoResolution(int width, int height, int fps) {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.setInputVideoResolution(width, height, fps);
        }
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
        AudioRouter.changeAudioOutput(mContext, isSpeakerOn);
    }

    /**
     * Resume the speaker output
     * In case of activity is resumed, the speaker output state also needs to be resumed
     */
    public void resumeSpeakerOutput() {
        changeSpeakerOutput(currentVideoSpeaker);
    }

    /**
     * Get the current speaker state
     */
    public boolean getCurrentVideoSpeaker() {
        return currentVideoSpeaker;
    }

    /**
     * Set the current speaker state
     */
    public void setCurrentVideoSpeaker(boolean isSpeakerOn) {
        currentVideoSpeaker = isSpeakerOn;
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
        skylinkConfig.setReportVideoResolutionOnChange(true);

        // Allow only 1 remote Peer to join.
        skylinkConfig.setMaxPeers(1); // Default is 4 remote Peers.

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);

        // Set default camera setting
        if (Utils.getDefaultCameraOutput())
            skylinkConfig.setDefaultVideoDevice(SkylinkConfig.VideoDevice.CAMERA_BACK);
        else
            skylinkConfig.setDefaultVideoDevice(SkylinkConfig.VideoDevice.CAMERA_FRONT);

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
}
