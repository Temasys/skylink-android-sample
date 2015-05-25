package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests related to GroupMessageProcessor
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class GroupMessageProcessorTest {

    private static final String MESSAGE_ONE = "message1";
    private static final String MESSAGE_TWO = "message2";
    private static final String MESSAGE_THREE = "message3";
    private static final String MESSAGE_FOUR = "message4";

    private SkylinkConnection skylinkConnection;
    private SkylinkConnectionService skylinkConnectionService;
    private SignalingMessageProcessingService signalingMessageProcessingService;
    private GroupMessageProcessor groupMessageProcessor;

    @Before
    public void setUp() throws Exception {

        skylinkConnection = mock(SkylinkConnection.class);
        skylinkConnectionService = mock(SkylinkConnectionService.class);
        signalingMessageProcessingService = mock(SignalingMessageProcessingService.class);

        when(skylinkConnection.getSkylinkConnectionService()).thenReturn(skylinkConnectionService);
        when(skylinkConnectionService.getSignalingMessageProcessingService())
                .thenReturn(signalingMessageProcessingService);

        groupMessageProcessor = new GroupMessageProcessor();
        groupMessageProcessor.setSkylinkConnection(skylinkConnection);
    }

    @Test
    public void testProcess() throws Exception {

        JSONArray jsonArray = new JSONArray();
        jsonArray.put(MESSAGE_ONE);
        jsonArray.put(MESSAGE_TWO);
        jsonArray.put(MESSAGE_THREE);
        jsonArray.put(MESSAGE_FOUR);

        JSONObject pcObject = new JSONObject();
        pcObject.put("lists", jsonArray);

        groupMessageProcessor.process(pcObject);

        verify(signalingMessageProcessingService).onMessage(MESSAGE_ONE);
        verify(signalingMessageProcessingService).onMessage(MESSAGE_TWO);
        verify(signalingMessageProcessingService).onMessage(MESSAGE_THREE);
        verify(signalingMessageProcessingService).onMessage(MESSAGE_FOUR);
    }
}