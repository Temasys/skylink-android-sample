package sg.com.temasys.skylink.sdk.adapter;

import sg.com.temasys.skylink.sdk.listener.FileTransferListener;

/**
 * @author Temasys Communications Pte Ltd
 */
public class FileTransferAdapter implements
        FileTransferListener {

    /**
     *
     */
    public FileTransferAdapter() {
    }

    @Override
    public void onFileTransferPermissionRequest(String remotePeerId, String fileName, boolean isPrivate) {
    }

    @Override
    public void onFileTransferPermissionResponse(String remotePeerId, String fileName, boolean isPermitted) {
    }

    @Override
    public void onFileTransferDrop(String remotePeerId, String fileName, String message,
                                   boolean isExplicit) {
    }

    @Override
    public void onFileSendComplete(String remotePeerId, String fileName) {

    }

    @Override
    public void onFileReceiveComplete(String remotePeerId, String fileName) {

    }

    @Override
    public void onFileSendProgress(String remotePeerId, String fileName, double percentage) {

    }

    @Override
    public void onFileReceiveProgress(String remotePeerId, String fileName, double percentage) {

    }

}
