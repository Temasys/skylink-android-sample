package sg.com.temasys.skylink.sdk.rtc;

import sg.com.temasys.skylink.sdk.listener.MessagesListener;

/**
 * @author temasys
 */
public class MessagesAdapter implements MessagesListener {

    /**
     *
     */
    public MessagesAdapter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    @Deprecated
    public void onChatMessage(String peerId, String nick, String message,
                              boolean isPrivate) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCustomMessage(String peerId, Object message, boolean isPrivate) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPeerMessage(String peerId, Object message, boolean isPrivate) {
        // TODO Auto-generated method stub

    }

}
