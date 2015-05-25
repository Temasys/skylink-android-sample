package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.webrtc.PeerConnection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
    public static final String SOCKET_ID = "1234";
    public static final String STUN = "stun:";
    public static final String TURN = "turn:";
    public static final String TEST_CREDENTIALS = "testCredentials";

    private InRoomMessageProcessor inRoomMessageProcessor;
    private SkylinkPeerService mockSkylinkPeerService;
    private SkylinkConnection mockSkylinkConnection;

    @Before
    public void setup() {
        ShadowLog.stream = System.out;
        inRoomMessageProcessor = new InRoomMessageProcessor();
        mockSkylinkPeerService = mock(SkylinkPeerService.class);

        mockSkylinkConnection = mock(SkylinkConnection.class);
        when(mockSkylinkConnection.getSkylinkPeerService()).thenReturn(mockSkylinkPeerService);
    }

    @Test
    public void testCreateInRoomMessageProcessor() {
        MessageProcessor processor = new InRoomMessageProcessor();
        assertNotNull(processor);
    }

    @Test
    public void testProcessingWithTurn() throws JSONException {
        // Configure the skylinkConfig object
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        Map<String, Object> advancedOptions = new HashMap<>();
        advancedOptions.put("TURN", true);
        skylinkConfig.setAdvancedOptions(advancedOptions);

        when(mockSkylinkConnection.getMyConfig()).thenReturn(skylinkConfig);

        inRoomMessageProcessor.setSkylinkConnection(mockSkylinkConnection);
        inRoomMessageProcessor.process(getJson(TURN));

        ArgumentCaptor<String> argCapPeerId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> argCapIceServers = ArgumentCaptor.forClass(List.class);

        verify(mockSkylinkConnection.getSkylinkPeerService()).receivedInRoom(argCapPeerId.capture(),
                argCapIceServers.capture());

        assertEquals(SOCKET_ID, argCapPeerId.getValue());
        assertTrue(argCapIceServers.getValue().size() == 1);
        assertNotNull(argCapIceServers.getValue().get(0));

        PeerConnection.IceServer server = (PeerConnection.IceServer) argCapIceServers.getValue().get(0);
        assertEquals(TURN, server.uri);
        assertEquals("", server.username);
        assertEquals(TEST_CREDENTIALS, server.password);
    }

    @Test
    public void testProcessingWithStun() throws JSONException {

        // Configure the skylinkConfig object
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        Map<String, Object> advancedOptions = new HashMap<>();
        advancedOptions.put("STUN", true);
        skylinkConfig.setAdvancedOptions(advancedOptions);

        when(mockSkylinkConnection.getMyConfig()).thenReturn(skylinkConfig);

        inRoomMessageProcessor.setSkylinkConnection(mockSkylinkConnection);
        inRoomMessageProcessor.process(getJson(STUN));

        ArgumentCaptor<String> argCapPeerId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> argCapIceServers = ArgumentCaptor.forClass(List.class);

        verify(mockSkylinkConnection.getSkylinkPeerService()).receivedInRoom(argCapPeerId.capture(),
                argCapIceServers.capture());

        assertEquals(SOCKET_ID, argCapPeerId.getValue());
        assertTrue(argCapIceServers.getValue().size() == 1);
        assertNotNull(argCapIceServers.getValue().get(0));

        PeerConnection.IceServer server = (PeerConnection.IceServer) argCapIceServers.getValue().get(0);
        assertEquals(STUN, server.uri);
        assertEquals("", server.username);
        assertEquals(TEST_CREDENTIALS, server.password);
    }

    @Test
    public void testProcessingWithoutAdvancedOptions() throws JSONException {

        when(mockSkylinkConnection.getMyConfig()).thenReturn(new SkylinkConfig());

        inRoomMessageProcessor.setSkylinkConnection(mockSkylinkConnection);
        inRoomMessageProcessor.process(getJson(STUN));

        ArgumentCaptor<String> argCapPeerId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> argCapIceServers = ArgumentCaptor.forClass(List.class);

        verify(mockSkylinkConnection.getSkylinkPeerService()).receivedInRoom(argCapPeerId.capture(),
                argCapIceServers.capture());

        assertEquals(SOCKET_ID, argCapPeerId.getValue());
        assertTrue(argCapIceServers.getValue().size() == 1);
        assertNotNull(argCapIceServers.getValue().get(0));

        PeerConnection.IceServer server = (PeerConnection.IceServer) argCapIceServers.getValue().get(0);
        assertEquals(STUN, server.uri);
        assertEquals("", server.username);
        assertEquals(TEST_CREDENTIALS, server.password);
    }

    private JSONObject getJson(String url) throws JSONException {

        JSONObject iceServer = new JSONObject();
        iceServer.put("url", url);
        iceServer.put("credential", TEST_CREDENTIALS);

        JSONArray jsonArray = new JSONArray();
        jsonArray.put(iceServer);

        JSONObject pcObject = new JSONObject();
        pcObject.put("iceServers", jsonArray);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sid", SOCKET_ID);
        jsonObject.put("pc_config", pcObject);

        return jsonObject;
    }
}
