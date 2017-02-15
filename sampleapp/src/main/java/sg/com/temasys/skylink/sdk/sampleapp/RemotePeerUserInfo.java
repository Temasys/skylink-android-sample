package sg.com.temasys.skylink.sdk.sampleapp;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by janidu on 13/4/15.
 */
public class RemotePeerUserInfo {

    private final String TAG = RemotePeerUserInfo.class.getName();
    private int videoHeight;
    private int videoWidth;
    private int videoFps;
    private boolean audioStereo;

    public RemotePeerUserInfo() {
    }

    public RemotePeerUserInfo(JSONObject jsonObject) {
        if (jsonObject != null && jsonObject.has("settings")) {
            try {
                JSONObject settings = jsonObject.getJSONObject("settings");
                audioStereo = settings.getJSONObject("audio").getBoolean("stereo");

                JSONObject videoRes = settings.getJSONObject("video").getJSONObject("resolution");
                videoHeight = videoRes.getInt("height");
                videoWidth = videoRes.getInt("width");

                videoFps = settings.getJSONObject("video").getInt("frameRate");
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
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
}
