package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose of this message processor is to handle enter message types
 */
class EnterMessageProcessor implements MessageProcessor {

    private static final String TAG = EnterMessageProcessor.class.getSimpleName();

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {

        String peerId = jsonObject.getString("mid");
        JSONObject userInfoJson = jsonObject.getJSONObject("userInfo");
        UserInfo userInfo = new UserInfo(userInfoJson);

        PeerInfo peerInfo = new PeerInfo();
        peerInfo.setAgent(jsonObject.getString("agent"));

        // Hack to accomodate the non-Android clients until the update to SM 0.1.1
        if (peerInfo.getAgent().equals("Android")) {
            // If it is Android, get receiveOnly value.
            peerInfo.setReceiveOnly(jsonObject.getBoolean("receiveOnly"));
        } else {
            // If web or others, let receiveOnly be false
            // TODO XR: Remove after JS client update to compatible restart protocol.
            Log.d(TAG, "[EnterMessageProcessor] Peer " + peerId + " is non-Android or has no receiveOnly.");
        }
        // SM0.1.0 - Browser version for web, SDK version for others.
        peerInfo.setVersion(jsonObject.getString("version"));

        skylinkConnection.getSkylinkPeerService().receivedEnter(peerId, peerInfo, userInfo);
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
