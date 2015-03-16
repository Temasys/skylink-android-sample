package sg.com.temasys.skylink.sdk.listener;

/**
 * Listener comprises of callbacks related to DataTransfer.
 */
public interface DataTransferListener {

    /**
     * This is triggered when data is received
     *
     * @param remotePeerId The id of the peer
     * @param data         Array of bytes
     */
    public void onDataReceive(String remotePeerId, byte[] data);
}
