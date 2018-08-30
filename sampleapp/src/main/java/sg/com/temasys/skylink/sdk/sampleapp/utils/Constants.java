package sg.com.temasys.skylink.sdk.sampleapp.utils;

public final class Constants {
    public static final int TIME_OUT = 30;

    public static final String ROOM_NAME_AUDIO_DEFAULT = "MuoiRoom-audio";
    public static final String ROOM_NAME_CHAT_DEFAULT = "MuoiRoom-chat";
    public static final String ROOM_NAME_DATA_DEFAULT = "MuoiRoom-dataTransfer";
    public static final String ROOM_NAME_FILE_DEFAULT = "MuoiRoom-fileTransfer";
    public static final String ROOM_NAME_PARTY_DEFAULT = "MuoiRoom-multiVideosCall";
    public static final String ROOM_NAME_VIDEO_DEFAULT = "MuoiRoom-video";

    public static final String USER_NAME_AUDIO_DEFAULT = "Muoi Pham";
    public static final String USER_NAME_CHAT_DEFAULT = "Muoi Pham";
    public static final String USER_NAME_DATA_DEFAULT = "Muoi Pham";
    public static final String USER_NAME_FILE_DEFAULT = "Muoi Pham";
    public static final String USER_NAME_PARTY_DEFAULT = "Muoi Pham";
    public static final String USER_NAME_VIDEO_DEFAULT = "Muoi Pham";

    public enum CONFIG_TYPE{
        AUDIO,
        VIDEO,
        CHAT,
        DATA,
        FILE,
        MULTI_PARTY_VIDEO
    }
}
