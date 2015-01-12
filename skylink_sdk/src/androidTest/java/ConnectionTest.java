import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by janidu on 12/1/15.
 */

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class ConnectionTest {

    @Test
    public void testConnection() {
        Assert.assertTrue(true);
    }
}
