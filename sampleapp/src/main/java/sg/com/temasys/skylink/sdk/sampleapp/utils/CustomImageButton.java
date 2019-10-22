package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;

import sg.com.temasys.skylink.sdk.sampleapp.R;

public class CustomImageButton extends AppCompatImageButton {
    private Drawable drawableActive;
    private Drawable drawableDisable;
    private Drawable drawableMuted;
    private Drawable drawableMutedDisable;
    private Drawable drawableStart;
    private Drawable drawableStop;

    private boolean isEnable;

    public CustomImageButton(Context context) {
        super(context);
    }

    public CustomImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public Drawable getDrawableActive() {
        return drawableActive;
    }

    public void setDrawableActive(Drawable drawableActive) {
        this.drawableActive = drawableActive;
    }

    public Drawable getDrawableDisable() {
        return drawableDisable;
    }

    public void setDrawableDisable(Drawable drawableDisable) {
        this.drawableDisable = drawableDisable;
    }

    public Drawable getDrawableMuted() {
        return drawableMuted;
    }

    public void setDrawableMuted(Drawable drawableMuted) {
        this.drawableMuted = drawableMuted;
    }

    public Drawable getDrawableMutedDisable() {
        return drawableMutedDisable;
    }

    public void setDrawableMutedDisable(Drawable drawableMutedDisable) {
        this.drawableMutedDisable = drawableMutedDisable;
    }

    public Drawable getDrawableStart() {
        return drawableStart;
    }

    public void setDrawableStart(Drawable drawableStart) {
        this.drawableStart = drawableStart;
    }

    public Drawable getDrawableStop() {
        return drawableStop;
    }

    public void setDrawableStop(Drawable drawableStop) {
        this.drawableStop = drawableStop;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.isEnable = enabled;

        if (enabled) {
            this.setBackground(getResources().getDrawable(R.drawable.button_circle_trans_green));
            this.setImageDrawable(drawableActive);
        } else {
            this.setBackground(getResources().getDrawable(R.drawable.button_circle_disable));
            this.setImageDrawable(drawableDisable);
        }
    }

    public void setMuted(boolean muted) {
        if (muted) {
            if (this.isEnable && drawableMuted != null)
                this.setImageDrawable(drawableActive);
            else if (!this.isEnable && drawableMutedDisable != null)
                this.setImageDrawable(drawableDisable);
        } else {
            if (this.isEnable)
                this.setImageDrawable(drawableMuted);
            else
                this.setImageDrawable(drawableMutedDisable);
        }
    }

    public void setStart(boolean started) {
        if (started) {
            this.setImageDrawable(drawableStart);
        } else {
            this.setImageDrawable(drawableStop);
        }
    }
}
