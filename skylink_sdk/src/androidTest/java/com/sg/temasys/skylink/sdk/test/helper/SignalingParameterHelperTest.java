package com.sg.temasys.skylink.sdk.test.helper;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * Created by janidu on 13/1/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class SignalingParameterHelperTest {

    private static final String TAG = SignalingParameterHelperTest.class.getName();

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
    }

    @Test
    public void testRetrieveSignalingParameters()
            throws UnsupportedEncodingException, InterruptedException {

        Log.d(TAG, "testRetrieveSignalingParameters");

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        SignalingParameterHelper.retrieveSignalingParameters(Robolectric.application,
                new SignalingParameterHelperListener() {
                    @Override
                    public void onSignalingParametersReceived(JSONObject jsonObject) {
                        Log.d(TAG, "onSignalingParametersReceived");
                        assertNotNull("Should not be null", jsonObject);
                        try {
                            assertNotNull("Should contain signaling server ip",
                                    jsonObject.getString(SignalingParameterHelper.JSON_KEY_SERVER));
                            assertNotNull("Should contain signaling server port",
                                    jsonObject.getString(SignalingParameterHelper.JSON_KEY_PORT));
                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                        }
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onSignalingParametersReceivedError(String error) {
                        Log.d(TAG, "onSignalingParametersReceivedError");
                        fail("Should receive signaling parameters successfully");
                        countDownLatch.countDown();
                    }
                });

        countDownLatch.await();
    }
}
