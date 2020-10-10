package sg.com.temasys.skylink.sdk.sampleapp.video

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startForegroundService
import org.webrtc.SurfaceViewRenderer
import sg.com.temasys.skylink.sdk.rtc.SkylinkInfo
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter
import sg.com.temasys.skylink.sdk.sampleapp.R
import sg.com.temasys.skylink.sdk.sampleapp.service.ScreenCaptureService
import sg.com.temasys.skylink.sdk.sampleapp.service.VideoService
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoContract.MainView
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionPresenter

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for implementing video logic.
 * User can both display video view from camera front/back and video view from screen
 */
class VideoPresenter(private val context: Context) : BasePresenter(), VideoContract.Presenter {
    private val TAG = VideoPresenter::class.java.name

    // The view instances for remote peer screen share view and remote peer camera view
    var mainVideoView: MainView? = null

    // The service instance
    private val videoService: VideoService

    // The video resolution presenter to implement video resolutions
    private var videoResPresenter: VideoResolutionPresenter? = null

    // Utils to process permission
    private val permissionUtils: PermissionUtils

    // the current speaker output {speaker/headset}
    private var currentVideoSpeaker = Utils.isDefaultSpeakerSettingForVideo()
    fun setMainView(view: MainView?) {
        mainVideoView = view
        mainVideoView!!.setPresenter(this)
    }

    /**
     * inject the video resolution presenter into this presenter
     * in order to let video resolution presenter handles the video resolution logic
     * both this presenter and video resolution presenter use the video call service
     */
    fun setVideoResPresenter(videoResolutionPresenter: VideoResolutionPresenter?) {
        videoResPresenter = videoResolutionPresenter
        videoResPresenter!!.setService(videoService)
        videoService.setResPresenter(videoResPresenter!!)
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for view to call
    // These methods are responsible for processing requests from view
    //----------------------------------------------------------------------------------------------
    override fun processConnectedLayout() {
        Log.d(TAG, "onViewLayoutRequested")

        //connect to room on Skylink connection
        processConnectToRoom()

        //get default audio output settings
//        mainView.updateUIAudioOutputChanged(this.currentVideoSpeaker);

        //after connected to skylink SDK, UI will be updated latter on processRoomConnected
    }

    override fun processResumeState() {

        // do not process if user has not started video from camera
        if (videoService.localVideo == null) {
            return
        }

        // turn back camera to active state
        // we just active local video camera if there is only 1 active camera at current time
        // if there is an active screen, do not ative local camera as it may be intention of the user
        val localVideo = videoService.localVideo
        val localScreen = videoService.localScreen
        if (localVideo != null && localVideo.mediaState == SkylinkMedia.MediaState.STOPPED && (localScreen == null || localScreen != null && localScreen.mediaState == SkylinkMedia.MediaState.UNAVAILABLE)) {
            videoService.toggleVideo(true)
        }
    }

    override fun processPauseState() {
        // stop camera when pausing so that camera will be available for the others to use
        // we just stop local video camera if there is only 1 active camera at current time
        // if there is an active screen, do not stop local camera as it may be intention of the user
        val localVideo = videoService.localVideo
        val localScreen = videoService.localScreen
        if (localVideo != null && (localVideo.mediaState == SkylinkMedia.MediaState.ACTIVE || localVideo.mediaState == SkylinkMedia.MediaState.MUTED)
                && (localScreen == null || localScreen != null && localScreen.mediaState == SkylinkMedia.MediaState.UNAVAILABLE)) {
            videoService.toggleVideo(false)
        }
    }

    override fun processDisconnectFromRoom() {
        videoService.disconnectFromRoom()
    }

    override fun processExit() {
        //process disconnect from room if connecting
        //after disconnected from skylink SDK, UI will be updated latter on processRoomDisconnected
        if (videoService.isConnectingOrConnected) {
            videoService.disconnectFromRoom()
        }

        // need to call disposeLocalMedia to clear all local media objects as disconnectFromRoom no longer dispose local media
        videoService.disposeLocalMedia()
    }

    override fun processChangeAudioState() {
        val localAudio = videoService.localAudio ?: return

        // do not process if user has not started audio

        // mute local audio if its current state is ACTIVE or STOPPED
        // active it if its current state is MUTED
        if (localAudio.mediaState == SkylinkMedia.MediaState.ACTIVE || localAudio.mediaState == SkylinkMedia.MediaState.STOPPED) {
            videoService.muteLocalAudio(true)
        } else if (localAudio.mediaState == SkylinkMedia.MediaState.MUTED) {
            videoService.muteLocalAudio(false)
        }
    }

    override fun processChangeVideoState() {
        val localVideo = videoService.localVideo ?: return

        // do not process if user has not started video from camera

        // mute local video if its current state is ACTIVE or STOPPED
        // active it if its current state is MUTED
        if (localVideo.mediaState == SkylinkMedia.MediaState.ACTIVE || localVideo.mediaState == SkylinkMedia.MediaState.STOPPED) {
            videoService.muteLocalVideo(true)
        } else if (localVideo.mediaState == SkylinkMedia.MediaState.MUTED) {
            videoService.muteLocalVideo(false)
        }
    }

    override fun processChangeScreenState() {
        // do not process if user has not started screen sharing
        val localScreen = videoService.localScreen ?: return

        // mute local screen if its current state is ACTIVE or STOPPED
        // active it if its current state is MUTED
        if (localScreen.mediaState == SkylinkMedia.MediaState.ACTIVE || localScreen.mediaState == SkylinkMedia.MediaState.STOPPED) {
            videoService.muteLocalScreen(true)
        } else if (localScreen.mediaState == SkylinkMedia.MediaState.MUTED) {
            videoService.muteLocalScreen(false)
        }
    }

    override fun processChangeAudioOutput() {
        //change current speakerOn
        currentVideoSpeaker = !currentVideoSpeaker

        // use service layer to change the audio output, update UI will be called later in processAudioOutputChanged
        videoService.changeSpeakerOutput(currentVideoSpeaker)
    }

    override fun processPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // screen share
        if (requestCode == VideoFragment.RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                        context,
                        "Permissions to capture audio granted. Click the button once again.",
                        Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                        context, "Permissions to capture audio denied.",
                        Toast.LENGTH_SHORT
                ).show()
            }
        }

        // delegate to PermissionUtils to process the permissions
        permissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, TAG)
    }

    override fun processActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // screen share
