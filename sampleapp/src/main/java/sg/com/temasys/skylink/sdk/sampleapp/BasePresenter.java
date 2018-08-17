package sg.com.temasys.skylink.sdk.sampleapp;

import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface BasePresenter {

    /**
     * process data to display on View
     */
    void onViewLayoutRequested();

    /**
     * process update view when connected to Skylink SDK
     */
    void onConnect(boolean isSuccessful);

    /**
     * process update view when disconnect from Skylink SDK
     */
    void onDisconnect();

    /**
     * process disconnect from room when view exit
     */
    void onViewExit();

    /**
     * process update view when remote peer joined the room
     */
    void onRemotePeerJoin(SkylinkPeer newPeer);

    /**
     * process update view when remote peer left the room
     */
    void onRemotePeerLeave(String remotePeerId);

}
