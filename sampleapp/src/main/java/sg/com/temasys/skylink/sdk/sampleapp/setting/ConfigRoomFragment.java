package sg.com.temasys.skylink.sdk.sampleapp.setting;


import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import sg.com.temasys.skylink.sdk.sampleapp.R;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.getPrefBoolean;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.getPrefString;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setPrefBoolean;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.setPrefString;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_AUDIO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_CHAT_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_COMMON_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_DATA_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_FILE_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_MULTI_VIDEOS_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_VIDEO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_AUDIO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_CHAT_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_COMMON_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_DATA_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_FILE_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_MULTI_VIDEOS_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_VIDEO_DEFAULT;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConfigRoomFragment extends Fragment {

    public static final String PREF_ROOM_NAME_AUDIO_SAVED = "RoomNameAudioSaved";
    public static final String PREF_ROOM_NAME_AUDIO_INPUT_VALUE = "RoomNameAudioInputted";
    public static final String PREF_ROOM_NAME_CHAT_SAVED = "RoomNameChatSaved";
    public static final String PREF_ROOM_NAME_CHAT_INPUT_VALUE = "RoomNameChatInputted";
    public static final String PREF_ROOM_NAME_DATA_SAVED = "RoomNameDataSaved";
    public static final String PREF_ROOM_NAME_DATA_INPUT_VALUE = "RoomNameDataInputted";
    public static final String PREF_ROOM_NAME_FILE_SAVED = "RoomNameFileSaved";
    public static final String PREF_ROOM_NAME_FILE_INPUT_VALUE = "RoomNameFileInputted";
    public static final String PREF_ROOM_NAME_MULTI_VIDEOS_SAVED = "RoomNameMultiVideosSaved";
    public static final String PREF_ROOM_NAME_MULTI_VIDEOS_INPUT_VALUE = "RoomNameMultiVideosInputted";
    public static final String PREF_ROOM_NAME_VIDEO_SAVED = "RoomNameVideoSaved";
    public static final String PREF_ROOM_NAME_VIDEO_INPUT_VALUE = "RoomNameVideoInputted";
    public static final String PREF_ROOM_NAME_COMMON_SAVED = "RoomNameCommonSaved";

    public static final String PREF_USER_NAME_AUDIO_SAVED = "UserNameAudioSaved";
    public static final String PREF_USER_NAME_AUDIO_INPUT_VALUE = "UserNameAudioInputted";
    public static final String PREF_USER_NAME_CHAT_SAVED = "UserNameChatSaved";
    public static final String PREF_USER_NAME_CHAT_INPUT_VALUE = "UserNameChatInputted";
    public static final String PREF_USER_NAME_DATA_SAVED = "UserNameDataSaved";
    public static final String PREF_USER_NAME_DATA_INPUT_VALUE = "UserNameDataInputted";
    public static final String PREF_USER_NAME_FILE_SAVED = "UserNameFileSaved";
    public static final String PREF_USER_NAME_FILE_INPUT_VALUE = "UserNameFileInputted";
    public static final String PREF_USER_NAME_MULTI_VIDEOS_SAVED = "UserNameMultiVideosSaved";
    public static final String PREF_USER_NAME_MULTI_VIDEOS_INPUT_VALUE = "UserNameMultiVideosInputted";
    public static final String PREF_USER_NAME_VIDEO_SAVED = "UserNameVideoSaved";
    public static final String PREF_USER_NAME_VIDEO_INPUT_VALUE = "UserNameVideoInputted";
    public static final String PREF_USER_NAME_COMMON_SAVED = "UserNameCommonSaved";

    public static final String PREF_USE_COMMON_SETTING = "UserCommonSetting";

    private EditText edtRoomNameAudio;
    private EditText edtRoomNameChat;
    private EditText edtRoomNameData;
    private EditText edtRoomNameFile;
    private EditText edtRoomNameMultiVideos;
    private EditText edtRoomNameVideo;
    private EditText edtRoomNameCommon;

    private EditText edtUserNameAudio;
    private EditText edtUserNameChat;
    private EditText edtUserNameData;
    private EditText edtUserNameFile;
    private EditText edtUserNameMultiVideos;
    private EditText edtUserNameVideo;
    private EditText edtUserNameCommon;
    private Switch switchUseCommon;
    private Button btnReset;

    private ActionBar actionBar;

    public ConfigRoomFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_room, container, false);

        getControlWidgets(view);
        initControls();

        switchUseCommon.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch (buttonView.getId()) {
                    case R.id.switch_use_common_settings:
                        setPrefBoolean(PREF_USE_COMMON_SETTING, isChecked, getActivity());
                        changeUISeparateSetting(isChecked);

                        if (isChecked) {
                            processSaveCommonValue(edtRoomNameCommon.getText().toString(), edtUserNameCommon.getText().toString());
                        } else {
                            // change the saved value
                            setPrefString(PREF_ROOM_NAME_AUDIO_SAVED, edtRoomNameAudio.getText().toString(), getActivity());
                            setPrefString(PREF_ROOM_NAME_VIDEO_SAVED, edtRoomNameVideo.getText().toString(), getActivity());
                            setPrefString(PREF_ROOM_NAME_CHAT_SAVED, edtRoomNameChat.getText().toString(), getActivity());
                            setPrefString(PREF_ROOM_NAME_FILE_SAVED, edtRoomNameFile.getText().toString(), getActivity());
                            setPrefString(PREF_ROOM_NAME_DATA_SAVED, edtRoomNameData.getText().toString(), getActivity());
                            setPrefString(PREF_ROOM_NAME_MULTI_VIDEOS_SAVED, edtRoomNameMultiVideos.getText().toString(), getActivity());

                            setPrefString(PREF_USER_NAME_AUDIO_SAVED, edtUserNameAudio.getText().toString(), getActivity());
                            setPrefString(PREF_USER_NAME_VIDEO_SAVED, edtUserNameVideo.getText().toString(), getActivity());
                            setPrefString(PREF_USER_NAME_DATA_SAVED, edtUserNameData.getText().toString(), getActivity());
                            setPrefString(PREF_USER_NAME_FILE_SAVED, edtUserNameFile.getText().toString(), getActivity());
                            setPrefString(PREF_USER_NAME_CHAT_SAVED, edtUserNameChat.getText().toString(), getActivity());
                            setPrefString(PREF_USER_NAME_MULTI_VIDEOS_SAVED, edtUserNameMultiVideos.getText().toString(), getActivity());
                        }
                        break;
                }
            }
        });

        // Button to reset all values to Default values (from Constants.java)
        btnReset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Set UI values to default values
                edtRoomNameAudio.setText(ROOM_NAME_AUDIO_DEFAULT);
                edtRoomNameChat.setText(ROOM_NAME_CHAT_DEFAULT);
                edtRoomNameData.setText(ROOM_NAME_DATA_DEFAULT);
                edtRoomNameFile.setText(ROOM_NAME_FILE_DEFAULT);
                edtRoomNameMultiVideos.setText(ROOM_NAME_MULTI_VIDEOS_DEFAULT);
                edtRoomNameVideo.setText(ROOM_NAME_VIDEO_DEFAULT);
                edtRoomNameCommon.setText(ROOM_NAME_COMMON_DEFAULT);

                edtUserNameAudio.setText(USER_NAME_AUDIO_DEFAULT);
                edtUserNameChat.setText(USER_NAME_CHAT_DEFAULT);
                edtUserNameData.setText(USER_NAME_DATA_DEFAULT);
                edtUserNameFile.setText(USER_NAME_FILE_DEFAULT);
                edtUserNameMultiVideos.setText(USER_NAME_MULTI_VIDEOS_DEFAULT);
                edtUserNameVideo.setText(USER_NAME_VIDEO_DEFAULT);
                edtUserNameCommon.setText(USER_NAME_COMMON_DEFAULT);

                // Reset saved and input value store in share preference
                setPrefString(PREF_ROOM_NAME_AUDIO_SAVED, ROOM_NAME_AUDIO_DEFAULT, getActivity());
                setPrefString(PREF_ROOM_NAME_AUDIO_INPUT_VALUE, ROOM_NAME_AUDIO_DEFAULT, getActivity());

                setPrefString(PREF_ROOM_NAME_CHAT_SAVED, ROOM_NAME_CHAT_DEFAULT, getActivity());
                setPrefString(PREF_ROOM_NAME_CHAT_INPUT_VALUE, ROOM_NAME_CHAT_DEFAULT, getActivity());

                setPrefString(PREF_ROOM_NAME_DATA_SAVED, ROOM_NAME_DATA_DEFAULT, getActivity());
                setPrefString(PREF_ROOM_NAME_DATA_INPUT_VALUE, ROOM_NAME_DATA_DEFAULT, getActivity());

                setPrefString(PREF_ROOM_NAME_FILE_SAVED, ROOM_NAME_FILE_DEFAULT, getActivity());
                setPrefString(PREF_ROOM_NAME_FILE_INPUT_VALUE, ROOM_NAME_FILE_DEFAULT, getActivity());

                setPrefString(PREF_ROOM_NAME_MULTI_VIDEOS_SAVED, ROOM_NAME_MULTI_VIDEOS_DEFAULT, getActivity());
                setPrefString(PREF_ROOM_NAME_MULTI_VIDEOS_INPUT_VALUE, ROOM_NAME_MULTI_VIDEOS_DEFAULT, getActivity());

                setPrefString(PREF_ROOM_NAME_VIDEO_SAVED, ROOM_NAME_VIDEO_DEFAULT, getActivity());
                setPrefString(PREF_ROOM_NAME_VIDEO_INPUT_VALUE, ROOM_NAME_VIDEO_DEFAULT, getActivity());

                setPrefString(PREF_ROOM_NAME_COMMON_SAVED, ROOM_NAME_COMMON_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_AUDIO_SAVED, USER_NAME_AUDIO_DEFAULT, getActivity());
                setPrefString(PREF_USER_NAME_AUDIO_INPUT_VALUE, USER_NAME_AUDIO_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_CHAT_SAVED, USER_NAME_CHAT_DEFAULT, getActivity());
                setPrefString(PREF_USER_NAME_CHAT_INPUT_VALUE, USER_NAME_CHAT_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_DATA_SAVED, USER_NAME_DATA_DEFAULT, getActivity());
                setPrefString(PREF_USER_NAME_DATA_INPUT_VALUE, USER_NAME_DATA_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_FILE_SAVED, USER_NAME_FILE_DEFAULT, getActivity());
                setPrefString(PREF_USER_NAME_FILE_INPUT_VALUE, USER_NAME_FILE_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_MULTI_VIDEOS_SAVED, USER_NAME_MULTI_VIDEOS_DEFAULT, getActivity());
                setPrefString(PREF_USER_NAME_MULTI_VIDEOS_INPUT_VALUE, USER_NAME_MULTI_VIDEOS_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_VIDEO_SAVED, USER_NAME_VIDEO_DEFAULT, getActivity());
                setPrefString(PREF_USER_NAME_VIDEO_INPUT_VALUE, USER_NAME_VIDEO_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_COMMON_SAVED, USER_NAME_COMMON_DEFAULT, getActivity());

                switchUseCommon.setChecked(false);
                switchUseCommon.setSelected(false);
            }
        });

        // setup the action bar
        setActionBar();

        return view;
    }

    private void initControls() {
        String inputValueRoomNameAudio = getPrefString(PREF_ROOM_NAME_AUDIO_INPUT_VALUE, ROOM_NAME_AUDIO_DEFAULT, getActivity());
        edtRoomNameAudio.setText(inputValueRoomNameAudio);

        String inputValueRoomNameVideo = getPrefString(PREF_ROOM_NAME_VIDEO_INPUT_VALUE, ROOM_NAME_VIDEO_DEFAULT, getActivity());
        edtRoomNameVideo.setText(inputValueRoomNameVideo);

        String inputValueRoomNameChat = getPrefString(PREF_ROOM_NAME_CHAT_INPUT_VALUE, ROOM_NAME_CHAT_DEFAULT, getActivity());
        edtRoomNameChat.setText(inputValueRoomNameChat);

        String inputValueRoomNameFile = getPrefString(PREF_ROOM_NAME_FILE_INPUT_VALUE, ROOM_NAME_FILE_DEFAULT, getActivity());
        edtRoomNameFile.setText(inputValueRoomNameFile);

        String inputValueRoomNameData = getPrefString(PREF_ROOM_NAME_DATA_INPUT_VALUE, ROOM_NAME_DATA_DEFAULT, getActivity());
        edtRoomNameData.setText(inputValueRoomNameData);

        String inputValueRoomNameMultiVideos = getPrefString(PREF_ROOM_NAME_MULTI_VIDEOS_INPUT_VALUE, ROOM_NAME_MULTI_VIDEOS_DEFAULT, getActivity());
        edtRoomNameMultiVideos.setText(inputValueRoomNameMultiVideos);

        String valueRoomNameCommon = getPrefString(PREF_ROOM_NAME_COMMON_SAVED, ROOM_NAME_COMMON_DEFAULT, getActivity());
        edtRoomNameCommon.setText(valueRoomNameCommon);


        String inputValueUserNameAudio = getPrefString(PREF_USER_NAME_AUDIO_INPUT_VALUE, USER_NAME_AUDIO_DEFAULT, getActivity());
        edtUserNameAudio.setText(inputValueUserNameAudio);

        String inputValueUserNameVideo = getPrefString(PREF_USER_NAME_VIDEO_INPUT_VALUE, USER_NAME_VIDEO_DEFAULT, getActivity());
        edtUserNameVideo.setText(inputValueUserNameVideo);

        String inputValueUserNameChat = getPrefString(PREF_USER_NAME_CHAT_INPUT_VALUE, USER_NAME_CHAT_DEFAULT, getActivity());
        edtUserNameChat.setText(inputValueUserNameChat);

        String inputValueUserNameFile = getPrefString(PREF_USER_NAME_FILE_INPUT_VALUE, USER_NAME_FILE_DEFAULT, getActivity());
        edtUserNameFile.setText(inputValueUserNameFile);

        String inputValueUserNameData = getPrefString(PREF_USER_NAME_DATA_INPUT_VALUE, USER_NAME_DATA_DEFAULT, getActivity());
        edtUserNameData.setText(inputValueUserNameData);

        String inputValueUserNameMultiVideos = getPrefString(PREF_USER_NAME_MULTI_VIDEOS_INPUT_VALUE, USER_NAME_MULTI_VIDEOS_DEFAULT, getActivity());
        edtUserNameMultiVideos.setText(inputValueUserNameMultiVideos);

        String valueUserNameCommon = getPrefString(PREF_USER_NAME_COMMON_SAVED, USER_NAME_COMMON_DEFAULT, getActivity());
        edtUserNameCommon.setText(valueUserNameCommon);

        boolean useCommonSetting = getPrefBoolean(PREF_USE_COMMON_SETTING, false, getActivity());
        switchUseCommon.setChecked(useCommonSetting);

        changeUISeparateSetting(useCommonSetting);

        setFocusChangeListener(edtRoomNameAudio, PREF_ROOM_NAME_AUDIO_SAVED);
        setFocusChangeListener(edtRoomNameChat, PREF_ROOM_NAME_CHAT_SAVED);
        setFocusChangeListener(edtRoomNameData, PREF_ROOM_NAME_DATA_SAVED);
        setFocusChangeListener(edtRoomNameFile, PREF_ROOM_NAME_FILE_SAVED);
        setFocusChangeListener(edtRoomNameMultiVideos, PREF_ROOM_NAME_MULTI_VIDEOS_SAVED);
        setFocusChangeListener(edtRoomNameVideo, PREF_ROOM_NAME_VIDEO_SAVED);
        setFocusChangeListener(edtRoomNameCommon, PREF_ROOM_NAME_COMMON_SAVED);
        //  - User names.
        setFocusChangeListener(edtUserNameAudio, PREF_USER_NAME_AUDIO_SAVED);
        setFocusChangeListener(edtUserNameChat, PREF_USER_NAME_CHAT_SAVED);
        setFocusChangeListener(edtUserNameData, PREF_USER_NAME_DATA_SAVED);
        setFocusChangeListener(edtUserNameFile, PREF_USER_NAME_FILE_SAVED);
        setFocusChangeListener(edtUserNameMultiVideos, PREF_USER_NAME_MULTI_VIDEOS_SAVED);
        setFocusChangeListener(edtUserNameVideo, PREF_USER_NAME_VIDEO_SAVED);
        setFocusChangeListener(edtUserNameCommon, PREF_USER_NAME_COMMON_SAVED);
    }

    private void getControlWidgets(View view) {
        edtUserNameAudio = (EditText) view.findViewById(R.id.edtUserNameAudio);
        edtRoomNameAudio = (EditText) view.findViewById(R.id.edtRoomNameAudio);
        edtUserNameChat = (EditText) view.findViewById(R.id.edtUserNameChat);
        edtRoomNameChat = (EditText) view.findViewById(R.id.edtRoomNameChat);
        edtUserNameData = (EditText) view.findViewById(R.id.edtUserNameData);
        edtRoomNameData = (EditText) view.findViewById(R.id.edtRoomNameData);
        edtUserNameFile = (EditText) view.findViewById(R.id.edtUserNameFile);
        edtRoomNameFile = (EditText) view.findViewById(R.id.edtRoomNameFile);
        edtUserNameMultiVideos = (EditText) view.findViewById(R.id.edtUserNameMultiVideos);
        edtRoomNameMultiVideos = (EditText) view.findViewById(R.id.edtRoomNameMultiVideos);
        edtUserNameVideo = (EditText) view.findViewById(R.id.edtUserNameVideo);
        edtRoomNameVideo = (EditText) view.findViewById(R.id.edtRoomNameVideo);
        edtUserNameCommon = (EditText) view.findViewById(R.id.edtUserNameCommon);
        edtRoomNameCommon = (EditText) view.findViewById(R.id.edtRoomNameCommon);
        switchUseCommon = (Switch) view.findViewById(R.id.switch_use_common_settings);
        btnReset = (Button) view.findViewById(R.id.btnResetDefault);
    }

    private void processSaveCommonValue(String savedRoomNameCommon, String savedUserNameCommon) {
        // change the saved value
        setPrefString(PREF_ROOM_NAME_AUDIO_SAVED, savedRoomNameCommon, getActivity());
        setPrefString(PREF_ROOM_NAME_VIDEO_SAVED, savedRoomNameCommon, getActivity());
        setPrefString(PREF_ROOM_NAME_CHAT_SAVED, savedRoomNameCommon, getActivity());
        setPrefString(PREF_ROOM_NAME_FILE_SAVED, savedRoomNameCommon, getActivity());
        setPrefString(PREF_ROOM_NAME_DATA_SAVED, savedRoomNameCommon, getActivity());
        setPrefString(PREF_ROOM_NAME_MULTI_VIDEOS_SAVED, savedRoomNameCommon, getActivity());
        setPrefString(PREF_ROOM_NAME_COMMON_SAVED, savedRoomNameCommon, getActivity());

        setPrefString(PREF_USER_NAME_AUDIO_SAVED, savedUserNameCommon, getActivity());
        setPrefString(PREF_USER_NAME_VIDEO_SAVED, savedUserNameCommon, getActivity());
        setPrefString(PREF_USER_NAME_DATA_SAVED, savedUserNameCommon, getActivity());
        setPrefString(PREF_USER_NAME_FILE_SAVED, savedUserNameCommon, getActivity());
        setPrefString(PREF_USER_NAME_CHAT_SAVED, savedUserNameCommon, getActivity());
        setPrefString(PREF_USER_NAME_MULTI_VIDEOS_SAVED, savedUserNameCommon, getActivity());
        setPrefString(PREF_USER_NAME_COMMON_SAVED, savedUserNameCommon, getActivity());
    }

    private void changeUISeparateSetting(boolean useCommonSetting) {
        if (useCommonSetting) {
            // change the UI
            edtRoomNameAudio.setEnabled(false);
            edtRoomNameAudio.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
            edtRoomNameChat.setEnabled(false);
            edtRoomNameChat.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
            edtRoomNameData.setEnabled(false);
            edtRoomNameData.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
            edtRoomNameFile.setEnabled(false);
            edtRoomNameFile.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
            edtRoomNameMultiVideos.setEnabled(false);
            edtRoomNameMultiVideos.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
            edtRoomNameVideo.setEnabled(false);
            edtRoomNameVideo.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
            edtUserNameAudio.setEnabled(false);
            edtUserNameAudio.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
            edtUserNameChat.setEnabled(false);
            edtUserNameChat.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
            edtUserNameData.setEnabled(false);
            edtUserNameData.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
            edtUserNameFile.setEnabled(false);
            edtUserNameFile.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
            edtUserNameMultiVideos.setEnabled(false);
            edtUserNameMultiVideos.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
            edtUserNameVideo.setEnabled(false);
            edtUserNameVideo.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));

            edtRoomNameCommon.setEnabled(true);
            edtRoomNameCommon.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));
            edtUserNameCommon.setEnabled(true);
            edtUserNameCommon.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));

        } else {
            edtRoomNameAudio.setEnabled(true);
            edtRoomNameAudio.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));
            edtRoomNameChat.setEnabled(true);
            edtRoomNameChat.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));
            edtRoomNameData.setEnabled(true);
            edtRoomNameData.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));
            edtRoomNameFile.setEnabled(true);
            edtRoomNameFile.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));
            edtRoomNameMultiVideos.setEnabled(true);
            edtRoomNameMultiVideos.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));
            edtRoomNameVideo.setEnabled(true);
            edtRoomNameVideo.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));
            edtUserNameAudio.setEnabled(true);
            edtUserNameAudio.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));
            edtUserNameChat.setEnabled(true);
            edtUserNameChat.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));
            edtUserNameData.setEnabled(true);
            edtUserNameData.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));
            edtUserNameFile.setEnabled(true);
            edtUserNameFile.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));
            edtUserNameMultiVideos.setEnabled(true);
            edtUserNameMultiVideos.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));
            edtUserNameVideo.setEnabled(true);
            edtUserNameVideo.setBackground(getResources().getDrawable(R.drawable.custom_edit_text));

            edtRoomNameCommon.setEnabled(false);
            edtRoomNameCommon.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
            edtUserNameCommon.setEnabled(false);
            edtUserNameCommon.setBackground(getResources().getDrawable(R.drawable.custom_edit_text_disable));
        }
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
                edtRoomNameCommon.setText(ROOM_NAME_COMMON_DEFAULT);

                edtUserNameAudio.setText(USER_NAME_AUDIO_DEFAULT);
                edtUserNameChat.setText(USER_NAME_CHAT_DEFAULT);
                edtUserNameData.setText(USER_NAME_DATA_DEFAULT);
                edtUserNameFile.setText(USER_NAME_FILE_DEFAULT);
                edtUserNameMultiVideos.setText(USER_NAME_MULTI_VIDEOS_DEFAULT);
                edtUserNameVideo.setText(USER_NAME_VIDEO_DEFAULT);
                edtUserNameCommon.setText(USER_NAME_COMMON_DEFAULT);

                // Reset saved and input value store in share preference
                setPrefString(PREF_ROOM_NAME_AUDIO_SAVED, ROOM_NAME_AUDIO_DEFAULT, getActivity());
                setPrefString(PREF_ROOM_NAME_AUDIO_INPUT_VALUE, ROOM_NAME_AUDIO_DEFAULT, getActivity());

                setPrefString(PREF_ROOM_NAME_CHAT_SAVED, ROOM_NAME_CHAT_DEFAULT, getActivity());
                setPrefString(PREF_ROOM_NAME_CHAT_INPUT_VALUE, ROOM_NAME_CHAT_DEFAULT, getActivity());

                setPrefString(PREF_ROOM_NAME_DATA_SAVED, ROOM_NAME_DATA_DEFAULT, getActivity());
                setPrefString(PREF_ROOM_NAME_DATA_INPUT_VALUE, ROOM_NAME_DATA_DEFAULT, getActivity());

                setPrefString(PREF_ROOM_NAME_FILE_SAVED, ROOM_NAME_FILE_DEFAULT, getActivity());
                setPrefString(PREF_ROOM_NAME_FILE_INPUT_VALUE, ROOM_NAME_FILE_DEFAULT, getActivity());

                setPrefString(PREF_ROOM_NAME_MULTI_VIDEOS_SAVED, ROOM_NAME_MULTI_VIDEOS_DEFAULT, getActivity());
                setPrefString(PREF_ROOM_NAME_MULTI_VIDEOS_INPUT_VALUE, ROOM_NAME_MULTI_VIDEOS_DEFAULT, getActivity());

                setPrefString(PREF_ROOM_NAME_VIDEO_SAVED, ROOM_NAME_VIDEO_DEFAULT, getActivity());
                setPrefString(PREF_ROOM_NAME_VIDEO_INPUT_VALUE, ROOM_NAME_VIDEO_DEFAULT, getActivity());

                setPrefString(PREF_ROOM_NAME_COMMON_SAVED, ROOM_NAME_COMMON_DEFAULT, getActivity());


                setPrefString(PREF_USER_NAME_AUDIO_SAVED, USER_NAME_AUDIO_DEFAULT, getActivity());
                setPrefString(PREF_USER_NAME_AUDIO_INPUT_VALUE, USER_NAME_AUDIO_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_CHAT_SAVED, USER_NAME_CHAT_DEFAULT, getActivity());
                setPrefString(PREF_USER_NAME_CHAT_INPUT_VALUE, USER_NAME_CHAT_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_DATA_SAVED, USER_NAME_DATA_DEFAULT, getActivity());
                setPrefString(PREF_USER_NAME_DATA_INPUT_VALUE, USER_NAME_DATA_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_FILE_SAVED, USER_NAME_FILE_DEFAULT, getActivity());
                setPrefString(PREF_USER_NAME_FILE_INPUT_VALUE, USER_NAME_FILE_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_MULTI_VIDEOS_SAVED, USER_NAME_MULTI_VIDEOS_DEFAULT, getActivity());
                setPrefString(PREF_USER_NAME_MULTI_VIDEOS_INPUT_VALUE, USER_NAME_MULTI_VIDEOS_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_VIDEO_SAVED, USER_NAME_VIDEO_DEFAULT, getActivity());
                setPrefString(PREF_USER_NAME_VIDEO_INPUT_VALUE, USER_NAME_VIDEO_DEFAULT, getActivity());

                setPrefString(PREF_USER_NAME_COMMON_SAVED, USER_NAME_COMMON_DEFAULT, getActivity());

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
                        case PREF_ROOM_NAME_AUDIO_SAVED:
                            setPrefString(PREF_ROOM_NAME_AUDIO_SAVED, uiValue, getActivity());
                            setPrefString(PREF_ROOM_NAME_AUDIO_INPUT_VALUE, uiValue, getActivity());
                            break;
                        case PREF_ROOM_NAME_CHAT_SAVED:
                            setPrefString(PREF_ROOM_NAME_CHAT_SAVED, uiValue, getActivity());
                            setPrefString(PREF_ROOM_NAME_CHAT_INPUT_VALUE, uiValue, getActivity());
                            break;
                        case PREF_ROOM_NAME_DATA_SAVED:
                            setPrefString(PREF_ROOM_NAME_DATA_SAVED, uiValue, getActivity());
                            setPrefString(PREF_ROOM_NAME_DATA_INPUT_VALUE, uiValue, getActivity());
                            break;
                        case PREF_ROOM_NAME_FILE_SAVED:
                            setPrefString(PREF_ROOM_NAME_FILE_SAVED, uiValue, getActivity());
                            setPrefString(PREF_ROOM_NAME_FILE_INPUT_VALUE, uiValue, getActivity());
                            break;
                        case PREF_ROOM_NAME_MULTI_VIDEOS_SAVED:
                            setPrefString(PREF_ROOM_NAME_MULTI_VIDEOS_SAVED, uiValue, getActivity());
                            setPrefString(PREF_ROOM_NAME_MULTI_VIDEOS_INPUT_VALUE, uiValue, getActivity());
                            break;
                        case PREF_ROOM_NAME_VIDEO_SAVED:
                            setPrefString(PREF_ROOM_NAME_VIDEO_SAVED, uiValue, getActivity());
                            setPrefString(PREF_ROOM_NAME_VIDEO_INPUT_VALUE, uiValue, getActivity());
                            break;
                        case PREF_ROOM_NAME_COMMON_SAVED:
                            setPrefString(PREF_ROOM_NAME_COMMON_SAVED, uiValue, getActivity());
                            processSaveCommonValue(edtRoomNameCommon.getText().toString(), edtUserNameCommon.getText().toString());
                            break;

                        // User name types
                        case PREF_USER_NAME_AUDIO_SAVED:
                            setPrefString(PREF_USER_NAME_AUDIO_SAVED, uiValue, getActivity());
                            setPrefString(PREF_USER_NAME_AUDIO_INPUT_VALUE, uiValue, getActivity());
                            break;
                        case PREF_USER_NAME_CHAT_SAVED:
                            setPrefString(PREF_USER_NAME_CHAT_SAVED, uiValue, getActivity());
                            setPrefString(PREF_USER_NAME_CHAT_INPUT_VALUE, uiValue, getActivity());
                            break;
                        case PREF_USER_NAME_DATA_SAVED:
                            setPrefString(PREF_USER_NAME_DATA_SAVED, uiValue, getActivity());
                            setPrefString(PREF_USER_NAME_DATA_INPUT_VALUE, uiValue, getActivity());
                            break;
                        case PREF_USER_NAME_FILE_SAVED:
                            setPrefString(PREF_USER_NAME_FILE_SAVED, uiValue, getActivity());
                            setPrefString(PREF_USER_NAME_FILE_INPUT_VALUE, uiValue, getActivity());
                            break;
                        case PREF_USER_NAME_MULTI_VIDEOS_SAVED:
                            setPrefString(PREF_USER_NAME_MULTI_VIDEOS_SAVED, uiValue, getActivity());
                            setPrefString(PREF_USER_NAME_MULTI_VIDEOS_INPUT_VALUE, uiValue, getActivity());
                            break;
                        case PREF_USER_NAME_VIDEO_SAVED:
                            setPrefString(PREF_USER_NAME_VIDEO_SAVED, uiValue, getActivity());
                            setPrefString(PREF_USER_NAME_VIDEO_INPUT_VALUE, uiValue, getActivity());
                            break;
                        case PREF_USER_NAME_COMMON_SAVED:
                            setPrefString(PREF_USER_NAME_COMMON_SAVED, uiValue, getActivity());
                            processSaveCommonValue(edtRoomNameCommon.getText().toString(), edtUserNameCommon.getText().toString());
                            break;
                    }
                }
            }
        });
    }
}
