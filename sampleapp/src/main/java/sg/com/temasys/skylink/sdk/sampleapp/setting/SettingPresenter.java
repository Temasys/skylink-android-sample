package sg.com.temasys.skylink.sdk.sampleapp.setting;

import android.content.Context;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.setting.SettingContract.Presenter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_SPEAKER_AUDIO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_VIDEO_DEVICE;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_SPEAKER_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_VIDEO_RESOLUTION;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_FHD;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_HDR;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_VGA;

public class SettingPresenter implements Presenter {

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

        boolean isAudioSpeaker = Utils.getDefaultSpeakerAudio();
        //default audio is headset
        if (!isAudioSpeaker) {
            mSettingView.onAudioHeadsetSelected();
        } else {
            mSettingView.onAudioSpeakerSelected();
        }

        boolean isVideoSpeaker = Utils.getDefaultSpeakerVideo();
        //default video is headset
        if (!isVideoSpeaker) {
            mSettingView.onVideoHeadsetSelected();
        } else {
            mSettingView.onVideoSpeakerSelected();
        }

        SkylinkConfig.VideoDevice videoDevice = Utils.getDefaultVideoDevice();
        switch (videoDevice) {
            case CAMERA_FRONT:
                mSettingView.onCameraFrontSelected();
                break;
            case CAMERA_BACK:
                mSettingView.onCameraBackSelected();
                break;
            case CUSTOM_CAPTURER:
                mSettingView.onCameraCustomSelected();
                break;
        }

        String defaultVideoResolution = Utils.getDefaultVideoResolution();
        //default video resolution is VGA
        if (defaultVideoResolution.equals(VIDEO_RESOLUTION_VGA)) {
            mSettingView.onVideoResVGASelected();
        } else if (defaultVideoResolution.equals(VIDEO_RESOLUTION_HDR)) {
            mSettingView.onVideoResHDRSelected();
        } else if (defaultVideoResolution.equals(VIDEO_RESOLUTION_FHD)) {
            mSettingView.onVideoResFHDSelected();
        }
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
    public void onProcessVideoDevice(SkylinkConfig.VideoDevice videoDevice) {
        //save default camera output to save sharePreference
        //value true is camera back, false is camera front
        // Config.setPrefBoolean(DEFAULT_VIDEO_DEVICE, isCameraBack, (SettingActivity) mContext);
        Config.setPrefString(DEFAULT_VIDEO_DEVICE, videoDevice.name(), (SettingActivity) mContext);
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
}
