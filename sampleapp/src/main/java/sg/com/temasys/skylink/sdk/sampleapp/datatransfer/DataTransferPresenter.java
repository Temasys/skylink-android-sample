package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;

import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.service.DataTransferService;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class DataTransferPresenter implements DataTransferContract.Presenter {

    private final String TAG = DataTransferPresenter.class.getName();

    private Context mContext;

    private DataTransferContract.View mDataTransferView;
    private DataTransferService mDataTransferService;


    public DataTransferPresenter(DataTransferContract.View dataTransferView, Context context) {
        this.mContext = context;

        this.mDataTransferView = dataTransferView;
        this.mDataTransferService = new DataTransferService(context);

        this.mDataTransferView.setPresenter(this);
        this.mDataTransferService.setPresenter(this);

        this.mDataTransferService.setTypeCall();
    }


    /**
     * Triggered when View request data to display to the user when entering room | rotating screen
     * Try to connect to room when entering room
     * Try to update UI if connected to room after changing configuration
     */
    @Override
    public void onViewLayoutRequested() {

        Log.d(TAG, "onViewLayoutRequested");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mDataTransferService.isConnectingOrConnected()) {

            //connect to room on Skylink connection
            mDataTransferService.connectToRoom();

            //after connected to skylink SDK, UI will be updated later on ChatService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //update UI into connected state
            updateUI();

            Log.d(TAG, "Try to update UI when changing configuration");
        }
    }

    @Override
    public void onConnect(boolean isSuccessful) {
        updateUI();
    }

    @Override
    public void onDisconnect() {

        //do nothing

    }

    @Override
    public void onViewExit() {

        //process disconnect from room
        mDataTransferService.disconnectFromRoom();

        //after disconnected from skylink SDK, UI will be updated later on ChatService.onDisconnect
    }

    @Override
    public void onRemotePeerJoin(SkylinkPeer newPeer) {

        //add new remote peer
        mDataTransferView.onAddPeerRadioBtn(newPeer);

        // Update textview to show room status when first remote peer has joined with self peer
        if (mDataTransferService.getTotalPeersInRoom() == 2) {
            updateRoomDetails();
        }
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId) {
        // Remove remote peer
        mDataTransferView.onRemovePeerRadioBtn(remotePeerId);

        // Update textview to show room status when last remote peer has left
        if (mDataTransferService.getTotalPeersInRoom() == 1) {
            updateRoomDetails();
        }
    }

    @Override
    public void onSendData(String remotePeerId, byte[] data) {
        // Do not allow button actions if there are no remote Peers in the room.
        if (mDataTransferService.getTotalPeersInRoom() < 2) {
            String log = mContext.getString(R.string.warn_no_peer_message);
            toastLog(TAG, mContext, log);
            return;
        }

        if(remotePeerId == null) {
            // Select All Peers RadioButton if not already selected
            String remotePeer = mDataTransferView.onGetPeerIdSelected();

            //force to select radio button peerAll
            if (remotePeer != null) {
                mDataTransferView.onSetRdPeerAllChecked(true);
            }
        }

        mDataTransferService.sendData(remotePeerId, data);
    }

    @Override
    public void onDataReceive(String remotePeerId, byte[] data) {
        // Check if it is one of the data that we can send.
        if (Arrays.equals(data, Utils.getDataPrivate()) || Arrays.equals(data, Utils.getDataGroup())) {
            String log = String.format(Utils.getString(R.string.data_transfer_received_expected),
                    String.valueOf(data.length));
            toastLog(TAG, mContext, log);
        } else {
            // Received some unexpected data that could be from other apps
            // or perhaps different due to so some problems somewhere.
            String log = String.format(Utils.getString(R.string.data_transfer_received_unexpected),
                    String.valueOf(data.length));
            toastLogLong(TAG, mContext, log);
        }
    }


    private void updateUI() {

        mDataTransferView.onFillPeerRadioBtn(mDataTransferService.getPeersList());

        updateRoomDetails();
    }

    private void updateRoomDetails() {
        String strRoomDetails = getRoomDetails();
        mDataTransferView.onUpdateRoomDetails(strRoomDetails);
    }

    private String getRoomDetails() {
        boolean isConnected = mDataTransferService.isConnectingOrConnected();
        String roomName = mDataTransferService.getRoomName(Config.ROOM_NAME_CHAT);
        String userName = mDataTransferService.getUserName(null, Config.USER_NAME_CHAT);

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

