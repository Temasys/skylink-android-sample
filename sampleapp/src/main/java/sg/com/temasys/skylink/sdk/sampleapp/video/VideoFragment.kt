package sg.com.temasys.skylink.sdk.sampleapp.video

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.webrtc.SurfaceViewRenderer
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia.MediaState
import sg.com.temasys.skylink.sdk.sampleapp.R
import sg.com.temasys.skylink.sdk.sampleapp.service.ScreenCaptureService
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config
import sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment
import sg.com.temasys.skylink.sdk.sampleapp.utils.*
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.VIDEO_TYPE
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoContract.MainView

/**
 * A simple [CustomActionBar] subclass.
 * This class is responsible for display UI and get user interaction
 */
class VideoFragment : CustomActionBar(), MainView, View.OnClickListener {
    private val TAG = VideoFragment::class.java.name
    private val SELF_CAM_VIEW = "selfCameraVideo"
    private val SELF_SCREEN_VIEW = "selfScreenVideo"
    private val REMOTE_CAM_VIEW = "remoteCameraVideo"
    private val REMOTE_SCREEN_VIEW = "remoteScreenVideo"
    private val MAIN_VIEW = "main"

    // view widgets
    private var videoViewLayout: LinearLayout? = null
    private var audioLayout: LinearLayout? = null
    private var videoLayout: LinearLayout? = null
    private var screenLayout: LinearLayout? = null
    private var videoToolLayout: RelativeLayout? = null
    private var btnVideoRes: CustomTriangleButton? = null
    private var btnConnectDisconnect: ImageButton? = null
    private var btnAudio: ImageButton? = null
    private var btnVideo: ImageButton? = null
    private var btnScreen: ImageButton? = null
    private var btnAudioSpeaker: CustomImageButton? = null
    private var btnAudioMute: CustomImageButton? = null
    private var btnAudioRemove: CustomImageButton? = null
    private var btnAudioStart: CustomImageButton? = null
    private var btnVideoSwitchCamera: CustomImageButton? = null
    private var btnVideoMute: CustomImageButton? = null
    private var btnVideoRemove: CustomImageButton? = null
    private var btnVideoStart: CustomImageButton? = null
    private var btnScreenMute: CustomImageButton? = null
    private var btnScreenRemove: CustomImageButton? = null
    private var btnScreenStart: CustomImageButton? = null
    private var btnLockRoom: ImageButton? = null
    private var stopScreenshareFloat: Button? = null
    private var btnFullScreen: Button? = null
    private var stopScreenshareLayoutParams: WindowManager.LayoutParams? = null

    // presenter instance to implement video logic
    private var presenter: VideoContract.Presenter? = null

    // local variables to keep views' state
    private var isShowScreenSharing = false
    private var isFullScreen = false
    private var isShowVideoRes = false
    private var isLockRoom = false
    private var localCameraView: SurfaceViewRenderer? = null
    private var localScreenView: SurfaceViewRenderer? = null
    private var remoteCameraView: SurfaceViewRenderer? = null
    private var remoteScreenView: SurfaceViewRenderer? = null

    // private variables to keep the internal view states
    private var toConnectToRoom = true
    private var isAudioOptionsShow = false
    private var isVideoOptionsShow = false
    private var isScreenOptionsShow = false
    private var currentMainVideoType: VIDEO_TYPE? = null
    private var exitFromRoomByUser = false

    private lateinit var mediaProjectionManager: MediaProjectionManager


    override fun setPresenter(presenter: VideoContract.Presenter) {
        this.presenter = presenter
    }

    //----------------------------------------------------------------------------------------------
    // Fragment life cycle methods
    //----------------------------------------------------------------------------------------------
    override fun onAttach(context: Context) {
        super.onAttach(context)
        super.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Allow volume to be controlled using volume keys
        (context as VideoActivity).volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[SA][Video][onCreateView] ")
        val rootView = inflater.inflate(R.layout.fragment_video, container, false)

        // get the UI controls from layout
        getControlWidgets(rootView)

        // setup the action bar
        setActionBar()

        // init the UI controls
        initComponents()
        return rootView
    }

    override fun onResume() {
        super.onResume()

        // just in case that user are not sharing screen, then stop the camera
        presenter!!.processResumeState()
    }

    override fun onPause() {
        super.onPause()

        // just in case that user are not sharing screen, then stop the camera
        presenter!!.processPauseState()
    }

    override fun onClick(view: View) {
        //Defining a click event actions for the buttons
        when (view.id) {
            R.id.btnBack -> processReturn()
            R.id.btnLocalPeer -> {
                changeLocalPeerUI(true)
                displayPeerInfo(0)
            }
            R.id.btnRemotePeer1 -> {
                changeRemotePeerUI(1, true)
                displayPeerInfo(1)
            }
            R.id.btnRemotePeer2 -> {
                changeRemotePeerUI(2, true)
                displayPeerInfo(2)
            }
            R.id.btnRemotePeer3 -> {
                changeRemotePeerUI(3, true)
                displayPeerInfo(3)
            }
            R.id.btnRemotePeer4 -> {
                changeRemotePeerUI(4, true)
                displayPeerInfo(4)
            }
            R.id.btnRemotePeer5 -> {
                changeRemotePeerUI(5, true)
                displayPeerInfo(5)
            }
            R.id.btnRemotePeer6 -> {
                changeRemotePeerUI(6, true)
                displayPeerInfo(6)
            }
            R.id.btnRemotePeer7 -> {
                changeRemotePeerUI(7, true)
                displayPeerInfo(7)
            }
            R.id.btn_connect_disconnect -> processConnectDisconnect()
            R.id.btn_audio -> showAudioOptions()
            R.id.btn_audio_start -> presenter!!.processStartAudio()
            R.id.btn_audio_remove -> presenter!!.processRemoveAudio()
            R.id.btn_audio_mute -> presenter!!.processChangeAudioState()
            R.id.btn_audio_speaker -> presenter!!.processChangeAudioOutput()
            R.id.btn_video -> showVideoOptions()
            R.id.btn_video_start -> presenter!!.processToggleVideo()
            R.id.btn_video_remove -> presenter!!.processRemoveVideo()
            R.id.btn_video_mute -> presenter!!.processChangeVideoState()
            R.id.btn_video_switch_camera -> presenter!!.processSwitchCamera()
            R.id.btn_full_screen -> processFullScreen()
            R.id.btn_video_res_video -> showHideVideoResolution()
            R.id.ll_videos -> showHideVideoResolution(false)
            R.id.btn_screen -> showScreenOptions()
//            R.id.btn_screen_start -> presenter!!.processToggleScreen()
            R.id.btn_screen_start -> startScreenCapturing()
//            R.id.btn_screen_remove -> presenter!!.processRemoveScreen()
            R.id.btn_screen_remove -> stopSeenCapturing()
            //stopSeenCapturing
            R.id.btn_screen_mute -> presenter!!.processChangeScreenState()
            R.id.btn_lock -> processLockUnlockRoom()
        }
    }

