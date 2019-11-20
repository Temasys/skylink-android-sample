package sg.com.temasys.skylink.sdk.sampleapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

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

    public void onServiceRequestConnect(boolean isSuccessful) {
    }

    public void onServiceRequestDisconnect() {
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from RemotePeerListener need to be implemented for all functions
    //----------------------------------------------------------------------------------------------

    public void onServiceRequestRemotePeerJoin(SkylinkPeer newPeer) {
    }

    public void onServiceRequestRemotePeerLeave(SkylinkPeer remotePeer, int removeIndex) {
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from MediaListener need to be implemented for audio and video
    //----------------------------------------------------------------------------------------------

    public void onServiceRequestRemotePeerAudioReceive(String remotePeerId) {
    }

    public void onServiceRequestRemotePeerVideoReceive(String remotePeerId, SkylinkMedia remoteVideo) {
    }

    public void onServiceRequestInputVideoResolutionObtained(SkylinkMedia.MediaType mediaType, int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
    }

    public void onServiceRequestReceivedVideoResolutionObtained(String peerId, SkylinkMedia.MediaType mediaType, int width, int height, int fps) {
    }

    public void onServiceRequestSentVideoResolutionObtained(String peerId, SkylinkMedia.MediaType mediaType, int width, int height, int fps) {
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from OsListener need to be implemented for audio, video, fileTransfer, multiVideo functions
    //----------------------------------------------------------------------------------------------

    public void onServiceRequestIntentRequired(Intent intent, int requestCode, SkylinkInfo skylinkInfo) {
    }

    public void onServiceRequestPermissionGranted(Intent intent, int requestCode, SkylinkInfo skylinkInfo) {
        PermissionUtils.onPermissionGrantedHandler(requestCode, skylinkInfo, true);
    }

    public void onServiceRequestPermissionDenied(Intent intent, int requestCode, SkylinkInfo skylinkInfo) {
        PermissionUtils.onPermissionGrantedHandler(requestCode, skylinkInfo, false);
    }

    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
    }

    public void onServiceRequestPermissionGranted(PermRequesterInfo info) {
        PermissionUtils.onPermissionGrantedHandler(info);
    }

    public void onServiceRequestPermissionDenied(Context context, PermRequesterInfo info) {
        PermissionUtils.onPermissionDeniedHandler(info, context);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from DataTransferListener need to be implemented for dataTransfer function
    //----------------------------------------------------------------------------------------------

    public void onServiceRequestDataReceive(Context context, String remotePeerId, byte[] data) {
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from FileTransferListener need to be implemented for fileTransfer function
    //----------------------------------------------------------------------------------------------

    public void onServiceRequestFileTransferPermissionRequest(String remotePeerId, String fileName, boolean isPrivate) {
    }

    public void onServiceRequestFileTransferPermissionResponse(Context context, String remotePeerId, String fileName, boolean isPermitted) {
        if (isPermitted) {
            String log = "Sending file";
            toastLog("FileTransfer", context, log);
        } else {
            String log = "Sorry, the remote peer has not granted permission for file transfer";
            toastLog("FileTransfer", context, log);
        }
    }

    public void onServiceRequestFileTransferDrop(Context context, String remotePeerId, String fileName, String message, boolean isExplicit) {
        String log = "The file transfer was dropped.\nReason : " + message;
        toastLogLong("FileTransfer", context, log);
    }

    public void onServiceRequestFileSendComplete(Context context, String remotePeerId, String fileName) {
    }

    public void onServiceRequestFileReceiveComplete(String remotePeerId, String fileName) {
    }

    public void onServiceRequestFileSendProgress(Context context, String remotePeerId, String fileName, double percentage) {
    }

    public void onServiceRequestFileReceiveProgress(Context context, String remotePeerId, String fileName, double percentage) {
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from MessagesListener need to be implemented for chat function
    //----------------------------------------------------------------------------------------------

    public void onServiceRequestServerMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
    }

    public void onServiceRequestP2PMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from RecordingListener need to be implemented for recording (in Multi Video function)
    //----------------------------------------------------------------------------------------------

    public void onServiceRequestRecordingStart(Context context, boolean recording) {
        String log = "[SRS][SA] Recording Started! isRecording=" +
                recording + ".";
        toastLogLong("MultiVideos", context, log);
    }

    public void onServiceRequestRecordingStop(Context context, boolean recording) {
        String log = "[SRS][SA] Recording Stopped! isRecording=" +
                recording + ".";
        toastLogLong("MultiVideos", context, log);
    }

    public void onServiceRequestRecordingError(Context context, String recordingId, int errorCode, String description) {
        String log = "[SRS][SA] Received Recording error with errorCode:" + errorCode +
                "! Error: " + description;
        toastLogLong("MultiVideos", context, log);
        Log.e("MultiVideos", log);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from StatsListener need to be implemented for stats (in Multi Video function)
    //----------------------------------------------------------------------------------------------

    public void onServiceRequestTransferSpeedReceived(double transferSpeed, String remotePeerId, boolean forSending, Context context) {

        String log;
        if (!forSending) {
            String direction = "Recv";
            log = "[SA][TransSpeed] Transfer speed " + direction + " from Peer " + remotePeerId + ": " + transferSpeed + " kbps";
        } else {
            String direction = "Send";
            log = "[SA][TransSpeed] Transfer speed " + direction + " to Peer " + remotePeerId + ": " + transferSpeed + " kbps";
        }
        // Log the transfer speeds.

        Log.d("MultiVideos", log);
        toastLog("MultiVideos", context, log);
    }

    public void onServiceRequestWebrtcStatsReceived(HashMap<String, String> stats) {
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
    public void onServiceRequestAudioOutputChanged(boolean isSpeakerOn) {
    }

    public void onServiceRequestLocalAudioCapture(SkylinkMedia localAudio) {
    }

    public void onServiceRequestLocalCameraCapture(SkylinkMedia localVideo) {
    }

    public void onServiceRequestLocalScreenCapture(SkylinkMedia localVideo) {
    }

    public void onServiceRequestMediaStateChange(SkylinkMedia media, boolean isLocal) {
    }

    public void onServiceRequestChangeRoomLockStatus(boolean roomLockStatus) {
    }
}
