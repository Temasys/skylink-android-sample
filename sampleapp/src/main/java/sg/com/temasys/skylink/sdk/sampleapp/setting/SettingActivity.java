package sg.com.temasys.skylink.sdk.sampleapp.setting;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;

public class SettingActivity extends AppCompatActivity {

    private final String SETTING_FRAGMENT_TAG = "SETTING_FRAGMENT_TAG";

    // presenter instance
    private SettingPresenter mSettingPresenter;

    // view instance
    private SettingFragment mSettingFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        //create presenter
        mSettingPresenter = new SettingPresenter(this);

        // check previous state in case of screen rotation
        // if it is new state, then create view instance
        // otherwise reuse the view instance and keep states
        if (savedInstanceState == null) {
            mSettingFragment = SettingFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameSetting, mSettingFragment, SETTING_FRAGMENT_TAG)
                    .commit();
        } else {
            mSettingFragment = (SettingFragment) getSupportFragmentManager()
                    .findFragmentByTag(SETTING_FRAGMENT_TAG);
        }

        //link view and presenter
        if (mSettingFragment != null)
            mSettingPresenter.setView(mSettingFragment);
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
        getSupportFragmentManager().putFragment(outState, SETTING_FRAGMENT_TAG, mSettingFragment);
    }

}
