package sg.com.temasys.skylink.sdk.sampleapp.service

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import sg.com.temasys.skylink.sdk.listener.*
import sg.com.temasys.skylink.sdk.rtc.*
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback.*
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig.VideoDevice
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.CONFIG_TYPE
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils
import java.util.*

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for implementing all SkylinkListeners for common use of all demos/functions
 * And directly works with SkylinkSDK.
 * In case user does not want to implement a specific demo/function, no need to implement corresponding listener(s).
 */
abstract class SkylinkCommonService(var context: Context?) : LifeCycleListener, MediaListener, OsListener, RemotePeerListener, MessagesListener, DataTransferListener, FileTransferListener, RecordingListener {
    private val TAG = SkylinkCommonService::class.java.name

    // main presenter for audio/video/chat/...
    protected var presenter: BasePresenter? = null

    // video resolution presenter to implements video resolution logic
    protected var videoResPresenter: BasePresenter? = null

    //Skylink connection manager object
    private val skylinkConnectionManager: SkylinkConnectionManager

    //list of peers (including selfPeer) joined in room
    protected var mPeersList: MutableList<SkylinkPeer>? = null

    //room name and user name for each demo/function
    protected var demoRoomName: String? = null
    protected var demoUserName: String? = null

    // local SkylinkMedia objects to control locally
    var localAudio: SkylinkMedia? = null
    var localVideo: SkylinkMedia? = null
    var localScreen: SkylinkMedia? = null
    fun setSkylinkConnection(skylinkConnection: SkylinkConnection) {
        currentSkylinkConnection = skylinkConnection
    }

    //abstract methods need to be implemented in each demo/function like AudioService, VideoService,...
    abstract fun setSkylinkListeners()
    abstract val skylinkConfig: SkylinkConfig?
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
    override fun onConnectToRoomSucessful() {
        Log.d(TAG, "onConnectToRoomSucessful")
        var log = "[onConnectToRoomSuccessful] "
        // get the self peer id
        val localPeerId = currentSkylinkConnection!!.localPeerId
        log += "Connected to room " + getRoomIdAndNickname(demoRoomName) + " as " + demoUserName + " (" + localPeerId + ")"
        Utils.toastLog(TAG, context, log)

        //init/reset peers list and add self/local peer to list
        if (mPeersList == null) {
            mPeersList = ArrayList()
        } else {
            mPeersList!!.clear()
        }

        //add self peer as a peer in list
        val selfPeer = SkylinkPeer(localPeerId, demoUserName)

        // add local media to selfPeer if it was created
        if (localAudio != null) {
            selfPeer.addMedia(localAudio)
        }
        if (localVideo != null) {
            selfPeer.addMedia(localVideo)
        }
        if (localScreen != null) {
            selfPeer.addMedia(localScreen)
        }
        mPeersList!!.add(selfPeer)

        //update UI to connected state
        presenter!!.processRoomConnected(true)
    }

    /**
     * This is triggered when there is error for connecting to the room
     */
    override fun onConnectToRoomFailed(errorMessage: String) {
        Log.d(TAG, "onConnectToRoomFailed($errorMessage)")
        Utils.toastLog(TAG, context, errorMessage)
        presenter!!.processRoomConnected(false)
    }

    /**
     * This method is triggered from SkylinkSDK to inform that user is disconnected from room successfully or not
     */
    override fun onDisconnectFromRoom(skylinkEvent: SkylinkEvent, contextDescription: String) {
        Log.d(TAG, "onDisconnectFromRoom($skylinkEvent, message: $contextDescription)")
        var log = "[onDisconnectFromRoom] "
        if (skylinkEvent === SkylinkInfo.ROOM_DISCONNECT) {
            log += "We have successfully disconnected from the room."
        } else if (skylinkEvent === SkylinkError.ROOM_DISCONNECT_ERROR) {
            log += "WARNING! We have been unexpectedly disconnected from the room!"
        }
        log += " Server message: $contextDescription"
        Utils.toastLogLong(TAG, context, log)

        //update UI to disconnected state for all types call
        presenter!!.processRoomDisconnected()
    }

    /**
     * This is triggered from SkylinkSDK when the lock status of the room that we are is changed
     */
    override fun onChangeRoomLockStatus(roomLockStatus: Boolean, peerId: String) {
        Log.d(TAG, "onChangeRoomLockStatus(roomLockStatus: $roomLockStatus, remotePeerId: $peerId)")
        var log = "[SA][onChangeRoomLockStatus] "
        log += ("[SA] Peer " + peerId + " changed Room locked status to "
                + roomLockStatus + ".")
        Utils.toastLog(TAG, context, log)
        presenter!!.processRoomLockStatusChanged(roomLockStatus)
    }

    /**
     * This is triggered from SkylinkSDK to deliver messages that might be useful to the user.
     */
    override fun onReceiveInfo(skylinkInfo: SkylinkInfo, details: HashMap<String, Any>) {
        val contextDescriptionString = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
        Log.d(TAG, "onReceiveInfo(skylinkInfo: $skylinkInfo, details: $contextDescriptionString")
        val tag = "[SA][onReceiveInfo] "
        Utils.handleSkylinkReceiveInfo(skylinkInfo, contextDescriptionString, context, tag)
    }

    /**
     * This is triggered from SkylinkSDK to deliver a warning message to the user
     */
    override fun onReceiveWarning(skylinkError: SkylinkError, details: HashMap<String, Any>) {
        val contextDescriptionString = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
        Log.d(TAG, "onReceiveWarning(skylinkError: $skylinkError, details: $contextDescriptionString")
        val tag = "[SA][onReceiveWarning] "
        Utils.handleSkylinkWarningErrorMsg(skylinkError, contextDescriptionString, context, tag)
    }

