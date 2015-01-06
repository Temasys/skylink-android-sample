package sg.com.temasys.skylink.sdk.rtc;

import android.graphics.Point;
import android.opengl.GLSurfaceView;

/**
 * @author temasys
 */
public class RemotePeerAdapter implements SkyLinkConnection.RemotePeerDelegate {

    /**
     *
     */
    public RemotePeerAdapter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onPeerJoin(String peerId, Object userData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGetPeerMedia(String peerId, GLSurfaceView videoView,
                               Point size) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUserData(String peerId, Object userData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPeerLeave(String peerId, String message) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOpenDataConnection(String peerId) {
        // TODO Auto-generated method stub

    }

}
