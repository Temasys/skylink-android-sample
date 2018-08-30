package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioCallContract;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class AudioService extends SDKService implements AudioCallContract.Service {

    private static boolean currentAudioSpeaker = Utils.getDefaultAudioSpeaker();

    public AudioService(Context mContext) {
        super(mContext);
    }

    @Override
    public void setPresenter(AudioCallContract.Presenter presenter) {
        mAudioPresenter = presenter;
    }

    @Override
    public void setTypeCall() {
        mTypeCall = Constants.CONFIG_TYPE.AUDIO;
    }

    public void changeAudioOutput(boolean isAudioSpeaker) {
        AudioRouter.changeAudioOutput(mContext, isAudioSpeaker);
    }

    public String getRemotePeerName() {
        if(mPeersList.size()>1)
            return mPeersList.get(1).getPeerName();
        return "";
    }

    public boolean getCurrentAudioSpeaker() {
        return currentAudioSpeaker;
    }

    public void setCurrenAudioSpeaker(boolean isSpeakerOn) {
        currentAudioSpeaker = isSpeakerOn;
    }
}
