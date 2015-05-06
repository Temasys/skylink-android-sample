package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose of this class is to handle bye message types
 * <p/>
 * Created by janidu on 5/5/15.
 */
public class ByeMessageProcessor implements MessageProcessor {

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {

        // Ignoring targeted bye
        if (jsonObject.has("target")) {
            return;
        }

        final String mid = jsonObject.getString("mid");

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
                        skylinkConnection.getRemotePeerListener()
                                .onRemotePeerLeave(mid, "The peer has left the room");
                    }
                }
            });
        }

        DataChannelManager dataChannelManager = skylinkConnection.getDataChannelManager();

        // Dispose DataChannel.
        if (dataChannelManager != null) {
            dataChannelManager.disposeDC(mid);
        }

        ProtocolHelper.disposePeerConnection(mid, skylinkConnection);
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
