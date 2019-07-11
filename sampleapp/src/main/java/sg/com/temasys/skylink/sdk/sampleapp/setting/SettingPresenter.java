package sg.com.temasys.skylink.sdk.sampleapp.setting;

import android.content.Context;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_SPEAKER_AUDIO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_SPEAKER_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_VIDEO_DEVICE;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_VIDEO_RESOLUTION;
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
}
