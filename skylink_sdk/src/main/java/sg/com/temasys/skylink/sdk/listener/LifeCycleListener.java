package sg.com.temasys.skylink.sdk.listener;

/**
 * Listener comprises of callbacks related to the life cycle of the
 * connection.
 */
public interface LifeCycleListener {

    /**
     * This is the first callback to specify whether the connection was
     * successful.
     *
     * @param isSuccess Specify success or failure
     * @param message   A message in case of isSuccess is 'false' describing the
     *                  reason of failure
     */
    public void onConnect(boolean isSuccess, String message);

    /**
     * This is triggered when the framework issues a warning to the client.
     *
     * @param message Warning message
     */
    public void onWarning(String message);

    /**
     * This is triggered whenever the connection between the client and the
     * infrastructure drops.
     *
     * @param message Message specifying the reason for disconnection
     */
    public void onDisconnect(String message);

    /**
     * Occasionally the framework sends some messages for the client to
     * intimate about certain happenings.
     *
     * @param message Happening message
     */
    public void onReceiveLog(String message);

}
