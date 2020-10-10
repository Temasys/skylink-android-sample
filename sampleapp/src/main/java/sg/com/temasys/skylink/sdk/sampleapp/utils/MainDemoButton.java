package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.drawable.DrawableCompat;
import sg.com.temasys.skylink.sdk.sampleapp.R;

public class MainDemoButton extends AppCompatButton {
    private Context context;

    private Paint imgPaint, txtPaint;
    public ButtonType buttonType;

    public enum ButtonType {
        AUDIO,
        VIDEO,
        CHAT,
        FILE,
        DATA,
        MULTI
    }

    public MainDemoButton(Context context) {
        super(context);
        this.context = context;
        init(null, context);
    }

    public MainDemoButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(null, context);
    }

    public MainDemoButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(null, context);
    }

    public MainDemoButton(Context context, ButtonType buttonType) {
        super(context);

        this.context = context;
        this.buttonType = buttonType;
        init(null, context);
    }

    public void setType(ButtonType type) {
        this.buttonType = type;
    }

    private void init(AttributeSet attrs, Context context) {
        // create image icon paint object
        this.imgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // set style
        this.imgPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        // set color
        this.imgPaint.setColor(getResources().getColor(android.R.color.white));

        // create text view paint object
        this.txtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // set style
        this.txtPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        // set color
        txtPaint.setColor(getResources().getColor(android.R.color.black));

        // Set text size.
        txtPaint.setTextSize(50);

        setBackgroundDrawable(context.getResources().getDrawable(R.drawable.button_corner_ripple));
    }

    public static Bitmap getBitmapFromVectorDrawable(Drawable drawable) {
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerXOnView = getWidth() / 2;
        int centerYOnView = getHeight() / 2;

        Bitmap icon_setting = null;
        String text = null;

        if (this.buttonType == null)
            return;

        switch (this.buttonType) {
            case AUDIO:
                icon_setting = getBitmapFromVectorDrawable(context.getResources().getDrawable(R.drawable.ic_audio_icon));
                text = context.getResources().getString(R.string.audio);
                break;
            case VIDEO:
                icon_setting = getBitmapFromVectorDrawable(context.getResources().getDrawable(R.drawable.ic_video_icon));
                text = context.getResources().getString(R.string.video);
                break;
            case CHAT:
                icon_setting = getBitmapFromVectorDrawable(context.getResources().getDrawable(R.drawable.ic_chat_icon));
                text = context.getResources().getString(R.string.chat);
                break;
            case FILE:
                icon_setting = getBitmapFromVectorDrawable(context.getResources().getDrawable(R.drawable.ic_file_transfer_icon));
                text = context.getResources().getString(R.string.fileTransfer);
                break;
            case DATA:
                icon_setting = getBitmapFromVectorDrawable(context.getResources().getDrawable(R.drawable.ic_data_transfer_icon));
                text = context.getResources().getString(R.string.dataTransfer);
                break;
            case MULTI:
                icon_setting = getBitmapFromVectorDrawable(context.getResources().getDrawable(R.drawable.ic_multi_video_call_icon));
                text = context.getResources().getString(R.string.multiVideo);
                break;
        }

        Rect textBound = new Rect();

        if (text != null) {
            txtPaint.getTextBounds(text, 0, text.length(), textBound);
        }

        int iconWidth = 0;
        int iconHeight = 0;
        int posIconwidth = 0;
        int posIconHeight = 0;

        // draw icon
        if (icon_setting != null) {
            iconWidth = icon_setting.getWidth();
            iconHeight = icon_setting.getHeight();

            posIconwidth = centerXOnView - (iconWidth / 2);
            posIconHeight = centerYOnView - (iconHeight / 2) + textBound.top;
            canvas.drawBitmap(icon_setting, posIconwidth, posIconHeight, imgPaint);
        }

        // draw textView
        if (text != null) {
            int startTextX = centerXOnView - (textBound.width()/2);
            int startTextY = centerYOnView + iconHeight + textBound.top;
            canvas.drawText(text, startTextX, startTextY, txtPaint);
        }

    }
}
