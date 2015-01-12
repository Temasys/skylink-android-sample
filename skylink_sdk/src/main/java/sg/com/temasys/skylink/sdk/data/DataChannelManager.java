package sg.com.temasys.skylink.sdk.data;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.PeerConnection;

import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection;
import android.app.Activity;
import android.util.Base64;
import android.util.Log;

public class DataChannelManager {
  
  private SkyLinkConnection connectionManager;
  
  private static final String TAG = "DataChannelManager";

  // Hashtable with Keys of tid (target/remote Peer id)
    // and Values of registered DcObserver.
  private Hashtable<String, DcObserver> dcInfoList = new Hashtable<String, DcObserver>();
  // List of DataChannel types
  public enum DcType {
    CONN, MESSAGE, WRQ, ACK, ERROR, CANCEL
  }
  
  // UserAgent string.
  private String uA = "Android";
  // Maximum size (bytes) of chunk in each transfer.
  private int CHUNK_SIZE_POST = 65536;
  private int CHUNK_SIZE_PRE = CHUNK_SIZE_POST * 6 / 8;

  // Time out 
    // Time out duration (seconds) for file transfers.
      // Will be over written by config if config's is valid.
  private int TIMEOUT = 60;
    // List of DcHandler for each DcObserver (unique for each DataChannel)
  private Hashtable<DcObserver, DcHandler> dcHandlerList = new Hashtable<DcObserver, DcHandler>();
    // Required in getDcHandler.
  private DcHandler dcHandler;

  // Self
  private String mid = "";
  private String displayName = "";
  private boolean hasPeerMessaging;
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
  
  public DataChannelManager( SkyLinkConnection connectionManager, int timeout,
    boolean hasPeerMessaging, boolean hasFileTransfer ) {
  	this.connectionManager = connectionManager;
    if( timeout > 0 ) TIMEOUT = timeout;
    this.hasPeerMessaging = hasPeerMessaging;
    this.hasFileTransfer = hasFileTransfer;
  }

// -------------------------------------------------------------------------------------------------
// Set and get methods
// -------------------------------------------------------------------------------------------------

