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
import static org.mockito.Mockito.never;
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

    private static final String TAG = PingMessageProcessorTest.class.getSimpleName();

    private static final String target = "1234";
    private static final String rid = "123456";
    private static final String mid = "12345";

    private SkylinkConnection mockSkylinkConnection;
    private PingMessageProcessor pingMessageProcessor;
    private WebServerClient mockWebServerClient;

    @Before
    public void setUp() throws Exception {
        mockSkylinkConnection = mock(SkylinkConnection.class);
        mockWebServerClient = mock(WebServerClient.class);

        pingMessageProcessor = new PingMessageProcessor();
        pingMessageProcessor.setSkylinkConnection(mockSkylinkConnection);
    }

    @Test
    public void testCanBeCreated() {
        assertNotNull(new PingMessageProcessor());
    }

    @Test
    public void testWillNotProcessForDifferentTarget() throws JSONException {
        when(mockWebServerClient.getSid()).thenReturn(target);
        when(mockSkylinkConnection.getWebServerClient()).thenReturn(mockWebServerClient);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("target", "differentTarget");
        jsonObject.put("mid", target);

        pingMessageProcessor.process(jsonObject);

        // Should not proceed up to this point
        verify(mockWebServerClient, never()).getRoomId();
    }

    @Test
    public void testProcess() throws JSONException {

        when(mockWebServerClient.getRoomId()).thenReturn(rid);
        when(mockWebServerClient.getSid()).thenReturn(target);
        when(mockSkylinkConnection.getWebServerClient()).thenReturn(mockWebServerClient);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("target", target);
        jsonObject.put("mid", mid);

        pingMessageProcessor.process(jsonObject);

        ArgumentCaptor<JSONObject> argument = ArgumentCaptor.forClass(JSONObject.class);
        verify(mockWebServerClient).sendMessage(argument.capture());

        assertEquals("ping", argument.getValue().getString("type"));
        assertEquals(target, argument.getValue().getString("mid"));
        assertEquals(mid, argument.getValue().getString("target"));
        assertEquals(rid, argument.getValue().getString("rid"));
    }
}
