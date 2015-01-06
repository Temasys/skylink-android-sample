package sg.com.temasys.skylink.sdk.rtc;

import android.graphics.Point;
import android.opengl.GLSurfaceView;

/**
 * @author temasys
 */
public class MediaAdapter implements SkyLinkConnection.MediaDelegate {

    /**
     *
     */
    public MediaAdapter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onVideoSize(GLSurfaceView videoView, Point size) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onToggleAudio(String peerId, boolean isMuted) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onToggleVideo(String peerId, boolean isMuted) {
        // TODO Auto-generated method stub

    }

}
