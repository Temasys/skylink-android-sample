package sg.com.temasys.skylink.sdk.sampleapp;

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
import android.widget.ToggleButton;

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
 * This class is used to demonstrate the VideoCall between two clients in WebRTC
 */
public class VideoCallFragment extends Fragment implements LifeCycleListener, MediaListener, RemotePeerListener {
    private static final String TAG = VideoCallFragment.class.getCanonicalName();
    //set height width for self-video when in call
    public static final int WIDTH = 350;
    public static final int HEIGHT = 350;
    private LinearLayout parentFragment;
    private ToggleButton toggleAudioButton;
    private ToggleButton toggleVideoButton;
    private Button btnEnterRoom;
    private EditText etRoomName;
    private SkyLinkConnection skyLinkConnection;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //initialize views
        View rootView = inflater.inflate(R.layout.fragment_video_call, container, false);
        parentFragment = (LinearLayout) rootView.findViewById(R.id.ll_video_call);
        btnEnterRoom = (Button) rootView.findViewById(R.id.btn_enter_room);
        etRoomName = (EditText) rootView.findViewById(R.id.et_room_name);
        toggleAudioButton = (ToggleButton) rootView.findViewById(R.id.toggle_audio);
        toggleVideoButton = (ToggleButton) rootView.findViewById(R.id.toggle_video);

        btnEnterRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String roomName = etRoomName.getText().toString();
                if (roomName.isEmpty()) {
                    Toast.makeText(getActivity(), "Please enter valid room name", Toast.LENGTH_SHORT).show();
                    return;
                }
                btnEnterRoom.setVisibility(View.GONE);

                try {
                    skyLinkConnection.connectToRoom(Constants.ROOM_NAME,
                            roomName, new Date(), Constants.DURATION);
                } catch (SignatureException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });

        toggleAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if toggle is checked, audio is is on, else audio is muted
                skyLinkConnection.muteLocalAudio(!((ToggleButton)v).isChecked());
            }
        });

        toggleVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if toggle is checked, video is is on, else video is muted
                skyLinkConnection.muteLocalVideo(!((ToggleButton)v).isChecked());
            }
        });

        return rootView;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeSkylinkConnection();
    }

    private void initializeSkylinkConnection() {
        skyLinkConnection = SkyLinkConnection.getInstance();
        //the app_key and app_secret is obtained from the temasys developer console.
        skyLinkConnection.init(getString(R.string.app_key),
                getString(R.string.app_secret), getSkylinkConfig(), this.getActivity().getApplicationContext());
        //set listeners to receive callbacks when events are triggered
        skyLinkConnection.setLifeCycleListener(this);
        skyLinkConnection.setMediaListener(this);
        skyLinkConnection.setRemotePeerListener(this);
    }

    private SkyLinkConfig getSkylinkConfig() {
        SkyLinkConfig config = new SkyLinkConfig();
        //AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY, AUDIO_AND_VIDEO;
        config.setAudioVideoSendConfig(SkyLinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        return config;
    }

    @Override
    public void onDetach() {
        //close the connection when the fragment is detached, so the streams are not open.
        if (skyLinkConnection != null) {
            skyLinkConnection.disconnectFromRoom();
            skyLinkConnection.setLifeCycleListener(null);
            skyLinkConnection.setMediaListener(null);
            skyLinkConnection.setRemotePeerListener(null);
        }
        super.onDetach();
    }


    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's lifecycle
     */


    /**
     * Triggered when connection is successful
     *
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        if (isSuccess) {
            etRoomName.setEnabled(false);
            Toast.makeText(getActivity(), "Connected to room + " + etRoomName.getText().toString() + " as " + Constants.MY_USER_NAME  , Toast.LENGTH_SHORT).show();
        } else {
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
     * Media Listeners Callbacks - triggered when receiving changes to Media Stream from the remote peer
     */

    /**
     * Triggered after the user's local media is captured.
     *
     * @param videoView
     * @param size
     */
    @Override
    public void onLocalMediaCapture(GLSurfaceView videoView, Point size) {
        if (videoView != null) {
            //show media on screen
            videoView.setTag("self");
            parentFragment.addView(videoView);
        }
    }

    @Override
    public void onVideoSizeChange(GLSurfaceView videoView, Point size) {
        Log.d(TAG, videoView + " got size");
    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        String message =null;
        if(isMuted)
            message = "Your peer muted their audio";
        else
            message = "Your peer unmuted their audio";

        Toast.makeText(getActivity(),message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemotePeerVideoToggle(String peerId, boolean isMuted) {
        String message =null;
        if(isMuted)
            message = "Your peer muted video";
        else
            message = "Your peer unmuted their video";

        Toast.makeText(getActivity(),message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection with remote peer changes
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
        Toast.makeText(getActivity(), "Your peer has left the room", Toast.LENGTH_SHORT).show();

        View peer = parentFragment.findViewWithTag("video");
        View self = parentFragment.findViewWithTag("self");
        if (peer != null) {
            parentFragment.removeView(peer);
            parentFragment.removeView(self);
            parentFragment.addView(self);
        }
    }

    @Override
    public void onOpenDataConnection(String peerId) {
        Log.d(TAG, "onOpenDataConnection");
    }
}
