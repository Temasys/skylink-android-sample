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
        boolean mcu = peerId.startsWith("MCU");
        if (mcu) {
            Log.d(TAG, "MCU Detected");
        }
        return mcu;
    }

    void receivedEnter(String peerId, PeerInfo peerInfo, UserInfo userInfo) {
        // Create a new PeerConnection if we can
        PeerConnection peerConnection = skylinkConnection
                .createPC(peerId, HealthChecker.ICE_ROLE_ANSWERER, userInfo);

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
            // We have reached the limit of max no. of Peers.
            Log.d(TAG, "Discarding this \"enter\" due to Peer number limit.");
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
            dataChannelManager.disposeDC(peerId, false);
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

        // Check if a PC was already created at "enter"
        if (skylinkConnection.getPeerConnection(peerId) != null) {
            // Check if should continue with "welcome" or use PC from "enter"
            if (skylinkConnection.shouldAcceptWelcome(peerId, weight)) {
                // Remove PC from "enter" to create new PC from "welcome"
                ProtocolHelper.disposePeerConnection(peerId, skylinkConnection);
            } else {
                // Do not continue "welcome" but use PC from "enter" instead.
                Log.d(TAG, "Ignoring this welcome as Peer " + userInfo.getUserData() +
                        " (" + peerId + ") is processing our welcome.");
                return;
            }
        }

        // Create a PC from "welcome"
        PeerConnection peerConnection =
                skylinkConnection.createPC(peerId, HealthChecker.ICE_ROLE_OFFERER, userInfo);

        // We have reached the limit of max no. of Peers.
        if (peerConnection == null) {
            Log.d(TAG, "Discarding this \"welcome\" due to Peer number limit.");
            return;
        }

        skylinkConnection.getPeerInfoMap().put(peerId, peerInfo);
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
                    peerConnection, skylinkConnection.getSkylinkConnectionService().getSid(),
                    peerId, "", null, peerId);
        }

        if (skylinkConnection.getSdpObserverPool() == null) {
            skylinkConnection.setSdpObserverPool(new Hashtable<String, SkylinkSdpObserver>());
        }
        SkylinkSdpObserver sdpObserver = skylinkConnection.getSdpObserverPool()
                .get(peerId);
        if (sdpObserver == null) {
            sdpObserver = new SkylinkSdpObserver(skylinkConnection);
            sdpObserver.setPeerId(peerId);
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
