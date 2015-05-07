package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

import java.util.Hashtable;
import java.util.List;

/**
 * Created by xiangrong on 4/5/15.
 */
class SkylinkPeerService {

    private static final String TAG = SkylinkPeerService.class.getSimpleName();

    private final SkylinkConnection skylinkConnection;

    public SkylinkPeerService(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }

    void receivedEnter(String peerId, PeerInfo peerInfo, JSONObject userInfo) {
        // Create a new PeerConnection if we can
        PeerConnection peerConnection = skylinkConnection
                .getPeerConnection(peerId, HealthChecker.ICE_ROLE_ANSWERER);

        // If we are over the max no. of peers, peerConnection here will be null.
        if (peerConnection != null) {
            skylinkConnection.setUserInfoMap(userInfo, peerId);
            skylinkConnection.getPeerInfoMap().put(peerId, peerInfo);

            // Add our local media stream to this PC, or not.
            if ((skylinkConnection.getMyConfig().hasAudioSend() || skylinkConnection.getMyConfig().hasVideoSend())) {
                peerConnection.addStream(skylinkConnection.getLocalMediaStream());
                Log.d(TAG, "Added localMedia Stream");
            }

            try {
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

    void receivedWelcomeRestart(String peerId, PeerInfo peerInfo, JSONObject userInfo, double weight, boolean isRestart) {

        if(isRestart){
            if (!ProtocolHelper.processRestart(peerId, skylinkConnection.getLocalMediaStream(), skylinkConnection)) {
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

        skylinkConnection.setUserInfoMap(userInfo, peerId);

        boolean receiveOnly = peerInfo.isReceiveOnly();

        // Add our local media stream to this PC, or not.
        if ((skylinkConnection.getMyConfig().hasAudioSend() || skylinkConnection.getMyConfig().hasVideoSend()) && !receiveOnly) {
            peerConnection.addStream(skylinkConnection.getLocalMediaStream());
            Log.d(TAG, "Added localMedia Stream");
        }

        Log.d(TAG, "[receivedWelcomeRestart] - create offer.");
        // Create DataChannel if both Peer and ourself desires it.
        if (peerInfo.isEnableDataChannel() &&
                (skylinkConnection.getMyConfig().hasPeerMessaging() || skylinkConnection.getMyConfig().hasFileTransfer()
                        || skylinkConnection.getMyConfig().hasDataTransfer()))

        {
            // It is stored by dataChannelManager.
            skylinkConnection.getDataChannelManager().createDataChannel(
                    peerConnection, skylinkConnection.getWebServerClient().getSid(), peerId, "", null, peerId);
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
                skylinkConnection.getSdpMediaConstraints());

        Log.d(TAG, "[receivedWelcomeRestart] - createOffer for " + peerId);
    }
}
