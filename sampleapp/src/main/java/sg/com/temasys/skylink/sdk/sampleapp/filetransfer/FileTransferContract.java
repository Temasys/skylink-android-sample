package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.support.v4.app.Fragment;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface FileTransferContract {

    interface View extends BaseView<Presenter> {

        void setRoomDetailsViewHandler(String roomDetails);

        void fillPeerRadioBtnViewHandler();

        void clearPeerListViewHandler();

        void onFileReceiveCompleteViewHandler(String msg);

        void addPeerRadioBtnViewHandler(String remotePeerId, String nick);

        int getPeerNumViewHandler();

        void removePeerRadioBtnViewHandler(String remotePeerId);

        int getPeerlistSizeViewHandler();

        Fragment getFragmentViewHandler();

        void setIsPeerJoinedViewHandler(boolean isPeerJoined);
    }

    interface Presenter extends BasePresenter {
        void setRoomDetailsPresenterHandler(boolean isPeerJoined);

        void sendFilePresenterHandler(String remotePeerId, String filePath);

        void disconnectFromRoomPresenterHandler();

        void connectToRoomPresenterHandler();

        void fillPeerRadioBtnPresenterHandler();
        
        void clearPeerListPresenterHandler();
        
        void onFileReceiveCompletePresenterHandler(String msg);
        
        void addPeerRadioBtnPresenterHandler(String remotePeerId, String nick);
        
        int getPeerNumPresenterHandler();

        void removePeerRadioBtnPresenterHandler(String remotePeerId);
        
        int getPeerlistSizePresenterHandler();

        Fragment getFragmentPresenterHandler();

        boolean isConnectingOrConnectedPresenterHandler();

        void setIsPeerJoinedPresenterHandler(boolean isPeerJoined);
    }

    interface Service extends BaseService<Presenter> {


    }

}
