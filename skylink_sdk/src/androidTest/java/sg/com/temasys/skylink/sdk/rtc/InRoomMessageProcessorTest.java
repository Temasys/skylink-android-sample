package sg.com.temasys.skylink.sdk.rtc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;

/**
 * Tests related to InRoomMessageProcessor
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class InRoomMessageProcessorTest {

    private static final String TAG = InRoomMessageProcessorTest.class.getSimpleName();

    @Test
    public void testCreateInRoomMessageProcessor() {
        MessageProcessor processor = new InRoomMessageProcessor();
        assertNotNull(processor);
    }
}
