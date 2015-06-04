package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

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
    private static final String PEER_CONNECTION_RESTART = "Peer connection is restarting";
    static final String CONNECTION_LOST = "Lost connection to room.";

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
            Log.d(TAG, "processRoomLockStatus: onLockRoomStatusChange " + lockStatus);
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
    static void sendRoomLockStatus(SkylinkConnectionService skylinkConnectionService, boolean lockStatus) throws JSONException {
        JSONObject dict = new JSONObject();
        dict.put("rid", skylinkConnectionService.getRoomId());
        dict.put("mid", skylinkConnectionService.getSid());
        dict.put("lock", lockStatus);
        dict.put("type", "roomLockEvent");
        skylinkConnectionService.sendMessage(dict);
        Log.d(TAG, "sendRoomLockStatus: sendMessage " + lockStatus);
    }

    /**
     * Processes restart
     *
     * @param remotePeerId
     * @param skylinkConnection
     * @return
     */
    static boolean processRestart(final String remotePeerId, final SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            // Dispose the peerConnection
            disposePeerConnection(remotePeerId, skylinkConnection);

            // Notify that the connection is restarting
            skylinkConnection.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    skylinkConnection.getRemotePeerListener().onRemotePeerLeave(
                            remotePeerId, PEER_CONNECTION_RESTART);
                }
            });

            // Should not create a peer connection at this time as it will be created
            // Later on when processing the welcome
            return true;
        }
        return false;
    }

    /**
     * Sends a restart message
     *
     * @param remotePeerId
     * @param skylinkConnection
     * @param skylinkConnectionService
     * @param localMediaStream
     * @param myConfig
     * @return
     * @throws JSONException
     */
    static boolean sendRestart(final String remotePeerId,
                               final SkylinkConnection skylinkConnection,
                               SkylinkConnectionService skylinkConnectionService,
                               MediaStream localMediaStream,
                               SkylinkConfig myConfig) throws JSONException {

        if (skylinkConnection != null) {

            // Dispose the peerConnection
            disposePeerConnection(remotePeerId, skylinkConnection);

            // Notify that the connection is restarting
            notifyPeerLeave(skylinkConnection, remotePeerId, PEER_CONNECTION_RESTART);

            // Create a new peer connection
            PeerConnection peerConnection = skylinkConnection
                    .getPeerConnection(remotePeerId, HealthChecker.ICE_ROLE_ANSWERER);

            // TODO: use exact value
            boolean receiveOnly = false;

            // Add our local media stream to this PC, or not.
            if ((myConfig.hasAudioSend() || myConfig.hasVideoSend()) && !receiveOnly) {
                peerConnection.addStream(localMediaStream);
                Log.d(TAG, "Added localMedia Stream");
            }

            if (peerConnection != null) {
                // Send "welcome".
                sendWelcome(remotePeerId, skylinkConnection, true);
            }

            return true;
        }

        return false;
    }

    /**
     * Notify that all remote peers are leaving.
     *
     * @param skylinkConnection
     * @param reason
     */
    static void notifyPeerLeaveAll(SkylinkConnection skylinkConnection, String reason) {
        Hashtable<String, SkylinkConnection.PCObserver> pcObserverPool = (Hashtable<String, SkylinkConnection.PCObserver>) skylinkConnection.getPcObserverPool();
        if (pcObserverPool != null) {
            // Create a new peerId set to prevent concurrent modification of the set
            Set<String> peerIdSet = new HashSet<String>(pcObserverPool.keySet());
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
                          SkylinkConnectionService skylinkConnectionService) throws JSONException {

        skylinkConnection.logMessage("*** SendEnter");
        JSONObject enterObject = new JSONObject();
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
        UserInfo userInfo = new UserInfo(skylinkConnection.getMyConfig(), skylinkConnection.getUserData(null));
        UserInfo.setUserInfo(enterObject, userInfo);
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
                               boolean isRestart) throws JSONException {

        SkylinkConnectionService skylinkConnectionService =
                skylinkConnection.getSkylinkConnectionService();
        SkylinkConfig myConfig = skylinkConnection.getMyConfig();

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
                    skylinkConnectionService.getSid());
            welcomeObject.put("target", remotePeerId);
            welcomeObject.put("rid",
                    skylinkConnectionService.getRoomId());
            welcomeObject.put("agent", "Android");
            welcomeObject.put("version", BuildConfig.VERSION_NAME);
            welcomeObject.put("receiveOnly", false);
            welcomeObject.put("enableIceTrickle", true);
            welcomeObject.put("enableDataChannel",
                    (myConfig.hasPeerMessaging() || myConfig.hasFileTransfer()
                            || myConfig.hasDataTransfer()));
            UserInfo userInfo = new UserInfo(myConfig, skylinkConnection.getUserData(null));
            UserInfo.setUserInfo(welcomeObject, userInfo);
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

        try {
            JSONObject msgJoinRoom = new JSONObject();
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
            skylinkConnectionService.sendMessage(msgJoinRoom);
            Log.d(TAG, "[SDK] Join Room msg: Sending...");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
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
            skylinkConnectionService.sendMessage(json);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }

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
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Dispose all PeerConnections and associated DC.
     *
     * @param skylinkConnection
     * @param reason
     */
    static void removeAllPeers(final SkylinkConnection skylinkConnection, final String reason) {
        Hashtable<String, SkylinkConnection.PCObserver> pcObserverPool = (Hashtable<String, SkylinkConnection.PCObserver>) skylinkConnection.getPcObserverPool();
        if (pcObserverPool != null) {
            // Create a new peerId set to prevent concurrent modification of the set
            Set<String> peerIdSet = new HashSet<String>(pcObserverPool.keySet());
            for (final String peerId : peerIdSet) {

                // Notify that the connection is restarting
                skylinkConnection.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        skylinkConnection.getRemotePeerListener().onRemotePeerLeave(
                                peerId, reason);
                    }
                });

                // Dispose DC
                if (skylinkConnection.getDataChannelManager() != null) {
                    skylinkConnection.getDataChannelManager().disposeDC(peerId);
                }
                // Dispose PeerConnection
                disposePeerConnection(peerId, skylinkConnection);
            }
        }
    }

    /**
     * Returns true if the peer connection is disposed successfully
     *
     * @param remotePeerId
     * @param skylinkConnection
     * @return
     */
    static boolean disposePeerConnection(String remotePeerId, SkylinkConnection skylinkConnection) {

        PeerConnection peerConnection = skylinkConnection.getPeerConnectionPool().get(remotePeerId);
        if (peerConnection != null) {

            // Dispose peer connection
            peerConnection.removeStream(skylinkConnection.getLocalMediaStream());
            peerConnection.dispose();

            skylinkConnection.getPeerConnectionPool().remove(remotePeerId);
            skylinkConnection.getPcObserverPool().remove(remotePeerId);
            skylinkConnection.getSdpObserverPool().remove(remotePeerId);
            skylinkConnection.getUserInfoMap().remove(remotePeerId);
            // This commenting is a hack to accommodate the non-Android clients until the update to SM 0.1.1.
            // PeerInfo of peer is required for sender of restart.
            // TODO XR: Remove commenting after JS client update to compatible restart protocol.
            // skylinkConnection.getPeerInfoMap().remove(remotePeerId);
            return true;
        }
        return false;
    }

    static void sendMuteAudio(boolean isMuted, SkylinkConnectionService skylinkConnectionService) {
        JSONObject dict = new JSONObject();
        try {
            dict.put("type", "muteAudioEvent");
            dict.put("mid", skylinkConnectionService.getSid());
            dict.put("rid", skylinkConnectionService.getRoomId());
            dict.put("muted", new Boolean(isMuted));
            skylinkConnectionService.sendMessage(dict);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    static void sendMuteVideo(boolean isMuted, SkylinkConnectionService skylinkConnectionService) {
        JSONObject dict = new JSONObject();
        try {
            dict.put("type", "muteVideoEvent");
            dict.put("mid", skylinkConnectionService.getSid());
            dict.put("rid", skylinkConnectionService.getRoomId());
            dict.put("muted", new Boolean(isMuted));
            skylinkConnectionService.sendMessage(dict);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
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
