package sg.com.temasys.skylink.sdk.sampleapp.setting;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import sg.com.temasys.skylink.sdk.sampleapp.R;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment.PREF_ROOM_NAME_AUDIO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment.PREF_ROOM_NAME_CHAT;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment.PREF_ROOM_NAME_DATA;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment.PREF_ROOM_NAME_FILE;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment.PREF_ROOM_NAME_MULTI_VIDEOS;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment.PREF_ROOM_NAME_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment.PREF_USER_NAME_AUDIO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment.PREF_USER_NAME_CHAT;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment.PREF_USER_NAME_DATA;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment.PREF_USER_NAME_FILE;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment.PREF_USER_NAME_MULTI_VIDEOS;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment.PREF_USER_NAME_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_AUDIO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_CHAT_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_DATA_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_FILE_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_MULTI_VIDEOS_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_VIDEO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_AUDIO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_CHAT_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_DATA_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_FILE_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_MULTI_VIDEOS_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_VIDEO_DEFAULT;

/**
 * Created by phyo.pwint on 27/7/16.
 */

public class Config {

    public static final String PREFERENCES_NAME = "SA_PREF_NAME";

    // Selected App key details Preferences Key values
    private static final String PREF_SELECTED_APP_KEY = "SelectedAppKey";
    private static final String PREF_SELECTED_APP_KEY_SECRET = "SelectedAppKeySecret";
    private static final String PREF_SELECTED_APP_KEY_DESC = "SelectedAppKeyDesc";
    private static final String PREF_SELECTED_APP_KEY_SMR = "SelectedAppKeySmr";

    // Config values for Selected App key.
    private static String APP_KEY = "App key.";
    private static String APP_KEY_SECRET = "App key secret.";
    private static String APP_KEY_DESCRIPTION = "Sample App Key description.";
    // Is Skylink Media Relay (SMR) enabled for this App key.
    private static boolean IS_APP_KEY_SMR = false;

    // Config values for room names.
    public static String ROOM_NAME_AUDIO = ROOM_NAME_AUDIO_DEFAULT;
    public static String ROOM_NAME_CHAT = ROOM_NAME_CHAT_DEFAULT;
    public static String ROOM_NAME_DATA = ROOM_NAME_DATA_DEFAULT;
    public static String ROOM_NAME_FILE = ROOM_NAME_FILE_DEFAULT;
    public static String ROOM_NAME_MULTI_VIDEOS = ROOM_NAME_MULTI_VIDEOS_DEFAULT;
    public static String ROOM_NAME_VIDEO = ROOM_NAME_VIDEO_DEFAULT;

    // Config values for user names.
    public static String USER_NAME_AUDIO = USER_NAME_AUDIO_DEFAULT;
    public static String USER_NAME_CHAT = USER_NAME_CHAT_DEFAULT;
    public static String USER_NAME_DATA = USER_NAME_DATA_DEFAULT;
    public static String USER_NAME_FILE = USER_NAME_FILE_DEFAULT;
    public static String USER_NAME_MULTI_VIDEOS = USER_NAME_MULTI_VIDEOS_DEFAULT;
    public static String USER_NAME_VIDEO = USER_NAME_VIDEO_DEFAULT;

    // Constants for saving default settings of audio and video
    // Is speaker mode the default for audio demo.
    public static String DEFAULT_SPEAKER_AUDIO = "defaultSpeakerAudio";
    // Is speaker mode the default for video demo.
    public static String DEFAULT_SPEAKER_VIDEO = "defaultSpeakerVideo";
    /**
     * The default {@link sg.com.temasys.skylink.sdk.rtc.SkylinkConfig.VideoDevice} for video demo.
     */
    public static String DEFAULT_VIDEO_DEVICE = "defaultVideoDevice";
    public static String DEFAULT_VIDEO_RESOLUTION = "defaultVideoResolution";
    public static String DEFAULT_SCREEN_RESOLUTION = "defaultScreenResolution";

    public static String VIDEO_RESOLUTION_VGA = "vga";
    public static String VIDEO_RESOLUTION_HDR = "hdr";
    public static String VIDEO_RESOLUTION_FHD = "fhd";

    public static String SCREEN_RESOLUTION_LARGE = "large";
    public static String SCREEN_RESOLUTION_MEDIUM = "medium";
    public static String SCREEN_RESOLUTION_SMALL = "small";

