package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.net.Uri;
import android.support.v4.app.Fragment;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 * This interface is responsible for specify behaviors of View, Presenter, Service
 */

public interface FileTransferContract {

    interface View extends BaseView<Presenter> {

        /**
         * Get instance of the fragment for processing permission
         */
        Fragment onPresenterRequestGetFragmentInstance();

        /**
         * Update UI details when remote peer joins the room
         */
        void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer);

        /**
         * Update UI details when remote peer leaves the room
         */
        void onPresenterRequestChangeUiRemotePeerLeave(String remotePeerId);

        /**
         * Update UI details when peers are in room
         */
        void onPresenterRequestFillPeers(List<SkylinkPeer> peersList);

        /**
         * Update UI details when need to display the file sent.
         */
        void onPresenterRequestDisplayFilePreview(Uri imgUri);

        /**
         * Update UI details when need to display the file transfered information.
         */
        void onPresenterRequestDisplayFileReveicedInfo(String info);

        /**
         * Update UI details with room information
         */
        void onPresenterRequestUpdateUi(String roomDetails);

        /**
         * get selected remote peer to send file
         */
        String onPresenterRequestGetPeerIdSelected();

        /**
         * set radio button Select All checked
         */
        void onPresenterRequestSetPeerAllSelected(boolean isSelected);
    }

    interface Presenter {

        /**
         * process data to display on view
         */
        void onViewRequestConnectedLayout();

        /**
         * process runtime file permission result
         */
        void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag);

        /**
         * process send file to remote peer
         */
        void onViewRequestSendFile(String remotePeerId, String filePath);

        /**
         * process change state when view exit/closed
         */
        void onViewRequestExit();

    }

    interface Service extends BaseService<Presenter> {


    }

}
