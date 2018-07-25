package sg.com.temasys.skylink.sdk.sampleapp.data.service;

import android.content.Context;
import android.util.Log;

import java.util.Date;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MessagesListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.chat.ChatContract;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class ChatService extends SDKService implements ChatContract.Service, LifeCycleListener, RemotePeerListener, MessagesListener {

    private final String TAG = ChatService.class.getName();

    private Context mContext;

    //this variable need to be static for configuration change
    private static ChatContract.Presenter mPresenter;

    private SdkConnectionManager sdkConnectionManager;

    //this variable need to be static for configuration change
    private static SkylinkConnection skylinkConnection;

    private String ROOM_NAME;
    private String MY_USER_NAME;

    private boolean isPeerJoined;

    public ChatService(Context mContext) {
        this.mContext = mContext;
        ROOM_NAME = Config.ROOM_NAME_CHAT;
        MY_USER_NAME = Config.USER_NAME_CHAT;
    }

    @Override
    public void setPresenter(ChatContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    public void connectToRoomServiceHandler() {

        sdkConnectionManager = new SdkConnectionManager(mContext);

        skylinkConnection = sdkConnectionManager.initializeSkylinkConnectionForChat();

        setListeners();

        String skylinkConnectionString = Utils.getSkylinkConnectionString(
                ROOM_NAME, new Date(), SkylinkConnection.DEFAULT_DURATION);

        boolean connectFailed = !connectToRoomBaseServiceHandler(skylinkConnection, skylinkConnectionString, Config.USER_NAME_CHAT);

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
        if (skylinkConnection != null && isConnectingOrConnectedBaseServiceHandler(skylinkConnection)) {
            disconnectFromRoomBaseServiceHandler(skylinkConnection);
        }
    }

    public boolean isConnectingOrConnectedServiceHandler() {
        if (skylinkConnection != null) {
            return isConnectingOrConnectedBaseServiceHandler(skylinkConnection);
        }
        return false;
    }

    public void sendServerMessageServiceHandler(String remotePeerId, String message) {
        if (skylinkConnection != null) {
            sendServerMessageBaseServiceHandler(skylinkConnection, remotePeerId, message);
        }
    }

    public void sendP2PMessageServiceHandler(String remotePeerId, String message) {
        if (skylinkConnection != null) {
            try {
                sendP2PMessageBaseServiceHandler(skylinkConnection, remotePeerId, message);
            } catch (SkylinkException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    public String getRoomDetailsServiceHandler(boolean isPeerInRoom) {
        boolean isConnected = isConnectingOrConnectedServiceHandler();
        String roomName = getRoomNameBaseServiceHandler(skylinkConnection, Config.ROOM_NAME_CHAT);
        String userName = getUserNameBaseServiceHandler(skylinkConnection, null, Config.USER_NAME_CHAT);

        String roomDetails = "You are not connected to any room";

        if (isConnected) {
            roomDetails = "Now connected to Room named : " + roomName
                    + "\nYou are signed in as : " + userName + "\n";
            if (isPeerInRoom) {
                roomDetails += "Peer(s) are in the room";
            } else {
                roomDetails += "You are alone in this room";
            }
        }

        return roomDetails;

    }

    public int getNumRemotePeersServiceHandler() {
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

    public void saveIsPeerJoinServiceHandler(boolean isPeerJoined) {
        this.isPeerJoined = isPeerJoined;
    }

    public void setRoomDetailsServiceHandler() {
        mPresenter.setRoomDetailsPresenterHandler(isPeerJoined);
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
            skylinkConnection.setMessagesListener(this);
            return true;
        } else {
            return false;
        }
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's
     * lifecycle
     */

    /**
     * Triggered if the connection is successful
     *
     * @param isSuccessful
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        //update textview if connection is successful
        if (isSuccessful) {
            String log = "Connected to room " + ROOM_NAME + " (" + getRoomIdBaseServiceHandler(skylinkConnection) +
                    ") as " + getPeerIdBaseServiceHandler(skylinkConnection) + " (" + MY_USER_NAME + ").";
            toastLogLong(TAG, mContext, log);
            // [MultiParty]

            mPresenter.listViewRefreshPresenterHandler();

            setRoomDetailsServiceHandler();

            mPresenter.fillPeerRadioBtnPresenterHandler();

        } else {
            String log = "Skylink failed to connect!\nReason : " + message;
            toastLogLong(TAG, mContext, log);
            setRoomDetailsServiceHandler();
        }
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        // [MultiParty]
        // Reset peerList
        mPresenter.clearPeerListPresenterHandler();

        setRoomDetailsServiceHandler();
        // Reset chat collection
        mPresenter.clearChatMessageCollectionPresenterHandler();

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
        String logTag = "[SA][onRemotePeerJoin] ";
        // [MultiParty]
        // When remote peer joins room, keep track of user and update UI.
        // If Peer has no userData, use an empty string for nick.
        String nick = "";
        if (userData != null) {
            nick = userData.toString();
        }
        String log = logTag + "Peer \"" + getPeerIdNickBaseServiceHandler(skylinkConnection, remotePeerId) + " connected.";
        toastLog(TAG, mContext, log);

        mPresenter.addPeerRadioBtnPresenterHandler(remotePeerId, nick);

        //Set room status if it's the only peer in the room.
        if (mPresenter.getPeerNumPresenterHandler() == 1) {
            isPeerJoined = true;
            // Update textview to show room status
            setRoomDetailsServiceHandler();

            //set isPeerJoined to view
            mPresenter.setMultiChatPeersInfoPresenterHandler(isPeerJoined);
        }
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message, UserInfo userInfo) {
        // [MultiParty]
        // Remove the Peer.
        mPresenter.removePeerRadioBtnPresenterHandler(remotePeerId);

        //Set room status if there are no more peers.
        if (mPresenter.getPeerListSizePresenterHandler() == 0) {
            isPeerJoined = false;
            // Update textview to show room status
            setRoomDetailsServiceHandler();

            //set isPeerJoined to view
            mPresenter.setMultiChatPeersInfoPresenterHandler(isPeerJoined);
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
    public void onOpenDataConnection(String peerId) {
        Log.d(TAG, "onOpenDataConnection");
    }

    /**
     * Message Listener Callbacks - triggered during events that happen when messages are received
     * from remotePeer
     */

    @Override
    public void onServerMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
        String chatPrefix = "[SIG] ";
        //add prefix if the chat is a private chat - not seen by other users.
        if (isPrivate) {
            chatPrefix = "[PTE]" + chatPrefix;
        } else {
            chatPrefix = "[GRP]" + chatPrefix;
        }
        // add message to listview and update ui
        if (message instanceof String) {
            String remotePeerName = getPeerIdNickBaseServiceHandler(skylinkConnection, remotePeerId);
            mPresenter.addToChatMessageCollectionPresenterHandler(remotePeerName + " : " + chatPrefix + message);
            mPresenter.listViewRefreshPresenterHandler();
        }
    }

    @Override
    public void onP2PMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
        //add prefix if the chat is a private chat - not seen by other users.
        String chatPrefix = "[P2P] ";
        if (isPrivate) {
            chatPrefix = "[PTE]" + chatPrefix;
        } else {
            chatPrefix = "[GRP]" + chatPrefix;
        }
        //add message to listview and update ui
        if (message instanceof String) {
            String remotePeerName = getPeerIdNickBaseServiceHandler(skylinkConnection, remotePeerId);
            mPresenter.addToChatMessageCollectionPresenterHandler(remotePeerName + " : " + chatPrefix + message);
            mPresenter.listViewRefreshPresenterHandler();
        }
    }

}
