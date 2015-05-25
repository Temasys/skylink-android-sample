package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by janidu on 12/5/15.
 */
class UpdateUserEventMessageProcessor implements MessageProcessor {

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {
        final String mid = jsonObject.getString("mid");
        final Object userData = jsonObject.get("userData");
        if (!skylinkConnection.isPeerIdMCU(mid)) {
            skylinkConnection.runOnUiThread(new Runnable() {
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (skylinkConnection.getLockDisconnect()) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (skylinkConnection.getConnectionState() ==
                                SkylinkConnection.ConnectionState.DISCONNECT) {
                            return;
                        }

                        skylinkConnection.setUserData(mid, userData);
                        skylinkConnection.getRemotePeerListener()
                                .onRemotePeerUserDataReceive(mid, userData);
                    }
                }
            });
        }
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
