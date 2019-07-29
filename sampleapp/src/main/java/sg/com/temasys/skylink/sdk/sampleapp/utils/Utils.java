package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.KeyInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;

import static sg.com.temasys.skylink.sdk.rtc.Info.CAM_SWITCH_FRONT;
import static sg.com.temasys.skylink.sdk.rtc.Info.CAM_SWITCH_NON_FRONT;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_SPEAKER_AUDIO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_SPEAKER_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_VIDEO_DEVICE;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.DEFAULT_VIDEO_RESOLUTION;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.PREFERENCES_NAME;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_FHD;

public class Utils {

    public static final String TIME_ZONE_UTC = "UTC";
    public static final String ISO_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ";
    private static final String TAG = Utils.class.getName();
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String EXTERNAL_STORAGE = "ExternalStorage";

    // sample file name to be transfered
    public static String SAMPLE_FILE_NAME = "FileTransfer.png";
    public static String SAMPLE_DATA_NAME = "DataTransfer.png";

    // maximum data size to be transfered
    public static final int MAX_TRANSFER_SIZE = 65456;

    private static Context context;
    private static SharedPreferences sharedPref;

    /**
     * The last Toast made with {@link #toastLog};
     */
    private static Toast toast;

    public Utils(Context context) {
        this.context = context;
        sharedPref = Utils.context.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
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
     * Remove a view from containing layout, if any.
     *
     * @param view
     */
    public static void removeViewFromParent(View view) {
        if (view != null) {
            Object viewParent = view.getParent();
            if (viewParent != null) {
                // If parent is a ViewGroup, remove from parent.
                if (ViewGroup.class.isInstance(viewParent)) {
                    ((ViewGroup) viewParent).removeView(view);
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
        // To enable logs from Skylink SDK (e.g. during debugging):
        // Do not enable logs for production apps!
        skylinkConfig.setEnableLogs(true);
*/
/*
        // To force TURN:
        skylinkConfig.setAllowHost(false);
        skylinkConfig.setAllowStun(false);
*/
        skylinkConfig.setTimeout(Constants.TIME_OUT);

        // Add following advanced options during development to catch errors and for better debugging:
        // To enable logs from Skylink SDK (e.g. during debugging):
        // Do not enable logs for production apps!
        skylinkConfig.setEnableLogs(true);
        // SDK_DETAILED_LOGGING
        skylinkConfig.getAdvancedOptions().put("SdkAdvancedOption)!", new Boolean(true));
        // OFF_ANS_VIA_USER_DATA
        skylinkConfig.getAdvancedOptions().put("OffererAnswererViaUserData", new Boolean(true));
        // SDK_RUNTIME_CRASH
        skylinkConfig.getAdvancedOptions().put("SdkAdvancedOption)@", new Boolean(true));
        // SDK_LOCK_LOG
        // skylinkConfig.getAdvancedOptions().put("SdkAdvancedOption)#", new Boolean(true));

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
        File path = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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
            InputStream is = context.getResources().openRawResource(fileIn);
            OutputStream os = new FileOutputStream(fileCopy);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(context,
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
     * @return Location to save the downloaded file on the file system
     */
    public static String getDownloadedFilePath() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return path.getAbsolutePath() + File.separator + SAMPLE_FILE_NAME;
    }

    /**
     * Creates a dummy file from the apk's asset folder to the device's filepath so that there is a
     * default file to transfer
     */
    public static void createExternalStoragePrivatePicture(String fileNameSent) {
        // Create a path where we will place our pictures in our own private
        // pictures directory.  Note that we don't really need to place a
        // picture in DIRECTORY_PICTURES, since the media scanner will see
        // all media in these directories; this may be useful with other
        // media types such as DIRECTORY_MUSIC however to help it classify
        // your media for display to the user.

        // Files to be created on device...
        File fileCopy = getFileToTransfer(fileNameSent);

        // ...copied from resource files here:
        int fileIn = R.raw.logo_icon;

        // Copy resource files into files on Device's directory.
        copyFile(fileIn, fileCopy);
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
    public static byte[] getDataSample() {
        byte[] data = null;

        InputStream inputStream = context.getResources().openRawResource(R.raw.logo_icon);
        try {
            data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return data;
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
        switch (typeCall) {
            case AUDIO:
                return Config.ROOM_NAME_AUDIO;
            case CHAT:
                return Config.ROOM_NAME_CHAT;
            case DATA:
                return Config.ROOM_NAME_DATA;
            case FILE:
                return Config.ROOM_NAME_FILE;
            case MULTI_PARTY_VIDEO:
                return Config.ROOM_NAME_PARTY;
            case VIDEO:
                return Config.ROOM_NAME_VIDEO;
        }

        return null;
    }

    public static String getUserNameByType(Constants.CONFIG_TYPE typeCall) {
        switch (typeCall) {
            case AUDIO:
                return Config.USER_NAME_AUDIO;
            case CHAT:
                return Config.USER_NAME_CHAT;
            case DATA:
                return Config.USER_NAME_DATA;
            case FILE:
                return Config.USER_NAME_FILE;
            case MULTI_PARTY_VIDEO:
                return Config.USER_NAME_PARTY;
            case VIDEO:
                return Config.USER_NAME_VIDEO;
        }

        return null;
    }

    public static boolean isInternetOn() {

        if (context == null)
            return false;

        // get Connectivity Manager object to check connection
        ConnectivityManager connec =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Check for network connections
        if (connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTED ||
                connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED) {

            return true;

        } else if (
                connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.DISCONNECTED ||
                        connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.DISCONNECTED) {

            return false;
        }
        return false;
    }


    public static boolean isDefaultSpeakerSettingForAudio() {
        return sharedPref.getBoolean(DEFAULT_SPEAKER_AUDIO, true);
    }

    public static boolean isDefaultSpeakerSettingForVideo() {
        return sharedPref.getBoolean(DEFAULT_SPEAKER_VIDEO, true);
    }

    /**
     * Get the default {@link SkylinkConfig.VideoDevice} that is used.
     * {@link SkylinkConfig.VideoDevice#CAMERA_FRONT} is set and used if none are set.
     *
     * @return {@link SkylinkConfig.VideoDevice} OR null if user selected camera custom or no device
     */
    public static SkylinkConfig.VideoDevice getDefaultVideoDevice() {
        String logTag = "[Utils][getDefaultVideoDevice] ";
        String log = logTag + "The value in Shared Preference is not set!";
        /** Default VideoDevice is {@link SkylinkConfig.VideoDevice#CAMERA_FRONT} */
        SkylinkConfig.VideoDevice defaultVideoDevice = null;

        // Get string value saved in sharePref.
        String savedValue = sharedPref.getString(DEFAULT_VIDEO_DEVICE, null);

        // Using front camera as default video device if user has not set
        if (savedValue == null) {
            savedValue = Constants.DEFAULT_VIDEO_DEVICE_FRONT_CAMERA;
        }

        switch (savedValue) {
            case Constants.DEFAULT_VIDEO_DEVICE_FRONT_CAMERA:
                defaultVideoDevice = SkylinkConfig.VideoDevice.CAMERA_FRONT;
                break;
            case Constants.DEFAULT_VIDEO_DEVICE_BACK_CAMERA:
                defaultVideoDevice = SkylinkConfig.VideoDevice.CAMERA_BACK;
                break;
            case Constants.DEFAULT_VIDEO_DEVICE_SCREEN:
                defaultVideoDevice = SkylinkConfig.VideoDevice.SCREEN;
                break;
        }

        log = logTag + "Shared Preference value: " + defaultVideoDevice + ".";
        Log.d(TAG, log);

        return defaultVideoDevice;
    }

    /**
     * Get the default string value of {@link SkylinkConfig.VideoDevice} that is used.
     * {@link SkylinkConfig.VideoDevice#CAMERA_FRONT} is set and used if none are set.
     * if user set default video device as custom camera and no device, return a representative string for it
     *
     * @return
     */
    public static String getDefaultVideoDeviceString() {
        String logTag = "[Utils][getDefaultVideoDeviceString] ";
        String log = logTag + "The value in Shared Preference is not set!";
        /** Default VideoDevice is {@link SkylinkConfig.VideoDevice#CAMERA_FRONT} */
        final String cameraFront = SkylinkConfig.VideoDevice.CAMERA_FRONT.getDeviceName();

        // Get string value saved in sharePref.
        String savedValue = sharedPref.getString(DEFAULT_VIDEO_DEVICE, cameraFront);

        /** If defaultVideoDevice is not set in sharedPref, set it to {@link cameraFront} */
        if (savedValue == null) {
            Log.d(TAG, log);
            savedValue = cameraFront;
            Config.setPrefString(DEFAULT_VIDEO_DEVICE,
                    savedValue, (Activity) context);
            log = logTag + "Set Shared Preference to " + savedValue + ".";
            Log.d(TAG, log);
        }

        log = logTag + "Shared Preference value: " + savedValue + ".";
        Log.d(TAG, log);

        return savedValue;
    }

    public static String getDefaultVideoResolution() {
        return sharedPref.getString(DEFAULT_VIDEO_RESOLUTION, VIDEO_RESOLUTION_FHD);
    }

    /**
     * Get the file path from Uri
     */
    @SuppressLint("NewApi")
    public static String getFilePath(Context context, Uri uri) {
        String selection = null;
        String[] selectionArgs = null;
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                if (id.split(":").length > 1) {
                    return id.split(":")[1];
                } else {
                    uri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                }
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{
                        split[1]
                };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return uri.toString();
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isImageFile(String filePath) {
        //image file end with {"jpg", "png", "gif","jpeg"}
        if (filePath != null && (filePath.toLowerCase().endsWith(".png") ||
                (filePath.toLowerCase().endsWith(".jpg")) ||
                (filePath.toLowerCase().endsWith(".jpeg")) ||
                (filePath.toLowerCase().endsWith(".gif")))) {
            return true;
        }

        return false;
    }

    /**
     * Get the bitmap from Uri
     */
    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    /**
     * Get the data array of the file
     */
    public static byte[] getDataFromFile(File file) throws FileNotFoundException {
        byte[] data = null;

        InputStream inputStream = new FileInputStream(file);
        try {
            data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return data;
    }

    public static boolean isDefaultCameraDeviceSetting() {
        SkylinkConfig.VideoDevice videoDevice = getDefaultVideoDevice();
        if (videoDevice == null) {
            return false;
        }

        switch (videoDevice) {
            case CAMERA_FRONT:
            case CAMERA_BACK:
                return true;
        }

        return false;
    }

    public static boolean isDefaultScreenDeviceSetting() {
        SkylinkConfig.VideoDevice videoDevice = getDefaultVideoDevice();
        if (videoDevice == null) {
            return false;
        }

        switch (videoDevice) {
            case SCREEN:
                return true;
        }

        return false;
    }

    public static boolean isDefaultCustomVideoDeviceSetting() {
        String defaultVideo = getDefaultVideoDeviceString();
        if (defaultVideo.equals(Constants.DEFAULT_VIDEO_DEVICE_CUSTOM)) {
            return true;
        }

        return false;
    }

    public static boolean isDefaultNoneVideoDeviceSetting() {
        String defaultVideo = getDefaultVideoDeviceString();
        if (defaultVideo.equals(Constants.DEFAULT_VIDEO_DEVICE_NONE)) {
            return true;
        }

        return false;
    }


    /////////////////////////////Create custom video capturer from camera///////////////////////////

    enum CameraState {
        CAMERA_OPENED, CAMERA_CLOSED, CAMERA_SWITCHED
    }

    /**
     * Check if able to use android.hardware.camera2 (Lollipop and above).
     *
     * @return null if unable to perform check, e.g. if context is null.
     */
    public static Boolean isUseCamera2() {
        String logTag = "[SMS][isUseCam2] ";
        String log = logTag;

        Context applicationContext = context.getApplicationContext();
        if (applicationContext == null) {
            log += "Failed as appContext is null.";
            Log.d(TAG, log);
            return null;
        }

        Boolean result = Camera2Enumerator.isSupported(applicationContext);
        log += "Camera2 is supported: " + result + ".";
        Log.d(TAG, log);
        return result;
    }

    /**
     * Get the CameraEnumerator to use.
     * If Camera2 is supported, use Camera2Enumerator.
     * Otherwise use Camera1Enumerator.
     *
     * @return
     */
    public static CameraEnumerator getCameraEnumerator() {
        String logTag = "[SMS][getCamEnum] ";
        String log = logTag;

        Context applicationContext = context.getApplicationContext();
        if (applicationContext == null) {
            log = logTag + "Failed as appContext is null.";
            Log.d(TAG, log);
            return null;
        }

        Boolean canUseCamera2 = isUseCamera2();
        if (canUseCamera2 == null) {
            log += "Unable to get CameraEnumerator!";
            Log.d(TAG, log);
            return null;
        }

        CameraEnumerator enumerator;
        if (canUseCamera2) {
            enumerator = new Camera2Enumerator(applicationContext);
            Log.d(TAG, "Using camera2 enumerator.");
        } else {
            enumerator = new Camera1Enumerator();
            Log.d(TAG, "Using camera1 enumerator.");
        }
        return enumerator;
    }

    /**
     * Create a {@link org.webrtc.CameraVideoCapturer}.
     *
     * @param videoDevice
     * @param skylinkConnection
     * @return Null if unable to create capturer.
     */
    public static VideoCapturer createCustomVideoCapturerFromCamera(SkylinkConfig.VideoDevice videoDevice, SkylinkConnection skylinkConnection) {
        String logTag = "";
        String log;
        CameraEnumerator cameraEnumerator = getCameraEnumerator();
        if (cameraEnumerator == null) {
            log = logTag + "Unable to create cameraVideoCapturer as we could not get a CameraEnumerator!";
            Log.d(TAG, log);
            return null;
        }

        String[] cameraNames = cameraEnumerator.getDeviceNames();
        if (cameraNames == null || cameraNames.length < 1) {
            log = logTag + "Unable to create cameraVideoCapturer as no camera was detected!";
            Log.d(TAG, log);
            return null;
        }

//        String cameraName = getCameraName(videoDevice);
        String cameraName = cameraNames[1];

        if (cameraName == null) {
            log = logTag + "Unable to create cameraVideoCapturer as we could not get a camera!";
            Log.d(TAG, log);
            return null;
        }

        CameraVideoCapturer.CameraEventsHandler cameraEventsHandler =
                new CameraVideoCapturer.CameraEventsHandler() {
                    String logTag = "[SA][CameraEventsHandler] ";
                    String log;

                    CameraState cameraState = CameraState.CAMERA_CLOSED;
                    final String cameraNameNone = "No camera opened.";
                    String cameraNameCurrent = cameraNameNone;
                    String cameraNamePrevious = cameraNameNone;

                    @Override
                    public void onCameraError(String errorDescription) {
                        log = logTag + "Camera had an error! Error: " + errorDescription
                                + " from State: " + cameraState.name() + ".";
                        Log.d(TAG, log);
                    }

                    @Override
                    public void onCameraDisconnected() {
                        log = logTag + "Camera disconnected"
                                + " from State: " + cameraState.name() + ".";
                        Log.d(TAG, log);
                    }

                    @Override
                    public void onCameraFreezed(String errorDescription) {
                        log = logTag + "Camera frozed! Error: " + errorDescription
                                + " from State: " + cameraState.name() + ".";
                        Log.d(TAG, log);
                    }

                    @Override
                    public void onCameraOpening(String cameraName) {
                        log = logTag + "Camera was ";
                        switch (cameraState) {

                            case CAMERA_OPENED:
                                // Camera opened from an opened state => Switched camera.
                                cameraState = CameraState.CAMERA_SWITCHED;
                                cameraNamePrevious = cameraNameCurrent;
                                cameraNameCurrent = cameraName;
                                log += "switched from: " + cameraNamePrevious +
                                        " to: " + cameraNameCurrent + ".";
                                break;
                            case CAMERA_CLOSED:
                                // Camera opened from a closed state => New camera just opened.
                                cameraState = CameraState.CAMERA_OPENED;
                                cameraNameCurrent = cameraName;
                                log += "just opened to: " + cameraNameCurrent + ".";
                                break;
                            case CAMERA_SWITCHED:
                                cameraState = CameraState.CAMERA_OPENED;
                                log += "opened from State:" + CameraState.CAMERA_SWITCHED.name()
                                        + " to: " + cameraName + " => Error!";
                                break;
                        }

                        Log.d(TAG, log);
                    }

                    @Override
                    public void onFirstFrameAvailable() {
                        log = logTag + "Camera (" + cameraNameCurrent + ") first frame available,"
                                + " from State: " + cameraState.name() + ".";
                        Log.d(TAG, log);
                    }

                    @Override
                    public void onCameraClosed() {
                        log = logTag;
                        switch (cameraState) {
                            case CAMERA_OPENED:
                                cameraState = CameraState.CAMERA_CLOSED;
                                cameraNamePrevious = cameraNameCurrent;
                                cameraNameCurrent = cameraNameNone;
                                log += "Closing all cameras. Last active ";
                                break;
                            case CAMERA_CLOSED:
                                cameraState = CameraState.CAMERA_CLOSED;
                                cameraNamePrevious = cameraNameCurrent;
                                cameraNameCurrent = cameraNameNone;
                                log += "Error! From State: " + cameraState.name() + ", ";
                                break;
                            case CAMERA_SWITCHED:
                                cameraState = CameraState.CAMERA_OPENED;
                                log += "Switch is complete. Previous ";
                                break;
                        }
                        log += "camera (" + cameraNamePrevious + ") was just closed.";
                        Log.d(TAG, log);
                    }
                };

        VideoCapturer cameraVideoCapturer =
                cameraEnumerator.createCapturer(cameraName, cameraEventsHandler);
        log = logTag + "Created CameraVideoCapturer: " + cameraVideoCapturer;
        Log.d(TAG, log);
        return cameraVideoCapturer;
    }
}
