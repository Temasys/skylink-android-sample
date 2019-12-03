package sg.com.temasys.skylink.sdk.sampleapp.chat;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
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
         * Update UI details when need to clear the input chat
         */
        void updateUIClearMessageInput();

        /**
         * Update UI details when we need to update the messages list when new message is sent/received
         */
        void updateUIChatCollection();
    }

    interface Presenter {

        /**
         * process data to display on view
         */
        void processConnectedLayout();

        /**
         * get list of chat message
         */
        List<String> processGetChatCollection();

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
    }

    interface Service extends BaseService<Presenter> {

    }
}
