package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;

/**
 * Handles Protocol related logic
 */
class ProtocolHelper {

    private static final String TAG = ProtocolHelper.class.getName();

    private static final String FAST_MSG = "fastmsg";
    private static final String LOCKED = "locked";
    private static final String ROOM_FULL = "roomfull";
    private static final String DUPLICATED_LOGIN = "duplicatedLogin";
    private static final String SERVER_ERROR = "serverError";
    private static final String VERIFICATION = "verification";
    private static final String EXPIRED = "expired";
    private static final String ROOM_CLOSE = "roomclose";
    private static final String TO_CLOSE = "toclose";
    private static final String SEAT_QUOTA = "seatquota";

    private ProtocolHelper() {
    }

    static void processRedirect(JSONObject jsonObject,
                                LifeCycleListener lifeCycleListener) throws JSONException {

        String info = jsonObject.getString("info");
        String action = jsonObject.getString("action");

        // If the reason key exist, get the relevant error code
        String reason = jsonObject.getString("reason");
        int errorCode = ProtocolHelper.getRedirectCode(reason);

        if ("warning".equals(action)) {
            // Send back the info received and the derived error code
            lifeCycleListener.onWarning(errorCode, info);
            Log.d(TAG, "processRedirect: onWarning " + errorCode);
        } else if ("reject".equals(action)) {
            // Send back the info received and the derived error code
            lifeCycleListener.onDisconnect(errorCode, info);
            Log.d(TAG, "processRedirect: onDisconnect " + errorCode);
        }
    }

    static boolean processRoomLockStatus(boolean currentRoomLockStatus,
                                         JSONObject jsonObject, LifeCycleListener lifeCycleListener) throws JSONException {
        boolean lockStatus = jsonObject.getBoolean("lock");
        // Only post updates if received lock status is not the same
        if (lockStatus != currentRoomLockStatus) {
            lifeCycleListener.onLockRoomStatusChange(jsonObject.getString("mid"), lockStatus);
            Log.d(TAG, "processRoomLockStatus: onLockRoomStatusChange " + lockStatus);
        }
        return lockStatus;
    }

    static void sendRoomLockStatus(WebServerClient webServerClient, boolean lockStatus) throws JSONException {
        JSONObject dict = new JSONObject();
        dict.put("rid", webServerClient.getRoomId());
        dict.put("mid", webServerClient.getSid());
        dict.put("lock", lockStatus);
        dict.put("type", "roomLockEvent");
        webServerClient.sendMessage(dict);
        Log.d(TAG, "sendRoomLockStatus: sendMessage " + lockStatus);
    }

    private static int getRedirectCode(String reason) {
        int redirectCode;
        switch (reason) {
            case FAST_MSG:
                redirectCode = ErrorCodes.REDIRECT_REASON_FAST_MSG;
                break;
            case LOCKED:
                redirectCode = ErrorCodes.REDIRECT_REASON_LOCKED;
                break;
            case ROOM_FULL:
                redirectCode = ErrorCodes.REDIRECT_REASON_ROOM_FULL;
                break;
            case DUPLICATED_LOGIN:
                redirectCode = ErrorCodes.REDIRECT_REASON_DUPLICATED_LOGIN;
                break;
            case SERVER_ERROR:
                redirectCode = ErrorCodes.REDIRECT_REASON_SERVER_ERROR;
                break;
            case VERIFICATION:
                redirectCode = ErrorCodes.REDIRECT_REASON_VERIFICATION;
                break;
            case EXPIRED:
                redirectCode = ErrorCodes.REDIRECT_REASON_EXPIRED;
                break;
            case ROOM_CLOSE:
                redirectCode = ErrorCodes.REDIRECT_REASON_ROOM_CLOSED;
                break;
            case TO_CLOSE:
                redirectCode = ErrorCodes.REDIRECT_REASON_ROOM_TO_CLOSED;
                break;
            case SEAT_QUOTA:
                redirectCode = ErrorCodes.REDIRECT_REASON_SEAT_QUOTA;
                break;
            default:
                redirectCode = ErrorCodes.REDIRECT_REASON_UNKNOWN;
                break;
        }

        return redirectCode;
    }
}
