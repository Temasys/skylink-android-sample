package sg.com.temasys.skylink.sdk.sampleapp.chat;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface ChatContract {

    interface View extends BaseView<Presenter> {

        void clearPeerListViewHandler();

        void fillPeerRadioBtnViewHandler();

        void addPeerRadioBtnViewHandler(SkylinkPeer skylinkPeer);

        int getPeerNumViewHandler();

        void removePeerRadioBtnViewHandler(String remotePeerId);

        int getPeerListSizeViewHandler();

        void listViewRefreshViewHandler();

        void clearChatMessageCollectionViewHandler();

        void addToChatMessageCollectionViewHandler(String s);

        void onUpdateUIViewHandler(String roomDetails);
    }

    interface Presenter extends BasePresenter {

        void sendServerMessagePresenterHandler(String remotePeerId, String message);

        void sendP2PMessagePresenterHandler(String remotePeerId, String message);

        void disconnectFromRoomPresenterHandler();

        void connectToRoomPresenterHandler();

        void setRoomDetailsPresenterHandler();

        boolean isConnectingOrConnectedPresenterHandler();

        void onMessageReceivePresenterHandler(String msg);
    }

    interface Service extends BaseService<Presenter> {

    }
}
