package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.util.Log;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.AudioService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for implementing audio logic.
 */

public class AudioCallPresenter extends BasePresenter implements AudioCallContract.Presenter {

    private final String TAG = AudioCallPresenter.class.getName();

    private Context mContext;

    //AudioCallFragment object
    private AudioCallContract.View mAudioCallView;

    //service object
    private AudioService mAudioCallService;

    //utils to process permission
    private PermissionUtils mPermissionUtils;

    //current audio output
    private boolean isSpeakerOn;

    //constructor
    public AudioCallPresenter(Context context) {
        this.mContext = context;
        this.mAudioCallService = new AudioService(context);
        this.mAudioCallService.setPresenter(this);
        this.mPermissionUtils = new PermissionUtils();
    }

    //link Presenter to View
    public void setView(AudioCallContract.View view) {
        mAudioCallView = view;
        mAudioCallView.setPresenter(this);
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for view to call
    // These methods are responsible for processing requests from view
    //----------------------------------------------------------------------------------------------

    /**
     * Triggered when View request data to display to the user when entering room | rotating screen
     * Try to connect to room when entering room
     * Update info when rotating screen
     */
    @Override
    public void onViewRequestConnectedLayout() {

        Log.d(TAG, "[onViewRequestConnectedLayout]");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mAudioCallService.isConnectingOrConnected()) {

            //reset permission request states.
            mPermissionUtils.permQReset();

            //connect to room on Skylink connection
            mAudioCallService.connectToRoom(Constants.CONFIG_TYPE.AUDIO);

            //set default for audio output
            mAudioCallService.setCurrenAudioSpeaker(Utils.getDefaultAudioSpeaker());

            //after connected to skylink SDK, UI will be updated later on AudioService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //if it already connected to room, then resume state
            mPermissionUtils.permQResume(mContext, mAudioCallView.onPresenterRequestGetFragmentInstance());

            //update UI into connected state
            processUpdateUIConnected();

            Log.d(TAG, "Try to update UI when changing configuration");
        }

        //get default audio output settings and change UI
        isSpeakerOn = mAudioCallService.getCurrentAudioSpeaker();
        mAudioCallView.onPresenterRequestChangeAudioOutput(mAudioCallService.isPeerJoin(), isSpeakerOn);
    }

    @Override
    public void onViewRequestChangeAudioOuput() {
        //change current speakerOn
        isSpeakerOn = !isSpeakerOn;

        // change button UI
        mAudioCallService.changeAudioOutput(isSpeakerOn);
    }

    @Override
    public void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag) {
        // delegate to PermissionUtils to process the permissions
        mPermissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, tag);
    }

    /**
     * Get the specific peer object according to the index
     */
    @Override
    public SkylinkPeer onViewRequestGetPeerByIndex(int index) {
        return mAudioCallService.getPeerByIndex(index);
    }

    @Override
    public void onViewRequestExit() {

        //process disconnect from room
        mAudioCallService.disconnectFromRoom();

        //reset default audio speaker
        mAudioCallService.setCurrenAudioSpeaker(Utils.getDefaultAudioSpeaker());

        //after disconnected from skylink SDK, UI will be updated later on onDisconnect()
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful) {

            // change UI to connected to room, but not connected to any peer
            processUpdateUIConnected();

            //start audio routing if has audio config
            SkylinkConfig skylinkConfig = mAudioCallService.getSkylinkConfig();
            if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
                AudioRouter.setPresenter(this);
                AudioRouter.startAudioRouting(mContext, Constants.CONFIG_TYPE.AUDIO);
            }
        }
    }

    @Override
    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
        // delegate to PermissionUtils to process the permissions require
        mPermissionUtils.onPermissionRequiredHandler(info, TAG, mContext, mAudioCallView.onPresenterRequestGetFragmentInstance());
    }

    @Override
    public void onServiceRequestAudioOutputChanged(boolean isSpeakerOn) {
        // change the current speaker state
        this.isSpeakerOn = isSpeakerOn;

        // change button UI
        mAudioCallView.onPresenterRequestChangeAudioOutput(mAudioCallService.isPeerJoin(), isSpeakerOn);

        // change speaker state in service layer
        mAudioCallService.setCurrenAudioSpeaker(isSpeakerOn);
    }

    @Override
    public void onServiceRequestRemotePeerMediaReceive(String log, UserInfo remotePeerUserInfo, String remotePeerId) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onServiceRequestRemotePeerConnectionRefreshed(String log, UserInfo remotePeerUserInfo) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onServiceRequestRemotePeerJoin(SkylinkPeer remotePeer) {
        // Fill the new peer in button in custom bar
        processAddNewPeer(remotePeer, mAudioCallService.getTotalPeersInRoom() - 1);
    }

    @Override
    public void onServiceRequestRemotePeerLeave(SkylinkPeer remotePeer, int removeIndex) {
        // do not process if the left peer is local peer
        if (removeIndex == -1)
            return;

        // Remove the peer in button in custom bar
        processRemoveRemotePeer();
    }

    @Override
    public void onServiceRequestDisconnect() {
        //stop audio routing
        SkylinkConfig skylinkConfig = mAudioCallService.getSkylinkConfig();
        if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
            AudioRouter.stopAudioRouting(mContext);
        }
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Update UI when connected to room
     */
    private void processUpdateUIConnected() {
        // Update the room id in the action bar
        mAudioCallView.onPresenterRequestUpdateRoomInfo(processGetRoomId());

        // Update the local peer info in the local peer button in action bar
        mAudioCallView.onPresenterRequestUpdateUIConnected(Config.USER_NAME_AUDIO);

    }

    /**
     * Get the room id info
     */
    private String processGetRoomId() {
        return mAudioCallService.getRoomId();
    }

    /**
     * Add new peer on UI when new peer joined in room in specific index
     *
     * @param newPeer the new peer joined in room
     * @param index   the index of the new peer to add
     */
    private void processAddNewPeer(SkylinkPeer newPeer, int index) {
        mAudioCallView.onPresenterRequestChangeUiRemotePeerJoin(newPeer, index);
    }

    /**
     * Remove a remote peer by re-fill total remote peer left in the room
     * to make sure the left peers are displayed correctly
     */
    private void processRemoveRemotePeer() {
        mAudioCallView.onPresenterRequestChangeUIRemotePeerLeft(mAudioCallService.getPeersList());
    }
}
