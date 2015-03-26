package sg.com.temasys.skylink.sdk.rtc;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;

/**
 * @author Temasys Communications Pte Ltd
 */
class LifeCycleAdapter implements LifeCycleListener {

    /**
     *
     */
    public LifeCycleAdapter() {
    }

    @Override
    public void onConnect(boolean isSuccessful, String message) {

    }

    @Override
    public void onWarning(int errorCode, String message) {

    }

    @Override
    public void onDisconnect(int errorCode, String message) {

    }

    @Override
    public void onReceiveLog(String message) {

    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {

    }

}
