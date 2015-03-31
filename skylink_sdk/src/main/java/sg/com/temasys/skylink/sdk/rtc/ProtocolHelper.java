package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

import sg.com.temasys.skylink.sdk.BuildConfig;
import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
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

    /**
     * Processes a redirect message
     *
     * @param jsonObject
     * @param lifeCycleListener
     * @return true if its a disconnection(reject) false if its a warning
     * @throws JSONException
     */
    static boolean processRedirect(JSONObject jsonObject,
                                   LifeCycleListener lifeCycleListener) throws JSONException {

        String info = jsonObject.getString("info");
        String action = jsonObject.getString("action");

        // If the reason key exist, get the relevant error code
        String reason = jsonObject.getString("reason");
        int errorCode = ProtocolHelper.getRedirectCode(reason);

        boolean shouldDisconnect = false;

        if ("warning".equals(action)) {
            // Send back the info received and the derived error code
            lifeCycleListener.onWarning(errorCode, info);
            Log.d(TAG, "processRedirect: onWarning " + errorCode);
        } else if ("reject".equals(action)) {
            // Send back the info received and the derived error code
            lifeCycleListener.onDisconnect(errorCode, info);
            Log.d(TAG, "processRedirect: onDisconnect " + errorCode);
            shouldDisconnect = true;
        }

        return shouldDisconnect;
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

    static boolean processRestart(String remotePeerId, MediaStream localMediaStream,
                                  SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            // Dispose the peerConnection
            disposePeerConnection(remotePeerId, skylinkConnection, localMediaStream);
            // Should not create a peer connection at this time as it will be created
            // Later on when processing the welcome
            return true;
        }
        return false;
    }

    static boolean sendRestart(String remotePeerId,
                               SkylinkConnection skylinkConnection,
                               WebServerClient webServerClient,
                               MediaStream localMediaStream,
                               SkylinkConfig myConfig) throws JSONException {

        if (skylinkConnection != null) {

            // Dispose the peerConnection
            disposePeerConnection(remotePeerId, skylinkConnection, localMediaStream);

            // Create a new peer connection
            PeerConnection peerConnection = skylinkConnection
                    .getPeerConnection(remotePeerId);

            // TODO: use exact value
            boolean receiveOnly = false;

            // TODO: enableIceTrickle, enableDataChannel

            // Add our local media stream to this PC, or not.
            if ((myConfig.hasAudioSend() || myConfig.hasVideoSend()) && !receiveOnly) {
                peerConnection.addStream(localMediaStream);
                Log.d(TAG, "Added localMedia Stream");
            }

            if (peerConnection != null) {
                // Send "welcome".
                sendWelcome(remotePeerId, skylinkConnection, webServerClient, myConfig, true);
            }

            return true;
        }

        return false;
    }

    // Set isRestart to true/false to create restart/welcome.
    static boolean sendWelcome(String remotePeerId,
                               SkylinkConnection skylinkConnection,
                               WebServerClient webServerClient,
                               SkylinkConfig myConfig,
                               boolean isRestart) throws JSONException {

        String typeStr = "restart";
        if (!isRestart) {
            typeStr = "welcome";
        }

        if (skylinkConnection != null) {

            Log.d(TAG, "[SDK] onMessage - Sending '" + typeStr + "'.");

            JSONObject welcomeObject = new JSONObject();
            welcomeObject.put("type", typeStr);
            welcomeObject.put("weight",
                    skylinkConnection.getPcObserverPool().get(remotePeerId)
                            .getMyWeight());
            welcomeObject.put("mid",
                    webServerClient.getSid());
            welcomeObject.put("target", remotePeerId);
            welcomeObject.put("rid",
                    webServerClient.getRoomId());
            welcomeObject.put("agent", "Android");
            welcomeObject.put("version", BuildConfig.VERSION_NAME);
            welcomeObject.put("receiveOnly", false);
            welcomeObject.put("enableIceTrickle", true);
            welcomeObject.put("enableDataChannel",
                    (myConfig.hasPeerMessaging() || myConfig.hasFileTransfer()
                            || myConfig.hasDataTransfer()));
            skylinkConnection.setUserInfo(welcomeObject);
            webServerClient
                    .sendMessage(welcomeObject);

            return true;
        }

        return false;
    }

    /**
     * Returns true if the peer connection is disposed successfully
     *
     * @param remotePeerId
     * @param localMediaStream
     * @param skylinkConnection
     * @return
     */

    static boolean disposePeerConnection(String remotePeerId, SkylinkConnection skylinkConnection,
                                         MediaStream localMediaStream) {

        PeerConnection peerConnection = skylinkConnection.getPeerConnectionPool().get(remotePeerId);
        if (peerConnection != null) {

            // Dispose peer connection
            peerConnection.removeStream(localMediaStream);
            peerConnection.dispose();

            skylinkConnection.getPeerConnectionPool().remove(remotePeerId);
            skylinkConnection.getPcObserverPool().remove(remotePeerId);
            skylinkConnection.getSdpObserverPool().remove(remotePeerId);
            skylinkConnection.getDisplayNameMap().remove(remotePeerId);
            return true;
        }

        return false;
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
