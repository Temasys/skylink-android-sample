package sg.com.temasys.skylink.sdk.rtc;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by janidu on 12/1/15.
 */

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class SignalingServerClientTest {

    private static final String TAG = SignalingServerClient.class.getName();
    private String mSignalingServer;
    private int mSignalingPort;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        SignalingParameterHelper.retrieveSignalingParameters(Robolectric.application,
                new SignalingParameterHelperListener() {
                    @Override
                    public void onSignalingParametersReceived(JSONObject jsonObject) {
                        assertNotNull(jsonObject);
                        Log.d(TAG, "onSignalingParametersReceived");
                        try {
                            mSignalingServer = jsonObject.getString(SignalingParameterHelper.
                                    JSON_KEY_SERVER);
                            mSignalingPort = jsonObject.getInt(SignalingParameterHelper.
                                    JSON_KEY_PORT);
                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                        } finally {
                            countDownLatch.countDown();
                        }
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

    @Test
    public void testOnOpen() throws InterruptedException, UnsupportedEncodingException {

        Log.d(TAG, "testOnOpen");
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        final SignalingServerClient signalingServerClient =
                new SignalingServerClient(new WebServerClient.MessageHandler() {
                    @Override
                    public void onOpen() {
                        Log.d(TAG, "onOpen");
                        assertEquals(true, true);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onMessage(String data) {
                        Log.d(TAG, "onMessage");
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onClose() {
                        Log.d(TAG, "onClose");
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onError(int code, String description) {
                        Log.d(TAG, "onError" + code + description);
                        assertEquals(true, false);
                        countDownLatch.countDown();
                    }
                }, "https://" + mSignalingServer, mSignalingPort);

        countDownLatch.await();
        assertNotNull(signalingServerClient.getSocketIO());
        assertTrue(signalingServerClient.getSocketIO().isConnected());
        signalingServerClient.getSocketIO().disconnect();
    }
}
