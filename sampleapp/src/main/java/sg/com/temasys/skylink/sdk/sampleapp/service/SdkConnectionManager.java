package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_FHD;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_HDR;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_VGA;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class SdkConnectionManager {

    private Context mContext;

    private SkylinkConnection mSkylinkConnection = null;

    public SdkConnectionManager(Context context) {
        this.mContext = context;
    }

    public SkylinkConnection initializeSkylinkConnection(Constants.CONFIG_TYPE type) {

        SkylinkConfig skylinkConfig = null;

        switch (type) {
            case AUDIO:
                skylinkConfig = getSkylinkConfigForAudioCall();
                break;
            case VIDEO:
                skylinkConfig = getSkylinkConfigForVideoCall();
                break;
            case CHAT:
                skylinkConfig = getSkylinkConfigForChat();
                break;
            case DATA:
                skylinkConfig = getSkylinkConfigForDataTransfer();
                break;
            case FILE:
                skylinkConfig = getSkylinkConfigForFileTransfer();
                break;
            case MULTI_PARTY_VIDEO:
                skylinkConfig = getSkylinkConfigForMultiPartyVideoCall();
                break;
        }

        if (skylinkConfig != null) {
            mSkylinkConnection = SkylinkConnection.getInstance();
            mSkylinkConnection.init(Config.getAppKey(), skylinkConfig,
                    mContext.getApplicationContext());
        }

        return mSkylinkConnection;

    }

    private SkylinkConfig getSkylinkConfigForAudioCall() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // AudioVideo config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        skylinkConfig.setHasPeerMessaging(true);
        skylinkConfig.setHasFileTransfer(true);

        // Allow only 1 remote Peer to join.
        skylinkConfig.setMaxPeers(1); // Default is 4 remote Peers.

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    private SkylinkConfig getSkylinkConfigForChat() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // Chat config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setHasPeerMessaging(true);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    private SkylinkConfig getSkylinkConfigForDataTransfer() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // DataTransfer config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setHasDataTransfer(true);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    private SkylinkConfig getSkylinkConfigForFileTransfer() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // FileTransfer config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setHasFileTransfer(true);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    private SkylinkConfig getSkylinkConfigForMultiPartyVideoCall() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // MultiPartyVideoCall config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setHasPeerMessaging(true);
        skylinkConfig.setHasFileTransfer(true);
        skylinkConfig.setMirrorLocalView(true);

        // Allow only 3 remote Peers to join, due to current UI design.
        skylinkConfig.setMaxPeers(3);

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

    private SkylinkConfig getSkylinkConfigForVideoCall() {
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
