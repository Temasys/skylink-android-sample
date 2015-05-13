package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose of this message processor is to handle enter message types
 * Created by xiangrong on 7/5/15.
 */
public class WelcomeRestartMessageProcessor implements MessageProcessor {

    private static final String TAG = WelcomeRestartMessageProcessor.class.getSimpleName();

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {

        boolean isRestart = false;
        if ("restart".equals(jsonObject.getString("type"))) {
            isRestart = true;
        }

        String peerId = jsonObject.getString("mid");
        JSONObject userInfo = jsonObject.getJSONObject("userInfo");

        PeerInfo peerInfo = new PeerInfo();
        peerInfo.setAgent(jsonObject.getString("agent"));

        // Hack to accomodate the non-Android clients until the update to SM 0.1.1
        if (jsonObject.has("receiveOnly")) {
            // If it has, get receiveOnly value.
            peerInfo.setReceiveOnly(jsonObject.getBoolean("receiveOnly"));
        } else {
            // If not, set it to a default value.
            // TODO XR: Remove after JS client update to compatible restart protocol.
            Log.d(TAG, "[WelcomeRestartMessageProcessor] Peer " + peerId +
                    " is non-Android or has no receiveOnly." +
                    " Setting to false by default.");
        }

        // SM0.1.0 - Browser version for web, SDK version for others.
        peerInfo.setVersion(jsonObject.getString("version"));

        if (jsonObject.has("enableIceTrickle")) {
            peerInfo.setEnableIceTrickle(jsonObject.getBoolean("enableIceTrickle"));
        } else {
            // Work around for JS and/or other clients that do not yet implement this flag.
            peerInfo.setEnableIceTrickle(true);
            Log.d(TAG, "[WelcomeRestartMessageProcessor] Peer " + peerId +
                    " is non-Android or has no enableIceTrickle." +
                    " Setting to true by default.");
        }

        if (jsonObject.has("enableDataChannel")) {
            peerInfo.setEnableDataChannel(jsonObject.getBoolean("enableDataChannel"));
        } else {
            // Work around for JS and/or other clients that do not yet implement this flag.
            peerInfo.setEnableDataChannel(true);
            Log.d(TAG, "[WelcomeRestartMessageProcessor] Peer " + peerId +
                    " is non-Android or has no enableIceTrickle." +
                    " Setting to true by default.");
        }

        double weight = 0.0;
        if (jsonObject.has("enableIceTrickle")) {
            weight = jsonObject.getDouble("weight");
        }

        skylinkConnection.getSkylinkPeerService()
                .receivedWelcomeRestart(peerId, peerInfo, userInfo, weight, isRestart);
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}

