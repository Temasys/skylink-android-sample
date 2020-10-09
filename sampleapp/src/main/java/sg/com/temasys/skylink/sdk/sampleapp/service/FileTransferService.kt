package sg.com.temasys.skylink.sdk.sampleapp.service

import android.content.Context
import android.util.Log
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig
import sg.com.temasys.skylink.sdk.rtc.SkylinkError
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter
import sg.com.temasys.skylink.sdk.sampleapp.filetransfer.FileTransferContract
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils
import java.io.File
import java.util.*

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for communicating with SkylinkSDK
 */
class FileTransferService(context: Context?) : SkylinkCommonService(context!!), FileTransferContract.Service {
    private val TAG = FileTransferService::class.java.name
    override fun setPresenter(presenter: FileTransferContract.Presenter) {
        this.presenter = presenter as BasePresenter
    }

    /**
     * Sends request(s) to share file with a specific remote peer or to all remote peers in a
     * direct peer to peer manner in the same room.
     * Only 1 file may be sent to the same Peer at the same time.
     * Sending and receiving concurrently with a non-Mobile (e.g. Web or C++) Peer is not supported.
     *
     * @param remotePeerId The id of the remote peer to send the file to. Use 'null' if the file is
     * to be sent to all our remote peers in the room.
     * @param file         The file that is to be shared.
     */
    fun sendFile(remotePeerId: String?, file: File) {
        if (currentSkylinkConnection == null) return

        // Send request to peer requesting permission for file transfer
        currentSkylinkConnection!!.sendFileTransfer(file.absolutePath, file.name, remotePeerId,
                object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
                        Utils.toastLog(TAG, context, "\"Unable to sendFileTransfer as $contextDescription")
                    }
                })
    }

    /**
     * Call this method to accept or reject the file share request from a remote peer.
     *
     * @param remotePeerId       The id of the remote peer that requested to share with us a file.
     * @param downloadedFilePath The absolute path of the file where we want it to be saved.
     * @param isPermitted        Whether permission was granted for the file share to proceed.
     */
    fun sendFileTransferPermissionResponse(remotePeerId: String?, downloadedFilePath: String?, isPermitted: Boolean) {
        if (currentSkylinkConnection == null) return
        if (isPermitted) {
            currentSkylinkConnection!!.acceptFileTransfer(downloadedFilePath, remotePeerId!!, object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to acceptFileTransfer as $contextDescription")
                }
            })
        } else {
            currentSkylinkConnection!!.rejectFileTransfer(remotePeerId!!, object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to sendFileTransferPermissionResponse as "
                            + contextDescription)
                }
            })
        }
    }

    /**
     * Sets the specified listeners for file transfer function
     * File transfer function needs to implement LifeCycleListener, RemotePeerListener, OsListener,
     * FileTransferListener
     */
    override fun setSkylinkListeners() {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.lifeCycleListener = this
            currentSkylinkConnection!!.remotePeerListener = this
            currentSkylinkConnection!!.osListener = this
            currentSkylinkConnection!!.fileTransferListener = this
        }
    }// Set some common configs base on the default setting on the setting page

    /**
     * Get the config for file transfer function
     * User can custom file transfer config by using SkylinkConfig
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
            skylinkConfig.setFileTransfer(true)
            skylinkConfig.setTimeout(SkylinkConfig.SkylinkAction.FILE_SEND_REQUEST, Constants.TIME_OUT_FILE_SEND_REQUEST)
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
        initializeSkylinkConnection(Constants.CONFIG_TYPE.FILE)
    }
}