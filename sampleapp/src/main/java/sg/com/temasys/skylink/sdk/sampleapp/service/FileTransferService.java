package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.HashMap;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkError;
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.filetransfer.FileTransferContract;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for communicating with SkylinkSDK
 */

public class FileTransferService extends SkylinkCommonService implements FileTransferContract.Service {

    private final String TAG = FileTransferService.class.getName();

    public FileTransferService(Context context) {
        super(context);
        initializeSkylinkConnection(Constants.CONFIG_TYPE.FILE);
    }

    @Override
    public void setPresenter(FileTransferContract.Presenter presenter) {
        this.presenter = (BasePresenter) presenter;
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
        if (skylinkConnection == null)
            return;

        // Send request to peer requesting permission for file transfer
        skylinkConnection.sendFileTransfer(file.getAbsolutePath(), file.getName(), remotePeerId,
                new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to sendFileTransfer as " + contextDescription);
                    }
                });
    }

    /**
     * Call this method to accept or reject the file share request from a remote peer.
     *
     * @param remotePeerId       The id of the remote peer that requested to share with us a file.
     * @param downloadedFilePath The absolute path of the file where we want it to be saved.
     * @param isPermitted        Whether permission was granted for the file share to proceed.
     */
    public void sendFileTransferPermissionResponse(String remotePeerId, String downloadedFilePath, boolean isPermitted) {
        if (skylinkConnection == null)
            return;

        if (isPermitted) {
            skylinkConnection.acceptFileTransfer(downloadedFilePath, remotePeerId, new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to acceptFileTransfer as " + contextDescription);
                }
            });
        } else {
            skylinkConnection.rejectFileTransfer(remotePeerId, new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to sendFileTransferPermissionResponse as "
                            + contextDescription);
                }
            });
        }
    }

    /**
     * Sets the specified listeners for file transfer function
     * File transfer function needs to implement LifeCycleListener, RemotePeerListener, OsListener,
     * FileTransferListener
     */
    @Override
    public void setSkylinkListeners() {
        if (skylinkConnection != null) {
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setRemotePeerListener(this);
            skylinkConnection.setOsListener(this);
            skylinkConnection.setFileTransferListener(this);
        }
    }

    /**
     * Get the config for file transfer function
     * User can custom file transfer config by using SkylinkConfig
     */
    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // Set some common configs base on the default setting on the setting page
        Utils.skylinkConfigCommonOptions(skylinkConfig);

        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);

        skylinkConfig.setSkylinkRoomSize(SkylinkConfig.SkylinkRoomSize.LARGE);

        int maxRemotePeer = Utils.getDefaultMaxPeerInNoMediaRoomConfig();
        skylinkConfig.setMaxRemotePeersConnected(maxRemotePeer, SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);

        skylinkConfig.setFileTransfer(true);
        skylinkConfig.setTimeout(SkylinkConfig.SkylinkAction.FILE_SEND_REQUEST, Constants.TIME_OUT_FILE_SEND_REQUEST);

        return skylinkConfig;
    }

    /**
     * Get the info of a peer in specific index
     */
    public SkylinkPeer getPeerByIndex(int index) {
        return mPeersList.get(index);
    }

    public void disposeLocalMedia() {
        clearInstance();
    }
}
