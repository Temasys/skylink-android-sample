package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.util.Log;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.AudioService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
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

    private Context context;

    //view object
    private AudioCallContract.View audioCallView;

    //service object
    private AudioService audioCallService;

    //utils to process permission
    private PermissionUtils permissionUtils;

    //current audio output
    private boolean currentAudioOutput = Utils.getDefaultAudioSpeaker();

    //constructor
    public AudioCallPresenter(Context context) {
        this.context = context;
        this.audioCallService = new AudioService(context);
        this.audioCallService.setPresenter(this);
        this.permissionUtils = new PermissionUtils();
    }

    //link Presenter to View
    public void setView(AudioCallContract.View view) {
        audioCallView = view;
        audioCallView.setPresenter(this);
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from AudioCallContract.Presenter for view to call
    // These methods are responsible for processing requests from view
    //----------------------------------------------------------------------------------------------

    /**
     * Triggered when View request data to display to the user when entering room
     * Try to connect to room when entering room
     */
    @Override
    public void onViewRequestConnectedLayout() {

        Log.d(TAG, "[onViewRequestConnectedLayout]");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!audioCallService.isConnectingOrConnected()) {

            //reset permission request states.
            permissionUtils.permQReset();

            //connect to room on Skylink connection
            audioCallService.connectToRoom(Constants.CONFIG_TYPE.AUDIO);

            //set default for audio output
            currentAudioOutput = Utils.getDefaultAudioSpeaker();

            //after connected to skylink SDK, UI will be updated later on onServiceRequestConnect

            Log.d(TAG, "Try to connect when entering room");
        }

        //get default audio output settings and change UI
        audioCallView.onPresenterRequestChangeAudioOutput(currentAudioOutput);
    }

    @Override
    public void onViewRequestChangeAudioOuput() {
        //change current speakerOn
        currentAudioOutput = !currentAudioOutput;

        // change audio output (speaker state) in AudioRouter
        AudioRouter.changeAudioOutput(context, currentAudioOutput);
    }

    @Override
    public void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, TAG);
    }

    /**
     * Get the specific peer object according to the index
     */
    @Override
    public SkylinkPeer onViewRequestGetPeerByIndex(int index) {
        return audioCallService.getPeerByIndex(index);
    }

    @Override
    public void onViewRequestExit() {
        //process disconnect from room
        audioCallService.disconnectFromRoom();
        //after disconnected from skylink SDK, UI will be updated latter on onServiceRequestDisconnect
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful) {

            // change UI to connected to room, but not connected to any peer
            processUpdateStateConnected();

            //start audio routing if has audio config
            SkylinkConfig skylinkConfig = audioCallService.getSkylinkConfig();
            if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
                AudioRouter.setPresenter(this);
                AudioRouter.startAudioRouting(context, Constants.CONFIG_TYPE.AUDIO);
            }
        }
    }

    @Override
    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
        // delegate to PermissionUtils to process the permissions require
        permissionUtils.onPermissionRequiredHandler(info, TAG, context, audioCallView.onPresenterRequestGetFragmentInstance());
    }

    @Override
    public void onServiceRequestAudioOutputChanged(boolean isSpeakerOn) {
        // change the current speaker state
        this.currentAudioOutput = isSpeakerOn;

        // change button UI
        audioCallView.onPresenterRequestChangeAudioOutput(currentAudioOutput);

        if (currentAudioOutput) {
            toastLog(TAG, context, "Speaker is turned ON");
        } else {
            toastLog(TAG, context, "Speaker is turned OFF");
        }
    }

    /**
     * Add new peer on UI when new peer joined in room
     *
     * @param remotePeer the new peer joined in room
     */
    @Override
    public void onServiceRequestRemotePeerJoin(SkylinkPeer remotePeer) {
        // Fill the new peer in button in custom bar
        // Display new peer at most right location in action bar
        audioCallView.onPresenterRequestChangeUIRemotePeerJoin(remotePeer,
                audioCallService.getTotalPeersInRoom() - 1);
    }

    @Override
    public void onServiceRequestRemotePeerAudioReceive(String log, UserInfo remotePeerUserInfo, String remotePeerId, String mediaId) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".";
        toastLog(TAG, context, log);
    }

    @Override
    public void onServiceRequestRemotePeerConnectionRefreshed(String log, UserInfo remotePeerUserInfo) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".";
        toastLog(TAG, context, log);
    }

    @Override
    public void onServiceRequestRemotePeerLeave(SkylinkPeer remotePeer, int removeIndex) {
        // do not process if the left peer is local peer
        if (removeIndex == -1)
            return;

        // Remove the peer in button in custom bar
        // Remove a remote peer by re-fill total remote peer left in the room
        // to make sure the left peers are displayed correctly
        audioCallView.onPresenterRequestChangeUIRemotePeerLeft(audioCallService.getPeersList());
    }

    @Override
    public void onServiceRequestDisconnect() {
        //stop audio routing
        SkylinkConfig skylinkConfig = audioCallService.getSkylinkConfig();
        if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
            AudioRouter.stopAudioRouting(context);
        }
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Update UI when connected to room
     */
    private void processUpdateStateConnected() {

        // Update the view into connected state
        audioCallView.onPresenterRequestUpdateUIConnected(processGetRoomId());
    }

    /**
     * Get the room id info from SDK
     */
    private String processGetRoomId() {
        return audioCallService.getRoomId();
    }
}
