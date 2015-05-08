package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Purpose of this message processor is to handle inRoom message types
 */
public class InRoomMessageProcessor implements MessageProcessor {

    private static final String TAG = InRoomMessageProcessor.class.getSimpleName();

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {

        JSONObject pcConfigJSON = jsonObject.getJSONObject("pc_config");

        List<PeerConnection.IceServer> result = new ArrayList<>();
        JSONArray iceServers = pcConfigJSON.getJSONArray("iceServers");

        for (int i = 0; i < iceServers.length(); i++) {
            JSONObject iceServer = iceServers.getJSONObject(i);
            String url = iceServer.getString("url");

            if (skylinkConnection.getMyConfig().isStunDisabled() && url.startsWith("stun:")) {
                Log.d(TAG, "[SDK] Not adding stun server as stun disabled in config.");
                continue;
            }
            if (skylinkConnection.getMyConfig().isTurnDisabled() && url.startsWith("turn:")) {
                Log.d(TAG, "[SDK] Not adding turn server as turn disabled in config.");
                continue;
            }
            if (skylinkConnection.getMyConfig().getTransport() != null) {
                url = url + "?transport=" + skylinkConnection.getMyConfig().getTransport();
            }

            String credential = iceServer.has("credential") ? iceServer.getString("credential") : "";

            Log.d(TAG, "[SDK] url [" + url
                    + "] - credential [" + credential + "]");

            PeerConnection.IceServer server = new PeerConnection.IceServer(url, "", credential);
            result.add(server);
        }

        skylinkConnection.getSkylinkPeerService().receivedInRoom(jsonObject.getString("sid"), result);
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
