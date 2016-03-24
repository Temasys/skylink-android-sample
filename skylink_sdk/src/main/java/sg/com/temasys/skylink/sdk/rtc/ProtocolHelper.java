package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.SessionDescription;

import java.util.Set;

import sg.com.temasys.skylink.sdk.BuildConfig;
import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logE;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logI;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logW;


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
    static final String PEER_CONNECTION_RESTART = "Peer connection is restarting";
    static final String CONNECTION_LOST = "Lost connection to room.";
    static final String DISCONNECTING = "Disconnecting from room.";

    private ProtocolHelper() {
    }

    /**
     * Processes a redirect message
     *
     * @param info
     * @param action
     * @param reason
     * @param lifeCycleListener
     * @return true if its a disconnection(reject) false if its a warning
     * @throws JSONException
     */
    static boolean processRedirect(String info, String action, String reason,
                                   LifeCycleListener lifeCycleListener) throws JSONException {

        int errorCode = ProtocolHelper.getRedirectCode(reason);
        String errorString = Errors.getErrorString(errorCode);
        if (errorString != null) {
            errorString = " " + errorString;
        } else {
            errorString = "";
        }

        boolean shouldDisconnect = false;

        // Send to user the error code and String (if any)
        if ("warning".equals(action)) {
            String warn = "[WARN:" + errorCode + "]" + errorString;
            String debug = warn + "\nDetails: " + info;
            lifeCycleListener.onWarning(errorCode, warn);
            logW(TAG, warn);
            logD(TAG, debug);
        } else if ("reject".equals(action)) {
            String error = "[ERROR:" + errorCode + "] We are being disconnected from the room."
                    + errorString;
            String debug = error + "\nDetails: " + info;
            logE(TAG, error);
            logD(TAG, debug);
            lifeCycleListener.onDisconnect(errorCode, error);
            shouldDisconnect = true;
        }

        return shouldDisconnect;
    }

    /**
     * Processes a room lock status
     *
     * @param currentRoomLockStatus
     * @param peerId
     * @param roomLock
     * @param lifeCycleListener
     * @return
     * @throws JSONException
     */
    static boolean processRoomLockStatus(boolean currentRoomLockStatus,
                                         String peerId, boolean roomLock,
                                         LifeCycleListener lifeCycleListener) throws JSONException {
        boolean lockStatus = roomLock;
        // Only post updates if received lock status is not the same
        if (lockStatus != currentRoomLockStatus) {
            lifeCycleListener.onLockRoomStatusChange(peerId, lockStatus);
            logI(TAG, "Room Lock Status has just been changed by Peer " + peerId +
                    " to: " + lockStatus + "!");
        }
        return lockStatus;
    }

    /**
     * Send room lock status
     *
     * @param skylinkConnectionService
     * @param lockStatus
     * @throws JSONException
     */
    static void sendRoomLockStatus(SkylinkConnectionService skylinkConnectionService, boolean lockStatus) {
        JSONObject dict = new JSONObject();
        try {
            dict.put("rid", skylinkConnectionService.getRoomId());
            dict.put("mid", skylinkConnectionService.getSid());
            dict.put("lock", lockStatus);
            dict.put("type", "roomLockEvent");
        } catch (JSONException e) {
            String error = "[ERROR] Unable to lock room!";
            String debug = error + "\nException: " + e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);
        }
        skylinkConnectionService.sendMessage(dict);
        logI(TAG, "Sending to Server new lock status: " + lockStatus + ".");
    }

    /**
     * Sends a restart message
     *
     * @param remotePeerId
     * @param skylinkConnection
     * @param localMediaStream
     * @param myConfig
     * @return True if restart initiation is successful (still pending outcome of handshake), false
     * otherwise.
     * @throws JSONException
     */
    static boolean sendRestart(final String remotePeerId,
                               final SkylinkConnection skylinkConnection,
                               MediaStream localMediaStream,
                               SkylinkConfig myConfig) {

        if (skylinkConnection != null) {

            // This is a hack to accommodate the non-Android clients until the update to SM 0.1.1.
            // PeerInfo of peer is required for sender of restart.
            // TODO XR: Remove all peerInfoHack and userInfoHack after JS client update to
            // compatible restart protocol.
            Peer peerOld = skylinkConnection.getSkylinkPeerService().getPeer(remotePeerId);
            PeerInfo peerInfoHack = peerOld.getPeerInfo();
            UserInfo userInfoHack = peerOld.getUserInfo();

            // Dispose, remove the Peer, and notify that the connection to Peer is restarting
            skylinkConnection.getSkylinkPeerService().removePeer(remotePeerId,
                    PEER_CONNECTION_RESTART, false);

            // Create a new peer
            // TODO XR: Remove all peerInfoHack and userInfoHack after JS client update to
            // compatible restart protocol.
            Peer peer = skylinkConnection.getSkylinkPeerService().createPeer(
                    remotePeerId, HealthChecker.ICE_ROLE_ANSWERER, userInfoHack, peerInfoHack);

            // We have reached the limit of max no. of Peers.
            if (peer == null) {
                logD(TAG, "[sendRestart] Unable to perform \"restart\" as we were unable to create a new Peer " +
                        "due to Peer number limit.");
                return false;
            }

            // Add our local media stream to this PC, or not.
            if ((myConfig.hasAudioSend() || myConfig.hasVideoSend())) {
                peer.getPc().addStream(localMediaStream);
                logD(TAG, "[sendRestart] Added localMedia Stream");
            }

            if (peer.getPc() != null) {
                // Send "welcome".
                sendWelcome(remotePeerId, skylinkConnection, true);
            }
            return true;
        }
        logD(TAG, "[sendRestart] Unable to perform \"restart\" as SkylinkConnection is not initiated.");
        return false;
    }

    /**
     * Notify that all remote peers are leaving.
     *
     * @param skylinkConnection
     * @param reason
     */
    static void notifyPeerLeaveAll(SkylinkConnection skylinkConnection, String reason) {
        if (skylinkConnection.getSkylinkPeerService().getPeerNumber() > 0) {
            // Create a new peerId set to prevent concurrent modification of the set
            Set<String> peerIdSet = skylinkConnection.getSkylinkPeerService().getPeerIdSet();
            for (String peerId : peerIdSet) {
                notifyPeerLeave(skylinkConnection, peerId, reason);
            }
        }
    }

    /**
     * Notify that a specific remote peer is leaving.
     *
     * @param skylinkConnection
     * @param remotePeerId
     * @param reason
     */
    static void notifyPeerLeave(final SkylinkConnection skylinkConnection, final String remotePeerId,
                                final String reason) {
        skylinkConnection.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                skylinkConnection.getRemotePeerListener().onRemotePeerLeave(
                        remotePeerId, reason);
            }
        });
    }

    /**
     * Send enter
     * <p/>
     * This is a hack to accomodate the non-Android clients until the update to SM 0.1.1 This is
     * esp. so for the JS clients which do not allow restarts for PeerIds without PeerConnection.
     *
     * @param remotePeerId             Set to null if sending to all Peers in room. Set to PeerId of
     *                                 remote Peer if targeted to send only to this remote Peer.
     * @param skylinkConnection
     * @param skylinkConnectionService
     * @throws JSONException
     */
    static void sendEnter(String remotePeerId,
                          SkylinkConnection skylinkConnection,
                          SkylinkConnectionService skylinkConnectionService) throws SkylinkException {

        logD(TAG, "*** SendEnter");
        JSONObject enterObject = new JSONObject();
        UserInfo userInfo = new UserInfo(skylinkConnection.getSkylinkConfig(),
                skylinkConnection.getSkylinkPeerService().getUserData(null));
        try {
            enterObject.put("type", "enter");
            enterObject.put("mid", skylinkConnectionService.getSid());
            enterObject.put("rid", skylinkConnectionService.getRoomId());
            enterObject.put("receiveOnly", false);
            enterObject.put("agent", "Android");
            enterObject.put("version", BuildConfig.VERSION_NAME);
            // TODO XR: Can remove after JS client update to compatible restart protocol.
            if (remotePeerId != null) {
                enterObject.put("target", remotePeerId);
            }
            UserInfo.setUserInfo(enterObject, userInfo);
        } catch (JSONException e) {
            String error = "[ERROR:" + Errors.SIG_MSG_UNABLE_TO_CREATE_ENTER_JSON +
                    "] Unable to connect with Peer(s) in the room!";
            String debug = error + "\nSIG_MSG_UNABLE_TO_CREATE_ENTER_JSON Exception:\n" +
                    e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);
            throw new SkylinkException(debug);
        }
        skylinkConnectionService.sendMessage(enterObject);
    }

    /**
     * Set isRestart to true/false to create restart/welcome.
     *
     * @param remotePeerId
     * @param skylinkConnection
     * @param isRestart
     * @return
     * @throws JSONException
     */
    static boolean sendWelcome(String remotePeerId,
                               SkylinkConnection skylinkConnection,
                               boolean isRestart) {

        SkylinkConnectionService skylinkConnectionService =
                skylinkConnection.getSkylinkConnectionService();
        SkylinkConfig myConfig = skylinkConnection.getSkylinkConfig();
        Peer peer = skylinkConnection.getSkylinkPeerService().getPeer(remotePeerId);

        String typeStr = "restart";
        if (!isRestart) {
            typeStr = "welcome";
        }

        if (skylinkConnection != null) {

            logD(TAG, "[SDK] onMessage - Sending '" + typeStr + "'.");

            JSONObject welcomeObject = new JSONObject();
            try {
                welcomeObject.put("type", typeStr);
                welcomeObject.put("weight", peer.getWeight());
                welcomeObject.put("mid", skylinkConnectionService.getSid());
                welcomeObject.put("target", remotePeerId);
                welcomeObject.put("rid", skylinkConnectionService.getRoomId());
                welcomeObject.put("agent", "Android");
                welcomeObject.put("version", BuildConfig.VERSION_NAME);
                welcomeObject.put("receiveOnly", false);
                welcomeObject.put("enableIceTrickle", true);
                welcomeObject.put("enableDataChannel",
                        (myConfig.hasPeerMessaging() || myConfig.hasFileTransfer()
                                || myConfig.hasDataTransfer()));
            } catch (JSONException e) {
                String error = "[ERROR] Unable to welcome Peer " + remotePeerId + "!";
                String debug = error + "\nUnable to create welcome JSON. Exception:\n" +
                        e.getMessage();
                logE(TAG, error);
                logD(TAG, debug);
            }
            UserInfo userInfo = new UserInfo(myConfig, skylinkConnection.getSkylinkPeerService()
                    .getUserData(null));
            try {
                UserInfo.setUserInfo(welcomeObject, userInfo);
            } catch (JSONException e) {
                String error = "[ERROR:" + Errors.HANDSHAKE_UNABLE_TO_SET_USERINFO_IN_WELCOME +
                        "] Unable to welcome Peer " + remotePeerId + "!";
                String debug = error + "\nHANDSHAKE_UNABLE_TO_SET_USERINFO_IN_WELCOME " +
                        "\nException: " + e.getMessage();
                logE(TAG, error);
                logD(TAG, debug);
            }
            skylinkConnectionService.sendMessage(welcomeObject);

            return true;
        }

        return false;
    }

    /**
     * Send joinRoom message to Signaling Server.
     *
     * @param skylinkConnectionService
     */
    static void sendJoinRoom(SkylinkConnectionService skylinkConnectionService) {

        JSONObject msgJoinRoom = new JSONObject();
        try {
            msgJoinRoom.put("type", "joinRoom");
            msgJoinRoom.put("rid",
                    skylinkConnectionService.getRoomId());
            msgJoinRoom.put("uid",
                    skylinkConnectionService.getUserId());
            msgJoinRoom.put("roomCred",
                    skylinkConnectionService.getRoomCred());
            msgJoinRoom.put("cid",
                    skylinkConnectionService.getCid());
            msgJoinRoom.put("userCred",
                    skylinkConnectionService.getUserCred());
            msgJoinRoom.put("timeStamp",
                    skylinkConnectionService.getTimeStamp());
            msgJoinRoom.put("apiOwner",
                    skylinkConnectionService.getAppOwner());
            msgJoinRoom.put("len",
                    skylinkConnectionService.getLen());
            msgJoinRoom.put("start",
                    skylinkConnectionService.getStart());
        } catch (JSONException e) {
            String error = "[ERROR:" + Errors.HANDSHAKE_UNABLE_TO_CREATE_JOINROOM_JSON +
                    "] Unable to initiate connection to room!";
            String debug = error + " Exception: " + e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);
            return;
        }
        skylinkConnectionService.sendMessage(msgJoinRoom);
        logD(TAG, "[SDK] Join Room msg: Sending...");
    }

    /**
     * Send candidate message to Signaling Server.
     *
     * @param skylinkConnectionService
     * @param candidate
     * @param peerId
     */
    static void sendCandidate(SkylinkConnectionService skylinkConnectionService,
                              final IceCandidate candidate, String peerId) {

        JSONObject json = new JSONObject();
        try {
            json.put("type", "candidate");
            json.put("label", candidate.sdpMLineIndex);
            json.put("id", candidate.sdpMid);
            json.put("candidate", candidate.sdp);
            json.put("mid", skylinkConnectionService.getSid());
            json.put("rid", skylinkConnectionService.getRoomId());
            json.put("target", peerId);
        } catch (JSONException e) {
            String warn = "[ERROR:" + Errors.HANDSHAKE_UNABLE_TO_CREATE_CANDIDATE_JSON + "]";
            String debug = warn + " Exception: " + e.getMessage();
            logW(TAG, warn);
            logD(TAG, debug);
            return;
        }
        skylinkConnectionService.sendMessage(json);
    }

    /**
     * Send candidate message to Signaling Server.
     *
     * @param skylinkConnectionService
     * @param sdp
     * @param peerId
     */
    static void sendSdp(SkylinkConnectionService skylinkConnectionService,
                        SessionDescription sdp, String peerId) {

        JSONObject json = new JSONObject();
        try {
            json.put("type", sdp.type.canonicalForm());
            json.put("sdp", sdp.description);
            json.put("mid", skylinkConnectionService.getSid());
            json.put("target", peerId);
            json.put("rid", skylinkConnectionService.getRoomId());
            skylinkConnectionService.sendMessage(json);
        } catch (JSONException e) {
            String error = "[ERROR:" + Errors.HANDSHAKE_UNABLE_TO_CREATE_SDP_JSON +
                    "] Unable to generate information required to connect with Peer " + peerId + "!";
            String debug = error + "\nDetails: Unable to form SDP JSON. Exception:\n" +
                    e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);
        }
        skylinkConnectionService.sendMessage(json);
    }

    static void sendMuteAudio(boolean isMuted, SkylinkConnectionService skylinkConnectionService) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", "muteAudioEvent");
            json.put("mid", skylinkConnectionService.getSid());
            json.put("rid", skylinkConnectionService.getRoomId());
            json.put("muted", new Boolean(isMuted));
            skylinkConnectionService.sendMessage(json);
        } catch (JSONException e) {
            String error = "[ERROR:" + Errors.HANDSHAKE_UNABLE_TO_CREATE_MUTE_AUDIO_JSON +
                    "] Unable to inform Peer(s) that we toggled Audio mute status!";
            String debug = error + " Reason: Unable to form mute audio JSON. Exception:\n" +
                    e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);
        }
        skylinkConnectionService.sendMessage(json);
    }

    static void sendMuteVideo(boolean isMuted, SkylinkConnectionService skylinkConnectionService) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", "muteVideoEvent");
            json.put("mid", skylinkConnectionService.getSid());
            json.put("rid", skylinkConnectionService.getRoomId());
            json.put("muted", new Boolean(isMuted));
            skylinkConnectionService.sendMessage(json);
        } catch (JSONException e) {
            String error = "[ERROR:" + Errors.HANDSHAKE_UNABLE_TO_CREATE_MUTE_VIDEO_JSON +
                    "] Unable to inform Peer(s) that we toggled video mute status!";
            String debug = error + " Reason: Unable to form mute video JSON. Exception:\n" +
                    e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);
        }
        skylinkConnectionService.sendMessage(json);
    }

    static void sendPingMessage(SkylinkConnection skylinkConnection,
                                String target) throws JSONException {
        JSONObject pingObject = new JSONObject();
        pingObject.put("type", "ping");
        pingObject.put("mid", skylinkConnection.getSkylinkConnectionService().getSid());
        pingObject.put("target", target);
        pingObject.put("rid",
                skylinkConnection.getSkylinkConnectionService().getRoomId());
        skylinkConnection.getSkylinkConnectionService().sendMessage(pingObject);
    }

    private static int getRedirectCode(String reason) {
        int redirectCode;
        switch (reason) {
            case FAST_MSG:
                redirectCode = Errors.REDIRECT_REASON_FAST_MSG;
                break;
            case LOCKED:
                redirectCode = Errors.REDIRECT_REASON_LOCKED;
                break;
            case ROOM_FULL:
                redirectCode = Errors.REDIRECT_REASON_ROOM_FULL;
                break;
            case DUPLICATED_LOGIN:
                redirectCode = Errors.REDIRECT_REASON_DUPLICATED_LOGIN;
                break;
            case SERVER_ERROR:
                redirectCode = Errors.REDIRECT_REASON_SERVER_ERROR;
                break;
            case VERIFICATION:
                redirectCode = Errors.REDIRECT_REASON_VERIFICATION;
                break;
            case EXPIRED:
                redirectCode = Errors.REDIRECT_REASON_EXPIRED;
                break;
            case ROOM_CLOSE:
                redirectCode = Errors.REDIRECT_REASON_ROOM_CLOSED;
                break;
            case TO_CLOSE:
                redirectCode = Errors.REDIRECT_REASON_ROOM_TO_CLOSED;
                break;
            case SEAT_QUOTA:
                redirectCode = Errors.REDIRECT_REASON_SEAT_QUOTA;
                break;
            default:
                redirectCode = Errors.REDIRECT_REASON_UNKNOWN;
                break;
        }

        return redirectCode;
    }
}
