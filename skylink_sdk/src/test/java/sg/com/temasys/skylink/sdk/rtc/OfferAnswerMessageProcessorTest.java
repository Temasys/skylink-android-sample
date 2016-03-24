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
 * Tests related to CurrentTimeService
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class OfferAnswerMessageProcessorTest {

    private static final String TAG = OfferAnswerMessageProcessorTest.class.getName();
    private static final String targetId = "1234";
    private static final String mid = "testMyId";
    private static final String sdp = "testSDP";
    private static final String typeAnswer = "answer";
    private static final String typeOffer = "offer";


    private MessageProcessor offerAnswerMessageProcessor;
    private SkylinkConnection skylinkConnection;

    @Before
    public void setup() {
        offerAnswerMessageProcessor = new OfferAnswerMessageProcessor();
        skylinkConnection = mock(SkylinkConnection.class);
    }

    @Test
    public void testCreateOfferAnswerMessageProcessor() {
        assertNotNull(offerAnswerMessageProcessor);
    }

    @Test
    public void testProcessingAnswer() throws JSONException {

        SkylinkPeerService skylinkPeerService = mock(SkylinkPeerService.class);
        when(skylinkConnection.getSkylinkPeerService()).thenReturn(skylinkPeerService);

        offerAnswerMessageProcessor.setSkylinkConnection(skylinkConnection);
        offerAnswerMessageProcessor.process(getJsonObject(typeAnswer));
        verify(skylinkPeerService).receivedOfferAnswer(mid, sdp, typeAnswer);
    }

    @Test
    public void testProcessingOffer() throws JSONException {

        SkylinkPeerService skylinkPeerService = mock(SkylinkPeerService.class);
        when(skylinkConnection.getSkylinkPeerService()).thenReturn(skylinkPeerService);

        offerAnswerMessageProcessor.setSkylinkConnection(skylinkConnection);
        offerAnswerMessageProcessor.process(getJsonObject(typeOffer));
        verify(skylinkPeerService).receivedOfferAnswer(mid, sdp, typeOffer);
    }

    private JSONObject getJsonObject(String type) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("target", targetId);
        jsonObject.put("mid", mid);
        jsonObject.put("sdp", sdp);
        jsonObject.put("type", type);
        return jsonObject;
    }
}
