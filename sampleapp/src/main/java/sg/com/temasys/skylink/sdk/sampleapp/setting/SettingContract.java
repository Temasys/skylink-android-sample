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

        void onCameraCustomSelected();

        void onCameraFrontSelected();

        void onCameraBackSelected();
    }

    interface Presenter {

        void onProcessSpeakerAudio(boolean isAudioSpeaker);

        void onProcessSpeakerVideo(boolean isVideoSpeaker);

        void onProcessVideoDevice(SkylinkConfig.VideoDevice videoDevice);

        void onProcessVideoResolution(Config.VideoResolution videoResolution);

        void onViewExit();

        void onViewLayoutRequested();
    }
}
