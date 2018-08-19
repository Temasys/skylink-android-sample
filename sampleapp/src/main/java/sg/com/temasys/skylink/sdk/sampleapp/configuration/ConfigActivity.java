package sg.com.temasys.skylink.sdk.sampleapp.configuration;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import sg.com.temasys.skylink.sdk.sampleapp.BuildConfig;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.about, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_about) {
            String log = "SDK Version: " + sg.com.temasys.skylink.sdk.BuildConfig.VERSION_NAME
                    + "\n" + "Sample application version: " + BuildConfig.VERSION_NAME;
            Utils.toastLogLong(CONFIG_FRAGMENT_TAG, this, log);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}