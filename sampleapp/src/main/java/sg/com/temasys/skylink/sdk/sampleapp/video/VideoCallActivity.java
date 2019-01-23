package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class VideoCallActivity extends AppCompatActivity {

    private final String VIDEO_CALL_FRAGMENT_TAG = "VIDEO_CALL_FRAGMENT";

    // presenter instance
    private VideoCallPresenter mVideoCallPresenter;

    // view instance
    private VideoCallFragment videoCallFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        //create presenter
        mVideoCallPresenter = new VideoCallPresenter(this);

        // check previous state in case of screen rotation
        // if it is new state, then create view instance
        // otherwise reuse the view instance and keep states
        if (savedInstanceState == null) {
            videoCallFragment = VideoCallFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameVideoCall, videoCallFragment, VIDEO_CALL_FRAGMENT_TAG)
                    .commit();
        } else {
            videoCallFragment = (VideoCallFragment) getSupportFragmentManager()
                    .findFragmentByTag(VIDEO_CALL_FRAGMENT_TAG);
        }

        //link between view and presenter
        mVideoCallPresenter.setView(videoCallFragment);
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
    }
}
