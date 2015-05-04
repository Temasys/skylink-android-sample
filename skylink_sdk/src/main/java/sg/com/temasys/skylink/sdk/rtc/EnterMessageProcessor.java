package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose of this message processor is to handle enter message types
 */
public class EnterMessageProcessor implements MessageProcessor {

    private static final String TAG = EnterMessageProcessor.class.getSimpleName();

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {

        String peerId = jsonObject.getString("mid");
        Object userInfo = "";
        try {
            userInfo = jsonObject.get("userInfo");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        PeerInfo peerInfo = new PeerInfo();
        try {
            peerInfo.setReceiveOnly(jsonObject.getBoolean("receiveOnly"));
            peerInfo.setAgent(jsonObject.getString("agent"));
            // SM0.1.0 - Browser version for web, SDK version for others.
            peerInfo.setVersion(jsonObject.getString("version"));
        } catch (JSONException e) {
        }

        skylinkConnection.getSkylinkPeerService().receivedEnter(peerId, peerInfo, userInfo);
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
