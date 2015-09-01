package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.Activity;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.FileTransferListener;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.ErrorCodes;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;

/**
 * Created by lavanyasudharsanam on 20/1/15.
 */
public class FileTransferFragment extends Fragment
        implements LifeCycleListener, FileTransferListener, RemotePeerListener {

    public static final String ROOM_NAME = Constants.ROOM_NAME_FILE;
    public static final String MY_USER_NAME = "fileTransferUser";
    public static final String EXTERNAL_STORAGE = "ExternalStorage";
    private static final String TAG = FileTransferFragment.class.getCanonicalName();
    private static final String ARG_SECTION_NUMBER = "section_number";

    // Constants for configuration change
    private static final String BUNDLE_IS_CONNECTED = "isConnected";
    private static final String BUNDLE_IS_PEER_JOINED = "peerJoined";
    private static final String BUNDLE_PEER_ID = "peerId";
    private static final String BUNDLE_PEER_NAME = "remotePeerName";
    private static SkylinkConnection skylinkConnection;
    private TextView tvRoomDetails;
    private EditText etSenderFilePath;
    private TextView tvFileTransferDetails;
    private ImageView ivFilePreview;
    private Button sendFilePrivate;
    private Button sendFileGroup;
    private String fileNamePrivate = "FileTransferPrivate.png";
    private String fileNameGroup = "FireTransferGroup.png";
    private String fileNameDownloaded = "downloadFile.png";
    private String remotePeerId;
    private String remotePeerName;
    private boolean connected;
    private boolean peerJoined;
    private boolean orientationChange;
    private Activity parentActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_file_transfer, container, false);
        sendFilePrivate = (Button) rootView.findViewById(R.id.btnSendFilePte);
        sendFileGroup = (Button) rootView.findViewById(R.id.btnSendFileGrp);
        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_room_details);
        etSenderFilePath = (EditText) rootView.findViewById(R.id.et_file_path);
        ivFilePreview = (ImageView) rootView.findViewById(R.id.iv_file_preview);
        tvFileTransferDetails = (TextView) rootView.findViewById(R.id.tv_file_transfer_details);

        // Prepare default file for transfer and set UI.
        prepFile(getFileToTransfer(fileNamePrivate).getAbsolutePath());

        String appKey = getString(R.string.app_key);
        String appSecret = getString(R.string.app_secret);

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
            }
        }

        if (!connected) {
            // Copy files raw/R.raw.icon and raw/R.raw.icon_group to the device's file system
            createExternalStoragePrivatePicture();

            skylinkConnection = null;
            // Initialize the skylink connection
            initializeSkylinkConnection();

            // Obtaining the Skylink connection string done locally
            // In a production environment the connection string should be given
            // by an entity external to the App, such as an App server that holds the Skylink App
            // secret
            // In order to avoid keeping the App secret within the application
            String skylinkConnectionString = Utils.
                                                          getSkylinkConnectionString(ROOM_NAME,
                                                                  appKey,
                                                                  appSecret, new Date(),
                                                                  SkylinkConnection
                                                                          .DEFAULT_DURATION);

            skylinkConnection.connectToRoom(skylinkConnectionString,
                    MY_USER_NAME);
        }

        // Set the appropriate UI
        Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, this.remotePeerName, ROOM_NAME,
                MY_USER_NAME);

        // Send file to specific Peer.
        sendFilePrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Prepare private file for transfer.
                prepFile(getFileToTransfer(fileNamePrivate).getAbsolutePath());
                sendFile(remotePeerId);
            }
        });

        // Send file to all Peers in room, i.e. via public (AKA group) message.
        sendFileGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Prepare group file for transfer.
                prepFile(getFileToTransfer(fileNameGroup).getAbsolutePath());
                sendFile(null);
            }
        });

        return rootView;
    }

    /**
     * Sends a file to a Peer or all Peers in room.
     *
     * @param tid Peer to send to. Use null to send to all in room.
     */
    private void sendFile(String tid) {
        //Check if peer exists
        if (remotePeerId == null) {
            Toast.makeText(getActivity(), "There is no peer in the room to send a file to",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if valid file
        String filePath = etSenderFilePath.getText().toString();
        File file = new File(filePath);
        String fileName;
        if (file.exists()) {
            ivFilePreview.setImageURI(Uri.parse(filePath));
            fileName = file.getName();
        } else {
            Toast.makeText(getActivity(), "Please enter a valid filename", Toast.LENGTH_SHORT)
                 .show();
            return;
        }

        // Send request to peer requesting permission for file transfer
        try {
            skylinkConnection.sendFileTransferPermissionRequest(tid, fileName,
                    getFileToTransfer(fileName).getAbsolutePath());
            String peer = "";
            if (tid == null) {
                peer = "all Peers in room";
            } else {
                peer = "Peer " + tid;
            }
            String toast = "Sending file to " + peer + ".";
            Toast.makeText(getActivity(), toast, Toast.LENGTH_SHORT).show();
        } catch (SkylinkException e) {
            Log.e(TAG, e.getMessage(), e);
        }
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
        //AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY,
        // AUDIO_AND_VIDEO;
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
        skylinkConnection.setFileTransferListener(this);
    }

    /**
     * Creates a dummy file from the apk's asset folder to the device's filepath so that there is a
     * default file to transfer
     */
    void createExternalStoragePrivatePicture() {
        // Create a path where we will place our pictures in our own private
        // pictures directory.  Note that we don't really need to place a
        // picture in DIRECTORY_PICTURES, since the media scanner will see
        // all media in these directories; this may be useful with other
        // media types such as DIRECTORY_MUSIC however to help it classify
        // your media for display to the user.

        // Files to be created on device...
        File fileCopy1 = getFileToTransfer(fileNamePrivate);
        File fileCopy2 = getFileToTransfer(fileNameGroup);

        // ...copied from resource files here:
        int fileIn1 = R.raw.icon;
        int fileIn2 = R.raw.icon_group;

        // Copy resource files into files on Device's directory.
        copyFile(fileIn1, fileCopy1);
        copyFile(fileIn2, fileCopy2);
    }

    /**
     * @param fileIn   File to be copied as a resource id.
     * @param fileCopy
     */
    private void copyFile(int fileIn, File fileCopy) {
        try {
            // Very simple code to copy a picture from the application's
            // resource into the external file.  Note that this code does
            // no error checking, and assumes the picture is small (does not
            // try to copy it in chunks).  Note that if external storage is
            // not currently mounted this will silently fail.
            InputStream is = getResources().openRawResource(fileIn);
            OutputStream os = new FileOutputStream(fileCopy);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this.getActivity(),
                    new String[]{fileCopy.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i(EXTERNAL_STORAGE, "Scanned " + path + ":");
                            Log.i(EXTERNAL_STORAGE, "-> uri=" + uri);
                        }
                    });
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w(EXTERNAL_STORAGE, "Error writing " + fileCopy, e);
        }
    }

    /***
     * UI helper methods
     */

    /**
     * Prepare file before transfer. Sets file path in edit text. Sets file preview.
     *
     * @param filePath
     */
    private void prepFile(String filePath) {
        //show preview of file to transfer
        ivFilePreview.setImageURI(Uri.parse(filePath));
        etSenderFilePath.setText(filePath);
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's
     * lifecycle
     */

    /**
     * Triggered if the connection is successful
     *
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        //Update textview if connection is successful
        if (isSuccess) {
            connected = true;
            Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, this.remotePeerName,
                    ROOM_NAME, MY_USER_NAME);
        } else {
            Log.d(TAG, "Skylink Failed");
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
        Log.d(TAG, message + " on receive log");
    }

    /**
     * File Transfer Listener Callbacks - triggered during events that happen during file transfer
     * between peers
     */

    @Override
    public void onFileTransferPermissionRequest(String peerId, String fileName, boolean isPrivate) {
        Toast.makeText(getActivity(), "Received a file request", Toast.LENGTH_LONG).show();
        // Take note of download file name.
        if (!"".equals(fileName)) {
            fileNameDownloaded = fileName;
        }
        //Send false to reject file transfer
        skylinkConnection.sendFileTransferPermissionResponse(peerId, getDownloadedFilePath(), true);
    }

    @Override
    public void onFileTransferPermissionResponse(String peerId, String fileName, boolean
            isPermitted) {
        if (isPermitted) {
            Toast.makeText(getActivity(), "Sending file", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(),
                    "Sorry, the remote peer has not granted permission for file transfer",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void onFileTransferDrop(String remotePeerId, String fileName, String message,
            boolean isExplicit) {
        Toast.makeText(getActivity(), "The file transfer was dropped.\nReason : " + message,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFileSendComplete(String remotePeerId, String fileName) {
        Toast.makeText(getActivity(), "Your file has been sent", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onFileSendProgress(String remotePeerId, String fileName, double percentage) {
        Toast.makeText(getActivity(), "Uploading... " + percentage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFileReceiveComplete(String remotePeerId, String fileName) {
        Toast.makeText(getActivity(), "A file has been received : " + fileName, Toast.LENGTH_SHORT)
             .show();
        tvFileTransferDetails
                .setText("File Transfer Successful\n\nDestination : " + getDownloadedFilePath());
    }

    @Override
    public void onFileReceiveProgress(String remotePeerId, String fileName, double percentage) {
        Toast.makeText(getActivity(), "Downloading... " + percentage, Toast.LENGTH_SHORT).show();
    }


    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    public void onRemotePeerJoin(String peerId, Object userData, boolean hasDataChannel) {
        if (this.remotePeerId != null) {
            // If there is an existing peer, prevent new remotePeer from joining call.
            Toast.makeText(getActivity(), "Rejected third peer from joining conversation",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getActivity(), "Your peer has just connected", Toast.LENGTH_SHORT).show();
        //If first remote peer to join room, keep track of user and update text-view to display
        // details
        peerJoined = true;
        this.remotePeerId = peerId;
        if (userData instanceof String) {
            this.remotePeerName = (String) userData;
            Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, this.remotePeerName,
                    ROOM_NAME, MY_USER_NAME);
        }
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        Log.d(TAG, "onRemotePeerUserDataReceive " + remotePeerId);
    }

    @Override
    public void onOpenDataConnection(String s) {
        Log.d(TAG, "onOpenDataConnection");
    }

    /**
     * @param fileName String to be used as the name of the file to be created.
     * @return File to be transferred from default directory (Pictures directory).
     */
    private File getFileToTransfer(String fileName) {
        File path = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(path, fileName);
    }

    /**
     * @return Location to save the downloaded file on the file system
     */
    private String getDownloadedFilePath() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return path.getAbsolutePath() + File.separator + fileNameDownloaded;
    }


    public void onRemotePeerLeave(String peerId, String message) {
        Toast.makeText(parentActivity, "Your peer has left the room", Toast.LENGTH_SHORT).show();
        // Reset peerId
        peerJoined = false;
        this.remotePeerId = null;
        this.remotePeerName = null;
        // Update textview to display room's status
        Utils.setRoomDetails(connected, peerJoined, tvRoomDetails, this.remotePeerName, ROOM_NAME,
                MY_USER_NAME);
    }

}
