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
    // To be shared amongst PeerConnection related classes.
    private static PeerConnectionFactory peerConnectionFactory;
    // Internal variables
    private static boolean factoryStaticInitialized = false;
    // Static variables
    private static String TAG = PcShared.class.getSimpleName();

    private Context applicationContext;
    private MediaConstraints pcMediaConstraints;
    private MediaConstraints sdpMediaConstraints;
    private MediaConstraints videoMediaConstraints;

    PcShared(Context applicationContext) {
        this.applicationContext = applicationContext;
        // Initialize PeerConnectionFactory.
        initializePcFactory(this.applicationContext);
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
     *  @param context
     *
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

        // Create peerConnectionFactory if required
            // Only 1 instance required.
        if (peerConnectionFactory == null) {
            peerConnectionFactory = new PeerConnectionFactory();
        }
    }

    // Getters and Setters
    PeerConnectionFactory getPeerConnectionFactory() {
        return peerConnectionFactory;
    }

    Context getApplicationContext() {
        return applicationContext;
    }

    void setApplicationContext(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    MediaConstraints getPcMediaConstraints() {
        return pcMediaConstraints;
    }

    void setPcMediaConstraints(MediaConstraints pcMediaConstraints) {
        this.pcMediaConstraints = pcMediaConstraints;
    }

    MediaConstraints getSdpMediaConstraints() {
        return sdpMediaConstraints;
    }

    void setSdpMediaConstraints(MediaConstraints sdpMediaConstraints) {
        this.sdpMediaConstraints = sdpMediaConstraints;
    }

    MediaConstraints getVideoMediaConstraints() {
        return videoMediaConstraints;
    }

    void setVideoMediaConstraints(MediaConstraints videoMediaConstraints) {
        this.videoMediaConstraints = videoMediaConstraints;
    }

}
