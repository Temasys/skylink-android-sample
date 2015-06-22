package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Service class that will extra room parameters from a Skylink App server response string.
 */
class SkylinkRoomParameterProcessor implements RoomParameterProcessor {

    public static final String TAG = SkylinkRoomParameterProcessor.class.getName();
    public static final String APP_OWNER = "apiOwner";
    public static final String CID = "cid";
    public static final String DISPLAY_NAME = "displayName";
    public static final String LEN = "len";
    public static final String ROOM_CRED = "roomCred";
    public static final String ROOM_KEY = "room_key";
    public static final String ROOM_START = "start";
    public static final String ROOM_TIME_STAMP = "timeStamp";
    public static final String USER_CRED = "userCred";
    public static final String USERNAME = "username";
    public static final String IP_SIGSERVER = "ipSigserver";
    public static final String PORT_SIGSERVER = "portSigserver";
    public static final String SUCCESS = "success";
    public static final String INFO = "info";
    public static final String PROTOCOL = "protocol";
    public static final String ERROR = "error";

    private RoomParameterListener roomParameterListener;

    /**
     * RoomParameterProcessor implementation
     *
     * @param serverResponse
     */
    @Override
    public void processRoomParameters(String serverResponse) {

        JSONObject roomJson;
        RoomParameters parameters = new RoomParameters();
        try {
            roomJson = new JSONObject(serverResponse);

            if (roomJson == null) {
                roomParameterListener.onRoomParameterError(ErrorCodes.UNEXPECTED_ERROR_ROOM_PARAMETERS);
            }

            if (roomJson.has(ERROR) && !roomJson.getBoolean(SUCCESS)) {
                roomParameterListener.onRoomParameterError(roomJson.getString(INFO));
            }

            parameters.setAppOwner(roomJson.getString(APP_OWNER));
            Log.d(TAG, "apiOwner->" + parameters.getAppOwner());

            parameters.setCid(roomJson.getString(CID));
            Log.d(TAG, "cid->" + parameters.getCid());

            parameters.setDisplayName(roomJson.getString(DISPLAY_NAME));
            Log.d(TAG, "displayName->" + parameters.getDisplayName());

            parameters.setLen(roomJson.getString(LEN));
            Log.d(TAG, "len->" + parameters.getLen());

            parameters.setRoomCred(roomJson.getString(ROOM_CRED));
            Log.d(TAG, "roomCred->" + parameters.getRoomCred());

            parameters.setRoomId(roomJson.getString(ROOM_KEY));
            Log.d(TAG, "room_key->" + parameters.getRoomId());

            parameters.setStart(roomJson.getString(ROOM_START));
            Log.d(TAG, "start->" + parameters.getStart());

            parameters.setTimeStamp(roomJson.getString(ROOM_TIME_STAMP));
            Log.d(TAG, "timeStamp->" + parameters.getTimeStamp());

            parameters.setUserCred(roomJson.getString(USER_CRED));
            Log.d(TAG, "userCred->" + parameters.getUserCred());

            parameters.setUserId(roomJson.getString(USERNAME));
            Log.d(TAG, "username->" + parameters.getUserId());

            String ipSignalingServer = roomJson.getString(IP_SIGSERVER);
            Log.d(TAG, "ipSigserver->" + ipSignalingServer);
            parameters.setIpSigserver(ipSignalingServer);

            int portSignalingServer = roomJson.getInt(PORT_SIGSERVER);
            Log.d(TAG, "portSigserver->" + portSignalingServer);

            parameters.setPortSigserver(portSignalingServer);

            if (ipSignalingServer == null || portSignalingServer <= 0) {
                Log.d(TAG, "Invalid signaling server ip and port. Returning ...");
                roomParameterListener.onRoomParameterError
                        ("Invalid signaling server ip and port. Returning ...");
            }

            parameters.setProtocol(roomJson.getString(PROTOCOL));
            Log.d(TAG, "protocol->" + parameters.getProtocol());
        } catch (JSONException e) {
            String strErr = e.getMessage();
            Log.e(TAG, strErr, e);
            roomParameterListener.onRoomParameterError(strErr);
            return;
        }

        roomParameterListener.onRoomParameterSuccessful(parameters);
    }

    /**
     * RoomParameterProcessor implementation
     *
     * @param roomParameterListener
     */
    @Override
    public void setRoomParameterListener(RoomParameterListener roomParameterListener) {
        this.roomParameterListener = roomParameterListener;
    }

}
