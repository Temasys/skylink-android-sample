package sg.com.temasys.skylink.sdk.sampleapp.data.model;

import java.io.Serializable;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class MultiPeersInfo implements Serializable{
    private boolean isPeerJoined;
    private String[] peerIdList;

    public MultiPeersInfo() {
    }

    public MultiPeersInfo(boolean isPeerJoined, String[] peerIdList) {
        this.isPeerJoined = isPeerJoined;
        this.peerIdList = peerIdList;
    }

    public boolean isPeerJoined() {
        return isPeerJoined;
    }

    public void setPeerJoined(boolean peerJoined) {
        isPeerJoined = peerJoined;
    }

    public String[] getPeerIdList() {
        return peerIdList;
    }

    public void setPeerIdList(String[] peerIdList) {
        this.peerIdList = peerIdList;
    }
}
