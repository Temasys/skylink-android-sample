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
         * Update UI details when we need to refresh the messages list
         */
        void onPresenterRequestRefreshChatCollection();

        /**
         * Update UI details when peers are in room
         */
        void onPresenterRequestFillPeers(List<SkylinkPeer> peersList);

        /**
         * Update UI details when remote peer joins the room
         */
        void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer);

        /**
         * Update UI details when remote peer leaves the room
         */
        void onPresenterRequestChangeUiRemotePeerLeave(String remotePeerId);

        /**
         * Update UI details with room information
         */
        void onPresenterRequestUpdateUi(String roomDetails);

        /**
         * Update UI details when need to clear the input
         */
        void onPresenterRequestClearInput();
    }

    interface Presenter {

        /**
         * process data to display on view
         */
        void onViewRequestConnectedLayout();

        /**
         * process send message through server to remote peer
         */
        void onViewRequestSendServerMessage(String remotePeerId, String message);

        /**
         * process send message directly to remote peer (not through server)
         */
        void onViewRequestSendP2PMessage(String remotePeerId, String message);

        /**
         * get list of chat message
         */
        List<String> onViewRequestGetChatCollection();

        /**
         * process change state when view exit/closed
         */
        void onViewRequestExit();

    }

    interface Service extends BaseService<Presenter> {

    }
}
