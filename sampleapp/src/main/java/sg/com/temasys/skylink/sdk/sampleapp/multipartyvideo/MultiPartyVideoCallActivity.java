package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class MultiPartyVideoCallActivity extends AppCompatActivity {

    private final String MULTI_PARTY_VIDEO_FRAGMENT_TAG = "MULTI_PARTY_VIDEO_FRAGMENT";

    // presenter instance
    private MultiPartyVideoCallPresenter multiPartyVideoPresenter;

    // view instance
    private MultiPartyVideoCallFragment multiPartyVideoFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_party_video_call);

        //create presenter
        multiPartyVideoPresenter = new MultiPartyVideoCallPresenter(this);

        // check previous state in case of screen rotation
        // if it is new state, then create view instance
        // otherwise reuse the view instance and keep states
        if (savedInstanceState == null) {
            multiPartyVideoFragment = MultiPartyVideoCallFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameMultiPartyVideoCall, multiPartyVideoFragment, MULTI_PARTY_VIDEO_FRAGMENT_TAG)
                    .commit();
        } else {
            multiPartyVideoFragment = (MultiPartyVideoCallFragment) getSupportFragmentManager()
                    .findFragmentByTag(MULTI_PARTY_VIDEO_FRAGMENT_TAG);
        }

        //link between view and presenter
        multiPartyVideoPresenter.setView(multiPartyVideoFragment);
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
        getSupportFragmentManager().putFragment(outState, MULTI_PARTY_VIDEO_FRAGMENT_TAG, multiPartyVideoFragment);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        multiPartyVideoPresenter.onViewRequestActivityResult(requestCode, resultCode, data);
    }
}
