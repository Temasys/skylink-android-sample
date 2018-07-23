package sg.com.temasys.skylink.sdk.sampleapp.data.model;

import java.io.Serializable;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class AudioRemotePeer implements Serializable{
    private boolean isPeerJoined;
    private String remotePeerId;
    private String remotePeerName;

    public AudioRemotePeer() {
    }

    public AudioRemotePeer(boolean isPeerJoined, String remotePeerId, String remotePeerName) {
        this.isPeerJoined = isPeerJoined;
        this.remotePeerId = remotePeerId;
        this.remotePeerName = remotePeerName;
    }

    public boolean isPeerJoined() {
        return isPeerJoined;
    }

    public void setPeerJoined(boolean peerJoined) {
        isPeerJoined = peerJoined;
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
