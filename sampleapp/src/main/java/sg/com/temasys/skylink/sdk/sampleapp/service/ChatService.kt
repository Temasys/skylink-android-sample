package sg.com.temasys.skylink.sdk.sampleapp.service

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.json.JSONArray
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback.StoredMessages
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig
import sg.com.temasys.skylink.sdk.rtc.SkylinkError
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter
import sg.com.temasys.skylink.sdk.sampleapp.chat.ChatContract
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils
import java.util.*

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for communicating with SkylinkSDK
 */
class ChatService(context: Context?) : SkylinkCommonService(context!!), ChatContract.Service {
    private val TAG = ChatService::class.java.name
    private val MAX_REMOTE_PEER = 7
    override fun setPresenter(presenter: ChatContract.Presenter) {
        this.presenter = presenter as BasePresenter
    }

    /**
     * Sends a user defined message to a specific remote peer or to all remote peers via a server.
     *
     * @param remotePeerId Id of the remote peer to whom we will send a message. Use 'null' if the
     * message is to be broadcast to all remote peers in the room.
     * @param message      User defined data
     */
    fun sendServerMessage(remotePeerId: String?, message: Any?) {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.sendServerMessage(message, remotePeerId, object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    presenter!!.processMessageSendFailed()
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to sendServerMessage as $contextDescription")
                }
            })
        }
    }

    /**
     * Sends a user defined message to a specific remote peer or to all remote peers in a direct
     * peer to peer manner.
     *
     * @param remotePeerId Id of the remote peer to whom we will send a message. Use 'null' if the
     * message is to be sent to all our remote peers in the room.
     * @param message      User defined data
     */
    fun sendP2PMessage(remotePeerId: String?, message: Any?) {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.sendP2PMessage(message, remotePeerId, object : SkylinkCallback {
                override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                    val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                    Log.e("SkylinkCallback", contextDescription)
                    Utils.toastLog(TAG, context, "\"Unable to sendP2PMessage as $contextDescription")
                }
            })
        }
    }

    /**
     * Sets the specified listeners for message function
     * Message function needs to implement LifeCycleListener, RemotePeerListener, MessagesListener
     */
    override fun setSkylinkListeners() {
        if (currentSkylinkConnection != null) {
            currentSkylinkConnection!!.lifeCycleListener = this
            currentSkylinkConnection!!.remotePeerListener = this
            currentSkylinkConnection!!.messagesListener = this
        }
    }// Set some common configs base on the default setting on the setting page

    // Set timeout for getting stored message

    /**
     * Get the config for message function
     * User can custom message config by using SkylinkConfig
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
            skylinkConfig.setP2PMessaging(true)

            // Set timeout for getting stored message
            skylinkConfig.setTimeout(SkylinkConfig.SkylinkAction.GET_MESSAGE_STORED, Utils.getDefaultNoOfStoredMsgTimeoutConfig() * 1000)
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

    fun setSelectedEncryptedSecret(secretId: String?) {
        if (currentSkylinkConnection != null) currentSkylinkConnection!!.selectedSecretId = secretId
    }

    val storedMessages: Unit
        get() {
            val messages = arrayOf(JSONArray())
            if (currentSkylinkConnection != null) {
                currentSkylinkConnection!!.getStoredMessages(object : StoredMessages {
                    override fun onObtainStoredMessages(storedMessages: JSONArray, errors: Map<SkylinkError, JSONArray>) {
                        if (storedMessages == null && errors == null) {
                            Toast.makeText(context, "There is no stored messages!", Toast.LENGTH_LONG).show()
                            presenter!!.processStoredMessagesResult(null)
                        } else {
                            if (storedMessages != null) {
                                messages[0] = storedMessages
                                Log.d(TAG, "result returned from stored msg history: $storedMessages")
                                presenter!!.processStoredMessagesResult(storedMessages)
                            }
                            if (errors != null) {
                                Log.e(TAG, "errors from stored msg history: $errors")
                                Toast.makeText(context, "There are errors on stored messages! Please check log!", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                })
            }
        }

    fun setStoreMessage(toPersist: Boolean) {
        currentSkylinkConnection!!.isMessagePersist = toPersist
    }

    fun setEncryptedMap(encryptionKeys: List<String?>, encryptionValues: List<String?>) {
        val encryptionMap: MutableMap<String?, String?> = HashMap()
        for (i in encryptionKeys.indices) {
            encryptionMap[encryptionKeys[i]] = encryptionValues[i]
        }
        currentSkylinkConnection!!.setEncryptSecretsMap(encryptionMap)
    }

    init {
        initializeSkylinkConnection(Constants.CONFIG_TYPE.CHAT)
    }
}