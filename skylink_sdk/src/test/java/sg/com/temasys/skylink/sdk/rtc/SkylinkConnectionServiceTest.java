package sg.com.temasys.skylink.sdk.rtc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

/**
 * Tests related to SkylinkConnectionService
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class SkylinkConnectionServiceTest {

    private static final String TAG = SkylinkConnectionServiceTest.class.getName();

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    // TODO
    @Test
    public void testOnErrorAppServerInt() {
    }

    // TODO
    @Test
    public void testOnErrorAppServerString() {
    }

    // TODO
    @Test
    public void testOnObtainedRoomParameters() {

    }

    // TODO
    @Test
    public void testOnConnectedToRoom() {

    }

    // TODO
    @Test
    public void testIsAlreadyConnected() {

    }

    // TODO
    @Test
    public void testIsDisconnected() {

    }


    // TODO
    @Test
    public void testSendServerMessage() {

    }

    // TODO
    @Test
    public void testSendLocalUserData() {

    }

    // TODO
    @Test
    public void testDisconnect() {

    }

    // TODO
    @Test
    public void testSendMessage() {

    }

    // TODO
    @Test
    public void testSendMuteAudio() {

    }

    // TODO
    @Test
    public void testSendMuteVideo() {

    }
}
