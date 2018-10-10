package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.util.Log;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.chat.ChatContract;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class ChatService extends SkylinkCommonService implements ChatContract.Service {

    private final String TAG = ChatService.class.getName();

    public ChatService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(ChatContract.Presenter presenter) {
        mPresenter = (BasePresenter) presenter;
    }

    public void sendServerMessage(String remotePeerId, String message) {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.sendServerMessage(remotePeerId, message);
        }
    }

    public void sendP2PMessage(String remotePeerId, String message) {
        if (mSkylinkConnection != null) {
            try {
                mSkylinkConnection.sendP2PMessage(remotePeerId, message);
            } catch (SkylinkException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    @Override
    public void setSkylinkListeners() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.setLifeCycleListener(this);
            mSkylinkConnection.setRemotePeerListener(this);
            mSkylinkConnection.setMessagesListener(this);
        }
    }

    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // Chat config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setHasPeerMessaging(true);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

}
