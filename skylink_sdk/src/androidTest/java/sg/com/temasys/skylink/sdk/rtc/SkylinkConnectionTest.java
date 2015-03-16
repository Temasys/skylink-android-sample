package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests related to SkylinkConnection
 */

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class SkylinkConnectionTest {

    private static final String TAG = SkylinkConnectionTest.class.getName();

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testUnique() {
        SkylinkConnection skylinkConnection = SkylinkConnection.getInstance();
        SkylinkConnection skylinkConnection1 = skylinkConnection.getInstance();
        assertEquals(true, skylinkConnection == skylinkConnection1);
    }

    @Test
    public void testSendData() {
        DataChannelManager mockDataChannelManager = mock(DataChannelManager.class);
        SkylinkConnection skylinkConnection = SkylinkConnection.getInstance();

        skylinkConnection.setDataChannelManager(mockDataChannelManager);

        String remotePeerId = "test";
        byte[] byteArray = new byte[128];
        try {
            skylinkConnection.sendData(remotePeerId, byteArray);
            verify(mockDataChannelManager).sendDataToPeer(remotePeerId, byteArray);
        } catch (SkylinkException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Test
    public void testMaximumDataLength() throws SkylinkException {

        final String expectedMessage = "Maximum data length is " +
                DataChannelManager.MAX_TRANSFER_SIZE;

        DataChannelManager mockDataChannelManager = mock(DataChannelManager.class);
        SkylinkConnection skylinkConnection = SkylinkConnection.getInstance();
        skylinkConnection.setDataChannelManager(mockDataChannelManager);

        String remotePeerId = "test";
        byte[] byteArray = new byte[DataChannelManager.MAX_TRANSFER_SIZE + 1];

        try {
            doThrow(new SkylinkException(expectedMessage)
            ).when(mockDataChannelManager).sendDataToPeer(remotePeerId, byteArray);

            skylinkConnection.sendData(remotePeerId, byteArray);
        } catch (SkylinkException e) {
            assertNotNull(e);
            assertTrue(e.getMessage().equals(expectedMessage));
        }
    }

    @Test
    public void testVerifyRunOnUiThread() {
        SkylinkConnection skylinkConnection = SkylinkConnection.getInstance();
        SkylinkConnection mockSkylinkConnection = mock(SkylinkConnection.class);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

            }
        };
        mockSkylinkConnection.runOnUiThread(runnable);
        verify(mockSkylinkConnection).runOnUiThread(runnable);

        Runnable mockRunnable = mock(Runnable.class);
        skylinkConnection.runOnUiThread(mockRunnable);
        verify(mockRunnable).run();
    }

}
