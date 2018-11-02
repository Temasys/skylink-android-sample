package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import sg.com.temasys.skylink.sdk.listener.DataTransferListener;
import sg.com.temasys.skylink.sdk.listener.FileTransferListener;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.MessagesListener;
import sg.com.temasys.skylink.sdk.listener.OsListener;
import sg.com.temasys.skylink.sdk.listener.RecordingListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.listener.StatsListener;
import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */

public abstract class SkylinkCommonService implements LifeCycleListener, MediaListener, OsListener, RemotePeerListener, MessagesListener,
        DataTransferListener, FileTransferListener, RecordingListener, StatsListener {

    private final String TAG = SkylinkCommonService.class.getName();

    protected Context mContext;

    //common presenter for all presenters
    protected BasePresenter mPresenter;

    //Skylink connection manager object
    private SkylinkConnectionManager mSkylinkConnectionManager;

    //these variables need to be static for configuration change
    protected static SkylinkConnection mSkylinkConnection;

    //list of peers joined in room
    protected static List<SkylinkPeer> mPeersList;

    protected String mRoomName;
    protected String mUserName;

    public SkylinkCommonService(Context context) {
        this.mContext = context;
        this.mSkylinkConnectionManager = new SkylinkConnectionManager(this, context);
    }

    protected static void setmSkylinkConnection(SkylinkConnection mSkylinkConnection) {
        SkylinkCommonService.mSkylinkConnection = mSkylinkConnection;
    }

    public abstract void setSkylinkListeners();

    public abstract SkylinkConfig getSkylinkConfig();

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        String log = "[onConnect] ";
        if (isSuccessful) {
            String localPeerId = mSkylinkConnection.getPeerId();

            log += "Connected to room " + getRoomRoomId(mRoomName) + " as " + mUserName + " (" + localPeerId + ")";
            toastLog(TAG, mContext, log);

            //init peers list and add self/local peer to list
            if (mPeersList == null) {
                mPeersList = new ArrayList<SkylinkPeer>();
            }

            //add self peer as a peer in list
            mPeersList.add(new SkylinkPeer(localPeerId, mUserName));

        } else {
            log += "Skylink failed to connect!\nReason : " + message;
            toastLog(TAG, mContext, log);
        }

        //update UI to connected state
        mPresenter.onServiceRequestConnect(isSuccessful);

    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        String log = "[onDisconnect] ";
        if (errorCode == Errors.DISCONNECT_FROM_ROOM) {
            log += "We have successfully disconnected from the room.";
        } else if (errorCode == Errors.DISCONNECT_UNEXPECTED_ERROR) {
            log += "WARNING! We have been unexpectedly disconnected from the room!";
        }
        log += " Server message: " + message;
        toastLogLong(TAG, mContext, log);

        //remove all peers in room
        if (mPeersList != null)
            mPeersList.clear();

        //update UI to disconnected state for all types call
        mPresenter.onServiceRequestDisconnect();
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        String log = "[onLockRoomStatusChange] ";
        log += "[SA] Peer " + remotePeerId + " changed Room locked status to "
                + lockStatus + ".";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onReceiveLog(int infoCode, String message) {
        Utils.handleSkylinkReceiveLog(infoCode, message, mContext, TAG);
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Utils.handleSkylinkWarning(errorCode, message, mContext, TAG);
    }

    @Override
    public void onLocalMediaCapture(SurfaceViewRenderer videoView) {
        Log.d(TAG, "[onLocalMediaCapture]");

        mPresenter.onServiceRequestLocalMediaCapture(videoView);
    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        String log = "[onRemotePeerAudioToggle] ";
        log += "Peer " + getPeerIdNick(remotePeerId) +
                " Audio mute status via:\r\nCallback: " + isMuted + ".";

        // It is also possible to get the mute status via the UserInfo.
        UserInfo userInfo = getUserInfo(remotePeerId);

        if (userInfo != null) {
            log += "\r\nUserInfo: " + userInfo.isAudioMuted() + ".";
        }
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onRemotePeerVideoToggle(String remotePeerId, boolean isMuted) {
        String log = "[onRemotePeerVideoToggle] ";
        log += "Peer " + getPeerIdNick(remotePeerId) +
                " Video mute status via:\r\nCallback: " + isMuted + ".";

        // It is also possible to get the mute status via the UserInfo.
        UserInfo userInfo = getUserInfo(remotePeerId);
        if (userInfo != null) {
            log += "\r\nUserInfo: " + userInfo.isVideoMuted() + ".";
        }
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, SurfaceViewRenderer videoView) {
        String log = "[onRemotePeerMediaReceive] ";
        log += "Received new ";
        if (videoView != null) {
            log += "Video ";
        } else {
            log += "Audio ";
        }
        log += "from Peer " + getPeerIdNick(remotePeerId) + ".\r\n";

        UserInfo remotePeerUserInfo = getUserInfo(remotePeerId);

        mPresenter.onServiceRequestRemotePeerMediaReceive(log, remotePeerUserInfo, remotePeerId);
    }

    @Override
    public void onInputVideoResolutionObtained(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        Log.d(TAG, "[onInputVideoResolutionObtained]");

        mPresenter.onServiceRequestInputVideoResolutionObtained(width, height, fps, captureFormat);
    }

    @Override
    public void onReceivedVideoResolutionObtained(String peerId, int width, int height, int fps) {
        Log.d(TAG, "[onReceivedVideoResolutionObtained]");

        mPresenter.onServiceRequestReceivedVideoResolutionObtained(peerId, width, height, fps);
    }

    @Override
    public void onSentVideoResolutionObtained(String peerId, int width, int height, int fps) {
        Log.d(TAG, "[onSentVideoResolutionObtained]");

        mPresenter.onServiceRequestSentVideoResolutionObtained(peerId, width, height, fps);
    }

    @Override
    public void onVideoSizeChange(String peerId, Point size) {
        Log.d(TAG, "[onVideoSizeChange]");

        mPresenter.onServiceRequestVideoSizeChange(peerId, size);
    }

    @Override
    public void onPermissionRequired(String[] permissions, int requestCode, int infoCode) {
        Log.d(TAG, "[onPermissionRequired]");
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        mPresenter.onServiceRequestPermissionRequired(info);
    }

    @Override
    public void onPermissionGranted(String[] permissions, int requestCode, int infoCode) {
        Log.d(TAG, "[onPermissionGranted]");
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        mPresenter.onServiceRequestPermissionGranted(info);
    }

    @Override
    public void onPermissionDenied(String[] permissions, int requestCode, int infoCode) {
        Log.d(TAG, "[onPermissionDenied]");
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        mPresenter.onServiceRequestPermissionDenied(mContext, info);
    }

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        String logTag = "[SA][onRemotePeerJoin] ";

        //not process if remote peer is MCU
        //just fix for running app
        //need to move to SDK
//        if(remotePeerId.equals("MCU")){
//            return;
//        }

        // When remote peer joins room, keep track of user and update text-view to display details
        String remotePeerName = null;
        if (userData instanceof String) {
            remotePeerName = (String) userData;
        } else if (userData instanceof JSONObject) {
            try {
                remotePeerName = ((JSONObject) userData).get("name").toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //add remote peer into list
        SkylinkPeer remotePeer = new SkylinkPeer(remotePeerId, remotePeerName);
        mPeersList.add(remotePeer);

        mPresenter.onServiceRequestRemotePeerJoin(remotePeer);

        logTag += "Your Peer " + getPeerIdNick(remotePeerId) + " connected.";
        toastLog(TAG, mContext, logTag);
    }

    @Override
    public void onRemotePeerConnectionRefreshed(String remotePeerId, Object userData, boolean hasDataChannel, boolean wasIceRestarted) {
        Log.d(TAG, "[onRemotePeerConnectionRefreshed]");
        String peer = "Skylink Media Relay server";
        if (remotePeerId != null) {
            peer = "Peer " + getPeerIdNick(remotePeerId);
        }
        String log = "Your connection with " + peer + " has just been refreshed";
        if (wasIceRestarted) {
            log += ", with ICE restarted.";
        } else {
            log += ".\r\n";
        }

        UserInfo remotePeerUserInfo = getUserInfo(remotePeerId);

        mPresenter.onServiceRequestRemotePeerConnectionRefreshed(log, remotePeerUserInfo);
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        // If Peer has no userData, use an empty string for nick.
        String nick = "";
        if (userData != null) {
            nick = userData.toString();
        }
        String log = "[SA][onRemotePeerUserDataReceive] Peer " + getPeerIdNick(remotePeerId) +
                ":\n" + nick;
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onOpenDataConnection(String remotePeerId) {
        Log.d(TAG, "onOpenDataConnection");
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message, UserInfo userInfo) {
        Log.d(TAG, "[onRemotePeerLeave]");

        //not process if remote peer is MCU
        //just fix for running app
        //need to move to SDK
//        if(remotePeerId.equals("MCU")){
//            return;
//        }

        //remove remote peer and keep the index of remote peer for multi party video call function
        int removeIndex = -1;
        for (int i = 0; i < mPeersList.size(); i++) {
            if (mPeersList.get(i).getPeerId().equals(remotePeerId)) {
                mPeersList.remove(i);
                removeIndex = i;
                break;
            }
        }

        mPresenter.onServiceRequestRemotePeerLeave(remotePeerId, removeIndex);

        int numRemotePeers = mPeersList.size() - 1;
        if (numRemotePeers >= 0) {
            String log = "Your Peer " + getPeerIdNick(remotePeerId, userInfo) + " ( " + remotePeerId + " ) left: " +
                    message + ". " + numRemotePeers + " remote Peer(s) left in the room.";
            toastLog(TAG, mContext, log);
        }
    }

    @Override
    public void onDataReceive(String remotePeerId, byte[] data) {
        Log.d(TAG, "[onDataReceive]");

        mPresenter.onServiceRequestDataReceive(mContext, remotePeerId, data);
    }

    @Override
    public void onFileTransferPermissionRequest(String remotePeerId, String fileName, boolean isPrivate) {
        Log.d(TAG, "[onFileTransferPermissionRequest]");

        mPresenter.onServiceRequestFileTransferPermissionRequest(remotePeerId, fileName, isPrivate);
    }

    @Override
    public void onFileTransferPermissionResponse(String remotePeerId, String fileName, boolean isPermitted) {
        Log.d(TAG, "[onFileTransferPermissionResponse]");

        mPresenter.onServiceRequestFileTransferPermissionResponse(mContext, remotePeerId, fileName, isPermitted);
    }

    @Override
    public void onFileTransferDrop(String remotePeerId, String fileName, String message, boolean isExplicit) {
        Log.d(TAG, "[onFileTransferDrop]");

        mPresenter.onServiceRequestFileTransferDrop(mContext, remotePeerId, fileName, message, isExplicit);
    }

    @Override
    public void onFileSendComplete(String remotePeerId, String fileName) {
        Log.d(TAG, "[onFileSendComplete]");

        mPresenter.onServiceRequestFileSendComplete(mContext, remotePeerId, fileName);
    }

    @Override
    public void onFileReceiveComplete(String remotePeerId, String fileName) {
        Log.d(TAG, "[onFileReceiveComplete]");

        mPresenter.onServiceRequestFileReceiveComplete(remotePeerId, fileName);
    }

    @Override
    public void onFileSendProgress(String remotePeerId, String fileName, double percentage) {
        Log.d(TAG, "[onFileSendProgress]");

        mPresenter.onServiceRequestFileSendProgress(mContext, remotePeerId, fileName, percentage);
    }

    @Override
    public void onFileReceiveProgress(String remotePeerId, String fileName, double percentage) {
        Log.d(TAG, "[onFileReceiveProgress]");

        mPresenter.onServiceRequestFileReceiveProgress(mContext, remotePeerId, fileName, percentage);
    }

    @Override
    public void onServerMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
        Log.d(TAG, "[onServerMessageReceive]");

        mPresenter.onServiceRequestServerMessageReceive(remotePeerId, message, isPrivate);
    }

    @Override
    public void onP2PMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
        Log.d(TAG, "[onP2PMessageReceive]");

        mPresenter.onServiceRequestP2PMessageReceive(remotePeerId, message, isPrivate);
    }

    @Override
    public void onRecordingStart(String recordingId) {
        Log.d(TAG, "[onRecordingStart]");

        mPresenter.onServiceRequestRecordingStart(mContext, mSkylinkConnection.isRecording());
    }

    @Override
    public void onRecordingStop(String recordingId) {
        Log.d(TAG, "[onRecordingStop]");

        mPresenter.onServiceRequestRecordingStop(mContext, mSkylinkConnection.isRecording());
    }

    @Override
    public void onRecordingVideoLink(String recordingId, String peerId, String videoLink) {
        Log.d(TAG, "[onRecordingVideoLink]");

        mPresenter.onServiceRequestRecordingVideoLink(recordingId, peerId, videoLink);
    }

    @Override
    public void onRecordingError(String recordingId, int errorCode, String description) {
        Log.d(TAG, "[onRecordingError]");

        mPresenter.onServiceRequestRecordingError(mContext, recordingId, errorCode, description);
    }

    @Override
    public void onTransferSpeedReceived(String peerId, int mediaDirection, int mediaType, double transferSpeed) {
        Log.d(TAG, "[onTransferSpeedReceived]");

        mPresenter.onServiceRequestTransferSpeedReceived(peerId, mediaDirection, mediaType, transferSpeed);
    }

    @Override
    public void onWebrtcStatsReceived(String peerId, int mediaDirection, int mediaType, HashMap<String, String> stats) {
        Log.d(TAG, "[onWebrtcStatsReceived]");

        mPresenter.onServiceRequestWebrtcStatsReceived(peerId, mediaDirection, mediaType, stats);
    }


    public boolean isConnectingOrConnected() {
        return mSkylinkConnectionManager.isConnectingOrConnected();
    }

    public void connectToRoom(Constants.CONFIG_TYPE typeCall) {
        mSkylinkConnectionManager.connectToRoom(typeCall);
    }

    public void disconnectFromRoom() {
        mSkylinkConnectionManager.disconnectFromRoom();
    }

    //static method for the other to access currentSkylinkConnection
    public static SkylinkConnection getCurrentSkylinkConnection() {
        return mSkylinkConnection;
    }

    /**
     * Check remote peers exist in room
     * if no remote peer, then local peer is alone in the room
     */
    public boolean isPeerJoin() {

        if (mPeersList != null) {
            int totalPeersInRoom = mPeersList.size();

            //check remote peer(s) in room
            if (totalPeersInRoom > 1) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get peerId of a Peer by its index using SkylinkConnection API.
     *
     * @param index 0 for self Peer, 1 onwards for remote Peer(s).
     * @return Desired peerId or null if not available.
     */
    public String getPeerId(int index) {

        if (mSkylinkConnection == null) {
            return null;
        }
        String[] peerIdList = mSkylinkConnection.getPeerIdList();
        // Ensure index does not exceed range of possible indices on peerIdList.
        if (index > peerIdList.length - 1 || index < 0) {
            return null;
        }
        return peerIdList[index];
    }

    /**
     * Get nickName or userName of a peer in room
     *
     * @param peerId null is local peer, no null is specific peer
     * @return Desired nickName or userName of the peer
     */
    public String getPeerIdNick(String peerId) {

        //get local peer from SDK
        if (peerId == null && mSkylinkConnection != null) {
            peerId = mSkylinkConnection.getPeerId();
        }

        String peerIdShow = peerId;

        //can not get local peer
        if (peerId == null) {
            peerIdShow = "Self";
        }

        String peerUserName = getUserDataString(peerIdShow);

        return peerUserName + " (" + peerId + ")";
    }

    private String getUserDataString(String peerId) {
        String userDataString = "";

        if (mSkylinkConnection != null) {
            Object userDataObject = mSkylinkConnection.getUserData(peerId);
            if (userDataObject != null) {
                userDataString = userDataObject.toString();
            }
        }
        return userDataString;
    }

    private String getPeerIdNick(String peerId, UserInfo userInfo) {
        return getUserDataString(userInfo);
    }

    private String getUserDataString(UserInfo userInfo) {
        if (userInfo == null && mSkylinkConnection != null) {
            userInfo = mSkylinkConnection.getUserInfo(null);
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

    private UserInfo getUserInfo(String userId) {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getUserInfo(userId);
        }
        return null;
    }

    public String getRoomName(String defaultName) {

        //in case of display room Name and room Id
        String roomId = "";
        if (mSkylinkConnection != null) {
            roomId = mSkylinkConnection.getRoomId();
        }

        return defaultName + "(" + roomId + ")";
    }

    public String getUserName(String peerId, String defaultName) {
        if (mSkylinkConnection == null) {
            return defaultName;
        }
        return getPeerIdNick(peerId);
    }

    public String getPeerNameById(String peerId) {
        if (mSkylinkConnection != null) {
            return getPeerIdNick(peerId);
        }

        return null;
    }

    public String getRoomPeerIdNick(Constants.CONFIG_TYPE typeCall) {
        if (mSkylinkConnection == null) {
            return null;
        }

        String roomName = Utils.getRoomNameByType(typeCall);

        String title = "Room: " + getRoomRoomId(roomName);
        // Add PeerId to title if a Peer occupies clicked location.
        title += "\r\n" + getPeerIdNick(getPeerId());
        return title;

    }

    public String getRoomRoomId(String roomName) {

        //in case of display room Name and room Id
        String roomId = "";
        if (mSkylinkConnection != null) {
            roomId = mSkylinkConnection.getRoomId();
        }

        return roomName + "(" + roomId + ")";
    }

    public String getPeerId() {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getPeerId();
        }

        return null;
    }

    public List<SkylinkPeer> getPeersList() {
        return mPeersList;
    }

    public int getTotalPeersInRoom() {
        if (mPeersList == null)
            return 0;

        return mPeersList.size();
    }

    public void setRoomName(String roomName) {
        mRoomName = roomName;
    }

    public void setUserName(String userName) {
        mUserName = userName;
    }

}
