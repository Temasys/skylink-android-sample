package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.Activity;
import android.graphics.Point;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

import java.util.Date;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;

/**
 * This class is used to demonstrate the AudioCall between two clients in WebRTC Created by
 * lavanyasudharsanam on 20/1/15.
 */
public class AudioCallFragment extends Fragment implements LifeCycleListener, MediaListener, RemotePeerListener {
    private static final String TAG = AudioCallFragment.class.getCanonicalName();
    public static final String ROOM_NAME = "audioCallRoom";
    public static final String MY_USER_NAME = "audioCallUser";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private SkylinkConnection skylinkConnection;
    private TextView tvRoomDetails;
    private String remotePeerId;
    private String peerName;
    private boolean connected;
    private AudioRouter audioRouter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_audio_call, container, false);

        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_room_details);

        final Button btnAudioCall = (Button) rootView.findViewById(R.id.btn_audio_call);
        btnAudioCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String apiKey = getString(R.string.app_key);
                String apiSecret = getString(R.string.app_secret);

                // Initialize the skylink connection
                initializeSkylinkConnection();

                // Initialize the audio router
                initializeAudioRouter();

                // Obtaining the Skylink connection string done locally
                // In a production environment the connection string should be given
                // by an entity external to the App, such as an App server that holds the Skylink API secret
                // In order to avoid keeping the API secret within the application
                String skylinkConnectionString = Utils.
                        getSkylinkConnectionString(ROOM_NAME, apiKey,
                                apiSecret, new Date(), SkylinkConnection.DEFAULT_DURATION);

                skylinkConnection.connectToRoom(skylinkConnectionString,
                        MY_USER_NAME);
                connected = true;

                // Use the Audio router to switch between headphone and headset
                audioRouter.startAudioRouting(getActivity().getApplicationContext());

                btnAudioCall.setEnabled(false);
                Toast.makeText(getActivity(), "Connecting....",
                        Toast.LENGTH_SHORT).show();
            }
        });
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Allow volume to be controlled using volume keys
        getActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    private void initializeAudioRouter() {
        if (audioRouter == null) {
            audioRouter = AudioRouter.getInstance();
            audioRouter.init(((AudioManager) getActivity().
                    getSystemService(android.content.Context.AUDIO_SERVICE)));
        }
    }

    private void initializeSkylinkConnection() {
        if (skylinkConnection == null) {
            skylinkConnection = SkylinkConnection.getInstance();
            skylinkConnection.init(getString(R.string.app_key), getSkylinkConfig(),
                    this.getActivity().getApplicationContext());

            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setMediaListener(this);
            skylinkConnection.setRemotePeerListener(this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //update actionbar title
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (skylinkConnection != null && connected) {
            skylinkConnection.disconnectFromRoom();
            skylinkConnection.setLifeCycleListener(null);
            skylinkConnection.setMediaListener(null);
            skylinkConnection.setRemotePeerListener(null);
            connected = false;
            audioRouter.stopAudioRouting(getActivity().getApplicationContext());
        }
    }

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        return config;
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
            Log.d(TAG, "Skylink Connected");
            Utils.setRoomDetails(false, tvRoomDetails, this.peerName, ROOM_NAME, MY_USER_NAME);
        } else {
            Toast.makeText(getActivity(), "Skylink Connection Failed\nReason : "
                    + message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Log.d(TAG, message + "warning");
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        Log.d(TAG, message + " disconnected");
    }

    @Override
    public void onReceiveLog(String message) {
        Log.d(TAG, message + "onReceiveLog");
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        Toast.makeText(getActivity(), "Peer " + remotePeerId +
                " has changed Room locked status to " + lockStatus, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocalMediaCapture(GLSurfaceView glSurfaceView, Point point) {
        Log.d(TAG, "onLocalMediaCapture");
    }

    @Override
    public void onVideoSizeChange(GLSurfaceView glSurfaceView, Point point) {
        Log.d(TAG, point.toString() + "got size");
    }

    @Override
    public void onRemotePeerAudioToggle(String s, boolean b) {
        Log.d(TAG, "onRemotePeerAudioToggle");
    }

    @Override
    public void onRemotePeerVideoToggle(String s, boolean b) {
        Log.d(TAG, "onRemotePeerVideoToggle");
    }

    @Override
    public void onRemotePeerMediaReceive(String s, GLSurfaceView glSurfaceView, Point point) {
        Log.d(TAG, "onRemotePeerVideoToggle");
    }

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        // If there is an existing peer, prevent new remotePeer from joining call.
        if (this.remotePeerId != null) {
            Toast.makeText(getActivity(), "Rejected third peer from joining conversation",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        //if first remote peer to join room, keep track of user and update text-view to display details
        this.remotePeerId = remotePeerId;
        if (userData instanceof String) {
            this.peerName = (String) userData;
            Utils.setRoomDetails(true, tvRoomDetails, this.peerName, ROOM_NAME, MY_USER_NAME);
        }
    }

    @Override
    public void onRemotePeerUserDataReceive(String s, Object o) {
        Log.d(TAG, "onRemotePeerUserDataReceive");
    }

    @Override
    public void onOpenDataConnection(String s) {
        Log.d(TAG, "onOpenDataConnection");

    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        Toast.makeText(getActivity(), "Your peer has left the room", Toast.LENGTH_SHORT).show();
        //reset peerId
        this.remotePeerId = null;
        this.peerName = null;
        //update textview to show room status
        Utils.setRoomDetails(false, tvRoomDetails, this.peerName, ROOM_NAME, MY_USER_NAME);
    }
}
