package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose is to process room lock message types
 * Created by janidu on 11/5/15.
 */
class RoomLockMessageProcessor implements MessageProcessor {

    private static final String TAG = RoomLockMessageProcessor.class.getSimpleName();

    private SkylinkConnection skylinkConnection;
    private JSONException jsonException;
    private final Object waitLock = new Object();

    @Override
    public void process(final JSONObject jsonObject) throws JSONException {

        // Wait for possible exception throwing to be past
        // before checking if exception should be thrown.
        /*synchronized (waitLock) {
            try {
                waitLock.wait();
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            if (jsonException != null) {
                throw jsonException;
            }
        }*/
        final SignalingMessageProcessingService signalingMessageProcessingService =
                skylinkConnection.getSkylinkConnectionService().
                        getSignalingMessageProcessingService();

        this.skylinkConnection.runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (skylinkConnection.getLockDisconnect()) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (skylinkConnection.getConnectionState() ==
                            SkylinkConnection.ConnectionState.DISCONNECT) {
                        synchronized (waitLock) {
                            // Waiting for possible exception is over.
                            waitLock.notifyAll();
                        }
                        return;
                    }
                    try {
                        boolean roomLocked = ProtocolHelper.processRoomLockStatus(
                                skylinkConnection.isRoomLocked(), jsonObject,
                                skylinkConnection.getLifeCycleListener());
                        skylinkConnection.setRoomLocked(roomLocked);
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage(), e);
                        signalingMessageProcessingService.onSignalingMessageException(e);
                    }

                    synchronized (waitLock) {
                        // Waiting for possible exception is over.
                        waitLock.notifyAll();
                    }
                }
            }
        });

    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }
}
