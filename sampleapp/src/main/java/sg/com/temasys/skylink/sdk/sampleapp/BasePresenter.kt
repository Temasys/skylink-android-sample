package sg.com.temasys.skylink.sdk.sampleapp

import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONArray
import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat
import sg.com.temasys.skylink.sdk.rtc.SkylinkInfo
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils
import java.util.*

/**
 * Created by muoi.pham on 20/07/18.
 *
 *
 * A common abstract class for all presenters.
 * This class defined all methods which responsible for updating GUI requested by the SDK.
 * Some of those which do not need to be override in the concrete classes can be
 * implemented in the BasePresenter (such as just displaying toast to inform changes to user)
 */
abstract class BasePresenter {
    //----------------------------------------------------------------------------------------------
    // Methods which are from LifeCycleListener need to be implemented for all functions
    //----------------------------------------------------------------------------------------------
    open fun processRoomConnected(isSuccessful: Boolean) {}
    open fun processRoomDisconnected() {}

    //----------------------------------------------------------------------------------------------
    // Methods which are from RemotePeerListener need to be implemented for all functions
    //----------------------------------------------------------------------------------------------
    open fun processRemotePeerConnected(newPeer: SkylinkPeer?) {}
    open fun processRemotePeerDisconnected(remotePeer: SkylinkPeer?, removeIndex: Int) {}

    //----------------------------------------------------------------------------------------------
    // Methods which are from MediaListener need to be implemented for audio and video
    //----------------------------------------------------------------------------------------------
    open fun processRemoteAudioReceived(remotePeerId: String?) {}
    open fun processRemoteVideoReceived(remotePeerId: String?, remoteVideo: SkylinkMedia?) {}
    open fun processInputVideoResolutionObtained(mediaType: SkylinkMedia.MediaType?, width: Int, height: Int, fps: Int, captureFormat: SkylinkCaptureFormat?) {}
    open fun processReceivedVideoResolutionObtained(peerId: String?, mediaType: SkylinkMedia.MediaType?, width: Int, height: Int, fps: Int) {}
    open fun processSentVideoResolutionObtained(peerId: String?, mediaType: SkylinkMedia.MediaType?, width: Int, height: Int, fps: Int) {}

    //----------------------------------------------------------------------------------------------
    // Methods which are from OsListener need to be implemented for audio, video, fileTransfer, multiVideo functions
    //----------------------------------------------------------------------------------------------
    open fun processIntentRequired(intent: Intent?, requestCode: Int, skylinkInfo: SkylinkInfo?) {}
    fun processPermissionGranted(intent: Intent?, requestCode: Int, skylinkInfo: SkylinkInfo?) {
        PermissionUtils.onPermissionGrantedHandler(requestCode, skylinkInfo, true)
    }

    fun processPermissionDenied(intent: Intent?, requestCode: Int, skylinkInfo: SkylinkInfo?) {
        PermissionUtils.onPermissionGrantedHandler(requestCode, skylinkInfo, false)
    }

    open fun processPermissionRequired(info: PermRequesterInfo?) {}
    fun processPermissionGranted(info: PermRequesterInfo?) {
        PermissionUtils.onPermissionGrantedHandler(info)
    }

