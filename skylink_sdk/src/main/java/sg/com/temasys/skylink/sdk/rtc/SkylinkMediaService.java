package sg.com.temasys.skylink.sdk.rtc;

import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;

/**
 * Created by xiangrong on 25/5/15.
 */
class SkylinkMediaService {
    private static final String TAG = SkylinkMediaService.class.getName();
    private static final String MAX_VIDEO_WIDTH_CONSTRAINT = "maxWidth";
    private static final String MIN_VIDEO_WIDTH_CONSTRAINT = "minWidth";

    private static final String MAX_VIDEO_HEIGHT_CONSTRAINT = "maxHeight";
    private static final String MIN_VIDEO_HEIGHT_CONSTRAINT = "minHeight";

    private static final String MAX_VIDEO_FPS_CONSTRAINT = "maxFrameRate";
    private static final String MIN_VIDEO_FPS_CONSTRAINT = "minFrameRate";
    private VideoCapturerAndroid localVideoCapturer;
    private MediaStream localMediaStream;
    private AudioSource localAudioSource;
    private AudioTrack localAudioTrack;
    private VideoSource localVideoSource;
    private VideoTrack localVideoTrack;
    private VideoRenderer localVideoRender;

    private SkylinkConnection skylinkConnection;
    private PcShared pcShared;

    private int numberOfCameras = 0;

    private boolean cameraUsingFront = true;

    public SkylinkMediaService(SkylinkConnection skylinkConnection,
                               PcShared pcShared) {
        this.skylinkConnection = skylinkConnection;
        this.pcShared = pcShared;
    }

