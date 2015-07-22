package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

/**
 * Created by xiangrong on 5/5/15.
 */


// TODO: Add unit tests.

/**
 * Object to keep track of Peer information.
 */
public class UserInfo {

    private final String TAG = UserInfo.class.getName();
    private boolean audioSend;
    private boolean videoSend;
    private int videoHeight;
    private int videoWidth;
    private int videoFps;
    private boolean audioStereo;
    private boolean audioMuted;
    private boolean videoMuted;
    private Object userData;

    /**
     * Create UserInfo using setter methods (if required).
     *
     * @return UserInfo object
     */
    public UserInfo() {
        userData = "";
    }

    /**
     * Create UserInfo from JSONObject
     *
     * @param jsonObject The userInfo JSONObject from which to get settings for the userInfo.
     * @return UserInfo object
     */
    public UserInfo(JSONObject jsonObject) {
        if (jsonObject != null && jsonObject.has("settings")) {
            try {
                JSONObject settings = jsonObject.getJSONObject("settings");

                Object audio = settings.get("audio");
                // audio is either false or an object.
                if (audio != null && !(audio instanceof Boolean)) {
                    audioStereo = settings.getJSONObject("audio").getBoolean("stereo");
                }

                // video is either false or an object.
                Object video = settings.get("video");
                if (video != null && !(video instanceof Boolean)) {
                    JSONObject videoRes = settings.getJSONObject("video").getJSONObject("resolution");
                    videoHeight = videoRes.getInt("height");
                    videoWidth = videoRes.getInt("width");
                    videoFps = settings.getJSONObject("video").getInt("frameRate");
                }

                audioMuted = jsonObject.getJSONObject("mediaStatus").getBoolean("audioMuted");
                videoMuted = jsonObject.getJSONObject("mediaStatus").getBoolean("videoMuted");
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }

        if (jsonObject != null && jsonObject.has("userData")) {
            try {
                userData = jsonObject.get("userData");
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    /**
     * Create UserInfo from SkylinkConfig and userData object
     *
     * @param skylinkConfig The SkylinkConfig from which to get settings for the userInfo.
     * @param userData      The String or JSONObject containing user related information.
     * @return UserInfo object
     */
    public UserInfo(SkylinkConfig skylinkConfig, Object userData) {
        // Set audio settings
        if (skylinkConfig.hasAudioSend()) {
            setAudioSend(true);
            setAudioStereo(skylinkConfig.isStereoAudio());
        }
        // Set video settings
        if (skylinkConfig.hasVideoSend()) {
            setVideoSend(true);
            setVideoFps(skylinkConfig.getVideoFps());
            setVideoHeight(skylinkConfig.getVideoHeight());
            setVideoWidth(skylinkConfig.getVideoWidth());
        }
        // Set userData
        setUserData(userData == null ? "" : userData);

        // NOTE XR: dictBandwidth object is not being used.
        // Commented out for now.
        // Consider removing code.
        /*JSONObject dictBandwidth = new JSONObject();
        if (skylinkConfig.hasAudioSend())
            dictBandwidth.put("audio", settingsObject.audio_bandwidth);
        if (skylinkConfig.hasVideoSend())
            dictBandwidth.put("video", settingsObject.video_bandwidth);
        if (skylinkConfig.hasPeerMessaging() || skylinkConfig.hasFileTransfer())
            dictBandwidth.put("data", settingsObject.data_bandwidth);*/
    }

    /**
     * Returns a JSONObject representing this UserInfo.
     *
     * @return JSONObject of UserInfo
     */
    public JSONObject getJson() {
        JSONObject dictUserInfo = new JSONObject();
        JSONObject dictSettings = new JSONObject();
        JSONObject dictAudio = new JSONObject();
        JSONObject dictVideo = new JSONObject();
        JSONObject resolution = new JSONObject();
        JSONObject dictMediaStatus = new JSONObject();

        try {
            // Media Settings
            dictUserInfo.put("settings", dictSettings);

            // - Audio properties
            if (hasAudioSend()) {
                dictSettings.put("audio", dictAudio);
                dictAudio.put("stereo", isAudioStereo());
            } else {
                dictSettings.put("audio", false);
            }

            // - Video properties
            if (hasVideoSend()) {
                dictSettings.put("video", dictVideo);
                // Resolution
                dictVideo.put("resolution", resolution);
                resolution.put("height", getVideoHeight());
                resolution.put("width", getVideoWidth());
                // Frame rate
                dictVideo.put("frameRate", getVideoFps());
            } else {
                dictSettings.put("video", false);
            }

            // Mediastream status
            dictUserInfo.put("mediaStatus", dictMediaStatus);
            // - AudioTrack
            dictMediaStatus.put("audioMuted", isAudioMuted());
            // - VideoTrack
            dictMediaStatus.put("videoMuted", isVideoMuted());

            // User information (can be string or JSON)
            dictUserInfo.put("userData", userData == null ? "" : userData);

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return dictUserInfo;
    }

    /**
     * Checks if the values of a UserInfo object are the same as this one.
     *
     * @param userInfo The UserInfo that is being compared.
     * @return true only if all values are the same as this one.
     */
    public boolean equals(UserInfo userInfo) {
        // Compare video sending
        if (videoSend != userInfo.hasVideoSend()) {
            return false;
        } else if (videoSend) {
            // If sending video,
            // - Compare video resolution
            if (videoHeight != userInfo.getVideoHeight() || videoWidth != userInfo.getVideoWidth() || videoFps != userInfo.getVideoFps()) {
                return false;
            }
        }

        // Compare audio sending
        if (audioSend != userInfo.hasAudioSend()) {
            return false;
        } else if (audioSend) {
            // If sending audio,
            // - Compare audio stereo
            if (audioStereo != userInfo.isAudioStereo()) {
                return false;
            }
        }

        // Compare media status
        if (audioMuted != userInfo.isAudioMuted() || videoMuted != userInfo.isVideoMuted()) {
            return false;
        }
        return true;
    }

    /**
     * Set a UserInfo object into a JSONObject.
     *
     * @param jsonObject The JSONObject in which to set a userInfo object
     * @param userInfo   The UserInfo that should be set.
     * @return
     */
    public static void setUserInfo(JSONObject jsonObject, UserInfo userInfo) throws JSONException {

        JSONObject userInfoJson = userInfo.getJson();
        jsonObject.put("userInfo", userInfoJson);
    }

    public boolean hasVideoSend() {
        return videoSend;
    }

    public void setVideoSend(boolean videoSend) {
        this.videoSend = videoSend;
    }

    public boolean hasAudioSend() {
        return audioSend;
    }

    public void setAudioSend(boolean audioSend) {
        this.audioSend = audioSend;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoFps() {
        return videoFps;
    }

    public void setVideoFps(int videoFps) {
        this.videoFps = videoFps;
    }

    public boolean isAudioStereo() {
        return audioStereo;
    }

    public void setAudioStereo(boolean audioStereo) {
        this.audioStereo = audioStereo;
    }

    public Object getUserData() {
        return userData;
    }

    public void setUserData(Object userData) {
        this.userData = userData;
    }

    public boolean isAudioMuted() {
        return audioMuted;
    }

    public void setAudioMuted(boolean audioMuted) {
        this.audioMuted = audioMuted;
    }

    public boolean isVideoMuted() {
        return videoMuted;
    }

    public void setVideoMuted(boolean videoMuted) {
        this.videoMuted = videoMuted;
    }

}
