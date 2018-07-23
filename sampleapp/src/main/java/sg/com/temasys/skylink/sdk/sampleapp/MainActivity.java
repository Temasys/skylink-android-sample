package sg.com.temasys.skylink.sdk.sampleapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.ConfigFragment;
import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioCallActivity;
import sg.com.temasys.skylink.sdk.sampleapp.chat.ChatActivity;
import sg.com.temasys.skylink.sdk.sampleapp.datatransfer.DataTransferActivity;
import sg.com.temasys.skylink.sdk.sampleapp.filetransfer.FileTransferActivity;
import sg.com.temasys.skylink.sdk.sampleapp.multipartyvideocall.MultiPartyVideoCallActivity;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.videocall.VideoCallActivity;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.CASE_FRAGMENT_AUDIO_CALL;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.CASE_FRAGMENT_CHAT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.CASE_FRAGMENT_DATA_TRANSFER;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.CASE_FRAGMENT_FILE_TRANSFER;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.CASE_FRAGMENT_MULTI_PARTY_VIDEO_CALL;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.CASE_FRAGMENT_VIDEO_CALL;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

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

        //init Utils
        Utils utils = new Utils(this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
//        // update the main content by replacing fragments

        switch (position){
            case CASE_FRAGMENT_AUDIO_CALL:
                startActivity(new Intent(this, AudioCallActivity.class));
                break;
            case CASE_FRAGMENT_CHAT:
                startActivity(new Intent(this, ChatActivity.class));
                break;
            case CASE_FRAGMENT_FILE_TRANSFER:
                startActivity(new Intent(this, FileTransferActivity.class));
                break;
            case CASE_FRAGMENT_DATA_TRANSFER:
                startActivity(new Intent(this, DataTransferActivity.class));
                break;
            case CASE_FRAGMENT_VIDEO_CALL:
                startActivity(new Intent(this, VideoCallActivity.class));
                break;
            case CASE_FRAGMENT_MULTI_PARTY_VIDEO_CALL:
                startActivity(new Intent(this, MultiPartyVideoCallActivity.class));
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

}
