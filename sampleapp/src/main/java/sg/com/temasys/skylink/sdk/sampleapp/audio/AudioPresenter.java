package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.util.Log;

import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
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

public class AudioPresenter extends BasePresenter implements AudioContract.Presenter {

    private final String TAG = AudioPresenter.class.getName();

    private Context context;

    //view object
    private AudioContract.View audioCallView;

    //service object
    private AudioService audioCallService;

    //utils to process permission
    private PermissionUtils permissionUtils;

    //current audio output getting from default setting
    private boolean currentAudioSpeaker = Utils.isDefaultSpeakerSettingForAudio();

    //constructor
    public AudioPresenter(Context context) {
        this.context = context;
        this.audioCallService = new AudioService(context);
        this.audioCallService.setPresenter(this);
        this.permissionUtils = new PermissionUtils();
    }

    //link Presenter to View
    public void setView(AudioContract.View view) {
        audioCallView = view;
        audioCallView.setPresenter(this);
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from AudioContract.Presenter for view to call
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

            // start local audio
            audioCallService.createLocalAudio();

            //after connected to skylink SDK, UI will be updated later on onServiceRequestConnect

            Log.d(TAG, "Try to connect when entering room");
        }

        //get default audio output settings and change UI
        audioCallView.onPresenterRequestChangeAudioOutput(currentAudioSpeaker);
    }

    @Override
    public void onViewRequestChangeAudioOuput() {
        //change current speakerOn
        currentAudioSpeaker = !currentAudioSpeaker;

        // change audio output (speaker state) in AudioRouter
        if(currentAudioSpeaker){
            AudioRouter.turnOnSpeaker();
        } else{
            AudioRouter.turnOffSpeaker();
        }
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

        // need to call disposeLocalMedia to clear all local media objects as disconnectFromRoom no longer dispose local media
        audioCallService.disposeLocalMedia();

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
        }
    }

    @Override
    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
        // delegate to PermissionUtils to process the permissions require
        permissionUtils.onPermissionRequiredHandler(info, TAG, context, audioCallView.onPresenterRequestGetFragmentInstance());
    }

    @Override
    public void onServiceRequestLocalAudioCapture(SkylinkMedia localAudio) {
        toastLog("[SA][onServiceRequestLocalAudioCapture]", context, "Local audio is on with id = " + localAudio.getMediaId());

        // change the audio output base on the default setting
        AudioRouter.setPresenter(this);
        AudioRouter.startAudioRouting(context, Constants.CONFIG_TYPE.AUDIO);

        // use service layer to change the audio output, update UI will be called later in onServiceRequestAudioOutputChanged
        if(currentAudioSpeaker){
            AudioRouter.turnOnSpeaker();
        } else {
            AudioRouter.turnOffSpeaker();
        }
    }

    @Override
    public void onServiceRequestAudioOutputChanged(boolean isSpeakerOn) {
        // change the current speaker state
        this.currentAudioSpeaker = isSpeakerOn;

        // change button UI
        audioCallView.onPresenterRequestChangeAudioOutput(currentAudioSpeaker);

        if (currentAudioSpeaker) {
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
                audioCallService.getTotalPeersInRoom() - 2);
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
        AudioRouter.stopAudioRouting(context);
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
