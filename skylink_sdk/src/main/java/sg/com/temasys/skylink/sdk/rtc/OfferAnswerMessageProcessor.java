package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

/**
 * Purpose of this message processor is to handle offer/answer message types
 * Created by janidu on 4/5/15.
 */
public class OfferAnswerMessageProcessor implements MessageProcessor {

    private static final String TAG = OfferAnswerMessageProcessor.class.getSimpleName();

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {

        String mid = jsonObject.getString("mid");
        PeerConnection peerConnection = skylinkConnection.getPeerConnection(mid);

        // Set the preferred audio codec
        String sdpString = Utils.preferCodec(jsonObject.getString("sdp"),
                skylinkConnection.getMyConfig().getPreferredAudioCodec().toString(), true);

        // Set the SDP
        SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(jsonObject.getString("type")),
                sdpString);

        peerConnection.setRemoteDescription(skylinkConnection.getSdpObserver(mid), sdp);
        Log.d(TAG, "PC - setRemoteDescription. Sending " + sdp.type + " to " + mid);
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
