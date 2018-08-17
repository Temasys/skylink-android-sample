package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.net.Uri;
import android.support.v4.app.Fragment;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface FileTransferContract {

    interface View extends BaseView<Presenter> {

        Fragment onGetFragment();

        void onAddPeerRadioBtn(SkylinkPeer newPeer);

        void onRemovePeerRadioBtn(String remotePeerId);

        void onFillPeerRadioBtn(List<SkylinkPeer> peersList);

        String onGetPeerIdSelected();

        void onSetRdPeerAllChecked(boolean isChecked);

        void onSetImagePreviewFromFile(Uri imgUri);

        void onUpdateTvFileTransferDetails(String info);

        void onUpdateRoomDetails(String roomDetails);
    }

    interface Presenter extends BasePresenter {

        void onPermissionRequired(PermRequesterInfo info);

        void onPermissionGranted(PermRequesterInfo info);

        void onPermissionDenied(PermRequesterInfo info);

        void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag);

        void onFileTransferPermissionResponse(String remotePeerId, String fileName, boolean isPermitted);

        void onFileTransferPermissionRequest(String remotePeerId, String fileName, boolean isPrivate);

        void onFileTransferDrop(String remotePeerId, String fileName, String message, boolean isExplicit);

        void onFileSendComplete(String remotePeerId, String fileName);

        void onFileReceiveComplete(String remotePeerId, String fileName);

        void onFileSendProgress(String remotePeerId, String fileName, double percentage);

        void onFileReceiveProgress(String remotePeerId, String fileName, double percentage);

        void onSendFile(String remotePeerId, String filePath);
    }

    interface Service extends BaseService<Presenter> {


    }

}
