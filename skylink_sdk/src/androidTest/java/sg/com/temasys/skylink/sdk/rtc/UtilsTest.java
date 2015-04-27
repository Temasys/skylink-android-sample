package sg.com.temasys.skylink.sdk.rtc;

import android.text.TextUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SignatureException;
import java.util.Date;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to Utils
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class UtilsTest {

    private final String TAG = UtilsTest.class.getName();

    @Test
    public void testPreferAudioCodecOpus() {
        String sdpDescription = addRTPMap("m=audio 9 RTP/SAVPF 104 103 111 9 0 8 106 105 13 126");
        assertEquals(addRTPMap("m=audio 9 RTP/SAVPF 111 104 103 9 0 8 106 105 13 126"),
                Utils.preferCodec(sdpDescription, SkylinkConfig.AudioCodec.OPUS.toString(), true));
    }

    @Test
    public void testPreferAudioCodecISAC() {
        String sdpDescription = "m=audio 9 RTP/SAVPF 111 103 104 9 0 8 106 105 13 126";
        String rtpMap = addRTPMap(sdpDescription);
        assertEquals(addRTPMap("m=audio 9 RTP/SAVPF 103 111 104 9 0 8 106 105 13 126"),
                Utils.preferCodec(rtpMap, SkylinkConfig.AudioCodec.ISAC.toString(), true));
    }

    private String addRTPMap(String sdpDescription) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(sdpDescription + "\r\n");
        stringBuilder.append("a=rtpmap:111 opus/48000/2" + "\r\n");
        stringBuilder.append("a=fmtp:111 minptime=10; useinbandfec=1;stereo=1;max-fr=50;max-recv-width=640;max-recv-height=480" + "\r\n");
        stringBuilder.append("a=rtpmap:103 ISAC/16000" + "\r\n");
        stringBuilder.append("a=rtpmap:9 G722/8000" + "\r\n");
        stringBuilder.append("a=rtpmap:0 PCMU/8000" + "\r\n");
        stringBuilder.append("a=rtpmap:106 CN/32000" + "\r\n");
        stringBuilder.append("a=rtpmap:105 CN/16000" + "\r\n");
        stringBuilder.append("a=rtpmap:13 CN/8000" + "\r\n");
        stringBuilder.append("a=rtpmap:126 telephone-event/8000" + "\r\n");
        return stringBuilder.toString();
    }

    @Test
    public void testCalculateRFC2104HMAC() throws SignatureException {

        String shouldBeResult = "6XQ62fspwYw4mWsWv6weBEuDuaM=";
        String roomName = "test";
        String duration = "200.0";
        String date = "2015-01-15T18:30:00.0Z";
        String secret = "4xcrx4w32zm8i";
        assertNotNull("Should calculate", Utils.calculateRFC2104HMAC(roomName + "_" + duration + "_"
                + date, secret));
    }

    @Test
    public void testGetSkylinkConnectionString() {

        String roomName = "testRoom";
        String apiKey = "TestKey";
        String secret = "TestSecret";
        int duration = SkylinkConnection.DEFAULT_DURATION;

        String skylinkConnectionString = Utils.getSkylinkConnectionString(roomName,
                apiKey, secret, new Date(), duration);

        assertNotNull(skylinkConnectionString);
        assertFalse("Skylink Connection should not be empty",
                TextUtils.isEmpty(skylinkConnectionString));
    }

    @Test
    public void testGetISOTimeStamp() {
        String isoTimeStamp = Utils.getISOTimeStamp(new Date());
        assertNotNull(isoTimeStamp);
    }

    @Test
    public void testDrainStream() {
        String str = "This is a String ~ GoGoGo";
        // convert String into InputStream
        InputStream inputStream = new ByteArrayInputStream(str.getBytes());
        try {
            assertTrue("Should be equal", str.equals(Utils.convertInputStreamToString(inputStream)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSdpSegmentFindsAudioWhenAudioPresent() {
        String strBefore =
                "a=mid:audioo\r\n" +
                        "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                        "a=sendonly\r\n";
        String strAudio =
                "a=mid:audio\r\n" +
                        "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
                        "a=sendonly\r\n" +
                        "a=rtcp-mux\r\n" +
                        "a=rtpmap:111 opus/48000/2\r\n" +
                        "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideo =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
                        "a=sendonly\r\n" +
                        "a=rtcp-mux\r\n" +
                        "a=rtpmap:100 VP8/90000\r\n";
        String strAfter =
                "a=mid:videoo\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
                        "a=sendonly\r\n";

        String sdpTest = strBefore + strAudio + strVideo + strAfter;

        // Perform method
        String[] sdpSegments = Utils.sdpSegment(sdpTest, "audio");

        // Check results
        // Before segment is correct.
        assertTrue(strBefore.equals(sdpSegments[0]));
        // Found segment is correct.
        assertTrue(strAudio.equals(sdpSegments[1]));
        // After segment is correct.
        assertTrue((strVideo + strAfter).equals(sdpSegments[2]));
    }

    @Test
    public void testSdpSegmentFindsNoAudioWhenAudioNotPresent() {
        String strBefore =
                "a=mid:audiooo\r\n" +
                        "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                        "a=sendonly\r\n";
        String strAudioo =
                "a=mid:audioo\r\n" +
                        "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
                        "a=sendonly\r\n" +
                        "a=rtcp-mux\r\n" +
                        "a=rtpmap:111 opus/48000/2\r\n" +
                        "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideo =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
                        "a=sendonly\r\n" +
                        "a=rtcp-mux\r\n" +
                        "a=rtpmap:100 VP8/90000\r\n";
        String strAfter =
                "a=mid:videooo\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
                        "a=sendonly\r\n";

        String sdpTest = strBefore + strAudioo + strVideo + strAfter;

        // Perform method
        String[] sdpSegments = Utils.sdpSegment(sdpTest, "audio");

        // Check results
        // Before segment is correct.
        assertTrue(sdpTest.equals(sdpSegments[0]));
        // Found segment is correct.
        assertTrue("".equals(sdpSegments[1]));
        // After segment is correct.
        assertTrue(("").equals(sdpSegments[2]));
    }

    @Test
    public void testSdpSegmentFindsVideoWhenVideoPresent() {
        String strBefore =
                "a=mid:audioo\r\n" +
                        "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                        "a=sendonly\r\n";
        String strAudio =
                "a=mid:audio\r\n" +
                        "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
                        "a=sendonly\r\n" +
                        "a=rtcp-mux\r\n" +
                        "a=rtpmap:111 opus/48000/2\r\n" +
                        "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideo =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
                        "a=sendonly\r\n" +
                        "a=rtcp-mux\r\n" +
                        "a=rtpmap:100 VP8/90000\r\n";
        String strAfter =
                "a=mid:videoo\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
                        "a=sendonly\r\n";

        String sdpTest = strBefore + strAudio + strVideo + strAfter;

        // Perform method
        String[] sdpSegments = Utils.sdpSegment(sdpTest, "video");

        // Check results
        // Before segment is correct.
        assertTrue((strBefore + strAudio).equals(sdpSegments[0]));
        // Found segment is correct.
        assertTrue(strVideo.equals(sdpSegments[1]));
        // After segment is correct.
        assertTrue(strAfter.equals(sdpSegments[2]));
    }

    @Test
    public void testSdpSegmentFindsNoVideoWhenVideoNotPresent() {
        String strBefore =
                "a=mid:audiooo\r\n" +
                        "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                        "a=sendonly\r\n";
        String strAudio =
                "a=mid:audio\r\n" +
                        "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
                        "a=sendonly\r\n" +
                        "a=rtcp-mux\r\n" +
                        "a=rtpmap:111 opus/48000/2\r\n" +
                        "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoo =
                "a=mid:videoo\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
                        "a=sendonly\r\n" +
                        "a=rtcp-mux\r\n" +
                        "a=rtpmap:100 VP8/90000\r\n";
        String strAfter =
                "a=mid:videooo\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
                        "a=sendonly\r\n";

        String sdpTest = strBefore + strAudio + strVideoo + strAfter;

        // Perform method
        String[] sdpSegments = Utils.sdpSegment(sdpTest, "video");

        // Check results
        // Before segment is correct.
        assertTrue(sdpTest.equals(sdpSegments[0]));
        // Found segment is correct.
        assertTrue("".equals(sdpSegments[1]));
        // After segment is correct.
        assertTrue(("").equals(sdpSegments[2]));
    }

    @Test
    public void testWillNotAddStereoIfNotOpus() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("a=rtpmap:111 opus/48000/2" + "\r\n");
        stringBuilder.append("a=fmtp:111 minptime=10; useinbandfec=1" + "\r\n");

        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setStereoAudio(true);
        skylinkConfig.setPreferredAudioCodec(SkylinkConfig.AudioCodec.ISAC);

        String modifiedSdp = Utils.modifyStereoAudio(stringBuilder.toString(), skylinkConfig);
        String[] lines = modifiedSdp.split("\r\n");
        assertFalse(lines[1].contains("stereo=1"));
    }

    @Test
    public void testWillNotAddStereoIfSdpHasNoOpus() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("a=rtpmap:13 CN/8000" + "\r\n");

        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setStereoAudio(true);
        skylinkConfig.setPreferredAudioCodec(SkylinkConfig.AudioCodec.OPUS);

        String modifiedSdp = Utils.modifyStereoAudio(stringBuilder.toString(), skylinkConfig);
        String[] lines = modifiedSdp.split("\r\n");
        assertFalse(lines[0].contains("stereo=1"));
    }

    @Test
    public void testAddStereo() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("a=rtpmap:111 opus/48000/2" + "\r\n");
        stringBuilder.append("a=fmtp:111 minptime=10; useinbandfec=1" + "\r\n");

        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setStereoAudio(true);

        String modifiedSdp = Utils.modifyStereoAudio(stringBuilder.toString(), skylinkConfig);
        String[] lines = modifiedSdp.split("\r\n");

        assertTrue(lines[1].equals("a=fmtp:111 minptime=10; useinbandfec=1;stereo=1"));
    }

    @Test
    public void testWillNotAddStereoIfExist() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("a=rtpmap:111 opus/48000/2" + "\r\n");
        stringBuilder.append("a=fmtp:111 minptime=10; stereo=1; useinbandfec=1" + "\r\n");

        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setStereoAudio(true);

        String modifiedSdp = Utils.modifyStereoAudio(stringBuilder.toString(), skylinkConfig);
        String[] lines = modifiedSdp.split("\r\n");

        assertTrue(lines[1].equals("a=fmtp:111 minptime=10; stereo=1; useinbandfec=1"));
    }

    @Test
    public void testWillNotAddStereoIfAtEnd() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("a=rtpmap:111 opus/48000/2" + "\r\n");
        stringBuilder.append("a=fmtp:111 minptime=10; useinbandfec=1; stereo=1;" + "\r\n");

        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setStereoAudio(true);

        String modifiedSdp = Utils.modifyStereoAudio(stringBuilder.toString(), skylinkConfig);
        String[] lines = modifiedSdp.split("\r\n");

        assertTrue(lines[1].equals("a=fmtp:111 minptime=10; useinbandfec=1; stereo=1;"));
    }

    @Test
    public void testRemoveStereo() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("a=rtpmap:111 opus/48000/2" + "\r\n");
        stringBuilder.append("a=fmtp:111 minptime=10; stereo=1; useinbandfec=1;" + "\r\n");

        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setStereoAudio(false);

        String modifiedSdp = Utils.modifyStereoAudio(stringBuilder.toString(), skylinkConfig);
        String[] lines = modifiedSdp.split("\r\n");
        assertFalse(lines[1].contains("stereo=1"));
    }

    @Test
    public void testRemoveStereoIfAtEnd() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("a=rtpmap:111 opus/48000/2" + "\r\n");
        stringBuilder.append("a=fmtp:111 minptime=10; useinbandfec=1; stereo=1" + "\r\n");

        SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setStereoAudio(false);

        String modifiedSdp = Utils.modifyStereoAudio(stringBuilder.toString(), skylinkConfig);
        String[] lines = modifiedSdp.split("\r\n");
        assertFalse(lines[1].contains("stereo=1"));
    }
}
