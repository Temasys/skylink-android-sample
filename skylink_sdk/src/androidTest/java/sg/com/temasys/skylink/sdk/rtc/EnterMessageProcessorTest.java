package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
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
    private EnterMessageProcessor enterMessageProcessor;

    // Test values
    private String mid = "mid";
    private String rid = "rid";
    private boolean receiveOnly = false;
    private String agent = "Android";
    private String version = "version";
    private String remotePeerId = "remotePeerId";
    private String userData = "userData";
    private Object userInfo = null;


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
        enterObject.put("mid", remotePeerId);
        enterObject.put("rid", rid);
        enterObject.put("receiveOnly", receiveOnly);
        enterObject.put("agent", agent);
        enterObject.put("version", version);
        // TODO XR: Can remove after JS client update to compatible restart protocol.
        enterObject.put("target", mid);

        // Create userInfo
        SkylinkConfig configTest = new SkylinkConfig();
        UserInfo userInfo = new UserInfo(configTest, userData);
        // Set userInfo into enterObject.
        UserInfo.setUserInfo(enterObject, userInfo);

        // Mock test objects
        SkylinkConnection mockSkylinkConnection = mock(SkylinkConnection.class);
        SkylinkPeerService mockSkylinkPeerService = mock(SkylinkPeerService.class);

        // Stub for mock object methods to be called
        when(mockSkylinkConnection.getSkylinkPeerService()).thenReturn(mockSkylinkPeerService);

        enterMessageProcessor.setSkylinkConnection(mockSkylinkConnection);

        // Test method
        enterMessageProcessor.process(enterObject);

        // Verify receivedEnter called with right arguments
        // - Prepare to capture arguments
        ArgumentCaptor<String> argPeerId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PeerInfo> argPeerInfo = ArgumentCaptor.forClass(PeerInfo.class);
        ArgumentCaptor<JSONObject> argUserInfo = ArgumentCaptor.forClass(JSONObject.class);

        verify(mockSkylinkPeerService).receivedEnter(argPeerId.capture(), argPeerInfo.capture(), argUserInfo.capture());

        // - Check receivedEnter parameter 1
        assertEquals(remotePeerId, argPeerId.getValue());

        // - Check receivedEnter parameter 2
        assertEquals(receiveOnly, argPeerInfo.getValue().isReceiveOnly());
        assertEquals(agent, argPeerInfo.getValue().getAgent());
        assertEquals(version, argPeerInfo.getValue().getVersion());

        // - Check receivedEnter parameter 3
        //assertTrue(userInfo.equals(new UserInfo(argUserInfo.getValue())));
    }
}
