package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.VIDEO_TYPE;
import sg.com.temasys.skylink.sdk.sampleapp.utils.SmallVideoViewFragment;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionFragment;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionPresenter;

/**
 * Created by muoi.pham on 04/04/19.
 */

public class VideoActivity extends AppCompatActivity {

    private final String VIDEO_MAIN_FRAGMENT_TAG = "VIDEO_MAIN_FRAGMENT";
    private final String VIDEO_RES_FRAGMENT_TAG = "VIDEO_RES_FRAGMENT";
    private final String VIDEO_LOCAL_CAMERA_VIEW_TAG = "VIDEO_LOCAL_CAMERA_VIEW_FRAGMENT";
    private final String VIDEO_LOCAL_SCREEN_VIEW_TAG = "VIDEO_LOCAL_SCREEN_VIEW_FRAGMENT";
    private final String VIDEO_REMOTE_CAMERA_VIEW_TAG = "VIDEO_REMOTE_CAMERA_VIEW_FRAGMENT";
    private final String VIDEO_REMOTE_SCREEN_VIEW_TAG = "VIDEO_REMOTE_SCREEN_VIEW_FRAGMENT";

    // main presenter instance for video {both video camera and video screen}
    private VideoPresenter videoPresenter;

    // video resolution presenter instance
    private VideoResolutionPresenter videoResPresenter;

    // main video call fragment
    private VideoFragment videoMainFragment;

    // video resolution fragment to be added to the main view
    private VideoResolutionFragment videoResolutionFragment;

    // local video view fragment to be added to the main view
    private SmallVideoViewFragment localVideoCameraFragment, localVideoScreenFragment,
            remoteVideoCameraFragment, remoteVideoScreenFragment;

