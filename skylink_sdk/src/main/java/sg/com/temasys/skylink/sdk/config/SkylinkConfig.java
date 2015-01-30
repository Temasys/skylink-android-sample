package sg.com.temasys.skylink.sdk.config;

import java.io.Serializable;
import java.util.Map;

/**
 * Configuration class used to configure the parameters of real time communication.
 *
 * @author Temasys Communications Pte Ltd
 */
public class SkylinkConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean audioSend;
    private boolean videoSend;
    private boolean audioReceive;
    private boolean videoReceive;
    private boolean peerMessaging;
    private boolean fileTransfer;
    private int timeout = 60;
    private Map<String, Object> advancedOptions;

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
     * Creates a new SkylinkConfig (Copy constructor).
     *
     * @param config Configuration object with settings to copy from.
     */
    public SkylinkConfig(SkylinkConfig config) {
        super();
        this.audioSend = config.audioSend;
        this.videoSend = config.videoSend;
        this.audioReceive = config.audioReceive;
        this.videoReceive = config.videoReceive;
        this.peerMessaging = config.peerMessaging;
        this.fileTransfer = config.fileTransfer;
        this.timeout = config.timeout;
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
        }

        // Set audio video receive with the same settings.
        setAudioVideoReceiveConfig(audioVideoConfig);
    }

    /**
     * Sets the audio video receive flag to the indicated boolean value.
     *
     * @param audioVideoConfig Audio video receive config value
     */
    private void setAudioVideoReceiveConfig(AudioVideoConfig audioVideoConfig) {
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
     * @return Timeout config value.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout value (in seconds) for various operations in the SDK, for e.g. file transfer waiting time for remote peer's response.
     *
     * @param timeout Timeout config value
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
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
        boolean result = false;
        if (advancedOptions != null) {
            Object object = advancedOptions.get("STUN");
            if (object != null)
                result = ((Boolean) object).booleanValue();
        }
        return result;
    }

    /**
     * @return TURN is enabled or not
     */
    public boolean isTurnDisabled() {
        boolean result = false;
        if (advancedOptions != null) {
            Object object = advancedOptions.get("TURN");
            if (object != null)
                result = ((Boolean) object).booleanValue();
        }
        return result;
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

    @Override
    public String toString() {
        return "TEMAConnectionConfig [videoSend=" + videoSend + ", audioSend=" + audioSend
                + ",videoReceive=" + videoReceive + ", audioReceive=" + audioReceive
                + ", p2PMessage=" + peerMessaging + ", fileTransfer=" + fileTransfer
                + ", timeout=" + timeout + "]";
    }

}