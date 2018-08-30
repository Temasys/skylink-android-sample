package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoLocalState;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoCallContract;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class VideoService extends SDKService implements VideoCallContract.Service {

    //this variable need to be static for configuration change
    private static VideoLocalState videoLocalState = new VideoLocalState();
    private static boolean currentVideoSpeaker = Utils.getDefaultVideoSpeaker();

    public VideoService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(VideoCallContract.Presenter presenter) {
        this.mVideoPresenter = presenter;
    }

    @Override
    public void setTypeCall() {
        mTypeCall = Constants.CONFIG_TYPE.VIDEO;
    }

    public boolean isAudioMute() {
        return videoLocalState.isAudioMute();
    }

    public void setAudioMute(boolean isAudioMuted) {
        videoLocalState.setAudioMute(isAudioMuted);
    }

    public boolean isVideoMute() {
        if (videoLocalState != null)
            return videoLocalState.isVideoMute();

        return false;
    }

    public void setVideoMute(boolean isVideoMuted) {
        videoLocalState.setVideoMute(isVideoMuted);
    }

    public boolean isCameraToggle() {
        return videoLocalState.isCameraToggle();
    }

    public void setCamToggle(boolean isCamToggle) {
        //isCamToggle = true, then camera is active
        //isCamToggle = false, then camera is stop

        videoLocalState.setCameraToggle(isCamToggle);
    }

    public boolean toggleCamera(boolean isToggle) {
        if (mSkylinkConnection != null)
            return mSkylinkConnection.toggleCamera(isToggle);
        return false;
    }

    public void muteLocalAudio(boolean audioMuted) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.muteLocalAudio(audioMuted);
    }

    public void muteLocalVideo(boolean videoMuted) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.muteLocalVideo(videoMuted);
    }

    public SurfaceViewRenderer getVideoView(String remotePeerId) {
        if (mSkylinkConnection != null)
            return mSkylinkConnection.getVideoView(remotePeerId);

        return null;
    }

    public SkylinkCaptureFormat[] getCaptureFormats(SkylinkConfig.VideoDevice videoDevice) {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getCaptureFormats(videoDevice);
        }

        return null;
    }

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

    public String getCurrentCameraName() {

        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getCurrentCameraName();
        }
        return null;
    }

    public SkylinkConfig.VideoDevice getCurrentVideoDevice() {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getCurrentVideoDevice();
        }
        return null;
    }

    public void setInputVideoResolution(int width, int height, int fps) {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.setInputVideoResolution(width, height, fps);
        }
    }

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

    public void switchCamera() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.switchCamera();
        }
    }

    public void changeAudioOutput(boolean isAudioSpeaker) {
        AudioRouter.changeAudioOutput(mContext, isAudioSpeaker);
    }

    public boolean getCurrentVideoSpeaker() {
        return currentVideoSpeaker;
    }

    public void setCurrentVideoSpeaker(boolean isSpeakerOn) {
        currentVideoSpeaker = isSpeakerOn;
    }
}
