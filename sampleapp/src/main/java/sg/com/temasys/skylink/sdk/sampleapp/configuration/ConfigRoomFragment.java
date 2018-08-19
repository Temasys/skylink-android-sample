package sg.com.temasys.skylink.sdk.sampleapp.configuration;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import sg.com.temasys.skylink.sdk.sampleapp.R;

import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.ROOM_NAME_AUDIO;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.ROOM_NAME_CHAT;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.ROOM_NAME_DATA;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.ROOM_NAME_FILE;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.ROOM_NAME_PARTY;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.ROOM_NAME_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.USER_NAME_AUDIO;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.USER_NAME_CHAT;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.USER_NAME_DATA;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.USER_NAME_FILE;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.USER_NAME_PARTY;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.USER_NAME_VIDEO;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.setRoomNameAudio;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.setRoomNameChat;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.setRoomNameData;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.setRoomNameFile;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.setRoomNameParty;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.setRoomNameVideo;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.setUserNameAudio;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.setUserNameChat;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.setUserNameData;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.setUserNameFile;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.setUserNameParty;
import static sg.com.temasys.skylink.sdk.sampleapp.configuration.Config.setUserNameVideo;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_AUDIO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_CHAT_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_DATA_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_FILE_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_PARTY_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.ROOM_NAME_VIDEO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_AUDIO_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_CHAT_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_DATA_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_FILE_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_PARTY_DEFAULT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.USER_NAME_VIDEO_DEFAULT;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConfigRoomFragment extends Fragment {

    public static final String PREF_ROOM_NAME_AUDIO = "RoomNameAudio";
    public static final String PREF_ROOM_NAME_CHAT = "RoomNameChat";
    public static final String PREF_ROOM_NAME_DATA = "RoomNameData";
    public static final String PREF_ROOM_NAME_FILE = "RoomNameFile";
    public static final String PREF_ROOM_NAME_PARTY = "RoomNameParty";
    public static final String PREF_ROOM_NAME_VIDEO = "RoomNameVideo";

    public static final String PREF_USER_NAME_AUDIO = "UserNameAudio";
    public static final String PREF_USER_NAME_CHAT = "UserNameChat";
    public static final String PREF_USER_NAME_DATA = "UserNameData";
    public static final String PREF_USER_NAME_FILE = "UserNameFile";
    public static final String PREF_USER_NAME_PARTY = "UserNameParty";
    public static final String PREF_USER_NAME_VIDEO = "UserNameVideo";

    private EditText edtRoomNameAudio;
    private EditText edtRoomNameChat;
    private EditText edtRoomNameData;
    private EditText edtRoomNameFile;
    private EditText edtRoomNameParty;
    private EditText edtRoomNameVideo;

    private EditText edtUserNameAudio;
    private EditText edtUserNameChat;
    private EditText edtUserNameData;
    private EditText edtUserNameFile;
    private EditText edtUserNameParty;
    private EditText edtUserNameVideo;

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

        // Multi Party Video Function Value Set
        edtUserNameParty = (EditText) view.findViewById(R.id.edtUserNameParty);
        edtUserNameParty.setText(USER_NAME_PARTY);

        edtRoomNameParty = (EditText) view.findViewById(R.id.edtRoomNameParty);
        edtRoomNameParty.setText(ROOM_NAME_PARTY);

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
        setFocusChangeListener(edtRoomNameParty, PREF_ROOM_NAME_PARTY);
        setFocusChangeListener(edtRoomNameVideo, PREF_ROOM_NAME_VIDEO);
        //  - User names.
        setFocusChangeListener(edtUserNameAudio, PREF_USER_NAME_AUDIO);
        setFocusChangeListener(edtUserNameChat, PREF_USER_NAME_CHAT);
        setFocusChangeListener(edtUserNameData, PREF_USER_NAME_DATA);
        setFocusChangeListener(edtUserNameFile, PREF_USER_NAME_FILE);
        setFocusChangeListener(edtUserNameParty, PREF_USER_NAME_PARTY);
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
                edtRoomNameParty.setText(ROOM_NAME_PARTY_DEFAULT);
                edtRoomNameVideo.setText(ROOM_NAME_VIDEO_DEFAULT);

                edtUserNameAudio.setText(USER_NAME_AUDIO_DEFAULT);
                edtUserNameChat.setText(USER_NAME_CHAT_DEFAULT);
                edtUserNameData.setText(USER_NAME_DATA_DEFAULT);
                edtUserNameFile.setText(USER_NAME_FILE_DEFAULT);
                edtUserNameParty.setText(USER_NAME_PARTY_DEFAULT);
                edtUserNameVideo.setText(USER_NAME_VIDEO_DEFAULT);

                // Set Config values to default values and write to Preferences if it had changed.
                setRoomNameAudio(ROOM_NAME_AUDIO_DEFAULT, getActivity());
                setRoomNameChat(ROOM_NAME_CHAT_DEFAULT, getActivity());
                setRoomNameData(ROOM_NAME_DATA_DEFAULT, getActivity());
                setRoomNameFile(ROOM_NAME_FILE_DEFAULT, getActivity());
                setRoomNameParty(ROOM_NAME_PARTY_DEFAULT, getActivity());
                setRoomNameVideo(ROOM_NAME_VIDEO_DEFAULT, getActivity());

                setUserNameAudio(USER_NAME_AUDIO_DEFAULT, getActivity());
                setUserNameChat(USER_NAME_CHAT_DEFAULT, getActivity());
                setUserNameData(USER_NAME_DATA_DEFAULT, getActivity());
                setUserNameFile(USER_NAME_FILE_DEFAULT, getActivity());
                setUserNameParty(USER_NAME_PARTY_DEFAULT, getActivity());
                setUserNameVideo(USER_NAME_VIDEO_DEFAULT, getActivity());
            }
        });

        return view;
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
                        case PREF_ROOM_NAME_PARTY:
                            setRoomNameParty(uiValue, getActivity());
                            editText.setText(ROOM_NAME_PARTY);
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
                        case PREF_USER_NAME_PARTY:
                            setUserNameParty(uiValue, getActivity());
                            editText.setText(USER_NAME_PARTY);
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
