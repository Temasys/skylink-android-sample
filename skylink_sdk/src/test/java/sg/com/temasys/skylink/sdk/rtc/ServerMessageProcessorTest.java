package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import sg.com.temasys.skylink.sdk.listener.MessagesListener;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests related to ServerMessageProcessor
 * Created by janidu on 11/5/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class ServerMessageProcessorTest {

    private static final String peerId = "1234";
    private static final String data = "TestData";

    private ServerMessageProcessor serverMessageProcessor;
    private SkylinkConnection skylinkConnection;
    private SkylinkConnectionService skylinkConnectionService;

    @Before
    public void setup() {
        skylinkConnection = spy(SkylinkConnection.getInstance());
        skylinkConnectionService = mock(SkylinkConnectionService.class);
        serverMessageProcessor = new ServerMessageProcessor();
        serverMessageProcessor.setSkylinkConnection(skylinkConnection);
    }

    @Test
    public void testProcessPublic() throws JSONException {

        MessagesListener messagesListener = new MessagesListener() {
            @Override
            public void onServerMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
                assertTrue(remotePeerId.equals(peerId));
                assertFalse(isPrivate);
                assertTrue(data.equals(message.toString()));
            }

            @Override
            public void onP2PMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
                fail();
            }
        };

        doReturn(messagesListener).when(skylinkConnection).getMessagesListener();
        doReturn(false).when(skylinkConnection).isPeerIdMCU(peerId);
        doReturn(skylinkConnectionService)
                .when(skylinkConnection).getSkylinkConnectionService();
        doReturn(SkylinkConnectionService.ConnectionState.CONNECTING)
                .when(skylinkConnectionService).getConnectionState();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mid", peerId);
        jsonObject.put("data", data);
        jsonObject.put("type", "public");

        serverMessageProcessor.process(jsonObject);
    }

    @Test
    public void testProcessPrivate() throws JSONException {

        MessagesListener messagesListener = new MessagesListener() {
            @Override
            public void onServerMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
                assertTrue(remotePeerId.equals(peerId));
                assertTrue(isPrivate);
                assertTrue(data.equals(message.toString()));
            }

            @Override
            public void onP2PMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
                fail();
            }
        };

        doReturn(messagesListener).when(skylinkConnection).getMessagesListener();
        doReturn(false).when(skylinkConnection).isPeerIdMCU(peerId);
        doReturn(skylinkConnectionService)
                .when(skylinkConnection).getSkylinkConnectionService();
        doReturn(SkylinkConnectionService.ConnectionState.CONNECTING)
                .when(skylinkConnectionService).getConnectionState();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mid", peerId);
        jsonObject.put("data", data);
        jsonObject.put("type", "private");

        serverMessageProcessor.process(jsonObject);
    }
}
