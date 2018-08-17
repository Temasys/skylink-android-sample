package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface DataTransferContract {

    interface View extends BaseView<Presenter> {

        void onFillPeerRadioBtn(List<SkylinkPeer> peersList);

        void onAddPeerRadioBtn(SkylinkPeer newPeer);

        void onRemovePeerRadioBtn(String remotePeerId);

        void onUpdateRoomDetails(String roomDetails);

        String onGetPeerIdSelected();

        void onSetRdPeerAllChecked(boolean isChecked);

    }

    interface Presenter extends BasePresenter {

        void onSendData(String remotePeerId, byte[] data);

        void onDataReceive(String remotePeerId, byte[] data);
    }

    interface Service extends BaseService<Presenter> {


    }
}
