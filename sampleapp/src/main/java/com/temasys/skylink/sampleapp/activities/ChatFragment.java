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
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

import org.json.JSONException;

import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Date;

import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection;

/**
 * Created by lavanyasudharsanam on 20/1/15.
 */
public class ChatFragment extends Fragment implements SkyLinkConnection.LifeCycleDelegate, SkyLinkConnection.RemotePeerDelegate, SkyLinkConnection.MessagesDelegate {
    private static final String TAG = ChatFragment.class.getCanonicalName();
    String peerId;
    String myName = "usernamechat";
    Button btnSendMessage;
    ListView listViewChats;
    private SkyLinkConnection skyLinkConnection;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> chatMessageCollection;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        listViewChats = (ListView) rootView.findViewById(R.id.lv_messages);
        btnSendMessage = (Button) rootView.findViewById(R.id.btn_send_chat);
        chatMessageCollection = new ArrayList();

        /** Defining the ArrayAdapter to set items to ListView */
        adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_list_item_1, chatMessageCollection);

        /** Defining a click event listener for the button "Add" */
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText edit = (EditText) getActivity().findViewById(R.id.chatMessage);

                String message = edit.getText().toString();
                chatMessageCollection.add(message);

                edit.setText("");

                skyLinkConnection.sendCustomMessage(peerId, message);

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

        skyLinkConnection = new SkyLinkConnection(getString(R.string.app_key),
                getString(R.string.app_secret), getSkylinkConfig(), this.getActivity());
//
//        Log.d(TAG, " lo " + this.getActivity());
        skyLinkConnection.setLifeCycleDelegate(this);
        skyLinkConnection.setMessagesDelegate(this);
        skyLinkConnection.setRemotePeerDelegate(this);

        try {
            skyLinkConnection.connectToRoom("room", myName, new Date(), 20000);
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private SkyLinkConnection.SkyLinkConfig getSkylinkConfig() {
        SkyLinkConnection.SkyLinkConfig config = new SkyLinkConnection.SkyLinkConfig();
        config.setHasAudio(true);
        config.setHasVideo(true);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(60);
        return config;
    }

    /***
     * Lifecycle delegate
     */

    /**
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        // TODO Auto-generated method stub
        if (isSuccess)
            Log.d(TAG, "Skylink Connected");
        else
            Log.d(TAG, "Skylink Failed");
    }

    @Override
    public void onGetUserMedia(GLSurfaceView videoView, Point size) {
        // TODO Auto-generated method stub
        //show media on screen
        videoView.setTag("self");
//        parentFragment.addView(videoView);
        Log.d(TAG, videoView + "received view");

    }

    @Override
    public void onWarning(String message) {
        // TODO Auto-generated method stub
        Log.d(TAG, message + "warning");

    }

    @Override
    public void onDisconnect(String message) {
        // TODO Auto-generated method stub

        Log.d(TAG, message + " disconnected");
    }

    @Override
    public void onReceiveLog(String message) {
        // TODO Auto-generated method stub
        Log.d(TAG, message + " on receive log");


    }

    /**
     * Remote Peer Callbacks
     */

    @Override
    public void onPeerJoin(String peerId, Object userData) {
        this.peerId = peerId;

    }

    @Override
    public void onGetPeerMedia(String peerId, GLSurfaceView videoView,
                               Point size) {
//        Toast.makeText(getActivity(), "Getting media", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onUserData(String peerId, Object userData) {
        // TODO Auto-generated method stub
        Toast.makeText(getActivity(), "Getting user data", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPeerLeave(String peerId, String message) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOpenDataConnection(String peerId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDetach() {
        super.onDetach();
        skyLinkConnection.disconnect();
    }

    @Override
    public void onCustomMessage(String peerId, Object message, boolean isPrivate) {
        Toast.makeText(getActivity(), "Got Message--custom", Toast.LENGTH_SHORT).show();
        chatMessageCollection.add("message received");
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPeerMessage(String peerId, Object message, boolean isPrivate) {
        Toast.makeText(getActivity(), "Got Message--peer", Toast.LENGTH_SHORT).show();
        chatMessageCollection.add("message received");
        adapter.notifyDataSetChanged();
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


}
