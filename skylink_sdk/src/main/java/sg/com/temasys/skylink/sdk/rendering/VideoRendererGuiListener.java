package sg.com.temasys.skylink.sdk.rendering;

import android.graphics.Point;
import android.opengl.GLSurfaceView;

public interface VideoRendererGuiListener {
    void updateDisplaySize(GLSurfaceView surface, Point screenDimensions);
}
