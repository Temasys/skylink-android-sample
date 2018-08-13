package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.content.Context;
import android.support.v4.app.Fragment;

import sg.com.temasys.skylink.sdk.sampleapp.data.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.data.service.FileTransferService;

/**
 * Created by muoi.pham on 20/07/18.
 */

public abstract class FileTransferPresenter implements FileTransferContract.Presenter{

    private FileTransferContract.View mFileTransferView;
    private FileTransferService mFileTransferService;

    public FileTransferPresenter(FileTransferContract.View fileTransferView, Context context) {
        this.mFileTransferView = fileTransferView;
        mFileTransferService = new FileTransferService(context);

        this.mFileTransferView.setPresenter(this);
//        this.mFileTransferService.setPresenter(this);
    }


    //---------------------------------Listener for view usage--------------------------------------

    @Override
    public void setRoomDetailsPresenterHandler(boolean isPeerJoined) {
//        String roomDetails = mFileTransferService.getRoomDetailsServiceHandler(isPeerJoined);
//        mFileTransferView.setRoomDetailsViewHandler(roomDetails);
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
    public void addPeerRadioBtnPresenterHandler(SkylinkPeer skylinkPeer) {
        mFileTransferView.addPeerRadioBtnViewHandler(skylinkPeer);
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
//        mFileTransferService.sendFileServiceHandler(remotePeerId, filePath);
    }

    @Override
    public void disconnectFromRoomPresenterHandler() {
//        mFileTransferService.disconnectFromRoomServiceHandler();
    }

    @Override
    public void connectToRoomPresenterHandler() {
//        mFileTransferService.connectToRoomServiceHandler();
    }

    @Override
    public boolean isConnectingOrConnectedPresenterHandler() {
//        return mFileTransferService.isConnectingOrConnectedServiceHandler();
        return false;
    }

//    @Override
    public void onViewLayoutRequestedPresenterHandler(boolean tryToConnect) {

    }

    @Override
    public void onViewLayoutRequestedPresenterHandler() {

    }

    @Override
    public void onViewExitPresenterHandler() {

    }

    public void onConnectPresenterHandler() {

    }

    @Override
    public void onDisconnectPresenterHandler() {

    }

    @Override
    public void onRemotePeerJoinPresenterHandler(SkylinkPeer skylinkPeer) {

    }

    @Override
    public void onRemotePeerLeavePresenterHandler(String remotePeerId) {

    }

//    @Override
    public void onPermissionRequiredPresenterHandler(PermRequesterInfo info) {

    }
}
