package sg.com.temasys.skylink.sdk.sampleapp.service.model;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class VideoResolution {

    // Video size
    private int width;
    private int height;

    // Frame rate in frames per second (fps).
    private int fps;

    // The array of SkylinkCaptureFormats support by the current camera/screen.
    private SkylinkCaptureFormat[] captureFormats;

    // The selected SkylinkCaptureFormat on UI
    private SkylinkCaptureFormat currentCaptureFormat;

    // The current camera/screen name.
    private String currentDeviceName;

    public VideoResolution() {
        this.width = -1;
        this.height = -1;
        this.fps = -1;
    }

    public VideoResolution(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    public SkylinkCaptureFormat[] getCaptureFormats() {
        return captureFormats;
    }

    public void setCaptureFormats(SkylinkCaptureFormat[] captureFormats) {
        this.captureFormats = captureFormats;
    }

    public SkylinkCaptureFormat getCurrentCaptureFormat() {
        return currentCaptureFormat;
    }

    public void setCurrentCaptureFormat(SkylinkCaptureFormat currentCaptureFormat) {
        this.currentCaptureFormat = currentCaptureFormat;
    }

    public String getCurrentDeviceName() {
        return currentDeviceName;
    }

    public void setCurrentDeviceName(String currentDeviceName) {
        this.currentDeviceName = currentDeviceName;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }
}
