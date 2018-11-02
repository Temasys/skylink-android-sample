package sg.com.temasys.skylink.sdk.sampleapp.setting;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingFragment extends Fragment implements SettingContract.View, View.OnClickListener {

    private static final String CONFIG_ROOM_FRAGMENT_TAG = "CONFIG_ROOM_FRAGMENT_TAG";
    private static final String CONFIG_KEY_FRAGMENT_TAG = "CONFIG_KEY_FRAGMENT_TAG";

    private final String TAG = SettingFragment.class.getName();

    private Context mContext;
    private ActionBar actionBar;

    private TextView txt_room_setting_name, txt_key_setting_name;
    private RadioGroup rdGroupAudioOutput, rdGroupVideoOutput, rdGroupCameraOutput, rdGroupVideoResolution;
    private RadioButton audioHeadset, audioSpeaker, videoHeadset, videoSpeaker, camera_front, camera_back, rd_video_res_VGA, rd_video_res_HDR, rd_video_res_FHD;


    private SettingContract.Presenter mPresenter;


    public static SettingFragment newInstance() {
        return new SettingFragment();
    }

    @Override
    public void setPresenter(SettingContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[SA][Setting][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_setting, container, false);

        getControlWidgets(rootView);

        setActionBar();

        initControls();

        requestViewLayout();

        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        // in case of changing screen orientation, do not close the connection
        if (!((SettingActivity) mContext).isChangingConfigurations()) {
            mPresenter.onViewExit();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.txt_room_setting_name:
                processSettingRoom();
                break;
            case R.id.txt_key_setting_name:
                processSettingKey();
                break;
            case R.id.audioHeadset:
                processAudioHeadset();
                break;
            case R.id.audioSpeaker:
                processAudioSpeaker();
                break;
            case R.id.videoHeadset:
                processVideoHeadset();
                break;
            case R.id.videoSpeaker:
                processVideoSpeaker();
                break;
            case R.id.camera_front:
                processCameraFront();
                break;
            case R.id.camera_back:
                processCameraBack();
                break;
            case R.id.rd_video_res_VGA:
                processVideoResVGA();
                break;
            case R.id.rd_video_res_HDR:
                processVideoResHDR();
                break;
            case R.id.rd_video_res_FHD:
                processVideoResFHD();
                break;
        }
    }

    @Override
    public void onAudioHeadsetSelected() {
        audioHeadset.setChecked(true);
        audioSpeaker.setChecked(false);
    }

    @Override
    public void onAudioSpeakerSelected() {
        audioHeadset.setChecked(false);
        audioSpeaker.setChecked(true);
    }

    @Override
    public void onVideoHeadsetSelected() {
        videoHeadset.setChecked(true);
        videoSpeaker.setChecked(false);
    }

    @Override
    public void onVideoSpeakerSelected() {
        videoHeadset.setChecked(false);
        videoSpeaker.setChecked(true);
    }

    @Override
    public void onVideoResVGASelected() {
        rd_video_res_VGA.setChecked(true);
        rd_video_res_HDR.setChecked(false);
        rd_video_res_FHD.setChecked(false);
    }

    @Override
    public void onVideoResHDRSelected() {
        rd_video_res_VGA.setChecked(false);
        rd_video_res_HDR.setChecked(true);
        rd_video_res_FHD.setChecked(false);
    }

    @Override
    public void onVideoResFHDSelected() {
        rd_video_res_VGA.setChecked(false);
        rd_video_res_HDR.setChecked(false);
        rd_video_res_FHD.setChecked(true);
    }

    @Override
    public void onCameraFrontSelected() {
        camera_front.setChecked(true);
        camera_back.setChecked(false);
    }

    @Override
    public void onCameraBackSelected() {
        camera_front.setChecked(false);
        camera_back.setChecked(true);
    }

    private void processSettingRoom() {
        //replace current fragment with ConfigRoomFragment
        ConfigRoomFragment configRoomFragment = new ConfigRoomFragment();
        ((SettingActivity) mContext).getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFrameSetting, configRoomFragment, CONFIG_ROOM_FRAGMENT_TAG)
                .addToBackStack(CONFIG_ROOM_FRAGMENT_TAG)
                .commit();
        actionBar.setTitle(getString(R.string.room_setting));
    }

    private void processSettingKey() {
        //replace current fragment with ConfigKeyFragment
        ConfigKeyFragment configKeyFragment = new ConfigKeyFragment();
        ((SettingActivity) mContext).getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFrameSetting, configKeyFragment, CONFIG_KEY_FRAGMENT_TAG)
                .addToBackStack(CONFIG_KEY_FRAGMENT_TAG)
                .commit();
        actionBar.setTitle(getString(R.string.key_setting));
    }

    private void processAudioHeadset() {
        mPresenter.onProcessAudioOutput(false);
    }

    private void processAudioSpeaker() {
        mPresenter.onProcessAudioOutput(true);
    }

    private void processVideoHeadset() {
        mPresenter.onProcessVideoOutput(false);
    }

    private void processVideoSpeaker() {
        mPresenter.onProcessVideoOutput(true);
    }

    private void processCameraFront() {
        mPresenter.onProcessCameraOutput(false);
    }

    private void processCameraBack() {
        mPresenter.onProcessCameraOutput(true);
    }

    private void processVideoResVGA() {
        mPresenter.onProcessVideoResolution(Config.VideoResolution.VGA);
    }

    private void processVideoResHDR() {
        mPresenter.onProcessVideoResolution(Config.VideoResolution.HDR);
    }

    private void processVideoResFHD() {
        mPresenter.onProcessVideoResolution(Config.VideoResolution.FHD);
    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        txt_room_setting_name = rootView.findViewById(R.id.txt_room_setting_name);
        txt_key_setting_name = rootView.findViewById(R.id.txt_key_setting_name);
        rdGroupAudioOutput = rootView.findViewById(R.id.rdGroupAudioOutput);
        rdGroupVideoOutput = rootView.findViewById(R.id.rdGroupVideoOutput);
        rdGroupCameraOutput = rootView.findViewById(R.id.rdGroupCameraOutput);
        rdGroupVideoResolution = rootView.findViewById(R.id.rdGroupVideoResolution);
        audioHeadset = rootView.findViewById(R.id.audioHeadset);
        audioSpeaker = rootView.findViewById(R.id.audioSpeaker);
        videoHeadset = rootView.findViewById(R.id.videoHeadset);
        videoSpeaker = rootView.findViewById(R.id.videoSpeaker);
        camera_front = rootView.findViewById(R.id.camera_front);
        camera_back = rootView.findViewById(R.id.camera_back);
        rd_video_res_VGA = rootView.findViewById(R.id.rd_video_res_VGA);
        rd_video_res_HDR = rootView.findViewById(R.id.rd_video_res_HDR);
        rd_video_res_FHD = rootView.findViewById(R.id.rd_video_res_FHD);
    }

    private void setActionBar() {
        actionBar = ((SettingActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        setHasOptionsMenu(true);
    }

    private void initControls() {
        txt_room_setting_name.setOnClickListener(this);
        txt_key_setting_name.setOnClickListener(this);
        rdGroupAudioOutput.setOnClickListener(this);
        rdGroupVideoOutput.setOnClickListener(this);
        rdGroupCameraOutput.setOnClickListener(this);
        rdGroupVideoResolution.setOnClickListener(this);
        audioHeadset.setOnClickListener(this);
        audioSpeaker.setOnClickListener(this);
        videoHeadset.setOnClickListener(this);
        videoSpeaker.setOnClickListener(this);
        camera_front.setOnClickListener(this);
        camera_back.setOnClickListener(this);
        rd_video_res_VGA.setOnClickListener(this);
        rd_video_res_HDR.setOnClickListener(this);
        rd_video_res_FHD.setOnClickListener(this);
    }

    /**
     * request info to display from presenter
     * try to display default settings from save shared preference
     */
    private void requestViewLayout() {
        if (mPresenter != null) {
            mPresenter.onViewLayoutRequested();
        }
    }
}
