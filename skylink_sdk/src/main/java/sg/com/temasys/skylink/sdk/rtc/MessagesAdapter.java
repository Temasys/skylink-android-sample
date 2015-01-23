package sg.com.temasys.skylink.sdk.rtc;

import sg.com.temasys.skylink.sdk.listener.MessagesListener;

/**
 * @author temasys
 */
class MessagesAdapter implements MessagesListener {

    /**
     *
     */
    public MessagesAdapter() {
    }

    @Override
    @Deprecated
    public void onChatMessage(String peerId, String nick, String message,
                              boolean isPrivate) {

    }

    @Override
    public void onCustomMessage(String peerId, Object message, boolean isPrivate) {

    }

    @Override
    public void onPeerMessage(String peerId, Object message, boolean isPrivate) {

    }

}
