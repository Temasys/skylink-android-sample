package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

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
 * This class is responsible for implementing all SkylinkListeners for common use of all demos/functions
 * And directly works with SkylinkSDK.
 * In case user does not want to implement a specific demo/function, no need to implement correspond listener(s).
 */

public abstract class SkylinkCommonService implements LifeCycleListener, MediaListener, OsListener, RemotePeerListener, MessagesListener,
        DataTransferListener, FileTransferListener, RecordingListener, StatsListener {

    private final String TAG = SkylinkCommonService.class.getName();

    protected Context mContext;

    //common presenter for all presenters
    protected BasePresenter presenter;

    //Skylink connection manager object
    private SkylinkConnectionManager mSkylinkConnectionManager;

    // SkylinkConnection instance for working with SkylinkSDK
    // this variable need to be static for configuration change
    protected static SkylinkConnection mSkylinkConnection;

    //list of peers joined in room
    //this variable need to be static for configuration change
    protected static List<SkylinkPeer> mPeersList;

    //room name and user name for each demo/function
    protected String mRoomName;
    protected String mUserName;

    public SkylinkCommonService(Context context) {
        this.mContext = context;
        this.mSkylinkConnectionManager = new SkylinkConnectionManager(this, context);
    }

    protected static void setmSkylinkConnection(SkylinkConnection mSkylinkConnection) {
        SkylinkCommonService.mSkylinkConnection = mSkylinkConnection;
    }

    //abstract methods need to be implemented in each demo/function like AudioService, VideoService,...
    public abstract void setSkylinkListeners();

    public abstract SkylinkConfig getSkylinkConfig();

    //----------------------------------------------------------------------------------------------
    // Override methods from SkylinkListeners
    // These methods are responsible to inform user the results from SkylinkSDK
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Methods which are from LifeCycleListener need to be implemented for all functions
    //----------------------------------------------------------------------------------------------

    /**
     * This is the first callback from SkylinkSDK to specify whether the attempt to connect to the room was successful.
     */
    @Override
    public void onConnect(boolean isSuccessful, String message) {
        String log = "[onConnect] ";
        if (isSuccessful) {
            String localPeerId = mSkylinkConnection.getPeerId();

            log += "Connected to room " + getRoomIdAndNickname(mRoomName) + " as " + mUserName + " (" + localPeerId + ")";
            toastLog(TAG, mContext, log);

            //init/reset peers list and add self/local peer to list
            if (mPeersList == null) {
                mPeersList = new ArrayList<SkylinkPeer>();
            } else {
                mPeersList.clear();
            }

            //add self peer as a peer in list
            mPeersList.add(new SkylinkPeer(localPeerId, mUserName));

        } else {
            log += "Skylink failed to connect!\nReason : " + message;
            toastLog(TAG, mContext, log);
        }

        //update UI to connected state
        presenter.onServiceRequestConnect(isSuccessful);
    }

    /**
     * This method is triggered from SkylinkSDK to inform that user is disconnected from room successfully or not
     */
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

        //update UI to disconnected state for all types call
        presenter.onServiceRequestDisconnect();

        //remove all peers in room
        if (mPeersList != null)
            mPeersList.clear();
    }

    /**
     * This is triggered from SkylinkSDK when the lock status of the room that we are in changes
     */
    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        String log = "[onLockRoomStatusChange] ";
        log += "[SA] Peer " + remotePeerId + " changed Room locked status to "
                + lockStatus + ".";
        toastLog(TAG, mContext, log);
    }

    /**
     * This method is triggered from SkylinkSDK to inform that changes in room lock
     */
    @Override
    public void onReceiveLog(int infoCode, String message) {
        Utils.handleSkylinkReceiveLog(infoCode, message, mContext, TAG);
    }

    /**
     * This is triggered from SkylinkSDK to deliver messages that might be useful to the user.
     */
    @Override
    public void onWarning(int errorCode, String message) {
        Utils.handleSkylinkWarning(errorCode, message, mContext, TAG);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from MediaListener need to be implemented for audio and video functions
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered from SkylinkSDK when successfully captures the local device's camera input.
     * This may be at the start of video usage or when video source has changed.
     * It can happen only if the connection is configured to have a video call.
     *
     * @param videoView Local video
     */
    @Override
    public void onLocalMediaCapture(SurfaceViewRenderer videoView) {
        Log.d(TAG, "[onLocalMediaCapture]");

        presenter.onServiceRequestLocalMediaCapture(videoView);
    }

    /**
     * This is triggered from SkylinkSDK when a remote peer enable / disable its audio.
     *
     * @param remotePeerId The id of the remote peer
     * @param isMuted      Flag specifying whether the audio is muted or not
     */
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

    /**
     * This is triggered from SkylinkSDK when a peer enable / disable its video.
     *
     * @param remotePeerId The id of the remote peer
     * @param isMuted      Flag specifying whether the video is muted or not
     */
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

    /**
     * This is triggered from SkylinkSDK upon receiving the media stream of the remote peer
     * if the connection is configured to have a audio and/or video call.
     * This will only be triggered after onRemotePeerJoin has triggered.
     *
     * @param remotePeerId The id of the peer
     * @param videoView    Remote peer's video. Will be null if Peer is sending only audio.
     */
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

        presenter.onServiceRequestRemotePeerMediaReceive(log, remotePeerUserInfo, remotePeerId);
    }

    /**
     * This is triggered from SkylinkSDK when a local camera starts to capture video,
     * or when it's video dimension changes,
     * if {@link SkylinkConfig#isReportVideoResolutionOnChange()} is true.
     * It can also be triggered by calling {@link SkylinkConnection#getInputVideoResolution()}.
     * The current resolution of the video being captured by the local camera
     * and the SkylinkCaptureFormat used will be provided.
     * Note: This might be different from the resolution of the video actually sent to Peers as
     * WebRTC may adjust resolution dynamically to try to match its bandwidth criteria.
     * If a particular value is not available, , e.g. if video is not capturing,
     * a negative integer or a null value will be provided.
     *
     * @param width         Video width in pixels of local captured video.
     * @param height        Video height in pixels of local captured video.
     * @param fps           Video frame per second of local captured video.
     * @param captureFormat The SkylinkCaptureFormat that is currently used by the camera.
     */
    @Override
    public void onInputVideoResolutionObtained(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        Log.d(TAG, "[onInputVideoResolutionObtained]");

        presenter.onServiceRequestInputVideoResolutionObtained(width, height, fps, captureFormat);
    }

    /**
     * This is triggered from SkylinkSDK when we receive a remote Peer's video,
     * or when it's video dimension changes,
     * if {@link SkylinkConfig#isReportVideoResolutionOnChange()} is true.
     * It can also be triggered by calling {@link SkylinkConnection#getReceivedVideoResolution}.
     * The current resolution of the video received from a specific Peer will be provided.
     * If a particular value is not available, a negative integer will be provided.
     *
     * @param peerId PeerId of the remote Peer whose Video Resolution we are obtaining.
     * @param width  Video width in pixels of remote Peer's video received by us.
     * @param height Video height in pixels of remote Peer's video received by us.
     * @param fps    Video frame per second of remote Peer's video received by us.
     */
    @Override
    public void onReceivedVideoResolutionObtained(String peerId, int width, int height, int fps) {
        Log.d(TAG, "[onReceivedVideoResolutionObtained]");

        presenter.onServiceRequestReceivedVideoResolutionObtained(peerId, width, height, fps);
    }

    /**
     * This is triggered from SkylinkSDK when a local video stream starts being sent to a remote Peer,
     * or when it's video dimension changes,
     * if {@link SkylinkConfig#isReportVideoResolutionOnChange()} is true.
     * It can also be triggered by calling {@link SkylinkConnection#getSentVideoResolution}.
     * The current resolution of the video being sent to a specific Peer will be provided.
     *
     * @param peerId PeerId of the remote Peer whom we are obtaining Video Resolution
     *               for the video we are sending.
     * @param width  Video width in pixels of local video sent to remote Peer.
     * @param height Video height in pixels of local video sent to remote Peer.
     * @param fps    Video frame per second of local video sent to remote Peer.
     */
    @Override
    public void onSentVideoResolutionObtained(String peerId, int width, int height, int fps) {
        Log.d(TAG, "[onSentVideoResolutionObtained]");

        presenter.onServiceRequestSentVideoResolutionObtained(peerId, width, height, fps);
    }

    /**
     * This is triggered from SkylinkSDK when any of the given video streams' frame size changes. It includes the
     * self stream also.
     *
     * @param peerId The id of the peer. If null, it indicates self stream.
     * @param size   Size of the video frame
     */
    @Override
    public void onVideoSizeChange(String peerId, Point size) {
        Log.d(TAG, "[onVideoSizeChange]");

        presenter.onServiceRequestVideoSizeChange(peerId, size);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from OsListener need to be implemented for audio, video, fileTransfer, multiVideo functions
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered from SkylinkSDK when Android Runtime permission is required to use
     * devices to perform media functions like audio, camera, file transfer,...
     * The SDK will process this result and also notify app via
     * {@link #onPermissionGranted} or {@link #onPermissionDenied}.
     *
     * @param permissions A Strings array containing the Android Runtime permission required
     *                    at the first array index (index 0).
     * @param requestCode An unique integer that represents this permission request.
     *                    This is to be used as the requestCode when calling
     *                    Android's requestPermissions.
     * @param infoCode    {@link sg.com.temasys.skylink.sdk.rtc.Info} Integer that explains why this permission is required.
     *                    Further info can be obtained by calling
     *                    {@link sg.com.temasys.skylink.sdk.rtc.Info#getInfoString}.
     *                    Based on this info, an explanation might be crafted
     *                    to educate the user on why this permission is required.
     */
    @Override
    public void onPermissionRequired(String[] permissions, int requestCode, int infoCode) {
        Log.d(TAG, "[onPermissionRequired]");
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        presenter.onServiceRequestPermissionRequired(info);
    }

    /**
     * This is triggered when Android Runtime permission has been granted for the usage of
     * a particular part of the SDK.
     *
     * @param permissions A Strings array containing the Android Runtime permission required
     *                    at the first array index (index 0).
     * @param requestCode An unique integer that represents this permission request.
     *                    This was used as the requestCode when calling
     *                    Android's requestPermissions.
     * @param infoCode    {@link sg.com.temasys.skylink.sdk.rtc.Info} Integer that explains why this permission is required.
     *                    Further info can be obtained by calling
     *                    {@link sg.com.temasys.skylink.sdk.rtc.Info#getInfoString}.
     */
    @Override
    public void onPermissionGranted(String[] permissions, int requestCode, int infoCode) {
        Log.d(TAG, "[onPermissionGranted]");
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        presenter.onServiceRequestPermissionGranted(info);
    }

    /**
     * This is triggered when Android Runtime permission has been denied for the usage of
     * a particular part of the SDK.
     *
     * @param permissions A Strings array containing the Android Runtime permission required
     *                    at the first array index (index 0).
     * @param requestCode An unique integer that represents this permission request.
     *                    This was used as the requestCode when calling
     *                    Android's requestPermissions.
     * @param infoCode    {@link sg.com.temasys.skylink.sdk.rtc.Info} Integer that explains why this permission is required.
     *                    Further info can be obtained by calling
     *                    {@link sg.com.temasys.skylink.sdk.rtc.Info#getInfoString}.
     *                    Based on this info, a warning might be crafted for the user as to what
     *                    issues might occur, now that this permission had been denied.
     */
    @Override
    public void onPermissionDenied(String[] permissions, int requestCode, int infoCode) {
        Log.d(TAG, "[onPermissionDenied]");
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        presenter.onServiceRequestPermissionDenied(mContext, info);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from RemotePeerListener need to be implemented for all functions
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered when a remote peer joins the room.
     * It will be triggered before onRemotePeerMediaReceive is triggered
     *
     * @param remotePeerId   The id of the peer
     * @param userData       User defined data relating to the remote peer. May be a
     *                       'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param hasDataChannel Whether peer has enabled DataChannel.
     */
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

        presenter.onServiceRequestRemotePeerJoin(remotePeer);

        logTag += "Your Peer " + getPeerIdNick(remotePeerId) + " connected.";
        toastLog(TAG, mContext, logTag);


    }

    /**
     * This is triggered when the connection with a remote peer is refreshed.
     *
     * @param remotePeerId    The id of the peer.
     * @param userData        User defined data relating to the remote peer. May be a
     *                        'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param hasDataChannel  Whether peer has enabled DataChannel.
     * @param wasIceRestarted Indicates if ICE restart was done when PeerConnection was refreshed.
     */
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

        presenter.onServiceRequestRemotePeerConnectionRefreshed(log, remotePeerUserInfo);
    }

    /**
     * This is triggered when an update is received in the user defined data of a remote peer.
     *
     * @param remotePeerId The id of the peer.
     * @param userData     User defined data relating to the remote peer. May be a
     *                     'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     */
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

    /**
     * This is triggered when the underlying peer to peer data connection is established with a
     * remote peer and is ready to send and receive peer messages and data (including files).
     *
     * @param remotePeerId The id of the remote peer
     */
    @Override
    public void onOpenDataConnection(String remotePeerId) {
        Log.d(TAG, "onOpenDataConnection");
    }

    /**
     * This is triggered when a remote peer leaves the room.
     *
     * @param remotePeerId The id of the remote peer
     * @param message      Message specifying the possible reason for leaving the room.
     * @param userInfo     UserInfo of this remote Peer.
     */
    @Override
    public void onRemotePeerLeave(String remotePeerId, String message, UserInfo userInfo) {
        Log.d(TAG, "[onRemotePeerLeave]");

        //remove remote peer and keep the index of remote peer for multi party video call function
        int removeIndex = -1;
        SkylinkPeer removedPeer = null;
        for (int i = 0; i < mPeersList.size(); i++) {
            if (mPeersList.get(i).getPeerId().equals(remotePeerId)) {
                removedPeer = mPeersList.get(i);
                mPeersList.remove(i);
                removeIndex = i;
                break;
            }
        }

        //update UI when remote peer lelf the room
        presenter.onServiceRequestRemotePeerLeave(removedPeer, removeIndex);

        int numRemotePeers = mPeersList.size() - 1;
        if (numRemotePeers >= 0) {
            String log = "Your Peer " + getUserDataString(userInfo) + " ( " + remotePeerId + " ) left: " +
                    message + ". " + numRemotePeers + " remote Peer(s) left in the room.";
            toastLog(TAG, mContext, log);
        }
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from DataTransferListener need to be implemented for dataTransfer function
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered when data is received
     *
     * @param remotePeerId The id of the peer
     * @param data         Array of bytes
     */
    @Override
    public void onDataReceive(String remotePeerId, byte[] data) {
        Log.d(TAG, "[onDataReceive]");

        presenter.onServiceRequestDataReceive(mContext, remotePeerId, data);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from FileTransferListener need to be implemented for fileTransfer function
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered upon receiving a file transfer request from a peer.
     *
     * @param remotePeerId The id of the peer
     * @param fileName     The name of the file
     * @param isPrivate    Flag to specify if file share was directed to only us (as opposed to all
     *                     peers in the room).
     */
    @Override
    public void onFileTransferPermissionRequest(String remotePeerId, String fileName, boolean isPrivate) {
        Log.d(TAG, "[onFileTransferPermissionRequest]");

        presenter.onServiceRequestFileTransferPermissionRequest(remotePeerId, fileName, isPrivate);
    }

    /**
     * This is triggered upon receiving the response of a peer to our request for file transfer with
     * the peer.
     *
     * @param remotePeerId The id of the peer
     * @param fileName     The name of the file
     * @param isPermitted  Flag to specify whether the peer has accepted the request
     */
    @Override
    public void onFileTransferPermissionResponse(String remotePeerId, String fileName, boolean isPermitted) {
        Log.d(TAG, "[onFileTransferPermissionResponse]");

        presenter.onServiceRequestFileTransferPermissionResponse(mContext, remotePeerId, fileName, isPermitted);
    }

    /**
     * This is triggered when an ongoing file transfer drops due to some reason.
     *
     * @param remotePeerId The id of the peer. Null if broadcast file transfer was attempted.
     * @param fileName     The name of the file
     * @param message      Message that possibly tells the reason for dropping
     * @param isExplicit   True if user canceled the transfer explicitly, as opposed to being due to
     *                     error in the transfer process.
     */
    @Override
    public void onFileTransferDrop(String remotePeerId, String fileName, String message, boolean isExplicit) {
        Log.d(TAG, "[onFileTransferDrop]");

        presenter.onServiceRequestFileTransferDrop(mContext, remotePeerId, fileName, message, isExplicit);
    }

    /**
     * This is triggered when we have sent a file successfully.
     *
     * @param remotePeerId The id of the peer receiving the file.
     * @param fileName     The name of the file
     */
    @Override
    public void onFileSendComplete(String remotePeerId, String fileName) {
        Log.d(TAG, "[onFileSendComplete]");

        presenter.onServiceRequestFileSendComplete(mContext, remotePeerId, fileName);
    }

    /**
     * This is triggered when we have received a file successfully.
     *
     * @param remotePeerId The id of the peer
     * @param fileName     The name of the file
     */
    @Override
    public void onFileReceiveComplete(String remotePeerId, String fileName) {
        Log.d(TAG, "[onFileReceiveComplete]");

        presenter.onServiceRequestFileReceiveComplete(remotePeerId, fileName);
    }

    /**
     * This is triggered timely to report the on going progress when sending a file
     *
     * @param remotePeerId The id of the peer receiving the file.
     * @param fileName     The name of the file
     * @param percentage   The percentage completed
     */
    @Override
    public void onFileSendProgress(String remotePeerId, String fileName, double percentage) {
        Log.d(TAG, "[onFileSendProgress]");

        presenter.onServiceRequestFileSendProgress(mContext, remotePeerId, fileName, percentage);
    }

    /**
     * This is triggered timely to report the on going progress when receiving a file
     *
     * @param remotePeerId The id of the peer sending the file.
     * @param fileName     The name of the file
     * @param percentage   The percentage completed
     */
    @Override
    public void onFileReceiveProgress(String remotePeerId, String fileName, double percentage) {
        Log.d(TAG, "[onFileReceiveProgress]");

        presenter.onServiceRequestFileReceiveProgress(mContext, remotePeerId, fileName, percentage);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from MessagesListener need to be implemented for chat/messaging function
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered when a broadcast or private message is received from a remote peer via a
     * server.
     *
     * @param remotePeerId The id of the remote peer
     * @param message      User defined message. May be a 'java.lang.String', 'org.json.JSONObject'
     *                     or 'org.json.JSONArray'.
     * @param isPrivate    Flag to specify whether the message was sent to us only (as opposed to
     *                     broadcast to all peers in the room).
     */
    @Override
    public void onServerMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
        Log.d(TAG, "[onServerMessageReceive]");

        presenter.onServiceRequestServerMessageReceive(remotePeerId, message, isPrivate);
    }

    /**
     * This is triggered when a broadcast or private message is received directly from the peer,
     * i.e. in a peer to peer manner.
     *
     * @param remotePeerId The id of the remote peer
     * @param message      User defined message. May be a 'java.lang.String', 'org.json.JSONObject'
     *                     or 'org.json.JSONArray'.
     * @param isPrivate    Flag to specify whether the message was sent to us only (as opposed to
     *                     broadcast to all peers in the room).
     */
    @Override
    public void onP2PMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
        Log.d(TAG, "[onP2PMessageReceive]");

        presenter.onServiceRequestP2PMessageReceive(remotePeerId, message, isPrivate);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from RecordingListener need to be implemented for recording (in Multi Video function)
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered to indicate that recording has started for this room.
     *
     * @param recordingId Id of Recording session this message is about.
     */
    @Override
    public void onRecordingStart(String recordingId) {
        Log.d(TAG, "[onRecordingStart]");

        presenter.onServiceRequestRecordingStart(mContext, mSkylinkConnection.isRecording());
    }

    /**
     * This is triggered to indicate that recording has stopped for this room.
     *
     * @param recordingId Id of Recording session this message is about.
     */
    @Override
    public void onRecordingStop(String recordingId) {
        Log.d(TAG, "[onRecordingStop]");

        presenter.onServiceRequestRecordingStop(mContext, mSkylinkConnection.isRecording());
    }

    /**
     * This is triggered when a recording video link is obtained
     *
     * @param recordingId Id of Recording session this message is about.
     * @param peerId      Id of the Peer whose video recording is obtained here.
     *                    If peerId is null, the recorded video is that of the room mixing video.
     * @param videoLink   URL where the recorded video of this Peer can be obtained.
     */
    @Override
    public void onRecordingVideoLink(String recordingId, String peerId, String videoLink) {
        Log.d(TAG, "[onRecordingVideoLink]");

        presenter.onServiceRequestRecordingVideoLink(recordingId, peerId, videoLink);
    }

    /**
     * This is triggered when a recording error occurs.
     *
     * @param recordingId Id of Recording session this message is about.
     * @param errorCode   {@link sg.com.temasys.skylink.sdk.rtc.Errors Errors} Integer error code of the recording error encountered.
     * @param description Description of the recording error encountered.
     */
    @Override
    public void onRecordingError(String recordingId, int errorCode, String description) {
        Log.d(TAG, "[onRecordingError]");

        presenter.onServiceRequestRecordingError(mContext, recordingId, errorCode, description);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from StatsListener need to be implemented for stats (in Multi Video function)
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered when the instantaneous transfer speed of a media stream,
     * at the moment of request, is available.
     *
     * @param peerId         PeerId of the remote Peer for which we are getting stats on.
     * @param mediaDirection Integer that defines the direction of media stream(s) reported on
     * @param mediaType      Integer that defines the type(s) of media reported on
     * @param transferSpeed  Transfer speed in kilobit per second (kbps).
     */
    @Override
    public void onTransferSpeedReceived(String peerId, int mediaDirection, int mediaType, double transferSpeed) {
        Log.d(TAG, "[onTransferSpeedReceived]");

        presenter.onServiceRequestTransferSpeedReceived(peerId, mediaDirection, mediaType, transferSpeed);
    }

    /**
     * This is triggered when WebRTC statistics of the specified media stream are available.
     *
     * @param peerId         PeerId of the remote Peer for which we are getting stats on.
     * @param mediaDirection Integer that defines the direction of media stream(s) reported on
     * @param mediaType      Integer that defines the type(s) of media reported on
     * @param stats          Map containing WebRTC provided statistics on the specified media.
     */
    @Override
    public void onWebrtcStatsReceived(String peerId, int mediaDirection, int mediaType, HashMap<String, String> stats) {
        Log.d(TAG, "[onWebrtcStatsReceived]");

        presenter.onServiceRequestWebrtcStatsReceived(peerId, mediaDirection, mediaType, stats);
    }

    //----------------------------------------------------------------------------------------------
    // Public methods helps to work with SkylinkSDK
    //----------------------------------------------------------------------------------------------

    public boolean isConnectingOrConnected() {
        return mSkylinkConnectionManager.isConnectingOrConnected();
    }

    /**
     * Connect to room using a {@link SkylinkConfig.VideoDevice} that:
     * - Is appropriate for the {@link SkylinkConfig}, and
     * - Takes into account the default {@link SkylinkConfig.VideoDevice} set in SA Config.
     *
     * @param typeCall
     */
    public void connectToRoom(Constants.CONFIG_TYPE typeCall) {
        // Check if custom VideoCapturer is required.
        boolean useCustomCapturer = false;
        switch (typeCall) {
            case AUDIO:
            case CHAT:
            case DATA:
            case FILE:
                break;
            case VIDEO:
            case MULTI_PARTY_VIDEO:
                useCustomCapturer = true;
                break;
        }

        SkylinkConfig.VideoDevice videoDevice = Utils.getDefaultVideoDevice();
        if (videoDevice != SkylinkConfig.VideoDevice.CUSTOM_CAPTURER) {
            useCustomCapturer = false;
        }
        VideoCapturer customCapturer = null;
        if (useCustomCapturer) {
            customCapturer = createCameraVideoCapturer();
        }

        mSkylinkConnectionManager.connectToRoom(typeCall, customCapturer);
    }

    /**
     * Create a {@link CameraVideoCapturer}.
     *
     * @return Null if unable to create capturer.
     */
    private VideoCapturer createCameraVideoCapturer() {
        String logTag = "";
        String log;
        CameraEnumerator cameraEnumerator = getCameraEnumerator();
        if (cameraEnumerator == null) {
            log = logTag + "Unable to create cameraVideoCapturer as we could not get a CameraEnumerator!";
            Log.d(TAG, log);
            return null;
        }
        String[] cameraNames = cameraEnumerator.getDeviceNames();
        if (cameraNames == null || cameraNames.length < 1) {
            log = logTag + "Unable to create cameraVideoCapturer as no camera was detected!";
            Log.d(TAG, log);
            return null;
        }
        // Start with the first named camera.
        String cameraName = cameraNames[0];
        CameraVideoCapturer.CameraEventsHandler cameraEventsHandler =
                new CameraVideoCapturer.CameraEventsHandler() {
                    String logTag = "[SA][CustomCapturer] ";
                    String log;

                    @Override
                    public void onCameraError(String errorDescription) {
                        log = logTag + "Camera had an error! Error: " + errorDescription;
                        Log.d(TAG, log);
                    }

                    @Override
                    public void onCameraDisconnected() {
                        log = logTag + "Camera disconnected.";
                        Log.d(TAG, log);
                    }

                    @Override
                    public void onCameraFreezed(String errorDescription) {
                        log = logTag + "Camera frozed! Error: " + errorDescription;
                        Log.d(TAG, log);
                    }

                    @Override
                    public void onCameraOpening(String cameraName) {
                        log = logTag + "Camera opened: Camera name: " + cameraName + ".";
                        Log.d(TAG, log);
                    }

                    @Override
                    public void onFirstFrameAvailable() {
                        log = logTag + "Camera first frame available.";
                        Log.d(TAG, log);
                    }

                    @Override
                    public void onCameraClosed() {
                        log = logTag + "Camera closed.";
                        Log.d(TAG, log);
                    }
                };
        VideoCapturer cameraVideoCapturer =
                cameraEnumerator.createCapturer(cameraName, cameraEventsHandler);
        log = logTag + "Created CameraVideoCapturer: " + cameraVideoCapturer;
        Log.d(TAG, log);
        return cameraVideoCapturer;
    }

    /**
     * Get the CameraEnumerator to use.
     * If Camera2 is supported, use Camera2Enumerator.
     * Otherwise use Camera1Enumerator.
     *
     * @return
     */
    private CameraEnumerator getCameraEnumerator() {
        String logTag = "[SMS][getCamEnum] ";
        String log = logTag;

        Context context = mContext.getApplicationContext();
        if (context == null) {
            log = logTag + "Failed as appContext is null.";
            Log.d(TAG, log);
            return null;
        }

        Boolean canUseCamera2 = isUseCamera2();
        if (canUseCamera2 == null) {
            log += "Unable to get CameraEnumerator!";
            Log.d(TAG, log);
            return null;
        }

        CameraEnumerator enumerator;
        if (canUseCamera2) {
            enumerator = new Camera2Enumerator(context);
            Log.d(TAG, "Using camera2 enumerator.");
        } else {
            enumerator = new Camera1Enumerator();
            Log.d(TAG, "Using camera1 enumerator.");
        }
        return enumerator;
    }

    /**
     * Check if able to use android.hardware.camera2 (Lollipop and above).
     *
     * @return null if unable to perform check, e.g. if context is null.
     */
    private Boolean isUseCamera2() {
        String logTag = "[SMS][isUseCam2] ";
        String log = logTag;

        Context context = mContext.getApplicationContext();
        if (context == null) {
            log += "Failed as appContext is null.";
            Log.d(TAG, log);
            return null;
        }

        Boolean result = Camera2Enumerator.isSupported(context);
        log += "Camera2 is supported: " + result + ".";
        Log.d(TAG, log);
        return result;
    }


    public void disconnectFromRoom() {
        mSkylinkConnectionManager.disconnectFromRoom();
    }

    //static method for the other to access current instance of SkylinkConnection
    public static SkylinkConnection getCurrentSkylinkConnection() {
        return mSkylinkConnection;
    }


    /**
     * Get peerId of a Peer by its index using SkylinkConnection API.
     * We can also get a specific peer by mPeersList
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
//        return mPeersList.get(index).getPeerId();
    }

    /**
     * Get nickName or userName combined with peerId of a peer in room
     *
     * @param peerId null is local peer, no null is specific peer
     * @return Desired nickName or userName combined with peer id of the peer
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

    /**
     * Return the SkylinkPeer with the specific peerId
     *
     * @param peerId id of the peer need to search
     * @return SkylinkPeer
     */
    public SkylinkPeer getPeerById(String peerId) {
        for (int i = 0; i < mPeersList.size(); i++) {
            if (mPeersList.get(i).getPeerId().equals(peerId))
                return mPeersList.get(i);
        }

        return null;
    }

    /**
     * Provides the room ID of the room currently connected to.
     */
    public String getRoomId() {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getRoomId();
        }

        return null;
    }

    public String getPeerNameById(String peerId) {
        if (mSkylinkConnection != null) {
            return getPeerIdNick(peerId);
        }

        return null;
    }

    /**
     * Get the room name with room id combined with local peer name and peer id
     * for a specific demo/function
     */
    public String getRoomIdAndNickname(Constants.CONFIG_TYPE typeCall) {
        if (mSkylinkConnection == null) {
            return null;
        }

        String roomName = Utils.getRoomNameByType(typeCall);

        String title = "Room: " + getRoomIdAndNickname(roomName);
        title += "\r\n" + getPeerIdNick(getPeerId());
        return title;

    }

    /**
     * Get the room name with room id
     */
    public String getRoomIdAndNickname(String roomName) {
        //in case of display room Name and room Id
        String roomId = "";
        if (mSkylinkConnection != null) {
            roomId = mSkylinkConnection.getRoomId();
        }

        return roomName + "(" + roomId + ")";
    }

    /**
     * Get local/self PeerId, or return null if not available.
     */
    public String getPeerId() {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getPeerId();
        }

        return null;
    }

    public List<SkylinkPeer> getPeersList() {
        return mPeersList;
    }

    /**
     * Get total number of peers in room
     */
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

    //----------------------------------------------------------------------------------------------
    // Private methods for internal processing
    //----------------------------------------------------------------------------------------------

    /**
     * Retrieves the user defined data object of a peer.
     *
     * @param peerId The PeerId of the peer whose UserData is to be retrieved, or NULL for self.
     * @return May be a 'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     */
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

    /**
     * Retrieves the UserInfo object of a Peer.
     *
     * @param userId The PeerId of the peer whose UserInfo is to be retrieved, or NULL for self.
     * @return UserInfo
     */
    private UserInfo getUserInfo(String userId) {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getUserInfo(userId);
        }
        return null;
    }
}
