package sg.com.temasys.skylink.sdk.sampleapp.screensharing;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.localvideo.LocalVideoFragment;
import sg.com.temasys.skylink.sdk.sampleapp.localvideo.LocalVideoPresenter;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionFragment;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionPresenter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.*;

/**
 * Created by muoi.pham on 04/04/19.
 */

public class ScreenSharingActivity extends AppCompatActivity {

    private final String TAG = ScreenSharingFragment.class.getName();

    private final String SCREEN_SHARE_MAIN_FRAGMENT_TAG = "SCREEN_SHARE_MAIN_FRAGMENT";
    private final String SCREEN_RES_FRAGMENT_TAG = "SCREEN_RES_FRAGMENT";
    private final String SCREEN_LOCAL_CAMERA_VIEW_TAG = "SCREEN_LOCAL_CAMERA_VIEW_FRAGMENT";
    private final String SCREEN_LOCAL_SCREEN_VIEW_TAG = "SCREEN_LOCAL_SCREEN_VIEW_FRAGMENT";
    private final String SCREEN_REMOTE_CAMERA_VIEW_TAG = "SCREEN_REMOTE_CAMERA_VIEW_FRAGMENT";

    // remote screen sharing presenter instance: for both remote camera video view and remote screen share video view
    private ScreenSharingPresenter screenSharingPresenter;

    // video resolution presenter instance
    private VideoResolutionPresenter screenResPresenter;

    // local video view presenter instance: for local camera video view and local screen share video view
    private LocalVideoPresenter localVideoCameraPresenter, localVideoScreensharePresenter;

    // view instances
    // main video call fragment
    private ScreenSharingFragment screenSharingMainFragment;

    // video resolution fragment to be added to the main view
    private VideoResolutionFragment videoResolutionFragment;


    // local video view fragment to be added to the main view
    private LocalVideoFragment localVideoCameraFragment, localVideoScreenShareFragment, remoteVideoCameraFragment;


