package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose of this class is to handle muteVideoEvents
 * <p/>
 * Created by janidu on 6/5/15.
 */
class MuteVideoMessageProcessor implements MessageProcessor {

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {

        if (skylinkConnection.getMyConfig().hasVideoReceive()) {

            final String mid = jsonObject.getString("mid");
            final boolean muted = jsonObject.getBoolean("muted");

            if (!skylinkConnection.isPeerIdMCU(mid)) {

                skylinkConnection.runOnUiThread(new Runnable() {
                    public void run() {
                        // Prevent thread from executing with disconnect concurrently.
                        synchronized (skylinkConnection.getLockDisconnectMsg()) {
                            // If user has indicated intention to disconnect,
                            // We should no longer process messages from signalling server.
                            if (skylinkConnection.getSkylinkConnectionService().getConnectionState()
                                    == SkylinkConnectionService.ConnectionState.DISCONNECTING) {
                                return;
                            }
                            skylinkConnection.getMediaListener().onRemotePeerVideoToggle(mid, muted);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
