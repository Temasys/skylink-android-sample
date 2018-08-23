package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.Date;
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
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioCallContract;
import sg.com.temasys.skylink.sdk.sampleapp.chat.ChatContract;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.datatransfer.DataTransferContract;
import sg.com.temasys.skylink.sdk.sampleapp.filetransfer.FileTransferContract;
import sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo.MultiPartyVideoCallContract;
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

    //type call to define current call function
    protected Constants.CONFIG_TYPE mTypeCall;

    protected Context mContext;

    private SdkConnectionManager mSdkConnectionManager;

    //presenter for each type call
    protected AudioCallContract.Presenter mAudioPresenter;

    protected VideoCallContract.Presenter mVideoPresenter;

    protected ChatContract.Presenter mChatPresenter;

    protected DataTransferContract.Presenter mDataPresenter;

    protected FileTransferContract.Presenter mFilePresenter;

    protected MultiPartyVideoCallContract.Presenter mMultiVideoPresenter;

    //these variables need to be static for configuration change
    protected static SkylinkConnection mSkylinkConnection;

    //list of peers joined in room
    protected static List<SkylinkPeer> mPeersList;

    private String mRoomName;
    private String mUserName;

    public SDKService(Context context) {
        this.mContext = context;
    }

    private void setListeners() {
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
        }
    }

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        String log = "[onConnect] ";
        if (isSuccessful) {
            String localPeerId = mSkylinkConnection.getPeerId();

            //for Grab client
            log += "Connected to room " + mRoomName +" as (" + mUserName + ").";
            toastLogLong(TAG, mContext, log);

            //init peers list and add self/local peer to list
            if (mPeersList == null) {
                mPeersList = new ArrayList<SkylinkPeer>();
            }

            //add self peer as a peer in list
            mPeersList.add(new SkylinkPeer(localPeerId, mUserName));

        } else {
            log += "Skylink failed to connect!\nReason : " + message;
            toastLogLong(TAG, mContext, log);
        }

        //update UI to connected state for all types call
        switch (mTypeCall) {
            case AUDIO:
                if (mAudioPresenter != null) {
                    mAudioPresenter.onConnect(isSuccessful);
                }
                break;
            case VIDEO:
                if (mVideoPresenter != null) {
                    mVideoPresenter.onConnect(isSuccessful);
                }
                break;
            case CHAT:
                if (mChatPresenter != null) {
                    mChatPresenter.onConnect(isSuccessful);
                }
                break;
            case DATA:
                if (mDataPresenter != null) {
                    mDataPresenter.onConnect(isSuccessful);
                }
                break;
            case FILE:
                if (mFilePresenter != null) {
                    mFilePresenter.onConnect(isSuccessful);
                }
                break;
            case MULTI_PARTY_VIDEO:
                if (mMultiVideoPresenter != null) {
                    mMultiVideoPresenter.onConnect(isSuccessful);
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

        //remove all peers in room
        if (mPeersList != null)
            mPeersList.clear();

        //update UI to disconnected state for all types call
        switch (mTypeCall) {
            case AUDIO:
                if (mAudioPresenter != null) {
                    mAudioPresenter.onDisconnect();
                }
                break;
            case VIDEO:
                if (mVideoPresenter != null) {
                    mVideoPresenter.onDisconnect();
                }
                break;
            case CHAT:
                if (mChatPresenter != null) {
                    mChatPresenter.onDisconnect();
                }
                break;
            case DATA:
                if (mDataPresenter != null) {
                    mDataPresenter.onDisconnect();
                }
                break;
            case FILE:
                if (mFilePresenter != null) {
                    mFilePresenter.onDisconnect();
                }
                break;
            case MULTI_PARTY_VIDEO:
                if (mMultiVideoPresenter != null) {
                    mMultiVideoPresenter.onDisconnect();
                }
                break;
        }
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

        //only video call and multi party video call need to implement this callback
        if (mVideoPresenter != null)
            mVideoPresenter.onLocalMediaCapture(videoView);
        else if (mMultiVideoPresenter != null)
            mMultiVideoPresenter.onLocalMediaCapture(videoView);
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

        //only audio call and video call and multi party video call need to implement this callback
        if (mAudioPresenter != null) {
            mAudioPresenter.onRemotePeerMediaReceive(log, remotePeerUserInfo);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onRemotePeerMediaReceive(log, remotePeerUserInfo);
        } else if (mMultiVideoPresenter != null) {
            mMultiVideoPresenter.onRemotePeerMediaReceive(log, remotePeerUserInfo, remotePeerId);
        }
    }

    @Override
    public void onInputVideoResolutionObtained(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        Log.d(TAG, "[onInputVideoResolutionObtained]");
        //only video call and multi party video call need to implement this callback
        if (mVideoPresenter != null)
            mVideoPresenter.onInputVideoResolutionObtained(width, height, fps, captureFormat);
        else if (mMultiVideoPresenter != null)
            mMultiVideoPresenter.onInputVideoResolutionObtained(width, height, fps, captureFormat);
    }

    @Override
    public void onReceivedVideoResolutionObtained(String peerId, int width, int height, int fps) {
        Log.d(TAG, "[onReceivedVideoResolutionObtained]");
        //only video call and multi party video call need to implement this callback
        if (mVideoPresenter != null)
            mVideoPresenter.onReceivedVideoResolutionObtained(peerId, width, height, fps);
        else if (mMultiVideoPresenter != null)
            mMultiVideoPresenter.onReceivedVideoResolutionObtained(peerId, width, height, fps);
    }

    @Override
    public void onSentVideoResolutionObtained(String peerId, int width, int height, int fps) {
        Log.d(TAG, "[onSentVideoResolutionObtained]");
        //only video call and multi party video call need to implement this callback
        if (mVideoPresenter != null)
            mVideoPresenter.onSentVideoResolutionObtained(peerId, width, height, fps);
        else if (mMultiVideoPresenter != null)
            mMultiVideoPresenter.onSentVideoResolutionObtained(peerId, width, height, fps);
    }

    @Override
    public void onVideoSizeChange(String peerId, Point size) {
        Log.d(TAG, "[onVideoSizeChange]");
        //only video call and multi party video call need to implement this callback
        if (mVideoPresenter != null)
            mVideoPresenter.onVideoSizeChange(peerId, size);
        else if (mMultiVideoPresenter != null)
            mMultiVideoPresenter.onVideoSizeChange(peerId, size);
    }

    @Override
    public void onPermissionRequired(String[] permissions, int requestCode, int infoCode) {
        Log.d(TAG, "[onPermissionRequired]");
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        //Data Transfer and Chat function do not need to implement permission
        if (mAudioPresenter != null) {
            mAudioPresenter.onPermissionRequired(info);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onPermissionRequired(info);
        } else if (mFilePresenter != null) {
            mFilePresenter.onPermissionRequired(info);
        } else if (mMultiVideoPresenter != null) {
            mMultiVideoPresenter.onPermissionRequired(info);
        }
    }

    @Override
    public void onPermissionGranted(String[] permissions, int requestCode, int infoCode) {
        Log.d(TAG, "[onPermissionGranted]");
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        if (mAudioPresenter != null) {
            mAudioPresenter.onPermissionGranted(info);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onPermissionGranted(info);
        } else if (mFilePresenter != null) {
            mFilePresenter.onPermissionGranted(info);
        } else if (mMultiVideoPresenter != null) {
            mMultiVideoPresenter.onPermissionGranted(info);
        }
    }

    @Override
    public void onPermissionDenied(String[] permissions, int requestCode, int infoCode) {
        Log.d(TAG, "[onPermissionDenied]");
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        if (mAudioPresenter != null) {
            mAudioPresenter.onPermissionDenied(info);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onPermissionDenied(info);
        } else if (mFilePresenter != null) {
            mFilePresenter.onPermissionDenied(info);
        } else if (mMultiVideoPresenter != null) {
            mMultiVideoPresenter.onPermissionDenied(info);
        }
    }

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        String logTag = "[SA][onRemotePeerJoin] ";

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

        //update UI
        if (mAudioPresenter != null) {
            mAudioPresenter.onRemotePeerJoin(remotePeer);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onRemotePeerJoin(remotePeer);
        } else if (mChatPresenter != null) {
            mChatPresenter.onRemotePeerJoin(remotePeer);
        } else if (mDataPresenter != null) {
            mDataPresenter.onRemotePeerJoin(remotePeer);
        } else if (mFilePresenter != null) {
            mFilePresenter.onRemotePeerJoin(remotePeer);
        } else if (mMultiVideoPresenter != null) {
            mMultiVideoPresenter.onRemotePeerJoin(remotePeer);
        }

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
        if (mAudioPresenter != null) {
            mAudioPresenter.onRemotePeerConnectionRefreshed(log, remotePeerUserInfo);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onRemotePeerConnectionRefreshed(log, remotePeerUserInfo);
        } else if (mMultiVideoPresenter != null) {
            mMultiVideoPresenter.onRemotePeerConnectionRefreshed(log, remotePeerUserInfo);
        }
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

        //remove remote peer and keep the index of remote peer for multi party video call function
        int removeIndex = -1;
        for (int i = 0; i < mPeersList.size(); i++) {
            if (mPeersList.get(i).getPeerId().equals(remotePeerId)) {
                mPeersList.remove(i);
                removeIndex = i;
                break;
            }
        }

        if (mAudioPresenter != null) {
            mAudioPresenter.onRemotePeerLeave(remotePeerId);
        } else if (mVideoPresenter != null) {
            mVideoPresenter.onRemotePeerLeave(remotePeerId);
        } else if (mChatPresenter != null) {
            mChatPresenter.onRemotePeerLeave(remotePeerId);
        } else if (mDataPresenter != null) {
            mDataPresenter.onRemotePeerLeave(remotePeerId);
        } else if (mFilePresenter != null) {
            mFilePresenter.onRemotePeerLeave(remotePeerId);
        } else if (mMultiVideoPresenter != null) {
            if (removeIndex > -1) {
                mMultiVideoPresenter.onSetRemovedPeerIndex(removeIndex);
                mMultiVideoPresenter.onRemotePeerLeave(remotePeerId);
            }
        }

        int numRemotePeers = mPeersList.size() - 1;
        if (numRemotePeers >= 0) {
            String log = "Your Peer " + getPeerIdNick(remotePeerId, userInfo) + " left: " +
                    message + ". " + numRemotePeers + " remote Peer(s) left in the room.";
            toastLog(TAG, mContext, log);
        }
    }

    @Override
    public void onDataReceive(String remotePeerId, byte[] data) {
        Log.d(TAG, "[onDataReceive]");
        if (mDataPresenter != null) {
            mDataPresenter.onDataReceive(remotePeerId, data);
        }
    }

    @Override
    public void onFileTransferPermissionRequest(String remotePeerId, String fileName, boolean isPrivate) {
        Log.d(TAG, "[onFileTransferPermissionRequest]");
        if (mFilePresenter != null) {
            mFilePresenter.onFileTransferPermissionRequest(remotePeerId, fileName, isPrivate);
        }
    }

    @Override
    public void onFileTransferPermissionResponse(String remotePeerId, String fileName, boolean isPermitted) {
        Log.d(TAG, "[onFileTransferPermissionResponse]");
        if (mFilePresenter != null) {
            mFilePresenter.onFileTransferPermissionResponse(remotePeerId, fileName, isPermitted);
        }
    }

    @Override
    public void onFileTransferDrop(String remotePeerId, String fileName, String message, boolean isExplicit) {
        Log.d(TAG, "[onFileTransferDrop]");
        if (mFilePresenter != null) {
            mFilePresenter.onFileTransferDrop(remotePeerId, fileName, message, isExplicit);
        }
    }

    @Override
    public void onFileSendComplete(String remotePeerId, String fileName) {
        Log.d(TAG, "[onFileSendComplete]");
        if (mFilePresenter != null) {
            mFilePresenter.onFileSendComplete(remotePeerId, fileName);
        }
    }

    @Override
    public void onFileReceiveComplete(String remotePeerId, String fileName) {
        Log.d(TAG, "[onFileReceiveComplete]");
        if (mFilePresenter != null) {
            mFilePresenter.onFileReceiveComplete(remotePeerId, fileName);
        }
    }

    @Override
    public void onFileSendProgress(String remotePeerId, String fileName, double percentage) {
        Log.d(TAG, "[onFileSendProgress]");
        if (mFilePresenter != null) {
            mFilePresenter.onFileSendProgress(remotePeerId, fileName, percentage);
        }
    }

    @Override
    public void onFileReceiveProgress(String remotePeerId, String fileName, double percentage) {
        Log.d(TAG, "[onFileReceiveProgress]");
        if (mFilePresenter != null) {
            mFilePresenter.onFileReceiveProgress(remotePeerId, fileName, percentage);
        }
    }

    @Override
    public void onServerMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
        Log.d(TAG, "[onServerMessageReceive]");
        if (mChatPresenter != null) {
            mChatPresenter.onServerMessageReceive(remotePeerId, message, isPrivate);
        }
    }

    @Override
    public void onP2PMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
        Log.d(TAG, "[onP2PMessageReceive]");
        if (mChatPresenter != null) {
            mChatPresenter.onP2PMessageReceive(remotePeerId, message, isPrivate);
        }
    }

    @Override
    public void onRecordingStart(String recordingId) {
        Log.d(TAG, "[onRecordingStart]");
        if (mMultiVideoPresenter != null && mSkylinkConnection != null) {
            mMultiVideoPresenter.onRecordingStart(mSkylinkConnection.isRecording());
        }
    }

    @Override
    public void onRecordingStop(String recordingId) {
        Log.d(TAG, "[onRecordingStop]");
        if (mMultiVideoPresenter != null && mSkylinkConnection != null) {
            mMultiVideoPresenter.onRecordingStop(mSkylinkConnection.isRecording());
        }
    }

    @Override
    public void onRecordingVideoLink(String recordingId, String peerId, String videoLink) {
        Log.d(TAG, "[onRecordingVideoLink]");
        if (mMultiVideoPresenter != null) {
            mMultiVideoPresenter.onRecordingVideoLink(recordingId, peerId, videoLink);
        }
    }

    @Override
    public void onRecordingError(String recordingId, int errorCode, String description) {
        Log.d(TAG, "[onRecordingError]");
        if (mMultiVideoPresenter != null) {
            mMultiVideoPresenter.onRecordingError(recordingId, errorCode, description);
        }
    }

    @Override
    public void onTransferSpeedReceived(String peerId, int mediaDirection, int mediaType, double transferSpeed) {
        Log.d(TAG, "[onTransferSpeedReceived]");
        if (mMultiVideoPresenter != null) {
            mMultiVideoPresenter.onTransferSpeedReceived(peerId, mediaDirection, mediaType, transferSpeed);
        }
    }

    @Override
    public void onWebrtcStatsReceived(String peerId, int mediaDirection, int mediaType, HashMap<String, String> stats) {
        Log.d(TAG, "[onWebrtcStatsReceived]");
        if (mMultiVideoPresenter != null) {
            mMultiVideoPresenter.onWebrtcStatsReceived(peerId, mediaDirection, mediaType, stats);
        }
    }

    public boolean connectToRoom() {

        //check internet connection first
        if(!Utils.isInternetOn()){
            String log = "Internet connection is off !";
            toastLog(TAG, mContext, log);
            return false;
        }

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
            return false;
        } else {
            String log = "Connecting...";
            toastLog(TAG, mContext, log);
        }

        // Initialize and use the Audio router to switch between headphone and headset
        if (mTypeCall == Constants.CONFIG_TYPE.AUDIO || mTypeCall == Constants.CONFIG_TYPE.VIDEO || mTypeCall == Constants.CONFIG_TYPE.MULTI_PARTY_VIDEO)
            AudioRouter.startAudioRouting(mContext);

        return true;
    }

    public void disconnectFromRoom() {

        if (mSkylinkConnection != null)
            mSkylinkConnection.disconnectFromRoom();

        if (mTypeCall == Constants.CONFIG_TYPE.AUDIO || mTypeCall == Constants.CONFIG_TYPE.VIDEO || mTypeCall == Constants.CONFIG_TYPE.MULTI_PARTY_VIDEO)
            AudioRouter.stopAudioRouting(mContext);
    }

    public boolean isConnectingOrConnected() {
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

        //for Grab client
        String peerUserName = getUserDataString(peerId);

        return peerUserName;

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
        String roomId = "";
        if (mSkylinkConnection != null) {
            roomId = mSkylinkConnection.getRoomId();
        }
        //for Grab client
        return defaultName;
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

    public String getRoomPeerIdNick() {
        if (mSkylinkConnection == null) {
            return null;
        }

        String roomName = Utils.getRoomNameByType(mTypeCall);

        String title = "Room: " + getRoomRoomId(roomName);
        // Add PeerId to title if a Peer occupies clicked location.
        title += "\r\n" + getPeerIdNick(getPeerId());
        return title;

    }

    public String getRoomRoomId(String roomName) {
        String roomId = "";
        if (mSkylinkConnection != null) {
            roomId = mSkylinkConnection.getRoomId();
        }

        return roomName;
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
}
