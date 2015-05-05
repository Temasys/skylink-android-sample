package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

import static org.junit.Assert.assertEquals;

/**
 * Tests related to CandidateMessageProcessor
 * Created by janidu on 5/5/15.
 */
@Config(emulateSdk = 18)
@RunWith(JmockitRunner.class)
public class CandidateMessageProcessorTest {

    private static final String target = "1234";
    private static final String mid = "1234";

    // Mock using Jmockit
    @Mocked(stubOutClassInitialization = true)
    private PeerConnection mockPeerConnection;

    @Mocked
    private SkylinkConnection skylinkConnection;

    @Mocked
    private WebServerClient webServerClient;

    @Test
    public void testProcessing() throws JSONException {
        // Use Jmockit as mocking a peerconnection is involved

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("target", mid);
        jsonObject.put("mid", mid);
        jsonObject.put("id", "testId");
        jsonObject.put("label", "1234");
        jsonObject.put("candidate", "testCandidate");

        new Expectations() {{
            skylinkConnection.getWebServerClient();
            result = webServerClient;

            webServerClient.getSid();
            result = mid;

            skylinkConnection.getPeerConnection(mid);
            result = mockPeerConnection;

        }};

        CandidateMessageProcessor candidateMessageProcessor = new CandidateMessageProcessor();
        candidateMessageProcessor.setSkylinkConnection(skylinkConnection);
        candidateMessageProcessor.process(jsonObject);

        new Verifications() {{
            IceCandidate iceCandidate;
            mockPeerConnection.addIceCandidate(iceCandidate = withCapture());
            assertEquals(iceCandidate.sdpMid, "testId");
            assertEquals(iceCandidate.sdpMLineIndex, 1234);
            assertEquals(iceCandidate.sdp, "testCandidate");
        }};
    }


}
