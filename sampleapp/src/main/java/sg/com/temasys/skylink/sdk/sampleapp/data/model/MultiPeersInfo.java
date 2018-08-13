package sg.com.temasys.skylink.sdk.sampleapp.data.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class MultiPeersInfo implements Serializable {
    private List<SkylinkPeer> peerList;

    public MultiPeersInfo() {
    }

    public MultiPeersInfo(List<SkylinkPeer> peerIdList) {
        this.peerList = peerIdList;
    }

    public List<SkylinkPeer> getPeerIdList() {
        return peerList;
    }

    public void setPeerIdList(List<SkylinkPeer> peerIdList) {
        this.peerList = peerIdList;
    }
}
