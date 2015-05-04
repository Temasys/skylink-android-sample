package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose of this method is to define the interface of a MessageProcessor
 * Created by janidu on 27/4/15.
 */
interface MessageProcessor {

    void process(JSONObject jsonObject) throws JSONException;

    void setSkylinkConnection(SkylinkConnection skylinkConnection);
}