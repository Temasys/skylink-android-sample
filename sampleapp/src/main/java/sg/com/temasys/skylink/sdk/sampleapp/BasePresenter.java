package sg.com.temasys.skylink.sdk.sampleapp;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface BasePresenter {

    /**
     * process data to display on View
     */
    void onViewLayoutRequestedPresenterHandler(boolean tryToConnect);

    /**
     * process update view when connected to Skylink SDK
     */
    void onConnectPresenterHandler();

    /**
     * process update view when disconnect to Skylink SDK
     */
    void onDisconnectPresenterHandler();

    /**
     * process update view when remote peer joined the room
     */
    void onRemotePeerJoinPresenterHandler();

    /**
     * process update view when remote peer left the room
     */
    void onRemotePeerLeavePresenterHandler();
}
