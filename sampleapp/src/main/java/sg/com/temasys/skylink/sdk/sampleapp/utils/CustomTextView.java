package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * A custom text view for displaying the sending/receiving progress
 */
public class CustomTextView extends AppCompatTextView {

    private Path myArc;

    private Paint myPaintText;

    public CustomTextView(Context context, AttributeSet ats, int defStyle) {
        super(context, ats, defStyle);
        init(ats, context);

    }

    public CustomTextView(Context context, AttributeSet ats) {
        super(context, ats);
        init(ats, context);
    }

    public CustomTextView(Context context) {
        super(context);
        init(null, context);
    }

    private void init(AttributeSet attrs, Context context) {
        this.myArc = new Path();
        // create paint object
        this.myPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        // set style
        this.myPaintText.setStyle(Paint.Style.FILL_AND_STROKE);
        // set color
        this.myPaintText.setColor(getResources().getColor(R.color.primary_dark));
        // set text Size
        this.myPaintText.setTextSize(getResources().getDimension(
                R.dimen.sp_15sp));
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int centerXOnView = getWidth() / 2;
        int centerYOnView = getHeight() / 2;

        int viewXCenterOnScreen = getLeft() + centerXOnView;
        int viewYCenterOnScreen = getTop() + centerYOnView;

        float threeDpPad = getResources().getDimension(R.dimen.dp_3dp);
        float rad = getResources().getDimension(R.dimen.dp_70dp);

        int leftOffset = (int) (viewXCenterOnScreen - (rad + (threeDpPad * 4)));
        int topOffset = (int) (viewYCenterOnScreen - (rad + (threeDpPad * 3)));
        int rightOffset = (int) (viewXCenterOnScreen + (rad + (threeDpPad * 4)));
        int bottomOffset = (int) (viewYCenterOnScreen + (rad + threeDpPad));

        RectF oval = new RectF(leftOffset, topOffset, rightOffset, bottomOffset);

        int textLength = getText().length();
        if ((textLength % 2) != 0) {
            textLength = textLength + 1;
        }

        this.myArc.addArc(oval, -90 - (textLength * 2), 90 + textLength + 10);

        canvas.drawTextOnPath((String) getText(), this.myArc, 0, 10,
                this.myPaintText);
    }
}
