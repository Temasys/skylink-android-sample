package sg.com.temasys.skylink.sdk.sampleapp.service.model;

import java.io.Serializable;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class VideoLocalState implements Serializable {
    // current audio state
    private boolean audioMute;

    // current video state
    private boolean videoMute;

    // current state of camera capturer;
    private boolean cameraCapturerStop;

    // current screen state
    private boolean screenMute;

    // current state of screen capturer;
    private boolean screenCapturerStop;

    public VideoLocalState() {
        this.audioMute = false;
        this.videoMute = false;
        this.screenMute = false;
        this.cameraCapturerStop = true;
        this.screenCapturerStop = true;
    }

    public boolean isAudioMute() {
        return audioMute;
    }

    public void setAudioMute(boolean audioMute) {
        this.audioMute = audioMute;
    }

    public boolean isVideoMute() {
        return videoMute;
    }

    public void setVideoMute(boolean videoMute) {
        this.videoMute = videoMute;
    }

    public boolean isCameraCapturerStop() {
        return cameraCapturerStop;
    }

    public void setCameraCapturerStop(boolean cameraCapturerStop) {
        this.cameraCapturerStop = cameraCapturerStop;
    }

    public boolean isScreenMute() {
        return screenMute;
    }

    public void setScreenMute(boolean screenMute) {
        this.screenMute = screenMute;
    }

    public boolean isScreenCapturerStop() {
        return screenCapturerStop;
    }

    public void setScreenCapturerStop(boolean screenCapturerStop) {
        this.screenCapturerStop = screenCapturerStop;
    }
}
