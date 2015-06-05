package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

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
 * Tests related to SignalingServerClient
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

        RoomParameterService roomParameterService = new RoomParameterService(new RoomParameterServiceListener() {
            @Override
            public void onRoomParameterSuccessful(AppRTCSignalingParameters params) {
                assertNotNull(params);
                Log.d(TAG, "onSignalingParametersReceived");
                mSignalingServer = params.getIpSigserver();
                mSignalingPort = params.getPortSigserver();
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

            @Override
            public void onShouldConnectToRoom() {
                assertTrue("Should notify to connect to room", true);
            }
        });

        roomParameterService.execute(TestConstants.SKYLINK_CONNECTION_STRING);
        countDownLatch.await();
    }

    @Test
    public void testOnOpen() throws InterruptedException, UnsupportedEncodingException {

        Log.d(TAG, "testOnOpen");
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        final SignalingServerClient signalingServerClient =
                new SignalingServerClient(new MessageHandler() {
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
                    public void onDisconnect() {
                        Log.d(TAG, "onDisconnect");
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
        assertTrue(signalingServerClient.getSocketIO().connected());
        signalingServerClient.getSocketIO().disconnect();
    }
}
