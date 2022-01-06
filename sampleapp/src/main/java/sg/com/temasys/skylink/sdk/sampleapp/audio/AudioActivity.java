package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class AudioActivity extends AppCompatActivity {

    private final String AUDIO_CALL_FRAGMENT_TAG = "AUDIO_CALL_FRAGMENT";

    // presenter instance
    private AudioPresenter audioPresenter;

    // view instance
    private AudioFragment audioFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        // create presenter
        audioPresenter = new AudioPresenter(this);

        // check previous state in case of screen rotation
        // if it is new state, then create view instance
        // otherwise reuse the view instance and keep states
        if (savedInstanceState == null) {
            audioFragment = AudioFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameAudioCall, audioFragment, AUDIO_CALL_FRAGMENT_TAG)
                    .commit();
        } else {
            audioFragment = (AudioFragment) getSupportFragmentManager()
                    .findFragmentByTag(AUDIO_CALL_FRAGMENT_TAG);
        }

        //link between view and presenter
        if (audioFragment != null)
            audioPresenter.setView(audioFragment);
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
        getSupportFragmentManager().putFragment(outState, AUDIO_CALL_FRAGMENT_TAG, audioFragment);
    }
}
