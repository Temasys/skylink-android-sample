package sg.com.temasys.skylink.sdk.rtc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

/**
 * Tests related to AppServerClient
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class AppServerClientTest {

    private static final String TAG = AppServerClientTest.class.getName();

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    // TODO
    @Test
    public void testConnectToRoom() {
    }

    // TODO
    @Test
    public void testOnRoomParameterSuccessful() {

    }

    // TODO
    @Test
    public void testOnRoomParameterErrorInt() {

    }

    // TODO
    @Test
    public void testOnRoomParameterErrorString() {

    }
}