    override fun onDetach() {
        super.onDetach()
        // only exit/disconnect from room when it is chosen by user
        // not changing configuration
        if (!(context as VideoActivity).isChangingConfigurations) {
            presenter!!.processExit()
            exitFromRoomByUser = true
            if (isShowScreenSharing) {
                showHideButton(stopScreenshareFloat, false)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // delegate presenter to implement the permission results
        presenter!!.processPermissionsResult(requestCode, permissions, grantResults)
    }
    //----------------------------------------------------------------------------------------------
    // Methods invoked from the Presenter to update UI
    //----------------------------------------------------------------------------------------------
    /**
     * Update UI into connected state when connected to room
     * Show room id and local peer button and display local avatar by the first character of the local username
     *
     * @param roomId the id of the connected room that generated by SDK
     */
    override fun updateUIConnected(roomId: String) {
        // update the room id that has been generated by the SDK
        updateRoomInfo(roomId)

        // update the local peer avatar with the user name configured in default setting
        updateUILocalPeer(Config.getPrefString(ConfigRoomFragment.PREF_USER_NAME_VIDEO_SAVED, Constants.USER_NAME_VIDEO_DEFAULT, context))

        // Change the connect button UI to disconnect button
        btnConnectDisconnect!!.setImageResource(R.drawable.ic_disconnect_white_25dp)
        btnConnectDisconnect!!.background = resources.getDrawable(R.drawable.button_circle_call_end)
        btnLockRoom!!.isEnabled = true

        // change variable toConnectToRoom
        toConnectToRoom = false
    }

    /**
     * Update information about new remote peer joining the room at a specific index
     *
     * @param newPeer remote peer joining the room
     * @param index   specific index
     */
    override fun updateUIRemotePeerConnected(newPeer: SkylinkPeer, index: Int) {
        updateUiRemotePeerJoin(newPeer, index)
    }

    /**
     * Update information about remote peer left the room
     * Re-fill the peers list in order to display correct order of peers in room
     *
     * @param peerList the list of left peer(s) in the room
     */
    override fun updateUIRemotePeerDisconnected(peerList: List<SkylinkPeer>) {
        processFillPeers(peerList)

        // reset the video resolution
        if (context != null && context is VideoActivity) {
            (context as VideoActivity).removeView(VIDEO_TYPE.REMOTE_CAMERA)
            (context as VideoActivity).removeView(VIDEO_TYPE.REMOTE_SCREEN)
            (context as VideoActivity).onShowHideRemoteCameraViewFragment(false, false)
            (context as VideoActivity).onShowHideRemoteScreenViewFragment(false, false)
        }
    }

    /**
     * Change certain UI elements when disconnecting from room.
     */
    override fun updateUIDisconnected() {
        changeUIDisconnected()
    }

    /**
     * Get the instance of the view for implementing runtime permission
     */
    override fun getInstance(): Fragment {
        return this
    }

    override fun updateUIShowButtonStopScreenShare() {
        if (!isShowScreenSharing) {
            showHideButton(stopScreenshareFloat, true)
        }
    }

    override fun updateUILocalAudioAdded(mediaId: String) {
        btnAudioMute!!.isEnabled = true
        btnAudioRemove!!.isEnabled = true

        // set disable for btnAudioStart because we do not support stop audio atm
        btnAudioStart!!.isEnabled = false

        // change UI button
        btnAudioStart!!.setStart(false)
    }

    /**
     * Add or update our self VideoView into the view layout when local peer connected to room and
     * local video view is ready
     *
     * @param mediaId   id of the media object
     * @param videoView local video view from camera
     */
    override fun updateUILocalCameraAdded(mediaId: String, videoView: SurfaceViewRenderer) {
        //save localCameraView
        localCameraView = videoView
        currentMainVideoType = VIDEO_TYPE.LOCAL_CAMERA
        btnVideoSwitchCamera!!.isEnabled = true
        btnVideoMute!!.isEnabled = true
        btnVideoRemove!!.isEnabled = true
        btnVideoStart!!.setStart(false)
        bringSmallViewToMainView(VIDEO_TYPE.LOCAL_CAMERA)
    }

    /**
     * Add or update our self VideoView into the view layout when local peer connected to room and
     * local video view is ready
     *
     * @param mediaId    id of the media object
     * @param screenView local video view from screen
     */
    override fun updateUILocalScreenAdded(mediaId: String, screenView: SurfaceViewRenderer) {
        //save localScreenView
        localScreenView = screenView
        currentMainVideoType = VIDEO_TYPE.LOCAL_SCREEN
        btnScreenRemove!!.isEnabled = true
        btnScreenMute!!.isEnabled = true
        btnScreenMute!!.setMuted(false)
        btnScreenStart!!.setStart(false)
        bringSmallViewToMainView(VIDEO_TYPE.LOCAL_SCREEN)
        if (!isShowScreenSharing) {
            showHideButton(stopScreenshareFloat, true)
        }
    }

    override fun updateUIReceiveRemoteAudio(remotePeerId: String) {
        btnAudioSpeaker!!.isEnabled = true
        //        if (Utils.isDefaultSpeakerSettingForVideo()) {
//            btnAudioSpeaker.setMuted(false);
//        } else {
//            btnAudioSpeaker.setMuted(true);
//        }
    }

    /**
     * Add or update remote Peer's VideoView into the view layout when receiving remote camera video view
     *
     * @param remoteVideoView
     */
    override fun updateUIReceiveRemoteVideo(remoteVideoView: SurfaceViewRenderer) {
        //save localScreenView
        remoteCameraView = remoteVideoView
        currentMainVideoType = VIDEO_TYPE.REMOTE_CAMERA

        // move other views to small view
        bringSmallViewToMainView(VIDEO_TYPE.REMOTE_CAMERA)
    }

    /**
     * Add remote screen video view to the main view when receiving remote screen video view
     *
     * @param remoteScreenView remote screen video view
     */
    override fun updateUIReceiveRemoteScreen(remoteScreenView: SurfaceViewRenderer) {
        //save localScreenView
        this.remoteScreenView = remoteScreenView
        currentMainVideoType = VIDEO_TYPE.REMOTE_SCREEN

        // move other views to small view
        bringSmallViewToMainView(VIDEO_TYPE.REMOTE_SCREEN)
    }

    override fun updateUIMediaStateChange(mediaType: SkylinkMedia.MediaType, mediaState: MediaState, isLocal: Boolean) {
        if (isLocal) {
            when (mediaState) {
                MediaState.ACTIVE -> if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
                    btnVideoMute!!.isEnabled = true
                    btnVideoMute!!.setMuted(false)
                    btnVideoStart!!.setStart(false)
                    btnVideoRemove!!.isEnabled = true
                    btnVideoSwitchCamera!!.isEnabled = true
                } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
                    btnScreenMute!!.setMuted(false)
                    btnScreenStart!!.setStart(false)
                    btnScreenMute!!.isEnabled = true
                    btnScreenRemove!!.isEnabled = true
                    if (!isShowScreenSharing) {
                        showHideButton(stopScreenshareFloat, true)
                    }
                } else if (mediaType == SkylinkMedia.MediaType.AUDIO_MIC) {
                    btnAudioMute!!.isEnabled = true
                    btnAudioRemove!!.isEnabled = true
                }
                MediaState.MUTED -> if (mediaType == SkylinkMedia.MediaType.AUDIO_MIC) {
                    btnAudioMute!!.setMuted(true)
                } else if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
                    btnVideoStart!!.setStart(false)
                    btnVideoMute!!.isEnabled = true
                    btnVideoSwitchCamera!!.isEnabled = true
                    btnVideoMute!!.setMuted(true)
                } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
                    btnScreenStart!!.setStart(false)
                    btnScreenMute!!.isEnabled = true
                    btnScreenMute!!.setMuted(true)
                }
                MediaState.STOPPED -> if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
                    btnVideoStart!!.setStart(true)
                    btnVideoMute!!.isEnabled = true
                    btnVideoSwitchCamera!!.isEnabled = false
                } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
                    btnScreenStart!!.setStart(true)
                    btnScreenMute!!.isEnabled = true
                    if (isShowScreenSharing) {
                        showHideButton(stopScreenshareFloat, false)
                    }
                }
                MediaState.UNAVAILABLE -> if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
                    btnVideoStart!!.isEnabled = true
                    btnVideoStart!!.setStart(true)
                    btnVideoMute!!.isEnabled = false
                    btnVideoSwitchCamera!!.isEnabled = false
                    btnVideoRemove!!.isEnabled = false
                    removeView(SkylinkMedia.MediaType.VIDEO_CAMERA, true)
                } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
                    btnScreenStart!!.isEnabled = true
                    btnScreenStart!!.setStart(true)
                    btnScreenMute!!.isEnabled = false
                    if (isShowScreenSharing) {
                        showHideButton(stopScreenshareFloat, false)
                    }
                    btnScreenRemove!!.isEnabled = false
                    removeView(SkylinkMedia.MediaType.VIDEO_SCREEN, true)
                } else if (mediaType == SkylinkMedia.MediaType.AUDIO_MIC) {
                    btnAudioMute!!.isEnabled = false
                    btnAudioStart!!.isEnabled = true
                    btnAudioStart!!.setStart(true)
                    btnAudioRemove!!.isEnabled = false
                }
            }
        } else {
            if (mediaState == MediaState.UNAVAILABLE) {
                when (mediaType) {
                    SkylinkMedia.MediaType.VIDEO, SkylinkMedia.MediaType.VIDEO_CAMERA, SkylinkMedia.MediaType.VIDEO_SCREEN, SkylinkMedia.MediaType.VIDEO_CUSTOM -> removeView(mediaType, false)
                    SkylinkMedia.MediaType.AUDIO, SkylinkMedia.MediaType.AUDIO_MIC -> btnAudioSpeaker!!.isEnabled = false
                }
            }
        }
    }

    /**
     * Remove a specific view, both main view and small views
     */
    private fun removeView(videoType: SkylinkMedia.MediaType, isLocal: Boolean) {
        when (videoType) {
            SkylinkMedia.MediaType.VIDEO_CAMERA -> {
                showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, isLocal, false)
                if (isLocal) {
                    if (currentMainVideoType == VIDEO_TYPE.LOCAL_CAMERA && isLocal) {
                        moveViewToSmallLocalCameraView(localCameraView)
                    }
                    localCameraView = null
                    (context as VideoActivity).removeView(VIDEO_TYPE.LOCAL_CAMERA)
                } else {
                    if (currentMainVideoType == VIDEO_TYPE.REMOTE_CAMERA && !isLocal) {
                        moveViewToSmallRemoteCameraView(remoteCameraView)
                    }
                    remoteCameraView = null
                    (context as VideoActivity).removeView(VIDEO_TYPE.REMOTE_CAMERA)
                }
            }
            SkylinkMedia.MediaType.VIDEO_SCREEN -> {
                showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, isLocal, false)
                if (isLocal) {
                    if (currentMainVideoType == VIDEO_TYPE.LOCAL_SCREEN && isLocal) {
                        moveViewToSmallLocalScreenView(localScreenView)
                    }
                    localScreenView = null
                    (context as VideoActivity).removeView(VIDEO_TYPE.LOCAL_SCREEN)
                } else {
                    if (currentMainVideoType == VIDEO_TYPE.REMOTE_SCREEN && !isLocal) {
                        moveViewToSmallRemoteScreenView(remoteScreenView)
                    }
                    remoteScreenView = null
                    (context as VideoActivity).removeView(VIDEO_TYPE.REMOTE_SCREEN)
                }
            }
        }
    }

    /**
     * Remove remote video view when remote peer left or local peer exit the room
     *
     *
     * Change layout orientation to Vertical when there is only 1 local video view
     */
    override fun updateUIRemoveRemotePeer() {
        remoteCameraView = null
        remoteScreenView = null

        // bring local camera view to main big view
        bringSmallViewToMainView(VIDEO_TYPE.LOCAL_CAMERA)
    }

    /**
     * Update the audio output/speaker button when being requested from presenter
     * For example: when the phone connects to a bluetooth headset, the speaker is automatically off
     */
    override fun updateUIAudioOutputChanged(isSpeakerOn: Boolean) {
        btnAudioSpeaker!!.setMuted(!isSpeakerOn)
        if (isSpeakerOn) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                btnAudioSpeaker!!.tooltipText = "Enable headset"
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                btnAudioSpeaker!!.tooltipText = "Enable speaker"
            }
        }
    }

    override fun updateUIRoomLockStatusChanged(isRoomLocked: Boolean) {
        isLockRoom = isRoomLocked
        if (isRoomLocked) {
            btnLockRoom!!.setImageDrawable(context.resources.getDrawable(R.drawable.icon_unlock_room))
        } else {
            btnLockRoom!!.setImageDrawable(context.resources.getDrawable(R.drawable.icon_lock_room))
        }
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------
    private fun getControlWidgets(rootView: View) {
        videoViewLayout = rootView.findViewById(R.id.ll_videos)
        videoToolLayout = rootView.findViewById(R.id.ll_video_tool)
        btnFullScreen = rootView.findViewById(R.id.btn_full_screen)
        btnVideoRes = rootView.findViewById(R.id.btn_video_res_video)
        btnConnectDisconnect = rootView.findViewById(R.id.btn_connect_disconnect)
        audioLayout = rootView.findViewById(R.id.ll_audio_call)
        videoLayout = rootView.findViewById(R.id.ll_video_call)
        screenLayout = rootView.findViewById(R.id.ll_screen_call)
        btnAudioSpeaker = rootView.findViewById(R.id.btn_audio_speaker)
        btnAudioMute = rootView.findViewById(R.id.btn_audio_mute)
        btnAudioRemove = rootView.findViewById(R.id.btn_audio_remove)
        btnAudioStart = rootView.findViewById(R.id.btn_audio_start)
        btnAudio = rootView.findViewById(R.id.btn_audio)
        btnVideoSwitchCamera = rootView.findViewById(R.id.btn_video_switch_camera)
        btnVideoMute = rootView.findViewById(R.id.btn_video_mute)
        btnVideoRemove = rootView.findViewById(R.id.btn_video_remove)
        btnVideoStart = rootView.findViewById(R.id.btn_video_start)
        btnVideo = rootView.findViewById(R.id.btn_video)
        btnScreenMute = rootView.findViewById(R.id.btn_screen_mute)
        btnScreenRemove = rootView.findViewById(R.id.btn_screen_remove)
        btnScreenStart = rootView.findViewById(R.id.btn_screen_start)
        btnScreen = rootView.findViewById(R.id.btn_screen)
        btnLockRoom = rootView.findViewById(R.id.btn_lock)
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private fun setActionBar() {
        val actionBar = (activity as VideoActivity?)!!.supportActionBar
        super.setActionBar(actionBar)
    }

    /**
     * Init value for view components
     */
    private fun initComponents() {
        btnAudioSpeaker!!.drawableActive = resources.getDrawable(R.drawable.ic_headset_white_20dp)
        btnAudioSpeaker!!.drawableDisable = resources.getDrawable(R.drawable.ic_headset_disable_20dp)
        btnAudioSpeaker!!.drawableMuted = resources.getDrawable(R.drawable.ic_speaker_white_20dp)
        btnAudioSpeaker!!.drawableMutedDisable = resources.getDrawable(R.drawable.ic_speaker_disable_20dp)
        btnAudioMute!!.drawableActive = resources.getDrawable(R.drawable.ic_audio_mute_white_20dp)
        btnAudioMute!!.drawableDisable = resources.getDrawable(R.drawable.ic_mic_off_disable_20dp)
        btnAudioMute!!.drawableMuted = resources.getDrawable(R.drawable.ic_mic_active_white_20dp)
        btnAudioMute!!.drawableMutedDisable = resources.getDrawable(R.drawable.ic_mic_disable_20dp)
        btnAudioRemove!!.drawableActive = resources.getDrawable(R.drawable.ic_remove_white_20dp)
        btnAudioRemove!!.drawableDisable = resources.getDrawable(R.drawable.ic_remove_disable_20dp)
        btnAudioStart!!.drawableActive = resources.getDrawable(R.drawable.ic_start_white_20dp)
        btnAudioStart!!.drawableDisable = resources.getDrawable(R.drawable.ic_start_disable_20dp)
        btnAudioStart!!.drawableStart = resources.getDrawable(R.drawable.ic_start_white_20dp)
        btnAudioStart!!.drawableStop = resources.getDrawable(R.drawable.ic_stop_white_20dp)
        btnVideoSwitchCamera!!.drawableActive = resources.getDrawable(R.drawable.ic_switch_camera_white_20dp)
        btnVideoSwitchCamera!!.drawableDisable = resources.getDrawable(R.drawable.ic_switch_camera_disable_20dp)
        btnVideoMute!!.drawableActive = resources.getDrawable(R.drawable.ic_videocam_mute_white_20dp)
        btnVideoMute!!.drawableDisable = resources.getDrawable(R.drawable.ic_videocam_mute_disable_20dp)
        btnVideoMute!!.drawableMuted = resources.getDrawable(R.drawable.ic_videocam_active_white_20dp)
        btnVideoMute!!.drawableMutedDisable = resources.getDrawable(R.drawable.ic_videocam_active_disable_20dp)
        btnVideoRemove!!.drawableActive = resources.getDrawable(R.drawable.ic_remove_white_20dp)
        btnVideoRemove!!.drawableDisable = resources.getDrawable(R.drawable.ic_remove_disable_20dp)
        btnVideoStart!!.drawableActive = resources.getDrawable(R.drawable.ic_start_white_20dp)
        btnVideoStart!!.drawableDisable = resources.getDrawable(R.drawable.ic_start_disable_20dp)
        btnVideoStart!!.drawableStart = resources.getDrawable(R.drawable.ic_start_white_20dp)
        btnVideoStart!!.drawableStop = resources.getDrawable(R.drawable.ic_stop_white_20dp)
        btnScreenMute!!.drawableActive = resources.getDrawable(R.drawable.ic_stop_screen_share_white_20dp)
        btnScreenMute!!.drawableDisable = resources.getDrawable(R.drawable.ic_stop_screen_share_disable_20dp)
        btnScreenMute!!.drawableMuted = resources.getDrawable(R.drawable.ic_start_screen_share_20dp)
        btnScreenMute!!.drawableMutedDisable = resources.getDrawable(R.drawable.ic_start_screen_share_disable_20dp)
        btnScreenRemove!!.drawableActive = resources.getDrawable(R.drawable.ic_remove_white_20dp)
        btnScreenRemove!!.drawableDisable = resources.getDrawable(R.drawable.ic_remove_disable_20dp)
        btnScreenStart!!.drawableActive = resources.getDrawable(R.drawable.ic_start_white_20dp)
        btnScreenStart!!.drawableDisable = resources.getDrawable(R.drawable.ic_start_disable_20dp)
        btnScreenStart!!.drawableStart = resources.getDrawable(R.drawable.ic_start_white_20dp)
        btnScreenStart!!.drawableStop = resources.getDrawable(R.drawable.ic_stop_white_20dp)

        // set onClick event for buttons in layout
        btnBack.setOnClickListener(this)
        btnLocalPeer.setOnClickListener(this)
        btnRemotePeer1.setOnClickListener(this)
        btnRemotePeer2.setOnClickListener(this)
        btnRemotePeer3.setOnClickListener(this)
        btnRemotePeer4.setOnClickListener(this)
        btnRemotePeer5.setOnClickListener(this)
        btnRemotePeer6.setOnClickListener(this)
        btnRemotePeer7.setOnClickListener(this)
        btnFullScreen!!.setOnClickListener(this)
        btnVideoRes!!.setOnClickListener(this)
        btnConnectDisconnect!!.setOnClickListener(this)
        audioLayout!!.setOnClickListener(this)
        videoLayout!!.setOnClickListener(this)
        screenLayout!!.setOnClickListener(this)
        btnAudioSpeaker!!.setOnClickListener(this)
        btnAudioMute!!.setOnClickListener(this)
        btnAudioRemove!!.setOnClickListener(this)
        btnAudioStart!!.setOnClickListener(this)
        btnAudio!!.setOnClickListener(this)
        btnVideoSwitchCamera!!.setOnClickListener(this)
        btnVideoMute!!.setOnClickListener(this)
        btnVideoRemove!!.setOnClickListener(this)
        btnVideoStart!!.setOnClickListener(this)
        btnVideo!!.setOnClickListener(this)
        btnScreenMute!!.setOnClickListener(this)
        btnScreenRemove!!.setOnClickListener(this)
        btnScreenStart!!.setOnClickListener(this)
        btnScreen!!.setOnClickListener(this)
        btnLockRoom!!.setOnClickListener(this)

        // init setting value for room name and room id in action bar
        // Update the UI when connecting to room: change the room_id
        updateRoomInfo(resources.getString(R.string.guide_room_id))
        txtRoomName.text = Config.getPrefString(ConfigRoomFragment.PREF_ROOM_NAME_VIDEO_SAVED, Constants.ROOM_NAME_VIDEO_DEFAULT, context)
        btnVideoRes!!.setDirection(CustomTriangleButton.ButtonDirection.TOP_RIGHT)

        // Set init audio/video state
        setAudioBtnLabel(false, false)
        setVideoBtnLabel(false, false)

        // set disable for some buttons
        btnAudioSpeaker!!.isEnabled = false
        btnAudioMute!!.isEnabled = false
        btnAudioRemove!!.isEnabled = false
        btnVideoSwitchCamera!!.isEnabled = false
        btnVideoMute!!.isEnabled = false
        btnVideoRemove!!.isEnabled = false
        btnScreenMute!!.isEnabled = false
        btnScreenRemove!!.isEnabled = false
        btnLockRoom!!.isEnabled = false

        // Add an system overlay button for stop screen share
        stopScreenshareFloat = Button(activity)
        stopScreenshareFloat!!.text = activity!!.resources.getText(R.string.stop_screenShare)
        stopScreenshareFloat!!.setTextColor(activity!!.resources.getColor(R.color.color_white))
        stopScreenshareFloat!!.textSize = activity!!.resources.getDimension(R.dimen.stop_screen_text_size)
        stopScreenshareFloat!!.background = activity!!.resources.getDrawable(R.drawable.button_stop_screen_sharing)
        stopScreenshareLayoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  //                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)
        } else {
            WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)
        }
        stopScreenshareLayoutParams!!.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        stopScreenshareFloat!!.setOnClickListener { view: View? ->
            // stop capturing screen
            presenter!!.processToggleScreen(false)
        }

        // add tool tip for buttons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnBack.tooltipText = "Back to main activity"
            btnLocalPeer.tooltipText = "Local peer"
            btnRemotePeer1.tooltipText = "Remote peer"
            btnRemotePeer2.tooltipText = "Remote peer"
            btnRemotePeer3.tooltipText = "Remote peer"
            btnVideoRes!!.tooltipText = "Video resolution setting"
            btnFullScreen!!.tooltipText = "Full screen"
            btnConnectDisconnect!!.tooltipText = "Connect to room"
            btnAudioSpeaker!!.tooltipText = "Turn on speaker/headset"
            btnAudioMute!!.tooltipText = "Mute/Active audio"
            btnAudioRemove!!.tooltipText = "Remove audio"
            btnAudioStart!!.tooltipText = "Start/Stop audio"
            btnAudio!!.tooltipText = "Audio setting options"
            btnVideoSwitchCamera!!.tooltipText = "Switch camera"
            btnVideoMute!!.tooltipText = "Mute/Active video"
            btnVideoRemove!!.tooltipText = "Remove video"
            btnVideoStart!!.tooltipText = "Start/Stop video"
            btnVideo!!.tooltipText = "Video setting options"
            btnScreenMute!!.tooltipText = "Mute/Active screen sharing"
            btnScreenRemove!!.tooltipText = "Remove screen sharing"
            btnScreenStart!!.tooltipText = "Start/Stop screen sharing"
            btnScreen!!.tooltipText = "Screen sharing setting optiosn"
        }
        if (Utils.isDefaultSpeakerSettingForVideo()) {
            btnAudioSpeaker!!.setMuted(false)
        } else {
            btnAudioSpeaker!!.setMuted(true)
        }

        // start a local video base on default device setting
        presenter!!.processStartLocalMediaIfConfigAllow()
    }

    /**
     * Set the mute audio button label according to the current state of audio.
     *
     * @param doToast If true, Toast about setting audio to current state.
     */
    private fun setAudioBtnLabel(isAudioMuted: Boolean, doToast: Boolean) {
        if (isAudioMuted) {
            if (doToast) {
                val log = getString(R.string.muted_audio)
                Utils.toastLog(TAG, context, log)
            }
        } else {
            if (doToast) {
                val log = getString(R.string.enabled_audio)
                Utils.toastLog(TAG, context, log)
            }
        }
    }

    /**
     * Set the mute video button label according to the current state of video.
     *
     * @param doToast If true, Toast about setting video to current state.
     */
    private fun setVideoBtnLabel(isVideoMuted: Boolean, doToast: Boolean) {
        if (isVideoMuted) {
            if (doToast) {
                val log = getString(R.string.muted_video)
                Utils.toastLog(TAG, context, log)
            }
        } else {
            if (doToast) {
                val log = getString(R.string.enabled_video)
                Utils.toastLog(TAG, context, log)
            }
        }
    }

    /**
     * Display the dialog of peer info including peer username and peer id
     * when the user click into the peer button in action bar
     */
    private fun displayPeerInfo(index: Int) {
        val peer = presenter!!.processGetPeerByIndex(index)
        if (index == 0) {
            processDisplayLocalPeer(peer)
        } else {
            processDisplayRemotePeer(peer)
        }
    }

    private fun addViewToMain(videoView: SurfaceViewRenderer?) {
        if (videoView == null) {
            val log = "[SA][addViewToMain] Not adding view to main view as videoView is null!"
            Log.d(TAG, log)
            return
        }

        // If previous self video exists,
        // Set new video to size of previous self video
        // And remove old self video.
        val self = videoViewLayout!!.findViewWithTag<View>(MAIN_VIEW)
        if (self != null) {
            // Remove the old self video.
            videoViewLayout!!.removeView(self)
        }

        // Tag new video as self and add onClickListener.
        videoView.tag = MAIN_VIEW

        // Show new video on screen
        // Remove video from previous parent, if any.
        Utils.removeViewFromParent(videoView)

        // And new self video.
        val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        params.gravity = Gravity.CENTER
        params.weight = 1f
        videoView.layoutParams = params

        //alway set self video as vertical orientation
        videoViewLayout!!.orientation = LinearLayout.VERTICAL
        videoViewLayout!!.addView(videoView)
    }

    private fun showHideButton(view: View?, isShow: Boolean) {
        if (view == null) return
        if (view === stopScreenshareFloat) {
            if (activity == null) return
            val windowManager = activity!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (isShow && !isShowScreenSharing) {
                windowManager.addView(stopScreenshareFloat, stopScreenshareLayoutParams)
                isShowScreenSharing = true
            } else if (!isShow && isShowScreenSharing) {
                windowManager.removeView(stopScreenshareFloat)
                isShowScreenSharing = false
            }
        } else {
            if (isShow) {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }
    }

    private fun processFullScreen() {
        isFullScreen = !isFullScreen
        val actionBar = (activity as VideoActivity?)!!.supportActionBar
        if (isFullScreen) {
            videoToolLayout!!.visibility = View.GONE
            btnVideoRes!!.visibility = View.GONE
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, true, false)
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, true, false)
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, false, false)
            showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, false, false)
            (activity as VideoActivity?)!!.onShowHideVideoResFragment(false)
            btnFullScreen!!.background = activity!!.resources.getDrawable(R.drawable.ic_full_screen_exit)
            actionBar!!.hide()

            // change tool tip for button
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                btnFullScreen!!.tooltipText = "Exit full screen"
            }
        } else {
            videoToolLayout!!.visibility = View.VISIBLE
            btnVideoRes!!.visibility = View.VISIBLE
            if (localCameraView != null) {
                showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, true, true)
            }
            if (localScreenView != null) {
                showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, true, true)
            }
            if (remoteCameraView != null) {
                showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, false, true)
            }
            if (remoteScreenView != null) {
                showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, false, true)
            }
            if (isShowVideoRes) {
                (activity as VideoActivity?)!!.onShowHideVideoResFragment(true)
            }
            btnFullScreen!!.background = activity!!.resources.getDrawable(R.drawable.ic_full_screen)
            actionBar!!.show()
            bringSmallViewToMainView(currentMainVideoType)

            // change tool tip for button
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                btnFullScreen!!.tooltipText = "Full screen"
            }
        }
    }

    private fun showHideVideoResolution() {
        isShowVideoRes = !isShowVideoRes
        (activity as VideoActivity?)!!.onShowHideVideoResFragment(isShowVideoRes)
        if (isShowVideoRes) {
            btnVideoRes!!.state = CustomTriangleButton.ButtonState.CLICKED
        } else {
            btnVideoRes!!.state = CustomTriangleButton.ButtonState.NORMAL
        }
    }

    private fun showHideVideoResolution(isShow: Boolean) {
        isShowVideoRes = isShow
        (activity as VideoActivity?)!!.onShowHideVideoResFragment(isShow)
        if (isShow) {
            btnVideoRes!!.state = CustomTriangleButton.ButtonState.CLICKED
        } else {
            btnVideoRes!!.state = CustomTriangleButton.ButtonState.NORMAL
        }
    }

    /**
     * Bring the small available on small view to main big view
     * and move all other views to small views
     *
     * @param videoType the type of small view to move to main big view
     */
    fun bringSmallViewToMainView(videoType: VIDEO_TYPE?) {
        if (videoType == null) return
        currentMainVideoType = videoType
        when (videoType) {
            VIDEO_TYPE.LOCAL_CAMERA -> {
                if (localCameraView != null) {
                    addViewToMain(localCameraView)
                    showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, true, false)
                }
                moveViewToSmallLocalScreenView(localScreenView)
                moveViewToSmallRemoteCameraView(remoteCameraView)
                moveViewToSmallRemoteScreenView(remoteScreenView)
            }
            VIDEO_TYPE.LOCAL_SCREEN -> {
                if (localScreenView != null) {
                    addViewToMain(localScreenView)
                    showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, true, false)
                }
                moveViewToSmallLocalCameraView(localCameraView)
                moveViewToSmallRemoteCameraView(remoteCameraView)
                moveViewToSmallRemoteScreenView(remoteScreenView)
            }
            VIDEO_TYPE.REMOTE_CAMERA -> {
                if (remoteCameraView != null) {
                    addViewToMain(remoteCameraView)
                    showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_CAMERA, false, false)
                }
                moveViewToSmallLocalCameraView(localCameraView)
                moveViewToSmallLocalScreenView(localScreenView)
                moveViewToSmallRemoteScreenView(remoteScreenView)
            }
            VIDEO_TYPE.REMOTE_SCREEN -> {
                if (remoteScreenView != null) {
                    addViewToMain(remoteScreenView)
                    showHideSmallFragment(SkylinkMedia.MediaType.VIDEO_SCREEN, false, false)
                }
                moveViewToSmallLocalCameraView(localCameraView)
                moveViewToSmallLocalScreenView(localScreenView)
                moveViewToSmallRemoteCameraView(remoteCameraView)
            }
        }
    }

    private fun moveViewToSmallLocalCameraView(view: SurfaceViewRenderer?) {
        if (view == null) return
        view.tag = SELF_CAM_VIEW
        if (context != null && context is VideoActivity) {
            (context as VideoActivity).setLocalCameraView(view)
        }
    }

    private fun moveViewToSmallLocalScreenView(view: SurfaceViewRenderer?) {
        if (view == null) return
        view.tag = SELF_SCREEN_VIEW
        if (context != null && context is VideoActivity) {
            (context as VideoActivity).setLocalScreenView(view)
        }
    }

    private fun moveViewToSmallRemoteCameraView(view: SurfaceViewRenderer?) {
        if (view == null) return
        view.tag = REMOTE_CAM_VIEW
        if (context != null && context is VideoActivity) {
            (context as VideoActivity).setRemoteCameraView(view)
        }
    }

    private fun moveViewToSmallRemoteScreenView(view: SurfaceViewRenderer?) {
        if (view == null) return
        view.tag = REMOTE_SCREEN_VIEW
        if (context != null && context is VideoActivity) {
            (context as VideoActivity).setRemoteScreenView(view)
        }
    }

    private fun processConnectDisconnect() {
        if (toConnectToRoom) {
            if (presenter != null) {
                presenter!!.processConnectedLayout()

                // Update the UI when connecting to room: change the room_id
                updateRoomInfo(resources.getString(R.string.initing_room_id))
            }
        } else {
            if (presenter != null) {
                presenter!!.processDisconnectFromRoom()
            }
        }
    }

    /**
     * Update the UI when disconnect from the room
     */
    private fun changeUIDisconnected() {
        // if user exit from the room by intentionally press back button or navigate up button,
        // no need to change the UI to avoid crashing the app
        if (exitFromRoomByUser) {
            return
        }
        val main = videoViewLayout!!.findViewWithTag<View>(MAIN_VIEW)
        if (currentMainVideoType == VIDEO_TYPE.REMOTE_CAMERA || currentMainVideoType == VIDEO_TYPE.REMOTE_SCREEN) {
            videoViewLayout!!.removeView(main)
            if (localCameraView != null) bringSmallViewToMainView(VIDEO_TYPE.LOCAL_CAMERA) else if (localScreenView != null) bringSmallViewToMainView(VIDEO_TYPE.LOCAL_SCREEN)
        }
        val peerCamera = videoViewLayout!!.findViewWithTag<View>(REMOTE_CAM_VIEW)
        val peerScreen = videoViewLayout!!.findViewWithTag<View>(REMOTE_SCREEN_VIEW)
        if (peerCamera != null) {
            videoViewLayout!!.removeView(peerCamera)
        }
        if (peerScreen != null) {
            videoViewLayout!!.removeView(peerScreen)
        }

        // change the connect/disconnect button
        btnConnectDisconnect!!.setImageResource(R.drawable.ic_connect_white_25dp)
        btnConnectDisconnect!!.background = resources.getDrawable(R.drawable.button_circle_connect_to_room)
        btnLockRoom!!.setImageDrawable(context.resources.getDrawable(R.drawable.icon_lock_room))
        btnLockRoom!!.isEnabled = false
        isLockRoom = false

        // reset the room id info and local peer button
        txtRoomId.setText(R.string.guide_room_id)
        btnLocalPeer.visibility = View.GONE
        btnRemotePeer1.visibility = View.GONE
        btnRemotePeer2.visibility = View.GONE
        btnRemotePeer3.visibility = View.GONE

        // reset the variable toConnectToRoom
        toConnectToRoom = true

        // reset the video resolution
        if (context != null && context is VideoActivity) {
            (context as VideoActivity).resetSmallRemoteViews()
        }
    }

    /**
     * Show or Hide the audio options
     */
    private fun showAudioOptions() {
        isAudioOptionsShow = !isAudioOptionsShow
        if (isAudioOptionsShow) {
            btnAudio!!.setImageResource(R.drawable.ic_collapse_green_20dp)
            showHideViewAudioOption(btnAudioStart, true, false)
            showHideViewAudioOption(btnAudioRemove, true, false)
            showHideViewAudioOption(btnAudioMute, true, false)
            showHideViewAudioOption(btnAudioSpeaker, true, true)
        } else {
            showHideViewAudioOption(btnAudioStart, false, false)
            showHideViewAudioOption(btnAudioRemove, false, false)
            showHideViewAudioOption(btnAudioMute, false, false)
            showHideViewAudioOption(btnAudioSpeaker, false, true)
            audioLayout!!.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    /**
     * Show or Hide the video options
     */
    private fun showVideoOptions() {
        isVideoOptionsShow = !isVideoOptionsShow
        if (isVideoOptionsShow) {
            btnVideo!!.setImageResource(R.drawable.ic_collapse_green_20dp)
            showHideViewVideoOption(btnVideoStart, true, false)
            showHideViewVideoOption(btnVideoRemove, true, false)
            showHideViewVideoOption(btnVideoMute, true, false)
            showHideViewVideoOption(btnVideoSwitchCamera, true, true)
        } else {
            showHideViewVideoOption(btnVideoStart, false, false)
            showHideViewVideoOption(btnVideoRemove, false, false)
            showHideViewVideoOption(btnVideoMute, false, false)
            showHideViewVideoOption(btnVideoSwitchCamera, false, true)
            videoLayout!!.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    /**
     * Show or Hide the screen sharing options
     */
    private fun showScreenOptions() {
        isScreenOptionsShow = !isScreenOptionsShow
        if (isScreenOptionsShow) {
            btnScreen!!.setImageResource(R.drawable.ic_collapse_green_20dp)
            showHideViewScreenOption(btnScreenMute, true, false)
            showHideViewScreenOption(btnScreenRemove, true, false)
            showHideViewScreenOption(btnScreenStart, true, true)
        } else {
            showHideViewScreenOption(btnScreenMute, false, false)
            showHideViewScreenOption(btnScreenRemove, false, false)
            showHideViewScreenOption(btnScreenStart, false, true)
            screenLayout!!.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun showHideViewAudioOption(view: View?, isShow: Boolean, isFinalView: Boolean) {
        if (isShow) {
            view!!.animate()
                    .translationX(0f)
                    .alpha(1.0f)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            view.visibility = View.VISIBLE
                            if (isFinalView) {
                                audioLayout!!.background = resources.getDrawable(R.drawable.frame_layout_round_border_tool)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnAudio!!.tooltipText = "Collapse buttons"
                                }
                            }
                        }
                    })
        } else {
            view!!.animate()
                    .translationX(btnAudio!!.height.toFloat())
                    .alpha(0.0f)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            view.visibility = View.GONE
                            if (isFinalView) {
                                btnAudio!!.setImageResource(R.drawable.ic_audio_green_20dp)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnAudio!!.tooltipText = "Audio options"
                                }
                            }
                        }
                    })
        }
    }

    private fun showHideViewVideoOption(view: View?, isShow: Boolean, isFinalView: Boolean) {
        if (isShow) {
            view!!.animate()
                    .translationX(0f)
                    .alpha(1.0f)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            view.visibility = View.VISIBLE
                            if (isFinalView) {
                                videoLayout!!.background = resources.getDrawable(R.drawable.frame_layout_round_border_tool)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnVideo!!.tooltipText = "Collapse buttons"
                                }
                            }
                        }
                    })
        } else {
            view!!.animate()
                    .translationX(btnVideo!!.height.toFloat())
                    .alpha(0.0f)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            view.visibility = View.GONE
                            if (isFinalView) {
                                btnVideo!!.setImageResource(R.drawable.ic_video_green_20dp)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnVideo!!.tooltipText = "Video options"
                                }
                            }
                        }
                    })
        }
    }

    private fun showHideViewScreenOption(view: View?, isShow: Boolean, isFinalView: Boolean) {
        if (isShow) {
            view!!.animate()
                    .translationX(0f)
                    .alpha(1.0f)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            view.visibility = View.VISIBLE
                            if (isFinalView) {
                                screenLayout!!.background = resources.getDrawable(R.drawable.frame_layout_round_border_tool)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnScreen!!.tooltipText = "Collapse buttons"
                                }
                            }
                        }
                    })
        } else {
            view!!.animate()
                    .translationX(btnScreen!!.height.toFloat())
                    .alpha(0.0f)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            view.visibility = View.GONE
                            if (isFinalView) {
                                btnScreen!!.setImageResource(R.drawable.ic_screen_share_green_20dp)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    btnScreen!!.tooltipText = "Screen sharing options"
                                }
                            }
                        }
                    })
        }
    }

    private fun showHideSmallFragment(mediaType: SkylinkMedia.MediaType, isLocal: Boolean, isShow: Boolean) {
        if (context == null || context !is VideoActivity) {
            return
        }
        var smallVideoFragment: SmallVideoViewFragment? = null
        when (mediaType) {
            SkylinkMedia.MediaType.VIDEO_CAMERA -> smallVideoFragment = if (isLocal) (context as VideoActivity).localVideoCameraFragment else (context as VideoActivity).remoteVideoCameraFragment
            SkylinkMedia.MediaType.VIDEO_SCREEN -> smallVideoFragment = if (isLocal) (context as VideoActivity).localVideoScreenFragment else (context as VideoActivity).remoteVideoScreenFragment
        }
        if (smallVideoFragment == null) {
            return
        }
        if (isShow) {
            (context as VideoActivity).attachSmallView(smallVideoFragment)
        } else {
            (context as VideoActivity).detachSmallView(smallVideoFragment)
        }
    }

    private fun processLockUnlockRoom() {
        if (isLockRoom) {
            presenter!!.processUnlockRoom()
        } else {
            presenter!!.processLockRoom()
        }
    }

    /**
     * process exit the demo when people press on back button in the menu
     */
    private fun processReturn() {
        presenter!!.processExit()
        processBack()
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun requestScreenCapturePermission() {
        Log.d(TAG, "Requesting permission to capture screen")
        val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // This initiates a prompt dialog for the user to confirm screen projection.
//        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
//                REQUEST_MEDIA_PROJECTION)
    }

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1000
        const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 1001
        const val MEDIA_PROJECTION_REQUEST_CODE = 1002

        fun newInstance(): VideoFragment {
            return VideoFragment()
        }
    }

    // for screen share
    private fun startScreenCapturing() {
        startMediaProjectionRequest()
    }

    private fun stopSeenCapturing() {

        context.startService(Intent(context, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP
        })

        presenter!!.processRemoveScreen()
    }

    /**
     * Before a capture session can be started, the capturing app must
     * call MediaProjectionManager.createScreenCaptureIntent().
     * This will display a dialog to the user, who must tap "Start now" in order for a
     * capturing session to be started. This will allow both video and audio to be captured.
     */
    private fun startMediaProjectionRequest() {
        // use applicationContext to avoid memory leak on Android 10.
        mediaProjectionManager =
                context.applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                MEDIA_PROJECTION_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
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
                ContextCompat.startForegroundService(context, screenCaptureIntent)

            } else {
                Toast.makeText(
                        context, "Request to obtain MediaProjection denied.",
                        Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}