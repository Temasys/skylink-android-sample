package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.Hashtable;
import java.util.List;

/**
 * Created by xiangrong on 4/5/15.
 */
class SkylinkPeerService {

    private static final String TAG = SkylinkPeerService.class.getSimpleName();

    private final SkylinkConnection skylinkConnection;
    private SkylinkMediaService skylinkMediaService;
    private SkylinkConnectionService skylinkConnectionService;

    public SkylinkPeerService(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }

    static boolean isPeerIdMCU(String peerId) {
        return peerId.startsWith("MCU");
    }

    void receivedEnter(String peerId, PeerInfo peerInfo, UserInfo userInfo) {
        // Create a new PeerConnection if we can
        PeerConnection peerConnection = skylinkConnection
                .getPeerConnection(peerId, HealthChecker.ICE_ROLE_ANSWERER);

        // If we are over the max no. of peers, peerConnection here will be null.
        if (peerConnection != null) {
            skylinkConnection.setUserInfo(peerId, userInfo);
            skylinkConnection.getPeerInfoMap().put(peerId, peerInfo);

            // Add our local media stream to this PC, or not.
            if ((skylinkConnection.getMyConfig().hasAudioSend() || skylinkConnection.getMyConfig().hasVideoSend())) {
                peerConnection.addStream(skylinkConnection.getLocalMediaStream());
                Log.d(TAG, "Added localMedia Stream");
            }

            try {
                /*ProtocolHelper.sendEnter(null, skylinkConnection,
                        skylinkConnection.getSkylinkConnectionService());*/
                ProtocolHelper.sendWelcome(peerId, skylinkConnection, false);
            } catch (JSONException e) {
                Log.d(TAG, e.getMessage(), e);
            }

        } else {
            Log.d(TAG, "I only support "
                    + skylinkConnection.getMaxPeerConnections()
                    + " connections are in this app. I am discarding this 'welcome'.");
        }
    }

