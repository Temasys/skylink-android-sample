package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by xiangrong on 4/5/15.
 */
class SkylinkPeerService implements PeerPoolClient {

    private static final String TAG = SkylinkPeerService.class.getSimpleName();
    private UserInfo myUserInfo;

    private final SkylinkConnection skylinkConnection;
    private SkylinkConnectionService skylinkConnectionService;
    private PeerPool peerPool;
    private PcShared pcShared;
    private WebrtcPeerService webrtcPeerService;
    private boolean isFirstPeer = true;

    public SkylinkPeerService(SkylinkConnection skylinkConnection, PcShared pcShared) {
        this.skylinkConnection = skylinkConnection;
        this.peerPool = new PeerPool(this);
        this.pcShared = pcShared;
        this.webrtcPeerService = new WebrtcPeerService(pcShared);
    }

    /**
     * Creates a Peer object with all related PeerConnection objects, for e.g.: PC,
     * SkylinkPcObserver, SkylinkSdpObserver, DC. Not added into PeerPool yet, should be done in
     * subsequent step(s).
     *
     * @param peerId
     * @param iceRole
     * @param userInfo
     * @param peerInfo
     * @return
     */
    Peer createPeer(String peerId, String iceRole, UserInfo userInfo, PeerInfo peerInfo) {
        Peer peer;
        boolean isMcu = false;

        // Check if Peer is MCU
        // MCU, if present, will always be first Peer to send welcome.
        if (isFirstPeer) {
            isFirstPeer = false;
            isMcu = isPeerIdMCU(peerId);
            if (isMcu) {
                // Set SkylinkConnection MCU flag
                this.skylinkConnection.setIsMcuRoom(isMcu);
                // Set DataChannelManager MCU flag.
                if (skylinkConnection.getDataChannelManager() != null) {
                    skylinkConnection.getDataChannelManager().setIsMcuRoom(skylinkConnection.isMcuRoom());
                }
            }
        }

        // Create a new Peer if there is currently room.
        if (peerPool.canAddPeer() || isMcu) {
            // Create a basic Peer and populate it more later.
            peer = new Peer(peerId, skylinkConnection);
            peer.setUserInfo(userInfo);
            peer.setPeerInfo(peerInfo);

            // Create SkylinkPcObserver
            SkylinkPcObserver pcObserver = new SkylinkPcObserver(peerId, peer, skylinkConnection);
            peer.setPcObserver(pcObserver);

            // Prevent thread from executing with disconnect concurrently.
            synchronized (skylinkConnection.getLockDisconnect()) {
                // Create PeerConnection
                if (!webrtcPeerService.addWebrtcP2PComponent(peer, skylinkConnectionService)) {
                    return null;
                }
            }

            // Initialise and start Health Checker.
            peer.initialiseHealthChecker(iceRole);

            // Create SkylinkSdpObserver
            SkylinkSdpObserver sdpObserver =
                    new SkylinkSdpObserver(peerId, peer, skylinkConnection);
            peer.setSdpObserver(sdpObserver);

            // Add and return new Peer
            if (isMcu) {
                // Set a MCU Peer
                peerPool.setPeerMcu(peer);
            } else {
                // Add a normal Peer if possible
                if (!peerPool.addPeer(peer)) {
                    Log.d(TAG, "Unable to create PeerConnection for Peer " + userInfo.getUserData() +
                            " (" + peerId + ") as I only support " +
                            skylinkConnection.getSkylinkConfig().getMaxPeers() + " connections " +
                            "in this app.");
                    return null;
                }
            }
            return peer;
        } else {
            // Return null if Peer cannot be created
            return null;
        }

    }

    static boolean isPeerIdMCU(String peerId) {
        boolean mcu = peerId.startsWith("MCU");
        if (mcu) {
            Log.d(TAG, "MCU Detected");
        }
        return mcu;
    }

    /**
     * Dispose all PeerConnections and associated DC. Including MCU DC if present.
     *
     * @param reason
     */
    void removeAllPeers(final String reason) {
        if (peerPool.getPeerNumber() > 0) {
            // Create a new peerId set to prevent concurrent modification of the set
            Set<String> peerIdSet = new HashSet<String>(peerPool.getPeerIdSet());
            // Remove each Peer
            for (final String peerId : peerIdSet) {
                removePeer(peerId, reason);
            }
        }
    }

