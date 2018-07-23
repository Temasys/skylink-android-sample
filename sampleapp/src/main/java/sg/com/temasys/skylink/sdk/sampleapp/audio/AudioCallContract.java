package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.support.v4.app.Fragment;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.AudioRemotePeer;

/**
 * Created by muoi.pham on 20/07/18.
 */
public interface AudioCallContract {
    interface View extends BaseView<Presenter> {

        void onDisconnectUIChangeViewHandler();

        Fragment getFragmentViewHandler();

        void setRoomDetailsViewHandler(String roomDetails);

        void setAudioRemotePeerViewHandler(AudioRemotePeer audioRemotePeer);
    }

    interface Presenter extends BasePresenter {

        void connectToRoomPresenterHandler();

        void disconnectFromRoomPresenterHandler();

        boolean isConnectingOrConnectedPresenterHandler();

        void setAudioRemotePeerPresenterHandler(AudioRemotePeer audioRemotePeer);

        void onDisconnectUIChangePresenterHandler();

        Fragment getFragmentPresenterHandler();

        String getRoomDetailsPresenterHandler(boolean isPeerJoined);

        void setRoomDetailsPresenterHandler(String roomDetails);

        int getNumRemotePeersPresenterHandler();
    }

    interface Service extends BaseService<Presenter> {


    }
}

