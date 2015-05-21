package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Purpose of this MessageProcessor is to handle redirect message types
 * <p/>
 * Created by janidu on 11/5/15.
 */
class RedirectMessageProcessor implements MessageProcessor {

    private static final String TAG = RedirectMessageProcessor.class.getSimpleName();

    private SkylinkConnection skylinkConnection;
    private JSONException jsonException;
    private final Object waitLock = new Object();

    @Override
    public void process(final JSONObject jsonObject) throws JSONException {

        /*Thread thread = new Thread() {
            public void run() {
                synchronized (waitLock) {
                    // Wait for possible exception throwing to be past
                    // before checking if exception should be thrown.
                    try {
                        waitLock.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                    if (jsonException != null) {
                        throw jsonException;
                    }
                }
            }
        thread.start();
        }*/

        final SignalingMessageProcessingService signalingMessageProcessingService =
                skylinkConnection.getSkylinkConnectionService().
                        getSignalingMessageProcessingService();

        skylinkConnection.runOnUiThread(new Runnable() {
            public void run() {
                boolean shouldDisconnect = false;
                // Prevent thread from executing with disconnect concurrently.
                synchronized (skylinkConnection.getLockDisconnect()) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (skylinkConnection.getConnectionState()
                            == SkylinkConnection.ConnectionState.DISCONNECT) {
                        synchronized (waitLock) {
                            // Waiting for possible exception is over.
                            waitLock.notifyAll();
                        }
                        return;
                    }
                    try {
                        shouldDisconnect = ProtocolHelper.processRedirect(jsonObject,
                                skylinkConnection.getLifeCycleListener());
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage(), e);
                        signalingMessageProcessingService.onSignalingMessageException(e);
                    }
                }

                synchronized (waitLock) {
                    // Waiting for possible exception is over.
                    waitLock.notifyAll();
                }

                if (shouldDisconnect) {
                    skylinkConnection.disconnectFromRoom();
                }
            }
        });

    }

    @Override
    public void setSkylinkConnection(SkylinkConnection skylinkConnection) {
        this.skylinkConnection = skylinkConnection;
    }

}
