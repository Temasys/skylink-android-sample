package sg.com.temasys.skylink.sdk.sampleapp.chat;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.MultiPeersInfo;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface ChatContract {

    interface View extends BaseView<Presenter> {

        void onListViewRefreshViewHandler();

        void fillPeerRadioBtnViewHandler(MultiPeersInfo peersList);

        void addPeerRadioBtnViewHandler(SkylinkPeer newPeer);

        void onRemovePeerRadioBtnViewHandler(String remotePeerId);

        void onUpdateRoomDetailsViewHandler(String roomDetails);

        void onClearEditTextViewHandler();
    }

    interface Presenter extends BasePresenter {

        void onSendServerMessagePresenterHandler(String remotePeerId, String message);

        void onSendP2PMessagePresenterHandler(String remotePeerId, String message);

        List<String> onGetChatMessageCollectionPresenterHandler();

        void onServerMessageReceivePresenterHandler(String remotePeerId, Object message, boolean isPrivate);

        void onP2PMessageReceivePresenterHandler(String remotePeerId, Object message, boolean isPrivate);
    }

    interface Service extends BaseService<Presenter> {

    }
}
