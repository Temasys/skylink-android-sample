package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
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
    private SkylinkConnectionService mockSkylinkConnectionService;
    private SkylinkPeerService mockSkylinkPeerService;

    @Before
    public void setUp() throws Exception {
        messageProcessor = new ByeMessageProcessor();
        mockSkylinkConnection = mock(SkylinkConnection.class);
        mockSkylinkConnectionService = mock(SkylinkConnectionService.class);
        mockSkylinkPeerService = mock(SkylinkPeerService.class);

        when(mockSkylinkConnection.getSkylinkConnectionService())
                .thenReturn(mockSkylinkConnectionService);
        when(mockSkylinkConnection.getSkylinkPeerService()).thenReturn(mockSkylinkPeerService);
    }

    @Test
    public void testCreateByeMessageProcessor() {
        assertNotNull(new ByeMessageProcessor());
    }

    @Test
    public void testProcess() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mid", mid);

        DataChannelManager dataChannelManager = mock(DataChannelManager.class);

        when(mockSkylinkConnection.getSkylinkConnectionService().getConnectionState()).thenReturn
                (SkylinkConnectionService.ConnectionState.CONNECTING);

        when(mockSkylinkConnection.getDataChannelManager()).thenReturn(dataChannelManager);

        messageProcessor.setSkylinkConnection(mockSkylinkConnection);
        messageProcessor.process(jsonObject);

        verify(mockSkylinkPeerService).receivedBye(mid);
    }

}
