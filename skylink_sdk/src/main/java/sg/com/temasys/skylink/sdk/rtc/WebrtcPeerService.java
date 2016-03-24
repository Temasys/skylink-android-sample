package sg.com.temasys.skylink.sdk.rtc;

import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;

import java.util.List;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;


/**
 * Created by xiangrong on 10/7/15.
 */

/**
 * Handles calls to webrtc library for P2P connectivity functions. APIs parameters will generally
 * not be very specific, and functions will extract the specific required parameters from the
 * function parameters. This allows clients to be better buffered from changes in the underlying
 * webrtc APIs that this class will eventually call.
 */
class WebrtcPeerService implements IWebrtcPeerService {
    private static String TAG = WebrtcPeerService.class.getName();
    private PcShared pcShared;

    public WebrtcPeerService(PcShared pcShared) {
        this.pcShared = pcShared;
    }

    /**
     * Adds a remote Peer's ICE Candidates to the Peer P2P component.
     *
     * @param iceCandidate
     * @param peer
     */
    void addIceCandidate(IceCandidate iceCandidate, Peer peer) {
        PeerConnection pc = peer.getPc();
        if (pc != null) {
            peerConnectionAddIceCandidate(iceCandidate, pc);
        }
    }

    /**
     * Add the webrtc component responsible for P2P communication into the Peer object. Currently,
     * this would be the PeerConnection object.
     *
     * @param peer
     * @param skylinkConnection
     * @return True if successfully added webrtc component, false otherwise.
     */
    boolean addWebrtcP2PComponent(Peer peer, SkylinkConnection skylinkConnection) {
        PeerConnection pc;
        String peerId = peer.getPeerId();
        List<PeerConnection.IceServer> iceServers =
                skylinkConnection.getSkylinkConnectionService().getIceServers();
        SkylinkPcObserver pcObserver = peer.getPcObserver();

        logD(TAG, "[addWebrtcP2PComponent] Creating a WebRTC PeerConnection for Peer " +
                peerId + "...");

        pc = peerConnectionCreate(pcShared.getPeerConnectionFactory(),
                iceServers,
                pcShared.getPcMediaConstraints(), pcObserver);

        if (pc != null) {
            peer.setPc(pc);
            logD(TAG, "[addWebrtcP2PComponent] Created new WebRTC PeerConnection for Peer " +
                    peerId + ".");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Set a remote Peer's SDP into the webrtc communication part of the Peer object.
     *
     * @param sdpString Remote Peer's SDP as a string.
     * @param peer
     * @param sdpType   The type of SDP received, i.e., an offer or an answer.
     */
    void setRemoteSdp(String sdpString, Peer peer, String sdpType) {
        String peerId = peer.getPeerId();
        PeerConnection pc = peer.getPc();
        SkylinkSdpObserver sdpObserver = peer.getSdpObserver();

        // Create the SDP
        SessionDescription sdp = sdpCreate(sdpString, sdpType);

        // Set the SDP
        peerConnectionSetRemoteSdp(pc, sdpObserver, sdp);
        logD(TAG, "[setRemoteSdp] Set WebRTC RemoteDescription " + sdpType +
                " in PeerConnection for Peer " + peerId + ".");
    }

    // Internal functions
    // These deal with webrtc lib.

    /**
     * Adds a remote Peer's ICE Candidates to a PeerConnection using webrtc library.
     *
     * @param iceCandidate
     * @param pc
     */
    private void peerConnectionAddIceCandidate(IceCandidate iceCandidate, PeerConnection pc) {
        pc.addIceCandidate(iceCandidate);
    }

    /**
     * Creates a PeerConnection object using webrtc lib.
     *
     * @param peerConnectionFactory
     * @param iceServers
     * @param pcMediaConstraints
     * @param pcObserver
     * @return
     */
    private PeerConnection peerConnectionCreate(
            PeerConnectionFactory peerConnectionFactory,
            List<PeerConnection.IceServer> iceServers, MediaConstraints pcMediaConstraints,
            PeerConnection.Observer pcObserver) {

        PeerConnection pc;
        pc = peerConnectionFactory.createPeerConnection(iceServers,
                pcMediaConstraints, pcObserver);
        return pc;
    }

    /**
     * Set a remote Peer's SDP into the Peer's PC.
     *
     * @param pc
     * @param sdpObserver
     * @param sdp
     */
    private void peerConnectionSetRemoteSdp(PeerConnection pc, SkylinkSdpObserver sdpObserver,
                                            SessionDescription sdp) {
        pc.setRemoteDescription(sdpObserver, sdp);
    }

    /**
     * Creates a SessionDescription object using the SDP's string and type.
     *
     * @param sdpString
     * @param sdpType
     * @return
     */
    private SessionDescription sdpCreate(String sdpString, String sdpType) {
        return new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(sdpType),
                sdpString);
    }

}
