package sg.com.temasys.skylink.sdk.sampleapp.chat;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.ChatService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.MessageModel;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.ChatListAdapter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.getUserNameByType;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for implementing message logic.
 */

public class ChatPresenter extends BasePresenter implements ChatContract.Presenter {

    private final String TAG = ChatPresenter.class.getName();

    Context context;

    private ChatContract.View chatView;

    private ChatService chatService;

    // when screen orientation changed, we need to maintain the message list
    private List<MessageModel> chatMessageCollection = new ArrayList<MessageModel>();

    // the index of the peer on the action bar that user selected to send message privately
    // default is 0 - send message to all peers
    private int selectedPeerIndex = 0;

    // message type to be sent
    private MESSAGE_TYPE messageType = MESSAGE_TYPE.TYPE_SERVER;
    private MESSAGE_FORMAT messageFormat = MESSAGE_FORMAT.FORMAT_STRING;

    private static final String PEER_USER_ID = "senderId";
    private static final String PEER_USERNAME = "senderName";
    private static final String MESSAGE_CONTENT = "data";
    private static final String TIMESTAMP = "timeStamp";
    //    private Map<String, String> encryptionMap = new HashMap<String, String>();
    private List<String> encryptionKeys = null;
    private List<String> encryptionValues = null;

    private boolean sentResult = true;

    public enum MESSAGE_TYPE {
        TYPE_SERVER,
        TYPE_P2P
    }

    public enum MESSAGE_FORMAT {
        FORMAT_STRING,
        FORMAT_JSON_OBJECT,
        FORMAT_JSON_ARRAY
    }

