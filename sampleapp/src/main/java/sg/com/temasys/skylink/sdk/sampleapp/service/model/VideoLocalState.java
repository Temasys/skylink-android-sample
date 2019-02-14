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

    // current camera state
    private boolean cameraMute;

    public VideoLocalState() {
        this.audioMute = false;
        this.videoMute = false;
        this.cameraMute = false;
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

    public boolean isCameraMute() {
        return cameraMute;
    }

    public void setCameraMute(boolean cameraMute) {
        this.cameraMute = cameraMute;
    }
}
