package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import android.content.Context;
import android.util.Log;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.DataTransferService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class DataTransferPresenter extends BasePresenter implements DataTransferContract.Presenter {

    private final String TAG = DataTransferPresenter.class.getName();

    private Context mContext;

    private DataTransferContract.View mDataTransferView;
    private DataTransferService mDataTransferService;

    public DataTransferPresenter(Context context) {
        this.mContext = context;
        this.mDataTransferService = new DataTransferService(context);
        this.mDataTransferService.setPresenter(this);
    }

    public void setView(DataTransferContract.View view) {
        mDataTransferView = view;
        mDataTransferView.setPresenter(this);
    }

    /**
     * Triggered when View request data to display to the user when entering room | rotating screen
     * Try to connect to room when entering room
     * Try to update UI if connected to room after changing configuration
     */
    @Override
    public void onViewRequestConnectedLayout() {

        Log.d(TAG, "onViewLayoutRequested");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mDataTransferService.isConnectingOrConnected()) {

            //connect to room on Skylink connection
            mDataTransferService.connectToRoom(Constants.CONFIG_TYPE.DATA);

            //after connected to skylink SDK, UI will be updated later on ChatService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //update UI into connected state
            processUpdateUI();

            Log.d(TAG, "Try to update UI when changing configuration");
        }
    }

    @Override
    public void onViewRequestExit() {

        //process disconnect from room
        mDataTransferService.disconnectFromRoom();
    }

    @Override
    public void onViewRequestSendData(String remotePeerId, byte[] data) {
        // Do not allow button actions if there are no remote Peers in the room.
        if (mDataTransferService.getTotalPeersInRoom() < 2) {
            String log = mContext.getString(R.string.warn_no_peer_message);
            toastLog(TAG, mContext, log);
            return;
        }

        if (remotePeerId == null) {
            // Select All Peers RadioButton if not already selected
            String remotePeer = mDataTransferView.onPresenterRequestGetPeerIdSelected();

            //force to select radio button peerAll
            if (remotePeer != null) {
                mDataTransferView.onPresenterRequestSetPeerAllSelected(true);
            }
        }

        mDataTransferService.sendData(remotePeerId, data);
    }

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful)
            processUpdateUI();
    }

    @Override
    public void onServiceRequestRemotePeerJoin(SkylinkPeer newPeer) {

        //add new remote peer
        mDataTransferView.onPresenterRequestChangeUiRemotePeerJoin(newPeer);

        // Update textview to show room status when first remote peer has joined with self peer
        if (mDataTransferService.getTotalPeersInRoom() == 2) {
            processUpdateRoomDetails();
        }
    }

    @Override
    public void onServiceRequestRemotePeerLeave(String remotePeerId, int removeIndex) {
        // Remove remote peer
        mDataTransferView.onPresenterRequestChangeUiRemotePeerLeave(remotePeerId);

        // Update textview to show room status when last remote peer has left
        if (mDataTransferService.getTotalPeersInRoom() == 1) {
            processUpdateRoomDetails();
        }
    }

    private void processUpdateUI() {

        mDataTransferView.onPresenterRequestFillPeers(mDataTransferService.getPeersList());

        processUpdateRoomDetails();
    }

    private void processUpdateRoomDetails() {
        String strRoomDetails = processGetRoomDetails();
        mDataTransferView.onPresenterRequestUpdateUi(strRoomDetails);
    }

    private String processGetRoomDetails() {
        boolean isConnected = mDataTransferService.isConnectingOrConnected();
        String roomName = mDataTransferService.getRoomName(Config.ROOM_NAME_DATA);
        String userName = mDataTransferService.getUserName(null, Config.USER_NAME_DATA);

        boolean isPeerJoined = mDataTransferService.isPeerJoin();

        String roomDetails = "You are not connected to any room";

        if (isConnected) {
            roomDetails = "Now connected to Room named : " + roomName
                    + "\n\nYou are signed in as : " + userName + "\n";
            if (isPeerJoined) {
                roomDetails += "\nPeer(s) are in the room";
            } else {
                roomDetails += "\nYou are alone in this room";
            }
        }

        return roomDetails;
    }
}

