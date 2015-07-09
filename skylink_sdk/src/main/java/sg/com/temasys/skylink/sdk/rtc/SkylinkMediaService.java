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

    private SkylinkConnection skylinkConnection;
    private SkylinkConnectionService skylinkConnectionService;
    private PcShared pcShared;

    private int numberOfCameras = 0;

    public SkylinkMediaService(SkylinkConnection skylinkConnection,
                               SkylinkConnectionService skylinkConnectionService,
                               PcShared pcShared) {
        this.skylinkConnection = skylinkConnection;
        this.skylinkConnectionService = skylinkConnectionService;
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
                    skylinkConnection.abortUnless(stream.audioTracks.size() <= 1
                                    && stream.videoTracks.size() <= 1,
                            "Weird-looking stream: " + stream);
                    GLSurfaceView remoteVideoView = null;
                    int numVideoTracks = stream.videoTracks.size();

                    // As long as a VideoTrack exists, we will render it, even if it turns out to be a totally black view.
                    if ((numVideoTracks >= 1)) {
                        Log.d(TAG, "[addMediaStream] Peer " + peerId + ": " + numVideoTracks +
                                " video track(s) has been added to PeerConnection.");
                        remoteVideoView = new GLSurfaceView(pcShared.getApplicationContext());

                        VideoRendererGui gui = new VideoRendererGui(remoteVideoView);
                        MyVideoRendererGuiListener myVideoRendererGuiListener =
                                new MyVideoRendererGuiListener();
                        myVideoRendererGuiListener.setPeerId(peerId);
                        gui.setListener(myVideoRendererGuiListener);

                        VideoRenderer.Callbacks remoteRender = gui.create(0, 0,
                                100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
                        stream.videoTracks.get(0).addRenderer(
                                new VideoRenderer(remoteRender));

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
        org.webrtc.AudioTrack localAudioTrack = skylinkConnection.getLocalAudioTrack();

        if (skylinkConnection.getSkylinkConfig().hasAudioSend() &&
                (localAudioTrack.enabled() == isMuted)) {

            localAudioTrack.setEnabled(!isMuted);
            // Inform Peers
            skylinkConnectionService.sendMuteAudio(isMuted);
        }
    }

    /**
     * Mutes the local user's video and notifies all the peers in the room.
     *
     * @param isMuted Flag that specifies whether video should be mute
     */
    void muteLocalVideo(boolean isMuted) {

        org.webrtc.VideoTrack localVideoTrack = skylinkConnection.getLocalVideoTrack();
        if (skylinkConnection.getSkylinkConfig().hasVideoSend() &&
                (localVideoTrack.enabled() == isMuted)) {

            localVideoTrack.setEnabled(!isMuted);
            // Inform Peers
            skylinkConnectionService.sendMuteVideo(isMuted);
        }
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

        // Prevent thread from executing with disconnect concurrently.
        synchronized (lock) {
            // If user has indicated intention to disconnect,
            // We should no longer process messages from signalling server.
            if (connectionState == SkylinkConnectionService.ConnectionState.DISCONNECTING)
                return;

            if (skylinkConnection.getLocalMediaStream() == null) {

                Log.d(TAG, "[SDK] Local video source: Creating...");
                lms = pcShared.getPeerConnectionFactory()
                        .createLocalMediaStream("ARDAMS");
                skylinkConnection.setLocalMediaStream(lms);

                if (skylinkConnection.getSkylinkConfig().hasVideoSend()) {

                    localVideoCapturer = getVideoCapturer();
                    skylinkConnection.setLocalVideoCapturer(localVideoCapturer);

                    if (localVideoCapturer == null) {
                        throw new RuntimeException("Failed to open capturer");
                    }

                    localVideoSource = pcShared.getPeerConnectionFactory().createVideoSource(
                            localVideoCapturer, pcShared.getVideoMediaConstraints());
                    skylinkConnection.setLocalVideoSource(localVideoSource);

                    final VideoTrack videoTrack = pcShared.getPeerConnectionFactory()
                            .createVideoTrack("ARDAMSv0", localVideoSource);
                    if (videoTrack != null) {
                        lms.addTrack(videoTrack);
                        localVideoTrack = videoTrack;
                        skylinkConnection.setLocalVideoTrack(localVideoTrack);
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
                            VideoRendererGui localVideoRendererGui;
                            if (skylinkConnection.getSkylinkConfig().hasVideoSend()) {
                                localVideoView = new GLSurfaceView(pcShared.getApplicationContext());
                                localVideoRendererGui = new VideoRendererGui(localVideoView);

                                MyVideoRendererGuiListener myVideoRendererGuiListener =
                                        new MyVideoRendererGuiListener();
                                localVideoRendererGui.setListener(myVideoRendererGuiListener);


                                VideoRenderer.Callbacks localRender = localVideoRendererGui.create(0,
                                        0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
                                skylinkConnection.getLocalVideoTrack().addRenderer(new VideoRenderer(localRender));
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
                    skylinkConnection.setLocalAudioSource(localAudioSource);
                    skylinkConnection.setLocalAudioTrack(localAudioTrack);
                    lms.addTrack(localAudioTrack);
                    Log.d(TAG, "[SDK] Local audio source: Created.");
                }
                // }
            }

        }
    }

    /**
     * Call the internal function to switch camera.
     *
     * @param lifeCycleListener
     */

    boolean switchCamera(final LifeCycleListener lifeCycleListener) {
        // Switch camera
        boolean success = false;
        String strLog = "";
        // Try to switch camera
        if (numberOfCameras < 2 || skylinkConnection.getLocalVideoCapturer() == null) {
            // No video is sent or only one camera is available,
            strLog = "Failed to switch camera. Number of cameras: " + numberOfCameras + ".";
        } else {
            success = skylinkConnection.getLocalVideoCapturer().switchCamera(null);
        }
        // Log about success or failure in switching camera.
        if (success) {
            strLog = "Switched camera.";
            Log.d(TAG, strLog);
        } else {
            // Switch is pending or error while trying to switch.
            lifeCycleListener.onWarning(ErrorCodes.VIDEO_SWITCH_CAMERA_ERROR, strLog);
            Log.e(TAG, strLog);
        }
        return success;
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
    public int getNumberOfCameras() {
        return numberOfCameras;
    }

    public void setNumberOfCameras(int numberOfCameras) {
        this.numberOfCameras = numberOfCameras;
    }

}
