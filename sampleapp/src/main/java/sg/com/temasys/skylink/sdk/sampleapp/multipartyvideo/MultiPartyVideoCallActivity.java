package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class MultiPartyVideoCallActivity extends AppCompatActivity {

    private final String MULTI_PARTY_VIDEO_FRAGMENT_TAG = "MULTI_PARTY_VIDEO_FRAGMENT";

    // presenter instance
    private MultiPartyVideoCallPresenter mMultiPartyVideoPresenter;

    // view instance
    private MultiPartyVideoCallFragment mMultiPartyVideoFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_party_video_call);

        //create presenter
        mMultiPartyVideoPresenter = new MultiPartyVideoCallPresenter(this);

        // check previous state in case of screen rotation
        // if it is new state, then create view instance
        // otherwise reuse the view instance, just update it
        if (savedInstanceState == null) {
            mMultiPartyVideoFragment = MultiPartyVideoCallFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameMultiPartyVideoCall, mMultiPartyVideoFragment, MULTI_PARTY_VIDEO_FRAGMENT_TAG)
                    .commit();
        } else {
            mMultiPartyVideoFragment = (MultiPartyVideoCallFragment) getSupportFragmentManager()
                    .findFragmentByTag(MULTI_PARTY_VIDEO_FRAGMENT_TAG);
        }

        //link between view and presenter
        mMultiPartyVideoPresenter.setView(mMultiPartyVideoFragment);
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
        getSupportFragmentManager().putFragment(outState, MULTI_PARTY_VIDEO_FRAGMENT_TAG, mMultiPartyVideoFragment);
    }
}
