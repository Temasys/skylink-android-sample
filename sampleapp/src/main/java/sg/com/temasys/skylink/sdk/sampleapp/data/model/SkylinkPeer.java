package sg.com.temasys.skylink.sdk.sampleapp.data.model;

public class SkylinkPeer {

    private String remotePeerId;
    private String remotePeerName;

    public SkylinkPeer() {
    }

    public SkylinkPeer(String remotePeerId, String remotePeerName) {
        this.remotePeerId = remotePeerId;
        this.remotePeerName = remotePeerName;
    }

    public String getRemotePeerId() {
        return remotePeerId;
    }

    public void setRemotePeerId(String remotePeerId) {
        this.remotePeerId = remotePeerId;
    }

    public String getRemotePeerName() {
        return remotePeerName;
    }

    public void setRemotePeerName(String remotePeerName) {
        this.remotePeerName = remotePeerName;
    }
}
