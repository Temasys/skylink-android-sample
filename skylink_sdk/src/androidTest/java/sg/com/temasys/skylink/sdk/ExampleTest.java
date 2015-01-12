package sg.com.temasys.skylink.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertTrue;


/**
 * Created by janidu on 12/1/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class ExampleTest {

    @Test
    public void example() {
        assertTrue(true);
    }
}
