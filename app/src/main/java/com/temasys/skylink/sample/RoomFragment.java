package com.temasys.skylink.sample;

import android.app.Fragment;
import android.opengl.GLSurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

public abstract class RoomFragment extends Fragment {

    final static private String TAG = "RoomFragment";

    protected void addVideoView(ViewGroup parent, final View child,
                                final String peerId) {
        ViewGroup existingParent = (ViewGroup) child.getParent();
        if (existingParent != null)
            existingParent.removeView(child);
        parent.addView(child);
        ViewTreeObserver vto = child.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                child.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                Utility.layoutSubviews((GLSurfaceView) child, null);
            }
        });
        if (peerId != null)
            child.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    OptionAlertFragment.newInstance(peerId).show(
                            RoomFragment.this.getFragmentManager(), TAG);
                }
            });
    }

}