package sg.com.temasys.skylink.sdk.adapter;

import sg.com.temasys.skylink.sdk.listener.MessagesListener;

/**
 * @author Temasys Communications Pte Ltd
 */
public class MessagesAdapter implements MessagesListener {

    /**
     *
     */
    public MessagesAdapter() {
    }

    @Override
    public void onServerMessageReceive(String remotePeerId, Object message, boolean isPrivate) {

    }

    @Override
    public void onP2PMessageReceive(String remotePeerId, Object message, boolean isPrivate) {

    }

}
