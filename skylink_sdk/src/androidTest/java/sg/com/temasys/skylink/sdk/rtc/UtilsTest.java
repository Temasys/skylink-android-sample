package sg.com.temasys.skylink.sdk.rtc;

import android.text.TextUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.SignatureException;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Tests related to Utils
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class UtilsTest {

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
}
