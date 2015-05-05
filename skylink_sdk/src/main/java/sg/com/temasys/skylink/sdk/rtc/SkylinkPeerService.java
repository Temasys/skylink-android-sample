package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

/**
 * Created by xiangrong on 4/5/15.
 */
public class SkylinkPeerService {

    private static final String TAG = SkylinkPeerService.class.getSimpleName();

    private final SkylinkConnection skylinkConnection;

    public SkylinkPeerService(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }

    void receivedEnter(String peerId, PeerInfo peerInfo, JSONObject userInfo) {
        PeerConnection peerConnection = skylinkConnection
                .getPeerConnection(peerId, HealthChecker.ICE_ROLE_ANSWERER);


        skylinkConnection.getPeerInfoMap().put(peerId, peerInfo);

        // Add our local media stream to this PC, or not.
        if ((skylinkConnection.getMyConfig().hasAudioSend() || skylinkConnection.getMyConfig().hasVideoSend())) {
            peerConnection.addStream(skylinkConnection.getLocalMediaStream());
            Log.d(TAG, "Added localMedia Stream");
        }

        if (peerConnection != null) {
            skylinkConnection.setUserInfoMap(userInfo, peerId);

            try {
                ProtocolHelper.sendWelcome(peerId, skylinkConnection, false);
            } catch (JSONException e) {
                Log.d(TAG, e.getMessage(), e);
            }

        } else {
            skylinkConnection
                    .logMessage("I only support "
                            + skylinkConnection.getMaxPeerConnections()
                            + " connections are in this app. I am discarding this 'welcome'.");
        }
    }
}
