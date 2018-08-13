package sg.com.temasys.skylink.sdk.sampleapp.data.service;

import android.content.Context;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.VideoLocalState;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoCallContract;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class VideoService extends SDKService implements VideoCallContract.Service {

    private static VideoLocalState videoLocalState = new VideoLocalState();

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

    public boolean isAudioMuteServiceHandler() {
        if (videoLocalState != null)
            return videoLocalState.isAudioMute();

        return false;
    }

    public void setAudioMuteServiceHandler(boolean isAudioMuted) {
        videoLocalState.setAudioMute(isAudioMuted);
    }

    public boolean isVideoMuteServiceHandler() {
        if (videoLocalState != null)
            return videoLocalState.isVideoMute();

        return false;
    }

    public void setVideoMuteServiceHandler(boolean isVideoMuted) {
        videoLocalState.setVideoMute(isVideoMuted);
    }

    public boolean isCameraToggleServiceHandler() {
        if (videoLocalState != null)
            return videoLocalState.isCameraToggle();

        return false;
    }

    public void setCamToggleServiceHandler(boolean isCamToggle) {
        videoLocalState.setCameraToggle(isCamToggle);
    }

    public boolean toggleCameraServiceHandler() {
        if (mSkylinkConnection != null)
            return mSkylinkConnection.toggleCamera();
        return false;
    }

    public boolean toggleCameraServiceHandler(boolean isToggle) {
        if (mSkylinkConnection != null)
            return mSkylinkConnection.toggleCamera(isToggle);
        return false;
    }

    public void muteLocalAudioServiceHandler(boolean audioMuted) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.muteLocalAudio(audioMuted);
    }

    public void muteLocalVideoServiceHandler(boolean videoMuted) {
        if (mSkylinkConnection != null)
            mSkylinkConnection.muteLocalVideo(videoMuted);
    }

    public SurfaceViewRenderer getVideoViewServiceHandler(String remotePeerId) {
        if (mSkylinkConnection != null)
            return mSkylinkConnection.getVideoView(remotePeerId);

        return null;
    }

    public SkylinkCaptureFormat[] getCaptureFormatsServiceHandler(SkylinkConfig.VideoDevice videoDevice) {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getCaptureFormats(videoDevice);
        }

        return null;
    }

    public String getCaptureFormatsStringServiceHandler(SkylinkCaptureFormat[] captureFormats) {
        String strFormat = "No CaptureFormat currently registered.";
        String strFormats = "No CaptureFormats currently registered.";

        if (Utils.isCaptureFormatsValid(captureFormats)) {
            strFormats = Utils.captureFormatsToString(captureFormats);
        }

        // Get the current CaptureFormat, if there is one.
        SkylinkCaptureFormat captureFormat = mSkylinkConnection.getCaptureFormat();


        if (captureFormat != null) {
            strFormat = captureFormat.toString();
        }

        String captureFormatString = "Current capture format: " + strFormat + ".\r\n" +
                "Supported capture formats: " + strFormats + ".";
        return captureFormatString;
    }

    public String getCurrentCameraNameServiceHandler() {

        if(mSkylinkConnection != null){
            return mSkylinkConnection.getCurrentCameraName();
        }
        return null;
    }

    public SkylinkConfig.VideoDevice getCurrentVideoDeviceServiceHandler() {
        if(mSkylinkConnection != null){
            return mSkylinkConnection.getCurrentVideoDevice();
        }
        return null;
    }

    public void setInputVideoResolutionServiceHandler(int width, int height, int fps) {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.setInputVideoResolution(width, height, fps);
        }
    }

    public void getVideoResolutionsServiceHandler(String peerId) {


        if (mSkylinkConnection == null) {
            return;
        }

        mSkylinkConnection.getInputVideoResolution();

        if (peerId != null) {

            mSkylinkConnection.getSentVideoResolution(peerId);

            mSkylinkConnection.getReceivedVideoResolution(peerId);
        }
    }

    public void switchCameraServiceHandler() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.switchCamera();
        }
    }
}
