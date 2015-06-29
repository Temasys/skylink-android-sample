package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Purpose of this class is to test WelcomeRestartMessageProcessor
 * <p/>
 * Created by janidu on 13/5/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)

public class WelcomeRestartMessageProcessorTest {

    private final String TAG = WelcomeRestartMessageProcessorTest.class.getName();

    private static final String peerId = "1234";

    // PeerInfo
    private static PeerInfo peerInfo;
    private static final String agent = "android";
    private static final String version = "1";
    private static final boolean receiveOnly = false;
    private static final boolean enableIceTrickle = true;
    private static final boolean enableDataChannel = true;
    private static final Double weight = new Double(1.00d);

    // UserInfo
    private UserInfo userInfo;
    private static final String userData = "welcome/Restart userData.";

    // Welcome or restart
    private static final String typeWelcome = "welcome";
    private static final String typeRestart = "restart";

    private SkylinkConnection skylinkConnection;
    private SkylinkPeerService skylinkPeerService;
    private WelcomeRestartMessageProcessor welcomeRestartMessageProcessor;

    @Before
    public void setUp() throws Exception {
        // Set up peerInfo
        peerInfo = new PeerInfo();
        peerInfo.setAgent(agent);
        peerInfo.setVersion(version);
        peerInfo.setReceiveOnly(receiveOnly);
        peerInfo.setEnableIceTrickle(enableIceTrickle);
        peerInfo.setEnableDataChannel(enableDataChannel);

        // Set up userInfo
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        userInfo = new UserInfo(skylinkConfig, userData);

        // Mock objects
        skylinkConnection = mock(SkylinkConnection.class);
        skylinkPeerService = mock(SkylinkPeerService.class);
        when(skylinkConnection.getSkylinkPeerService()).thenReturn(skylinkPeerService);

        welcomeRestartMessageProcessor = new WelcomeRestartMessageProcessor();
        welcomeRestartMessageProcessor.setSkylinkConnection(skylinkConnection);
    }

    @Test
    public void testProcessWelcome() throws Exception {
        testProcessWelcomeRestart(typeWelcome);
    }

    @Test
    public void testProcessRestart() throws Exception {
        testProcessWelcomeRestart(typeRestart);
    }

    public void testProcessWelcomeRestart(String type) throws Exception {
        String logStr = "[" + TAG + "] Testing for " + type + "...";
        System.out.print(logStr);
        Log.d(TAG, logStr);

        boolean isRestart = false;
        if (typeRestart.equals(type)) {
            isRestart = true;
        }

        welcomeRestartMessageProcessor.process(getJsonObject(type));

        ArgumentCaptor<String> argPeerId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PeerInfo> argPeerInfo = ArgumentCaptor.forClass(PeerInfo.class);
        ArgumentCaptor<UserInfo> argUserInfo = ArgumentCaptor.forClass(UserInfo.class);
        ArgumentCaptor<Double> argWeight = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Boolean> argIsRestart = ArgumentCaptor.forClass(Boolean.class);

        verify(skylinkPeerService).receivedWelcomeRestart(argPeerId.capture(),
                argPeerInfo.capture(), argUserInfo.capture(), argWeight.capture(),
                argIsRestart.capture());

        // Check peerId
        String peerIdTest = argPeerId.getValue();
        assertEquals("peerId should be " + peerId + " but got " + peerIdTest + " instead.",
                peerId, peerIdTest);

        // Check peerInfo
        PeerInfo peerInfoTest = argPeerInfo.getValue();
        assertTrue("peerInfo should be " + peerInfo + " but got " + peerInfoTest + " instead.",
                peerInfo.equals(peerInfoTest));

        // Check userInfo
        UserInfo userInfoTest = argUserInfo.getValue();
        assertTrue("userInfo should be " + userInfo + " but got " + userInfoTest + " instead.",
                userInfo.equals(userInfoTest));

        // Check the rest
        double weightTest = argWeight.getValue().doubleValue();
        assertTrue("weight should be " + weight + " but got " + weightTest + " instead.",
                weight.doubleValue() == weightTest);
        boolean isRestartTest = argIsRestart.getValue();
        assertTrue("isRestart should be " + isRestart + " but got " + isRestartTest + " instead.",
                isRestart == isRestartTest);

        logStr = "[" + TAG + "] Testing done.";
        System.out.print(logStr);
        Log.d(TAG, logStr);
    }

    private JSONObject getJsonObject(String type) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", type);
        jsonObject.put("mid", peerId);
        jsonObject.put("userInfo", userInfo.getJson());
        jsonObject.put("agent", agent);
        jsonObject.put("version", version);
        jsonObject.put("receiveOnly", receiveOnly);
        jsonObject.put("enableIceTrickle", enableIceTrickle);
        jsonObject.put("enableDataChannel", enableDataChannel);
        jsonObject.put("weight", weight.doubleValue());
        return jsonObject;
    }
}