package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.content.Context;
import android.support.v4.app.Fragment;

import sg.com.temasys.skylink.sdk.sampleapp.data.service.FileTransferService;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class FileTransferPresenter implements FileTransferContract.Presenter{

    private FileTransferContract.View mFileTransferView;
    private FileTransferService mFileTransferService;

    public FileTransferPresenter(FileTransferContract.View fileTransferView, Context context) {
        this.mFileTransferView = fileTransferView;
        mFileTransferService = new FileTransferService(context);

        this.mFileTransferView.setPresenter(this);
        this.mFileTransferService.setPresenter(this);
    }


    //---------------------------------Listener for view usage--------------------------------------

    @Override
    public void setRoomDetailsPresenterHandler(boolean isPeerJoined) {
        String roomDetails = mFileTransferService.getRoomDetailsServiceHandler(isPeerJoined);
        mFileTransferView.setRoomDetailsViewHandler(roomDetails);
    }

    @Override
    public void fillPeerRadioBtnPresenterHandler() {
        mFileTransferView.fillPeerRadioBtnViewHandler();
    }

    @Override
    public void clearPeerListPresenterHandler() {
        mFileTransferView.clearPeerListViewHandler();
    }

    @Override
    public void onFileReceiveCompletePresenterHandler(String msg) {
        mFileTransferView.onFileReceiveCompleteViewHandler(msg);
    }

    @Override
    public void addPeerRadioBtnPresenterHandler(String remotePeerId, String nick) {
        mFileTransferView.addPeerRadioBtnViewHandler(remotePeerId, nick);
    }

    @Override
    public int getPeerNumPresenterHandler() {
        return mFileTransferView.getPeerNumViewHandler();
    }

    @Override
    public void removePeerRadioBtnPresenterHandler(String remotePeerId) {
        mFileTransferView.removePeerRadioBtnViewHandler(remotePeerId);
    }

    @Override
    public int getPeerlistSizePresenterHandler() {
        return mFileTransferView.getPeerlistSizeViewHandler();
    }

    @Override
    public Fragment getFragmentPresenterHandler() {
        return mFileTransferView.getFragmentViewHandler();
    }

    @Override
    public void setIsPeerJoinedPresenterHandler(boolean isPeerJoined) {
        mFileTransferView.setIsPeerJoinedViewHandler(isPeerJoined);
    }


    //---------------------------------Listener for service usage-----------------------------------

    @Override
    public void sendFilePresenterHandler(String remotePeerId, String filePath) {
        mFileTransferService.sendFileServiceHandler(remotePeerId, filePath);
    }

    @Override
    public void disconnectFromRoomPresenterHandler() {
        mFileTransferService.disconnectFromRoomServiceHandler();
    }

    @Override
    public void connectToRoomPresenterHandler() {
        mFileTransferService.connectToRoomServiceHandler();
    }

    @Override
    public boolean isConnectingOrConnectedPresenterHandler() {
        return mFileTransferService.isConnectingOrConnectedServiceHandler();
    }

}
