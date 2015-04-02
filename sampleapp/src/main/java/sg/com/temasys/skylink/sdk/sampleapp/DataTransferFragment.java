package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.Activity;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.DataTransferListener;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;

public class DataTransferFragment extends Fragment implements
        RemotePeerListener, DataTransferListener, LifeCycleListener {

    private static final String MY_USER_NAME = "fileTransferUser";
    private static final String ROOM_NAME = "dataTransferRoom";
    private static final String TAG = DataTransferFragment.class.getName();

    private Button btnSendDataRoom;
    private Button btnSendDataPeer;

    private SkylinkConnection skylinkConnection;
    private byte[] data;
    private Set<String> peerIds;
    private boolean connected;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        peerIds = new HashSet<>();

        View rootView = inflater.inflate(R.layout.fragment_data_transfer, container, false);

        TextView transferStatus = (TextView) rootView.findViewById(R.id.txt_data_transfer_status);

        btnSendDataPeer = (Button) rootView.findViewById(R.id.btn_send_data_to_peer);
        btnSendDataPeer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send data to peers specifying their ID's
                for (String peerId : peerIds) {

                    try {
                        skylinkConnection.sendData(peerId, data);
                    } catch (SkylinkException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }

                }
            }
        });

        btnSendDataRoom = (Button) rootView.findViewById(R.id.btn_send_data_to_room);
        btnSendDataRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send data to all the peers
                try {
                    skylinkConnection.sendData(null, data);
                } catch (SkylinkException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });

        getData();
        transferStatus.setText(String.format(getString(R.string.data_transfer_status),
                String.valueOf(data.length)));

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String apiKey = getString(R.string.app_key);
        String apiSecret = getString(R.string.app_secret);

        // Initialize the skylink connection
        initializeSkylinkConnection();

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
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //update actionbar title
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(MainActivity.ARG_SECTION_NUMBER));
    }

    @Override
    public void onDetach() {
        //close the connection when the fragment is detached, so the streams are not open.
        if (skylinkConnection != null && connected) {
            skylinkConnection.disconnectFromRoom();
            skylinkConnection.setRemotePeerListener(null);
            skylinkConnection.setDataTransferListener(null);
            skylinkConnection.setLifeCycleListener(null);
            connected = false;
        }
        super.onDetach();
    }

    private void initializeSkylinkConnection() {
        if (skylinkConnection == null) {
            skylinkConnection = SkylinkConnection.getInstance();
            //the app_key and app_secret is obtained from the temasys developer console.
            skylinkConnection.init(getString(R.string.app_key), getSkylinkConfig(),
                    this.getActivity().getApplicationContext());
            //set listeners to receive callbacks when events are triggered
            skylinkConnection.setRemotePeerListener(this);
            skylinkConnection.setDataTransferListener(this);
            skylinkConnection.setLifeCycleListener(this);
        }
    }

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        // AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY, AUDIO_AND_VIDEO;
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        config.setHasDataTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        return config;
    }

    @Override
    public void onDataReceive(String remotePeerId, byte[] data) {
        if (Arrays.equals(data, this.data)) {
            Toast.makeText(getActivity(), String.format(getString(R.string.data_transfer_received),
                    String.valueOf(data.length)), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Read an image to a byte array
     */
    private void getData() {
        InputStream inputStream = getActivity().getResources().openRawResource(R.raw.icon);
        try {
            data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        peerIds.add(remotePeerId);
        // Enable data send buttons
        btnSendDataPeer.setEnabled(true);
        btnSendDataRoom.setEnabled(true);

        Toast.makeText(getActivity(), String.format(getString(R.string.peer_count),
                peerIds.size()), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        if (peerIds.contains(remotePeerId)) {
            peerIds.remove(remotePeerId);
        }
        if (peerIds.isEmpty()) {
            // Enable data send buttons
            btnSendDataPeer.setEnabled(false);
            btnSendDataRoom.setEnabled(false);
        }

        Toast.makeText(getActivity(), String.format(getString(R.string.peer_count),
                peerIds.size()), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        Log.d(TAG, remotePeerId + " onRemotePeerUserDataReceive");
    }

    @Override
    public void onOpenDataConnection(String remotePeerId) {
        Log.d(TAG, remotePeerId + " onOpenDataConnection");
    }

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        if (isSuccessful) {
            Toast.makeText(getActivity(), String.format(getString(R.string.data_transfer_waiting),
                    ROOM_NAME), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), "Skylink Connection Failed\nReason :" +
                    " " + message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        Toast.makeText(getActivity(), "Peer " + remotePeerId +
                " has changed Room locked status to " + lockStatus, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Log.d(TAG, "onWarning " + message);
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        Log.d(TAG, "onDisconnect " + message);
    }

    @Override
    public void onReceiveLog(String message) {
        Log.d(TAG, "onReceiveLog " + message);
    }
}
