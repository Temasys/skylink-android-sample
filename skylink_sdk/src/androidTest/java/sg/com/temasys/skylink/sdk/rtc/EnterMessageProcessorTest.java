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
 * Tests related to EnterMessageProcessor
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class EnterMessageProcessorTest {

    private static final String TAG = EnterMessageProcessorTest.class.getSimpleName();
    private enterMessageProcessor EnterMessageProcessor;

    // Test values
    private String mid = "mid";
    private String rid = "rid";
    private boolean receiveOnly = false;
    private String agent = "Android";
    private String version = "version";
    private String remotePeerId = "remotePeerId";
    private String userData = "userData";


    @Before
    public void setup() {
        enterMessageProcessor = new EnterMessageProcessor();
    }

    @Test
    public void testCreateEnterMessageProcessor() {
        MessageProcessor processor = new EnterMessageProcessor();
        assertNotNull(processor);
    }

    @Test
    public void testProcessing() throws JSONException {

        // Create enter JSON for testing
        JSONObject enterObject = new JSONObject();
        enterObject.put("type", "enter");
        enterObject.put("mid", webServerClient.getSid());
        enterObject.put("rid", webServerClient.getRoomId());
        enterObject.put("receiveOnly", false);
        enterObject.put("agent", "Android");
        enterObject.put("version", BuildConfig.VERSION_NAME);
        // TODO XR: Can remove after JS client update to compatible restart protocol.
        enterObject.put("target", remotePeerId);
        enterObject.put("userData", userData);


        // Mock test objects
        SkylinkConnection mockSkylinkConnection = mock(SkylinkConnection.class);
        SkylinkPeerService mockSkylinkPeerService = mock(SkylinkPeerService.class);
        WebServerClient.IceServersObserver mockIceServersObserver = mock(WebServerClient.IceServersObserver.class);

        DataChannelManager mockDataChannelManager = mock(DataChannelManager.class);

        // Stub for mock object methods to be called
        when(mockSkylinkConnection.getSkylinkPeerService()).thenReturn(mockSkylinkPeerService);


        enterMessageProcessor.setSkylinkConnection(mockSkylinkConnection);


        enterMessageProcessor.process(enterObject);

        ArgumentCaptor<PeerInfo> peerInfo = ArgumentCaptor.forClass(PeerInfo.class);
        verify(mock).doSomething(argument.capture());

        verify(mockSkylinkPeerService).receivedEnter(peerId, peerInfo.capture(), userInfo);
        assertEquals(agent, peerInfo.getValue().getAgent());

        // Should call rejoinRestart
        verify(mockSkylinkConnection).rejoinRestart();

        // Should call to initializePcRelatedMaps since PCObserverPool is null
        when(mockSkylinkConnection.getPcObserverPool()).thenReturn(null);
        enterMessageProcessor.process(jsonObject);
        verify(mockSkylinkConnection).initializePcRelatedMaps();
    }
}
