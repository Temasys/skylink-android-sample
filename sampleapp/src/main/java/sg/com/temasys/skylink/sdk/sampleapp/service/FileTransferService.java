package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.util.Log;

import java.io.File;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.filetransfer.FileTransferContract;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class FileTransferService extends SkylinkCommonService implements FileTransferContract.Service {

    private final String TAG = FileTransferService.class.getName();

    public FileTransferService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(FileTransferContract.Presenter presenter) {
        mPresenter = (BasePresenter) presenter;
    }

    /**
     * Sends request(s) to share file with a specific remote peer or to all remote peers in a
     * direct peer to peer manner in the same room.
     * Only 1 file may be sent to the same Peer at the same time.
     * Sending and receiving concurrently with a non-Mobile (e.g. Web or C++) Peer is not supported.
     *
     * @param remotePeerId The id of the remote peer to send the file to. Use 'null' if the file is
     *                     to be sent to all our remote peers in the room.
     * @param file         The file that is to be shared.
     */
    public void sendFile(String remotePeerId, File file) {
        if (mSkylinkConnection == null)
            return;

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

    /**
     * Call this method to accept or reject the file share request from a remote peer.
     *
     * @param remotePeerId       The id of the remote peer that requested to share with us a file.
     * @param downloadedFilePath The absolute path of the file where we want it to be saved.
     * @param isPermitted        Whether permission was granted for the file share to proceed.
     */
    public void sendFileTransferPermissionResponse(String remotePeerId, String downloadedFilePath, boolean isPermitted) {
        if (mSkylinkConnection == null)
            return;

        try {
            mSkylinkConnection.sendFileTransferPermissionResponse(remotePeerId, downloadedFilePath, isPermitted);
        } catch (SkylinkException e) {
            String log = e.getMessage();
            toastLogLong(TAG, mContext, log);
        }
    }

    /**
     * Sets the specified listeners for file transfer function
     * File transfer function needs to implement LifeCycleListener, RemotePeerListener, OsListener,
     * FileTransferListener
     */
    @Override
    public void setSkylinkListeners() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.setLifeCycleListener(this);
            mSkylinkConnection.setRemotePeerListener(this);
            mSkylinkConnection.setOsListener(this);
            mSkylinkConnection.setFileTransferListener(this);
        }
    }

    /**
     * Get the config for file transfer function
     * User can custom file transfer config by using SkylinkConfig
     */
    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // FileTransfer config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setHasFileTransfer(true);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    /**
     * Get the info of a peer in specific index
     */
    public SkylinkPeer getPeerByIndex(int index) {
        return mPeersList.get(index);
    }
}
