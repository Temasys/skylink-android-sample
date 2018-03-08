package sg.com.temasys.skylink.sdk.sampleapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigFragment;

import static sg.com.temasys.skylink.sdk.sampleapp.Utils.toastLogLong;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    public static final String ARG_SECTION_NUMBER = "section_number";
    private static final int CASE_SECTION_AUDIO_CALL = 1;
    private static final int CASE_SECTION_VIDEO_CALL = 2;
    private static final int CASE_SECTION_CHAT = 3;
    private static final int CASE_SECTION_FILE_TRANSFER = 4;
    private static final int CASE_SECTION_DATA_TRANSFER = 5;
    private static final int CASE_SECTION_MULTI_PARTY_VIDEO_CALL = 6;

    private static final int CASE_FRAGMENT_AUDIO_CALL = 0;
    private static final int CASE_FRAGMENT_VIDEO_CALL = 1;
    private static final int CASE_FRAGMENT_CHAT = 2;
    private static final int CASE_FRAGMENT_FILE_TRANSFER = 3;
    private static final int CASE_FRAGMENT_DATA_TRANSFER = 4;
    private static final int CASE_FRAGMENT_MULTI_PARTY_VIDEO_CALL = 5;
    private static final int CASE_FRAGMENT_CONFIG = 6;
    private static final String TAG = MainActivity.class.getName();

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // Load selected App key details
        Config.loadSelectedAppKey(this);
        Config.loadRoomUserNames(this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();

        Fragment fragmentToLaunch = getFragmentToLaunch(position);

        fragmentManager.beginTransaction()
                .replace(R.id.container, fragmentToLaunch)
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case CASE_SECTION_AUDIO_CALL:
                mTitle = getString(R.string.title_section1);
                break;
            case CASE_SECTION_VIDEO_CALL:
                mTitle = getString(R.string.title_section2);
                break;
            case CASE_SECTION_CHAT:
                mTitle = getString(R.string.title_section3);
                break;
            case CASE_SECTION_FILE_TRANSFER:
                mTitle = getString(R.string.title_section4);
                break;
            case CASE_SECTION_DATA_TRANSFER:
                mTitle = getString(R.string.title_section5);
                break;
            case CASE_SECTION_MULTI_PARTY_VIDEO_CALL:
                mTitle = getString(R.string.title_section6);
                break;
            default:
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_build_info) {
            String log = "SDK Version: " + sg.com.temasys.skylink.sdk.BuildConfig.VERSION_NAME
                    + "\n" + "Sample application version: " + BuildConfig.VERSION_NAME;
            toastLogLong(TAG, this, log);
            return true;
        } else if (id == R.id.action_configuration) {
            // update the main content by replacing fragments
            FragmentManager fragmentManager = getSupportFragmentManager();
            ConfigFragment fragment = new ConfigFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * returns fragment
     *
     * @param position
     * @return fragment to launch based on the item clicked on the navigation drawer
     */
    public Fragment getFragmentToLaunch(int position) {
        Fragment fragmentToLaunch = null;
        switch (position) {
            case CASE_FRAGMENT_AUDIO_CALL:
                fragmentToLaunch = new AudioCallFragment();
                break;
            case CASE_FRAGMENT_VIDEO_CALL:
                fragmentToLaunch = new VideoCallFragment();
                break;
            case CASE_FRAGMENT_CHAT:
                fragmentToLaunch = new ChatFragment();
                break;
            case CASE_FRAGMENT_FILE_TRANSFER:
                fragmentToLaunch = new FileTransferFragment();
                break;
            case CASE_FRAGMENT_DATA_TRANSFER:
                fragmentToLaunch = new DataTransferFragment();
                break;
            case CASE_FRAGMENT_MULTI_PARTY_VIDEO_CALL:
                fragmentToLaunch = new MultiPartyVideoCallFragment();
                break;
            case CASE_FRAGMENT_CONFIG:
                fragmentToLaunch = new ConfigFragment();
                break;
            default:
                break;
        }

        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, position + 1);
        fragmentToLaunch.setArguments(args);

        return fragmentToLaunch;
    }
}
