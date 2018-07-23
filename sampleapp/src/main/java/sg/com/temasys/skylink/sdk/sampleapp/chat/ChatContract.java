package sg.com.temasys.skylink.sdk.sampleapp.chat;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface ChatContract {

    interface View extends BaseView<Presenter> {

        void clearPeerListViewHandler();

        void fillPeerRadioBtnViewHandler();

        void addPeerRadioBtnViewHandler(String remotePeerId, String nick);

        int getPeerNumViewHandler();

        void removePeerRadioBtnViewHandler(String remotePeerId);

        int getPeerListSizeViewHandler();

        void listViewRefreshViewHandler();

        void setRoomDetailsViewHandler(String roomDetails);

        void clearChatMessageCollectionViewHandler();

        void addToChatMessageCollectionViewHandler(String s);

        void setMultiChatPeersInfoViewHandler(boolean isPeerJoined);
    }

    interface Presenter extends BasePresenter {

        //for service call
        void clearPeerListPresenterHandler();

        void fillPeerRadioBtnPresenterHandler() ;

        void addPeerRadioBtnPresenterHandler(String remotePeerId, String nick);

        int getPeerNumPresenterHandler();

        void removePeerRadioBtnPresenterHandler(String remotePeerId);

        int getPeerListSizePresenterHandler();

        void listViewRefreshPresenterHandler();

        //for view call
        void sendServerMessagePresenterHandler(String remotePeerId, String message);

        void sendP2PMessagePresenterHandler(String remotePeerId, String message);

        void disconnectFromRoomPresenterHandler();

        void connectToRoomPresenterHandler();

        void setRoomDetailsPresenterHandler(boolean isPeerInRoom);

        void setMultiChatPeersInfoPresenterHandler(boolean isPeerJoined);

        void saveIsPeerJoinPresenterHandler(boolean isPeerJoined);

        void clearChatMessageCollectionPresenterHandler();

        void addToChatMessageCollectionPresenterHandler(String s);

        boolean isConnectingOrConnectedPresenterHandler();
    }

    interface Service extends BaseService<Presenter> {

    }
}