  public SkyLinkConnection getConnectionManager() {
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

  public void setConnectionManager( SkyLinkConnection connectionManager ) {
	  this.connectionManager = connectionManager;
  }
  
  public void setMid( String mid ) {
    this.mid = mid;
  }

  public void setDisplayName( String displayName ) {
    this.displayName = displayName;
  }
  
  public void setIsMcuRoom( boolean isMcuRoom ) {
    this.isMcuRoom = isMcuRoom;
  }
  
// -------------------------------------------------------------------------------------------------
// Inner classes
// -------------------------------------------------------------------------------------------------


/**
 * Note:
 *  DcObserver allows us to implement the DataChannel methods, i.e.
 *    - onStateChange()
 *    - onMessage()
 *
 * @class DcObserver
 * @param {String} createId - User id of the one creating the DataChannel
 * @param {String} receiveId - User id of the one receiving the DataChannel
 * @param {String} channelName - The Name of the Channel. If null, it would be generated
 * @param {RTCDataChannel} dc - The DataChannel object passed inside
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
    public DcObserver( String createId, String receiveId, String tid ) {
      this.createId = createId;
      this.receiveId = receiveId;
      this.tid = tid;
    }
    
    @SuppressWarnings("unused")
    public String getCreateId() { return createId; }
    @SuppressWarnings("unused")
    public String getReceiveId() { return receiveId; }
    public String getTid() { return tid; }
    public DataChannel getDc() { return dc; }
    /*public DcHandler getDcHandler() {
      // handler must be created in thread that has called Looper.prepare().
      ( ( Activity ) getConnectionManager().getContext() ).runOnUiThread(new Runnable() {
        public void run() {
          if( dcHandler == null ) dcHandler = new DcHandler();
        }
      });
      return dcHandler;       
    }*/
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public String getSaveFileName() { return saveFileName; }
    public String getSaveFilePath() { return saveFilePath; }
    public long getFileSize() { return fileSize; }
    public long getFileSizeEncoded() { return fileSizeEncoded; }
    public long getSaveFileSizeEncoded() { return saveFileSizeEncoded; }
    public long getChunkSizePre() { return chunkSizePre; }
    public long getChunkSizePost() { return chunkSizePost; }
    public long getChunk() { return chunk; }
    public boolean getNowSending() { return nowSending; }

   //  @SuppressWarnings("unused")
  	// public void setPc( PeerConnection _pc ) { pc = _pc; }
    @SuppressWarnings("unused")
  	public void setCreateId( String _createId ) { createId = _createId; }
    @SuppressWarnings("unused")
  	public void setReceiveId( String _receiveId ) { receiveId = _receiveId; }
    @SuppressWarnings("unused")
  	public void setTid( String _tid ) { tid = _tid; }
    public void setDc( DataChannel _dc ) { dc = _dc; }
    public void setFileName( String _fileName ) { fileName = _fileName; }
    public void setFilePath( String _filePath ) { filePath = _filePath; }
    public void setSaveFileName( String _saveFileName ) { saveFileName = _saveFileName; }
    public void setSaveFilePath( String _saveFilePath ) { saveFilePath = _saveFilePath; }
    public void setFileSize( long _fileSize ) { fileSize = _fileSize; }
    public void setFileSizeEncoded( long _fileSizeEncoded ) { fileSizeEncoded = _fileSizeEncoded; }
    public void setSaveFileSizeEncoded( long _saveFileSizeEncoded ) { 
      saveFileSizeEncoded = _saveFileSizeEncoded; }
    public void setChunkSizePre( long _chunkSizePre ) { chunkSizePre = _chunkSizePre; }
    public void setChunkSizePost( long _chunkSizePost ) { chunkSizePost = _chunkSizePost; }
    public void setChunk( long _chunk ) { chunk = _chunk; }
    public void setNowSending( boolean _nowSending ) { nowSending = _nowSending; }
    
  /** Triggered when a new ICE candidate has been found. */
    // Send ICE candidates.
    @Override public void onMessage( final DataChannel.Buffer buffer ){
      final byte[] bytes = new byte[buffer.data.capacity()];
      buffer.data.get(bytes);

      if( !buffer.binary ) {
        String dataStr = new String( bytes );
        Log.d(TAG, "dataStr:" + dataStr );
        // Create JSON out of string.
        JSONObject dataJson = null;
        try {
          dataJson = new JSONObject( dataStr );
        } catch( JSONException e ) {
          // Handle base64 encoded string that represent file parts.
          dataChannelDATAHandler( this, dataStr );
          return;
        }
        // For message that are JSON formatted, handle the types of messages.
        try {
          DataChannelManager.DcType dataType = DataChannelManager.DcType.valueOf( dataJson.getString( "type") );
          // Handle each dataType with handler.
          switch( dataType ) {
            case CONN:
              break;
            case MESSAGE:
              String msg = dataJson.getString( "data" );
              boolean isPrivate = dataJson.getBoolean( "isPrivate" );
              processDcChat( tid, msg, isPrivate );
              break;
            case WRQ:
              dataChannelWRQHandler( this, dataJson );
              break;
            case ACK:
              dataChannelACKHandler( this, dataJson );
              break;
            case ERROR:
              dataChannelErrorHandler( this, dataJson );
              break;
            case CANCEL:
              dataChannelCancelHandler( this, dataJson );
              break;
            default:
              break;
          }
        } catch( JSONException e ) {
          Log.e(TAG, e.getMessage(), e);
        }
      }
    }

    @Override public void onStateChange() {
      DataChannel.State state = dc.state();
      if( state == DataChannel.State.CLOSED ) {
        // No need to disposeDC here.
        Log.d( TAG, "Peer " + tid + "'s DC closed.");
      }
      if (state == DataChannel.State.OPEN)
    	  if (!isPeerIdMCU(tid))
    	  ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
    		  public void run() {
    			  connectionManager.getRemotePeerDelegate().onOpenDataConnection(tid);
    			  }
    		  });
    }
  }

// -------------------------------------------------------------------------------------------------
// DC management
// -------------------------------------------------------------------------------------------------
/**
 * Note:
 *   Create DataChannel - Started during createOffer,
 *  - SCTP Supported Browsers (Older chromes won't work, it will fall back to RTP)
 *  - For now, Mozilla supports Blob and Chrome supports ArrayBuffer
 *
 * @method createDataChannel
 * @public
 * @param {PeerConnection} pc - PeerConnection to generate the DataChannel.
 * @param {String} createId - The socketId (mid or tid) of the offerer of this DataChannel.
 * @param {String} receiveId - The socketId (mid or tid) of the receiver of this DataChannel.
 * @param {String} channelName - The Name of the Channel. If null, it would be generated.
 * @param {RTCDataChannel} dc - The DataChannel object passed inside.
 * @param {String} tid - The socketId of the remote Peer of this DataChannel.
 */
  public DataChannel createDataChannel( 
    PeerConnection pc, String createId, String receiveId, String channelName, DataChannel dc,
    String tid ) {
    
    DcObserver dcObserver = new DcObserver( createId, receiveId, tid );

    // For the offerer (createId), DC has to be created.
    if( dc == null ) {
      // We are the offerer, the other party is the receiver.
      // Create our DC
        // Create channel name if not given.
      if( channelName.equals( "" ) ) {
        channelName = createId + "_" + receiveId;
      }
        // Create DC
      dc = pc.createDataChannel( channelName, new DataChannel.Init() );
    } else {
      // If DC already exist, then we are:
        // NOT the offerer.
        // The receiveId.
      channelName = dc.label();
    }
    
    // Add reference to dc in DcObserver
    dcObserver.setDc( dc );
    // Add DcObserver to dc, even if dc was not created by us.
    dc.registerObserver( dcObserver );

    // Add tid and dcObserver pair to dcInfoList if not MCU
    if( isPeerIdMCU( tid ) ) {
      dcMcu = dc;
      dcObsMcu = dcObserver;
      tidMcu = tid;
    } else {
      dcInfoList.put( tid, dcObserver );
    }
    
    return dc;
    // dc.close();
    // dc.dispose();
  }

