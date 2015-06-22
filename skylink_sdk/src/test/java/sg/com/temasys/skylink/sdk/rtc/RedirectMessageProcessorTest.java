package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;


/**
 * Tests related to RedirectMessage.
 * Created by janidu on 11/5/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class RedirectMessageProcessorTest {

    public static final String TEST_INFO = "testInfo";
    private SkylinkConnection skylinkConnection;
    private SkylinkConnectionService skylinkConnectionService;
    private SignalingMessageProcessingService signalingMessageProcessingService;
    private RedirectMessageProcessor redirectMessageProcessor;

    @Before
    public void setup() {
        redirectMessageProcessor = new RedirectMessageProcessor();

        // Mock objects
        skylinkConnection = spy(SkylinkConnection.getInstance());
        skylinkConnectionService = spy(new SkylinkConnectionService(skylinkConnection
        ));
        signalingMessageProcessingService = spy(new SignalingMessageProcessingService(
                skylinkConnection, skylinkConnectionService, new MessageProcessorFactory(), skylinkConnectionService));
        redirectMessageProcessor.setSkylinkConnection(skylinkConnection);

        doReturn(skylinkConnectionService)
                .when(skylinkConnection).getSkylinkConnectionService();
        doReturn(SkylinkConnectionService.ConnectionState.CONNECTING)
                .when(skylinkConnectionService).getConnectionState();

        doReturn(skylinkConnectionService).when(skylinkConnection).getSkylinkConnectionService();
        doReturn(signalingMessageProcessingService).when(skylinkConnectionService)
                .getSignalingMessageProcessingService();
    }

    @Test
    public void testProcessWarning() throws JSONException {
        LifeCycleListener lifeCycleListener = new LifeCycleListener() {

            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail();
            }

            @Override
            public void onWarning(int errorCode, String message) {
                assertEquals(ErrorCodes.REDIRECT_REASON_FAST_MSG, errorCode);
                assertTrue(TEST_INFO.equals(message));
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

        doReturn(lifeCycleListener).when(skylinkConnection).getLifeCycleListener();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", TEST_INFO);
        jsonObject.put("action", "warning");
        jsonObject.put("reason", "fastmsg");
        redirectMessageProcessor.process(jsonObject);
    }

    @Test
    public void testProcessReject() throws JSONException {

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
                assertEquals(ErrorCodes.REDIRECT_REASON_LOCKED, errorCode);
                assertTrue(TEST_INFO.equals(message));
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

        doReturn(lifeCycleListener).when(skylinkConnection).getLifeCycleListener();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("info", TEST_INFO);
        jsonObject.put("action", "reject");
        jsonObject.put("reason", "locked");
        redirectMessageProcessor.process(jsonObject);
    }
}
