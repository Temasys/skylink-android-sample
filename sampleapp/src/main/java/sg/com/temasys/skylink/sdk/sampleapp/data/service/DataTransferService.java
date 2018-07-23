package sg.com.temasys.skylink.sdk.sampleapp.data.service;

import android.content.Context;
import android.util.Log;
import java.util.Arrays;
import java.util.Date;

import sg.com.temasys.skylink.sdk.listener.DataTransferListener;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.datatransfer.DataTransferContract;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class DataTransferService extends SDKService implements DataTransferContract.Service, RemotePeerListener, DataTransferListener,
        LifeCycleListener{

    private static final String TAG = DataTransferService.class.getName();

    private Context mContext;

    private DataTransferContract.Presenter mPresenter;

    private SdkConnectionManager sdkConnectionManager;

    private static SkylinkConnection skylinkConnection;

    private String ROOM_NAME;
    private String MY_USER_NAME;

    private boolean isPeerJoined;

    private byte[] dataPrivate;
    private byte[] dataGroup;


    public DataTransferService(Context mContext) {
        this.mContext = mContext;
        ROOM_NAME = Config.ROOM_NAME_DATA;
        MY_USER_NAME = Config.USER_NAME_DATA;
    }

    @Override
    public void setPresenter(DataTransferContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    //----------------------------------------------------------------------------------------------
    // DataTransferService method implementation to work with SkylinkSDK
    //----------------------------------------------------------------------------------------------

    public void connectToRoomServiceHandler() {
        sdkConnectionManager = new SdkConnectionManager(mContext);
        skylinkConnection = sdkConnectionManager.initializeSkylinkConnectionForDataTransfer();

        setListeners();

        String skylinkConnectionString = Utils.getSkylinkConnectionString(
                ROOM_NAME, new Date(), SkylinkConnection.DEFAULT_DURATION);

        boolean connectFailed = !connectToRoomBaseServiceHandler(skylinkConnection, skylinkConnectionString, Config.USER_NAME_DATA);

        if (connectFailed) {
            String log = "Unable to connect to room!";
            toastLog(TAG, mContext, log);
            return;
        } else {
            String log = "Connecting...";
            toastLog(TAG, mContext, log);
        }
    }

    public void disconnectFromRoomServiceHandler() {
        if(skylinkConnection != null && isConnectingOrConnectedServiceHandler()){
            disconnectFromRoomBaseServiceHandler(skylinkConnection);
        }
    }

    private void onConnectUIChangeServiceHandler(){
        mPresenter.setRoomDetailsPresenterHandler(isPeerJoined);
        mPresenter.fillPeerRadioBtnPresenterHandler();
    }

    /**
     * Set dataGroup to contain 2 of dataPrivate.
     * Will get dataPrivate if dataGroup and dataPrivate are null.
     * same sample data with DataTransferFragment
     */
    private void getDataTranferedServiceHandler() {
        dataPrivate = Utils.getDataPrivate();
        dataGroup = Utils.getDataGroup();
    }

    private int getNumRemotePeersServiceHandler(){
        int totalInRoom = getTotalInRoomServiceHandler();
        if (totalInRoom == 0) {
            return 0;
        }
        // The first Peer is the local Peer.
        return totalInRoom - 1;
    }

    public int getTotalInRoomServiceHandler() {
        String[] peerIdList = getPeerIdListBaseServiceHandler(skylinkConnection);
        if (peerIdList == null) {
            return 0;
        }
        // Size of array is number of Peers in room.
        return peerIdList.length;
    }

    public void sendDataServiceHandler(String remotePeerId, byte[] data) {
        try {
            sendDataBaseServiceHandler(skylinkConnection, remotePeerId, data);
        } catch (SkylinkException e) {
            String log = e.getMessage();
            toastLogLong(TAG, mContext, log);
        } catch (UnsupportedOperationException e) {
            String log = e.getMessage();
            toastLogLong(TAG, mContext, log);
        }
    }

    public boolean isConnectingOrConnectedServiceHandler() {
        if (skylinkConnection != null) {
            return isConnectingOrConnectedBaseServiceHandler(skylinkConnection);
        }
        return false;
    }

    public String getRoomDetailsServiceHandler(boolean isPeerJoined) {
        boolean isConnected = isConnectingOrConnectedServiceHandler();
        String roomName = getRoomNameBaseServiceHandler(skylinkConnection, Config.ROOM_NAME_DATA);
        String userName = getUserNameBaseServiceHandler(skylinkConnection, null, Config.USER_NAME_DATA);

        String roomDetails = "You are not connected to any room";

        if (isConnected) {
            roomDetails = "Now connected to Room named : " + roomName
                    + "\nYou are signed in as : " + userName + "\n";
            if (isPeerJoined) {
                roomDetails += "Peer(s) are in the room";
            } else {
                roomDetails += "You are alone in this room";
            }
        }

        return roomDetails;
    }

    public void saveIsPeerJoinedServiceHandler(boolean isPeerJoined) {
        this.isPeerJoined = isPeerJoined;
    }

    //----------------------------------------------------------------------------------------------
    // Skylink Listeners
    //----------------------------------------------------------------------------------------------

    /**
     * Set listeners to receive callbacks when events are triggered.
     * SkylinkConnection instance must not be null or listeners cannot be set.
     * Do not set before {@link SkylinkConnection#init} as that will remove all existing Listeners.
     *
     * @return false if listeners could not be set.
     */
    private boolean setListeners() {
        if (skylinkConnection != null) {
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setRemotePeerListener(this);
            skylinkConnection.setDataTransferListener(this);
            return true;
        } else {
            return false;
        }
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's
     * lifecycle
     */

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        //Update textview if connection is successful
        if (isSuccessful) {
            String log = "Connected to room " + ROOM_NAME + " (" + getRoomIdBaseServiceHandler(skylinkConnection) +
                    ") as " + getPeerIdBaseServiceHandler(skylinkConnection) + " (" + MY_USER_NAME + ").";
            toastLogLong(TAG, mContext, log);
            // [MultiParty]
            // Set the appropriate UI if already isConnected().
            onConnectUIChangeServiceHandler();

            getDataTranferedServiceHandler();

        } else {
            String log = "Skylink failed to connect!\nReason : " + message;
            toastLogLong(TAG, mContext, log);
            mPresenter.setRoomDetailsPresenterHandler(isPeerJoined);
        }
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        // [MultiParty]
        // Reset peerList
        mPresenter.clearPeerListPresenterHandler();
        // Set the appropriate UI after disconnecting.
        onConnectUIChangeServiceHandler();
        String log = "[onDisconnect] ";
        if (errorCode == Errors.DISCONNECT_FROM_ROOM) {
            log += "We have successfully disconnected from the room.";
        } else if (errorCode == Errors.DISCONNECT_UNEXPECTED_ERROR) {
            log += "WARNING! We have been unexpectedly disconnected from the room!";
        }
        log += " Server message: " + message;
        toastLogLong(TAG, mContext, log);
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        String log = "[SA] Peer " + remotePeerId + " changed Room locked status to "
                + lockStatus + ".";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onReceiveLog(int infoCode, String message) {
        Utils.handleSkylinkReceiveLog(infoCode, message, mContext, TAG);
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Utils.handleSkylinkWarning(errorCode, message, mContext, TAG);
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        // [MultiParty]
        //When remote peer joins room, keep track of user and update UI.
        // If Peer has no userData, use an empty string for nick.
        String nick = "";
        if (userData != null) {
            nick = userData.toString();
        }
        mPresenter.addPeerRadioBtnPresenterHandler(remotePeerId, nick);
        //Set room status if it's the only peer in the room.
        if (mPresenter.getPeerlistSizePresenterHandler() == 1) {
            isPeerJoined = true;
            // Update textview to show room status
            mPresenter.setRoomDetailsPresenterHandler(isPeerJoined);
            mPresenter.setIsPeerJoinedPresenterHandler(isPeerJoined);
        }
        String log = "Your Peer " + getPeerIdNickBaseServiceHandler(skylinkConnection, remotePeerId) + " connected.";
        toastLog(TAG, mContext, log);

        //create sample data to transfer
        // Show info about data sizes that can be transferred.
//        getDataGroup();
//        transferStatus.setText(String.format(getString(R.string.data_transfer_status),
//                String.valueOf(dataPrivate.length), String.valueOf(dataGroup.length)));
        //
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message, UserInfo userInfo) {
        // [MultiParty]
        // Remove the Peer.
        mPresenter.removePeerRadioBtnPresenterHandler(remotePeerId);

        //Set room status if there are no more peers.
        if (mPresenter.getPeerlistSizePresenterHandler() == 0) {
            isPeerJoined = false;
            // Update textview to show room status
            mPresenter.setRoomDetailsPresenterHandler(isPeerJoined);
        }

        int numRemotePeers = getNumRemotePeersServiceHandler();
        String log = "Your Peer " + getPeerIdNickBaseServiceHandler(skylinkConnection, remotePeerId, userInfo) + " left: " +
                message + ". " + numRemotePeers + " remote Peer(s) left in the room.";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onRemotePeerConnectionRefreshed(String remotePeerId, Object userData,
                                                boolean hasDataChannel, boolean wasIceRestarted) {
        String peer = "Skylink Media Relay server";
        if (remotePeerId != null) {
            peer = "Peer " + getPeerIdNickBaseServiceHandler(skylinkConnection, remotePeerId);
        }
        String log = "Your connection with " + peer + " has just been refreshed";
        if (wasIceRestarted) {
            log += ", with ICE restarted.";
        } else {
            log += ".\r\n";
        }

        toastLog(TAG, mContext, log);
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        // If Peer has no userData, use an empty string for nick.
        String nick = "";
        if (userData != null) {
            nick = userData.toString();
        }
        String log = "[SA][onRemotePeerUserDataReceive] Peer " + getPeerIdNickBaseServiceHandler(skylinkConnection, remotePeerId) +
                ":\n" + nick;
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onOpenDataConnection(String remotePeerId) {
        Log.d(TAG, remotePeerId + " onOpenDataConnection");
    }

    /**
     * Data Transfer Listener Callbacks - triggered during events that happen when data or
     * connection
     * with remote peer changes
     */

    @Override
    public void onDataReceive(String remotePeerId, byte[] data) {
        // Check if it is one of the data that we can send.
        if (Arrays.equals(data, this.dataPrivate) || Arrays.equals(data, this.dataGroup)) {
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

}
