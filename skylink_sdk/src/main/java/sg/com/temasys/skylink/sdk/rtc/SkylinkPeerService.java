package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

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
}
