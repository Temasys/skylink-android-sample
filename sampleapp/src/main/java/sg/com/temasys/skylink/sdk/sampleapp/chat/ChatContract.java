package sg.com.temasys.skylink.sdk.sampleapp.chat;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface ChatContract {

    interface View extends BaseView<Presenter> {

        void onListViewRefresh();

        void fillPeerRadioBtn(List<SkylinkPeer> peersList);

        void addPeerRadioBtn(SkylinkPeer newPeer);

        void onRemovePeerRadioBtn(String remotePeerId);

        void onUpdateRoomDetails(String roomDetails);

        void onClearEditText();
    }

    interface Presenter extends BasePresenter {

        void onSendServerMessage(String remotePeerId, String message);

        void onSendP2PMessage(String remotePeerId, String message);

        List<String> onGetChatMessageCollection();

        void onServerMessageReceive(String remotePeerId, Object message, boolean isPrivate);

        void onP2PMessageReceive(String remotePeerId, Object message, boolean isPrivate);
    }

    interface Service extends BaseService<Presenter> {

    }
}