    private FrameLayout contentFrameLocalCameraView, contentFrameLocalScreenView,
            contentFrameRemoteCameraView, contentFrameRemoteScreenView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video);
        contentFrameLocalCameraView = findViewById(R.id.contentFrameLocalCameraView);
        contentFrameLocalScreenView = findViewById(R.id.contentFrameLocalScreenView);
        contentFrameRemoteCameraView = findViewById(R.id.contentFrameRemoteCameraView);
        contentFrameRemoteScreenView = findViewById(R.id.contentFrameRemoteScreenView);

        //create presenters
        videoResPresenter = new VideoResolutionPresenter(this);
        videoPresenter = new VideoPresenter(this);

        // check previous state in case of screen rotation/configuration change
        // if it is new state, then create view instance
        // otherwise reuse the view instance and keep states
        if (savedInstanceState == null) {
            videoResolutionFragment = VideoResolutionFragment.newInstance();
            videoMainFragment = VideoFragment.newInstance();
            localVideoCameraFragment = SmallVideoViewFragment.newInstance();
            localVideoCameraFragment.setVideoType(VIDEO_TYPE.LOCAL_CAMERA);
            localVideoScreenFragment = SmallVideoViewFragment.newInstance();
            localVideoScreenFragment.setVideoType(VIDEO_TYPE.LOCAL_SCREEN);
            remoteVideoCameraFragment = SmallVideoViewFragment.newInstance();
            remoteVideoCameraFragment.setVideoType(VIDEO_TYPE.REMOTE_CAMERA);
            remoteVideoScreenFragment = SmallVideoViewFragment.newInstance();
            remoteVideoScreenFragment.setVideoType(VIDEO_TYPE.REMOTE_SCREEN);

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameVideoRes, videoResolutionFragment, VIDEO_RES_FRAGMENT_TAG)
                    .add(R.id.contentFrameMainVideo, videoMainFragment, VIDEO_MAIN_FRAGMENT_TAG)
                    .add(R.id.contentFrameLocalCameraView, localVideoCameraFragment, VIDEO_LOCAL_CAMERA_VIEW_TAG)
                    .add(R.id.contentFrameLocalScreenView, localVideoScreenFragment, VIDEO_LOCAL_SCREEN_VIEW_TAG)
                    .add(R.id.contentFrameRemoteCameraView, remoteVideoCameraFragment, VIDEO_REMOTE_CAMERA_VIEW_TAG)
                    .add(R.id.contentFrameRemoteScreenView, remoteVideoScreenFragment, VIDEO_REMOTE_SCREEN_VIEW_TAG)
                    .commit();
        } else {
            videoResolutionFragment = (VideoResolutionFragment) getSupportFragmentManager()
                    .findFragmentByTag(VIDEO_RES_FRAGMENT_TAG);

            videoMainFragment = (VideoFragment) getSupportFragmentManager()
                    .findFragmentByTag(VIDEO_MAIN_FRAGMENT_TAG);

            localVideoCameraFragment = (SmallVideoViewFragment) getSupportFragmentManager()
                    .findFragmentByTag(VIDEO_LOCAL_CAMERA_VIEW_TAG);

            localVideoScreenFragment = (SmallVideoViewFragment) getSupportFragmentManager()
                    .findFragmentByTag(VIDEO_LOCAL_SCREEN_VIEW_TAG);

            remoteVideoCameraFragment = (SmallVideoViewFragment) getSupportFragmentManager()
                    .findFragmentByTag(VIDEO_REMOTE_CAMERA_VIEW_TAG);

            remoteVideoScreenFragment = (SmallVideoViewFragment) getSupportFragmentManager()
                    .findFragmentByTag(VIDEO_REMOTE_SCREEN_VIEW_TAG);
        }

        // hide the small views at the first time
        getSupportFragmentManager().beginTransaction()
                .hide(videoResolutionFragment)
                .hide(localVideoCameraFragment)
                .hide(localVideoScreenFragment)
                .hide(remoteVideoCameraFragment)
                .hide(remoteVideoScreenFragment)
                .commit();

        //link between view and presenter
        videoResPresenter.setView(videoResolutionFragment);
        videoPresenter.setVideoResPresenter(videoResPresenter);
        videoMainFragment.setPresenter(videoPresenter);
        videoPresenter.setMainView(videoMainFragment);
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
        getSupportFragmentManager().putFragment(outState, VIDEO_MAIN_FRAGMENT_TAG, videoMainFragment);
        getSupportFragmentManager().putFragment(outState, VIDEO_RES_FRAGMENT_TAG, videoResolutionFragment);
        getSupportFragmentManager().putFragment(outState, VIDEO_LOCAL_CAMERA_VIEW_TAG, localVideoCameraFragment);
        getSupportFragmentManager().putFragment(outState, VIDEO_LOCAL_SCREEN_VIEW_TAG, localVideoScreenFragment);
        getSupportFragmentManager().putFragment(outState, VIDEO_REMOTE_CAMERA_VIEW_TAG, remoteVideoCameraFragment);
        getSupportFragmentManager().putFragment(outState, VIDEO_REMOTE_SCREEN_VIEW_TAG, remoteVideoScreenFragment);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (data != null) {
                videoMainFragment.onHandleActivityResult(requestCode, resultCode, data);
            }
        } else {
            videoPresenter.processActivityResult(requestCode, resultCode, data);
        }
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
        if (localVideoCameraFragment.getView() == null)
            return;

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
        if (localVideoScreenFragment.getView() == null)
            return;

        if (isVisible) {
            if (isFullscreenMode) {
                localVideoScreenFragment.displayView();
                contentFrameLocalScreenView.setVisibility(View.VISIBLE);
            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(localVideoScreenFragment)
                        .commit();
                localVideoScreenFragment.displayView();
                contentFrameLocalScreenView.setVisibility(View.VISIBLE);
            }
        } else {
            if (isFullscreenMode) {
                localVideoScreenFragment.hide();
                contentFrameLocalScreenView.setVisibility(View.GONE);

            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(localVideoScreenFragment)
                        .commit();
                localVideoScreenFragment.hide();
                contentFrameLocalScreenView.setVisibility(View.GONE);
            }
        }
    }

    public void onShowHideRemoteCameraViewFragment(boolean isVisible, boolean isFullscreenMode) {
        if (remoteVideoCameraFragment.getView() == null)
            return;

        if (isVisible) {
            if (isFullscreenMode) {
                remoteVideoCameraFragment.displayView();
                contentFrameRemoteCameraView.setVisibility(View.VISIBLE);
            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(remoteVideoCameraFragment)
                        .commit();
                remoteVideoCameraFragment.displayView();
                contentFrameRemoteCameraView.setVisibility(View.VISIBLE);
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
                remoteVideoCameraFragment.hide();
                contentFrameRemoteCameraView.setVisibility(View.GONE);
            }
        }
    }

    public void onShowHideRemoteScreenViewFragment(boolean isVisible, boolean isFullscreenMode) {
        if (remoteVideoScreenFragment.getView() == null)
            return;

        if (isVisible) {
            if (isFullscreenMode) {
                remoteVideoScreenFragment.displayView();
                contentFrameRemoteScreenView.setVisibility(View.VISIBLE);
            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(remoteVideoScreenFragment)
                        .commit();
                remoteVideoScreenFragment.displayView();
                contentFrameRemoteScreenView.setVisibility(View.VISIBLE);
            }
        } else {
            if (isFullscreenMode) {
                remoteVideoScreenFragment.hide();
                contentFrameRemoteScreenView.setVisibility(View.GONE);

            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(remoteVideoScreenFragment)
                        .commit();
                remoteVideoScreenFragment.hide();
                contentFrameRemoteScreenView.setVisibility(View.GONE);
            }
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

    public void processBringSmallViewToMainView(VIDEO_TYPE type) {
        videoMainFragment.bringSmallViewToMainView(type);

        switch (type) {
            case LOCAL_CAMERA:
                onShowHideLocalCameraViewFragment(false, false);
                break;
            case LOCAL_SCREEN:
                onShowHideLocalScreenViewFragment(false, false);
                break;
            case REMOTE_CAMERA:
                onShowHideRemoteCameraViewFragment(false, false);
                break;
            case REMOTE_SCREEN:
                onShowHideRemoteScreenViewFragment(false, false);
                break;
        }
    }

    public void setLocalCameraView(SurfaceViewRenderer localView) {
        localVideoCameraFragment.setView(localView);
        localVideoCameraFragment.setVideoType(VIDEO_TYPE.LOCAL_CAMERA);
        onShowHideLocalCameraViewFragment(true, false);
    }

    public void setLocalScreenView(SurfaceViewRenderer localView) {
        localVideoScreenFragment.setView(localView);
        localVideoScreenFragment.setVideoType(VIDEO_TYPE.LOCAL_SCREEN);
        onShowHideLocalScreenViewFragment(true, false);
    }

    public void setRemoteCameraView(SurfaceViewRenderer remoteView) {
        remoteVideoCameraFragment.setView(remoteView);
        remoteVideoCameraFragment.setVideoType(VIDEO_TYPE.REMOTE_CAMERA);
        onShowHideRemoteCameraViewFragment(true, false);
    }

    public void setRemoteScreenView(SurfaceViewRenderer remoteView) {
        remoteVideoScreenFragment.setView(remoteView);
        remoteVideoScreenFragment.setVideoType(VIDEO_TYPE.REMOTE_SCREEN);
        onShowHideRemoteScreenViewFragment(true, false);
    }

    public FrameLayout getContentFrameLocalCameraView() {
        return contentFrameLocalCameraView;
    }

    public FrameLayout getContentFrameLocalScreenView() {
        return contentFrameLocalScreenView;
    }

    public FrameLayout getContentFrameRemoteCameraView() {
        return contentFrameRemoteCameraView;
    }

    public FrameLayout getContentFrameRemoteScreenView() {
        return contentFrameRemoteScreenView;
    }

    public SmallVideoViewFragment getLocalVideoCameraFragment() {
        return localVideoCameraFragment;
    }

    public SmallVideoViewFragment getLocalVideoScreenFragment() {
        return localVideoScreenFragment;
    }

    public SmallVideoViewFragment getRemoteVideoCameraFragment() {
        return remoteVideoCameraFragment;
    }

    public SmallVideoViewFragment getRemoteVideoScreenFragment() {
        return remoteVideoScreenFragment;
    }

    public void detachSmallView(SmallVideoViewFragment smallVideoViewFragment) {
        if (smallVideoViewFragment == localVideoCameraFragment) {
            onShowHideLocalCameraViewFragment(false, false);
        } else if (smallVideoViewFragment == localVideoScreenFragment) {
            onShowHideLocalScreenViewFragment(false, false);
        } else if (smallVideoViewFragment == remoteVideoCameraFragment) {
            onShowHideRemoteCameraViewFragment(false, false);
        } else if (smallVideoViewFragment == remoteVideoScreenFragment) {
            onShowHideRemoteScreenViewFragment(false, false);
        }
    }

    public void attachSmallView(SmallVideoViewFragment smallVideoViewFragment) {
        if (smallVideoViewFragment == localVideoCameraFragment) {
            onShowHideLocalCameraViewFragment(true, false);
        } else if (smallVideoViewFragment == localVideoScreenFragment) {
            onShowHideLocalScreenViewFragment(true, false);
        } else if (smallVideoViewFragment == remoteVideoCameraFragment) {
            onShowHideRemoteCameraViewFragment(true, false);
        } else if (smallVideoViewFragment == remoteVideoScreenFragment) {
            onShowHideRemoteScreenViewFragment(true, false);
        }

    }

    public void resetSmallRemoteViews() {
        onShowHideRemoteCameraViewFragment(false, false);
        onShowHideRemoteScreenViewFragment(false, false);
    }

    public void removeView(Constants.VIDEO_TYPE videoType) {
        switch (videoType) {
            case LOCAL_CAMERA:
                onShowHideLocalCameraViewFragment(false, false);
                localVideoCameraFragment.setView(null);
                break;
            case LOCAL_SCREEN:
                onShowHideLocalScreenViewFragment(false, false);
                localVideoScreenFragment.setView(null);
                break;
            case REMOTE_CAMERA:
                onShowHideRemoteCameraViewFragment(false, false);
                remoteVideoCameraFragment.setView(null);
                break;
            case REMOTE_SCREEN:
                onShowHideRemoteScreenViewFragment(false, false);
                remoteVideoScreenFragment.setView(null);
                break;
        }
    }
}
