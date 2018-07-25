package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface DataTransferContract {
    interface View extends BaseView<Presenter> {

        void setRoomDetailsViewHandler(String roomDetails);

        void fillPeerRadioBtnViewHandler();

        void clearPeerListViewHandler();

        void addPeerRadioBtnViewHandler(String remotePeerId, String nick);

        void removePeerRadioBtnViewHandler(String remotePeerId);

        int getPeerlistSizeViewHandler();

        void setIsPeerJoinedViewHandler(boolean isPeerJoined);
    }

    interface Presenter extends BasePresenter {
        void setRoomDetailsPresenterHandler(boolean isPeerJoined);

        void disconnectFromRoomPresenterHandler();

        void connectToRoomPresenterHandler();

        void fillPeerRadioBtnPresenterHandler();

        void clearPeerListPresenterHandler();

        void addPeerRadioBtnPresenterHandler(String remotePeerId, String nick);

        void removePeerRadioBtnPresenterHandler(String remotePeerId);

        int getPeerlistSizePresenterHandler();

        void sendDataPresenterHandler(String remotePeerId, byte[] data);

        boolean isConnectingOrConnectedPresenterHandler();

        void saveIsPeerJoinedPresenterHandler(boolean peerJoined);

        void setIsPeerJoinedPresenterHandler(boolean isPeerJoined);
    }

    interface Service extends BaseService<Presenter> {


    }
}