    public ChatPresenter(Context context) {
        this.context = context;
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

            // Get and display all cached messages of the room
            // while connecting to the room.
            chatService.getCachedMessagesIfExist();
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
     * Base on the selected message format, will form the proper message type to send
     * The acceptable types are 'java.lang.String', 'org.json.JSONObject', 'org.json.JSONArray'.
     */
    @Override
    public void processSendMessage(String message) {
        sentResult = true;

        switch (this.messageFormat) {
            case FORMAT_STRING:
                processSendMessageString(message);
                break;
            case FORMAT_JSON_OBJECT:
                processSendMessageJson(message);
                break;
            case FORMAT_JSON_ARRAY:
                processSendMessageArray(message);
                break;
        }
    }

    @Override
    public void processSelectMessageFormat(MESSAGE_FORMAT formatMsg) {
        this.messageFormat = formatMsg;
    }

    @Override
    public void processAddEncryption(String enryptionKey, String encryptionValue) {
        // need to check existing encryptionValue in the map
        // in order to save to share pref
        for (String value : encryptionKeys) {
            if (value.equals(enryptionKey)) {
                Toast.makeText(context, "Encryption key is existing!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        for (String value : encryptionValues) {
            if (value.equals(encryptionValue)) {
                Toast.makeText(context, "Encryption value is existing!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        this.encryptionKeys.add(enryptionKey);
        this.encryptionValues.add(encryptionValue);

        chatService.setEncryptedMap(encryptionKeys, encryptionValues);

        // no save as not handle the order of key-value
        // save the encryption key-value to share pref
        Utils.saveEncryptionMap(context, encryptionKeys, encryptionValues);

        chatView.updateUIEncryptionKeys(encryptionKeys);
    }

    private List<String> initMessageFormatList() {
        List<String> list = new ArrayList<String>();
        list.add("String");
        list.add("JSONObject");
        list.add("JSONArray");

        return list;
    }

    @Override
    public String processGetEncryptionValueFromKey(String encryptionKey) {

        int pos = Utils.getKeyPosition(encryptionKey, encryptionKeys);

        return encryptionValues.get(pos);
    }

    @Override
    public void processGetStoredSeverMessages() {
        chatService.getStoredMessages();
    }

    @Override
    public void processStoreMessageSet(boolean isChecked) {
        chatService.setStoreMessage(isChecked);

        Utils.saveStoreMessageSet(context, isChecked);
    }

    @Override
    public void processSelectSecretKey(String secretKey) {
        chatService.setSelectedEncryptedSecret(secretKey);

        Utils.saveSelectedEncrytionKey(context, secretKey);
    }

    @Override
    public void processDeleteEncryption(String enryptionKey, String encryptionValue) {

        this.encryptionKeys.remove(enryptionKey);
        this.encryptionValues.remove(encryptionValue);

        chatService.setEncryptedMap(encryptionKeys, encryptionValues);

        // no save as not handle the order of key-value
        // save the encryption key-value to share pref
        Utils.saveEncryptionMap(context, encryptionKeys, encryptionValues);

        chatView.updateUIEncryptionKeys(encryptionKeys);
    }

    @Override
    public void processStoredMessagesResult(JSONArray messages) {
        // remove the UI for getting message history in the dataset
        chatMessageCollection.clear();
        chatView.updateUIChatCollection(false);

        if (messages == null || messages.length() == 0) {
            return;
        }

        Object message;

        // process stored messages
        for (int i = 0; i < messages.length(); i++) {
            String userName, messageContent;
            String timeStamp;
            JSONObject messageJson = null;
            JSONArray messageArray = null;
            try {
                message = messages.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }

            if (message instanceof JSONArray) {
                messageArray = (JSONArray) message;

                try {
                    messageJson = messageArray.getJSONObject(0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (message instanceof JSONObject) {
                messageJson = (JSONObject) message;
            }

            try {
                userName = messageJson.getString(PEER_USERNAME);
            } catch (JSONException e) {
                // use peerId as senderName if userName is not present
                try {
                    userName = messageJson.getString(PEER_USER_ID);
                } catch (JSONException e1) {
                    userName = "N/A";
                }
            }
            try {
                messageContent = messageJson.getString(MESSAGE_CONTENT);
            } catch (JSONException e) {
                messageContent = "N/A";
            }
            try {
                Long timeStampLong = messageJson.getLong(TIMESTAMP);

                timeStamp = Utils.getDefaultShortTimeStamp(new Date(timeStampLong));

            } catch (JSONException e) {
                timeStamp = "Not correct format as Long or N/A";
            }

            addRemoteMessage(userName, userName, messageContent, timeStamp, ChatListAdapter.MessageRowType.CHAT_REMOTE_GRP_SIG_HISTORY);

        }
    }

    /**
     * Get the list of message to set to the ArrayAdapter
     */
    @Override
    public List<MessageModel> processGetChatCollection() {
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

            if (encryptionKeys == null || encryptionValues == null) {
                encryptionKeys = new ArrayList<>();
                encryptionValues = new ArrayList<>();
            } else {
                encryptionKeys.clear();
                encryptionValues.clear();
            }

            // load the stored encyption map, selected encryption value and storeMessage setting in share pref
            if (Utils.getStoredEncryptionList(context) != null) {
                List<String> storedJson = Utils.getStoredEncryptionList(context);

                for (String json : storedJson) {
                    try {
                        JSONObject jsonObject = new JSONObject(json);

                        String key = jsonObject.getString("encryptionKey");
                        String value = jsonObject.getString("encryptionValue");

                        encryptionKeys.add(key);
                        encryptionValues.add(value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // save to SDK
            chatService.setEncryptedMap(encryptionKeys, encryptionValues);

            // update UI
            chatView.initUIEncryptionKeys(encryptionKeys);
            chatView.initMessageFormats(initMessageFormatList());

            String selectedEncryptionKey = Utils.getStoredSelectedEncryptionKey(context);
            if (selectedEncryptionKey != null) {
                chatService.setSelectedEncryptedSecret(selectedEncryptionKey);
                int position = Utils.getKeyPosition(selectedEncryptionKey, encryptionKeys);
                chatView.initUIEncryptionSelectedKey(selectedEncryptionKey, encryptionValues.get(position), position + 1);
            }

            boolean toPersist = Utils.getStoredMessageSetting(context);
            chatService.setStoreMessage(toPersist);
            chatView.initUIStoreMessageSetting(toPersist);
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

        // Add meta data into the message collection
        addMetadataMessage(newPeer, " joined the room.");
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

        // Remove the peer in button in custom bar
        processRemoveRemotePeer();

        // Add meta data into the message collection
        addMetadataMessage(removePeer, " left the room.");
    }

    /**
     * process logic when receiving message from the server, both normal server message and server message history
     *
     * @param remotePeerId the remote peer that message was sent
     * @param message
     * @param isPrivate    the message sent is private or public for all peers
     */
    @Override
    public void processServerMessageReceived(String remotePeerId, Object message, boolean isPrivate, Long timeStamp) {

        // do not process null message
        // if user has hasPersistentMessage and input encryptedKey, but there is no message history before user join the room,
        // OR user do not has hasPersistentMessage configured
        // user will get null message from SDK
        if (message == null)
            return;

        String userName;
        String messageContent;

        //add prefix if the chat is a private chat - not seen by other users.
        if (isPrivate) {
            if (message instanceof String) {
                String messageString = (String) message;

                userName = chatService.getPeerNameById(remotePeerId);
                messageContent = messageString;

                // Add remote message into the message collection
                addRemoteMessage(remotePeerId, userName, messageContent, Utils.getDefaultShortTimeStamp(new Date(timeStamp)), ChatListAdapter.MessageRowType.CHAT_REMOTE_PTE_SIG);

            } else if (message instanceof JSONObject) {
                try {
                    JSONObject messageJson = (JSONObject) message;

                    userName = messageJson.getString(PEER_USERNAME);
                    messageContent = messageJson.getString(MESSAGE_CONTENT);

                    addRemoteMessage(remotePeerId, userName, messageContent, Utils.getDefaultShortTimeStamp(new Date(timeStamp)), ChatListAdapter.MessageRowType.CHAT_REMOTE_PTE_SIG);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (message instanceof JSONArray) {
                try {
                    JSONArray messageArray = (JSONArray) message;

                    for (int i = 0; i < messageArray.length(); i++) {
                        JSONObject messageJson = messageArray.getJSONObject(i);

                        userName = messageJson.getString(PEER_USERNAME);
                        messageContent = messageJson.getString(MESSAGE_CONTENT);

                        addRemoteMessage(remotePeerId, userName, messageContent, Utils.getDefaultShortTimeStamp(new Date(timeStamp)), ChatListAdapter.MessageRowType.CHAT_REMOTE_PTE_SIG);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // normal public message if remotePeerId is not null
            // messageHistory if remotePeer is null
            // check the type  of message then process properly
            if (message instanceof String) {

                ChatListAdapter.MessageRowType messageRowType;

                if (remotePeerId != null) {
                    userName = chatService.getPeerNameById(remotePeerId);
                    messageContent = (String) message;
                    messageRowType = ChatListAdapter.MessageRowType.CHAT_REMOTE_GRP_SIG;
                } else { // process message history
                    userName = "Undefined";
                    messageContent = (String) message;
                    messageRowType = ChatListAdapter.MessageRowType.CHAT_REMOTE_GRP_SIG_HISTORY;
                }

                addRemoteMessage(remotePeerId, userName, messageContent, Utils.getDefaultShortTimeStamp(new Date(timeStamp)), messageRowType);

            } else if (message instanceof JSONObject) {
                try {
                    JSONObject messageJson = (JSONObject) message;

                    userName = messageJson.getString(PEER_USERNAME);
                    messageContent = messageJson.getString(MESSAGE_CONTENT);

                    ChatListAdapter.MessageRowType messageRowType;

                    if (remotePeerId != null)
                        messageRowType = ChatListAdapter.MessageRowType.CHAT_REMOTE_GRP_SIG;
                    else
                        messageRowType = ChatListAdapter.MessageRowType.CHAT_REMOTE_GRP_SIG_HISTORY;

                    addRemoteMessage(remotePeerId, userName, messageContent, Utils.getDefaultShortTimeStamp(new Date(timeStamp)), messageRowType);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (message instanceof JSONArray) {
                try {
                    JSONArray messageArray = (JSONArray) message;

                    for (int i = 0; i < messageArray.length(); i++) {
                        JSONObject messageJson = messageArray.getJSONObject(i);
                        userName = messageJson.getString(PEER_USERNAME);
                        messageContent = messageJson.getString(MESSAGE_CONTENT);

                        ChatListAdapter.MessageRowType messageRowType;

                        if (remotePeerId != null)
                            messageRowType = ChatListAdapter.MessageRowType.CHAT_REMOTE_GRP_SIG;
                        else
                            messageRowType = ChatListAdapter.MessageRowType.CHAT_REMOTE_GRP_SIG_HISTORY;

                        addRemoteMessage(remotePeerId, userName, messageContent, Utils.getDefaultShortTimeStamp(new Date(timeStamp)), messageRowType);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * process logic when receiving message directly P2P from the remote peer
     * Note that P2P message does not support message history
     *
     * @param remotePeerId the remote peer that message was sent
     * @param message
     * @param isPrivate    the message sent is private or public for all peers
     */
    @Override
    public void processP2PMessageReceived(String remotePeerId, Object message, boolean isPrivate) {
        if (message == null)
            return;

        ChatListAdapter.MessageRowType messageRowType;
        if (isPrivate) {
            messageRowType = ChatListAdapter.MessageRowType.CHAT_REMOTE_PTE_P2P;
        } else {
            messageRowType = ChatListAdapter.MessageRowType.CHAT_REMOTE_GRP_P2P;
        }

        String userName;
        String messageContent;
        String timeStamp;

        if (message instanceof String) {
            String messageString = (String) message;

            userName = chatService.getPeerNameById(remotePeerId);
            messageContent = messageString;
            timeStamp = Utils.getDefaultShortTimeStamp(new Date());

            addRemoteMessage(remotePeerId, userName, messageContent, timeStamp, messageRowType);

        } else if (message instanceof JSONObject) {
            try {
                JSONObject messageJson = (JSONObject) message;

                userName = messageJson.getString(PEER_USERNAME);
                messageContent = messageJson.getString(MESSAGE_CONTENT);
                timeStamp = Utils.getDefaultShortTimeStamp(new Date(messageJson.getLong(TIMESTAMP)));

                addRemoteMessage(remotePeerId, userName, messageContent, timeStamp, messageRowType);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (message instanceof JSONArray) {
            try {
                JSONArray messageArray = (JSONArray) message;

                for (int i = 0; i < messageArray.length(); i++) {
                    JSONObject messageJson = messageArray.getJSONObject(i);

                    userName = messageJson.getString(PEER_USERNAME);
                    messageContent = messageJson.getString(MESSAGE_CONTENT);
                    timeStamp = Utils.getDefaultShortTimeStamp(new Date(messageJson.getLong(TIMESTAMP)));

                    addRemoteMessage(remotePeerId, userName, messageContent, timeStamp, messageRowType);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void processMessageSendFailed() {
        sentResult = false;
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
     * Update UI when connected to room
     */
    private void processUpdateStateConnected() {

        // Update the view into connected state
        chatView.updateUIConnected(processGetRoomId());

        addMetadataMessage(new SkylinkPeer(chatService.getPeerId(),
                        Utils.getUserNameByType(Constants.CONFIG_TYPE.CHAT)),
                " joined the room.");
        addMessageHistoryMetaData();
    }

    /**
     * Get the room id info
     */
    private String processGetRoomId() {
        return chatService.getRoomId();
    }

    /**
     * Send message in String format
     * Just send the message content that was inputted by the user to the remote peers
     */
    private void processSendMessageString(String message) {
        // Check remote peer to send message to
        // if User did not select the specific peer to send message to,
        // then default is send to all peers in room

        ChatListAdapter.MessageRowType messageRowType = null;
        if (selectedPeerIndex == 0) {
            //add message to list view for displaying
            if (messageType == MESSAGE_TYPE.TYPE_SERVER) {
                chatService.sendServerMessage(null, message);
                messageRowType = ChatListAdapter.MessageRowType.CHAT_LOCAL_GRP_SIG;

            } else if (messageType == MESSAGE_TYPE.TYPE_P2P) {
                chatService.sendP2PMessage(null, message);
                messageRowType = ChatListAdapter.MessageRowType.CHAT_LOCAL_GRP_P2P;
            }
        } else {
            // get the selected peer to send message to
            SkylinkPeer selectedPeer = chatService.getPeerByIndex(selectedPeerIndex);

            // send message to the selected peer
            if (messageType == MESSAGE_TYPE.TYPE_SERVER) {
                chatService.sendServerMessage(selectedPeer.getPeerId(), message);
                messageRowType = ChatListAdapter.MessageRowType.CHAT_LOCAL_PTE_SIG;
            } else if (messageType == MESSAGE_TYPE.TYPE_P2P) {
                chatService.sendP2PMessage(selectedPeer.getPeerId(), message);
                messageRowType = ChatListAdapter.MessageRowType.CHAT_LOCAL_PTE_P2P;
            }
        }

        if (sentResult) {
            addLocalMessage(chatService.getPeerId(), getUserNameByType(Constants.CONFIG_TYPE.CHAT),
                    message, new Date().getTime(), messageRowType);
        }
    }

    /**
     * Send message in JSONObject format
     * JSONObject will have 3 items: PEER_USERNAME, MESSAGE_CONTENT
     */
    private void processSendMessageJson(String message) {
        // Check remote peer to send message to
        // if User did not select the specific peer to send message to,
        // then default is send to all peers in room

        JSONObject sentMessage;
        String peerId = chatService.getPeerId();
        String userName = getUserNameByType(Constants.CONFIG_TYPE.CHAT);
        ChatListAdapter.MessageRowType messageRowType = null;

        if (selectedPeerIndex == 0) {
            //add message to list view for displaying
            if (messageType == MESSAGE_TYPE.TYPE_SERVER) {

                sentMessage = createMessageJSONObject(userName, message);

                chatService.sendServerMessage(null, sentMessage);
                messageRowType = ChatListAdapter.MessageRowType.CHAT_LOCAL_GRP_SIG;

            } else if (messageType == MESSAGE_TYPE.TYPE_P2P) {

                sentMessage = createMessageJSONObject(userName, message);

                chatService.sendP2PMessage(null, sentMessage);
                messageRowType = ChatListAdapter.MessageRowType.CHAT_LOCAL_GRP_P2P;
            }
        } else {
            // get the selected peer to send message to
            SkylinkPeer selectedPeer = chatService.getPeerByIndex(selectedPeerIndex);

            // send message to the selected peer
            if (messageType == MESSAGE_TYPE.TYPE_SERVER) {

                sentMessage = createMessageJSONObject(userName, message);

                chatService.sendServerMessage(selectedPeer.getPeerId(), sentMessage);
                messageRowType = ChatListAdapter.MessageRowType.CHAT_LOCAL_PTE_SIG;
            } else if (messageType == MESSAGE_TYPE.TYPE_P2P) {

                sentMessage = createMessageJSONObject(userName, message);

                chatService.sendP2PMessage(selectedPeer.getPeerId(), sentMessage);
                messageRowType = ChatListAdapter.MessageRowType.CHAT_LOCAL_PTE_P2P;
            }
        }

        if (sentResult) {
            addLocalMessage(peerId, userName, message, new Date().getTime(), messageRowType);
        }
    }

    /**
     * Send message in JSONObject format
     * JSONObject will have 3 items: PEER_USERNAME, MESSAGE_CONTENT, TIMESTAMP
     */
    private void processSendMessageArray(String message) {
        // Check remote peer to send message to
        // if User did not select the specific peer to send message to,
        // then default is send to all peers in room

        JSONArray sentMessage = new JSONArray();
        String peerId = chatService.getPeerId();
        String userName = getUserNameByType(Constants.CONFIG_TYPE.CHAT);
        ChatListAdapter.MessageRowType messageRowType = null;

        JSONObject jsonObjectMessage;

        if (selectedPeerIndex == 0) {
            //add message to list view for displaying
            if (messageType == MESSAGE_TYPE.TYPE_SERVER) {

                jsonObjectMessage = createMessageJSONObject(userName, message);

                sentMessage.put(jsonObjectMessage);
                chatService.sendServerMessage(null, sentMessage);
                messageRowType = ChatListAdapter.MessageRowType.CHAT_LOCAL_GRP_SIG;

            } else if (messageType == MESSAGE_TYPE.TYPE_P2P) {

                jsonObjectMessage = createMessageJSONObject(userName, message);

                sentMessage.put(jsonObjectMessage);
                chatService.sendP2PMessage(null, sentMessage);
                messageRowType = ChatListAdapter.MessageRowType.CHAT_LOCAL_GRP_P2P;
            }
        } else {
            // get the selected peer to send message to
            SkylinkPeer selectedPeer = chatService.getPeerByIndex(selectedPeerIndex);

            // send message to the selected peer
            if (messageType == MESSAGE_TYPE.TYPE_SERVER) {

                jsonObjectMessage = createMessageJSONObject(userName, message);

                sentMessage.put(jsonObjectMessage);
                chatService.sendServerMessage(selectedPeer.getPeerId(), sentMessage);
                messageRowType = ChatListAdapter.MessageRowType.CHAT_LOCAL_PTE_SIG;
            } else if (messageType == MESSAGE_TYPE.TYPE_P2P) {

                jsonObjectMessage = createMessageJSONObject(userName, message);

                sentMessage.put(jsonObjectMessage);
                chatService.sendP2PMessage(selectedPeer.getPeerId(), sentMessage);
                messageRowType = ChatListAdapter.MessageRowType.CHAT_LOCAL_PTE_P2P;
            }
        }

        if (sentResult) {
            addLocalMessage(peerId, userName, message, new Date().getTime(), messageRowType);
        }
    }

    private JSONObject createMessageJSONObject(String userName, String message) {
        JSONObject sentMessage = new JSONObject();
        try {
            sentMessage.put(PEER_USERNAME, userName);
            sentMessage.put(MESSAGE_CONTENT, message);
            return sentMessage;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void addMetadataMessage(SkylinkPeer peer, String metadata) {

        // Adding info to message collection
        // This message is metadata message to inform the peer join the room
        MessageModel messageModel = new MessageModel();
        messageModel.setPeerId(peer.getPeerId());
        messageModel.setPeerUserName(peer.getPeerName());
        messageModel.setMessageContent(metadata);
        messageModel.setTimeStamp(Utils.getDefaultTimeStamp(new Date()));
        messageModel.setMessageRowType(ChatListAdapter.MessageRowType.CHAT_METADATA);

        chatMessageCollection.add(messageModel);
        chatView.updateUIChatCollection(false);
    }

    private void addMessageHistoryMetaData() {
        // Adding info to message collection
        // This message is metadata message to add the button get message history
        MessageModel messageModel = new MessageModel();
        messageModel.setMessageRowType(ChatListAdapter.MessageRowType.CHAT_METADATA_MSG_HISTORY);

        chatMessageCollection.add(messageModel);
        chatView.updateUIChatCollection(false);
    }

    private void addLocalMessage(String localPeerId, String userName, String message,
                                 Long timeStamp, ChatListAdapter.MessageRowType messageRowType) {
        MessageModel messageModel = new MessageModel();
        messageModel.setPeerId(localPeerId);
        messageModel.setPeerUserName(userName);
        messageModel.setMessageContent(message);
        messageModel.setTimeStamp(Utils.getDefaultShortTimeStamp(new Date(timeStamp)));
        messageModel.setMessageRowType(messageRowType);

        chatMessageCollection.add(messageModel);
        chatView.updateUIChatCollection(true);
    }

    private void addRemoteMessage(String remotePeerId, String userName, String messageContent,
                                  String timeStamp, ChatListAdapter.MessageRowType messageRowType) {
        MessageModel messageModel = new MessageModel();
        messageModel.setPeerId(remotePeerId);
        messageModel.setPeerUserName(userName);
        messageModel.setMessageContent(messageContent);
        messageModel.setTimeStamp(timeStamp);
        messageModel.setMessageRowType(messageRowType);

        chatMessageCollection.add(messageModel);
        chatView.updateUIChatCollection(false);
    }
}
