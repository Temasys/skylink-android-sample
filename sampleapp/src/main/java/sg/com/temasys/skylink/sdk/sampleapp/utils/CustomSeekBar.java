package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatSeekBar;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * A custom Seekbar for displaying video resolution
 */
public class CustomSeekBar extends AppCompatSeekBar {

    Context context;
    private Paint dividerPaint, locationPaint, txtPaint;
    private Seekbar_Type type = Seekbar_Type.WIDTH_HEIGHT;
    private String currentWidthHeight = "WidthxHeight";
    private String currentFps = "Fps";
    private int noOfDivider = 0;

    public CustomSeekBar(Context context) {
        super(context);

        this.context = context;

        dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        locationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        txtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        init();
    }

    public CustomSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        locationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        txtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        init();

    }

    public CustomSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.context = context;

        dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        locationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        txtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        init();
    }

    private void init() {
        dividerPaint.setColor(getResources().getColor(R.color.primary));
        dividerPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        locationPaint.setColor(getResources().getColor(R.color.primary));
        locationPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        txtPaint.setColor(getResources().getColor(android.R.color.white));
        txtPaint.setStyle(Paint.Style.STROKE);

        // Set text size.
        txtPaint.setTextSize(30);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        // First draw the regular progress bar, then custom draw our text
        super.onDraw(canvas);

        // Get the actual seekbar width and start position for thumb
        float width = getWidth()
                - getPaddingLeft()
                - getPaddingRight();
        float startPos = getPaddingLeft() + width * getProgress() / getMax();

        int centerYOnView = getHeight() / 2;

        // draw divider
        float space = width / noOfDivider;
        float i = getPaddingLeft();

        while (i < width + getPaddingRight()) {
            float startPoint = i;
            if (type == Seekbar_Type.WIDTH_HEIGHT) {
                canvas.drawCircle(startPoint, centerYOnView, 5f, dividerPaint);
            } else if (type == Seekbar_Type.FPS) {
                canvas.drawCircle(startPoint, centerYOnView, 4f, dividerPaint);
            }

            i += space;
        }

        // draw the last stick
        if (type == Seekbar_Type.WIDTH_HEIGHT) {
            canvas.drawCircle(getPaddingLeft() + width, centerYOnView, 5f, dividerPaint);
        } else if (type == Seekbar_Type.FPS) {
            canvas.drawCircle(getPaddingLeft() + width, centerYOnView, 4f, dividerPaint);
        }


        // draw triangle
        drawPoly(canvas,
                new PointF[]{
                        new PointF(startPos, centerYOnView - 20),
                        new PointF(startPos - 20, centerYOnView - 40),
                        new PointF(startPos + 20, centerYOnView - 40)
                });

        // draw rectangle and text
        // prepare rect to draw
        RectF rect = new RectF();
        String text = this.currentWidthHeight;

        Rect textBound = new Rect();
        txtPaint.getTextBounds(text, 0, text.length(), textBound);

        if (type == Seekbar_Type.WIDTH_HEIGHT) {
            rect.left = startPos - 100;
            if (rect.left < 0) {
                rect.left = -8;
            }

            rect.right = rect.left + 200;
            if (rect.right > getWidth()) {
                rect.right = getWidth() + 8;
            }

            rect.top = centerYOnView - 110;
            rect.bottom = centerYOnView - 40;
        } else if (type == Seekbar_Type.FPS) {
            rect.left = startPos - 50;
            rect.right = rect.left + 100;
            rect.top = centerYOnView - 110;
            rect.bottom = centerYOnView - 40;
            text = this.currentFps;
        }

        canvas.drawRoundRect(
                rect, // rect
                20, // rx
                20, // ry
                locationPaint // Paint
        );

        int startTextX = (int) (rect.left + ((rect.width() - textBound.width())) / 2);
        int startTextY = (int) (rect.top + (rect.height() - textBound.height()));
        canvas.drawText(text, startTextX, startTextY, txtPaint);
    }

    /**
     * Draw polygon
     *
     * @param canvas The canvas to draw on
     * @param points Polygon corner points
     */
    private void drawPoly(Canvas canvas, PointF[] points) {
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
        canvas.drawPath(polyPath, locationPaint);
    }

    /**
     * Update the max value for seekbar and update the number of dividers in the seekbar
     *
     * @param maxRange the number of available values in the seekbar
     */
    public void setMaxRange(int maxRange) {
        // update the number of divider in seekbar
        this.noOfDivider = maxRange;

        // update the min/max value of seek bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.setMin(0);
        }

        this.setMax(maxRange);
    }

    public enum Seekbar_Type {
        WIDTH_HEIGHT,
        FPS
    }

    public void setType(Seekbar_Type type) {
        this.type = type;
    }

    public String getCurrentWidthHeight() {
        return currentWidthHeight;
    }

    public void setCurrentWidthHeight(String currentWidthHeight) {
        this.currentWidthHeight = currentWidthHeight;
    }

    public String getCurrentFps() {
        return currentFps;
    }

    public void setCurrentFps(String currentFps) {
        this.currentFps = currentFps;
    }
}
