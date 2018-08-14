package sg.com.temasys.skylink.sdk.sampleapp;

import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.MultiPeersInfo;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface BasePresenter {

    /**
     * process data to display on View
     */
    void onViewLayoutRequestedPresenterHandler();

    /**
     * process update view when connected to Skylink SDK
     */
    void onConnectPresenterHandler(boolean isSuccessful);

    /**
     * process update view when disconnect from Skylink SDK
     */
    void onDisconnectPresenterHandler();

    /**
     * process disconnect from room when view exit
     */
    void onViewExitPresenterHandler();

    /**
     * process update view when remote peer joined the room
     * @param newPeer
     */
    void onRemotePeerJoinPresenterHandler(SkylinkPeer newPeer);

    /**
     * process update view when remote peer left the room
     * @param remotePeerId
     */
    void onRemotePeerLeavePresenterHandler(String remotePeerId);



}
