package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.KeyInfo;

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
    // Queue of Permission requesting objects.
    private static Deque<PermRequester> permQ = new ArrayDeque<>();
    /**
     * Captures state of whether an Android Runtime permission is currently being processed.
     * Processing is complete when onRequestPermissionsResult is called, or if
     */
    private static Boolean permProcessing = new Boolean(false);
    /**
     * The currently processing Permission request.
     * Used for resuming after device rotation.
     */
    private static PermRequester permRequester = null;

    private Utils() {
    }

    /**
     * Returns the userData of a Peer as a String.
     * If there is no userData, returns the empty string, "".
     *
     * @param peerId The PeerId for which to search. Use null for self (local Peer).
     * @return
     */
    public static String getUserDataString(String peerId) {
        Object userDataObject = SkylinkConnection.getInstance().getUserData(peerId);
        String userDataString = "";
        if (userDataObject != null) {
            userDataString = userDataObject.toString();
        }
        return userDataString;
    }

    /**
     * Returns the userData of a Peer as a String.
     * If there is no userData, returns the empty string, "".
     *
     * @param userInfo The Peer's UserInfo for which to search. Use null for self (local Peer).
     * @return
     */
    public static String getUserDataString(UserInfo userInfo) {
        SkylinkConnection skylinkConnection = SkylinkConnection.getInstance();
        if (userInfo == null) {
            userInfo = skylinkConnection.getUserInfo(null);
        }
        if (userInfo == null) {
            return "";
        }

        Object userDataObject = userInfo.getUserData();
        String userDataString = "";
        if (userDataObject != null) {
            userDataString = userDataObject.toString();
        }
        return userDataString;
    }

    /**
     * Provide a display name that is the PeerId followed by userData in brackets.
     * In case PeerId is not available, the defaultName provided will be displayed.
     *
     * @param skylinkConnection
     * @param defaultName       String to return if PeerId is not available.
     * @param peerId            Use null for self Peer.
     * @return
     */
    public static String getDisplayName(SkylinkConnection skylinkConnection, String defaultName,
                                        String peerId) {
        if (skylinkConnection == null) {
            return defaultName;
        }
        return getPeerIdNick(peerId);
    }

    /**
     * Returns the PeerId followed by userData in brackets of a Peer.
     * This is often useful for displaying a Peer's identity in UI or logs.
     *
     * @param peerId Use null or peerId for self Peer.
     * @return
     */
    public static String getPeerIdNick(String peerId) {
        String peerIdShow = peerId;
        if (peerId == null) {
            SkylinkConnection skylinkConnection = SkylinkConnection.getInstance();
            if (skylinkConnection != null) {
                peerIdShow = skylinkConnection.getPeerId();
            }
            if (peerIdShow == null) {
                peerIdShow = "Self";
            }
        }
        final String peerIdNick = peerIdShow + " (" + getUserDataString(peerId) + ")";
        return peerIdNick;
    }

    /**
     * Returns the PeerId followed by userData in brackets of a Peer.
     * This is often useful for displaying a Peer's identity in UI or logs.
     *
     * @param peerId
     * @param userInfo
     * @return
     */
    public static String getPeerIdNick(String peerId, UserInfo userInfo) {
        return peerId + " (" + getUserDataString(userInfo) + ")";
    }

    /**
     * Returns the nickname of a Peer as the userData as a string.
     * If there is no userData, return the PeerId.
     *
     * @param peerId
     * @return
     */
    public static String getNick(String peerId) {
        String nick = getUserDataString(peerId);
        if ("".equals(nick)) {
            nick = peerId;
        }
        return nick;
    }

    /**
     * Returns a string that contains roomName, roomId, peerId, and peer nick.
     *
     * @param skylinkConnection
     * @param roomName
     * @param peerId
     * @return
     */
    @NonNull
    public static String getRoomPeerIdNick(SkylinkConnection skylinkConnection, String roomName,
                                           String peerId) {
        String title = "Room: " + getRoomRoomId(skylinkConnection, roomName);
        // Add PeerId to title if a Peer occupies clicked location.
        title += "\r\n" + Utils.getPeerIdNick(peerId);
        return title;
    }

    /**
     * Gets the roomName, followed with roomId in brackets.
     *
     * @param skylinkConnection
     * @param roomName
     * @return
     */
    @NonNull
    public static String getRoomRoomId(SkylinkConnection skylinkConnection, String roomName) {
        String roomId = "";
        if (skylinkConnection != null) {
            roomId = skylinkConnection.getRoomId();
        }
        return roomName + " (" + roomId + ")";
    }

    /**
     * Set certain room UI for single party room.
     *
     * @param isConnected
     * @param isPeerInRoom
     * @param textView
     * @param peerName
     * @param roomName
     * @param userName
     */
    public static void setRoomDetails(boolean isConnected, boolean isPeerInRoom, TextView textView,
                                      String peerName, String roomName, String userName) {
        String roomDetails = "You are not connected to any room";
        if (isConnected) {
            roomDetails = "Room Name : " + roomName
                    + "\nYou are signed in as : " + userName + "\n";
            if (isPeerInRoom) {
                roomDetails += "Peer Name : " + peerName;
            } else {
                roomDetails += "You are alone in this room";
            }
        }
        textView.setText(roomDetails);
    }

    /**
     * Set Room status info on UI for multi party room, including:
     * - Whether connected to a room and room name.
     * - User name
     * - If there are Peers in the room.
     *
     * @param isConnected
     * @param isPeerInRoom
     * @param textView
     * @param roomName
     * @param userName
     */
    public static void setRoomDetailsMulti(boolean isConnected, boolean isPeerInRoom,
                                           TextView textView, String roomName, String userName) {
        String roomDetails = "You are not connected to any room";
        if (isConnected) {
            roomDetails = "Now connected to Room named : " + roomName
                    + "\nYou are signed in as : " + userName + "\n";
            if (isPeerInRoom) {
                roomDetails += "Peer(s) are in the room";
            } else {
                roomDetails += "You are alone in this room";
            }
        }
        textView.setText(roomDetails);
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
     * Get the number of remote Peers connected to us.
     *
     * @return
     */
    public static int getNumRemotePeers() {
        int totalInRoom = getTotalInRoom();
        if (totalInRoom == 0) {
            return 0;
        }
        // The first Peer is the local Peer.
        return totalInRoom - 1;
    }

    /**
     * Get total number of Peers in room, including self Peer.
     *
     * @return
     */
    public static int getTotalInRoom() {
        SkylinkConnection skylinkConnection = SkylinkConnection.getInstance();
        String[] peerIdList = skylinkConnection.getPeerIdList();
        if (peerIdList == null) {
            return 0;
        }
        // Size of array is number of Peers in room.
        return peerIdList.length;
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
        skylinkConfig.setDefaultCameraBack(); // Default is front camera.
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
     * @param parentActivity
     * @param logTag
     */
    public static void handleSkylinkReceiveLog(int infoCode, String message,
                                               Activity parentActivity, String logTag) {
        switch (infoCode) {
            case CAM_SWITCH_FRONT:
            case CAM_SWITCH_NON_FRONT:
                Toast.makeText(parentActivity, message, Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        Log.d(logTag, "Received SDK log: " + message);
    }

    /**
     * Log and Toast warning provided by SkylinkConnection.
     *
     * @param errorCode
     * @param message
     * @param parentActivity
     * @param logTag
     */
    public static void handleSkylinkWarning(int errorCode, String message, Activity parentActivity,
                                            String logTag) {
        String log = "Skylink Error: " + errorCode + " (" + Errors.getErrorString(errorCode)
                + ")\r\n" + message;
        Toast.makeText(parentActivity, log, Toast.LENGTH_LONG).show();
        Log.w(logTag, log);
    }

    /**
     * Handles the Android provided result(s) of requested Android permission(s).
     * Sends to Skylink SDK for processing.
     * If the permission requests did not originate from the SDK, the App should process them.
     *
     * @param requestCode       As given in Android method.
     * @param permissions       As given in Android method.
     * @param grantResults      As given in Android method.
     * @param tag               Tag string for logging.
     * @param skylinkConnection The instance of SkylinkConnection being used here.
     */
    public static void onRequestPermissionsResultHandler(
            int requestCode, String[] permissions, int[] grantResults,
            String tag, SkylinkConnection skylinkConnection) {

        // Trigger for next permission to be processed.
        permQTaskCompleted();

        // Null check.
        String error = "";
        if (permissions.length < 1) {
            error = "[SA][onPermRes] Unable to process empty permissions array!";
        }
        if (grantResults.length < 1) {
            error = "[SA][onPermRes] Unable to process empty grantResults array!";
        }
        if (!"".equals(error)) {
            Log.e(tag, error);
            return;
        }

        String permission = permissions[0];
        int grantResult = grantResults[0];
        String log = "[SA][onPermRes] Received results for requestCode:" + requestCode +
                ", Permissions:" + permission + ", with results:" + grantResult + ", that ";
        // Call SDK processPermissionsResult with the given parameters.
        boolean wasSkylinkRequest = skylinkConnection
                .processPermissionsResult(requestCode, permissions, grantResults);
        if (wasSkylinkRequest) {
            log += "originates ";
        } else {
            log += "does NOT originate ";
            // If result is false, process the results in the app
            // (permission request was not from SDK).
        }
        log += "from the Skylink SDK.";
        Log.d(tag, log);
    }

    /**
     * Handles the Skylink SDK OsListener callback onPermissionRequired.
     * If permission required have already been granted,
     * directly call the SDK's processPermissionsResult with PackageManager.PERMISSION_GRANTED.
     * If not, ask user for the required permission.
     * If user had denied required permission before, but did not indicate to never ask again,
     * provide a dialog to inform user why such permissions are required,
     * and provide the chance to set the required permissions again.
     *
     * @param permissions       As given in OsListener method.
     * @param requestCode       As given in OsListener method.
     * @param infoCode          As given in OsListener method.
     * @param tag               Tag string for logging.
     * @param context           Current context.
     * @param fragment          Current fragment.
     * @param skylinkConnection The instance of SkylinkConnection being used here.
     */
    public static void onPermissionRequiredHandler(
            final String[] permissions, final int requestCode, final int infoCode,
            final String tag, final Context context, final Fragment fragment,
            final SkylinkConnection skylinkConnection) {

        // Create a new PermRequester to represent this request.
        PermRequester permRequester = new Utils.PermRequester(permissions, requestCode, infoCode,
                tag, context, fragment, skylinkConnection);

        // Add PermRequester to Queue.
        permQOfferLast(permRequester);
    }

    /**
     * Handles Skylink SDK OsListener callback onPermissionGranted.
     * Log the permission that had been granted.
     *
     * @param permissions As given in OsListener method.
     * @param infoCode    As given in OsListener method.
     * @param tag         Tag string for logging.
     */
    public static void onPermissionGrantedHandler(String[] permissions, int infoCode, String tag) {
        String log = "[SA][onPermGrant] Permission has been GRANTED for " + permissions[0] +
                ", infoCode:" + infoCode + " (" + getInfoString(infoCode) + ").";
        Log.d(tag, log);
    }

    /**
     * @param infoCode As given in OsListener method.
     * @param context  Current context to show AlertDialog.
     * @param tag      Tag string for logging.
     */
    public static void onPermissionDeniedHandler(int infoCode, Context context, String tag) {
        // Create alert to inform user about the permission denied and resultant feature disabled.
        // Log the same.
        // Check if should explain reason for requesting permission, which happens if the user
        // has denied this Permission before, but did not indicate to never ask again.
        String alertText = "";
        switch (infoCode) {
            case PERM_AUDIO_MIC:
                alertText += "Android permission to use the Microphone was denied. " +
                        "We are now NOT able to send our audio to a remote Peer!";
                break;
            case PERM_VIDEO_CAM:
                alertText += "Android permission to use the Camera was denied. " +
                        "We are now NOT able to send our video to a remote Peer!";
                break;
            case PERM_STORAGE_READ:
                alertText += "Android permission to read from device storage was denied. " +
                        "We are now NOT able to send file to a remote Peer!";
                break;
            case PERM_STORAGE_WRITE:
                alertText += "Android permission to write to device storage was denied. " +
                        "We are now NOT able to receive file from a remote Peer!";
                break;
        }
        alertText += "\r\nTo enable feature, restart this feature and grant the permission(s) " +
                "required. Alternatively, go to Android's Settings -> \"Apps\", select this App, " +
                "go to \"Permissions\", grant required permission(s), and restart this feature.";

        // Create AlertDialog to warn user of consequences of permission denied.
        AlertDialog.Builder permissionDeniedDialogBuilder =
                new AlertDialog.Builder(context);
        permissionDeniedDialogBuilder.setTitle("Warning! Feature(s) unavailable " +
                "due to Permission(s) denied.");

        // Create TextView for permission alert.
        final TextView msgTxtView = new TextView(context);
        msgTxtView.setText(alertText);
        msgTxtView.setMovementMethod(LinkMovementMethod.getInstance());
        permissionDeniedDialogBuilder.setView(msgTxtView);
        permissionDeniedDialogBuilder.setPositiveButton("Ok", null);

        alertText = "[SA][onPermDenied] " + alertText;
        Log.d(tag, alertText);
        permissionDeniedDialogBuilder.show();
    }

    /**
     * Stores parameters required for making permission request
     * and have methods to perform permission request.
     */
    public static class PermRequester {

        String[] permissions;
        int requestCode;
        int infoCode;
        String tag;
        // Static elements that are common to all PermRequester
        static Context context;
        static Fragment fragment;
        static SkylinkConnection skylinkConnection;

        public PermRequester(String[] permissions, int requestCode, int infoCode, String tag,
                             Context context, Fragment fragment,
                             SkylinkConnection skylinkConnection) {
            this.permissions = permissions;
            this.requestCode = requestCode;
            this.infoCode = infoCode;
            this.tag = tag;
            PermRequester.context = context;
            PermRequester.fragment = fragment;
            PermRequester.skylinkConnection = skylinkConnection;
        }

        /**
         * Sets new Permission request parameters specific to caller.
         * This could happen when a new calling fragment wishes to execute a previously requested
         * Permission, for e.g. after a screen rotation where a new fragment continues the
         * permission request of the previous fragment.
         *
         * @param context
         * @param fragment
         * @param skylinkConnection
         */
        static void setNewCallerInfo(Context context, Fragment fragment,
                                     SkylinkConnection skylinkConnection) {
            PermRequester.context = context;
            PermRequester.fragment = fragment;
            PermRequester.skylinkConnection = skylinkConnection;
        }

        /**
         * Do the actual work of processing onPermissionRequired using class members as parameters.
         */
        void processOnPermReq() {
            processOnPermReq(permissions, requestCode, infoCode, tag,
                    context, fragment, skylinkConnection);
        }

        /**
         * Do the actual work of processing onPermissionRequired using supplied parameters.
         *
         * @param permissions
         * @param requestCode
         * @param infoCode
         * @param tag
         * @param context
         * @param fragment
         * @param skylinkConnection
         * @return
         */
        void processOnPermReq(final String[] permissions, final int requestCode,
                              final int infoCode, final String tag,
                              final Context context, final Fragment fragment,
                              final SkylinkConnection skylinkConnection) {
            String log = "[SA][PR][procPermReq] SDK requesting permission for " + permissions[0] +
                    ", which ";

            // For permission already granted,
            // call Sylink SDK processPermissionsResult with PERMISSION_GRANTED as the result.
            int permissionState = ContextCompat.checkSelfPermission(context, permissions[0]);
            if (permissionState == PackageManager.PERMISSION_GRANTED) {

                log += "has already been granted, no need to request again. " +
                        "infoCode:" + infoCode + " (" + getInfoString(infoCode) + ")";

                int[] grantResults = new int[]{PackageManager.PERMISSION_GRANTED};
                if (!skylinkConnection.processPermissionsResult(requestCode, permissions,
                        grantResults)) {
                    // If result is false, an error has occurred.
                    log += "\r\n[ERROR] The SDK should but does not recognise permission requestCode: "
                            + " " + requestCode + "!";
                    Log.e(tag, log);
                } else {
                    Log.d(tag, log);
                }

                // Trigger for next permission to be processed.
                permQTaskCompleted();
                return;

            } else {
                log += "has not been granted. ";
            }

            // Create explanation based on permission required.
            String alertText = "";
            switch (infoCode) {
                case PERM_AUDIO_MIC:
                    alertText += "Android permission to use the Microphone must be given " +
                            "in order to send our audio to a remote Peer!";
                    break;
                case PERM_VIDEO_CAM:
                    alertText += "Android permission to use the Camera must be given " +
                            "in order to send our video to a remote Peer!";
                    break;
                case PERM_STORAGE_READ:
                    alertText += "Android permission to read from device storage must be given " +
                            "in order to send file to a remote Peer!";
                    break;
                case PERM_STORAGE_WRITE:
                    alertText += "Android permission to write to device storage must be given " +
                            "in order to receive file from a remote Peer!";
                    break;
            }
            log += alertText + "\r\ninfoCode:" + infoCode + " (" + getInfoString(infoCode) + ")";

            // Explain rationale for permission request if the user
            // has denied this Permission before, but did not indicate to never ask again.
            if (fragment.shouldShowRequestPermissionRationale(permissions[0])) {

                // Create AlertDialog to present Permission rationale message.
                AlertDialog.Builder permissionRationaleDialogBuilder =
                        new AlertDialog.Builder(context);
                permissionRationaleDialogBuilder.setTitle("Why this permission is requested...");

                // Create TextView for permission rationale alert.
                final TextView msgTxtView = new TextView(context);
                msgTxtView.setText(alertText);
                msgTxtView.setMovementMethod(LinkMovementMethod.getInstance());
                permissionRationaleDialogBuilder.setView(msgTxtView);

                // Indicates if permission request had been made (regardless of request outcome).
                final boolean[] requestMade = {false};

                // User denies permission even after providing rationale.
                final String finalLogDeny = log + "\r\n" +
                        "Not requesting Android for Permission after current & previous denial.";
                permissionRationaleDialogBuilder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestMade[0] = true;
                                String logDeny = finalLogDeny;

                                // Call Sylink SDK processPermissionsResult with
                                // PERMISSION_DENIED as the result.
                                int[] grantResults = new int[]{PackageManager.PERMISSION_DENIED};
                                if (!skylinkConnection.processPermissionsResult(requestCode, permissions,
                                        grantResults)) {
                                    // If result is false, an error has occurred.
                                    logDeny += "\r\n[ERROR] The SDK should but does not recognise "
                                            + "permission requestCode: " + requestCode + "!";
                                    Log.e(tag, logDeny);
                                } else {
                                    // Log permission denied.
                                    Log.d(tag, logDeny);
                                }

                                // Trigger for next permission to be processed.
                                permQTaskCompleted();
                            }
                        });

                // User agrees to grant permission after rationale was shown.
                final String finalLogRequest = log +
                        "\r\nRequesting Android for Permission after previous denial.";
                permissionRationaleDialogBuilder.setPositiveButton("Grant Permission",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestMade[0] = true;
                                // Request for permission:
                                Log.d(tag, finalLogRequest);
                                fragment.requestPermissions(permissions, requestCode);
                            }
                        });

                // Set this permission in Q again if it was canceled without being requested.
                final String finalLogCancel = log + "\r\npermissionRationaleDialog canceled ";
                permissionRationaleDialogBuilder.setOnCancelListener(
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                // If request had not been made, add permission Runnable again to
                                // Head of Queue, to be processed again.
                                if (!requestMade[0]) {
                                    String logCancel = finalLogCancel + "without having made "
                                            + "request! Will make request again.";
                                    Log.d(tag, logCancel);
                                    PermRequester permRequester =
                                            new PermRequester(permissions, requestCode, infoCode,
                                                    tag, context, fragment, skylinkConnection);
                                    permQOfferFirst(permRequester);
                                    return;
                                }

                                // Otherwise, it was from elsewhere where
                                // the permission request was already made.
                                String logCancel = finalLogCancel + "after having made request, "
                                        + "hence will not make request again.";
                                Log.d(tag, logCancel);
                            }
                        });

                // Show explanation.
                permissionRationaleDialogBuilder.show();
                return;
            }

            // Request for permission for the first time:
            log += ".\r\nRequesting Android for Permission for the first time.";
            Log.d(tag, log);

            fragment.requestPermissions(permissions, requestCode);
            return;
        }
    }

    /**
     * Adds new permission request to the head of the queue.
     * This is for repeating a particular request immediately.
     * Synchronised with permProcessing.
     *
     * @param permRequester
     */
    public static void permQOfferFirst(PermRequester permRequester) {
        synchronized (permProcessing) {
            permQ.offerFirst(permRequester);

            // Try to process permRun immediately.
            permProcessing = false;
            Utils.permRequester = null;

            String log = "[SA][permQOfferFirst] Added permission request to head of permQ. " +
                    "Set permProcessing to false and try to process queue immediately.";
            Log.d(TAG, log);
            permQPoll();
        }
    }

    /**
     * Adds new permission requests to the tail of the queue.
     * This is for normal adding of new permission requests to the queue.
     * Synchronised with permProcessing.
     *
     * @param permRequester
     */
    public static void permQOfferLast(PermRequester permRequester) {
        synchronized (permProcessing) {
            permQ.offerLast(permRequester);
            String log = "[SA][permQOfferLast] Added permission request to tail of permQ. " +
                    "Triggered for queue processing.";
            Log.d(TAG, log);
            permQPoll();
        }
    }

    /**
     * Process the element at the head of the queue if not already currently processing.
     * Synchronised with permProcessing.
     *
     * @return True if a new permission request is triggered to process, false otherwise.
     */
    public static boolean permQPoll() {
        synchronized (permProcessing) {
            String log = "[SA][permQPoll] ";
            if (permProcessing) {
                log += "Tried to process permQ but not starting new attempt as " +
                        "it is currently being processed.";
                Log.d(TAG, log);
                return false;
            }

            // Try to process next permission request since Q is not currently being processed.
            PermRequester permRequesterNext = permQ.poll();
            // Do not process if there are no more element in the queue.
            if (permRequesterNext == null) {
                log += "Tried to process permQ but not starting new attempt as " +
                        "there are no more permissions waiting to be processed.";
                Log.d(TAG, log);
                return false;
            }

            // Set states and process permission request.
            permProcessing = true;
            permRequester = permRequesterNext;
            log += "Processing next permission request in permQ. Permission states updated.";
            Log.d(TAG, log);
            permRequesterNext.processOnPermReq();
            return true;
        }
    }

    /**
     * Reset permQ tasks and states, typically at the start of a sample fragment.
     * This is required when for e.g.:
     * Restarting a sample after disruption by "Screen Overlay detected" permission setting error.
     * Synchronised with permProcessing.
     */
    public static void permQReset() {
        synchronized (permProcessing) {

            permProcessing = false;
            permRequester = null;
            permQ = new ArrayDeque<>();

            String log = "[SA][permQReset] Reset all permQ related tasks and states! " +
                    "permQ is now empty.";
            Log.d(TAG, log);
        }
    }

    /**
     * Resume previously running permQ task (if any) after rotation.
     * Previous caller related parameters will be replaced by current caller.
     * Synchronised with permProcessing.
     *
     * @param context
     * @param fragment
     * @param skylinkConnection
     */
    public static void permQResume(Context context, Fragment fragment,
                                   SkylinkConnection skylinkConnection) {
        synchronized (permProcessing) {
            if (permRequester == null) {
                return;
            }

            String log = "[SA][permQResume] Resuming permission request that was disrupted!";
            Log.d(TAG, log);

            // Set new caller for all PermRequesters, including those in Q.
            PermRequester.setNewCallerInfo(context, fragment, skylinkConnection);
            permQOfferFirst(permRequester);
        }
    }

    /**
     * Call on completion of a permQ task.
     * Will trigger permQ to process next task.
     * Synchronised with permProcessing.
     */
    public static void permQTaskCompleted() {
        synchronized (permProcessing) {
            // Set state to not processing.
            permProcessing = false;
            permRequester = null;

            String log = "[SA][permQTaskCom] Completed permission request. " +
                    "Set permProcessing to false and try to process next task in queue.";
            Log.d(TAG, log);
            permQPoll();
        }
    }

}