    public static final String HAS_DATA_TRANSFER_CONFIG = "hasDataTransfer";
    public static final String HAS_PEER_MESSAGING_CONFIG = "hasPeerMessage";
    public static final String HAS_FILE_TRANSFER_CONFIG = "hasFileConfig";
    public static final String DEFAULT_ENCRYPTED_SECRET_CONFIG = "defaultEncryptedSecret";
    public static final String USE_HW_ACC_CONFIG = "useHWAccConfig";
    public static final String USE_H246_PROFILE_CONFIG = "useH246ProfileConfig";
    public static final String USE_VP8_ENCODER_CONFIG = "useVP8EncoderConfig";
    public static final String USE_AUDIO_STEREO_CONFIG = "userAudioStereoConfig";
    public static final String USE_AUTO_GAIN_CONTROL_CONFIG = "useAutoGainControlConfig";
    public static final String USE_AUDIO_ECHO_CANCELLATION_CONFIG = "userAudioEchoCancellationConfig";
    public static final String USE_AUDIO_HIGH_PASS_CONFIG = "useAudioHighPassConfig";
    public static final String USE_AUDIO_NOISE_SUPPRESSION_CONFIG = "useAudioNoiseSuppressionConfig";
    public static final String MIRROR_LOCAL_VIEW_CONFIG = "mirrorLocalViewConfig";
    public static final String REPORT_VIDEO_RES_STABLE_CONFIG = "reportVideoResUntilStableConfig";
    public static final String REPORT_VIDEO_RES_ON_CHANGED_CONFIG = "reportVideoResOnChangedConfig";
    public static final String NO_OF_REPORT_VIDEO_RES_UNTILL_STABLE_CONFIG = "noOfReportVideoResUntilStableConfig";
    public static final String TIME_OF_REPORT_VIDEO_RES_NEXT_CHECKING_CONFIG = "timeOfReportVideoResForNextChecking";
    public static final String MAX_AUDIO_BITRATE_CONFIG = "maxOfAudioBitrate";
    public static final String MAX_VIDEO_BITRATE_CONFIG = "maxOfVideoBitrate";
    public static final String MAX_DATA_BITRATE_CONFIG = "maxOfDataBitrate";
    public static final String MAX_PEER_IN_AUDIO_ROOM_CONFIG = "maxOfPeerInAudioRoom";
    public static final String MAX_PEER_IN_VIDEO_ROOM_CONFIG = "maxOfPeerInVideoRoom";
    public static final String MAX_PEER_IN_NO_MEDIA_ROOM_CONFIG = "maxOfPeerInNoMediaRoom";
    public static final String USE_TURN_SERVER_CONFIG = "useTurnServerConfig";
    public static final String USE_STURN_SERVER_CONFIG = "useSturnServerConfig";
    public static final String USE_HOST_SERVER_CONFIG = "useHostServerConfig";
    public static final String ALLOW_ICE_RESTART_CONFIG = "allowIceRestartConfig";
    public static final String RECONNECT_ATTEMPS_CONFIG = "reconnectAttemps";
    public static final String RECONNECT_DELAY_CONFIG = "reconnectDelays";
    public static final String USE_MULTI_TRACKS_UP_CONFIG = "useMultiTracksUPConfig";
    public static final String DEFAULT_AUDIO_VIDEO_SEND_CONFIG = "audioVideoSendConfig";
    public static final String DEFAULT_AUDIO_VIDEO_RECEIVE_CONFIG = "audioVideoReceiveConfig";


    public static final String AUDIO_AND_VIDEO = "audioAndVideo";
    public static final String AUDIO_ONLY = "audioOnly";
    public static final String VIDEO_ONLY = "videoOnly";
    public static final String NO_AUDIO_NO_VIDEO = "noAudioNoVideo";

    public static final String DEFAULT_AUDIO_CODEC_CONFIG = "audioCodecConfig";
    public static final String AUDIO_CODEC_OPUS = "opus";
    public static final String AUDIO_CODEC_ISAC = "isac";
    public static final String DEFAULT_SOCKET_TRANSPORT_CONFIG = "socketTransportConfig";
    public static final String SOCKET_POLLING = "polling";
    public static final String SOCKET_WEB = "webSocket";
    public static final String DEFAULT_NETWORK_TRANSPORT_CONFIG = "networkTransportConfig";
    public static final String NETWORK_UDP = "udp";
    public static final String NETWORK_TCP = "tcp";
    public static final String DEFAULT_ROOM_SIZE_CONFIG = "roomSizeConfig";
    public static final String ROOM_SIZE_XS = "extraSmall";
    public static final String ROOM_SIZE_S = "small";
    public static final String ROOM_SIZE_M = "medium";
    public static final String ROOM_SIZE_L = "large";

