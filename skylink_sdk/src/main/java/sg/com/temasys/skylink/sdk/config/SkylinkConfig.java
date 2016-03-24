package sg.com.temasys.skylink.sdk.config;

import java.io.Serializable;
import java.util.Map;

import sg.com.temasys.skylink.sdk.BuildConfig;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logE;

/**
 * Configuration class used to configure the parameters of real time communication.
 *
 * @author Temasys Communications Pte Ltd
 */
public class SkylinkConfig implements Serializable {

    private static final String TAG = SkylinkConfig.class.getName();
    private static final long serialVersionUID = 1L;

    public static int MAX_PEERS = 4;

    public static int MAX_VIDEO_HEIGHT = 480;
    public static int MAX_VIDEO_WIDTH = 640;
    public static int MAX_VIDEO_FPS = 30;

    private boolean audioSend;
    private boolean videoSend;
    private boolean audioReceive;
    private boolean videoReceive;
    private boolean dataTransfer;
    /**
     * This allows external logs (info, warn, error) to be logged if true.
     * By default it takes the value of DEBUG in BuildConfig, so it is true for debug builds,
     * false for release builds.
     * However, it can be set by SDK users.
     */
    private boolean enableLogs = BuildConfig.DEBUG;
    private boolean fileTransfer;
    private int maxPeers = MAX_PEERS;
    /**
     * Config to set if local front camera video view should be mirrored.
     */
    private boolean mirrorLocalView;
    private boolean peerMessaging;
    private AudioCodec preferredAudioCodec = AudioCodec.OPUS;
    private boolean stereoAudio = true;
    private int videoHeight = MAX_VIDEO_HEIGHT;
    private int videoWidth = MAX_VIDEO_WIDTH;
    private int videoFps = MAX_VIDEO_FPS;
    private int timeout = 60;
    private Map<String, Object> advancedOptions;

    /**
     * Audio codec to be used
     */
    public enum AudioCodec {

        OPUS("opus"), ISAC("ISAC");

        private final String value;

        AudioCodec(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return this.getValue();
        }
    }

    /**
     * List of enums that specify an audio video setting. Each option restricts the sending and
     * receiving of the local user's audio and/or video stream.
     */
    public enum AudioVideoConfig {
        NO_AUDIO_NO_VIDEO,
        AUDIO_ONLY,
        VIDEO_ONLY,
        AUDIO_AND_VIDEO,
    }

    /**
     * Creates a new SkylinkConfig object.
     */
    public SkylinkConfig() {
        super();
    }

    /**
     * @return true if sending user's audio stream is enabled
     */
    public boolean hasAudioSend() {
        return audioSend;
    }

    /**
     * @return true if sending user's video stream is enabled
     */
    public boolean hasVideoSend() {
        return videoSend;
    }

    /**
     * @return true if receiving remote peer's audio stream is enabled
     */
    public boolean hasAudioReceive() {
        return audioReceive;
    }

    /**
     * @return true if receiving remote peer's video stream is enabled
     */
    public boolean hasVideoReceive() {
        return videoReceive;
    }

    /**
     * Sets the audio video send and receive flag to the indicated boolean value.
     *
     * @param audioVideoConfig Audio video send config value
     */
    public void setAudioVideoSendConfig(AudioVideoConfig audioVideoConfig) {
        switch (audioVideoConfig) {
            case NO_AUDIO_NO_VIDEO:
                this.audioSend = false;
                this.videoSend = false;
                break;
            case AUDIO_ONLY:
                this.audioSend = true;
                this.videoSend = false;
                break;
            case VIDEO_ONLY:
                this.audioSend = false;
                this.videoSend = true;
                break;
            case AUDIO_AND_VIDEO:
                this.audioSend = true;
                this.videoSend = true;
                break;
            default:
                String error = "[ERROR] Send Audio and Video config not set.\n" +
                        "Due to unknown Audio Video type: \"" + audioVideoConfig + "\".";
                logE(TAG, error);
        }
    }

