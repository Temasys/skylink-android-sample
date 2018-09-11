package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioCallContract;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class AudioService extends SkylinkCommonService implements AudioCallContract.Service {

    private static boolean currentAudioSpeaker = Utils.getDefaultAudioSpeaker();

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
            return mPeersList.get(1).getPeerName();
        return "";
    }

    public boolean getCurrentAudioSpeaker() {
        return currentAudioSpeaker;
    }

    public void setCurrenAudioSpeaker(boolean isSpeakerOn) {
        currentAudioSpeaker = isSpeakerOn;
    }

    public void resumeAudioOutput() {
        changeAudioOutput(currentAudioSpeaker);
    }

    @Override
    public void setListeners(SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setRemotePeerListener(this);
            skylinkConnection.setMediaListener(this);
            skylinkConnection.setOsListener(this);
        }
    }

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
}
