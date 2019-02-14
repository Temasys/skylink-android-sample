package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import android.support.v4.app.Fragment;

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
         * Get instance of the fragment for processing runtime permission
         */
        Fragment onPresenterRequestGetFragmentInstance();

        /**
         * Update UI into connected state
         */
        void onPresenterRequestUpdateUIConnected(String roomId);

        /**
         * Update UI details when new remote peer joins at a specific index the room
         */
        void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer, int index);

        /**
         * Update UI details when remote peer left the room
         */
        void onPresenterRequestChangeUiRemotePeerLeft(List<SkylinkPeer> peersList);

        /**
         * Update UI when received data from remote peer
         * */
        void onPresenterRequestChangeUIReceivedData(SkylinkPeer remotePeer, byte[] data);
    }

    interface Presenter {

        /**
         * request a init connection
         */
        void onViewRequestConnectedLayout();

        /**
         * request runtime permission for read internal storage
         */
        boolean onViewRequestFilePermission();

        /**
         * process runtime file permission result
         */
        void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

        /**
         * process logic when user deny the permission
         */
        void onViewRequestPermissionDeny();

        /**
         * process selecting the specific remote peer to send message to
         */
        void onViewRequestSelectedRemotePeer(int index);

        /**
         * process get current selected peer index
         */
        int onViewRequestGetCurrentSelectedPeer();

        /**
         * process get peer info at specific index
         */
        SkylinkPeer onViewRequestGetPeerByIndex(int index);

        /**
         * process send file to remote peer
         */
        void onViewRequestSendData(byte[] data);

        /**
         * process change state when view exit/closed
         */
        void onViewRequestExit();
    }

    interface Service extends BaseService<Presenter> {


    }
}
