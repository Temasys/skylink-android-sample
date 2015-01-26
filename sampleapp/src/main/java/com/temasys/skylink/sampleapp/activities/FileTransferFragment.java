package com.temasys.skylink.sampleapp.activities;

import android.graphics.Point;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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

import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection;

/**
 * Created by lavanyasudharsanam on 20/1/15.
 */
public class FileTransferFragment extends Fragment implements SkyLinkConnection.LifeCycleDelegate, SkyLinkConnection.FileTransferDelegate, SkyLinkConnection.RemotePeerDelegate {
    private static final String TAG = FileTransferFragment.class.getCanonicalName();
    final String userName = "userFileTransfer";
    LinearLayout parentFragment;
    SkyLinkConnection skyLinkConnection;
    String peerId;
    Button sendFile;
    String fileName = "demofile.png";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_file_transfer, container, false);
//        myAudioGLSV = (GLSurfaceView) rootView.findViewById(R.id.myAudio);
//        peerVideoGSLV = (GLSurfaceView) rootView.findViewById(R.id.peerVideo);
        parentFragment = (LinearLayout) rootView.findViewById(R.id.ll_file_transfer);
        sendFile = (Button) rootView.findViewById(R.id.button2);
        //to create a dummy file to transfer
        createExternalStoragePrivatePicture();
        sendFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File path = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                skyLinkConnection.sendFileTransferRequest(peerId, fileName, path.getAbsolutePath() + "/" + fileName);
            }
        });
        return rootView;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        skyLinkConnection = new SkyLinkConnection(getString(R.string.app_key),
                getString(R.string.app_secret), getSkylinkConfig(), this.getActivity());

        Log.d(TAG, " lo " + this.getActivity());
        skyLinkConnection.setLifeCycleDelegate(this);
        skyLinkConnection.setFileTransferDelegate(this);
        skyLinkConnection.setRemotePeerDelegate(this);
        try {
            skyLinkConnection.connectToRoom("room", userName, new Date(), 200);
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private SkyLinkConnection.SkyLinkConfig getSkylinkConfig() {
        SkyLinkConnection.SkyLinkConfig config = new SkyLinkConnection.SkyLinkConfig();
        config.setHasAudio(true);
        config.setHasVideo(true);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(60);
        return config;
    }

/***
 * Lifecycle delegate
 */

    /**
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        // TODO Auto-generated method stub
        if (isSuccess)
            Log.d(TAG, "Skylink Connected");
        else
            Log.d(TAG, "Skylink Failed");
    }

    @Override
    public void onGetUserMedia(GLSurfaceView videoView, Point size) {
        // TODO Auto-generated method stub
        Log.d(TAG, videoView + "received view");

    }

    @Override
    public void onWarning(String message) {
        // TODO Auto-generated method stub
        Log.d(TAG, message + "warning");

    }

    @Override
    public void onDisconnect(String message) {
        // TODO Auto-generated method stub

        Log.d(TAG, message + " disconnected");
    }

    @Override
    public void onReceiveLog(String message) {
        // TODO Auto-generated method stub
        Log.d(TAG, message + " on receive log");
    }

    /**
     * File Transfer Callbacks
     */

    @Override
    public void onRequest(String peerId, String fileName, boolean isPrivate) {
        Toast.makeText(getActivity(), "Received a file request", Toast.LENGTH_LONG).show();

        String path = Environment.DIRECTORY_DOCUMENTS;
        skyLinkConnection.acceptFileTransferRequest(peerId, true, path);
    }

    @Override
    public void onPermission(String peerId, String fileName, boolean isPermitted) {
        if (isPermitted) {
            Toast.makeText(getActivity(), "Sending file", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Sorry no permission", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDrop(String peerId, String fileName, String message,
                       boolean isExplicit) {
        Toast.makeText(getActivity(), "You drop file", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onComplete(String peerId, String fileName, boolean isSending) {
        Toast.makeText(getActivity(), "You got file - sending?" + isSending, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProgress(String peerId, String fileName, double percentage,
                           boolean isSending) {
        Toast.makeText(getActivity(), "You got file", Toast.LENGTH_SHORT).show();
    }


    /**
     * Remote Peer Callbacks
     */

    @Override
    public void onPeerJoin(String peerId, Object userData) {
        Toast.makeText(getActivity(), "Your peer has just connected", Toast.LENGTH_SHORT).show();
        this.peerId = peerId;
    }

    @Override
    public void onGetPeerMedia(String peerId, GLSurfaceView videoView,
                               Point size) {
    }

    @Override
    public void onUserData(String peerId, Object userData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPeerLeave(String peerId, String message) {
        Toast.makeText(getActivity(), "Peer go bye bye", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onOpenDataConnection(String peerId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDetach() {
        super.onDetach();
        skyLinkConnection.disconnect();
    }


    void createExternalStoragePrivatePicture() {
        // Create a path where we will place our picture in our own private
        // pictures directory.  Note that we don't really need to place a
        // picture in DIRECTORY_PICTURES, since the media scanner will see
        // all media in these directories; this may be useful with other
        // media types such as DIRECTORY_MUSIC however to help it classify
        // your media for display to the user.
        File path = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(path, fileName);

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
}
