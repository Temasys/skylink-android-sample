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
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection;

/**
 * A placeholder fragment containing a simple view.
 */
public class VideoCallFragment extends Fragment implements LifeCycleListener, MediaListener, RemotePeerListener {
    private static final String TAG = VideoCallFragment.class.getCanonicalName();
    public static final int WIDTH = 350;
    public static final int HEIGHT = 350;
    private LinearLayout parentFragment;
    private Button btnEnterRoom;
    private EditText etRoomName;
    private SkyLinkConnection skyLinkConnection;

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
                    skyLinkConnection.connectToRoom(Constants.ROOM_NAME,
                            Constants.MY_USER_NAME, new Date(), Constants.DURATION);
                } catch (SignatureException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });

        return rootView;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        skyLinkConnection = SkyLinkConnection.getInstance();
        skyLinkConnection.init(getString(R.string.app_key),
                getString(R.string.app_secret), getSkylinkConfig(), this.getActivity().getApplicationContext());

        Log.d(TAG, " lo " + this.getActivity());
        skyLinkConnection.setLifeCycleListener(this);
        skyLinkConnection.setMediaListener(this);
        skyLinkConnection.setRemotePeerListener(this);
    }

    private SkyLinkConfig getSkylinkConfig() {
        SkyLinkConfig config = new SkyLinkConfig();
        config.setAudioVideoSendConfig(SkyLinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        return config;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        skyLinkConnection.disconnectFromRoom();
        skyLinkConnection.setLifeCycleListener(null);
        skyLinkConnection.setMediaListener(null);
        skyLinkConnection.setRemotePeerListener(null);
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
        if (isSuccess) {
            etRoomName.setEnabled(false);
            Toast.makeText(getActivity(), "Connected to room", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Skylink Failed");
        }
    }

    @Override
    public void onLocalMediaCapture(GLSurfaceView videoView, Point size) {
        if (videoView != null) {
            //show media on screen
            videoView.setTag("self");
            parentFragment.addView(videoView);
            Log.d(TAG, "received view");
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
     * Media Listeners
     */

    @Override
    public void onVideoSizeChange(GLSurfaceView videoView, Point size) {
        Log.d(TAG, videoView + " got size");
    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        Log.d(TAG, "onRemotePeerAudioToggle");
    }

    @Override
    public void onRemotePeerVideoToggle(String peerId, boolean isMuted) {
        Log.d(TAG, "onRemotePeerVideoToggle");
    }

    /**
     * Remote Peer Callbacks
     */

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData) {
        Toast.makeText(getActivity(), "Your peer has just connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, GLSurfaceView videoView, Point size) {

        if (videoView == null) {
            return;
        }

        if (parentFragment.findViewWithTag("peer") != null) {
            Toast.makeText(getActivity(), " You are already in connection with two peers",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        View self = parentFragment.findViewWithTag("self");
        parentFragment.removeView(self);
        self.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, HEIGHT));
        parentFragment.addView(self);

        videoView.setTag("peer");
        parentFragment.addView(videoView);
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        Log.d(TAG, "onRemotePeerUserDataReceive " + remotePeerId);
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        Toast.makeText(getActivity(), "Peer go bye bye", Toast.LENGTH_SHORT).show();

        View peer = parentFragment.findViewWithTag("video");
        if (peer != null) {
            parentFragment.removeView(peer);
        }
    }

    @Override
    public void onOpenDataConnection(String peerId) {
        Log.d(TAG, "onOpenDataConnection");
    }
}
