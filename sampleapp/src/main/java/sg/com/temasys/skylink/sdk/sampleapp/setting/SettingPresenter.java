package sg.com.temasys.skylink.sdk.sampleapp.setting;

import android.content.Context;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.ALLOW_ICE_RESTART_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.AUDIO_AND_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.AUDIO_CODEC_ISAC;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.AUDIO_CODEC_OPUS;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.AUDIO_ONLY;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_AUDIO_CODEC_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_AUDIO_VIDEO_RECEIVE_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_AUDIO_VIDEO_SEND_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_NETWORK_TRANSPORT_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_ROOM_SIZE_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_SCREEN_RESOLUTION;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_SOCKET_TRANSPORT_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_SPEAKER_AUDIO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_SPEAKER_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_STORED_MESSAGE_TIMEOUT_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_VIDEO_DEVICE;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_VIDEO_RESOLUTION;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.GET_STORED_MESSAGE_TIMEOUT;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.HAS_DATA_TRANSFER_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.HAS_FILE_TRANSFER_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.HAS_PEER_MESSAGING_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.MAX_AUDIO_BITRATE_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.MAX_DATA_BITRATE_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.MAX_PEER_IN_AUDIO_ROOM_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.MAX_PEER_IN_NO_MEDIA_ROOM_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.MAX_PEER_IN_VIDEO_ROOM_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.MAX_VIDEO_BITRATE_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.MIRROR_LOCAL_VIEW_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.NETWORK_TCP;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.NETWORK_UDP;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.NO_AUDIO_NO_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.NO_OF_REPORT_VIDEO_RES_UNTILL_STABLE_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.RECONNECT_ATTEMPS_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.RECONNECT_DELAY_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.REPORT_VIDEO_RES_ON_CHANGED_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.REPORT_VIDEO_RES_STABLE_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.ROOM_SIZE_L;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.ROOM_SIZE_M;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.ROOM_SIZE_S;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.ROOM_SIZE_XS;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.SCREEN_RESOLUTION_LARGE;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.SCREEN_RESOLUTION_MEDIUM;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.SCREEN_RESOLUTION_SMALL;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.SOCKET_POLLING;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.SOCKET_WEB;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.TIME_OF_REPORT_VIDEO_RES_NEXT_CHECKING_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USE_AUDIO_ECHO_CANCELLATION_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USE_AUDIO_HIGH_PASS_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USE_AUDIO_NOISE_SUPPRESSION_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USE_AUDIO_STEREO_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USE_AUTO_GAIN_CONTROL_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USE_H246_PROFILE_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USE_HOST_SERVER_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USE_HW_ACC_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USE_MULTI_TRACKS_UP_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USE_STURN_SERVER_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USE_TURN_SERVER_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USE_VP8_ENCODER_CONFIG;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_ONLY;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_FHD;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_HDR;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_VGA;

public class SettingPresenter extends BasePresenter implements SettingContract.Presenter {

    private Context mContext;

    //view object
    private SettingContract.View mSettingView;

    //constructor
    public SettingPresenter(Context context) {
        this.mContext = context;
    }

    //link Presenter to View
    public void setView(SettingContract.View view) {
        mSettingView = view;
        mSettingView.setPresenter(this);
    }

