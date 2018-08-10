package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import android.content.Context;
import android.util.Log;

import sg.com.temasys.skylink.sdk.sampleapp.data.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.data.service.DataTransferService;

/**
 * Created by muoi.pham on 20/07/18.
 */

public abstract class DataTransferPresenter implements DataTransferContract.Presenter {

    private static final String TAG = DataTransferFragment.class.getName();

    private DataTransferContract.View mDataTransferView;
    private DataTransferService mDataTransferService;

    public DataTransferPresenter(DataTransferContract.View dataTransferView, Context context) {
        this.mDataTransferView = dataTransferView;
        mDataTransferService = new DataTransferService(context);

        this.mDataTransferView.setPresenter(this);
//        this.mDataTransferService.setPresenter(this);
    }

    //---------------------------------Listener for view usage-----------------------------------

    @Override
    public void setRoomDetailsPresenterHandler() {
//        String roomDetails = mDataTransferService.getRoomDetailsServiceHandler();
//        mDataTransferView.onUpdateUIViewHandler(roomDetails);
    }

    //---------------------------------Listener for service usage-----------------------------------

    @Override
    public void disconnectFromRoomPresenterHandler() {
//        mDataTransferService.disconnectFromRoomServiceHandler();
    }

    @Override
    public void connectToRoomPresenterHandler() {
//        mDataTransferService.connectToRoomServiceHandler();
    }

    @Override
    public void sendDataPresenterHandler(String remotePeerId, byte[] data) {
//        mDataTransferService.sendDataServiceHandler(remotePeerId, data);
    }

    @Override
    public boolean isConnectingOrConnectedPresenterHandler() {
//        return mDataTransferService.isConnectingOrConnectedServiceHandler();
        return false;
    }

//    @Override
    public void onViewLayoutRequestedPresenterHandler(boolean tryToConnect) {
        Log.d(TAG, "onViewLayoutRequestedPresenterHandler");

        if (tryToConnect) {
            //start to connect to room when entering room
            //if not being connected, then connect
//            if (!mDataTransferService.isConnectingOrConnectedServiceHandler()) {

                //connect to room on Skylink connection
//                mDataTransferService.connectToRoomServiceHandler();

                //after connected to skylink SDK, UI will be updated latter on AudioService.onConnect

                Log.d(TAG, "Try to connect when entering room");

//            } else {
//
//                //update UI into connected
//                updateUIPresenterHandler();
//
//                Log.d(TAG, "Try to update UI when changing configuration");
//            }

        } else {
            //process disconnect from room
//            mDataTransferService.disconnectFromRoomServiceHandler();

            //after disconnected from skylink SDK, UI will be updated latter on AudioService.onDisconnect

            Log.d(TAG, "Try to disconnect from room");
        }
    }

    @Override
    public void onViewLayoutRequestedPresenterHandler() {

    }

    @Override
    public void onViewExitPresenterHandler() {

    }

    public void onConnectPresenterHandler() {
        updateUIPresenterHandler();
    }

    @Override
    public void onDisconnectPresenterHandler() {

        mDataTransferView.clearPeerListViewHandler();

        updateUIPresenterHandler();

    }

    @Override
    public void onRemotePeerJoinPresenterHandler(String remotePeerId, String nick) {

        mDataTransferView.addPeerRadioBtnViewHandler(remotePeerId, nick);

        //Set room status if it's the only peer in the room.
        if (mDataTransferView.getPeerNumViewHandler() == 1) {
            updateRoomDetailsPresenterHandler();
        }
    }

    @Override
    public void onRemotePeerLeavePresenterHandler(String remotePeerId) {
        mDataTransferView.removePeerRadioBtnViewHandler(remotePeerId);

        //Set room status if there are no more peers.
        if (mDataTransferView.getPeerListSizeViewHandler() == 0) {
            updateRoomDetailsPresenterHandler();
        }
    }

    @Override
    public void onPermissionRequiredPresenterHandler(PermRequesterInfo info) {

    }

    private void updateUIPresenterHandler() {

        mDataTransferView.fillPeerRadioBtnViewHandler();

        updateRoomDetailsPresenterHandler();
    }

    private void updateRoomDetailsPresenterHandler() {
//        String strRoomDetails = mDataTransferService.getRoomDetailsServiceHandler();
//        mDataTransferView.onUpdateUIViewHandler(strRoomDetails);
    }
}

