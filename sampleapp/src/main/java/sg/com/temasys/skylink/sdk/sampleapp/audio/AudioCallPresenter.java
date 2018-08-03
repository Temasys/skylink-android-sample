package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.Log;

import sg.com.temasys.skylink.sdk.sampleapp.data.service.AudioCallService;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class AudioCallPresenter implements AudioCallContract.Presenter {

    private final String TAG = AudioCallPresenter.class.getName();

    private Context mContext;

    //view object
    private AudioCallContract.View mAudioCallView;

    //service object
    private AudioCallService mAudioCallService;

    //utils to process permission
    private PermissionUtils permissionUtils;

    //constructor
    public AudioCallPresenter(AudioCallContract.View AudioCallView, Context context) {

        this.mContext = context;

        this.mAudioCallView = AudioCallView;
        this.mAudioCallService = new AudioCallService(context);

        //link between view and presenter
        this.mAudioCallView.setPresenter(this);

        //link between service and presenter
        this.mAudioCallService.setPresenter(this);

        permissionUtils = new PermissionUtils(context);
    }

    @Override
    public Fragment getFragmentPresenterHandler() {
        return mAudioCallView.getFragmentViewHandler();
    }

    @Override
    public void setRoomDetailsPresenterHandler(String roomDetails) {
        mAudioCallView.setRoomDetailsViewHandler(roomDetails);
    }

    @Override
    public void onRequestPermissionsResultPresenterHandler(int requestCode, String[] permissions, int[] grantResults, String tag) {
        permissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, tag);
    }

    /**
     * Triggered when View request data to display to the user when entering room | leaving room | rotating screen
     * Try to connect to room when entering room
     * Try to disconnect from room when leaving room
     * Update info when rotating screen
     *
     * @param tryToConnect define action enter room or leave room
     */
    @Override
    public void onViewLayoutRequestedPresenterHandler(boolean tryToConnect) {

        Log.d(TAG, "onViewLayoutRequestedPresenterHandler");

        if (tryToConnect) {
            //start to connect to room when entering room
            //if not being connected, then connect
            if (!mAudioCallService.isConnectingOrConnectedServiceHandler()) {

                //reset permission request states.
                permissionUtils.permQReset();

                //connect to room on Skylink connection
                mAudioCallService.connectToRoomServiceHandler();

                //after connected to skylink SDK, UI will be updated latter on AudioCallService.onConnect

                Log.d(TAG, "Try to connect when entering room");

            } else {

                //if it already connected to room, then resume permission
                permissionUtils.permQResume(mContext, mAudioCallView.getFragmentViewHandler());

                //update UI into connected
                String strRoomDetails = mAudioCallService.getRoomDetailsServiceHandler();
                mAudioCallView.setRoomDetailsViewHandler(strRoomDetails);

                Log.d(TAG, "Try to update UI when changing configuration");
            }

        } else {
            //process disconnect from room
            mAudioCallService.disconnectFromRoomServiceHandler();

            //after disconnected from skylink SDK, UI will be updated latter on AudioCallService.onDisconnect

            Log.d(TAG, "Try to disconnect from room");
        }
    }
}
