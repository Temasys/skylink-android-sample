package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
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
    public void processConnectedLayout() {

        Log.d(TAG, "[processConnectedLayout]");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!audioCallService.isConnectingOrConnected()) {

            //reset permission request states.
            permissionUtils.permQReset();

            //connect to room on Skylink connection
            audioCallService.connectToRoom(Constants.CONFIG_TYPE.AUDIO);

            // start local audio
            audioCallService.createLocalAudio();

            // change the audio output base on the default setting
            AudioRouter.setPresenter(this);
            AudioRouter.startAudioRouting(context, Constants.CONFIG_TYPE.AUDIO);

            if (Utils.isDefaultSpeakerSettingForAudio() && AudioRouter.unsupportedHWAECList.contains(Build.MODEL)) {
                // temporary turn off speaker first to avoid echo for Xiaomi device in the black list
                AudioRouter.turnOffSpeaker();

                currentAudioSpeaker = false;
            } else {
                // use service layer to change the audio output, update UI will be called later in processAudioOutputChanged
                if (currentAudioSpeaker) {
                    AudioRouter.turnOnSpeaker();
                } else {
                    AudioRouter.turnOffSpeaker();
                }
            }

            audioCallView.updateUIAudioOutputChanged(currentAudioSpeaker);

            //after connected to skylink SDK, UI will be updated later on processRoomConnected

            Log.d(TAG, "Try to connect when entering room");
        }
    }

    @Override
    public void processChangeAudioOutput() {
        //change current speakerOn
        currentAudioSpeaker = !currentAudioSpeaker;

        // change audio output (speaker state) in AudioRouter
        if (currentAudioSpeaker) {
            AudioRouter.turnOnSpeaker();
        } else {
            AudioRouter.turnOffSpeaker();
        }
    }

    @Override
    public void processPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // delegate to PermissionUtils to process the permissions
        permissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, TAG);
    }

    /**
     * Get the specific peer object according to the index
     */
    @Override
    public SkylinkPeer processGetPeerByIndex(int index) {
        return audioCallService.getPeerByIndex(index);
    }

    @Override
    public void processExit() {
        //process disconnect from room
        audioCallService.disconnectFromRoom();

        // need to call disposeLocalMedia to clear all local media objects as disconnectFromRoom no longer dispose local media
        audioCallService.disposeLocalMedia();

        //after disconnected from skylink SDK, UI will be updated latter on processRoomDisconnected
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    @Override
    public void processRoomConnected(boolean isSuccessful) {
        if (isSuccessful) {

            // change UI to connected to room, but not connected to any peer
            processUpdateStateConnected();
        }
    }

    @Override
    public void processRoomDisconnected() {
        //stop audio routing
        AudioRouter.stopAudioRouting(context);

        // update UI
        audioCallView.updateUIDisconnected();
    }

    @Override
    public void processPermissionRequired(PermRequesterInfo info) {
        // delegate to PermissionUtils to process the permissions require
        permissionUtils.onPermissionRequiredHandler(info, TAG, context, audioCallView.getInstance());
    }

    @Override
    public void processLocalAudioCaptured(SkylinkMedia localAudio) {
        toastLog("[SA][processLocalAudioCaptured]", context, "Local audio is on with id = " + localAudio.getMediaId());
    }

    @Override
    public void processAudioOutputChanged(boolean isSpeakerOn) {
        // change the current speaker state
        this.currentAudioSpeaker = isSpeakerOn;

        // change button UI
        audioCallView.updateUIAudioOutputChanged(currentAudioSpeaker);

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
    public void processRemotePeerConnected(SkylinkPeer remotePeer) {
        // Fill the new peer in button in custom bar
        // Display new peer at most right location in action bar
        audioCallView.updateUIRemotePeerConnected(remotePeer,
                audioCallService.getTotalPeersInRoom() - 2);
    }

    @Override
    public void processRemotePeerDisconnected(SkylinkPeer remotePeer, int removeIndex) {
        // do not process if the left peer is local peer
        if (removeIndex == -1)
            return;

        // Remove the peer in button in custom bar
        // Remove a remote peer by re-fill total remote peer left in the room
        // to make sure the left peers are displayed correctly
        audioCallView.updateUIRemotePeerDisconnected(audioCallService.getPeersList());
    }

    @Override
    public void processRemoteAudioReceived(String remotePeerId) {

        // Add delay 3 seconds for audio speaker turned on to avoid audio echo if the device model is not supported AEC
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // turn on speaker now as default setting
                if (Utils.isDefaultSpeakerSettingForAudio() && (AudioRouter.unsupportedHWAECList.contains(Build.MODEL))) {
                    // temporary turn off speaker first to avoid echo for Xiaomi device in the black list
                    AudioRouter.turnOnSpeaker();

                    currentAudioSpeaker = true;

                    //get default audio output settings and change UI
                    audioCallView.updateUIAudioOutputChanged(currentAudioSpeaker);
                }
            }
        }, 3000);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Update UI when connected to room
     */
    private void processUpdateStateConnected() {

        // Update the view into connected state
        audioCallView.updateUIConnected(processGetRoomId());
    }

    /**
     * Get the room id info from SDK
     */
    private String processGetRoomId() {
        return audioCallService.getRoomId();
    }
}
