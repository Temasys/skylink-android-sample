package sg.com.temasys.skylink.sdk.listener;

import android.graphics.Point;
import android.opengl.GLSurfaceView;

/**
 * Listener comprises of callbacks related to audio / video manipulation
 * during the call.
 */
public interface MediaListener {
    /**
     * This is triggered when any of the given video streams' frame size
     * changes. It includes the self stream also.
     *
     * @param videoView The video view for which the frame size is changed
     * @param size      Size of the video frame
     */
    void onVideoSize(GLSurfaceView videoView, Point size);

    /**
     * This is triggered when a peer enable / disable its audio.
     *
     * @param peerId  The id of the peer
     * @param isMuted Flag specifying whether the audio is muted or not
     */
    void onToggleAudio(String peerId, boolean isMuted);

    /**
     * This is triggered when a peer enable / disable its video.
     *
     * @param peerId  The id of the peer
     * @param isMuted Flag specifying whether the video is muted or not
     */
    void onToggleVideo(String peerId, boolean isMuted);
}
