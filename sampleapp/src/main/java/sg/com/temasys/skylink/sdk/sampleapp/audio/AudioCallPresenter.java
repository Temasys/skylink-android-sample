package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.data.service.AudioService;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;

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

    //constructor
    public AudioCallPresenter(AudioCallContract.View AudioCallView, Context context) {

        this.mContext = context;

        this.mAudioCallView = AudioCallView;
        this.mAudioCallService = new AudioService(context);

        //link between view and presenter
        this.mAudioCallView.setPresenter(this);

        //link between service and presenter
        this.mAudioCallService.setPresenter(this);

        mPermissionUtils = new PermissionUtils(context);

        this.mAudioCallService.setTypeCall();
    }

    @Override
    public void onRequestPermissionsResultPresenterHandler(int requestCode, String[] permissions, int[] grantResults, String tag) {
        mPermissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, tag);
    }

    @Override
    public void onPermissionRequiredPresenterHandler(PermRequesterInfo info) {
        mPermissionUtils.onPermissionRequiredHandler(info, TAG, mContext, mAudioCallView.onGetFragmentViewHandler());
    }

    @Override
    public void onPermissionGrantedPresenterHandler(String[] permissions, int infoCode){
        mPermissionUtils.onPermissionGrantedHandler(permissions, infoCode, TAG);
    }

    @Override
    public void onPermissionDeniedPresenterHandler(int infoCode){
        mPermissionUtils.onPermissionDeniedHandler(infoCode, mContext, TAG);
    }

    @Override
    public void onDisconnectPresenterHandler() {
        updateUIPresenterHandler();
    }

    @Override
    public void onRemotePeerJoinPresenterHandler(String remotePeerId, String nick) {
        updateUIPresenterHandler();
    }

    @Override
    public void onRemotePeerLeavePresenterHandler(String remotePeerId) {
        updateUIPresenterHandler();
    }

    /**
     * Triggered when View request data to display to the user when entering room | leaving room | rotating screen
     * Try to connect to room when entering room
     * Try to disconnect from room when leaving room
     * Update info when rotating screen
     */
    @Override
    public void onViewLayoutRequestedPresenterHandler() {

        Log.d(TAG, "onViewLayoutRequestedPresenterHandler");

        //start to connect to room when entering room
        //if not being connected, then connect
        if (!mAudioCallService.isConnectingOrConnectedServiceHandler()) {

            //reset permission request states.
            mPermissionUtils.permQReset();

            //connect to room on Skylink connection
            mAudioCallService.connectToRoomServiceHandler();

            //after connected to skylink SDK, UI will be updated latter on AudioService.onConnect

            Log.d(TAG, "Try to connect when entering room");

        } else {

            //if it already connected to room, then resume permission
            mPermissionUtils.permQResume(mContext, mAudioCallView.onGetFragmentViewHandler());

            //update UI into connected
            updateUIPresenterHandler();

            Log.d(TAG, "Try to update UI when changing configuration");
        }
    }

    @Override
    public void onViewExitPresenterHandler() {

        //process disconnect from room
        mAudioCallService.disconnectFromRoomServiceHandler();

        //after disconnected from skylink SDK, UI will be updated latter on AudioService.onDisconnect
    }

    @Override
    public void onConnectPresenterHandler(boolean isSuccessful) {
            updateUIPresenterHandler();
    }

    private void updateUIPresenterHandler() {
        String strRoomDetails = getRoomDetailsPresenterHandler();
        mAudioCallView.onUpdateUIViewHandler(strRoomDetails);
    }

    public String getRoomDetailsPresenterHandler() {
        boolean isConnected = mAudioCallService.isConnectingOrConnectedServiceHandler();
        String roomName = mAudioCallService.getRoomNameBaseServiceHandler(Config.ROOM_NAME_AUDIO);
        String userName = mAudioCallService.getUserNameBaseServiceHandler(null, Config.USER_NAME_AUDIO);

        boolean isPeerJoined = mAudioCallService.isPeerJoinServiceHandler();

        String roomDetails = "You are not connected to any room";

        if (isConnected) {
            roomDetails = "Now connected to Room named : " + roomName
                    + "\n\nYou are signed in as : " + userName + "\n";
            if (isPeerJoined) {
                roomDetails += "\nPeer(s) are in the room";
//                roomDetails += "\n" + mAudioCallService..getRemotePeerName();
            } else {
                roomDetails += "\nYou are alone in this room";
            }
        }

        return roomDetails;
    }

    @Override
    public void onRemotePeerConnectionRefreshedPresenterHandler(String log, UserInfo remotePeerUserInfo){
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".";
        toastLog(TAG, mContext, log);
    }

    @Override
    public void onLocalMediaCapturePresenterHandler(SurfaceViewRenderer videoView){
        //do nothing
    }

    @Override
    public void onInputVideoResolutionObtainedPresenterHandler(int width, int height, int fps, SkylinkCaptureFormat captureFormat){
        // Will not be implemented in Audio only client.
    }

    @Override
    public void onReceivedVideoResolutionObtainedPresenterHandler(String peerId, int width, int height, int fps){
        // Will not be implemented in Audio only client.
    }

    @Override
    public void onSentVideoResolutionObtainedPresenterHandler(String peerId, int width, int height, int fps){
        // Will not be implemented in Audio only client.
    }

    @Override
    public void onVideoSizeChangePresenterHandler(String peerId, Point size){
        // Will not be implemented in Audio only client.
    }

    @Override
    public void onRemotePeerMediaReceivePresenterHandler(String log, UserInfo remotePeerUserInfo){
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".";
        toastLog(TAG, mContext, log);
    }




}
