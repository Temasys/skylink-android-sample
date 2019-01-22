package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioCallContract;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class AudioService extends SkylinkCommonService implements AudioCallContract.Service {

    private boolean currentAudioOutput = Utils.getDefaultAudioSpeaker();

    public AudioService(Context mContext) {
        super(mContext);
    }

    @Override
    public void setPresenter(AudioCallContract.Presenter presenter) {
        mPresenter = (BasePresenter) presenter;
    }

    public void changeAudioOutput(boolean isAudioSpeaker) {
        AudioRouter.changeAudioOutput(mContext, isAudioSpeaker);
    }

    public String getRemotePeerName() {
        if (mPeersList != null && mPeersList.size() > 1)
            return mPeersList.get(1).getPeerName() + "(" + mPeersList.get(1).getPeerId() + ")";
        return "";
    }

    public boolean getCurrentAudioSpeaker() {
        return currentAudioOutput;
    }

    public void setCurrenAudioSpeaker(boolean isSpeakerOn) {
        currentAudioOutput = isSpeakerOn;
    }

    public void resumeAudioOutput() {
        changeAudioOutput(currentAudioOutput);
    }

    /**
     * Sets the specified listeners for audio function
     * Audio function needs to implement LifeCycleListener, RemotePeerListener, MediaListener, OsListener
     */
    @Override
    public void setSkylinkListeners() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.setLifeCycleListener(this);
            mSkylinkConnection.setRemotePeerListener(this);
            mSkylinkConnection.setMediaListener(this);
            mSkylinkConnection.setOsListener(this);
        }
    }

    /**
     * Get the config for audio function
     * User can custom audio config by using SkylinkConfig
     * */
    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // AudioVideo config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        skylinkConfig.setHasPeerMessaging(true);
        skylinkConfig.setHasFileTransfer(true);

        // Allow only 1 remote Peer to join.
        skylinkConfig.setMaxPeers(1); // Default is 4 remote Peers.

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    /**
     * Get the info of a peer in specific index
     */
    public SkylinkPeer getPeerByIndex(int index) {
        return mPeersList.get(index);
    }
}