/**
 * Note:
 *   For a specific or all DataChannel(s), dispose of native resources attached.
 *   DataChannel is that of peer whose peerId is provided.
 *   If provided tid is null, all DC will be disposed.
 *
 * @method disposeDC
 * @public
 * @param {String} peerId - The socketId of the remote Peer of this DataChannel.
 */
  public void disposeDC( String peerId ) {

    // Send to all Peers with DC.
    Iterator<String> iPeerId = dcInfoList.keySet().iterator();
    while (iPeerId.hasNext()) {
      String tid = iPeerId.next();

      if( peerId != null && !tid.equals( peerId) ) continue;

      // Dispose DC.
      DcObserver dcObserver = dcInfoList.get( tid );
      if( dcObserver != null ) {
        DataChannel dc = dcObserver.getDc();
        if( dc != null ) {
          // Do not close dc here!
          dc.unregisterObserver();
          dc.dispose();
        }
      }
      // Exit loop if required DC has been closed and disposed.
      if( peerId != null ) {
        // Remove Peer from dcInfoList
        dcInfoList.remove( tid );
        return;
      }
    }
    // When we leave room, disposeDc for dcMcu.
    if( isMcuRoom ) {
      dcMcu.unregisterObserver();
      dcMcu.dispose();
    }
  }

