package sg.com.temasys.skylink.sdk.sampleapp.service

import android.content.Context
import android.util.Log
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig
import sg.com.temasys.skylink.sdk.rtc.SkylinkError
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter
import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioContract
import sg.com.temasys.skylink.sdk.sampleapp.screen.ScreenContract
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils
import java.util.*

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for communicating with SkylinkSDK
 */
class ScreenService(context: Context?) : SkylinkCommonService(context), ScreenContract.Service {
    override fun setPresenter(presenter: ScreenContract.Presenter?) {
        this.presenter = presenter as BasePresenter
    }

    /**
     * Get the info of a peer in specific index
     */
    fun getPeerByIndex(index: Int): SkylinkPeer {
        return mPeersList!![index]
    }

    /**
     * Sets the specified listeners for audio function
     * Audio function needs to implement LifeCycleListener, RemotePeerListener, MediaListener, OsListener
     */
    override fun setSkylinkListeners() {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.lifeCycleListener = this
            currentSkylinkConnection!!.remotePeerListener = this
            currentSkylinkConnection!!.mediaListener = this
            currentSkylinkConnection!!.osListener = this
        }
    }// Set some common configs base on the default setting on the setting page

    // set unsupportedHWAEC list to the skylinkConfig

    /**
     * Get the config for audio function
     * User can custom audio config by using SkylinkConfig
     */
    override val skylinkConfig: SkylinkConfig
        get() {
            val skylinkConfig = SkylinkConfig()
            // Set some common configs base on the default setting on the setting page
            Utils.skylinkConfigCommonOptions(skylinkConfig)
            skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY)
            skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY)
            skylinkConfig.skylinkRoomSize = SkylinkConfig.SkylinkRoomSize.MEDIUM
            val maxRemotePeer = Utils.getDefaultMaxPeerInAudioRoomConfig()
            skylinkConfig.setMaxRemotePeersConnected(maxRemotePeer, SkylinkConfig.AudioVideoConfig.AUDIO_ONLY)

            // set unsupportedHWAEC list to the skylinkConfig
            AudioRouter.unsupportedHWAECList.add("Mi A2")
            AudioRouter.unsupportedHWAECList.add("TA-1196")
            AudioRouter.unsupportedHWAECList.add("TA-1119")
            skylinkConfig.unsupportedAECModels = AudioRouter.unsupportedHWAECList
            return skylinkConfig
        }

    /**
     * Start local audio in order to communicate with the remote peer
     */
    fun createLocalAudio() {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.createLocalMedia(SkylinkConfig.AudioDevice.MICROPHONE, "local audio mic", object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog("AudioService", context, "\"Unable to createLocalAudio as $contextDescription")
                }
            })
        }
    }

    fun disposeLocalMedia() {
        clearInstance()
    }

    init {
        initializeSkylinkConnection(Constants.CONFIG_TYPE.AUDIO)
    }
}