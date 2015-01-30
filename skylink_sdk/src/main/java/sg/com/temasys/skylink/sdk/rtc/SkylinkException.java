package sg.com.temasys.skylink.sdk.rtc;

/**
 * @author Temasys Communications Pte Ltd
 */
public class SkylinkException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Exception thrown by the framework.
     * @param message Message describing this exception.
     */
    public SkylinkException(String message) {
        super(message);
    }

}
