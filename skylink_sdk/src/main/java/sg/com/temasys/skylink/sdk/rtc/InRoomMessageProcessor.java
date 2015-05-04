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

        String mid = jsonObject.getString("sid");
        skylinkConnection.getWebServerClient().setSid(mid);

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

        skylinkConnection.getIceServersObserver().onIceServers(result);

        // Set mid and displayName in DataChannelManager
        if (skylinkConnection.getDataChannelManager() != null) {
            skylinkConnection.getDataChannelManager().setMid(mid);
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

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
