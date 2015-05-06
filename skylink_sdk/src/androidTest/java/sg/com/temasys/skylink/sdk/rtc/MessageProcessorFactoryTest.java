package sg.com.temasys.skylink.sdk.rtc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to CurrentTimeService
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class MessageProcessorFactoryTest {

    private static final String TAG = SkylinkConnectionTest.class.getName();
    private MessageProcessorFactory messageProcessorFactory;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
        messageProcessorFactory = new MessageProcessorFactory();
    }

    @Test
    public void testGetInRoomMessageProcessor() {
        MessageProcessor processor = messageProcessorFactory.getMessageProcessor("inRoom");
        assertNotNull(processor);
        assertTrue(processor instanceof InRoomMessageProcessor);
    }

    @Test
    public void testGetPingMessageProcessor() {
        MessageProcessor processor = messageProcessorFactory.getMessageProcessor("ping");
        assertNotNull(processor);
        assertTrue(processor instanceof PingMessageProcessor);
    }

    @Test
    public void testReturnsNullForInvalidMessageTypes() {
        MessageProcessor processor = messageProcessorFactory.getMessageProcessor("unsupportedMessage");
        assertNull(processor);
    }
}
