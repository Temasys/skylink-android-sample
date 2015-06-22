package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Tests related to SignalingServerClient
 */

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class SignalingServerClientTest {


    private static final String TAG = SignalingServerClient.class.getName();
    private AppServerClient appServerClient;
    private AppServerClientListener appServerClientListener;
    private SkylinkRoomParameterProcessor skylinkRoomParameterProcessor;

    private String mSignalingServer;
    private int mSignalingPort;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
        appServerClientListener = mock(AppServerClientListener.class);
        skylinkRoomParameterProcessor = new SkylinkRoomParameterProcessor();
        appServerClient = new AppServerClient(appServerClientListener,
                skylinkRoomParameterProcessor);

    }

    /**
     * Test that connectSigServer can connect to to Signaling server and trigger onConnect().
     *
     * @throws InterruptedException
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testOnConnect() throws InterruptedException, IOException, JSONException {

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        // Get signaling parameters from App Server.
        skylinkRoomParameterProcessor.setRoomParameterListener(
                new RoomParameterListener() {
                    @Override
                    public void onRoomParameterSuccessful(RoomParameters params) {
                        Log.d(TAG, "onRoomParameterSuccessful called.");
                        assertNotNull("Parameters should not be null", params);
                        mSignalingServer = params.getIpSigserver();
                        mSignalingPort = params.getPortSigserver();
                        String log = "Signaling Server IP: " + mSignalingServer +
                                "\nSignaling Server Port: " + mSignalingPort;
                        Log.d(TAG, log);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onRoomParameterError(int message) {
                        Log.d(TAG, "onSignalingParametersReceivedError");
                        fail("Should receive signaling parameters successfully");
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onRoomParameterError(String message) {
                        Log.d(TAG, "onSignalingParametersReceivedError");
                        fail("Should receive signaling parameters successfully");
                        countDownLatch.countDown();
                    }
                });

        appServerClient.connectToRoom(TestConstants.SKYLINK_CONNECTION_STRING);
        countDownLatch.await();

        // Test if able to connect Signaling Server and start onOpen call.
        Log.d(TAG, "testOnConnect");
        final CountDownLatch countDownLatch2 = new CountDownLatch(1);

        final SignalingServerClient signalingServerClient =
                new SignalingServerClient(new SignalingServerClientListener() {
                    @Override
                    public void onOpen() {
                        Log.d(TAG, "onOpen");
                        countDownLatch2.countDown();
                    }

                    @Override
                    public void onMessage(String data) {
                        String strErr = "onOpen not called but onMessage called with:\n" + data;
                        Log.e(TAG, strErr);
                        fail(strErr);
                        countDownLatch2.countDown();
                    }

                    @Override
                    public void onDisconnect() {
                        String strErr = "onOpen not called but onDisconnect called.";
                        Log.e(TAG, strErr);
                        fail(strErr);
                        countDownLatch2.countDown();
                    }

                    @Override
                    public void onClose() {
                        String strErr = "onOpen not called but onClose called.";
                        Log.e(TAG, strErr);
                        fail(strErr);
                        countDownLatch2.countDown();
                    }

                    @Override
                    public void onError(int code, String description) {
                        String strErr = "onOpen not called but onErrorAppServer called with:\n" +
                                "Code " + code + ", " + description + ".";
                        Log.e(TAG, strErr);
                        fail(strErr);
                        countDownLatch2.countDown();
                    }
                }, "https://" + mSignalingServer, mSignalingPort);

        countDownLatch2.await();
        assertNotNull(signalingServerClient.getSocketIO());
        assertTrue(signalingServerClient.getSocketIO().connected());
        signalingServerClient.getSocketIO().disconnect();
    }

    /* TODO */
    @Test
    public void testOnMessageJson() {

    }

    /* TODO */
    @Test
    public void testOnMessageString() {

    }

    /* TODO */
    @Test
    public void testOnDisconnect() {

    }

    /* TODO */
    @Test
    public void testOnTimeOut() {

    }

    /* TODO */
    @Test
    public void testOnError() {

    }

}
