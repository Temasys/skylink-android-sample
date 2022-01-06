package sg.com.temasys.skylink.sdk.sampleapp.service.model;

import androidx.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Henry on 16/8/2.
 */

public class KeyInfo {

    private static final String TAG = KeyInfo.class.getName();
    public static final String KEY = "key";
    public static final String SECRET = "secret";
    public static final String DESC = "desc";
    public static final String IS_SMR = "isSmr";

    private String key;
    private String secret;
    private String desc;
    private boolean isSmr;


    public KeyInfo() {
    }

    public KeyInfo(String key, String secret, String description, boolean isSmr) {
        this.key = key;
        this.secret = secret;
        this.desc = description;
        this.isSmr = isSmr;
    }

    public KeyInfo(JSONObject keyInfoJson) {
        if (keyInfoJson == null) {
            return;
        }

        String key = getKey(keyInfoJson);
        String secret = getSecret(keyInfoJson);
        String description = getDesc(keyInfoJson);
        boolean isSmr = isSmr(keyInfoJson);

        this.key = key;
        this.secret = secret;
        this.desc = description;
        this.isSmr = isSmr;
    }

    //----------------------------------------------------------------------------------------------
    // APIs
    //----------------------------------------------------------------------------------------------

    /**
     * Returns the JSONObject representation of this KeyInfo
     *
     * @return
     */
    public JSONObject getJson() {
        JSONObject keyInfoJson = new JSONObject();
        jsonPutKeyInfo(keyInfoJson, key, secret, desc, isSmr);
        return keyInfoJson;
    }

    /**
     * Get the App key from a KeyInfo JSONObject.
     *
     * @param json
     * @return The App key, or null if it can't be found.
     */
    public static String getKey(JSONObject json) {
        return getStringFromKeyInfoJson(json, KEY);
    }

    /**
     * Get the App key secret from a KeyInfo JSONObject.
     *
     * @param json
     * @return The App key secret, or null if it can't be found.
     */
    public static String getSecret(JSONObject json) {
        return getStringFromKeyInfoJson(json, SECRET);
    }

    /**
     * Get the App key description from a KeyInfo JSONObject.
     *
     * @param json
     * @return The App key description, or null if it can't be found.
     */
    public static String getDesc(JSONObject json) {
        return getStringFromKeyInfoJson(json, DESC);
    }

    /**
     * Get the App key isSmr from a KeyInfo JSONObject.
     *
     * @param json
     * @return The App key isSmr, or false if it can't be found.
     */
    public static boolean isSmr(JSONObject json) {
        return getBooleanFromKeyInfoJson(json, IS_SMR);
    }

    @Nullable
    /**
     * Get a KeyInfo property from its JSONObject, based on the key value.
     */
    public static String getStringFromKeyInfoJson(JSONObject keyInfoJson, String property) {
        try {
            return keyInfoJson.getString(property);
        } catch (JSONException e) {
            Log.e(TAG, "[getKey] Error: " + e.getMessage() + ".");
            return null;
        }
    }

    /**
     * Puts KeyInfo info into the provided JSONObject.
     *
     * @param jsonKeyInfo
     * @param key
     * @param secret
     * @param desc
     * @param isSmr
     * @throws JSONException
     */
    public static void jsonPutKeyInfo(JSONObject jsonKeyInfo,
                                      String key, String secret, String desc, boolean isSmr) {
        try {
            jsonKeyInfo.put(KEY, key);
            jsonKeyInfo.put(SECRET, secret);
            jsonKeyInfo.put(DESC, desc);
            jsonKeyInfo.put(IS_SMR, isSmr);
        } catch (JSONException e) {
            Log.e(TAG, "[getJson] Error: " + e.getMessage() + ".");
        }
    }

    /**
     * Get a KeyInfo property from its JSONObject, based on the key value.
     */
    public static boolean getBooleanFromKeyInfoJson(JSONObject keyInfoJson, String property) {
        try {
            return keyInfoJson.getBoolean(property);
        } catch (JSONException e) {
            Log.e(TAG, "[getKey] Error: " + e.getMessage() + ".");
            return false;
        }
    }

    //----------------------------------------------------------------------------------------------
    // Getters & Setters
    //----------------------------------------------------------------------------------------------

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isSmr() {
        return isSmr;
    }

    public void setSmr(boolean smr) {
        this.isSmr = smr;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public String toString() {
        return getJson().toString();
    }
}
