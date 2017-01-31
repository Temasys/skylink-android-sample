package sg.com.temasys.skylink.sdk.sampleapp;

import android.util.Base64;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import org.webrtc.SurfaceViewRenderer;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;

class Utils {

    public static final String TIME_ZONE_UTC = "UTC";
    public static final String ISO_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ";
    private static final String TAG = Utils.class.getName();
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

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
        if(userInfo == null) {
            userInfo = skylinkConnection.getUserInfo(null);
        }
        if(userInfo == null) {
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
     * Returns the PeerId followed by userData in brackets of a Peer.
     * This is often useful for displaying a Peer's identity in UI or logs.
     *
     * @param peerId
     * @return
     */
    public static String getPeerIdNick(String peerId) {
        return peerId + "(" + getUserDataString(peerId) + ")";
    }

    /**
     * Returns the PeerId followed by userData in brackets of a Peer.
     * This is often useful for displaying a Peer's identity in UI or logs.
     *
     *
     * @param peerId
     * @param userInfo
     * @return
     */
    public static String getPeerIdNick(String peerId, UserInfo userInfo) {
        return peerId + "(" + getUserDataString(userInfo) + ")";
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
     * Returns the SkylinkConnectionString
     *
     * @param roomName  Name of the room
     * @param appKey    App Key
     * @param secret    App secret
     * @param startTime Room Start Time
     * @param duration  Duration of the room in Hours
     * @return
     */
    public static String getSkylinkConnectionString(String roomName, String appKey,
                                                    String secret,
                                                    Date startTime, int duration) {

        Log.d(TAG, "Room name " + roomName);
        Log.d(TAG, "startTime " + startTime);
        Log.d(TAG, "duration " + duration);

        // Convert the date in to ISO format
        String dateString = Utils.getISOTimeStamp(startTime);

        // Compute RFC 2104-compliant HMAC signature
        String cred = calculateRFC2104HMAC(roomName + "_" + duration + "_"
                + dateString, secret);
        try {
            cred = URLEncoder.encode(cred, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        // Encode roomName so that it is url safe.
        String roomNameEncoded = null;
        URI uri = null;
        try {
            uri = new URI("http", "host", "/" + roomName + "/", null);
        } catch (URISyntaxException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        String uriStr = uri.toString();

        /* Parse roomNameEncoded from:
        http://host/<roomNameEncoded>/
        */
        roomNameEncoded = uriStr.substring("http://host/".length(), uriStr.length() - 1);

        return appKey + "/"
                + roomNameEncoded + "/" + dateString + "/" + duration + "?cred="
                + cred;
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
        if ( totalInRoom == 0) {
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
}