    private FrameLayout contentFrameLocalCameraView, contentFrameLocalScreenShareView, contentFrameRemoteCameraView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_screen_sharing);
        contentFrameLocalCameraView = findViewById(R.id.contentFrameLocalCameraView);
        contentFrameLocalScreenShareView = findViewById(R.id.contentFrameLocalScreenView);
        contentFrameRemoteCameraView = findViewById(R.id.contentFrameRemoteCameraView);

        //create presenters
        screenResPresenter = new VideoResolutionPresenter(this);
        screenSharingPresenter = new ScreenSharingPresenter(this);
        localVideoCameraPresenter = new LocalVideoPresenter(this);
        localVideoScreensharePresenter = new LocalVideoPresenter(this);

        // check previous state in case of screen rotation/configuration change
        // if it is new state, then create view instance
        // otherwise reuse the view instance and keep states
        if (savedInstanceState == null) {
            screenSharingMainFragment = ScreenSharingFragment.newInstance();
            videoResolutionFragment = VideoResolutionFragment.newInstance();
            localVideoCameraFragment = LocalVideoFragment.newInstance();
            localVideoCameraFragment.setVideoType(VIDEO_TYPE.LOCAL_CAMERA);
            localVideoScreenShareFragment = LocalVideoFragment.newInstance();
            localVideoScreenShareFragment.setVideoType(VIDEO_TYPE.LOCAL_SCREEN);
            remoteVideoCameraFragment = LocalVideoFragment.newInstance();
            remoteVideoCameraFragment.setVideoType(VIDEO_TYPE.REMOTE_CAMERA);

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameMainScreenShare, screenSharingMainFragment, SCREEN_SHARE_MAIN_FRAGMENT_TAG)
                    .addToBackStack("1")
                    .add(R.id.contentFrameScreenRes, videoResolutionFragment, SCREEN_RES_FRAGMENT_TAG)
                    .addToBackStack("2")
                    .add(R.id.contentFrameLocalCameraView, localVideoCameraFragment, SCREEN_LOCAL_CAMERA_VIEW_TAG)
                    .addToBackStack("3")
                    .add(R.id.contentFrameLocalScreenView, localVideoScreenShareFragment, SCREEN_LOCAL_SCREEN_VIEW_TAG)
                    .addToBackStack("4")
                    .add(R.id.contentFrameRemoteCameraView, remoteVideoCameraFragment, SCREEN_REMOTE_CAMERA_VIEW_TAG)
                    .addToBackStack("5")
                    .commit();
        } else {
            screenSharingMainFragment = (ScreenSharingFragment) getSupportFragmentManager()
                    .findFragmentByTag(SCREEN_SHARE_MAIN_FRAGMENT_TAG);

            videoResolutionFragment = (VideoResolutionFragment) getSupportFragmentManager()
                    .findFragmentByTag(SCREEN_RES_FRAGMENT_TAG);

            localVideoCameraFragment = (LocalVideoFragment) getSupportFragmentManager()
                    .findFragmentByTag(SCREEN_LOCAL_CAMERA_VIEW_TAG);

            localVideoScreenShareFragment = (LocalVideoFragment) getSupportFragmentManager()
                    .findFragmentByTag(SCREEN_LOCAL_SCREEN_VIEW_TAG);

            remoteVideoCameraFragment = (LocalVideoFragment) getSupportFragmentManager()
                    .findFragmentByTag(SCREEN_REMOTE_CAMERA_VIEW_TAG);
        }

        // hide the small views at the first time
        getSupportFragmentManager().beginTransaction()
                .hide(videoResolutionFragment)
                .hide(localVideoCameraFragment)
                .hide(localVideoScreenShareFragment)
                .hide(remoteVideoCameraFragment)
                .commit();

        //link between view and presenter
        screenSharingPresenter.setMainView(screenSharingMainFragment);
        screenSharingPresenter.setSmallView(remoteVideoCameraFragment);

        screenResPresenter.setView(videoResolutionFragment);
        screenSharingPresenter.setVideoResPresenter(screenResPresenter);


        localVideoCameraFragment.setLocalPresenter(localVideoCameraPresenter);
        localVideoScreenShareFragment.setLocalPresenter(localVideoScreensharePresenter);
        screenSharingMainFragment.setPresenter(screenSharingPresenter);
        remoteVideoCameraFragment.setRemotePresenter(screenSharingPresenter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance when changing configuration
        getSupportFragmentManager().putFragment(outState, SCREEN_SHARE_MAIN_FRAGMENT_TAG, screenSharingMainFragment);
        getSupportFragmentManager().putFragment(outState, SCREEN_RES_FRAGMENT_TAG, videoResolutionFragment);
        getSupportFragmentManager().putFragment(outState, SCREEN_LOCAL_CAMERA_VIEW_TAG, localVideoCameraFragment);
        getSupportFragmentManager().putFragment(outState, SCREEN_LOCAL_SCREEN_VIEW_TAG, localVideoScreenShareFragment);
        getSupportFragmentManager().putFragment(outState, SCREEN_REMOTE_CAMERA_VIEW_TAG, remoteVideoCameraFragment);
    }

    public void onShowHideVideoResFragment(boolean isVisible) {
        if (isVisible) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                    .show(videoResolutionFragment)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                    .hide(videoResolutionFragment)
                    .commit();
        }
    }

    public void onShowHideLocalCameraViewFragment(boolean isVisible, boolean isFullscreenMode) {
        if (isVisible) {
            if (isFullscreenMode) {
                localVideoCameraFragment.displayView();
                contentFrameLocalCameraView.setVisibility(View.VISIBLE);
            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(localVideoCameraFragment)
                        .commit();
                localVideoCameraFragment.displayView();
                contentFrameLocalCameraView.setVisibility(View.VISIBLE);
            }
        } else {
            if (isFullscreenMode) {
                localVideoCameraFragment.hide();
                contentFrameLocalCameraView.setVisibility(View.GONE);

            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(localVideoCameraFragment)
                        .commit();
                localVideoCameraFragment.hide();
                contentFrameLocalCameraView.setVisibility(View.GONE);
            }
        }
    }

    public void onShowHideLocalScreenViewFragment(boolean isVisible, boolean isFullscreenMode) {
        if (isVisible) {
            if (isFullscreenMode) {
                localVideoScreenShareFragment.displayView();
                contentFrameLocalScreenShareView.setVisibility(View.VISIBLE);
            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(localVideoScreenShareFragment)
                        .commit();
                localVideoScreenShareFragment.displayView();
                contentFrameLocalScreenShareView.setVisibility(View.VISIBLE);
            }
        } else {
            if (isFullscreenMode) {
                localVideoScreenShareFragment.hide();
                contentFrameLocalScreenShareView.setVisibility(View.GONE);

            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(localVideoScreenShareFragment)
                        .commit();
                localVideoScreenShareFragment.hide();
                contentFrameLocalScreenShareView.setVisibility(View.GONE);
            }
        }
    }

    public void onShowHideRemoteCameraViewFragment(boolean isVisible, boolean isFullscreenMode) {
        if (isVisible) {
            if (isFullscreenMode) {
                remoteVideoCameraFragment.displayView();
                contentFrameRemoteCameraView.setVisibility(View.VISIBLE);
            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(remoteVideoCameraFragment)
                        .commit();
            }
        } else {
            if (isFullscreenMode) {
                remoteVideoCameraFragment.hide();
                contentFrameRemoteCameraView.setVisibility(View.GONE);

            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(remoteVideoCameraFragment)
                        .commit();
            }
        }
    }

    public void onShowHideRemoteCameraViewFragment(boolean isVisible) {
        if (isVisible) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                    .show(remoteVideoCameraFragment)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                    .hide(remoteVideoCameraFragment)
                    .commit();
        }
    }

    public void changeViewPosition(View view, int X, int Y, int _xDelta, int _yDelta) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;


        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
        layoutParams.leftMargin = Math.max((X - _xDelta), 0) - (int) getResources().getDimension(R.dimen.dp_50dp);
        if (layoutParams.leftMargin < 0) {
            layoutParams.leftMargin = 0;
        }
        if (layoutParams.leftMargin > screenWidth - layoutParams.width) {
            layoutParams.leftMargin = screenWidth - layoutParams.width;
        }
        layoutParams.topMargin = Math.max((Y - _yDelta), 0) - (int) getResources().getDimension(R.dimen.dp_70dp);
        if (layoutParams.topMargin < 0) {
            layoutParams.topMargin = 0;
        }
        if (layoutParams.topMargin > screenHeight - layoutParams.height) {
            layoutParams.topMargin = screenHeight - layoutParams.height;
        }
        layoutParams.rightMargin = 0;
        layoutParams.bottomMargin = 0;


        view.setLayoutParams(layoutParams);
    }


    public void setLocalCameraView(SurfaceViewRenderer localView) {
        localVideoCameraFragment.setView(localView);
        localVideoCameraFragment.setVideoType(VIDEO_TYPE.LOCAL_CAMERA);
        onShowHideLocalCameraViewFragment(true, false);
    }

    public void setLocalScreenView(SurfaceViewRenderer localView) {
//        localVideoScreenShareFragment.setView(localView);
//        onShowHideLocalScreenViewFragment(true, false);
        localVideoCameraFragment.setView(localView);
        localVideoCameraFragment.setVideoType(VIDEO_TYPE.LOCAL_SCREEN);
        onShowHideLocalCameraViewFragment(true, false);
    }

    public void setRemoteCameraView(SurfaceViewRenderer remoteView) {
        onShowHideRemoteCameraViewFragment(true, false);

        remoteVideoCameraFragment.setViewRemote(remoteView);
    }

    public LocalVideoFragment getLocalVideoCameraFragment() {
        return localVideoCameraFragment;
    }

    public LocalVideoFragment getLocalVideoScreenShareFragment() {
        return localVideoScreenShareFragment;
    }

    public LocalVideoFragment getRemoteVideoCameraFragment() {
        return remoteVideoCameraFragment;
    }

    public FrameLayout getContentFrameLocalCameraView() {
        return contentFrameLocalCameraView;
    }

    public FrameLayout getContentFrameLocalScreenShareView() {
        return contentFrameLocalScreenShareView;
    }

    public FrameLayout getContentFrameRemoteCameraView() {
        return contentFrameRemoteCameraView;
    }

    public void processBringLocalCameraToMain(VIDEO_TYPE type) {
        screenSharingMainFragment.bringSmallViewToMainView(type);
    }
}
