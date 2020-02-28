package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkError;
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.chat.ChatContract;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for communicating with SkylinkSDK
 */

public class ChatService extends SkylinkCommonService implements ChatContract.Service {

    private final String TAG = ChatService.class.getName();

    private final int MAX_REMOTE_PEER = 7;

    public ChatService(Context context) {
        super(context);
        initializeSkylinkConnection(Constants.CONFIG_TYPE.CHAT);
    }

    @Override
    public void setPresenter(ChatContract.Presenter presenter) {
        this.presenter = (BasePresenter) presenter;
    }

    /**
     * Sends a user defined message to a specific remote peer or to all remote peers via a server.
     *
     * @param remotePeerId Id of the remote peer to whom we will send a message. Use 'null' if the
     *                     message is to be broadcast to all remote peers in the room.
     * @param message      User defined data
     */
    public void sendServerMessage(String remotePeerId, Object message) {
        if (skylinkConnection != null) {

            skylinkConnection.sendServerMessage(message, remotePeerId, new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    presenter.processMessageSendFailed();

                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to sendServerMessage as " + contextDescription);
                }
            });
        }
    }

    /**
     * Sends a user defined message to a specific remote peer or to all remote peers in a direct
     * peer to peer manner.
     *
     * @param remotePeerId Id of the remote peer to whom we will send a message. Use 'null' if the
     *                     message is to be sent to all our remote peers in the room.
     * @param message      User defined data
     * @throws SkylinkException if the system was unable to send the message.
     */
    public void sendP2PMessage(String remotePeerId, Object message) {
        if (skylinkConnection != null) {
            skylinkConnection.sendP2PMessage(message, remotePeerId, new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to sendP2PMessage as " + contextDescription);
                }
            });
        }
    }

    /**
     * Sets the specified listeners for message function
     * Message function needs to implement LifeCycleListener, RemotePeerListener, MessagesListener
     */
    @Override
    public void setSkylinkListeners() {
        if (skylinkConnection != null) {
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setRemotePeerListener(this);
            skylinkConnection.setMessagesListener(this);
        }
    }

    /**
     * Get the config for message function
     * User can custom message config by using SkylinkConfig
     */
    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // Chat config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setP2PMessaging(true);

        // set to 7 remote peers connected as our UI just support maximum 8 peers
        skylinkConfig.setMaxRemotePeersConnected(MAX_REMOTE_PEER, SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);

        // Set the room size
        skylinkConfig.setSkylinkRoomSize(SkylinkConfig.SkylinkRoomSize.LARGE);

        // Set timeout for getting stored message
        skylinkConfig.setTimeout(SkylinkConfig.SkylinkAction.GET_STORED_MESSAGE, Utils.getDefaultNoOfStoredMsgTimeoutConfig() * 1000);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    /**
     * Get the info of a peer in specific index
     */
    public SkylinkPeer getPeerByIndex(int index) {
        return mPeersList.get(index);
    }

    public void disposeLocalMedia() {
        clearInstance();
    }

    public void setSelectedEncryptedSecret(String secretId) {
        if (skylinkConnection != null)
            skylinkConnection.setSelectedSecretId(secretId);
    }

    public void getStoredMessages() {

        final JSONArray[] messages = {new JSONArray()};

        if (skylinkConnection != null) {
            skylinkConnection.getStoredMessages(new SkylinkCallback.StoredMessages() {
                @Override
                public void onObtainStoredMessages(JSONArray storedMessages, Map<SkylinkError, JSONArray> errors) {
                    if (storedMessages != null) {
                        messages[0] = storedMessages;
                        Log.d(TAG, "result returned from stored msg history: " + storedMessages.toString());

                        presenter.processStoredMessagesResult(storedMessages);
                    }
                    if (errors != null && errors.size() > 0) {
                        Log.e(TAG, "errors from stored msg history: " + errors.toString());
                        Toast.makeText(context, "There are errors on stored messages! Please check log!", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    public void setStoreMessage(boolean toPersist) {
        skylinkConnection.setMessagePersist(toPersist);
    }

    public void setEncryptedMap(List<String> encryptionKeys, List<String> encryptionValues) {
        Map<String, String> encryptionMap = new HashMap<>();

        for (int i = 0; i < encryptionKeys.size(); i++) {
            encryptionMap.put(encryptionKeys.get(i), encryptionValues.get(i));
        }

        skylinkConnection.setEncryptSecretsMap(encryptionMap);
    }
}
