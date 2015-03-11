package sg.com.temasys.skylink.sdk.rtc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
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
        skylinkConnection.sendData(remotePeerId, byteArray);
        verify(mockDataChannelManager).sendDataToPeer(remotePeerId, byteArray);
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
