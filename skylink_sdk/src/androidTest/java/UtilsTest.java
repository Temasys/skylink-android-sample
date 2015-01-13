import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.SignatureException;

import sg.com.temasys.skylink.sdk.utils.Utils;

import static org.junit.Assert.assertNotNull;

/**
 * Created by janidu on 13/1/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class UtilsTest {

    @Test
    public void testCalculateRFC2104HMAC() throws SignatureException {
        assertNotNull("Should calculate", Utils.calculateRFC2104HMAC("Test", "Data"));
    }
}
