package sg.com.temasys.skylink.sdk.rtc;


public class Errors {

    /**
     * The user is jamming the system with many continuous messages send less than the interval.
     */
    public static final int REDIRECT_REASON_FAST_MSG = 0;

    /**
     * The user is locked out from the room from joining.
     */
    public static final int REDIRECT_REASON_LOCKED = 1;

    /**
     * The user is unable to join the room as room is already full.
     */
    public static final int REDIRECT_REASON_ROOM_FULL = 2;

    /**
     * The server detects a duplicated login with the same credentials provided.
     */
    public static final int REDIRECT_REASON_DUPLICATED_LOGIN = 3;

    /**
     * The server has an exception occurring.
     */
    public static final int REDIRECT_REASON_SERVER_ERROR = 4;

    /**
     * There is a verification issue with the user.
     */
    public static final int REDIRECT_REASON_VERIFICATION = 5;

    /**
     * The user is attempting to join a persistent room that has ended.
     */
    public static final int REDIRECT_REASON_EXPIRED = 6;

    /**
     * The user is rejected from the room because the persistent room has ended.
     */
    public static final int REDIRECT_REASON_ROOM_CLOSED = 7;

    /**
     * The user is warned that the room is closing soon.
     */
    public static final int REDIRECT_REASON_ROOM_TO_CLOSED = 8;

    /**
     * The user is unable to join due to insufficient seat quota for the App Key.
     */
    public static final int REDIRECT_REASON_SEAT_QUOTA = 9;

    /**
     * The user is redirected due to an unknown reason
     */
    public static final int REDIRECT_REASON_UNKNOWN = 10;

    /**
     * User is disconnected due to an unexpected error
     */
    public static final int DISCONNECT_UNEXPECTED_ERROR = 11;

    /**
     * Error message from Socket.io connection
     */
    public static final int SIGNALING_CONNECTION_ERROR = 12;

    /**
     * Unable to switch camera
     */
    public static final int VIDEO_SWITCH_CAMERA_ERROR = 13;

    /**
     * Error message when room parameters cannot be fetched
     */
    public static final int UNEXPECTED_ERROR_ROOM_PARAMETERS = 14;


    /**
     * User disconnected from the room
     */
    public static final int DISCONNECT_FROM_ROOM = 15;

    /**
     * HealthChecker reach max restart attempts.
     */
    public static final int PC_MAX_RESTART_ATTEMPTS_REACHED = 16;

    public static final int DC_MSG_UNKNOWN_TYPE = 17;

    public static final int DC_UNABLE_TO_SEND_MESSAGE = 17;

    public static final int HANDSHAKE_UNABLE_TO_SET_USERINFO_IN_WELCOME = 18;

    public static final int HANDSHAKE_UNABLE_TO_CREATE_JOINROOM_JSON = 19;

    public static final int HANDSHAKE_UNABLE_TO_CREATE_CANDIDATE_JSON = 20;

    public static final int HANDSHAKE_UNABLE_TO_CREATE_SDP_JSON = 20;

    public static final int HANDSHAKE_UNABLE_TO_CREATE_MUTE_AUDIO_JSON = 21;

    public static final int HANDSHAKE_UNABLE_TO_CREATE_MUTE_VIDEO_JSON = 22;

    public static final int HANDSHAKE_UNABLE_TO_READ_JSON_REDIRECT = 23;

    public static final int HANDSHAKE_RESTART_TIMER_FAILED = 24;

    public static final int SIG_MSG_TARGETED_FOR_OTHERS = 25;

    public static final int SIG_MSG_UNKNOWN_TYPE = 26;

    public static final int SIG_MSG_OUTGOING_HAS_NO_TYPE = 27;

    public static final int SIG_MSG_UNABLE_TO_CREATE_ENTER_JSON = 28;

    public static final int SIG_MSG_UNABLE_TO_CREATE_GROUP_JSON = 29;

    public static final int SIG_MSG_UNABLE_TO_READ_JSON = 30;

    public static final int CONNECT_UNABLE_APP_SERVER = 31;

    public static final int CONNECT_IS_EMPTY_ROOM_PARAMS = 32;

    public static final int CONNECT_IS_ERROR_IN_ROOM_PARAMS = 33;

    public static final int CONNECT_IS_SIG_SERVER_IP_WRONG_IN_ROOM_PARAMS = 34;

    public static final int CONNECT_UNABLE_TO_READ_ROOM_PARAMS_JSON = 35;

    public static final int CONNECT_UNABLE_SIG_SERVER = 36;

    public static final int CONNECT_NO_RESPONSE_APP_SERVER = 37;

    public static final int CONNECT_SOCKET_ERROR_SIG_SERVER = 38;

    public static final int CONNECT_UNABLE_CLOSE_STREAM_APP_SERVER = 39;

    public static final int CONNECT_UNABLE_RESTART_ON_REJOIN = 40;

    public static final int USERINFO_UNABLE_TO_CREATE_FROM_JSON = 41;

    public static final int USERINFO_UNABLE_TO_GET_JSON = 42;

    /**
     * Return the user viewable description for a particular error code.
     * Return null if there is no description available.
     *
     * @param errorCode
     * @return
     */
    public static String getErrorString(int errorCode) {
        switch (errorCode) {
            case REDIRECT_REASON_FAST_MSG:
                return "Too many messages were sent to the Skylink server over a short interval!\n"
                        + "Send no more than 16 messages in 1 second.";

            case REDIRECT_REASON_LOCKED:
                return "The room is locked.";

            case REDIRECT_REASON_ROOM_FULL:
                return "Unable to join the room as room is already full.";

            case REDIRECT_REASON_DUPLICATED_LOGIN:
                return "Another login with the same credentials was detected.";

            case REDIRECT_REASON_SERVER_ERROR:
                return "Exception on the Server.";

            case REDIRECT_REASON_VERIFICATION:
                return "Problem with user verification.";

            case REDIRECT_REASON_EXPIRED:
                return "Attempted to join a persistent room that has ended.";

            case REDIRECT_REASON_ROOM_CLOSED:
                return "The persistent room has ended.";

            case REDIRECT_REASON_ROOM_TO_CLOSED:
                return "The room is closing soon.";

            case REDIRECT_REASON_SEAT_QUOTA:
                return "Insufficient seat quota for this App Key.";

            case REDIRECT_REASON_UNKNOWN:
                return "Unknown error resulted in redirect.";

            default:
                return null;
        }
    }
}
