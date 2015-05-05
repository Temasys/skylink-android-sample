package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;

/**
 * Created by janidu on 5/5/15.
 */
public class CandidateMessageProcessor implements MessageProcessor {

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {

        if (!skylinkConnection.getWebServerClient()
                .getSid().equals(jsonObject.getString("target"))) {
            return;
        }

        PeerConnection peerConnection = skylinkConnection.
                getPeerConnection(jsonObject.getString("mid"));

        if (peerConnection != null) {
            peerConnection.addIceCandidate(new IceCandidate(jsonObject.getString("id"),
                    jsonObject.getInt("label"), jsonObject.getString("candidate")));
        }
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }

}
