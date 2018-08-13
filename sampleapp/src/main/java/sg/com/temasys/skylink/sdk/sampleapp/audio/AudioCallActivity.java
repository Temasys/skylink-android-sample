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


        //link between view and presenter
        mAudioCallPresenter = new AudioCallPresenter(mAudioCallFragment, this);
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
        getSupportFragmentManager().putFragment(outState, AUDIO_CALL_FRAGMENT_TAG, mAudioCallFragment);
    }


}
