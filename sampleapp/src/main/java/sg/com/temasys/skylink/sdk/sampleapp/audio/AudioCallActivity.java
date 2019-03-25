package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class AudioCallActivity extends AppCompatActivity {

    private final String AUDIO_CALL_FRAGMENT_TAG = "AUDIO_CALL_FRAGMENT";

    // presenter instance
    private AudioCallPresenter audioCallPresenter;

    // view instance
    private AudioCallFragment audioCallFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_call);

        // create presenter
        audioCallPresenter = new AudioCallPresenter(this);

        // check previous state in case of screen rotation
        // if it is new state, then create view instance
        // otherwise reuse the view instance and keep states
        if (savedInstanceState == null) {
            audioCallFragment = AudioCallFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameAudioCall, audioCallFragment, AUDIO_CALL_FRAGMENT_TAG)
                    .commit();
        } else {
            audioCallFragment = (AudioCallFragment) getSupportFragmentManager()
                    .findFragmentByTag(AUDIO_CALL_FRAGMENT_TAG);
        }

        //link between view and presenter
        if (audioCallFragment != null)
            audioCallPresenter.setView(audioCallFragment);
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
        getSupportFragmentManager().putFragment(outState, AUDIO_CALL_FRAGMENT_TAG, audioCallFragment);
    }
}