    public enum VideoResolution {
        VGA,
        HDR,
        FHD
    }

    // Custom ScreenResolution
    // Normally the screen size on mobile has height = 2*width in portrait mode
    public enum ScreenResolution {
        LARGE_PORTRAIT(800, 1600),
        MEDIUM_PORTRAIT(500, 1000),
        SMALL_PORTRAIT(230, 460),
        LARGE_LANDSCAPE(1600, 800),
        MEDIUM_LANDSCAPE(1000, 500),
        SMALL_LANDSCAPE(460, 230);

        ScreenResolution(int width, int height) {
            this.width = width;
            this.height = height;
        }

        int width;
        int height;

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    private Config() {
    }

    //----------------------------------------------------------------------------------------------
    // APIs
    //----------------------------------------------------------------------------------------------

    /**
     * Load selected App key.
     * Load from SharedPreferences if available, else load from config.xml.
     *
     * @param activity
     */
    public static void loadSelectedAppKey(Activity activity) {
        // Load from Preferences if available.
        final SharedPreferences sharedPref = activity.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String appKey = sharedPref.getString(PREF_SELECTED_APP_KEY, null);
        String appKeySecret;
        String appKeyDesc;
        boolean appKeySmr;

        if (appKey != null) {
            // Load from Preferences.
            appKeySecret = sharedPref.getString(PREF_SELECTED_APP_KEY_SECRET, "");
            appKeyDesc = sharedPref.getString(PREF_SELECTED_APP_KEY_DESC, "");
            appKeySmr = sharedPref.getBoolean(PREF_SELECTED_APP_KEY_SMR, false);
        } else {
            // Load from config.xml
            // Get resource Id for the specified type of App key (SMR enabled or not).
            boolean isSmr = activity.getResources().getBoolean(R.bool.is_app_key_smr);
            int appKeyResId = R.string.app_key_no_smr;
            int appKeySecretResId = R.string.app_key_secret_no_smr;
            int appKeyDescResId = R.string.app_key_desc_no_smr;

            if (isSmr) {
                appKeyResId = R.string.app_key_smr;
                appKeySecretResId = R.string.app_key_secret_smr;
                appKeyDescResId = R.string.app_key_desc_smr;
            }

            appKey = activity.getString(appKeyResId);
            appKeySecret = activity.getString(appKeySecretResId);
            appKeyDesc = activity.getString(appKeyDescResId);
            appKeySmr = isSmr;
        }

        // Set values to Config and Preferences.
        setAppKey(appKey, activity);
        setAppKeySecret(appKeySecret, activity);
        setAppKeyDescription(appKeyDesc, activity);
        setAppKeySmr(appKeySmr, activity);
    }

    public static void loadRoomUserNames(Activity activity) {
        final SharedPreferences sharedPref = activity.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        // Populate room and user names with values from Preferences if they exist.
        ROOM_NAME_AUDIO = sharedPref.getString(PREF_ROOM_NAME_AUDIO, ROOM_NAME_AUDIO);
        ROOM_NAME_VIDEO = sharedPref.getString(PREF_ROOM_NAME_VIDEO, ROOM_NAME_VIDEO);
        ROOM_NAME_CHAT = sharedPref.getString(PREF_ROOM_NAME_CHAT, ROOM_NAME_CHAT);
        ROOM_NAME_DATA = sharedPref.getString(PREF_ROOM_NAME_DATA, ROOM_NAME_DATA);
        ROOM_NAME_FILE = sharedPref.getString(PREF_ROOM_NAME_FILE, ROOM_NAME_FILE);
        ROOM_NAME_MULTI_VIDEOS = sharedPref.getString(PREF_ROOM_NAME_MULTI_VIDEOS, ROOM_NAME_MULTI_VIDEOS);

        USER_NAME_AUDIO = sharedPref.getString(PREF_USER_NAME_AUDIO, USER_NAME_AUDIO);
        USER_NAME_VIDEO = sharedPref.getString(PREF_USER_NAME_VIDEO, USER_NAME_VIDEO);
        USER_NAME_CHAT = sharedPref.getString(PREF_USER_NAME_CHAT, USER_NAME_CHAT);
        USER_NAME_DATA = sharedPref.getString(PREF_USER_NAME_DATA, USER_NAME_DATA);
        USER_NAME_FILE = sharedPref.getString(PREF_USER_NAME_FILE, USER_NAME_FILE);
        USER_NAME_MULTI_VIDEOS = sharedPref.getString(PREF_USER_NAME_MULTI_VIDEOS, USER_NAME_MULTI_VIDEOS);
    }

