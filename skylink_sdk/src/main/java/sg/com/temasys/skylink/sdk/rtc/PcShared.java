package sg.com.temasys.skylink.sdk.rtc;

import android.content.Context;

import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;

/**
 * Created by xiangrong on 9/7/15.
 */

/**
 * Shares object related to PeerConnection.
 */
public class PcShared {
    private PeerConnectionFactory peerConnectionFactory;
    private Context applicationContext;
    private MediaConstraints pcMediaConstraints;
    private MediaConstraints sdpMediaConstraints;
    private MediaConstraints videoMediaConstraints;

    public PcShared() {
        peerConnectionFactory = new PeerConnectionFactory();
    }

    // Getters and Setters
    public PeerConnectionFactory getPeerConnectionFactory() {
        return peerConnectionFactory;
    }

    public void setPeerConnectionFactory(PeerConnectionFactory peerConnectionFactory) {
        this.peerConnectionFactory = peerConnectionFactory;
    }

    public Context getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public MediaConstraints getPcMediaConstraints() {
        return pcMediaConstraints;
    }

    public void setPcMediaConstraints(MediaConstraints pcMediaConstraints) {
        this.pcMediaConstraints = pcMediaConstraints;
    }

    public MediaConstraints getSdpMediaConstraints() {
        return sdpMediaConstraints;
    }

    public void setSdpMediaConstraints(MediaConstraints sdpMediaConstraints) {
        this.sdpMediaConstraints = sdpMediaConstraints;
    }

    public MediaConstraints getVideoMediaConstraints() {
        return videoMediaConstraints;
    }

    public void setVideoMediaConstraints(MediaConstraints videoMediaConstraints) {
        this.videoMediaConstraints = videoMediaConstraints;
    }
}
