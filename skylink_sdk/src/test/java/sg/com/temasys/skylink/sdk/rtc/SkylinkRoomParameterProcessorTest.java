package sg.com.temasys.skylink.sdk.rtc;

import android.text.TextUtils;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;

/**
 * Tests related to SkylinkRoomParameterProcessor
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class SkylinkRoomParameterProcessorTest {

    private static final String TAG = SkylinkConnectionTest.class.getName();
    private AppServerClient appServerClient;
    private AppServerClientListener appServerClientListener;
    private SkylinkRoomParameterProcessor skylinkRoomParameterProcessor;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
        appServerClientListener = mock(AppServerClientListener.class);
        skylinkRoomParameterProcessor = new SkylinkRoomParameterProcessor();
        appServerClient = new AppServerClient(appServerClientListener,
                skylinkRoomParameterProcessor);
    }

    @Test
    public void testGetParametersForRoomUrl() throws InterruptedException, IOException, JSONException {

        logD(TAG, "testGetParametersForRoomUrl");
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        skylinkRoomParameterProcessor.setRoomParameterListener(
                new RoomParameterListener() {
                    @Override
                    public void onRoomParameterSuccessful(RoomParameters params) {
                        logD(TAG, "onRoomParameterSuccessful");
                        assertNotNull("Parameters should not be null", params);
                        assertFalse(TextUtils.isEmpty(params.getAppOwner()));
                        assertFalse(TextUtils.isEmpty(params.getCid()));
                        assertFalse(TextUtils.isEmpty(params.getDisplayName()));
                        assertFalse(TextUtils.isEmpty(params.getLen()));
                        assertFalse(TextUtils.isEmpty(params.getRoomCred()));
                        assertFalse(TextUtils.isEmpty(params.getStart()));
                        assertFalse(TextUtils.isEmpty(params.getTimeStamp()));
                        assertFalse(TextUtils.isEmpty(params.getUserCred()));
                        assertFalse(TextUtils.isEmpty(params.getUserId()));
                        assertFalse(TextUtils.isEmpty(params.getIpSigserver()));
                        assertFalse(params.getPortSigserver() == 0);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onRoomParameterError(String message) {
                        fail("Should not be called!!");
                        countDownLatch.countDown();
                    }

                }
        );

        appServerClient.connectToRoom(TestConstants.SKYLINK_CONNECTION_STRING);
        countDownLatch.await();
    }

    @Test
    public void testGetRoomParametersForInvalidUrl() throws InterruptedException, IOException, JSONException {

        logD(TAG, "testGetRoomParametersForInvalidUrl");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        skylinkRoomParameterProcessor.setRoomParameterListener(
                new RoomParameterListener() {
                    @Override
                    public void onRoomParameterSuccessful(RoomParameters params) {
                        fail("Should not be called!! for invalid URL");
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onRoomParameterError(String message) {
                        logD(TAG, "onRoomParameterError(String): " + message);
                        countDownLatch.countDown();
                    }

                });

        appServerClient.connectToRoom(TestConstants.INVALID_SKYLINK_CONNECTION_STRING);
        countDownLatch.await();
    }
}
