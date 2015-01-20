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
 * Created by janidu on 20/1/15.
 */

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class SkyLinkConnectionTest {

    private static final String TAG = SkyLinkConnectionTest.class.getName();

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testUnique() {
        SkyLinkConnection skyLinkConnection = SkyLinkConnection.getInstance();
        SkyLinkConnection skyLinkConnection1 = skyLinkConnection.getInstance();
        assertEquals(true, skyLinkConnection == skyLinkConnection1);
    }

    @Test
    public void testVerifyRunOnUiThread() {
        SkyLinkConnection skyLinkConnection = SkyLinkConnection.getInstance();
        SkyLinkConnection mockSkyLinkConnection = mock(SkyLinkConnection.class);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

            }
        };
        mockSkyLinkConnection.runOnUiThread(runnable);
        verify(mockSkyLinkConnection).runOnUiThread(runnable);

        Runnable mockRunnable = mock(Runnable.class);
        skyLinkConnection.runOnUiThread(mockRunnable);
        verify(mockRunnable).run();
    }

}
