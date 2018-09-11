package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class AudioCallActivity extends AppCompatActivity {

    private final String AUDIO_CALL_FRAGMENT_TAG = "AUDIO_CALL_FRAGMENT";

    private AudioCallPresenter mAudioCallPresenter;
    private AudioCallFragment mAudioCallFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_call);

        //create presenter
        mAudioCallPresenter = new AudioCallPresenter(this);

        //check previous state in case of screen rotation
        if (savedInstanceState == null) {
            mAudioCallFragment = AudioCallFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameAudioCall, mAudioCallFragment, AUDIO_CALL_FRAGMENT_TAG)
                    .commit();
        } else {
            mAudioCallFragment = (AudioCallFragment) getSupportFragmentManager()
                    .findFragmentByTag(AUDIO_CALL_FRAGMENT_TAG);
        }

        //link view and presenter
        if(mAudioCallFragment != null)
            mAudioCallPresenter.setView(mAudioCallFragment);
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
        getSupportFragmentManager().putFragment(outState, AUDIO_CALL_FRAGMENT_TAG, mAudioCallFragment);
    }
}