// -------------------------------------------------------------------------------------------------
// DC Message Handlers
// -------------------------------------------------------------------------------------------------
  // Prepare to save file.
  public void dataChannelWRQHandler( final DcObserver dcObserver, JSONObject dataJson ) {
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
    try {
      fileNameTemp = dataJson.getString( "name" );
      filesize = Long.parseLong( dataJson.getString( "size" ) );
      dcObserver.setSaveFileSizeEncoded( filesize );
      // @SuppressWarnings("unused");
      chunkSize = Long.parseLong( dataJson.getString( "chunkSize" ) );
      // @SuppressWarnings("unused")
      timeout = Integer.parseInt( dataJson.getString( "timeout" ) );
      isPrivateTemp = dataJson.getBoolean( "isPrivate" );
    } catch (JSONException e) {
      // Send DC error message and terminate transfer.
      String jsonErr = e.getMessage();
      Log.e(TAG, jsonErr, e);
      String errorMessage = "File transfer with Peer " + getDisplayName( tid ) + " (" + tid + 
        ") has been cancelled as the WRQ message was not properly formed. Error:\n" + jsonErr;
      sendError( errorMessage, false, tid );
      return;
    }
    
    fileName = fileNameTemp;
    isPrivate = isPrivateTemp;
    dcObserver.setSaveFileName( fileName );
    dcObserver.setChunkSizePre( chunkSize * 6/8 );
    dcObserver.setChunkSizePost( chunkSize );

    // Trigger callback 
    ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
		  public void run() {
        connectionManager.getFileTransferDelegate()
          .onRequest( dcObserver.getTid(), fileName, isPrivate );
			  }
		  });
  }
  
  // Save file sent as base64 encoded string.
  public void dataChannelDATAHandler( final DcObserver dcObserver, String dataStr ) {
    // Clear save timer.
    if( !getDcHandler( dcObserver ).clearSaveTimer() ) {
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
    byte[] data = Base64.decode( dataStr, Base64.DEFAULT );
    // Get position of chunk to write.
    // long pos = CHUNK_SIZE_PRE * chunk;
    long pos = chunkSizePre * chunk;
    
    // Trigger callback for percentage received.
    long fileSize = dcObserver.getSaveFileSizeEncoded();

    double pctTemp = ( double ) chunk * ( double ) chunkSizePost / ( double ) fileSize * 100;
    // If an additional empty chunk was sent to indicate end of file,
      // or if file did not fully fill last chunck,
      // use previous chunk to calculate to avoid showing > 100%
    if( pctTemp > 100 ) pctTemp = 
      ( double ) ( chunk - 1 ) * ( double ) chunkSizePost / ( double ) fileSize * 100;
    final double pct = pctTemp;
    ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
		  public void run() {
			  connectionManager.getFileTransferDelegate().onProgress(tid, filePath, pct, false);
			  }
		  });
    
    // Write byte array to file.
    addBytesToFile( data, filePath, pos );
    // If byte array is less than full chunkSize
    if( data.length < chunkSizePre ) {
      // It is the end of file transfer.
        // Reset chunk to 0
      dcObserver.setChunk( 0 );
      // Let Peer know we have received chunk by sending 1 more ACK:
      sendACK( dcObserver, chunk + 1, true );
      ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
		  public void run() {
			  connectionManager.getFileTransferDelegate().onComplete(dcObserver.getTid(), filePath, false);
			  }
		  });
      return;
    } else {
      // Otherwise, request for next chunk.
      chunkNext = chunk + 1;
      dcObserver.setChunk( chunkNext );
      sendACK( dcObserver, chunkNext, false );
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
        getDcHandler( dcObserver ).stopSaveTimer();
        // Set chunk back to 0.
        dcObserver.setChunk( 0 );
      }
    };
      // Queue the Runnable on this thread.
    getDcHandler( dcObserver ).postDelayed( run, ( long ) ( TIMEOUT * 1000 ) );
      // Set the Send state of our dcHandler.
    getDcHandler( dcObserver ).startSaveTimer( run );*/
  }
  
  // Send file chunk requested.
  public void dataChannelACKHandler( final DcObserver dcObserver, JSONObject dataJson ) {
    // Process DC ACK message in JSON format
    /*
    Keys of DataChannel ACK message:
    type, sender.
    agent = Android for Android device, iOS for iOS device, Mozilla for Mozilla Browser etc.
    name = Name of the file being transferred, including extension but not file path.
    ackN = Chunk number that is expected (starting from 0); -1: The transfer is rejected.
    */

    // Clear send timer.
    if( !getDcHandler( dcObserver ).clearSendTimer() ) {
      // Timeout had occurred, return and do nothing as Timer would have sent:
        // didHaltFileTransfer to self.
        // DC ERROR message to Peer.
      return;
    }
    final int chunk;
    int chunkTemp = -2;
    final String fileName = dcObserver.getFileName();
    // String fileNameTemp = "";
    try {
      // fileNameTemp = dataJson.getString( "name" );
      chunkTemp = dataJson.getInt( "ackN" );
    } catch (JSONException e) {
      Log.e(TAG, e.getMessage(), e);
    }
    chunk = chunkTemp;
    // fileName = fileNameTemp;

    final String filePath = dcObserver.getFilePath();
    long fileSize = dcObserver.getFileSize();
    final String tid = dcObserver.getTid();
    // Get the DC to send message.
    DataChannel dc;
    dc = dcObserver.getDc();
    // Get the right DcHandler.
    DcHandler dcHandler;
    if( isMcuRoom ) {
      // Get the dcMcu DcHandler for this DC.
    }

    boolean sendLast = false;
    
    if( chunk <= 0 ) {
    // If this is first ACK, determine if transfer should proceed.
      if( chunk == -1 ) {
        // Transfer declined.
    	  ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
    		  public void run() {
    			  connectionManager.getFileTransferDelegate().onPermission(tid, filePath, false);
    			  }
    		  });
        return;
      } else if( chunk == 0 ) {
        // Transfer accepted, notify UI.
    	  ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
    		  public void run() {
    			  connectionManager.getFileTransferDelegate().onPermission(tid, filePath, true);
    			  }
    		  });
      }
    } else {
      // Trigger callback for percentage sent.
      double pctTemp = ( double ) chunk * ( double ) CHUNK_SIZE_PRE / ( double ) fileSize * 100;
      // If an additional empty chunk was sent to indicate end of file,
        // or if file did not fully fill last chunck,
        // use previous chunk to calculate to avoid showing > 100%
      if( pctTemp > 100 ) pctTemp = 
        ( double ) ( chunk - 1 ) * ( double ) CHUNK_SIZE_PRE / ( double ) fileSize * 100;
      final double pct = pctTemp;
      ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
		  public void run() {
			  connectionManager.getFileTransferDelegate().onProgress(tid, filePath, pct, true);
			  }
		  });
    }

    // Do not send any data if not allowed to by nowSending flag.
    if( !dcObserver.getNowSending() ) return;
      
    // Get position of chunk requested.
    long pos = CHUNK_SIZE_PRE * chunk;
      // End of file transfer if position reaches file size or beyond.
    if( pos == fileSize ) {
      // Receiver will only request for 1 past the last chunk if the last chunk was still
        // at the full chunkSize.
        // In this case, send an empty string to denote end of file.
      sendDcString( dc, "" );
      sendLast = true;
    } else if( pos > fileSize ) {
      // This happens when the previous chunk was < full size.
      // It is the receiver acknowledging that he got the last chunk.
        // Hence do nothing and assume the receiver will assume that we got the acknowledgement.
    	((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
  		  public void run() {
  			  connectionManager.getFileTransferDelegate().onComplete(tid, filePath, true);
  			  }
  		  });
      // End of send operation
      dcObserver.setNowSending( false );
      return;
    }
    
    if( ! sendLast ) {
      byte[] chunkBA = null;
      // Set size of chunkBA
      if( pos + CHUNK_SIZE_PRE >= fileSize ) {
        // Set a possibly smaller array for last chunk.
        chunkBA = new byte[ ( int ) ( fileSize - pos ) ];
      } else {
        // All chunks before the last should be of CHUNK_SIZE_PRE size.
        chunkBA = new byte[ CHUNK_SIZE_PRE ];
      }
      // Get chunk as byte[]
      readBytesFromFile( chunkBA, dcObserver.getFilePath(), pos );
      
      // Encode chunk as base 64 encoded string.
      String chunkStr = new String( Base64.encodeToString( chunkBA, Base64.DEFAULT ) );
      
      // Send binary string message
      sendDcString( dc, chunkStr );
    }
    
    // Calculate expected last chunk
    final long chunkLast = ( fileSize / CHUNK_SIZE_PRE ) + 1;
    
    // Start send timer.
      // Runnable to execute on timeout:
    Runnable run = new Runnable() {
      public void run() {
        final String errorMessage = "Aborting sending of file \"" + filePath + "\" as:\n" +
          "Waited for " + Integer.toString( TIMEOUT ) + " seconds for ACK of chunk " + chunk + 
          " out of " + chunkLast + " expected chunks, " +
          "but did not get any from Peer " + getDisplayName( tid ) + " (" + tid + ").";
        ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
  		  public void run() {
  			  connectionManager.getFileTransferDelegate().onDrop(tid, fileName, errorMessage, false);
  			  }
  		  });
        sendError( errorMessage, true, tid );
        getDcHandler( dcObserver ).stopSendTimer();
        // End of send operation
        dcObserver.setNowSending( false );
      }
    };
      // Queue the Runnable on this thread.
    getDcHandler( dcObserver ).postDelayed( run, ( long ) ( TIMEOUT * 1000 ) );
      // Set the Send state of our dcHandler.
    getDcHandler( dcObserver ).startSendTimer( run );
  }

  // Terminate file transfer on receiving ERROR message from Peer.
  public void dataChannelErrorHandler( DcObserver dcObserver, JSONObject dataJson ) {
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
      fileNameTemp = dataJson.getString( "name" );
      desc = dataJson.getString( "content" );
      isUploadError = dataJson.getBoolean( "isUploadError" );
    } catch (JSONException e) {
      e.printStackTrace();
    }

    fileName = fileNameTemp;
    String errorMessage = "Aborting ";

    DcHandler dcHandler = getDcHandler( dcObserver );
    if( isUploadError ) {
      // If we are the receiver
        // Clear timer.
      if( dcHandler != null ) dcHandler.clearSaveTimer();
        // reset file info.
      dcObserver.setChunk( 0 );
      dcObserver.setSaveFilePath( "" );
      dcObserver.setSaveFileName( "" );
      errorMessage += "file saving ";
    } else {
      // If we are the sender
        // Clear timer.
      if( dcHandler != null ) dcHandler.clearSendTimer();
        // End of send operation
      dcObserver.setNowSending( false );
        // reset file info.
      dcObserver.setFileName( "" );
      dcObserver.setFilePath( "" );
      dcObserver.setFileSize( 0 );
      dcObserver.setFileSizeEncoded( 0 );
      errorMessage += "file sending ";
    }
    errorMessage += "as Peer " + getDisplayName( tid ) + " (" + tid + ")." +
      " sent DC Error:\n" + desc;
    final String newErrorMessage = errorMessage;
    // Trigger callback 
    ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
      public void run() {
        connectionManager.getFileTransferDelegate().onDrop(tid, fileName, newErrorMessage, false);
      }
    });
  }
  
  // Terminate file transfer on receiving CANCEL message from Peer.
  public void dataChannelCancelHandler( DcObserver dcObserver, JSONObject dataJson ) {
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
      fileNameTemp = dataJson.getString( "name" );
      desc = dataJson.getString( "content" );
    } catch (JSONException e) {
      e.printStackTrace();
    }

    fileName = fileNameTemp;
    String cancelMessage = "Aborting ";

    // We are the sender
      // Clear timer.
    getDcHandler( dcObserver ).clearSendTimer();
      // End of send operation
    dcObserver.setNowSending( false );
      // reset file info.
    dcObserver.setFileName( "" );
    dcObserver.setFilePath( "" );
    dcObserver.setFileSize( 0 );
    dcObserver.setFileSizeEncoded( 0 );
    cancelMessage += "file sending ";

    cancelMessage += "as Peer " + getDisplayName( tid ) + " (" + tid + ")." +
      " sent DC CANCEL:\n" + desc;
    final String newCancelMessage = cancelMessage;
    // Trigger callback 
    ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
		  public void run() {
			  connectionManager.getFileTransferDelegate().onDrop( tid, fileName, newCancelMessage, true );
		  }
	  });
  }
  
