package sg.com.temasys.skylink.sdk.rtc;

import android.annotation.SuppressLint;
import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

import java.util.Date;
import java.util.Random;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

/**
 * Created by xiangrong on 2/7/15.
 */ // Implementation detail: observe ICE & stream changes and react
// accordingly.
class SkylinkPcObserver implements PeerConnection.Observer {

    private static final String TAG = SkylinkPcObserver.class.getSimpleName();

    private SkylinkConnection skylinkConnection;

    private PeerConnection pc;
    private double myWeight;
    private String myId;
    private HealthChecker healthChecker;

    public PeerConnection getPc() {
        return pc;
    }

    public void setPc(PeerConnection pc) {
        this.pc = pc;
    }

    public double getMyWeight() {
        return myWeight;
    }

    @SuppressWarnings("unused")
    public void setMyWeight(double myWeight) {
        this.myWeight = myWeight;
    }

    @SuppressWarnings("unused")
    public String getMyId() {
        return myId;
    }

    public void setMyId(String myId) {
        this.myId = myId;
    }

    public SkylinkPcObserver(SkylinkConnection skylinkConnection) {
        super();
        this.skylinkConnection = skylinkConnection;
        this.myWeight = new Random(new Date().getTime()).nextDouble()
                * (double) 1000000;
    }

    void initialiseHealthChecker(String iceRole) {
        healthChecker = new HealthChecker(myId, skylinkConnection, getSkylinkConnectionService(), skylinkConnection.getLocalMediaStream(), getSkylinkConfig(), pc);
        healthChecker.setIceRole(iceRole);
        healthChecker.startRestartTimer();
    }

    // Methods to get SkylinkConnection attributes or methods.

    /**
     * Get the current SkylinkConnectionService from SkylinkConnection.
     *
     * @return SkylinkConnectionService
     */
    private SkylinkConnectionService getSkylinkConnectionService() {
        return skylinkConnection.getSkylinkConnectionService();
    }

    /**
     * Get the current SkylinkConfig from SkylinkConnection.
     *
     * @return SkylinkConfig
     */
    private SkylinkConfig getSkylinkConfig() {
        return skylinkConnection.getSkylinkConfig();
    }

    /**
     * Get the current lockDisconnect from SkylinkConnection.
     *
     * @return lockDisconnect
     */
    private Object getLockDisconnect() {
        return skylinkConnection.getLockDisconnect();
    }

    /**
     * Call SkylinkConnection's runOnUiThread
     *
     * @param runnable
     */
    private void runOnUiThread(Runnable runnable) {
        skylinkConnection.runOnUiThread(runnable);
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (getLockDisconnect()) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (getSkylinkConnectionService().isDisconnected()) {
                        return;
                    }

                    ProtocolHelper.sendCandidate(getSkylinkConnectionService(), candidate,
                            SkylinkPcObserver.this.myId);
                }
            }
        });
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState newState) {
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
        healthChecker.setIceState(newState);
        Log.d(TAG, "Peer " + myId + " : onIceConnectionChange : iceState : " + newState + ".");
        switch (newState) {
            case NEW:
                break;
            case CHECKING:
                break;
            case CONNECTED:
                break;
            case COMPLETED:
                break;
            case DISCONNECTED:
                break;
            case CLOSED:
                break;
            case FAILED:
                // restartConnectionInternal(PCObserver.this.peerId);
                break;
            default:
                break;
        }
    }

    @Override
    public void onIceGatheringChange(
            PeerConnection.IceGatheringState newState) {
        Log.d(TAG, "[onIceGatheringChange] New ICE Gathering State is now: "
                + newState.toString() + ".");
    }

    @SuppressLint("NewApi")
    @Override
    public void onAddStream(final MediaStream stream) {
        skylinkConnection.getSkylinkMediaService().addMediaStream(stream, this.myId, skylinkConnection.getLockDisconnectMediaLocal());
    }

    @Override
    public void onRemoveStream(final MediaStream stream) {
        runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (getLockDisconnect()) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (getSkylinkConnectionService().isDisconnected()) {
                        return;
                    }

                    stream.videoTracks.get(0).dispose();
                }
            }
        });
    }

    @Override
    public void onDataChannel(final DataChannel dc) {
        PeerInfo peerInfo = skylinkConnection.getPeerInfoMap().get(this.myId);
        peerInfo.setEnableDataChannel(true);
        // Prevent thread from executing with disconnect concurrently.
        synchronized (getLockDisconnect()) {
            // If user has indicated intention to disconnect,
            // We should no longer process messages from signalling server.
            if (getSkylinkConnectionService().isDisconnected()) {
                return;
            }

            if (getSkylinkConfig().hasPeerMessaging() || getSkylinkConfig().hasFileTransfer()
                    || getSkylinkConfig().hasDataTransfer()) {
                // Create our DataChannel based on given dc.
                // It is stored by dataChannelManager.
                // Get PeerConnection.
                PeerConnection pc = skylinkConnection.getPeerConnectionPool().get(this.myId);
                String mid = getSkylinkConnectionService().getSid();
                skylinkConnection.getDataChannelManager().createDataChannel(pc,
                        this.myId, mid, "", dc, this.myId);
            }
        }
    }

    @Override
    public void onRenegotiationNeeded() {
        // No need to do anything; AppRTC follows a pre-agreed-upon
        // signaling/negotiation protocol.
    }
}
