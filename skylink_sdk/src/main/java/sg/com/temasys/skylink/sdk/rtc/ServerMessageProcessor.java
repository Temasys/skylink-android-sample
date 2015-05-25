package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose of this MessageProcessor is to handle ServerMessage Types
 * <p/>
 * Created by janidu on 11/5/15.
 */
class ServerMessageProcessor implements MessageProcessor {

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(final JSONObject jsonObject) throws JSONException {

        final Object data = jsonObject.get("data");
        final String mid = jsonObject.getString("mid");
        final String type = jsonObject.getString("type");

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

                        skylinkConnection.getMessagesListener().onServerMessageReceive(mid,
                                data, type.equals("private"));
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
