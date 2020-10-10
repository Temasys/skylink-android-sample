package sg.com.temasys.skylink.sdk.sampleapp.service

import android.content.Context
import android.util.Log
import org.webrtc.SurfaceViewRenderer
import sg.com.temasys.skylink.sdk.rtc.*
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback.*
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig.VideoDevice
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter
import sg.com.temasys.skylink.sdk.sampleapp.multivideos.MultiVideosContract
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils
import java.util.*

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for communicating with SkylinkSDK
 */
class MultiVideosService(context: Context?) : SkylinkCommonService(context!!), MultiVideosContract.Service {
    private val TAG = MultiVideosService::class.java.name
    private val MAX_REMOTE_PEER = 3
    override fun setPresenter(presenter: MultiVideosContract.Presenter) {
        this.presenter = presenter as BasePresenter
    }

    /**
     * Sets the specified listeners for multi videos function
     * Video call needs to implement LifeCycleListener, RemotePeerListener, MediaListener, OsListener,
     * RecordingListener, StatsListener
     */
    override fun setSkylinkListeners() {
        if (currentSkylinkConnection != null) {
            // LifeCycleListener for connect, disconnect,.. with room
            currentSkylinkConnection!!.lifeCycleListener = this

            //RemotePeerListener for communicate with remote peer(s)
            currentSkylinkConnection!!.remotePeerListener = this

            // MediaListener for media using like audio, video,..
            currentSkylinkConnection!!.mediaListener = this

            // OsListener for permission of media
            currentSkylinkConnection!!.osListener = this

            // RecordingListener for recording audio, video
            currentSkylinkConnection!!.recordingListener = this
        }
    }// Set some common configs base on the default setting on the setting page

    // just allow 3 remote peers join the room as the UI supported maximum 3 remote peers

    // set unsupportedHWAEC list to the skylinkConfig

