package sg.com.temasys.skylink.sdk.listener;

/**
 * Listener comprises of callbacks related to the life cycle of the connection.
 */
public interface LifeCycleListener {

    /**
     * This is the first callback to specify whether the attempt to connect to the room was
     * successful.
     *
     * @param isSuccessful Specify success or failure
     * @param message      A message in case of isSuccessful is 'false' describing the reason of
     *                     failure
     */
    public void onConnect(boolean isSuccessful, String message);

    /**
     * This is triggered when the framework issues a warning to the client.
     *
     * @param errorCode @see sg.com.temasys.skylink.sdk.rtc.ErrorCodes
     * @param message   Warning message
     */
    public void onWarning(int errorCode, String message);

    /**
     * This is triggered when we disconnect from the room we were in.
     *
     * @param errorCode @see sg.com.temasys.skylink.sdk.rtc.ErrorCodes
     * @param message   Message specifying the reason for disconnection
     */
    public void onDisconnect(int errorCode, String message);

    /**
     * Occasionally the framework sends some messages for the client to intimate about certain
     * happenings.
     *
     * @param message Message from framework.
     */
    public void onReceiveLog(String message);

    /**
     * This is triggered when the lock status of the room that we are in changes
     *
     * @param remotePeerId The id of the peer that changed the lock status.
     * @param lockStatus   True if the room is locked & false if the room has been unlocked
     */
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus);

}
