package sg.com.temasys.skylink.sdk.rtc;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.PeerConnection;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import sg.com.temasys.skylink.sdk.BuildConfig;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logE;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logI;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logW;

class DataChannelManager {

    private SkylinkConnection connectionManager;

    private static final String TAG = "DataChannelManager";
    public static final int MAX_TRANSFER_SIZE = 65456;

    // Hashtable with Keys of tid (target/remote Peer id)
    // and Values of registered DcObserver.
    private Hashtable<String, DcObserver> dcInfoList = new Hashtable<String, DcObserver>();

    // List of DataChannel types
    public enum DcType {
        CONN, MESSAGE, WRQ, ACK, ERROR, CANCEL
    }

    // UserAgent string.
    private final String agentStr = "Android";
    // Maximum size (bytes) of chunk in each transfer.
    private int CHUNK_SIZE_POST = 65536;
    private int CHUNK_SIZE_PRE = CHUNK_SIZE_POST * 6 / 8;

    // Time out
    // Time out duration (seconds) for file transfers.
    // Will be over written by config if config's is valid.
    private int TIMEOUT = 60;
    // List of DcHandler for each DcObserver (unique for each DataChannel)
    private Hashtable<DcObserver, DcHandler> dcHandlerList = new Hashtable<DcObserver, DcHandler>();

    // Self
    private String mid = "";
    private String displayName = "";
    private boolean hasPeerMessaging;
    // PeerId of Peer receiving file or data from us.
    // Null if
    private String peerRecv = "";
    // PeerId of Peer sending file or data to us.
    private String peerSend = "";

    @SuppressWarnings("unused")
    private boolean hasFileTransfer;

    // MCU
    // If room is supported by MCU (i.e. a MCU room).
    private boolean isMcuRoom;
    // MCU specific variables when sending DC Message in a MCU room.
    // Set at createDataChannel for MCU.
    private DataChannel dcMcu;
    private DcObserver dcObsMcu;
    private String tidMcu;

    public DataChannelManager(SkylinkConnection connectionManager, int timeout,
                              boolean hasPeerMessaging, boolean hasFileTransfer) {
        this.connectionManager = connectionManager;
        if (timeout > 0) TIMEOUT = timeout;
        this.hasPeerMessaging = hasPeerMessaging;
        this.hasFileTransfer = hasFileTransfer;
    }

// -------------------------------------------------------------------------------------------------
// Set and get methods
// -------------------------------------------------------------------------------------------------

    public SkylinkConnection getConnectionManager() {
        return connectionManager;
    }

