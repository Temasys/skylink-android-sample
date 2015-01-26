package sg.com.temasys.skylink.sdk.listener;

/**
 * Listener comprises of callbacks upon receiving various messages from
 * peers.
 *
 * @author temasys
 */
public interface MessagesListener {

    /**
     * This is triggered when a custom broadcast or private message is
     * received from a peer via signaling channel.
     *
     * @param remotePeerId The id of the peer
     * @param message      User defined message. May be a 'java.lang.String',
     *                     'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param isPrivate    Flag to specify whether the message was broadcast to all
     *                     the peers
     */
    public void onServerMessageReceive(String remotePeerId, Object message,
                                       boolean isPrivate);

    /**
     * This is triggered when a broadcast or private peer message is
     * received via data channel.
     *
     * @param remotePeerId The id of the peer
     * @param message      User defined message. May be a 'java.lang.String',
     *                     'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param isPrivate    Flag to specify whether the message was broadcast to all
     *                     the peers
     */
    public void onP2PMessageReceive(String remotePeerId, Object message,
                                    boolean isPrivate);

}
