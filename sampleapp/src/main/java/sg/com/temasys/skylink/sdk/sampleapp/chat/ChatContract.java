package sg.com.temasys.skylink.sdk.sampleapp.chat;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.MessageModel;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 * This interface is responsible for specify behaviors of View, Presenter, Service
 */

public interface ChatContract {

    interface View extends BaseView<Presenter> {

        /**
         * Update UI into connected state
         */
        void updateUIConnected(String roomId);

        /**
         * Update UI into disconnected state
         */
        void updateUIDisconnected();

        /**
         * Update UI details when new remote peer joins at a specific index the room
         */
        void updateUIRemotePeerConnected(SkylinkPeer newPeer, int index);

        /**
         * Update UI details when remote peer left the room
         */
        void updateUIRemotePeerDisconnected(List<SkylinkPeer> peersList);

        /**
         * Update UI details when we need to update the messages list when new message is sent/received
         */
        void updateUIChatCollection(boolean isLocalMessaege);

        void updateUIEncryptionKeys(List<String> encryptionKeyList);

        void getStoredServerMessages();

        void initUIEncryptionSelectedKey(String storedSelectedEncryptionKey, String storedSelectedEncryptionValue, int pos);

        void initUIStoreMessageSetting(boolean storedMessageSetting);

        void initUIEncryptionKeys(List<String> encryptionList);

        void initMessageFormats(List<String> messageFormatList);
    }

    interface Presenter {

        /**
         * process data to display on view
         */
        void processConnectedLayout();

        /**
         * get list of chat message
         */
        List<MessageModel> processGetChatCollection();

        /**
         * process change state when view exit/closed
         */
        void processExit();

        /**
         * process get peer info at specific index
         */
        SkylinkPeer processGetPeerByIndex(int index);

        /**
         * process get current selected peer index
         */
        int processGetCurrentSelectedPeer();

        /**
         * process selecting the specific remote peer to send message to
         */
        void processSelectRemotePeer(int index);

        /**
         * process selecting message type: server or P2P
         */
        void processSelectMessageType(ChatPresenter.MESSAGE_TYPE message_type);

        /**
         * process sending message
         */
        void processSendMessage(String message);

        void processSelectMessageFormat(ChatPresenter.MESSAGE_FORMAT formatMsg);

        void processAddEncryption(String enryptionKey, String encryptionValue);

        String processGetEncryptionValueFromKey(String encryptionKey);

        void processGetStoredSeverMessages();

        void processStoreMessageSet(boolean isChecked);

        void processSelectSecretKey(String secretKey);

        void processDeleteEncryption(String enryptionKey, String encryptionValue);
    }

    interface Service extends BaseService<Presenter> {

    }
}
