package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.localvideo.LocalVideoContract;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_FHD;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_HDR;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_VGA;

/**
 * Created by muoi.pham on 20/07/18.
 * The service class is responsible for communicating with the SkylinkSDK API by using SkylinkConnection instance
 */

public class LocalVideoService extends SkylinkCommonService implements LocalVideoContract.Service {

    public LocalVideoService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(LocalVideoContract.Presenter presenter) {
        this.presenter = (BasePresenter) presenter;
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
     * Sets the specified listeners for video function
     * Video call needs to implement LifeCycleListener, RemotePeerListener, MediaListener, OsListener
     */
    @Override
    public void setSkylinkListeners() {
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
}
