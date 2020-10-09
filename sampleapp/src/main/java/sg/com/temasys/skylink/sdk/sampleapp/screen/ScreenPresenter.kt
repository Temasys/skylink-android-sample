package sg.com.temasys.skylink.sdk.sampleapp.screen

import android.content.Context
import android.os.Build
import android.os.Handler
import android.util.Log
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter
import sg.com.temasys.skylink.sdk.sampleapp.service.ScreenService
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for implementing audio logic.
 */
class ScreenPresenter(private val context: Context) : BasePresenter(), ScreenContract.Presenter {
    private val TAG = ScreenPresenter::class.java.name

    //view object
    private var screenView: ScreenContract.View? = null

    //service object
    private val screenService: ScreenService

    //utils to process permission
    private val permissionUtils: PermissionUtils

    //current audio output getting from default setting
    private var currentAudioSpeaker = Utils.isDefaultSpeakerSettingForAudio()

    //link Presenter to View
    fun setView(view: ScreenContract.View?) {
        screenView = view
        screenView!!.setPresenter(this)
    }
    //----------------------------------------------------------------------------------------------
    // Override methods from ScreenContract.Presenter for view to call
    // These methods are responsible for processing requests from view
    //----------------------------------------------------------------------------------------------
    /**
     * Triggered when View request data to display to the user when entering room
     * Try to connect to room when entering room
     */
    override fun processConnectedLayout() {
        Log.d(TAG, "[processConnectedLayout]")

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!screenService.isConnectingOrConnected) {

            //reset permission request states.
            permissionUtils.permQReset()

            //connect to room on Skylink connection
            screenService.connectToRoom(Constants.CONFIG_TYPE.AUDIO)

            // start local audio
            screenService.createLocalAudio()

            // change the audio output base on the default setting
            AudioRouter.setPresenter(this)
            AudioRouter.startAudioRouting(context, Constants.CONFIG_TYPE.AUDIO)
            if (Utils.isDefaultSpeakerSettingForAudio() && AudioRouter.unsupportedHWAECList.contains(Build.MODEL)) {
                // temporary turn off speaker first to avoid echo for Xiaomi device in the black list
                AudioRouter.turnOffSpeaker()
                currentAudioSpeaker = false
            } else {
                // use service layer to change the audio output, update UI will be called later in processAudioOutputChanged
                if (currentAudioSpeaker) {
                    AudioRouter.turnOnSpeaker()
                } else {
                    AudioRouter.turnOffSpeaker()
                }
            }
            screenView!!.updateUIAudioOutputChanged(currentAudioSpeaker)

            //after connected to skylink SDK, UI will be updated later on processRoomConnected
            Log.d(TAG, "Try to connect when entering room")
        }
    }

    override fun processChangeAudioOutput() {
        //change current speakerOn
        currentAudioSpeaker = !currentAudioSpeaker

        // change audio output (speaker state) in AudioRouter
        if (currentAudioSpeaker) {
            AudioRouter.turnOnSpeaker()
        } else {
            AudioRouter.turnOffSpeaker()
        }
    }

    override fun processPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray?) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, TAG)
    }

    /**
     * Get the specific peer object according to the index
     */
    override fun processGetPeerByIndex(index: Int): SkylinkPeer? {
        return screenService.getPeerByIndex(index)
    }

    override fun processExit() {
        //process disconnect from room
        screenService.disconnectFromRoom()

        // need to call disposeLocalMedia to clear all local media objects as disconnectFromRoom no longer dispose local media
        screenService.disposeLocalMedia()

        //after disconnected from skylink SDK, UI will be updated latter on processRoomDisconnected
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------
    override fun processRoomConnected(isSuccessful: Boolean) {
        if (isSuccessful) {

            // change UI to connected to room, but not connected to any peer
            processUpdateStateConnected()
        }
    }

    override fun processRoomDisconnected() {
        //stop audio routing
        AudioRouter.stopAudioRouting(context)

        // update UI
        screenView!!.updateUIDisconnected()
    }

    override fun processPermissionRequired(info: PermRequesterInfo) {
        // delegate to PermissionUtils to process the permissions require
        permissionUtils.onPermissionRequiredHandler(info, TAG, context, screenView!!.instance)
    }

    override fun processLocalAudioCaptured(localAudio: SkylinkMedia) {
        Utils.toastLog("[SA][processLocalAudioCaptured]", context, "Local audio is on with id = " + localAudio.mediaId)
    }

    override fun processAudioOutputChanged(isSpeakerOn: Boolean) {
        // change the current speaker state
        currentAudioSpeaker = isSpeakerOn

        // change button UI
        screenView!!.updateUIAudioOutputChanged(currentAudioSpeaker)
        if (currentAudioSpeaker) {
            Utils.toastLog(TAG, context, "Speaker is turned ON")
        } else {
            Utils.toastLog(TAG, context, "Speaker is turned OFF")
        }
    }

    /**
     * Add new peer on UI when new peer joined in room
     *
     * @param remotePeer the new peer joined in room
     */
    override fun processRemotePeerConnected(remotePeer: SkylinkPeer) {
        // Fill the new peer in button in custom bar
        // Display new peer at most right location in action bar
        screenView!!.updateUIRemotePeerConnected(remotePeer,
                screenService.totalPeersInRoom - 2)
    }

    override fun processRemotePeerDisconnected(remotePeer: SkylinkPeer, removeIndex: Int) {
        // do not process if the left peer is local peer
        if (removeIndex == -1) return

        // Remove the peer in button in custom bar
        // Remove a remote peer by re-fill total remote peer left in the room
        // to make sure the left peers are displayed correctly
        screenView!!.updateUIRemotePeerDisconnected(screenService.peersList)
    }

    override fun processRemoteAudioReceived(remotePeerId: String) {

        // Add delay 3 seconds for audio speaker turned on to avoid audio echo if the device model is not supported AEC
        val handler = Handler()
        handler.postDelayed({
            // turn on speaker now as default setting
            if (Utils.isDefaultSpeakerSettingForAudio() && AudioRouter.unsupportedHWAECList.contains(Build.MODEL)) {
                // temporary turn off speaker first to avoid echo for Xiaomi device in the black list
                AudioRouter.turnOnSpeaker()
                currentAudioSpeaker = true

                //get default audio output settings and change UI
                screenView!!.updateUIAudioOutputChanged(currentAudioSpeaker)
            }
        }, 3000)
    }
    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------
    /**
     * Update UI when connected to room
     */
    private fun processUpdateStateConnected() {

        // Update the view into connected state
        screenView!!.updateUIConnected(processGetRoomId())
    }

    /**
     * Get the room id info from SDK
     */
    private fun processGetRoomId(): String? {
        return screenService.roomId
    }

    //constructor
    init {
        screenService = ScreenService(context)
        screenService.setPresenter(this)
        permissionUtils = PermissionUtils()
    }
}