    /**
     * Sets the audio video receive flag to the indicated boolean value.
     *
     * @param audioVideoConfig Audio video receive config value
     */
    public void setAudioVideoReceiveConfig(AudioVideoConfig audioVideoConfig) {
        switch (audioVideoConfig) {
            case NO_AUDIO_NO_VIDEO:
                this.audioReceive = false;
                this.videoReceive = false;
                break;
            case AUDIO_ONLY:
                this.audioReceive = true;
                this.videoReceive = false;
                break;
            case VIDEO_ONLY:
                this.audioReceive = false;
                this.videoReceive = true;
                break;
            case AUDIO_AND_VIDEO:
                this.audioReceive = true;
                this.videoReceive = true;
                break;
            default:
                String error = "[ERROR] Receive Audio and Video config not set.\n" +
                        "Due to unknown Audio Video type: \"" + audioVideoConfig + "\".";
                logE(TAG, error);
        }
    }

    /**
     * @return PeerMessaging config value.
     */
    public boolean hasPeerMessaging() {
        return peerMessaging;
    }

    /**
     * Sets if peerMessaging is enabled.
     *
     * @param peerMessaging PeerMessaging config value
     */
    public void setHasPeerMessaging(boolean peerMessaging) {
        this.peerMessaging = peerMessaging;
    }

    /**
     * @return FileTransfer config value.
     */
    public boolean hasFileTransfer() {
        return fileTransfer;
    }

    /**
     * Sets the fileTransfer is enabled.
     *
     * @param fileTransfer FileTransfer config value
     */
    public void setHasFileTransfer(boolean fileTransfer) {
        this.fileTransfer = fileTransfer;
    }

    /**
     * Checks if logging is enabled for SDK.
     *
     * @return
     */
    public boolean isEnableLogs() {
        return enableLogs;
    }

    /**
     * Sets if logging should be enabled for SDK.
     * By default, logging is disabled for released SDK.
     * Enabling logs might be useful for debugging.
     *
     * @param enableLogs True/false to enable/disable logs.
     */
    public void setEnableLogs(boolean enableLogs) {
        this.enableLogs = enableLogs;
    }

    /**
     * @return DataTransfer config value.
     */
    public boolean hasDataTransfer() {
        return dataTransfer;
    }

    /**
     * Sets if dataTransfer is enabled.
     *
     * @param dataTransfer DataTransfer config value
     */
    public void setHasDataTransfer(boolean dataTransfer) {
        this.dataTransfer = dataTransfer;
    }

    /**
     * @return mirrorLocalView Whether local front camera video view will be mirrored.
     */
    public boolean isMirrorLocalView() {
        return mirrorLocalView;
    }

    /**
     * Sets whether local view rendered is a mirror image of the actual video.
     * <p/>
     * Default is false.
     * <p/>
     * When set to true:
     * <p/>
     * <ul>
     * <p/>
     * <li>Only front (and not back) camera local view will be mirrored.</li>
     * <p/>
     * <li>This will not change (e.g. mirror) the remote video view of us as seen by a remote
     * Peer.</li>
     * <p/>
     * <li>Will create a new GLSurfaceView (at onLocalMediaCapture) each time camera is
     * switched.</li>
     * <p/>
     * <ul>
     * <p/>
     * <li>User need to handle this new GLSurfaceView to display self video correctly.</li>
     * <p/>
     * </ul>
     * <p/>
     * </ul>
     *
     * @param mirrorLocalView True to mirror local view, false to show local view as it is.
     */
    public void setMirrorLocalView(boolean mirrorLocalView) {
        this.mirrorLocalView = mirrorLocalView;
    }

    /**
     * @return Timeout config value.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout value (in seconds) for various operations in the SDK, for e.g. file transfer
     * waiting time for remote peer's response.
     *
     * @param timeout Timeout config value
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * @return Gets the maximum number of Peers that we can connect to at one time.
     */
    public int getMaxPeers() {
        return maxPeers;
    }

