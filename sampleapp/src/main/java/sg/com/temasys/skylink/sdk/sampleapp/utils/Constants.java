package sg.com.temasys.skylink.sdk.sampleapp.utils;

public final class Constants {
    public static final int TIME_OUT = 30;

    // default values for room and username setting
    public static final String ROOM_NAME_AUDIO_DEFAULT = "Room-1234";
    public static final String ROOM_NAME_CHAT_DEFAULT = "Room-10";
    public static final String ROOM_NAME_DATA_DEFAULT = "Room-10";
    public static final String ROOM_NAME_FILE_DEFAULT = "Room-10";
    public static final String ROOM_NAME_PARTY_DEFAULT = "Room-1234";
    public static final String ROOM_NAME_VIDEO_DEFAULT = "Room-1234";

    public static final String USER_NAME_AUDIO_DEFAULT = "User-audio";
    public static final String USER_NAME_CHAT_DEFAULT = "User-chat";
    public static final String USER_NAME_DATA_DEFAULT = "User-dataTransfer";
    public static final String USER_NAME_FILE_DEFAULT = "User-fileTransfer";
    public static final String USER_NAME_PARTY_DEFAULT = "User-multiVideosCall";
    public static final String USER_NAME_VIDEO_DEFAULT = "User-video";

    public enum CONFIG_TYPE {
        AUDIO,
        VIDEO,
        CHAT,
        DATA,
        FILE,
        MULTI_PARTY_VIDEO
    }
}
