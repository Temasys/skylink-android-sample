package sg.com.temasys.skylink.sdk.sampleapp.service.model;

import java.io.Serializable;

public class SkylinkPeer implements Serializable {

    private String peerId;
    private String peerName;

    public SkylinkPeer() {
    }

    public SkylinkPeer(String peerId) {
        this.peerId = peerId;
    }

    public SkylinkPeer(String peerId, String peerName) {
        this.peerId = peerId;
        this.peerName = peerName;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public String getPeerName() {
        return peerName;
    }

    public void setPeerName(String peerName) {
        this.peerName = peerName;
    }

    @Override
    public String toString() {
        return peerName + "(" + peerId + ")";
    }
}
