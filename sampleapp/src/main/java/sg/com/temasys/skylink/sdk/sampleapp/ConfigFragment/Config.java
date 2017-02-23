package sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import sg.com.temasys.skylink.sdk.sampleapp.R;

import static sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigRoomFragment.PREF_ROOM_NAME_AUDIO;
import static sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigRoomFragment.PREF_ROOM_NAME_CHAT;
import static sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigRoomFragment.PREF_ROOM_NAME_DATA;
import static sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigRoomFragment.PREF_ROOM_NAME_FILE;
import static sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigRoomFragment.PREF_ROOM_NAME_PARTY;
import static sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigRoomFragment.PREF_ROOM_NAME_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigRoomFragment.PREF_USER_NAME_AUDIO;
import static sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigRoomFragment.PREF_USER_NAME_CHAT;
import static sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigRoomFragment.PREF_USER_NAME_DATA;
import static sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigRoomFragment.PREF_USER_NAME_FILE;
import static sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigRoomFragment.PREF_USER_NAME_PARTY;
import static sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigRoomFragment.PREF_USER_NAME_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.Constants.ROOM_NAME_AUDIO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.Constants.ROOM_NAME_CHAT_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.Constants.ROOM_NAME_DATA_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.Constants.ROOM_NAME_FILE_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.Constants.ROOM_NAME_PARTY_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.Constants.ROOM_NAME_VIDEO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.Constants.USER_NAME_AUDIO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.Constants.USER_NAME_CHAT_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.Constants.USER_NAME_DATA_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.Constants.USER_NAME_FILE_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.Constants.USER_NAME_PARTY_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.Constants.USER_NAME_VIDEO_DEFAULT;

/**
 * Created by phyo.pwint on 27/7/16.
 */

public class Config {

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
    public static String ROOM_NAME_PARTY = ROOM_NAME_PARTY_DEFAULT;
    public static String ROOM_NAME_VIDEO = ROOM_NAME_VIDEO_DEFAULT;

    // Config values for user names.
    public static String USER_NAME_AUDIO = USER_NAME_AUDIO_DEFAULT;
    public static String USER_NAME_CHAT = USER_NAME_CHAT_DEFAULT;
    public static String USER_NAME_DATA = USER_NAME_DATA_DEFAULT;
    public static String USER_NAME_FILE = USER_NAME_FILE_DEFAULT;
    public static String USER_NAME_PARTY = USER_NAME_PARTY_DEFAULT;
    public static String USER_NAME_VIDEO = USER_NAME_VIDEO_DEFAULT;

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
        final SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
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
        final SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        // Populate room and user names with values from Preferences if they exist.
        ROOM_NAME_AUDIO = sharedPref.getString(PREF_ROOM_NAME_AUDIO, ROOM_NAME_AUDIO);
        ROOM_NAME_CHAT = sharedPref.getString(PREF_ROOM_NAME_CHAT, ROOM_NAME_CHAT);
        ROOM_NAME_DATA = sharedPref.getString(PREF_ROOM_NAME_DATA, ROOM_NAME_DATA);
        ROOM_NAME_FILE = sharedPref.getString(PREF_ROOM_NAME_FILE, ROOM_NAME_FILE);
        ROOM_NAME_PARTY = sharedPref.getString(PREF_ROOM_NAME_PARTY, ROOM_NAME_PARTY);
        ROOM_NAME_VIDEO = sharedPref.getString(PREF_ROOM_NAME_VIDEO, ROOM_NAME_VIDEO);

        USER_NAME_AUDIO = sharedPref.getString(PREF_USER_NAME_AUDIO, USER_NAME_AUDIO);
        USER_NAME_CHAT = sharedPref.getString(PREF_USER_NAME_CHAT, USER_NAME_CHAT);
        USER_NAME_DATA = sharedPref.getString(PREF_USER_NAME_DATA, USER_NAME_DATA);
        USER_NAME_FILE = sharedPref.getString(PREF_USER_NAME_FILE, USER_NAME_FILE);
        USER_NAME_PARTY = sharedPref.getString(PREF_USER_NAME_PARTY, USER_NAME_PARTY);
        USER_NAME_VIDEO = sharedPref.getString(PREF_USER_NAME_VIDEO, USER_NAME_VIDEO);
    }

    /**
     * Get Preferences for specified activity and set boolean value for specific key.
     *
     * @param key
     * @param value
     * @param activity
     */
    public static void setPrefBoolean(String key, boolean value, Activity activity) {
        final SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value).commit();
    }

    /**
     * Get Preferences for specified activity and set String value for specific key.
     *
     * @param key
     * @param value
     * @param activity
     */
    public static void setPrefString(String key, String value, Activity activity) {
        final SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value).commit();
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
    public static void setUserNameParty(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = USER_NAME_PARTY_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(USER_NAME_PARTY)) {
            USER_NAME_PARTY = newValue;
            setPrefString(PREF_USER_NAME_PARTY, newValue, activity);
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
    public static void setRoomNameParty(String newValue, Activity activity) {
        if (newValue == null || "".equals(newValue)) {
            newValue = ROOM_NAME_PARTY_DEFAULT;
        }
        // Write to Config and Preferences only if value changed.
        if (!newValue.equals(ROOM_NAME_PARTY)) {
            ROOM_NAME_PARTY = newValue;
            setPrefString(PREF_ROOM_NAME_PARTY, newValue, activity);
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
