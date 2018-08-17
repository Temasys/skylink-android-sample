package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.datatransfer.DataTransferContract;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class DataTransferService extends SDKService implements DataTransferContract.Service {

    private static final String TAG = DataTransferService.class.getName();

    public DataTransferService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(DataTransferContract.Presenter presenter) {
        mDataPresenter = presenter;
    }

    @Override
    public void setTypeCall() {
        mTypeCall = Constants.CONFIG_TYPE.DATA;
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

}
