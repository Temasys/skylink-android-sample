package com.sg.temasys.skylink.sdk.test.helper;

import org.json.JSONObject;

/**
 * Created by janidu on 13/1/15.
 */
public interface SignalingParameterHelperListener {

    public void onSignalingParametersReceived(JSONObject jsonObject);

    public void onSignalingParametersReceivedError(String error);
}
