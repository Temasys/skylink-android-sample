package sg.com.temasys.skylink.sdk.rtc;

import android.text.TextUtils;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SignatureException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;

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
        String appKey = "TestKey";
        String secret = "TestSecret";
        int duration = SkylinkConnection.DEFAULT_DURATION;

        String skylinkConnectionString = Utils.getSkylinkConnectionString(roomName,
                appKey, secret, new Date(), duration);

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
            Log.d(TAG, e.getMessage());
        }
    }


    @Test
    public void testSdpAudioSendReceiveOnly() {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String strSend = "a=" + strS + "\r\n";
        String strRecv = "a=" + strR + "\r\n";
        String strSendRecv = "a=" + strSR + "\r\n";

        // List of possible test string.
        String[] strTestArr = {strSend, strRecv, strSendRecv};
        // Map to store list of replacement strings
        // Each replacement string is paired with a method calling object
        // that can call the method that will change an SDP to result in it.
        Hashtable<sdpSendReceiveOnlyCaller, String> replaceMap =
                new Hashtable<sdpSendReceiveOnlyCaller, String>();
        // Add replacement strings with respective method calling object.
        replaceMap.put(new sdpSendReceiveOnlyAudioCaller() {
            @Override
            public String callSdpSendReceiveOnly(String sdpTest) {
                return Utils.sdpAudioSendOnly(sdpTest);
            }
        }, strSend);
        replaceMap.put(new sdpSendReceiveOnlyAudioCaller() {
            @Override
            public String callSdpSendReceiveOnly(String sdpTest) {
                return Utils.sdpAudioReceiveOnly(sdpTest);
            }
        }, strRecv);
        replaceMap.put(new sdpSendReceiveOnlyVideoCaller() {
            @Override
            public String callSdpSendReceiveOnly(String sdpTest) {
                return Utils.sdpVideoSendOnly(sdpTest);
            }
        }, strSend);
        replaceMap.put(new sdpSendReceiveOnlyVideoCaller() {
            @Override
            public String callSdpSendReceiveOnly(String sdpTest) {
                return Utils.sdpVideoReceiveOnly(sdpTest);
            }
        }, strRecv);

        // List the components used to generate test and expected SDP strings.
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

        Set<sdpSendReceiveOnlyCaller> replaceKey = replaceMap.keySet();

        String mediaType = "";

        // For each replacement string
        for (sdpSendReceiveOnlyCaller callerMethod : replaceKey) {
            // Test all possible test strings.
            for (String strTest : strTestArr) {
                String strReplaced = replaceMap.get(callerMethod);
                // Test string
                String sdpTest = str1 + strTest +
                        strAudioA + strTest + strAudioB +
                        strVideoA + strTest + strVideoB +
                        str4 + strTest;
                // Expected string
                String sdpExpected = str1 + strTest;
                // Perform method
                String results = "";
                if (callerMethod instanceof sdpSendReceiveOnlyAudioCaller) {
                    mediaType = "audio";
                    sdpExpected +=
                            strAudioA + strReplaced + strAudioB +
                                    strVideoA + strTest + strVideoB;
                    results = ((sdpSendReceiveOnlyAudioCaller) callerMethod).callSdpSendReceiveOnly(sdpTest);
                } else if (callerMethod instanceof sdpSendReceiveOnlyVideoCaller) {
                    mediaType = "video";
                    sdpExpected +=
                            strAudioA + strTest + strAudioB +
                                    strVideoA + strReplaced + strVideoB;
                    results = ((sdpSendReceiveOnlyVideoCaller) callerMethod).callSdpSendReceiveOnly(sdpTest);
                }
                sdpExpected += str4 + strTest;

                // Error message to be shown if test fails
                String strErr = "Testing " + mediaType + " set " + strTest.replaceAll("\r\n", "") +
                        " to " + strReplaced.replaceAll("\r\n", "") + ".\n" +
                        "Test input:\n" + sdpTest + "\n" +
                        "Expected:\n" + sdpExpected + "\n" +
                        "Results:\n" + results + "\n";
                // Check results
                assertEquals(strErr, sdpExpected, results);
            }
        }
    }

    // Interface to mark objects that calls a sdp Send or Receive Only method.
    private interface sdpSendReceiveOnlyCaller {
    }

    // Interface to create object that calls a sdp Send or Receive Only method for audio.
    private interface sdpSendReceiveOnlyAudioCaller extends sdpSendReceiveOnlyCaller {
        public String callSdpSendReceiveOnly(String sdpTest);
    }

    // Interface to create object that calls a sdp Send or Receive Only method for video.
    private interface sdpSendReceiveOnlyVideoCaller extends sdpSendReceiveOnlyCaller {
        public String callSdpSendReceiveOnly(String sdpTest);
    }

    @Test
    public void testSdpReplaceReplacesWhenTargetFoundForFirstOccurrence() {
        GenTestStr genTestStr = new GenTestStr();
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String replaceType = GenTestStr.FIRST;
        String regex = "^a=(" + strSR + "|" + strS + "|" + strR + ")$";

        String newLine = "a=" + strS;

        String[] testStr = genTestStr.genTestStrSdpReplace(replaceType);
        // Test string
        String sdpTest = testStr[0];
        // Expected string
        String sdpExpected = testStr[1];

        // Perform method
        String results = Utils.sdpReplace(sdpTest, regex, newLine, true);

        // Error message to be shown if test fails
        String strErr = "Testing SDP " + replaceType + "\n" +
                "Test input:\n" + sdpTest + "\n" +
                "Expected:\n" + sdpExpected + "\n" +
                "Results:\n" + results + "\n";
        // Check results
        assertEquals(strErr, sdpExpected, results);

    }

    @Test
    public void testSdpReplaceReplacesWhenTargetFoundForAllOccurrence() {
        GenTestStr genTestStr = new GenTestStr();
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String replaceType = GenTestStr.ALL;
        String regex = "^a=(" + strSR + "|" + strS + "|" + strR + ")$";

        String newLine = "a=" + strS;

        String[] testStr = genTestStr.genTestStrSdpReplace(replaceType);
        // Test string
        String sdpTest = testStr[0];
        // Expected string
        String sdpExpected = testStr[1];

        // Perform method
        String results = Utils.sdpReplace(sdpTest, regex, newLine, false);

        // Error message to be shown if test fails
        String strErr = "Testing SDP " + replaceType + "\n" +
                "Test input:\n" + sdpTest + "\n" +
                "Expected:\n" + sdpExpected + "\n" +
                "Results:\n" + results + "\n";
        // Check results
        assertEquals(strErr, sdpExpected, results);

    }

    @Test
    public void testSdpReplaceNotReplaceWhenTargetNotFoundForFirstOccurrence() {
        GenTestStr genTestStr = new GenTestStr();
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String replaceType = GenTestStr.NONE;
        String regex = "^a=(" + strSR + "v|" + strS + "y|" + strR + "y)$";

        String newLine = "a=" + strS;

        String[] testStr = genTestStr.genTestStrSdpReplace(replaceType);
        // Test string
        String sdpTest = testStr[0];
        // Expected string
        String sdpExpected = testStr[1];

        // Perform method
        String results = Utils.sdpReplace(sdpTest, regex, newLine, true);

        // Error message to be shown if test fails
        String strErr = "Testing SDP " + replaceType + "\n" +
                "Test input:\n" + sdpTest + "\n" +
                "Expected:\n" + sdpExpected + "\n" +
                "Results:\n" + results + "\n";
        // Check results
        assertEquals(strErr, sdpExpected, results);
    }

    // Class with methods to generate test strings required by some tests.
    private class GenTestStr {
        public static final String FIRST = "Replace first occurrence only.";
        public static final String ALL = "Replace all occurrences.";
        public static final String NONE = "Replace no occurrences.";

        // For sdpReplace
        public String[] genTestStrSdpReplace(String testType) {

            String strSR = "sendrecv";
            String strS = "sendonly";
            String strR = "recvonly";

            String strSend = "a=" + strS + "\r\n";
            String strRecv = "a=" + strR + "\r\n";
            String strSendRecv = "a=" + strSR + "\r\n";

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

            // Generate specific expected string.
            switch (testType) {
                case FIRST:
                    // Only the first occurrence is replaced.
                    sdpExpected = str1 + newLine + "\r\n" +
                            str2a + strSendRecv + str2b +
                            str3a + strSendRecv + str3b +
                            str4 + strRecv;
                    break;
                case ALL:
                    // All occurrences are replaced.
                    sdpExpected = str1 + newLine + "\r\n" +
                            str2a + newLine + "\r\n" + str2b +
                            str3a + newLine + "\r\n" + str3b +
                            str4 + newLine + "\r\n";
                    break;
                case NONE:
                    // No occurrences are found (due to regex being different), hence return "".
                default:
                    break;
            }

            // Generate return array
            String[] strArr = new String[2];
            strArr[0] = sdpTest;
            strArr[1] = sdpExpected;

            return strArr;
        }

        // For sdpSegment
        public String[] genTestStrSdpSegment(boolean isAudio, boolean isPresent) {

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
            String strAudioo =
                    "a=mid:audioo\r\n" +
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

            // Generate return array
            String[] strArr = new String[4];
            // Test string
            String sdpTest = strBefore;

            // Generate specific expected string.
            if (isAudio) {
                if (isPresent) {
                    // Search for Audio when it is present.
                    sdpTest += strAudio + strVideo + strAfter;
                    strArr[0] = strBefore;
                    strArr[1] = strAudio;
                    strArr[2] = strVideo + strAfter;
                } else {
                    // Search for Audio when it is NOT present.
                    sdpTest += strAudioo + strVideo + strAfter;
                    strArr[0] = sdpTest;
                    strArr[1] = "";
                    strArr[2] = "";
                }
            } else {
                // For Video case
                if (isPresent) {
                    // Search for Video when it is present.
                    sdpTest += strAudio + strVideo + strAfter;
                    strArr[0] = strBefore + strAudio;
                    strArr[1] = strVideo;
                    strArr[2] = strAfter;
                } else {
                    // Search for Video when it is NOT present.
                    sdpTest += strAudio + strVideoo + strAfter;
                    strArr[0] = sdpTest;
                    strArr[1] = "";
                    strArr[2] = "";
                }
            }

            strArr[3] = sdpTest;
            return strArr;
        }
    }

    // Test that able to find audio and video segments if and only if they exist.
    @Test
    public void testSdpSegment() {

        GenTestStr genTestStr = new GenTestStr();
        // Test for audio / video segments.
        boolean[] mediaIsAudioArr = {true, false};
        // Test the cases when the audio/video segments are present / not present.
        boolean[] mediaPresentArr = {true, false};

        for (boolean mediaIsAudio : mediaIsAudioArr) {
            for (boolean mediaPresent : mediaPresentArr) {

                String mediaType = "audio";
                if (!mediaIsAudio) {
                    mediaType = "video";
                }

                // Generate all test Strings
                String[] strArr = genTestStr.genTestStrSdpSegment(mediaIsAudio, mediaPresent);

                // Get the test SDP.
                String sdpTest = strArr[3];

                // Perform methodds
                String[] sdpSegments = Utils.sdpSegment(sdpTest, mediaType);

                // Error message to be shown if test fails
                String strErr = "";
                // Base message for every error message.
                String strErrA = "Testing SDP Segment for " + mediaType + " " + mediaPresent + ".\n" +
                        "Test input:\n" + sdpTest + "\n" +
                        "Expected:\n";

                // Check results
                // Before segment is correct.
                strErr = strErrA +
                        "Segment[0]:\n" + strArr[0] + "\n" +
                        "Results:\n" +
                        "Segment[0]:\n" + sdpSegments[0] + "\n";
                assertEquals(strErr, strArr[0], sdpSegments[0]);

                // Found segment is correct.
                strErr = strErrA +
                        "Segment[1]:\n" + strArr[1] + "\n" +
                        "Results:\n" +
                        "Segment[1]:\n" + sdpSegments[1] + "\n";
                assertEquals(strErr, strArr[1], sdpSegments[1]);

                // After segment is correct.
                strErr = strErrA +
                        "Segment[2]:\n" + strArr[2] + "\n" +
                        "Results:\n" +
                        "Segment[2]:\n" + sdpSegments[2] + "\n";
                assertEquals(strErr, strArr[2], sdpSegments[2]);
            }
        }
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
