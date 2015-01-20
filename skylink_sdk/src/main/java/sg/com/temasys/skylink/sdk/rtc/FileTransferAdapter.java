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
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onRequest(String peerId, String fileName, boolean isPrivate) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPermission(String peerId, String fileName, boolean isPermitted) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDrop(String peerId, String fileName, String message,
                       boolean isExplicit) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onComplete(String peerId, String fileName, boolean isSending) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProgress(String peerId, String fileName, double percentage,
                           boolean isSending) {
        // TODO Auto-generated method stub

    }

}