    public String getMid() {
        return mid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean getIsMcuRoom() {
        return isMcuRoom;
    }

    public void setConnectionManager(SkylinkConnection connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setIsMcuRoom(boolean isMcuRoom) {
        this.isMcuRoom = isMcuRoom;
    }

// -------------------------------------------------------------------------------------------------
// Inner classes
// -------------------------------------------------------------------------------------------------


    /**
     * Note: DcObserver allows us to implement the DataChannel methods, i.e. - onStateChange() -
     * onMessage()
     *
     * @param {String}         createId - User id of the one creating the DataChannel
     * @param {String}         receiveId - User id of the one receiving the DataChannel
     * @param {String}         channelName - The Name of the Channel. If null, it would be
     *                         generated
     * @param {RTCDataChannel} dc - The DataChannel object passed inside
     * @class DcObserver
     */
    private class DcObserver implements DataChannel.Observer {

        // Add createId and receiveId so as to be able to use these values in messaging, etc.
        // private PeerConnection pc;
        private String createId;
        private String receiveId;
        private String tid;
        private DataChannel dc;

        // Timer for handling timeouts
        private DcHandler dcHandler = null;
        // 0.5.5 MCU only supports 1 send operation at a time.
        // This flag tracks if there is a current send operation ongoing.
        private boolean nowSending = false;

        private String fileName = ""; // This is populated when sending WRQ.
        private String filePath = ""; // This is populated when sending WRQ.
        private String saveFileName = ""; // This is populated when getting WRQ.
        private String saveFilePath = ""; // This is populated only when file transfer is accepted.
        private long fileSize = 0; // file size in bytes.
        private long fileSizeEncoded = 0; // file size in bytes, after base64 encoding.
        private long saveFileSizeEncoded = 0; // When saving, file size in bytes, after base64 encoding.
        private long chunk = 0; // Chunk number of save file currently being requested.
        private long chunkSizePre = 0; // When saving, packet size before base64 encoding in bytes.
        private long chunkSizePost = 0; // When saving, packet size after base64 encoding in bytes.

        // Add a constructor that takes in createId, receiveId, and sets them.
        public DcObserver(String createId, String receiveId, String tid) {
            this.createId = createId;
            this.receiveId = receiveId;
            this.tid = tid;
            getDcHandler();
        }

        @SuppressWarnings("unused")
        public String getCreateId() {
            return createId;
        }

        @SuppressWarnings("unused")
        public String getReceiveId() {
            return receiveId;
        }

        public String getTid() {
            return tid;
        }

        public DataChannel getDc() {
            return dc;
        }

        public DcHandler getDcHandler() {
            // handler must be created in thread that has called Looper.prepare().
            connectionManager.runOnUiThread(new Runnable() {
                public void run() {
                    if (dcHandler == null) dcHandler = new DcHandler();
                }
            });
            return dcHandler;
        }

        public void setDcHandler(DcHandler dcHandler) {
            this.dcHandler = dcHandler;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getSaveFileName() {
            return saveFileName;
        }

        public String getSaveFilePath() {
            return saveFilePath;
        }

        public long getFileSize() {
            return fileSize;
        }

        public long getFileSizeEncoded() {
            return fileSizeEncoded;
        }

        public long getSaveFileSizeEncoded() {
            return saveFileSizeEncoded;
        }

        public long getChunkSizePre() {
            return chunkSizePre;
        }

        public long getChunkSizePost() {
            return chunkSizePost;
        }

        public long getChunk() {
            return chunk;
        }

        public boolean getNowSending() {
            return nowSending;
        }

        //  @SuppressWarnings("unused")
        // public void setPc( PeerConnection _pc ) { pc = _pc; }
        @SuppressWarnings("unused")
        public void setCreateId(String _createId) {
            createId = _createId;
        }

        @SuppressWarnings("unused")
        public void setReceiveId(String _receiveId) {
            receiveId = _receiveId;
        }

        @SuppressWarnings("unused")
        public void setTid(String _tid) {
            tid = _tid;
        }

        public void setDc(DataChannel _dc) {
            dc = _dc;
        }

        public void setFileName(String _fileName) {
            fileName = _fileName;
        }

        public void setFilePath(String _filePath) {
            filePath = _filePath;
        }

        public void setSaveFileName(String _saveFileName) {
            saveFileName = _saveFileName;
        }

        public void setSaveFilePath(String _saveFilePath) {
            saveFilePath = _saveFilePath;
        }

        public void setFileSize(long _fileSize) {
            fileSize = _fileSize;
        }

        public void setFileSizeEncoded(long _fileSizeEncoded) {
            fileSizeEncoded = _fileSizeEncoded;
        }

        public void setSaveFileSizeEncoded(long _saveFileSizeEncoded) {
            saveFileSizeEncoded = _saveFileSizeEncoded;
        }

        public void setChunkSizePre(long _chunkSizePre) {
            chunkSizePre = _chunkSizePre;
        }

        public void setChunkSizePost(long _chunkSizePost) {
            chunkSizePost = _chunkSizePost;
        }

        public void setChunk(long _chunk) {
            chunk = _chunk;
        }

        public void setNowSending(boolean _nowSending) {
            nowSending = _nowSending;
        }

        /**
         * Triggered when a new ICE candidate has been found.
         */
        // Send ICE candidates.
        @Override
        public void onMessage(final DataChannel.Buffer buffer) {
            final byte[] bytes = new byte[buffer.data.capacity()];
            buffer.data.get(bytes);

            if (!buffer.binary) {
                String dataStr = new String(bytes);
                logD(TAG, "dataStr:" + dataStr);
                // Create JSON out of string.
                JSONObject dataJson = null;
                try {
                    dataJson = new JSONObject(dataStr);
                } catch (JSONException e) {
                    // Handle base64 encoded string that represent file parts.
                    dataChannelDATAHandler(this, dataStr);
                    logD(TAG, "dataStr is a file part.");
                    return;
                }
                logD(TAG, "dataStr is a JSON message.");
                // For message that are JSON formatted, handle the types of messages.
                DataChannelManager.DcType dataType = null;
                try {
                    dataType = DataChannelManager.DcType.valueOf(dataJson.getString("type"));
                    // Handle each dataType with handler.
                    switch (dataType) {
                        case CONN:
                            break;
                        case MESSAGE:
                            String msg = dataJson.getString("data");
                            boolean isPrivate = dataJson.getBoolean("isPrivate");
                            processDcChat(tid, msg, isPrivate);
                            break;
                        case WRQ:
                            dataChannelWRQHandler(this, dataJson);
                            break;
                        case ACK:
                            dataChannelACKHandler(this, dataJson);
                            break;
                        case ERROR:
                            dataChannelErrorHandler(this, dataJson);
                            break;
                        case CANCEL:
                            dataChannelCancelHandler(this, dataJson);
                            break;
                        default:
                            break;
                    }
                } catch (JSONException e) {
                    String error = "[ERROR:" + Errors.DC_MSG_UNKNOWN_TYPE + "] Ignoring some " +
                            "unknown message type from Peer " + tid + "!";
                    String debug = error + "\nIgnoring the message: \"" + dataJson +
                            "\".\n" +
                            "Due to unknown DC message \"type\": " + dataType;
                    logE(TAG, error);
                    logD(TAG, debug);
                }
            } else {
                final String tid = this.getTid();
                connectionManager.runOnUiThread(new Runnable() {
                    public void run() {
                        connectionManager.getDataTransferListener().onDataReceive(tid, bytes);
                    }
                });
            }
        }

        @Override
        public void onStateChange() {
            DataChannel.State state = dc.state();
            logD(TAG, "Peer " + tid + "'s DataChannel state is now: " + state + ".");
            if (state == DataChannel.State.CLOSED) {
                // No need to disposeDC here.
            }
            if (state == DataChannel.State.OPEN)
                if (!SkylinkPeerService.isPeerIdMCU(tid))
                    connectionManager.runOnUiThread(new Runnable() {
                        public void run() {
                            connectionManager.getRemotePeerListener().onOpenDataConnection(tid);
                        }
                    });
        }

        @Override
        // The data channel's bufferedAmount has changed.
        // bufferedAmount is the number of bytes of application data (UTF-8 text and binary data)
        // that have been queued using SendBuffer but have not yet been transmitted
        // to the network.
        public void onBufferedAmountChange(long previousAmount) {
            // Log the current and changed amount:
            final long now = dc.bufferedAmount();
            final long changed = now - previousAmount;
            connectionManager.runOnUiThread(new Runnable() {
                public void run() {
                    logD(TAG, "Peer " + tid + "'s DC bufferedAmount has changed by " + changed
                            + " bytes. It is now " + now + " bytes.");
                }
            });
        }
    }

// -------------------------------------------------------------------------------------------------
// DC management
// -------------------------------------------------------------------------------------------------

    /**
     * Note: Create DataChannel - Started during createOffer, - SCTP Supported Browsers (Older
     * chromes won't work, it will fall back to RTP) - For now, Mozilla supports Blob and Chrome
     * supports ArrayBuffer
     *
     * @param {PeerConnection} pc - PeerConnection to generate the DataChannel.
     * @param {String}         createId - The socketId (mid or tid) of the offerer of this
     *                         DataChannel.
     * @param {String}         receiveId - The socketId (mid or tid) of the receiver of this
     *                         DataChannel.
     * @param {String}         channelName - The Name of the Channel. If null, it would be
     *                         generated.
     * @param {RTCDataChannel} dc - The DataChannel object passed inside.
     * @param {String}         tid - The socketId of the remote Peer of this DataChannel.
     * @method createDataChannel
     * @public
     */
    public DataChannel createDataChannel(
            PeerConnection pc, String createId, String receiveId, String channelName, DataChannel dc,
            String tid) {

        DcObserver dcObserver = new DcObserver(createId, receiveId, tid);

        // For the offerer (createId), DC has to be created.
        if (dc == null) {
            // We are the offerer, the other party is the receiver.
            // Create our DC
            // Create channel name if not given.
            if (channelName.equals("")) {
                channelName = createId + "_" + receiveId;
            }
            // Create DC
            dc = pc.createDataChannel(channelName, new DataChannel.Init());
        } else {
            // If DC already exist, then we are:
            // NOT the offerer.
            // The receiveId.
            channelName = dc.label();
        }

        // Add reference to dc in DcObserver
        dcObserver.setDc(dc);
        // Add DcObserver to dc, even if dc was not created by us.
        dc.registerObserver(dcObserver);

        // Add tid and dcObserver pair to dcInfoList if not MCU
        if (SkylinkPeerService.isPeerIdMCU(tid)) {
            dcMcu = dc;
            dcObsMcu = dcObserver;
            tidMcu = tid;
        } else {
            dcInfoList.put(tid, dcObserver);
        }

        return dc;
        // dc.close();
        // dc.dispose();
    }

    /**
     * Note: For a specific or all DataChannel(s), dispose of native resources attached. DataChannel
     * is that of peer whose peerId is provided. If provided tid is null, all DC will be disposed.
     *
     * @param {String}   peerId - The socketId of the remote Peer of this DataChannel.
     * @param disconnect Whether to disconnect from room.
     * @method disposeDC
     * @public
     */
    void disposeDC(String peerId, boolean disconnect) {

        // Send to all Peers with DC.
        Set<String> peerIdSet = new HashSet<String>(dcInfoList.keySet());
        for (String tid : peerIdSet) {

            if (peerId != null && !tid.equals(peerId)) {
                continue;
            }

            // Dispose DcObserver and DC.
            DcObserver dcObserver = dcInfoList.get(tid);
            if (dcObserver != null) {
                // Remove the DcHandler
                dcObserver.setDcHandler(null);
                // Dispose DC.
                DataChannel dc = dcObserver.getDc();
                if (dc != null) {
                    // Do not close dc here!
                    dc.unregisterObserver();
                    dc.dispose();
                }
            }
            // Remove Peer from dcInfoList
            dcInfoList.remove(tid);

            // Exit loop if only 1 DC is required to be closed and disposed.
            if (peerId != null) {
                break;
            }
        }

        // When we leave room, disposeDc for dcMcu.
        if (isMcuRoom && disconnect && dcInfoList.size() == 0) {
            if (dcObsMcu != null) {
                dcObsMcu.setDcHandler(null);
                dcMcu.unregisterObserver();
                dcMcu.dispose();
                dcObsMcu = null;
                dcMcu = null;
            }
        }
    }

    // -------------------------------------------------------------------------------------------------
// DC Message Handlers
// -------------------------------------------------------------------------------------------------
    // Prepare to save file.
    public void dataChannelWRQHandler(final DcObserver dcObserver, JSONObject dataJson) {
        // Process DC WRQ message in JSON format
    /*
    Keys of DataChannel WRQ message:
    type, sender.
    agent = iOS for iOS device, Mozilla for Mozilla Browser etc.
    name = Name of the file being transferred, including extension but not file path.
    size = Size of file in bytes after encoding.
    chunkSize = Number of bytes to be sent in one go after encoding. The maximum should be 65536.
    timeout = Number of seconds for our response to be received before timeout.
    isPrivate = Number of seconds for our response to be received before timeout.
    */

        final String fileName;
        String fileNameTemp = "";
        long filesize;
        long chunkSize = CHUNK_SIZE_POST; // Set default chunkSize, to be overwritten by actual value.
        @SuppressWarnings("unused")
        int timeout;
        boolean isPrivateTemp = true;
        String tid = dcObserver.getTid();
        final boolean isPrivate;
        String sender = "";

        String errorMessage = "";
        String debug = "";

        // Get file transfer parameters
        try {
            fileNameTemp = dataJson.getString("name");
            filesize = Long.parseLong(dataJson.getString("size"));
            dcObserver.setSaveFileSizeEncoded(filesize);
            // @SuppressWarnings("unused");
            chunkSize = Long.parseLong(dataJson.getString("chunkSize"));
            // @SuppressWarnings("unused")
            timeout = Integer.parseInt(dataJson.getString("timeout"));
            isPrivateTemp = dataJson.getBoolean("isPrivate");
            sender = dataJson.getString("sender");
        } catch (JSONException e) {
            // Send DC error message and terminate transfer.
            String jsonErr = e.getMessage();
            errorMessage = "[ERROR] File transfer with Peer " + getDisplayName(tid) + " (" + tid +
                    ") has been cancelled as the WRQ message was not properly formed. Error:\n" +
                    jsonErr;
            sendError(errorMessage, false, tid);
            logD(TAG, errorMessage);
            return;
        }

        // Limiting to receiving 1 file at a time.
        if ("".equals(peerSend)) {
            // Set new Peer sending.
            peerSend = tid;
        } else {
            String peer = peerSend;
            errorMessage = "currently not ready to receive the file \"" + fileNameTemp + "\"";
            // Log from our perspective.
            debug = "We are " + errorMessage + " from Peer " +
                    getDisplayName(sender) + " (" + sender + ")," +
                    " as we are still receiving a file from Peer "
                    + getDisplayName(peer) + " (" + peer + ").";
            // Send error message to Peer from their perspective.
            errorMessage = "Peer " + displayName + " (" + mid + ") is " + errorMessage +
                    ", you can try sending again later.";
            logD(TAG, debug);
            sendError(errorMessage, false, tid);
            return;
        }

        fileName = fileNameTemp;
        isPrivate = isPrivateTemp;
        dcObserver.setSaveFileName(fileName);
        dcObserver.setChunkSizePre(chunkSize * 6 / 8);
        dcObserver.setChunkSizePost(chunkSize);

        // Trigger callback
        connectionManager.runOnUiThread(new Runnable() {
            public void run() {
                connectionManager.getFileTransferListener()
                        .onFileTransferPermissionRequest(dcObserver.getTid(), fileName, isPrivate);
            }
        });
    }

    // Save file sent as base64 encoded string.
    public void dataChannelDATAHandler(final DcObserver dcObserver, String dataStr) {
        // Clear save timer.
        if (!(dcObserver.getDcHandler()).clearSaveTimer()) {
            // Timeout had occurred, return and do nothing as Timer would have sent:
            // didHaltFileTransfer to self.
            // DC ERROR message to Peer.
            return;
        }
        long chunk = dcObserver.getChunk();
        final long chunkNext;
        final String filePath = dcObserver.getSaveFilePath();
        final String tid = dcObserver.getTid();
        long chunkSizePre = dcObserver.getChunkSizePre();
        long chunkSizePost = dcObserver.getChunkSizePost();

        // Convert base64 encoded string to byte array.
        byte[] data = Base64.decode(dataStr, Base64.DEFAULT);
        // Get position of chunk to write.
        // long pos = CHUNK_SIZE_PRE * chunk;
        long pos = chunkSizePre * chunk;

        // Trigger callback for percentage received.
        long fileSize = dcObserver.getSaveFileSizeEncoded();

        double pctTemp = (double) chunk * (double) chunkSizePost / (double) fileSize * 100;
        // If an additional empty chunk was sent to indicate end of file,
        // or if file did not fully fill last chunck,
        // use previous chunk to calculate to avoid showing > 100%
        if (pctTemp > 100) pctTemp =
                (double) (chunk - 1) * (double) chunkSizePost / (double) fileSize * 100;
        final double pct = pctTemp;
        connectionManager.runOnUiThread(new Runnable() {
            public void run() {
                connectionManager.getFileTransferListener().onFileReceiveProgress(tid, filePath, pct);
            }
        });

        // Write byte array to file.
        addBytesToFile(data, filePath, pos);
        // If byte array is less than full chunkSize
        if (data.length < chunkSizePre) {
            // It is the end of file transfer.
            // End of Receive operation
            // Reset peerSend
            peerSend = "";
            // Reset chunk to 0
            dcObserver.setChunk(0);
            // Let Peer know we have received chunk by sending 1 more ACK:
            sendACK(dcObserver, chunk + 1, true);
            connectionManager.runOnUiThread(new Runnable() {
                public void run() {
                    connectionManager.getFileTransferListener().onFileReceiveComplete(dcObserver.getTid(), filePath);
                }
            });
            return;
        } else {
            // Otherwise, request for next chunk.
            chunkNext = chunk + 1;
            dcObserver.setChunk(chunkNext);
            sendACK(dcObserver, chunkNext, false);
        }

    /*// Start save timer.
      // Runnable to execute on timeout:
    Runnable run = new Runnable() {
      public void run() {
        final String errorMessage = "Aborting saving of file \"" + filePath + "\" as:\n" +
          "Waited for " + Integer.toString( TIMEOUT ) + " seconds for file chunk " + chunkNext +
          "but did not get any from Peer " + getDisplayName( tid ) + " (" + tid + ").";
        ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
  		  public void run() {
  			  connectionManager.getFileTransferDelegate().onDrop(tid, filePath, errorMessage, false);
  			  }
  		  });
        sendError( errorMessage, false, tid );
        (dcObserver.getDcHandler()).stopSaveTimer();
        // Set chunk back to 0.
        dcObserver.setChunk( 0 );
      }
    };
      // Queue the Runnable on this thread.
    (dcObserver.getDcHandler()).postDelayed( run, ( long ) ( TIMEOUT * 1000 ) );
      // Set the Send state of our dcHandler.
    (dcObserver.getDcHandler()).startSaveTimer( run );*/
    }

    // Send file chunk requested.
    public void dataChannelACKHandler(final DcObserver dcObserverIn, JSONObject dataJson) {
        // Process DC ACK message in JSON format
    /*
    Keys of DataChannel ACK message:
    type, sender.
    agent = Android for Android device, iOS for iOS device, Mozilla for Mozilla Browser etc.
    name = Name of the file being transferred, including extension but not file path.
    ackN = Chunk number that is expected (starting from 0); -1: The transfer is rejected.
    */
        // Get DcObserver of Peer (not the MCU, if present)
        final DcObserver dcObserver;
        if (isMcuRoom) {
            // For broadcasting dcObserver of MCU is used.
            if (peerRecv == null) {
                dcObserver = dcObsMcu;
            } else {
                dcObserver = dcInfoList.get(peerRecv);
            }
        } else {
            dcObserver = dcObserverIn;
        }

        // Get peerId of Peer (not the MCU, if present)
        String tidPeer = dcObserver.getTid();

        // Get the sender
        String sender = "";
        try {
            sender = dataJson.getString("sender");
        } catch (JSONException e) {
            String debug = "[ERROR] Unable to get \"sender\" of ACK. Exception:\n" + e.getMessage();
            logD(TAG, debug);
        }

        // Clear send timer.
        if (!(dcObserver.getDcHandler()).clearSendTimer()) {
            // Timeout had occurred, return and do nothing as Timer would have sent:
            // didHaltFileTransfer to self.
            // DC ERROR message to Peer.
            return;
        }
        final int chunk;
        int chunkTemp = -2;
        final String fileName = dcObserver.getFileName();
        try {
            chunkTemp = dataJson.getInt("ackN");
        } catch (JSONException e) {
            String debug = "[ERROR] Unable to read \"ackN\" from ACK! Exception:\n" +
                    e.getMessage();
            logD(TAG, debug);
            sendError(debug, true, tidPeer);
            return;
        }
        chunk = chunkTemp;

        final String filePath = dcObserver.getFilePath();
        long fileSize = dcObserver.getFileSize();
        final String tid = dcObserver.getTid();
        boolean sendLast = false;

        if (chunk <= 0) {
            // If this is first ACK, determine if transfer should proceed.
            if (chunk == -1) {
                // Transfer declined.
                connectionManager.runOnUiThread(new Runnable() {
                    public void run() {
                        connectionManager.getFileTransferListener().onFileTransferPermissionResponse(tid, filePath, false);
                    }
                });
                // End of send operation
                // Reset peerRecv
                peerRecv = "";
                dcObserver.setNowSending(false);
                return;
            } else if (chunk == 0) {
                // Transfer accepted, notify UI.
                connectionManager.runOnUiThread(new Runnable() {
                    public void run() {
                        connectionManager.getFileTransferListener().onFileTransferPermissionResponse(tid, filePath, true);
                    }
                });
            }
        } else {
            // Trigger callback for percentage sent.
            double pctTemp = (double) chunk * (double) CHUNK_SIZE_PRE / (double) fileSize * 100;
            // If an additional empty chunk was sent to indicate end of file,
            // or if file did not fully fill last chunck,
            // use previous chunk to calculate to avoid showing > 100%
            if (pctTemp > 100) pctTemp =
                    (double) (chunk - 1) * (double) CHUNK_SIZE_PRE / (double) fileSize * 100;
            final double pct = pctTemp;
            connectionManager.runOnUiThread(new Runnable() {
                public void run() {
                    connectionManager.getFileTransferListener().onFileSendProgress(tid, filePath, pct);
                }
            });
        }

        // Do not send any data if not allowed to by nowSending flag.
        if (!dcObserver.getNowSending()) return;

        // Get position of chunk requested.
        long pos = CHUNK_SIZE_PRE * chunk;
        // End of file transfer if position reaches file size or beyond.
        if (pos == fileSize) {
            // Receiver will only request for 1 past the last chunk if the last chunk was still
            // at the full chunkSize.
            // In this case, send an empty string to denote end of file.
            sendDcString(tidPeer, "", true);
            sendLast = true;
        } else if (pos > fileSize) {
            // This happens when the previous chunk was < full size.
            // It is the receiver acknowledging that he got the last chunk.
            // Hence do nothing and assume the receiver will assume that we got the acknowledgement.
            connectionManager.runOnUiThread(new Runnable() {
                public void run() {
                    connectionManager.getFileTransferListener().onFileSendComplete(tid, filePath);
                }
            });
            // End of send operation
            // Reset peerRecv
            peerRecv = "";
            dcObserver.setNowSending(false);
            return;
        }

        if (!sendLast) {
            byte[] chunkBA = null;
            // Set size of chunkBA
            if (pos + CHUNK_SIZE_PRE >= fileSize) {
                // Set a possibly smaller array for last chunk.
                chunkBA = new byte[(int) (fileSize - pos)];
            } else {
                // All chunks before the last should be of CHUNK_SIZE_PRE size.
                chunkBA = new byte[CHUNK_SIZE_PRE];
            }
            // Get chunk as byte[]
            readBytesFromFile(chunkBA, dcObserver.getFilePath(), pos);

            // Encode chunk as base 64 encoded string.
            String chunkStr = new String(Base64.encodeToString(chunkBA, Base64.DEFAULT));

            // Send binary string message
            sendDcString(tidPeer, chunkStr, true);
        }

        // Calculate expected last chunk
        final long chunkLast = (fileSize / CHUNK_SIZE_PRE) + 1;

        // Start send timer.
        // Runnable to execute on timeout:
        Runnable run = new Runnable() {
            public void run() {
                final String errorMessage = "Aborting sending of file \"" + filePath + "\" as:\n" +
                        "Waited for " + Integer.toString(TIMEOUT) + " seconds for ACK of chunk " + chunk +
                        " out of " + chunkLast + " expected chunks, " +
                        "but did not get any from Peer " + getDisplayName(tid) + " (" + tid + ").";
                connectionManager.runOnUiThread(new Runnable() {
                    public void run() {
                        connectionManager.getFileTransferListener().onFileTransferDrop(tid, fileName, errorMessage, false);
                    }
                });
                sendError(errorMessage, true, tid);
            }
        };
        // Queue the Runnable on this thread.
        (dcObserver.getDcHandler()).postDelayed(run, (long) (TIMEOUT * 1000));
        // Set the Send state of our dcHandler.
        (dcObserver.getDcHandler()).startSendTimer(run);
    }

    // Terminate file transfer on receiving ERROR message from Peer.
    public void dataChannelErrorHandler(DcObserver dcObserver, JSONObject dataJson) {
        // Process DC ERROR message in JSON format
    /*
    Keys of DataChannel ERROR message:
    type, sender.
    name = Name of the file being transferred, including extension but not file path.
    content = The error message.
    isUploadError = Boolean to indicate if error occurred at upload (true) or download (false).
    */

        final String tid = dcObserver.getTid();
        final String fileName;
        String fileNameTemp = "";
        String desc = "";
        boolean isUploadError = false;

        try {
            fileNameTemp = dataJson.getString("name");
            desc = dataJson.getString("content");
            isUploadError = dataJson.getBoolean("isUploadError");
        } catch (JSONException e) {
            String error = "[ERROR] Error processing DataChannel ERROR message from Peer " +
                    getDisplayName(tid) + " (" + tid + ")!";
            String debug = error + "\nException: " + e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);
        }

        fileName = fileNameTemp;
        String error = "Aborting ";

        DcHandler dcHandler = (dcObserver.getDcHandler());
        if (isUploadError) {
            // If we are the receiver
            // Clear timer.
            if (dcHandler != null) dcHandler.clearSaveTimer();
            // End of receive operation
            // Reset peerSend
            peerSend = "";
            // reset file info.
            dcObserver.setChunk(0);
            dcObserver.setSaveFilePath("");
            dcObserver.setSaveFileName("");
            error += "file saving ";
        } else {
            // If we are the sender
            // Clear timer.
            if (dcHandler != null) dcHandler.clearSendTimer();
            // End of send operation
            // Reset peerRecv
            peerRecv = "";
            dcObserver.setNowSending(false);
            // reset file info.
            dcObserver.setFileName("");
            dcObserver.setFilePath("");
            dcObserver.setFileSize(0);
            dcObserver.setFileSizeEncoded(0);
            error += "file sending ";
        }
        error += "as Peer " + getDisplayName(tid) + " (" + tid + ")." +
                " reported an error in file transfer!";
        String debug = error + "\nError message was:\n" + desc;
        logE(TAG, error);
        logD(TAG, debug);
        final String newErrorMessage = error;
        // Trigger callback
        connectionManager.runOnUiThread(new Runnable() {
            public void run() {
                connectionManager.getFileTransferListener().onFileTransferDrop(tid, fileName, newErrorMessage, false);
            }
        });
    }

    /* *
    * Terminate file transfer on receiving CANCEL message from Peer.
    * As of DT 0.1.0, this can only be sent from receiver to sender, and not the other way round.
    * @param dcObserver
    * @param dataJson
    */
    public void dataChannelCancelHandler(DcObserver dcObserver, JSONObject dataJson) {
        // Process DC CANCEL message in JSON format
    /*
    Keys of DataChannel CANCEL message:
    type, sender.
    name = Name of the file being transferred, including extension but not file path.
    content = The error message.
    */

        final String tid = dcObserver.getTid();
        final String fileName;
        String fileNameTemp = "";
        String desc = "";

        try {
            fileNameTemp = dataJson.getString("name");
            desc = dataJson.getString("content");
        } catch (JSONException e) {
            String error = "[ERROR] Could not process request from Peer " + getDisplayName(tid) +
                    " (" + tid + ") to cancel file transfer.\nCancelling our file share with Peer.";
            String debug = error + "\nException: " + e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);
        }

        fileName = fileNameTemp;
        String cancelMessage = "Aborting ";

        // We are the sender
        // Clear timer.
        (dcObserver.getDcHandler()).clearSendTimer();
        // End of send operation
        // Reset peerRecv
        peerRecv = "";
        dcObserver.setNowSending(false);
        // reset file info.
        dcObserver.setFileName("");
        dcObserver.setFilePath("");
        dcObserver.setFileSize(0);
        dcObserver.setFileSizeEncoded(0);
        cancelMessage += "file sending ";

        cancelMessage += "as Peer " + getDisplayName(tid) + " (" + tid + ")." +
                " sent DC CANCEL:\n" + desc;
        final String newCancelMessage = cancelMessage;
        // Trigger callback
        connectionManager.runOnUiThread(new Runnable() {
            public void run() {
                connectionManager.getFileTransferListener().onFileTransferDrop(tid, fileName, newCancelMessage, true);
            }
        });
    }

// -------------------------------------------------------------------------------------------------
// DC File transfer methods
// -------------------------------------------------------------------------------------------------
    // Send a WRQ message via DC.
    // Format a WRQ message into DC WRQ format.

    /**
     * @param dcObserver DcObserver of Peer sending to.
     * @param agentStr   Android
     * @param fileName
     * @param timeOut
     * @param isPrivate
     */
    private void sendWRQ(final DcObserver dcObserver, String agentStr,
                         final String fileName, int timeOut, boolean isPrivate) throws SkylinkException {
        // Create DC WRQ message in JSON format
    /*
    Keys of DataChannel WRQ message:
    type, sender.
    agent = iOS for iOS device, Mozilla for Mozilla Browser etc.
    name = Name of the file being transferred, including extension but not file path.
    size = Size of file in bytes after encoding.
    chunkSize = Number of bytes to be sent in one go after encoding. The maximum should be 65536.
    timeout = Number of seconds for our response to be received before timeout.
    isPrivate = Number of seconds for our response to be received before timeout.
    */
        long fileSizeEncoded = dcObserver.getFileSizeEncoded();
        final String tid = dcObserver.getTid();

        JSONObject msgJson = new JSONObject();
        try {
            msgJson.put("type", "WRQ");
            msgJson.put("sender", mid);
            msgJson.put("agent", agentStr);
            msgJson.put("version", BuildConfig.VERSION_NAME);
            msgJson.put("name", fileName);
            msgJson.put("size", fileSizeEncoded);
            msgJson.put("chunkSize", CHUNK_SIZE_POST);
            msgJson.put("timeout", timeOut);
            msgJson.put("isPrivate", isPrivate);
            /* For DT 0.1.1
            // IFF MCU room and Private message, include target.
            if (isMcuRoom && isPrivate)*/
            // DT 0.1.0 Include target for all private messages.
            if (isPrivate)
                msgJson.put("target", tid);
        } catch (JSONException e) {
            String error = "[ERROR] Error forming file share request. File share request not sent.";
            String debug = error + "\nDetails: Unable to sendWRQ due to following exception:\n" +
                    e.getMessage();
            logD(TAG, debug);
            throw new SkylinkException(error);
        }

        String msgStr = msgJson.toString();
        sendDcString(tid, msgStr, true);

        // Start send timer.
        // Runnable to execute on timeout:
        Runnable run = new Runnable() {
            public void run() {
                final String errorMessage = "Aborting sending of file \"" + fileName + "\" as:\n" +
                        "Waited for " + Integer.toString(TIMEOUT) +
                        " seconds but did not get file acceptance response from Peer " +
                        getDisplayName(tid) + " (" + tid + ").";
                connectionManager.runOnUiThread(new Runnable() {
                    public void run() {
                        connectionManager.getFileTransferListener().
                                onFileTransferDrop(tid, fileName, errorMessage, false);
                    }
                });
                sendError(errorMessage, true, tid);
            }
        };
        // Queue the Runnable on this thread.
        (dcObserver.getDcHandler()).postDelayed(run, (long) (TIMEOUT * 1000));
        // Set the Send state of our dcHandler.
        (dcObserver.getDcHandler()).startSendTimer(run);

    }

    // Send a ACK message via DC.
    // Format a ACK message into DC ACK format.
    private void sendACK(final DcObserver dcObserver, final long chunk, boolean finalAck) {
        // Create DC ACK message in JSON format
      /*
      Keys of DataChannel ACK message:
      type, sender.
      agent = Android for Android device, iOS for iOS device, Mozilla for Mozilla Browser etc.
      name = Name of the file being transferred, including extension but not file path.
      ackN = Chunk number that is expected (starting from 0); -1: The transfer is rejected.
      */

        JSONObject msgJson = new JSONObject();
        final String filePath = dcObserver.getSaveFilePath();
        final String tid = dcObserver.getTid();

        try {
            msgJson.put("type", "ACK");
            msgJson.put("sender", mid);
            msgJson.put("ackN", chunk);
        } catch (JSONException e) {
            final String fileName = dcObserver.getFileName();
            String error = "[ERROR] ";
            if (chunk <= 0) {
                error += "Unable to send file share permission to Peer " + tid + " (" +
                        getDisplayName(tid) + ") ";
            } else {
                error += "Unable to request Peer " + tid + " (" + getDisplayName(tid) +
                        ") to continue file share!";
                sendError(error, false, tid);
            }
            String debug = error + "\nException: " + e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);

            //Drop file transfer if already started
            if (chunk >= 0) {
                final String finalError = error;
                connectionManager.runOnUiThread(new Runnable() {
                    public void run() {
                        connectionManager.getFileTransferListener()
                                .onFileTransferDrop(tid, fileName, finalError, false);
                    }
                });
            }
            return;
        }

        String msgStr = msgJson.toString();
        sendDcString(tid, msgStr, false);

        // Start save timer only if not finalAck or file share decline.
        if (!finalAck && chunk != -1) {
            // Runnable to execute on timeout:
            Runnable run = new Runnable() {
                public void run() {
                    final String errorMessage = "Aborting saving of file \"" + filePath + "\" as:\n" +
                            "Waited for " + Integer.toString(TIMEOUT) +
                            " seconds but did not get file chunk " + chunk + " from Peer " + getDisplayName(tid) +
                            " (" + tid + ").";
                    connectionManager.runOnUiThread(new Runnable() {
                        public void run() {
                            connectionManager.getFileTransferListener().onFileTransferDrop(tid, filePath, errorMessage, false);
                        }
                    });
                    sendError(errorMessage, false, tid);
                }
            };
            // Queue the Runnable on this thread.
            (dcObserver.getDcHandler()).postDelayed(run, (long) (TIMEOUT * 1000));
            // Set the Send state of our dcHandler.
            (dcObserver.getDcHandler()).startSaveTimer(run);
        }
    }


