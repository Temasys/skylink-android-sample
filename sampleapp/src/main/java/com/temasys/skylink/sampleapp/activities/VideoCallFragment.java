package com.temasys.skylink.sampleapp.activities;

/**
 * Created by lavanyasudharsanam on 20/1/15.
 */

import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

import org.json.JSONException;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Date;

import sg.com.temasys.skylink.sdk.config.SkyLinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection;

/**
 * A placeholder fragment containing a simple view.
 */
public class VideoCallFragment extends Fragment implements SkyLinkConnection.LifeCycleDelegate, SkyLinkConnection.MediaDelegate, SkyLinkConnection.RemotePeerDelegate {
    private static final String TAG = VideoCallFragment.class.getCanonicalName();
    final String userName = "userVideo";
    LinearLayout parentFragment;
    Button btnEnterRoom;
    EditText etRoomName;
    SkyLinkConnection skyLinkConnection;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video_call, container, false);
        parentFragment = (LinearLayout) rootView.findViewById(R.id.ll_video_call);
        btnEnterRoom = (Button) rootView.findViewById(R.id.btn_enter_room);
        etRoomName = (EditText) rootView.findViewById(R.id.et_room_name);

        etRoomName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etRoomName.setEnabled(true);
            }
        });
        btnEnterRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String roomName = etRoomName.getText().toString();
                if (roomName.isEmpty()) {
                    Toast.makeText(getActivity(), "Please enter valid room name", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    skyLinkConnection.connectToRoom(roomName, userName, new Date(), 200);
                } catch (SignatureException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });

        return rootView;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        skyLinkConnection = new SkyLinkConnection(getString(R.string.app_key),
                getString(R.string.app_secret), getSkylinkConfig(), this.getActivity());

        Log.d(TAG, " lo " + this.getActivity());
        skyLinkConnection.setLifeCycleDelegate(this);
        skyLinkConnection.setMediaDelegate(this);
        skyLinkConnection.setRemotePeerDelegate(this);
        skyLinkConnection.setMessagesDelegate(this);
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

    /***
     * Lifecycle delegate
     */

    /**
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        if (isSuccess) {
            etRoomName.setEnabled(false);
            Toast.makeText(getActivity(), "Connected to room", Toast.LENGTH_SHORT).show();
        } else
            Log.d(TAG, "Skylink Failed");
    }

    @Override
    public void onGetUserMedia(GLSurfaceView videoView, Point size) {
        // TODO Auto-generated method stub
        //show media on screen
        videoView.setTag("self");
        parentFragment.addView(videoView);
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
     * Media Listeners
     */

    @Override
    public void onVideoSize(GLSurfaceView videoView, Point size) {
        // TODO Auto-generated method stub
        Log.d(TAG, videoView + " got size");


    }

    @Override
    public void onToggleAudio(String peerId, boolean isMuted) {
        // TODO Auto-generated method stub


    }

    @Override
    public void onToggleVideo(String peerId, boolean isMuted) {
        // TODO Auto-generated method stub

    }

    /**
     * Remote Peer Callbacks
     */

    @Override
    public void onPeerJoin(String peerId, Object userData) {
        // TODO Auto-generated method stub
        Toast.makeText(getActivity(), "Your peer has just connected", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onGetPeerMedia(String peerId, GLSurfaceView videoView,
                               Point size) {

        if (parentFragment.findViewWithTag("peer") != null) {
            Toast.makeText(getActivity(), " You are already in connection with two peers", Toast.LENGTH_SHORT).show();
            return;
        }

        View self = parentFragment.findViewWithTag("self");
        parentFragment.removeView(self);
        self.setLayoutParams(new ViewGroup.LayoutParams(350, 350));
        parentFragment.addView(self);

        videoView.setTag("peer");
        parentFragment.addView(videoView);
    }

    @Override
    public void onUserData(String peerId, Object userData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPeerLeave(String peerId, String message) {
        Toast.makeText(getActivity(), "Peer go bye bye", Toast.LENGTH_SHORT).show();

        View peer = parentFragment.findViewWithTag("video");
        if (peer != null)
            parentFragment.removeView(peer);
        //TODO: resize self

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

}
