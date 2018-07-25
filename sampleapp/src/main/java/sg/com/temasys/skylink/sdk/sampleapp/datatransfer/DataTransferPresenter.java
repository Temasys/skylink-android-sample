package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import android.content.Context;

import sg.com.temasys.skylink.sdk.sampleapp.data.service.DataTransferService;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class DataTransferPresenter implements DataTransferContract.Presenter{

    private DataTransferContract.View mDataTransferView;
    private DataTransferService mDataTransferService;

    public DataTransferPresenter(DataTransferContract.View dataTransferView, Context context) {
        this.mDataTransferView = dataTransferView;
        mDataTransferService = new DataTransferService(context);

        this.mDataTransferView.setPresenter(this);
        this.mDataTransferService.setPresenter(this);
    }

    //---------------------------------Listener for view usage-----------------------------------

    @Override
    public void setRoomDetailsPresenterHandler(boolean isPeerJoined) {
        String roomDetails = mDataTransferService.getRoomDetailsServiceHandler(isPeerJoined);
        mDataTransferView.setRoomDetailsViewHandler(roomDetails);
    }

    @Override
    public void fillPeerRadioBtnPresenterHandler() {
        mDataTransferView.fillPeerRadioBtnViewHandler();
    }

    @Override
    public void clearPeerListPresenterHandler() {
        mDataTransferView.clearPeerListViewHandler();
    }

    @Override
    public void addPeerRadioBtnPresenterHandler(String remotePeerId, String nick) {
        mDataTransferView.addPeerRadioBtnViewHandler(remotePeerId, nick);
    }

    @Override
    public void removePeerRadioBtnPresenterHandler(String remotePeerId) {
        mDataTransferView.removePeerRadioBtnViewHandler(remotePeerId);
    }

    @Override
    public int getPeerlistSizePresenterHandler() {
        return mDataTransferView.getPeerlistSizeViewHandler();
    }

    @Override
    public void setIsPeerJoinedPresenterHandler(boolean isPeerJoined) {
        mDataTransferView.setIsPeerJoinedViewHandler(isPeerJoined);
    }

    //---------------------------------Listener for service usage-----------------------------------

    @Override
    public void disconnectFromRoomPresenterHandler() {
        mDataTransferService.disconnectFromRoomServiceHandler();
    }

    @Override
    public void connectToRoomPresenterHandler() {
        mDataTransferService.connectToRoomServiceHandler();
    }

    @Override
    public void sendDataPresenterHandler(String remotePeerId, byte[] data) {
        mDataTransferService.sendDataServiceHandler(remotePeerId, data);
    }

    @Override
    public boolean isConnectingOrConnectedPresenterHandler() {
        return mDataTransferService.isConnectingOrConnectedServiceHandler();
    }

    @Override
    public void saveIsPeerJoinedPresenterHandler(boolean isPeerJoined) {
        mDataTransferService.saveIsPeerJoinedServiceHandler(isPeerJoined);
    }

}

