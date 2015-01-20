package sg.com.temasys.skylink.sdk.rtc;

import android.graphics.Point;
import android.opengl.GLSurfaceView;

import sg.com.temasys.skylink.sdk.listener.MediaListener;

/**
 * @author temasys
 */
class MediaAdapter implements MediaListener {

    /**
     *
     */
    public MediaAdapter() {
    }

    @Override
    public void onVideoSize(GLSurfaceView videoView, Point size) {

    }

    @Override
    public void onToggleAudio(String peerId, boolean isMuted) {

    }

    @Override
    public void onToggleVideo(String peerId, boolean isMuted) {

    }

}
