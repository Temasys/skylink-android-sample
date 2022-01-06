package sg.com.temasys.skylink.sdk.sampleapp.multivideos;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class MultiVideosActivity extends AppCompatActivity {

    private final String MULTI_VIDEOS_FRAGMENT_TAG = "MULTI_VIDEOS_FRAGMENT";

    // presenter instance
    private MultiVideosPresenter multiVideosPresenter;

    // view instance
    private MultiVideosFragment multiVideosFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_videos);

        //create presenter
        multiVideosPresenter = new MultiVideosPresenter(this);

        // check previous state in case of screen rotation
        // if it is new state, then create view instance
        // otherwise reuse the view instance and keep states
        if (savedInstanceState == null) {
            multiVideosFragment = MultiVideosFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameMultiVideos, multiVideosFragment, MULTI_VIDEOS_FRAGMENT_TAG)
                    .commit();
        } else {
            multiVideosFragment = (MultiVideosFragment) getSupportFragmentManager()
                    .findFragmentByTag(MULTI_VIDEOS_FRAGMENT_TAG);
        }

        //link between view and presenter
        multiVideosPresenter.setView(multiVideosFragment);
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
        getSupportFragmentManager().putFragment(outState, MULTI_VIDEOS_FRAGMENT_TAG, multiVideosFragment);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        multiVideosPresenter.processActivityResult(requestCode, resultCode, data);
    }
}
