package sg.com.temasys.skylink.sdk.sampleapp.utils;

public final class Constants {
    public static final int TIME_OUT_FILE_SEND_REQUEST = 10000;

    // default values for room and username setting
    public static final String ROOM_NAME_AUDIO_DEFAULT = "Room-audio";
    public static final String ROOM_NAME_CHAT_DEFAULT = "Room-chat";
    public static final String ROOM_NAME_DATA_DEFAULT = "Room-data streaming";
    public static final String ROOM_NAME_FILE_DEFAULT = "Room-file transfer";
    public static final String ROOM_NAME_MULTI_VIDEOS_DEFAULT = "Room-multi parties";
    public static final String ROOM_NAME_VIDEO_DEFAULT = "Room-one to one media";
    public static final String ROOM_NAME_COMMON_DEFAULT = "Room-common";

    public static final String USER_NAME_AUDIO_DEFAULT = Utils.getDeviceModel() + "_User-audio";
    public static final String USER_NAME_CHAT_DEFAULT = Utils.getDeviceModel() + "_User-chat";
    public static final String USER_NAME_DATA_DEFAULT = Utils.getDeviceModel() + "_User-dataStreaming";
    public static final String USER_NAME_FILE_DEFAULT = Utils.getDeviceModel() + "_User-fileTransfer";
    public static final String USER_NAME_MULTI_VIDEOS_DEFAULT = Utils.getDeviceModel() + "_User-multiParties";
    public static final String USER_NAME_VIDEO_DEFAULT = Utils.getDeviceModel() + "_User-media";
    public static final String USER_NAME_COMMON_DEFAULT = Utils.getDeviceModel() + "_User-common";

    public static final String DEFAULT_VIDEO_DEVICE_FRONT_CAMERA = "Front camera";
    public static final String DEFAULT_VIDEO_DEVICE_BACK_CAMERA = "Back camera";
    public static final String DEFAULT_VIDEO_DEVICE_SCREEN = "Device screen";
    public static final String DEFAULT_VIDEO_DEVICE_CUSTOM = "Camera Custom";
    public static final String DEFAULT_VIDEO_DEVICE_NONE = "No device";

    public enum CONFIG_TYPE {
        AUDIO,
        VIDEO,
        CHAT,
        DATA,
        FILE,
        MULTI_VIDEOS,
        SCREEN_SHARE
    }

    public enum VIDEO_TYPE {
        LOCAL_CAMERA,
        LOCAL_SCREEN,
        REMOTE_CAMERA,
        REMOTE_SCREEN
    }
}
