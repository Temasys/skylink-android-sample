package sg.com.temasys.skylink.sdk.rtc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class SkylinkConfigTest {

    private static final String TAG = SkylinkConfigTest.class.getName();

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testSetAudioVideoSendConfigNoAudioNoVideo() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        assertFalse(skylinkConfig.hasAudioSend());
        assertFalse(skylinkConfig.hasVideoSend());
    }

    @Test
    public void testSetAudioVideoSendConfigAudioOnly() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        assertTrue(skylinkConfig.hasAudioSend());
        assertFalse(skylinkConfig.hasVideoSend());
    }

    @Test
    public void testSetAudioVideoSendConfigVideoOnly() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.VIDEO_ONLY);
        assertFalse(skylinkConfig.hasAudioSend());
        assertTrue(skylinkConfig.hasVideoSend());
    }

    @Test
    public void testSetAudioVideoSendConfigAudioAndVideo() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        assertTrue(skylinkConfig.hasAudioSend());
        assertTrue(skylinkConfig.hasVideoSend());
    }

    @Test
    public void testSetAudioVideoReceiveConfigNoAudioNoVideo() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        assertFalse(skylinkConfig.hasAudioReceive());
        assertFalse(skylinkConfig.hasVideoReceive());
    }

    @Test
    public void testSetAudioVideoReceiveConfigAudioOnly() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        assertTrue(skylinkConfig.hasAudioReceive());
        assertFalse(skylinkConfig.hasVideoReceive());
    }

    @Test
    public void testSetAudioVideoReceiveConfigVideoOnly() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.VIDEO_ONLY);
        assertFalse(skylinkConfig.hasAudioReceive());
        assertTrue(skylinkConfig.hasVideoReceive());
    }

    @Test
    public void testSetAudioVideoReceiveConfigAudioAndVideo() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        assertTrue(skylinkConfig.hasAudioReceive());
        assertTrue(skylinkConfig.hasVideoReceive());
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
