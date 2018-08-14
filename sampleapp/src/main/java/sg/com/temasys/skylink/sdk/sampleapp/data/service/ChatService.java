package sg.com.temasys.skylink.sdk.sampleapp.data.service;

import android.content.Context;
import android.util.Log;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.MultiPeersInfo;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.chat.ChatContract;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class ChatService extends SDKService implements ChatContract.Service {

    private final String TAG = ChatService.class.getName();

    public ChatService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(ChatContract.Presenter presenter) {
        mChatPresenter = presenter;
    }

    @Override
    public void setTypeCall(){
        mTypeCall = Constants.CONFIG_TYPE.CHAT;
    }

    public void sendServerMessageServiceHandler(String remotePeerId, String message) {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.sendServerMessage(remotePeerId, message);
        }
    }

    public void sendP2PMessageServiceHandler(String remotePeerId, String message) {
        if (mSkylinkConnection != null) {
            try {
                mSkylinkConnection.sendP2PMessage(remotePeerId, message);
            } catch (SkylinkException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    public MultiPeersInfo getPeersListServiceHandler() {
        return mPeersList;
    }

    public int getTotalPeersInRoomServiceHandler() {
        if(mPeersList == null)
            return 0;

        return mPeersList.getSize();
    }


}
