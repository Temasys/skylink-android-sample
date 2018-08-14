package sg.com.temasys.skylink.sdk.sampleapp.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class MultiPeersInfo implements Serializable {
    private List<SkylinkPeer> peerList;

    public MultiPeersInfo() {
        this.peerList = new ArrayList<SkylinkPeer>();
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

    public void addPeer(SkylinkPeer skylinkPeer){
        peerList.add(skylinkPeer);
    }

    public SkylinkPeer removePeer(String peerId){

        for (SkylinkPeer peer: peerList) {
            if(peer.getPeerId().equals(peerId)){
                peerList.remove(peer);
                return peer;
            }
        }

        return null;

    }

    public int getSize(){
        return peerList.size();
    }

    public SkylinkPeer getPeerByIndex(int index){
        return peerList.get(index);
    }

    public void clearAllPeers(){
        peerList.clear();
    }
}
