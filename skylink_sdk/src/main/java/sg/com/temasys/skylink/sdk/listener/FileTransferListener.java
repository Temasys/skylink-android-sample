package sg.com.temasys.skylink.sdk.listener;

/**
 * Listener comprises of callbacks related to file transfer operation.
 */
public interface FileTransferListener {

    /**
     * This is triggered upon receiving a file transfer request from a peer.
     *
     * @param remotePeerId The id of the peer
     * @param fileName     The name of the file
     * @param isPrivate    Flag to specify if file share was directed to only us (as opposed to all
     *                     peers in the room).
     */
    public void onFileTransferPermissionRequest(String remotePeerId,
                                                String fileName, boolean isPrivate);

    /**
     * This is triggered upon receiving the response of a peer to our request for file transfer with
     * the peer.
     *
     * @param remotePeerId The id of the peer
     * @param fileName     The name of the file
     * @param isPermitted  Flag to specify whether the peer has accepted the request
     */
    public void onFileTransferPermissionResponse(String remotePeerId, String fileName,
                                                 boolean isPermitted);

    /**
     * This is triggered when an ongoing file transfer drops due to some reason.
     *
     * @param remotePeerId The id of the peer
     * @param fileName     The name of the file
     * @param message      Message that possibly tells the reason for dropping
     * @param isExplicit   True if user canceled the transfer explicitly, as opposed to being due to
     *                     error in the transfer process.
     */
    public void onFileTransferDrop(String remotePeerId, String fileName, String message,
                                   boolean isExplicit);

    /**
     * This is triggered when we have sent a file successfully.
     *
     * @param remotePeerId The id of the peer receiving the file.
     * @param fileName     The name of the file
     */
    public void onFileSendComplete(String remotePeerId, String fileName);

    /**
     * This is triggered when we have received a file successfully.
     *
     * @param remotePeerId The id of the peer
     * @param fileName     The name of the file
     */
    public void onFileReceiveComplete(String remotePeerId, String fileName);

    /**
     * This is triggered timely to report the on going progress when sending a file
     *
     * @param remotePeerId The id of the peer receiving the file.
     * @param fileName     The name of the file
     * @param percentage   The percentage completed
     */
    public void onFileSendProgress(String remotePeerId, String fileName,
                                   double percentage);

    /**
     * This is triggered timely to report the on going progress when receiving a file
     *
     * @param remotePeerId The id of the peer sending the file.
     * @param fileName     The name of the file
     * @param percentage   The percentage completed
     */
    public void onFileReceiveProgress(String remotePeerId, String fileName,
                                      double percentage);
}
