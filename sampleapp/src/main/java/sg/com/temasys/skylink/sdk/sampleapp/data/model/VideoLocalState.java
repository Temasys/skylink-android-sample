package sg.com.temasys.skylink.sdk.sampleapp.data.model;

import java.io.Serializable;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class VideoLocalState implements Serializable{
    private boolean audioMute;
    private boolean videoMute;
    private boolean cameraToggle;

    public VideoLocalState() {
        this.audioMute = false;
        this.videoMute = false;
        this.cameraToggle = false;
    }

    public VideoLocalState(boolean audioMute, boolean videoMute, boolean cameraToggle) {
        this.audioMute = audioMute;
        this.videoMute = videoMute;
        this.cameraToggle = cameraToggle;
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

    public boolean isCameraToggle() {
        return cameraToggle;
    }

    public void setCameraToggle(boolean cameraToggle) {
        this.cameraToggle = cameraToggle;
    }
}
