package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logE;

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
                String error = "[ERROR:" + Errors.CONNECT_IS_EMPTY_ROOM_PARAMS +
                        "] Unable to connect to room!";
                String debug = error + "\nDetails: Room params from App Server is null!" +
                        "\nAborting connect to room...";
                logE(TAG, error);
                logD(TAG, debug);
                roomParameterListener.onRoomParameterError(error);
                return;
            }

            if (roomJson.has(ERROR) && !roomJson.getBoolean(SUCCESS)) {
                String error = "[ERROR:" + Errors.CONNECT_IS_ERROR_IN_ROOM_PARAMS +
                        "] Unable to connect to room!";
                String debug = error + "\nDetails: Room params from App Server has error!\n" +
                        "Server Info: " + roomJson.getString(INFO) +
                        "\nAborting connect to room...";
                logE(TAG, error);
                logD(TAG, debug);
                roomParameterListener.onRoomParameterError(error);
                return;
            }

            parameters.setAppOwner(roomJson.getString(APP_OWNER));
            logD(TAG, "[processRoomParameters] apiOwner: " + parameters.getAppOwner() + ".");

            parameters.setCid(roomJson.getString(CID));
            logD(TAG, "[processRoomParameters] cid: " + parameters.getCid() + ".");

            parameters.setDisplayName(roomJson.getString(DISPLAY_NAME));
            logD(TAG, "[processRoomParameters] displayName: " + parameters.getDisplayName() + ".");

            parameters.setLen(roomJson.getString(LEN));
            logD(TAG, "[processRoomParameters] len: " + parameters.getLen() + ".");

            parameters.setRoomCred(roomJson.getString(ROOM_CRED));
            logD(TAG, "[processRoomParameters] roomCred: " + parameters.getRoomCred() + ".");

            parameters.setRoomId(roomJson.getString(ROOM_KEY));
            logD(TAG, "[processRoomParameters] room_key: " + parameters.getRoomId() + ".");

            parameters.setStart(roomJson.getString(ROOM_START));
            logD(TAG, "[processRoomParameters] start: " + parameters.getStart() + ".");

            parameters.setTimeStamp(roomJson.getString(ROOM_TIME_STAMP));
            logD(TAG, "[processRoomParameters] timeStamp: " + parameters.getTimeStamp() + ".");

            parameters.setUserCred(roomJson.getString(USER_CRED));
            logD(TAG, "[processRoomParameters] userCred: " + parameters.getUserCred() + ".");

            parameters.setUserId(roomJson.getString(USERNAME));
            logD(TAG, "[processRoomParameters] username: " + parameters.getUserId() + ".");

            String ipSignalingServer = roomJson.getString(IP_SIGSERVER);
            logD(TAG, "[processRoomParameters] ipSigserver: " + ipSignalingServer + ".");
            parameters.setIpSigserver(ipSignalingServer);

            int portSignalingServer = roomJson.getInt(PORT_SIGSERVER);
            logD(TAG, "[processRoomParameters] portSigserver: " + portSignalingServer + ".");
            parameters.setPortSigserver(portSignalingServer);

            if (ipSignalingServer == null || portSignalingServer <= 0) {
                String error = "[ERROR:" + Errors.CONNECT_IS_SIG_SERVER_IP_WRONG_IN_ROOM_PARAMS +
                        "] Unable to connect to room!";
                String debug = error + "\nDetails: Signaling server IP or port has invalid values" +
                        " in Room params! Aborting connect to room...";
                logE(TAG, error);
                logD(TAG, debug);
                roomParameterListener.onRoomParameterError(error);
            }

            parameters.setProtocol(roomJson.getString(PROTOCOL));
            logD(TAG, "[processRoomParameters] protocol: " + parameters.getProtocol() + ".");
        } catch (JSONException e) {
            String error = "[ERROR:" + Errors.CONNECT_UNABLE_TO_READ_ROOM_PARAMS_JSON +
                    "] Unable to connect to room!";
            String debug = error + "\nDetails: Error reading room params from App Server.\n" +
                    "Exception: " + e.getMessage() + "\nAborting connect to room...";
            logE(TAG, error);
            logD(TAG, debug);
            roomParameterListener.onRoomParameterError(error);
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
