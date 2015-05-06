package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose of this message processor is to handle ping message types
 * Created by janidu on 6/5/15.
 */
public class PingMessageProcessor implements MessageProcessor {

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {
        if (!skylinkConnection.getWebServerClient()
                .getSid().equals(jsonObject.getString("target"))) {
            return;
        }

        ProtocolHelper.sendPingMessage(skylinkConnection, jsonObject.getString("mid"));
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
