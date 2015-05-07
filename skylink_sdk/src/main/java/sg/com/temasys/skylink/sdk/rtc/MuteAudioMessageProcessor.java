package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles messages related to MuteAudio
 * <p/>
 * Created by janidu on 6/5/15.
 */
public class MuteAudioMessageProcessor implements MessageProcessor {

    private SkylinkConnection skylinkConnection;

    @Override
    public void process(JSONObject jsonObject) throws JSONException {
        if (skylinkConnection.getMyConfig().hasAudioReceive()) {

            final String mid = jsonObject.getString("mid");
            final boolean muted = jsonObject.getBoolean("muted");

            if (!skylinkConnection.isPeerIdMCU(mid)) {
                skylinkConnection.runOnUiThread(new Runnable() {
                    public void run() {
                        // Prevent thread from executing with disconnect concurrently.
                        synchronized (skylinkConnection.getLockDisconnect()) {
                            // If user has indicated intention to disconnect,
                            // We should no longer process messages from signalling server.
                            if (skylinkConnection.getConnectionState()
                                    == SkylinkConnection.ConnectionState.DISCONNECT) {
                                return;
                            }
                            skylinkConnection.getMediaListener().onRemotePeerAudioToggle(mid, muted);
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