// -------------------------------------------------------------------------------------------------
// DC File transfer methods
// -------------------------------------------------------------------------------------------------
  // Send a WRQ message via DC.
    // Format a WRQ message into DC WRQ format.
  private void sendWRQ( final DcObserver dcObserver, String uA, final String fileName,
    int timeOut, boolean isPrivate ) {
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
    int chunkSize = CHUNK_SIZE_POST;
    final String tid = dcObserver.getTid();

    JSONObject msgJson = new JSONObject();
    try {
      msgJson.put( "type", "WRQ" );
      msgJson.put( "sender", mid );      
      msgJson.put( "agent", uA );      
      msgJson.put( "name", fileName );      
      msgJson.put( "size", fileSizeEncoded );      
      msgJson.put( "chunkSize", chunkSize );      
      msgJson.put( "timeout", timeOut );      
      msgJson.put( "isPrivate", isPrivate );
      // IFF MCU room and Private message, include target.
      if( isMcuRoom && isPrivate )
        msgJson.put( "target", tid );
    } catch (JSONException e) {
      e.printStackTrace();
  		Log.e( TAG, e.getMessage(), e );
    }      

    String msgStr = msgJson.toString();

    // Get the DC to send message.
    DataChannel dc;
    if( isMcuRoom ) dc = dcMcu;
    else dc = dcObserver.getDc();

    sendDcString( dc, msgStr );

    // Start send timer.
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
        getDcHandler( dcObserver ).stopSendTimer();
      }
    };
      // Queue the Runnable on this thread.
    getDcHandler( dcObserver ).postDelayed( run, ( long ) ( TIMEOUT * 1000 ) );
      // Set the Send state of our dcHandler.
    getDcHandler( dcObserver ).startSendTimer( run );

  }
  
  // Send a ACK message via DC.
    // Format a ACK message into DC ACK format.
  private void sendACK( final DcObserver dcObserver, final long chunk, boolean finalAck ) {
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
      msgJson.put( "type", "ACK" );
      msgJson.put( "sender", mid );      
      msgJson.put( "name", dcObserver.getSaveFileName() );      
      msgJson.put( "agent", uA );      
      msgJson.put( "ackN", chunk );      
    } catch (JSONException e) {
      e.printStackTrace();
    }

    String msgStr = msgJson.toString();
    DataChannel dc = dcObserver.getDc();
    sendDcString( dc, msgStr );

    // Start save timer only if not finalAck
    if( !finalAck ) {
      // Runnable to execute on timeout:
      Runnable run = new Runnable() {
        public void run() {
          final String errorMessage = "Aborting saving of file \"" + filePath + "\" as:\n" +
            "Waited for " + Integer.toString( TIMEOUT ) + 
            " seconds but did not get file chunk " + chunk + " from Peer " + getDisplayName( tid ) +
            " (" + tid + ").";
          ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
          public void run() {
            connectionManager.getFileTransferDelegate().onDrop( tid, filePath, errorMessage, false );
            }
          });
          sendError( errorMessage, false, tid );
          getDcHandler( dcObserver ).stopSaveTimer();
        }
      };
        // Queue the Runnable on this thread.
      getDcHandler( dcObserver ).postDelayed( run, ( long ) ( TIMEOUT * 1000 ) );
        // Set the Send state of our dcHandler.
      getDcHandler( dcObserver ).startSaveTimer( run );
    }
  }

  // API to send an ERROR message.
    // Send by either side anytime during file transfer to indicate halt to transfer,
      // due to reason given in the description.
  public void sendError( String errorMessage, boolean isUploadError, String remotePeerId ) {
    // Create DC ERROR message in JSON format
    /*
    Keys of DataChannel ERROR message:
    type, sender.
    name = Name of the file being transferred, including extension but not file path.
    content = The error message.
    isUploadError = Boolean to indicate if error occurred at upload (true) or download (false).
    */

    DcObserver dcObserver = dcInfoList.get( remotePeerId );

    JSONObject msgJson = new JSONObject();
    try {
      msgJson.put( "type", "ERROR" );
      msgJson.put( "sender", mid );     
      if( isUploadError ) 
        msgJson.put( "name", dcObserver.getFileName() );
      else
        msgJson.put( "name", dcObserver.getSaveFileName() );
      msgJson.put( "content", errorMessage );      
      msgJson.put( "isUploadError", isUploadError );      
      // IFF MCU room, include target.
      if( isMcuRoom )
        msgJson.put( "target", remotePeerId );
    } catch (JSONException e) {
      e.printStackTrace();
    }

    String msgStr = msgJson.toString();
    // Get the DC to send message.
    DataChannel dc;
    if( isMcuRoom ) dc = dcMcu;
    else dc = dcObserver.getDc();

    sendDcString( dc, msgStr );
  }
  
  // API to send a CANCEL message.
    // Send by receiver anytime during file transfer to indicate halt to transfer,
      // due to reason given in the description.
    // This is different from declining the WRQ via ACK with -1 ackN.
  public void sendCancel( String cancelMessage, String remotePeerId ) {
    // Create DC CANCEL message in JSON format
    /*
    Keys of DataChannel CANCEL message:
    type, sender.
    name = Name of the file being transferred, including extension but not file path.
    content = The error message.
    */

    DcObserver dcObserver = dcInfoList.get( remotePeerId );

    JSONObject msgJson = new JSONObject();
    try {
      msgJson.put( "type", "CANCEL" );
      msgJson.put( "sender", mid );      
      msgJson.put( "name", dcObserver.getSaveFileName() );      
      msgJson.put( "content", cancelMessage );      
      // IFF MCU room, include target.
      if( isMcuRoom )
        msgJson.put( "target", remotePeerId );
    } catch (JSONException e) {
      e.printStackTrace();
    }

    String msgStr = msgJson.toString();
    // Get the DC to send message.
    DataChannel dc;
    if( isMcuRoom ) dc = dcMcu;
    else dc = dcObserver.getDc();

    sendDcString( dc, msgStr );
  }
  
  // API to initiate file transfer.
    // If tid is null, send to all Peers with DC.
  public void sendFileTransferRequest( String tid, final String fileName, String filePath  ) {

    String str1 = "Sending WRQ for file \"" + fileName + "\" to Peer ";
    if( tid == null ) {
      // Send to all Peers with DC.
      Iterator<String> iPeerId = dcInfoList.keySet().iterator();
      while (iPeerId.hasNext()) {
        tid = iPeerId.next();
        sendFtrToPeer( fileName, filePath, tid, false );
        final String str = str1 + tid + ".";
        ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
          public void run() {
            connectionManager.getLifeCycleDelegate().onReceiveLog(str);
          }
        });
      }
    } else {
      // Send to specific Peer.
      sendFtrToPeer( fileName, filePath, tid, true );
      final String str = str1 + tid + ".";
      ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
        public void run() {
          connectionManager.getLifeCycleDelegate().onReceiveLog(str);
        }
      });
    }
  } 

  // Do the real DC work of sending a WRQ (FTR) to a specific Peer. 
  private void sendFtrToPeer( final String fileName, String filePath,
    final String tid, boolean isPrivate ) {
    
    // Send a DataChannel WRQ message.
    int timeOut = TIMEOUT;
    final DcObserver dcObserver = dcInfoList.get( tid );
    // Start of send operation
    dcObserver.setNowSending( true );
    // Record in DcObserver the file being transferred.
    dcObserver.setFileName( fileName );
    dcObserver.setFilePath( filePath );
    File file = new File( filePath );
    long fileSize = file.length();
    dcObserver.setFileSize( fileSize );
    // Get file size after encoding
    long adjustedSize = fileSize;
    if( ( fileSize % 3 ) > 0 ) adjustedSize += 3 - ( fileSize % 3 );
    long fileSizeEncoded = adjustedSize * 4 / 3;
    dcObserver.setFileSizeEncoded( fileSizeEncoded );
    // Send WRQ to Peer.
    sendWRQ( dcObserver, uA, fileName, timeOut, isPrivate );
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
        getDcHandler( dcObserver ).stopSendTimer();
      }
    };
      // Queue the Runnable on this thread.
    getDcHandler( dcObserver ).postDelayed( run, ( long ) ( TIMEOUT * 1000 ) );
      // Set the Send state of our dcHandler.
    getDcHandler( dcObserver ).startSendTimer( run );*/
  }
  

  
  // API to accept or reject file transfer.
  public void acceptFileTransfer( 
    final String remotePeerId, boolean accept, final String filePath ) {
    final DcObserver dcObserver = dcInfoList.get( remotePeerId );
    // Get the chunk required.
    long chunk = -1;
    if( accept ) {
      chunk = 0;
      dcObserver.setChunk( 0 );
      dcObserver.setSaveFilePath( filePath );
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
          getDcHandler( dcObserver ).stopSaveTimer();
        }
      };
        // Queue the Runnable on this thread.
      getDcHandler( dcObserver ).postDelayed( run, ( long ) ( TIMEOUT * 1000 ) );
        // Set the Send state of our dcHandler.
      getDcHandler( dcObserver ).startSaveTimer( run );*/
    } else {
      dcObserver.setSaveFilePath( "" );
      dcObserver.setSaveFileSizeEncoded( 0 );
    }
    // Send response to Peer.
    sendACK( dcObserver, chunk, false );
  }
  
  // Fill a byte array with bytes from a file, starting from a given position.
  public void readBytesFromFile( byte[] data, String pathStr, long pos ) {
    File file = new File( pathStr );
    ByteBuffer dataBB = ByteBuffer.wrap(data);
    try {
      // Open as random access file for read only.
      RandomAccessFile raFile = new RandomAccessFile( file, "r" );
      FileChannel fileChannel = raFile.getChannel();
      // Set fileChannel position to pos.
      fileChannel.position( pos );
      int nread = 0;
      while( nread != -1 && dataBB.hasRemaining() ) {
        nread = fileChannel.read( dataBB );
      }
      raFile.close();
      // Flip ByteBuffer for reading.
      dataBB.flip();
    } catch (IOException e) {
      System.out.println( "I/O Exception: " + e );
    }
    // Get data into byte[]
    // dataBB.get( data, 0, data.length );
  }
  
  // Write a byte array to a file position
  public void addBytesToFile( byte[] data, String pathStr, long pos ) {
    File file = new File( pathStr );
    ByteBuffer dataBB = ByteBuffer.wrap(data);
    // No need to flip ByteBuffer for writing as position is at 0.
    try {
      // Open as random access file for read and write.
        // If file does not exist, will attempt to create it.
      RandomAccessFile raFile = new RandomAccessFile( file, "rw" );
      FileChannel fileChannel = raFile.getChannel();
      // Set fileChannel position to pos.
      fileChannel.position( pos );
      // Write data at pos position of the file.
      while( dataBB.hasRemaining() )
        fileChannel.write( dataBB );
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
    // Returns false if message could not be sent.
  public boolean sendDcChat( boolean isPrivate, Object msg, String tid ) {
    // Check if Peer is still in room
      // if not, abort and inform caller. 
    if( connectionManager.getUserData( tid ) == null ) return false;

    // Get the DC to send message.
    DataChannel dc;
    if( isMcuRoom ) dc = dcMcu;
    else {
      DcObserver dcObserver = dcInfoList.get( tid );
      dc = dcObserver.getDc();
    }

    // If PC does not contain DC, return false
    if( dc == null ) return false;

    // Create DC chat message in JSON format
      // Keys of DataChannel Chat message:
      // type, sender, target, data, isPrivate
    JSONObject msgJson = new JSONObject();
    try {
      msgJson.put( "type", "MESSAGE" );
      msgJson.put( "sender", mid );
      // if MCU room and Public message, do not include target.
      if( !isMcuRoom || isPrivate )
        msgJson.put( "target", tid );
      msgJson.put( "data", msg );
      msgJson.put( "isPrivate", isPrivate );
    } catch (JSONException e) {
      Log.e(TAG, e.getMessage(), e);
    }

    String msgStr = msgJson.toString();
      
    // Send string over DC
    sendDcString( dc, msgStr );
    return true;
  }

  /**
   * Check if a DC event is a chat message.
   *  If not a chat message, return false.
   *  If it is,
   *    Process it into a signalling channel chat message.
   *    Allow normal chat display to handle it.
   *    Return true.
   *
   * @method processDcChat
   * @param {String} dataStr DC event data of this chat message.
   */
  public void processDcChat( final String tid, final String msg, final boolean isPrivate ) {
    // Fire callback to client if PeerMessaging is activated:
    if( hasPeerMessaging ) {
      ((Activity)connectionManager.getContext()).runOnUiThread(new Runnable() {
  		  public void run() {
  			  connectionManager.getMessagesDelegate().onPeerMessage(tid, msg, isPrivate);
			  }
		  });
    } else {
      // Notify sender privately that we do not have PeerMessaging
      String autoReply = "Peer " + mid + " did not activate Peer to Peer messaging." +
        "Your message below was not received:\n" + msg;
      sendDcChat( true, autoReply, tid );
    }
  }
  
// -------------------------------------------------------------------------------------------------
// DC Common methods
// -------------------------------------------------------------------------------------------------
  // Send a string message over DataChannel.
    // Create a ByteBuffer version of a string message.
    // Send ByteBuffer via DC.
  private void sendDcString( DataChannel dc, String msgStr ) {
    // Create DataChannel Buffer from ByteBuffer from chat string.
      // Create byte array using String, using UTF-8 encoding.
    byte[] byteArray = msgStr.getBytes( Charset.forName( "UTF-8" ) );
      // TODO: Check that Maximum of 65536 bytes per message;
      // ByteBuffer data = ByteBuffer.allocate( msgSig.length() );

      // Create a ByteBuffer's using a byte array.
        // capacity and limit will be array.length, position = 0, mark = undefined.
        // Backing array will be the given array, array offset = 0.
    ByteBuffer byteBuffer = ByteBuffer.wrap( byteArray );
      // Finally create the DataChannel.Buffer with binary = false.
    DataChannel.Buffer buffer = new DataChannel.Buffer( byteBuffer, false );
  
    // NOTE XR: May need to wait for DC to be opened here before sending buffer.
      // Can consider using channel open call back for this.
      // Send msg via DC.
    dc.send( buffer );  
  }
  
// -------------------------------------------------------------------------------------------------
// Helper methods
// -------------------------------------------------------------------------------------------------
  // Gets the display name of a Peer.
    // This overloads the get method for getting self display name.
  private String getDisplayName( String tid ) {
    String nick = (String) connectionManager.getUserData( tid );
    return nick;
  }
  
	private boolean isPeerIdMCU(String peerId) {
		return peerId.startsWith("MCU");
	}

  // Get DcHandler associated with DcObserver
  private DcHandler getDcHandler( final DcObserver dcObserver ) {
    final DcHandler dcHandlerTemp = dcHandlerList.get( dcObserver );
    // handler must be created in thread that has called Looper.prepare().
    ( ( Activity ) getConnectionManager().getContext() ).runOnUiThread(new Runnable() {
      public void run() {
        if( dcHandlerTemp == null ) {
          dcHandler = new DcHandler();
          dcHandlerList.put( dcObserver, dcHandler );
        } else dcHandler = dcHandlerTemp;
      }
    });

    return dcHandler;       
  }

}