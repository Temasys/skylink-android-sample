package sg.com.temasys.skylink.sdk.listener;

/**
 * Listener comprises of callbacks related to file transfer operation.
 */
public interface FileTransferListener {

    /**
     * This is triggered upon receiving a file transfer request from a peer.
     *
     * @param peerId   The id of the peer
     * @param fileName The name of the file
     */
    public void onRequest(String peerId, String fileName, boolean isPrivate);

    /**
     * This is triggered upon receiving the response of a file transfer
     * request from a peer.
     *
     * @param peerId      The id of the peer
     * @param fileName    The name of the file
     * @param isPermitted Flag to specify whether the peer has accepted the request
     */
    public void onPermission(String peerId, String fileName,
                             boolean isPermitted);

    /**
     * This is triggered when an ongoing file transfer drops due to some
     * reason
     *
     * @param peerId   The id of the peer
     * @param fileName The name of the file
     * @param message  Message that possibly tells the reason for dropping
     */
    public void onDrop(String peerId, String fileName, String message,
                       boolean isExplicit);

    /**
     * This is triggered when a file transfer is completed successfully.
     *
     * @param peerId    The id of the peer
     * @param fileName  The name of the file
     * @param isSending Flag to specify whether the completed transfer is for
     *                  sending or receiving a file
     */
    public void onComplete(String peerId, String fileName, boolean isSending);

    /**
     * This is triggered timely to report the on going file transfer
     * progress.
     *
     * @param peerId     The id of the peer
     * @param fileName   The name of the file
     * @param percentage The percentage completed
     * @param isSending  Flag to specify whether the completed transfer is for
     *                   sending or receiving a file
     */
    public void onProgress(String peerId, String fileName,
                           double percentage, boolean isSending);

}
