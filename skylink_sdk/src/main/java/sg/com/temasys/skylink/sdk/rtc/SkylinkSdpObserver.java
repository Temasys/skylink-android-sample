package sg.com.temasys.skylink.sdk.rtc;

import org.webrtc.PeerConnection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

/**
 * Created by xiangrong on 2/7/15.
 */


/**
 * Handle offer creation/signaling and answer setting, as well as adding remote ICE candidates once
 * the answer SDP is set.
 */
class SkylinkSdpObserver implements SdpObserver {

    private static final String TAG = SkylinkSdpObserver.class.getSimpleName();

    private SkylinkConnection skylinkConnection;
    private SessionDescription localSdp;

    private String peerId;
    private Peer peer;

    public SkylinkSdpObserver(String peerId, Peer peer, SkylinkConnection skylinkConnection) {
        this.peerId = peerId;
        this.peer = peer;
        this.skylinkConnection = skylinkConnection;
    }

    @SuppressWarnings("unused")
    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
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
    public void onCreateSuccess(final SessionDescription origSdp) {
        final PeerConnection pc;
        final SessionDescription sdp;
        // Prevent thread from executing with disconnect concurrently.
        synchronized (skylinkConnection.getLockDisconnectSdpCreate()) {
            // If user has indicated intention to disconnect,
            // We should no longer process messages from signalling server.
            if (getSkylinkConnectionService() == null || getSkylinkConnectionService().isDisconnected
                    ()) {
                return;
            }

            Utils.abortUnless(this.localSdp == null, "multiple SDP create?!?");

            String sdpType = origSdp.type.canonicalForm();

            // Set the preferred audio codec
            String sdpString = Utils.preferCodec(origSdp.description,
                    getSkylinkConfig().getPreferredAudioCodec().toString(), true);

            // Modify stereo audio in the SDP if required
            sdpString = Utils.modifyStereoAudio(sdpString, getSkylinkConfig());

            // If answer, may need to mangle to respect our own MediaConstraints:
            /* Note XR:
            The webrtc designed behaviour seems to be that if an offerer SDP indicates to send media,
            the answerer will generate an SDP to accept it, even if the answerer had put in its
            MediaConstraints not to accept that media (provided it sends that media):
            https://code.google.com/p/webrtc/issues/detail?id=2404
            Hence, for our answerer to respect its own MediaConstraints, the answer SDP will be
            mangled (if required) to respect the MediaConstraints (sdpMediaConstraints).
            */
            if ("answer".equals(sdpType)) {
                if (!getSkylinkConfig().hasAudioReceive() && getSkylinkConfig().hasAudioSend()) {
                    sdpString = Utils.sdpAudioSendOnly(sdpString);
                }
                if (!getSkylinkConfig().hasVideoReceive() && getSkylinkConfig().hasVideoSend()) {
                    sdpString = Utils.sdpVideoSendOnly(sdpString);
                }
            }

            sdp = new SessionDescription(origSdp.type, sdpString);

            this.localSdp = sdp;
            pc = peer.getPc();
        }
        runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (skylinkConnection.getLockDisconnectSdpSend()) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (getSkylinkConnectionService() == null ||
                            getSkylinkConnectionService().getRoomParameters() == null ||
                            getSkylinkConnectionService()
                            .isDisconnected()) {
                        return;
                    }


                    pc.setLocalDescription(SkylinkSdpObserver.this, sdp);
                    ProtocolHelper.sendSdp(getSkylinkConnectionService(),
                            sdp, SkylinkSdpObserver.this.peerId);
                }
            }
        });
    }

    @Override
    public void onSetSuccess() {
        final PeerConnection pc;
        // Prevent thread from executing with disconnect concurrently.
        synchronized (skylinkConnection.getLockDisconnectSdp()) {
            // If user has indicated intention to disconnect,
            // We should no longer process messages from signalling server.
            if (getSkylinkConnectionService() == null || getSkylinkConnectionService().isDisconnected
                    ()) {
                return;
            }

            pc = peer.getPc();
        }
        runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (skylinkConnection.getLockDisconnectSdpSet()) {

                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (getSkylinkConnectionService() == null || getSkylinkConnectionService()
                            .isDisconnected()) {
                        return;
                    }

                    if (pc.signalingState() == PeerConnection.SignalingState.HAVE_REMOTE_OFFER) {
                        if (pc.getRemoteDescription() != null
                                && pc.getLocalDescription() == null) {
                            skylinkConnection
                                    .logMessage("Callee, setRemoteDescription succeeded");
                            pc.createAnswer(SkylinkSdpObserver.this,
                                    skylinkConnection.getPcShared().getSdpMediaConstraints());
                            skylinkConnection.logMessage("PC - createAnswer.");
                        } else {
                            drainRemoteCandidates();
                        }
                    } else {
                        if (pc.getRemoteDescription() != null) {
                            skylinkConnection.logMessage("SDP onSuccess - drain candidates");
                            drainRemoteCandidates();
                            if (!SkylinkPeerService.isPeerIdMCU(peerId)) {
                                String tid = SkylinkSdpObserver.this.peerId;
                                Peer peer = skylinkConnection.getSkylinkPeerService().getPeer(peerId);
                                PeerInfo peerInfo = peer.getPeerInfo();
                                boolean eDC = false;
                                if (peerInfo != null) eDC = peerInfo.isEnableDataChannel();
                                skylinkConnection.getRemotePeerListener().onRemotePeerJoin(tid,
                                        skylinkConnection.getSkylinkPeerService().getUserData(tid), eDC);
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onCreateFailure(final String error) {
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

                    throw new RuntimeException("createSDP error: " + error);
                }
            }
        });
    }

    @Override
    public void onSetFailure(final String error) {
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

                    throw new RuntimeException("setSDP error: " + error);
                }
            }
        });
    }

    private void drainRemoteCandidates() {
        // Prevent thread from executing with disconnect concurrently.
        synchronized (skylinkConnection.getLockDisconnectSdpDrain()) {
            // If user has indicated intention to disconnect,
            // We should no longer process messages from signalling server.
            if (getSkylinkConnectionService() == null || getSkylinkConnectionService().isDisconnected
                    ()) {
                return;
            }

            skylinkConnection.logMessage("Inside SDPObserver.drainRemoteCandidates()");
        }
    }

    // Getters and Setters
    public Peer getPeer() {
        return peer;
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }
}
