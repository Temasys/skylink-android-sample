package sg.com.temasys.skylink.sdk.rtc;

public class TestConstants {

    private TestConstants() {
    }

    private static final String SKYLINK_APP_URL = "http://api.temasys.com.sg/api/";

    public static final String SKYLINK_CONNECTION_STRING = SKYLINK_APP_URL +
            "cff8ff52-ce29-4840-a489-0ceef3af81f0/UnitTestRoom/2015-02-23T17:04:00.0Z/" +
            "3.4028235E38?cred=oNDnb7ZP42Ta9aeGLOBFN77fTFw%3D";

    public static final String INVALID_SKYLINK_CONNECTION_STRING = SKYLINK_APP_URL +
            "cff8ff52-ce29-4840-a489-0ceef3af81f0/UnitTestRoom/2015-02-23T17:04:00.0Z/" +
            "3.4028235E38?cred=oNDnb7ZP42Ta9aeGLOBFN7";
}
