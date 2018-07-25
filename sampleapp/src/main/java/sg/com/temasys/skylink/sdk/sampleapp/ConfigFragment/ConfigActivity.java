package sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.PermRequesterInfor;

public class ConfigActivity extends AppCompatActivity {

    private final String CONFIG_FRAGMENT_TAG = ConfigActivity.class.getName();

    private ConfigFragment mConfigFragment;

    public static ConfigActivity newInstance() {
        return new ConfigActivity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_fragment);

        //check previous state in case of screen rotation
        if (savedInstanceState == null) {
            mConfigFragment = ConfigFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameConfiguration, mConfigFragment, CONFIG_FRAGMENT_TAG)
                    .commit();
        } else {
            mConfigFragment = (ConfigFragment) getSupportFragmentManager()
                    .findFragmentByTag(CONFIG_FRAGMENT_TAG);
        }
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
        getSupportFragmentManager().putFragment(outState, CONFIG_FRAGMENT_TAG, mConfigFragment);
    }
}