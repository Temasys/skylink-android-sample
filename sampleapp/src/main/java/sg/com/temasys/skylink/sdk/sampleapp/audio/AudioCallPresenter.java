package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.support.v4.app.Fragment;

import sg.com.temasys.skylink.sdk.sampleapp.data.model.AudioRemotePeer;
import sg.com.temasys.skylink.sdk.sampleapp.data.service.AudioCallService;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class AudioCallPresenter implements AudioCallContract.Presenter {

    private AudioCallContract.View mAudioCallView;

    private AudioCallService mAudioCallService;

    public AudioCallPresenter(AudioCallContract.View AudioCallView, Context context) {
        this.mAudioCallView = AudioCallView;
        this.mAudioCallService = new AudioCallService(context);

        //link between view and service
        this.mAudioCallView.setPresenter(this);
        this.mAudioCallService.setPresenter(this);

    }

    /////////////////////////////Listener to work with AudioCallFragment////////////////////////////

    @Override
    public void setAudioRemotePeerPresenterHandler(AudioRemotePeer audioRemotePeer) {
        mAudioCallView.setAudioRemotePeerViewHandler(audioRemotePeer);
    }

    @Override
    public void onDisconnectUIChangePresenterHandler() {
        mAudioCallView.onDisconnectUIChangeViewHandler();
    }

    @Override
    public Fragment getFragmentPresenterHandler() {
        return mAudioCallView.getFragmentViewHandler();
    }

    @Override
    public void setRoomDetailsPresenterHandler(String roomDetails) {
        mAudioCallView.setRoomDetailsViewHandler(roomDetails);
    }

    /////////////////////////////////Listener to work with AudioCallService/////////////////////////

    @Override
    public void connectToRoomPresenterHandler() {
        mAudioCallService.connectToRoomServiceHandler();
    }

    @Override
    public void disconnectFromRoomPresenterHandler() {
        mAudioCallService.disconnectFromRoomServiceHandler();
    }

    @Override
    public boolean isConnectingOrConnectedPresenterHandler() {
        return mAudioCallService.isConnectingOrConnectedServiceHandler();
    }

    @Override
    public String getRoomDetailsPresenterHandler(boolean isPeerJoined) {
        return mAudioCallService.getRoomDetailsServiceHandler(isPeerJoined);
    }

    @Override
    public int getNumRemotePeersPresenterHandler() {
        return mAudioCallService.getNumRemotePeersServiceHandler();
    }

}
