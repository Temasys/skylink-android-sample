package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import android.content.Context;
import android.util.Log;

import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.DataTransferService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for implementing data transfer logic.
 */

public class DataTransferPresenter extends BasePresenter implements DataTransferContract.Presenter {

    private final String TAG = DataTransferPresenter.class.getName();

    private Context mContext;

    private DataTransferContract.View mDataTransferView;
    private DataTransferService mDataTransferService;
    //utils to process permission
    private PermissionUtils mPermissionUtils;

    // the index of the peer on the action bar that user selected to send message privately
    // default is 0 - send message to all peers
    private int selectedPeerIndex = 0;

    public DataTransferPresenter(Context context) {
        this.mContext = context;
        this.mDataTransferService = new DataTransferService(context);
        this.mDataTransferService.setPresenter(this);
        this.mPermissionUtils = new PermissionUtils();
    }

    public void setView(DataTransferContract.View view) {
        mDataTransferView = view;
        mDataTransferView.setPresenter(this);
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for view to call
    // These methods are responsible for processing requests from view
    //----------------------------------------------------------------------------------------------

    /**
     * Triggered when View request data to display to the user when entering room | rotating screen
     * Try to connect to room when entering room
     * Try to update UI if connected to room after changing configuration
     */
    @Override
    public void onViewRequestConnectedLayout() {

        Log.d(TAG, "onViewLayoutRequested");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mDataTransferService.isConnectingOrConnected()) {

            //connect to room on Skylink connection
            mDataTransferService.connectToRoom(Constants.CONFIG_TYPE.DATA);

            //after connected to skylink SDK, UI will be updated later on ChatService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //update UI into connected state
            processUpdateUIConnected();

            Log.d(TAG, "Try to update UI when changing configuration");
        }
    }

    /**
     * process file permission that comes from the app
     * when user first choose browsing file from device, permission request dialog will be display
     */
    @Override
    public boolean onViewRequestFilePermission() {
        return mPermissionUtils.requestFilePermission(mContext, mDataTransferView.onPresenterRequestGetFragmentInstance());
    }

    /**
     * display a warning if user deny the file permission
     */
    @Override
    public void onViewRequestPermissionDeny() {
        mPermissionUtils.displayFilePermissionWarning(mContext);
    }

    /**
     * process result of permission that comes from SDK
     */
    @Override
    public void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag) {
        // delegate to the PermissionUtils to process the permission
        mPermissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, tag);
    }

    /**
     * Save the current index of the selected peer
     */
    @Override
    public void onViewRequestSelectedRemotePeer(int index) {
        // check the selected index with the current selectedPeerIndex
        // if it is equal which means user in selects the peer
        if (this.selectedPeerIndex == index) {
            this.selectedPeerIndex = 0;
        } else {
            this.selectedPeerIndex = index;
        }
    }

    /**
     * Get the current index of selected peer
     */
    @Override
    public int onViewRequestGetCurrentSelectedPeer() {
        return this.selectedPeerIndex;
    }

    /**
     * Get the specific peer object according to the index
     */
    @Override
    public SkylinkPeer onViewRequestGetPeerByIndex(int index) {
        return mDataTransferService.getPeerByIndex(index);
    }

    @Override
    public void onViewRequestSendData(byte[] data) {
        // Do not allow button actions if there are no remote Peers in the room.
        if (mDataTransferService.getTotalPeersInRoom() < 2) {
            String log = mContext.getString(R.string.warn_no_peer_message);
            toastLog(TAG, mContext, log);
            return;
        }

        // send data to all peers in room if user do not select any specific peer
        // send data to specific peer id if user choose the peer
        String remotePeerId = null;
        if (selectedPeerIndex != 0) {
            remotePeerId = mDataTransferService.getPeerByIndex(selectedPeerIndex).getPeerId();
        }

        // delegate to service layer to implement sending data
        String error = null;
        try {
            mDataTransferService.sendData(remotePeerId, data);
        } catch (SkylinkException e) {
            error = e.getMessage();
        } catch (UnsupportedOperationException e) {
            error = e.getMessage();
        }

        if (error != null) {
            toastLogLong(TAG, mContext, error);
        } else {
            toastLog(TAG, mContext, "You have sent an array of data");
        }
    }

    @Override
    public void onViewRequestExit() {

        //process disconnect from room
        mDataTransferService.disconnectFromRoom();
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful)
            processUpdateUIConnected();
    }

    @Override
    public void onServiceRequestRemotePeerJoin(SkylinkPeer newPeer) {
        // Fill the new peer in button in custom bar
        mDataTransferView.onPresenterRequestChangeUiRemotePeerJoin(newPeer,
                mDataTransferService.getTotalPeersInRoom() - 1);
    }

    @Override
    public void onServiceRequestRemotePeerLeave(SkylinkPeer remotePeer, int removeIndex) {
        // do not process if the left peer is local peer
        if (removeIndex == -1)
            return;

        // Remove the peer in button in custom bar
        mDataTransferView.onPresenterRequestChangeUiRemotePeerLeft(mDataTransferService.getPeersList());
    }

    /**
     * process SDK permission
     */
    @Override
    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
        // delegate to the PermissionUtils to process the permission
        mPermissionUtils.onPermissionRequiredHandler(info, TAG, mContext, mDataTransferView.onPresenterRequestGetFragmentInstance());
    }

    @Override
    public void onServiceRequestDataReceive(Context context, String remotePeerId, byte[] data) {
        SkylinkPeer remotePeer = mDataTransferService.getPeerById(remotePeerId);
        mDataTransferView.onPresenterRequestChangeUIReceivedData(remotePeer, data);

        toastLog("DataTransfer", mContext, "You have received an array of data");
    }


    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------


    /**
     * Update UI when connected to room
     */
    private void processUpdateUIConnected() {
        // Update the room id in the action bar
        mDataTransferView.onPresenterRequestUpdateRoomInfo(processGetRoomId());

        // Update the local peer info in the local peer button in action bar
        mDataTransferView.onPresenterRequestUpdateLocalPeer(Config.USER_NAME_DATA);
    }


    /**
     * Get the room id info
     */
    private String processGetRoomId() {
        return mDataTransferService.getRoomId();
    }
}

