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
import sg.com.temasys.skylink.sdk.listener.StatsListener;
import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.Info;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
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
        DataTransferListener, FileTransferListener, RecordingListener, StatsListener {

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
    public void onConnect(boolean isSuccessful, String message) {
        String log = "[onConnect] ";
        if (isSuccessful) {
            String localPeerId = mSkylinkConnection.getPeerId();

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

        } else {
            log += "Skylink failed to connect!\nReason : " + message;
            toastLog(TAG, context, log);
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
        toastLogLong(TAG, context, log);

        //update UI to disconnected state for all types call
        presenter.onServiceRequestDisconnect();

        clearInstance();
    }

    /**
     * This is triggered from SkylinkSDK when the lock status of the room that we are in changes
     */
    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        String log = "[onLockRoomStatusChange] ";
        log += "[SA] Peer " + remotePeerId + " changed Room locked status to "
                + lockStatus + ".";
        toastLog(TAG, context, log);
    }

    /**
     * This method is triggered from SkylinkSDK to inform that changes in room lock
     */
    @Override
    public void onReceiveLog(int infoCode, String message) {
//        Utils.handleSkylinkReceiveLog(infoCode, message, context, TAG);
    }

    /**
     * This is triggered from SkylinkSDK to deliver messages that might be useful to the user.
     */
    @Override
    public void onWarning(int errorCode, String message) {
        Utils.handleSkylinkWarning(errorCode, message, context, TAG);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from MediaListener need to be implemented for audio and video functions
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered from SkylinkSDK when successfully captures the local device's camera input.
     * This may be at the start of video usage or when video source has changed.
     * It can happen only if the connection is configured to have a video call.
     *
     * @param localAudio the local audio object
     */
    @Override
    public void onLocalAudioCapture(SkylinkMedia localAudio) {
        Log.d(TAG, "[onLocalMediaCapture]");

        if (localAudio == null)
            return;

        presenter.onServiceRequestLocalAudioCapture(localAudio);

        localAudioId = localAudio.getMediaId();
    }

    @Override
    public void onLocalVideoCapture(SkylinkMedia localVideo) {
        Log.d(TAG, "[onLocalMediaCapture]");

        if (localVideo == null)
            return;

        if (localVideo.getMediaType() == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            presenter.onServiceRequestLocalCameraCapture(localVideo);
            localVideoId = localVideo.getMediaId();
        } else if (localVideo.getMediaType() == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            presenter.onServiceRequestLocalScreenCapture(localVideo);
            localScreenSharingId = localVideo.getMediaId();
        }
    }

    /**
     * This is triggered from SkylinkSDK when a peer enable / disable its video.
     *
     * @param peerId The id of the peer (can be selfPeer or remote peer)
     * @param media  The media object with new state
     */
    @Override
    public void onMediaStateChange(String peerId, SkylinkMedia media) {
        String log = "[SA][onMediaStateChange] ";
        log += "Peer " + getPeerIdNick(peerId) +
                " Media (" + media.getMediaId() + ") state changed status via:\r\nCallback: " + media.getMediaState() + ".";

        toastLog(TAG, context, log);

        if (mPeersList != null && mPeersList.size() > 1 && !mPeersList.get(0).getPeerId().equals(peerId)) {
            presenter.onServiceRequestMediaStateChange(media, false);
        } else {
            presenter.onServiceRequestMediaStateChange(media, true);

            // process remove local media id for the peer(s) if media state is UNAVAILABLE
            if (media.getMediaState() == SkylinkMedia.MediaState.UNAVAILABLE) {
                if (mPeersList != null && mPeersList.size() > 0) {
                    for (int i = 0; i < mPeersList.size(); i++) {
                        mPeersList.get(i).removeMediaId(media.getMediaId());
                    }
                }

                // remove local media id
                if (media.getMediaType() == SkylinkMedia.MediaType.AUDIO_MIC) {
                    localAudioId = null;
                } else if (media.getMediaType() == SkylinkMedia.MediaType.VIDEO_CAMERA) {
                    localVideoId = null;
                } else if (media.getMediaType() == SkylinkMedia.MediaType.VIDEO_SCREEN) {
                    localScreenSharingId = null;
                }
            }
        }
    }

    /**
     * This is triggered from SkylinkSDK upon receiving the audio track of the remote peer
     * if the connection is configured to have an audio call.
     * This will only be triggered after onRemotePeerJoin has triggered.
     *
     * @param remotePeerId The id of the peer
     */
    @Override
    public void onRemotePeerAudioReceive(String remotePeerId, SkylinkMedia remoteAudio) {
        String log = "[onRemotePeerAudioReceive] ";
        log += "Received new Audio (" + remoteAudio.getMediaId() + ") from Peer " + getPeerIdNick(remotePeerId) + ".\r\n";

        UserInfo remotePeerUserInfo = getUserInfo(remotePeerId);

        // add audio id for the peer
        addPeerMedia(remotePeerId, remoteAudio.getMediaId(), SkylinkMedia.MediaType.AUDIO_MIC);

        presenter.onServiceRequestRemotePeerAudioReceive(log, remotePeerUserInfo, remotePeerId, remoteAudio);
    }

    /**
     * This is triggered from SkylinkSDK upon receiving the video view of the remote peer
     * if the connection is configured to have a video call.
     * This will only be triggered after onRemotePeerJoin has triggered.
     *
     * @param remotePeerId The id of the remote peer
     * @param remoteMedia  the received video object, can be video or screen share
     */
    @Override
    public void onRemotePeerVideoReceive(String remotePeerId, SkylinkMedia remoteMedia) {
        if (remoteMedia == null)
            return;

        String log = "[onRemotePeerVideoReceive] ";
        log += "Received new Video (" + remoteMedia.getMediaId() + ") from Peer " + getPeerIdNick(remotePeerId) + ".\r\n";

        if (remoteMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_CAMERA || remoteMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO) {
            addPeerMedia(remotePeerId, remoteMedia.getMediaId(), SkylinkMedia.MediaType.VIDEO_CAMERA);
        } else if (remoteMedia.getMediaType() == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            addPeerMedia(remotePeerId, remoteMedia.getMediaId(), SkylinkMedia.MediaType.VIDEO_SCREEN);
        }

        presenter.onServiceRequestRemotePeerVideoReceive(log, remotePeerId, remoteMedia);
    }

    /**
     * This is triggered from SkylinkSDK when a local camera starts to capture video,
     * if {@link SkylinkConfig#isReportVideoResolutionOnVideoChange()} is true;
     * or when its currently reporting and video resolution is not yet stable,
     * if {@link SkylinkConfig#isReportVideoResolutionUntilStable()} is true;
     * It can also be triggered by calling {@link SkylinkConnection#getInputVideoResolutionByVideoId(String videoId)}.
     * The current resolution of the video being captured by the local camera
     * and the SkylinkCaptureFormat used will be provided.
     * Note: This might be different from the resolution of the video actually sent to Peers as
     * WebRTC may adjust resolution dynamically to try to match its bandwidth criteria.
     * If a particular value is not available, , e.g. if video is not capturing,
     * a negative integer or a null value will be provided.
     *
     * @param videoId       Id of the local video object
     * @param width         Video width in pixels of local captured video.
     * @param height        Video height in pixels of local captured video.
     * @param fps           Video frame per second of local captured video.
     * @param captureFormat The SkylinkCaptureFormat that is currently used by the camera.
     */
    @Override
    public void onInputVideoResolutionObtained(String videoId, int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        Log.d(TAG, "[onInputVideoResolutionObtained]");

        SkylinkMedia.MediaType mediaType = null;
        // get the video type from videoId
        if (videoId.equals(localVideoId)) {
            mediaType = SkylinkMedia.MediaType.VIDEO_CAMERA;
        } else if (videoId.equals(localScreenSharingId)) {
            mediaType = SkylinkMedia.MediaType.VIDEO_SCREEN;
        }

        if (videoResPresenter != null)
            videoResPresenter.onServiceRequestInputVideoResolutionObtained(mediaType, width, height, fps, captureFormat);
    }

    /**
     * This is triggered from SkylinkSDK when we receive a remote Peer's video,
     * if {@link SkylinkConfig#isReportVideoResolutionOnVideoChange()} is true;
     * or when its currently reporting and video resolution is not yet stable,
     * if {@link SkylinkConfig#isReportVideoResolutionUntilStable()} is true.
     * It can also be triggered by calling {@link SkylinkConnection#getReceivedVideoResolutionByVideoId}.
     * The current resolution of the video received from a specific Peer will be provided.
     * If a particular value is not available, a negative integer will be provided.
     *
     * @param peerId  PeerId of the remote Peer whose Video Resolution we are obtaining.
     * @param videoId id of the remote video that video resolutionc comes from
     * @param width   Video width in pixels of remote Peer's video received by us.
     * @param height  Video height in pixels of remote Peer's video received by us.
     * @param fps     Video frame per second of remote Peer's video received by us.
     */
    @Override
    public void onReceivedVideoResolutionObtained(String peerId, String videoId, int width, int height, int fps) {
        Log.d(TAG, "[onReceivedVideoResolutionObtained]");

        SkylinkMedia.MediaType mediaType = null;

        for (SkylinkPeer peer : mPeersList) {
            if (peer.getPeerId().equals(peerId)) {
                Map<String, SkylinkMedia.MediaType> remoteMedia = peer.getMediaIds();
                if (remoteMedia != null && remoteMedia.size() > 0) {
                    mediaType = remoteMedia.get(videoId);
                    break;
                }
            }
        }

        if (videoResPresenter != null && mediaType != null)
            videoResPresenter.onServiceRequestReceivedVideoResolutionObtained(peerId, mediaType, width, height, fps);
    }

    /**
     * This is triggered from SkylinkSDK when a local video stream starts being sent to a remote Peer,
     * if {@link SkylinkConfig#isReportVideoResolutionOnVideoChange()} is true;
     * or when its currently reporting and video resolution is not yet stable,
     * if {@link SkylinkConfig#isReportVideoResolutionUntilStable()} is true;
     * It can also be triggered by calling {@link SkylinkConnection#getSentVideoResolutionByVideoId}.
     * The current resolution of the video being sent to a specific Peer will be provided.
     *
     * @param peerId  PeerId of the remote Peer whom we are obtaining Video Resolution
     *                for the video we are sending.
     * @param videoId id of the video that video resolution comes from
     * @param width   Video width in pixels of local video sent to remote Peer.
     * @param height  Video height in pixels of local video sent to remote Peer.
     * @param fps     Video frame per second of local video sent to remote Peer.
     */
    @Override
    public void onSentVideoResolutionObtained(String peerId, String videoId, int width, int height, int fps) {
        Log.d(TAG, "[onSentVideoResolutionObtained]");

        SkylinkMedia.MediaType mediaType = null;
        // get the video type from videoId
        if (videoId.equals(localVideoId)) {
            mediaType = SkylinkMedia.MediaType.VIDEO_CAMERA;
        } else if (videoId.equals(localScreenSharingId)) {
            mediaType = SkylinkMedia.MediaType.VIDEO_SCREEN;
        }

        if (videoResPresenter != null)
            videoResPresenter.onServiceRequestSentVideoResolutionObtained(peerId, mediaType, width, height, fps);
    }

    /**
     * This is triggered from SkylinkSDK when any of the given video streams' frame size changes. It includes the
     * self stream also.
     *
     * @param peerId  The id of the peer. If null, it indicates self stream.
     * @param videoId Id of the video
     * @param size    Size of the video frame
     */
    @Override
    public void onVideoSizeChange(String peerId, String videoId, Point size) {
        Log.d(TAG, "[onVideoSizeChange]");

        presenter.onServiceRequestVideoSizeChange(peerId, videoId, size);
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
     * {@link #onPermissionGranted} or {@link #onPermissionDenied}.
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
    public void onIntentRequired(Intent intent, int requestCode, int infoCode) {
        Log.d(TAG, "[onIntentRequired]");
        presenter.onServiceRequestIntentRequired(intent, requestCode, infoCode);
    }

    /**
     * This is triggered when Android Intent based permission has been granted for the usage of
     * a particular part of the SDK.
     *
     * @param intent      As that in {@link #onIntentRequired(Intent, int, int)}.
     * @param requestCode As that in {@link #onIntentRequired(Intent, int, int)}.
     * @param infoCode    As that in {@link #onIntentRequired(Intent, int, int)}.
     */
    public void onPermissionGranted(Intent intent, int requestCode, int infoCode) {
        Log.d(TAG, "[onIntentPermissionGranted]");
        presenter.onServiceRequestPermissionGranted(intent, requestCode, infoCode);
    }

    /**
     * This is triggered when Android Intent based permission has been denied for the usage of
     * a particular part of the SDK.
     *
     * @param intent      As that in {@link #onIntentRequired(Intent, int, int)}.
     * @param requestCode As that in {@link #onIntentRequired(Intent, int, int)}.
     * @param infoCode    As that in {@link #onIntentRequired(Intent, int, int)}.
     */
    public void onPermissionDenied(Intent intent, int requestCode, int infoCode) {
        Log.d(TAG, "[onIntentPermissionDenied]");
        presenter.onServiceRequestPermissionDenied(intent, requestCode, infoCode);
    }

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

        presenter.onServiceRequestPermissionDenied(context, info);
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
        toastLog(TAG, context, logTag);
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
        toastLog(TAG, context, log);
    }

    /**
     * This is triggered when the underlying peer to peer data connection is established with a
     * remote peer and is ready to send and receive peer messages and data (including files).
     *
     * @param remotePeerId The id of the remote peer
     */
    @Override
    public void onOpenDataConnection(String remotePeerId) {
        Log.d(TAG, "[SA][onOpenDataConnection] " + remotePeerId + ".");
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
                    message + ". " + numRemotePeers + " remote Peer(s) left in the room.";
            toastLog(TAG, context, log);
        }
    }

    /**
     * This is triggered when having fail connection with a remote peer
     * When user get this callback, they can reconnect or refresh the connection with the remote peer,
     * or simply ignore this peer
     *
     * @param remotePeerId The id of the peer
     * @param userInfo     User info of the remote peer
     * @param errorCode    the error code for fail connection
     */
    @Override
    public void onRemotePeerConnectionFailed(String remotePeerId, UserInfo userInfo, int errorCode) {
        String log = "Your connection with the peer (" + remotePeerId + ") is NOT successful. " +
                "ErrorCode = " + errorCode;
        toastLog(TAG, context, log);
        Log.e(TAG, log);
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

        presenter.onServiceRequestFileTransferPermissionResponse(context, remotePeerId, fileName, isPermitted);
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

        presenter.onServiceRequestFileTransferDrop(context, remotePeerId, fileName, message, isExplicit);
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

        presenter.onServiceRequestFileSendComplete(context, remotePeerId, fileName);
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
    public void onFileReceiveProgress(String remotePeerId, String fileName, double percentage) {
        Log.d(TAG, "[onFileReceiveProgress]");

        presenter.onServiceRequestFileReceiveProgress(context, remotePeerId, fileName, percentage);
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

        presenter.onServiceRequestRecordingStart(context, mSkylinkConnection.isRecording());
    }

    /**
     * This is triggered to indicate that recording has stopped for this room.
     *
     * @param recordingId Id of Recording session this message is about.
     */
    @Override
    public void onRecordingStop(String recordingId) {
        Log.d(TAG, "[onRecordingStop]");

        presenter.onServiceRequestRecordingStop(context, mSkylinkConnection.isRecording());
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

        presenter.onServiceRequestRecordingError(context, recordingId, errorCode, description);
    }

    //----------------------------------------------------------------------------------------------
    // Methods which are from StatsListener need to be implemented for stats (in Multi Video function)
    //----------------------------------------------------------------------------------------------

    /**
     * This is triggered when the instantaneous transfer speed of a media stream,
     * at the moment of request, is available.
     *
     * @param peerId         PeerId of the remote Peer for which we are getting stats on.
     * @param mediaId        Media id of the SkylinkMedia object that transfer speed comes from
     * @param mediaDirection Integer that defines the direction of media stream(s) reported on
     * @param mediaType      Integer that defines the type(s) of media reported on
     * @param transferSpeed  Transfer speed in kilobit per second (kbps).
     */
    @Override
    public void onTransferSpeedReceived(String peerId, String mediaId, int mediaDirection, int mediaType, double transferSpeed) {
        Log.d(TAG, "[onTransferSpeedReceived]");

        presenter.onServiceRequestTransferSpeedReceived(peerId, mediaId, mediaDirection, mediaType, transferSpeed);
    }

    /**
     * This is triggered when WebRTC statistics of the specified media stream are available.
     *
     * @param peerId         PeerId of the remote Peer for which we are getting stats on.
     * @param mediaId        id of the media object
     * @param mediaDirection Integer that defines the direction of media stream(s) reported on
     * @param mediaType      Integer that defines the type(s) of media reported on
     * @param stats          Map containing WebRTC provided statistics on the specified media.
     */
    @Override
    public void onWebrtcStatsReceived(String peerId, String mediaId, int mediaDirection, int mediaType, HashMap<String, String> stats) {
        Log.d(TAG, "[onWebrtcStatsReceived]");

        presenter.onServiceRequestWebrtcStatsReceived(peerId, mediaDirection, mediaType, mediaId, stats);
    }

    //----------------------------------------------------------------------------------------------
    // Public methods helps to work with SkylinkSDK
    //----------------------------------------------------------------------------------------------

    protected void initializeSkylinkConnection(Constants.CONFIG_TYPE typeCall) {
        this.mSkylinkConnection = skylinkConnectionManager.initializeSkylinkConnection(typeCall);
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
        skylinkConnectionManager.connectToRoom(typeCall);
    }


    public boolean disconnectFromRoom() {
        return skylinkConnectionManager.disconnectFromRoom();
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
        if (peerIdList == null || peerIdList.length == 0) {
            return null;
        }

        // Ensure index does not exceed range of possible indices on peerIdList.
        if (index > peerIdList.length - 1 || index < 0) {
            return null;
        }
        return peerIdList[index];
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
        String mainVideoId = getProperVideoId(mediaType);

        mSkylinkConnection.setInputVideoResolutionByVideoId(mainVideoId, width, height, fps);
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
            return mSkylinkConnection.getCaptureFormats(videoDevice);
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
            SkylinkCaptureFormat captureFormat = mSkylinkConnection.getCaptureFormat();


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
     * {@link SkylinkCommonService#onInputVideoResolutionObtained} for input video resolution
     * {@link SkylinkCommonService#onReceivedVideoResolutionObtained} for received video resolution
     * {@link SkylinkCommonService#onSentVideoResolutionObtained} for sent video resolution
     *
     * @param mediaType the type of the video (video or screen)
     * @param peerId    id of the peer that video belongs to
     */
    public void getVideoResolutions(SkylinkMedia.MediaType mediaType, String peerId) {
        if (mSkylinkConnection == null)
            return;

        String mainVideoId = getProperVideoId(mediaType);

        mSkylinkConnection.getInputVideoResolutionByVideoType(mediaType);

        if (peerId != null) {
            mSkylinkConnection.getSentVideoResolutionByVideoType(peerId, mediaType);
            mSkylinkConnection.getReceivedVideoResolutionByVideoType(peerId, mediaType);
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
    private String getProperVideoId(SkylinkMedia.MediaType mediaType) {
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
