package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.support.v4.app.Fragment;

import java.io.File;
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
         * Update UI details when complete sending file to remote peer
         */
        void onPresenterRequestFileSent();

        /**
         * Update UI when complete receiving file from remote peer
         */
        void onPresenterRequestFileReceived(SkylinkPeer remotePeer, String filePath);

        /**
         * Update UI while sending file to remote peer
         */
        void onPresenterRequestFileSendProgress(int percentage);

        /**
         * Update UI while receiving file from remote peer
         */
        void onPresenterRequestFileReceiveProgress(int percentage);

        /**
         * Update file preview with file path
         */
        void onPresenterRequestDisplayFilePreview(String filePath);
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
        void onViewRequestSendFile(File file);

        /**
         * process change state when view exit/closed
         */
        void onViewRequestExit();
    }

    interface Service extends BaseService<Presenter> {


    }

}
