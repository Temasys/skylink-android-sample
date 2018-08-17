package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioCallContract;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class AudioService extends SDKService implements AudioCallContract.Service {

    public AudioService(Context mContext) {
        super(mContext);
    }

    @Override
    public void setPresenter(AudioCallContract.Presenter presenter) {
        mAudioPresenter = presenter;
    }

    @Override
    public void setTypeCall(){
        mTypeCall = Constants.CONFIG_TYPE.AUDIO;
    }
}