    //
    //

    /**
     * API to send an ERROR message. Send by either side anytime during file transfer to indicate
     * halt to transfer, due to reason given in the description.
     *
     * @param errorMessage  A string to describe the nature of this error.
     * @param isUploadError True/false if we (who triggered this msg) are the sender/receiver.
     * @param remotePeerId  The id of the peer. Null if broadcast file transfer was attempted.
     */
    public void sendError(String errorMessage, boolean isUploadError, String remotePeerId) {
        // Create DC ERROR message in JSON format
    /*
    Keys of DataChannel ERROR message:
    type, sender.
    name = Name of the file being transferred, including extension but not file path.
    content = The error message.
    isUploadError = Boolean to indicate if this error msg is fired from the sender or receiver.
    */

        DcObserver dcObserver = dcInfoList.get(remotePeerId);

        JSONObject msgJson = new JSONObject();
        try {
            msgJson.put("type", "ERROR");
            msgJson.put("sender", mid);
            if (isUploadError)
                msgJson.put("name", dcObserver.getFileName());
            else
                msgJson.put("name", dcObserver.getSaveFileName());
            msgJson.put("content", errorMessage);
            msgJson.put("isUploadError", isUploadError);
        } catch (JSONException e) {
            String error = "[ERROR] Unable to send error message to Peer regarding " +
                    "failed file transfer!";
            String debug = error + "\nDetails: Unable to form error JSON. Content of message:\n" +
                    errorMessage + "\nException: " + e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);
        }