    /**
     * Sets the maximum number of Peers that we can connect to at one time. If not set, default is
     * 4, i.e. we can connect up to 4 Peers at one time.
     *
     * @param maxPeers Timeout config value
     */
    public void setMaxPeers(int maxPeers) {
        this.maxPeers = maxPeers;
    }

    /**
     * @return Map of Advanced Options.
     */
    public Map<String, Object> getAdvancedOptions() {
        return advancedOptions;
    }

    /**
     * Sets advanced options. (For advanced users only).
     *
     * @param advancedOptions A map containing optional entries as follows: "STUN":"boolean" or
     *                        "STUN",boolean "TURN":"boolean", or "TURN":boolean "transport":"UDP"
     *                        or "transport":"TCP"
     */
    public void setAdvancedOptions(Map<String, Object> advancedOptions) {
        this.advancedOptions = advancedOptions;
    }

    /**
     * @return STUN is enabled or not
     */
    public boolean isStunDisabled() {
        if (advancedOptions != null) {
            Object object = advancedOptions.get("STUN");
            if (object != null) {
                return !((Boolean) object).booleanValue();
            }
        }
        return false;
    }

    /**
     * @return TURN is enabled or not
     */
    public boolean isTurnDisabled() {
        boolean result = false;
        if (advancedOptions != null) {
            Object object = advancedOptions.get("TURN");
            if (object != null) {
                result = !((Boolean) object).booleanValue();
            }
        }
        return false;
    }

    /**
     * @return transport used. Possible values are "UDP" or "TCP"
     */
    public String getTransport() {
        String result = null;
        if (advancedOptions != null) {
            Object object = advancedOptions.get("transport");
            if (object != null)
                result = (String) object;
        }
        return result;
    }

    /**
     * @return preferred audio codec used. Possible values {@link sg.com.temasys.skylink.sdk.config.SkylinkConfig.AudioCodec}
     */
    public AudioCodec getPreferredAudioCodec() {
        return preferredAudioCodec;
    }

    /**
     * Sets the preferredAudioCodec. Possible values {@link sg.com.temasys.skylink.sdk.config.SkylinkConfig.AudioCodec}
     *
     * @param preferredAudioCodec
     */
    public void setPreferredAudioCodec(AudioCodec preferredAudioCodec) {
        this.preferredAudioCodec = preferredAudioCodec;
    }

    /**
     * @return the video height
     */
    public int getVideoHeight() {
        return videoHeight;
    }

    /**
     * Sets the video height.
     *
     * @param videoHeight
     */
    public void setVideoHeight(int videoHeight) {
        this.videoHeight = Math.min(videoHeight, MAX_VIDEO_HEIGHT);
    }

    /**
     * @return the video width
     */
    public int getVideoWidth() {
        return videoWidth;
    }

    /**
     * Sets the video width
     *
     * @param videoWidth
     */
    public void setVideoWidth(int videoWidth) {
        this.videoWidth = Math.min(videoWidth, MAX_VIDEO_WIDTH);
    }

    /**
     * @return true if stereo audio is used
     */
    public boolean isStereoAudio() {
        return stereoAudio;
    }

    /**
     * Sets if stereo audio is enabled.
     *
     * @param stereoAudio configuration value
     */
    public void setStereoAudio(boolean stereoAudio) {
        this.stereoAudio = stereoAudio;
    }

    /**
     * @return the video FPS
     */
    public int getVideoFps() {
        return videoFps;
    }

    /**
     * Sets the video FPS
     *
     * @param videoFps
     */
    public void setVideoFps(int videoFps) {
        this.videoFps = Math.min(videoFps, MAX_VIDEO_FPS);
    }

    @Override
    public String toString() {
        return "SkylinkConfig [videoSend=" + videoSend + ", audioSend=" + audioSend
                + ",videoReceive=" + videoReceive + ", audioReceive=" + audioReceive
                + ", p2PMessage=" + peerMessaging + ", fileTransfer=" + fileTransfer
                + ", preferredAudioCodec=" + preferredAudioCodec
                + ", timeout=" + timeout + "]";
    }

}