package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import androidx.fragment.app.Fragment;

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
        Fragment getInstance();

        /**
         * Update UI into connected state
         */
        void updateUIConnected(String roomId);

        /**
         * Update UI into disconnected state
         */
        void updateUIDisconnected();

        /**
         * Update UI details when new remote peer joins at a specific index the room
         */
        void updateUIRemotePeerConnected(SkylinkPeer newPeer, int index);

        /**
         * Update UI details when remote peer left the room
         */
        void updateUIRemotePeerDisconnected(List<SkylinkPeer> peersList);

        /**
         * Update UI when received data from remote peer
         */
        void updateUIDataReceived(SkylinkPeer remotePeer, byte[] data);
    }

    interface Presenter {

        /**
         * request a init connection
         */
        void processConnectedLayout();

        /**
         * request runtime permission for read internal storage
         */
        boolean processFilePermission();

        /**
         * process runtime file permission result
         */
        void processPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

        /**
         * process logic when user deny the permission
         */
        void processDenyPermission();

        /**
         * process selecting the specific remote peer to send message to
         */
        void processSelectRemotePeer(int index);

        /**
         * process get current selected peer index
         */
        int processGetCurrentSelectedPeer();

        /**
         * process get peer info at specific index
         */
        SkylinkPeer processGetPeerByIndex(int index);

        /**
         * process send file to remote peer
         */
        void processSendData(byte[] data);

        /**
         * process change state when view exit/closed
         */
        void processExit();
    }

    interface Service extends BaseService<Presenter> {


    }
}
