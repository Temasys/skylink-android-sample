package com.temasys.skylink.sampleapp.activities;

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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SignatureException;
import java.util.Date;

import sg.com.temasys.skylink.sdk.config.SkyLinkConfig;
import sg.com.temasys.skylink.sdk.listener.FileTransferListener;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection;

/**
 * Created by lavanyasudharsanam on 20/1/15.
 */
public class FileTransferFragment extends Fragment implements LifeCycleListener, FileTransferListener, RemotePeerListener {
    private static final String TAG = FileTransferFragment.class.getCanonicalName();
    final String myName = "userFileTransfer";
    LinearLayout parentFragment;
    TextView tvRoomDetails;
    EditText etSenderFilePath;
    TextView tvFileTransferDetails;
    ImageView ivFilePreview;
    SkyLinkConnection skyLinkConnection;
    String peerId;
    Button sendFile;
    String fileName = "demofile.png";
    private String peerName;
    private final String roomName = "room";
    Toast progressToast = null;
    private Button btnLoadImage;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_file_transfer, container, false);
        parentFragment = (LinearLayout) rootView.findViewById(R.id.ll_file_transfer);
        sendFile = (Button) rootView.findViewById(R.id.button2);
        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_room_details);
        etSenderFilePath = (EditText)rootView.findViewById(R.id.et_file_path);
        ivFilePreview = (ImageView) rootView.findViewById(R.id.iv_file_preview);
        tvFileTransferDetails = (TextView) rootView.findViewById(R.id.tv_file_transfer_details);

        createExternalStoragePrivatePicture();

        String filePath =  getFileToTransfer().getAbsolutePath();
        ivFilePreview.setImageURI(Uri.parse(filePath));
        etSenderFilePath.setText(filePath);

        sendFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(peerId==null){
                    Toast.makeText(getActivity(),"There is no peer in the room to send a file to",Toast.LENGTH_SHORT).show();
                    return;
                }

                String fName = etSenderFilePath.getText().toString();
                File file = new File(fName);
                if(file.exists()) {
                    ivFilePreview.setImageURI(Uri.parse(fName));
                    fileName = file.getName();
                }
                else {
                    Toast.makeText(getActivity(), "Please enter a valid filename",Toast.LENGTH_SHORT ).show();
                    return;
                }

                skyLinkConnection.sendFileTransferPermissionRequest(peerId, fileName, getFileToTransfer().getAbsolutePath());
            }
        });

        return rootView;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeSkylinkConnection();

        try {
            skyLinkConnection.connectToRoom(roomName, myName, new Date(), 200);
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        skyLinkConnection.disconnectFromRoom();
        skyLinkConnection.setLifeCycleListener(this);
        skyLinkConnection.setFileTransferListener(this);
        skyLinkConnection.setRemotePeerListener(this);
    }


    private void initializeSkylinkConnection() {
        skyLinkConnection = SkyLinkConnection.getInstance();
        skyLinkConnection.init(getString(R.string.app_key),
                getString(R.string.app_secret), getSkylinkConfig(), this.getActivity().getApplicationContext());
        skyLinkConnection.setLifeCycleListener(this);
        skyLinkConnection.setRemotePeerListener(this);
        skyLinkConnection.setFileTransferListener(this);
    }

    private void setRoomDetails(boolean isPeerInRoom){

        String roomDetails = "Room Name : " + roomName + "\nYou are signed in as : " + myName + "\n";
        if(isPeerInRoom)
            roomDetails += "Peer Name : " + this.peerName;
        else
            roomDetails += "You are alone in this room";

        tvRoomDetails.setText(roomDetails);

    }

    private SkyLinkConfig getSkylinkConfig() {
        SkyLinkConfig config = new SkyLinkConfig();
        config.setAudioVideoSendConfig(SkyLinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(60);
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
        if (isSuccess){
            setRoomDetails(false);
        }
        else {
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
     * File Transfer Callbacks
     */

    @Override
    public void onFileTransferPermissionRequest(String peerId, String fileName, boolean isPrivate) {
        Toast.makeText(getActivity(), "Received a file request", Toast.LENGTH_LONG).show();
        String path = Environment.DIRECTORY_DOCUMENTS;
        skyLinkConnection.sendFileTransferPermissionResponse(peerId, path, true);
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
        Toast.makeText(getActivity(), "The file transfer was dropped.\nReason : " + message , Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFileSendComplete(String remotePeerId, String fileName) {
        Toast.makeText(getActivity(), "Your file has been sent" , Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onFileSendProgress(String remotePeerId, String fileName, double percentage) {
        Toast.makeText(getActivity(), "Uploading... " + percentage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFileReceiveComplete(String remotePeerId, String fileName) {
        Toast.makeText(getActivity(), "A file has been received : " + fileName, Toast.LENGTH_SHORT).show();
        tvFileTransferDetails.setText("File Transfer Successful\n\nDestination : " +getDownloadedFilePath());
    }

    @Override
    public void onFileReceiveProgress(String remotePeerId, String fileName, double percentage) {
        Toast.makeText(getActivity(), "Downloading... " + percentage, Toast.LENGTH_SHORT).show();
    }


    /**
     * Remote Peer Callbacks
     */

    public void onRemotePeerJoin(String peerId, Object userData) {
        if(this.peerId!=null) { //means there is an existing peer
            Toast.makeText(getActivity(), "Rejected third peer from joining conversation",Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getActivity(), "Your peer has just connected", Toast.LENGTH_SHORT).show();
        this.peerId = peerId;
        if(userData instanceof String) {
            this.peerName = (String) userData;
            setRoomDetails(true);
        }
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        Log.d(TAG, "onRemotePeerUserDataReceive " + remotePeerId);
    }

    @Override
    public void onOpenDataConnection(String s) {
    }

    File getFileToTransfer(){
        File path = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(path, fileName);
        return file;
    }


    public void onRemotePeerLeave(String peerId, String message) {
        Toast.makeText(getActivity(), "Your peer has left the room", Toast.LENGTH_SHORT).show();
        this.peerId = null;
        this.peerName = null;
        setRoomDetails(false);    }

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
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + file, e);
        }
    }

    void deleteExternalStoragePrivatePicture() {
        // Create a path where we will place our picture in the user's
        // public pictures directory and delete the file.  If external
        // storage is not currently mounted this will fail.
        File path = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (path != null) {
            File file = new File(path, fileName);
            file.delete();
        }
    }

    boolean hasExternalStoragePrivatePicture() {
        // Create a path where we will place our picture in the user's
        // public pictures directory and check if the file exists.  If
        // external storage is not currently mounted this will think the
        // picture doesn't exist.
        File path = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (path != null) {
            File file = new File(path, fileName);
            return file.exists();
        }
        return false;
    }

    public String getDownloadedFilePath() {
        File path = getActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        return path.getAbsolutePath() + File.separator + "downloadFile.png";    }
}
