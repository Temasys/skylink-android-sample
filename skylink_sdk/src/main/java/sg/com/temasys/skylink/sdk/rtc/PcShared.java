package sg.com.temasys.skylink.sdk.rtc;

import android.content.Context;
import android.opengl.EGLContext;
import android.util.Log;

import org.webrtc.MediaCodecVideoEncoder;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;

/**
 * Created by xiangrong on 9/7/15.
 */

/**
 * Shares object related to PeerConnection.
 */
class PcShared {
    // Static variables
    private static String TAG = PcShared.class.getSimpleName();

    // To be shared amongst PeerConnection related classes.
    private PeerConnectionFactory peerConnectionFactory;
    private Context applicationContext;
    private MediaConstraints pcMediaConstraints;
    private MediaConstraints sdpMediaConstraints;
    private MediaConstraints videoMediaConstraints;

    // Internal variables
    private boolean factoryStaticInitialized = false;

    PcShared(Context applicationContext) {
        this.applicationContext = applicationContext;
        // Initialize PeerConnectionFactory.
        initializePcFactory(this.applicationContext);
        peerConnectionFactory = new PeerConnectionFactory();
    }

    // APIs

    /**
     * Dispose and remove PeerConnectionFactory
     */
    void removePcFactory() {
        if (peerConnectionFactory != null) {
            Log.d(TAG, "Disposing Peer Connection Factory");
            peerConnectionFactory.dispose();
            Log.d(TAG, "Disposed Peer Connection Factory");
            peerConnectionFactory = null;
        }
    }

    // Internal methods

    /**
     * Initialize PeerConnectionFactory
     *
     * @param context
     */
    private void initializePcFactory(Context context) {
        if (!factoryStaticInitialized) {

            boolean hardwareAccelerated = false;
            EGLContext eglContext = null;

            // Enable hardware acceleration if supported
            if (MediaCodecVideoEncoder.isVp8HwSupported()) {
                hardwareAccelerated = true;
                eglContext = VideoRendererGui.getEGLContext();
                Log.d(TAG, "Enabled hardware acceleration");
            }

            /*
            Note XR:
             PeerConnectionFactory.initializeAndroidGlobals to always use true for initializeAudio
             and initializeVideo, as otherwise, new PeerConnectionFactory() crashes.
            */
            Utils.abortUnless(PeerConnectionFactory.initializeAndroidGlobals(context,
                    true, true, hardwareAccelerated, eglContext
            ), "Failed to initializeAndroidGlobals");

            factoryStaticInitialized = true;
        }
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
