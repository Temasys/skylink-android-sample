package sg.com.temasys.skylink.sdk.rtc;

/**
 * Created by xiangrong on 6/7/15.
 */

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PeerPool manages the Peer objects. It gets info on the max number of peers that can be in the
 * PeerPool at one time. It has logic to constrain number of peers to that max.
 */
class PeerPool {

    /**
     * ConcurrentHashMap to contain all the current Peers (MCU Peer not included).
     */
    Map<String, Peer> peerMap;

    /**
     * The MCU peer, if it exists in the room.
     */
    Peer peerMcu;

    /**
     * The client object using PeerPool. Some info, like the max number of Peers at one time will be
     * provided by it.
     */
    PeerPoolClient peerPoolClient;

    /**
     * Lock for integrity of PeerPool. - addPeer will not violate Max Peer. - canAddPeer will report
     * after current add or remove Peer is complete.
     */
    private Object lock = new Object();

    public PeerPool(PeerPoolClient peerPoolClient) {
        this.peerPoolClient = peerPoolClient;
        peerMap = new ConcurrentHashMap<String, Peer>(peerPoolClient.getMaxPeer());
    }

    /**
     * Add a Peer into PeerPool, if within max Peers limit.
     *
     * @param peer
     * @return
     */
    boolean addPeer(Peer peer) {
        synchronized (lock) {
            //Check if can add peer
            if (canAddPeer()) {
                peerMap.put(peer.getPeerId(), peer);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Checks if currently possible to add a Peer into PeerPool, abiding by max number of peers
     * constraint. Note that this does not guarantee that by the time a Peer is ready to be added
     * into PeerPool, there will be room to add it in. addPeer will be the final check of that.
     *
     * @return
     */
    boolean canAddPeer() {
        synchronized (lock) {
            if (getPeerNumber() < getMaxPeer()) {
                return true;
            } else {
                return false;
            }
        }

    }

    /**
     * Get a particular Peer.
     *
     * @param peerId
     * @return Peer or null if unable to find peer.
     */
    Peer getPeer(String peerId) {
        return peerMap.get(peerId);
    }

    /**
     * Get the set of PeerIds.
     *
     * @return PeerId set.
     */
    Set<String> getPeerIdSet() {
        return peerMap.keySet();
    }

    /**
     * Get the Collection of Peers.
     *
     * @return Peer Collection.
     */
    Collection<Peer> getPeerCollection() {
        return peerMap.values();
    }

    /**
     * Get the number of Peers currently connected with us.
     *
     * @return
     */
    int getPeerNumber() {
        return peerMap.size();
    }

    /**
     * Remove a Peer from PeerPool.
     *
     * @param peerId
     */
    void removePeer(String peerId) {
        synchronized (lock) {
            peerMap.remove(peerId);
        }
    }

    // Internal functions

    /**
     * Get max no. Peers allowed from PeerPoolClient.
     *
     * @return
     */
    private int getMaxPeer() {
        return peerPoolClient.getMaxPeer();
    }

    // Getters and Setters
    public Peer getPeerMcu() {
        return peerMcu;
    }

    public void setPeerMcu(Peer peerMcu) {
        this.peerMcu = peerMcu;
    }

}


interface PeerPoolClient {
    int getMaxPeer();
}