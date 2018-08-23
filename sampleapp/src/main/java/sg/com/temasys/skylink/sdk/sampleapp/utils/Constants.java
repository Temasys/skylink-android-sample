package sg.com.temasys.skylink.sdk.sampleapp.utils;

public final class Constants {
    public static final int TIME_OUT = 30;

    public static final String ROOM_NAME_AUDIO_DEFAULT = "GrabRoom-audio";
    public static final String ROOM_NAME_CHAT_DEFAULT = "GrabRoom-chat";
    public static final String ROOM_NAME_DATA_DEFAULT = "GrabRoom-dataTransfer";
    public static final String ROOM_NAME_FILE_DEFAULT = "GrabRoom-fileTransfer";
    public static final String ROOM_NAME_PARTY_DEFAULT = "GrabRoom-multiVideosCall";
    public static final String ROOM_NAME_VIDEO_DEFAULT = "GrabRoom-video";

    public static final String USER_NAME_AUDIO_DEFAULT = "GrabUser-audio";
    public static final String USER_NAME_CHAT_DEFAULT = "GrabUser-chat";
    public static final String USER_NAME_DATA_DEFAULT = "GrabUser-dataTransfer";
    public static final String USER_NAME_FILE_DEFAULT = "GrabUser-fileTransfer";
    public static final String USER_NAME_PARTY_DEFAULT = "GrabUser-multiVideosCall";
    public static final String USER_NAME_VIDEO_DEFAULT = "GrabUser-video";

    public enum CONFIG_TYPE{
        AUDIO,
        VIDEO,
        CHAT,
        DATA,
        FILE,
        MULTI_PARTY_VIDEO
    }
}
