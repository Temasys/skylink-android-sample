package sg.com.temasys.skylink.sdk.sampleapp.configuration;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import sg.com.temasys.skylink.sdk.sampleapp.R;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link ConfigFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

public class ConfigFragment extends Fragment {

    private final String TAG = ConfigFragment.class.getName();

    // Constants for configuration change
    private final String BUNDLE_DATA = "BUNDLE_DATA";

    public static ConfigFragment newInstance() {
        return new ConfigFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_config, container, false);

        setActionBar();

        // Use tabs to switch between config pages.
        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Rooms"));
        tabLayout.addTab(tabLayout.newTab().setText("App Keys"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager configViewPager = (ViewPager) view.findViewById(R.id.pager);

        final ConfigPagerAdapter configPagerAdapter =
                new ConfigPagerAdapter(getChildFragmentManager(), tabLayout.getTabCount());

        configViewPager.setAdapter(configPagerAdapter);

        configViewPager.addOnPageChangeListener(
                new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        configViewPager.setCurrentItem(tab.getPosition());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {

                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {

                    }

                });

        return view;
    }

    private void setActionBar() {
        ActionBar actionBar = ((ConfigActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        setHasOptionsMenu(true);
    }

}

