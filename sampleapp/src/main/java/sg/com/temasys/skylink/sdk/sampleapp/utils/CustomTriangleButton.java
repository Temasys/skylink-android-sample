package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import sg.com.temasys.skylink.sdk.sampleapp.R;

public class CustomTriangleButton extends AppCompatButton {

    private Context context;
    private Paint iconPaint, backgroundNormalPaint, backgroundClickedMainPaint;
    private ButtonState state = ButtonState.NORMAL;
    private ButtonDirection currentDirection = ButtonDirection.TOP_LEFT;

    public enum ButtonState {
        NORMAL,
        CLICKED
    }

    public enum ButtonDirection {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public CustomTriangleButton(Context context) {
        super(context);
        this.context = context;
        init(null, context);
    }

    public CustomTriangleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs, context);
    }

    public CustomTriangleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(attrs, context);
    }

    private void init(AttributeSet attrs, Context context) {
        // create icon paint object
        this.iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // set style
        this.iconPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        // set color
        this.iconPaint.setColor(getResources().getColor(android.R.color.white));

        // create normal background paint object
        this.backgroundNormalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // set style
        this.backgroundNormalPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        // set color
        this.backgroundNormalPaint.setColor(getResources().getColor(R.color.primary));

        // create clicked background paint object for main triangle
        this.backgroundClickedMainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // set style
        this.backgroundClickedMainPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        // set color
        this.backgroundClickedMainPaint.setColor(getResources().getColor(R.color.primary_dark));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerXOnView = getWidth() / 2;
        int centerYOnView = getHeight() / 2;

        // draw background is a triangle
        drawTriangleMain(canvas, getDirection());

        int centerPosX = centerXOnView / 3 * 2;
        int centerPosY = centerYOnView / 3 * 2;

        // draw icons
        Bitmap icon_setting = getBitmapFromVectorDrawable(this.context, R.drawable.ic_settings_white_24dp);

        int width = icon_setting.getWidth();
        int height = icon_setting.getHeight();
        int poswidth = centerPosX - (width / 2);
        int posHeight = centerPosY - (height / 2);

        if (currentDirection == ButtonDirection.TOP_LEFT) {

            poswidth = centerPosX - (width / 2);
            posHeight = centerPosY - (height / 2);
            canvas.drawBitmap(icon_setting, poswidth, posHeight, iconPaint);
        } else if (currentDirection == ButtonDirection.TOP_RIGHT) {
            icon_setting = getBitmapFromVectorDrawable(this.context, R.drawable.ic_settings_white_20dp);

            poswidth = centerPosX + (width / 4);
            posHeight = centerPosY - (height / 2);
            canvas.drawBitmap(icon_setting, poswidth, posHeight, iconPaint);
        } else if (currentDirection == ButtonDirection.BOTTOM_LEFT) {

        } else if (currentDirection == ButtonDirection.BOTTOM_RIGHT) {

        }
    }

    /**
     * Draw polygon
     *
     * @param canvas The canvas to draw on
     * @param points Polygon corner points
     */
    private void drawTriangleMain(Canvas canvas, PointF[] points) {
        // line at minimum...
        if (points.length < 2) {
            return;
        }

        // path
        Path polyPath = new Path();
        polyPath.moveTo(points[0].x, points[0].y);
        int i, len;
        len = points.length;
        for (i = 0; i < len; i++) {
            polyPath.lineTo(points[i].x, points[i].y);
        }
        polyPath.lineTo(points[0].x, points[0].y);

        // draw
        if (state == ButtonState.NORMAL) {
            canvas.drawPath(polyPath, backgroundNormalPaint);
        } else if (state == ButtonState.CLICKED) {
            canvas.drawPath(polyPath, backgroundClickedMainPaint);
        }
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public ButtonState getState() {
        return this.state;
    }

    public void setState(ButtonState state) {
        this.state = state;
    }

    public void setDirection(ButtonDirection direction) {
        this.currentDirection = direction;
    }

    // set the direction of the button
    // 1 for top-left
    // 2 for top-right
    // 3 for bottom-left
    // 4 for bottom-right
    public PointF[] getDirection() {
        PointF[] point = null;

        switch (currentDirection) {
            case TOP_LEFT:
                point = new PointF[]{
                        new PointF(0, 0),
                        new PointF(getWidth(), 0),
                        new PointF(0, getHeight())
                };
                break;
            case TOP_RIGHT:
                point = new PointF[]{
                        new PointF(0, 0),
                        new PointF(getWidth(), 0),
                        new PointF(getWidth(), getHeight())
                };
                break;
            case BOTTOM_LEFT:
                point = new PointF[]{
                        new PointF(0, 0),
                        new PointF(0, getHeight()),
                        new PointF(getWidth(), getHeight())
                };
                break;
            case BOTTOM_RIGHT:
                point = new PointF[]{
                        new PointF(getWidth(), 0),
                        new PointF(getWidth(), getHeight()),
                        new PointF(getWidth(), 0)
                };
                break;
        }

        return point;
    }
}
