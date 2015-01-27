package com.temasys.skylink.sampleapp.activities;

import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

import org.json.JSONException;

import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Date;

import sg.com.temasys.skylink.sdk.config.SkyLinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MessagesListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkyLinkException;

/**
 * Created by lavanyasudharsanam on 20/1/15.
 */
public class ChatFragment extends Fragment implements LifeCycleListener, RemotePeerListener, MessagesListener {
    private static final String TAG = ChatFragment.class.getCanonicalName();
    String peerId;
    final String myName = "usernamechat";
    final String roomName = "room";
    Button btnSendServerMessage;
    ListView listViewChats;
    TextView tvRoomDetails;
    private SkyLinkConnection skyLinkConnection;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> chatMessageCollection;
    private String peerName;
    private Button btnSendP2PMessage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        listViewChats = (ListView) rootView.findViewById(R.id.lv_messages);
        btnSendServerMessage = (Button) rootView.findViewById(R.id.btn_send_server_message);
        btnSendP2PMessage = (Button) rootView.findViewById(R.id.btn_send_private_chat);
        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_room_details);
        chatMessageCollection = new ArrayList();

        /** Defining the ArrayAdapter to set items to ListView */
        adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_list_item_1, chatMessageCollection);

        /** Defining a click event listener for the button "Send Message" */
        btnSendServerMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText edit = (EditText) getActivity().findViewById(R.id.chatMessage);
                String message = edit.getText().toString();
                chatMessageCollection.add("You : " + message);
                edit.setText("");
                skyLinkConnection.sendCustomMessage(peerId, message);

                adapter.notifyDataSetChanged();
            }
        });

        /** Defining a click event listener for the button "Send Private Message" */
        btnSendP2PMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(peerId==null){
                    Toast.makeText(getActivity(),"There is no peer in the room to send a private message to",Toast.LENGTH_SHORT).show();
                    return;
                }

                EditText edit = (EditText) getActivity().findViewById(R.id.chatMessage);
                String message = edit.getText().toString();
                chatMessageCollection.add("You : " + message);
                edit.setText("");

                try {
                    skyLinkConnection.sendPeerMessage(peerId, message);
                } catch (SkyLinkException e) {
                    e.printStackTrace();
                }

                adapter.notifyDataSetChanged();
            }
        });

        /** Setting the adapter to the ListView */
        listViewChats.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        skyLinkConnection = SkyLinkConnection.getInstance();
        skyLinkConnection.init(getString(R.string.app_key),
                getString(R.string.app_secret), getSkylinkConfig(), this.getActivity().getApplicationContext());
        skyLinkConnection.setLifeCycleListener(this);
        skyLinkConnection.setMessagesListener(this);
        skyLinkConnection.setRemotePeerListener(this);

        try {
            skyLinkConnection.connectToRoom(roomName, myName, new Date(), 20000);
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private SkyLinkConfig getSkylinkConfig() {
        SkyLinkConfig config = new SkyLinkConfig();
        config.setHasAudio(true);
        config.setHasVideo(true);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(60);
        return config;
    }

    private void setRoomDetails(boolean isPeerInRoom){
        String roomDetails = "Room Name : " + roomName + "\nYou are signed in as : " + myName + "\n";
        if(isPeerInRoom)
            roomDetails += "Peer Name : " + this.peerName;
        else
            roomDetails += "You are alone in this room";

        tvRoomDetails.setText(roomDetails);

    }

    /***
     * Lifecycle Listener
     */

    /**
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        if (isSuccess){
            setRoomDetails(false);
        }
        else {
            Log.d(TAG, "Skylink Failed");
        }
    }

    @Override
    public void onWarning(String message) {
        Log.d(TAG, message + "warning");
    }

    @Override
    public void onDisconnect(String message) {
        Log.d(TAG, message + " disconnected");
    }

    @Override
    public void onReceiveLog(String message) {
        Log.d(TAG, message + " on receive log");
    }

    /**
     * Remote Peer Callbacks
     */

    @Override
    public void onPeerJoin(String peerId, Object userData) {
        if(this.peerId!=null) { //means there is an existing peer
            Toast.makeText(getActivity(), "Rejected third peer from joining conversation",Toast.LENGTH_SHORT).show();
            return;
        }
        this.peerId = peerId;
        if(userData instanceof String) {
            this.peerName = (String) userData;
            setRoomDetails(true);
        }

    }

    @Override
    public void onGetPeerMedia(String peerId, GLSurfaceView videoView,
                               Point size) {
    }

    @Override
    public void onUserData(String peerId, Object userData) {
        Toast.makeText(getActivity(), "Getting user data", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPeerLeave(String peerId, String message) {
        Toast.makeText(getActivity(), "Your peer has left the room", Toast.LENGTH_SHORT).show();
        this.peerId = null;
        this.peerName = null;
        setRoomDetails(false);
    }

    @Override
    public void onOpenDataConnection(String peerId) {
    }

    @Override
    public void onCustomMessage(String peerId, Object message, boolean isPrivate) {
        String chatPrefix = "";
        if(isPrivate)
            chatPrefix = "<Private> ";

        if(message instanceof  String) {
            chatMessageCollection.add(this.peerName + " : " + chatPrefix+message);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPeerMessage(String peerId, Object message, boolean isPrivate) {
        String chatPrefix = "";
        if(isPrivate)
            chatPrefix = "<Private> ";
        if(message instanceof  String) {
            chatMessageCollection.add(this.peerName + " : " + chatPrefix+message);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    @Deprecated
    public void onChatMessage(String peerId, String nick, String message,
                              boolean isPrivate) {
        Toast.makeText(getActivity(), "Got Message--onchat", Toast.LENGTH_SHORT).show();
        chatMessageCollection.add("message received");
        chatMessageCollection.add(message);
        adapter.notifyDataSetChanged();
    }


    @Override
    public void onDetach() {
        if (skyLinkConnection != null) {
            skyLinkConnection.disconnect();
            skyLinkConnection = null;
        }
        super.onDetach();
    }

    @Override
    public void onDestroy(){
        if (skyLinkConnection != null) {
            skyLinkConnection.disconnect();
            skyLinkConnection = null;
        }
        super.onDestroy();
    }
}
