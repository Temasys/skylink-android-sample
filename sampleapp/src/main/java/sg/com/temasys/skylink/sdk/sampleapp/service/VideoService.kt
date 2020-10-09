package sg.com.temasys.skylink.sdk.sampleapp.service

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import sg.com.temasys.skylink.sdk.rtc.*
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig.VideoDevice
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoContract
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionContract
import java.util.*

/**
 * Created by muoi.pham on 20/07/18.
 * The service class is responsible for communicating with the SkylinkSDK API by using SkylinkConnection instance
 */
class VideoService(context: Context?) : SkylinkCommonService(context), VideoContract.Service {
    private val MAX_REMOTE_PEER = 1
    override fun setPresenter(presenter: VideoContract.Presenter) {
        this.presenter = presenter as BasePresenter
    }

    override fun setResPresenter(videoResPresenter: VideoResolutionContract.Presenter) {
        this.videoResPresenter = videoResPresenter as BasePresenter
    }

    /**
     * Create local video camera if it has not been started
     * if the local video camera is started:
     * - Stop or restart the local camera based on the parameter |toActive|,
     * given that the local video source is available, i.e., had been started and not removed.
     * However, if the intended state of the camera (active or stopped) is already the current
     * state, then no change will be effected.
     * Trigger callback SkylinkCallback.onError if an error occurs, for example:
     * if local video source is not available.
     */
    fun toggleVideo(toActive: Boolean) {
        if (currentSkylinkConnection != null && localVideo != null) {
            if (toActive) {
                currentSkylinkConnection!!.changeLocalMediaState(localVideo!!.mediaId, SkylinkMedia.MediaState.ACTIVE, object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to active local video as $contextDescription")
                    }
                })
            } else {
                currentSkylinkConnection!!.changeLocalMediaState(localVideo!!.mediaId, SkylinkMedia.MediaState.STOPPED, object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to stop local video as $contextDescription")
                    }
                })
            }
        } else if (toActive) {
            createLocalVideo()
        }
    }

    /**
     * Create local video screen sharing if it has not been started
     * if the local video screen is started:
     * - Stop or restart the local screen based on the parameter |toActive|,
     * given that the local screen source is available, i.e., had been started and not removed.
     * However, if the intended state of the screen (active or stopped) is already the current
     * state, then no change will be effected.
     * Trigger callback SkylinkCallback.onError if an error occurs, for example:
     * if local screen source is not available.
     */
    fun toggleScreen(toActive: Boolean) {
        if (currentSkylinkConnection != null && localScreen != null) {
            if (toActive) {
                currentSkylinkConnection!!.changeLocalMediaState(localScreen!!.mediaId, SkylinkMedia.MediaState.ACTIVE, object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to active screen as $contextDescription")
                    }
                })
            } else {
                currentSkylinkConnection!!.changeLocalMediaState(localScreen!!.mediaId, SkylinkMedia.MediaState.STOPPED, object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to stop screen as $contextDescription")
                    }
                })
            }
        } else createLocalScreen()
    }

    /**
     * Mutes the local user's audio and notifies all the peers in the room.
     *
     * @param toMuted Flag that specifies whether audio should be mute
     */
    fun muteLocalAudio(toMuted: Boolean) {
        if (currentSkylinkConnection != null) {
            if (toMuted) {
                currentSkylinkConnection!!.changeLocalMediaState(localAudio?.mediaId, SkylinkMedia.MediaState.MUTED, object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to mute local audio as $contextDescription")
                    }
                })
            } else {
                currentSkylinkConnection!!.changeLocalMediaState(localAudio?.mediaId, SkylinkMedia.MediaState.ACTIVE, object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to active local audio as $contextDescription")
                    }
                })
            }
        }
    }

    /**
     * Mutes the local user's video and notifies all the peers in the room.
     * Note that black video frames (consuming bandwidth) are still being sent to remote Peer(s).
     *
     * @param toMuted Flag that specifies whether video should be mute
     */
    fun muteLocalVideo(toMuted: Boolean) {
        if (currentSkylinkConnection != null) {
            if (toMuted) {
                currentSkylinkConnection!!.changeLocalMediaState(localVideo?.mediaId, SkylinkMedia.MediaState.MUTED, object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to mute local video as $contextDescription")
                    }
                })
            } else {
                currentSkylinkConnection!!.changeLocalMediaState(localVideo?.mediaId, SkylinkMedia.MediaState.ACTIVE, object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to active local video as $contextDescription")
                    }
                })
            }
        }
    }

    /**
     * Mutes the local user's screen video and notifies all the peers in the room.
     * Note that black video frames (consuming bandwidth) are still being sent to remote Peer(s).
     *
     * @param toMuted Flag that specifies whether screen video should be mute
     */
    fun muteLocalScreen(toMuted: Boolean) {
        if (currentSkylinkConnection != null) {
            if (toMuted) {
                currentSkylinkConnection!!.changeLocalMediaState(localScreen?.mediaId, SkylinkMedia.MediaState.MUTED, object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to mute local screen as $contextDescription")
                    }
                })
            } else {
                currentSkylinkConnection!!.changeLocalMediaState(localScreen?.mediaId, SkylinkMedia.MediaState.ACTIVE, object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to active local screen as $contextDescription")
                    }
                })
            }
        }
    }

    /**
     * Return the specific video view of the SkylinkMedia that mediaId is provided
     *
     * @return Video View of Peer or null if none is matched with the input id.
     */
    fun getVideoView(mediaId: String?): SurfaceViewRenderer? {
        if (currentSkylinkConnection != null) {
            val media = currentSkylinkConnection!!.getSkylinkMedia(mediaId!!)
            if (media != null) {
                return media.videoView
            }
        }
        return null
    }

    /**
     * Return the list of video view from a list of SkylinkMedia objects that can get from SDK by the peer id and media type
     *
     * @param peerId id of the peer that the media belongs
     * if null, consider as self peer
     * @return lis of video views that comes with peer id and media type
     */
    fun getVideoViews(peerId: String?, mediaType: SkylinkMedia.MediaType?): List<SurfaceViewRenderer>? {
        var mediaObjects: List<SkylinkMedia>? = null
        val videoViews: MutableList<SurfaceViewRenderer> = ArrayList()
        if (currentSkylinkConnection != null) {
            mediaObjects = currentSkylinkConnection!!.getSkylinkMediaList(mediaType, peerId)
            if (mediaObjects == null || mediaObjects.size == 0) {
                return null
            }
        }
        for (media in mediaObjects!!) {
            if (media.mediaState != SkylinkMedia.MediaState.UNAVAILABLE) {
                videoViews.add(media.videoView)
            }
        }
        return videoViews
    }

    /**
     * Change the speaker output to on/off
     * The speaker is automatically turned off when audio bluetooth or headset is connected.
     */
    fun changeSpeakerOutput(isSpeakerOn: Boolean) {
        if (isSpeakerOn) {
            AudioRouter.turnOnSpeaker()
        } else {
            AudioRouter.turnOffSpeaker()
        }
    }

    /**
     * Sets the specified listeners for video function
     * Video call needs to implement LifeCycleListener, RemotePeerListener, MediaListener, OsListener
     */
    override fun setSkylinkListeners() {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.lifeCycleListener = this
            currentSkylinkConnection!!.remotePeerListener = this
            currentSkylinkConnection!!.mediaListener = this
            currentSkylinkConnection!!.osListener = this
        }
    }// Set some common configs base on the default setting on the setting page

    // just 1 to 1 video call

    // set unsupportedHWAEC list to the skylinkConfig

    /**
     * Get the config for video function
     * User can custom video config by using SkylinkConfig
     */
    override val skylinkConfig: SkylinkConfig
        get() {
            val skylinkConfig = SkylinkConfig()

            // Set some common configs base on the default setting on the setting page
            Utils.skylinkConfigCommonOptions(skylinkConfig)
            skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO)
            skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO)
            skylinkConfig.skylinkRoomSize = SkylinkConfig.SkylinkRoomSize.EXTRA_SMALL
            skylinkConfig.isMirrorLocalFrontCameraView = true
            skylinkConfig.isReportVideoResolutionUntilStable = true
            skylinkConfig.isReportVideoResolutionOnVideoChange = true

            // just 1 to 1 video call
            skylinkConfig.setMaxRemotePeersConnected(1, SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO)

            // set unsupportedHWAEC list to the skylinkConfig
            AudioRouter.unsupportedHWAECList.add("Mi A2")
            AudioRouter.unsupportedHWAECList.add("TA-1196")
            AudioRouter.unsupportedHWAECList.add("TA-1119")
            skylinkConfig.unsupportedAECModels = AudioRouter.unsupportedHWAECList
            return skylinkConfig
        }

    /**
     * Get the info of a peer in specific index
     */
    fun getPeerByIndex(index: Int): SkylinkPeer {
        return mPeersList!![index]
    }

    fun switchCamera() {
        currentSkylinkConnection!!.switchCamera(object : SkylinkCallback {
            override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                Log.e("SkylinkCallback", contextDescription)
                Utils.toastLog(TAG, context, "\"Unable to switch local camera as $contextDescription")
            }
        })
    }

    fun createLocalAudio() {
        Log.d(TAG, "createLocalAudio()")
        //Start audio.
        if (currentSkylinkConnection != null && localAudio == null) {
            currentSkylinkConnection!!.createLocalMedia(SkylinkConfig.AudioDevice.MICROPHONE, "mobile's audio", object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to createLocalAudio as $contextDescription")
                }
            })
        }
    }

    fun createLocalVideo() {
        Log.d(TAG, "createLocalVideo()")
        if (currentSkylinkConnection != null && localVideo == null) {

            // Get default setting for videoDevice
            val videoDevice = Utils.getDefaultVideoDevice()

            // If user select back camera as default video device, start back camera
            // else start front camera as default
            if (videoDevice == VideoDevice.CAMERA_BACK) {
                currentSkylinkConnection!!.createLocalMedia(VideoDevice.CAMERA_BACK, "mobile cam back", object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to createLocalVideo as $contextDescription")
                    }
                })
            } else {
                currentSkylinkConnection!!.createLocalMedia(VideoDevice.CAMERA_FRONT, "mobile cam front", object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to createLocalVideo as $contextDescription")
                    }
                })
            }
        }
    }

    fun createLocalScreen() {
        Log.d(TAG, "createLocalScreen()")

        //get default video resolution (widthxheight) from setting to create local screen with preferred resolution (optional)
        var width = 800
        var height = 1600
        val screenResolution = Utils.getDefaultScreenResolution()
        val screenOrientation = context?.resources?.configuration?.orientation
        if (screenResolution == Config.SCREEN_RESOLUTION_LARGE) {
            if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                width = Config.ScreenResolution.LARGE_PORTRAIT.width
                height = Config.ScreenResolution.LARGE_PORTRAIT.height
            } else if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                width = Config.ScreenResolution.LARGE_LANDSCAPE.width
                height = Config.ScreenResolution.LARGE_LANDSCAPE.height
            }
        } else if (screenResolution == Config.SCREEN_RESOLUTION_MEDIUM) {
            if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                width = Config.ScreenResolution.MEDIUM_PORTRAIT.width
                height = Config.ScreenResolution.MEDIUM_PORTRAIT.height
            } else if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                width = Config.ScreenResolution.MEDIUM_LANDSCAPE.width
                height = Config.ScreenResolution.MEDIUM_LANDSCAPE.height
            }
        } else if (screenResolution == Config.SCREEN_RESOLUTION_SMALL) {
            if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                width = Config.ScreenResolution.SMALL_PORTRAIT.width
                height = Config.ScreenResolution.SMALL_PORTRAIT.height
            } else if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                width = Config.ScreenResolution.SMALL_LANDSCAPE.width
                height = Config.ScreenResolution.SMALL_LANDSCAPE.height
            }
        }
        val defaultScreenFps = 60
        if (currentSkylinkConnection != null && localScreen == null) {
            val videoDevice = VideoDevice.SCREEN
            //Start screen by default video resolution in setting.
            currentSkylinkConnection!!.createLocalMedia(videoDevice, "screen capture from mobile",
                    width, height, defaultScreenFps, object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to createLocalScreen as $contextDescription")
                }
            })
        }
    }

    fun createLocalCustomVideo(customVideoCapturer: VideoCapturer?) {
        Log.d(TAG, "createLocalCustomVideo()")
        // create a new custom video capturer to input for the method
//        VideoCapturer customVideoCapturer = Utils.createCustomVideoCapturerFromCamera(
//                CAMERA_FRONT, skylinkConnection);
        if (customVideoCapturer != null) {
            currentSkylinkConnection!!.createLocalMedia(VideoDevice.SCREEN, "SCREEN video from mobile",
                    customVideoCapturer, -1, -1, -1, object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to createLocalCustomVideo as $contextDescription")
                }
            })
        }
    }

    fun disposeLocalMedia() {
        clearInstance()
    }

    /**
     * Remove local audio object
     * Result will be informed in [MediaListener.onChangeLocalMedia]
     * with [SkylinkMedia.MediaState] is [SkylinkMedia.MediaState.UNAVAILABLE] if local audio
     * is removed successful OR [LifeCycleListener.onReceiveWarning] if local audio
     * can not be removed or any error occurs
     */
    fun destroyLocalAudio() {
        if (localAudio != null) {
            currentSkylinkConnection!!.destroyLocalMedia(localAudio!!.mediaId, object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to destroyLocalAudio as $contextDescription")
                }
            })
        }
    }

    /**
     * Remove local video camera object
     * Result will be informed in [MediaListener.onChangeLocalMedia]
     * with [SkylinkMedia.MediaState] is [SkylinkMedia.MediaState.UNAVAILABLE] if local video camera
     * is removed successful OR [LifeCycleListener.onReceiveWarning]  if local video camera
     * can not be removed or any error occurs
     */
    fun destroyLocalVideo() {
        if (localVideo != null) {
            currentSkylinkConnection!!.destroyLocalMedia(localVideo!!.mediaId, object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to destroyLocalVideo as $contextDescription")
                }
            })
        }
    }

    /**
     * Remove local screen object
     * Result will be informed in [MediaListener.onChangeLocalMedia]
     * with [SkylinkMedia.MediaState] is [SkylinkMedia.MediaState.UNAVAILABLE] if local screen
     * is removed successful OR [LifeCycleListener.onReceiveWarning] if local screen
     * can not be removed or any error occurs
     */
    fun destroyLocalScreen() {
        if (localScreen != null) {
            currentSkylinkConnection!!.destroyLocalMedia(localScreen!!.mediaId, object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to destroyLocalScreen as $contextDescription")
                }
            })
        }
    }

    companion object {
        private val TAG = VideoService::class.java.canonicalName
    }

    init {
        initializeSkylinkConnection(Constants.CONFIG_TYPE.VIDEO)
    }
}