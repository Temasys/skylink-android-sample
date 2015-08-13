package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MessagesListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.ErrorCodes;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;

/**
 * This class is used to demonstrate the Chat between two clients in WebRTC Created by
 * lavanyasudharsanam on 20/1/15.
 */
public class ChatFragment extends Fragment implements LifeCycleListener, RemotePeerListener, MessagesListener {

    private static final String TAG = ChatFragment.class.getCanonicalName();
    public static final String ROOM_NAME = Constants.ROOM_NAME_CHAT;
    public static final String MY_USER_NAME = "chatRoomUser";
    private static final String ARG_SECTION_NUMBER = "section_number";

    // Constants for configuration change
    private static final String BUNDLE_IS_CONNECTED = "isConnected";
    private static final String BUNDLE_IS_PEER_JOINED = "peerJoined";
    private static final String BUNDLE_PEER_ID = "peerId";
    private static final String BUNDLE_PEER_NAME = "remotePeerName";

    private static SkylinkConnection skylinkConnection;
    private static List<String> chatMessageCollection;

    private Button btnSendPrivateServerMessage;
    private Button btnSendP2PPublicMessage;
    private Button btnSendPublicServerMessage;
    private ListView listViewChats;
    private TextView tvRoomDetails;
    private BaseAdapter adapter;
    private Button btnSendP2PMessage;

