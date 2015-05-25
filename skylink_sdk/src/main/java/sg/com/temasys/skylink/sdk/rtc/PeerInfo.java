package sg.com.temasys.skylink.sdk.rtc;

/**
 * Created by xiangrong on 30/3/15.
 */
public class PeerInfo {
    private String agent = "";
    private String version = "";
    private boolean receiveOnly = false;
    private boolean enableIceTrickle = false;
    private boolean enableDataChannel = false;

    /**
     * Checks if the values of a PeerInfo object are the same as this one.
     *
     * @param peerInfo The PeerInfo that is being compared.
     * @return true only if all values are the same as this one.
     */
    public boolean equals(PeerInfo peerInfo) {
        // Compare all attributes
        if (receiveOnly != peerInfo.isReceiveOnly()) {
            return false;
        } else if (!agent.equals(peerInfo.getAgent())) {
            return false;
        } else if (!version.equals(peerInfo.getVersion())) {
            return false;
        } else if (enableIceTrickle != peerInfo.isEnableIceTrickle()) {
            return false;
        } else if (enableDataChannel != peerInfo.isEnableDataChannel()) {
            return false;
        }

        return true;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public boolean isReceiveOnly() {
        return receiveOnly;
    }

    public void setReceiveOnly(boolean receiveOnly) {
        this.receiveOnly = receiveOnly;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isEnableIceTrickle() {
        return enableIceTrickle;
    }

    public void setEnableIceTrickle(boolean enableIceTrickle) {
        this.enableIceTrickle = enableIceTrickle;
    }

    public boolean isEnableDataChannel() {
        return enableDataChannel;
    }

    public void setEnableDataChannel(boolean enableDataChannel) {
        this.enableDataChannel = enableDataChannel;
    }

}
