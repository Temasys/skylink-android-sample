package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.util.Log;

import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.service.AudioService;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class AudioCallPresenter implements AudioCallContract.Presenter {

    private final String TAG = AudioCallPresenter.class.getName();

    private Context mContext;

    //view object
    private AudioCallContract.View mAudioCallView;

    //service object
    private AudioService mAudioCallService;

    //utils to process permission
    private PermissionUtils mPermissionUtils;

    //current audio output
    private boolean isSpeakerOn;

    //constructor
    public AudioCallPresenter(AudioCallContract.View AudioCallView, Context context) {

        this.mContext = context;

        this.mAudioCallView = AudioCallView;
        this.mAudioCallService = new AudioService(context);

        //link between view and presenter
        this.mAudioCallView.setPresenter(this);

        //link between service and presenter
        this.mAudioCallService.setPresenter(this);

        this.mPermissionUtils = new PermissionUtils();

        this.mAudioCallService.setTypeCall();
    }

    /**
     * Triggered when View request data to display to the user when entering room | rotating screen
     * Try to connect to room when entering room
     * Update info when rotating screen
     */
    @Override
    public void onViewLayoutRequested() {

        Log.d(TAG, "onViewLayoutRequested");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mAudioCallService.isConnectingOrConnected()) {

            //reset permission request states.
            mPermissionUtils.permQReset();

            //connect to room on Skylink connection
            mAudioCallService.connectToRoom();

            //after connected to skylink SDK, UI will be updated later on AudioService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //if it already connected to room, then resume permission
            mPermissionUtils.permQResume(mContext, mAudioCallView.onGetFragment());

            //update UI into connected state
            updateUI(mAudioCallService.isPeerJoin());

            Log.d(TAG, "Try to update UI when changing configuration");
        }

        //get default audio output settings and change UI
        isSpeakerOn = mAudioCallService.getCurrentAudioSpeaker();
        mAudioCallView.onChangeBtnAudioSpeakerUI(isSpeakerOn);
    }

    @Override
    public void onConnect(boolean isSuccessful) {
        updateUI(false);
    }

    @Override
    public void onDisconnect() {
        updateUI(false);
    }

    @Override
    public void onViewExit() {

        //process disconnect from room
        mAudioCallService.disconnectFromRoom();

        //reset default audio speaker
        mAudioCallService.setCurrenAudioSpeaker(Utils.getDefaultAudioSpeaker());

        //after disconnected from skylink SDK, UI will be updated later on AudioService.onDisconnect
    }

    @Override
    public void onChangeAudioToSpeaker() {
        //check current speakerOn
        isSpeakerOn = !isSpeakerOn;

        mAudioCallService.changeAudioOutput(isSpeakerOn);
    }

    @Override
    public void onAudioChangedToSpeaker(boolean isSpeakerOn) {
        mAudioCallView.onChangeBtnAudioSpeakerUI(isSpeakerOn);
        mAudioCallService.setCurrenAudioSpeaker(isSpeakerOn);
    }

    @Override
    public void onRemotePeerJoin(SkylinkPeer remotePeer) {
        updateUI(true);
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId) {
        updateUI(false);
    }

    @Override
    public void onRemotePeerConnectionRefreshed(String log, UserInfo remotePeerUserInfo) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onRemotePeerMediaReceive(String log, UserInfo remotePeerUserInfo) {
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onPermissionRequired(PermRequesterInfo info) {
        mPermissionUtils.onPermissionRequiredHandler(info, TAG, mContext, mAudioCallView.onGetFragment());
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

    private void updateUI(boolean isPeerJoined) {
        String strRoomDetails = getRoomDetails();
        mAudioCallView.onUpdateUI(strRoomDetails, isPeerJoined);
    }

    private String getRoomDetails() {
        boolean isConnected = mAudioCallService.isConnectingOrConnected();
        String roomName = mAudioCallService.getRoomName(Config.ROOM_NAME_AUDIO);
        String userName = mAudioCallService.getUserName(null, Config.USER_NAME_AUDIO);
        String remotePeerName = mAudioCallService.getRemotePeerName();

        boolean isPeerJoined = mAudioCallService.isPeerJoin();

        String roomDetails = "You are not connected to any room";

        if (isConnected) {
            roomDetails = "Now connected to Room named : " + roomName
                    + "\n\nYou are signed in as : " + userName + "\n";
            if (isPeerJoined) {
                roomDetails += "\nPeer(s) are in the room : "+ remotePeerName;
            } else {
                roomDetails += "\nYou are alone in this room";
            }
        }

        return roomDetails;
    }
}
