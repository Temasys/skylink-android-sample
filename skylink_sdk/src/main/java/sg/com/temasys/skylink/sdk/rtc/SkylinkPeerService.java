package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;

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

    void receivedBye(final String peerId) {

        if (!skylinkConnection.isPeerIdMCU(peerId)) {
            skylinkConnection.runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (skylinkConnection.getLockDisconnect()) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (skylinkConnection.getConnectionState() ==
                                SkylinkConnection.ConnectionState.DISCONNECT) {
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

        skylinkConnection.getWebServerClient().setSid(peerId);

        skylinkConnection.getIceServersObserver().onIceServers(iceServers);

        // Set mid and displayName in DataChannelManager
        if (skylinkConnection.getDataChannelManager() != null) {
            skylinkConnection.getDataChannelManager().setMid(peerId);
            skylinkConnection.getDataChannelManager().setDisplayName(
                    skylinkConnection.getMyUserData().toString());
        }

        // Check if pcObserverPool has been populated.
        if (skylinkConnection.getPcObserverPool() != null) {
            // If so, chances are this is a rejoin of room.
            // Send restart to all.
            skylinkConnection.rejoinRestart();
        } else {
            // If not, chances are this is a first join room, or there were no peers from before.
            // Create afresh all PC related maps.
            skylinkConnection.initializePcRelatedMaps();
            // Send enter.
            try {
                ProtocolHelper.sendEnter(null, skylinkConnection, skylinkConnection.getWebServerClient());
            } catch (JSONException e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    }
}
