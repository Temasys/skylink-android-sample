package sg.com.temasys.skylink.sdk.rtc;

/**
 * Created by xiangrong on 30/3/15.
 */
class PeerInfo {
    private boolean receiveOnly = false;
    private String agent = "";
    private String version = "";
    private boolean enableIceTrickle = false;
    private boolean enableDataChannel = false;

    String getAgent() {
        return agent;
    }

    void setAgent(String agent) {
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
