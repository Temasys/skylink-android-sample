package sg.com.temasys.skylink.sdk.rtc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.SignatureException;

import sg.com.temasys.skylink.sdk.rtc.Utils;

import static org.junit.Assert.assertNotNull;

/**
 * Created by janidu on 13/1/15.
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
}
