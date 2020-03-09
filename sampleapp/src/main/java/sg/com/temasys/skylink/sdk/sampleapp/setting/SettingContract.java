package sg.com.temasys.skylink.sdk.sampleapp.setting;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;

/**
 * Created by muoi.pham on 28/08/18.
 */
public interface SettingContract {
    interface View extends BaseView<Presenter> {

        void onAudioHeadsetSelected();

        void onAudioSpeakerSelected();

        void onVideoHeadsetSelected();

        void onVideoSpeakerSelected();

        void onVideoResVGASelected();

        void onVideoResHDRSelected();

        void onVideoResFHDSelected();

        void onVideoResScreenLargeSelected();

        void onVideoResScreenMediumSelected();

        void onVideoResScreenSmallSelected();

        void onCameraNoneSelected();

        void onCameraCustomSelected();

        void onCameraFrontSelected();

        void onCameraBackSelected();

        void onScreenDeviceSelected();

        void onAudioVideoSendSelected();

        void onAudioSendOnlySelected();

        void onVideoSendOnlySelected();

        void onNoneAudioVideoSendSelected();

        void onAudioVideoRecSelected();

        void onAudioRecOnlySelected();

        void onVideoRecOnlySelected();

        void onNoneAudioVideoRecSelected();

        void onAudioCodecOPUSSelected();

        void onAudioCodecISACSelected();

        void onWebSocketTransportSelected();

        void onPollingTransportSelected();

        void onNetworkTransportDefaultSelected();

        void onNetworkTransportUDPSelected();

        void onNetworkTransportTCPSelected();

        void onRoomSizeXSSelected();

        void onRoomSizeSSelected();

        void onRoomSizeMSelected();

        void onRoomSizeLSelected();

        void onDataTransferSelected(boolean hasDataTransfer);

        void onFileTransferSelected(boolean hasDataTransfer);

        void onMessagingSelected(boolean hasMessaging);

        void onVideoHwAccSelected(boolean useHWAcc);

        void onH246ProfileSelected(boolean useH246Profile);

        void onVp8EncoderSelected(boolean useIntelVP8Encoder);

        void onAudioSterioSelected(boolean useAudioSterio);

        void onAudioAutoGainSelected(boolean useAudioAutoGain);

        void onAudioEchoCancellationSelected(boolean useAudioEchoCancellation);

        void onAudioHighPassFilterSelected(boolean useAudioHighPassFilter);

        void onAudioNoiseSuppressionSelected(boolean useAudioNoiseSuppression);

        void onMirrorLocalViewSelected(boolean useMirrorLocalView);

        void onAutoReportVideoResSelected(boolean autoReportVideoRes);

        void onAutoReportVideoResUntillStableSelected(boolean autoReportVideoResUntillStable);

        void onNoOfCheckingVideoResForStableFilled(int noOfCheckingVideoResForStable);

        void onNoOfTimeWaitingForNextCheckingVideoResFilled(int noOfTimeWaitingForNextCheckingVideoRes);

        void onMaxAudioBitrateFilled(int maxAudioBitrate);

        void onMaxVideoBitrateFilled(int maxVideoBitrate);

        void onMaxDataBitrateFilled(int maxDataBitrate);

        void onMaxPeerInAudioRoomFilled(int maxPeerInAudioRoom);

        void onMaxPeerInVideoRoomFilled(int maxPeerInVideoRoom);

        void onMaxPeerInNoMediaRoomFilled(int maxPeerInNoMediaRoom);

        void onUseTurnServerSelected(boolean useTurnServer);

        void onUseSTurnServerSelected(boolean useSTurnServer);

        void onUseHostServerSelected(boolean useHostServer);

        void onAllowIceRestartSelected(boolean allowIceRestart);

        void onUseMultiTrackSelected(boolean useMultiTrack);

        void onNoOfReconnectAttemptsFilled(int noOfReconnectAttempts);

        void onNoOfReconnectDelayFilled(int noOfReconnectDelay);

        void onNoOfGetStoredMessageTimeout(int timeout);
    }

    interface Presenter {

        void onProcessSpeakerAudio(boolean isAudioSpeaker);

        void onProcessSpeakerVideo(boolean isVideoSpeaker);

        void onProcessVideoDevice(String videoDevice);

        void onProcessVideoResolution(Config.VideoResolution videoResolution);

        void onProcessScreenResolution(Config.ScreenResolution screenResolution);

        void onViewExit();

        void onViewLayoutRequested();

        void onProcessChangeHasDataTransferState(boolean isChecked);

        void onProcessChangeHasPeerMessagingState(boolean isChecked);

        void onProcessChangeHasFileTransferState(boolean isChecked);

        void onProcessChangeUseHWAccState(boolean isChecked);

        void onProcessChangeUseH246ProfileState(boolean isChecked);

        void onProcessChangeUseVP8EncoderState(boolean isChecked);

        void onProcessChangeUseAudioStereoState(boolean isChecked);

        void onProcessChangeUseAutoGainControlState(boolean isChecked);

        void onProcessChangeUseAudioEchoCancellationState(boolean isChecked);

        void onProcessChangeUseAudioHighPassFilterrState(boolean isChecked);

        void onProcessChangeUseAudioNoiseSuppressionState(boolean isChecked);

        void onProcessChangeMirrorLocalViewState(boolean isChecked);

        void onProcessChangeReportVideoResStableState(boolean isChecked);

        void onProcessChangeReportVideoResWhenChangedState(boolean isChecked);

        void onProcessChangeUseTurnServerState(boolean isChecked);

        void onProcessChangeUseSturnServerState(boolean isChecked);

        void onProcessChangeUseHostServerState(boolean isChecked);

        void onProcessChangeAllowIceRestartState(boolean isChecked);

        void onProcessChangeUseMultiTracksUPState(boolean isChecked);

        void onProcessAudioVideoSend(SkylinkConfig.AudioVideoConfig audioAndVideoSendConfig);

        void onProcessAudioVideoRec(SkylinkConfig.AudioVideoConfig audioAndVideoRecConfig);

        void onProcessAudioCodec(SkylinkConfig.AudioCodec audioCodec);

        void onProcessWebSocketTransport(SkylinkConfig.SocketTransport transport);

        void onProcessNetworkTransport(SkylinkConfig.NetworkTransport networkTransport);

        void onProcessRoomSize(SkylinkConfig.SkylinkRoomSize roomSize);

        void onProcessNumCheckStableValue(String value);

        void onProcessNumWaitSValue(String value);

        void onProcessMaxAudioBitrateValue(String value);

        void onProcessMaxVideoBitrateValue(String value);

        void onProcessMaxDataBitrateValue(String value);

        void onProcessMaxPeersOfAudioValue(String value);

        void onProcessMaxPeersOfVideoValue(String value);

        void onProcessMaxPeersOfNoAudioNoVideValue(String value);

        void onProcessReconnectAttemptValue(String value);

        void onProcessReconnectDelayValue(String value);

        void onProcessResetAllValues();

        void onProcessGetStoredMessageTimeoutValue(String value);
    }
}
