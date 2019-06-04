package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.util.Log;

import org.webrtc.VideoCapturer;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for managing connection with SkylinkSDK like connect, disconnect,...
 */

public class SkylinkConnectionManager {

    private final String TAG = SkylinkConnectionManager.class.getName();

    private Context context;

    private SkylinkConnection skylinkConnection = null;

    private SkylinkCommonService skylinkCommonService = null;

    /**
     * Both {@link SkylinkCommonService} and {@link Context} are required for {@link SkylinkConnectionManager} functioning.
     *
     * @param skylinkCommonService
     * @param context
     */
    public SkylinkConnectionManager(SkylinkCommonService skylinkCommonService, Context context) {
        this.skylinkCommonService = skylinkCommonService;
        this.context = context;
    }

    /**
     * Initialize a SkylinkConnection object
     */
    public SkylinkConnection initializeSkylinkConnection(Constants.CONFIG_TYPE typeCall, VideoCapturer videoCapturer) {
        String logTag = "[SA][SCM][initializeSkylinkConnection] ";
        String log = logTag;

        //check internet connection
        if (!Utils.isInternetOn()) {
            log = "Internet connection is off !";
            toastLog(TAG, context, log);
            return null;
        }

        if (skylinkCommonService == null) {
            log += "Error: SkylinkCommonService is null";
            Log.e(TAG, log);
            return null;
        }

        //Get the user config for connection
        SkylinkConfig skylinkConfig = skylinkCommonService.getSkylinkConfig();

        if (skylinkConfig == null) {
            log += "Error: SkylinkConfig is null";
            Log.e(TAG, log);
            return null;
        }

        skylinkConnection = SkylinkConnection.getInstance();
        // Set SkylinkConnection instance in skylinkCommonService ASAP.
        skylinkCommonService.setmSkylinkConnection(skylinkConnection);
        // Initialize this SkylinkConnection instance.
        skylinkConnection.init(Config.getAppKey(), skylinkConfig,
                context.getApplicationContext());

        // Set Skylink listeners necessary for current demo/call
        skylinkCommonService.setSkylinkListeners();

        // Set custom VideoCapturer if any.
        if (videoCapturer != null) {
            log = logTag + "With Custom VideoCapturer: " + videoCapturer + ".";
            toastLog(TAG, context, log);
            skylinkConnection.setCustomVideoCapturer(videoCapturer);
        }

        // Get room name and user name in setting
        String mRoomName = Utils.getRoomNameByType(typeCall);
        String mUserName = Utils.getUserNameByType(typeCall);

        skylinkCommonService.setRoomName(mRoomName);
        skylinkCommonService.setUserName(mUserName);

        return skylinkConnection;
    }

    /**
     * Connects to a room using a SkylinkConnectionString that caller MUST ensure is URL safe.
     *
     * @param typeCall      Specify which is current demo/call like audio/video/file/...
     * @param videoCapturer Custom {@link VideoCapturer} if desired, null if not.
     * @return SkylinkConnection
     */
    public SkylinkConnection connectToRoom(Constants.CONFIG_TYPE typeCall, VideoCapturer videoCapturer) {
        String logTag = "[SA][SCM][connectToRoom] ";
        String log = logTag;

//        //check internet connection
//        if (!Utils.isInternetOn()) {
//            log = "Internet connection is off !";
//            toastLog(TAG, context, log);
//            return null;
//        }
//
//        if (skylinkCommonService == null) {
//            log += "Error: SkylinkCommonService is null";
//            Log.e(TAG, log);
//            return null;
//        }
//


        // Initialize the skylink connection using SkylinkConnectionManager if it is not initialized
        if (skylinkConnection == null) {
            skylinkConnection = initializeSkylinkConnection(typeCall, videoCapturer);
        }

//        // Set custom VideoCapturer if any.
//        if (videoCapturer != null) {
//            log = logTag + "With Custom VideoCapturer: " + videoCapturer + ".";
//            toastLog(TAG, context, log);
//            skylinkConnection.setCustomVideoCapturer(videoCapturer);
//        }
//
//        // Get room name and user name in setting
        String mRoomName = Utils.getRoomNameByType(typeCall);
        String mUserName = Utils.getUserNameByType(typeCall);
//
//        skylinkCommonService.setRoomName(mRoomName);
//        skylinkCommonService.setUserName(mUserName);

        SkylinkConfig skylinkConfig = skylinkCommonService.getSkylinkConfig();


        // Create the Skylink connection string.
        // In production, the connection string should be generated by an external entity
        // (such as a secure App server that has the Skylink App Key secret), and sent to the App.
        // This is to avoid keeping the App Key secret within the application, for better security.
        String skylinkConnectionString = getSkylinkConnectionString(
                mRoomName, new Date(), SkylinkConnection.DEFAULT_DURATION, skylinkConfig.getRoomSize());

        // The skylinkConnectionString should not be logged in production,
        // as it contains potentially sensitive information like the Skylink App Key ID.
        boolean connectFailed = !skylinkConnection.connectToRoom(skylinkConnectionString, mUserName);

        if (connectFailed) {
            log = logTag + "Unable to connect to room!";
            toastLog(TAG, context, log);
            return null;
        } else {
            log = logTag + "Connecting...";
            toastLog(TAG, context, log);
        }

        return skylinkConnection;
    }

    /**
     * Disconnects from the room we are currently in.
     * Once disconnect is complete, {@link SkylinkCommonService#onDisconnect(int, String)}
     * will be called.
     */
    public void disconnectFromRoom() {
        boolean disConnectSuccess = false;

        if (skylinkConnection != null)
            disConnectSuccess = skylinkConnection.disconnectFromRoom();

        if (disConnectSuccess)
            skylinkConnection = null;
    }

    /**
     * Check the current {@link sg.com.temasys.skylink.sdk.rtc.SkylinkConnection.SkylinkState SkylinkState}
     * of the current SkylinkConnection instance.
     *
     * @return true if connects to room, false if not or disconnects
     */
    public boolean isConnectingOrConnected() {
        if (skylinkConnection == null) return false;

        SkylinkConnection.SkylinkState state = skylinkConnection.getSkylinkState();
        if (state == SkylinkConnection.SkylinkState.CONNECTING ||
                state == SkylinkConnection.SkylinkState.CONNECTED) {
            return true;
        }
        return false;
    }

    /**
     * Returns the SkylinkConnectionString, which MUST BE URL SAFE.
     * Required inputs are: App key, App secret, Room name, Room start time, and Room duration.
     *
     * @param roomName  Name of the room
     * @param startTime Room Start Time
     * @param duration  Duration of the room in Hours
     * @param roomSize  The size of the room, restricted by the server
     *                  Only the first Peer to start the room will have it's room_size effected.
     *                  Later Peers who join the room will not have their room_size value respected.
     *                  If more Peers than indicated by the effected room_size join the room,
     *                  they may get warnings and/or not be allowed to join room.
     * @return
     */
    public String getSkylinkConnectionString(String roomName, Date startTime, int duration,
                                             SkylinkConfig.RoomSize roomSize) {

        String info = "Room name: " + roomName + ", startTime: " + startTime +
                ", duration: " + duration + ".\r\n";

        // Convert the date in to ISO format
        String dateString = Utils.getISOTimeStamp(startTime);

        // Compute RFC 2104-compliant HMAC signature
        String cred = Utils.calculateRFC2104HMAC(roomName + "_" + duration + "_"
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
                + "?cred=" + cred + "&room_size=" + roomSize.getValue();
        info += "URL safe connectionString: \"" + connectionString + "\"";
        Log.d(TAG, info);

        return connectionString;
    }
}
