package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests related to SignalingMessageProcessingService
 * <p/>
 * Created by janidu on 7/5/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class SignalingMessageProcessingServiceTest {

    private static final String TARGET = "1234";
    private static final String SOCKET_ID = "12345610";
    private static final String INROOM = "inRoom";

    @Before
    public void setup() {

    }

    @Test
    public void testOnMessageDifferentTarget() throws JSONException {

        MessageProcessorFactory messageProcessorFactory = mock(MessageProcessorFactory.class);
        SkylinkConnection skylinkConnection = mock(SkylinkConnection.class);
        SkylinkConnectionService skylinkConnectionService = mock(SkylinkConnectionService.class);
        InRoomMessageProcessor byeMessageProcessor = mock(InRoomMessageProcessor.class);

        when(messageProcessorFactory.getMessageProcessor(INROOM)).thenReturn(byeMessageProcessor);

        when(skylinkConnectionService.getSid()).thenReturn(SOCKET_ID);
        when(skylinkConnection.getLockDisconnectMsg()).thenReturn(new Object());
        when(skylinkConnection.getSkylinkConnectionService()).thenReturn(skylinkConnectionService);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("target", TARGET);
        jsonObject.put("type", INROOM);

        SignalingMessageProcessingService service = new
                SignalingMessageProcessingService(skylinkConnection, skylinkConnectionService, messageProcessorFactory);
        service.onMessage(jsonObject.toString());
        // Should not process with a different target
        verify(byeMessageProcessor, never()).process(any(JSONObject.class));
    }
}
