package sg.com.temasys.skylink.sdk.rtc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

import static org.junit.Assert.assertEquals;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class SkylinkConfigTest {

    private static final String TAG = SkylinkConfigTest.class.getName();

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testSetPreferredAudioCodecOpus() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setPreferredAudioCodec(SkylinkConfig.AudioCodec.OPUS);
        assertEquals(skylinkConfig.getPreferredAudioCodec(), SkylinkConfig.AudioCodec.OPUS);
    }

    @Test
    public void testSetPreferredAudioCodecISAC() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setPreferredAudioCodec(SkylinkConfig.AudioCodec.ISAC);
        assertEquals(skylinkConfig.getPreferredAudioCodec(), SkylinkConfig.AudioCodec.ISAC);
    }

    @Test
    public void testDefaultPreferredAudioCodec() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        assertEquals(skylinkConfig.getPreferredAudioCodec(), SkylinkConfig.AudioCodec.OPUS);
    }
}
