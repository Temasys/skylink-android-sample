package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.support.v4.app.Fragment;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 * This interface is responsible for specify behaviors of View, Presenter, Service
 */

public interface AudioContract {

    interface View extends BaseView<Presenter> {

        /**
         * Get instance of the fragment for processing runtime audio permission
         */
        Fragment onPresenterRequestGetFragmentInstance();

        /**
         * Update audio output button UI
         */
        void onPresenterRequestChangeAudioOutput(boolean isSpeakerOn);

        /**
         * Update UI into connected state
         */
        void onPresenterRequestUpdateUIConnected(String roomId);

        /**
         * Update UI into disconnected state
         */
        void onPresenterRequestUpdateUIDisconnected();

        /**
         * Update UI when remote peer join the room
         */
        void onPresenterRequestChangeUIRemotePeerJoin(SkylinkPeer newPeer, int index);

        /**
         * Update UI details when remote peer left the room
         */
        void onPresenterRequestChangeUIRemotePeerLeft(List<SkylinkPeer> peersList);
    }

    interface Presenter {

        /**
         * process data to display on view at initiative connected
         */
        void onViewRequestConnectedLayout();

        /**
         * process permission result (grant/deny)
         */
        void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

        /**
         * process change audio output between headset and speaker
         */
        void onViewRequestChangeAudioOuput();

        /**
         * process change state when view exit/closed
         */
        void onViewRequestExit();

        /**
         * process get peer info at specific index
         */
        SkylinkPeer onViewRequestGetPeerByIndex(int index);

    }

    interface Service extends BaseService<Presenter> {

    }
}