    fun processPermissionDenied(context: Context?, info: PermRequesterInfo?) {
        PermissionUtils.onPermissionDeniedHandler(info, context)
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from DataTransferListener need to be implemented for dataTransfer function
    //----------------------------------------------------------------------------------------------
    open fun processDataReceive(context: Context?, remotePeerId: String?, data: ByteArray?) {}

    //----------------------------------------------------------------------------------------------
    // Methods which are from FileTransferListener need to be implemented for fileTransfer function
    //----------------------------------------------------------------------------------------------
    open fun processFilePermissionRequested(remotePeerId: String?, fileName: String?, isPrivate: Boolean) {}
    fun processFilePermissionResponded(context: Context?, remotePeerId: String?, fileName: String?, isPermitted: Boolean) {
        if (isPermitted) {
            val log = "Sending file"
            Utils.toastLog("FileTransfer", context, log)
        } else {
            val log = "Sorry, the remote peer has not granted permission for file transfer"
            Utils.toastLog("FileTransfer", context, log)
        }
    }

    fun processFileDropped(context: Context?, remotePeerId: String?, fileName: String?, message: String, isExplicit: Boolean) {
        val log = "The file transfer was dropped.\nReason : $message"
        Utils.toastLogLong("FileTransfer", context, log)
    }

    open fun processFileSentCompleted(context: Context?, remotePeerId: String?, fileName: String?) {}
    open fun processFileReceivedCompleted(remotePeerId: String?, fileName: String?) {}
    open fun processFileSentProgressed(context: Context?, remotePeerId: String?, fileName: String?, percentage: Double) {}
    open fun processFileReceivedProgressed(context: Context?, remotePeerId: String?, fileName: String?, percentage: Double) {}

    //----------------------------------------------------------------------------------------------
    // Methods which are from MessagesListener need to be implemented for chat function
    //----------------------------------------------------------------------------------------------
    open fun processServerMessageReceived(remotePeerId: String?, message: Any?, isPrivate: Boolean, timeStamp: Long?) {}
    open fun processP2PMessageReceived(remotePeerId: String?, message: Any?, isPrivate: Boolean) {}
    open fun processMessageSendFailed() {}

    //----------------------------------------------------------------------------------------------
    // Methods which are from RecordingListener need to be implemented for recording (in Multi Video function)
    //----------------------------------------------------------------------------------------------
    fun processRecordingStarted(context: Context?, recording: Boolean) {
        val log = "[SRS][SA] Recording Started! isRecording=" +
                recording + "."
        Utils.toastLogLong("MultiVideos", context, log)
    }

    fun processRecordingStopped(context: Context?, recording: Boolean) {
        val log = "[SRS][SA] Recording Stopped! isRecording=" +
                recording + "."
        Utils.toastLogLong("MultiVideos", context, log)
    }

    fun processRecordingError(context: Context?, recordingId: String?, errorCode: Int, description: String) {
        val log = "[SRS][SA] Received Recording error with errorCode:" + errorCode +
                "! Error: " + description
        Utils.toastLogLong("MultiVideos", context, log)
        Log.e("MultiVideos", log)
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from StatsListener need to be implemented for stats (in Multi Video function)
    //----------------------------------------------------------------------------------------------
    fun processTransferSpeedReceived(transferSpeed: Double, remotePeer: String, forSending: Boolean, context: Context?) {
        val log: String
        log = if (!forSending) {
            val direction = "Recv"
            "[SA][TransSpeed] Transfer speed $direction from Peer $remotePeer: $transferSpeed kbps"
        } else {
            val direction = "Send"
            "[SA][TransSpeed] Transfer speed $direction to Peer $remotePeer: $transferSpeed kbps"
        }
        // Log the transfer speeds.
        Log.d("MultiVideos", log)
        Utils.toastLog("MultiVideos", context, log)
    }

    fun processWebrtcStatsReceived(stats: HashMap<String?, String?>) {
        // Log the WebRTC stats.
        val log = StringBuilder("""[SA][WStatsRecv] Received stats: :
 """)
        for ((key, value) in stats) {
            log.append(key).append(": ").append(value).append(".\r\n")
        }
        Log.d("MultiVideos", log.toString())
    }
    //----------------------------------------------------------------------------------------------
    // Methods which is required from AudioRouter need to be implemented for audio/video
    //----------------------------------------------------------------------------------------------
    /**
     * Update UI with the audio output state changed
     * User can manually change audio output in the UI or change the default setting
     * in case of changing the state in default setting, the UI need to update corresponding
     * with the setting state.
     *
     * @param isSpeakerOn the state of audio speaker on/off
     */
    open fun processAudioOutputChanged(isSpeakerOn: Boolean) {}
    open fun processLocalAudioCaptured(localAudio: SkylinkMedia?) {}
    open fun processLocalCameraCaptured(localVideo: SkylinkMedia?) {}
    open fun processLocalScreenCaptured(localVideo: SkylinkMedia?) {}
    open fun processMediaStateChanged(media: SkylinkMedia?, isLocal: Boolean) {}
    open fun processRoomLockStatusChanged(roomLocked: Boolean) {}
    open fun processStoredMessagesResult(storedMessages: JSONArray?) {}
}