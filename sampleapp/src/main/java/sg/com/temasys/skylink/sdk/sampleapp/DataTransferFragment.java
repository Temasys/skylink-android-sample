package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import sg.com.temasys.skylink.sdk.listener.DataTransferListener;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;

import static sg.com.temasys.skylink.sdk.sampleapp.Utils.getNumRemotePeers;

public class DataTransferFragment extends MultiPartyFragment implements
        RemotePeerListener, DataTransferListener,
        LifeCycleListener {

    private String ROOM_NAME;
    private String MY_USER_NAME;

    private static final String TAG = DataTransferFragment.class.getName();

    // Constants for configuration change
    private static final String BUNDLE_IS_PEER_JOINED = "peerJoined";
    private static SkylinkConnection skylinkConnection;
    private static byte[] dataPrivate;
    private static byte[] dataGroup;
    private TextView tvRoomDetails;
    private TextView transferStatus;
    private Button btnSendDataRoom;
    private Button btnSendDataPeer;
    private boolean peerJoined;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        MY_USER_NAME = Config.USER_NAME_DATA;
        ROOM_NAME = Config.ROOM_NAME_DATA;

        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_data_transfer, container, false);

        peerRadioGroup = (RadioGroup) rootView.findViewById(R.id.radio_grp_peers);
        peerAll = (RadioButton) rootView.findViewById(R.id.radio_btn_peer_all);
        peer1 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer1);
        peer2 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer2);
        peer3 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer3);
        peer4 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer4);

        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_room_details);
        transferStatus = (TextView) rootView.findViewById(R.id.txt_data_transfer_status);
        btnSendDataRoom = (Button) rootView.findViewById(R.id.btn_send_data_to_room);
        btnSendDataPeer = (Button) rootView.findViewById(R.id.btn_send_data_to_peer);

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
                popPeerList(savedInstanceState.getStringArray(BUNDLE_PEER_ID_LIST)
                );
                // Set the appropriate UI if already connected.
                onConnectUIChange();
            }
        } else {
            // [MultiParty]
            // Just set room details
            Utils.setRoomDetailsMulti(isConnected(), peerJoined, tvRoomDetails, ROOM_NAME, MY_USER_NAME);
        }

        // Show info about data sizes that can be transferred.
        getDataGroup();
        transferStatus.setText(String.format(getString(R.string.data_transfer_status),
                String.valueOf(dataPrivate.length), String.valueOf(dataGroup.length)));

        // Try to connect to room if not yet connected.
        if (!isConnected()) {
            connectToRoom();
        }

        btnSendDataPeer.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // [MultiParty]
                        String remotePeerId = getPeerIdSelectedWithWarning();
                        // Do not allow button actions if there are no Peers in the room.
                        if ("".equals(remotePeerId)) {
                            return;
                        }

                        try {
                            if (remotePeerId == null) {
                                // Send dataGroup to all Peer(s)
                                skylinkConnection.sendData(remotePeerId, dataGroup);
                            } else {
                                // Send dataPrivate to specific Peer
                                skylinkConnection.sendData(remotePeerId, dataPrivate);
                            }
                        } catch (SkylinkException e) {
                            String exMsg = e.getMessage();
                            Toast.makeText(parentActivity, exMsg, Toast.LENGTH_LONG).show();
                            Log.e(TAG, exMsg, e);
                        } catch (UnsupportedOperationException e) {
                            String exMsg = e.getMessage();
                            Toast.makeText(parentActivity, exMsg, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, exMsg, e);
                        }

                    }
                }
        );

        btnSendDataRoom.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // [MultiParty]
                        // Do not allow button actions if there are no Peers in the room.
                        if (getPeerNum() == 0) {
                            Toast.makeText(parentActivity,
                                    getString(R.string.warn_no_peer_message),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Select All Peers RadioButton if not already selected
                        String remotePeerId = getPeerIdSelected(
                        );
                        if (remotePeerId != null) {
                            peerAll.setChecked(true);
                        }

                        // Send dataGroup to all Peers
                        try {
                            skylinkConnection.sendData(null, dataGroup);
                        } catch (SkylinkException e) {
                            String exMsg = e.getMessage();
                            Toast.makeText(parentActivity, exMsg, Toast.LENGTH_LONG).show();
                            Log.e(TAG, exMsg, e);
                        } catch (UnsupportedOperationException e) {
                            String exMsg = e.getMessage();
                            Toast.makeText(parentActivity, exMsg, Toast.LENGTH_LONG).show();
                            Log.e(TAG, exMsg, e);
                        }
                    }
                }

        );

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
        // Save states for fragment restart
        outState.putBoolean(BUNDLE_IS_PEER_JOINED, peerJoined);
        // [MultiParty]
        outState.putStringArray(BUNDLE_PEER_ID_LIST, getPeerIdList());
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        if (!parentActivity.isChangingConfigurations() && skylinkConnection != null
                && isConnected()) {
            skylinkConnection.disconnectFromRoom();
            dataPrivate = null;
            dataGroup = null;
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
        // AudioVideo config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        config.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        config.setHasDataTransfer(true);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(config);
        return config;
    }

    private void connectToRoom() {
        // Initialize the skylink connection
        initializeSkylinkConnection();

        // Obtaining the Skylink connection string done locally
        // In a production environment the connection string should be given
        // by an entity external to the App, such as an App server that holds the Skylink App
        // secret
        // In order to avoid keeping the App secret within the application
        String skylinkConnectionString = Utils.getSkylinkConnectionString(
                ROOM_NAME, new Date(), SkylinkConnection.DEFAULT_DURATION);

        skylinkConnection.connectToRoom(skylinkConnectionString, MY_USER_NAME);
    }

    private void initializeSkylinkConnection() {
        skylinkConnection = SkylinkConnection.getInstance();
        //the app_key and app_secret is obtained from the temasys developer console.
        skylinkConnection.init(Config.getAppKey(), getSkylinkConfig(),
                parentActivity.getApplicationContext());

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
            skylinkConnection.setDataTransferListener(this);
            return true;
        } else {
            return false;
        }
    }

    /***
     * Helper methods
     */

    /**
     * Set dataGroup to contain 2 of dataPrivate.
     * Will get dataPrivate if dataGroup and dataPrivate are null.
     */
    private void getDataGroup() {
        getDataPrivate();
        if (dataGroup == null) {
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
            InputStream inputStream = parentActivity.getResources().openRawResource(R.raw.icon);
            try {
                dataPrivate = new byte[inputStream.available()];
                inputStream.read(dataPrivate);
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    /***
     * UI helper methods
     */

    /**
     * Change certain UI elements once isConnected() to room or when Peer(s) join or leave.
     */
    private void onConnectUIChange() {
        // [MultiParty]
        Utils.setRoomDetailsMulti(isConnected(), peerJoined, tvRoomDetails, ROOM_NAME, MY_USER_NAME);
        fillPeerRadioBtn();
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's
     * lifecycle
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        //Update textview if connection is successful
        if (isSuccess) {
            // [MultiParty]
            // Set the appropriate UI if already isConnected().
            onConnectUIChange();
        } else {
            Log.d(TAG, "Skylink failed to connect!");
            Toast.makeText(parentActivity, "Skylink failed to connect!\nReason: "
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
        Utils.handleSkylinkWarning(errorCode, message, parentActivity, TAG);
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        // [MultiParty]
        // Reset peerList
        peerList.clear();
        // Set the appropriate UI after disconnecting.
        onConnectUIChange();
        String log = "";
        if (errorCode == Errors.DISCONNECT_FROM_ROOM) {
            log += "We have successfully disconnected from the room.";
        } else if (errorCode == Errors.DISCONNECT_UNEXPECTED_ERROR) {
            log += "WARNING! We have been unexpectedly disconnected from the room!";
        }
        log += " Server message: " + message;

        Toast.makeText(parentActivity, log, Toast.LENGTH_LONG).show();
        log = "[onDisconnect] " + log;
        Log.d(TAG, log);
    }

    @Override
    public void onReceiveLog(int infoCode, String message) {
        Utils.handleSkylinkReceiveLog(infoCode, message, parentActivity, TAG);
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
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
        String log = "Your Peer " + Utils.getPeerIdNick(remotePeerId) + " connected.";
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message, UserInfo userInfo) {
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

        int numRemotePeers = getNumRemotePeers();
        String log = "Your Peer " + Utils.getPeerIdNick(remotePeerId, userInfo) + " left: " +
                message + ". " + numRemotePeers + " remote Peer(s) left in the room.";
        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
    }

    @Override
    public void onRemotePeerConnectionRefreshed(String remotePeerId, Object userData,
                                                boolean hasDataChannel, boolean wasIceRestarted) {
        String peer = "Skylink Media Relay server";
        if (remotePeerId != null) {
            peer = "Peer " + Utils.getPeerIdNick(remotePeerId);
        }
        String log = "Your connection with " + peer + " has just been refreshed";
        if (wasIceRestarted) {
            log += ", with ICE restarted.";
        } else {
            log += ".\r\n";
        }

        Toast.makeText(parentActivity, log, Toast.LENGTH_SHORT).show();
        Log.d(TAG, log);
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
     * Data Transfer Listener Callbacks - triggered during events that happen when data or
     * connection
     * with remote peer changes
     */

    @Override
    public void onDataReceive(String remotePeerId, byte[] data) {
        // Check if it is one of the data that we can send.
        if (Arrays.equals(data, this.dataPrivate) || Arrays.equals(data, this.dataGroup)) {
            Toast.makeText(parentActivity,
                    String.format(getString(R.string.data_transfer_received_expected),
                            String.valueOf(data.length)), Toast.LENGTH_SHORT).show();
        } else {
            // Received some unexpected data that could be from other apps
            // or perhaps different due to so some problems somewhere.
            Toast.makeText(parentActivity,
                    String.format(getString(R.string.data_transfer_received_unexpected),
                            String.valueOf(data.length)), Toast.LENGTH_LONG).show();
        }
    }

}
