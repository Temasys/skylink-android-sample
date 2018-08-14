package sg.com.temasys.skylink.sdk.sampleapp.chat;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.data.service.ChatService;


/**
 * Created by muoi.pham on 20/07/18.
 */

public class ChatPresenter implements ChatContract.Presenter {

    private final String TAG = ChatPresenter.class.getName();

    public ChatContract.View mChatView;

    private ChatService mChatService;

    //this variable need to be static for configuration change
    private static List<String> chatMessageCollection = new ArrayList<String>();

    public ChatPresenter(ChatContract.View chatView, Context context) {

        this.mChatView = chatView;
        mChatService = new ChatService(context);

        this.mChatView.setPresenter(this);
        this.mChatService.setPresenter(this);

        this.mChatService.setTypeCall();
    }

    /**
     * Triggered when View request data to display to the user when entering room | rotating screen
     * Try to connect to room when entering room
     * Try to update UI if connected to room after changing configuration
     */
    @Override
    public void onViewLayoutRequestedPresenterHandler() {

        Log.d(TAG, "onViewLayoutRequestedPresenterHandler");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mChatService.isConnectingOrConnectedServiceHandler()) {

            //connect to room on Skylink connection
            mChatService.connectToRoomServiceHandler();

            //after connected to skylink SDK, UI will be updated later on ChatService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //update UI into connected state
            updateUIPresenterHandler();

            Log.d(TAG, "Try to update UI when changing configuration");
        }
    }

    @Override
    public void onConnectPresenterHandler(boolean isSuccessful) {
        updateUIPresenterHandler();
    }

    @Override
    public void onDisconnectPresenterHandler() {

        // Reset chat collection
        chatMessageCollection.clear();

    }

    @Override
    public void onViewExitPresenterHandler() {

        //process disconnect from room
        mChatService.disconnectFromRoomServiceHandler();

        //after disconnected from skylink SDK, UI will be updated later on ChatService.onDisconnect
    }

    @Override
    public void onRemotePeerJoinPresenterHandler(SkylinkPeer newPeer) {

        //add new remote peer
        mChatView.addPeerRadioBtnViewHandler(newPeer);

        // Update textview to show room status when first remote peer has joined with self peer
        if (mChatService.getTotalPeersInRoomServiceHandler() == 2) {
            updateRoomDetailsPresenterHandler();
        }
    }

    @Override
    public void onRemotePeerLeavePresenterHandler(String remotePeerId) {
        // Remove remote peer
        mChatView.onRemovePeerRadioBtnViewHandler(remotePeerId);

        // Update textview to show room status when last remote peer has left
        if (mChatService.getTotalPeersInRoomServiceHandler() == 1) {
            updateRoomDetailsPresenterHandler();
        }
    }

    @Override
    public void onSendServerMessagePresenterHandler(String remotePeerId, String message) {

        //add message to listview for displaying
        addSelfMessageToListViewPresenterHandler(remotePeerId, false, message);

        //send message to SDK
        mChatService.sendServerMessageServiceHandler(remotePeerId, message);
    }

    @Override
    public void onSendP2PMessagePresenterHandler(String remotePeerId, String message) {

        //need to check remotePeerId existed
        //remotePeerId = null when selecting All peer(s)
        //remotePeerId = "" when not selecting any peer
        if(remotePeerId == null || !remotePeerId.equals("")) {

            addSelfMessageToListViewPresenterHandler(remotePeerId, true, message);

            mChatService.sendP2PMessageServiceHandler(remotePeerId, message);
        }
    }

    @Override
    public List<String> onGetChatMessageCollectionPresenterHandler() {
        return chatMessageCollection;
    }

    @Override
    public void onServerMessageReceivePresenterHandler(String remotePeerId, Object message, boolean isPrivate) {

        String chatPrefix = "[SIG] ";
        //add prefix if the chat is a private chat - not seen by other users.
        if (isPrivate) {
            chatPrefix = "[PTE]" + chatPrefix;
        } else {
            chatPrefix = "[GRP]" + chatPrefix;
        }

        // add message to listview and update ui
        if (message instanceof String) {
            String remotePeerName = mChatService.getPeerNameByIdServiceHandler(remotePeerId);
            chatMessageCollection.add(remotePeerName + " : " + chatPrefix + message);
            mChatView.onListViewRefreshViewHandler();
        }
    }

    @Override
    public void onP2PMessageReceivePresenterHandler(String remotePeerId, Object message, boolean isPrivate) {

        //add prefix if the chat is a private chat - not seen by other users.
        String chatPrefix = "[P2P] ";
        if (isPrivate) {
            chatPrefix = "[PTE]" + chatPrefix;
        } else {
            chatPrefix = "[GRP]" + chatPrefix;
        }

        // add message to listview and update ui
        if (message instanceof String) {
            String remotePeerName = mChatService.getPeerNameByIdServiceHandler(remotePeerId);
            chatMessageCollection.add(remotePeerName + " : " + chatPrefix + message);
            mChatView.onListViewRefreshViewHandler();
        }
    }

    /**
     * Retrieves self message written in edit text and adds it to the chatMessageCollection.
     * Will refresh listView to display new chatMessageCollection.
     *
     * @param remotePeerId remote peer id to send msg
     * @param isP2P        is send P2P msg or server msg
     * @param message      msg to be sent
     */
    private void addSelfMessageToListViewPresenterHandler(String remotePeerId, boolean isP2P, String message) {
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

        mChatView.onListViewRefreshViewHandler();

        mChatView.onClearEditTextViewHandler();
    }

    private void updateUIPresenterHandler() {

        mChatView.onListViewRefreshViewHandler();

        updateRoomDetailsPresenterHandler();

        mChatView.fillPeerRadioBtnViewHandler(mChatService.getPeersListServiceHandler());
    }

    private void updateRoomDetailsPresenterHandler() {
        String strRoomDetails = getRoomDetailsPresenterHandler();
        mChatView.onUpdateRoomDetailsViewHandler(strRoomDetails);
    }

    private String getRoomDetailsPresenterHandler() {
        boolean isConnected = mChatService.isConnectingOrConnectedServiceHandler();
        String roomName = mChatService.getRoomNameServiceHandler(Config.ROOM_NAME_CHAT);
        String userName = mChatService.getUserNameServiceHandler(null, Config.USER_NAME_CHAT);

        boolean isPeerJoined = mChatService.isPeerJoinServiceHandler();

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