    @Override
    public void onViewLayoutRequested() {

        boolean isAudioSpeaker = Utils.isDefaultSpeakerSettingForAudio();

        //default audio is headset
        if (!isAudioSpeaker) {
            mSettingView.onAudioHeadsetSelected();
        } else {
            mSettingView.onAudioSpeakerSelected();
        }

        boolean isVideoSpeaker = Utils.isDefaultSpeakerSettingForVideo();

        //default video is headset
        if (!isVideoSpeaker) {
            mSettingView.onVideoHeadsetSelected();
        } else {
            mSettingView.onVideoSpeakerSelected();
        }

        String videoDevice = Utils.getDefaultVideoDeviceString();
        switch (videoDevice) {
            case Constants.DEFAULT_VIDEO_DEVICE_FRONT_CAMERA:
                mSettingView.onCameraFrontSelected();
                break;
            case Constants.DEFAULT_VIDEO_DEVICE_BACK_CAMERA:
                mSettingView.onCameraBackSelected();
                break;
            case Constants.DEFAULT_VIDEO_DEVICE_SCREEN:
                mSettingView.onScreenDeviceSelected();
                break;
            case Constants.DEFAULT_VIDEO_DEVICE_CUSTOM:
                mSettingView.onCameraCustomSelected();
                break;
            case Constants.DEFAULT_VIDEO_DEVICE_NONE:
                mSettingView.onCameraNoneSelected();
                break;
            default:
                mSettingView.onCameraFrontSelected();
        }

        String defaultVideoResolution = Utils.getDefaultVideoResolution();
        if (defaultVideoResolution.equals(VIDEO_RESOLUTION_VGA)) {
            mSettingView.onVideoResVGASelected();
        } else if (defaultVideoResolution.equals(VIDEO_RESOLUTION_HDR)) {
            mSettingView.onVideoResHDRSelected();
        } else if (defaultVideoResolution.equals(VIDEO_RESOLUTION_FHD)) {
            mSettingView.onVideoResFHDSelected();
        }

        String defaultScreenResolution = Utils.getDefaultScreenResolution();
        if (defaultScreenResolution.equals(SCREEN_RESOLUTION_LARGE)) {
            mSettingView.onVideoResScreenLargeSelected();
        } else if (defaultScreenResolution.equals(SCREEN_RESOLUTION_MEDIUM)) {
            mSettingView.onVideoResScreenMediumSelected();
        } else if (defaultScreenResolution.equals(SCREEN_RESOLUTION_SMALL)) {
            mSettingView.onVideoResScreenSmallSelected();
        }

        String defaultAudioVideoSend = Utils.getDefaultAudioVideoSendConfig();
        if (defaultAudioVideoSend.equals(AUDIO_AND_VIDEO)) {
            mSettingView.onAudioVideoSendSelected();
        } else if (defaultAudioVideoSend.equals(AUDIO_ONLY)) {
            mSettingView.onAudioSendOnlySelected();
        } else if (defaultAudioVideoSend.equals(VIDEO_ONLY)) {
            mSettingView.onVideoSendOnlySelected();
        } else if (defaultAudioVideoSend.equals(NO_AUDIO_NO_VIDEO)) {
            mSettingView.onNoneAudioVideoSendSelected();
        }

        String defaultAudioVideoRec = Utils.getDefaultAudioVideoRecConfig();
        if (defaultAudioVideoRec.equals(AUDIO_AND_VIDEO)) {
            mSettingView.onAudioVideoRecSelected();
        } else if (defaultAudioVideoRec.equals(AUDIO_ONLY)) {
            mSettingView.onAudioRecOnlySelected();
        } else if (defaultAudioVideoRec.equals(VIDEO_ONLY)) {
            mSettingView.onVideoRecOnlySelected();
        } else if (defaultAudioVideoRec.equals(NO_AUDIO_NO_VIDEO)) {
            mSettingView.onNoneAudioVideoRecSelected();
        }

        String defaultAudioCodec = Utils.getDefaultAudioCodecConfig();
        if (defaultAudioCodec.equals(AUDIO_CODEC_OPUS)) {
            mSettingView.onAudioCodecOPUSSelected();
        } else if (defaultAudioCodec.equals(AUDIO_CODEC_ISAC)) {
            mSettingView.onAudioCodecISACSelected();
        }

        String defaultWebSocketTransport = Utils.getDefaultSocketTransportConfig();
        if (defaultWebSocketTransport.equals(SOCKET_POLLING)) {
            mSettingView.onPollingTransportSelected();
        } else if (defaultWebSocketTransport.equals(SOCKET_WEB)) {
            mSettingView.onWebSocketTransportSelected();
        }

        String defaultNetworkTransport = Utils.getDefaultNetworkTransportConfig();
        if (defaultNetworkTransport.equals(NETWORK_UDP)) {
            mSettingView.onNetworkTransportUDPSelected();
        } else if (defaultNetworkTransport.equals(NETWORK_TCP)) {
            mSettingView.onNetworkTransportTCPSelected();
        }

        String defaultRoomSize = Utils.getDefaultRoomsizeConfig();
        if (defaultRoomSize.equals(ROOM_SIZE_XS)) {
            mSettingView.onRoomSizeXSSelected();
        } else if (defaultRoomSize.equals(ROOM_SIZE_S)) {
            mSettingView.onRoomSizeSSelected();
        } else if (defaultRoomSize.equals(ROOM_SIZE_M)) {
            mSettingView.onRoomSizeMSelected();
        } else if (defaultRoomSize.equals(ROOM_SIZE_L)) {
            mSettingView.onRoomSizeLSelected();
        }

        boolean hasDataTransfer = Utils.getDefaultDataTransferConfig();
        mSettingView.onDataTransferSelected(hasDataTransfer);

        boolean hasFileTransfer = Utils.getDefaultFileTransferConfig();
        mSettingView.onFileTransferSelected(hasFileTransfer);

        boolean hasMessaging = Utils.getDefaultMessageConfig();
        mSettingView.onMessagingSelected(hasMessaging);

        boolean useHWAcc = Utils.getDefaultVideoHwAccConfig();
        mSettingView.onVideoHwAccSelected(useHWAcc);

        boolean useH246Profile = Utils.getDefaultH246ProfileConfig();
        mSettingView.onH246ProfileSelected(useH246Profile);

        boolean useIntelVP8Encoder = Utils.getDefaultVp8EncoderConfig();
        mSettingView.onVp8EncoderSelected(useIntelVP8Encoder);

        boolean useAudioSterio = Utils.getDefaultAudioStereoConfig();
        mSettingView.onAudioSterioSelected(useAudioSterio);

        boolean useAudioAutoGain = Utils.getDefaultAudioAutoGainConfig();
        mSettingView.onAudioAutoGainSelected(useAudioAutoGain);

        boolean useAudioEchoCancellation = Utils.getDefaultAudioEchoCancellationConfig();
        mSettingView.onAudioEchoCancellationSelected(useAudioEchoCancellation);

        boolean useAudioHighPassFilter = Utils.getDefaultAudioHighPassFilterConfig();
        mSettingView.onAudioHighPassFilterSelected(useAudioHighPassFilter);

        boolean useAudioNoiseSuppression = Utils.getDefaultAudioNoiseSuppressionConfig();
        mSettingView.onAudioNoiseSuppressionSelected(useAudioNoiseSuppression);

        boolean useMirrorLocalView = Utils.getDefaultMirrorlocalViewConfig();
        mSettingView.onMirrorLocalViewSelected(useMirrorLocalView);

        boolean autoReportVideoRes = Utils.getDefaultAutoReportVideoResOnChangeConfig();
        mSettingView.onAutoReportVideoResSelected(autoReportVideoRes);

        boolean autoReportVideoResUntillStable = Utils.getDefaultAutoReportVideoUntillStableConfig();
        mSettingView.onAutoReportVideoResUntillStableSelected(autoReportVideoResUntillStable);

        int noOfCheckingVideoResForStable = Utils.getDefaultNoOfCheckingVideoResForStableConfig();
        mSettingView.onNoOfCheckingVideoResForStableFilled(noOfCheckingVideoResForStable);

        int noOfTimeWaitingForNextCheckingVideoRes = Utils.getDefaultTimeWaitingForNextCheckingVideoResConfig();
        mSettingView.onNoOfTimeWaitingForNextCheckingVideoResFilled(noOfTimeWaitingForNextCheckingVideoRes);

        int maxAudioBitrate = Utils.getDefaultMaxAudioBitrateConfig();
        mSettingView.onMaxAudioBitrateFilled(maxAudioBitrate);

        int maxVideoBitrate = Utils.getDefaultMaxVideoBitrateConfig();
        mSettingView.onMaxVideoBitrateFilled(maxVideoBitrate);

        int maxDataBitrate = Utils.getDefaultMaxDataBitrateConfig();
        mSettingView.onMaxDataBitrateFilled(maxDataBitrate);

        int maxPeerInAudioRoom = Utils.getDefaultMaxPeerInAudioRoomConfig();
        mSettingView.onMaxPeerInAudioRoomFilled(maxPeerInAudioRoom);

        int maxPeerInVideoRoom = Utils.getDefaultMaxPeerInVideoRoomConfig();
        mSettingView.onMaxPeerInVideoRoomFilled(maxPeerInVideoRoom);

        int maxPeerInNoMediaRoom = Utils.getDefaultMaxPeerInNoMediaRoomConfig();
        mSettingView.onMaxPeerInNoMediaRoomFilled(maxPeerInNoMediaRoom);

        boolean useTurnServer = Utils.getDefaultTurnConfig();
        mSettingView.onUseTurnServerSelected(useTurnServer);

        boolean useSTurnServer = Utils.getDefaultSTurnConfig();
        mSettingView.onUseSTurnServerSelected(useSTurnServer);

        boolean useHostServer = Utils.getDefaultHostConfig();
        mSettingView.onUseHostServerSelected(useHostServer);

        boolean allowIceRestart = Utils.getDefaultIceRestartConfig();
        mSettingView.onAllowIceRestartSelected(allowIceRestart);

        boolean useMultiTrack = Utils.getDefaultMultiTrackConfig();
        mSettingView.onUseMultiTrackSelected(useMultiTrack);

        int noOfReconnectAttempts = Utils.getDefaultNoOfReconnectAttempsConfig();
        mSettingView.onNoOfReconnectAttemptsFilled(noOfReconnectAttempts);

        int noOfReconnectDelay = Utils.getDefaultNoOfReconnectDelayConfig();
        mSettingView.onNoOfReconnectDelayFilled(noOfReconnectDelay);

        int noOfStoredMsgTimeout = Utils.getDefaultNoOfStoredMsgTimeoutConfig();
        mSettingView.onNoOfGetStoredMessageTimeout(noOfStoredMsgTimeout);
    }

