package sg.com.temasys.skylink.sdk.sampleapp.data.service;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

import java.util.Date;
import java.util.HashMap;

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
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioCallContract;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoCallContract;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class SDKService implements LifeCycleListener, MediaListener, OsListener, RemotePeerListener, MessagesListener,
        DataTransferListener, FileTransferListener, RecordingListener, StatsListener {

    private final String TAG = SDKService.class.getName();

    protected Constants.CONFIG_TYPE mTypeCall;

    private Context mContext;

    private SdkConnectionManager mSdkConnectionManager;

    protected AudioCallContract.Presenter mAudioPresenter;

    protected VideoCallContract.Presenter mVideoPresenter;

    //these variables need to be static for configuration change
    protected static SkylinkConnection mSkylinkConnection;

    public static SkylinkPeer mSkylinkPeer;

    private String mRoomName;
    private String mUserName;

    public SDKService(Context context) {
        this.mContext = context;
    }

    private boolean setListeners() {
        if (mSkylinkConnection != null) {

            //common listener for all
            mSkylinkConnection.setLifeCycleListener(this);
            mSkylinkConnection.setRemotePeerListener(this);

            //MediaListener for audio, video, multi_video call
            if (mTypeCall == Constants.CONFIG_TYPE.AUDIO || mTypeCall == Constants.CONFIG_TYPE.VIDEO ||
                    mTypeCall == Constants.CONFIG_TYPE.MULTI_PARTY_VIDEO) {
                mSkylinkConnection.setMediaListener(this);
            }

            //OsListener for audio, video, file, multi_video call
            if (mTypeCall == Constants.CONFIG_TYPE.AUDIO || mTypeCall == Constants.CONFIG_TYPE.VIDEO ||
                    mTypeCall == Constants.CONFIG_TYPE.FILE | mTypeCall == Constants.CONFIG_TYPE.MULTI_PARTY_VIDEO) {
                mSkylinkConnection.setOsListener(this);
            }

            //MessagesListener for chat
            if (mTypeCall == Constants.CONFIG_TYPE.CHAT)
                mSkylinkConnection.setMessagesListener(this);

            //DataTransferListener for data transfer
            if (mTypeCall == Constants.CONFIG_TYPE.DATA)
                mSkylinkConnection.setDataTransferListener(this);

            //FileTransferListener for file transfer
            if (mTypeCall == Constants.CONFIG_TYPE.FILE)
                mSkylinkConnection.setFileTransferListener(this);

            //RecordingListener and StatsListener for multi_video call
            if (mTypeCall == Constants.CONFIG_TYPE.MULTI_PARTY_VIDEO) {
                mSkylinkConnection.setRecordingListener(this);
                mSkylinkConnection.setStatsListener(this);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        if (isSuccessful) {
            String log = "Connected to room " + mRoomName + " (" + mSkylinkConnection.getRoomId() +
                    ") as " + mSkylinkConnection.getPeerId() + " (" + mUserName + ").";
            toastLogLong(TAG, mContext, log);

        } else {
            String log = "Skylink failed to connect!\nReason : " + message;
            toastLogLong(TAG, mContext, log);
        }

        //update UI to connected state for all types call
        switch (mTypeCall) {
            case AUDIO:
                if (mAudioPresenter != null) {
                    mAudioPresenter.onConnectPresenterHandler(isSuccessful);
                }
                break;
            case VIDEO:
                if (mVideoPresenter != null) {
                    mVideoPresenter.onConnectPresenterHandler(isSuccessful);
                }
                break;
        }


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

        //update UI to disconnect state
        if (mAudioPresenter != null) {
            mAudioPresenter.onDisconnectPresenterHandler();
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onDisconnectPresenterHandler();
        }

    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        String log = "[SA] Peer " + remotePeerId + " changed Room locked status to "
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
        Log.d(TAG, "onLocalMediaCapture");

        //only video call need to implement this callback
        if (mVideoPresenter != null)
            mVideoPresenter.onLocalMediaCapturePresenterHandler(videoView);
    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        String log = "Peer " + getPeerIdNickServiceHandler(remotePeerId) +
                " Audio mute status via:\r\nCallback: " + isMuted + ".";

        // It is also possible to get the mute status via the UserInfo.
        UserInfo userInfo = getUserInfoServiceHandler(remotePeerId);

        if (userInfo != null) {
            log += "\r\nUserInfo: " + userInfo.isAudioMuted() + ".";
        }
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onRemotePeerVideoToggle(String remotePeerId, boolean isMuted) {
        String log = "Peer " + getPeerIdNickServiceHandler(remotePeerId) +
                " Video mute status via:\r\nCallback: " + isMuted + ".";

        // It is also possible to get the mute status via the UserInfo.
        UserInfo userInfo = getUserInfoServiceHandler(remotePeerId);
        if (userInfo != null) {
            log += "\r\nUserInfo: " + userInfo.isVideoMuted() + ".";
        }
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, SurfaceViewRenderer videoView) {
        String log = "Received new ";
        if (videoView != null) {
            log += "Video ";
        } else {
            log += "Audio ";
        }
        log += "from Peer " + getPeerIdNickServiceHandler(remotePeerId) + ".\r\n";

        UserInfo remotePeerUserInfo = getUserInfoServiceHandler(remotePeerId);

        if (mAudioPresenter != null) {
            mAudioPresenter.onRemotePeerMediaReceivePresenterHandler(log, remotePeerUserInfo);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onRemotePeerMediaReceivePresenterHandler(log, remotePeerUserInfo);
        }
    }

    @Override
    public void onInputVideoResolutionObtained(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {

        //only video call need to implement this callback
        if (mVideoPresenter != null)
            mVideoPresenter.onInputVideoResolutionObtainedPresenterHandler(width, height, fps, captureFormat);
    }

    @Override
    public void onReceivedVideoResolutionObtained(String peerId, int width, int height, int fps) {

        //only video call need to implement this callback
        if (mVideoPresenter != null)
            mVideoPresenter.onReceivedVideoResolutionObtainedPresenterHandler(peerId, width, height, fps);
    }

    @Override
    public void onSentVideoResolutionObtained(String peerId, int width, int height, int fps) {

        //only video call need to implement this callback
        if (mVideoPresenter != null)
            mVideoPresenter.onSentVideoResolutionObtainedPresenterHandler(peerId, width, height, fps);
    }

    @Override
    public void onVideoSizeChange(String peerId, Point size) {

        //only video call need to implement this callback
        if (mVideoPresenter != null)
            mVideoPresenter.onVideoSizeChangePresenterHandler(peerId, size);
    }

    @Override
    public void onPermissionRequired(String[] permissions, int requestCode, int infoCode) {

        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        if (mAudioPresenter != null) {
            mAudioPresenter.onPermissionRequiredPresenterHandler(info);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onPermissionRequiredPresenterHandler(info);
        }
    }

    @Override
    public void onPermissionGranted(String[] permissions, int requestCode, int infoCode) {

        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        if (mAudioPresenter != null) {
            mAudioPresenter.onPermissionGrantedPresenterHandler(info);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onPermissionGrantedPresenterHandler(info);
        }
    }

    @Override
    public void onPermissionDenied(String[] permissions, int requestCode, int infoCode) {

        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        if (mAudioPresenter != null) {
            mAudioPresenter.onPermissionDeniedPresenterHandler(info);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onPermissionDeniedPresenterHandler(info);
        }
    }

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        // When remote peer joins room, keep track of user and update text-view to display details
        String remotePeerName = null;
        if (userData instanceof String) {
            remotePeerName = (String) userData;
        }

        mSkylinkPeer = new SkylinkPeer(remotePeerId, remotePeerName);

        if (mAudioPresenter != null) {
            mAudioPresenter.onRemotePeerJoinPresenterHandler(mSkylinkPeer);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onRemotePeerJoinPresenterHandler(mSkylinkPeer);
        }

        String log = "Your Peer " + getPeerIdNickServiceHandler(remotePeerId) + " connected.";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onRemotePeerConnectionRefreshed(String remotePeerId, Object userData, boolean hasDataChannel, boolean wasIceRestarted) {
        String peer = "Skylink Media Relay server";
        if (remotePeerId != null) {
            peer = "Peer " + getPeerIdNickServiceHandler(remotePeerId);
        }
        String log = "Your connection with " + peer + " has just been refreshed";
        if (wasIceRestarted) {
            log += ", with ICE restarted.";
        } else {
            log += ".\r\n";
        }

        UserInfo remotePeerUserInfo = getUserInfoServiceHandler(remotePeerId);
        if (mAudioPresenter != null) {
            mAudioPresenter.onRemotePeerConnectionRefreshedPresenterHandler(log, remotePeerUserInfo);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onRemotePeerConnectionRefreshedPresenterHandler(log, remotePeerUserInfo);
        }
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        // If Peer has no userData, use an empty string for nick.
        String nick = "";
        if (userData != null) {
            nick = userData.toString();
        }
        String log = "[SA][onRemotePeerUserDataReceive] Peer " + getPeerIdNickServiceHandler(remotePeerId) +
                ":\n" + nick;
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onOpenDataConnection(String remotePeerId) {
        Log.d(TAG, "onOpenDataConnection");
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message, UserInfo userInfo) {
        //reset mAudioRemotePeer
        mSkylinkPeer = null;

        if (mAudioPresenter != null) {
            mAudioPresenter.onRemotePeerLeavePresenterHandler(remotePeerId);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onRemotePeerLeavePresenterHandler(remotePeerId);
        }

        int numRemotePeers = getNumRemotePeersServiceHandler();

        String log = "Your Peer " + getPeerIdNickServiceHandler(remotePeerId, userInfo) + " left: " +
                message + ". " + numRemotePeers + " remote Peer(s) left in the room.";
        toastLog(TAG, mContext, log);
    }

    public void connectToRoomServiceHandler() {
        mSdkConnectionManager = new SdkConnectionManager(mContext);

        // Initialize the skylink connection using SdkConnectionManager
        mSkylinkConnection = mSdkConnectionManager.initializeSkylinkConnection(mTypeCall);

        setListeners();

        mRoomName = Utils.getRoomNameByType(mTypeCall);
        mUserName = Utils.getUserNameByType(mTypeCall);

        // Create the Skylink connection string.
        // In production, the connection string should be generated by an external entity
        // (such as a secure App server that has the Skylink App Key secret), and sent to the App.
        // This is to avoid keeping the App Key secret within the application, for better security.
        String skylinkConnectionString = Utils.getSkylinkConnectionString(
                mRoomName, new Date(), SkylinkConnection.DEFAULT_DURATION);

        // The skylinkConnectionString should not be logged in production,
        // as it contains potentially sensitive information like the Skylink App Key ID.
        boolean connectFailed = !mSkylinkConnection.connectToRoom(skylinkConnectionString, mUserName);

        if (connectFailed) {
            String log = "Unable to connect to room!";
            toastLog(TAG, mContext, log);
            return;
        } else {
            String log = "Connecting...";
            toastLog(TAG, mContext, log);
        }

        // Initialize and use the Audio router to switch between headphone and headset
        if (mTypeCall == Constants.CONFIG_TYPE.AUDIO || mTypeCall == Constants.CONFIG_TYPE.VIDEO || mTypeCall == Constants.CONFIG_TYPE.MULTI_PARTY_VIDEO)
            AudioRouter.startAudioRouting(mContext);
    }

    public void disconnectFromRoomServiceHandler() {

        mSkylinkConnection.disconnectFromRoom();

        if (mTypeCall == Constants.CONFIG_TYPE.AUDIO || mTypeCall == Constants.CONFIG_TYPE.VIDEO || mTypeCall == Constants.CONFIG_TYPE.MULTI_PARTY_VIDEO)
            AudioRouter.stopAudioRouting(mContext);
    }

    public boolean isConnectingOrConnectedServiceHandler() {
        if (mSkylinkConnection == null) return false;

        SkylinkConnection.SkylinkState state = mSkylinkConnection.getSkylinkState();
        if (state == SkylinkConnection.SkylinkState.CONNECTING ||
                state == SkylinkConnection.SkylinkState.CONNECTED) {
            return true;
        }
        return false;
    }

    //static method for the other to access currentSkylinkConnection
    public static SkylinkConnection getCurrentSkylinkConnection() {
        return mSkylinkConnection;
    }

    private String getPeerIdNickServiceHandler(String peerId) {
        String peerIdShow = peerId;
        if (peerId == null) {
            if (mSkylinkConnection != null) {
                peerIdShow = mSkylinkConnection.getPeerId();
            }
            if (peerIdShow == null) {
                peerIdShow = "Self";
            }
        }
        final String peerIdNick = "\"" + peerIdShow + "\"(" + getUserDataStringServiceHandler(peerId) + ")";
        return peerIdNick;
    }

    private String getPeerIdNickServiceHandler(SkylinkConnection skylinkConnection, String peerId) {
        String peerIdShow = peerId;
        if (peerId == null) {
            if (skylinkConnection != null) {
                peerIdShow = skylinkConnection.getPeerId();
            }
            if (peerIdShow == null) {
                peerIdShow = "Self";
            }
        }
        final String peerIdNick = "\"" + peerIdShow + "\"(" + getUserDataStringServiceHandler(peerId) + ")";
        return peerIdNick;
    }

    private String getPeerIdNickServiceHandler(String peerId, UserInfo userInfo) {
        return peerId + " (" + getUserDataStringServiceHandler(userInfo) + ")";
    }

    private String getUserDataStringServiceHandler(String peerId) {
        String userDataString = "";

        if (mSkylinkConnection != null) {
            Object userDataObject = mSkylinkConnection.getUserData(peerId);
            if (userDataObject != null) {
                userDataString = userDataObject.toString();
            }
        }
        return userDataString;
    }

    private String getUserDataStringServiceHandler(UserInfo userInfo) {
        if (userInfo == null) {
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

    private UserInfo getUserInfoServiceHandler(String userId) {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getUserInfo(userId);
        }

        return null;

    }

    private int getNumRemotePeersServiceHandler() {
        int totalInRoom = getTotalInRoomServiceHandler();
        if (totalInRoom == 0) {
            return 0;
        }
        // The first Peer is the local Peer.
        return totalInRoom - 1;
    }

    private int getTotalInRoomServiceHandler() {
        String[] peerIdList = mSkylinkConnection.getPeerIdList();

        if (peerIdList == null) {
            return 0;
        }
        // Size of array is number of Peers in room.
        return peerIdList.length;
    }

    public String getRoomNameServiceHandler(String defaultName) {
        String roomId = "";
        if (mSkylinkConnection != null) {
            roomId = mSkylinkConnection.getRoomId();
        }
        return defaultName + " (" + roomId + ")";
    }

    public String getUserNameServiceHandler(String peerId, String defaultName) {
        if (mSkylinkConnection == null) {
            return defaultName;
        }
        return getPeerIdNickServiceHandler(peerId);
    }

    public boolean isPeerJoinServiceHandler() {
        return mSkylinkPeer == null ? false : true;
    }

    public String getRoomPeerIdNickServiceHandler() {
        if (mSkylinkConnection == null) {
            return null;
        }

        String roomName = Config.ROOM_NAME_VIDEO;

        String title = "Room: " + getRoomRoomIdServiceHandler(roomName);
        // Add PeerId to title if a Peer occupies clicked location.
        title += "\r\n" + getPeerIdNickServiceHandler(getPeerIdServiceHandler());
        return title;

    }

    public String getRoomRoomIdServiceHandler(String roomName) {
        String roomId = "";
        if (mSkylinkConnection != null) {
            roomId = mSkylinkConnection.getRoomId();
        }

        return roomName + " (" + roomId + ")";
    }

    public String getPeerIdServiceHandler() {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getPeerId();
        }

        return null;
    }

    /**
     * Get peerId of a Peer using SkylinkConnection API.
     *
     * @param index 0 for self Peer, 1 onwards for remote Peer(s).
     * @return Desired peerId or null if not available.
     */
    public String getPeerIdServiceHandler(int index) {

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

    @Override
    public void onDataReceive(String remotePeerId, byte[] data) {

    }

    @Override
    public void onFileTransferPermissionRequest(String remotePeerId, String fileName, boolean isPrivate) {

    }

    @Override
    public void onFileTransferPermissionResponse(String remotePeerId, String fileName, boolean isPermitted) {

    }

    @Override
    public void onFileTransferDrop(String remotePeerId, String fileName, String message, boolean isExplicit) {

    }

    @Override
    public void onFileSendComplete(String remotePeerId, String fileName) {

    }

    @Override
    public void onFileReceiveComplete(String remotePeerId, String fileName) {

    }

    @Override
    public void onFileSendProgress(String remotePeerId, String fileName, double percentage) {

    }

    @Override
    public void onFileReceiveProgress(String remotePeerId, String fileName, double percentage) {

    }

    @Override
    public void onServerMessageReceive(String remotePeerId, Object message, boolean isPrivate) {

    }

    @Override
    public void onP2PMessageReceive(String remotePeerId, Object message, boolean isPrivate) {

    }

    @Override
    public void onRecordingStart(String recordingId) {

    }

    @Override
    public void onRecordingStop(String recordingId) {

    }

    @Override
    public void onRecordingVideoLink(String recordingId, String peerId, String videoLink) {

    }

    @Override
    public void onRecordingError(String recordingId, int errorCode, String description) {

    }

    @Override
    public void onTransferSpeedReceived(String peerId, int mediaDirection, int mediaType, double transferSpeed) {

    }

    @Override
    public void onWebrtcStatsReceived(String peerId, int mediaDirection, int mediaType, HashMap<String, String> stats) {

    }
}
