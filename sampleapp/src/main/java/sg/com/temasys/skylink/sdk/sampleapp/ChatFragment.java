package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.Activity;
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
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

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

/**
 * This class is used to demonstrate the Chat between two clients in WebRTC Created by
 * lavanyasudharsanam on 20/1/15.
 */
public class ChatFragment extends MultiPartyFragment
        implements LifeCycleListener, RemotePeerListener, MessagesListener {

    public static final String ROOM_NAME = Constants.ROOM_NAME_CHAT;
    public static final String MY_USER_NAME = "chatRoomUser";
    private static final String TAG = ChatFragment.class.getCanonicalName();
    private static final String ARG_SECTION_NUMBER = "section_number";

    // Constants for configuration change
    private static final String BUNDLE_IS_CONNECTED = "isConnected";
    private static final String BUNDLE_IS_CONNECT_ATTEMPTED = "isConnectAttempted";
    private static final String BUNDLE_IS_PEER_JOINED = "peerJoined";

    private static SkylinkConnection skylinkConnection;
    private static List<String> chatMessageCollection;

    private Button btnSendServerMessage;
    private Button btnSendP2PMessage;
    private ListView listViewChats;
    private TextView tvRoomDetails;
    private BaseAdapter adapter;

    private boolean peerJoined;
    private boolean orientationChange;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //initialize views
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

        String appKey = getString(R.string.app_key);
        String appSecret = getString(R.string.app_secret);

        if (chatMessageCollection == null) {
            chatMessageCollection = new ArrayList();
        }

        /** Defining the ArrayAdapter to set items to ListView */
        adapter = new ArrayAdapter<String>(parentActivity, android.R.layout.simple_list_item_1,
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

            if (isConnected()) {
                // Set states
                peerJoined = savedInstanceState.getBoolean(BUNDLE_IS_PEER_JOINED);
                // [MultiParty]
                // Populate peerList
                popPeerList(savedInstanceState.getStringArray(BUNDLE_PEER_ID_LIST),
                        skylinkConnection);
                // Set the appropriate UI if already connected.
                onConnectUIChange();
            }
        } else {
            // [MultiParty]
            // Just set room details
            Utils.setRoomDetailsMulti(isConnected(), peerJoined, tvRoomDetails, ROOM_NAME, MY_USER_NAME);
        }

        // Try to connect to room if not yet connected.
        if (!isConnected()) {
            connectToRoom(appKey, appSecret);
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
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
        parentActivity = getActivity();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Note that orientation change is happening.
        orientationChange = true;
        // Save states for fragment restart
        outState.putBoolean(BUNDLE_IS_PEER_JOINED, peerJoined);
        // [MultiParty]
        outState.putStringArray(BUNDLE_PEER_ID_LIST, getPeerIdList());
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Remove static members from views.
        listViewChats = null;

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already connected and not changing orientation.
        if (!orientationChange && skylinkConnection != null && isConnected()) {
            skylinkConnection.disconnectFromRoom();
        }
    }

    /***
     * Skylink Helper methods
     */

    /**
     * Check if we are currently connected to the room.
     *
     * @return True if we are connected and false otherwise.
     */
    private boolean isConnected() {
        if (skylinkConnection != null) {
            return skylinkConnection.isConnected();
        }
        return false;
    }

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        // AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY,
        // AUDIO_AND_VIDEO;
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        config.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        // To enable logs from Skylink SDK (e.g. during debugging),
        // Uncomment the following. Do not enable logs for production apps!
        // config.setEnableLogs(true);

        return config;
    }

    private void connectToRoom(String appKey, String appSecret) {
        // Initialize the skylink connection
        initializeSkylinkConnection();

        // Obtaining the Skylink connection string done locally
        // In a production environment the connection string should be given
        // by an entity external to the App, such as an App server that holds the Skylink App
        // secret
        // In order to avoid keeping the App secret within the application
        String skylinkConnectionString = Utils.
                getSkylinkConnectionString(ROOM_NAME,
                        appKey,
                        appSecret, new Date(),
                        SkylinkConnection.DEFAULT_DURATION);

        skylinkConnection.connectToRoom(skylinkConnectionString, MY_USER_NAME);
    }

    private void initializeSkylinkConnection() {
        skylinkConnection = SkylinkConnection.getInstance();
        //the app_key and app_secret is obtained from the temasys developer console.
        skylinkConnection.init(getString(R.string.app_key),
                getSkylinkConfig(), parentActivity.getApplicationContext());

        //set listeners to receive callbacks when events are triggered
        setListeners();
    }

    /**
     * Set listeners to receive callbacks when events are triggered.
     * SkylinkConnection instance must not be null or listeners cannot be set.
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
     * Helper methods
     */

    /***
     * UI helper methods
     */

    /**
     * Retrieves self message written in edit text and adds it to the chatlistview.
     * Will refresh listView.
     *
     * @param isPrivateMessage
     * @param isP2P
     * @return message that was added to the listview
     */
    private String addSelfMessageToListView(boolean isPrivateMessage, boolean isP2P) {
        EditText edit = (EditText) parentActivity.findViewById(R.id.chatMessage);
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

    /**
     * Change certain UI elements once connected to room or when Peer(s) join or leave.
     */
    private void onConnectUIChange() {
        listViewRefresh();
        // [MultiParty]
        Utils.setRoomDetailsMulti(isConnected(), peerJoined, tvRoomDetails, ROOM_NAME, MY_USER_NAME);
        fillPeerRadioBtn();
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's
     * lifecycle
     */

    /**
     * Triggered if the connection is successful
     *
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        //update textview if connection is successful
        if (isSuccess) {
            // [MultiParty]
            // Set the appropriate UI if already connected.
            onConnectUIChange();
        } else {
            Log.d(TAG, "Skylink failed to connect!");
            Toast.makeText(parentActivity, "Skylink failed to connect!\nReason : "
                    + message, Toast.LENGTH_SHORT).show();
            Utils.setRoomDetailsMulti(isConnected(), peerJoined, tvRoomDetails, ROOM_NAME, MY_USER_NAME);
        }
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        Toast.makeText(parentActivity, "Peer " + remotePeerId +
                " has changed Room locked status to " + lockStatus, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Log.d(TAG, "On warning: " + message);
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        // [MultiParty]
        // Reset peerList
        peerList.clear();
        Utils.setRoomDetailsMulti(isConnected(), peerJoined, tvRoomDetails, ROOM_NAME, MY_USER_NAME);
        // Reset chat collection
        chatMessageCollection.clear();

        String log = message;
        if (errorCode == Errors.DISCONNECT_FROM_ROOM) {
            log = "[onDisconnect] We have successfully disconnected from the room. Server message: "
                    + message;
        }
        Toast.makeText(parentActivity, log, Toast.LENGTH_LONG).show();
        Log.d(TAG, log);
    }

    @Override
    public void onReceiveLog(int infoCode, String message) {
        Log.d(TAG, "On receive log: " + message);
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        Toast.makeText(parentActivity, "Peer " + remotePeerId + " has joined the room",
                Toast.LENGTH_SHORT).show();
        // [MultiParty]
        //When remote peer joins room, keep track of user and update UI.
        // If Peer has no userData, use an empty string for nick.
        String nick = "";
        if (userData != null) {
            nick = userData.toString();
        }
        addPeerRadioBtn(remotePeerId, nick);
        //Set room status if it's the only peer in the room.
        if (getPeerNum() == 1) {
            peerJoined = true;
            // Update textview to show room status
            Utils.setRoomDetailsMulti(isConnected(), peerJoined, tvRoomDetails, ROOM_NAME,
                    MY_USER_NAME);
        }
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        Toast.makeText(parentActivity, "Getting user data", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        Toast.makeText(parentActivity, "Peer " + remotePeerId + " has left the room",
                Toast.LENGTH_SHORT).show();
        // [MultiParty]
        // Remove the Peer.
        removePeerRadioBtn(remotePeerId);

        //Set room status if there are no more peers.
        if (peerList.size() == 0) {
            peerJoined = false;
            // Update textview to show room status
            Utils.setRoomDetailsMulti(isConnected(), peerJoined, tvRoomDetails, ROOM_NAME,
                    MY_USER_NAME);
        }
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
            String remotePeerName = Utils.getUserNick(skylinkConnection, remotePeerId);
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
            String remotePeerName = Utils.getUserNick(skylinkConnection, remotePeerId);
            chatMessageCollection.add(remotePeerName + " : " + chatPrefix + message);
            listViewRefresh();
        }
    }
}
