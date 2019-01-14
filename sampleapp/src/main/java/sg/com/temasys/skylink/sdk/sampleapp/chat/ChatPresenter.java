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

    // the index of the peer on the action bar that user selected to send message privately
    private static int selectedPeerIndex = 0;

    //
    private static MESSAGE_TYPE messageType = MESSAGE_TYPE.TYPE_SERVER;

    enum MESSAGE_TYPE {
        TYPE_SERVER,
        TYPE_P2P
    }


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

    /**
     * Process sending message
     * Base on the selected state of remotePeer button and message type button
     * to send message privately to peer or to all peers in group
     * to send message via server or P2P directly
     * default value is send to all peers in group and send via server if user do not selected
     */
    @Override
    public void onViewRequestSendMessage(String message) {
        // Check remote peer to send message to
        // if User did not select the specific peer to send message to,
        // then default is send to all peers in room
        if (selectedPeerIndex == 0) {
            //add message to list view for displaying
            if (messageType == MESSAGE_TYPE.TYPE_SERVER) {
                processAddSelfMessageToChatCollection(null, false, message);

                // using service to send message to remote peer(s) via signaling server
                mChatService.sendServerMessage(null, message);
            } else if (messageType == MESSAGE_TYPE.TYPE_P2P) {
                processAddSelfMessageToChatCollection(null, true, message);

                // using service to send message to remote peer(s) directly P2P
                mChatService.sendP2PMessage(null, message);
            }
        } else {
            // get the selected peer to send message to
            SkylinkPeer selectedPeer = mChatService.getPeerByIndex(selectedPeerIndex);

            // send message to the selected peer
            if (messageType == MESSAGE_TYPE.TYPE_SERVER) {
                processAddSelfMessageToChatCollection(selectedPeer.getPeerId(), false, message);

                // using service to send message to remote peer(s) via signaling server
                mChatService.sendServerMessage(selectedPeer.getPeerId(), message);
            } else if (messageType == MESSAGE_TYPE.TYPE_P2P) {
                processAddSelfMessageToChatCollection(selectedPeer.getPeerId(), true, message);

                // using service to send message to remote peer(s) directly P2P
                mChatService.sendP2PMessage(selectedPeer.getPeerId(), message);
            }
        }
    }

    @Override
    public List<String> onViewRequestGetChatCollection() {
        return chatMessageCollection;
    }

    @Override
    public SkylinkPeer onViewRequestGetPeerByIndex(int index) {
        return mChatService.getPeerByIndex(index);
    }

    @Override
    public void onViewRequestSelectedRemotePeer(int index) {
        // check the selected index with the current selectedPeerIndex
        // if it is equal which means user in selects the peer
        if (this.selectedPeerIndex == index) {
            this.selectedPeerIndex = 0;
        } else {
            this.selectedPeerIndex = index;
        }
    }

    @Override
    public int onViewRequestGetCurrentSelectedPeer() {
        return this.selectedPeerIndex;
    }


    @Override
    public void onViewRequestSelectedMessageType(ChatPresenter.MESSAGE_TYPE message_type) {
        this.messageType = message_type;
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful) {
            processUpdateUI();
            processUpdateUIConnected();
        }
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

        // Update remote peer info
        // Fill the new peer in button in custom bar
        processAddNewPeer(newPeer, mChatService.getTotalPeersInRoom() - 1);

        // Adding info to message collection
        chatMessageCollection.add("[Metadata]:" + newPeer.toString() + " joined the room.");

        mChatView.onPresenterRequestUpdateChatCollection();

    }

    @Override
    public void onServiceRequestRemotePeerLeave(SkylinkPeer removePeer, int removeIndex) {
        // do not process if the left peer is local peer
        if (removeIndex == -1)
            return;

        // Adding info to message collection
        chatMessageCollection.add("[Metadata]:" + removePeer.getPeerName() +
                "(" + removePeer.getPeerId() + ")" + " left the room.");

        mChatView.onPresenterRequestUpdateChatCollection();

        // Remove remote peer
        mChatView.onPresenterRequestChangeUiRemotePeerLeave(removePeer.getPeerId());

        // Update textview to show room status when last remote peer has left
        if (mChatService.getTotalPeersInRoom() == 1) {
            processUpdateRoomDetails();
        }

        // Update remote peer info
        // Remove the peer in button in custom bar
        processRemoveRemotePeer();
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
            mChatView.onPresenterRequestUpdateChatCollection();
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
            mChatView.onPresenterRequestUpdateChatCollection();
        }
    }


    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Add new peer on UI when new peer joined in room in specific index
     *
     * @param newPeer the new peer joined in room
     * @param index   the index of the new peer to add
     */
    private void processAddNewPeer(SkylinkPeer newPeer, int index) {
        mChatView.onPresenterRequestChangeUiRemotePeerJoin(newPeer, index);
    }

    /**
     * Remove a remote peer by re-fill total remote peer left in the room
     * to make sure the left peers are displayed correctly
     */
    private void processRemoveRemotePeer() {
        mChatView.onPresenterRequestFillPeers(mChatService.getPeersList());
    }

    /**
     * Retrieves self message written in edit text and adds it to the chatMessageCollection.
     * Will refresh listView to display new chatMessageCollection.
     *
     * @param remotePeerId remote peer id to send msg, null value if sending to all peers in room
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

        // adding prefix to the message
        String localPeerId = mChatService.getPeerId();
        String prefix = "You---" + Config.USER_NAME_CHAT + " (" + localPeerId + ")" + " : ";

        prefix += isPrivateMessage ? "[PTE]" : "[GRP]";
        prefix += isP2P ? "[P2P] " : "[SIG] ";

        chatMessageCollection.add(prefix + message);

        mChatView.onPresenterRequestUpdateChatCollection();

        mChatView.onPresenterRequestClearInput();
    }

    /*
     * Update UI when changing app state
     * */
    private void processUpdateUI() {

        // reset the chat collection
        mChatView.onPresenterRequestUpdateChatCollection();

        // re fill the peers
        mChatView.onPresenterRequestFillPeers(mChatService.getPeersList());

        // update the display info about the room
        processUpdateRoomInfo();
    }

    /*
     * Update UI when connected to room
     * */
    private void processUpdateUIConnected() {
        // Update the room info
        mChatView.onPresenterRequestUpdateRoomInfo(processGetRoomId());

        // Update the local peer info
        mChatView.onPresenterRequestUpdateLocalPeer(Config.USER_NAME_CHAT);

        // Adding info to message collection
        chatMessageCollection.add("[Metadata]:You (" + Config.USER_NAME_CHAT + "_" +
                mChatService.getPeerId() + ") joined the room.");

        mChatView.onPresenterRequestUpdateChatCollection();

//        chatMessageCollection.add("[Metadata]:" + newPeer.toString() + " joined the room.");
    }

    private void processUpdateRoomInfo() {
//        String strRoomDetails = processGetRoomDetails();
//        mChatView.onPresenterRequestUpdateUi(strRoomDetails);

        mChatView.onPresenterRequestUpdateRoomInfo(processGetRoomId());
    }

    private void processUpdateRoomDetails() {
        String strRoomDetails = processGetRoomDetails();
        mChatView.onPresenterRequestUpdateUi(strRoomDetails);

//        mChatView.onPresenterRequestUpdateRoomInfo(processGetRoomId());
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

    private String processGetRoomId() {
        return mChatService.getRoomId();
    }


}
