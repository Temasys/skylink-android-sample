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
    public void testCanBeCreated() {
        assertNotNull(new MessageProcessorFactory());
    }

    @Test
    public void testGetInRoomMessageProcessor() {
        MessageProcessor processor = messageProcessorFactory.getMessageProcessor("inRoom");
        assertNotNull(processor);
        assertTrue(processor instanceof InRoomMessageProcessor);
    }

    @Test
    public void testGetByeMessageProcessor() {
        MessageProcessor processor = messageProcessorFactory.getMessageProcessor("bye");
        assertNotNull(processor);
        assertTrue(processor instanceof ByeMessageProcessor);
    }

    @Test
    public void testGetPingMessageProcessor() {
        MessageProcessor processor = messageProcessorFactory.getMessageProcessor("ping");
        assertNotNull(processor);
        assertTrue(processor instanceof PingMessageProcessor);
    }

    @Test
    public void testGetOfferAnswerMessageProcessor() {
        MessageProcessor processor = messageProcessorFactory.getMessageProcessor("offer");
        assertNotNull(processor);
        assertTrue(processor instanceof OfferAnswerMessageProcessor);

        processor = messageProcessorFactory.getMessageProcessor("answer");
        assertNotNull(processor);
        assertTrue(processor instanceof OfferAnswerMessageProcessor);
    }

    @Test
    public void testGetEnterMessageProcessor() {
        MessageProcessor processor = messageProcessorFactory.getMessageProcessor("enter");
        assertNotNull(processor);
        assertTrue(processor instanceof EnterMessageProcessor);
    }

    @Test
    public void testReturnsNullForInvalidMessageTypes() {
        MessageProcessor processor = messageProcessorFactory.getMessageProcessor("unsupportedMessage");
        assertNull(processor);
    }

    @Test
    public void testMuteVideoMessageProcessor() {
        MessageProcessor processor = messageProcessorFactory.getMessageProcessor("muteVideoEvent");
        assertNotNull(processor);
        assertTrue(processor instanceof MuteVideoMessageProcessor);
    }

    @Test
    public void testGetMuteAudioMessageProcessor(){
        MessageProcessor processor = messageProcessorFactory.getMessageProcessor("muteAudioEvent");
        assertTrue(processor instanceof MuteAudioMessageProcessor);
    }
}
