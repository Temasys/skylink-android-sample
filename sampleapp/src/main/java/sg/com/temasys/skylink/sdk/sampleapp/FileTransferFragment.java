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
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;

/**
 * Created by lavanyasudharsanam on 20/1/15.
 */
public class FileTransferFragment extends Fragment implements LifeCycleListener, FileTransferListener, RemotePeerListener {

    private static final String TAG = FileTransferFragment.class.getCanonicalName();
    public static final String ROOM_NAME = "fileTransferRoom";
    public static final String MY_USER_NAME = "fileTransferUser";
    public static final String EXTERNAL_STORAGE = "ExternalStorage";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private TextView tvRoomDetails;
    private EditText etSenderFilePath;
    private TextView tvFileTransferDetails;
    private ImageView ivFilePreview;
    private SkylinkConnection skylinkConnection;
    private String peerId;
    private Button sendFile;
    private String fileName = "demofile.png";
    private String peerName;
    private boolean connected;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_file_transfer, container, false);
        sendFile = (Button) rootView.findViewById(R.id.button2);
        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_room_details);
        etSenderFilePath = (EditText) rootView.findViewById(R.id.et_file_path);
        ivFilePreview = (ImageView) rootView.findViewById(R.id.iv_file_preview);
        tvFileTransferDetails = (TextView) rootView.findViewById(R.id.tv_file_transfer_details);

        //save file raw/R.android.icon to the device's file system
        createExternalStoragePrivatePicture();

        String filePath = getFileToTransfer().getAbsolutePath();
        //show preview of file to transfer
        ivFilePreview.setImageURI(Uri.parse(filePath));
        etSenderFilePath.setText(filePath);

        sendFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if peer exists
                if (peerId == null) {
                    Toast.makeText(getActivity(), "There is no peer in the room to send a file to",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                //check if valid file
                String fName = etSenderFilePath.getText().toString();
                File file = new File(fName);
                if (file.exists()) {
                    ivFilePreview.setImageURI(Uri.parse(fName));
                    fileName = file.getName();
                } else {
                    Toast.makeText(getActivity(), "Please enter a valid filename", Toast.LENGTH_SHORT).show();
                    return;
                }

                //send request to peer requesting permission for file transfer
                try {
                    skylinkConnection.sendFileTransferPermissionRequest(peerId, fileName,
                            getFileToTransfer().getAbsolutePath());
                } catch (SkylinkException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });

        return rootView;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeSkylinkConnection();

        String apiKey = getString(R.string.app_key);
        String apiSecret = getString(R.string.app_secret);

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
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onDetach() {
        //close the connection when the fragment is detached, so the streams are not open.
        if (skylinkConnection != null && connected) {
            skylinkConnection.disconnectFromRoom();
            skylinkConnection.setLifeCycleListener(null);
            skylinkConnection.setFileTransferListener(null);
            skylinkConnection.setRemotePeerListener(null);
            connected = false;
        }
        super.onDetach();
    }


    private void initializeSkylinkConnection() {
        skylinkConnection = SkylinkConnection.getInstance();
        //the app_key and app_secret is obtained from the temasys developer console.
        skylinkConnection.init(getString(R.string.app_key), getSkylinkConfig(),
                this.getActivity().getApplicationContext());
        //set listeners to receive callbacks when events are triggered
        skylinkConnection.setLifeCycleListener(this);
        skylinkConnection.setRemotePeerListener(this);
        skylinkConnection.setFileTransferListener(this);
    }

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        //AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY, AUDIO_AND_VIDEO;
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        return config;
    }

/***
 * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's lifecycle
 */

    /**
     * Triggered if the connection is successful
     *
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        //update textview if connection is successful
        if (isSuccess) {
            Utils.setRoomDetails(false, tvRoomDetails, this.peerName, ROOM_NAME, MY_USER_NAME);
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
        Log.d(TAG, message + " disconnected");
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
        //send false to reject file transfer
        skylinkConnection.sendFileTransferPermissionResponse(peerId, getDownloadedFilePath(), true);
    }

    @Override
    public void onFileTransferPermissionResponse(String peerId, String fileName, boolean isPermitted) {
        if (isPermitted) {
            Toast.makeText(getActivity(), "Sending file", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Sorry, the remote peer has not granted permission for file transfer", Toast.LENGTH_SHORT).show();
        }
    }

    public void onFileTransferDrop(String remotePeerId, String fileName, String message,
                                   boolean isExplicit) {
        Toast.makeText(getActivity(), "The file transfer was dropped.\nReason : " + message, Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getActivity(), "A file has been received : " + fileName, Toast.LENGTH_SHORT).show();
        tvFileTransferDetails.setText("File Transfer Successful\n\nDestination : " + getDownloadedFilePath());
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
        if (this.peerId != null) {
            // If there is an existing peer, prevent new remotePeer from joining call.
            Toast.makeText(getActivity(), "Rejected third peer from joining conversation", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getActivity(), "Your peer has just connected", Toast.LENGTH_SHORT).show();
        //if first remote peer to join room, keep track of user and update text-view to display details
        this.peerId = peerId;
        if (userData instanceof String) {
            this.peerName = (String) userData;
            Utils.setRoomDetails(true, tvRoomDetails, this.peerName, ROOM_NAME, MY_USER_NAME);
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
     * @return default file to be transferred.
     */
    private File getFileToTransfer() {
        File path = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(path, fileName);
    }

    /**
     * @return location to save the downloaded file on the file system
     */
    private String getDownloadedFilePath() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return path.getAbsolutePath() + File.separator + "downloadFile.png";
    }


    public void onRemotePeerLeave(String peerId, String message) {
        Toast.makeText(getActivity(), "Your peer has left the room", Toast.LENGTH_SHORT).show();
        //reset peer id
        this.peerId = null;
        this.peerName = null;
        //update textview to display room's status
        Utils.setRoomDetails(false, tvRoomDetails, this.peerName, ROOM_NAME, MY_USER_NAME);
    }

    /**
     * Creates a dummy file from the apk's asset folder to the device's filepath so that there is a
     * default file to transfer
     */
    void createExternalStoragePrivatePicture() {
        // Create a path where we will place our picture in our own private
        // pictures directory.  Note that we don't really need to place a
        // picture in DIRECTORY_PICTURES, since the media scanner will see
        // all media in these directories; this may be useful with other
        // media types such as DIRECTORY_MUSIC however to help it classify
        // your media for display to the user.

        File file = getFileToTransfer();
        try {
            // Very simple code to copy a picture from the application's
            // resource into the external file.  Note that this code does
            // no error checking, and assumes the picture is small (does not
            // try to copy it in chunks).  Note that if external storage is
            // not currently mounted this will silently fail.
            InputStream is = getResources().openRawResource(R.raw.icon);
            OutputStream os = new FileOutputStream(file);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this.getActivity(),
                    new String[]{file.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i(EXTERNAL_STORAGE, "Scanned " + path + ":");
                            Log.i(EXTERNAL_STORAGE, "-> uri=" + uri);
                        }
                    });
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w(EXTERNAL_STORAGE, "Error writing " + file, e);
        }
    }

}
