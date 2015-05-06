package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests related to ByeMessageProcessor
 * <p/>
 * Created by janidu on 5/5/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class ByeMessageProcessorTest {

    private static final String TAG = ByeMessageProcessorTest.class.getSimpleName();
    private static final String mid = "1234";

    private MessageProcessor messageProcessor;
    private SkylinkConnection mockSkylinkConnection;

    @Before
    public void setUp() throws Exception {
        messageProcessor = new ByeMessageProcessor();
        mockSkylinkConnection = mock(SkylinkConnection.class);
    }

    @Test
    public void testCreateByeMessageProcessor() {
        assertNotNull(new ByeMessageProcessor());
    }

    @Test
    public void testWillIgnoreTargetedBye() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("target", "1234");

        messageProcessor.setSkylinkConnection(mockSkylinkConnection);

        // Should return immediately
        messageProcessor.process(jsonObject);
        verify(mockSkylinkConnection, never()).isPeerIdMCU(anyString());
    }

    @Test
    public void testProcess() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mid", mid);

        DataChannelManager dataChannelManager = mock(DataChannelManager.class);

        when(mockSkylinkConnection.getConnectionState()).thenReturn
                (SkylinkConnection.ConnectionState.CONNECT);

        when(mockSkylinkConnection.getDataChannelManager()).thenReturn(dataChannelManager);

        messageProcessor.setSkylinkConnection(mockSkylinkConnection);
        messageProcessor.process(jsonObject);

        verify(mockSkylinkConnection).isPeerIdMCU(mid);
        verify(dataChannelManager).disposeDC(mid);
    }
}
