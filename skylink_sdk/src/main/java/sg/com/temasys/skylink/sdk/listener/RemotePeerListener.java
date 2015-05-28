package sg.com.temasys.skylink.sdk.listener;

/**
 * Listener comprises of callbacks related to the remote peers' activities.
 */
public interface RemotePeerListener {

    /**
     * This is triggered when a remote peer joins the room.
     *
     * @param remotePeerId   The id of the peer
     * @param userData       User defined data relating to the remote peer. May be a
     *                       'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     * @param hasDataChannel Whether peer has enabled DataChannel.
     */
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel);


    /**
     * This is triggered when an update is received in the user defined data of a remote peer.
     *
     * @param remotePeerId The id of the peer
     * @param userData     User defined data relating to the remote peer. May be a
     *                     'java.lang.String', 'org.json.JSONObject' or 'org.json.JSONArray'.
     */
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData);

    /**
     * This is triggered when the underlying peer to peer data connection is established with a
     * remote peer and is ready to send and receive peer messages and data (including files).
     *
     * @param remotePeerId The id of the remote peer
     */
    public void onOpenDataConnection(String remotePeerId);

    /**
     * This is triggered when a remote peer leaves the room.
     *
     * @param remotePeerId The id of the remote peer
     * @param message      Message specifying the possible reason for leaving the room.
     */
    public void onRemotePeerLeave(String remotePeerId, String message);

}
