package sg.com.temasys.skylink.sdk.rtc;

import android.text.TextUtils;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests related to RoomParameterService
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class RoomParameterServiceTest {

    private static final String TAG = SkylinkConnectionTest.class.getName();

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
    }

    @Test
    public void testGetParametersForRoomUrl() throws InterruptedException {

        Log.d(TAG, "testGetParametersForRoomUrl");
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        RoomParameterService roomParameterService = new RoomParameterService
                (new RoomParameterServiceListener() {
                    @Override
                    public void onRoomParameterSuccessful(AppRTCSignalingParameters params) {
                        Log.d(TAG, "onRoomParameterSuccessful");
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
                        assertNotNull(params.getVideoConstraints());
                        assertFalse(TextUtils.isEmpty(params.getIpSigserver()));
                        assertFalse(params.getPortSigserver() == 0);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onRoomParameterError(int message) {
                        fail("Should not be called!!");
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onRoomParameterError(String message) {
                        fail("Should not be called!!");
                        countDownLatch.countDown();
                    }

                });

        roomParameterService.execute(TestConstants.SKYLINK_CONNECTION_STRING);
        countDownLatch.await();
    }

    @Test
    public void testGetRoomParametersForInvalidUrl() throws InterruptedException {

        Log.d(TAG, "testGetRoomParametersForInvalidUrl");
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        RoomParameterService roomParameterService = new RoomParameterService
                (new RoomParameterServiceListener() {
                    @Override
                    public void onRoomParameterSuccessful(AppRTCSignalingParameters params) {
                        fail("Should not be called!! for invalid URL");
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onRoomParameterError(int message) {
                        Log.d(TAG, "onRoomParameterError " + message);
                        assertTrue(true);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onRoomParameterError(String message) {
                        Log.d(TAG, "onRoomParameterError " + message);
                        assertTrue(true);
                        countDownLatch.countDown();
                    }

                });

        roomParameterService.execute(TestConstants.INVALID_SKYLINK_CONNECTION_STRING);
        countDownLatch.await();
    }
}
