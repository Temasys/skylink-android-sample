package sg.com.temasys.skylink.sdk.sampleapp.setting;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import sg.com.temasys.skylink.sdk.sampleapp.R;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.ROOM_NAME_AUDIO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.ROOM_NAME_CHAT;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.ROOM_NAME_DATA;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.ROOM_NAME_FILE;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.ROOM_NAME_MULTI_VIDEOS;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.ROOM_NAME_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USER_NAME_AUDIO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USER_NAME_CHAT;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USER_NAME_DATA;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USER_NAME_FILE;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USER_NAME_MULTI_VIDEOS;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.USER_NAME_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setRoomNameAudio;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setRoomNameChat;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setRoomNameData;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setRoomNameFile;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setRoomNameMultiVideos;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setRoomNameVideo;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setUserNameAudio;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setUserNameChat;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setUserNameData;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setUserNameFile;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setUserNameMultiVideos;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setUserNameVideo;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_AUDIO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_CHAT_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_DATA_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_FILE_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_MULTI_VIDEOS_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_VIDEO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_AUDIO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_CHAT_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_DATA_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_FILE_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_MULTI_VIDEOS_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_VIDEO_DEFAULT;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConfigRoomFragment extends Fragment {

    public static final String PREF_ROOM_NAME_AUDIO = "RoomNameAudio";
    public static final String PREF_ROOM_NAME_CHAT = "RoomNameChat";
    public static final String PREF_ROOM_NAME_DATA = "RoomNameData";
    public static final String PREF_ROOM_NAME_FILE = "RoomNameFile";
    public static final String PREF_ROOM_NAME_MULTI_VIDEOS = "RoomNameMultiVideos";
    public static final String PREF_ROOM_NAME_VIDEO = "RoomNameVideo";

    public static final String PREF_USER_NAME_AUDIO = "UserNameAudio";
    public static final String PREF_USER_NAME_CHAT = "UserNameChat";
    public static final String PREF_USER_NAME_DATA = "UserNameData";
    public static final String PREF_USER_NAME_FILE = "UserNameFile";
    public static final String PREF_USER_NAME_MULTI_VIDEOS = "UserNameMultiVideos";
    public static final String PREF_USER_NAME_VIDEO = "UserNameVideo";

    private EditText edtRoomNameAudio;
    private EditText edtRoomNameChat;
    private EditText edtRoomNameData;
    private EditText edtRoomNameFile;
    private EditText edtRoomNameMultiVideos;
    private EditText edtRoomNameVideo;

    private EditText edtUserNameAudio;
    private EditText edtUserNameChat;
    private EditText edtUserNameData;
    private EditText edtUserNameFile;
    private EditText edtUserNameMultiVideos;
    private EditText edtUserNameVideo;

    private ActionBar actionBar;

    public ConfigRoomFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();

        View view = inflater.inflate(R.layout.fragment_room, container, false);

        // Audio Function Value Set
        edtUserNameAudio = (EditText) view.findViewById(R.id.edtUserNameAudio);
        edtUserNameAudio.setText(USER_NAME_AUDIO);

        edtRoomNameAudio = (EditText) view.findViewById(R.id.edtRoomNameAudio);
        edtRoomNameAudio.setText(ROOM_NAME_AUDIO);

        // Chat Function Value Set
        edtUserNameChat = (EditText) view.findViewById(R.id.edtUserNameChat);
        edtUserNameChat.setText(USER_NAME_CHAT);

        edtRoomNameChat = (EditText) view.findViewById(R.id.edtRoomNameChat);
        edtRoomNameChat.setText(ROOM_NAME_CHAT);

        // Data Transfer Function Value Set
        edtUserNameData = (EditText) view.findViewById(R.id.edtUserNameData);
        edtUserNameData.setText(USER_NAME_DATA);

        edtRoomNameData = (EditText) view.findViewById(R.id.edtRoomNameData);
        edtRoomNameData.setText(ROOM_NAME_DATA);

        // File Transfer Function Value Set
        edtUserNameFile = (EditText) view.findViewById(R.id.edtUserNameFile);
        edtUserNameFile.setText(USER_NAME_FILE);

        edtRoomNameFile = (EditText) view.findViewById(R.id.edtRoomNameFile);
        edtRoomNameFile.setText(ROOM_NAME_FILE);

        // Multi Videos Function Value Set
        edtUserNameMultiVideos = (EditText) view.findViewById(R.id.edtUserNameMultiVideos);
        edtUserNameMultiVideos.setText(USER_NAME_MULTI_VIDEOS);

        edtRoomNameMultiVideos = (EditText) view.findViewById(R.id.edtRoomNameMultiVideos);
        edtRoomNameMultiVideos.setText(ROOM_NAME_MULTI_VIDEOS);

        // Video Function Value Set
        edtUserNameVideo = (EditText) view.findViewById(R.id.edtUserNameVideo);
        edtUserNameVideo.setText(USER_NAME_VIDEO);

        edtRoomNameVideo = (EditText) view.findViewById(R.id.edtRoomNameVideo);
        edtRoomNameVideo.setText(ROOM_NAME_VIDEO);

        // Set Listener actions for each EditText field.
        //  - Room names.
        setFocusChangeListener(edtRoomNameAudio, PREF_ROOM_NAME_AUDIO);
        setFocusChangeListener(edtRoomNameChat, PREF_ROOM_NAME_CHAT);
        setFocusChangeListener(edtRoomNameData, PREF_ROOM_NAME_DATA);
        setFocusChangeListener(edtRoomNameFile, PREF_ROOM_NAME_FILE);
        setFocusChangeListener(edtRoomNameMultiVideos, PREF_ROOM_NAME_MULTI_VIDEOS);
        setFocusChangeListener(edtRoomNameVideo, PREF_ROOM_NAME_VIDEO);
        //  - User names.
        setFocusChangeListener(edtUserNameAudio, PREF_USER_NAME_AUDIO);
        setFocusChangeListener(edtUserNameChat, PREF_USER_NAME_CHAT);
        setFocusChangeListener(edtUserNameData, PREF_USER_NAME_DATA);
        setFocusChangeListener(edtUserNameFile, PREF_USER_NAME_FILE);
        setFocusChangeListener(edtUserNameMultiVideos, PREF_USER_NAME_MULTI_VIDEOS);
        setFocusChangeListener(edtUserNameVideo, PREF_USER_NAME_VIDEO);

        // Button to reset all values to Default values (from Constants.java)
        final Button button = (Button) view.findViewById(R.id.btnResetDefault);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Set UI values to default values
                edtRoomNameAudio.setText(ROOM_NAME_AUDIO_DEFAULT);
                edtRoomNameChat.setText(ROOM_NAME_CHAT_DEFAULT);
                edtRoomNameData.setText(ROOM_NAME_DATA_DEFAULT);
                edtRoomNameFile.setText(ROOM_NAME_FILE_DEFAULT);
                edtRoomNameMultiVideos.setText(ROOM_NAME_MULTI_VIDEOS_DEFAULT);
                edtRoomNameVideo.setText(ROOM_NAME_VIDEO_DEFAULT);

                edtUserNameAudio.setText(USER_NAME_AUDIO_DEFAULT);
                edtUserNameChat.setText(USER_NAME_CHAT_DEFAULT);
                edtUserNameData.setText(USER_NAME_DATA_DEFAULT);
                edtUserNameFile.setText(USER_NAME_FILE_DEFAULT);
                edtUserNameMultiVideos.setText(USER_NAME_MULTI_VIDEOS_DEFAULT);
                edtUserNameVideo.setText(USER_NAME_VIDEO_DEFAULT);

                // Set Config values to default values and write to Preferences if it had changed.
                setRoomNameAudio(ROOM_NAME_AUDIO_DEFAULT, getActivity());
                setRoomNameChat(ROOM_NAME_CHAT_DEFAULT, getActivity());
                setRoomNameData(ROOM_NAME_DATA_DEFAULT, getActivity());
                setRoomNameFile(ROOM_NAME_FILE_DEFAULT, getActivity());
                setRoomNameMultiVideos(ROOM_NAME_MULTI_VIDEOS_DEFAULT, getActivity());
                setRoomNameVideo(ROOM_NAME_VIDEO_DEFAULT, getActivity());

                setUserNameAudio(USER_NAME_AUDIO_DEFAULT, getActivity());
                setUserNameChat(USER_NAME_CHAT_DEFAULT, getActivity());
                setUserNameData(USER_NAME_DATA_DEFAULT, getActivity());
                setUserNameFile(USER_NAME_FILE_DEFAULT, getActivity());
                setUserNameMultiVideos(USER_NAME_MULTI_VIDEOS_DEFAULT, getActivity());
                setUserNameVideo(USER_NAME_VIDEO_DEFAULT, getActivity());
            }
        });

        // setup the action bar
        setActionBar();

        return view;
    }

    private void setActionBar() {
        actionBar = ((SettingActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        actionBar.setTitle("Rooms setting");

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Get menu inflater object.
        MenuInflater menuInflater = getActivity().getMenuInflater();
        // Inflate the custom overflow menu
        menuInflater.inflate(R.menu.setting_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_reset:
                // Set UI values to default values
                edtRoomNameAudio.setText(ROOM_NAME_AUDIO_DEFAULT);
                edtRoomNameChat.setText(ROOM_NAME_CHAT_DEFAULT);
                edtRoomNameData.setText(ROOM_NAME_DATA_DEFAULT);
                edtRoomNameFile.setText(ROOM_NAME_FILE_DEFAULT);
                edtRoomNameMultiVideos.setText(ROOM_NAME_MULTI_VIDEOS_DEFAULT);
                edtRoomNameVideo.setText(ROOM_NAME_VIDEO_DEFAULT);

                edtUserNameAudio.setText(USER_NAME_AUDIO_DEFAULT);
                edtUserNameChat.setText(USER_NAME_CHAT_DEFAULT);
                edtUserNameData.setText(USER_NAME_DATA_DEFAULT);
                edtUserNameFile.setText(USER_NAME_FILE_DEFAULT);
                edtUserNameMultiVideos.setText(USER_NAME_MULTI_VIDEOS_DEFAULT);
                edtUserNameVideo.setText(USER_NAME_VIDEO_DEFAULT);

                // Set Config values to default values and write to Preferences if it had changed.
                setRoomNameAudio(ROOM_NAME_AUDIO_DEFAULT, getActivity());
                setRoomNameChat(ROOM_NAME_CHAT_DEFAULT, getActivity());
                setRoomNameData(ROOM_NAME_DATA_DEFAULT, getActivity());
                setRoomNameFile(ROOM_NAME_FILE_DEFAULT, getActivity());
                setRoomNameMultiVideos(ROOM_NAME_MULTI_VIDEOS_DEFAULT, getActivity());
                setRoomNameVideo(ROOM_NAME_VIDEO_DEFAULT, getActivity());

                setUserNameAudio(USER_NAME_AUDIO_DEFAULT, getActivity());
                setUserNameChat(USER_NAME_CHAT_DEFAULT, getActivity());
                setUserNameData(USER_NAME_DATA_DEFAULT, getActivity());
                setUserNameFile(USER_NAME_FILE_DEFAULT, getActivity());
                setUserNameMultiVideos(USER_NAME_MULTI_VIDEOS_DEFAULT, getActivity());
                setUserNameVideo(USER_NAME_VIDEO_DEFAULT, getActivity());

                break;
            case android.R.id.home:
                FragmentManager fm = ((SettingActivity) getContext()).getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                    return true;
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setFocusChangeListener(final EditText editText, final String valueType) {
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // Act on changes only when leaving EditText.
                if (!hasFocus) {
                    String uiValue = editText.getText().toString().trim();

                    switch (valueType) {
                        // Room name types
                        case PREF_ROOM_NAME_AUDIO:
                            setRoomNameAudio(uiValue, getActivity());
                            editText.setText(ROOM_NAME_AUDIO);
                            break;
                        case PREF_ROOM_NAME_CHAT:
                            setRoomNameChat(uiValue, getActivity());
                            editText.setText(ROOM_NAME_CHAT);
                            break;
                        case PREF_ROOM_NAME_DATA:
                            setRoomNameData(uiValue, getActivity());
                            editText.setText(ROOM_NAME_DATA);
                            break;
                        case PREF_ROOM_NAME_FILE:
                            setRoomNameFile(uiValue, getActivity());
                            editText.setText(ROOM_NAME_FILE);
                            break;
                        case PREF_ROOM_NAME_MULTI_VIDEOS:
                            setRoomNameMultiVideos(uiValue, getActivity());
                            editText.setText(ROOM_NAME_MULTI_VIDEOS);
                            break;
                        case PREF_ROOM_NAME_VIDEO:
                            setRoomNameVideo(uiValue, getActivity());
                            editText.setText(ROOM_NAME_VIDEO);
                            break;

                        // User name types
                        case PREF_USER_NAME_AUDIO:
                            setUserNameAudio(uiValue, getActivity());
                            editText.setText(USER_NAME_AUDIO);
                            break;
                        case PREF_USER_NAME_CHAT:
                            setUserNameChat(uiValue, getActivity());
                            editText.setText(USER_NAME_CHAT);
                            break;
                        case PREF_USER_NAME_DATA:
                            setUserNameData(uiValue, getActivity());
                            editText.setText(USER_NAME_DATA);
                            break;
                        case PREF_USER_NAME_FILE:
                            setUserNameFile(uiValue, getActivity());
                            editText.setText(USER_NAME_FILE);
                            break;
                        case PREF_USER_NAME_MULTI_VIDEOS:
                            setUserNameMultiVideos(uiValue, getActivity());
                            editText.setText(USER_NAME_MULTI_VIDEOS);
                            break;
                        case PREF_USER_NAME_VIDEO:
                            setUserNameVideo(uiValue, getActivity());
                            editText.setText(USER_NAME_VIDEO);
                            break;
                    }
                }
            }
        });
    }
}
