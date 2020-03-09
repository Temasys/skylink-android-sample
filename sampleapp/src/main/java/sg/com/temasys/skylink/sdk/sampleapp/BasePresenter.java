package sg.com.temasys.skylink.sdk.sampleapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkInfo;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 * <p>
 * A common abstract class for all presenters.
 * This class defined all methods which responsible for updating GUI requested by the SDK.
 * Some of those which do not need to be override in the concrete classes can be
 * implemented in the BasePresenter (such as just displaying toast to inform changes to user)
 */

public abstract class BasePresenter {

    //----------------------------------------------------------------------------------------------
    // Methods which are from LifeCycleListener need to be implemented for all functions
    //----------------------------------------------------------------------------------------------

    public void processRoomConnected(boolean isSuccessful) {
    }

    public void processRoomDisconnected() {
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from RemotePeerListener need to be implemented for all functions
    //----------------------------------------------------------------------------------------------

    public void processRemotePeerConnected(SkylinkPeer newPeer) {
    }

    public void processRemotePeerDisconnected(SkylinkPeer remotePeer, int removeIndex) {
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from MediaListener need to be implemented for audio and video
    //----------------------------------------------------------------------------------------------

    public void processRemoteAudioReceived(String remotePeerId) {
    }

    public void processRemoteVideoReceived(String remotePeerId, SkylinkMedia remoteVideo) {
    }

    public void processInputVideoResolutionObtained(SkylinkMedia.MediaType mediaType, int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
    }

    public void processReceivedVideoResolutionObtained(String peerId, SkylinkMedia.MediaType mediaType, int width, int height, int fps) {
    }

    public void processSentVideoResolutionObtained(String peerId, SkylinkMedia.MediaType mediaType, int width, int height, int fps) {
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from OsListener need to be implemented for audio, video, fileTransfer, multiVideo functions
    //----------------------------------------------------------------------------------------------

    public void processIntentRequired(Intent intent, int requestCode, SkylinkInfo skylinkInfo) {
    }

    public void processPermissionGranted(Intent intent, int requestCode, SkylinkInfo skylinkInfo) {
        PermissionUtils.onPermissionGrantedHandler(requestCode, skylinkInfo, true);
    }

    public void processPermissionDenied(Intent intent, int requestCode, SkylinkInfo skylinkInfo) {
        PermissionUtils.onPermissionGrantedHandler(requestCode, skylinkInfo, false);
    }

    public void processPermissionRequired(PermRequesterInfo info) {
    }

    public void processPermissionGranted(PermRequesterInfo info) {
        PermissionUtils.onPermissionGrantedHandler(info);
    }

    public void processPermissionDenied(Context context, PermRequesterInfo info) {
        PermissionUtils.onPermissionDeniedHandler(info, context);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from DataTransferListener need to be implemented for dataTransfer function
    //----------------------------------------------------------------------------------------------

    public void processDataReceive(Context context, String remotePeerId, byte[] data) {
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from FileTransferListener need to be implemented for fileTransfer function
    //----------------------------------------------------------------------------------------------

    public void processFilePermissionRequested(String remotePeerId, String fileName, boolean isPrivate) {
    }

    public void processFilePermissionResponded(Context context, String remotePeerId, String fileName, boolean isPermitted) {
        if (isPermitted) {
            String log = "Sending file";
            toastLog("FileTransfer", context, log);
        } else {
            String log = "Sorry, the remote peer has not granted permission for file transfer";
            toastLog("FileTransfer", context, log);
        }
    }

    public void processFileDropped(Context context, String remotePeerId, String fileName, String message, boolean isExplicit) {
        String log = "The file transfer was dropped.\nReason : " + message;
        toastLogLong("FileTransfer", context, log);
    }

    public void processFileSentCompleted(Context context, String remotePeerId, String fileName) {
    }

    public void processFileReceivedCompleted(String remotePeerId, String fileName) {
    }

    public void processFileSentProgressed(Context context, String remotePeerId, String fileName, double percentage) {
    }

    public void processFileReceivedProgressed(Context context, String remotePeerId, String fileName, double percentage) {
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from MessagesListener need to be implemented for chat function
    //----------------------------------------------------------------------------------------------

    public void processServerMessageReceived(String remotePeerId, Object message, boolean isPrivate, Long timeStamp) {
    }

    public void processP2PMessageReceived(String remotePeerId, Object message, boolean isPrivate) {
    }

    public void processMessageSendFailed(){

    }
    //----------------------------------------------------------------------------------------------
    // Methods which are from RecordingListener need to be implemented for recording (in Multi Video function)
    //----------------------------------------------------------------------------------------------

    public void processRecordingStarted(Context context, boolean recording) {
        String log = "[SRS][SA] Recording Started! isRecording=" +
                recording + ".";
        toastLogLong("MultiVideos", context, log);
    }

    public void processRecordingStopped(Context context, boolean recording) {
        String log = "[SRS][SA] Recording Stopped! isRecording=" +
                recording + ".";
        toastLogLong("MultiVideos", context, log);
    }

    public void processRecordingError(Context context, String recordingId, int errorCode, String description) {
        String log = "[SRS][SA] Received Recording error with errorCode:" + errorCode +
                "! Error: " + description;
        toastLogLong("MultiVideos", context, log);
        Log.e("MultiVideos", log);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from StatsListener need to be implemented for stats (in Multi Video function)
    //----------------------------------------------------------------------------------------------

    public void processTransferSpeedReceived(double transferSpeed, String remotePeer, boolean forSending, Context context) {

        String log;
        if (!forSending) {
            String direction = "Recv";
            log = "[SA][TransSpeed] Transfer speed " + direction + " from Peer " + remotePeer + ": " + transferSpeed + " kbps";
        } else {
            String direction = "Send";
            log = "[SA][TransSpeed] Transfer speed " + direction + " to Peer " + remotePeer + ": " + transferSpeed + " kbps";
        }
        // Log the transfer speeds.

        Log.d("MultiVideos", log);
        toastLog("MultiVideos", context, log);
    }

    public void processWebrtcStatsReceived(HashMap<String, String> stats) {
        // Log the WebRTC stats.
        StringBuilder log =
                new StringBuilder("[SA][WStatsRecv] Received stats: " +
                        ":\r\n ");
        for (Map.Entry<String, String> entry : stats.entrySet()) {
            log.append(entry.getKey()).append(": ").append(entry.getValue()).append(".\r\n");
        }
        Log.d("MultiVideos", log.toString());
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
    public void processAudioOutputChanged(boolean isSpeakerOn) {
    }

    public void processLocalAudioCaptured(SkylinkMedia localAudio) {
    }

    public void processLocalCameraCaptured(SkylinkMedia localVideo) {
    }

    public void processLocalScreenCaptured(SkylinkMedia localVideo) {
    }

    public void processMediaStateChanged(SkylinkMedia media, boolean isLocal) {
    }

    public void processRoomLockStatusChanged(boolean roomLocked) {
    }

    public void processStoredMessagesResult(JSONArray storedMessages) {
    }
}
