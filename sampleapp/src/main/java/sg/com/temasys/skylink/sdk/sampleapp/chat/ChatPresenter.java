package sg.com.temasys.skylink.sdk.sampleapp.chat;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.ChatService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for implementing message logic.
 */

public class ChatPresenter extends BasePresenter implements ChatContract.Presenter {

    private final String TAG = ChatPresenter.class.getName();

    private ChatContract.View mChatView;

    private ChatService mChatService;

    //this variable need to be static for configuration change
    // when screen orientation changed, we need to maintain the message list
    private static List<String> chatMessageCollection = new ArrayList<String>();

    public ChatPresenter(Context context) {
        mChatService = new ChatService(context);
        this.mChatService.setPresenter(this);
    }

    public void setView(ChatContract.View view) {
        mChatView = view;
        mChatView.setPresenter(this);
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for view to call
    // These methods are responsible for processing requests from view
    //----------------------------------------------------------------------------------------------

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
        if (!mChatService.isConnectingOrConnected()) {

            //connect to room on Skylink connection
            mChatService.connectToRoom(Constants.CONFIG_TYPE.CHAT);

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
        mChatService.disconnectFromRoom();

        //after disconnected from skylink SDK, UI will be updated later on onDisconnect()
    }

    @Override
    public void onViewRequestSendServerMessage(String remotePeerId, String message) {

        //add message to list view for displaying
        processAddSelfMessageToChatCollection(remotePeerId, false, message);

        // using service to send message to remote peer through server
        mChatService.sendServerMessage(remotePeerId, message);
    }

    @Override
    public void onViewRequestSendP2PMessage(String remotePeerId, String message) {

        //need to check remotePeerId existed
        //remotePeerId = null when selecting All peer(s)
        //remotePeerId = "" when not selecting any peer
        if (remotePeerId == null || !remotePeerId.equals("")) {

            //add message to list view for displaying
            processAddSelfMessageToChatCollection(remotePeerId, true, message);

            // using service to send message to remote peer directly P2P
            mChatService.sendP2PMessage(remotePeerId, message);
        }
    }

    @Override
    public List<String> onViewRequestGetChatCollection() {
        return chatMessageCollection;
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful)
            processUpdateUI();
    }

    @Override
    public void onServiceRequestDisconnect() {

        // Reset chat collection
        chatMessageCollection.clear();
    }

    @Override
    public void onServiceRequestRemotePeerJoin(SkylinkPeer newPeer) {

        //add new remote peer
        mChatView.onPresenterRequestChangeUiRemotePeerJoin(newPeer);

        // Update textview to show room status when first remote peer has joined with self peer
        if (mChatService.getTotalPeersInRoom() == 2) {
            processUpdateRoomDetails();
        }
    }

    @Override
    public void onServiceRequestRemotePeerLeave(String remotePeerId, int removeIndex) {
        // Remove remote peer
        mChatView.onPresenterRequestChangeUiRemotePeerLeave(remotePeerId);

        // Update textview to show room status when last remote peer has left
        if (mChatService.getTotalPeersInRoom() == 1) {
            processUpdateRoomDetails();
        }
    }

    @Override
    public void onServiceRequestServerMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
        String chatPrefix = "[SIG] ";
        //add prefix if the chat is a private chat - not seen by other users.
        if (isPrivate) {
            chatPrefix = "[PTE]" + chatPrefix;
        } else {
            chatPrefix = "[GRP]" + chatPrefix;
        }

        // add message to listview and update ui
        if (message instanceof String) {
            String remotePeerName = mChatService.getPeerNameById(remotePeerId);
            chatMessageCollection.add(remotePeerName + " : " + chatPrefix + message);
            mChatView.onPresenterRequestRefreshChatCollection();
        }
    }

    @Override
    public void onServiceRequestP2PMessageReceive(String remotePeerId, Object message, boolean isPrivate) {
        //add prefix if the chat is a private chat - not seen by other users.
        String chatPrefix = "[P2P] ";
        if (isPrivate) {
            chatPrefix = "[PTE]" + chatPrefix;
        } else {
            chatPrefix = "[GRP]" + chatPrefix;
        }

        // add message to listview and update ui
        if (message instanceof String) {
            String remotePeerName = mChatService.getPeerNameById(remotePeerId);
            chatMessageCollection.add(remotePeerName + " : " + chatPrefix + message);
            mChatView.onPresenterRequestRefreshChatCollection();
        }
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Retrieves self message written in edit text and adds it to the chatMessageCollection.
     * Will refresh listView to display new chatMessageCollection.
     *
     * @param remotePeerId remote peer id to send msg
     * @param isP2P        is send P2P msg or server msg
     * @param message      msg to be sent
     */
    private void processAddSelfMessageToChatCollection(String remotePeerId, boolean isP2P, String message) {
        boolean isPrivateMessage = true;

        // Do not allow button actions if there are no Peers in the room.
        if ("".equals(remotePeerId)) {
            return;
        } else if (remotePeerId == null) {
            isPrivateMessage = false;
        }

        String prefix = "You : ";
        prefix += isPrivateMessage ? "[PTE]" : "[GRP]";
        prefix += isP2P ? "[P2P] " : "[SIG] ";

        chatMessageCollection.add(prefix + message);

        mChatView.onPresenterRequestRefreshChatCollection();

        mChatView.onPresenterRequestClearInput();
    }

    /*
     * Update UI when changing app state
     * */
    private void processUpdateUI() {

        // reset the chat collection
        mChatView.onPresenterRequestRefreshChatCollection();

        // re fill the peers
        mChatView.onPresenterRequestFillPeers(mChatService.getPeersList());

        // update the display info
        processUpdateRoomDetails();
    }

    private void processUpdateRoomDetails() {
        String strRoomDetails = processGetRoomDetails();
        mChatView.onPresenterRequestUpdateUi(strRoomDetails);
    }

    /*
     * Get the info about room and app state to update the UI
     * */
    private String processGetRoomDetails() {
        boolean isConnected = mChatService.isConnectingOrConnected();
        String roomName = mChatService.getRoomName(Config.ROOM_NAME_CHAT);
        String userName = mChatService.getUserName(null, Config.USER_NAME_CHAT);

        boolean isPeerJoined = mChatService.isPeerJoin();

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
