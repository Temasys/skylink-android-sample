package sg.com.temasys.skylink.sdk.sampleapp.utils;

public final class Constants {
    public static final int TIME_OUT = 30;

    // default values for room and username setting
    public static final String ROOM_NAME_AUDIO_DEFAULT = "Room-audio";
    public static final String ROOM_NAME_CHAT_DEFAULT = "Room-chat";
    public static final String ROOM_NAME_DATA_DEFAULT = "Room-dataTransfer";
    public static final String ROOM_NAME_FILE_DEFAULT = "Room-fileTransfer";
    public static final String ROOM_NAME_PARTY_DEFAULT = "Room-multiVideosCall";
    public static final String ROOM_NAME_VIDEO_DEFAULT = "Room-video";

    public static final String USER_NAME_AUDIO_DEFAULT = "User-audio";
    public static final String USER_NAME_CHAT_DEFAULT = "User-chat";
    public static final String USER_NAME_DATA_DEFAULT = "User-dataTransfer";
    public static final String USER_NAME_FILE_DEFAULT = "User-fileTransfer";
    public static final String USER_NAME_PARTY_DEFAULT = "User-multiVideosCall";
    public static final String USER_NAME_VIDEO_DEFAULT = "User-video";

    public static final String NON_VIDEO_DEVICE_DEFAULT = "NONE_VIDEO_DEVICE";

    public enum CONFIG_TYPE {
        AUDIO,
        VIDEO,
        CHAT,
        DATA,
        FILE,
        MULTI_PARTY_VIDEO,
        SCREEN_SHARE
    }

    public enum VIDEO_TYPE {
        LOCAL_CAMERA,
        LOCAL_SCREEN,
        REMOTE_CAMERA,
        REMOTE_SCREEN
    }
}
