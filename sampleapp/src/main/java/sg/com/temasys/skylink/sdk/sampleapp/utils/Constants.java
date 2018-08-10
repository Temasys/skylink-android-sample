package sg.com.temasys.skylink.sdk.sampleapp.utils;

public final class Constants {
    public static final int TIME_OUT = 30;

    public static final String ROOM_NAME_AUDIO_DEFAULT = "audioRoom";
    public static final String ROOM_NAME_CHAT_DEFAULT = "chatRoom";
    public static final String ROOM_NAME_DATA_DEFAULT = "dataRoom";
    public static final String ROOM_NAME_FILE_DEFAULT = "fileRoom";
    public static final String ROOM_NAME_PARTY_DEFAULT = "partyRoom";
    public static final String ROOM_NAME_VIDEO_DEFAULT = "videoRoom";

    public static final String USER_NAME_AUDIO_DEFAULT = "audioUser";
    public static final String USER_NAME_CHAT_DEFAULT = "chatUser";
    public static final String USER_NAME_DATA_DEFAULT = "dataUser";
    public static final String USER_NAME_FILE_DEFAULT = "fileUser";
    public static final String USER_NAME_PARTY_DEFAULT = "partyUser";
    public static final String USER_NAME_VIDEO_DEFAULT = "videoUser";

    public enum CONFIG_TYPE{
        AUDIO,
        VIDEO,
        CHAT,
        DATA,
        FILE,
        MULTI_PARTY_VIDEO
    }

    private Constants() {
    }
}
