package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests related to PingMessageProcessor
 * <p/>
 * Created by janidu on 5/5/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class PingMessageProcessorTest {

    private static final String TAG = PingMessageProcessorTest.class.getName();

    private static final String target = "1234";
    private static final String socketId = "uniqueSocketId";
    private static final String rid = "123456";
    private static final String mid = "12345";

    private SkylinkConnection mockSkylinkConnection;
    private PingMessageProcessor pingMessageProcessor;
    private SkylinkConnectionService skylinkConnectionService;

    @Before
    public void setUp() throws Exception {
        mockSkylinkConnection = mock(SkylinkConnection.class);
        skylinkConnectionService = mock(SkylinkConnectionService.class);

        pingMessageProcessor = new PingMessageProcessor();
        pingMessageProcessor.setSkylinkConnection(mockSkylinkConnection);
    }

    @Test
    public void testCanBeCreated() {
        assertNotNull(new PingMessageProcessor());
    }

    @Test
    public void testProcess() throws JSONException {

        when(skylinkConnectionService.getRoomId()).thenReturn(rid);
        when(skylinkConnectionService.getSid()).thenReturn(socketId);
        when(mockSkylinkConnection.getSkylinkConnectionService()).thenReturn(skylinkConnectionService);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("target", target);
        jsonObject.put("mid", mid);

        pingMessageProcessor.process(jsonObject);

        ArgumentCaptor<JSONObject> argument = ArgumentCaptor.forClass(JSONObject.class);
        verify(skylinkConnectionService).sendMessage(argument.capture());

        assertEquals("ping", argument.getValue().getString("type"));
        assertEquals(socketId, argument.getValue().getString("mid"));
        assertEquals(mid, argument.getValue().getString("target"));
        assertEquals(rid, argument.getValue().getString("rid"));
    }
}
