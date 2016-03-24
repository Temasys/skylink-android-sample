package sg.com.temasys.skylink.sdk.rtc;

import android.annotation.SuppressLint;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;


/**
 * Created by xiangrong on 2/7/15.
 */ // Implementation detail: observe ICE & stream changes and react
// accordingly.
class SkylinkPcObserver implements PeerConnection.Observer {

    private static final String TAG = SkylinkPcObserver.class.getName();

    private SkylinkConnection skylinkConnection;

    private String peerId;
    private Peer peer;


    @SuppressWarnings("unused")
    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public SkylinkPcObserver(String peerId, Peer peer, SkylinkConnection skylinkConnection) {
        super();
        this.peerId = peerId;
        this.peer = peer;
        this.skylinkConnection = skylinkConnection;
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
                    if (getSkylinkConnectionService() == null ||
                            getSkylinkConnectionService().getRoomParameters() == null ||
                            getSkylinkConnectionService().isDisconnected()) {
                        return;
                    }

                    ProtocolHelper.sendCandidate(getSkylinkConnectionService(), candidate, peerId);
                }
            }
        });
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState newState) {
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
        peer.setHealthCheckerIceState(newState);
        logD(TAG, "Peer " + peerId + " : onIceConnectionChange : iceState : " + newState + ".");
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
    public void onIceConnectionReceivingChange(boolean receiving) {
        logD(TAG, "Peer " + peerId + " : IceConnectionReceiving changed to " + receiving + ".");
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
        logD(TAG, "Peer " + peerId + " : IceGatheringState changed to " + newState.toString() + ".");
    }

    @SuppressLint("NewApi")
    @Override
    public void onAddStream(final MediaStream stream) {
        skylinkConnection.getSkylinkMediaService().addMediaStream(stream, this.peerId, skylinkConnection.getLockDisconnectMediaLocal());
    }

    @Override
    public void onRemoveStream(final MediaStream stream) {
        runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (getLockDisconnect()) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (getSkylinkConnectionService() == null || getSkylinkConnectionService()
                            .isDisconnected()) {
                        return;
                    }

                    stream.videoTracks.get(0).dispose();
                }
            }
        });
    }

    @Override
    public void onDataChannel(final DataChannel dc) {
        PeerInfo peerInfo = peer.getPeerInfo();
        peerInfo.setEnableDataChannel(true);
        // Prevent thread from executing with disconnect concurrently.
        synchronized (getLockDisconnect()) {
            // If user has indicated intention to disconnect,
            // We should no longer process messages from signalling server.
            if (getSkylinkConnectionService() == null ||
                    getSkylinkConnectionService().getRoomParameters() == null ||
                    getSkylinkConnectionService().isDisconnected()) {
                return;
            }

            if (getSkylinkConfig().hasPeerMessaging() || getSkylinkConfig().hasFileTransfer()
                    || getSkylinkConfig().hasDataTransfer()) {
                // Create our DataChannel based on given dc.
                // It is stored by dataChannelManager and Peer.
                // Get PeerConnection.
                PeerConnection pc = peer.getPc();
                String mid = getSkylinkConnectionService().getSid();
                DataChannel myDc = skylinkConnection.getDataChannelManager().createDataChannel(pc,
                        this.peerId, mid, "", dc, this.peerId);
                peer.setDc(myDc);
            }
        }
    }

    @Override
    public void onRenegotiationNeeded() {
        // No need to do anything; AppRTC follows a pre-agreed-upon
        // signaling/negotiation protocol.
    }

    // Getters and Setters
    public Peer getPeer() {
        return peer;
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }
}
