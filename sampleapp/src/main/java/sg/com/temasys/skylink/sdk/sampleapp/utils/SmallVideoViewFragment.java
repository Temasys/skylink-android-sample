package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.VIDEO_TYPE;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoActivity;


/**
 * A simple {@link CustomActionBar} subclass.
 * This class is responsible for display UI and get user interaction
 */

// need to refactor the name of the class to apply for both local view and remote view
public class SmallVideoViewFragment extends Fragment implements
        OnClickListener, View.OnTouchListener {

    private final String TAG = SmallVideoViewFragment.class.getName();
    private Context context;

    // view widgets
    private LinearLayout localVideoViewLayout;
    private Button btnLocalOption;
    private ImageButton btnBringToMain;

    // the type of this small view, can be one of Constants.VIDEO_TYPE
    private VIDEO_TYPE type = VIDEO_TYPE.LOCAL_CAMERA;
    private SurfaceViewRenderer currentView = null;


    public static SmallVideoViewFragment newInstance() {
        return new SmallVideoViewFragment();
    }

    public void setVideoType(VIDEO_TYPE videoType) {
        this.type = videoType;
    }

    //----------------------------------------------------------------------------------------------
    // Fragment life cycle methods
    //----------------------------------------------------------------------------------------------

    @Override
    public void onAttach(Context context) {
        this.context = context;
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[SA][Video][onCreateView] ");

        View rootView = inflater.inflate(R.layout.local_video_view_layout, container, false);

        // get the UI controls from layout
        getControlWidgets(rootView);

        // init the UI controls
        initComponents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        //Defining a click event actions for the buttons
        switch (view.getId()) {
            case R.id.ll_local_video_view:
                break;
            case R.id.btn_bring_to_main:
                processBringViewToMain();
                break;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------
    private void getControlWidgets(View rootView) {
        localVideoViewLayout = rootView.findViewById(R.id.ll_local_video_view);
        btnLocalOption = rootView.findViewById(R.id.btnLocalVideoOption);
        btnBringToMain = rootView.findViewById(R.id.btn_bring_to_main);
    }

    /**
     * Init value for view components
     */
    private void initComponents() {
        // set onClick event for buttons in layout
        btnLocalOption.setOnClickListener(this);
        localVideoViewLayout.setOnClickListener(this);
        localVideoViewLayout.setOnTouchListener(this);
        btnBringToMain.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnBringToMain.setTooltipText("Bring view to main view");
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        final int X = (int) e.getRawX();
        final int Y = (int) e.getRawY();

        int _xDelta = 0, _yDelta = 0;

        switch (e.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
                _xDelta = X - lParams.leftMargin;
                _yDelta = Y - lParams.topMargin;
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            case MotionEvent.ACTION_MOVE:
                // send data for the fragment to move
                VideoActivity activity = (VideoActivity) getActivity();
                if (activity != null) {
                    activity.changeViewPosition(getContentFrameContainer(type), X, Y, _xDelta, _yDelta);
                }
                break;
        }

        return false;
    }

    private FrameLayout getContentFrameContainer(VIDEO_TYPE videoType) {
        VideoActivity activity = null;

        if (context != null && context instanceof VideoActivity) {
            activity = (VideoActivity) (getActivity());
        }

        switch (videoType) {
            case LOCAL_CAMERA:
                return activity.getContentFrameLocalCameraView();
            case LOCAL_SCREEN:
                return activity.getContentFrameLocalScreenView();
            case REMOTE_CAMERA:
                return activity.getContentFrameRemoteCameraView();
            case REMOTE_SCREEN:
                return activity.getContentFrameRemoteScreenView();
        }

        return null;

    }

    public void setView(SurfaceViewRenderer localView) {
        if (localView == null) {
            String log = "[SA][addRemoteView] Not adding remote view as videoView is null!";
            Log.d(TAG, log);
            return;
        }

        // If previous self video exists,
        // Set new video to size of previous self video
        // And remove old self video.
        View self = localVideoViewLayout.findViewWithTag("self");
        if (self != null) {
            // Remove the old self video.
            localVideoViewLayout.removeView(self);
        }

        Utils.removeViewFromParent(localView);

        // Tag new video as self and add onClickListener.
        localView.setTag("self");
        // And new self video.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        params.gravity = Gravity.CENTER;
        params.weight = 1;
        localView.setLayoutParams(params);

        //alway set self video as vertical orientation
        localVideoViewLayout.setOrientation(LinearLayout.VERTICAL);

        localVideoViewLayout.addView(localView);

        localVideoViewLayout.invalidate();

        currentView = localView;
    }

    public SurfaceViewRenderer getView() {
        return currentView;
    }

    public void displayView() {
        if (currentView == null) {
            String log = "[SA][addRemoteView] Not adding remote view as videoView is null!";
            Log.d(TAG, log);
            return;
        }

        // If previous self video exists,
        // Set new video to size of previous self video
        // And remove old self video.
        View self = localVideoViewLayout.findViewWithTag("self");
        if (self != null) {
            // Remove the old self video.
            localVideoViewLayout.removeView(self);
        }

        // Tag new video as self and add onClickListener.
        currentView.setTag("self");
        // And new self video.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        params.gravity = Gravity.CENTER;
        params.weight = 1;
        currentView.setLayoutParams(params);

        //alway set self video as vertical orientation
        localVideoViewLayout.setOrientation(LinearLayout.VERTICAL);

        Utils.removeViewFromParent(currentView);

        localVideoViewLayout.addView(currentView);

        localVideoViewLayout.invalidate();
    }

    private void processBringViewToMain() {
        if (context != null && context instanceof VideoActivity) {
            ((VideoActivity) getActivity()).processBringSmallViewToMainView(type);
        }
    }

    public void hide() {
        View self = localVideoViewLayout.findViewWithTag("self");
        if (self != null) {
            // Remove the old self video.
            localVideoViewLayout.removeView(self);
        }

    }
}
