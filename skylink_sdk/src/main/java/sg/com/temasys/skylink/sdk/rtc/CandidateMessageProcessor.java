package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;

/**
 * Created by janidu on 5/5/15.
 */
class CandidateMessageProcessor implements MessageProcessor {

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {
        this.skylinkConnection.getSkylinkPeerService().addIceCandidate(jsonObject.getString("mid"),
                new IceCandidate(jsonObject.getString("id"), jsonObject.getInt("label"),
                        jsonObject.getString("candidate")));
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }

}
