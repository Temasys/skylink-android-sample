package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkError;
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.datatransfer.DataTransferContract;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for communicating with SkylinkSDK
 */

public class DataTransferService extends SkylinkCommonService implements DataTransferContract.Service {

    private final String TAG = DataTransferService.class.getName();

    private final int MAX_REMOTE_PEER = 7;

    public DataTransferService(Context context) {
        super(context);
        initializeSkylinkConnection(Constants.CONFIG_TYPE.DATA);
    }

    @Override
    public void setPresenter(DataTransferContract.Presenter presenter) {
        this.presenter = (BasePresenter) presenter;
    }

    /**
     * Sends a byte array to a specified remotePeer or to all participants of the room if the
     * remotePeerId is null
     * The byte array cannot be null, and its maximum size is 65456 bytes.
     * Notes:
     * - This operation is currently not supported with Skylink Media Relay.
     * - This operation is currently only supported between Skylink Mobile SDKs.
     *
     * @param remotePeerId remotePeerID of a specified peer
     * @param data         Array of bytes
     */
    public void sendData(String remotePeerId, byte[] data) {
        if (skylinkConnection == null)
            return;

        skylinkConnection.sendData(data, remotePeerId, new SkylinkCallback() {
            @Override
            public void onError(SkylinkError error, HashMap<String, Object> details) {
                String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                Log.e("SkylinkCallback", contextDescription);
                Utils.toastLog(TAG, context, "\"Unable to sendData as " + contextDescription);
            }
        });
    }

    /**
     * Sets the specified listeners for data transfer function
     * Data transfer function needs to implement LifeCycleListener, RemotePeerListener, DataTransferListener
     */
    @Override
    public void setSkylinkListeners() {
        if (skylinkConnection != null) {
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setRemotePeerListener(this);
            skylinkConnection.setDataTransferListener(this);
        }
    }

    /**
     * Get the config for data transfer function
     * User can custom data transfer config by using SkylinkConfig
     */
    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // Set some common configs base on the default setting on the setting page
        Utils.skylinkConfigCommonOptions(skylinkConfig);

        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);

        skylinkConfig.setSkylinkRoomSize(SkylinkConfig.SkylinkRoomSize.LARGE);

        int maxRemotePeer = Utils.getDefaultMaxPeerInNoMediaRoomConfig();
        skylinkConfig.setMaxRemotePeersConnected(maxRemotePeer, SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);

        skylinkConfig.setDataTransfer(true);

        return skylinkConfig;
    }

    /**
     * Get the info of a peer in specific index
     */
    public SkylinkPeer getPeerByIndex(int index) {
        return mPeersList.get(index);
    }

    public void disposeLocalMedia() {
        clearInstance();
    }
}
