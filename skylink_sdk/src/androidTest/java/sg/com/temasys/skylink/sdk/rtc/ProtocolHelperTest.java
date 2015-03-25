package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests related to ProtocolHelper
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class ProtocolHelperTest {

    private String expectedInfo = "ExpectedInfo";

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testWarningWithReasonFastMessage() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "fastmsg");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_FAST_MSG);
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testWarningWithReasonLocked() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "locked");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_LOCKED);
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testWarningWithReasonRoomFull() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "roomfull");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_ROOM_FULL);
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testWarningWithReasonDuplicatedLogin() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "duplicatedLogin");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_DUPLICATED_LOGIN);
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testWarningWithReasonServerError() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "serverError");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_SERVER_ERROR);
            }


            @Override
            public void onDisconnect(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testWarningWithReasonVerification() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "verification");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_VERIFICATION);
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testWarningWithReasonExpired() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "expired");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_EXPIRED);
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testWarningWithReasonRoomClosed() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "roomclose");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_ROOM_CLOSED);
            }


            @Override
            public void onDisconnect(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testWarningWithReasonRoomToClose() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "toclose");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }


            @Override
            public void onWarning(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_ROOM_TO_CLOSED);
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testWarningWithReasonSeatQuota() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "seatquota");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_SEAT_QUOTA);
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testRejectWithReasonFastMessage() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "reject");
        jsonObject.put("reason", "fastmsg");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_FAST_MSG);
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testRejectWithReasonLocked() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "reject");
        jsonObject.put("reason", "locked");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }


            @Override
            public void onWarning(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_LOCKED);
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testRejectWithReasonRoomFull() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "reject");
        jsonObject.put("reason", "roomfull");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_ROOM_FULL);
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testRejectWithReasonDuplicatedLogin() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "reject");
        jsonObject.put("reason", "duplicatedLogin");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail("Should not be called");
            }


            @Override
            public void onDisconnect(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_DUPLICATED_LOGIN);
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    public void testRejectWithReasonServerError() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "reject");
        jsonObject.put("reason", "serverError");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail("Should not be called");
            }


            @Override
            public void onDisconnect(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_DUPLICATED_LOGIN);
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testRejectWithReasonVerification() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "reject");
        jsonObject.put("reason", "verification");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_VERIFICATION);
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testRejectWithReasonExpired() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "reject");
        jsonObject.put("reason", "expired");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_EXPIRED);
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testRejectWithReasonRoomClosed() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "reject");
        jsonObject.put("reason", "roomclose");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail("Should not be called");
            }


            @Override
            public void onDisconnect(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_ROOM_CLOSED);
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    public void testRejectWithReasonRoomToClose() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "reject");
        jsonObject.put("reason", "toclose");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_ROOM_TO_CLOSED);
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }

    @Test
    public void testRejectWithReasonSeatQuota() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "reject");
        jsonObject.put("reason", "seatquota");

        ProtocolHelper.processRedirect(jsonObject, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("Should not be called");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail("Should not be called");
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                assertEquals(message, expectedInfo);
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_SEAT_QUOTA);
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }
        });
    }
}
