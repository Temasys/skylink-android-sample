package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.util.Log;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkError;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.datatransfer.DataTransferContract;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for communicating with SkylinkSDK
 */

public class DataTransferService extends SkylinkCommonService implements DataTransferContract.Service {

    private final String TAG = DataTransferService.class.getName();

    public DataTransferService(Context context) {
        super(context);
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
     * @throws SkylinkException When byte array is not of a size allowed.
     */
    public void sendData(String remotePeerId, byte[] data) throws SkylinkException {
        if (mSkylinkConnection == null)
            return;

        final boolean[] success = {true};
        mSkylinkConnection.sendData(remotePeerId, data, new SkylinkCallback() {
            @Override
            public void onError(SkylinkError error, String contextDescription) {
                Log.e("SkylinkCallback", contextDescription);
                success[0] = false;
            }
        });

        if (!success[0]) {
            String error = "Unable to sendData!";
            Utils.toastLog(TAG, context, error);
        }
    }

    /**
     * Sets the specified listeners for data transfer function
     * Data transfer function needs to implement LifeCycleListener, RemotePeerListener, DataTransferListener
     */
    @Override
    public void setSkylinkListeners() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.setLifeCycleListener(this);
            mSkylinkConnection.setRemotePeerListener(this);
            mSkylinkConnection.setDataTransferListener(this);
        }
    }

    /**
     * Get the config for data transfer function
     * User can custom data transfer config by using SkylinkConfig
     */
    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // DataTransfer config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setDataTransfer(true);

        // Set the room size
        skylinkConfig.setSkylinkRoomSize(SkylinkConfig.SkylinkRoomSize.MEDIUM);

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
