package sg.com.temasys.skylink.sdk.rtc;

import android.graphics.Point;
import android.opengl.GLSurfaceView;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.MediaListener;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests related to MuteVideoMessageProcessor
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class MuteVideoMessageProcessorTest {

    private static final String MID = "1234";
    private static final boolean MUTED = true;

    private MuteVideoMessageProcessor muteVideoMessageProcessor;
    private SkylinkConnection skylinkConnection;
    private SkylinkConnectionService skylinkConnectionService;

    @Before
    public void setup() {
        muteVideoMessageProcessor = new MuteVideoMessageProcessor();
    }

    @Test
    public void testProcess() throws JSONException {

        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mid", MID);
        jsonObject.put("muted", String.valueOf(MUTED));

        skylinkConnection = spy(SkylinkConnection.getInstance());
        skylinkConnectionService = mock(SkylinkConnectionService.class);

        doReturn(skylinkConfig).when(skylinkConnection).getMyConfig();
        SkylinkPeerService.isPeerIdMCU(MID);
        doReturn(skylinkConnectionService)
                .when(skylinkConnection).getSkylinkConnectionService();
        doReturn(SkylinkConnectionService.ConnectionState.CONNECTING)
                .when(skylinkConnectionService).getConnectionState();

        MediaListener mediaListener = new MediaListener() {
            @Override
            public void onLocalMediaCapture(GLSurfaceView videoView) {

            }

            @Override
            public void onVideoSizeChange(String peerId, Point size) {

            }

            @Override
            public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {

            }

            @Override
            public void onRemotePeerVideoToggle(String remotePeerId, boolean isMuted) {
                assertEquals(MID, remotePeerId);
                assertEquals(MUTED, isMuted);
            }

            @Override
            public void onRemotePeerMediaReceive(String remotePeerId, GLSurfaceView videoView) {

            }
        };

        doReturn(mediaListener).when(skylinkConnection).getMediaListener();
        muteVideoMessageProcessor.setSkylinkConnection(skylinkConnection);
        muteVideoMessageProcessor.process(jsonObject);
    }
}
