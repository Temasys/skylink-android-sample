package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface DataTransferContract {

    interface View extends BaseView<Presenter> {

        /**
         * Update UI details when changing state
         */
        void onPresenterRequestFillPeers(List<SkylinkPeer> peersList);

        void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer);

        void onPresenterRequestChangeUiRemotePeerLeave(String remotePeerId);

        void onPresenterRequestUpdateUi(String roomDetails);

        /**
         * get selected remote peer
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
        void onViewRequestLayout();

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
