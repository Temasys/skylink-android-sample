package sg.com.temasys.skylink.sdk.sampleapp;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MessagesListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;

import static sg.com.temasys.skylink.sdk.sampleapp.MainActivity.ARG_SECTION_NUMBER;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.getNumRemotePeers;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.getPeerIdNick;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.getRoomRoomId;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.isConnectingOrConnected;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.toastLogLong;

/**
 * This class is used to demonstrate the Chat between two clients in WebRTC Created by
 * lavanyasudharsanam on 20/1/15.
 */
public class ChatFragment extends MultiPartyFragment
        implements LifeCycleListener, RemotePeerListener, MessagesListener {

    private String ROOM_NAME;
    private String MY_USER_NAME;

    private static final String TAG = ChatFragment.class.getCanonicalName();

    // Constants for configuration change
    private static final String BUNDLE_IS_PEER_JOINED = "peerJoined";

    private static SkylinkConnection skylinkConnection;
    private static SkylinkConfig skylinkConfig;
    private static List<String> chatMessageCollection;

    private Button btnSendServerMessage;
    private Button btnSendP2PMessage;
    private ListView listViewChats;
    private TextView tvRoomDetails;
    private BaseAdapter adapter;

    private boolean peerJoined;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ROOM_NAME = Config.ROOM_NAME_CHAT;
        MY_USER_NAME = Config.USER_NAME_CHAT;

        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        listViewChats = (ListView) rootView.findViewById(R.id.lv_messages);

        // [MultiParty]
        peerRadioGroup = (RadioGroup) rootView.findViewById(R.id.radio_grp_peers);
        peerAll = (RadioButton) rootView.findViewById(R.id.radio_btn_peer_all);
        peer1 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer1);
        peer2 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer2);
        peer3 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer3);
        peer4 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer4);

        btnSendServerMessage = (Button) rootView.findViewById(R.id.btn_send_server_message);
        btnSendP2PMessage = (Button) rootView.findViewById(R.id.btn_send_p2p_message);
        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_room_details);

        if (chatMessageCollection == null) {
            chatMessageCollection = new ArrayList();
        }

        /** Defining the ArrayAdapter to set items to ListView */
        adapter = new ArrayAdapter<String>(context, R.layout.list_item,
                chatMessageCollection);
        /** Setting the adapter to the ListView */
        listViewChats.setAdapter(adapter);

        // [MultiParty]
        // Initialise peerList if required.
        if (peerList == null) {
            peerList = new ArrayList<Pair<String, String>>();
        }

        // Check if it was an orientation change
        if (savedInstanceState != null) {
            // Set listeners to receive callbacks when events are triggered
            setListeners();

            if (isConnectingOrConnected()) {
                // Set states
                peerJoined = savedInstanceState.getBoolean(BUNDLE_IS_PEER_JOINED);
                // [MultiParty]
                // Populate peerList
                popPeerList(savedInstanceState.getStringArray(BUNDLE_PEER_ID_LIST)
                );
                // Set the appropriate UI if already connected.
                onConnectUIChange();
            }
        } else {
            // [MultiParty]

            // Just set room details
            setRoomDetails();
        }

        // Try to connect to room if not yet connected.
        if (!isConnectingOrConnected()) {
            connectToRoom();
        }

        /** Defining a click event listener for the button "Send Server Message" */
        btnSendServerMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // [MultiParty]
                boolean isPrivate = true;
                String remotePeerId = getPeerIdSelectedWithWarning();
                // Do not allow button actions if there are no Peers in the room.
                if ("".equals(remotePeerId)) {
                    return;
                } else if (remotePeerId == null) {
                    isPrivate = false;
                }

                //Add chat message to the listview
                String message = addSelfMessageToListView(isPrivate, false);

                // Sends message using the signalling server
                // Pass null for remotePeerId to send message to all users in the room
                skylinkConnection.sendServerMessage(remotePeerId, message);
            }
        });

        /** Defining a click event listener for the button "Send Private Message" */
        btnSendP2PMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // [MultiParty]
                boolean isPrivate = true;
                String remotePeerId = getPeerIdSelectedWithWarning();
                // Do not allow button actions if there are no Peers in the room.
                if ("".equals(remotePeerId)) {
                    return;
                } else if (remotePeerId == null) {
                    isPrivate = false;
                }

                //Add chat message to the listview
                String message = addSelfMessageToListView(isPrivate, true);

                try {
                    // Sends message using a DataChannel.
                    // Pass null for remotePeerId to send message to all users in the room
                    skylinkConnection.sendP2PMessage(remotePeerId, message);
                } catch (SkylinkException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("Set1", Config.ROOM_NAME_CHAT);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //update actionbar title
        ((MainActivity) context).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
        this.context = context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save states for fragment restart
        outState.putBoolean(BUNDLE_IS_PEER_JOINED, peerJoined);
        // [MultiParty]
        String[] peerIdList = getPeerIdList();
        outState.putStringArray(BUNDLE_PEER_ID_LIST, peerIdList);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Remove static members from views.
        listViewChats = null;

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already connected and not changing orientation.
        if (!((MainActivity) context).isChangingConfigurations() && skylinkConnection != null
                && isConnectingOrConnected()) {
            skylinkConnection.disconnectFromRoom();
        }
    }

    //----------------------------------------------------------------------------------------------
    // Skylink helper methods
    //----------------------------------------------------------------------------------------------

    private SkylinkConfig getSkylinkConfig() {
        if (skylinkConfig != null) {
            return skylinkConfig;
        }

        skylinkConfig = new SkylinkConfig();
        // AudioVideo config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setHasPeerMessaging(true);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    private void connectToRoom() {
        // Initialize the skylink connection
        initializeSkylinkConnection();

        // Create the Skylink connection string.
        // In production, the connection string should be generated by an external entity
        // (such as a secure App server that has the Skylink App Key secret), and sent to the App.
        // This is to avoid keeping the App Key secret within the application, for better security.
        String skylinkConnectionString = Utils.getSkylinkConnectionString(
                ROOM_NAME, new Date(), SkylinkConnection.DEFAULT_DURATION);

        // The skylinkConnectionString should not be logged in production,
        // as it contains potentially sensitive information like the Skylink App Key ID.

        skylinkConnection.connectToRoom(skylinkConnectionString, MY_USER_NAME);
    }

    private void initializeSkylinkConnection() {
        skylinkConnection = SkylinkConnection.getInstance();
        //the app_key and app_secret is obtained from the temasys developer console.
        skylinkConnection.init(Config.getAppKey(), getSkylinkConfig(),
                context.getApplicationContext());

        //set listeners to receive callbacks when events are triggered
        setListeners();
    }

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

    //----------------------------------------------------------------------------------------------
    // UI helper methods
    //----------------------------------------------------------------------------------------------

    /**
     * Change certain UI elements once connected to room or when Peer(s) join or leave.
     */
    private void onConnectUIChange() {
        listViewRefresh();
        // [MultiParty]
        setRoomDetails();
        fillPeerRadioBtn();
    }

    /**
     * Set the room details on UI.
     */
    void setRoomDetails() {
        Utils.setRoomDetailsMulti(isConnectingOrConnected(), peerJoined, tvRoomDetails,
                getRoomRoomId(skylinkConnection, ROOM_NAME),
                Utils.getDisplayName(skylinkConnection, MY_USER_NAME, null));
    }

    /**
     * Retrieves self message written in edit text and adds it to the chatlistview.
     * Will refresh listView.
     *
     * @param isPrivateMessage
     * @param isP2P
     * @return message that was added to the listview
     */
    private String addSelfMessageToListView(boolean isPrivateMessage, boolean isP2P) {
        EditText edit = (EditText) ((MainActivity) context).findViewById(R.id.chatMessage);
        String prefix = "You : ";
        prefix += isPrivateMessage ? "[PTE]" : "[GRP]";
        prefix += isP2P ? "[P2P] " : "[SIG] ";
        String message = edit.getText().toString();
        chatMessageCollection.add(prefix + message);
        edit.setText("");
        listViewRefresh();
        return message;
    }

    /**
     * Refresh the ListView's data set and position displayed.
     * Will display latest data.
     */
    private void listViewRefresh() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            if (listViewChats != null) {
                listViewChats.setSelection(adapter.getCount() - 1);
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Skylink Listeners
    //----------------------------------------------------------------------------------------------

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
            String log = "Connected to room " + ROOM_NAME + " (" + skylinkConnection.getRoomId() +
                    ") as " + skylinkConnection.getPeerId() + " (" + MY_USER_NAME + ").";
            toastLogLong(TAG, context, log);
            // [MultiParty]
            // Set the appropriate UI if already connected.
            onConnectUIChange();
        } else {
            String log = "Skylink failed to connect!\nReason : " + message;
            toastLogLong(TAG, context, log);
            setRoomDetails();
        }
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        // [MultiParty]
        // Reset peerList
        peerList.clear();
        setRoomDetails();
        // Reset chat collection
        chatMessageCollection.clear();

        String log = "[onDisconnect] ";
        if (errorCode == Errors.DISCONNECT_FROM_ROOM) {
            log += "We have successfully disconnected from the room.";
        } else if (errorCode == Errors.DISCONNECT_UNEXPECTED_ERROR) {
            log += "WARNING! We have been unexpectedly disconnected from the room!";
        }
        log += " Server message: " + message;
        toastLogLong(TAG, context, log);
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        String log = "[SA] Peer " + remotePeerId + " changed Room locked status to "
                + lockStatus + ".";
        toastLog(TAG, context, log);
    }

    @Override
    public void onReceiveLog(int infoCode, String message) {
        Utils.handleSkylinkReceiveLog(infoCode, message, context, TAG);
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Utils.handleSkylinkWarning(errorCode, message, context, TAG);
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
        String log = logTag + "Peer \"" + getPeerIdNick(remotePeerId) + " connected.";
        toastLog(TAG, context, log);
        addPeerRadioBtn(remotePeerId, nick);
        //Set room status if it's the only peer in the room.
        if (getPeerNum() == 1) {
            peerJoined = true;
            // Update textview to show room status
            setRoomDetails();
        }
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message, UserInfo userInfo) {
        // [MultiParty]
        // Remove the Peer.
        removePeerRadioBtn(remotePeerId);

        //Set room status if there are no more peers.
        if (peerList.size() == 0) {
            peerJoined = false;
            // Update textview to show room status
            setRoomDetails();
        }

        int numRemotePeers = getNumRemotePeers();
        String log = "Your Peer " + getPeerIdNick(remotePeerId, userInfo) + " left: " +
                message + ". " + numRemotePeers + " remote Peer(s) left in the room.";
        toastLog(TAG, context, log);
    }

    @Override
    public void onRemotePeerConnectionRefreshed(String remotePeerId, Object userData, boolean hasDataChannel, boolean wasIceRestarted) {
        String peer = "Skylink Media Relay server";
        if (remotePeerId != null) {
            peer = "Peer " + getPeerIdNick(remotePeerId);
        }
        String log = "Your connection with " + peer + " has just been refreshed";
        if (wasIceRestarted) {
            log += ", with ICE restarted.";
        } else {
            log += ".\r\n";
        }

        toastLog(TAG, context, log);
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        // If Peer has no userData, use an empty string for nick.
        String nick = "";
        if (userData != null) {
            nick = userData.toString();
        }
        String log = "[SA][onRemotePeerUserDataReceive] Peer " + Utils.getPeerIdNick(remotePeerId) +
                ":\n" + nick;
        toastLog(TAG, context, log);
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
            String remotePeerName = getPeerIdNick(remotePeerId);
            chatMessageCollection.add(remotePeerName + " : " + chatPrefix + message);
            listViewRefresh();
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
            String remotePeerName = getPeerIdNick(remotePeerId);
            chatMessageCollection.add(remotePeerName + " : " + chatPrefix + message);
            listViewRefresh();
        }
    }
}
