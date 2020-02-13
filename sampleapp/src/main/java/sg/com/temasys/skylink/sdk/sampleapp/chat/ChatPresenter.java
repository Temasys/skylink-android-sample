package sg.com.temasys.skylink.sdk.sampleapp.chat;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.ChatService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for implementing message logic.
 */

public class ChatPresenter extends BasePresenter implements ChatContract.Presenter {

    private final String TAG = ChatPresenter.class.getName();

    private ChatContract.View chatView;

    private ChatService chatService;

    // when screen orientation changed, we need to maintain the message list
    private List<String> chatMessageCollection = new ArrayList<String>();

    // the index of the peer on the action bar that user selected to send message privately
    // default is 0 - send message to all peers
    private int selectedPeerIndex = 0;

    // message type to be sent
    private MESSAGE_TYPE messageType = MESSAGE_TYPE.TYPE_SERVER;

    public enum MESSAGE_TYPE {
        TYPE_SERVER,
        TYPE_P2P
    }

    public ChatPresenter(Context context) {
        chatService = new ChatService(context);
        this.chatService.setPresenter(this);
    }

    /**
     * Set the view for the presenter
     */
    public void setView(ChatContract.View view) {
        chatView = view;
        chatView.setPresenter(this);
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for view to call
    // These methods are responsible for processing requests from view, like connect/disconnect/...
    // with the SkylinkSDK
    //----------------------------------------------------------------------------------------------

    /**
     * Triggered when View request data to display to the user when entering room
     * Try to connect to room when entering room
     */
    @Override
    public void processConnectedLayout() {

        Log.d(TAG, "onViewLayoutRequested");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!chatService.isConnectingOrConnected()) {

            //connect to room on Skylink connection
            chatService.connectToRoom(Constants.CONFIG_TYPE.CHAT);

            //after connected to skylink SDK, UI will be updated later on processRoomConnected

            Log.d(TAG, "Try to connect when entering room");
        }
    }

    @Override
    public void processExit() {
        //process disconnect from room
        chatService.disconnectFromRoom();

        // need to call disposeLocalMedia to clear all local media objects as disconnectFromRoom no longer dispose local media
        chatService.disposeLocalMedia();

        //after disconnected from skylink SDK, UI will be updated latter on processRoomDisconnected
    }

    /**
     * Process sending message
     * Base on the selected state of remotePeer button and message type button
     * to send message privately to peer or to all peers in group
     * to send message via server or P2P directly
     * default value is send to all peers in group and send via server if user do not selected any
     * specific peer to send
     */
    @Override
    public void processSendMessage(String message) {
        // Check remote peer to send message to
        // if User did not select the specific peer to send message to,
        // then default is send to all peers in room
        if (selectedPeerIndex == 0) {
            //add message to list view for displaying
            if (messageType == MESSAGE_TYPE.TYPE_SERVER) {
                processAddSelfMessageToChatCollection(null, false, message);

                // using service to send message to remote peer(s) via signaling server
                chatService.sendServerMessage(null, message);
            } else if (messageType == MESSAGE_TYPE.TYPE_P2P) {
                processAddSelfMessageToChatCollection(null, true, message);

                // using service to send message to remote peer(s) directly P2P
                chatService.sendP2PMessage(null, message);
            }
        } else {
            // get the selected peer to send message to
            SkylinkPeer selectedPeer = chatService.getPeerByIndex(selectedPeerIndex);

            // send message to the selected peer
            if (messageType == MESSAGE_TYPE.TYPE_SERVER) {
                processAddSelfMessageToChatCollection(selectedPeer.getPeerId(), false, message);

                // using service to send message to remote peer(s) via signaling server
                chatService.sendServerMessage(selectedPeer.getPeerId(), message);
            } else if (messageType == MESSAGE_TYPE.TYPE_P2P) {
                processAddSelfMessageToChatCollection(selectedPeer.getPeerId(), true, message);

                // using service to send message to remote peer(s) directly P2P
                chatService.sendP2PMessage(selectedPeer.getPeerId(), message);
            }
        }
    }

    /**
     * Get the list of message to set to the ArrayAdapter
     */
    @Override
    public List<String> processGetChatCollection() {
        return chatMessageCollection;
    }

    /**
     * Get the specific peer object according to the index
     */
    @Override
    public SkylinkPeer processGetPeerByIndex(int index) {
        return chatService.getPeerByIndex(index);
    }

    /**
     * Save the current index of the selected peer
     */
    @Override
    public void processSelectRemotePeer(int index) {
        // check the selected index with the current selectedPeerIndex
        // if it is equal which means user in selects the peer
        if (this.selectedPeerIndex == index) {
            this.selectedPeerIndex = 0;
        } else {
            this.selectedPeerIndex = index;
        }
    }

    /**
     * Get the current index of selected peer
     */
    @Override
    public int processGetCurrentSelectedPeer() {
        return this.selectedPeerIndex;
    }

