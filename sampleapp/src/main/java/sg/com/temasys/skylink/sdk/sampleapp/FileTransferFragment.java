package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

import sg.com.temasys.skylink.sdk.listener.FileTransferListener;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.OsListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;

import static sg.com.temasys.skylink.sdk.sampleapp.MainActivity.ARG_SECTION_NUMBER;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.getNumRemotePeers;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.getRoomRoomId;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.isConnectingOrConnected;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.permQReset;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.permQResume;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.toastLogLong;

/**
 * Created by lavanyasudharsanam on 20/1/15.
 */
public class FileTransferFragment extends MultiPartyFragment
        implements LifeCycleListener, FileTransferListener, OsListener, RemotePeerListener {

    private String ROOM_NAME;
    private String MY_USER_NAME;

    public static final String EXTERNAL_STORAGE = "ExternalStorage";
    private static final String TAG = FileTransferFragment.class.getCanonicalName();

    // Constants for configuration change
    private static final String BUNDLE_IS_PEER_JOINED = "peerJoined";
    private static SkylinkConnection skylinkConnection;
    private static SkylinkConfig skylinkConfig;
    private TextView tvRoomDetails;
    private EditText etSenderFilePath;
    private TextView tvFileTransferDetails;
    private ImageView ivFilePreview;
    private Button sendFilePrivate;
    private Button sendFileGroup;
    private String fileNamePrivate = "FileTransferPrivate.png";
    private String fileNameGroup = "FileTransferGroup.png";
    private String fileNameDownloaded = "downloadFile.png";
    private boolean peerJoined;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ROOM_NAME = Config.ROOM_NAME_FILE;
        MY_USER_NAME = Config.USER_NAME_FILE;

        View rootView = inflater.inflate(R.layout.fragment_file_transfer, container, false);

        // [MultiParty]
        peerRadioGroup = (RadioGroup) rootView.findViewById(R.id.radio_grp_peers);
        peerAll = (RadioButton) rootView.findViewById(R.id.radio_btn_peer_all);
        peer1 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer1);
        peer2 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer2);
        peer3 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer3);
        peer4 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer4);

        sendFilePrivate = (Button) rootView.findViewById(R.id.btn_send_file_pte);
        sendFileGroup = (Button) rootView.findViewById(R.id.btn_send_file_grp);
        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_room_details);
        etSenderFilePath = (EditText) rootView.findViewById(R.id.et_file_path);

        // Manual selection of file to send is now enabled.
        // After selecting Peer(s) to send to, click on file path (etSenderFilePath)
        // and enter desired file path.
        etSenderFilePath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create Dialog to change file path.
                // File path will be validated to be a file before change is accepted.
                AlertDialog.Builder changePathDialogBuilder =
                        new AlertDialog.Builder(getContext());
                changePathDialogBuilder.setTitle("Set the path to the file to be transferred.");

                // Create EditText for file path in Dialog.
                final EditText filePathEdtTxt = new EditText(getContext());
                filePathEdtTxt.setText(etSenderFilePath.getText());
                filePathEdtTxt.setMovementMethod(LinkMovementMethod.getInstance());
                changePathDialogBuilder.setView(filePathEdtTxt);

                // Create a Positive button but this will be overridden later.
                changePathDialogBuilder.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing here as this will be overridden later.
                            }
                        });

                // Negative button to cancel setting of file path.
                changePathDialogBuilder.setNegativeButton("Cancel", null);
                final AlertDialog changePathDialog = changePathDialogBuilder.create();
                changePathDialog.show();

                // Override the handler to prevent changePathDialog from auto closing
                // after clicking button.
                // Note: Has to be after show() is called.
                changePathDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Close changePathDialog only if file exists at path provided.
                                final String filePathNew = filePathEdtTxt.getText().toString();
                                File file = new File(filePathNew);
                                if (file.isFile()) {
                                    // Set file to be transferred to this path.
                                    prepFile(filePathNew);
                                    changePathDialog.dismiss();
                                } else {
                                    // Else changePathDialog stays open.
                                    String log = "[SA] File does not exist at newly provided path. "
                                            + "Please make corrections or cancel operation.";
                                    toastLog(TAG, context, log);
                                    Log.e(TAG, log);
                                }
                            }
                        });
            }
        });

        ivFilePreview = (ImageView) rootView.findViewById(R.id.iv_file_preview);
        tvFileTransferDetails = (TextView) rootView.findViewById(R.id.tv_file_transfer_details);

        // Prepare default file for transfer and set UI.
        prepFile(getFileToTransfer(fileNamePrivate).getAbsolutePath());

        // [MultiParty]
        // Initialise peerList if required.
        if (peerList == null) {
            peerList = new ArrayList<Pair<String, String>>();
        }

        // Check if it was an orientation change
        if (savedInstanceState != null) {
            // Resume previous permission request, if any.
            permQResume(getContext(), this, skylinkConnection);

            // Set listeners to receive callbacks when events are triggered
            setListeners();

            if (isConnectingOrConnected()) {
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
            // This is the start of this sample, reset permission request states.
            permQReset();

            // [MultiParty]
            // Just set room details
            setRoomDetails();
        }

        // Copy files raw/R.raw.icon and raw/R.raw.icon_group to the device's file system
        createExternalStoragePrivatePicture();

        // Try to connect to room if not yet connected.
        if (!isConnectingOrConnected()) {
            connectToRoom();
        }


        // Set file to send based on selected Peer.
        peerRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radio_btn_peer_all) {
                    // Prepare group file for transfer.
                    prepFile(getFileToTransfer(fileNameGroup).getAbsolutePath());
                } else {
                    // Prepare private file for transfer.
                    prepFile(getFileToTransfer(fileNamePrivate).getAbsolutePath());
                }
            }
        });

        // Send file to specific Peer.
        sendFilePrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // [MultiParty]
                String remotePeerId = getPeerIdSelectedWithWarning();
                // Do not allow button actions if there are no Peers in the room.
                if ("".equals(remotePeerId)) {
                    return;
                }

                sendFile(remotePeerId);
            }
        });

        // Send file to all Peers in room, i.e. via public (AKA group) message.
        sendFileGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // [MultiParty]
                // Do not allow button actions if there are no Peers in the room.
                if (getPeerNum() == 0) {
                    String log = getString(R.string.warn_no_peer_message);
                    toastLog(TAG, context, log);
                    return;
                }
                // Select All Peers RadioButton if not already selected
                String remotePeerId = getPeerIdSelected();
                if (remotePeerId != null) {
                    peerAll.setChecked(true);
                    // Prepare group file for transfer.
                    prepFile(getFileToTransfer(fileNameGroup).getAbsolutePath());
                }
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
        // Check if valid file
        String filePath = etSenderFilePath.getText().toString();
        File file = new File(filePath);
        String fileName;
        if (file.isFile()) {
            ivFilePreview.setImageURI(Uri.parse(filePath));
            fileName = file.getName();
        } else {
            String log = "Please enter a valid filename";
            toastLog(TAG, context, log);
            return;
        }

        // Send request to peer requesting permission for file transfer
        try {
            skylinkConnection.sendFileTransferPermissionRequest(
                    tid, fileName, file.getAbsolutePath());
            String peer = "";
            if (tid == null) {
                peer = "all Peers in room";
            } else {
                peer = "Peer " + tid;
            }
            String log = "Sending file to " + peer + ".";
            toastLog(TAG, context, log);
        } catch (SkylinkException e) {
            String log = e.getMessage();
            toastLogLong(TAG, context, log);
            Log.e(TAG, log, e);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //update actionbar title
        ((MainActivity) context).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
        this.context = context;
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
        if (!((MainActivity) context).isChangingConfigurations() && skylinkConnection != null
                && isConnectingOrConnected()) {
            skylinkConnection.disconnectFromRoom();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        Utils.onRequestPermissionsResultHandler(
                requestCode, permissions, grantResults, TAG, skylinkConnection);
    }

    //----------------------------------------------------------------------------------------------
    // Skylink helper methods
    //----------------------------------------------------------------------------------------------

    private SkylinkConfig getSkylinkConfig() {
        if (skylinkConfig != null) {
            return skylinkConfig;
        }

        skylinkConfig = new SkylinkConfig();
        // AudioVideo config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setHasFileTransfer(true);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    private void connectToRoom() {
        // Initialize the skylink connection
        initializeSkylinkConnection();

        // Create the Skylink connection string.
        // In production, the connection string should be generated by an external entity
        // (such as a secure App server that has the Skylink App Key secret), and sent to the App.
        // This is to avoid keeping the App Key secret within the application, for better security.
        String skylinkConnectionString = Utils.getSkylinkConnectionString(
                ROOM_NAME, new Date(), SkylinkConnection.DEFAULT_DURATION);

        // The skylinkConnectionString should not be logged in production,
        // as it contains potentially sensitive information like the Skylink App Key ID.

        skylinkConnection.connectToRoom(skylinkConnectionString, MY_USER_NAME);
    }

    private void initializeSkylinkConnection() {
        skylinkConnection = SkylinkConnection.getInstance();
        //the app_key and app_secret is obtained from the temasys developer console.
        skylinkConnection.init(Config.getAppKey(), getSkylinkConfig(), context);

        //set listeners to receive callbacks when events are triggered
        setListeners();
    }

    /**
     * Set listeners to receive callbacks when events are triggered.
     * SkylinkConnection instance must not be null or listeners cannot be set.
     * Do not set before {@link SkylinkConnection#init} as that will remove all existing Listeners.
     *
     * @return false if listeners could not be set.
     */
    private boolean setListeners() {
        if (skylinkConnection != null) {
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setRemotePeerListener(this);
            skylinkConnection.setOsListener(this);
            skylinkConnection.setFileTransferListener(this);
            return true;
        } else {
            return false;
        }
    }

    //----------------------------------------------------------------------------------------------
    // UI helper methods
    //----------------------------------------------------------------------------------------------

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
            MediaScannerConnection.scanFile(context,
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

    //----------------------------------------------------------------------------------------------
    // UI helper methods
    //----------------------------------------------------------------------------------------------

    /**
     * Change certain UI elements once isConnected() to room or when Peer(s) join or leave.
     */
    private void onConnectUIChange() {
        // [MultiParty]
        setRoomDetails();
        fillPeerRadioBtn();
    }

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

    /**
     * Set the room details on UI.
     */
    void setRoomDetails() {
        Utils.setRoomDetailsMulti(isConnectingOrConnected(), peerJoined, tvRoomDetails,
                getRoomRoomId(skylinkConnection, ROOM_NAME),
                Utils.getDisplayName(skylinkConnection, MY_USER_NAME, null));
    }

    //----------------------------------------------------------------------------------------------
    // Skylink Listeners
    //----------------------------------------------------------------------------------------------

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's
     * lifecycle
     */

    /**
     * Triggered if the connection is successful
     *
     * @param isSuccessful
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        //Update textview if connection is successful
        if (isSuccessful) {
            String log = "Connected to room " + ROOM_NAME + " (" + skylinkConnection.getRoomId() +
                    ") as " + skylinkConnection.getPeerId() + " (" + MY_USER_NAME + ").";
            toastLogLong(TAG, context, log);
            // [MultiParty]
            // Set the appropriate UI if already isConnected().
            onConnectUIChange();
        } else {
            String log = "Skylink failed to connect!\nReason : " + message;
            toastLogLong(TAG, context, log);
            setRoomDetails();
        }
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        // [MultiParty]
        // Reset peerList
        peerList.clear();
        // Set the appropriate UI after disconnecting.
        onConnectUIChange();
        String log = "[onDisconnect] ";
        if (errorCode == Errors.DISCONNECT_FROM_ROOM) {
            log += "We have successfully disconnected from the room.";
        } else if (errorCode == Errors.DISCONNECT_UNEXPECTED_ERROR) {
            log += "WARNING! We have been unexpectedly disconnected from the room!";
        }
        log += " Server message: " + message;
        toastLogLong(TAG, context, log);
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        String log = "[SA] Peer " + remotePeerId + " changed Room locked status to "
                + lockStatus + ".";
        toastLog(TAG, context, log);
    }

    @Override
    public void onReceiveLog(int infoCode, String message) {
        Utils.handleSkylinkReceiveLog(infoCode, message, context, TAG);
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Utils.handleSkylinkWarning(errorCode, message, context, TAG);
    }

    /**
     * File Transfer Listener Callbacks - triggered during events that happen during file transfer
     * between peers
     */

    @Override
    public void onFileTransferPermissionRequest(String peerId, String fileName, boolean isPrivate) {
        String log = "Received a file request";
        toastLogLong(TAG, context, log);
        // Take note of download file name.
        if (!"".equals(fileName)) {
            fileNameDownloaded = fileName;
        }
        //Send false to reject file transfer
        try {
            skylinkConnection.sendFileTransferPermissionResponse(peerId, getDownloadedFilePath(), true);
        } catch (SkylinkException e) {
            log = e.getMessage();
            toastLogLong(TAG, context, log);
        }
    }

    @Override
    public void onFileTransferPermissionResponse(String peerId, String fileName, boolean
            isPermitted) {
        if (isPermitted) {
            String log = "Sending file";
            toastLog(TAG, context, log);
        } else {
            String log = "Sorry, the remote peer has not granted permission for file transfer";
            toastLog(TAG, context, log);
        }
    }

    public void onFileTransferDrop(String remotePeerId, String fileName, String message,
                                   boolean isExplicit) {
        String log = "The file transfer was dropped.\nReason : " + message;
        toastLogLong(TAG, context, log);
    }

    @Override
    public void onFileSendComplete(String remotePeerId, String fileName) {
        String log = "Your file has been sent";
        toastLog(TAG, context, log);
    }

    @Override
    public void onFileSendProgress(String remotePeerId, String fileName, double percentage) {
        String log = "Uploading... " + percentage;
        toastLog(TAG, context, log);
    }

    @Override
    public void onFileReceiveComplete(String remotePeerId, String fileName) {
        String log = "A file has been received : " + fileName;
        toastLog(TAG, context, log);
        tvFileTransferDetails
                .setText("File Transfer Successful\n\nDestination : " + getDownloadedFilePath());
    }

    @Override
    public void onFileReceiveProgress(String remotePeerId, String fileName, double percentage) {
        String log = "Downloading... " + percentage;
        toastLog(TAG, context, log);
    }


    /**
     * OsListener Callbacks - triggered by Android OS related events.
     */
    @Override
    public void onPermissionRequired(
            final String[] permissions, final int requestCode, final int infoCode) {
        Utils.onPermissionRequiredHandler(permissions, requestCode, infoCode, TAG, getContext(), this, skylinkConnection);
    }

    @Override
    public void onPermissionGranted(String[] permissions, int requestCode, int infoCode) {
        Utils.onPermissionGrantedHandler(permissions, infoCode, TAG);
    }

    @Override
    public void onPermissionDenied(String[] permissions, int requestCode, int infoCode) {
        Utils.onPermissionDeniedHandler(infoCode, getContext(), TAG);
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

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
            setRoomDetails();
        }
        String log = "Your Peer " + Utils.getPeerIdNick(remotePeerId) + " connected.";
        toastLog(TAG, context, log);
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
            setRoomDetails();
        }

        int numRemotePeers = getNumRemotePeers();
        String log = "Your Peer " + Utils.getPeerIdNick(remotePeerId, userInfo) + " left: " +
                message + ". " + numRemotePeers + " remote Peer(s) left in the room.";
        toastLog(TAG, context, log);
    }

    @Override
    public void onRemotePeerConnectionRefreshed(String remotePeerId, Object userData, boolean hasDataChannel, boolean wasIceRestarted) {
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

        toastLog(TAG, context, log);
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        // If Peer has no userData, use an empty string for nick.
        String nick = "";
        if (userData != null) {
            nick = userData.toString();
        }
        String log = "[SA][onRemotePeerUserDataReceive] Peer " + Utils.getPeerIdNick(remotePeerId) +
                ":\n" + nick;
        toastLog(TAG, context, log);
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
        File path = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(path, fileName);
    }

    /**
     * @return Location to save the downloaded file on the file system
     */
    private String getDownloadedFilePath() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return path.getAbsolutePath() + File.separator + fileNameDownloaded;
    }

}