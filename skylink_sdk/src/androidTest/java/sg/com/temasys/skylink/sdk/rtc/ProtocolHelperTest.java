package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    public void testProcessRoomLockStatus() throws JSONException {

        // If the currently the room is locked and we receive that the room is locked
        // listener should not be called
        boolean currentStatus = ProtocolHelper.processRoomLockStatus(true, "1000", true,
                new LifeCycleListener() {
                    @Override
                    public void onConnect(boolean isSuccessful, String message) {
                        fail();
                    }

                    @Override
                    public void onWarning(int errorCode, String message) {
                        fail();
                    }

                    @Override
                    public void onDisconnect(int errorCode, String message) {
                        fail();
                    }

                    @Override
                    public void onReceiveLog(String message) {
                        fail();
                    }

                    @Override
                    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                        fail();
                    }
                });

        assertTrue(currentStatus);

        // If the currently the room is locked and we receive that the room is unlocked
        // listener should be called

        currentStatus = ProtocolHelper.processRoomLockStatus(true, "1000", false, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail();
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail();
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail();
            }

            @Override
            public void onReceiveLog(String message) {
                fail();
            }

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                assertFalse(lockStatus);
                assertEquals(remotePeerId, "1000");
            }
        });

        assertFalse(currentStatus);


        // If the currently the room is unlocked and we receive that the room is locked
        // listener should be called

        currentStatus = ProtocolHelper.processRoomLockStatus(false, "1000", true, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail();
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail();
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail();
            }

            @Override
            public void onReceiveLog(String message) {
                fail();
            }

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                assertTrue(lockStatus);
                assertEquals(remotePeerId, "1000");
            }
        });

        assertTrue(currentStatus);

        // If the currently the room is unlocked and we receive that the room is unlocked
        // listener should not be called

        currentStatus = ProtocolHelper.processRoomLockStatus(false, "1000", false, new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail();
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail();
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail();
            }

            @Override
            public void onReceiveLog(String message) {
                fail();
            }

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertFalse(currentStatus);
    }

    @Test
    public void testSendRoomLockStatusLockRoom() throws JSONException {

        WebServerClient mockedWebServerClient = mock(WebServerClient.class);
        when(mockedWebServerClient.getRoomId()).thenReturn("1000");
        when(mockedWebServerClient.getSid()).thenReturn("senderId");

        ProtocolHelper.sendRoomLockStatus(mockedWebServerClient, true);

        JSONObject dict = new JSONObject();
        dict.put("rid", "1000");
        dict.put("mid", "senderId");
        dict.put("lock", true);
        dict.put("type", "roomLockEvent");
        verify(mockedWebServerClient).sendMessage(Mockito.any(JSONObject.class));
    }

    @Test
    public void testWarningWithReasonFastMessage() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "fastmsg");

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "warning", "fastmsg", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertFalse(shouldDisconnect);
    }

    @Test
    public void testWarningWithReasonLocked() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "warning", "locked",
                new LifeCycleListener() {
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

                    @Override
                    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                        fail();
                    }
                });

        assertFalse(shouldDisconnect);
    }

    @Test
    public void testWarningWithReasonRoomFull() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "warning", "roomfull",
                new LifeCycleListener() {
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

                    @Override
                    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                        fail();
                    }
                });

        assertFalse(shouldDisconnect);
    }

    @Test
    public void testWarningWithReasonDuplicatedLogin() throws JSONException {


        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "warning", "duplicatedLogin"
                , new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertFalse(shouldDisconnect);
    }

    @Test
    public void testWarningWithReasonServerError() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "warning", "serverError", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertFalse(shouldDisconnect);
    }

    @Test
    public void testWarningWithReasonVerification() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "warning", "verification"
                , new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertFalse(shouldDisconnect);
    }

    @Test
    public void testWarningWithReasonExpired() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "expired");

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "warning", "expired"
                , new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertFalse(shouldDisconnect);
    }

    @Test
    public void testWarningWithReasonRoomClosed() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "roomclose");

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "warning", "roomclose"
                , new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertFalse(shouldDisconnect);
    }

    @Test
    public void testWarningWithReasonRoomToClose() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "toclose");

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "warning", "toclose", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertFalse(shouldDisconnect);
    }

    @Test
    public void testWarningWithReasonSeatQuota() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "warning", "seatquota", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertFalse(shouldDisconnect);
    }

    @Test
    public void testRejectWithReasonFastMessage() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "reject", "fastmsg", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertTrue(shouldDisconnect);
    }

    @Test
    public void testRejectWithReasonLocked() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "reject", "locked", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertTrue(shouldDisconnect);
    }

    @Test
    public void testRejectWithReasonRoomFull() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", expectedInfo);
        jsonObject.put("action", "reject");
        jsonObject.put("reason", "roomfull");

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "reject", "roomfull", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertTrue(shouldDisconnect);
    }

    @Test
    public void testRejectWithReasonDuplicatedLogin() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "reject", "duplicatedLogin", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertTrue(shouldDisconnect);
    }

    @Test
    public void testRejectWithReasonServerError() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "reject", "serverError", new LifeCycleListener() {
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
                assertEquals(errorCode, ErrorCodes.REDIRECT_REASON_SERVER_ERROR);
            }

            @Override
            public void onReceiveLog(String message) {
                fail("Should not be called");
            }

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertTrue(shouldDisconnect);
    }

    @Test
    public void testRejectWithReasonVerification() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "reject", "verification", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertTrue(shouldDisconnect);
    }

    @Test
    public void testRejectWithReasonExpired() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "reject", "expired", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertTrue(shouldDisconnect);
    }

    @Test
    public void testRejectWithReasonRoomClosed() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "reject", "roomclose", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertTrue(shouldDisconnect);
    }

    public void testRejectWithReasonRoomToClose() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "reject", "toclose", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertTrue(shouldDisconnect);
    }

    @Test
    public void testRejectWithReasonSeatQuota() throws JSONException {

        boolean shouldDisconnect = ProtocolHelper.processRedirect(expectedInfo, "reject", "seatquota", new LifeCycleListener() {
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

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail();
            }
        });

        assertTrue(shouldDisconnect);
    }
}
