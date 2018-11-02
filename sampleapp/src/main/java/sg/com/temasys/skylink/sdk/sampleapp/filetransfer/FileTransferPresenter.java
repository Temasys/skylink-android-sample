package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.FileTransferService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class FileTransferPresenter extends BasePresenter implements FileTransferContract.Presenter {

    private final String TAG = FileTransferPresenter.class.getName();

    private Context mContext;

    private FileTransferContract.View mFileTransferView;
    private FileTransferService mFileTransferService;

    //utils to process permission
    private PermissionUtils mPermissionUtils;

    //fileName to download
    private String fileNameDownloaded = "downloadFile.png";


    public FileTransferPresenter(Context context) {
        this.mContext = context;

        this.mFileTransferService = new FileTransferService(context);

        this.mFileTransferService.setPresenter(this);

        this.mPermissionUtils = new PermissionUtils();
    }

    public void setView(FileTransferContract.View view) {
        mFileTransferView = view;
        mFileTransferView.setPresenter(this);
    }

    /**
     * Triggered when View request data to display to the user when entering room | rotating screen
     * Try to connect to room when entering room
     * Try to update info when rotating screen
     */
    @Override
    public void onViewRequestConnectedLayout() {

        Log.d(TAG, "onViewLayoutRequested");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mFileTransferService.isConnectingOrConnected()) {

            //reset permission request states.
            mPermissionUtils.permQReset();

            //connect to room on Skylink connection
            mFileTransferService.connectToRoom(Constants.CONFIG_TYPE.FILE);

            //after connected to skylink SDK, UI will be updated later on AudioService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //if it already connected to room, then resume permission
            mPermissionUtils.permQResume(mContext, mFileTransferView.onPresenterRequestGetFragmentInstance());

            //update UI into connected state
            processUpdateUI();

            Log.d(TAG, "Try to update UI when changing configuration");
        }
    }

    @Override
    public void onViewRequestExit() {

        //process disconnect from room
        mFileTransferService.disconnectFromRoom();

    }

    @Override
    public void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag) {
        mPermissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, tag);
    }

    @Override
    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
        mPermissionUtils.onPermissionRequiredHandler(info, TAG, mContext, mFileTransferView.onPresenterRequestGetFragmentInstance());
    }

    /**
     * Sends a file to a Peer or all Peers in room.
     *
     * @param remotePeerId Peer to send to. Use null to send to all in room.
     * @param filePath
     */
    @Override
    public void onViewRequestSendFile(String remotePeerId, String filePath) {

        // Do not allow button actions if there are no remote Peers in the room.
        if (mFileTransferService.getTotalPeersInRoom() < 2) {
            String log = mContext.getString(R.string.warn_no_peer_message);
            toastLog(TAG, mContext, log);
            return;
        }

        if (remotePeerId == null) {
            // Select All Peers RadioButton if not already selected
            String remotePeer = mFileTransferView.onPresenterRequestGetPeerIdSelected();

            //force to select radio button peerAll
            if (remotePeer != null) {
                mFileTransferView.onPresenterRequestSetPeerAllSelected(true);
            }
        }

        //Check file valid
        File file = new File(filePath);

        if (file.isFile()) {

            mFileTransferView.onPresenterRequestDisplayFilePreview(Uri.parse(filePath));

        } else {
            String log = "Please enter a valid filename";
            toastLog(TAG, mContext, log);
            return;
        }

        mFileTransferService.sendFile(remotePeerId, file);
    }

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful)
            processUpdateUI();
    }

    @Override
    public void onServiceRequestRemotePeerJoin(SkylinkPeer newPeer) {
        //add new remote peer
        mFileTransferView.onPresenterRequestChangeUiRemotePeerJoin(newPeer);

        // Update textview to show room status when first remote peer has joined with self peer
        if (mFileTransferService.getTotalPeersInRoom() == 2) {
            processUpdateRoomDetails();
        }
    }

    @Override
    public void onServiceRequestRemotePeerLeave(String remotePeerId, int removeIndex) {
        // Remove remote peer
        mFileTransferView.onPresenterRequestChangeUiRemotePeerLeave(remotePeerId);

        // Update textview to show room status when last remote peer has left
        if (mFileTransferService.getTotalPeersInRoom() == 1) {
            processUpdateRoomDetails();
        }
    }

    @Override
    public void onServiceRequestFileTransferPermissionRequest(String remotePeerId, String fileName, boolean isPrivate) {

        String log = "Received a file request";
        toastLogLong(TAG, mContext, log);

        // Take note of download file name.
        if (!"".equals(fileName)) {
            fileNameDownloaded = fileName;
        }
        //Send false to reject file transfer
        mFileTransferService.sendFileTransferPermissionResponse(remotePeerId, processGetDownloadedFilePath(), true);

    }

    @Override
    public void onServiceRequestFileReceiveComplete(String remotePeerId, String fileName) {
        String log = "A file has been received : " + fileName;
        toastLog(TAG, mContext, log);

        String info = "File Transfer Successful\n\nDestination : " + processGetDownloadedFilePath();
        mFileTransferView.onPresenterRequestDisplayFileReveicedInfo(info);
    }

    private void processUpdateUI() {

        mFileTransferView.onPresenterRequestFillPeers(mFileTransferService.getPeersList());

        processUpdateRoomDetails();
    }

    private void processUpdateRoomDetails() {
        String strRoomDetails = processGetRoomDetails();
        mFileTransferView.onPresenterRequestUpdateUi(strRoomDetails);
    }

    private String processGetRoomDetails() {
        boolean isConnected = mFileTransferService.isConnectingOrConnected();
        String roomName = mFileTransferService.getRoomName(Config.ROOM_NAME_FILE);
        String userName = mFileTransferService.getUserName(null, Config.USER_NAME_FILE);

        boolean isPeerJoined = mFileTransferService.isPeerJoin();

        String roomDetails = "You are not connected to any room";

        if (isConnected) {
            roomDetails = "Now connected to Room named : " + roomName
                    + "\n\nYou are signed in as : " + userName + "\n";
            if (isPeerJoined) {
                roomDetails += "\nPeer(s) are in the room";
            } else {
                roomDetails += "\nYou are alone in this room";
            }
        }

        return roomDetails;
    }

    /**
     * @return Location to save the downloaded file on the file system
     */
    private String processGetDownloadedFilePath() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return path.getAbsolutePath() + File.separator + fileNameDownloaded;
    }

}
