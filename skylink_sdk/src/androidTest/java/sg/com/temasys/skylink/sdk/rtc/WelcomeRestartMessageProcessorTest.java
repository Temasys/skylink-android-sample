package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Purpose of this class is to test WelcomeRestartMessageProcessor
 * Created by janidu on 13/5/15.
 */
public class WelcomeRestartMessageProcessorTest {

    private static final String peerId = "1234";
    private static final JSONObject userInfo = new JSONObject();
    private static final String agent = "android";
    private static final boolean receiveOnly = true;
    private static final boolean enableIceTrickle = true;
    private static final boolean enableDataChannel = true;
    private static final String version = "1";
    private static final Double weight = new Double(1.00d);

    private SkylinkConnection skylinkConnection;
    private SkylinkPeerService skylinkPeerService;
    private WelcomeRestartMessageProcessor welcomeRestartMessageProcessor;

    @Before
    public void setUp() throws Exception {
        skylinkConnection = mock(SkylinkConnection.class);
        skylinkPeerService = mock(SkylinkPeerService.class);
        when(skylinkConnection.getSkylinkPeerService()).thenReturn(skylinkPeerService);

        welcomeRestartMessageProcessor = new WelcomeRestartMessageProcessor();
        welcomeRestartMessageProcessor.setSkylinkConnection(skylinkConnection);
    }

    @Test
    public void testProcessWelcome() throws Exception {

        welcomeRestartMessageProcessor.process(getJsonObject("welcome"));

        ArgumentCaptor<String> argPeerId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PeerInfo> argPeerInfo = ArgumentCaptor.forClass(PeerInfo.class);
        ArgumentCaptor<JSONObject> argUserInfo = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<Double> argWeight = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Boolean> argRestart = ArgumentCaptor.forClass(Boolean.class);

        verify(skylinkPeerService).receivedWelcomeRestart(argPeerId.capture(),
                argPeerInfo.capture(), argUserInfo.capture(), argWeight.capture(),
                argRestart.capture());

        assertEquals(peerId, argPeerId.getValue());

        PeerInfo peerInfo = argPeerInfo.getValue();
        assertTrue(agent.equals(peerInfo.getAgent()));
        assertEquals(receiveOnly, peerInfo.isReceiveOnly());
        assertEquals(version, peerInfo.getVersion());
        assertEquals(enableIceTrickle, peerInfo.isEnableIceTrickle());
        assertEquals(enableDataChannel, peerInfo.isEnableDataChannel());

        assertTrue(weight.doubleValue() == argWeight.getValue().doubleValue());
        assertFalse(argRestart.getValue());
    }

    @Test
    public void testProcessRestart() throws Exception {

        welcomeRestartMessageProcessor.process(getJsonObject("restart"));

        ArgumentCaptor<String> argPeerId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PeerInfo> argPeerInfo = ArgumentCaptor.forClass(PeerInfo.class);
        ArgumentCaptor<JSONObject> argUserInfo = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<Double> argWeight = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Boolean> argRestart = ArgumentCaptor.forClass(Boolean.class);

        verify(skylinkPeerService).receivedWelcomeRestart(argPeerId.capture(),
                argPeerInfo.capture(), argUserInfo.capture(), argWeight.capture(),
                argRestart.capture());

        assertEquals(peerId, argPeerId.getValue());

        PeerInfo peerInfo = argPeerInfo.getValue();
        assertTrue(agent.equals(peerInfo.getAgent()));
        assertEquals(receiveOnly, peerInfo.isReceiveOnly());
        assertEquals(version, peerInfo.getVersion());
        assertEquals(enableIceTrickle, peerInfo.isEnableIceTrickle());
        assertEquals(enableDataChannel, peerInfo.isEnableDataChannel());

        assertTrue(weight.doubleValue() == argWeight.getValue().doubleValue());
        assertTrue(argRestart.getValue());
    }

    private JSONObject getJsonObject(String type) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", type);
        jsonObject.put("mid", peerId);
        jsonObject.put("userInfo", userInfo);
        jsonObject.put("agent", agent);
        jsonObject.put("receiveOnly", receiveOnly);
        jsonObject.put("version", version);
        jsonObject.put("enableIceTrickle", enableIceTrickle);
        jsonObject.put("enableDataChannel", enableDataChannel);
        jsonObject.put("weight", weight.doubleValue());
        return jsonObject;
    }
}