        String msgStr = msgJson.toString();

        sendDcString(remotePeerId, msgStr, isUploadError);

        if (isUploadError) {
            // If we are the sender:
            // Terminate send operation
            // Reset peerRecv
            (dcObserver.getDcHandler()).stopSendTimer();
            dcObserver.setNowSending(false);
            peerRecv = "";
        } else {
            // If we are the receiver and this is an ongoing transfer.
            if (remotePeerId.equals(peerSend)) {
                // End of receive operation
                // Reset peerSend
                // Note that this case cannot be that of same Peer trying to send before previous
                // send is done as the SDK will(should) prevent this from the sending side.
                (dcObserver.getDcHandler()).stopSaveTimer();
                peerSend = "";
            } else {
                // Do not reset peerSend if error due to Peer trying to send
                // when another Peer is still sending.
                // Just log the case.
                String warn = "[WARN] Rejecting Peer " + remotePeerId + "'s file share as" +
                        " still receiving from Peer " + peerSend + ".";
                String debug = warn + "\nError Message: " + errorMessage;
                logW(TAG, warn);
                logD(TAG, debug);
            }
        }
    }


    /**
     * API to send a CANCEL message.
     * <p/>
     * Send by receiver anytime during file transfer
     * <p/>
     * to indicate halt to transfer,
     * <p/>
     * due to reason given in the description.
     *
     * @param cancelMessage Comment for canceling file transfer.
     * @param remotePeerId  Peer whose file we wish to cancel receiving.
     */
    public void sendCancel(String cancelMessage, final String remotePeerId) {
        // This is different from declining the WRQ via ACK with -1 ackN.
        // Create DC CANCEL message in JSON format
    /*
    Keys of DataChannel CANCEL message:
    type, sender.
    name = Name of the file being transferred, including extension but not file path.
    content = The error message.
    */

        DcObserver dcObserver = dcInfoList.get(remotePeerId);

        JSONObject msgJson = new JSONObject();
        try {
            msgJson.put("type", "CANCEL");
            msgJson.put("sender", mid);
            msgJson.put("name", dcObserver.getSaveFileName());
            msgJson.put("content", cancelMessage);
            // IFF MCU room, include target.
            if (isMcuRoom)
                msgJson.put("target", remotePeerId);
        } catch (JSONException e) {
            final String fileName = dcObserver.getFileName();
            String error = "[ERROR] Unable to request Peer " + remotePeerId + " (" +
                    getDisplayName(remotePeerId) + ") to cancel file share!";
            sendError(error, false, remotePeerId);
            String debug = error + "\nException: " + e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);

            //Drop file transfer
            final String finalError = error;
            connectionManager.runOnUiThread(new Runnable() {
                public void run() {
                    connectionManager.getFileTransferListener()
                            .onFileTransferDrop(remotePeerId, fileName, finalError, false);
                }
            });
            // End of receive operation
            // Reset peerSend
            peerSend = "";
            return;
        }

        String msgStr = msgJson.toString();

        sendDcString(remotePeerId, msgStr, false);

        // End of receive operation
        // Reset peerSend
        peerSend = "";
    }

    // API to initiate file transfer.
    // If tid is null, send to all Peers with DC.
    public String sendFileTransferRequest(String tid, final String fileName, String filePath)
            throws SkylinkException {

        String error = "";
        // Limiting to sending 1 file at a time.
        if ("".equals(peerRecv)) {
            // Set new Peer sending.
            peerRecv = tid;
        } else {
            String peer = "";
            if (peerRecv == null) {
                peer = "all Peers";
            } else {
                peer = "Peer " + getDisplayName(peerRecv) + " (" + peerRecv + ")";
            }
            error = "Unable to send a file now as still sending a file to Peer " + peer +
                    "! Hence not sharing file.";
            logE(TAG, error);
            return error;
        }

        String info = "Sending request to share file \"" + fileName + "\" to Peer ";
        if (tid == null) {
            // If MCU in room, it will broadcast so no need to send to everyone.
            if (isMcuRoom) {
                sendFtrToPeer(fileName, filePath, tid, false);
            } else {
                // Send to all Peers with DC.
                for (String peerId : dcInfoList.keySet()) {
                    tid = peerId;
                    sendFtrToPeer(fileName, filePath, tid, false);
                    info += tid + ".";
                    final String log = info;
                    connectionManager.runOnUiThread(new Runnable() {
                        public void run() {
                            connectionManager.getLifeCycleListener().onReceiveLog(log);
                        }
                    });
                }
            }
        } else {
            // Send to specific Peer.
            sendFtrToPeer(fileName, filePath, tid, true);
            info += tid + ".";
            final String log = info;
            connectionManager.runOnUiThread(new Runnable() {
                public void run() {
                    connectionManager.getLifeCycleListener().onReceiveLog(log);
                }
            });
        }

        logI(TAG, info);
        // Return empty string if able to send WRQ.
        return "";
    }

    // Do the real DC work of sending a WRQ (FTR) to a specific Peer.
    private void sendFtrToPeer(final String fileName, String filePath,
                               final String tid, boolean isPrivate) throws SkylinkException {

        // Send a DataChannel WRQ message.
        int timeOut = TIMEOUT;
        final DcObserver dcObserver;

        // Get the DcObserver of the Peer
        // If broadcasting, use MCU dcObserver.
        if (tid == null) {
            dcObserver = dcObsMcu;
        } else {
            dcObserver = dcInfoList.get(tid);
        }

        // Start of send operation
        dcObserver.setNowSending(true);
        // Record in DcObserver the file being transferred.
        dcObserver.setFileName(fileName);
        dcObserver.setFilePath(filePath);
        File file = new File(filePath);
        long fileSize = file.length();
        dcObserver.setFileSize(fileSize);
        // Get file size after encoding
        long adjustedSize = fileSize;
        if ((fileSize % 6) > 0) {
            adjustedSize += 6 - (fileSize % 6);
        }
        long fileSizeEncoded = adjustedSize * 8 / 6;
        dcObserver.setFileSizeEncoded(fileSizeEncoded);
        // Send WRQ to Peer.
        sendWRQ(dcObserver, agentStr, fileName, timeOut, isPrivate);

    /*// Start send timer.
      // Runnable to execute on timeout:
    Runnable run = new Runnable() {
      public void run() {
        final String errorMessage = "Aborting sending of file \"" + fileName + "\" as:\n" +
          "Waited for " + Integer.toString( TIMEOUT ) + 
          " seconds but did not get file acceptance response from Peer " + getDisplayName( tid ) +
          " (" + tid + ").";
        ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
  		  public void run() {
  			  connectionManager.getFileTransferDelegate().onDrop(tid, fileName, errorMessage, false);
  			  }
  		  });
        sendError( errorMessage, true, tid );
        (dcObserver.getDcHandler()).stopSendTimer();
      }
    };
      // Queue the Runnable on this thread.
    (dcObserver.getDcHandler()).postDelayed( run, ( long ) ( TIMEOUT * 1000 ) );
      // Set the Send state of our dcHandler.
    (dcObserver.getDcHandler()).startSendTimer( run );*/
    }


    // API to accept or reject file transfer.
    public void acceptFileTransfer(
            final String remotePeerId, boolean accept, final String filePath) {
        final DcObserver dcObserver = dcInfoList.get(remotePeerId);
        // Get the chunk required.
        long chunk = -1;
        if (accept) {
            chunk = 0;
            dcObserver.setChunk(0);
            dcObserver.setSaveFilePath(filePath);
      /*// Start save timer.
        // Runnable to execute on timeout:
      Runnable run = new Runnable() {
        public void run() {
          final String errorMessage = "Aborting saving of file \"" + filePath + "\" as:\n" +
            "Waited for " + Integer.toString( TIMEOUT ) + 
            " seconds but did not get 1st file chunk from Peer " + getDisplayName( remotePeerId ) +
            " (" + remotePeerId + ").";
          ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
          public void run() {
            connectionManager.getFileTransferDelegate().onDrop(remotePeerId, filePath, errorMessage, false);
            }
          });
          sendError( errorMessage, false, remotePeerId );
          (dcObserver.getDcHandler()).stopSaveTimer();
        }
      };
        // Queue the Runnable on this thread.
      (dcObserver.getDcHandler()).postDelayed( run, ( long ) ( TIMEOUT * 1000 ) );
        // Set the Send state of our dcHandler.
      (dcObserver.getDcHandler()).startSaveTimer( run );*/
        } else {
            dcObserver.setSaveFilePath("");
            dcObserver.setSaveFileSizeEncoded(0);
        }
        // Send response to Peer.
        sendACK(dcObserver, chunk, false);
    }

    // Fill a byte array with bytes from a file, starting from a given position.
    public void readBytesFromFile(byte[] data, String pathStr, long pos) {
        File file = new File(pathStr);
        ByteBuffer dataBB = ByteBuffer.wrap(data);
        try {
            // Open as random access file for read only.
            RandomAccessFile raFile = new RandomAccessFile(file, "r");
            FileChannel fileChannel = raFile.getChannel();
            // Set fileChannel position to pos.
            fileChannel.position(pos);
            int nread = 0;
            while (nread != -1 && dataBB.hasRemaining()) {
                nread = fileChannel.read(dataBB);
            }
            raFile.close();
            // Flip ByteBuffer for reading.
            dataBB.flip();
        } catch (IOException e) {
            System.out.println("I/O Exception: " + e);
        }
        // Get data into byte[]
        // dataBB.get( data, 0, data.length );
    }

    // Write a byte array to a file position
    public void addBytesToFile(byte[] data, String pathStr, long pos) {
        File file = new File(pathStr);
        ByteBuffer dataBB = ByteBuffer.wrap(data);
        // No need to flip ByteBuffer for writing as position is at 0.
        try {
            // Open as random access file for read and write.
            // If file does not exist, will attempt to create it.
            RandomAccessFile raFile = new RandomAccessFile(file, "rw");
            FileChannel fileChannel = raFile.getChannel();
            // Set fileChannel position to pos.
            fileChannel.position(pos);
            // Write data at pos position of the file.
            while (dataBB.hasRemaining())
                fileChannel.write(dataBB);
            raFile.close();
        } catch (IOException x) {
            System.out.println("I/O Exception: " + x);
        }
    }

    // -------------------------------------------------------------------------------------------------
