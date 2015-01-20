package sg.com.temasys.skylink.sdk.rtc;

import android.graphics.Point;
import android.opengl.GLSurfaceView;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;

/**
 * @author temasys
 */
class LifeCycleAdapter implements LifeCycleListener {

    /**
     *
     */
    public LifeCycleAdapter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onConnect(boolean isSuccess, String message) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGetUserMedia(GLSurfaceView videoView, Point size) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onWarning(String message) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDisconnect(String message) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onReceiveLog(String message) {
        // TODO Auto-generated method stub

    }

}
