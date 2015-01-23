package sg.com.temasys.skylink.sdk.listener;

/**
 * Listener comprises of callbacks upon receiving various messages from
 * peers.
 *
 * @author temasys
 */
public interface MessagesListener {

    /**
     * This is triggered when a text message is received from a peer. This
     * functionality is deprecated and will be removed eventually. One may
     * continue to use 'onCustomMessage'.
     *
     * @param peerId    The id of the peer.
     * @param nick      The nick of the peer
     * @param message   The message itself
     * @param isPrivate Flag to specify whether the message was broadcast to all
     *                  the peers
     */
    @Deprecated
    public void onChatMessage(String peerId, String nick, String message,
                              boolean isPrivate);

    /**
     * This is triggered when a custom broadcast or private message is
     * received from a peer via signaling channel.
     *
     * @param peerId    The id of the peer
     * @param message   User defined message. May be a 'java.lang.String',
     *                  'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param isPrivate Flag to specify whether the message was broadcast to all
     *                  the peers
     */
    public void onCustomMessage(String peerId, Object message,
                                boolean isPrivate);

    /**
     * This is triggered when a broadcast or private peer message is
     * received via data channel.
     *
     * @param peerId    The id of the peer
     * @param message   User defined message. May be a 'java.lang.String',
     *                  'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param isPrivate Flag to specify whether the message was broadcast to all
     *                  the peers
     */
    public void onPeerMessage(String peerId, Object message,
                              boolean isPrivate);

}