    /**
     * Dispose all objects in Peer and remove Peer from PeerPool. Notify user that Peer has left if
     * reason is not null.
     *
     * @param peerId PeerId of Peer that had left.
     * @param reason Reason for removing Peer, will be conveyed to user if not null.
     * @return
     */
    boolean removePeer(final String peerId, final String reason) {
        Peer peer = getPeer(peerId);

        if (peer != null) {
            if (reason != null) {
                // Notify that the Peer has left
                ProtocolHelper.notifyPeerLeave(skylinkConnection, peerId, reason);
            }
            // Dispose of webrtc objects in Peer.
            disposePeer(peerId);
            // Remove from PeerPool
            peerPool.removePeer(peerId);
            return true;
        } else {
            return false;
        }
    }

    void receivedEnter(String peerId, PeerInfo peerInfo, UserInfo userInfo) {
        // Create a new Peer if we can
        Peer peer = createPeer(peerId, HealthChecker.ICE_ROLE_ANSWERER, userInfo, peerInfo);

        // If we are over the max no. of peers, peer here will be null.
        if (peer != null) {

            // Add our local media stream to this PC, or not.
            if ((skylinkConnection.getSkylinkConfig().hasAudioSend() || skylinkConnection.getSkylinkConfig().hasVideoSend())) {
                peer.getPc().addStream(getSkylinkMediaService().getLocalMediaStream());
                Log.d(TAG, "Added localMedia Stream");
            }
            try {
                ProtocolHelper.sendWelcome(peerId, skylinkConnection, false);
            } catch (JSONException e) {
                Log.d(TAG, e.getMessage(), e);
            }

        } else {
            // We have reached the limit of max no. of Peers.
            Log.d(TAG, "Discarding this \"enter\" due to Peer number limit.");
        }
    }

    void receivedBye(final String peerId) {

        if (!isPeerIdMCU(peerId)) {
            // Actually remove the Peer.
            removePeer(peerId, "The peer has left the room");
        }
    }

    /**
     * Adds the remote Peer's ICE Candidates
     *
     * @param peerId
     * @param iceCandidate
     */
    void addIceCandidate(String peerId, IceCandidate iceCandidate) {
        Peer peer = getPeer(peerId);
        webrtcPeerService.addIceCandidate(iceCandidate, peer);
    }

