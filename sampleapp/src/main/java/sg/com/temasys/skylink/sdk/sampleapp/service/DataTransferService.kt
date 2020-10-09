package sg.com.temasys.skylink.sdk.sampleapp.service

import android.content.Context
import android.util.Log
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig
import sg.com.temasys.skylink.sdk.rtc.SkylinkError
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter
import sg.com.temasys.skylink.sdk.sampleapp.datatransfer.DataTransferContract
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils
import java.util.*

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for communicating with SkylinkSDK
 */
class DataTransferService(context: Context?) : SkylinkCommonService(context!!), DataTransferContract.Service {
    private val TAG = DataTransferService::class.java.name
    private val MAX_REMOTE_PEER = 7
    override fun setPresenter(presenter: DataTransferContract.Presenter) {
        this.presenter = presenter as BasePresenter
    }

    /**
     * Sends a byte array to a specified remotePeer or to all participants of the room if the
     * remotePeerId is null
     * The byte array cannot be null, and its maximum size is 65456 bytes.
     * Notes:
     * - This operation is currently not supported with Skylink Media Relay.
     * - This operation is currently only supported between Skylink Mobile SDKs.
     *
     * @param remotePeerId remotePeerID of a specified peer
     * @param data         Array of bytes
     */
    fun sendData(remotePeerId: String?, data: ByteArray?) {
        if (currentSkylinkConnection == null) return
        currentSkylinkConnection!!.sendData(data, remotePeerId, object : SkylinkCallback {
            override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                Log.e("SkylinkCallback", contextDescription)
                Utils.toastLog(TAG, context, "\"Unable to sendData as $contextDescription")
            }
        })
    }

    /**
     * Sets the specified listeners for data transfer function
     * Data transfer function needs to implement LifeCycleListener, RemotePeerListener, DataTransferListener
     */
    override fun setSkylinkListeners() {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.lifeCycleListener = this
            currentSkylinkConnection!!.remotePeerListener = this
            currentSkylinkConnection!!.dataTransferListener = this
        }
    }// Set some common configs base on the default setting on the setting page

    /**
     * Get the config for data transfer function
     * User can custom data transfer config by using SkylinkConfig
     */
    override val skylinkConfig: SkylinkConfig
        get() {
            val skylinkConfig = SkylinkConfig()
            // Set some common configs base on the default setting on the setting page
            Utils.skylinkConfigCommonOptions(skylinkConfig)
            skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO)
            skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO)
            skylinkConfig.skylinkRoomSize = SkylinkConfig.SkylinkRoomSize.LARGE
            val maxRemotePeer = Utils.getDefaultMaxPeerInNoMediaRoomConfig()
            skylinkConfig.setMaxRemotePeersConnected(maxRemotePeer, SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO)
            skylinkConfig.setDataTransfer(true)
            return skylinkConfig
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
        initializeSkylinkConnection(Constants.CONFIG_TYPE.DATA)
    }
}