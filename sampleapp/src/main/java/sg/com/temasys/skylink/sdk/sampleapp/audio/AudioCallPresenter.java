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

    /**
     * Triggered when View request data to display to the user when entering room | rotating screen
     * Try to connect to room when entering room
     * Update info when rotating screen
     */
    @Override
    public void onViewRequestConnectedLayout() {

        Log.d(TAG, "onViewLayoutRequested");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mAudioCallService.isConnectingOrConnected()) {

            //reset permission request states.
            mPermissionUtils.permQReset();

            //connect to room on Skylink connection
            mAudioCallService.connectToRoom(Constants.CONFIG_TYPE.AUDIO);

            //set default for audio output
            mAudioCallService.setCurrenAudioSpeaker(Utils.getDefaultAudioOutput());

            //after connected to skylink SDK, UI will be updated later on AudioService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //if it already connected to room, then resume state
            mPermissionUtils.permQResume(mContext, mAudioCallView.onPresenterRequestGetFragmentInstance());

            //update UI into connected state
            processUpdateUI(mAudioCallService.isPeerJoin());

            Log.d(TAG, "Try to update UI when changing configuration");
        }

        //get default audio output settings and change UI
        isSpeakerOn = mAudioCallService.getCurrentAudioSpeaker();
        mAudioCallView.onPresenterRequestChangeAudioOutput(mAudioCallService.isPeerJoin(), isSpeakerOn);
    }

    @Override
    public void onViewRequestStop() {
    }

    @Override
    public void onViewRequestResume() {
    }

    @Override
    public void onViewRequestExit() {

        //process disconnect from room
        mAudioCallService.disconnectFromRoom();

        //reset default audio speaker
        mAudioCallService.setCurrenAudioSpeaker(Utils.getDefaultAudioOutput());

        //after disconnected from skylink SDK, UI will be updated later on onDisconnect()
    }

    @Override
    public void onViewRequestChangeAudioOuput() {
        //check current speakerOn
        isSpeakerOn = !isSpeakerOn;

        mAudioCallService.changeAudioOutput(isSpeakerOn);
    }

    @Override
    public void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag) {
        mPermissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, tag);
    }

    @Override
    public void onServiceRequestConnect(boolean isSuccessful) {
        if (isSuccessful) {
            processUpdateUI(false);

            //start audio routing
            SkylinkConfig skylinkConfig = mAudioCallService.getSkylinkConfig();
            if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
                AudioRouter.setPresenter(this);
                AudioRouter.startAudioRouting(mContext, Constants.CONFIG_TYPE.AUDIO);
            }
        }
    }

    @Override
    public void onServiceRequestPermissionRequired(PermRequesterInfo info) {
        mPermissionUtils.onPermissionRequiredHandler(info, TAG, mContext, mAudioCallView.onPresenterRequestGetFragmentInstance());
    }

    @Override
    public void onServiceRequestDisconnect() {

        //stop audio routing
        SkylinkConfig skylinkConfig = mAudioCallService.getSkylinkConfig();
        if (skylinkConfig.hasAudioSend() && skylinkConfig.hasAudioReceive()) {
            AudioRouter.stopAudioRouting(mContext);
        }

        processUpdateUI(false);
    }

    @Override
    public void onServiceRequestAudioOutputChanged(boolean isSpeakerOn) {
        this.isSpeakerOn = isSpeakerOn;
        mAudioCallView.onPresenterRequestChangeAudioOutput(mAudioCallService.isPeerJoin(), isSpeakerOn);
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
        processUpdateUI(true);
    }

    @Override
    public void onServiceRequestRemotePeerLeave(String remotePeerId, int removeIndex) {
        processUpdateUI(false);
    }

    private void processUpdateUI(boolean isPeerJoined) {
        String strRoomDetails = processGetRoomDetails();
        mAudioCallView.onPresenterRequestUpdateUI(strRoomDetails, isPeerJoined, isSpeakerOn);
    }

    private String processGetRoomDetails() {
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
                roomDetails += "\nPeer(s) are in the room : " + remotePeerName;
            } else {
                roomDetails += "\nYou are alone in this room";
            }
        }

        return roomDetails;
    }
}
