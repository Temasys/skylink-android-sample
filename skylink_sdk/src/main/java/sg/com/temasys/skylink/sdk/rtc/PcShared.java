package sg.com.temasys.skylink.sdk.rtc;

import android.content.Context;
import android.opengl.EGLContext;

import org.webrtc.MediaCodecVideoEncoder;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoRendererGui;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;

/**
 * Created by xiangrong on 9/7/15.
 */

/**
 * Shares object related to PeerConnection.
 */
class PcShared {
    // To be shared amongst PeerConnection related classes.
    private PeerConnectionFactory peerConnectionFactory;
    // Internal variables
    private static boolean factoryStaticInitialized = false;
    // Static variables
    private static String TAG = PcShared.class.getName();
    // Flags to track if specific hardware acceleration is available on Device.
    public static boolean vp8HwSupported = MediaCodecVideoEncoder.isVp8HwSupported();
    public static boolean h264HwSupported = MediaCodecVideoEncoder.isH264HwSupported();

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
            logD(TAG, "Peer Connection Factory disposing...");
            peerConnectionFactory.dispose();
            logD(TAG, "Peer Connection Factory Disposed!");
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
            String info = "[INFO] Following hardware acceleration is supported on this device:\n" +
                    "VP8: " + vp8HwSupported + "\nH264: " + h264HwSupported + ".";
            // Enable hardware acceleration if supported
            if (vp8HwSupported || h264HwSupported) {
                hardwareAccelerated = true;
                info += "\nHardware acceleration enabled in SkylinkConnection!";
            } else {
                info += "\nHardware acceleration not enabled in SkylinkConnection!";
            }
            logD(TAG, info);

            /*
            Note XR:
             PeerConnectionFactory.initializeAndroidGlobals to always use true for initializeAudio
             and initializeVideo, as otherwise, new PeerConnectionFactory() crashes.
            */
            Utils.abortUnless(PeerConnectionFactory.initializeAndroidGlobals(context,
                    true, true, hardwareAccelerated), "Failed to initializeAndroidGlobals");
            factoryStaticInitialized = true;
        }

        // Create peerConnectionFactory if required
        // Only 1 instance required.
        if (peerConnectionFactory == null) {
            peerConnectionFactory = new PeerConnectionFactory();
            EGLContext eglContext = VideoRendererGui.getEGLContext();
            peerConnectionFactory.setVideoHwAccelerationOptions(eglContext);
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
