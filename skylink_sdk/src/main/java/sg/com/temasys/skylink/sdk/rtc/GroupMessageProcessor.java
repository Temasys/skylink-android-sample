package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose of this message processor is to process group message types
 * Created by janidu on 11/5/15.
 */
public class GroupMessageProcessor implements MessageProcessor {

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {
        // Split up group message
        // Format:
        // { type: "group", lists: [<group msg>...], mid: "xxx", rid: "xxx" }
        JSONArray msgArr = jsonObject.getJSONArray("lists");
        for (int i = 0; i < msgArr.length(); ++i) {
            String message = (String) msgArr.get(i);
            // Send each message to be processed like a non-group message.
            if (message != null) {
                skylinkConnection.
                        getSignalingMessageProcessingService().onMessage(message);
            }
        }
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
