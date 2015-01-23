package sg.com.temasys.skylink.sdk.sample;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

public class Utility {

    final static public int REQUEST_CODE_PICK_DIR = 6383;
    final static public int REQUEST_CODE_PICK_FILE = 6384;

    private static Toast toastRapid = null;

    public static void showShortToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showShortToast(Context context, int messageId,
                                      String... args) {
        Toast.makeText(context,
                String.format(context.getString(messageId), (Object[]) args),
                Toast.LENGTH_SHORT).show();
    }

    // This will cancel any pending toast to toast the current toast.
    // This is suitable for rapid fire toasting where
    // it is not essential to show every single toast, like showing progress of
    // file transfer.
    public static void showRapidShortToast(Context context, int messageId,
                                           String... args) {
        if (toastRapid != null)
            toastRapid.cancel();
        toastRapid = Toast.makeText(context,
                String.format(context.getString(messageId), (Object[]) args),
                Toast.LENGTH_SHORT);
        toastRapid.show();
    }

    public static void layoutSubviews(GLSurfaceView surfaceView, Point size) {
        if (size == null) {
            size = RoomManager.get().getRemoteVideoSize(surfaceView);
            if (size == null)
                return;
        }
        if (surfaceView.getParent() == null)
            return;
        Point maximumSize = new Point(
                ((RelativeLayout) surfaceView.getParent()).getMeasuredWidth(),
                ((RelativeLayout) surfaceView.getParent()).getMeasuredHeight());
        if (maximumSize.x == 0 || maximumSize.y == 0)
            return;
        Point takenSize = resize(size, maximumSize);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) surfaceView
                .getLayoutParams();
        if (takenSize.x < maximumSize.x) {
            int delta = (maximumSize.x - takenSize.x) / 2;
            layoutParams.setMargins(delta, 0, delta, 0);
        } else {
            int delta = (maximumSize.y - takenSize.y) / 2;
            layoutParams.setMargins(0, delta, 0, delta);
        }
        surfaceView.setLayoutParams(layoutParams);
    }

    private static Point resize(Point actualSize, Point maximumSize) {
        float ratio = Math.min((float) maximumSize.x / (float) actualSize.x,
                (float) maximumSize.y / (float) actualSize.y);
        return new Point((int) (ratio * (float) actualSize.x),
                (int) (ratio * (float) actualSize.y));
    }

    public static void showChooser(Activity callingActivity, String peerId,
                                   boolean isPrivate) {
        // Use the GET_CONTENT intent from the utility class
        Intent target = FileUtils.createGetContentIntent();
        // Create the chooser Intent
        Intent intent = Intent.createChooser(target,
                callingActivity.getString(R.string.chooser_title));
        try {
            callingActivity.startActivityForResult(intent,
                    REQUEST_CODE_PICK_FILE);
        } catch (ActivityNotFoundException e) {
            // The reason for the existence of aFileChooser
        }
    }

}
