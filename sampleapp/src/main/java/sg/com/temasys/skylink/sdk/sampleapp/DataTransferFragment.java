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
import sg.com.temasys.skylink.sdk.rtc.ErrorCodes;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;

public class DataTransferFragment extends Fragment implements
        RemotePeerListener, DataTransferListener, LifeCycleListener {

    private static final String MY_USER_NAME = "DataTransferUser";
    private static final String ROOM_NAME = Constants.ROOM_NAME_DATA;
    private static final String TAG = DataTransferFragment.class.getName();

    // Constants for configuration change
    private static final String BUNDLE_IS_CONNECTED = "isConnected";

    TextView transferStatus;
    private Button btnSendDataRoom;
    private Button btnSendDataPeer;

    private static SkylinkConnection skylinkConnection;
    private static Set<String> peerIdSet;
    private static byte[] dataPrivate;
    private static byte[] dataGroup;
    private boolean connected;
    private boolean orientationChange;
    private Activity parentActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_data_transfer, container, false);

        transferStatus = (TextView) rootView.findViewById(R.id.txt_data_transfer_status);
        btnSendDataRoom = (Button) rootView.findViewById(R.id.btn_send_data_to_room);
        btnSendDataPeer = (Button) rootView.findViewById(R.id.btn_send_data_to_peer);

        String appKey = getString(R.string.app_key);
        String appSecret = getString(R.string.app_secret);

        // Check if it was an orientation change
        if (savedInstanceState != null) {
            connected = savedInstanceState.getBoolean(BUNDLE_IS_CONNECTED);
            if (connected) {
                // Set listeners to receive callbacks when events are triggered
                setListeners();
                // Set states
            }
        }

        if (!connected) {
            peerIdSet = new HashSet<>();
            getDataGroup();

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

        // Set UI
        onConnectUIChange();

        btnSendDataPeer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send dataPrivate to specific Peer
                for (String peerId : peerIdSet) {

                    try {
                        skylinkConnection.sendData(peerId, dataPrivate);
                    } catch (SkylinkException e) {
                        Log.e(TAG, e.getMessage(), e);
                    } catch (UnsupportedOperationException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }
        });

        btnSendDataRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send dataGroup to all Peers
                try {
                    skylinkConnection.sendData(null, dataGroup);
                } catch (SkylinkException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (UnsupportedOperationException e) {
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
        //update actionbar title
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(MainActivity.ARG_SECTION_NUMBER));
        parentActivity = getActivity();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Note that orientation change is happening.
        orientationChange = true;
        // Save states for fragment restart
        outState.putBoolean(BUNDLE_IS_CONNECTED, connected);
    }

    @Override
    public void onDetach() {
        super.onDetach();

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

    /**
     * Set dataGroup to contain 2 of dataPrivate.
     */
    private void getDataGroup() {
        if (dataGroup == null) {
            getDataPrivate();
            int len = dataPrivate.length;
            dataGroup = new byte[2 * len];
            System.arraycopy(dataPrivate, 0, dataGroup, 0, len);
            System.arraycopy(dataPrivate, 0, dataGroup, len, len);
        }
    }

    /**
     * Read an image to a byte array and put in dataPrivate
     */
    private void getDataPrivate() {
        if (dataPrivate == null) {
            InputStream inputStream = getActivity().getResources().openRawResource(R.raw.icon);
            try {
                dataPrivate = new byte[inputStream.available()];
                inputStream.read(dataPrivate);
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        // AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY, AUDIO_AND_VIDEO;
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        config.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        config.setHasDataTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        return config;
    }

    private void initializeSkylinkConnection() {
        if (skylinkConnection == null) {
            skylinkConnection = SkylinkConnection.getInstance();
            //the app_key and app_secret is obtained from the temasys developer console.
            skylinkConnection.init(getString(R.string.app_key), getSkylinkConfig(),
                    this.getActivity().getApplicationContext());

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
        skylinkConnection.setDataTransferListener(this);
    }

    /***
     * UI helper methods
     */

    /**
     * Change certain UI elements once connected to room or when Peer(s) join or leave.
     */
    private void onConnectUIChange() {
        // Enable data buttons IFF at least 1 Peer is in the room.
        if (peerIdSet != null && !peerIdSet.isEmpty()) {
            btnSendDataPeer.setEnabled(true);
            btnSendDataRoom.setEnabled(true);
            // Show info about data sizes that can be transferred.
            transferStatus.setText(String.format(getString(R.string.data_transfer_status),
                    String.valueOf(dataPrivate.length), String.valueOf(dataGroup.length)));
        } else {
            btnSendDataPeer.setEnabled(false);
            btnSendDataRoom.setEnabled(false);
            transferStatus.setText(String.format(getString(R.string.no_peer)));
        }
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's lifecycle
     */

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        if (isSuccessful) {
            connected = true;
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
        skylinkConnection = null;
        // Reset PeerId set
        peerIdSet.clear();
        dataPrivate = null;
        dataGroup = null;

        String log = message;
        if (errorCode == ErrorCodes.DISCONNECT_FROM_ROOM) {
            log = "[onDisconnect] We have successfully disconnected from the room. Server message: "
                    + message;
        }
        Toast.makeText(parentActivity, "[onDisconnect] " + log, Toast.LENGTH_LONG).show();
        Log.d(TAG, log);
    }

    @Override
    public void onReceiveLog(String message) {
        Log.d(TAG, "onReceiveLog " + message);
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        peerIdSet.add(remotePeerId);

        // Set UI
        onConnectUIChange();

        Toast.makeText(getActivity(), String.format(getString(R.string.peer_count),
                peerIdSet.size()), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        if (peerIdSet.contains(remotePeerId)) {
            peerIdSet.remove(remotePeerId);
        }
        // Set UI
        onConnectUIChange();

        Toast.makeText(parentActivity, String.format(getString(R.string.peer_count),
                peerIdSet.size()), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        Log.d(TAG, remotePeerId + " onRemotePeerUserDataReceive");
    }

    @Override
    public void onOpenDataConnection(String remotePeerId) {
        Log.d(TAG, remotePeerId + " onOpenDataConnection");
    }

    /**
     * Data Transfer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onDataReceive(String remotePeerId, byte[] data) {
        // Check if it is one of the data that we can send.
        if (Arrays.equals(data, this.dataPrivate) || Arrays.equals(data, this.dataGroup)) {
            Toast.makeText(getActivity(), String.format(getString(R.string.data_transfer_received_expected),
                    String.valueOf(data.length)), Toast.LENGTH_SHORT).show();
        } else {
            // Received some unexpected data that could be from other apps
            // or perhaps different due to so some problems somewhere.
            Toast.makeText(getActivity(), String.format(getString(R.string.data_transfer_received_unexpected),
                    String.valueOf(data.length)), Toast.LENGTH_LONG).show();
        }
    }

}
