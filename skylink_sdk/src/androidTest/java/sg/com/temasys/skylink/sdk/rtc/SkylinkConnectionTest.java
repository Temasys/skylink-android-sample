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
    public void testVideoWidthDefault() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        assertEquals(skylinkConfig.getVideoWidth(), SkylinkConfig.MAX_VIDEO_WIDTH);
    }

    @Test
    public void testVideoHeightDefault() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        assertEquals(skylinkConfig.getVideoHeight(), SkylinkConfig.MAX_VIDEO_HEIGHT);
    }

    @Test
    public void testVideoHeight() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setVideoHeight(200);

        SkylinkConfig newConfig = new SkylinkConfig(skylinkConfig);
        assertEquals(newConfig.getVideoHeight(), 200);
    }

    @Test
    public void testVideoHeightMax() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setVideoHeight(20000);

        SkylinkConfig newConfig = new SkylinkConfig(skylinkConfig);
        assertEquals(newConfig.getVideoHeight(), SkylinkConfig.MAX_VIDEO_HEIGHT);
    }

    @Test
    public void testVideoWidth() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setVideoWidth(200);

        SkylinkConfig newConfig = new SkylinkConfig(skylinkConfig);
        assertEquals(newConfig.getVideoWidth(), 200);
    }

    @Test
    public void testVideoWidthMax() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setVideoWidth(20000);

        SkylinkConfig newConfig = new SkylinkConfig(skylinkConfig);
        assertEquals(newConfig.getVideoWidth(), SkylinkConfig.MAX_VIDEO_WIDTH);
    }

    @Test
    public void testAudioStereoDefault() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        assertTrue(skylinkConfig.isStereoAudio());
    }

    @Test
    public void testAudioStereoTrue() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setStereoAudio(true);
        assertTrue(skylinkConfig.isStereoAudio());
    }

    @Test
    public void testVideoFPSDefault() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        assertEquals(skylinkConfig.getVideoFps(), SkylinkConfig.MAX_VIDEO_FPS);
    }

    @Test
    public void testVideoFPSMax() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setVideoFps(4000);
        assertEquals(new SkylinkConfig(skylinkConfig).getVideoFps(), SkylinkConfig.MAX_VIDEO_FPS);
    }

    @Test
    public void testUnique() {
        SkylinkConnection skylinkConnection = SkylinkConnection.getInstance();
        SkylinkConnection skylinkConnection1 = skylinkConnection.getInstance();
        assertEquals(true, skylinkConnection == skylinkConnection1);
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