// DC Chat methods
// -------------------------------------------------------------------------------------------------
    // Send a chat message via DC.
    // Format a chat message into DC chat format.
    // Create a ByteBuffer version of a string message.
    // Returns false if message could not be sent due to an error.
    public boolean sendDcChat(boolean isPrivate, Object msg, String tid) {

        // Get the DC to send message.
        DataChannel dc;
        // Tid can only be null when in MCU roon and sending public message.
        if (tid == null) {
            if (isPrivate || !isMcuRoom) {
                return false;
            }
        } else {
            // Check if Peer is still in room
            // if not, abort and inform caller.
            if (getSkylinkPeerService().getUserData(tid) == null) return false;
        }

        // Create DC chat message in JSON format
        // Keys of DataChannel Chat message:
        // type, sender, target, data, isPrivate
        JSONObject msgJson = new JSONObject();
        try {
            msgJson.put("type", "MESSAGE");
            msgJson.put("sender", mid);
            /* For DT 0.1.1
            // IFF MCU room and Private message, include target.
            if (isMcuRoom && isPrivate)*/
            // DT 0.1.0 Include target for all private messages.
            if (isPrivate)
                msgJson.put("target", tid);
            msgJson.put("data", msg);
            msgJson.put("isPrivate", isPrivate);
        } catch (JSONException e) {
            String error = "[ERROR:" + Errors.DC_UNABLE_TO_SEND_MESSAGE +
                    "] Unable to send P2P message: \"" + msg.toString() + "\"";
            String debug = error + "\nError: " + e.getMessage();
            logE(TAG, error);
            logD(TAG, debug);
            return false;
        }

        String msgStr = msgJson.toString();

        // Send string over DC
        sendDcString(tid, msgStr, true);
        return true;
    }

    /**
     * Check if a DC event is a chat message. If not a chat message, return false. If it is, Process
     * it into a signalling channel chat message. Allow normal chat display to handle it. Return
     * true.
     *
     * @param {String} dataStr DC event data of this chat message.
     * @method processDcChat
     */
    public void processDcChat(final String tid, final String msg, final boolean isPrivate) {
        // Fire callback to client if PeerMessaging is activated:
        if (hasPeerMessaging) {
            connectionManager.runOnUiThread(new Runnable() {
                public void run() {
                    connectionManager.getMessagesListener().onP2PMessageReceive(tid, msg, isPrivate);
                }
            });
        } else {
            // Notify sender privately that we do not have PeerMessaging
            String autoReply = "Peer " + mid + " did not activate Peer to Peer messaging." +
                    "Your message below was not received:\n" + msg;
            sendDcChat(true, autoReply, tid);
        }
    }

    // -------------------------------------------------------------------------------------------------