    /**
     * Get the config for multi videos function
     * User can custom video config by using SkylinkConfig
     */
    override val skylinkConfig: SkylinkConfig
        get() {
            val skylinkConfig = SkylinkConfig()

            // Set some common configs base on the default setting on the setting page
            Utils.skylinkConfigCommonOptions(skylinkConfig)
            skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO)
            skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO)
            skylinkConfig.skylinkRoomSize = SkylinkConfig.SkylinkRoomSize.SMALL
            skylinkConfig.isMirrorLocalFrontCameraView = true

            // just allow 3 remote peers join the room as the UI supported maximum 3 remote peers
            skylinkConfig.setMaxRemotePeersConnected(MAX_REMOTE_PEER, SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO)

            // set unsupportedHWAEC list to the skylinkConfig
            AudioRouter.unsupportedHWAECList.add("Mi A2")
            AudioRouter.unsupportedHWAECList.add("TA-1196")
            AudioRouter.unsupportedHWAECList.add("TA-1119")
            skylinkConfig.unsupportedAECModels = AudioRouter.unsupportedHWAECList
            return skylinkConfig
        }

    /**
     * Call this method to switch between available camera.
     * On successful operation, camera switched to will be delivered via callback at
     * [LifeCycleListener.onReceiveInfo]
     * with possible [sg.com.temasys.skylink.sdk.rtc.SkylinkInfo]:
     * -- [sg.com.temasys.skylink.sdk.rtc.SkylinkInfo.CAM_OPEN_FRONT].
     * -- [sg.com.temasys.skylink.sdk.rtc.SkylinkInfo.CAM_OPEN_NON_FRONT].
     */
    fun switchCamera() {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.switchCamera(object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to switchCamera as $contextDescription")
                }
            })
        }
    }

    fun createLocalAudio() {
        //Start audio.
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.createLocalMedia(SkylinkConfig.AudioDevice.MICROPHONE, null, null)
        }
    }

    fun createLocalVideo() {
        // start custom camera if default video device setting is custom device
        if (Utils.isDefaultCustomVideoDeviceSetting()) {
            createLocalCustomVideo()
            return
        }

        //Start audio.
        if (currentSkylinkConnection != null) {

            // Get default setting for videoDevice
            val videoDevice = Utils.getDefaultVideoDevice()

            // If user select back camera as default video device, start back camera
            // else start front camera as default
            if (videoDevice == VideoDevice.CAMERA_BACK) {
                currentSkylinkConnection!!.createLocalMedia(VideoDevice.CAMERA_BACK, null, null)
            } else {
                currentSkylinkConnection!!.createLocalMedia(VideoDevice.CAMERA_FRONT, null, null)
            }
        }
    }

    fun toggleVideo(isRestart: Boolean) {
        if (currentSkylinkConnection != null && localVideo != null) {
            if (isRestart) {
                currentSkylinkConnection!!.changeLocalMediaState(localVideo!!.mediaId, SkylinkMedia.MediaState.ACTIVE, null)
            } else {
                currentSkylinkConnection!!.changeLocalMediaState(localVideo!!.mediaId, SkylinkMedia.MediaState.STOPPED, null)
            }
        }
    }

    fun createLocalScreen() {
        if (currentSkylinkConnection != null) {
            val videoDevice = VideoDevice.SCREEN
            //Start video.
            currentSkylinkConnection!!.createLocalMedia(videoDevice, null, null)
        }
    }

    fun createLocalCustomVideo() {
        // create a new custom video capturer to input for the method
        val customVideoCapturer = Utils.createCustomVideoCapturerFromCamera(VideoDevice.CAMERA_FRONT, currentSkylinkConnection)
        if (customVideoCapturer != null) {
            currentSkylinkConnection!!.createLocalMedia(VideoDevice.CAMERA_FRONT, null, customVideoCapturer, -1, -1, -1, null)
        }
    }

    fun toggleScreen(restart: Boolean) {
        if (currentSkylinkConnection != null && localScreen != null) if (restart) {
            currentSkylinkConnection!!.changeLocalMediaState(localScreen!!.mediaId, SkylinkMedia.MediaState.ACTIVE, null)
        } else {
            currentSkylinkConnection!!.changeLocalMediaState(localScreen!!.mediaId, SkylinkMedia.MediaState.STOPPED, null)
        } else createLocalScreen()
    }

    /**
     * Start recording with conditions:
     * - We must be using Skylink Media Relay (SMR key)
     * - Recording should not be already started.
     * - We should not have just tried to start recording.
     * Actual start of recording will be notified via relevant callback on
     * [BasePresenter.processRecordingStarted]
     */
    fun startRecording() {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.startRecording(object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to startRecording as $contextDescription")
                }
            })
        }
    }

    /**
     * Stop recording with conditions:
     * - We must be already be recording.
     * - We should not have just tried to stop recording.
     * Actual stop of recording will be notified via relevant callback on
     * [BasePresenter.processRecordingStopped]
     */
    fun stopRecording() {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.stopRecording(object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to stopRecording as $contextDescription")
                }
            })
        }
    }

    /**
     * Get the current resolution of the input video being captured by the local camera
     * and the SkylinkCaptureFormat used.
     * If resolution is available, it will be returned in
     * [BasePresenter.processInputVideoResolutionObtained].
     * Note:
     * - Resolution may not always be available, e.g. if no video is captured.
     * - This might be different from the resolution of the video actually sent to Peers as
     * SkylinkSDK may adjust resolution dynamically to try to match its bandwidth criteria.
     */
    val inputVideoResolution: Unit
        get() {
            if (currentSkylinkConnection != null && localVideo != null) {
                currentSkylinkConnection!!.getInputVideoResolution(localVideo!!.mediaId, object : InputVideoResolution {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to getInputVideoResolution as $contextDescription")
                    }

                    override fun onObtainInputVideoResolution(width: Int, height: Int, fps: Int, captureFormat: SkylinkCaptureFormat?) {
                        obtainInputVideoResolution(width, height, fps, captureFormat, localVideo!!.mediaType)
                    }
                })
            }
        }

    /**
     * Get the current resolution of the video being sent to a specific Peer.
     * If resolution is available, it will be returned in
     * [BasePresenter.processSentVideoResolutionObtained]
     *
     * @param peerIndex Index of the remote Peer in frame from whom we want to get sent video resolution.
     * Use -1 to get sent video resolutions of all connected remote Peers.
     * @param mediaType Type of the SkylinkMedia video object that to get video resolution
     */
    fun getSentVideoResolution(peerIndex: Int, mediaType: SkylinkMedia.MediaType?) {
        val mediaId = getProperLocalMediaId(mediaType)
        if (currentSkylinkConnection != null) {
            if (peerIndex != -1 && mediaId != null) {
                // get sent video res to remote peer
                val remotePeerId = mPeersList!![peerIndex].peerId
                currentSkylinkConnection!!.getSentVideoResolution(remotePeerId, mediaId,
                        object : SentVideoResolution {
                            override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                                val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                                Log.e("SkylinkCallback", contextDescription)
                                Utils.toastLog(TAG, context, "\"Unable to getSentVideoResolution as $contextDescription")
                            }

                            override fun onObtainSentVideoResolution(width: Int, height: Int, fps: Int) {
                                obtainSentVideoResolution(width, height, fps, mediaType!!, remotePeerId)
                            }
                        })
            }
        }
    }

    /**
     * Get the current resolution of the video received from a specific Peer's index.
     * If resolution is available, it will be returned in
     * [BasePresenter.processReceivedVideoResolutionObtained]
     *
     * @param peerIndex Index of the remote Peer in frame from whom we want to get received video resolution.
     * Use -1 to get received video resolutions of all connected remote Peers.
     * @param mediaType type of the SkylinkMedia video object to get received video resolution
     */
    fun getReceivedVideoResolution(peerIndex: Int, mediaType: SkylinkMedia.MediaType?) {
        // we also can get media id from remote peer id
        val remotePeerId = mPeersList!![peerIndex].peerId
        val remoteSkylinkMediaList = currentSkylinkConnection!!.getSkylinkMediaList(mediaType, remotePeerId)
        if (remoteSkylinkMediaList == null || remoteSkylinkMediaList.size == 0) {
            return
        }

        // TODO @Muoi need to update when SDK finished get stats by specific media track
        // currently the SDK is just able to get full stats for receiving track, no all mediaTypes or
        // media tracks will get the same stats
        val remoteMedia = remoteSkylinkMediaList[0]
        if (remoteMedia != null && remoteMedia.mediaState != SkylinkMedia.MediaState.UNAVAILABLE) {
            val mediaId = remoteMedia.mediaId
            currentSkylinkConnection!!.getReceivedVideoResolution(mediaId, object : ReceivedVideoResolution {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to getReceivedVideoResolution as $contextDescription")
                }

                override fun onObtainReceivedVideoResolution(width: Int, height: Int, fps: Int) {
                    obtainReceivedVideoResolution(width, height, fps, remoteMedia.mediaType, remotePeerId)
                }
            })
        }
    }

    /**
     * Request for full WebRTC statistics of the specified remote peer by peer index
     * Results will be reported via
     * [BasePresenter.processWebrtcStatsReceived]
     *
     * @param peerIndex Index of the remote Peer in frame for which we are getting transfer speed on.
     */
    fun getWebrtcStats(peerIndex: Int) {
        val peerId = mPeersList!![peerIndex].peerId ?: return

        // get sending stats from local media to remote peer
        val localMediaMap = mPeersList!![0].mediaMap
        if (localMediaMap != null && localMediaMap.size > 0) {
            for (mediaId in localMediaMap.keys) {
                currentSkylinkConnection!!.getSentWebRtcStats(mediaId!!, peerId,
                        object : WebRtcStats {
                            override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                                val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                                Log.e("SkylinkCallback", contextDescription)
                                Utils.toastLog(TAG, context, "\"Unable to getSentWebRtcStats as $contextDescription")
                            }

                            override fun onReceiveWebRtcStats(stats: HashMap<String?, String?>) {
                                presenter!!.processWebrtcStatsReceived(stats)
                            }
                        })
            }
        }

        // get receiving web rtc stats from remote media of remote peer
        val mediaMap = mPeersList!![peerIndex].mediaMap
        if (mediaMap == null || mediaMap.size == 0) return
        for (mediaId in mediaMap.keys) {
            currentSkylinkConnection!!.getReceivedWebRtcStats(mediaId!!,
                    object : WebRtcStats {
                        override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                            val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                            Log.e("SkylinkCallback", contextDescription)
                            Utils.toastLog(TAG, context, "\"Unable to getReceivedWebRtcStats as $contextDescription")
                        }

                        override fun onReceiveWebRtcStats(stats: HashMap<String?, String?>) {
                            presenter!!.processWebrtcStatsReceived(stats)
                        }
                    })
        }
    }

    /**
     * Request for the instantaneous transfer speed(s) of media stream(s), at the moment of request.
     * Results will be reported via
     * [BasePresenter.processTransferSpeedReceived]
     *
     * @param peerIndex  Index of the remote Peer in frame for which we are getting transfer speed on.
     * @param mediaType  type of the media object to get resolution
     * @param forSending The flag to distinguish getting from sending/uploading or from receiving/downloading
     */
    fun getTransferSpeeds(peerIndex: Int, mediaType: SkylinkMedia.MediaType, forSending: Boolean) {
        val peerId = mPeersList!![peerIndex].peerId
        val peerName = mPeersList!![peerIndex].peerName + "(" + peerId + ")"
        if (peerId == null) return

        // get sending stats from local media to remote peer
        if (forSending) {
            val localMediaMap = mPeersList!![0].mediaMap
            if (localMediaMap != null && localMediaMap.size > 0) {
                for (mediaId in localMediaMap.keys) {
                    if (localMediaMap[mediaId]!!.mediaType == mediaType) {
                        currentSkylinkConnection!!.getSentTransferSpeed(mediaId, peerId,
                                object : TransferSpeed {
                                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                                        Log.e("SkylinkCallback", contextDescription)
                                        Utils.toastLog(TAG, context, "\"Unable to getSentTransferSpeed for sending as $contextDescription")
                                    }

                                    override fun onReceiveTransferSpeed(transferSpeed: Double) {
                                        presenter!!.processTransferSpeedReceived(transferSpeed, peerName, true, context)
                                    }
                                })
                    }
                }
            }
        } else {
            val mediaMap = mPeersList!![peerIndex].mediaMap
            if (mediaMap == null || mediaMap.size == 0) return
            if (mediaMap != null && mediaMap.size > 0) {
                for (mediaId in mediaMap.keys) {
                    if (mediaMap[mediaId]!!.mediaType == mediaType) {
                        currentSkylinkConnection!!.getReceivedTransferSpeed(mediaId,
                                object : TransferSpeed {
                                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                                        Log.e("SkylinkCallback", contextDescription)
                                        Utils.toastLog(TAG, context, "\"Unable to getReceivedTransferSpeed for receiving as $contextDescription")
                                    }

                                    override fun onReceiveTransferSpeed(transferSpeed: Double) {
                                        presenter!!.processTransferSpeedReceived(transferSpeed, peerName, false, context)
                                    }
                                })
                    }
                }
            }
        }
    }

    /**
     * Refreshes a connection with a specific peer or all peer(s) connections
     * Able to indicate preference for ICE restart.
     *
     * @param peerIndex  Index of the remote peer in frame to whom we will refresh connection. Use -1 if
     * refresh is to be done with all our remote peers in the room.
     * @param iceRestart Specify if ICE restart should be performed. ICE restart is recommended
     * if network conditions had changed, for e.g. a different network is used.
     */
    fun refreshConnection(peerIndex: Int, iceRestart: Boolean) {
        if (currentSkylinkConnection == null) return
        var peerStr = "All peers "

        //list of peers that are failed for refreshing
        val failedPeers = arrayOfNulls<String>(MAX_REMOTE_PEER)
        if (peerIndex == -1) {
            currentSkylinkConnection!!.refreshConnection(null, iceRestart, object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    failedPeers[0] = details[SkylinkEvent.REMOTE_PEER_ID] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to refreshConnection with remote peer " + failedPeers[0] + " as " + contextDescription)
                }
            })
        } else {
            val peer = mPeersList!![peerIndex]
            currentSkylinkConnection!!.refreshConnection(peer.peerId, iceRestart, object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    failedPeers[0] = details[SkylinkEvent.REMOTE_PEER_ID] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to refreshConnection with remote peer " + failedPeers[0] + " as " + contextDescription)
                }
            })
            peerStr = "Peer " + peer.peerName
        }
        var log = "Refreshing connection for $peerStr"
        log += if (iceRestart) {
            " with ICE restart."
        } else {
            "."
        }
        Utils.toastLog(TAG, context, log)
    }

    /**
     * Return the list of video view of Peer whose PeerId was provided.
     * If peerId is null, local video view list will be returned.
     *
     * @param mediaId id of the Media which videoView belongs to.
     * @return video view of the SkylinkMedia object
     */
    fun getVideoViewById(mediaId: String?): SurfaceViewRenderer? {
        if (currentSkylinkConnection == null || mediaId == null) {
            return null
        }
        val localMedia = currentSkylinkConnection!!.getSkylinkMedia(mediaId)
        return localMedia?.videoView
    }

    /**
     * Return list of the video views of Peer whose peerIndex was provided.
     * If peerIndex is -1, local video views will be returned.
     * Return null if:
     * - peerIndex is not in peerList.
     * - No video view exists for given PeerIndex.
     *
     * @param peerIndex index of the Peer whose videoView to be returned.
     * @return List of Video View of Peer or null if none present.
     */
    fun getVideoViewByIndex(peerIndex: Int): List<SurfaceViewRenderer?>? {
        var mediaList: List<SkylinkMedia>? = null
        var videoViews: MutableList<SurfaceViewRenderer?>? = null

        // for local video view
        if (peerIndex == -1) {
            mediaList = currentSkylinkConnection!!.getSkylinkMediaList(SkylinkMedia.MediaType.VIDEO, null)
        } else {
            if (peerIndex < mPeersList!!.size) {
                val skylinkPeer = mPeersList!![peerIndex]
                mediaList = currentSkylinkConnection!!.getSkylinkMediaList(SkylinkMedia.MediaType.VIDEO, skylinkPeer.peerId)
            }
        }
        if (mediaList == null || mediaList.size == 0) {
            return null
        }
        videoViews = ArrayList()
        for (media in mediaList) {
            val videoView = media.videoView
            if (videoView != null) {
                videoViews.add(videoView)
            }
        }
        return videoViews
    }

    /**
     * Get total number of peers in room including local peer
     *
     * @return number of peer(s) or 0 if nothing in room.
     */
    val totalInRoom: Int
        get() = if (mPeersList != null) mPeersList!!.size else 0

    /**
     * Get list of remote peer id in room using SkylinkConnection API.
     *
     * @return list of peerId or null if not available.
     */
    val peerIdList: Array<String>?
        get() = if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.peerIdList
        } else null

    /**
     * Get index of the peer whose peerId is provided
     * Return -1 if:
     * - No peer exists for given id.
     *
     * @return index in frame of the peer
     */
    fun getPeerIndexByPeerId(peerId: String): Int {
        for (i in mPeersList!!.indices) {
            val peer = mPeersList!![i]
            if (peer.peerId == peerId) {
                return i
            }
        }
        return -1
    }

    /**
     * Get id of the peer in specific index
     * Return null if:
     * - peerIndex is not in peerList.
     * - No peer exists for given index.
     *
     * @return id of peer
     */
    fun getPeerIdByIndex(peerIndex: Int): String? {
        return if (currentSkylinkConnection != null && peerIndex < mPeersList!!.size) {
            mPeersList!![peerIndex].peerId
        } else null
    }

    /**
     * Get the info of a peer in specific index
     */
    fun getPeerByIndex(index: Int): SkylinkPeer {
        return mPeersList!![index]
    }

    fun disposeLocalMedia() {
        clearInstance()
    }

    init {
        initializeSkylinkConnection(Constants.CONFIG_TYPE.MULTI_VIDEOS)
    }
}