//        if (requestCode == VideoFragment.MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(
                        context,
                        "MediaProjection permission obtained. Foreground service will be started to capture audio.",
                        Toast.LENGTH_SHORT
                ).show()

                val screenCaptureIntent = Intent(context, ScreenCaptureService::class.java).apply {
                    action = ScreenCaptureService.ACTION_START
                    putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data!!)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(context, screenCaptureIntent)
                }

//                setButtonsEnabled(isCapturingAudio = true)
            } else {
                Toast.makeText(
                        context, "Request to obtain MediaProjection denied.",
                        Toast.LENGTH_SHORT
                ).show()
            }
//        }
        //
//        permissionUtils.onRequestActivityResultHandler(requestCode, resultCode, data)

//        // for displaying overlay button permission
//        if (resultCode == Activity.RESULT_OK && data != null) {
//            // send request permission for displaying overlay button
//            if (requestButtonOverlayPermission()) {
//                mainVideoView!!.updateUIShowButtonStopScreenShare()
//            }
//            return
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (Settings.canDrawOverlays(context) && data != null) {
//                mainVideoView!!.updateUIShowButtonStopScreenShare()
//            } else {
//                if (permissionUtils.isSendOverlayAlready) {
//                    permissionUtils.displayOverlayButtonPermissionWarning(context)
//                } else {
//                    requestButtonOverlayPermission()
//                }
//            }
//        }
    }

    /**
     * Get the specific peer object according to the index
     */
    override fun processGetPeerByIndex(index: Int): SkylinkPeer {
        return videoService.getPeerByIndex(index)
    }

    override fun processSwitchCamera() {
        videoService.switchCamera()
    }

    override fun processStartAudio() {
        videoService.createLocalAudio()
    }

    override fun processToggleVideo() {
        // implement start or stop video base on the state of the current video from camera
        val localVideo = videoService.localVideo

        // change local video state to STOPPED if its current state is active or muted
        if (localVideo != null && (localVideo.mediaState == SkylinkMedia.MediaState.ACTIVE || localVideo.mediaState == SkylinkMedia.MediaState.MUTED)) {
            videoService.toggleVideo(false)
        } else if (localVideo == null || localVideo.mediaState == SkylinkMedia.MediaState.STOPPED) {
            videoService.toggleVideo(true)
        }
    }

    override fun processToggleScreen() {
        Log.e("Muoi", "processToggleScreen()...")
        // implement start or stop video base on the state of the current video from camera
        val localScreen = videoService.localScreen

//        startCapturing()

        // change local video state to STOPPED if its current state is active or muted
        if (localScreen != null && (localScreen.mediaState == SkylinkMedia.MediaState.ACTIVE || localScreen.mediaState == SkylinkMedia.MediaState.MUTED)) {
//            videoService.toggleScreen(false)
        } else if (localScreen == null || localScreen.mediaState == SkylinkMedia.MediaState.STOPPED) {
//            videoService.toggleScreen(true)
        }
    }

    override fun processToggleScreen(toActive: Boolean) {
        videoService.toggleScreen(toActive)
    }

    override fun processRemoveAudio() {
        videoService.destroyLocalAudio()
    }

    override fun processRemoveVideo() {
        videoService.destroyLocalVideo()
    }

    override fun processRemoveScreen() {
        videoService.destroyLocalScreen()
    }

    override fun processStartLocalMediaIfConfigAllow() {
        var log = "[SA][processStartLocalMediaIfConfigAllow] "
        if (Utils.isDefaultNoneVideoDeviceSetting()) {
            log += " Default video device setting is No device. So do not start any local media automatically! "
            Log.w(TAG, log)
            return
        }

        // start local audio
        videoService.createLocalAudio()

        // change the audio output base on the default setting
        AudioRouter.setPresenter(this)
        AudioRouter.startAudioRouting(context, Constants.CONFIG_TYPE.AUDIO)
        if (Utils.isDefaultSpeakerSettingForVideo() && AudioRouter.unsupportedHWAECList.contains(Build.MODEL)) {
            // temporary turn off speaker first to avoid echo for deviceS in the black list
            AudioRouter.turnOffSpeaker()
            currentVideoSpeaker = false
        } else {
            // use service layer to change the audio output, update UI will be called later in processAudioOutputChanged
            if (currentVideoSpeaker) {
                AudioRouter.turnOnSpeaker()
            } else {
                AudioRouter.turnOffSpeaker()
            }
        }
        mainVideoView!!.updateUIAudioOutputChanged(currentVideoSpeaker)

        // check the default setting for video device and start local video accordingly
        if (Utils.isDefaultCameraDeviceSetting()) {
            videoService.createLocalVideo()
            return
        }
        if (Utils.isDefaultScreenDeviceSetting()) {
            videoService.createLocalScreen()
            return
        }

        // we create a custom video device from back camera of the device, so start custom video device
        // will similarly start back camera
        if (Utils.isDefaultCustomVideoDeviceSetting()) {
            videoService.createLocalCustomVideo(null)
            return
        }
    }

    override fun processLockRoom() {
        videoService.lockRoom()
    }

    override fun processUnlockRoom() {
        videoService.unlockRoom()
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------
    override fun processRoomConnected(isSuccessful: Boolean) {
        if (isSuccessful) {
            processUpdateStateConnected()
        } else {
            processDisconnectUIChange()
        }
    }

    override fun processRoomDisconnected() {
        processDisconnectUIChange()
    }

    override fun processIntentRequired(intent: Intent?, requestCode: Int, skylinkInfo: SkylinkInfo?) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onIntentRequiredHandler(intent, requestCode, skylinkInfo, context as Activity)
    }

    override fun processPermissionRequired(info: PermRequesterInfo?) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onPermissionRequiredHandler(info, TAG, context, mainVideoView!!.instance)
    }

    override fun processLocalAudioCaptured(localAudio: SkylinkMedia?) {
        Utils.toastLog("[SA][processLocalAudioCaptured]", context, "Local audio is on with id = " + localAudio?.mediaId)

        //notify view to change the UI
        mainVideoView!!.updateUILocalAudioAdded(localAudio?.mediaId)
    }

    override fun processLocalCameraCaptured(localVideo: SkylinkMedia?) {
        var log = "[SA][processLocalCameraCaptured] "
        Utils.toastLog(log, context, "Local video camera is on with id = " + localVideo?.mediaId)
        var selfVideoView = localVideo?.videoView
        if (selfVideoView == null) {
            log += "VideoView is null! Try to get video from SDK"
            Log.w(TAG, log)
            selfVideoView = videoService.getVideoView(localVideo?.mediaId)
        } else {
            log += "Adding VideoView as selfView."
            Log.d(TAG, log)
        }

        //notify view to change the UI
        mainVideoView!!.updateUILocalCameraAdded(localVideo?.mediaId, selfVideoView)
        videoResPresenter!!.processMediaTypeSelected(SkylinkMedia.MediaType.VIDEO_CAMERA)
    }

    override fun processLocalScreenCaptured(localScreen: SkylinkMedia?) {
        var log = "[SA][processLocalScreenCaptured] "
        Utils.toastLog(log, context, "Local video screen is on with id = " + localScreen?.mediaId)
        var selfVideoView = localScreen?.videoView
        if (selfVideoView == null) {
            log += "VideoView is null! Try to get video from SDK"
            Log.w(TAG, log)
            selfVideoView = videoService.getVideoView(localScreen?.mediaId)
        } else {
            log += "Adding VideoView as selfView."
            Log.d(TAG, log)
        }

        //notify view to change the UI
        mainVideoView!!.updateUILocalScreenAdded(localScreen?.mediaId, selfVideoView)
        videoResPresenter!!.processMediaTypeSelected(SkylinkMedia.MediaType.VIDEO_SCREEN)
    }

    override fun processMediaStateChanged(media: SkylinkMedia?, isLocal: Boolean) {
        // change the UI
        mainVideoView!!.updateUIMediaStateChange(media?.mediaType, media?.mediaState, isLocal)

        // stop audio routing when remote audio is unavailable
        if (!isLocal && !(media?.isVideo)!! && media?.mediaState == SkylinkMedia.MediaState.UNAVAILABLE) {
            AudioRouter.stopAudioRouting(context)
        }
    }

    override fun processRemotePeerConnected(remotePeer: SkylinkPeer?) {
        // Fill the new peer in button in custom bar
        mainVideoView!!.updateUIRemotePeerConnected(remotePeer, videoService.totalPeersInRoom - 2)
    }

    override fun processRemotePeerDisconnected(remotePeer: SkylinkPeer?, removeIndex: Int) {
        // do not process if the left peer is local peer
        if (removeIndex == -1 || remotePeer == null) return

        // Remove the peer in button in custom bar
        mainVideoView!!.updateUIRemotePeerDisconnected(videoService.peersList)

        // remove the remote peer video view
        mainVideoView!!.updateUIRemoveRemotePeer()
        videoResPresenter!!.processSentVideoResolutionObtained(remotePeer.peerId, SkylinkMedia.MediaType.VIDEO_CAMERA, -1, -1, -1)
        videoResPresenter!!.processSentVideoResolutionObtained(remotePeer.peerId, SkylinkMedia.MediaType.VIDEO_SCREEN, -1, -1, -1)
        videoResPresenter!!.processReceivedVideoResolutionObtained(remotePeer.peerId, SkylinkMedia.MediaType.VIDEO_CAMERA, -1, -1, -1)
        videoResPresenter!!.processReceivedVideoResolutionObtained(remotePeer.peerId, SkylinkMedia.MediaType.VIDEO_SCREEN, -1, -1, -1)
    }

    override fun processRemoteAudioReceived(remotePeerId: String?) {
        mainVideoView!!.updateUIReceiveRemoteAudio(remotePeerId)

        // Add delay 3 seconds for audio speaker turned on to avoid audio echo if the device model is not supported AEC
        val handler = Handler()
        handler.postDelayed({
            // turn on speaker now as default setting
            if (Utils.isDefaultSpeakerSettingForVideo() && AudioRouter.unsupportedHWAECList.contains(Build.MODEL)) {
                // temporary turn off speaker first to avoid echo for Xiaomi device in the black list
                AudioRouter.turnOnSpeaker()
                currentVideoSpeaker = true

                //get default audio output settings and change UI
                mainVideoView!!.updateUIAudioOutputChanged(currentVideoSpeaker)
            }
        }, 3000)
    }

    override fun processRemoteVideoReceived(remotePeerId: String?, remoteVideo: SkylinkMedia?) {
        processAddRemoteView(remotePeerId, remoteVideo)
    }

    override fun processAudioOutputChanged(isSpeakerOn: Boolean) {
        mainVideoView!!.updateUIAudioOutputChanged(isSpeakerOn)
        currentVideoSpeaker = isSpeakerOn
        if (isSpeakerOn) {
            val log = context.getString(R.string.enable_speaker)
            Utils.toastLog(TAG, context, log)
        } else {
            val log = context.getString(R.string.enable_headset)
            Utils.toastLog(TAG, context, log)
        }
    }

    override fun processRoomLockStatusChanged(roomLocked: Boolean) {
        mainVideoView!!.updateUIRoomLockStatusChanged(roomLocked)
    }
    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------
    /**
     * Process connect to room on service layer and update UI accordingly
     */
    private fun processConnectToRoom() {

        //connect to SDK
        videoService.connectToRoom(Constants.CONFIG_TYPE.VIDEO)
    }

    /**
     * Update UI into disconnected state
     */
    private fun processDisconnectUIChange() {
        // update UI
        mainVideoView!!.updateUIDisconnected()
    }

    /**
     * process file permission that comes from the app
     * when user first choose browsing file from device, permission request dialog will be display
     */
    private fun requestButtonOverlayPermission(): Boolean {
        return permissionUtils.requestButtonOverlayPermission(context, mainVideoView!!.instance)
    }

    /**
     * Get the remote video view from peer id
     */
    private fun processGetRemoteViews(remotePeerId: String?, mediaType: SkylinkMedia.MediaType?): List<SurfaceViewRenderer>? {
        return videoService.getVideoViews(remotePeerId, mediaType)
    }

    /**
     * Add remote video view into the layout
     */
    private fun processAddRemoteView(remotePeerId: String?, remoteMedia: SkylinkMedia?) {
        var videoView = remoteMedia?.videoView
        if (videoView == null) {
            val videoViews = processGetRemoteViews(remotePeerId!!, remoteMedia?.mediaType)
            if (videoViews != null && videoViews.size > 0) {
                // get the first video view of the media type
                videoView = videoViews[0]
            }
        }
        if (videoView == null) return

        // for testing getSkylinkMediaList API
        val videoViews = processGetRemoteViews(remotePeerId!!, remoteMedia?.mediaType)
        if (videoViews != null && videoViews.size > 0) {
            // get the first video view of the media type
            videoView = videoViews[0]
        }

        // setTag for the remote video view
        videoView.tag = remoteMedia?.mediaId
        if (remoteMedia?.mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA ||
                remoteMedia?.mediaType == SkylinkMedia.MediaType.VIDEO) {
            mainVideoView!!.updateUIReceiveRemoteVideo(videoView)
        } else if (remoteMedia?.mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            mainVideoView!!.updateUIReceiveRemoteScreen(videoView)
        }
    }

    /**
     * Update UI when connected to room
     */
    private fun processUpdateStateConnected() {
        mainVideoView!!.updateUIConnected(processGetRoomId())
    }

    /**
     * Get the room id info
     */
    private fun processGetRoomId(): String? {
        return videoService.roomId
    }

    init {
        videoService = VideoService(context)
        videoService.setPresenter(this)
        permissionUtils = PermissionUtils()
    }
}