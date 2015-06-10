package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests related to RoomLockMessageProcessor
 * Created by janidu on 5/5/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class RoomLockMessageProcessorTest {

    private static final String EXPECTED_ID = "123456";
    private static final boolean EXPECTED_LOCK_STATUS = true;
    private static final boolean EXPECTED_UNLOCK_STATUS = false;

    private SkylinkConnection skylinkConnection;
    private SkylinkConnectionService skylinkConnectionService;
    private SignalingMessageProcessingService signalingMessageProcessingService;
    private RoomLockMessageProcessor roomLockMessageProcessor;

    @Before
    public void setup() {
        roomLockMessageProcessor = new RoomLockMessageProcessor();

        skylinkConnection = spy(SkylinkConnection.getInstance());
        // Mock objects
        skylinkConnection = spy(SkylinkConnection.getInstance());
        skylinkConnectionService = spy(new SkylinkConnectionService(skylinkConnection
        ));
        signalingMessageProcessingService = spy(new SignalingMessageProcessingService(
                skylinkConnection, skylinkConnectionService, new MessageProcessorFactory(), skylinkConnectionService));
        roomLockMessageProcessor.setSkylinkConnection(skylinkConnection);

        doReturn(skylinkConnectionService)
                .when(skylinkConnection).getSkylinkConnectionService();
        doReturn(SkylinkConnectionService.ConnectionState.CONNECTING)
                .when(skylinkConnectionService).getConnectionState();

        doReturn(skylinkConnectionService).when(skylinkConnection).getSkylinkConnectionService();
        doReturn(signalingMessageProcessingService).when(skylinkConnectionService)
                .getSignalingMessageProcessingService();
        doReturn(SkylinkConnectionService.ConnectionState.CONNECTING)
                .when(skylinkConnectionService).getConnectionState();

    }

    @Test
    public void testProcessingWithSameLockStatusLocked() throws JSONException {

        LifeCycleListener lifeCycleListener = new LifeCycleListener() {
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
        };

        doReturn(true).when(skylinkConnection)
                .isRoomLocked();
        doReturn(lifeCycleListener).when(skylinkConnection).getLifeCycleListener();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lock", true);
        jsonObject.put("mid", EXPECTED_ID);

        roomLockMessageProcessor.process(jsonObject);
    }

    @Test
    public void testProcessingWithSameLockStatusUnlocked() throws JSONException {

        LifeCycleListener lifeCycleListener = new LifeCycleListener() {
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
        };

        doReturn(false).when(skylinkConnection)
                .isRoomLocked();
        doReturn(lifeCycleListener).when(skylinkConnection).getLifeCycleListener();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lock", false);
        jsonObject.put("mid", EXPECTED_ID);

        roomLockMessageProcessor.process(jsonObject);
    }

    @Test
    public void testProcessingWithLockedRoom() throws JSONException {

        LifeCycleListener lifeCycleListener = new LifeCycleListener() {
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
                assertTrue(remotePeerId.equals(EXPECTED_ID));
                assertEquals(EXPECTED_LOCK_STATUS, lockStatus);
            }
        };

        doReturn(false).when(skylinkConnection)
                .isRoomLocked();
        doReturn(lifeCycleListener).when(skylinkConnection).getLifeCycleListener();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lock", EXPECTED_LOCK_STATUS);
        jsonObject.put("mid", EXPECTED_ID);

        roomLockMessageProcessor.process(jsonObject);
        verify(skylinkConnection).setRoomLocked(EXPECTED_LOCK_STATUS);
    }

    @Test
    public void testProcessingWithUnLockedRoom() throws JSONException {

        LifeCycleListener lifeCycleListener = new LifeCycleListener() {
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
                assertTrue(remotePeerId.equals(EXPECTED_ID));
                assertEquals(EXPECTED_UNLOCK_STATUS, lockStatus);
            }
        };

        doReturn(true).when(skylinkConnection)
                .isRoomLocked();
        doReturn(lifeCycleListener).when(skylinkConnection).getLifeCycleListener();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lock", EXPECTED_UNLOCK_STATUS);
        jsonObject.put("mid", EXPECTED_ID);

        roomLockMessageProcessor.process(jsonObject);
        verify(skylinkConnection).setRoomLocked(EXPECTED_UNLOCK_STATUS);
    }
}
