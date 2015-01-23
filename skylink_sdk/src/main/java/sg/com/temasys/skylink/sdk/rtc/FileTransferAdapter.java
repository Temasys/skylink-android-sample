package sg.com.temasys.skylink.sdk.rtc;

import sg.com.temasys.skylink.sdk.listener.FileTransferListener;

/**
 * @author temasys
 */
class FileTransferAdapter implements
        FileTransferListener {

    /**
     *
     */
    public FileTransferAdapter() {
    }

    @Override
    public void onRequest(String peerId, String fileName, boolean isPrivate) {
    }

    @Override
    public void onPermission(String peerId, String fileName, boolean isPermitted) {
    }

    @Override
    public void onDrop(String peerId, String fileName, String message,
                       boolean isExplicit) {
    }

    @Override
    public void onComplete(String peerId, String fileName, boolean isSending) {
    }

    @Override
    public void onProgress(String peerId, String fileName, double percentage,
                           boolean isSending) {
    }

}