    /**
     * Get Preferences for specified activity and set boolean value for specific key.
     *
     * @param key
     * @param value
     * @param activity
     */
    public static void setPrefBoolean(String key, boolean value, Activity activity) {
        final SharedPreferences sharedPref = activity.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    /**
     * Get Preferences for specified activity and set String value for specific key.
     *
     * @param key
     * @param value
     * @param activity
     */
    public static void setPrefString(String key, String value, Activity activity) {
        final SharedPreferences sharedPref = activity.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    //----------------------------------------------------------------------------------------------
    // Getters & Setters
    //----------------------------------------------------------------------------------------------

    public static String getAppKey() {
        return APP_KEY;
    }

    public static void setAppKey(String appKey, Activity activity) {
        if (appKey == null || "".equals(appKey)) {
            appKey = "appKey";
        }
        APP_KEY = appKey;
        setPrefString(PREF_SELECTED_APP_KEY, appKey, activity);
    }

    public static String getAppKeySecret() {
        return APP_KEY_SECRET;
    }

    public static void setAppKeySecret(String appKeySecret, Activity activity) {
        if (appKeySecret == null || "".equals(appKeySecret)) {
            appKeySecret = "appKeySecret";
        }
        // Write to Config and Preferences only if value changed.
        if (!APP_KEY_SECRET.equals(appKeySecret)) {
            APP_KEY_SECRET = appKeySecret;
            setPrefString(PREF_SELECTED_APP_KEY_SECRET, appKeySecret, activity);
        }
    }

    public static String getAppKeyDescription() {
        return APP_KEY_DESCRIPTION;
    }

    public static void setAppKeyDescription(String appKeyDescription, Activity activity) {
        if (appKeyDescription == null || "".equals(appKeyDescription)) {
            appKeyDescription = "appKeyDescription";
        }

        // Write to Config and Preferences only if value changed.
        if (!APP_KEY_DESCRIPTION.equals(appKeyDescription)) {
            APP_KEY_DESCRIPTION = appKeyDescription;
            setPrefString(PREF_SELECTED_APP_KEY_DESC, appKeyDescription, activity);
        }
    }

    public static boolean isAppKeySmr() {
        return IS_APP_KEY_SMR;
    }

    public static void setAppKeySmr(boolean appKeySmr, Activity activity) {

        // Write to Config and Preferences only if value changed.
        if (IS_APP_KEY_SMR != appKeySmr) {
            IS_APP_KEY_SMR = appKeySmr;
            setPrefBoolean(PREF_SELECTED_APP_KEY_SMR, appKeySmr, activity);
        }
    }

    /**
     * Set Config value.
     * If value is null or "", set to Default value.
     * Write to Preferences only if new value differs from current value.
     *
     * @param newValue
     * @param activity
     */
    public static void setUserNameAudio(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = USER_NAME_AUDIO_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(USER_NAME_AUDIO)) {
            USER_NAME_AUDIO = newValue;
            setPrefString(PREF_USER_NAME_AUDIO, newValue, activity);
        }
    }

    /**
     * Set Config value.
     * If value is null or "", set to Default value.
     * Write to Preferences only if new value differs from current value.
     *
     * @param newValue
     * @param activity
     */
    public static void setUserNameChat(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = USER_NAME_CHAT_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(USER_NAME_CHAT)) {
            USER_NAME_CHAT = newValue;
            setPrefString(PREF_USER_NAME_CHAT, newValue, activity);
        }
    }

