package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONObject;

/**
 * Created by janidu on 27/4/15.
 */
interface MessageProcessor {

    void process(JSONObject jsonObject);
}
