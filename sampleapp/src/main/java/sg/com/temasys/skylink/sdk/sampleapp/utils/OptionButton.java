package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * A custom Button for displaying menu context of video view
 */
public class OptionButton extends AppCompatButton {

    private Paint myPaint;

    public OptionButton(Context context) {
        super(context);
        init(null, context);
    }

    public OptionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, context);
    }

    public OptionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, context);
    }

    private void init(AttributeSet attrs, Context context) {
        // create paint object
        this.myPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // set style
        this.myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        // set color
        this.myPaint.setColor(getResources().getColor(R.color.primary));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int centerXOnView = getWidth() / 2;
        int centerYOnView = getHeight() / 2;

        // draw 3 dots in vertical line
        canvas.drawCircle(centerXOnView, centerYOnView - 20f, 5f, myPaint);
        canvas.drawCircle(centerXOnView, centerYOnView, 5f, myPaint);
        canvas.drawCircle(centerXOnView, centerYOnView + 20f, 5f, myPaint);
    }

}
