package sg.com.temasys.skylink.sdk.sampleapp.setting;

import android.content.Context;

import sg.com.temasys.skylink.sdk.sampleapp.setting.SettingContract.Presenter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_AUDIO_OUTPUT;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_CAMERA_OUTPUT;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_VIDEO_OUTPUT;
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

        boolean isAudioSpeaker = Utils.getDefaultAudioOutput();
        //default audio is headset
        if (!isAudioSpeaker) {
            mSettingView.onAudioHeadsetSelected();
        } else {
            mSettingView.onAudioSpeakerSelected();
        }

        boolean isVideoSpeaker = Utils.getDefaultVideoOuput();
        //default video is headset
        if (!isVideoSpeaker) {
            mSettingView.onVideoHeadsetSelected();
        } else {
            mSettingView.onVideoSpeakerSelected();
        }

        boolean isCameraBack = Utils.getDefaultCameraOutput();
        //default camera is front
        if (!isCameraBack) {
            mSettingView.onCameraFrontSelected();
        } else {
            mSettingView.onCameraBackSelected();
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
    public void onProcessAudioOutput(boolean isAudioSpeaker) {
        //save default audio output to save sharePreference
        //value true is speaker, false is headset
        Config.setPrefBoolean(DEFAULT_AUDIO_OUTPUT, isAudioSpeaker, (SettingActivity) mContext);
    }

    @Override
    public void onProcessVideoOutput(boolean isVideoSpeaker) {
        //save default video output to save sharePreference
        //value true is speaker, false is headset
        Config.setPrefBoolean(DEFAULT_VIDEO_OUTPUT, isVideoSpeaker, (SettingActivity) mContext);
    }

    @Override
    public void onProcessCameraOutput(boolean isCameraBack) {
        //save default camera output to save sharePreference
        //value true is camera back, false is camera front
        Config.setPrefBoolean(DEFAULT_CAMERA_OUTPUT, isCameraBack, (SettingActivity) mContext);
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
