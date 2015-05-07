package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests related to CurrentTimeService
 */
@Config(emulateSdk = 18)
@RunWith(JmockitRunner.class)
public class OfferAnswerMessageProcessorTest {

    private static final String TAG = OfferAnswerMessageProcessorTest.class.getSimpleName();

    private MessageProcessor offerAnswerMessageProcessor;
    private final String targetId = "1234";
    private final String mid = "testMyId";

    // Mock using Jmockit
    @Mocked(stubOutClassInitialization = true)
    private PeerConnection mockPeerConnection;
    @Mocked
    private SkylinkConnection skylinkConnection;
    @Mocked
    private WebServerClient webServerClient;

    @Before
    public void setup() {
        offerAnswerMessageProcessor = new OfferAnswerMessageProcessor();
    }

    @Test
    public void testCreateOfferAnswerMessageProcessor() {
        MessageProcessor processor = new OfferAnswerMessageProcessor();
        assertNotNull(processor);
    }

    @Test
    public void testProcessingDifferentTarget() throws JSONException {

        SkylinkConnection mockSkylinkConnection = mock(SkylinkConnection.class);

        WebServerClient mockWebServerClient = mock(WebServerClient.class);
        when(mockWebServerClient.getCid()).thenReturn("differentCid");
        when(mockSkylinkConnection.getWebServerClient()).thenReturn(mockWebServerClient);

        offerAnswerMessageProcessor.setSkylinkConnection(mockSkylinkConnection);
        offerAnswerMessageProcessor.process(getJsonObject());

        // Should not proceed with the rest of the processing, when different target
        verify(mockSkylinkConnection, never()).getPeerConnection(mid);
    }

    @Test
    public void testProcessing() throws JSONException {
        // Use Jmockit as mocking a peerconnection is involved

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("target", mid);
        jsonObject.put("mid", mid);
        jsonObject.put("sdp", "testSDP");
        jsonObject.put("type", "answer");

        new Expectations() {{
            skylinkConnection.getWebServerClient();
            result = webServerClient;

            webServerClient.getSid();
            result = mid;

            skylinkConnection.getPeerConnection(mid);
            result = mockPeerConnection;

            skylinkConnection.getMyConfig();
            result = new SkylinkConfig();

            skylinkConnection.getSdpObserver(mid);
        }};

        OfferAnswerMessageProcessor offerAnswerMessageProcessor = new OfferAnswerMessageProcessor();
        offerAnswerMessageProcessor.setSkylinkConnection(skylinkConnection);
        offerAnswerMessageProcessor.process(jsonObject);

        new Verifications() {{
            mockPeerConnection.setRemoteDescription(
                    (org.webrtc.SdpObserver) any, (SessionDescription) any);
        }};
    }

    private JSONObject getJsonObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("target", targetId);
        jsonObject.put("mid", mid);
        return jsonObject;
    }
}
