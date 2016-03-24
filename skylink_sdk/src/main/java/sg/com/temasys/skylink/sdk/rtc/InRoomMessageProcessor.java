package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logI;


/**
 * Purpose of this message processor is to handle inRoom message types
 */
class InRoomMessageProcessor implements MessageProcessor {

    private static final String TAG = InRoomMessageProcessor.class.getName();

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {

        JSONObject pcConfigJSON = jsonObject.getJSONObject("pc_config");

        List<PeerConnection.IceServer> result = new ArrayList<>();
        JSONArray iceServers = pcConfigJSON.getJSONArray("iceServers");

        for (int i = 0; i < iceServers.length(); i++) {
            JSONObject iceServer = iceServers.getJSONObject(i);
            String url = iceServer.getString("url");

            if (skylinkConnection.getSkylinkConfig().isStunDisabled() && url.startsWith("stun:")) {
                String info = "[INFO] Not adding STUN server as STUN is disabled in SkylinkConfig.";
                String debug = info + "\nDetails: Url: " + url;
                logI(TAG, info);
                logD(TAG, debug);
                continue;
            }
            if (skylinkConnection.getSkylinkConfig().isTurnDisabled() && url.startsWith("turn:")) {
                String info = "[INFO] Not adding TURN server as TURN is disabled in SkylinkConfig.";
                String debug = info + "\nDetails: Url: " + url;
                logI(TAG, info);
                logD(TAG, debug);
                continue;
            }
            if (skylinkConnection.getSkylinkConfig().getTransport() != null) {
                url = url + "?transport=" + skylinkConnection.getSkylinkConfig().getTransport();
            }

            String credential = iceServer.has("credential") ? iceServer.getString("credential") : "";

            logD(TAG, "ICE server adding...\nurl: " + url + ";\ncredential: " + credential + ".");

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
