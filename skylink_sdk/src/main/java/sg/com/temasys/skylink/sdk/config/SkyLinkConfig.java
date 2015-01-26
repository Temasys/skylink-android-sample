package sg.com.temasys.skylink.sdk.config;

import java.io.Serializable;
import java.util.Map;

/**
 * Configuration class used to configure the parameters of conversation.
 *
 * @author temasys
 */
public class SkyLinkConfig implements Serializable {

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
     * List of enums that specify an audio video setting.
     */
    public enum AudioVideoConfig {
        NO_AUDIO_NO_VIDEO,
        AUDIO_ONLY,
        VIDEO_ONLY,
        AUDIO_AND_VIDEO,
    }

    /**
     * Creates a new SkyLinkConfig object.
     */
    public SkyLinkConfig() {
        super();
    }

    /**
     * Creates a new SkyLinkConfig (Copy constructor).
     *
     * @param config Configuration object to copy from
     */
    public SkyLinkConfig(SkyLinkConfig config) {
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
     * @return Audio send config value.
     */
    public boolean hasAudioSend() {
        return audioSend;
    }

    /**
     * @return Video send config value.
     */
    public boolean hasVideoSend() {
        return videoSend;
    }

    /**
     * @return Audio receive config value.
     */
    public boolean hasAudioReceive() {
        return audioReceive;
    }

    /**
     * @return Video receive config value.
     */
    public boolean hasVideoReceive() {
        return videoReceive;
    }

    /**
     * Sets the audio video send flag to the indicated boolean value.
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
        }
    }

    /**
     * @return PeerMessaging config value.
     */
    public boolean hasPeerMessaging() {
        return peerMessaging;
    }

    /**
     * Sets the peerMessaging flag for this object to the indicated boolean
     * value.
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
     * Sets the fileTransfer flag for this object to the indicated boolean
     * value.
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
     * Sets the timeout value of this object.
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
     * @param advancedOptions A map containing optional entries as follows:
     *                        "STUN":"boolean", "TURN":"boolean", "transport":"TCP/UDP"
     */
    public void setAdvancedOptions(Map<String, Object> advancedOptions) {
        this.advancedOptions = advancedOptions;
    }

    /**
     * @return Stun status
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
     * @return Turn status
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
     * @return transport used
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