    /**
     * Get the current selected message type to send message.
     */
    @Override
    public void processSelectMessageType(ChatPresenter.MESSAGE_TYPE message_type) {
        this.messageType = message_type;
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    /**
     * Process update UI into connected state
     */
    @Override
    public void processRoomConnected(boolean isSuccessful) {
        if (isSuccessful) {
            processUpdateStateConnected();
        }
    }

    /**
     * Process udate UI into dis connected state
     */
    @Override
    public void processRoomDisconnected() {
        chatMessageCollection.clear();
        selectedPeerIndex = 0;
        messageType = MESSAGE_TYPE.TYPE_SERVER;

        chatView.updateUIDisconnected();
    }

    /**
     * process logic when remote peer joined the room
     *
     * @param newPeer the remote peer
     */
    @Override
    public void processRemotePeerConnected(SkylinkPeer newPeer) {
        // Fill the new peer in button in custom bar
        processAddNewPeer(newPeer, chatService.getTotalPeersInRoom() - 2);

        // Adding info to message collection
        // This message is metadata message to inform the peer join the room
        chatMessageCollection.add("[Metadata]:" + newPeer.toString() + " joined the room." + "\n" +
                Utils.getISOTimeStamp(new Date()));

        // notify the adapter to update list view
        chatView.updateUIChatCollection();
    }

    /**
     * process logic when remote peer left the room
     *
     * @param removePeer  the peer left
     * @param removeIndex the index of the remove peer
     */
    @Override
    public void processRemotePeerDisconnected(SkylinkPeer removePeer, int removeIndex) {
        // do not process if the left peer is local peer
        if (removeIndex == -1 || removePeer == null)
            return;

        // Adding info to message collection
        // This message is metadata message to inform the peer left the room
        chatMessageCollection.add("[Metadata]:" + removePeer.getPeerName() +
                "(" + removePeer.getPeerId() + ")" + " left the room." + "\n" +
                Utils.getISOTimeStamp(new Date()));

        chatView.updateUIChatCollection();

        // Remove the peer in button in custom bar
        processRemoveRemotePeer();
    }

    /**
     * process logic when receiving message from the server
     *
     * @param remotePeerId the remote peer that message was sent
     * @param message
     * @param isPrivate    the message sent is private or public for all peers
     */
    @Override
    public void processServerMessageReceived(String remotePeerId, Object message, boolean isPrivate) {
        String chatPrefix = "[SIG] ";
        //add prefix if the chat is a private chat - not seen by other users.
        if (isPrivate) {
            chatPrefix = "[PTE]" + chatPrefix;
        } else {
            chatPrefix = "[GRP]" + chatPrefix;
        }

        String remotePeerName = null;
        String data = null;

        // add message to listview and update ui
        if (message instanceof String) {
            remotePeerName = chatService.getPeerNameById(remotePeerId);
            data = (String) message;
        } else if (message instanceof JSONObject) {
            JSONObject jsonMessage = (JSONObject) message;
            try {
                remotePeerName = jsonMessage.getString("senderId");
                data = jsonMessage.getString("data");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        chatMessageCollection.add(remotePeerName + " : " + chatPrefix + data);
        chatView.updateUIChatCollection();
    }

    /**
     * process logic when receiving message directly P2P from the remote peer
     *
     * @param remotePeerId the remote peer that message was sent
     * @param message
     * @param isPrivate    the message sent is private or public for all peers
     */
    @Override
    public void processP2PMessageReceived(String remotePeerId, Object message, boolean isPrivate) {
        //add prefix if the chat is a private chat - not seen by other users.
        String chatPrefix = "[P2P] ";
        if (isPrivate) {
            chatPrefix = "[PTE]" + chatPrefix;
        } else {
            chatPrefix = "[GRP]" + chatPrefix;
        }

        // add message to listview and update ui
        if (message instanceof String) {
            String remotePeerName = chatService.getPeerNameById(remotePeerId);
            chatMessageCollection.add(remotePeerName + " : " + chatPrefix + message);
            chatView.updateUIChatCollection();
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
        chatView.updateUIRemotePeerConnected(newPeer, index);
    }

    /**
     * Remove a remote peer by re-fill total remote peer left in the room
     * to make sure the left peers are displayed correctly
     */
    private void processRemoveRemotePeer() {
        chatView.updateUIRemotePeerDisconnected(chatService.getPeersList());
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
        String localPeerId = chatService.getPeerId();
        String prefix = "You---" + Config.USER_NAME_CHAT + " (" + localPeerId + ")" + " : ";

        prefix += isPrivateMessage ? "[PTE]" : "[GRP]";
        prefix += isP2P ? "[P2P] " : "[SIG] ";

        chatMessageCollection.add(prefix + message);

        chatView.updateUIChatCollection();

        chatView.updateUIClearMessageInput();
    }

    /**
     * Update UI when connected to room
     */
    private void processUpdateStateConnected() {

        // Update the view into connected state
        chatView.updateUIConnected(processGetRoomId());

        // This message is metadata message to inform the user is connected to the room
        chatMessageCollection.add("[Metadata]:You (" + Config.USER_NAME_CHAT + "_" +
                chatService.getPeerId() + ") joined the room." + "\n" +
                Utils.getISOTimeStamp(new Date()));

        // notify the adapter to update list view
        chatView.updateUIChatCollection();

    }

    /**
     * Get the room id info
     */
    private String processGetRoomId() {
        return chatService.getRoomId();
    }
}