// DC Common methods
// -------------------------------------------------------------------------------------------------

    /**
     * Send a string message over DataChannel. Create a ByteBuffer version of a string message. Send
     * ByteBuffer via DC.
     *
     * @param tid
     * @param msgStr
     * @param isSending True if this is a send file/data/chat operation, false otherwise.
     */
    private void sendDcString(String tid, String msgStr, boolean isSending) {
        DataChannel dc = getDcToSend(tid, isSending);

        // Create DataChannel Buffer from ByteBuffer from chat string.
        // Create byte array using String, using UTF-8 encoding.
        byte[] byteArray = msgStr.getBytes(Charset.forName("UTF-8"));
        // TODO: Check that Maximum of 65536 bytes per message;
        // ByteBuffer data = ByteBuffer.allocate( msgSig.length() );

        // Create a ByteBuffer's using a byte array.
        // capacity and limit will be array.length, position = 0, mark = undefined.
        // Backing array will be the given array, array offset = 0.
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        // Finally create the DataChannel.Buffer with binary = false.
        DataChannel.Buffer buffer = new DataChannel.Buffer(byteBuffer, false);

        // NOTE XR: May need to wait for DC to be opened here before sending buffer.
        // Can consider using channel open call back for this.
        // Send msg via DC.
        dc.send(buffer);
        logD(TAG, "Sent DC String: " + msgStr);
    }

    /**
     * Gets the right DataChannel to use for sending to a Peer. Will take into consideration if MCU
     * is in the room, And if this is a DC file/data/chat send or receive operation.
     *
     * @param tid
     * @param isSending
     * @return DataChannel to use for sending.
     */
    private DataChannel getDcToSend(String tid, boolean isSending) {
        DataChannel dc;

        // Select the right DC to use.
        if (isMcuRoom) {
            if (isSending) {
                dc = dcMcu;
            } else {
                // Use Peer DC to receive file/data/chat when receiving.
                DcObserver dcObserver = dcInfoList.get(tid);
                dc = dcObserver.getDc();
            }
        } else {
            // No MCU in room, hence just use Peer's DC.
            DcObserver dcObserver = dcInfoList.get(tid);
            dc = dcObserver.getDc();
        }
        return dc;
    }

    // -------------------------------------------------------------------------------------------------
