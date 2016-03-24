package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logW;

/**
 * Purpose is to process room lock message types
 * <p/>
 * Created by janidu on 11/5/15.
 */
class RoomLockMessageProcessor implements MessageProcessor {

    private static final String TAG = RoomLockMessageProcessor.class.getName();

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(final JSONObject jsonObject) throws JSONException {

        final SignalingMessageProcessingService signalingMessageProcessingService =
                skylinkConnection.getSkylinkConnectionService().
                        getSignalingMessageProcessingService();

        this.skylinkConnection.runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (skylinkConnection.getLockDisconnectMsg()) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (skylinkConnection.getSkylinkConnectionService().getConnectionState() ==
                            SkylinkConnectionService.ConnectionState.DISCONNECTING) {
                        return;
                    }
                    try {
                        boolean roomLocked = ProtocolHelper.processRoomLockStatus(
                                skylinkConnection.isRoomLocked(), jsonObject.getString("mid"),
                                jsonObject.getBoolean("lock"),
                                skylinkConnection.getLifeCycleListener());
                        skylinkConnection.setRoomLocked(roomLocked);
                    } catch (JSONException e) {
                        String warn = "[WARN] Received a malformed room lock toggle message!" +
                                "Not using it!";
                        String debug = warn + "\nException: " + e.getMessage();
                        logW(TAG, warn);
                        logD(TAG, debug);
                    }
                }
            }
        });

    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
