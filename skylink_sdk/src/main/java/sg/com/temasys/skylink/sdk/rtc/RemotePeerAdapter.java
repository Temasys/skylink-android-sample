package sg.com.temasys.skylink.sdk.rtc;

import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;

/**
 * @author Temasys Communications Pte Ltd
 */
class RemotePeerAdapter implements RemotePeerListener {

    /**
     *
     */
    public RemotePeerAdapter() {
    }

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {

    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {

    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {

    }

    @Override
    public void onOpenDataConnection(String remotePeerId) {

    }

}
