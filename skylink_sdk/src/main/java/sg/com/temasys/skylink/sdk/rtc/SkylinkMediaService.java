package sg.com.temasys.skylink.sdk.rtc;

import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

/**
 * Created by xiangrong on 25/5/15.
 */
class SkylinkMediaService {
    private static final String TAG = SkylinkMediaService.class.getName();
    private SkylinkConnection skylinkConnection;
    private SkylinkConnectionService skylinkConnectionService;

    public SkylinkMediaService(SkylinkConnection skylinkConnection,
                               SkylinkConnectionService skylinkConnectionService) {
        this.skylinkConnection = skylinkConnection;
        this.skylinkConnectionService = skylinkConnectionService;
    }

    void addMediaStream(final MediaStream stream, final String peerId, final Object lock) {
        skylinkConnection.runOnUiThread(new Runnable() {
            public void run() {
                // Prevent thread from executing with disconnect concurrently.
                synchronized (lock) {
                    // If user has indicated intention to disconnect,
                    // We should no longer process messages from signalling server.
                    if (skylinkConnection.getConnectionState() ==
                            SkylinkConnection.ConnectionState.DISCONNECT) return;

                        /* Note XR:
                        Do not handle Audio or video tracks manually to satisfy hasVideoReceive() and hasAudioReceive(),
                        as webrtc sdp will take care of it, albeit possibly with us doing SDP mangling when we are the answerer.
                        */
                    skylinkConnection.abortUnless(stream.audioTracks.size() <= 1
                                    && stream.videoTracks.size() <= 1,
                            "Weird-looking stream: " + stream);
                    GLSurfaceView remoteVideoView = null;
                    // As long as a VideoTrack exists, we will render it, even if it turns out to be a totally black view.
                    if ((stream.videoTracks.size() >= 1)) {
                        remoteVideoView = new GLSurfaceView(skylinkConnection.getApplicationContext());

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
                        if (!skylinkConnection.isPeerIdMCU(peerId))
                            skylinkConnection.getMediaListener().onRemotePeerMediaReceive(peerId, rVideoView);
                    } else {
                        // If:
                        // This is an audio only stream (audio will be added automatically)
                        // OR
                        // This is a no audio and no video stream
                        // still send a null videoView to alert user stream is received.
                        if (!skylinkConnection.isPeerIdMCU(peerId))
                            skylinkConnection.getMediaListener()
                                    .onRemotePeerMediaReceive(peerId, null);
                    }
                }
            }
        });

    }

    /**
     * Mutes the local user's audio and notifies all the peers in the room.
     *
     * @param isMuted Flag that specifies whether audio should be mute
     */
    void muteLocalAudio(boolean isMuted) {
        org.webrtc.AudioTrack localAudioTrack = skylinkConnection.getLocalAudioTrack();

        if (skylinkConnection.getMyConfig().hasAudioSend() &&
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
        if (skylinkConnection.getMyConfig().hasVideoSend() &&
                (localVideoTrack.enabled() == isMuted)) {

            localVideoTrack.setEnabled(!isMuted);
            // Inform Peers
            skylinkConnectionService.sendMuteVideo(isMuted);
        }
    }

    void startLocalMedia(final Object lock) {
        final SkylinkConnection.ConnectionState connectionState =
                skylinkConnection.getConnectionState();
        VideoCapturerAndroid localVideoCapturer;
        MediaStream lms;
        AudioSource localAudioSource;
        VideoSource localVideoSource;
        VideoTrack localVideoTrack;
        AudioTrack localAudioTrack;
        PeerConnectionFactory peerConnectionFactory = skylinkConnection.getPeerConnectionFactory();

        // Prevent thread from executing with disconnect concurrently.
        synchronized (lock) {
            // If user has indicated intention to disconnect,
            // We should no longer process messages from signalling server.
            if (connectionState == SkylinkConnection.ConnectionState.DISCONNECT)
                return;

            if (peerConnectionFactory == null) {
                peerConnectionFactory = new PeerConnectionFactory();
                skylinkConnection.setPeerConnectionFactory(peerConnectionFactory);

                Log.d(TAG, "[SDK] Local video source: Creating...");
                lms = peerConnectionFactory
                        .createLocalMediaStream("ARDAMS");
                skylinkConnection.setLocalMediaStream(lms);

                if (skylinkConnection.getMyConfig().hasVideoSend()) {

                    localVideoCapturer = getVideoCapturer();
                    skylinkConnection.setLocalVideoCapturer(localVideoCapturer);

                    if (localVideoCapturer == null) {
                        throw new RuntimeException("Failed to open capturer");
                    }

                    localVideoSource = peerConnectionFactory
                            .createVideoSource(localVideoCapturer,
                                    skylinkConnection.getVideoConstraints());
                    skylinkConnection.setLocalVideoSource(localVideoSource);

                    final VideoTrack videoTrack = peerConnectionFactory
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
                            if (connectionState == SkylinkConnection.ConnectionState.DISCONNECT)
                                return;
                            GLSurfaceView localVideoView = null;
                            VideoRendererGui localVideoRendererGui;
                            if (skylinkConnection.getMyConfig().hasVideoSend()) {
                                localVideoView = new GLSurfaceView(skylinkConnection.getApplicationContext());
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
                if (connectionState == SkylinkConnection.ConnectionState.DISCONNECT) return;
                if (skylinkConnection.getMyConfig().hasAudioSend()) {
                    Log.d(TAG, "[SDK] Local audio source: Creating...");
                    localAudioSource = peerConnectionFactory
                            .createAudioSource(new MediaConstraints());
                    localAudioTrack = peerConnectionFactory
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
     * Cycle through likely device names for the camera and return the first capturer that works, or
     * crash if none do.
     *
     * @return
     */
    private VideoCapturerAndroid getVideoCapturer() {
        String frontCameraDeviceName =
                VideoCapturerAndroid.getNameOfFrontFacingDevice();
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
                        if (skylinkConnection.getConnectionState() ==
                                SkylinkConnection.ConnectionState.DISCONNECT) {
                            return;
                        }
                        skylinkConnection.getMediaListener().onVideoSizeChange(peerId, screenDimensions);
                    }
                }
            });
        }
    }

}
