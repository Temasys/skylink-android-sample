package sg.com.temasys.skylink.sdk.sampleapp.configuration;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by phyo.pwint on 26/7/16.
 */

public class ConfigPagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    public ConfigPagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                ConfigRoomFragment roomFragment = new ConfigRoomFragment();
                return roomFragment;
            case 1:
                ConfigKeyFragment keyFragment = new ConfigKeyFragment();
                return keyFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}

