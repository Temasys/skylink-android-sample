package sg.com.temasys.skylink.sdk.rtc;

import android.graphics.Point;
import android.opengl.GLSurfaceView;

import sg.com.temasys.skylink.sdk.listener.MediaListener;

/**
 * @author Temasys Communications Pte Ltd
 */
class MediaAdapter implements MediaListener {

    /**
     *
     */
    public MediaAdapter() {
    }

    @Override
    public void onVideoSizeChange(GLSurfaceView videoView, Point size) {

    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {

    }

    @Override
    public void onRemotePeerVideoToggle(String remotePeerId, boolean isMuted) {

    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, GLSurfaceView videoView, Point size) {

    }

    @Override
    public void onLocalMediaCapture(GLSurfaceView videoView, Point size) {

    }

}
