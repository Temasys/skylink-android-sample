package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONObject;

/**
 * Created by janidu on 13/1/15.
 */
public interface SignalingParameterHelperListener {

    public void onSignalingParametersReceived(JSONObject jsonObject);

    public void onSignalingParametersReceivedError(String error);
}
