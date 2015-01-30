package sg.com.temasys.skylink.sdk.listener;

import android.graphics.Point;
import android.opengl.GLSurfaceView;

/**
 * Listener comprises of callbacks related to audio / video manipulation
 * during the call.
 */
public interface MediaListener {

    /**
     * This is triggered when the framework successfully captures the camera
     * input from one's device if the connection is configured to have a
     * video call.
     *
     * @param videoView Video of oneself
     * @param size      Size of the video frame
     */
    public void onLocalMediaCapture(GLSurfaceView videoView, Point size);

    /**
     * This is triggered when any of the given video streams' frame size
     * changes. It includes the self stream also.
     *
     * @param videoView The video view for which the frame size is changed
     * @param size      Size of the video frame
     */
    void onVideoSizeChange(GLSurfaceView videoView, Point size);

    /**
     * This is triggered when a remote peer enable / disable its audio.
     *
     * @param remotePeerId The id of the remote peer
     * @param isMuted      Flag specifying whether the audio is muted or not
     */
    void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted);

    /**
     * This is triggered when a peer enable / disable its video.
     *
     * @param remotePeerId The id of the remote peer
     * @param isMuted      Flag specifying whether the video is muted or not
     */
    void onRemotePeerVideoToggle(String remotePeerId, boolean isMuted);

    /**
     * The is triggered upon receiving the media stream of the remote peer if the
     * connection is configured to have a audio and/or video call.
     *
     * @param remotePeerId The id of the peer
     * @param videoView    Video of the peer
     * @param size         Size of the peer video frame
     */
    public void onRemotePeerMediaReceive(String remotePeerId, GLSurfaceView videoView,
                                         Point size);

}
