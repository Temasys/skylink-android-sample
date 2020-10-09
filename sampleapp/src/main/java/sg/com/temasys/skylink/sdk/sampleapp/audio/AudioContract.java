package sg.com.temasys.skylink.sdk.sampleapp.audio;

import androidx.fragment.app.Fragment;

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
        Fragment getInstance();

        /**
         * Update UI into connected state
         */
        void updateUIConnected(String roomId);

        /**
         * Update UI into disconnected state
         */
        void updateUIDisconnected();

        /**
         * Update UI when remote peer join the room
         */
        void updateUIRemotePeerConnected(SkylinkPeer newPeer, int index);

        /**
         * Update UI details when remote peer left the room
         */
        void updateUIRemotePeerDisconnected(List<SkylinkPeer> peersList);

        /**
         * Update audio output button UI
         */
        void updateUIAudioOutputChanged(boolean isSpeakerOn);
    }

    interface Presenter {

        /**
         * process data to display on view at initiative connected
         */
        void processConnectedLayout();

        /**
         * process permission result (grant/deny)
         */
        void processPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

        /**
         * process change audio output between headset and speaker
         */
        void processChangeAudioOutput();

        /**
         * process change state when view exit/closed
         */
        void processExit();

        /**
         * process get peer info at specific index
         */
        SkylinkPeer processGetPeerByIndex(int index);

    }

    interface Service extends BaseService<Presenter> {

    }
}

