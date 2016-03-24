package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logE;


/**
 * Purpose of this MessageProcessor is to handle redirect message types
 * <p/>
 * Created by janidu on 11/5/15.
 */
class RedirectMessageProcessor implements MessageProcessor {

    private static final String TAG = RedirectMessageProcessor.class.getName();

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(final JSONObject jsonObject) throws JSONException {

        final SignalingMessageProcessingService signalingMessageProcessingService =
                skylinkConnection.getSkylinkConnectionService().
                        getSignalingMessageProcessingService();

        skylinkConnection.runOnUiThread(new Runnable() {
            public void run() {
                boolean shouldDisconnect = false;
                // Prevent thread from executing with disconnect concurrently.
                synchronized (skylinkConnection.getLockDisconnectMsg()) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (skylinkConnection.getSkylinkConnectionService().getConnectionState()
                            == SkylinkConnectionService.ConnectionState.DISCONNECTING) {
                        return;
                    }
                    try {
                        shouldDisconnect = ProtocolHelper.processRedirect(
                                jsonObject.getString("info"),
                                jsonObject.getString("action"),
                                jsonObject.getString("reason"),
                                skylinkConnection.getLifeCycleListener());
                    } catch (JSONException e) {
                        String error = "[ERROR:" + Errors.HANDSHAKE_UNABLE_TO_READ_JSON_REDIRECT +
                                "] Disconnecting as we were unable to read an important Skylink server message!";
                        String debug = error + "\nDetails: Could not parse \"redirect\" JSON. Exception:\n" +
                                e.getMessage();
                        logE(TAG, error);
                        logD(TAG, debug);
                        shouldDisconnect = true;
                    }
                }
                if (shouldDisconnect) {
                    skylinkConnection.disconnectFromRoom();
                }
            }
        });

    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }

}
