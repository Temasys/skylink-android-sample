package sg.com.temasys.skylink.sdk.rtc;


public class ErrorCodes {

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
}
