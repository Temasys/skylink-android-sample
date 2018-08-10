package sg.com.temasys.skylink.sdk.sampleapp.data.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class MultiPeersInfo implements Serializable{
    private boolean isPeerJoined;
    private List<String> peerIdList;

    public MultiPeersInfo() {
    }

    public MultiPeersInfo(boolean isPeerJoined, List<String> peerIdList) {
        this.isPeerJoined = isPeerJoined;
        this.peerIdList = peerIdList;
    }

    public boolean isPeerJoined() {
        return isPeerJoined;
    }

    public void setPeerJoined(boolean peerJoined) {
        isPeerJoined = peerJoined;
    }

    public List<String> getPeerIdList() {
        return peerIdList;
    }

    public void setPeerIdList(List<String> peerIdList) {
        this.peerIdList = peerIdList;
    }
}
