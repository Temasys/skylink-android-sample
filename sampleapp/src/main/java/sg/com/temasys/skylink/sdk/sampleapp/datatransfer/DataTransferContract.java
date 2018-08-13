package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface DataTransferContract {
    interface View extends BaseView<Presenter> {

        void fillPeerRadioBtnViewHandler();

        void clearPeerListViewHandler();

        void addPeerRadioBtnViewHandler(SkylinkPeer skylinkPeer);

        void removePeerRadioBtnViewHandler(String remotePeerId);

        void onUpdateUIViewHandler(String strRoomDetails);

        int getPeerNumViewHandler();

        int getPeerListSizeViewHandler();
    }

    interface Presenter extends BasePresenter {

        void setRoomDetailsPresenterHandler();

        void disconnectFromRoomPresenterHandler();

        void connectToRoomPresenterHandler();

        void sendDataPresenterHandler(String remotePeerId, byte[] data);

        boolean isConnectingOrConnectedPresenterHandler();

    }

    interface Service extends BaseService<Presenter> {


    }
}
