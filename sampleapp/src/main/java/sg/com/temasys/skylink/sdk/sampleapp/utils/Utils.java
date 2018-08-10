package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SurfaceViewRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.KeyInfo;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.VideoResolution;

import static sg.com.temasys.skylink.sdk.rtc.Info.CAM_SWITCH_FRONT;
import static sg.com.temasys.skylink.sdk.rtc.Info.CAM_SWITCH_NON_FRONT;
import static sg.com.temasys.skylink.sdk.rtc.Info.PERM_AUDIO_MIC;
import static sg.com.temasys.skylink.sdk.rtc.Info.PERM_STORAGE_READ;
import static sg.com.temasys.skylink.sdk.rtc.Info.PERM_STORAGE_WRITE;
import static sg.com.temasys.skylink.sdk.rtc.Info.PERM_VIDEO_CAM;
import static sg.com.temasys.skylink.sdk.rtc.Info.getInfoString;

public class Utils {

    public static final String TIME_ZONE_UTC = "UTC";
    public static final String ISO_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ";
    private static final String TAG = Utils.class.getName();
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String EXTERNAL_STORAGE = "ExternalStorage";

    private static Context mContext;

    /**
     * The last Toast made with {@link #toastLog};
     */
    private static Toast toast;

    public Utils(Context context){
        this.mContext = context;
    }

    /**
     * Returns the userData of a Peer as a String.
     * If there is no userData, returns the empty string, "".
     *
     * @param peerId The PeerId for which to search. Use null for self (local Peer).
     * @return
     */
    public static String getUserDataString(SkylinkConnection skylinkConnection, String peerId) {
        Object userDataObject = skylinkConnection.getUserData(peerId);
        String userDataString = "";
        if (userDataObject != null) {
            userDataString = userDataObject.toString();
        }
        return userDataString;
    }

    /**
     * Returns the nickname of a Peer as the userData as a string.
     * If there is no userData, return the PeerId.
     *
     * @param peerId
     * @return
     */
    public static String getNick(SkylinkConnection skylinkConnection, String peerId) {
        String nick = getUserDataString(skylinkConnection, peerId);
        if ("".equals(nick)) {
            nick = peerId;
        }
        return nick;
    }

    /**
     * Returns the SkylinkConnectionString, which MUST BE URL SAFE.
     * Required inputs are: App key, App secret, Room name, Room start time, and Room duration.
     *
     * @param roomName  Name of the room
     * @param startTime Room Start Time
     * @param duration  Duration of the room in Hours
     * @return
     */
    public static String getSkylinkConnectionString(String roomName, Date startTime, int duration) {

        String info = "Room name: " + roomName + ", startTime: " + startTime +
                ", duration: " + duration + ".\r\n";

        // Convert the date in to ISO format
        String dateString = Utils.getISOTimeStamp(startTime);

        // Compute RFC 2104-compliant HMAC signature
        String cred = calculateRFC2104HMAC(roomName + "_" + duration + "_"
                + dateString, Config.getAppKeySecret());
        try {
            cred = URLEncoder.encode(cred, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            String error = "[ERROR] Unable to encode credentials. Not joining room!"
                    + "\nDetails: Could not URLEncode generated credentials."
                    + "\nException: " + e.getMessage();
            Log.e(TAG, error);
            return null;
        }

        // Ensure connectionString is url safe
        // This is very IMPORTANT!!!
        // A connectionString that is not url safe will likely generate error(s)
        // and not be able to connect to room.
        String urlStart = "http://host/";
        String connectionString = urlStart + Config.getAppKey() + "/"
                + roomName + "/" + dateString + "/" + duration;
        info += "Precursor connectionString: \"" + connectionString + "\"\r\n";
        URL urlObject = null;
        URI uriObject = null;
        try {
            urlObject = new URL(connectionString);
            uriObject =
                    new URI(urlObject.getProtocol(), urlObject.getUserInfo(), urlObject.getHost(),
                            urlObject.getPort(), urlObject.getPath(),
                            urlObject.getQuery(), urlObject.getRef());
        } catch (MalformedURLException e) {
            info += "Error: Could not create URL safe connectionString:\r\n" + e.getMessage();
            Log.e(TAG, info);
            return null;
        } catch (URISyntaxException e) {
            info += "Error: Could not create URL safe connectionString:\r\n" + e.getMessage();
            Log.e(TAG, info);
            return null;
        }

        // Add credentials to string.
        String uriString = uriObject.toString();
        /* Parse connectionString from:
        http://host/<connectionString>
        */
        connectionString = uriString.substring(urlStart.length(), uriString.length())
                + "?cred=" + cred;
        info += "URL safe connectionString: \"" + connectionString + "\"";
        Log.d(TAG, info);

        return connectionString;
    }

    /**
     * Computes RFC 2104-compliant HMAC signature.
     *
     * @param data data The data to be signed.
     * @param key  The signing key.
     * @return The Base64-encoded RFC 2104-compliant HMAC signature.
     */
    public static String calculateRFC2104HMAC(String data, String key) {
        String result = null;
        try {

            // Get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(),
                    HMAC_SHA1_ALGORITHM);

            // Get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            // Compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());

            // Base64-encode the hmac
            result = Base64
                    .encodeToString(rawHmac, Base64.DEFAULT);

        } catch (Exception e) {
            Log.e(TAG, "Failed to generate HMAC : " + e.getMessage(), e);
        }
        return result.substring(0, result.length() - 1);
    }

