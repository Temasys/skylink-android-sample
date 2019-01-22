package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import sg.com.temasys.skylink.sdk.sampleapp.R;

public class MultiVideoFrameLayout extends FrameLayout {

//    private Path myArc;

    private Paint mPaint;

    public MultiVideoFrameLayout(Context context, AttributeSet ats, int defStyle) {
        super(context, ats, defStyle);
        init(ats, context);

    }

    public MultiVideoFrameLayout(Context context, AttributeSet ats) {
        super(context, ats);
        init(ats, context);
    }

    public MultiVideoFrameLayout(Context context) {
        super(context);
        init(null, context);
    }

    private void init(AttributeSet attrs, Context context) {
//        this.myArc = new Path();
        // create paint object
        this.mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // set style
        this.mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        // set color
        this.mPaint.setColor(getResources().getColor(R.color.color_black));
//        // set text Size
//        this.mPaintText.setTextSize(getResources().getDimension(
//                R.dimen.sp_15sp));
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

//        int textLength = getText().length();
//        if ((textLength % 2) != 0) {
//            textLength = textLength + 1;
//        }
//
//        this.myArc.addArc(oval, -90 - (textLength * 2), 90 + textLength + 10);
//
        canvas.drawCircle(200f, 200f, 200f, mPaint);
//        canvas.
    }
}