    private String remotePeerId;
    private String remotePeerName;
    private boolean connected;
    private boolean peerJoined;
    private boolean orientationChange;
    private Activity parentActivity;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //initialize views
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        listViewChats = (ListView) rootView.findViewById(R.id.lv_messages);
        btnSendPrivateServerMessage = (Button) rootView.findViewById(R.id.btn_send_server_message);
        btnSendPublicServerMessage = (Button) rootView.findViewById(R.id.btn_send_public_server_message);
        btnSendP2PMessage = (Button) rootView.findViewById(R.id.btn_send_private_chat);
        btnSendP2PPublicMessage = (Button) rootView.findViewById(R.id.btn_send_p2p_public_message);
        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_room_details);

        String appKey = getString(R.string.app_key);
        String appSecret = getString(R.string.app_secret);

        if (chatMessageCollection == null) {
            chatMessageCollection = new ArrayList();
        }
        /** Defining the ArrayAdapter to set items to ListView */
        adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_list_item_1, chatMessageCollection);
        /** Setting the adapter to the ListView */
        listViewChats.setAdapter(adapter);


        // Check if it was an orientation change
        if (savedInstanceState != null) {
            connected = savedInstanceState.getBoolean(BUNDLE_IS_CONNECTED);
            if (connected) {
                // Set listeners to receive callbacks when events are triggered
                setListeners();
                // Set states
                peerJoined = savedInstanceState.getBoolean(BUNDLE_IS_PEER_JOINED);
                remotePeerId = savedInstanceState.getString(BUNDLE_PEER_ID, null);
                remotePeerName = savedInstanceState.getString(BUNDLE_PEER_NAME, null);
                // Set the appropriate UI if already connected.
                onConnectUIChange();
            }
        }

        if (!connected) {
            skylinkConnection = null;
            // Initialize the skylink connection
            initializeSkylinkConnection();

            // Obtaining the Skylink connection string done locally
            // In a production environment the connection string should be given
            // by an entity external to the App, such as an App server that holds the Skylink App secret
            // In order to avoid keeping the App secret within the application
            String skylinkConnectionString = Utils.
                    getSkylinkConnectionString(ROOM_NAME, appKey,
                            appSecret, new Date(), SkylinkConnection.DEFAULT_DURATION);

            skylinkConnection.connectToRoom(skylinkConnectionString,
                    MY_USER_NAME);
        }

        /** Defining a click event listener for the button "Send Private Server Message" */
        btnSendPrivateServerMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Add chat message to the listview
                String message = addSelfMessageToListView(true, false);

                //pass null for remotePeerId to send message to send mesage to all users in the room
                //sends message using the signalling server
                skylinkConnection.sendServerMessage(remotePeerId, message);
            }
        });

        /** Defining a click event listener for the button "Send Public Server Message" */
        btnSendPublicServerMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Add chat message to the listview
                String message = addSelfMessageToListView(false, false);

                //pass remotePeerId instead of null to send message to specific peer
                //sends message using the signalling server
                skylinkConnection.sendServerMessage(null, message);
            }
        });

        /** Defining a click event listener for the button "Send Private Message" */
        btnSendP2PMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (remotePeerId == null) {
                    Toast.makeText(getActivity(), "There is no peer in the room to send a private message to", Toast.LENGTH_SHORT).show();
                    return;
                }

                //Add chat message to the listview
                String message = addSelfMessageToListView(true, true);

                try {
                    //sends p2p message using the datachannel to the specific user
                    skylinkConnection.sendP2PMessage(remotePeerId, message);
                } catch (SkylinkException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });

        /** Defining a click event listener for the button "Send Public P2P Message" */
        btnSendP2PPublicMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (remotePeerId == null) {
                    Toast.makeText(getActivity(), "There is no peer in the room to send a private message to", Toast.LENGTH_SHORT).show();
                    return;
                }

                //Add chat message to the listview
                String message = addSelfMessageToListView(false, true);

                try {
                    //sends p2p message using the datachannel to the all users
                    skylinkConnection.sendP2PMessage(null, message);
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
        outState.putBoolean(BUNDLE_IS_CONNECTED, connected);
        outState.putBoolean(BUNDLE_IS_PEER_JOINED, peerJoined);
        outState.putString(BUNDLE_PEER_ID, remotePeerId);
        outState.putString(BUNDLE_PEER_NAME, remotePeerName);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Remove static members from views.
        listViewChats = null;

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already connected and not changing orientation.
        if (!orientationChange && skylinkConnection != null && connected) {
            skylinkConnection.disconnectFromRoom();
            connected = false;
        }
    }

    /***
     * Helper methods
     */

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        // AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY, AUDIO_AND_VIDEO;
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        config.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        return config;
    }

    private void initializeSkylinkConnection() {
        if (skylinkConnection == null) {
            skylinkConnection = SkylinkConnection.getInstance();
            //the app_key and app_secret is obtained from the temasys developer console.
            skylinkConnection.init(getString(R.string.app_key),
                    getSkylinkConfig(), this.getActivity().getApplicationContext());

            //set listeners to receive callbacks when events are triggered
            setListeners();
        }
    }

    /**
     * Set listeners to receive callbacks when events are triggered
     */
    private void setListeners() {
        skylinkConnection.setLifeCycleListener(this);
        skylinkConnection.setRemotePeerListener(this);
        skylinkConnection.setMessagesListener(this);
    }

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
        EditText edit = (EditText) getActivity().findViewById(R.id.chatMessage);
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
     * Change certain UI elements once connected to room.
     */
    private void onConnectUIChange() {
        Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME, MY_USER_NAME);
        listViewRefresh();
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's lifecycle
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
            connected = true;
            // Set the appropriate UI if already connected.
            onConnectUIChange();
        } else {
            Toast.makeText(getActivity(), "Skylink Connection Failed\nReason : "
                    + message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        Toast.makeText(getActivity(), "Peer " + remotePeerId +
                " has changed Room locked status to " + lockStatus, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Log.d(TAG, message + "warning");
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        skylinkConnection = null;
        Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME, MY_USER_NAME);
        // Reset chat collection
        chatMessageCollection.clear();

        String log = message;
        if (errorCode == ErrorCodes.DISCONNECT_FROM_ROOM) {
            log = "[onDisconnect] We have successfully disconnected from the room. Server message: "
                    + message;
        }
        Toast.makeText(parentActivity, log, Toast.LENGTH_LONG).show();
        Log.d(TAG, log);
    }

    @Override
    public void onReceiveLog(String message) {
        Log.d(TAG, message + " on receive log");
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        // If there is an existing peer, prevent new remotePeer from joining call.
        if (this.remotePeerId != null) {
            Toast.makeText(getActivity(), "Rejected third peer from joining conversation",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        //if first remote peer to join room, keep track of user and update text-view to display details
        peerJoined = true;
        this.remotePeerId = remotePeerId;
        if (userData instanceof String) {
            this.remotePeerName = (String) userData;
        }
        Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME, MY_USER_NAME);
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        Toast.makeText(getActivity(), "Getting user data", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        Toast.makeText(parentActivity, "Your peer has left the room", Toast.LENGTH_SHORT).show();
        //reset peerId
        peerJoined = false;
        this.remotePeerId = null;
        this.remotePeerName = null;
        //update textview to show room status
        Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, remotePeerName, ROOM_NAME, MY_USER_NAME);
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
        //add message to listview and update ui
        if (message instanceof String) {
            chatMessageCollection.add(this.remotePeerName + " : " + chatPrefix + message);
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
            chatMessageCollection.add(this.remotePeerName + " : " + chatPrefix + message);
            listViewRefresh();
        }
    }
}
