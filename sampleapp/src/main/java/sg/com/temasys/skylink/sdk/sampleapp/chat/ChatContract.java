package sg.com.temasys.skylink.sdk.sampleapp.chat;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface ChatContract {

    interface View extends BaseView<Presenter> {

        /**
         * process update view when changing state
         */
        void onPresenterRequestRefreshChatCollection();

        void onPresenterRequestFillPeers(List<SkylinkPeer> peersList);

        void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer);

        void onPresenterRequestChangeUiRemotePeerLeave(String remotePeerId);

        void onPresenterRequestUpdateUi(String roomDetails);

        void onPresenterRequestClearInput();
    }

    interface Presenter {

        /**
         * process data to display on view
         */
        void onViewRequestLayout();

        /**
         * process send message through server to remote peer
         */
        void onViewRequestSendServerMessage(String remotePeerId, String message);

        /**
         * process send message directly to remote peer
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
