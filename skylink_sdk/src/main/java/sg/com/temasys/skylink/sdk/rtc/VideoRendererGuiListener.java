package sg.com.temasys.skylink.sdk.rtc;

import android.graphics.Point;
import android.opengl.GLSurfaceView;

interface VideoRendererGuiListener {
    void updateDisplaySize(GLSurfaceView surface, Point screenDimensions, String peerId);
}
