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
}
