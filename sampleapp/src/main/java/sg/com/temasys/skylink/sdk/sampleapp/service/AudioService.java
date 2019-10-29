package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkError;
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioCallContract;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for communicating with SkylinkSDK
 */

public class AudioService extends SkylinkCommonService implements AudioCallContract.Service {

    public AudioService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(AudioCallContract.Presenter presenter) {
        this.presenter = (BasePresenter) presenter;
    }

    /**
     * Get the info of a peer in specific index
     */
    public SkylinkPeer getPeerByIndex(int index) {
        return mPeersList.get(index);
    }

    /**
     * Sets the specified listeners for audio function
     * Audio function needs to implement LifeCycleListener, RemotePeerListener, MediaListener, OsListener
     */
    @Override
    public void setSkylinkListeners() {
        if (skylinkConnection != null) {
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setRemotePeerListener(this);
            skylinkConnection.setMediaListener(this);
            skylinkConnection.setOsListener(this);
        }
    }

    /**
     * Get the config for audio function
     * User can custom audio config by using SkylinkConfig
     */
    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // AudioVideo config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        skylinkConfig.setP2PMessaging(false);
        skylinkConfig.setFileTransfer(false);

        // Allow only 1 remote Peer to join.
        skylinkConfig.setMaxRemotePeersConnected(1, SkylinkConfig.AudioVideoConfig.AUDIO_ONLY); // Default is 4 remote Peers.

        // Set the room size
        skylinkConfig.setSkylinkRoomSize(SkylinkConfig.SkylinkRoomSize.SMALL);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);

        // set enable multitrack to false to interop with JS-SDK
        // skylinkConfig.setMultitrackCreateEnable(false);

        return skylinkConfig;
    }

    /**
     * Start local audio in order to communicate with the remote peer
     */
    public void createLocalAudio() {
        if (skylinkConnection != null) {
            skylinkConnection.createLocalMedia(SkylinkConfig.AudioDevice.MICROPHONE, "local audio mic", new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog("AudioService", context, "\"Unable to createLocalAudio as " + contextDescription);
                }
            });
        }
    }

    public void disposeLocalMedia() {
        clearInstance();
    }
}
