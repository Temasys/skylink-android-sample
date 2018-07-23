package sg.com.temasys.skylink.sdk.sampleapp.videocall;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;

import android.content.DialogInterface;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class VideoCallActivity extends AppCompatActivity {

    private final String VIDEO_CALL_FRAGMENT_TAG = "VIDEO_CALL_FRAGMENT";

    private VideoCallPresenter mVideoCallPresenter;
    private VideoCallFragment videoCallFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        //check previous state in case of screen rotation
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
        mVideoCallPresenter = new VideoCallPresenter(videoCallFragment, this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance
        getSupportFragmentManager().putFragment(outState, VIDEO_CALL_FRAGMENT_TAG, videoCallFragment);
    }


}
