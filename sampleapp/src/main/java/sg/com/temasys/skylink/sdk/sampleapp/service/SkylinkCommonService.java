package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkError;
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent;
import sg.com.temasys.skylink.sdk.rtc.SkylinkInfo;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkEvent.CONTEXT_DESCRIPTION;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkEvent.REMOTE_PEER_ID;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for implementing all SkylinkListeners for common use of all demos/functions
 * And directly works with SkylinkSDK.
 * In case user does not want to implement a specific demo/function, no need to implement corresponding listener(s).
 */

public abstract class SkylinkCommonService implements LifeCycleListener, MediaListener, OsListener, RemotePeerListener, MessagesListener,
        DataTransferListener, FileTransferListener, RecordingListener {

    private final String TAG = SkylinkCommonService.class.getName();

    protected Context context;

    // main presenter for audio/video/chat/...
    protected BasePresenter presenter;
    // video resolution presenter to implements video resolution logic
    protected BasePresenter videoResPresenter;

    //Skylink connection manager object
    private SkylinkConnectionManager skylinkConnectionManager;

    // SkylinkConnection instance for working with SkylinkSDK
    protected static SkylinkConnection skylinkConnection;

    //list of peers (including selfPeer) joined in room
    protected List<SkylinkPeer> mPeersList;

    //room name and user name for each demo/function
    protected String roomName;
    protected String userName;

    // local SkylinkMedia objects to control locally
    protected SkylinkMedia localAudio, localVideo, localScreen;

    public SkylinkCommonService(Context context) {
        this.context = context;
        this.skylinkConnectionManager = new SkylinkConnectionManager(this, context);
    }

    protected void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }

    //abstract methods need to be implemented in each demo/function like AudioService, VideoService,...
    public abstract void setSkylinkListeners();

    public abstract SkylinkConfig getSkylinkConfig();

    //----------------------------------------------------------------------------------------------
    // Override methods from Skylink Listener(s)
    // These methods are responsible to inform user the results from SkylinkSDK
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Methods which are from LifeCycleListener need to be implemented for all functions
    //----------------------------------------------------------------------------------------------

    /**
     * This is the first callback from SkylinkSDK to specify whether the attempt to connect to the room was successful.
     */
    @Override
    public void onConnectToRoomSucessful() {
        Log.d(TAG, "onConnectToRoomSucessful");

        String log = "[onConnectToRoomSuccessful] ";
        // get the self peer id
        String localPeerId = skylinkConnection.getLocalPeerId();

        log += "Connected to room " + getRoomIdAndNickname(roomName) + " as " + userName + " (" + localPeerId + ")";
        toastLog(TAG, context, log);

        //init/reset peers list and add self/local peer to list
        if (mPeersList == null) {
            mPeersList = new ArrayList<SkylinkPeer>();
        } else {
            mPeersList.clear();
        }

        //add self peer as a peer in list
        SkylinkPeer selfPeer = new SkylinkPeer(localPeerId, userName);

        // add local media to selfPeer if it was created
        if (localAudio != null) {
            selfPeer.addMedia(localAudio);
        }

        if (localVideo != null) {
            selfPeer.addMedia(localVideo);
        }

        if (localScreen != null) {
            selfPeer.addMedia(localScreen);
        }

        mPeersList.add(selfPeer);

        //update UI to connected state
        presenter.processRoomConnected(true);
    }

    /**
     * This is triggered when there is error for connecting to the room
     */
    @Override
    public void onConnectToRoomFailed(String errorMessage) {
        Log.d(TAG, "onConnectToRoomFailed(" + errorMessage + ")");
        toastLog(TAG, context, errorMessage);
        presenter.processRoomConnected(false);
    }

    /**
     * This method is triggered from SkylinkSDK to inform that user is disconnected from room successfully or not
     */
    @Override
    public void onDisconnectFromRoom(SkylinkEvent skylinkEvent, String contextDescription) {
        Log.d(TAG, "onDisconnectFromRoom(" + skylinkEvent + ", message: " + contextDescription + ")");
        String log = "[onDisconnectFromRoom] ";

        if (skylinkEvent == SkylinkInfo.ROOM_DISCONNECT) {
            log += "We have successfully disconnected from the room.";
        } else if (skylinkEvent == SkylinkError.ROOM_DISCONNECT_ERROR) {
            log += "WARNING! We have been unexpectedly disconnected from the room!";
        }
        log += " Server message: " + contextDescription;
        toastLogLong(TAG, context, log);

        //update UI to disconnected state for all types call
        presenter.processRoomDisconnected();
    }

    /**
     * This is triggered from SkylinkSDK when the lock status of the room that we are is changed
     */
    @Override
    public void onChangeRoomLockStatus(boolean roomLockStatus, String peerId) {
        Log.d(TAG, "onChangeRoomLockStatus(roomLockStatus: " + roomLockStatus + ", remotePeerId: " + peerId + ")");

        String log = "[SA][onChangeRoomLockStatus] ";

        log += "[SA] Peer " + peerId + " changed Room locked status to "
                + roomLockStatus + ".";
        toastLog(TAG, context, log);

        presenter.processRoomLockStatusChanged(roomLockStatus);
    }

    /**
     * This is triggered from SkylinkSDK to deliver messages that might be useful to the user.
     */
    @Override
    public void onReceiveInfo(SkylinkInfo skylinkInfo, HashMap<String, Object> details) {
        String contextDescriptionString = (String) details.get(CONTEXT_DESCRIPTION);
        Log.d(TAG, "onReceiveInfo(skylinkInfo: " + skylinkInfo.toString() + ", details: " + contextDescriptionString);

        String tag = "[SA][onReceiveInfo] ";
        Utils.handleSkylinkReceiveInfo(skylinkInfo, contextDescriptionString, context, tag);
    }

    /**
     * This is triggered from SkylinkSDK to deliver a warning message to the user
     */
    @Override
    public void onReceiveWarning(SkylinkError skylinkError, HashMap<String, Object> details) {
        String contextDescriptionString = (String) details.get(CONTEXT_DESCRIPTION);
        Log.d(TAG, "onReceiveWarning(skylinkError: " + skylinkError.toString() + ", details: " + contextDescriptionString);

        String tag = "[SA][onReceiveWarning] ";
        Utils.handleSkylinkWarningErrorMsg(skylinkError, contextDescriptionString, context, tag);
    }

    /**
     * This is triggered from SkylinkSDK to deliver a error message to the user
     */
    @Override
    public void onReceiveError(SkylinkError skylinkError, HashMap<String, Object> details) {
        String contextDescriptionString = (String) details.get(CONTEXT_DESCRIPTION);
        Log.d(TAG, "onReceiveError(skylinkError: " + skylinkError.toString() + ", details: " + contextDescriptionString);

        String tag = "[SA][onReceiveError] ";
        Utils.handleSkylinkWarningErrorMsg(skylinkError, contextDescriptionString, context, tag);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from MediaListener need to be implemented for audio and video functions
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered from SkylinkSDK when successfully captures the local device's media input,
     * for both audio, video, screen
     * This may be at the start of media usage or when media source has been changed.
     * It can happen only if the connection is configured to have a media configuration.
     *
     * @param localMedia the local media object
     */
    @Override
    public void onCreateLocalMedia(SkylinkMedia localMedia) {
        Log.d(TAG, "onCreateLocalMedia(localMedia: " + localMedia.toString());

        if (!localMedia.isVideo()) {
            localAudio = localMedia;
            presenter.processLocalAudioCaptured(localMedia);
        } else {
            if (SkylinkMedia.MediaType.VIDEO_CAMERA == localMedia.getMediaType()
                    || SkylinkMedia.MediaType.VIDEO == localMedia.getMediaType()) {
                localVideo = localMedia;
                presenter.processLocalCameraCaptured(localMedia);
            } else if (SkylinkMedia.MediaType.VIDEO_SCREEN == localMedia.getMediaType()) {
                localScreen = localMedia;
                presenter.processLocalScreenCaptured(localMedia);
            }
        }
    }

    /**
     * This is triggered from SkylinkSDK upon receiving the media object of the remote peer
     * if the connection is configured to have media configuration.
     * This will only be triggered after onConnectWithRemotePeer has triggered.
     *
     * @param remoteMedia  the remote media object
     * @param remotePeerId The id of the remote peer that media comes form
     */
    @Override
    public void onReceiveRemoteMedia(SkylinkMedia remoteMedia, String remotePeerId) {
        Log.d(TAG, "onReceiveRemoteMedia(remoteMedia: " + remoteMedia.toString() + ", remotePeerId: " + remotePeerId);

        String log = "[SA][onReceiveRemoteMedia] ";
        log += "Received new remote media (" + remoteMedia.getMediaId() + ") from Peer " + getPeerIdNick(remotePeerId) + ".\r\n";
        toastLog(TAG, context, log);

        SkylinkPeer remotePeer = getPeerById(remotePeerId);
        if (remotePeer != null) {
            remotePeer.addMedia(remoteMedia);
        }

        if (remoteMedia.isVideo()) {
            presenter.processRemoteVideoReceived(remotePeerId, remoteMedia);
        } else {
            presenter.processRemoteAudioReceived(remotePeerId);
        }
    }

    /**
     * This is triggered from SkylinkSDK when a local media has been changed it state
     *
     * @param localMedia The media object with new state
     */
    @Override
    public void onChangeLocalMedia(SkylinkMedia localMedia) {
        Log.d(TAG, "onChangeLocalMedia(localMedia: " + localMedia.toString());

        String log = "[SA][onChangeLocalMedia] ";
        log += "Local media (" + localMedia.getMediaId() + ") state has been changed to " + localMedia.getMediaState() + ".";

        toastLog(TAG, context, log);

        // update local media objects
        if (localMedia.getMediaType() == SkylinkMedia.MediaType.AUDIO || localMedia.getMediaType() == SkylinkMedia.MediaType.AUDIO_MIC) {
            localAudio = localMedia;
        } else if (localMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO || localMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            localVideo = localMedia;
        } else if (localMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            localScreen = localMedia;
        }

        if (mPeersList != null && mPeersList.size() > 0) {
            SkylinkPeer selfPeer = mPeersList.get(0);
            selfPeer.updateMedia(localMedia);
        }

        presenter.processMediaStateChanged(localMedia, true);
    }

    /**
     * This is triggered from SkylinkSDK when a remote peer has changed his media state.
     *
     * @param remoteMedia  The remote media object with new value like state
     * @param remotePeerId The id of the peer (can be selfPeer or remote peer)
     */
    @Override
    public void onChangeRemoteMedia(SkylinkMedia remoteMedia, String remotePeerId) {
        Log.d(TAG, "onChangeRemoteMedia(remoteMedia: " + remoteMedia.toString() + ", remotePeerId: " + remotePeerId);

        String log = "[SA][onChangeRemoteMedia] ";
        log += "Peer " + getPeerIdNick(remotePeerId) +
                " Media (" + remoteMedia.getMediaId() + ") state has been changed to " + remoteMedia.getMediaState() + ".";

        toastLog(TAG, context, log);

        // update remote media object for remote peer
        SkylinkPeer remotePeer = getPeerById(remotePeerId);

        if (remotePeer == null)
            return;

        remotePeer.updateMedia(remoteMedia);

        presenter.processMediaStateChanged(remoteMedia, false);

        // process remove local media id for the self peer if media state is UNAVAILABLE
        if (remoteMedia.getMediaState() == SkylinkMedia.MediaState.UNAVAILABLE) {
            remotePeer.removeMediaId(remoteMedia.getMediaId());
        }
    }

    /**
     * This is triggered when a specific local media object is destroyed
     *
     * @param localMedia The local media object that was destroyed
     */
    @Override
    public void onDestroyLocalMedia(SkylinkMedia localMedia) {
        Log.d(TAG, "onDestroyLocalMedia(localMedia: " + localMedia.toString());

        String log = "[SA][onDestroyLocalMedia] ";
        log += "Local media (" + localMedia.getMediaId() + ") has been destroyed.";

        toastLog(TAG, context, log);

        presenter.processMediaStateChanged(localMedia, true);

        if (mPeersList != null && mPeersList.size() > 0) {
            mPeersList.get(0).removeMediaId(localMedia.getMediaId());
        }

        // remove local media id
        if (localMedia.getMediaType() == SkylinkMedia.MediaType.AUDIO || localMedia.getMediaType() == SkylinkMedia.MediaType.AUDIO_MIC) {
            localAudio = null;
        } else if (localMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO || localMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            localVideo = null;
        } else if (localMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            localScreen = null;
        }
    }

    @Override
    public void onObtainInputVideoResolution(int width, int height, int fps, SkylinkCaptureFormat captureFormat, String mediaId) {
        Log.d(TAG, "onObtainInputVideoResolution(width: " + width + ", height: " + height + ", fps: " + fps);

        SkylinkMedia.MediaType mediaType = null;
        if (localVideo != null && mediaId.equals(localVideo.getMediaId())) {
            mediaType = SkylinkMedia.MediaType.VIDEO_CAMERA;
        } else if (localScreen != null && mediaId.equals(localScreen.getMediaId())) {
            mediaType = SkylinkMedia.MediaType.VIDEO_SCREEN;
        }

        if (mediaType != null)
            obtainInputVideoResolution(width, height, fps, captureFormat, mediaType);
    }

    @Override
    public void onObtainReceivedVideoResolution(int width, int height, int fps, String mediaId, String remotePeerId) {
        Log.d(TAG, "onObtainReceivedVideoResolution(width: " + width + ", height: " + height + ", fps: " + fps +
                ", mediaId: " + mediaId + ", remotePeerId: " + remotePeerId);

        SkylinkMedia.MediaType mediaType = null;
        SkylinkPeer remotePeer = getPeerById(remotePeerId);
        if (remotePeer != null && remotePeer.getMediaMap().get(mediaId) != null) {
            mediaType = remotePeer.getMediaMap().get(mediaId).getMediaType();
        }

        if (mediaType != null)
            obtainReceivedVideoResolution(width, height, fps, mediaType, remotePeerId);
    }

    @Override
    public void onObtainSentVideoResolution(int width, int height, int fps, String mediaId, String remotePeerId) {
        Log.d(TAG, "onObtainSentVideoResolution(width: " + width + ", height: " + height + ", fps: " + fps +
                ", mediaId: " + mediaId + ", remotePeerId: " + remotePeerId);

        SkylinkMedia.MediaType mediaType = null;
        if (localVideo != null && mediaId.equals(localVideo.getMediaId())) {
            mediaType = SkylinkMedia.MediaType.VIDEO_CAMERA;
        } else if (localScreen != null && mediaId.equals(localScreen.getMediaId())) {
            mediaType = SkylinkMedia.MediaType.VIDEO_SCREEN;
        }

        if (mediaType != null)
            obtainSentVideoResolution(width, height, fps, mediaType, remotePeerId);
    }

    /**
     * This is triggered from SkylinkSDK when any of the given video streams' frame size changes. It includes the
     * self stream also.
     *
     * @param size         Size of the video frame
     * @param media        The SkylinkMedia video object
     * @param remotePeerId The id of the peer. If null, it indicates self stream.
     */
    @Override
    public void onChangeVideoSize(Point size, SkylinkMedia media, String remotePeerId) {
        Log.d(TAG, "[onChangeVideoSize] The media(" + media.getMediaId() + ") from Peer " +
                "(" + getPeerNameById(remotePeerId) + ") has changed the size to " + size.toString());
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from OsListener need to be implemented for audio, video, fileTransfer, multiVideo functions
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered when an Android Intent is required to use a part of the SDK. For e.g.,
     * the intent from {@link android.app.Activity#onActivityResult(int, int, Intent)} generated
     * from {@link android.app.Activity#startActivityForResult(Intent, int)}, using the intent of
     * {@link android.media.projection.MediaProjectionManager#createScreenCaptureIntent},
     * is required before screen capture can be performed.
     * When received, app should use the intent and requestCode provided here to call
     * {@link android.app.Activity#startActivityForResult(Intent, int)}.
     * Once the corresponding {@link android.app.Activity#onActivityResult(int, int, Intent)}
     * is received, app should pass the parameters (requestCode, resultCode, Intent) to the SDK's
     * {@link SkylinkConnection#processActivityResult(int, int, Intent)}.
     * The SDK will process this result and also notify app via
     * {@link OsListener#onGrantPermission} or {@link OsListener#onDenyPermission}.
     *
     * @param intent      An Intent to be passed to
     *                    {@link android.app.Activity#startActivityForResult(Intent, int)}.
     * @param requestCode An unique integer that represents this permission request.
     *                    This is to be used as the requestCode when calling Android's
     *                    {@link android.app.Activity#startActivityForResult(Intent, int)}.
     * @param skylinkInfo
     */
    public void onRequirePermission(Intent intent, int requestCode, SkylinkInfo skylinkInfo) {
        Log.d(TAG, "onRequirePermission(intent: " + intent.getAction() + ", requestCode: " + requestCode + ", SkylinkInfo: " + skylinkInfo.getDescription());
        presenter.processIntentRequired(intent, requestCode, skylinkInfo);
    }

    /**
     * This is triggered when Android Intent based permission has been granted for the usage of
     * a particular part of the SDK.
     *
     * @param intent      As that in {@link OsListener#onRequirePermission(Intent, int, SkylinkInfo)}.
     * @param requestCode As that in {@link OsListener#onRequirePermission(Intent, int, SkylinkInfo)}.
     * @param skylinkInfo
     */
    public void onGrantPermission(Intent intent, int requestCode, SkylinkInfo skylinkInfo) {
        Log.d(TAG, "onGrantPermission(intent: " + intent.getAction() + ", requestCode: " + requestCode + ", SkylinkInfo: " + skylinkInfo.getDescription());
        presenter.processPermissionGranted(intent, requestCode, skylinkInfo);
    }

    /**
     * This is triggered when Android Intent based permission has been denied for the usage of
     * a particular part of the SDK.
     *
     * @param intent      As that in {@link OsListener#onRequirePermission(Intent, int, SkylinkInfo)}.
     * @param requestCode As that in {@link OsListener#onRequirePermission(Intent, int, SkylinkInfo)}.
     * @param skylinkInfo
     */
    public void onDenyPermission(Intent intent, int requestCode, SkylinkInfo skylinkInfo) {
        Log.d(TAG, "onDenyPermission(intent: " + intent + ", requestCode: " + requestCode + ", SkylinkInfo: " + skylinkInfo.getDescription());
        presenter.processPermissionDenied(intent, requestCode, skylinkInfo);
    }

    /**
     * This is triggered from SkylinkSDK when Android Runtime permission is required to use
     * devices to perform media functions like audio, camera, file transfer,...
     * The SDK will process this result and also notify app via
     * {@link OsListener#onGrantPermission} or {@link OsListener#onDenyPermission}.
     *
     * @param permissions A Strings array containing the Android Runtime permission required
     *                    at the first array index (index 0).
     * @param requestCode An unique integer that represents this permission request.
     *                    This is to be used as the requestCode when calling
     *                    Android's requestPermissions.
     * @param skylinkInfo
     */
    @Override
    public void onRequirePermission(String[] permissions, int requestCode, SkylinkInfo
            skylinkInfo) {
        Log.d(TAG, "onRequirePermission(permissions: " + permissions + ", requestCode: " + requestCode + ", SkylinkInfo: " + skylinkInfo.getDescription());
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, skylinkInfo);

        presenter.processPermissionRequired(info);
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
     * @param skylinkInfo
     */
    @Override
    public void onGrantPermission(String[] permissions, int requestCode, SkylinkInfo
            skylinkInfo) {
        Log.d(TAG, "onGrantPermission(permissions: " + permissions + ", requestCode: " + requestCode + ", SkylinkInfo: " + skylinkInfo.getDescription());
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, skylinkInfo);

        presenter.processPermissionGranted(info);
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
     * @param skylinkInfo
     */
    @Override
    public void onDenyPermission(String[] permissions,
                                 int requestCode, SkylinkInfo skylinkInfo) {
        Log.d(TAG, "onDenyPermission(permissions: " + permissions + ", requestCode: " + requestCode + ", SkylinkInfo: " + skylinkInfo.getDescription());
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, skylinkInfo);

        presenter.processPermissionDenied(context, info);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from RemotePeerListener need to be implemented for all functions
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered when a remote peer joins the room.
     * It will be triggered before onConnectWithRemotePeer is triggered
     *
     * @param remotePeerId The id of the peer
     * @param userInfo     Info about the remote user, including userData (for eg.user name). UserData May be a
     *                     'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     */
    @Override
    public void onReceiveRemotePeerJoinRoom(String remotePeerId, UserInfo
            userInfo) {
        Log.d(TAG, "onReceiveRemotePeerJoinRoom(remotePeerId: " + remotePeerId + ", userInfo: " + userInfo.getUserData());

        String log = "[SA][onReceiveRemotePeerJoinRoom]" + " Remote Peer " + getPeerIdNick(remotePeerId) + " has joined the room.";
        toastLog(TAG, context, log);
    }

    /**
     * This is triggered when our connection with the remote peer has been established
     * It will be triggered before onRemotePeerMediaReceive is triggered
     *
     * @param remotePeerId   The id of the remote peer
     * @param userInfo       Info about the remote user, including userData. UserData May be a
     *                       'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param hasDataChannel Whether peer has enabled DataChannel.
     */
    @Override
    public void onConnectWithRemotePeer(String remotePeerId, UserInfo userInfo,
                                        boolean hasDataChannel) {
        Log.d(TAG, "onConnectWithRemotePeer(remotePeerId: " + remotePeerId + ", userInfo: " + userInfo.getUserData() + ", hasDataChannel: " + hasDataChannel);

        String logTag = "[SA][onConnectWithRemotePeer] ";

        // When remote peer joins room, keep track of user and update text-view to display details
        String remotePeerName = null;
        Object userData = userInfo.getUserData();
        if (userData instanceof String) {
            remotePeerName = (String) userData;
        } else if (userData instanceof JSONObject) {
            try {
                remotePeerName = ((JSONObject) userData).get("name").toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // use peer id if remote peer does not send the user name
        if (remotePeerName == null || remotePeerName.equals("")) {
            remotePeerName = remotePeerId;
        }

        //add remote peer into list
        SkylinkPeer remotePeer = new SkylinkPeer(remotePeerId, remotePeerName);
        mPeersList.add(remotePeer);

        presenter.processRemotePeerConnected(remotePeer);

        logTag += "Your Peer " + getPeerIdNick(remotePeerId) + " connected.";
        toastLog(TAG, context, logTag);
    }

    /**
     * This is triggered when the connection with a remote peer is refreshed.
     *
     * @param remotePeerId   The id of the peer.
     * @param userInfo       Info of the remote user, including UserData. UserData may be a
     *                       'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param hasDataChannel Whether peer has enabled DataChannel.
     * @param isIceRestarted Indicates if ICE restart was done when PeerConnection was refreshed.
     */
    @Override
    public void onRefreshRemotePeerConnection(String remotePeerId, UserInfo
            userInfo, boolean hasDataChannel, boolean isIceRestarted) {
        Log.d(TAG, "onRefreshRemotePeerConnection(remotePeerId: " + remotePeerId + ", userInfo: " + userInfo.getUserData() + ", hasDataChannel: " + hasDataChannel + ", isIceRestarted: " + isIceRestarted);

        String logTag = "[SA][onRefreshRemotePeerConnection]";
        String peer = "Skylink Media Relay server";
        if (remotePeerId != null) {
            peer = "Peer " + getPeerIdNick(remotePeerId);
        }
        String log = logTag + "Your connection with " + peer + " has just been refreshed";
        if (isIceRestarted) {
            log += ", with ICE restarted.\r\n";
        } else {
            log += ".\r\n";
        }

        toastLog(TAG, context, log);
    }

    /**
     * This is triggered when an update is received in the user defined data of a remote peer.
     *
     * @param userData     User defined data relating to the remote peer. May be a
     *                     'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param remotePeerId The id of the peer.
     */
    @Override
    public void onReceiveRemotePeerUserData(Object userData, String
            remotePeerId) {
        Log.d(TAG, "onReceiveRemotePeerUserData(userData: " + userData.toString() + ", remotePeerId: " + remotePeerId);

        // If Peer has no userData, use an empty string for nick.
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

        String log = "[SA][onReceiveRemotePeerUserData] Peer (" + remotePeerId + ") " +
                " has a new user data :\n" + remotePeerName;
        toastLog(TAG, context, log);
    }

    /**
     * This is triggered when the underlying peer to peer data connection is established with a
     * remote peer and is ready to send and receive peer messages and data (including files).
     *
     * @param remotePeerId The id of the remote peer
     */
    @Override
    public void onOpenRemotePeerDataConnection(String remotePeerId) {
        Log.d(TAG, "onOpenRemotePeerDataConnection(remotePeerId: " + remotePeerId);
    }

    @Override
    public void onDisconnectWithRemotePeer(String remotePeerId, UserInfo
            userInfo, boolean hasDataChannel) {
        Log.d(TAG, "onDisconnectWithRemotePeer(remotePeerId: " + remotePeerId + ", userInfo: " + userInfo.getUserData() + ", hasDataChannel: " + hasDataChannel);

        String log = "[SA][onDisconnectWithRemotePeer]" + " Our connection with Remote Peer " + getPeerIdNick(remotePeerId) + " has been terminated.";
        toastLog(TAG, context, log);

        disconnectRemotePeer(remotePeerId);
    }

    /**
     * This is triggered when a remote peer leaves the room.
     *
     * @param remotePeerId The id of the remote peer
     * @param skylinkInfo  The {@link SkylinkInfo} with more details on this event of
     *                     Peer leaving room.
     * @param userInfo     UserInfo of this remote Peer.
     */
    @Override
    public void onReceiveRemotePeerLeaveRoom(String remotePeerId, SkylinkInfo skylinkInfo, UserInfo userInfo) {
        Log.d(TAG, "onReceiveRemotePeerLeaveRoom(remotePeerId: " + remotePeerId + ", SkylinkInfo: " + skylinkInfo.getDescription() + ", userInfo: " + userInfo.getUserData());

        disconnectRemotePeer(remotePeerId);

        int numRemotePeers = mPeersList.size() - 1;
        if (numRemotePeers >= 0) {
            String log = "Your Peer " + getUserDataString(userInfo) + " (" + remotePeerId + ") left."
                    + skylinkInfo.getDescription() + " " + numRemotePeers + " remote Peer(s) left in the room.";
            toastLog(TAG, context, log);
        }
    }

    /**
     * This is triggered when having fail connection with a remote peer
     * When user get this callback, they can reconnect or refresh the connection with the remote peer,
     * or simply ignore this peer
     *
     * @param skylinkError {@link SkylinkError} with more details about this error.
     * @param details      Additional details in a map.
     */
    @Override
    public void onErrorForRemotePeerConnection(SkylinkError skylinkError, HashMap<String, Object> details) {
        String logTag = "[SA][onErrorForRemotePeerConnection] ";
        String contextDescription = (String) details.get(CONTEXT_DESCRIPTION);
        String remotePeerId = (String) details.get(REMOTE_PEER_ID);
        String log = logTag + "Your connection with the peer (" + remotePeerId +
                ") is NOT successful. " + contextDescription + " " + skylinkError;
        Log.d(TAG, log);
        toastLog(TAG, context, log);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from DataTransferListener need to be implemented for dataTransfer function
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered when data is received
     *
     * @param data         Array of bytes
     * @param remotePeerId The id of the remote peer
     */
    @Override
    public void onReceiveData(byte[] data, String remotePeerId) {
        Log.d(TAG, "onReceiveData(data: " + data.length + ", remotePeerId: " + remotePeerId);

        presenter.processDataReceive(context, remotePeerId, data);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from FileTransferListener need to be implemented for fileTransfer function
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered upon receiving a file transfer request from a peer.
     *
     * @param fileName     The name of the file
     * @param isPublic     Flag to specify if file share was directed to only us (as opposed to all
     *                     peers in the room).
     * @param remotePeerId The id of the peer that the file comes from
     */
    @Override
    public void onReceiveFileTransferRequest(String fileName,
                                             boolean isPublic, String remotePeerId) {
        Log.d(TAG, "onReceiveFileTransferRequest(fileName: " + fileName + ", isPublic: " + isPublic + ", remotePeerId: " + remotePeerId);

        presenter.processFilePermissionRequested(remotePeerId, fileName, !isPublic);
    }

    /**
     * This is triggered upon receiving the response of a peer to our request for file transfer with
     * the peer.
     *
     * @param wasAccepted  Flag to specify whether the peer has accepted the request or not
     * @param fileName     The name of the file
     * @param remotePeerId The id of the peer that has accept for reject the file
     */
    @Override
    public void onReceiveFileTransferResponse(boolean wasAccepted, String
            fileName, String remotePeerId) {
        Log.d(TAG, "onReceiveFileTransferResponse(wasAccepted: " + wasAccepted + ",fileName: " + fileName + ", remotePeerId: " + remotePeerId);

        presenter.processFilePermissionResponded(context, remotePeerId, fileName, wasAccepted);
    }

    /**
     * This is triggered when an ongoing file transfer drops due to some reason.
     *
     * @param fileName     The name of the file
     * @param message      Message that possibly tells the reason for dropping
     * @param isExplicit   True if user canceled the transfer explicitly, as opposed to being due to
     *                     error in the transfer process.
     * @param remotePeerId The id of the peer. Null if broadcast file transfer was attempted
     */
    @Override
    public void onDropFileTransfer(String fileName, String message,
                                   boolean isExplicit, String remotePeerId) {
        Log.d(TAG, "onDropFileTransfer(fileName: " + fileName + ",message: " + message + ", isExplicit: " + isExplicit + ",remotePeerId: " + remotePeerId);

        presenter.processFileDropped(context, remotePeerId, fileName, message, isExplicit);
    }

    /**
     * This is triggered when we have sent a file successfully.
     *
     * @param fileName     The name of the file
     * @param remotePeerId The id of the peer receiving the file.
     */
    @Override
    public void onCompleteFileTransferSending(String fileName, String
            remotePeerId) {
        Log.d(TAG, "onCompleteFileTransferSending(fileName: " + fileName + ", remotePeerId: " + remotePeerId);

        presenter.processFileSentCompleted(context, remotePeerId, fileName);
    }

    /**
     * This is triggered when we have received a file successfully.
     *
     * @param fileName     The name of the file
     * @param remotePeerId The id of the peer receiving the file
     */
    @Override
    public void onCompleteFileTransferReceiving(String fileName, String
            remotePeerId) {
        Log.d(TAG, "onCompleteFileTransferReceiving(fileName: " + fileName + ", remotePeerId: " + remotePeerId);

        presenter.processFileReceivedCompleted(remotePeerId, fileName);
    }

    /**
     * This is triggered timely to report the on going progress when sending a file
     *
     * @param percentage   The percentage completed
     * @param fileName     The name of the file
     * @param remotePeerId The id of the peer receiving the file.
     */
    @Override
    public void onUpdateFileTransferSendingProgress(double percentage, String
            fileName, String remotePeerId) {
        Log.d(TAG, "onUpdateFileTransferSendingProgress(percentage: " + percentage + ",fileName: " + fileName + ", remotePeerId: " + remotePeerId);

        presenter.processFileSentProgressed(context, remotePeerId, fileName, percentage);
    }

    /**
     * This is triggered timely to report the on going progress when receiving a file
     *
     * @param percentage   The percentage completed
     * @param fileName     The name of the file
     * @param remotePeerId The id of the peer sending the file.
     */
    @Override
    public void onUpdateFileTransferReceivingProgress(
            double percentage, String fileName, String remotePeerId) {
        Log.d(TAG, "onUpdateFileTransferReceivingProgress(percentage: " + percentage + ",fileName: " + fileName + ", remotePeerId: " + remotePeerId);

        presenter.processFileReceivedProgressed(context, remotePeerId, fileName, percentage);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from MessagesListener need to be implemented for chat/messaging function
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered when a broadcast or private message is received from a remote peer via a
     * server.
     *
     * @param message      User defined message. May be a 'java.lang.String', 'org.json.JSONObject'
     *                     or 'org.json.JSONArray'.
     * @param isPublic     Flag to specify whether the message was sent to us only (as opposed to
     *                     broadcast to all peers in the room).
     * @param timeStamp    The timestamp of the message,
     *                     The number of milliseconds since the epoch of 1970-01-01T00:00:00Z (midnight, January 1, 1970 UTC).
     * @param remotePeerId The id of the remote peer
     */
    @Override
    public void onReceiveServerMessage(Object message, boolean isPublic, Long timeStamp, String
            remotePeerId) {
        Log.d(TAG, "onReceiveServerMessage(message: " + message + ",isPublic: " + isPublic + ", timeStamp: " + timeStamp + ", remotePeerId: " + remotePeerId);

        presenter.processServerMessageReceived(remotePeerId, message, !isPublic, timeStamp);
    }

    /**
     * This is triggered when a broadcast or private message is received directly from the peer,
     * i.e. in a peer to peer manner.
     *
     * @param message      User defined message. May be a 'java.lang.String', 'org.json.JSONObject'
     *                     or 'org.json.JSONArray'.
     * @param isPublic     Flag to specify whether the message was sent to us only (as opposed to
     *                     broadcast to all peers in the room).
     * @param timeStamp    The timestamp of the message,
     *                     The number of milliseconds since the epoch of 1970-01-01T00:00:00Z (midnight, January 1, 1970 UTC).
     * @param remotePeerId The id of the remote peer
     */
    @Override
    public void onReceiveP2PMessage(Object message, boolean isPublic, Long timeStamp, String
            remotePeerId) {
        Log.d(TAG, "onReceiveP2PMessage(message: " + message + ",isPublic: " + isPublic + ", timeStamp: " + timeStamp + ", remotePeerId: " + remotePeerId);

        presenter.processP2PMessageReceived(remotePeerId, message, !isPublic);
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
    public void onStartRecording(String recordingId) {
        Log.d(TAG, "onStartRecording(recordingId: " + recordingId);

        presenter.processRecordingStarted(context, skylinkConnection.isRecording());
    }

    /**
     * This is triggered to indicate that recording has stopped for this room.
     *
     * @param recordingId Id of Recording session this message is about.
     */
    @Override
    public void onStopRecording(String recordingId) {
        Log.d(TAG, "onStopRecording(recordingId: " + recordingId);

        presenter.processRecordingStopped(context, skylinkConnection.isRecording());
    }

    /**
     * This is triggered when a recording error occurs.
     *
     * @param errorCode   {@link sg.com.temasys.skylink.sdk.rtc.Errors Errors} Integer error code of the recording error encountered.
     * @param description Description of the recording error encountered.
     * @param recordingId Id of Recording session this message is about.
     */
    @Override
    public void onReceiveRecordingError(int errorCode, String
            description, String recordingId) {
        Log.d(TAG, "onReceiveRecordingError(errorCode: " + errorCode + ",description: " + description + ",recordingId: " + recordingId);

        toastLogLong(TAG, context, "onReceiveRecordingError(errorCode: " + errorCode + ",description: " + description + ",recordingId: " + recordingId);

        presenter.processRecordingError(context, recordingId, errorCode, description);
    }

    //----------------------------------------------------------------------------------------------
    // Public methods helps to work with SkylinkSDK
    //----------------------------------------------------------------------------------------------
    protected void initializeSkylinkConnection(Constants.CONFIG_TYPE typeCall) {
        Log.d(TAG, "initializeSkylinkConnection with typeCall: " + typeCall);
        this.skylinkConnection = skylinkConnectionManager.initializeSkylinkConnection(typeCall);

        // enable debugging log
        if (this.skylinkConnection != null) {
            setEnableLogsForDebugging(true);
        }
    }

    public boolean isConnectingOrConnected() {
        return skylinkConnectionManager.isConnectingOrConnected();
    }

    /**
     * Connect to room using a {@link SkylinkConfig.VideoDevice} that:
     * - Is appropriate for the {@link SkylinkConfig}, and
     * - Takes into account the default {@link SkylinkConfig.VideoDevice} set in SA Config.
     *
     * @param typeCall
     */
    public void connectToRoom(Constants.CONFIG_TYPE typeCall) {
        skylinkConnectionManager.connectToRoomByConnectionString(typeCall);

        // we can use another method to connect to the room
        // skylinkConnectionManager.connectToRoomByAppKey(typeCall);
    }


    public void disconnectFromRoom() {
        skylinkConnectionManager.disconnectFromRoom();
    }

    //static method for the other to access current instance of SkylinkConnection
    public static SkylinkConnection getCurrentSkylinkConnection() {
        return skylinkConnection;
    }

    /**
     * To enable logs from Skylink SDK (e.g. during debugging):
     * Should not enable logs for production apps!
     */
    protected void setEnableLogsForDebugging(boolean enableLogsForDebugging) {
        this.skylinkConnection.setEnableLogs(enableLogsForDebugging);
    }

    public String getPeerId(int index) {
        return mPeersList.get(index).getPeerId();
    }

    /**
     * Get nickName or userName combined with peerId of a peer in room
     *
     * @param peerId null is local peer, no null is specific peer
     * @return Desired nickName or userName combined with peer id of the peer
     */
    public String getPeerIdNick(String peerId) {

        //get local peer from SDK
        if (peerId == null && skylinkConnection != null) {
            peerId = skylinkConnection.getLocalPeerId();
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
     * Get nickName or userName combined with peerId of a peer in room
     *
     * @param peerId null is local peer, no null is specific peer
     * @return Desired nickName or userName of the peer
     */
    public String getPeerUserName(String peerId) {

        //get local peer from SDK
        if (peerId == null && skylinkConnection != null) {
            peerId = skylinkConnection.getLocalPeerId();
        }

        String peerIdShow = peerId;

        //can not get local peer
        if (peerId == null) {
            peerIdShow = "Self";
        }

        String peerUserName = getUserDataString(peerIdShow);

        return peerUserName;
    }

    /**
     * Return the SkylinkPeer with the specific peerId
     *
     * @param peerId id of the peer need to search
     * @return SkylinkPeer
     */
    public SkylinkPeer getPeerById(String peerId) {
        if (mPeersList == null || mPeersList.size() == 0)
            return null;

        for (SkylinkPeer peer : mPeersList) {
            if (peer.getPeerId().equals(peerId)) {
                return peer;
            }
        }

        return null;
    }

    /**
     * Provides the room ID of the room currently connected to.
     */
    public String getRoomId() {
        if (skylinkConnection != null) {
            return skylinkConnection.getRoomId();
        }

        return null;
    }

    public String getPeerNameById(String peerId) {
        if (skylinkConnection != null) {
            return getPeerUserName(peerId);
        }

        return null;
    }

    /**
     * Get the room name with room id combined with local peer name and peer id
     * for a specific demo/function
     */
    public String getRoomIdAndNickname(Constants.CONFIG_TYPE typeCall) {
        if (skylinkConnection == null) {
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
        if (skylinkConnection != null) {
            roomId = skylinkConnection.getRoomId();
        }

        return roomName + "(" + roomId + ")";
    }

    /**
     * Get local/self PeerId, or return null if not available.
     */
    public String getPeerId() {
        if (skylinkConnection != null) {
            return skylinkConnection.getLocalPeerId();
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
        this.roomName = roomName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * If the current local input video device is a camera,
     * change the current captured video stream to the specified resolution,
     * and the specified resolution will be set into SkylinkConfig.
     * Non-camera supported resolution can be accepted,
     * but a camera supported resolution will be used when opening camera.
     * There is no guarantee that a specific camera resolution will be maintained
     * as WebRTC may adjust the resolution dynamically to match its bandwidth criteria.
     *
     * @param mediaType the current type of video to set video resolution
     * @param width
     * @param height
     * @param fps
     */
    public void setInputVideoResolution(SkylinkMedia.MediaType mediaType,
                                        int width, int height, int fps) {
        if (skylinkConnection == null)
            return;

        // get video id
        String videoId = getProperLocalMediaId(mediaType);

        if (videoId == null)
            return;

        skylinkConnection.setInputVideoResolution(videoId, width, height, fps, new SkylinkCallback() {
            @Override
            public void onError(SkylinkError error, HashMap<String, Object> details) {
                String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                Log.e("SkylinkCallback", contextDescription);
                toastLog(TAG, context, "\"Unable to setInputVideoResolution as " + contextDescription);
            }
        });

        // get input video resolution to check the result
        skylinkConnection.getInputVideoResolution(videoId, new SkylinkCallback.InputVideoResolution() {
            @Override
            public void onObtainInputVideoResolution(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
                obtainInputVideoResolution(width, height, fps, captureFormat, mediaType);
            }
        });
    }

    /**
     * Get the name of the current camera being used.
     * If no camera or if a custom VideoCapturer is being used, return null.
     *
     * @return
     */
    public String getCurrentCameraName() {

        if (skylinkConnection != null) {
            return skylinkConnection.getCurrentCameraName();
        }
        return null;
    }

    /**
     * Get the current {@link SkylinkConfig.VideoDevice VideoDevice} being used.
     * If none are active, return null.
     *
     * @return
     */
    public SkylinkConfig.VideoDevice getCurrentVideoDevice() {
        if (skylinkConnection != null) {
            return skylinkConnection.getCurrentVideoDevice();
        }
        return null;
    }

    /**
     * Get the possible capture format(s) of the specified camera device in an array.
     * Return null if current {@link SkylinkConfig.VideoDevice VideoDevice} is not a defined camera,
     * or if it was not possible to get the capture formats.
     *
     * @param videoDevice Use null to specific the current VideoDevice.
     * @return
     */
    public SkylinkCaptureFormat[] getCaptureFormats(SkylinkConfig.VideoDevice
                                                            videoDevice) {
        if (skylinkConnection != null) {
            return skylinkConnection.getAllCaptureFormats(videoDevice);
        }

        return null;
    }

    /**
     * Return the info of the SkylinkCaptureFormat that is currently being used by the camera.
     * Note that the current CaptureFormat may change whenever the
     * video resolution dimensions change.
     *
     * @return null if there is no CaptureFormat in use now, e.g. if video is not capturing.
     */
    public String getCaptureFormatsString(SkylinkCaptureFormat[] captureFormats) {
        String strFormat = "No CaptureFormat currently registered.";
        String strFormats = "No CaptureFormats currently registered.";

        if (Utils.isCaptureFormatsValid(captureFormats)) {
            strFormats = Utils.captureFormatsToString(captureFormats);
        }

        // Get the current CaptureFormat, if there is one.
        String captureFormatString = null;
        if (skylinkConnection != null) {
            SkylinkCaptureFormat captureFormat = skylinkConnection.getCurrentCaptureFormat();


            if (captureFormat != null) {
                strFormat = captureFormat.toString();
            }

            captureFormatString = "Current capture format: " + strFormat + ".\r\n" +
                    "Supported capture formats: " + strFormats + ".";
        }
        return captureFormatString;
    }

    /**
     * Get the input/sent/received video resolution of a specified peer with specific media type (video or screen)
     * Note:
     * - Resolution may not always be available, e.g. if no video is captured.
     * - If resolution are available, they will be returned in the below callback as the process is asynchronous
     * {@link SkylinkCallback.InputVideoResolution#onObtainInputVideoResolution(int, int, int, SkylinkCaptureFormat)} for input video resolution
     * {@link SkylinkCallback.SentVideoResolution#onObtainSentVideoResolution(int, int, int)} for sent video resolution
     * {@link SkylinkCallback.ReceivedVideoResolution#onObtainReceivedVideoResolution(int, int, int)} for received video resolution
     *
     * @param mediaType the type of the video (video or screen)
     * @param peerIndex index of the remote Peer from whom we want to get sent or received video resolution.
     */
    public void getVideoResolutions(SkylinkMedia.MediaType mediaType,
                                    int peerIndex) {
        if (skylinkConnection == null)
            return;

        String videoId = getProperLocalMediaId(mediaType);
        String remotePeerId = null;

        if (mPeersList != null && mPeersList.size() > 1) {
            remotePeerId = mPeersList.get(peerIndex).getPeerId();
        }

        // if local video has not been created, get received video resolution from remote media
        if (videoId == null) {
            obtainInputVideoResolution(-1, -1, -1, null, mediaType);

            if (remotePeerId != null) {
                obtainSentVideoResolution(-1, -1, -1, mediaType, remotePeerId);

                // get received video res from remote peer
                List<SkylinkMedia> remoteMediaList = skylinkConnection.getSkylinkMediaList(mediaType, remotePeerId);
                if (remoteMediaList == null || remoteMediaList.size() == 0) {
                    obtainReceivedVideoResolution(-1, -1, -1, mediaType, remotePeerId);
                    return;
                }

                SkylinkMedia remoteMedia = remoteMediaList.get(0);

                if (remoteMedia != null && remoteMedia.getMediaState() != SkylinkMedia.MediaState.UNAVAILABLE) {
                    String mediaId = remoteMedia.getMediaId();
                    String finalRemotePeerId2 = remotePeerId;
                    skylinkConnection.getReceivedVideoResolution(mediaId, new SkylinkCallback.ReceivedVideoResolution() {
                        @Override
                        public void onError(SkylinkError error, HashMap<String, Object> details) {
                            String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                            Log.e("SkylinkCallback", contextDescription);
                            obtainReceivedVideoResolution(-1, -1, -1, mediaType, finalRemotePeerId2);
                        }

                        @Override
                        public void onObtainReceivedVideoResolution(int width, int height, int fps) {
                            obtainReceivedVideoResolution(width, height, fps, mediaType, finalRemotePeerId2);
                        }
                    });
                } else {
                    obtainReceivedVideoResolution(-1, -1, -1, mediaType, remotePeerId);
                }
            }
        } else {
            // get input video res
            skylinkConnection.getInputVideoResolution(videoId, new SkylinkCallback.InputVideoResolution() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    obtainInputVideoResolution(-1, -1, -1, null, mediaType);
                }

                @Override
                public void onObtainInputVideoResolution(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
                    obtainInputVideoResolution(width, height, fps, captureFormat, mediaType);
                }
            });

            // get sent and received video res if there is any remote peer in the room
            if (remotePeerId != null) {
                // get sent video res to remote peer
                String finalRemotePeerId1 = remotePeerId;
                skylinkConnection.getSentVideoResolution(remotePeerId, videoId, new SkylinkCallback.SentVideoResolution() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        obtainSentVideoResolution(-1, -1, -1, mediaType, finalRemotePeerId1);
                    }

                    @Override
                    public void onObtainSentVideoResolution(int width, int height, int fps) {
                        obtainSentVideoResolution(width, height, fps, mediaType, finalRemotePeerId1);
                    }
                });

                // get received video res from remote peer
                List<SkylinkMedia> remoteMediaList = skylinkConnection.getSkylinkMediaList(mediaType, remotePeerId);
                if (remoteMediaList == null || remoteMediaList.size() == 0) {
                    obtainReceivedVideoResolution(-1, -1, -1, mediaType, remotePeerId);
                    return;
                }

                SkylinkMedia remoteMedia = remoteMediaList.get(0);
                if (remoteMedia != null && remoteMedia.getMediaState() != SkylinkMedia.MediaState.UNAVAILABLE) {
                    String mediaId = remoteMedia.getMediaId();
                    String finalRemotePeerId = remotePeerId;
                    skylinkConnection.getReceivedVideoResolution(mediaId, new SkylinkCallback.ReceivedVideoResolution() {
                        @Override
                        public void onError(SkylinkError error, HashMap<String, Object> details) {
                            String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                            Log.e("SkylinkCallback", contextDescription);
                            obtainReceivedVideoResolution(-1, -1, -1, mediaType, finalRemotePeerId);
                        }

                        @Override
                        public void onObtainReceivedVideoResolution(int width, int height, int fps) {
                            obtainReceivedVideoResolution(width, height, fps, mediaType, finalRemotePeerId);
                        }
                    });
                } else {
                    obtainReceivedVideoResolution(-1, -1, -1, mediaType, remotePeerId);
                }
            }
        }
    }

    /**
     * This is triggered from SkylinkSDK when a local camera starts to capture video,
     * if {@link SkylinkConfig#isReportVideoResolutionOnVideoChange()} is true;
     * or when its currently reporting and video resolution is not yet stable,
     * if {@link SkylinkConfig#isReportVideoResolutionUntilStable()} is true;
     * Note: This might be different from the resolution of the video actually sent to Peers as
     * WebRTC may adjust resolution dynamically to try to match its bandwidth criteria.
     * If a particular value is not available, , e.g. if video is not capturing,
     * a negative integer or a null value will be provided.
     *
     * @param width         Video width in pixels of local captured video.
     * @param height        Video height in pixels of local captured video.
     * @param fps           Video frame per second of local captured video.
     * @param captureFormat The SkylinkCaptureFormat that is currently used by the camera.
     * @param mediaType     type of the local media object
     */
    protected void obtainInputVideoResolution(int width, int height,
                                              int fps, SkylinkCaptureFormat captureFormat, SkylinkMedia.MediaType mediaType) {
        // if the current demo has video resolution fragment to reflect the change
        if (videoResPresenter != null)
            videoResPresenter.processInputVideoResolutionObtained(mediaType, width, height, fps, captureFormat);
        else {
            String log = "[SA][obtainInputVideoResolution] The local video (" + mediaType.toString() + ") input has width x height, fps: " +
                    width + " x " + height + ", " + fps + " fps.\r\n";
            toastLogLong(TAG, context, log);
        }
    }

    /**
     * This is triggered from SkylinkSDK when we receive a remote Peer's video,
     * if {@link SkylinkConfig#isReportVideoResolutionOnVideoChange()} is true;
     * or when its currently reporting and video resolution is not yet stable,
     * if {@link SkylinkConfig#isReportVideoResolutionUntilStable()} is true.
     * The current resolution of the video received from a specific Peer will be provided.
     * If a particular value is not available, a negative integer will be provided.
     *
     * @param width        Video width in pixels of remote Peer's video received by us.
     * @param height       Video height in pixels of remote Peer's video received by us.
     * @param fps          Video frame per second of remote Peer's video received by us.
     * @param mediaType    type of the remote video that video resolution comes from
     * @param remotePeer PeerId of the remote Peer whose Video Resolution we are obtaining.
     */
    protected void obtainReceivedVideoResolution(int width, int height,
                                                 int fps, SkylinkMedia.MediaType mediaType, String remotePeer) {
        // if the current demo has video resolution fragment to reflect the change
        if (videoResPresenter != null && mediaType != null)
            videoResPresenter.processReceivedVideoResolutionObtained(remotePeer, mediaType, width, height, fps);
        else {
            String log = "[SA][obtainReceivedVideoResolution] The video (" + mediaType + ") received from Peer " + remotePeer +
                    " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
            toastLogLong(TAG, context, log);
        }
    }

    /**
     * This is triggered from SkylinkSDK when a local video stream starts being sent to a remote Peer,
     * if {@link SkylinkConfig#isReportVideoResolutionOnVideoChange()} is true;
     * or when its currently reporting and video resolution is not yet stable,
     * if {@link SkylinkConfig#isReportVideoResolutionUntilStable()} is true;
     * The current resolution of the video being sent to a specific Peer will be provided.
     *
     * @param width        Video width in pixels of local video sent to remote Peer.
     * @param height       Video height in pixels of local video sent to remote Peer.
     * @param fps          Video frame per second of local video sent to remote Peer.
     * @param mediaType    type of the video that video resolution comes from
     * @param remotePeerId PeerId of the remote Peer whom we are obtaining Video Resolution
     *                     for the video we are sending.
     */
    protected void obtainSentVideoResolution(int width, int height,
                                             int fps, SkylinkMedia.MediaType mediaType, String remotePeerId) {
        // if the current demo has video resolution fragment to reflect the change
        if (videoResPresenter != null)
            videoResPresenter.processSentVideoResolutionObtained(remotePeerId, mediaType, width, height, fps);
        else {
            String log = "[SA][obtainSentVideoResolution] The video (" + mediaType.toString() + ") sent to Peer " + remotePeerId +
                    " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
            toastLogLong(TAG, context, log);
        }
    }

    public void lockRoom() {
        if (skylinkConnection != null) {
            skylinkConnection.lockRoom(new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to lockRoom as " + contextDescription);
                }
            });
        }
    }

    public void unlockRoom() {
        if (skylinkConnection != null) {
            skylinkConnection.unlockRoom(new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to unlockRoom as " + contextDescription);
                }
            });
        }
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

        if (skylinkConnection != null) {
            UserInfo userInfo = skylinkConnection.getUserInfo(peerId);
            Object userDataObject = null;

            if (userInfo != null) {
                userDataObject = userInfo.getUserData();
            }
            if (userDataObject != null) {
                userDataString = userDataObject.toString();
            }
        }
        return userDataString;
    }

    private String getUserDataString(UserInfo userInfo) {
        if (userInfo == null && skylinkConnection != null) {
            userInfo = skylinkConnection.getUserInfo(null);
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
        if (skylinkConnection != null) {
            return skylinkConnection.getUserInfo(userId);
        }
        return null;
    }

    /**
     * Get proper video id for input to SDK to get video resolution or video stats
     */
    protected String getProperLocalMediaId(SkylinkMedia.MediaType mediaType) {
        String mediaId = null;

        // default main video id from current local video
        if (localVideo != null && (mediaType == SkylinkMedia.MediaType.VIDEO || mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA || mediaType == null)) {
            mediaId = localVideo.getMediaId();
        } else if (localScreen != null && mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            mediaId = localScreen.getMediaId();
        }

        return mediaId;
    }

    /**
     * Get proper video id for input to SDK to get video resolution or video stats
     */
    protected String getProperRemoteMediaId(String
                                                    remotePeerId, SkylinkMedia.MediaType mediaType, boolean isLocal) {

        // TODO @Muoi need to update when SDK finished get stats by specific media track
        // currently the SDK is just able to get full stats for receiving track, no all mediaTypes or
        // media tracks will get the same stats

        if (isLocal) {
            return getProperLocalMediaId(mediaType);
        } else {
            return skylinkConnection.getSkylinkMediaList(mediaType, remotePeerId).get(0).getMediaId();
        }
    }

    protected void clearInstance() {
//        if (skylinkConnection == null)
//            return;

        //remove all peers in room
        if (mPeersList != null)
            mPeersList.clear();

        // reset class variables instance
        skylinkConnection.clearInstance();
        localAudio = null;
        localVideo = null;
        localScreen = null;
//        skylinkConnectionManager.setSkylinkConnection(null);

//        skylinkConnection = null;
    }

    private void disconnectRemotePeer(String remotePeerId) {
        //remove remote peer in the mPeersList and remove the peer index in view
        int removeIndex = -1;
        SkylinkPeer removedPeer = null;

        // re-fill all peers, except local peer
        for (int i = 1; i < mPeersList.size(); i++) {
            if (mPeersList.get(i).getPeerId().equals(remotePeerId)) {
                removedPeer = mPeersList.get(i);
                mPeersList.remove(i);
                removeIndex = i;
                break;
            }
        }

        //update UI when remote peer left the room
        if (removedPeer != null && removeIndex != -1)
            presenter.processRemotePeerDisconnected(removedPeer, removeIndex - 1);
    }
}
