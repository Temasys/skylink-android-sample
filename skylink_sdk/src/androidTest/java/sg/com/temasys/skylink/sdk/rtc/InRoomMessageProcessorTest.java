package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests related to InRoomMessageProcessor
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class InRoomMessageProcessorTest {

    private static final String TAG = InRoomMessageProcessorTest.class.getSimpleName();
    private InRoomMessageProcessor inRoomMessageProcessor;

    @Before
    public void setup() {
        inRoomMessageProcessor = new InRoomMessageProcessor();
    }

    @Test
    public void testCreateInRoomMessageProcessor() {
        MessageProcessor processor = new InRoomMessageProcessor();
        assertNotNull(processor);
    }

    @Test
    public void testProcessing() throws JSONException {

        // Mock
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        Map<String, Object> advancedOptions = new HashMap<>();
        advancedOptions.put("STUN", true);
        skylinkConfig.setAdvancedOptions(advancedOptions);

        SkylinkConnection mockSkylinkConnection = mock(SkylinkConnection.class);
        WebServerClient mockWebServerClient = mock(WebServerClient.class);
        WebServerClient.IceServersObserver mockIceServersObserver = mock(WebServerClient.IceServersObserver.class);

        DataChannelManager mockDataChannelManager = mock(DataChannelManager.class);

        // Stub
        when(mockSkylinkConnection.getWebServerClient()).thenReturn(mockWebServerClient);
        when(mockSkylinkConnection.getMyConfig()).thenReturn(skylinkConfig);
        when(mockSkylinkConnection.getIceServersObserver()).thenReturn(mockIceServersObserver);
        when(mockSkylinkConnection.getDataChannelManager()).thenReturn(mockDataChannelManager);
        when(mockSkylinkConnection.getMyUserData()).thenReturn("1234");
        when(mockSkylinkConnection.getPcObserverPool()).thenReturn(new HashMap<String, SkylinkConnection.PCObserver>());

        inRoomMessageProcessor.setSkylinkConnection(mockSkylinkConnection);

        JSONObject iceServer = new JSONObject();
        iceServer.put("url", "stun:");
        iceServer.put("credential", "testCredentials");

        JSONArray jsonArray = new JSONArray();
        jsonArray.put(iceServer);

        JSONObject pcObject = new JSONObject();
        pcObject.put("iceServers", jsonArray);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sid", "1234");
        jsonObject.put("pc_config", pcObject);

        inRoomMessageProcessor.process(jsonObject);

        verify(mockWebServerClient).setSid("1234");
        verify(mockDataChannelManager).setMid("1234");
        verify(mockDataChannelManager).setDisplayName("1234");

        // Should call rejoinRestart
        verify(mockSkylinkConnection).rejoinRestart();

        // Should call to initializePcRelatedMaps since PCObserverPool is null
        when(mockSkylinkConnection.getPcObserverPool()).thenReturn(null);
        inRoomMessageProcessor.process(jsonObject);
        verify(mockSkylinkConnection).initializePcRelatedMaps();
    }
}