    /**
     * This is triggered from SkylinkSDK to deliver a error message to the user
     */
    override fun onReceiveError(skylinkError: SkylinkError, details: HashMap<String, Any>) {
        val contextDescriptionString = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
        Log.d(TAG, "onReceiveError(skylinkError: $skylinkError, details: $contextDescriptionString")
        val tag = "[SA][onReceiveError] "
        Utils.handleSkylinkWarningErrorMsg(skylinkError, contextDescriptionString, context, tag)
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
    override fun onCreateLocalMedia(localMedia: SkylinkMedia) {
        Log.d(TAG, "onCreateLocalMedia(localMedia: $localMedia")
        if (!localMedia.isVideo) {
            localAudio = localMedia
            presenter!!.processLocalAudioCaptured(localMedia)
        } else {
            if (SkylinkMedia.MediaType.VIDEO_CAMERA == localMedia.mediaType
                    || SkylinkMedia.MediaType.VIDEO == localMedia.mediaType) {
                localVideo = localMedia
                presenter!!.processLocalCameraCaptured(localMedia)
            } else if (SkylinkMedia.MediaType.VIDEO_SCREEN == localMedia.mediaType) {
                localScreen = localMedia
                presenter!!.processLocalScreenCaptured(localMedia)
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
    override fun onReceiveRemoteMedia(remoteMedia: SkylinkMedia, remotePeerId: String) {
        Log.d(TAG, "onReceiveRemoteMedia(remoteMedia: $remoteMedia, remotePeerId: $remotePeerId")
        var log = "[SA][onReceiveRemoteMedia] "
        log += """
            Received new remote media (${remoteMedia.mediaId}) from Peer ${getPeerIdNick(remotePeerId)}.

            """.trimIndent()
        Utils.toastLog(TAG, context, log)
        val remotePeer = getPeerById(remotePeerId)
        remotePeer?.addMedia(remoteMedia)
        if (remoteMedia.isVideo) {
            presenter!!.processRemoteVideoReceived(remotePeerId, remoteMedia)
        } else {
            presenter!!.processRemoteAudioReceived(remotePeerId)
        }
    }

    /**
     * This is triggered from SkylinkSDK when a local media has been changed it state
     *
     * @param localMedia The media object with new state
     */
    override fun onChangeLocalMedia(localMedia: SkylinkMedia) {
        Log.d(TAG, "onChangeLocalMedia(localMedia: $localMedia")
        var log = "[SA][onChangeLocalMedia] "
        log += "Local media (" + localMedia.mediaId + ") state has been changed to " + localMedia.mediaState + "."
        Utils.toastLog(TAG, context, log)

        // update local media objects
        if (localMedia.mediaType == SkylinkMedia.MediaType.AUDIO || localMedia.mediaType == SkylinkMedia.MediaType.AUDIO_MIC) {
            localAudio = localMedia
        } else if (localMedia.mediaType == SkylinkMedia.MediaType.VIDEO || localMedia.mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            localVideo = localMedia
        } else if (localMedia.mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            localScreen = localMedia
        }
        if (mPeersList != null && mPeersList!!.size > 0) {
            val selfPeer = mPeersList!![0]
            selfPeer.updateMedia(localMedia)
        }
        presenter!!.processMediaStateChanged(localMedia, true)
    }

    /**
     * This is triggered from SkylinkSDK when a remote peer has changed his media state.
     *
     * @param remoteMedia  The remote media object with new value like state
     * @param remotePeerId The id of the peer (can be selfPeer or remote peer)
     */
    override fun onChangeRemoteMedia(remoteMedia: SkylinkMedia, remotePeerId: String) {
        Log.d(TAG, "onChangeRemoteMedia(remoteMedia: $remoteMedia, remotePeerId: $remotePeerId")
        var log = "[SA][onChangeRemoteMedia] "
        log += "Peer " + getPeerIdNick(remotePeerId) +
                " Media (" + remoteMedia.mediaId + ") state has been changed to " + remoteMedia.mediaState + "."
        Utils.toastLog(TAG, context, log)

        // update remote media object for remote peer
        val remotePeer = getPeerById(remotePeerId) ?: return
        remotePeer.updateMedia(remoteMedia)
        presenter!!.processMediaStateChanged(remoteMedia, false)

        // process remove local media id for the self peer if media state is UNAVAILABLE
        if (remoteMedia.mediaState == SkylinkMedia.MediaState.UNAVAILABLE) {
            remotePeer.removeMediaId(remoteMedia.mediaId)
        }
    }

    /**
     * This is triggered when a specific local media object is destroyed
     *
     * @param localMedia The local media object that was destroyed
     */
    override fun onDestroyLocalMedia(localMedia: SkylinkMedia) {
        Log.d(TAG, "onDestroyLocalMedia(localMedia: $localMedia")
        var log = "[SA][onDestroyLocalMedia] "
        log += "Local media (" + localMedia.mediaId + ") has been destroyed."
        Utils.toastLog(TAG, context, log)
        presenter!!.processMediaStateChanged(localMedia, true)
        if (mPeersList != null && mPeersList!!.size > 0) {
            mPeersList!![0].removeMediaId(localMedia.mediaId)
        }

        // remove local media id
        if (localMedia.mediaType == SkylinkMedia.MediaType.AUDIO || localMedia.mediaType == SkylinkMedia.MediaType.AUDIO_MIC) {
            localAudio = null
        } else if (localMedia.mediaType == SkylinkMedia.MediaType.VIDEO || localMedia.mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            localVideo = null
        } else if (localMedia.mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            localScreen = null
        }
    }

    override fun onObtainInputVideoResolution(width: Int, height: Int, fps: Int, captureFormat: SkylinkCaptureFormat?, mediaId: String) {
        Log.d(TAG, "onObtainInputVideoResolution(width: $width, height: $height, fps: $fps")
        var mediaType: SkylinkMedia.MediaType? = null
        if (localVideo != null && mediaId == localVideo!!.mediaId) {
            mediaType = SkylinkMedia.MediaType.VIDEO_CAMERA
        } else if (localScreen != null && mediaId == localScreen!!.mediaId) {
            mediaType = SkylinkMedia.MediaType.VIDEO_SCREEN
        }
        mediaType?.let { obtainInputVideoResolution(width, height, fps, captureFormat, it) }
    }

    override fun onObtainReceivedVideoResolution(width: Int, height: Int, fps: Int, mediaId: String, remotePeerId: String) {
        Log.d(TAG, "onObtainReceivedVideoResolution(width: " + width + ", height: " + height + ", fps: " + fps +
                ", mediaId: " + mediaId + ", remotePeerId: " + remotePeerId)
        var mediaType: SkylinkMedia.MediaType? = null
        val remotePeer = getPeerById(remotePeerId)
        if (remotePeer != null && remotePeer.mediaMap[mediaId] != null) {
            mediaType = remotePeer.mediaMap[mediaId]!!.mediaType
        }
        mediaType?.let { obtainReceivedVideoResolution(width, height, fps, it, remotePeerId) }
    }

    override fun onObtainSentVideoResolution(width: Int, height: Int, fps: Int, mediaId: String, remotePeerId: String) {
        Log.d(TAG, "onObtainSentVideoResolution(width: " + width + ", height: " + height + ", fps: " + fps +
                ", mediaId: " + mediaId + ", remotePeerId: " + remotePeerId)
        var mediaType: SkylinkMedia.MediaType? = null
        if (localVideo != null && mediaId == localVideo!!.mediaId) {
            mediaType = SkylinkMedia.MediaType.VIDEO_CAMERA
        } else if (localScreen != null && mediaId == localScreen!!.mediaId) {
            mediaType = SkylinkMedia.MediaType.VIDEO_SCREEN
        }
        mediaType?.let { obtainSentVideoResolution(width, height, fps, it, remotePeerId) }
    }

    /**
     * This is triggered from SkylinkSDK when any of the given video streams' frame size changes. It includes the
     * self stream also.
     *
     * @param size         Size of the video frame
     * @param media        The SkylinkMedia video object
     * @param remotePeerId The id of the peer. If null, it indicates self stream.
     */
    override fun onChangeVideoSize(size: Point, media: SkylinkMedia, remotePeerId: String) {
        Log.d(TAG, "[onChangeVideoSize] The media(" + media.mediaId + ") from Peer " +
                "(" + getPeerNameById(remotePeerId) + ") has changed the size to " + size.toString())
    }
    //----------------------------------------------------------------------------------------------
    // Methods which are from OsListener need to be implemented for audio, video, fileTransfer, multiVideo functions
    //----------------------------------------------------------------------------------------------
    /**
     * This is triggered when an Android Intent is required to use a part of the SDK. For e.g.,
     * the intent from [android.app.Activity.onActivityResult] generated
     * from [android.app.Activity.startActivityForResult], using the intent of
     * [android.media.projection.MediaProjectionManager.createScreenCaptureIntent],
     * is required before screen capture can be performed.
     * When received, app should use the intent and requestCode provided here to call
     * [android.app.Activity.startActivityForResult].
     * Once the corresponding [android.app.Activity.onActivityResult]
     * is received, app should pass the parameters (requestCode, resultCode, Intent) to the SDK's
     * [SkylinkConnection.processActivityResult].
     * The SDK will process this result and also notify app via
     * [OsListener.onGrantPermission] or [OsListener.onDenyPermission].
     *
     * @param intent      An Intent to be passed to
     * [android.app.Activity.startActivityForResult].
     * @param requestCode An unique integer that represents this permission request.
     * This is to be used as the requestCode when calling Android's
     * [android.app.Activity.startActivityForResult].
     * @param skylinkInfo
     */
    override fun onRequirePermission(intent: Intent, requestCode: Int, skylinkInfo: SkylinkInfo) {
        Log.d(TAG, "onRequirePermission(intent: " + intent.action + ", requestCode: " + requestCode + ", SkylinkInfo: " + skylinkInfo.description)
        presenter!!.processIntentRequired(intent, requestCode, skylinkInfo)
    }

    /**
     * This is triggered when Android Intent based permission has been granted for the usage of
     * a particular part of the SDK.
     *
     * @param intent      As that in [OsListener.onRequirePermission].
     * @param requestCode As that in [OsListener.onRequirePermission].
     * @param skylinkInfo
     */
    override fun onGrantPermission(intent: Intent, requestCode: Int, skylinkInfo: SkylinkInfo) {
        Log.d(TAG, "onGrantPermission(intent: " + intent.action + ", requestCode: " + requestCode + ", SkylinkInfo: " + skylinkInfo.description)
        presenter!!.processPermissionGranted(intent, requestCode, skylinkInfo)
    }

    /**
     * This is triggered when Android Intent based permission has been denied for the usage of
     * a particular part of the SDK.
     *
     * @param intent      As that in [OsListener.onRequirePermission].
     * @param requestCode As that in [OsListener.onRequirePermission].
     * @param skylinkInfo
     */
    override fun onDenyPermission(intent: Intent, requestCode: Int, skylinkInfo: SkylinkInfo) {
        Log.d(TAG, "onDenyPermission(intent: " + intent + ", requestCode: " + requestCode + ", SkylinkInfo: " + skylinkInfo.description)
        presenter!!.processPermissionDenied(intent, requestCode, skylinkInfo)
    }

    /**
     * This is triggered from SkylinkSDK when Android Runtime permission is required to use
     * devices to perform media functions like audio, camera, file transfer,...
     * The SDK will process this result and also notify app via
     * [OsListener.onGrantPermission] or [OsListener.onDenyPermission].
     *
     * @param permissions A Strings array containing the Android Runtime permission required
     * at the first array index (index 0).
     * @param requestCode An unique integer that represents this permission request.
     * This is to be used as the requestCode when calling
     * Android's requestPermissions.
     * @param skylinkInfo
     */
    override fun onRequirePermission(permissions: Array<String>, requestCode: Int, skylinkInfo: SkylinkInfo) {
        Log.d(TAG, "onRequirePermission(permissions: " + permissions + ", requestCode: " + requestCode + ", SkylinkInfo: " + skylinkInfo.description)
        val info = PermRequesterInfo(permissions, requestCode, skylinkInfo)
        presenter!!.processPermissionRequired(info)
    }

    /**
     * This is triggered when Android Runtime permission has been granted for the usage of
     * a particular part of the SDK.
     *
     * @param permissions A Strings array containing the Android Runtime permission required
     * at the first array index (index 0).
     * @param requestCode An unique integer that represents this permission request.
     * This was used as the requestCode when calling
     * Android's requestPermissions.
     * @param skylinkInfo
     */
    override fun onGrantPermission(permissions: Array<String>, requestCode: Int, skylinkInfo: SkylinkInfo) {
        Log.d(TAG, "onGrantPermission(permissions: " + permissions + ", requestCode: " + requestCode + ", SkylinkInfo: " + skylinkInfo.description)
        val info = PermRequesterInfo(permissions, requestCode, skylinkInfo)
        presenter!!.processPermissionGranted(info)
    }

    /**
     * This is triggered when Android Runtime permission has been denied for the usage of
     * a particular part of the SDK.
     *
     * @param permissions A Strings array containing the Android Runtime permission required
     * at the first array index (index 0).
     * @param requestCode An unique integer that represents this permission request.
     * This was used as the requestCode when calling
     * Android's requestPermissions.
     * @param skylinkInfo
     */
    override fun onDenyPermission(permissions: Array<String>,
                                  requestCode: Int, skylinkInfo: SkylinkInfo) {
        Log.d(TAG, "onDenyPermission(permissions: " + permissions + ", requestCode: " + requestCode + ", SkylinkInfo: " + skylinkInfo.description)
        val info = PermRequesterInfo(permissions, requestCode, skylinkInfo)
        presenter!!.processPermissionDenied(context, info)
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
     * 'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     */
    override fun onReceiveRemotePeerJoinRoom(remotePeerId: String, userInfo: UserInfo) {
        Log.d(TAG, "onReceiveRemotePeerJoinRoom(remotePeerId: " + remotePeerId + ", userInfo: " + userInfo.userData)
        val log = "[SA][onReceiveRemotePeerJoinRoom]" + " Remote Peer " + getPeerIdNick(remotePeerId) + " has joined the room."
        Utils.toastLog(TAG, context, log)
    }

    /**
     * This is triggered when our connection with the remote peer has been established
     * It will be triggered before onRemotePeerMediaReceive is triggered
     *
     * @param remotePeerId   The id of the remote peer
     * @param userInfo       Info about the remote user, including userData. UserData May be a
     * 'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param hasDataChannel Whether peer has enabled DataChannel.
     */
    override fun onConnectWithRemotePeer(remotePeerId: String, userInfo: UserInfo,
                                         hasDataChannel: Boolean) {
        Log.d(TAG, "onConnectWithRemotePeer(remotePeerId: " + remotePeerId + ", userInfo: " + userInfo.userData + ", hasDataChannel: " + hasDataChannel)
        var logTag = "[SA][onConnectWithRemotePeer] "

        // When remote peer joins room, keep track of user and update text-view to display details
        var remotePeerName: String? = null
        val userData = userInfo.userData
        if (userData is String) {
            remotePeerName = userData
        } else if (userData is JSONObject) {
            try {
                remotePeerName = userData["name"].toString()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        // use peer id if remote peer does not send the user name
        if (remotePeerName == null || remotePeerName == "") {
            remotePeerName = remotePeerId
        }

        //add remote peer into list
        val remotePeer = SkylinkPeer(remotePeerId, remotePeerName)
        mPeersList!!.add(remotePeer)
        presenter!!.processRemotePeerConnected(remotePeer)
        logTag += "Your Peer " + getPeerIdNick(remotePeerId) + " connected."
        Utils.toastLog(TAG, context, logTag)
    }

    /**
     * This is triggered when the connection with a remote peer is refreshed.
     *
     * @param remotePeerId   The id of the peer.
     * @param userInfo       Info of the remote user, including UserData. UserData may be a
     * 'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param hasDataChannel Whether peer has enabled DataChannel.
     * @param isIceRestarted Indicates if ICE restart was done when PeerConnection was refreshed.
     */
    override fun onRefreshRemotePeerConnection(remotePeerId: String, userInfo: UserInfo, hasDataChannel: Boolean, isIceRestarted: Boolean) {
        Log.d(TAG, "onRefreshRemotePeerConnection(remotePeerId: " + remotePeerId + ", userInfo: " + userInfo.userData + ", hasDataChannel: " + hasDataChannel + ", isIceRestarted: " + isIceRestarted)
        val logTag = "[SA][onRefreshRemotePeerConnection]"
        var peer = "Skylink Media Relay server"
        if (remotePeerId != null) {
            peer = "Peer " + getPeerIdNick(remotePeerId)
        }
        var log = logTag + "Your connection with " + peer + " has just been refreshed"
        log += if (isIceRestarted) {
            ", with ICE restarted.\r\n"
        } else {
            ".\r\n"
        }
        Utils.toastLog(TAG, context, log)
    }

    /**
     * This is triggered when an update is received in the user defined data of a remote peer.
     *
     * @param userData     User defined data relating to the remote peer. May be a
     * 'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param remotePeerId The id of the peer.
     */
    override fun onReceiveRemotePeerUserData(userData: Any, remotePeerId: String) {
        Log.d(TAG, "onReceiveRemotePeerUserData(userData: $userData, remotePeerId: $remotePeerId")

        // If Peer has no userData, use an empty string for nick.
        var remotePeerName: String? = null
        if (userData is String) {
            remotePeerName = userData
        } else if (userData is JSONObject) {
            try {
                remotePeerName = userData["name"].toString()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        val log = """
            [SA][onReceiveRemotePeerUserData] Peer ($remotePeerId)  has a new user data :
            $remotePeerName
            """.trimIndent()
        Utils.toastLog(TAG, context, log)
    }

    /**
     * This is triggered when the underlying peer to peer data connection is established with a
     * remote peer and is ready to send and receive peer messages and data (including files).
     *
     * @param remotePeerId The id of the remote peer
     */
    override fun onOpenRemotePeerDataConnection(remotePeerId: String) {
        Log.d(TAG, "onOpenRemotePeerDataConnection(remotePeerId: $remotePeerId")
    }

    override fun onDisconnectWithRemotePeer(remotePeerId: String, userInfo: UserInfo, hasDataChannel: Boolean) {
        Log.d(TAG, "onDisconnectWithRemotePeer(remotePeerId: " + remotePeerId + ", userInfo: " + userInfo.userData + ", hasDataChannel: " + hasDataChannel)
        val log = "[SA][onDisconnectWithRemotePeer]" + " Our connection with Remote Peer " + getPeerIdNick(remotePeerId) + " has been terminated."
        Utils.toastLog(TAG, context, log)
        disconnectRemotePeer(remotePeerId)
    }

    /**
     * This is triggered when a remote peer leaves the room.
     *
     * @param remotePeerId The id of the remote peer
     * @param skylinkInfo  The [SkylinkInfo] with more details on this event of
     * Peer leaving room.
     * @param userInfo     UserInfo of this remote Peer.
     */
    override fun onReceiveRemotePeerLeaveRoom(remotePeerId: String, skylinkInfo: SkylinkInfo, userInfo: UserInfo) {
        Log.d(TAG, "onReceiveRemotePeerLeaveRoom(remotePeerId: " + remotePeerId + ", SkylinkInfo: " + skylinkInfo.description + ", userInfo: " + userInfo.userData)
        disconnectRemotePeer(remotePeerId)
        val numRemotePeers = mPeersList!!.size - 1
        if (numRemotePeers >= 0) {
            val log = ("Your Peer " + getUserDataString(userInfo) + " (" + remotePeerId + ") left."
                    + skylinkInfo.description + " " + numRemotePeers + " remote Peer(s) left in the room.")
            Utils.toastLog(TAG, context, log)
        }
    }

    /**
     * This is triggered when having fail connection with a remote peer
     * When user get this callback, they can reconnect or refresh the connection with the remote peer,
     * or simply ignore this peer
     *
     * @param skylinkError [SkylinkError] with more details about this error.
     * @param details      Additional details in a map.
     */
    override fun onErrorForRemotePeerConnection(skylinkError: SkylinkError, details: HashMap<String, Any>) {
        val logTag = "[SA][onErrorForRemotePeerConnection] "
        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
        val remotePeerId = details[SkylinkEvent.REMOTE_PEER_ID] as String?
        val log = logTag + "Your connection with the peer (" + remotePeerId +
                ") is NOT successful. " + contextDescription + " " + skylinkError
        Log.d(TAG, log)
        Utils.toastLog(TAG, context, log)
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
    override fun onReceiveData(data: ByteArray, remotePeerId: String) {
        Log.d(TAG, "onReceiveData(data: " + data.size + ", remotePeerId: " + remotePeerId)
        presenter!!.processDataReceive(context, remotePeerId, data)
    }
    //----------------------------------------------------------------------------------------------
    // Methods which are from FileTransferListener need to be implemented for fileTransfer function
    //----------------------------------------------------------------------------------------------
    /**
     * This is triggered upon receiving a file transfer request from a peer.
     *
     * @param fileName     The name of the file
     * @param isPublic     Flag to specify if file share was directed to only us (as opposed to all
     * peers in the room).
     * @param remotePeerId The id of the peer that the file comes from
     */
    override fun onReceiveFileTransferRequest(fileName: String,
                                              isPublic: Boolean, remotePeerId: String) {
        Log.d(TAG, "onReceiveFileTransferRequest(fileName: $fileName, isPublic: $isPublic, remotePeerId: $remotePeerId")
        presenter!!.processFilePermissionRequested(remotePeerId, fileName, !isPublic)
    }

    /**
     * This is triggered upon receiving the response of a peer to our request for file transfer with
     * the peer.
     *
     * @param wasAccepted  Flag to specify whether the peer has accepted the request or not
     * @param fileName     The name of the file
     * @param remotePeerId The id of the peer that has accept for reject the file
     */
    override fun onReceiveFileTransferResponse(wasAccepted: Boolean, fileName: String, remotePeerId: String) {
        Log.d(TAG, "onReceiveFileTransferResponse(wasAccepted: $wasAccepted,fileName: $fileName, remotePeerId: $remotePeerId")
        presenter!!.processFilePermissionResponded(context, remotePeerId, fileName, wasAccepted)
    }

    /**
     * This is triggered when an ongoing file transfer drops due to some reason.
     *
     * @param fileName     The name of the file
     * @param message      Message that possibly tells the reason for dropping
     * @param isExplicit   True if user canceled the transfer explicitly, as opposed to being due to
     * error in the transfer process.
     * @param remotePeerId The id of the peer. Null if broadcast file transfer was attempted
     */
    override fun onDropFileTransfer(fileName: String, message: String,
                                    isExplicit: Boolean, remotePeerId: String) {
        Log.d(TAG, "onDropFileTransfer(fileName: $fileName,message: $message, isExplicit: $isExplicit,remotePeerId: $remotePeerId")
        presenter!!.processFileDropped(context, remotePeerId, fileName, message, isExplicit)
    }

    /**
     * This is triggered when we have sent a file successfully.
     *
     * @param fileName     The name of the file
     * @param remotePeerId The id of the peer receiving the file.
     */
    override fun onCompleteFileTransferSending(fileName: String, remotePeerId: String) {
        Log.d(TAG, "onCompleteFileTransferSending(fileName: $fileName, remotePeerId: $remotePeerId")
        presenter!!.processFileSentCompleted(context, remotePeerId, fileName)
    }

    /**
     * This is triggered when we have received a file successfully.
     *
     * @param fileName     The name of the file
     * @param remotePeerId The id of the peer receiving the file
     */
    override fun onCompleteFileTransferReceiving(fileName: String, remotePeerId: String) {
        Log.d(TAG, "onCompleteFileTransferReceiving(fileName: $fileName, remotePeerId: $remotePeerId")
        presenter!!.processFileReceivedCompleted(remotePeerId, fileName)
    }

    /**
     * This is triggered timely to report the on going progress when sending a file
     *
     * @param percentage   The percentage completed
     * @param fileName     The name of the file
     * @param remotePeerId The id of the peer receiving the file.
     */
    override fun onUpdateFileTransferSendingProgress(percentage: Double, fileName: String, remotePeerId: String) {
        Log.d(TAG, "onUpdateFileTransferSendingProgress(percentage: $percentage,fileName: $fileName, remotePeerId: $remotePeerId")
        presenter!!.processFileSentProgressed(context, remotePeerId, fileName, percentage)
    }

    /**
     * This is triggered timely to report the on going progress when receiving a file
     *
     * @param percentage   The percentage completed
     * @param fileName     The name of the file
     * @param remotePeerId The id of the peer sending the file.
     */
    override fun onUpdateFileTransferReceivingProgress(
            percentage: Double, fileName: String, remotePeerId: String) {
        Log.d(TAG, "onUpdateFileTransferReceivingProgress(percentage: $percentage,fileName: $fileName, remotePeerId: $remotePeerId")
        presenter!!.processFileReceivedProgressed(context, remotePeerId, fileName, percentage)
    }
    //----------------------------------------------------------------------------------------------
    // Methods which are from MessagesListener need to be implemented for chat/messaging function
    //----------------------------------------------------------------------------------------------
    /**
     * This is triggered when a broadcast or private message is received from a remote peer via a
     * server.
     *
     * @param message      User defined message. May be a 'java.lang.String', 'org.json.JSONObject'
     * or 'org.json.JSONArray'.
     * @param isPublic     Flag to specify whether the message was sent to us only (as opposed to
     * broadcast to all peers in the room).
     * @param timeStamp    The timestamp of the message,
     * The number of milliseconds since the epoch of 1970-01-01T00:00:00Z (midnight, January 1, 1970 UTC).
     * @param remotePeerId The id of the remote peer
     */
    override fun onReceiveServerMessage(message: Any, isPublic: Boolean, timeStamp: Long, remotePeerId: String) {
        Log.d(TAG, "onReceiveServerMessage(message: $message,isPublic: $isPublic, timeStamp: $timeStamp, remotePeerId: $remotePeerId")
        presenter!!.processServerMessageReceived(remotePeerId, message, !isPublic, timeStamp)
    }

    /**
     * This is triggered when a broadcast or private message is received directly from the peer,
     * i.e. in a peer to peer manner.
     *
     * @param message      User defined message. May be a 'java.lang.String', 'org.json.JSONObject'
     * or 'org.json.JSONArray'.
     * @param isPublic     Flag to specify whether the message was sent to us only (as opposed to
     * broadcast to all peers in the room).
     * @param timeStamp    The timestamp of the message,
     * The number of milliseconds since the epoch of 1970-01-01T00:00:00Z (midnight, January 1, 1970 UTC).
     * @param remotePeerId The id of the remote peer
     */
    override fun onReceiveP2PMessage(message: Any, isPublic: Boolean, timeStamp: Long, remotePeerId: String) {
        Log.d(TAG, "onReceiveP2PMessage(message: $message,isPublic: $isPublic, timeStamp: $timeStamp, remotePeerId: $remotePeerId")
        presenter!!.processP2PMessageReceived(remotePeerId, message, !isPublic)
    }
    //----------------------------------------------------------------------------------------------
    // Methods which are from RecordingListener need to be implemented for recording (in Multi Video function)
    //----------------------------------------------------------------------------------------------
    /**
     * This is triggered to indicate that recording has started for this room.
     *
     * @param recordingId Id of Recording session this message is about.
     */
    override fun onStartRecording(recordingId: String) {
        Log.d(TAG, "onStartRecording(recordingId: $recordingId")
        presenter!!.processRecordingStarted(context, currentSkylinkConnection!!.isRecording)
    }

    /**
     * This is triggered to indicate that recording has stopped for this room.
     *
     * @param recordingId Id of Recording session this message is about.
     */
    override fun onStopRecording(recordingId: String) {
        Log.d(TAG, "onStopRecording(recordingId: $recordingId")
        presenter!!.processRecordingStopped(context, currentSkylinkConnection!!.isRecording)
    }

    /**
     * This is triggered when a recording error occurs.
     *
     * @param errorCode   [Errors][sg.com.temasys.skylink.sdk.rtc.Errors] Integer error code of the recording error encountered.
     * @param description Description of the recording error encountered.
     * @param recordingId Id of Recording session this message is about.
     */
    override fun onReceiveRecordingError(errorCode: Int, description: String, recordingId: String) {
        Log.d(TAG, "onReceiveRecordingError(errorCode: $errorCode,description: $description,recordingId: $recordingId")
        Utils.toastLogLong(TAG, context, "onReceiveRecordingError(errorCode: $errorCode,description: $description,recordingId: $recordingId")
        presenter!!.processRecordingError(context, recordingId, errorCode, description)
    }

    //----------------------------------------------------------------------------------------------
    // Public methods helps to work with SkylinkSDK
    //----------------------------------------------------------------------------------------------
    protected fun initializeSkylinkConnection(typeCall: CONFIG_TYPE) {
        Log.d(TAG, "initializeSkylinkConnection with typeCall: $typeCall")
        currentSkylinkConnection = skylinkConnectionManager.initializeSkylinkConnection(typeCall)

        // enable debugging log
        if (currentSkylinkConnection != null) {
            setEnableLogsForDebugging(true)
        }
    }

    val isConnectingOrConnected: Boolean
        get() = skylinkConnectionManager.isConnectingOrConnected

    /**
     * Connect to room using a [SkylinkConfig.VideoDevice] that:
     * - Is appropriate for the [SkylinkConfig], and
     * - Takes into account the default [SkylinkConfig.VideoDevice] set in SA Config.
     *
     * @param typeCall
     */
    fun connectToRoom(typeCall: CONFIG_TYPE?) {
        skylinkConnectionManager.connectToRoomByConnectionString(typeCall)

        // we can use another method to connect to the room
        // skylinkConnectionManager.connectToRoomByAppKey(typeCall);
    }

    fun disconnectFromRoom() {
        skylinkConnectionManager.disconnectFromRoom()
    }

    /**
     * To enable logs from Skylink SDK (e.g. during debugging):
     * Should not enable logs for production apps!
     */
    protected fun setEnableLogsForDebugging(enableLogsForDebugging: Boolean) {
        currentSkylinkConnection?.setEnableLogs(enableLogsForDebugging)
    }

    fun getPeerId(index: Int): String {
        return mPeersList!![index].peerId
    }

    /**
     * Get nickName or userName combined with peerId of a peer in room
     *
     * @param peerId null is local peer, no null is specific peer
     * @return Desired nickName or userName combined with peer id of the peer
     */
    fun getPeerIdNick(peerId: String?): String {

        //get local peer from SDK
        var peerId = peerId
        if (peerId == null && currentSkylinkConnection != null) {
            peerId = currentSkylinkConnection!!.localPeerId
        }
        var peerIdShow = peerId

        //can not get local peer
        if (peerId == null) {
            peerIdShow = "Self"
        }
        val peerUserName = getUserDataString(peerIdShow)
        return "$peerUserName ($peerId)"
    }

    /**
     * Get nickName or userName combined with peerId of a peer in room
     *
     * @param peerId null is local peer, no null is specific peer
     * @return Desired nickName or userName of the peer
     */
    fun getPeerUserName(peerId: String?): String {

        //get local peer from SDK
        var peerId = peerId
        if (peerId == null && currentSkylinkConnection != null) {
            peerId = currentSkylinkConnection!!.localPeerId
        }
        var peerIdShow = peerId

        //can not get local peer
        if (peerId == null) {
            peerIdShow = "Self"
        }
        return getUserDataString(peerIdShow)
    }

    /**
     * Return the SkylinkPeer with the specific peerId
     *
     * @param peerId id of the peer need to search
     * @return SkylinkPeer
     */
    fun getPeerById(peerId: String): SkylinkPeer? {
        if (mPeersList == null || mPeersList!!.size == 0) return null
        for (peer in mPeersList!!) {
            if (peer.peerId == peerId) {
                return peer
            }
        }
        return null
    }

    /**
     * Provides the room ID of the room currently connected to.
     */
    val roomId: String?
        get() = if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.roomId
        } else null

    fun getPeerNameById(peerId: String?): String? {
        return if (currentSkylinkConnection != null) {
            getPeerUserName(peerId)
        } else null
    }

    /**
     * Get the room name with room id combined with local peer name and peer id
     * for a specific demo/function
     */
    fun getRoomIdAndNickname(typeCall: CONFIG_TYPE?): String? {
        if (currentSkylinkConnection == null) {
            return null
        }
        val roomName = Utils.getRoomNameByType(typeCall)
        var title = "Room: " + getRoomIdAndNickname(roomName)
        title += """

            ${getPeerIdNick(peerId)}
            """.trimIndent()
        return title
    }

    /**
     * Get the room name with room id
     */
    fun getRoomIdAndNickname(roomName: String?): String {
        //in case of display room Name and room Id
        var roomId = ""
        if (currentSkylinkConnection != null) {
            roomId = currentSkylinkConnection!!.roomId
        }
        return "$roomName($roomId)"
    }

    /**
     * Get local/self PeerId, or return null if not available.
     */
    val peerId: String?
        get() = if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.localPeerId
        } else null

    val peersList: List<SkylinkPeer>?
        get() = mPeersList

    /**
     * Get total number of peers in room
     */
    val totalPeersInRoom: Int
        get() = if (mPeersList == null) 0 else mPeersList!!.size

    fun setRoomName(roomName: String?) {
        this.demoRoomName = roomName
    }

    fun setUserName(userName: String?) {
        this.demoUserName = userName
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
    fun setInputVideoResolution(mediaType: SkylinkMedia.MediaType,
                                width: Int, height: Int, fps: Int) {
        if (currentSkylinkConnection == null) return

        // get video id
        val videoId = getProperLocalMediaId(mediaType) ?: return
        currentSkylinkConnection!!.setInputVideoResolution(videoId, width, height, fps, object : SkylinkCallback {
            override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                Log.e("SkylinkCallback", contextDescription)
                Utils.toastLog(TAG, context, "\"Unable to setInputVideoResolution as $contextDescription")
            }
        })

        // get input video resolution to check the result
        currentSkylinkConnection!!.getInputVideoResolution(videoId, object : InputVideoResolution {
            override fun onObtainInputVideoResolution(width: Int, height: Int, fps: Int, captureFormat: SkylinkCaptureFormat?) {
                obtainInputVideoResolution(width, height, fps, captureFormat, mediaType)
            }
        })
    }

    /**
     * Get the name of the current camera being used.
     * If no camera or if a custom VideoCapturer is being used, return null.
     *
     * @return
     */
    val currentCameraName: String?
        get() = if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.currentCameraName
        } else null

    /**
     * Get the current [VideoDevice][SkylinkConfig.VideoDevice] being used.
     * If none are active, return null.
     *
     * @return
     */
    val currentVideoDevice: VideoDevice?
        get() = if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.currentVideoDevice
        } else null

    /**
     * Get the possible capture format(s) of the specified camera device in an array.
     * Return null if current [VideoDevice][SkylinkConfig.VideoDevice] is not a defined camera,
     * or if it was not possible to get the capture formats.
     *
     * @param videoDevice Use null to specific the current VideoDevice.
     * @return
     */
    fun getCaptureFormats(videoDevice: VideoDevice?): Array<SkylinkCaptureFormat>? {
        return if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.getAllCaptureFormats(videoDevice)
        } else null
    }

    /**
     * Return the info of the SkylinkCaptureFormat that is currently being used by the camera.
     * Note that the current CaptureFormat may change whenever the
     * video resolution dimensions change.
     *
     * @return null if there is no CaptureFormat in use now, e.g. if video is not capturing.
     */
    fun getCaptureFormatsString(captureFormats: Array<SkylinkCaptureFormat?>?): String? {
        var strFormat = "No CaptureFormat currently registered."
        var strFormats = "No CaptureFormats currently registered."
        if (Utils.isCaptureFormatsValid(captureFormats)) {
            strFormats = Utils.captureFormatsToString(captureFormats)
        }

        // Get the current CaptureFormat, if there is one.
        var captureFormatString: String? = null
        if (currentSkylinkConnection != null) {
            val captureFormat = currentSkylinkConnection!!.currentCaptureFormat
            if (captureFormat != null) {
                strFormat = captureFormat.toString()
            }
            captureFormatString = """
                Current capture format: $strFormat.
                Supported capture formats: $strFormats.
                """.trimIndent()
        }
        return captureFormatString
    }

    /**
     * Get the input/sent/received video resolution of a specified peer with specific media type (video or screen)
     * Note:
     * - Resolution may not always be available, e.g. if no video is captured.
     * - If resolution are available, they will be returned in the below callback as the process is asynchronous
     * [SkylinkCallback.InputVideoResolution.onObtainInputVideoResolution] for input video resolution
     * [SkylinkCallback.SentVideoResolution.onObtainSentVideoResolution] for sent video resolution
     * [SkylinkCallback.ReceivedVideoResolution.onObtainReceivedVideoResolution] for received video resolution
     *
     * @param mediaType the type of the video (video or screen)
     * @param peerIndex index of the remote Peer from whom we want to get sent or received video resolution.
     */
    fun getVideoResolutions(mediaType: SkylinkMedia.MediaType,
                            peerIndex: Int) {
        if (currentSkylinkConnection == null) return
        val videoId = getProperLocalMediaId(mediaType)
        var remotePeerId: String? = null
        if (mPeersList != null && mPeersList!!.size > 1) {
            remotePeerId = mPeersList!![peerIndex].peerId
        }

        // if local video has not been created, get received video resolution from remote media
        if (videoId == null) {
            obtainInputVideoResolution(-1, -1, -1, null, mediaType)
            if (remotePeerId != null) {
                obtainSentVideoResolution(-1, -1, -1, mediaType, remotePeerId)

                // get received video res from remote peer
                val remoteMediaList = currentSkylinkConnection!!.getSkylinkMediaList(mediaType, remotePeerId)
                if (remoteMediaList == null || remoteMediaList.size == 0) {
                    obtainReceivedVideoResolution(-1, -1, -1, mediaType, remotePeerId)
                    return
                }
                val remoteMedia = remoteMediaList[0]
                if (remoteMedia != null && remoteMedia.mediaState != SkylinkMedia.MediaState.UNAVAILABLE) {
                    val mediaId = remoteMedia.mediaId
                    val finalRemotePeerId2: String = remotePeerId
                    currentSkylinkConnection!!.getReceivedVideoResolution(mediaId, object : ReceivedVideoResolution {
                        override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                            val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                            Log.e("SkylinkCallback", contextDescription)
                            obtainReceivedVideoResolution(-1, -1, -1, mediaType, finalRemotePeerId2)
                        }

                        override fun onObtainReceivedVideoResolution(width: Int, height: Int, fps: Int) {
                            obtainReceivedVideoResolution(width, height, fps, mediaType, finalRemotePeerId2)
                        }
                    })
                } else {
                    obtainReceivedVideoResolution(-1, -1, -1, mediaType, remotePeerId)
                }
            }
        } else {
            // get input video res
            currentSkylinkConnection!!.getInputVideoResolution(videoId, object : InputVideoResolution {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    obtainInputVideoResolution(-1, -1, -1, null, mediaType)
                }

                override fun onObtainInputVideoResolution(width: Int, height: Int, fps: Int, captureFormat: SkylinkCaptureFormat?) {
                    obtainInputVideoResolution(width, height, fps, captureFormat, mediaType)
                }
            })

            // get sent and received video res if there is any remote peer in the room
            if (remotePeerId != null) {
                // get sent video res to remote peer
                val finalRemotePeerId1: String = remotePeerId
                currentSkylinkConnection!!.getSentVideoResolution(remotePeerId, videoId, object : SentVideoResolution {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        obtainSentVideoResolution(-1, -1, -1, mediaType, finalRemotePeerId1)
                    }

                    override fun onObtainSentVideoResolution(width: Int, height: Int, fps: Int) {
                        obtainSentVideoResolution(width, height, fps, mediaType, finalRemotePeerId1)
                    }
                })

                // get received video res from remote peer
                val remoteMediaList = currentSkylinkConnection!!.getSkylinkMediaList(mediaType, remotePeerId)
                if (remoteMediaList == null || remoteMediaList.size == 0) {
                    obtainReceivedVideoResolution(-1, -1, -1, mediaType, remotePeerId)
                    return
                }
                val remoteMedia = remoteMediaList[0]
                if (remoteMedia != null && remoteMedia.mediaState != SkylinkMedia.MediaState.UNAVAILABLE) {
                    val mediaId = remoteMedia.mediaId
                    val finalRemotePeerId: String = remotePeerId
                    currentSkylinkConnection!!.getReceivedVideoResolution(mediaId, object : ReceivedVideoResolution {
                        override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                            val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                            Log.e("SkylinkCallback", contextDescription)
                            obtainReceivedVideoResolution(-1, -1, -1, mediaType, finalRemotePeerId)
                        }

                        override fun onObtainReceivedVideoResolution(width: Int, height: Int, fps: Int) {
                            obtainReceivedVideoResolution(width, height, fps, mediaType, finalRemotePeerId)
                        }
                    })
                } else {
                    obtainReceivedVideoResolution(-1, -1, -1, mediaType, remotePeerId)
                }
            }
        }
    }

    /**
     * This is triggered from SkylinkSDK when a local camera starts to capture video,
     * if [SkylinkConfig.isReportVideoResolutionOnVideoChange] is true;
     * or when its currently reporting and video resolution is not yet stable,
     * if [SkylinkConfig.isReportVideoResolutionUntilStable] is true;
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
    protected fun obtainInputVideoResolution(width: Int, height: Int,
                                             fps: Int, captureFormat: SkylinkCaptureFormat?, mediaType: SkylinkMedia.MediaType) {
        // if the current demo has video resolution fragment to reflect the change
        if (videoResPresenter != null) videoResPresenter!!.processInputVideoResolutionObtained(mediaType, width, height, fps, captureFormat) else {
            val log = """[SA][obtainInputVideoResolution] The local video ($mediaType) input has width x height, fps: $width x $height, $fps fps.
"""
            Utils.toastLogLong(TAG, context, log)
        }
    }

    /**
     * This is triggered from SkylinkSDK when we receive a remote Peer's video,
     * if [SkylinkConfig.isReportVideoResolutionOnVideoChange] is true;
     * or when its currently reporting and video resolution is not yet stable,
     * if [SkylinkConfig.isReportVideoResolutionUntilStable] is true.
     * The current resolution of the video received from a specific Peer will be provided.
     * If a particular value is not available, a negative integer will be provided.
     *
     * @param width        Video width in pixels of remote Peer's video received by us.
     * @param height       Video height in pixels of remote Peer's video received by us.
     * @param fps          Video frame per second of remote Peer's video received by us.
     * @param mediaType    type of the remote video that video resolution comes from
     * @param remotePeer PeerId of the remote Peer whose Video Resolution we are obtaining.
     */
    protected fun obtainReceivedVideoResolution(width: Int, height: Int,
                                                fps: Int, mediaType: SkylinkMedia.MediaType?, remotePeer: String) {
        // if the current demo has video resolution fragment to reflect the change
        if (videoResPresenter != null && mediaType != null) videoResPresenter!!.processReceivedVideoResolutionObtained(remotePeer, mediaType, width, height, fps) else {
            val log = """[SA][obtainReceivedVideoResolution] The video ($mediaType) received from Peer $remotePeer has width x height, fps: $width x $height, $fps fps.
"""
            Utils.toastLogLong(TAG, context, log)
        }
    }

    /**
     * This is triggered from SkylinkSDK when a local video stream starts being sent to a remote Peer,
     * if [SkylinkConfig.isReportVideoResolutionOnVideoChange] is true;
     * or when its currently reporting and video resolution is not yet stable,
     * if [SkylinkConfig.isReportVideoResolutionUntilStable] is true;
     * The current resolution of the video being sent to a specific Peer will be provided.
     *
     * @param width        Video width in pixels of local video sent to remote Peer.
     * @param height       Video height in pixels of local video sent to remote Peer.
     * @param fps          Video frame per second of local video sent to remote Peer.
     * @param mediaType    type of the video that video resolution comes from
     * @param remotePeerId PeerId of the remote Peer whom we are obtaining Video Resolution
     * for the video we are sending.
     */
    protected fun obtainSentVideoResolution(width: Int, height: Int,
                                            fps: Int, mediaType: SkylinkMedia.MediaType, remotePeerId: String) {
        // if the current demo has video resolution fragment to reflect the change
        if (videoResPresenter != null) videoResPresenter!!.processSentVideoResolutionObtained(remotePeerId, mediaType, width, height, fps) else {
            val log = """[SA][obtainSentVideoResolution] The video ($mediaType) sent to Peer $remotePeerId has width x height, fps: $width x $height, $fps fps.
"""
            Utils.toastLogLong(TAG, context, log)
        }
    }

    fun lockRoom() {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.lockRoom(object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to lockRoom as $contextDescription")
                }
            })
        }
    }

    fun unlockRoom() {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.unlockRoom(object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to unlockRoom as $contextDescription")
                }
            })
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
    private fun getUserDataString(peerId: String?): String {
        var userDataString = ""
        if (currentSkylinkConnection != null) {
            val userInfo = currentSkylinkConnection!!.getUserInfo(peerId)
            var userDataObject: Any? = null
            if (userInfo != null) {
                userDataObject = userInfo.userData
            }
            if (userDataObject != null) {
                userDataString = userDataObject.toString()
            }
        }
        return userDataString
    }

    private fun getUserDataString(userInfo: UserInfo): String {
        var userInfo: UserInfo? = userInfo
        if (userInfo == null && currentSkylinkConnection != null) {
            userInfo = currentSkylinkConnection!!.getUserInfo(null)
        }
        if (userInfo == null) {
            return ""
        }
        val userDataObject = userInfo.userData
        var userDataString = ""
        if (userDataObject != null) {
            userDataString = userDataObject.toString()
        }
        return userDataString
    }

    /**
     * Retrieves the UserInfo object of a Peer.
     *
     * @param userId The PeerId of the peer whose UserInfo is to be retrieved, or NULL for self.
     * @return UserInfo
     */
    private fun getUserInfo(userId: String): UserInfo? {
        return if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.getUserInfo(userId)
        } else null
    }

    /**
     * Get proper video id for input to SDK to get video resolution or video stats
     */
    protected fun getProperLocalMediaId(mediaType: SkylinkMedia.MediaType?): String? {
        var mediaId: String? = null

        // default main video id from current local video
        if (localVideo != null && (mediaType == SkylinkMedia.MediaType.VIDEO || mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA || mediaType == null)) {
            mediaId = localVideo!!.mediaId
        } else if (localScreen != null && mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            mediaId = localScreen!!.mediaId
        }
        return mediaId
    }

    /**
     * Get proper video id for input to SDK to get video resolution or video stats
     */
    protected fun getProperRemoteMediaId(remotePeerId: String?, mediaType: SkylinkMedia.MediaType?, isLocal: Boolean): String? {

        // TODO @Muoi need to update when SDK finished get stats by specific media track
        // currently the SDK is just able to get full stats for receiving track, no all mediaTypes or
        // media tracks will get the same stats
        return if (isLocal) {
            getProperLocalMediaId(mediaType)
        } else {
            currentSkylinkConnection!!.getSkylinkMediaList(mediaType, remotePeerId)[0].mediaId
        }
    }

    protected fun clearInstance() {
//        if (skylinkConnection == null)
//            return;

        //remove all peers in room
        if (mPeersList != null) mPeersList!!.clear()

        // reset class variables instance
        currentSkylinkConnection!!.clearInstance()
        localAudio = null
        localVideo = null
        localScreen = null
        //        skylinkConnectionManager.setSkylinkConnection(null);

//        skylinkConnection = null;
    }

    private fun disconnectRemotePeer(remotePeerId: String) {
        //remove remote peer in the mPeersList and remove the peer index in view
        var removeIndex = -1
        var removedPeer: SkylinkPeer? = null

        // re-fill all peers, except local peer
        for (i in 1 until mPeersList!!.size) {
            if (mPeersList!![i].peerId == remotePeerId) {
                removedPeer = mPeersList!![i]
                mPeersList!!.removeAt(i)
                removeIndex = i
                break
            }
        }

        //update UI when remote peer left the room
        if (removedPeer != null && removeIndex != -1) presenter!!.processRemotePeerDisconnected(removedPeer, removeIndex - 1)
    }

    companion object {
        //static method for the other to access current instance of SkylinkConnection
        // SkylinkConnection instance for working with SkylinkSDK
        var currentSkylinkConnection: SkylinkConnection? = null
            protected set

    }

    init {
        skylinkConnectionManager = context?.let { SkylinkConnectionManager(this, it) }!!
    }
}