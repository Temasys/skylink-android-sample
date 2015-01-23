package sg.com.temasys.skylink.sdk.rendering;

import android.graphics.Point;
import android.opengl.GLSurfaceView;

/**
 * Created by janidu on 22/1/15.
 */
public interface VideoRendererGuiListener {
    void updateDisplaySize(GLSurfaceView surface, Point screenDimensions);
}
