package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.datatransfer.DataTransferContract;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class DataTransferService extends SkylinkCommonService implements DataTransferContract.Service {

    private static final String TAG = DataTransferService.class.getName();

    public DataTransferService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(DataTransferContract.Presenter presenter) {
        mPresenter = (BasePresenter) presenter;
    }

    public void sendData(String remotePeerId, byte[] data) {
        try {
            mSkylinkConnection.sendData(remotePeerId, data);
        } catch (SkylinkException e) {
            String log = e.getMessage();
            toastLogLong(TAG, mContext, log);
        } catch (UnsupportedOperationException e) {
            String log = e.getMessage();
            toastLogLong(TAG, mContext, log);
        }
    }

    @Override
    public void setListeners(SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setRemotePeerListener(this);
            skylinkConnection.setDataTransferListener(this);
        }
    }

    @Override
    public SkylinkConfig getSkylinkConfig(){
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // DataTransfer config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setHasDataTransfer(true);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

}
