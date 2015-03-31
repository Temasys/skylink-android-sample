package sg.com.temasys.skylink.sdk.listener;

/**
 * Listener comprises of callbacks upon receiving various messages from remote peers.
 *
 * @author Temasys Communications Pte Ltd
 */
public interface MessagesListener {

    /**
     * This is triggered when a custom broadcast or private message is received from a remote peer
     * via a server.
     *
     * @param remotePeerId The id of the remote peer
     * @param message      User defined message. May be a 'java.lang.String', 'org.json.JSONObject'
     *                     or 'org.json.JSONArray'.
     * @param isPrivate    Flag to specify whether the message was sent to us only (as opposed to
     *                     broadcast to all peers in the room).
     */
    public void onServerMessageReceive(String remotePeerId, Object message,
                                       boolean isPrivate);

    /**
     * This is triggered when a broadcast or private peer message is received directly from the
     * peer, i.e. in a peer to peer manner.
     *
     * @param remotePeerId The id of the remote peer
     * @param message      User defined message. May be a 'java.lang.String', 'org.json.JSONObject'
     *                     or 'org.json.JSONArray'.
     * @param isPrivate    Flag to specify whether the message was sent to us only (as opposed to
     *                     broadcast to all peers in the room).
     */
    public void onP2PMessageReceive(String remotePeerId, Object message,
                                    boolean isPrivate);

}
