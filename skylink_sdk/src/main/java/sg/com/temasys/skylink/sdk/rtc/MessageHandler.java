package sg.com.temasys.skylink.sdk.rtc;

/**
 * Callback interface for messages delivered on the Google AppEngine channel.
 * <p/>
 * Methods are guaranteed to be invoked on the UI thread of |activity| passed
 * to GAEChannelClient's constructor.
 */
interface MessageHandler {
    void onOpen();

    void onMessage(String data);

    void onClose();

    void onError(int code, String description);
}
