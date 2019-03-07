package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionFragment;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionPresenter;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class VideoCallActivity extends AppCompatActivity {

    private final String VIDEO_CALL_FRAGMENT_TAG = "VIDEO_CALL_FRAGMENT";
    private final String VIDEO_RES_FRAGMENT_TAG = "VIDEO_RES_FRAGMENT";

    // video call presenter instance
    private VideoCallPresenter videoCallPresenter;

    // video resolution presenter instance
    private VideoResolutionPresenter videoResPresenter;

    // view instances
    // main video call fragment
    private VideoCallFragment videoCallFragment;

    // video resolution fragment to be added to the main view
    private VideoResolutionFragment videoResolutionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        //create presenters
        videoResPresenter = new VideoResolutionPresenter(this);
        videoCallPresenter = new VideoCallPresenter(this);

        // check previous state in case of screen rotation/configuration change
        // if it is new state, then create view instance
        // otherwise reuse the view instance and keep states
        if (savedInstanceState == null) {
            videoCallFragment = VideoCallFragment.newInstance();
            videoResolutionFragment = VideoResolutionFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameVideoCall, videoCallFragment, VIDEO_CALL_FRAGMENT_TAG)
                    .add(R.id.contentFrameVideoRes, videoResolutionFragment, VIDEO_RES_FRAGMENT_TAG)
                    .commit();
        } else {
            videoCallFragment = (VideoCallFragment) getSupportFragmentManager()
                    .findFragmentByTag(VIDEO_CALL_FRAGMENT_TAG);

            videoResolutionFragment = (VideoResolutionFragment) getSupportFragmentManager()
                    .findFragmentByTag(VIDEO_RES_FRAGMENT_TAG);
        }

        // hide the video resolution fragment at the first time
        getSupportFragmentManager().beginTransaction()
                .hide(videoResolutionFragment)
                .commit();

        //link between view and presenter
        videoCallPresenter.setView(videoCallFragment);
        videoResPresenter.setView(videoResolutionFragment);
        videoCallPresenter.setVideoResPresenter(videoResPresenter);
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
        getSupportFragmentManager().putFragment(outState, VIDEO_CALL_FRAGMENT_TAG, videoCallFragment);
        getSupportFragmentManager().putFragment(outState, VIDEO_RES_FRAGMENT_TAG, videoResolutionFragment);
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
}
