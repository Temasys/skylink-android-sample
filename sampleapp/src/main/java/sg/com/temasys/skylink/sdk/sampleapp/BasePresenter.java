package sg.com.temasys.skylink.sdk.sampleapp;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

import java.util.HashMap;
import java.util.Map;

import sg.com.temasys.skylink.sdk.rtc.Info;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
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

    public void onServiceRequestRemotePeerConnectionRefreshed(String log, UserInfo remotePeerUserInfo) {
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from MediaListener need to be implemented for audio and video
    //----------------------------------------------------------------------------------------------

    public void onServiceRequestLocalMediaCapture(SurfaceViewRenderer videoView) {
    }

    public void onServiceRequestRemotePeerAudioReceive(String log, UserInfo remotePeerUserInfo, String remotePeerId) {
    }

    public void onServiceRequestRemotePeerVideoReceive(String log, UserInfo remotePeerUserInfo, String remotePeerId) {
    }

    public void onServiceRequestInputVideoResolutionObtained(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
    }

    public void onServiceRequestReceivedVideoResolutionObtained(String peerId, int width, int height, int fps) {
    }

    public void onServiceRequestSentVideoResolutionObtained(String peerId, int width, int height, int fps) {
    }

    public void onServiceRequestVideoSizeChange(String peerId, Point size) {
        String peer = "Peer " + peerId;
        // If peerId is null, this call is for our local video.
        if (peerId == null) {
            peer = "We've";
        }
        Log.d("VideoCall", peer + " got video size changed to: " + size.toString() + ".");
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from OsListener need to be implemented for audio, video, fileTransfer, multiVideo functions
    //----------------------------------------------------------------------------------------------

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
        toastLogLong("MultiPartyVideoCall", context, log);
    }

    public void onServiceRequestRecordingStop(Context context, boolean recording) {
        String log = "[SRS][SA] Recording Stopped! isRecording=" +
                recording + ".";
        toastLogLong("MultiPartyVideoCall", context, log);
    }

    public void onServiceRequestRecordingVideoLink(String recordingId, String peerId, String videoLink) {
    }

    public void onServiceRequestRecordingError(Context context, String recordingId, int errorCode, String description) {
        String log = "[SRS][SA] Received Recording error with errorCode:" + errorCode +
                "! Error: " + description;
        toastLogLong("MultiPartyVideoCall", context, log);
        Log.e("MultiPartyVideoCall", log);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from StatsListener need to be implemented for stats (in Multi Video function)
    //----------------------------------------------------------------------------------------------

    public void onServiceRequestTransferSpeedReceived(String peerId, int mediaDirection, int mediaType, double transferSpeed) {
        String direction = "Send";
        if (Info.MEDIA_DIRECTION_RECV == mediaDirection) {
            direction = "Recv";
        }
        // Log the transfer speeds.
        String log = "[SA][TransSpeed] Transfer speed for Peer " + peerId + ": " +
                Info.getInfoString(mediaType) + " " + direction + " = " + transferSpeed + " kbps";
        Log.d("MultiPartyVideoCall", log);
    }

    public void onServiceRequestWebrtcStatsReceived(String peerId, int mediaDirection, int mediaType, HashMap<String, String> stats) {
        // Log the WebRTC stats.
        StringBuilder log =
                new StringBuilder("[SA][WStatsRecv] Received for Peer " + peerId + ":\r\n");
        for (Map.Entry<String, String> entry : stats.entrySet()) {
            log.append(entry.getKey()).append(": ").append(entry.getValue()).append(".\r\n");
        }
        Log.d("MultiPartyVideoCall", log.toString());
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


}
