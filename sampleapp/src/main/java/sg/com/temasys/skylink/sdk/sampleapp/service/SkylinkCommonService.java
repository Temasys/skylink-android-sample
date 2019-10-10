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
import java.util.Map;

import sg.com.temasys.skylink.sdk.listener.DataTransferListener;
import sg.com.temasys.skylink.sdk.listener.FileTransferListener;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.MessagesListener;
import sg.com.temasys.skylink.sdk.listener.OsListener;
import sg.com.temasys.skylink.sdk.listener.RecordingListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.Info;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkError;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
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
    // this variable need to be static for configuration change
    protected static SkylinkConnection mSkylinkConnection;

    //list of peers joined in room
    //this variable need to be static for configuration change
    protected static List<SkylinkPeer> mPeersList;

    //room name and user name for each demo/function
    protected String roomName;
    protected String userName;

    protected String localAudioId, localVideoId, localScreenSharingId, localMainVideoId;


    public SkylinkCommonService(Context context) {
        this.context = context;
        this.skylinkConnectionManager = new SkylinkConnectionManager(this, context);
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
    public void onConnectToRoomSucessful() {
        String log = "[onConnect] ";
        String localPeerId = mSkylinkConnection.getLocalPeerId();

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

        if (localAudioId != null) {
            selfPeer.addMediaId(localAudioId, SkylinkMedia.MediaType.AUDIO_MIC);
        }

        if (localVideoId != null) {
            selfPeer.addMediaId(localVideoId, SkylinkMedia.MediaType.VIDEO_CAMERA);
        }

        if (localScreenSharingId != null) {
            selfPeer.addMediaId(localVideoId, SkylinkMedia.MediaType.VIDEO_SCREEN);
        }

        mPeersList.add(selfPeer);

        //update UI to connected state
        presenter.onServiceRequestConnect(true);
    }

    @Override
    public void onConnectToRoomFailed(String errorMessage) {
        presenter.onServiceRequestConnect(false);
    }

    /**
     * This method is triggered from SkylinkSDK to inform that user is disconnected from room successfully or not
     */
    @Override
    public void onDisconnectFromRoom(int errorCode, String message) {
        String log = "[onDisconnect] ";
        if (errorCode == Errors.DISCONNECT_FROM_ROOM) {
            log += "We have successfully disconnected from the room.";
        } else if (errorCode == Errors.DISCONNECT_UNEXPECTED_ERROR) {
            log += "WARNING! We have been unexpectedly disconnected from the room!";
        }
        log += " Server message: " + message;
        toastLogLong(TAG, context, log);

        //update UI to disconnected state for all types call
        presenter.onServiceRequestDisconnect();

        clearInstance();
    }

    /**
     * This is triggered from SkylinkSDK when the lock status of the room that we are in changes
     */
    @Override
    public void onChangeRoomLockStatus(boolean roomLockStatus, String remotePeerId) {
        String log = "[onChangeRoomLockStatus] ";
        log += "[SA] Peer " + remotePeerId + " changed Room locked status to "
                + roomLockStatus + ".";
        toastLog(TAG, context, log);
    }

    /**
     * This is triggered from SkylinkSDK to deliver messages that might be useful to the user.
     */
    @Override
    public void onReceiveInfo(int infoCode, String message) {
        Utils.handleSkylinkReceiveInfo(infoCode, message, context, TAG);
    }

    /**
     * This is triggered from SkylinkSDK to deliver a warning message to the user
     */
    @Override
    public void onReceiveWarning(int infoCode, String message) {
        Utils.handleSkylinkWarningErrorMsg(infoCode, message, context, TAG);
    }

    /**
     * This is triggered from SkylinkSDK to deliver a error message to the user
     */
    @Override
    public void onReceiveError(int errorCode, String message) {
        Utils.handleSkylinkWarningErrorMsg(errorCode, message, context, TAG);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from MediaListener need to be implemented for audio and video functions
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered from SkylinkSDK when successfully captures the local device's media input.
     * This may be at the start of media usage or when media source has been changed.
     * It can happen only if the connection is configured to have a media configuration.
     *
     * @param localMedia the local media object
     */
    @Override
    public void onCreateLocalMedia(SkylinkMedia localMedia) {
        Log.d(TAG, "[onCreateLocalMedia]");

        if (localMedia == null)
            return;

        if (!localMedia.isVideo()) {
            presenter.onServiceRequestLocalAudioCapture(localMedia);
            localAudioId = localMedia.getMediaId();
        } else {
            if (SkylinkMedia.MediaType.VIDEO_CAMERA == localMedia.getMediaType()) {
                presenter.onServiceRequestLocalCameraCapture(localMedia);
                localVideoId = localMedia.getMediaId();
            } else if (SkylinkMedia.MediaType.VIDEO_SCREEN == localMedia.getMediaType()) {
                presenter.onServiceRequestLocalScreenCapture(localMedia);
                localScreenSharingId = localMedia.getMediaId();
            }
        }
    }

    /**
     * This is triggered from SkylinkSDK when a local media has been changed it state
     *
     * @param localMedia The media object with new state
     */
    @Override
    public void onChangeLocalMedia(SkylinkMedia localMedia) {
        String log = "[SA][onChangeLocalMedia] ";
        log += "Local media (" + localMedia.getMediaId() + ") state changed status via:\r\nCallback: " + localMedia.getMediaState() + ".";

        toastLog(TAG, context, log);


        presenter.onServiceRequestMediaStateChange(localMedia, true);

        // process remove local media id for the self peer if media state is UNAVAILABLE
        if (localMedia.getMediaState() == SkylinkMedia.MediaState.UNAVAILABLE) {
            if (mPeersList != null && mPeersList.size() > 0) {
                for (int i = 0; i < mPeersList.size(); i++) {
                    mPeersList.get(i).removeMediaId(localMedia.getMediaId());
                }
            }

            // remove local media id
            if (localMedia.getMediaType() == SkylinkMedia.MediaType.AUDIO_MIC) {
                localAudioId = null;
            } else if (localMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_CAMERA) {
                localVideoId = null;
            } else if (localMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_SCREEN) {
                localScreenSharingId = null;
            }
        }
    }

    /**
     * This is triggered from SkylinkSDK when a remote peer has changed his media state.
     *
     * @param remoteMedia  The remote media object with new value like state
     * @param remotePeerId The id of the peer (can be selfPeer or remote peer)
     */
    @Override
    public void onChangeRemoteMedia(SkylinkMedia remoteMedia, String remotePeerId) {
        String log = "[SA][onChangeRemoteMedia] ";
        log += "Peer " + getPeerIdNick(remotePeerId) +
                " Media (" + remoteMedia.getMediaId() + ") state changed status via:\r\nCallback: " + remoteMedia.getMediaState() + ".";

        toastLog(TAG, context, log);

        presenter.onServiceRequestMediaStateChange(remoteMedia, false);
    }

    /**
     * This is triggered from SkylinkSDK upon receiving the media object of the remote peer
     * if the connection is configured to have media configuration.
     * This will only be triggered after onConnectWithRemotePeer has triggered.
     *
     * @param remoteMedia  the remote media object
     * @param remotePeerId The id of the peer
     */
    @Override
    public void onReceiveRemoteMedia(SkylinkMedia remoteMedia, String remotePeerId) {
        String log = "[onReceiveRemoteMedia] ";
        log += "Received new remote media (" + remoteMedia.getMediaId() + ") from Peer " + getPeerIdNick(remotePeerId) + ".\r\n";

        UserInfo remotePeerUserInfo = getUserInfo(remotePeerId);

        if (!remoteMedia.isVideo()) {
            // add audio id for the peer
            addPeerMedia(remotePeerId, remoteMedia.getMediaId(), SkylinkMedia.MediaType.AUDIO_MIC);
            presenter.onServiceRequestRemotePeerAudioReceive(log, remotePeerUserInfo, remotePeerId, remoteMedia);
        } else {
            if (remoteMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_CAMERA || remoteMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO) {
                addPeerMedia(remotePeerId, remoteMedia.getMediaId(), SkylinkMedia.MediaType.VIDEO_CAMERA);
            } else if (remoteMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_SCREEN) {
                addPeerMedia(remotePeerId, remoteMedia.getMediaId(), SkylinkMedia.MediaType.VIDEO_SCREEN);
            }

            presenter.onServiceRequestRemotePeerVideoReceive(log, remotePeerId, remoteMedia);
        }
    }

    @Override
    public void onObtainInputVideoResolution(int width, int height, int fps, SkylinkCaptureFormat captureFormat, String mediaId) {
        obtainInputVideoResolution(width, height, fps, captureFormat, mediaId);
    }

    @Override
    public void onObtainReceivedVideoResolution(int width, int height, int fps, String mediaId, String remotePeerId) {
        obtainReceivedVideoResolution(width, height, fps, mediaId, remotePeerId);
    }

    @Override
    public void onObtainSentVideoResolution(int width, int height, int fps, String mediaId, String remotePeerId) {
        obtainSentVideoResolution(width, height, fps, mediaId, remotePeerId);
    }

    /**
     * This is triggered from SkylinkSDK when any of the given video streams' frame size changes. It includes the
     * self stream also.
     *
     * @param remotePeerId The id of the peer. If null, it indicates self stream.
     * @param media        The SkylinkMedia video object
     * @param size         Size of the video frame
     */
    @Override
    public void onChangeVideoSize(Point size, SkylinkMedia media, String remotePeerId) {
        Log.d(TAG, "[onChangeVideoSize]");

        presenter.onServiceRequestVideoSizeChange(remotePeerId, media.getMediaId(), size);
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
     * {@link #onGrantPermission} or {@link #onDenyPermission}.
     *
     * @param intent      An Intent to be passed to
     *                    {@link android.app.Activity#startActivityForResult(Intent, int)}.
     * @param requestCode An unique integer that represents this permission request.
     *                    This is to be used as the requestCode when calling Android's
     *                    {@link android.app.Activity#startActivityForResult(Intent, int)}.
     * @param infoCode    {@link Info} Integer that explains why this permission is required.
     *                    Further info can be obtained by calling
     *                    {@link Info#getInfoString Info.getInfoString(infoCode)}.
     *                    Based on this info, an explanation might be crafted
     *                    to educate the user on why this permission is required.
     */
    public void onRequirePermission(Intent intent, int requestCode, int infoCode) {
        Log.d(TAG, "[onIntentRequired]");
        presenter.onServiceRequestIntentRequired(intent, requestCode, infoCode);
    }

    /**
     * This is triggered when Android Intent based permission has been granted for the usage of
     * a particular part of the SDK.
     *
     * @param intent      As that in {@link #onRequirePermission(Intent, int, int)}.
     * @param requestCode As that in {@link #onRequirePermission(Intent, int, int)}.
     * @param infoCode    As that in {@link #onRequirePermission(Intent, int, int)}.
     */
    public void onGrantPermission(Intent intent, int requestCode, int infoCode) {
        Log.d(TAG, "[onIntentPermissionGranted]");
        presenter.onServiceRequestPermissionGranted(intent, requestCode, infoCode);
    }

    /**
     * This is triggered when Android Intent based permission has been denied for the usage of
     * a particular part of the SDK.
     *
     * @param intent      As that in {@link #onRequirePermission(Intent, int, int)}.
     * @param requestCode As that in {@link #onRequirePermission(Intent, int, int)}.
     * @param infoCode    As that in {@link #onRequirePermission(Intent, int, int)}.
     */
    public void onDenyPermission(Intent intent, int requestCode, int infoCode) {
        Log.d(TAG, "[onIntentPermissionDenied]");
        presenter.onServiceRequestPermissionDenied(intent, requestCode, infoCode);
    }

    /**
     * This is triggered from SkylinkSDK when Android Runtime permission is required to use
     * devices to perform media functions like audio, camera, file transfer,...
     * The SDK will process this result and also notify app via
     * {@link #onGrantPermission} or {@link #onDenyPermission}.
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
    public void onRequirePermission(String[] permissions, int requestCode, int infoCode) {
        Log.d(TAG, "[onRequirePermission]");
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
    public void onGrantPermission(String[] permissions, int requestCode, int infoCode) {
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
    public void onDenyPermission(String[] permissions, int requestCode, int infoCode) {
        Log.d(TAG, "[onPermissionDenied]");
        PermRequesterInfo info = new PermRequesterInfo(permissions, requestCode, infoCode);

        presenter.onServiceRequestPermissionDenied(context, info);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from RemotePeerListener need to be implemented for all functions
    //----------------------------------------------------------------------------------------------

    @Override
    public void onReceiveRemotePeerJoinRoom(String remotePeerId, UserInfo userInfo) {

    }

    /**
     * This is triggered when a remote peer joins the room.
     * It will be triggered before onRemotePeerMediaReceive is triggered
     *
     * @param remotePeerId   The id of the peer
     * @param userInfo       Info about the remote user, including userData. UserData May be a
     *                       'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param hasDataChannel Whether peer has enabled DataChannel.
     */
    @Override
    public void onConnectWithRemotePeer(String remotePeerId, UserInfo userInfo, boolean hasDataChannel) {
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

        //add remote peer into list
        SkylinkPeer remotePeer = new SkylinkPeer(remotePeerId, remotePeerName);
        mPeersList.add(remotePeer);

        presenter.onServiceRequestRemotePeerJoin(remotePeer);

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
    public void onRefreshRemotePeerConnection(String remotePeerId, UserInfo userInfo, boolean hasDataChannel, boolean isIceRestarted) {
        Log.d(TAG, "[onRefreshRemotePeerConnection]");
        String peer = "Skylink Media Relay server";
        if (remotePeerId != null) {
            peer = "Peer " + getPeerIdNick(remotePeerId);
        }
        String log = "Your connection with " + peer + " has just been refreshed";
        if (isIceRestarted) {
            log += ", with ICE restarted.\r\n";
        } else {
            log += ".\r\n";
        }

        UserInfo remotePeerUserInfo = getUserInfo(remotePeerId);

        presenter.onServiceRequestRemotePeerConnectionRefreshed(log, remotePeerUserInfo);
    }

    /**
     * This is triggered when an update is received in the user defined data of a remote peer.
     *
     * @param userData     User defined data relating to the remote peer. May be a
     *                     'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param remotePeerId The id of the peer.
     */
    @Override
    public void onReceiveRemotePeerUserData(Object userData, String remotePeerId) {
        // If Peer has no userData, use an empty string for nick.
        String nick = "";
        if (userData != null) {
            nick = userData.toString();
        }
        String log = "[SA][onReceiveRemotePeerUserData] Peer " + getPeerIdNick(remotePeerId) +
                ":\n" + nick;
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
        Log.d(TAG, "[SA][onOpenRemotePeerDataConnection] " + remotePeerId + ".");
    }

    @Override
    public void onDisconnectWithRemotePeer(String remotePeerId, UserInfo userInfo, boolean hasDataChannel) {

    }

    /**
     * This is triggered when a remote peer leaves the room.
     *
     * @param remotePeerId The id of the remote peer
     * @param infoCode     The info code specifying the possible reason for leaving the room.
     * @param userInfo     UserInfo of this remote Peer.
     */
    @Override
    public void onReceiveRemotePeerLeaveRoom(String remotePeerId, int infoCode, UserInfo userInfo) {
        Log.d(TAG, "[onReceiveRemotePeerLeaveRoom]");

        //remove remote peer and keep the index of remote peer for multi party video call function
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

        //update UI when remote peer lelf the room
        presenter.onServiceRequestRemotePeerLeave(removedPeer, removeIndex - 1);

        int numRemotePeers = mPeersList.size() - 1;
        if (numRemotePeers >= 0) {
            String log = "Your Peer " + getUserDataString(userInfo) + " ( " + remotePeerId + " ) left: " +
                    Info.getInfoString(infoCode) + ". " + numRemotePeers + " remote Peer(s) left in the room.";
            toastLog(TAG, context, log);
        }
    }

    /**
     * This is triggered when having fail connection with a remote peer
     * When user get this callback, they can reconnect or refresh the connection with the remote peer,
     * or simply ignore this peer
     *
     * @param remotePeerId The id of the peer
     * @param errorCode    the error code for fail connection
     */
    @Override
    public void onErrorForRemotePeerConnection(int errorCode, String remotePeerId) {
        String log = "Your connection with the peer (" + remotePeerId + ") is NOT successful. " +
                "ErrorCode: " + errorCode + ", Reason: " + Errors.getErrorString(errorCode);
        toastLog(TAG, context, log);
        Log.e(TAG, log);
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
        Log.d(TAG, "[onReceiveData]");

        presenter.onServiceRequestDataReceive(context, remotePeerId, data);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from FileTransferListener need to be implemented for fileTransfer function
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered upon receiving a file transfer request from a peer.
     *
     * @param remotePeerId The id of the peer
     * @param fileName     The name of the file
     * @param isPublic     Flag to specify if file share was directed to only us (as opposed to all
     *                     peers in the room).
     */
    @Override
    public void onReceiveFileTransferRequest(String fileName, boolean isPublic, String remotePeerId) {
        Log.d(TAG, "[onReceiveFileTransferRequest]");

        presenter.onServiceRequestFileTransferPermissionRequest(remotePeerId, fileName, !isPublic);
    }

    /**
     * This is triggered upon receiving the response of a peer to our request for file transfer with
     * the peer.
     *
     * @param remotePeerId The id of the peer
     * @param fileName     The name of the file
     * @param wasAccepted  Flag to specify whether the peer has accepted the request
     */
    @Override
    public void onReceiveFileTransferResponse(boolean wasAccepted, String fileName, String remotePeerId) {
        Log.d(TAG, "[onReceiveFileTransferResponse]");

        presenter.onServiceRequestFileTransferPermissionResponse(context, remotePeerId, fileName, wasAccepted);
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
    public void onDropFileTransfer(String fileName, String message, boolean isExplicit, String remotePeerId) {
        Log.d(TAG, "[onDropFileTransfer]");

        presenter.onServiceRequestFileTransferDrop(context, remotePeerId, fileName, message, isExplicit);
    }

    /**
     * This is triggered when we have sent a file successfully.
     *
     * @param remotePeerId The id of the peer receiving the file.
     * @param fileName     The name of the file
     */
    @Override
    public void onCompleteFileTransferSending(String fileName, String remotePeerId) {
        Log.d(TAG, "[onCompleteFileTransferSending]");

        presenter.onServiceRequestFileSendComplete(context, remotePeerId, fileName);
    }

    /**
     * This is triggered when we have received a file successfully.
     *
     * @param remotePeerId The id of the peer
     * @param fileName     The name of the file
     */
    @Override
    public void onCompleteFileTransferReceiving(String fileName, String remotePeerId) {
        Log.d(TAG, "[onCompleteFileTransferReceiving]");

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
    public void onUpdateFileTransferSendingProgress(double percentage, String fileName, String remotePeerId) {
        Log.d(TAG, "[onUpdateFileTransferSendingProgress]");

        presenter.onServiceRequestFileSendProgress(context, remotePeerId, fileName, percentage);
    }

    /**
     * This is triggered timely to report the on going progress when receiving a file
     *
     * @param remotePeerId The id of the peer sending the file.
     * @param fileName     The name of the file
     * @param percentage   The percentage completed
     */
    @Override
    public void onUpdateFileTransferReceivingProgress(double percentage, String fileName, String remotePeerId) {
        Log.d(TAG, "[onUpdateFileTransferReceivingProgress]");

        presenter.onServiceRequestFileReceiveProgress(context, remotePeerId, fileName, percentage);
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
     * @param remotePeerId The id of the remote peer
     */
    @Override
    public void onReceiveServerMessage(Object message, boolean isPublic, String remotePeerId) {
        Log.d(TAG, "[onReceiveServerMessage]");

        presenter.onServiceRequestServerMessageReceive(remotePeerId, message, !isPublic);
    }

    /**
     * This is triggered when a broadcast or private message is received directly from the peer,
     * i.e. in a peer to peer manner.
     *
     * @param message      User defined message. May be a 'java.lang.String', 'org.json.JSONObject'
     *                     or 'org.json.JSONArray'.
     * @param isPublic     Flag to specify whether the message was sent to us only (as opposed to
     *                     broadcast to all peers in the room).
     * @param remotePeerId The id of the remote peer
     */
    @Override
    public void onReceiveP2PMessage(Object message, boolean isPublic, String remotePeerId) {
        Log.d(TAG, "[onReceiveP2PMessage]");

        presenter.onServiceRequestP2PMessageReceive(remotePeerId, message, !isPublic);
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
        Log.d(TAG, "[onStartRecording]");

        presenter.onServiceRequestRecordingStart(context, mSkylinkConnection.isRecording());
    }

    /**
     * This is triggered to indicate that recording has stopped for this room.
     *
     * @param recordingId Id of Recording session this message is about.
     */
    @Override
    public void onStopRecording(String recordingId) {
        Log.d(TAG, "[onStopRecording]");

        presenter.onServiceRequestRecordingStop(context, mSkylinkConnection.isRecording());
    }

    /**
     * This is triggered when a recording error occurs.
     *
     * @param errorCode   {@link sg.com.temasys.skylink.sdk.rtc.Errors Errors} Integer error code of the recording error encountered.
     * @param description Description of the recording error encountered.
     * @param recordingId Id of Recording session this message is about.
     */
    @Override
    public void onReceiveRecordingError(int errorCode, String description, String recordingId) {
        Log.d(TAG, "[onReceiveRecordingError]");

        presenter.onServiceRequestRecordingError(context, recordingId, errorCode, description);
    }

    //----------------------------------------------------------------------------------------------
    // Public methods helps to work with SkylinkSDK
    //----------------------------------------------------------------------------------------------
    protected void initializeSkylinkConnection(Constants.CONFIG_TYPE typeCall) {
        this.mSkylinkConnection = skylinkConnectionManager.initializeSkylinkConnection(typeCall);

        // enable debugging log
        if (this.mSkylinkConnection != null) {
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


    public boolean disconnectFromRoom() {
        return skylinkConnectionManager.disconnectFromRoom();
    }

    //static method for the other to access current instance of SkylinkConnection
    public static SkylinkConnection getCurrentSkylinkConnection() {
        return mSkylinkConnection;
    }

    /**
     * To enable logs from Skylink SDK (e.g. during debugging):
     * Should not enable logs for production apps!
     */
    protected void setEnableLogsForDebugging(boolean enableLogsForDebugging) {
        this.mSkylinkConnection.setEnableLogs(enableLogsForDebugging);
    }

    /**
     * Get peerId of a Peer by its index using SkylinkConnection API.
     * We can also get a specific peer by mPeersList
     *
     * @param index 0 for self Peer, 1 onwards for remote Peer(s).
     * @return Desired peerId or null if not available.
     */
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
        if (peerId == null && mSkylinkConnection != null) {
            peerId = mSkylinkConnection.getLocalPeerId();
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
            return mSkylinkConnection.getLocalPeerId();
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

    // Video resolution

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
    public void setInputVideoResolution(SkylinkMedia.MediaType mediaType, int width, int height, int fps) {
        if (mSkylinkConnection == null)
            return;

        // get video id
        String mainVideoId = getProperLocalMediaId(mediaType);

        if (mainVideoId == null)
            return;

        mSkylinkConnection.setInputVideoResolution(mainVideoId, width, height, fps, new SkylinkCallback.InputVideoResolution() {
            @Override
            public void onError(SkylinkError error, String contextDescription) {
                Log.e("SkylinkCallback", contextDescription);
            }

            @Override
            public void onObtainInputVideoResolution(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
                obtainInputVideoResolution(width, height, fps, captureFormat, mainVideoId);
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

        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getCurrentCameraName();
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
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getCurrentVideoDevice();
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
    public SkylinkCaptureFormat[] getCaptureFormats(SkylinkConfig.VideoDevice videoDevice) {
        if (mSkylinkConnection != null) {
            return mSkylinkConnection.getAllCaptureFormats(videoDevice);
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
        if (mSkylinkConnection != null) {
            SkylinkCaptureFormat captureFormat = mSkylinkConnection.getCurrentCaptureFormat();


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
     * - If resolution are available, they will be returned in
     * {@link SkylinkCommonService#onObtainInputVideoResolution} for input video resolution
     * {@link SkylinkCommonService#onObtainReceivedVideoResolution} for received video resolution
     * {@link SkylinkCommonService#onObtainSentVideoResolution} for sent video resolution
     *
     * @param mediaType the type of the video (video or screen)
     * @param peerIndex index of the remote Peer from whom we want to get sent or received video resolution.
     */
    public void getVideoResolutions(SkylinkMedia.MediaType mediaType, int peerIndex) {
        if (mSkylinkConnection == null)
            return;

        String mainVideoId = getProperLocalMediaId(mediaType);

        if (mainVideoId == null)
            return;

        // get input video res
        mSkylinkConnection.getInputVideoResolution(mainVideoId, new SkylinkCallback.InputVideoResolution() {
            @Override
            public void onError(SkylinkError error, String contextDescription) {
                Log.e("SkylinkCallback", contextDescription);
            }

            @Override
            public void onObtainInputVideoResolution(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
                obtainInputVideoResolution(width, height, fps, captureFormat, mainVideoId);
            }
        });

        // get sent and received video res if there is any remote peer in the room
        if (mPeersList != null && mPeersList.size() > 1) {
            String remotePeerId = mPeersList.get(peerIndex).getPeerId();
            if (remotePeerId != null) {
                // get sent video res to remote peer
                mSkylinkConnection.getSentVideoResolution(remotePeerId, mainVideoId, new SkylinkCallback.SentVideoResolution() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                    }

                    @Override
                    public void onObtainSentVideoResolution(int width, int height, int fps) {
                        obtainSentVideoResolution(width, height, fps, mainVideoId, remotePeerId);
                    }
                });
            }

            if (remotePeerId != null) {
                // get received video res from remote peer
                List<SkylinkMedia> remoteMediaList = mSkylinkConnection.getSkylinkMediaList(mediaType, remotePeerId);
                if (remoteMediaList == null || remoteMediaList.size() == 0) {
                    return;
                }

                SkylinkMedia remoteMedia = remoteMediaList.get(0);

                if (remoteMedia != null && remoteMedia.getMediaState() != SkylinkMedia.MediaState.UNAVAILABLE) {
                    String mediaId = remoteMedia.getMediaId();
                    mSkylinkConnection.getReceivedVideoResolution(remotePeerId, mediaId, new SkylinkCallback.ReceivedVideoResolution() {
                        @Override
                        public void onError(SkylinkError error, String contextDescription) {
                            Log.e("SkylinkCallback", contextDescription);
                        }

                        @Override
                        public void onObtainReceivedVideoResolution(int width, int height, int fps) {
                            obtainReceivedVideoResolution(width, height, fps, mediaId, remotePeerId);
                        }
                    });
                }
            }
        }
    }

    /**
     * This is triggered from SkylinkSDK when a local camera starts to capture video,
     * if {@link SkylinkConfig#isReportVideoResolutionOnVideoChange()} is true;
     * or when its currently reporting and video resolution is not yet stable,
     * if {@link SkylinkConfig#isReportVideoResolutionUntilStable()} is true;
     * It can also be triggered by calling {@link SkylinkConnection#getInputVideoResolution(String, SkylinkCallback.InputVideoResolution)}.
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
     * @param mediaId       Id of the local media object
     */
    protected void obtainInputVideoResolution(int width, int height, int fps, SkylinkCaptureFormat captureFormat, String mediaId) {
        Log.d(TAG, "[obtainInputVideoResolution]");

        SkylinkMedia.MediaType mediaType = null;
        // get the video type from videoId
        if (mediaId.equals(localVideoId)) {
            mediaType = SkylinkMedia.MediaType.VIDEO_CAMERA;
        } else if (mediaId.equals(localScreenSharingId)) {
            mediaType = SkylinkMedia.MediaType.VIDEO_SCREEN;
        }

        if (videoResPresenter != null)
            videoResPresenter.onServiceRequestInputVideoResolutionObtained(mediaType, width, height, fps, captureFormat);
        else {
            String log = "[SA][obtainInputVideoResolution] The local video (" + mediaType.toString() + ") input has width x height, fps: " +
                    width + " x " + height + ", " + fps + " fps.\r\n";
            Log.d(TAG, log);
            toastLogLong(TAG, context, log);
        }
    }

    /**
     * This is triggered from SkylinkSDK when we receive a remote Peer's video,
     * if {@link SkylinkConfig#isReportVideoResolutionOnVideoChange()} is true;
     * or when its currently reporting and video resolution is not yet stable,
     * if {@link SkylinkConfig#isReportVideoResolutionUntilStable()} is true.
     * It can also be triggered by calling {@link SkylinkConnection#getReceivedVideoResolution}.
     * The current resolution of the video received from a specific Peer will be provided.
     * If a particular value is not available, a negative integer will be provided.
     *
     * @param width        Video width in pixels of remote Peer's video received by us.
     * @param height       Video height in pixels of remote Peer's video received by us.
     * @param fps          Video frame per second of remote Peer's video received by us.
     * @param mediaId      id of the remote video that video resolutionc comes from
     * @param remotePeerId PeerId of the remote Peer whose Video Resolution we are obtaining.
     */
    protected void obtainReceivedVideoResolution(int width, int height, int fps, String mediaId, String remotePeerId) {
        Log.d(TAG, "[obtainReceivedVideoResolution]");

        SkylinkMedia.MediaType mediaType = null;

        for (SkylinkPeer peer : mPeersList) {
            if (peer.getPeerId().equals(remotePeerId)) {
                Map<String, SkylinkMedia.MediaType> remoteMedia = peer.getMediaIds();
                if (remoteMedia != null && remoteMedia.size() > 0) {
                    mediaType = remoteMedia.get(mediaId);
                    break;
                }
            }
        }

        if (videoResPresenter != null && mediaType != null)
            videoResPresenter.onServiceRequestReceivedVideoResolutionObtained(remotePeerId, mediaType, width, height, fps);
        else {
            String log = "[SA][obtainReceivedVideoResolution] The video (" + mediaType.toString() + ") received from Peer " + remotePeerId +
                    " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
            Log.d(TAG, log);
            toastLogLong(TAG, context, log);
        }
    }

    /**
     * This is triggered from SkylinkSDK when a local video stream starts being sent to a remote Peer,
     * if {@link SkylinkConfig#isReportVideoResolutionOnVideoChange()} is true;
     * or when its currently reporting and video resolution is not yet stable,
     * if {@link SkylinkConfig#isReportVideoResolutionUntilStable()} is true;
     * It can also be triggered by calling {@link SkylinkConnection#getSentVideoResolution}.
     * The current resolution of the video being sent to a specific Peer will be provided.
     *
     * @param width        Video width in pixels of local video sent to remote Peer.
     * @param height       Video height in pixels of local video sent to remote Peer.
     * @param fps          Video frame per second of local video sent to remote Peer.
     * @param mediaId      id of the video that video resolution comes from
     * @param remotePeerId PeerId of the remote Peer whom we are obtaining Video Resolution
     *                     for the video we are sending.
     */
    protected void obtainSentVideoResolution(int width, int height, int fps, String mediaId, String remotePeerId) {
        Log.d(TAG, "[obtainSentVideoResolution]");

        SkylinkMedia.MediaType mediaType = null;
        // get the video type from videoId
        if (mediaId.equals(localVideoId)) {
            mediaType = SkylinkMedia.MediaType.VIDEO_CAMERA;
        } else if (mediaId.equals(localScreenSharingId)) {
            mediaType = SkylinkMedia.MediaType.VIDEO_SCREEN;
        }

        if (videoResPresenter != null)
            videoResPresenter.onServiceRequestSentVideoResolutionObtained(remotePeerId, mediaType, width, height, fps);
        else {
            String log = "[SA][obtainSentVideoResolution] The video (" + mediaType.toString() + ") sent to Peer " + remotePeerId +
                    " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
            Log.d(TAG, log);
            toastLogLong(TAG, context, log);
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

        if (mSkylinkConnection != null) {
            UserInfo userInfo = mSkylinkConnection.getUserInfo(peerId);
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

    /**
     * Add received media for the peer
     * 1 peer can have multiple audios/videos and we keep track of their ids in @link{#SkylinkPeer.mediaIds}
     *
     * @param remotePeerId remote peer id that media belongs to
     * @param mediaId      the id of the remote video view
     */
    private void addPeerMedia(String remotePeerId, String mediaId, SkylinkMedia.MediaType mediaType) {
        String logTag = "[SCS][addPeerMedia] ";
        String log = logTag;

        //get the remote peer instance to add media
        SkylinkPeer remotePeer = getPeerById(remotePeerId);

        if (remotePeer == null) {
            log = logTag + "Can not add video view for peer " + remotePeerId;
            log += " cause the peer is not existed";
            toastLog(TAG, context, log);
            return;
        }

        // create a new mediaIds list if it is not existed
        if (remotePeer.getMediaIds() == null) {
            Map<String, SkylinkMedia.MediaType> mediaIds = new HashMap<String, SkylinkMedia.MediaType>();
            remotePeer.setMediaIds(mediaIds);
        }

        remotePeer.getMediaIds().put(mediaId, mediaType);
        log = logTag + "Successfully add media " + mediaId + " for peer" + remotePeerId;
        Log.d(TAG, log);
        toastLog(TAG, context, log);
    }

    /**
     * Get proper video id for input to SDK to get video resolution or video stats
     */
    protected String getProperLocalMediaId(SkylinkMedia.MediaType mediaType) {
        String mainVideoId = null;
        // default main video id from current local video
        if (localVideoId != null) {
            mainVideoId = localVideoId;
        } else if (localScreenSharingId != null) {
            mainVideoId = localVideoId;
        }

        // change main video id if user choose screen
        if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN && localScreenSharingId != null) {
            mainVideoId = localScreenSharingId;
        }

        return mainVideoId;
    }

    /**
     * Get proper video id for input to SDK to get video resolution or video stats
     */
    protected String getProperRemoteMediaId(String remotePeerId, SkylinkMedia.MediaType mediaType, boolean isLocal) {

        // TODO @Muoi need to update when SDK finished get stats by specific media track
        // currently the SDK is just able to get full stats for receiving track, no all mediaTypes or
        // media tracks will get the same stats

        if (isLocal) {
            return getProperLocalMediaId(mediaType);
        } else {
            return mSkylinkConnection.getSkylinkMediaList(mediaType, remotePeerId).get(0).getMediaId();
        }
    }

    protected void clearInstance() {
        //remove all peers in room
        if (mPeersList != null)
            mPeersList.clear();

        // reset class variables instance
        mSkylinkConnection = null;
        localAudioId = null;
        localVideoId = null;
        localScreenSharingId = null;
        localMainVideoId = null;
    }
}