    void addMediaStream(final MediaStream stream, final String peerId, final Object lock) {
        skylinkConnection.runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (lock) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (skylinkConnection.getSkylinkConnectionService().getConnectionState() ==
                            SkylinkConnectionService.ConnectionState.DISCONNECTING) return;

                        /* Note XR:
                        Do not handle Audio or video tracks manually to satisfy hasVideoReceive() and hasAudioReceive(),
                        as webrtc sdp will take care of it, albeit possibly with us doing SDP mangling when we are the answerer.
                        */
                    Utils.abortUnless(stream.audioTracks.size() <= 1
                                    && stream.videoTracks.size() <= 1,
                            "Weird-looking stream: " + stream);
                    GLSurfaceView remoteVideoView = null;
                    int numVideoTracks = stream.videoTracks.size();

                    // As long as a VideoTrack exists, we will render it, even if it turns out to be a totally black view.
                    if ((numVideoTracks >= 1)) {
                        Log.d(TAG, "[addMediaStream] Peer " + peerId + ": " + numVideoTracks +
                                " video track(s) has been added to PeerConnection.");

                        remoteVideoView = createVideoView(stream.videoTracks.get(0),
                                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL,
                                false);

                        final GLSurfaceView rVideoView = remoteVideoView;
                        if (!SkylinkPeerService.isPeerIdMCU(peerId))
                            skylinkConnection.getMediaListener().onRemotePeerMediaReceive(peerId, rVideoView);
                    } else {
                        // If:
                        // This is an audio only stream (audio will be added automatically)
                        // OR
                        // This is a no audio and no video stream
                        // still send a null videoView to alert user stream is received.
                        Log.d(TAG, "[addMediaStream] Peer " + peerId + ": " +
                                "NO video track has been added to PeerConnection.");
                        if (!SkylinkPeerService.isPeerIdMCU(peerId))
                            skylinkConnection.getMediaListener()
                                    .onRemotePeerMediaReceive(peerId, null);
                    }
                }
            }
        });

    }

    /**
     * Generate MediaConstraints for PC and SDP. Populate them with values of Audio and Video
     * receive from SkylinkConfig. Populate PC with P2P security values.
     *
     * @param skylinkConfig SkylinkConfig to get constraint values from.
     */
    void genMediaConstraints(SkylinkConfig skylinkConfig) {
        MediaConstraints[] constraintsArray = new MediaConstraints[2];
        MediaConstraints sdpMediaConstraints = new MediaConstraints();
        MediaConstraints pcMediaConstraints = new MediaConstraints();
        constraintsArray[0] = sdpMediaConstraints;
        constraintsArray[1] = pcMediaConstraints;

        for (MediaConstraints mediaConstraints : constraintsArray) {
            mediaConstraints.mandatory
                    .add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio",
                            String.valueOf(skylinkConfig.hasAudioReceive())));
            mediaConstraints.mandatory
                    .add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo",
                            String.valueOf(skylinkConfig.hasVideoReceive())));
        }

        pcMediaConstraints.optional.add(new MediaConstraints.KeyValuePair(
                "internalSctpDataChannels", "true"));
        pcMediaConstraints.optional.add(new MediaConstraints.KeyValuePair(
                "DtlsSrtpKeyAgreement", "true"));
        pcMediaConstraints.optional.add(new MediaConstraints.KeyValuePair("googDscp",
                "true"));

        // Set to pcShared
        pcShared.setSdpMediaConstraints(sdpMediaConstraints);
        pcShared.setPcMediaConstraints(pcMediaConstraints);
    }

    /**
     * Mutes the local user's audio and notifies all the peers in the room.
     *
     * @param isMuted Flag that specifies whether audio should be mute
     */
    void muteLocalAudio(boolean isMuted) {
        org.webrtc.AudioTrack localAudioTrack = getLocalAudioTrack();

        if (skylinkConnection.getSkylinkConfig().hasAudioSend() &&
                (localAudioTrack.enabled() == isMuted)) {

            localAudioTrack.setEnabled(!isMuted);
            // Inform Peers
            skylinkConnection.getSkylinkConnectionService().sendMuteAudio(isMuted);
        }
    }

    /**
     * Mutes the local user's video and notifies all the peers in the room.
     *
     * @param isMuted Flag that specifies whether video should be mute
     */
    void muteLocalVideo(boolean isMuted) {

        org.webrtc.VideoTrack localVideoTrack = getLocalVideoTrack();
        if (skylinkConnection.getSkylinkConfig().hasVideoSend() &&
                (localVideoTrack.enabled() == isMuted)) {

            localVideoTrack.setEnabled(!isMuted);
            // Inform Peers
            skylinkConnection.getSkylinkConnectionService().sendMuteVideo(isMuted);
        }
    }

    /**
     * Dispose and remove local media streams, sources and tracks
     */
    void removeLocalMedia() {
        localMediaStream = null;
        localAudioTrack = null;
        localVideoTrack = null;

        if (localVideoSource != null) {
            // Stop the video source
            localVideoSource.stop();
            Log.d(TAG, "Stopped local Video Source");
        }

        localVideoSource = null;
        localAudioSource = null;

        // Dispose video capturer
        if (localVideoCapturer != null) {
            localVideoCapturer.dispose();
        }

        localVideoCapturer = null;
    }

    void setVideoConstrains(SkylinkConfig skylinkConfig) {
        MediaConstraints videoConstraints = new MediaConstraints();
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                MIN_VIDEO_WIDTH_CONSTRAINT, Integer.toString(skylinkConfig.getVideoWidth())));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                MAX_VIDEO_WIDTH_CONSTRAINT, Integer.toString(skylinkConfig.getVideoWidth())));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                MIN_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(skylinkConfig.getVideoHeight())));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                MAX_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(skylinkConfig.getVideoHeight())));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                MIN_VIDEO_FPS_CONSTRAINT, Integer.toString(skylinkConfig.getVideoFps())));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                MAX_VIDEO_FPS_CONSTRAINT, Integer.toString(skylinkConfig.getVideoFps())));

        // Add to pcShared
        pcShared.setVideoMediaConstraints(videoConstraints);
    }

    /**
     * Get local media (video and audio) if SkylinkConfig allows it.
     */
    void startLocalMedia(final Object lock) {
        final SkylinkConnectionService.ConnectionState connectionState =
                skylinkConnection.getSkylinkConnectionService().getConnectionState();
        VideoCapturerAndroid localVideoCapturer;
        MediaStream lms;
        AudioSource localAudioSource;
        VideoSource localVideoSource;
        VideoTrack localVideoTrack;
        AudioTrack localAudioTrack;

        // Proceed only if SkylinkConfig sends audio or video
        if (!skylinkConnection.getSkylinkConfig().hasAudioSend() &&
                !skylinkConnection.getSkylinkConfig().hasVideoSend()) {
            return;
        }

        // Prevent thread from executing with disconnect concurrently.
        synchronized (lock) {
            // If user has indicated intention to disconnect,
            // We should no longer process messages from signalling server.
            if (connectionState == SkylinkConnectionService.ConnectionState.DISCONNECTING)
                return;

            if (getLocalMediaStream() == null) {

                Log.d(TAG, "[SDK] Local video source: Creating...");
                lms = pcShared.getPeerConnectionFactory()
                        .createLocalMediaStream("ARDAMS");
                setLocalMediaStream(lms);

                if (skylinkConnection.getSkylinkConfig().hasVideoSend()) {

                    localVideoCapturer = getVideoCapturer();
                    setLocalVideoCapturer(localVideoCapturer);

                    if (localVideoCapturer == null) {
                        throw new RuntimeException("Failed to open capturer");
                    }

                    localVideoSource = pcShared.getPeerConnectionFactory().createVideoSource(
                            localVideoCapturer, pcShared.getVideoMediaConstraints());
                    setLocalVideoSource(localVideoSource);

                    final VideoTrack videoTrack = pcShared.getPeerConnectionFactory()
                            .createVideoTrack("ARDAMSv0", localVideoSource);
                    if (videoTrack != null) {
                        lms.addTrack(videoTrack);
                        localVideoTrack = videoTrack;
                        setLocalVideoTrack(localVideoTrack);
                    }
                }

                skylinkConnection.runOnUiThread(new Runnable() {
                    public void run() {
                        // Prevent thread from executing with disconnect concurrently.
                        synchronized (lock) {
                            // If user has indicated intention to disconnect,
                            // We should no longer process messages from signalling server.
                            if (connectionState == SkylinkConnectionService.ConnectionState.DISCONNECTING)
                                return;

                            GLSurfaceView localVideoView = null;
                            if (skylinkConnection.getSkylinkConfig().hasVideoSend()) {
                                localVideoView = createVideoView(
                                        getLocalVideoTrack(),
                                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL,
                                        true
                                );
                            }

                            Log.d(TAG, "[SDK] Local video source: Created.");
                            skylinkConnection.getMediaListener().onLocalMediaCapture(localVideoView);
                            Log.d(TAG, "[SDK] Local video source: Sent to App.");
                        }
                    }
                });

                // synchronized (lockDisconnect) {
                // If user has indicated intention to disconnect,
                // We should no longer process messages from signalling server.
                if (connectionState == SkylinkConnectionService.ConnectionState.DISCONNECTING)
                    return;
                if (skylinkConnection.getSkylinkConfig().hasAudioSend()) {
                    Log.d(TAG, "[SDK] Local audio source: Creating...");
                    localAudioSource = pcShared.getPeerConnectionFactory()
                            .createAudioSource(new MediaConstraints());
                    localAudioTrack = pcShared.getPeerConnectionFactory()
                            .createAudioTrack("ARDAMSa0",
                                    localAudioSource);
                    setLocalAudioSource(localAudioSource);
                    setLocalAudioTrack(localAudioTrack);
                    lms.addTrack(localAudioTrack);
                    Log.d(TAG, "[SDK] Local audio source: Created.");
                }
                // }
            }

        }
    }

    /**
     * Create and return a GLSurfaceView from a VideoTrack. Knows whether to mirror local video
     * based on SkylinkConfig. No mirroring for remote video.
     *
     * @param videoTrack  To which place the renderer
     * @param scalingType
     * @param isLocal     Whether or not this is for the local video view.
     * @return
     */
    private GLSurfaceView createVideoView(
            VideoTrack videoTrack, VideoRendererGui.ScalingType scalingType, boolean isLocal) {

        boolean mirrorFrontVideo = skylinkConnection.getSkylinkConfig().isMirrorLocalView();
        boolean mirrorThisVideo = false;
        // For local video view, check if config says to mirror it.
        if (isLocal && cameraUsingFront) {
            mirrorThisVideo = mirrorFrontVideo;
        }

        ArrayList<Object> input = genVideoViewFromVideoTrack(videoTrack, scalingType, mirrorThisVideo);
        GLSurfaceView videoView = (GLSurfaceView) input.get(0);

        // For local video view when SkylinkConfig is set to mirror,
        // Remove previous renderer, if any.
        if (isLocal && mirrorFrontVideo) {
            VideoRenderer videoRenderer = (VideoRenderer) input.get(1);
            if (localVideoRender != null) {
                videoTrack.removeRenderer(localVideoRender);
            }
            // Record new local VideoRenderer
            localVideoRender = videoRenderer;
        }
        return videoView;
    }

    /**
     * Create a GLSurfaceView and its VideoRenderer from a VideoTrack.
     *
     * @param videoTrack
     * @param scalingType
     * @param mirror      Whether the output video should be left-right reflected, like a mirror.
     * @return ArrayList containing the GLSurfaceView and its VideoRenderer.
     */
    private ArrayList<Object> genVideoViewFromVideoTrack(
            VideoTrack videoTrack,
            VideoRendererGui.ScalingType scalingType, boolean mirror) {
        GLSurfaceView videoView = new GLSurfaceView(pcShared.getApplicationContext());
        VideoRendererGui videoRendererGui = new VideoRendererGui(videoView);

        MyVideoRendererGuiListener myVideoRendererGuiListener =
                new MyVideoRendererGuiListener();
        videoRendererGui.setListener(myVideoRendererGuiListener);

        // Create and add new renderer
        VideoRenderer videoRenderer = new VideoRenderer(videoRendererGui.create(
                0, 0, 100, 100,
                scalingType,
                mirror));
        videoTrack.addRenderer(videoRenderer);
        ArrayList<Object> output = new ArrayList<Object>();
        output.add(0, (Object) videoView);
        output.add(1, (Object) videoRenderer);
        return output;
    }

    /**
     * Switch camera used between all available cameras on the phone
     * Render videoView again if local front videoView set to be mirrored.
     *
     * @param lifeCycleListener
     */

    boolean switchCameraAndRender(final LifeCycleListener lifeCycleListener) {
        // Switch camera
        boolean success = false;
        String strLog = "";
        // Try to switch camera
        /*if (numberOfCameras < 2 || getLocalVideoCapturer() == null) {
            // No video is sent or only one camera is available,
            strLog = "Failed to switch camera. Number of cameras: " + numberOfCameras + ".";
        } else {
            success = getLocalVideoCapturer().switchCamera(null);
        }*/

        strLog = switchCamera();
        if (strLog == null) {
            success = true;
        }

        // Log about success or failure in switching camera.
        if (success) {
            strLog = "Switched camera.";
            Log.d(TAG, strLog);
            cameraUsingFront = !cameraUsingFront;
            // Change videoView if config sets front camera to be mirrored
            if (skylinkConnection.getSkylinkConfig().isMirrorLocalView()) {
                GLSurfaceView localVideoView = createVideoView(getLocalVideoTrack(),
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL,
                        true
                );
                Log.d(TAG, "[switchCamera] New local video view and renderer created.");
                skylinkConnection.getMediaListener().onLocalMediaCapture(localVideoView);
                Log.d(TAG, "[switchCamera] New local video view sent to App.");
            }
        } else {
            // Switch is pending or error while trying to switch.
            lifeCycleListener.onWarning(ErrorCodes.VIDEO_SWITCH_CAMERA_ERROR, strLog);
            Log.e(TAG, strLog);
        }
        return success;
    }

    /**
     * Switch camera used between all available cameras on the phone.
     *
     * @return null if successful or the error message if not.
     */
    String switchCamera() {
        boolean success = false;
        String strLog = null;
        // Try to switch camera
        if (numberOfCameras < 2 || getLocalVideoCapturer() == null) {
            // No video is sent or only one camera is available,
            strLog = "Failed to switch camera. Number of cameras: " + numberOfCameras + ".";
        } else {
            success = getLocalVideoCapturer().switchCamera(null);
            if (!success) {
                strLog = "Encountered error when switching camera, even though we have at least 2" +
                        " cameras (Number of cameras: " + numberOfCameras + ".";
            }
        }
        return strLog;
    }

    /**
     * Cycle through likely device names for the camera and return the first capturer that works.
     * Return null if none available.
     *
     * @return
     */

    private VideoCapturerAndroid getVideoCapturer() {
        // Check if there is a camera on device and disable video call if not.
        numberOfCameras = VideoCapturerAndroid.getDeviceCount();
        if (numberOfCameras == 0) {
            Log.w(TAG, "No camera on device. Video call will not be possible.");
            return null;
        }

        String frontCameraDeviceName = VideoCapturerAndroid.getNameOfFrontFacingDevice();
        Log.d(TAG, "Opening camera: " + frontCameraDeviceName);

        return VideoCapturerAndroid.create(frontCameraDeviceName);
    }

    MediaStream getLocalMediaStream() {
        return localMediaStream;
    }

    /*
     * VideoRendererGui.VideoRendererGuiListener
     */
    private class MyVideoRendererGuiListener implements
            VideoRendererGuiListener {

        private String peerId = null;

        public String getPeerId() {
            return peerId;
        }

        public void setPeerId(String peerId) {
            this.peerId = peerId;
        }

        @Override
        public void updateDisplaySize(final Point screenDimensions) {
            skylinkConnection.runOnUiThread(new Runnable() {
                @SuppressWarnings("unused")
                public void run() {
                    // Prevent thread from executing with disconnect concurrently.
                    synchronized (skylinkConnection.getLockDisconnectMedia()) {
                        // If user has indicated intention to disconnect,
                        // We should no longer process messages from signalling server.
                        if (skylinkConnection.getSkylinkConnectionService().getConnectionState() ==
                                SkylinkConnectionService.ConnectionState.DISCONNECTING) {
                            return;
                        }
                        skylinkConnection.getMediaListener().onVideoSizeChange(peerId, screenDimensions);
                    }
                }
            });
        }
    }

    // Getters and Setters
    public boolean isCameraUsingFront() {
        return cameraUsingFront;
    }

    public int getNumberOfCameras() {
        return numberOfCameras;
    }

    public void setNumberOfCameras(int numberOfCameras) {
        this.numberOfCameras = numberOfCameras;
    }

    void setLocalMediaStream(MediaStream localMediaStream) {
        this.localMediaStream = localMediaStream;
    }

    VideoCapturerAndroid getLocalVideoCapturer() {
        return localVideoCapturer;
    }

    void setLocalVideoCapturer(VideoCapturerAndroid localVideoCapturer) {
        this.localVideoCapturer = localVideoCapturer;
    }

    AudioSource getLocalAudioSource() {
        return localAudioSource;
    }

    void setLocalAudioSource(AudioSource localAudioSource) {
        this.localAudioSource = localAudioSource;
    }

    VideoSource getLocalVideoSource() {
        return localVideoSource;
    }

    void setLocalVideoSource(VideoSource localVideoSource) {
        this.localVideoSource = localVideoSource;
    }

    AudioTrack getLocalAudioTrack() {
        return localAudioTrack;
    }

    void setLocalAudioTrack(AudioTrack localAudioTrack) {
        this.localAudioTrack = localAudioTrack;
    }

    VideoTrack getLocalVideoTrack() {
        return localVideoTrack;
    }

    void setLocalVideoTrack(VideoTrack localVideoTrack) {
        this.localVideoTrack = localVideoTrack;
    }

}
