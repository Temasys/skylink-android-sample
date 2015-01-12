package sg.com.temasys.skylink.sdk.data;

// Timeout implementation
public class DcHandler extends android.os.Handler {
  private Runnable sendRunnable;
  private Runnable saveRunnable;
  
  private boolean sendTimerSet;
  private boolean saveTimerSet;

  public DcHandler() { super(); }
  
  // Set send timer states when timer starts.
  public void startSendTimer( Runnable r ) {
    sendRunnable = r;
    sendTimerSet = true;
  }
  // Set save timer states when timer starts.
  public void startSaveTimer( Runnable r ) {
    saveRunnable = r;
    saveTimerSet = true;
  }
  // Set send timer states when timer goes off.
  public void stopSendTimer() {
    sendTimerSet = false;
    // Remove reference to Runnable
    sendRunnable = null;
  }    
  // Set save timer states when timer goes off.
  public void stopSaveTimer() {
    saveTimerSet = false;
    // Remove reference to Runnable
    saveRunnable = null;
  }
  // Clear send timer
  public boolean clearSendTimer() {
    // Check if timeout has already occurred
    if( !sendTimerSet ) {
      // if so, return false
      return false;
      // return true; // Timer effect off! For Debugging.
    } else {
      // If not, remove any pending posts of sendRunnable that are in the message queue.
      removeCallbacks( sendRunnable );
      // Remove reference to Runnable
      sendRunnable = null;
    }
    return true;
  }
  // Clear save timer
  public boolean clearSaveTimer() {
    // Check if timeout has already occurred
    if( !saveTimerSet ) {
      // if so, return false
      return false;
      // return true; // Timer effect off! For Debugging.        
    } else {
      // If not, remove any pending posts of sendRunnable that are in the message queue.
      removeCallbacks( saveRunnable );
      // Remove reference to Runnable
      saveRunnable = null;
    }
    return true;
  }    
}