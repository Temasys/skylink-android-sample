package sg.com.temasys.skylink.sdk.rtc;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;

import mockit.internal.startup.Startup;

/**
 * This TestRunner is used to support Jmockit with Robolectric
 * <p/>
 * Created by janidu on 4/5/15.
 */
public class JmockitRunner extends RobolectricTestRunner {

    static {
        Startup.initializeIfPossible();
    }

    /**
     * Constructs a new instance of the test runner.
     *
     * @throws InitializationError if the test class is malformed
     */
    public JmockitRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }
}