    /**
     * Set Config value.
     * If value is null or "", set to Default value.
     * Write to Preferences only if new value differs from current value.
     *
     * @param newValue
     * @param activity
     */
    public static void setUserNameData(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = USER_NAME_DATA_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(USER_NAME_DATA)) {
            USER_NAME_DATA = newValue;
            setPrefString(PREF_USER_NAME_DATA, newValue, activity);
        }
    }

    /**
     * Set Config value.
     * If value is null or "", set to Default value.
     * Write to Preferences only if new value differs from current value.
     *
     * @param newValue
     * @param activity
     */
    public static void setUserNameFile(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = USER_NAME_FILE_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(USER_NAME_FILE)) {
            USER_NAME_FILE = newValue;
            setPrefString(PREF_USER_NAME_FILE, newValue, activity);
        }
    }

    /**
     * Set Config value.
     * If value is null or "", set to Default value.
     * Write to Preferences only if new value differs from current value.
     *
     * @param newValue
     * @param activity
     */
    public static void setUserNameMultiVideos(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = USER_NAME_MULTI_VIDEOS_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(USER_NAME_MULTI_VIDEOS)) {
            USER_NAME_MULTI_VIDEOS = newValue;
            setPrefString(PREF_USER_NAME_MULTI_VIDEOS, newValue, activity);
        }
    }

    /**
     * Set Config value.
     * If value is null or "", set to Default value.
     * Write to Preferences only if new value differs from current value.
     *
     * @param newValue
     * @param activity
     */
    public static void setUserNameVideo(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = USER_NAME_VIDEO_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(USER_NAME_VIDEO)) {
            USER_NAME_VIDEO = newValue;
            setPrefString(PREF_USER_NAME_VIDEO, newValue, activity);
        }
    }

    /**
     * Set Config value.
     * If value is null or "", set to Default value.
     * Write to Preferences only if new value differs from current value.
     *
     * @param newValue
     * @param activity
     */
    public static void setRoomNameAudio(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = ROOM_NAME_AUDIO_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(ROOM_NAME_AUDIO)) {
            ROOM_NAME_AUDIO = newValue;
            setPrefString(PREF_ROOM_NAME_AUDIO, newValue, activity);
        }
    }

    /**
     * Set Config value.
     * If value is null or "", set to Default value.
     * Write to Preferences only if new value differs from current value.
     *
     * @param newValue
     * @param activity
     */
    public static void setRoomNameChat(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = ROOM_NAME_CHAT_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(ROOM_NAME_CHAT)) {
            ROOM_NAME_CHAT = newValue;
            setPrefString(PREF_ROOM_NAME_CHAT, newValue, activity);
        }
    }

    /**
     * Set Config value.
     * If value is null or "", set to Default value.
     * Write to Preferences only if new value differs from current value.
     *
     * @param newValue
     * @param activity
     */
    public static void setRoomNameData(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = ROOM_NAME_DATA_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(ROOM_NAME_DATA)) {
            ROOM_NAME_DATA = newValue;
            setPrefString(PREF_ROOM_NAME_DATA, newValue, activity);
        }
    }

    /**
     * Set Config value.
     * If value is null or "", set to Default value.
     * Write to Preferences only if new value differs from current value.
     *
     * @param newValue
     * @param activity
     */
    public static void setRoomNameFile(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = ROOM_NAME_FILE_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(ROOM_NAME_FILE)) {
            ROOM_NAME_FILE = newValue;
            setPrefString(PREF_ROOM_NAME_FILE, newValue, activity);
        }
    }

    /**
     * Set Config value.
     * If value is null or "", set to Default value.
     * Write to Preferences only if new value differs from current value.
     *
     * @param newValue
     * @param activity
     */
    public static void setRoomNameMultiVideos(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = ROOM_NAME_MULTI_VIDEOS_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(ROOM_NAME_MULTI_VIDEOS)) {
            ROOM_NAME_MULTI_VIDEOS = newValue;
            setPrefString(PREF_ROOM_NAME_MULTI_VIDEOS, newValue, activity);
        }
    }

    /**
     * Set Config value.
     * If value is null or "", set to Default value.
     * Write to Preferences only if new value differs from current value.
     *
     * @param newValue
     * @param activity
     */
    public static void setRoomNameVideo(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = ROOM_NAME_VIDEO_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(ROOM_NAME_VIDEO)) {
            ROOM_NAME_VIDEO = newValue;
            setPrefString(PREF_ROOM_NAME_VIDEO, newValue, activity);
        }
    }
}
