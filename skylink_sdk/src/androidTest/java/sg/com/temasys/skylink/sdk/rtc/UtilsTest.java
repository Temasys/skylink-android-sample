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
    public void testSdpAudioSendOnlyWhenAudioSendRecv() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String strAudioA = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strAudioB = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoA =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strVideoB = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strSendRecv +
                strAudioA + strSendRecv + strAudioB +
                strVideoA + strSendRecv + strVideoB +
                str4 + strSendRecv;

        // Expected string
        String sdpExpected = str1 + strSendRecv +
                strAudioA + strSend + strAudioB +
                strVideoA + strSendRecv + strVideoB +
                str4 + strSendRecv;

        // Perform method
        String results = Utils.sdpAudioSendOnly(sdpTest);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpAudioSendOnlyWhenAudioRecvOnly() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String strAudioA = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strAudioB = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoA =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strVideoB = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strRecv +
                strAudioA + strRecv + strAudioB +
                strVideoA + strRecv + strVideoB +
                str4 + strRecv;

        // Expected string
        String sdpExpected = str1 + strRecv +
                strAudioA + strSend + strAudioB +
                strVideoA + strRecv + strVideoB +
                str4 + strRecv;

        // Perform method
        String results = Utils.sdpAudioSendOnly(sdpTest);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpAudioSendOnlyWhenAudioSendOnly() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String strAudioA = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strAudioB = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoA =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strVideoB = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strSend +
                strAudioA + strSend + strAudioB +
                strVideoA + strSend + strVideoB +
                str4 + strSend;

        // Expected string
        String sdpExpected = str1 + strSend +
                strAudioA + strSend + strAudioB +
                strVideoA + strSend + strVideoB +
                str4 + strSend;

        // Perform method
        String results = Utils.sdpAudioSendOnly(sdpTest);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpAudioReceiveOnlyWhenAudioSendRecv() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String strAudioA = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strAudioB = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoA =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strVideoB = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strSendRecv +
                strAudioA + strSendRecv + strAudioB +
                strVideoA + strSendRecv + strVideoB +
                str4 + strSendRecv;

        // Expected string
        String sdpExpected = str1 + strSendRecv +
                strAudioA + strRecv + strAudioB +
                strVideoA + strSendRecv + strVideoB +
                str4 + strSendRecv;

        // Perform method
        String results = Utils.sdpAudioReceiveOnly(sdpTest);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpAudioReceiveOnlyWhenAudioRecvOnly() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String strAudioA = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strAudioB = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoA =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strVideoB = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strRecv +
                strAudioA + strRecv + strAudioB +
                strVideoA + strRecv + strVideoB +
                str4 + strRecv;

        // Expected string
        String sdpExpected = str1 + strRecv +
                strAudioA + strRecv + strAudioB +
                strVideoA + strRecv + strVideoB +
                str4 + strRecv;

        // Perform method
        String results = Utils.sdpAudioReceiveOnly(sdpTest);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpAudioReceiveOnlyWhenAudioSendOnly() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String strAudioA = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strAudioB = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoA =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strVideoB = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strSend +
                strAudioA + strSend + strAudioB +
                strVideoA + strSend + strVideoB +
                str4 + strSend;

        // Expected string
        String sdpExpected = str1 + strSend +
                strAudioA + strRecv + strAudioB +
                strVideoA + strSend + strVideoB +
                str4 + strSend;

        // Perform method
        String results = Utils.sdpAudioReceiveOnly(sdpTest);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpVideoSendOnlyWhenVideoSendRecv() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String strAudioA = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strAudioB = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoA =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strVideoB = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strSendRecv +
                strAudioA + strSendRecv + strAudioB +
                strVideoA + strSendRecv + strVideoB +
                str4 + strSendRecv;

        // Expected string
        String sdpExpected = str1 + strSendRecv +
                strAudioA + strSendRecv + strAudioB +
                strVideoA + strSend + strVideoB +
                str4 + strSendRecv;

        // Perform method
        String results = Utils.sdpVideoSendOnly(sdpTest);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpVideoSendOnlyWhenVideoRecvOnly() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String strAudioA = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strAudioB = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoA =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strVideoB = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strRecv +
                strAudioA + strRecv + strAudioB +
                strVideoA + strRecv + strVideoB +
                str4 + strRecv;

        // Expected string
        String sdpExpected = str1 + strRecv +
                strAudioA + strRecv + strAudioB +
                strVideoA + strSend + strVideoB +
                str4 + strRecv;

        // Perform method
        String results = Utils.sdpVideoSendOnly(sdpTest);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpVideoSendOnlyWhenVideoSendOnly() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String strAudioA = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strAudioB = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoA =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strVideoB = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strSend +
                strAudioA + strSend + strAudioB +
                strVideoA + strSend + strVideoB +
                str4 + strSend;

        // Expected string
        String sdpExpected = str1 + strSend +
                strAudioA + strSend + strAudioB +
                strVideoA + strSend + strVideoB +
                str4 + strSend;

        // Perform method
        String results = Utils.sdpVideoSendOnly(sdpTest);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpVideoReceiveOnlyWhenVideoSendRecv() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String strAudioA = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strAudioB = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoA =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strVideoB = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strSendRecv +
                strAudioA + strSendRecv + strAudioB +
                strVideoA + strSendRecv + strVideoB +
                str4 + strSendRecv;

        // Expected string
        String sdpExpected = str1 + strSendRecv +
                strAudioA + strSendRecv + strAudioB +
                strVideoA + strRecv + strVideoB +
                str4 + strSendRecv;

        // Perform method
        String results = Utils.sdpVideoReceiveOnly(sdpTest);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpVideoReceiveOnlyWhenVideoRecvOnly() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String strAudioA = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strAudioB = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoA =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strVideoB = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strRecv +
                strAudioA + strRecv + strAudioB +
                strVideoA + strRecv + strVideoB +
                str4 + strRecv;

        // Expected string
        String sdpExpected = str1 + strRecv +
                strAudioA + strRecv + strAudioB +
                strVideoA + strRecv + strVideoB +
                str4 + strRecv;

        // Perform method
        String results = Utils.sdpVideoReceiveOnly(sdpTest);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpVideoReceiveOnlyWhenVideoSendOnly() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String strAudioA = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strAudioB = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String strVideoA =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String strVideoB = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strSend +
                strAudioA + strSend + strAudioB +
                strVideoA + strSend + strVideoB +
                str4 + strSend;

        // Expected string
        String sdpExpected = str1 + strSend +
                strAudioA + strSend + strAudioB +
                strVideoA + strRecv + strVideoB +
                str4 + strSend;

        // Perform method
        String results = Utils.sdpVideoReceiveOnly(sdpTest);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpReplaceReplacesWhenTargetFoundForFirstOccurrence() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String regex = "^a=(" + strSR + "|" + strS + "|" + strR + ")$";
        String newLine = "a=" + strS;

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String str2a = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String str2b = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String str3a =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String str3b = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strSend +
                str2a + strSendRecv + str2b +
                str3a + strSendRecv + str3b +
                str4 + strRecv;

        // Expected string
        String sdpExpected = str1 + newLine + "\r\n" +
                str2a + strSendRecv + str2b +
                str3a + strSendRecv + str3b +
                str4 + strRecv;

        // Perform method
        String results = Utils.sdpReplace(sdpTest, regex, newLine, true);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpReplaceReplacesWhenTargetFoundForAllOccurrence() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String regex = "^a=(" + strSR + "|" + strS + "|" + strR + ")$";
        String newLine = "a=" + strS;

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String str2a = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String str2b = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String str3a =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String str3b = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strSend +
                str2a + strSendRecv + str2b +
                str3a + strSendRecv + str3b +
                str4 + strRecv;

        // Expected string
        String sdpExpected = str1 + newLine + "\r\n" +
                str2a + newLine + "\r\n" + str2b +
                str3a + newLine + "\r\n" + str3b +
                str4 + newLine + "\r\n";

        // Perform method
        String results = Utils.sdpReplace(sdpTest, regex, newLine, false);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
    }

    @Test
    public void testSdpReplaceNotReplaceWhenTargetNotFoundForFirstOccurrence() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        String regex = "^a=(" + strSR + "v|" + strS + "y|" + strR + "y)$";

        String newLine = "a=" + strS;

        String str1 = "a=mid:audioo\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n";
        String str2a = "a=mid:audio\r\n" +
                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String str2b = "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=fmtp:111 minptime=10; useinbandfec=1\r\n";
        String str3a =
                "a=mid:video\r\n" +
                        "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                        "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";
        String str3b = "a=rtcp-mux\r\n" +
                "a=rtpmap:100 VP8/90000\r\n";
        String str4 = "a=mid:videoo\r\n" +
                "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
                "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n";

        // Test string
        String sdpTest = str1 + strSend +
                str2a + strSendRecv + str2b +
                str3a + strSendRecv + str3b +
                str4 + strRecv;

        // Expected string
        String sdpExpected = "";

        // Perform method
        String results = Utils.sdpReplace(sdpTest, regex, newLine, true);
        boolean testOutcome = sdpExpected.equals(results);

        // Print Debug info if test fails
        if (!testOutcome) {
            System.out.println("Expected:" + sdpExpected);
            System.out.println("Results:" + results);
        }

        // Check results
        assertTrue(testOutcome);
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
