package sg.com.temasys.skylink.sdk.sampleapp;

import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface BaseService<T> {

    /**
     * set connection between service and presenter
     */
    void setPresenter(T presenter);

    /**
     * set connection between service and presenter
     */
    void setTypeCall();

    /**
     * make connection with SkylinkSDK
     */
    void connectToRoomServiceHandler();

    /**
     * close connection with SkylinkSDK
     */
    void disconnectFromRoomServiceHandler();

    /**
     * check connection state with Skylink SDK
     */
    boolean isConnectingOrConnectedServiceHandler();



}
