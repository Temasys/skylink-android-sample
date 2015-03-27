package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class SignalingServerMessageSender {

    private final static String TAG = "SocketTesterClient";

    private final int WAIT_MS = 1000;
    private final String GROUP_TYPE_NAME = "group";

    private String mid;
    private String rid;
    private long msLastSent = 0;
    private long msNewMsg = 0;
    private SignalingServerClient socketTester;
    private JSONObject msgNew;
    private JSONArray msgArr = null;

    public SignalingServerMessageSender(String mid, String rid) {
        this.mid = mid;
        this.rid = rid;
    }

    public void sendMessage(SignalingServerClient socketTester, JSONObject dictMessage) {
        this.socketTester = socketTester;

        // Determine sending type
        String type = null;
        try {
            type = dictMessage.getString("type");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        switch (type) {
            // Conditional sending for some message types.
            case "stream":
            case "updateUserEvent":
            case "roomLockEvent":
            case "muteAudioEvent":
            case "muteVideoEvent":
            case "public":
                msNewMsg = System.currentTimeMillis();
                msgNew = dictMessage;
                processNewMessage();
                break;
            // Send immediately for the rest.
            default:
                sendSignalingMessage(dictMessage, false);
        }
    }

    private void processNewMessage() {
        // Check if can send immediately or need to add to queue
        if ((msNewMsg - msLastSent <= WAIT_MS) || msgArr != null) {
            // Add to queue.
            addToQueue();
        } else {
            // Send immediately
            sendSignalingMessage(msgNew, true);
        }
    }

    // Create new msgArr if required and add to it.
    // Start timer to check for sending group message.
    private void addToQueue() {
        // If no msgArr currently exists
        if (msgArr == null) {
            // Create it
            msgArr = new JSONArray();
            // Trigger message sending WAIT_MS second after msLastSent
            long delay = msLastSent + WAIT_MS - System.currentTimeMillis();
            if (delay <= 0) delay = 1;

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.schedule(
                    new Runnable() {
                        public void run() {
                            try {
                                sendGroupMessage();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }, delay, TimeUnit.MILLISECONDS);
        }

        // Add new msg to queue
        msgArr.put(msgNew.toString());
    }

    private void sendGroupMessage() {
        // Create group message
        // Format:
        // { type: "group", lists: [<group msg>...], mid: "xxx", rid: "xxx" }      
        JSONObject msgGroup = new JSONObject();
        try {
            msgGroup.put("type", GROUP_TYPE_NAME);
            msgGroup.put("lists", msgArr);
            msgGroup.put("mid", mid);
            msgGroup.put("rid", rid);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Send group message
        sendSignalingMessage(msgGroup, true);
    }

    // Send msgSig to signaling server
    // Parameter group is true only if the message is a group message or
    // is of a type eligible to be a group message.
    private void sendSignalingMessage(JSONObject msgSig, boolean group) {
        socketTester.getSocketIO().send(msgSig.toString());
        if (group) {
            msLastSent = System.currentTimeMillis();
            msgArr = null;
        }
    }

}