// Helper methods
// -------------------------------------------------------------------------------------------------
    // Gets the display name of a Peer.
    // This overloads the get method for getting self display name.
    private String getDisplayName(String tid) {
        String nick = (String) getSkylinkPeerService().getUserData(tid);
        return nick;
    }

    private SkylinkPeerService getSkylinkPeerService() {
        return connectionManager.getSkylinkPeerService();
    }

    /**
     * Sends a byte array to a specified remotePeer or to all participants of the room if the
     * remotePeerId is empty or null
     *
     * @param remotePeerId remotePeerID of a specified peer
     * @param byteArray    Array of bytes
     */
    void sendDataToPeer(String remotePeerId, byte[] byteArray) throws SkylinkException {

        if (byteArray.length > MAX_TRANSFER_SIZE) {
            throw new SkylinkException("Maximum data length is " + MAX_TRANSFER_SIZE);
        }

        // If remotePeerId is null, it means sending via MCU to all Peers.
        String peer = "all Peers in room.";
        if (remotePeerId != null) {
            peer = "Peer " + getDisplayName(remotePeerId) + " (" + remotePeerId + ").";
        }
        logD(TAG, "Sending data to " + peer);

        sendData(getDcToSend(remotePeerId, true), byteArray);
    }

    /**
     * Use the webrtc DataChannel to send binary data.
     *
     * @param dc
     * @param byteArray
     */
    private void sendData(DataChannel dc, byte[] byteArray) {
        logD(TAG, "Sending data with byte array length " + byteArray.length);
        dc.send(new DataChannel.Buffer(ByteBuffer.wrap(byteArray), true));
    }
}