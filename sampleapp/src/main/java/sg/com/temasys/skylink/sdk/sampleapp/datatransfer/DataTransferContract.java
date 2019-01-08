package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 * This interface is responsible for specify behaviors of View, Presenter, Service
 */

public interface DataTransferContract {

    interface View extends BaseView<Presenter> {

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
         * get selected remote peer to send data
         */
        String onPresenterRequestGetPeerIdSelected();

        /**
         * set button Select all checked
         */
        void onPresenterRequestSetPeerAllSelected(boolean isSelected);

    }

    interface Presenter {

        /**
         * process data to display on view
         */
        void onViewRequestConnectedLayout();

        /**
         * process send data to remote Peer
         */
        void onViewRequestSendData(String remotePeerId, byte[] data);

        /**
         * process change state when view exit/closed
         */
        void onViewRequestExit();
    }

    interface Service extends BaseService<Presenter> {


    }
}