    void receivedBye(final String peerId) {

        if (!isPeerIdMCU(peerId)) {
            skylinkConnection.runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (skylinkConnection.getLockDisconnect()) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (skylinkConnection.getSkylinkConnectionService().getConnectionState() ==
                                SkylinkConnectionService.ConnectionState.DISCONNECTING) {
                            return;
                        }
                        skylinkConnection.getRemotePeerListener()
                                .onRemotePeerLeave(peerId, "The peer has left the room");
                    }
                }
            });
        }

        DataChannelManager dataChannelManager = skylinkConnection.getDataChannelManager();

        // Dispose DataChannel.
        if (dataChannelManager != null) {
            dataChannelManager.disposeDC(peerId);
        }

        ProtocolHelper.disposePeerConnection(peerId, skylinkConnection);
    }


    void addIceCandidate(String peerId, IceCandidate iceCandidate) {
        PeerConnection peerConnection = skylinkConnection.getPeerConnection(peerId);
        if (peerConnection != null) {
            peerConnection.addIceCandidate(iceCandidate);
        }
    }

    void receivedInRoom(String peerId, List<PeerConnection.IceServer> iceServers) {

        skylinkConnection.getSkylinkConnectionService().setSid(peerId);

        skylinkConnectionService.setIceServers(iceServers);

        // Set mid and displayName in DataChannelManager
        if (skylinkConnection.getDataChannelManager() != null) {
            skylinkConnection.getDataChannelManager().setMid(peerId);
            skylinkConnection.getDataChannelManager().setDisplayName(
                    skylinkConnection.getMyUserData().toString());
        }

        // Check if pcObserverPool has been populated.
        if (skylinkConnection.getPcObserverPool() != null) {
            // If so, chances are this is a rejoin of room.
            // Log it
            Log.d(TAG, "[receivedInRoom] This is a rejoin of room.");
        }

        // Create afresh all PC related maps.
        skylinkConnection.initializePcRelatedMaps();
        // Send enter.
        try {
            ProtocolHelper.sendEnter(null, skylinkConnection,
                    skylinkConnection.getSkylinkConnectionService());
        } catch (JSONException e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    void receivedOfferAnswer(String peerId, String sdp, String type) {
        PeerConnection peerConnection = skylinkConnection.getPeerConnection(peerId);

        // Set the preferred audio codec
        String sdpString = Utils.preferCodec(sdp,
                skylinkConnection.getMyConfig().getPreferredAudioCodec().toString(), true);

        // Set the SDP
        SessionDescription sessionDescription = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(type),
                sdpString);

        peerConnection.setRemoteDescription(skylinkConnection.getSdpObserver(peerId), sessionDescription);
        Log.d(TAG, "PC - setRemoteDescription. Setting " + sessionDescription.type + " from " + peerId);
    }

    void receivedWelcomeRestart(String peerId, PeerInfo peerInfo,
                                UserInfo userInfo, double weight, boolean isRestart) {

        if (isRestart) {
            if (!ProtocolHelper.processRestart(peerId, skylinkConnection)) {
                return;
            }
        }

        skylinkConnection.getPeerInfoMap().put(peerId, peerInfo);
        PeerConnection peerConnection = null;
        List<Object> weightedConnection = skylinkConnection.getWeightedPeerConnection(peerId, weight);
        if (!(Boolean) weightedConnection.get(0)) {
            Log.d(TAG, "Ignoring this welcome");
            return;
        }

        Object secondObject = weightedConnection.get(1);
        if (secondObject instanceof PeerConnection)
            peerConnection = (PeerConnection) secondObject;

        if (peerConnection == null) {
            Log.d(TAG, "I only support "
                    + skylinkConnection.getMaxPeerConnections()
                    + " connections are in this app. I am discarding this 'welcome'.");
            return;
        }

        skylinkConnection.setUserInfo(peerId, userInfo);

        boolean receiveOnly = peerInfo.isReceiveOnly();

        // Add our local media stream to this PC, or not.
        if ((skylinkConnection.getMyConfig().hasAudioSend() ||
                skylinkConnection.getMyConfig().hasVideoSend()) && !receiveOnly) {
            peerConnection.addStream(skylinkConnection.getLocalMediaStream());
            Log.d(TAG, "Added localMedia Stream");
        }

        Log.d(TAG, "[receivedWelcomeRestart] - create offer.");
        // Create DataChannel if both Peer and ourself desires it.
        if (peerInfo.isEnableDataChannel() &&
                (skylinkConnection.getMyConfig().hasPeerMessaging()
                        || skylinkConnection.getMyConfig().hasFileTransfer()
                        || skylinkConnection.getMyConfig().hasDataTransfer())) {
            // It is stored by dataChannelManager.
            skylinkConnection.getDataChannelManager().createDataChannel(
                    peerConnection, skylinkConnection.getSkylinkConnectionService().getSid(), peerId, "", null, peerId);
        }

        if (skylinkConnection.getSdpObserverPool() == null) {
            skylinkConnection.setSdpObserverPool(new Hashtable<String, SkylinkConnection.SDPObserver>());
        }
        SkylinkConnection.SDPObserver sdpObserver = skylinkConnection.getSdpObserverPool()
                .get(peerId);
        if (sdpObserver == null) {
            sdpObserver = skylinkConnection.new SDPObserver();
            sdpObserver.setMyId(peerId);
            skylinkConnection.getSdpObserverPool().put(peerId, sdpObserver);
        }

        peerConnection.createOffer(sdpObserver,
                skylinkMediaService.getSdpMediaConstraints());

        Log.d(TAG, "[receivedWelcomeRestart] - createOffer for " + peerId);
    }

    // Getters and Setters
    public void setSkylinkMediaService(SkylinkMediaService skylinkMediaService) {
        this.skylinkMediaService = skylinkMediaService;
    }

    public void setSkylinkConnectionService(SkylinkConnectionService skylinkConnectionService) {
        this.skylinkConnectionService = skylinkConnectionService;
    }

}
