package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.util.Log;

import java.io.File;

import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.filetransfer.FileTransferContract;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class FileTransferService extends SDKService implements FileTransferContract.Service{

    private final String TAG = FileTransferService.class.getName();

    public FileTransferService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(FileTransferContract.Presenter presenter) {
        mFilePresenter = presenter;
    }

    @Override
    public void setTypeCall() {
        mTypeCall = Constants.CONFIG_TYPE.FILE;
    }

    public void sendFile(String remotePeerId, File file) {

        // Send request to peer requesting permission for file transfer
        try {
            mSkylinkConnection.sendFileTransferPermissionRequest(remotePeerId, file.getName(), file.getAbsolutePath());

            String peer = "";
            if (remotePeerId == null) {
                peer = "all Peers in room";
            } else {
                peer = "Peer " + remotePeerId;
            }
            String log = "Sending file to " + peer + ".";
            toastLog(TAG, mContext, log);
        } catch (SkylinkException e) {
            String log = e.getMessage();
            toastLogLong(TAG, mContext, log);
            Log.e(TAG, log, e);
        }
    }

    public void sendFileTransferPermissionResponse(String remotePeerId, String downloadedFilePath, boolean isPermitted) {
        try {
            mSkylinkConnection.sendFileTransferPermissionResponse(remotePeerId, downloadedFilePath, isPermitted);
        } catch (SkylinkException e) {
            String log = e.getMessage();
            toastLogLong(TAG, mContext, log);
        }
    }

}
