package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose of this class is to handle bye message types
 * <p/>
 * Created by janidu on 5/5/15.
 */
public class ByeMessageProcessor implements MessageProcessor {

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {
        skylinkConnection.getSkylinkPeerService().receivedBye(jsonObject.getString("mid"));
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