    /**
     * Returns the date in ISO time format
     *
     * @param date
     * @return ISO timestamp
     */
    public static String getISOTimeStamp(Date date) {
        TimeZone tz = TimeZone.getTimeZone(TIME_ZONE_UTC);
        DateFormat df = new SimpleDateFormat(ISO_TIME_FORMAT);
        df.setTimeZone(tz);
        return df.format(date);
    }

    /**
     * Remove video from containing layout, if any.
     *
     * @param videoView
     */
    public static void removeViewFromParent(SurfaceViewRenderer videoView) {
        if (videoView != null) {
            Object viewParent = videoView.getParent();
            if (viewParent != null) {
                // If parent is a ViewGroup, remove from parent.
                if (ViewGroup.class.isInstance(viewParent)) {
                    ((ViewGroup) viewParent).removeView(videoView);
                }
            }
        }
    }

    /**
     * Return a new JSONArray like the one provided,
     * except less the element at the specified position.
     * JSONArray.remove is not available for API levels before 19.
     *
     * @param arrayOld
     * @param position
     * @return
     */
    public static JSONArray jsonArrayRemove(JSONArray arrayOld, int position) {
        JSONArray arrayNew = new JSONArray();
        Log.e("Position", String.valueOf(position));
        try {
            int len = arrayOld.length();
            if (arrayOld != null) {
                for (int i = 0; i < len; i++) {
                    if (i != position) {
                        JSONObject joOld = arrayOld.getJSONObject(i);
                        JSONObject joNew = new KeyInfo(joOld).getJson();
                        arrayNew.put(joNew);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String log = "[jsonArrayRemove] Array old:\r\n" + arrayOld + ".\r\n" +
                "[jsonArrayRemove] Array new:\r\n" + arrayNew + ".";
        Log.d(TAG, log);
        return arrayNew;
    }

    public static List<KeyInfo> convertJSONArrayToKeyInfoList(JSONArray ja) {
        List keyInfoList = new ArrayList<>();
        for (int i = 0; i < ja.length(); i++) {
            try {
                JSONObject jo = ja.getJSONObject(i);
                KeyInfo ki = new KeyInfo(jo);
                keyInfoList.add(ki);
            } catch (JSONException e) {
                Log.e(TAG, "[convertJSONArrayToKeyInfoList] Error: " + e.getMessage() + ".");
            }
        }
        return keyInfoList;
    }

    /**
     * Checks if an App Key exists in a JSONArray of KeyInfo JSONObject.
     *
     * @param key
     * @param keyList
     * @return The position (array index) of the App Key if it exists,
     * or else a negative nubmer if it does not.
     */
    public static int getKeyPosition(String key, JSONArray keyList) {
        if (key == null) {
            return -1;
        }

        for (int i = 0; i < keyList.length(); ++i) {
            JSONObject keyInfoJson = null;
            try {
                keyInfoJson = keyList.getJSONObject(i);
            } catch (JSONException e) {
                Log.e(TAG, "[getKeyPosition] Error: " + e.getMessage() + ".");
                continue;
            }
            if (keyInfoJson != null) {
                if (key.equals(KeyInfo.getKey(keyInfoJson))) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static boolean checkAppKeyAndSecret(String key, String secret) {
        boolean correct = false;
        if (key.length() == 36 && secret.length() == 13) {
            correct = true;
        }
        return correct;
    }

    /**
     * This is a convenience method to set some common SkylinkConfig options.
     *
     * @param skylinkConfig
     */
    public static SkylinkConfig skylinkConfigCommonOptions(SkylinkConfig skylinkConfig) {
/*
        // To limit audio/video/data bandwidth:
        skylinkConfig.setMaxAudioBitrate(20);  // Default is not limited.
        skylinkConfig.setMaxVideoBitrate(256); // Default is 512 kbps.
        skylinkConfig.setMaxDataBitrate(30);   // Default is not limited.
*/
/*
        // To NOT limit audio/video/data bandwidth:
        // Audio and Data by default are already not limited.
        skylinkConfig.setMaxVideoBitrate(-1); // Default is 512 kbps.
*/
/*
        // To set the start up camera to back:
        skylinkConfig.setDefaultVideoDevice(SkylinkConfig.VideoDevice.CAMERA_BACK);
        // By default, the default VideoDevice is the front camera.
*/
/*
        // To set local video resolution (only use those supported by camera):
        skylinkConfig.setVideoHeight(SkylinkConfig.VIDEO_HEIGHT_HDR); // Default is 480 (VGA).
        skylinkConfig.setVideoWidth(SkylinkConfig.VIDEO_WIDTH_HDR);   // Default is 640 (VGA).
*/
/*
        // To enable logs from Skylink SDK (e.g. during debugging),
        // Uncomment the following. Do not enable logs for production apps!
        skylinkConfig.setEnableLogs(true);
*/
        skylinkConfig.setTimeout(Constants.TIME_OUT);

        return skylinkConfig;
    }

    /**
     * Log and Toast some info provided by SkylinkConnection.
     *
     * @param infoCode
     * @param message
     * @param context
     * @param tag
     */
    public static void handleSkylinkReceiveLog(int infoCode, String message,
                                               Context context, String tag) {
        String log = "[SA][SkylinkLog] " + message;
        switch (infoCode) {
            case CAM_SWITCH_FRONT:
            case CAM_SWITCH_NON_FRONT:
                toastLog(TAG, context, log);
                break;
            default:
                break;
        }
    }

    /**
     * Log and Toast warning provided by SkylinkConnection.
     *
     * @param errorCode
     * @param message
     * @param context
     * @param tag
     */
    public static void handleSkylinkWarning(int errorCode, String message, Context context,
                                            String tag) {
        String log = "[SA][SkylinkWarn] Error:" + errorCode + " (" +
                Errors.getErrorString(errorCode) + ")\r\n" + message;
        toastLog(tag, context, log);
        Log.w(tag, log);
    }

    /**
     * Will cancel the previous {@link #toastLog} attempt to Toast if still ongoing, and
     * Toast the given log with the given Toast length.
     * Will also Log.d the given log.
     *
     * @param context
     * @param log
     */
    synchronized public static void toastLog(String tag, Context context, String log, int toastLength) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, log, toastLength);
        toast.show();
        Log.d(TAG, log);
    }

    /**
     * Like {@link #toastLog} but with Toast.LENGTH_SHORT.
     *
     * @param tag
     * @param context
     * @param log
     */
    public static void toastLog(String tag, Context context, String log) {
        toastLog(tag, context, log, Toast.LENGTH_SHORT);
    }

    /**
     * Like {@link #toastLog} but with Toast.LENGTH_LONG.
     *
     * @param tag
     * @param context
     * @param log
     */
    public static void toastLogLong(String tag, Context context, String log) {
        toastLog(tag, context, log, Toast.LENGTH_LONG);
    }

    /**
     * @param fileName String to be used as the name of the file to be created.
     * @return File to be transferred from default directory (Pictures directory).
     */
    public static File getFileToTransfer(String fileName) {
        File path = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(path, fileName);
    }

    /**
     * @param fileIn   File to be copied as a resource id.
     * @param fileCopy
     */
    public static void copyFile(int fileIn, File fileCopy) {
        try {
            // Very simple code to copy a picture from the application's
            // resource into the external file.  Note that this code does
            // no error checking, and assumes the picture is small (does not
            // try to copy it in chunks).  Note that if external storage is
            // not currently mounted this will silently fail.
            InputStream is = mContext.getResources().openRawResource(fileIn);
            OutputStream os = new FileOutputStream(fileCopy);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(mContext,
                    new String[]{fileCopy.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i(EXTERNAL_STORAGE, "Scanned " + path + ":");
                            Log.i(EXTERNAL_STORAGE, "-> uri=" + uri);
                        }
                    });
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w(EXTERNAL_STORAGE, "Error writing " + fileCopy, e);
        }
    }

    /**
     * Return a localized string from the application's package's
     * default string table.
     *
     * @param resId Resource id for the string
     */
    public static final String getString(@StringRes int resId) {
        return mContext.getResources().getString(resId);
    }

    /**
     * Checks if given {@link SkylinkCaptureFormat SkylinkCaptureFormat[]} is valid for using.
     * To be valid, it cannot be null or empty.
     *
     * @param captureFormats
     * @return
     */
    public static boolean isCaptureFormatsValid(SkylinkCaptureFormat[] captureFormats) {
        if (captureFormats == null || captureFormats.length == 0) {
            return false;
        }
        return true;
    }

    @NonNull
    /**
     * Return a description of each CaptureFormat in the Array of CaptureFormats provided.
     */
    public static String captureFormatsToString(SkylinkCaptureFormat[] captureFormats) {
        if (!isCaptureFormatsValid(captureFormats)) {
            return null;
        }
        String formats = "";
        for (SkylinkCaptureFormat supportedFormat : captureFormats) {
            formats += "\r\n" + supportedFormat.toString();
        }
        return formats;
    }

    /**
     * Get the frame rate (fps) that should be selected, for a given {@link SkylinkCaptureFormat}.
     * Use the given fps if supported by given CaptureFormat, else use the new max fps.
     *
     * @param fps    Frame rate that should be selected if possible.
     * @param format {@link SkylinkCaptureFormat} that defines the possible frame rate range.
     * @return The appropriate fps, or a negative number if the given CaptureFormat is invalid.
     */
    public static int getFpsForNewCaptureFormat(int fps, SkylinkCaptureFormat format) {

        // Check if given CaptureFormat is valid.
        if (!isCaptureFormatValid(format)) {
            return -1;
        }

        int fpsMinNew = format.getFpsMin();
        int fpsMaxNew = format.getFpsMax();

        // Set new fps UI max value if the current one is out of the new range.
        if (fps < fpsMinNew || fps > fpsMaxNew) {
            return fpsMaxNew;
        }
        return fps;
    }

    /**
     * Checks if given {@link SkylinkCaptureFormat} is valid for using.
     * To be valid it cannot be null, and the fps range cannot be negative.
     *
     * @param format
     * @return True if valid and false if not.
     */
    public static boolean isCaptureFormatValid(SkylinkCaptureFormat format) {
        if (format == null) {
            return false;
        }

        // Check fps range based on min and max fps of this CaptureFormat.
        int range = format.getFpsMax() - format.getFpsMin();
        if (range < 0 || format.getFpsMin() < 0) {
            return false;
        }
        return true;
    }

    /**
     * Set dataGroup to contain 2 of dataPrivate.
     * Will get dataPrivate if dataGroup and dataPrivate are null.
     */
    public static byte[] getDataGroup() {
        byte[] dataGroup = null;

        byte[] dataPrivate = getDataPrivate();

        int len = dataPrivate.length;
        dataGroup = new byte[2 * len];
        System.arraycopy(dataPrivate, 0, dataGroup, 0, len);
        System.arraycopy(dataPrivate, 0, dataGroup, len, len);

        return dataGroup;
    }

    /**
     * Read an image to a byte array and put in dataPrivate
     */
    public static byte[] getDataPrivate() {
        byte[] dataPrivate = null;

        InputStream inputStream = mContext.getResources().openRawResource(R.raw.icon);
        try {
            dataPrivate = new byte[inputStream.available()];
            inputStream.read(dataPrivate);
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return dataPrivate;
    }

    /**
     * Creates a dummy file from the apk's asset folder to the device's filepath so that there is a
     * default file to transfer
     */
    public static void createExternalStoragePrivatePicture(String filenamePrivate, String filenameGroup) {
        // Create a path where we will place our pictures in our own private
        // pictures directory.  Note that we don't really need to place a
        // picture in DIRECTORY_PICTURES, since the media scanner will see
        // all media in these directories; this may be useful with other
        // media types such as DIRECTORY_MUSIC however to help it classify
        // your media for display to the user.

        // Files to be created on device...
        File fileCopy1 = getFileToTransfer(filenamePrivate);
        File fileCopy2 = getFileToTransfer(filenameGroup);

        // ...copied from resource files here:
        int fileIn1 = R.raw.icon;
        int fileIn2 = R.raw.icon_group;

        // Copy resource files into files on Device's directory.
        copyFile(fileIn1, fileCopy1);
        copyFile(fileIn2, fileCopy2);
    }

    /**
     * Generate a string to display frame rate in fps.
     *
     * @param fps Framerate in frames per seconds.
     * @return
     */
    @NonNull
    public static String getResFpsStr(int fps) {
        return fps + " fps";
    }

    @NonNull
    public static String getResFpsStr(VideoResolution videoResolution) {
        return videoResolution.getFps() + " fps";
    }

    /**
     * Generate a string to display a set of video resolution dimensions (i.e. width and height).
     *
     * @param width
     * @param height
     * @return
     */
    @NonNull
    public static String getResDimStr(int width, int height) {
        return width + " x " + height;
    }

    /**
     * Generate a string to display a set of video resolution dimensions (i.e. width and height).
     *
     * @param videoResolution
     * @return
     */
    @NonNull
    public static String getResDimStr(VideoResolution videoResolution) {
        return videoResolution.getWidth() + " x " + videoResolution.getHeight();
    }

    public static String getRoomNameByType(Constants.CONFIG_TYPE typeCall) {
        switch (typeCall){
            case AUDIO:
                return Config.ROOM_NAME_AUDIO;
            case VIDEO:
                return Config.ROOM_NAME_VIDEO;
            case CHAT:
                return Config.ROOM_NAME_CHAT;
            case DATA:
                return Config.ROOM_NAME_DATA;
            case FILE:
                return Config.ROOM_NAME_FILE;
            case MULTI_PARTY_VIDEO:
                return Config.ROOM_NAME_PARTY;
        }

        return null;
    }

    public static String getUserNameByType(Constants.CONFIG_TYPE typeCall) {
        switch (typeCall){
            case AUDIO:
                return Config.USER_NAME_AUDIO;
            case VIDEO:
                return Config.USER_NAME_VIDEO;
            case CHAT:
                return Config.USER_NAME_CHAT;
            case DATA:
                return Config.USER_NAME_DATA;
            case FILE:
                return Config.USER_NAME_FILE;
            case MULTI_PARTY_VIDEO:
                return Config.USER_NAME_PARTY;
        }

        return null;
    }

}