    @Override
    public void onProcessChangeHasDataTransferState(boolean isChecked) {
        //save default value to sharePreference
        Config.setPrefBoolean(HAS_DATA_TRANSFER_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeHasPeerMessagingState(boolean isChecked) {
        //save default value to sharePreference
        Config.setPrefBoolean(HAS_PEER_MESSAGING_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeHasFileTransferState(boolean isChecked) {
        Config.setPrefBoolean(HAS_FILE_TRANSFER_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeUseHWAccState(boolean isChecked) {
        Config.setPrefBoolean(USE_HW_ACC_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeUseH246ProfileState(boolean isChecked) {
        Config.setPrefBoolean(USE_H246_PROFILE_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeUseVP8EncoderState(boolean isChecked) {
        Config.setPrefBoolean(USE_VP8_ENCODER_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeUseAudioStereoState(boolean isChecked) {
        Config.setPrefBoolean(USE_AUDIO_STEREO_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeUseAutoGainControlState(boolean isChecked) {
        Config.setPrefBoolean(USE_AUTO_GAIN_CONTROL_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeUseAudioEchoCancellationState(boolean isChecked) {
        Config.setPrefBoolean(USE_AUDIO_ECHO_CANCELLATION_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeUseAudioHighPassFilterrState(boolean isChecked) {
        Config.setPrefBoolean(USE_AUDIO_HIGH_PASS_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeUseAudioNoiseSuppressionState(boolean isChecked) {
        Config.setPrefBoolean(USE_AUDIO_NOISE_SUPPRESSION_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeMirrorLocalViewState(boolean isChecked) {
        Config.setPrefBoolean(MIRROR_LOCAL_VIEW_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeReportVideoResStableState(boolean isChecked) {
        Config.setPrefBoolean(REPORT_VIDEO_RES_STABLE_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeReportVideoResWhenChangedState(boolean isChecked) {
        Config.setPrefBoolean(REPORT_VIDEO_RES_ON_CHANGED_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeUseTurnServerState(boolean isChecked) {
        Config.setPrefBoolean(USE_TURN_SERVER_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeUseSturnServerState(boolean isChecked) {
        Config.setPrefBoolean(USE_STURN_SERVER_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeUseHostServerState(boolean isChecked) {
        Config.setPrefBoolean(USE_HOST_SERVER_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeAllowIceRestartState(boolean isChecked) {
        Config.setPrefBoolean(ALLOW_ICE_RESTART_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessChangeUseMultiTracksUPState(boolean isChecked) {
        Config.setPrefBoolean(USE_MULTI_TRACKS_UP_CONFIG, isChecked, (SettingActivity) mContext);
    }

    @Override
    public void onProcessAudioVideoSend(SkylinkConfig.AudioVideoConfig audioAndVideoSendConfig) {
        switch (audioAndVideoSendConfig) {
            case AUDIO_AND_VIDEO:
                Config.setPrefString(DEFAULT_AUDIO_VIDEO_SEND_CONFIG, AUDIO_AND_VIDEO, (SettingActivity) mContext);
                break;
            case AUDIO_ONLY:
                Config.setPrefString(DEFAULT_AUDIO_VIDEO_SEND_CONFIG, AUDIO_ONLY, (SettingActivity) mContext);
                break;
            case VIDEO_ONLY:
                Config.setPrefString(DEFAULT_AUDIO_VIDEO_SEND_CONFIG, VIDEO_ONLY, (SettingActivity) mContext);
                break;
            case NO_AUDIO_NO_VIDEO:
                Config.setPrefString(DEFAULT_AUDIO_VIDEO_SEND_CONFIG, NO_AUDIO_NO_VIDEO, (SettingActivity) mContext);
                break;
        }
    }

    @Override
    public void onProcessAudioVideoRec(SkylinkConfig.AudioVideoConfig audioAndVideoRecConfig) {
        switch (audioAndVideoRecConfig) {
            case AUDIO_AND_VIDEO:
                Config.setPrefString(DEFAULT_AUDIO_VIDEO_RECEIVE_CONFIG, AUDIO_AND_VIDEO, (SettingActivity) mContext);
                break;
            case AUDIO_ONLY:
                Config.setPrefString(DEFAULT_AUDIO_VIDEO_RECEIVE_CONFIG, AUDIO_ONLY, (SettingActivity) mContext);
                break;
            case VIDEO_ONLY:
                Config.setPrefString(DEFAULT_AUDIO_VIDEO_RECEIVE_CONFIG, VIDEO_ONLY, (SettingActivity) mContext);
                break;
            case NO_AUDIO_NO_VIDEO:
                Config.setPrefString(DEFAULT_AUDIO_VIDEO_RECEIVE_CONFIG, NO_AUDIO_NO_VIDEO, (SettingActivity) mContext);
                break;
        }
    }

    @Override
    public void onProcessAudioCodec(SkylinkConfig.AudioCodec audioCodec) {
        switch (audioCodec) {
            case OPUS:
                Config.setPrefString(DEFAULT_AUDIO_CODEC_CONFIG, AUDIO_CODEC_OPUS, (SettingActivity) mContext);
                break;
            case ISAC:
                Config.setPrefString(DEFAULT_AUDIO_CODEC_CONFIG, AUDIO_CODEC_ISAC, (SettingActivity) mContext);
                break;
        }
    }

    @Override
    public void onProcessWebSocketTransport(SkylinkConfig.SocketTransport transport) {
        switch (transport) {
            case POLLING:
                Config.setPrefString(DEFAULT_SOCKET_TRANSPORT_CONFIG, SOCKET_POLLING, (SettingActivity) mContext);
                break;
            case WEBSOCKET:
                Config.setPrefString(DEFAULT_SOCKET_TRANSPORT_CONFIG, SOCKET_WEB, (SettingActivity) mContext);
                break;
        }
    }

    @Override
    public void onProcessNetworkTransport(SkylinkConfig.NetworkTransport networkTransport) {
        switch (networkTransport) {
            case UDP:
                Config.setPrefString(DEFAULT_NETWORK_TRANSPORT_CONFIG, NETWORK_UDP, (SettingActivity) mContext);
                break;
            case TCP:
                Config.setPrefString(DEFAULT_NETWORK_TRANSPORT_CONFIG, NETWORK_TCP, (SettingActivity) mContext);
                break;
        }
    }

    @Override
    public void onProcessRoomSize(SkylinkConfig.SkylinkRoomSize roomSize) {
        switch (roomSize) {
            case EXTRA_SMALL:
                Config.setPrefString(DEFAULT_ROOM_SIZE_CONFIG, ROOM_SIZE_XS, (SettingActivity) mContext);
                break;
            case SMALL:
                Config.setPrefString(DEFAULT_ROOM_SIZE_CONFIG, ROOM_SIZE_S, (SettingActivity) mContext);
                break;
            case MEDIUM:
                Config.setPrefString(DEFAULT_ROOM_SIZE_CONFIG, ROOM_SIZE_M, (SettingActivity) mContext);
                break;
            case LARGE:
                Config.setPrefString(DEFAULT_ROOM_SIZE_CONFIG, ROOM_SIZE_L, (SettingActivity) mContext);
                break;

        }
    }

    @Override
    public void onProcessNumCheckStableValue(String value) {
        Config.setPrefString(NO_OF_REPORT_VIDEO_RES_UNTILL_STABLE_CONFIG, value, (SettingActivity) mContext);
    }

    @Override
    public void onProcessNumWaitSValue(String value) {
        Config.setPrefString(TIME_OF_REPORT_VIDEO_RES_NEXT_CHECKING_CONFIG, value, (SettingActivity) mContext);
    }

    @Override
    public void onProcessMaxAudioBitrateValue(String value) {
        Config.setPrefString(MAX_AUDIO_BITRATE_CONFIG, value, (SettingActivity) mContext);
    }

    @Override
    public void onProcessMaxVideoBitrateValue(String value) {
        Config.setPrefString(MAX_VIDEO_BITRATE_CONFIG, value, (SettingActivity) mContext);
    }

    @Override
    public void onProcessMaxDataBitrateValue(String value) {
        Config.setPrefString(MAX_DATA_BITRATE_CONFIG, value, (SettingActivity) mContext);
    }

    @Override
    public void onProcessMaxPeersOfAudioValue(String value) {
        Config.setPrefString(MAX_PEER_IN_AUDIO_ROOM_CONFIG, value, (SettingActivity) mContext);
    }

    @Override
    public void onProcessMaxPeersOfVideoValue(String value) {
        Config.setPrefString(MAX_PEER_IN_VIDEO_ROOM_CONFIG, value, (SettingActivity) mContext);
    }

    @Override
    public void onProcessMaxPeersOfNoAudioNoVideValue(String value) {
        Config.setPrefString(MAX_PEER_IN_NO_MEDIA_ROOM_CONFIG, value, (SettingActivity) mContext);
    }

    @Override
    public void onProcessReconnectAttemptValue(String value) {
        Config.setPrefString(RECONNECT_ATTEMPS_CONFIG, value, (SettingActivity) mContext);
    }

    @Override
    public void onProcessReconnectDelayValue(String value) {
        Config.setPrefString(RECONNECT_DELAY_CONFIG, value, (SettingActivity) mContext);
    }

    @Override
    public void onProcessGetStoredMessageTimeoutValue(String value) {
        Config.setPrefString(GET_STORED_MESSAGE_TIMEOUT, value, (SettingActivity) mContext);
    }

    @Override
    public void onProcessResetAllValues() {
        Config.setPrefBoolean(DEFAULT_SPEAKER_AUDIO, true, (SettingActivity) mContext);
        Config.setPrefBoolean(DEFAULT_SPEAKER_VIDEO, true, (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_VIDEO_DEVICE, SkylinkConfig.VideoDevice.CAMERA_FRONT.getDeviceName(), (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_VIDEO_RESOLUTION, VIDEO_RESOLUTION_VGA, (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_SCREEN_RESOLUTION, SCREEN_RESOLUTION_LARGE, (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_SCREEN_RESOLUTION, SCREEN_RESOLUTION_LARGE, (SettingActivity) mContext);
        Config.setPrefBoolean(HAS_DATA_TRANSFER_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(HAS_PEER_MESSAGING_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(HAS_FILE_TRANSFER_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(USE_AUDIO_STEREO_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_STORED_MESSAGE_TIMEOUT_CONFIG, "30", (SettingActivity) mContext);
        Config.setPrefBoolean(USE_HW_ACC_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(USE_H246_PROFILE_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(USE_VP8_ENCODER_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(USE_AUTO_GAIN_CONTROL_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(USE_AUDIO_ECHO_CANCELLATION_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(USE_AUDIO_HIGH_PASS_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(USE_AUDIO_NOISE_SUPPRESSION_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(MIRROR_LOCAL_VIEW_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(REPORT_VIDEO_RES_STABLE_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(REPORT_VIDEO_RES_ON_CHANGED_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefString(NO_OF_REPORT_VIDEO_RES_UNTILL_STABLE_CONFIG, "5", (SettingActivity) mContext);
        Config.setPrefString(TIME_OF_REPORT_VIDEO_RES_NEXT_CHECKING_CONFIG, "275", (SettingActivity) mContext);
        Config.setPrefString(MAX_AUDIO_BITRATE_CONFIG, "-1", (SettingActivity) mContext);
        Config.setPrefString(MAX_VIDEO_BITRATE_CONFIG, "-1", (SettingActivity) mContext);
        Config.setPrefString(MAX_DATA_BITRATE_CONFIG, "-1", (SettingActivity) mContext);
        Config.setPrefString(MAX_PEER_IN_AUDIO_ROOM_CONFIG, "8", (SettingActivity) mContext);
        Config.setPrefString(MAX_PEER_IN_VIDEO_ROOM_CONFIG, "4", (SettingActivity) mContext);
        Config.setPrefString(MAX_PEER_IN_NO_MEDIA_ROOM_CONFIG, "16", (SettingActivity) mContext);
        Config.setPrefBoolean(USE_TURN_SERVER_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(USE_STURN_SERVER_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(USE_HOST_SERVER_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefBoolean(ALLOW_ICE_RESTART_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefString(RECONNECT_ATTEMPS_CONFIG, "-1", (SettingActivity) mContext);
        Config.setPrefString(RECONNECT_DELAY_CONFIG, "1000", (SettingActivity) mContext);
        Config.setPrefBoolean(USE_MULTI_TRACKS_UP_CONFIG, true, (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_AUDIO_VIDEO_SEND_CONFIG, AUDIO_AND_VIDEO, (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_AUDIO_VIDEO_RECEIVE_CONFIG, AUDIO_AND_VIDEO, (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_AUDIO_VIDEO_RECEIVE_CONFIG, AUDIO_AND_VIDEO, (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_AUDIO_CODEC_CONFIG, AUDIO_CODEC_OPUS, (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_SOCKET_TRANSPORT_CONFIG, SOCKET_WEB, (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_NETWORK_TRANSPORT_CONFIG, NETWORK_UDP, (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_ROOM_SIZE_CONFIG, ROOM_SIZE_S, (SettingActivity) mContext);

        onViewLayoutRequested();
    }

    @Override
    public void onViewExit() {
        //do nothing
    }

    @Override
    public void onProcessSpeakerAudio(boolean isAudioSpeaker) {
        //save default audio output to save sharePreference
        //value true is speaker, false is headset
        Config.setPrefBoolean(DEFAULT_SPEAKER_AUDIO, isAudioSpeaker, (SettingActivity) mContext);
    }

    @Override
    public void onProcessSpeakerVideo(boolean isVideoSpeaker) {
        //save default video output to save sharePreference
        //value true is speaker, false is headset
        Config.setPrefBoolean(DEFAULT_SPEAKER_VIDEO, isVideoSpeaker, (SettingActivity) mContext);
    }

    @Override
    public void onProcessVideoDevice(String videoDevice) {
        //save default camera output to save sharePreference
        Config.setPrefString(DEFAULT_VIDEO_DEVICE, videoDevice, (SettingActivity) mContext);
    }

    @Override
    public void onProcessVideoResolution(Config.VideoResolution videoResolution) {
        //save default video resolution to save sharePreference
        switch (videoResolution) {
            case VGA:
                Config.setPrefString(DEFAULT_VIDEO_RESOLUTION, VIDEO_RESOLUTION_VGA, (SettingActivity) mContext);
                break;
            case HDR:
                Config.setPrefString(DEFAULT_VIDEO_RESOLUTION, VIDEO_RESOLUTION_HDR, (SettingActivity) mContext);
                break;
            case FHD:
                Config.setPrefString(DEFAULT_VIDEO_RESOLUTION, VIDEO_RESOLUTION_FHD, (SettingActivity) mContext);
                break;
        }
    }

    @Override
    public void onProcessScreenResolution(Config.ScreenResolution screenResolution) {
        //save default video resolution to save sharePreference
        switch (screenResolution) {
            case LARGE_PORTRAIT:
                Config.setPrefString(DEFAULT_SCREEN_RESOLUTION, SCREEN_RESOLUTION_LARGE, (SettingActivity) mContext);
                break;
            case MEDIUM_PORTRAIT:
                Config.setPrefString(DEFAULT_SCREEN_RESOLUTION, SCREEN_RESOLUTION_MEDIUM, (SettingActivity) mContext);
                break;
            case SMALL_PORTRAIT:
                Config.setPrefString(DEFAULT_SCREEN_RESOLUTION, SCREEN_RESOLUTION_SMALL, (SettingActivity) mContext);
                break;
        }
    }
}