    void receivedInRoom(String peerId, List<PeerConnection.IceServer> iceServers) {

        skylinkConnection.getSkylinkConnectionService().setSid(peerId);

        skylinkConnectionService.setIceServers(iceServers);

        // Set mid and displayName in DataChannelManager
        if (skylinkConnection.getDataChannelManager() != null) {
            skylinkConnection.getDataChannelManager().setMid(peerId);
            skylinkConnection.getDataChannelManager().setDisplayName(
                    getUserData(null).toString());
        }

        // Check if pcObserverPool has been populated.
        if (getPeerNumber() > 0) {
            // If so, chances are this is a rejoin of room.
            // Log it
            Log.d(TAG, "[receivedInRoom] This is a rejoin of room.");
        }

        // Send enter.
        try {
            ProtocolHelper.sendEnter(null, skylinkConnection,
                    skylinkConnection.getSkylinkConnectionService());
        } catch (JSONException e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    void receivedOfferAnswer(String peerId, String sdp, String type) {
        // Set the preferred audio codec
        String sdpString = Utils.preferCodec(sdp,
                skylinkConnection.getSkylinkConfig().getPreferredAudioCodec().toString(), true);
        Peer peer = getPeer(peerId);

        // Set the remote SDP
        webrtcPeerService.setRemoteSdp(sdpString, peer, type);
    }

    /**
     * Get the Peer via its PeerId.
     *
     * @param peerId
     * @return
     */
    Peer getPeer(String peerId) {
        if (peerPool == null) {
            return null;
        }
        // See if we should return MCU Peer or normal Peer.
        if (isPeerIdMCU(peerId)) {
            return peerPool.getPeerMcu();
        } else {
            return peerPool.getPeer(peerId);
        }
    }

    /**
     * Get the set of PeerIds.
     *
     * @return PeerId set.
     */
    Set<String> getPeerIdSet() {
        return peerPool.getPeerIdSet();
    }

    /**
     * Gets PeerInfo object of a specific peer.
     *
     * @param peerId PeerId of specific peer for which PeerInfo is desired.
     */
    PeerInfo getPeerInfo(String peerId) {
        Peer peer = getPeer(peerId);
        if (peer != null) {
            return peer.getPeerInfo();
        }
        return null;
    }

    /**
     * Get the number of Peers currently connected with us.
     *
     * @return
     */
    int getPeerNumber() {
        return peerPool.getPeerNumber();
    }

    /**
     * Get the Collection of Peers.
     *
     * @return Peer Collection.
     */
    Collection<Peer> getPeerCollection() {
        return peerPool.getPeerCollection();
    }


    /**
     * Get the SkylinkSdpObserver of a Peer.
     *
     * @param mid
     * @return SkylinkSdpObserver or null if Peer is not available.
     */
    SkylinkSdpObserver getSdpObserver(String mid) {

        Peer peer = getPeer(mid);
        if (peer != null) {
            SkylinkSdpObserver sdpObserver = peer.getSdpObserver();
            return sdpObserver;
        }
        return null;
    }

    void receivedWelcomeRestart(String peerId, PeerInfo peerInfo,
                                UserInfo userInfo, double weight, boolean isRestart) {

        // For restart, existing Peer has to be first removed, before processing like a welcome
        if (isRestart) {
            removePeer(peerId, ProtocolHelper.PEER_CONNECTION_RESTART);
        }

        // Check if a Peer was already created at "enter"
        if (getPeer(peerId) != null) {
            // Check if should continue with "welcome" or use Peer from "enter"
            if (shouldAcceptWelcome(peerId, weight)) {
                // Remove Peer from "enter" to create new Peer from "welcome"
                removePeer(peerId, null);
            } else {
                // Do not continue "welcome" but use Peer from "enter" instead.
                Log.d(TAG, "Ignoring this welcome as Peer " + userInfo.getUserData() +
                        " (" + peerId + ") is processing our welcome.");
                return;
            }
        }

        // Create a Peer from "welcome"
        Peer peer = createPeer(peerId, HealthChecker.ICE_ROLE_OFFERER, userInfo, peerInfo);

        // We have reached the limit of max no. of Peers.
        if (peer == null) {
            Log.d(TAG, "Discarding this \"welcome\" due to Peer number limit.");
            return;
        }

        /*peer.setPeerId(peerId);
        peer.setUserInfo(userInfo);*/

        boolean receiveOnly = peerInfo.isReceiveOnly();

        // Add our local media stream to this PC, or not.
        if ((skylinkConnection.getSkylinkConfig().hasAudioSend() ||
                skylinkConnection.getSkylinkConfig().hasVideoSend()) && !receiveOnly) {
            peer.getPc().addStream(getSkylinkMediaService().getLocalMediaStream());
            Log.d(TAG, "Added localMedia Stream");
        }

        Log.d(TAG, "[receivedWelcomeRestart] - create offer.");
        // Create DataChannel if both Peer and ourself desires it.
        if (peerInfo.isEnableDataChannel() &&
                (skylinkConnection.getSkylinkConfig().hasPeerMessaging()
                        || skylinkConnection.getSkylinkConfig().hasFileTransfer()
                        || skylinkConnection.getSkylinkConfig().hasDataTransfer())) {
            // It is stored by dataChannelManager and Peer.
            DataChannel dc = skylinkConnection.getDataChannelManager().createDataChannel(
                    peer.getPc(), skylinkConnection.getSkylinkConnectionService().getSid(),
                    peerId, "", null, peerId);
            peer.setDc(dc);
        }

        // Create SkylinkSdpObserver for Peer.
        SkylinkSdpObserver sdpObserver = getSdpObserver(peerId);

        peer.getPc().createOffer(sdpObserver,
                pcShared.getSdpMediaConstraints());
        Log.d(TAG, "[receivedWelcomeRestart] - createOffer for " + peerId);
    }

    // Internal methods

    /**
     * Disposes all webrtc objects in a Peer.
     *
     * @param peerId
     * @return True if properly disposed. False if error occurred.
     */
    private boolean disposePeer(String peerId) {
        Peer peer = getPeer(peerId);
        if (peer != null) {
            // Dispose DC
            if (skylinkConnection.getDataChannelManager() != null) {
                skylinkConnection.getDataChannelManager().disposeDC(peerId, true);
                peer.setDc(null);
            }
            // Dispose peer connection
            disposePeerConnection(peerId);
            // Dispose other webrtc objects
            peer.setPcObserver(null);
            peer.setSdpObserver(null);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Dispose the PeerConnection of a Peer.
     *
     * @param remotePeerId
     * @return True if the peer connection is disposed successfully, false if Peer does not exists.
     */
    private boolean disposePeerConnection(String remotePeerId) {

        Peer peer = getPeer(remotePeerId);
        if (peer != null) {
            PeerConnection peerConnection = peer.getPc();
            if (peerConnection != null) {
                // Dispose peer connection
                peerConnection.removeStream(getSkylinkMediaService().getLocalMediaStream());
                peerConnection.dispose();
            }
            return true;
        }
        return false;
    }

    /**
     * When received welcome, check if we should proceed or not. If we had both sent "enter" to each
     * other (due to entering the room together), the one whose weight is smaller should continue
     * the handshake, while the other should abandon handshake.
     *
     * @param peerId
     * @param weight
     * @return True if welcome should be accepted, false if should be dropped.
     */
    private boolean shouldAcceptWelcome(String peerId, double weight) {
        Peer peer = getPeer(peerId);
        if (weight > 0) {
            if (peer != null) {
                if (peer.getWeight() > weight) {
                    // Use this welcome (ours will be discarded on peer's side).
                    return true;
                } else {
                    // Discard this welcome (ours will be used on peer's side).
                    return false;
                }
            } else {
                // Use this welcome (we did not send one to the peer).
                return true;
            }
        } else {
            // Peer did not send a weight, use Peer's welcome.
            return true;
        }
    }

    // Getters and Setters
    private SkylinkMediaService getSkylinkMediaService() {
        return skylinkConnection.getSkylinkMediaService();
    }

    public void setSkylinkConnectionService(SkylinkConnectionService skylinkConnectionService) {
        this.skylinkConnectionService = skylinkConnectionService;
    }

    @Override
    public int getMaxPeer() {
        return skylinkConnection.getSkylinkConfig().getMaxPeers();
    }

    /**
     * Retrieves the user defined data object of a peer.
     *
     * @param remotePeerId The id of the remote peer whose UserData is to be retrieved, or NULL for
     *                     self.
     * @return May be a 'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     */
    Object getUserData(String remotePeerId) {
        Object userData = null;
        if (remotePeerId == null) {
            userData = myUserInfo.getUserData();
        } else {
            Peer peer = getPeer(remotePeerId);
            if (peer != null) {
                UserInfo userInfo = peer.getUserInfo();
                userData = userInfo.getUserData();
            }
        }
        return userData;
    }

    /**
     * Sets the userData for a peer
     *
     * @param remotePeerId The id of the remote peer whose UserData is to be set, or NULL for self.
     * @param userData
     */
    void setUserData(String remotePeerId, Object userData) {
        // Set self UserData
        if (remotePeerId == null) {
            if (myUserInfo == null) {
                myUserInfo = new UserInfo(skylinkConnection.getSkylinkConfig(), userData);
            } else {
                myUserInfo.setUserData(userData);
            }
        } else {
            // Set Peer UserData
            Peer peer = getPeer(remotePeerId);
            if (peer != null) {
                UserInfo userInfo = peer.getUserInfo();
                if (userInfo != null) {
                    userInfo.setUserData(userData);
                }
            }
        }
    }

    /**
     * Retrieves the UserInfo object of a peer.
     *
     * @param remotePeerId The id of the remote peer whose UserInfo is to be retrieved, or NULL for
     *                     self.
     * @return UserInfo
     */
    UserInfo getUserInfo(String remotePeerId) {
        if (remotePeerId == null) {
            return myUserInfo;
        } else {
            Peer peer = getPeer(remotePeerId);
            if (peer != null) {
                return peer.getUserInfo();
            }
        }
        return null;
    }

    /**
     * Sets the userInfo to the relevant peer
     *
     * @param remotePeerId The id of the remote peer whose userInfo is to be set, or NULL for self.
     * @param userInfo
     */

    void setUserInfo(String remotePeerId, UserInfo userInfo) {
        // Set self UserData
        if (remotePeerId == null) {
            myUserInfo = userInfo;
        } else {
            // Set Peer UserData
            Peer peer = getPeer(remotePeerId);
            if (peer != null) {
                peer.setUserInfo(userInfo);
            }
        }
    }

}
