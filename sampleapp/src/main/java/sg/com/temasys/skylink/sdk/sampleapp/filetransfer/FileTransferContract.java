package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.net.Uri;
import android.support.v4.app.Fragment;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface FileTransferContract {

    interface View extends BaseView<Presenter> {

        /**
         * Get instance of the fragment for processing permission
         */
        Fragment onPresenterRequestGetFragmentInstance();

        /**
         * Update UI details when changing state
         */
        void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer);

        void onPresenterRequestChangeUiRemotePeerLeave(String remotePeerId);

        void onPresenterRequestFillPeers(List<SkylinkPeer> peersList);

        void onPresenterRequestDisplayFilePreview(Uri imgUri);

        void onPresenterRequestDisplayFileReveicedInfo(String info);

        void onPresenterRequestUpdateUi(String roomDetails);

        /**
         * get selected remote peer
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
         * process permission result
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
