package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
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


    void addIceCandidate(String peerId, IceCandidate iceCandidate) {
        PeerConnection peerConnection = skylinkConnection.getPeerConnection(peerId);
        if (peerConnection != null) {
            peerConnection.addIceCandidate(iceCandidate);
        }
    }
}
