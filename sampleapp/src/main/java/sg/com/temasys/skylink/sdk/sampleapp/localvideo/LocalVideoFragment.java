package sg.com.temasys.skylink.sdk.sampleapp.localvideo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.screensharing.ScreenSharingActivity;
import sg.com.temasys.skylink.sdk.sampleapp.screensharing.ScreenSharingContract;
import sg.com.temasys.skylink.sdk.sampleapp.screensharing.ScreenSharingPresenter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.VIDEO_TYPE;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static android.widget.PopupMenu.OnMenuItemClickListener;

/**
 * A simple {@link CustomActionBar} subclass.
 * This class is responsible for display UI and get user interaction
 */

// need to refactor the name of the class to apply for both local view and remote view
public class LocalVideoFragment extends Fragment implements LocalVideoContract.View,
        OnClickListener, OnMenuItemClickListener, View.OnTouchListener, ScreenSharingContract.SmallView {

    private final String TAG = LocalVideoFragment.class.getName();
    private Context context;

    // view widgets
    private LinearLayout localVideoViewLayout;
    private Button btnLocalOption;

    // presenter instance to implement video call logic
    private LocalVideoContract.Presenter presenter;
    private ScreenSharingPresenter remoteCameraPresenter;
    private VIDEO_TYPE type = VIDEO_TYPE.LOCAL_CAMERA;
    private SurfaceViewRenderer currentView = null;


    public static LocalVideoFragment newInstance() {
        return new LocalVideoFragment();
    }

    @Override
    public void setLocalPresenter(LocalVideoContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public void setRemotePresenter(ScreenSharingPresenter screenSharingPresenter) {
        this.remoteCameraPresenter = screenSharingPresenter;
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
            case R.id.btnLocalVideoOption:
                onMenuOptionLocalPeer(btnLocalOption);
                break;
            case R.id.ll_local_video_view:
                break;
        }
    }

    /**
     * define the action for each menu items for local peer
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.switch_camera:
                presenter.onViewRequestSwitchCamera();
                break;
            case R.id.video_resolution:
                presenter.onViewRequestGetVideoResolutions();
                break;
            case R.id.bring_to_main:
                processBringViewToMain();
                break;
            default:
                Log.e(TAG, "Unknown menu option: " + menuItem.getItemId() + "!");
                return false;
        }
        return true;
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
    }

    /**
     * Init value for view components
     */
    private void initComponents() {
        // set onClick event for buttons in layout
        btnLocalOption.setOnClickListener(this);
        localVideoViewLayout.setOnClickListener(this);
        localVideoViewLayout.setOnTouchListener(this);
    }

    /**
     * Display local peer menu option
     */
    private void onMenuOptionLocalPeer(View view) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.setOnMenuItemClickListener(this);
        switch (type) {
            case LOCAL_CAMERA:
                popup.inflate(R.menu.local_camera_menu);
                break;
            case LOCAL_SCREEN:
                popup.inflate(R.menu.local_screen_menu);
                break;
            case REMOTE_CAMERA:
                popup.inflate(R.menu.remote_camera_menu);
                break;
            case REMOTE_SCREEN:
                popup.inflate(R.menu.remote_screen_menu);
                break;
        }

        popup.show();
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
                ScreenSharingActivity activity = (ScreenSharingActivity) getActivity();
                if (activity != null) {
                    activity.changeViewPosition(getContentFrameContainer(type), X, Y, _xDelta, _yDelta);
                }
                break;
        }

        return false;
    }

    private FrameLayout getContentFrameContainer(VIDEO_TYPE videoType) {
        ScreenSharingActivity activity = null;

        if (context != null && context instanceof ScreenSharingActivity) {
            activity = (ScreenSharingActivity) (getActivity());
        }

        switch (videoType) {
            case LOCAL_CAMERA:
                return activity.getContentFrameLocalCameraView();
            case LOCAL_SCREEN:
                return activity.getContentFrameLocalCameraView();

            case REMOTE_CAMERA:
                return activity.getContentFrameRemoteCameraView();
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

    public void setViewRemote(SurfaceViewRenderer remoteView) {
        if (remoteView == null) {
            String log = "[SA][addRemoteView] Not adding remote view as videoView is null!";
            Log.d(TAG, log);
            return;
        }

        // If previous self video exists,
        // Set new video to size of previous self video
        // And remove old self video.
        View self = localVideoViewLayout.findViewWithTag("peer");
        if (self != null) {
            // Remove the old self video.
            localVideoViewLayout.removeView(self);
        }

        Utils.removeViewFromParent(remoteView);

        // Tag new video as self and add onClickListener.
        remoteView.setTag("peer");
        // And new self video.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        params.gravity = Gravity.CENTER;
        params.weight = 1;
        remoteView.setLayoutParams(params);

        //alway set self video as vertical orientation
        localVideoViewLayout.setOrientation(LinearLayout.VERTICAL);

        localVideoViewLayout.addView(remoteView);

        localVideoViewLayout.invalidate();

        currentView = remoteView;
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
        if (context != null && context instanceof ScreenSharingActivity) {
            ((ScreenSharingActivity) getActivity()).processBringLocalCameraToMain(type);
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
