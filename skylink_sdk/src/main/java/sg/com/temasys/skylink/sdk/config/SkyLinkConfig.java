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

    private boolean video;
    private boolean audio;
    private boolean peerMessaging;
    private boolean fileTransfer;
    private int timeout = 60;
    private Map<String, Object> advancedOptions;

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
        this.video = config.video;
        this.audio = config.audio;
        this.peerMessaging = config.peerMessaging;
        this.fileTransfer = config.fileTransfer;
        this.timeout = config.timeout;
    }

    /**
     * @return Audio config value.
     */
    public boolean hasAudio() {
        return audio;
    }

    /**
     * Sets the audio flag for this object to the indicated boolean value.
     *
     * @param audio Audio config value
     */
    public void setHasAudio(boolean audio) {
        this.audio = audio;
        this.video = this.video && this.audio;
    }

    /**
     * @return Video config value.
     */
    public boolean hasVideo() {
        return video;
    }

    /**
     * Sets the video flag for this object to the indicated boolean value.
     *
     * @param video Video config value
     */
    public void setHasVideo(boolean video) {
        this.video = video && this.audio;
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
        return "TEMAConnectionConfig [video=" + video + ", audio=" + audio
                + ", p2PMessage=" + peerMessaging + ", fileTransfer="
                + fileTransfer + ", timeout=" + timeout + "]";
    }

}