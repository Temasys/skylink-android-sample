package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import sg.com.temasys.skylink.sdk.sampleapp.configuration.Config;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.service.FileTransferService;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class FileTransferPresenter implements FileTransferContract.Presenter{

    private final String TAG = FileTransferPresenter.class.getName();

    private Context mContext;

    private FileTransferContract.View mFileTransferView;
    private FileTransferService mFileTransferService;

    //utils to process permission
    private PermissionUtils mPermissionUtils;

    //fileName to download
    private String fileNameDownloaded = "downloadFile.png";


    public FileTransferPresenter(FileTransferContract.View fileTransferView, Context context) {
        this.mContext = context;

        this.mFileTransferView = fileTransferView;
        this.mFileTransferService = new FileTransferService(context);

        this.mFileTransferView.setPresenter(this);
        this.mFileTransferService.setPresenter(this);

        this.mFileTransferService.setTypeCall();

        this.mPermissionUtils = new PermissionUtils();
    }

    /**
     * Triggered when View request data to display to the user when entering room | rotating screen
     * Try to connect to room when entering room
     * Try to update info when rotating screen
     */
    @Override
    public void onViewLayoutRequested() {

        Log.d(TAG, "onViewLayoutRequested");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mFileTransferService.isConnectingOrConnected()) {

            //reset permission request states.
            mPermissionUtils.permQReset();

            //connect to room on Skylink connection
            mFileTransferService.connectToRoom();

            //after connected to skylink SDK, UI will be updated later on AudioService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //if it already connected to room, then resume permission
            mPermissionUtils.permQResume(mContext, mFileTransferView.onGetFragment());

            //update UI into connected state
            updateUI();

            Log.d(TAG, "Try to update UI when changing configuration");
        }
    }

    @Override
    public void onConnect(boolean isSuccessful) {
        updateUI();
    }

    @Override
    public void onDisconnect() {
        //do nothing
    }

    @Override
    public void onViewExit() {

        //process disconnect from room
        mFileTransferService.disconnectFromRoom();

        //after disconnected from skylink SDK, UI will be updated later on ChatService.onDisconnect
    }

    @Override
    public void onRemotePeerJoin(SkylinkPeer newPeer) {
        //add new remote peer
        mFileTransferView.onAddPeerRadioBtn(newPeer);

        // Update textview to show room status when first remote peer has joined with self peer
        if (mFileTransferService.getTotalPeersInRoom() == 2) {
            updateRoomDetails();
        }
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId) {
        // Remove remote peer
        mFileTransferView.onRemovePeerRadioBtn(remotePeerId);

        // Update textview to show room status when last remote peer has left
        if (mFileTransferService.getTotalPeersInRoom() == 1) {
            updateRoomDetails();
        }
    }

    @Override
    public void onPermissionRequired(PermRequesterInfo info) {
        mPermissionUtils.onPermissionRequiredHandler(info, TAG, mContext, mFileTransferView.onGetFragment());
    }

    @Override
    public void onPermissionGranted(PermRequesterInfo info) {
        mPermissionUtils.onPermissionGrantedHandler(info, TAG);
    }

    @Override
    public void onPermissionDenied(PermRequesterInfo info) {
        mPermissionUtils.onPermissionDeniedHandler(info, mContext, TAG);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag) {
        mPermissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, tag);
    }

    @Override
    public void onFileTransferPermissionResponse(String remotePeerId, String fileName, boolean isPermitted){
        if (isPermitted) {
            String log = "Sending file";
            toastLog(TAG, mContext, log);
        } else {
            String log = "Sorry, the remote peer has not granted permission for file transfer";
            toastLog(TAG, mContext, log);
        }
    }

    @Override
    public void onFileTransferPermissionRequest(String remotePeerId, String fileName, boolean isPrivate){

        String log = "Received a file request";
        toastLogLong(TAG, mContext, log);

        // Take note of download file name.
        if (!"".equals(fileName)) {
            fileNameDownloaded = fileName;
        }
        //Send false to reject file transfer
        mFileTransferService.sendFileTransferPermissionResponse(remotePeerId, getDownloadedFilePath(), true);

    }

    @Override
    public void onFileTransferDrop(String remotePeerId, String fileName, String message, boolean isExplicit){
        String log = "The file transfer was dropped.\nReason : " + message;
        toastLogLong(TAG, mContext, log);
    }

    @Override
    public void onFileSendComplete(String remotePeerId, String fileName){
        String log = "Your file has been sent";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onFileReceiveComplete(String remotePeerId, String fileName){
        String log = "A file has been received : " + fileName;
        toastLog(TAG, mContext, log);

        String info = "File Transfer Successful\n\nDestination : " + getDownloadedFilePath();
        mFileTransferView.onUpdateTvFileTransferDetails(info);
    }

    @Override
    public void onFileSendProgress(String remotePeerId, String fileName, double percentage){
        String log = "Uploading... " + percentage;
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onFileReceiveProgress(String remotePeerId, String fileName, double percentage){
        String log = "Downloading... " + percentage;
        toastLog(TAG, mContext, log);
    }

    /**
     * Sends a file to a Peer or all Peers in room.
     *
     * @param remotePeerId Peer to send to. Use null to send to all in room.
     * @param filePath
     */
    @Override
    public void onSendFile(String remotePeerId, String filePath) {

        // Do not allow button actions if there are no remote Peers in the room.
        if (mFileTransferService.getTotalPeersInRoom() < 2) {
            String log = mContext.getString(R.string.warn_no_peer_message);
            toastLog(TAG, mContext, log);
            return;
        }

        if(remotePeerId == null) {
            // Select All Peers RadioButton if not already selected
            String remotePeer = mFileTransferView.onGetPeerIdSelected();

            //force to select radio button peerAll
            if (remotePeer != null) {
                mFileTransferView.onSetRdPeerAllChecked(true);
            }
        }

        //Check file valid
        File file = new File(filePath);

        if (file.isFile()) {

            mFileTransferView.onSetImagePreviewFromFile(Uri.parse(filePath));

        } else {
            String log = "Please enter a valid filename";
            toastLog(TAG, mContext, log);
            return;
        }

        mFileTransferService.sendFile(remotePeerId, file);
    }


    private void updateUI() {

        mFileTransferView.onFillPeerRadioBtn(mFileTransferService.getPeersList());

        updateRoomDetails();
    }

    private void updateRoomDetails() {
        String strRoomDetails = getRoomDetails();
        mFileTransferView.onUpdateRoomDetails(strRoomDetails);
    }

    private String getRoomDetails() {
        boolean isConnected = mFileTransferService.isConnectingOrConnected();
        String roomName = mFileTransferService.getRoomName(Config.ROOM_NAME_CHAT);
        String userName = mFileTransferService.getUserName(null, Config.USER_NAME_CHAT);

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
    private String getDownloadedFilePath() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return path.getAbsolutePath() + File.separator + fileNameDownloaded;
    }
}
