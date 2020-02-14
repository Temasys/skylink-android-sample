package sg.com.temasys.skylink.sdk.sampleapp.setting;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingFragment extends Fragment implements SettingContract.View, View.OnClickListener,
        CompoundButton.OnCheckedChangeListener, View.OnFocusChangeListener {

    private static final String CONFIG_ROOM_FRAGMENT_TAG = "CONFIG_ROOM_FRAGMENT_TAG";
    private static final String CONFIG_KEY_FRAGMENT_TAG = "CONFIG_KEY_FRAGMENT_TAG";

    private final String TAG = SettingFragment.class.getName();

    private Context mContext;
    private ActionBar actionBar;

    private TextView txtRoomSettingName, txtKeySettingName;
    private RadioGroup rdGroupAudioOutput, rdGroupVideoOutput, rdGroupVideoDevice, rdGroupCameraResolution,
            rdGroupScreenResolution, rdGroupAudioVideoSend, rdGroupAudioVideoRec, rdGroupAudioCodec,
            rdGroupWebSocketTransport, rdGroupNetworkTransport, rdGroupRoomSize;
    private RadioButton audioHeadset, audioSpeaker, videoHeadset, videoSpeaker, cameraNone, cameraCustom, cameraFront,
            cameraBack, screenDevice, rdVideoResVGA, rdVideoResHDR, rdVideoResFHD, rdVideoResLarge,
            rdVideoResMedium, rdVideoResSmall, rdAudioVideoSend, rdAudioSendOnly, rdVideoSendOnly,
            rdNoneAudioVideoSend, rdAudioVideoRec, rdAudioRecOnly, rdVideoRecOnly,
            rdNoneAudioVideoRec, rdAudioCodecOPUS, rdAudioCodecISAC, rdSocketPolling, rdSocketWeb,
            rdNetworkUDP, rdNetworkTCP, rdRoomSizeSX, rdRoomSizeS, rdRoomSizeM, rdRoomSizeL;
    private Switch switchHasDataTransfer, switchHasPeerMessage, switchHasFileTransfer, switchUseHWAcceleration,
            switchUseH246Profile, switchUseVP8Encoder, switchUseAudioStereo, switchUseAutoGainControl,
            switchUseAudioEchoCancellation, switchUseAudioHighPassFilter, switchUseAudioNoiseSuppression,
            switchMirrorLocalView, switchReportVideoResStable, switchReportVideoResWhenChanged,
            switchUseTurnServer, switchUseSturnServer, switchUseHostServer, switchAllowIceRestart, switchUseMultiTracksUP;
    private EditText editVideoResStableNo, editVideoResWaitNo, editMaxAudioBitrate, editMaxVideoBitrate,
            editMaxDataBitrate, editMaxAudioRoomPeer, editMaxVideoRoomPeer, editMaxNoneMediaRoomPeer,
            editReconnectAttempts, editReconnectDelay, editEncryptedSecret;


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

        // get the UI controls from layout
        getControlWidgets(rootView);

        // setup the action bar
        setActionBar();

        initControls();

        //request an initiative connection
        requestViewLayout();

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Get menu inflater object.
        MenuInflater menuInflater = ((SettingActivity) mContext).getMenuInflater();
        // Inflate the custom overflow menu
        menuInflater.inflate(R.menu.setting_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_reset:
                mPresenter.onProcessResetAllValues();
                break;
            case android.R.id.home:
                FragmentManager fm = ((SettingActivity) mContext).getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                    return true;
                }
                break;
        }

        return super.onOptionsItemSelected(item);
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
            case R.id.camera_none:
                processCameraNone();
                break;
            case R.id.camera_custom:
                processCameraCustom();
                break;
            case R.id.camera_front:
                processCameraFront();
                break;
            case R.id.camera_back:
                processCameraBack();
                break;
            case R.id.screen_device:
                processScreenDevice();
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
            case R.id.rd_video_res_large_screen:
                processVideoResScreenLarge();
                break;
            case R.id.rd_video_res_medium_screen:
                processVideoResScreenMedium();
                break;
            case R.id.rd_video_res_small_screen:
                processVideoResScreenSmall();
                break;
            case R.id.rdAudioVideoSend:
                mPresenter.onProcessAudioVideoSend(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
                break;
            case R.id.rdAudioOnlySend:
                mPresenter.onProcessAudioVideoSend(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
                break;
            case R.id.rdVideoOnlySend:
                mPresenter.onProcessAudioVideoSend(SkylinkConfig.AudioVideoConfig.VIDEO_ONLY);
                break;
            case R.id.rdNoAudioNoVideoSend:
                mPresenter.onProcessAudioVideoSend(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
                break;
            case R.id.rdAudioVideoRec:
                mPresenter.onProcessAudioVideoRec(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
                break;
            case R.id.rdAudioOnlyRec:
                mPresenter.onProcessAudioVideoRec(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
                break;
            case R.id.rdVideoOnlyRec:
                mPresenter.onProcessAudioVideoRec(SkylinkConfig.AudioVideoConfig.VIDEO_ONLY);
                break;
            case R.id.rdNoAudioNoVideoRec:
                mPresenter.onProcessAudioVideoRec(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
                break;
            case R.id.rdAudioCodecOpus:
                mPresenter.onProcessAudioCodec(SkylinkConfig.AudioCodec.OPUS);
                break;
            case R.id.rdAudioCodecIsac:
                mPresenter.onProcessAudioCodec(SkylinkConfig.AudioCodec.ISAC);
                break;
            case R.id.rdSocketPolling:
                mPresenter.onProcessWebSocketTransport(SkylinkConfig.SocketTransport.POLLING);
                break;
            case R.id.rdSocketWebSocket:
                mPresenter.onProcessWebSocketTransport(SkylinkConfig.SocketTransport.WEBSOCKET);
                break;
            case R.id.rdNetworkUDP:
                mPresenter.onProcessNetworkTransport(SkylinkConfig.NetworkTransport.UDP);
                break;
            case R.id.rdNetworkTCP:
                mPresenter.onProcessNetworkTransport(SkylinkConfig.NetworkTransport.TCP);
                break;
            case R.id.rdRoomSizeXS:
                mPresenter.onProcessRoomSize(SkylinkConfig.SkylinkRoomSize.EXTRA_SMALL);
                break;
            case R.id.rdRoomSizeS:
                mPresenter.onProcessRoomSize(SkylinkConfig.SkylinkRoomSize.SMALL);
                break;
            case R.id.rdRoomSizeM:
                mPresenter.onProcessRoomSize(SkylinkConfig.SkylinkRoomSize.MEDIUM);
                break;
            case R.id.rdRoomSizeL:
                mPresenter.onProcessRoomSize(SkylinkConfig.SkylinkRoomSize.LARGE);
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
        rdVideoResVGA.setChecked(true);
        rdVideoResHDR.setChecked(false);
        rdVideoResFHD.setChecked(false);
    }

    @Override
    public void onVideoResHDRSelected() {
        rdVideoResVGA.setChecked(false);
        rdVideoResHDR.setChecked(true);
        rdVideoResFHD.setChecked(false);
    }

    @Override
    public void onVideoResFHDSelected() {
        rdVideoResVGA.setChecked(false);
        rdVideoResHDR.setChecked(false);
        rdVideoResFHD.setChecked(true);
    }

    @Override
    public void onVideoResScreenLargeSelected() {
        rdVideoResLarge.setChecked(true);
        rdVideoResMedium.setChecked(false);
        rdVideoResSmall.setChecked(false);
    }

    @Override
    public void onVideoResScreenMediumSelected() {
        rdVideoResLarge.setChecked(false);
        rdVideoResMedium.setChecked(true);
        rdVideoResSmall.setChecked(false);
    }

    @Override
    public void onVideoResScreenSmallSelected() {
        rdVideoResLarge.setChecked(false);
        rdVideoResMedium.setChecked(false);
        rdVideoResSmall.setChecked(true);
    }

    @Override
    public void onCameraNoneSelected() {
        cameraNone.setChecked(true);
        cameraCustom.setChecked(false);
        cameraFront.setChecked(false);
        cameraBack.setChecked(false);
        screenDevice.setChecked(false);
    }

    @Override
    public void onCameraCustomSelected() {
        cameraNone.setChecked(false);
        cameraCustom.setChecked(true);
        cameraFront.setChecked(false);
        cameraBack.setChecked(false);
        screenDevice.setChecked(false);
    }

    @Override
    public void onCameraFrontSelected() {
        cameraNone.setChecked(false);
        cameraCustom.setChecked(false);
        cameraFront.setChecked(true);
        cameraBack.setChecked(false);
        screenDevice.setChecked(false);
    }

    @Override
    public void onCameraBackSelected() {
        cameraNone.setChecked(false);
        cameraCustom.setChecked(false);
        cameraFront.setChecked(false);
        cameraBack.setChecked(true);
        screenDevice.setChecked(false);
    }

    @Override
    public void onScreenDeviceSelected() {
        cameraNone.setChecked(false);
        cameraCustom.setChecked(false);
        cameraFront.setChecked(false);
        cameraBack.setChecked(false);
        screenDevice.setChecked(true);
    }

    @Override
    public void onAudioVideoSendSelected() {
        rdAudioVideoSend.setChecked(true);
        rdAudioSendOnly.setChecked(false);
        rdVideoSendOnly.setChecked(false);
        rdNoneAudioVideoSend.setChecked(false);
    }

    @Override
    public void onAudioSendOnlySelected() {
        rdAudioVideoSend.setChecked(false);
        rdAudioSendOnly.setChecked(true);
        rdVideoSendOnly.setChecked(false);
        rdNoneAudioVideoSend.setChecked(false);
    }

    @Override
    public void onVideoSendOnlySelected() {
        rdAudioVideoSend.setChecked(false);
        rdAudioSendOnly.setChecked(false);
        rdVideoSendOnly.setChecked(true);
        rdNoneAudioVideoSend.setChecked(false);
    }

    @Override
    public void onNoneAudioVideoSendSelected() {
        rdAudioVideoSend.setChecked(false);
        rdAudioSendOnly.setChecked(false);
        rdVideoSendOnly.setChecked(false);
        rdNoneAudioVideoSend.setChecked(true);
    }

    @Override
    public void onAudioVideoRecSelected() {
        rdAudioVideoRec.setChecked(true);
        rdAudioRecOnly.setChecked(false);
        rdVideoRecOnly.setChecked(false);
        rdNoneAudioVideoRec.setChecked(false);
    }

    @Override
    public void onAudioRecOnlySelected() {
        rdAudioVideoRec.setChecked(false);
        rdAudioRecOnly.setChecked(true);
        rdVideoRecOnly.setChecked(false);
        rdNoneAudioVideoRec.setChecked(false);
    }

    @Override
    public void onVideoRecOnlySelected() {
        rdAudioVideoRec.setChecked(false);
        rdAudioRecOnly.setChecked(false);
        rdVideoRecOnly.setChecked(true);
        rdNoneAudioVideoRec.setChecked(false);
    }

    @Override
    public void onNoneAudioVideoRecSelected() {
        rdAudioVideoRec.setChecked(false);
        rdAudioRecOnly.setChecked(false);
        rdVideoRecOnly.setChecked(false);
        rdNoneAudioVideoRec.setChecked(true);
    }

    @Override
    public void onAudioCodecOPUSSelected() {
        rdAudioCodecOPUS.setChecked(true);
        rdAudioCodecISAC.setChecked(false);
    }

    @Override
    public void onAudioCodecISACSelected() {
        rdAudioCodecOPUS.setChecked(false);
        rdAudioCodecISAC.setChecked(true);
    }

    @Override
    public void onWebSocketTransportSelected() {
        rdSocketWeb.setChecked(true);
        rdSocketPolling.setChecked(false);
    }

    @Override
    public void onPollingTransportSelected() {
        rdSocketWeb.setChecked(false);
        rdSocketPolling.setChecked(true);
    }

    @Override
    public void onNetworkTransportUDPSelected() {
        rdNetworkUDP.setChecked(true);
        rdNetworkTCP.setChecked(false);
    }

    @Override
    public void onNetworkTransportTCPSelected() {
        rdNetworkUDP.setChecked(false);
        rdNetworkTCP.setChecked(true);
    }

    @Override
    public void onRoomSizeXSSelected() {
        rdRoomSizeSX.setChecked(true);
        rdRoomSizeS.setChecked(false);
        rdRoomSizeM.setChecked(false);
        rdRoomSizeL.setChecked(false);
    }

    @Override
    public void onRoomSizeSSelected() {
        rdRoomSizeSX.setChecked(false);
        rdRoomSizeS.setChecked(true);
        rdRoomSizeM.setChecked(false);
        rdRoomSizeL.setChecked(false);
    }

    @Override
    public void onRoomSizeMSelected() {
        rdRoomSizeSX.setChecked(false);
        rdRoomSizeS.setChecked(false);
        rdRoomSizeM.setChecked(true);
        rdRoomSizeL.setChecked(false);
    }

    @Override
    public void onRoomSizeLSelected() {
        rdRoomSizeSX.setChecked(false);
        rdRoomSizeS.setChecked(false);
        rdRoomSizeM.setChecked(false);
        rdRoomSizeL.setChecked(true);
    }

    @Override
    public void onDataTransferSelected(boolean hasDataTransfer) {
        switchHasDataTransfer.setChecked(hasDataTransfer);
    }

    @Override
    public void onFileTransferSelected(boolean hasDataTransfer) {
        switchHasFileTransfer.setChecked(hasDataTransfer);
    }

    @Override
    public void onMessagingSelected(boolean hasMessaging) {
        switchHasPeerMessage.setChecked(hasMessaging);
    }

    @Override
    public void onEncryptedSecretFilled(String defaultEncryptedSecret) {
        if (defaultEncryptedSecret != null)
            editEncryptedSecret.setText(defaultEncryptedSecret);
    }

    @Override
    public void onVideoHwAccSelected(boolean useHWAcc) {
        switchUseHWAcceleration.setChecked(useHWAcc);
    }

    @Override
    public void onH246ProfileSelected(boolean useH246Profile) {
        switchUseH246Profile.setChecked(useH246Profile);
    }

    @Override
    public void onVp8EncoderSelected(boolean useIntelVP8Encoder) {
        switchUseVP8Encoder.setChecked(useIntelVP8Encoder);
    }

    @Override
    public void onAudioSterioSelected(boolean useAudioSterio) {
        switchUseAudioStereo.setChecked(useAudioSterio);
    }

    @Override
    public void onAudioAutoGainSelected(boolean useAudioAutoGain) {
        switchUseAutoGainControl.setChecked(useAudioAutoGain);
    }

    @Override
    public void onAudioEchoCancellationSelected(boolean useAudioEchoCancellation) {
        switchUseAudioEchoCancellation.setChecked(useAudioEchoCancellation);
    }

    @Override
    public void onAudioHighPassFilterSelected(boolean useAudioHighPassFilter) {
        switchUseAudioHighPassFilter.setChecked(useAudioHighPassFilter);
    }

    @Override
    public void onAudioNoiseSuppressionSelected(boolean useAudioNoiseSuppression) {
        switchUseAudioNoiseSuppression.setChecked(useAudioNoiseSuppression);
    }

    @Override
    public void onMirrorLocalViewSelected(boolean useMirrorLocalView) {
        switchMirrorLocalView.setChecked(useMirrorLocalView);
    }

    @Override
    public void onAutoReportVideoResSelected(boolean autoReportVideoRes) {
        switchReportVideoResWhenChanged.setChecked(autoReportVideoRes);
    }

    @Override
    public void onAutoReportVideoResUntillStableSelected(boolean autoReportVideoResUntillStable) {
        switchReportVideoResStable.setChecked(autoReportVideoResUntillStable);
    }

    @Override
    public void onNoOfCheckingVideoResForStableFilled(int noOfCheckingVideoResForStable) {
        editVideoResStableNo.setText(noOfCheckingVideoResForStable + "");
    }

    @Override
    public void onNoOfTimeWaitingForNextCheckingVideoResFilled(int noOfTimeWaitingForNextCheckingVideoRes) {
        editVideoResWaitNo.setText(noOfTimeWaitingForNextCheckingVideoRes + "");
    }

    @Override
    public void onMaxAudioBitrateFilled(int maxAudioBitrate) {
        editMaxAudioBitrate.setText(maxAudioBitrate + "");
    }

    @Override
    public void onMaxVideoBitrateFilled(int maxVideoBitrate) {
        editMaxVideoBitrate.setText(maxVideoBitrate + "");
    }

    @Override
    public void onMaxDataBitrateFilled(int maxDataBitrate) {
        editMaxDataBitrate.setText(maxDataBitrate + "");
    }

    @Override
    public void onMaxPeerInAudioRoomFilled(int maxPeerInAudioRoom) {
        editMaxAudioRoomPeer.setText(maxPeerInAudioRoom + "");
    }

    @Override
    public void onMaxPeerInVideoRoomFilled(int maxPeerInVideoRoom) {
        editMaxVideoRoomPeer.setText(maxPeerInVideoRoom + "");
    }

    @Override
    public void onMaxPeerInNoMediaRoomFilled(int maxPeerInNoMediaRoom) {
        editMaxNoneMediaRoomPeer.setText(maxPeerInNoMediaRoom + "");
    }

    @Override
    public void onUseTurnServerSelected(boolean useTurnServer) {
        switchUseTurnServer.setChecked(useTurnServer);
    }

    @Override
    public void onUseSTurnServerSelected(boolean useSTurnServer) {
        switchUseSturnServer.setChecked(useSTurnServer);
    }

    @Override
    public void onUseHostServerSelected(boolean useHostServer) {
        switchUseHostServer.setChecked(useHostServer);
    }

    @Override
    public void onAllowIceRestartSelected(boolean allowIceRestart) {
        switchAllowIceRestart.setChecked(allowIceRestart);
    }

    @Override
    public void onUseMultiTrackSelected(boolean useMultiTrack) {
        switchUseMultiTracksUP.setChecked(useMultiTrack);
    }

    @Override
    public void onNoOfReconnectAttemptsFilled(int noOfReconnectAttempts) {
        editReconnectAttempts.setText(noOfReconnectAttempts + "");
    }

    @Override
    public void onNoOfReconnectDelayFilled(int noOfReconnectDelay) {
        editReconnectDelay.setText(noOfReconnectDelay + "");
    }

    private void processSettingRoom() {
        //replace current fragment with ConfigRoomFragment
        ConfigRoomFragment configRoomFragment = new ConfigRoomFragment();
        ((SettingActivity) mContext).getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFrameSetting, configRoomFragment, CONFIG_ROOM_FRAGMENT_TAG)
                .addToBackStack(CONFIG_ROOM_FRAGMENT_TAG)
                .commit();
    }

    private void processSettingKey() {
        //replace current fragment with ConfigKeyFragment
        ConfigKeyFragment configKeyFragment = new ConfigKeyFragment();
        ((SettingActivity) mContext).getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFrameSetting, configKeyFragment, CONFIG_KEY_FRAGMENT_TAG)
                .addToBackStack(CONFIG_KEY_FRAGMENT_TAG)
                .commit();
    }

    private void processAudioHeadset() {
        mPresenter.onProcessSpeakerAudio(false);
    }

    private void processAudioSpeaker() {
        mPresenter.onProcessSpeakerAudio(true);
    }

    private void processVideoHeadset() {
        mPresenter.onProcessSpeakerVideo(false);
    }

    private void processVideoSpeaker() {
        mPresenter.onProcessSpeakerVideo(true);
    }

    private void processCameraNone() {
        mPresenter.onProcessVideoDevice(getString(R.string.no_camera));
    }

    private void processCameraCustom() {
        mPresenter.onProcessVideoDevice(getString(R.string.camera_custom));
    }

    private void processCameraFront() {
        mPresenter.onProcessVideoDevice(SkylinkConfig.VideoDevice.CAMERA_FRONT.getDeviceName());
    }

    private void processCameraBack() {
        mPresenter.onProcessVideoDevice(SkylinkConfig.VideoDevice.CAMERA_BACK.getDeviceName());
    }

    private void processScreenDevice() {
        mPresenter.onProcessVideoDevice(SkylinkConfig.VideoDevice.SCREEN.getDeviceName());
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

    private void processVideoResScreenLarge() {
        mPresenter.onProcessScreenResolution(Config.ScreenResolution.LARGE_PORTRAIT);
    }

    private void processVideoResScreenMedium() {
        mPresenter.onProcessScreenResolution(Config.ScreenResolution.MEDIUM_PORTRAIT);
    }

    private void processVideoResScreenSmall() {
        mPresenter.onProcessScreenResolution(Config.ScreenResolution.SMALL_PORTRAIT);
    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        txtRoomSettingName = rootView.findViewById(R.id.txt_room_setting_name);
        txtKeySettingName = rootView.findViewById(R.id.txt_key_setting_name);
        rdGroupAudioOutput = rootView.findViewById(R.id.rdGroupAudioOutput);
        rdGroupVideoOutput = rootView.findViewById(R.id.rdGroupVideoOutput);
        rdGroupVideoDevice = rootView.findViewById(R.id.rdGroupVideoDevice);
        rdGroupCameraResolution = rootView.findViewById(R.id.rdGroupVideoResolution_cam);
        rdGroupScreenResolution = rootView.findViewById(R.id.rdGroupVideoResolution_screen);
        audioHeadset = rootView.findViewById(R.id.audioHeadset);
        audioSpeaker = rootView.findViewById(R.id.audioSpeaker);
        videoHeadset = rootView.findViewById(R.id.videoHeadset);
        videoSpeaker = rootView.findViewById(R.id.videoSpeaker);
        cameraNone = rootView.findViewById(R.id.camera_none);
        cameraCustom = rootView.findViewById(R.id.camera_custom);
        cameraFront = rootView.findViewById(R.id.camera_front);
        cameraBack = rootView.findViewById(R.id.camera_back);
        screenDevice = rootView.findViewById(R.id.screen_device);
        rdVideoResVGA = rootView.findViewById(R.id.rd_video_res_VGA);
        rdVideoResHDR = rootView.findViewById(R.id.rd_video_res_HDR);
        rdVideoResFHD = rootView.findViewById(R.id.rd_video_res_FHD);
        rdVideoResLarge = rootView.findViewById(R.id.rd_video_res_large_screen);
        rdVideoResMedium = rootView.findViewById(R.id.rd_video_res_medium_screen);
        rdVideoResSmall = rootView.findViewById(R.id.rd_video_res_small_screen);

        rdAudioVideoSend = rootView.findViewById(R.id.rdAudioVideoSend);
        rdAudioSendOnly = rootView.findViewById(R.id.rdAudioOnlySend);
        rdVideoSendOnly = rootView.findViewById(R.id.rdVideoOnlySend);
        rdNoneAudioVideoSend = rootView.findViewById(R.id.rdNoAudioNoVideoSend);

        rdAudioVideoRec = rootView.findViewById(R.id.rdAudioVideoRec);
        rdAudioRecOnly = rootView.findViewById(R.id.rdAudioOnlyRec);
        rdVideoRecOnly = rootView.findViewById(R.id.rdVideoOnlyRec);
        rdNoneAudioVideoRec = rootView.findViewById(R.id.rdNoAudioNoVideoRec);

        rdAudioCodecOPUS = rootView.findViewById(R.id.rdAudioCodecOpus);
        rdAudioCodecISAC = rootView.findViewById(R.id.rdAudioCodecIsac);

        rdSocketPolling = rootView.findViewById(R.id.rdSocketPolling);
        rdSocketWeb = rootView.findViewById(R.id.rdSocketWebSocket);

        rdNetworkUDP = rootView.findViewById(R.id.rdNetworkUDP);
        rdNetworkTCP = rootView.findViewById(R.id.rdNetworkTCP);

        rdRoomSizeSX = rootView.findViewById(R.id.rdRoomSizeXS);
        rdRoomSizeS = rootView.findViewById(R.id.rdRoomSizeS);
        rdRoomSizeM = rootView.findViewById(R.id.rdRoomSizeM);
        rdRoomSizeL = rootView.findViewById(R.id.rdRoomSizeL);

        switchHasDataTransfer = rootView.findViewById(R.id.switchHasdataTransfer);
        switchHasPeerMessage = rootView.findViewById(R.id.switchHasPeerMessaging);
        switchHasFileTransfer = rootView.findViewById(R.id.switchHasFileTransfer);

        switchUseHWAcceleration = rootView.findViewById(R.id.switchUseHWAcc);
        switchUseH246Profile = rootView.findViewById(R.id.switchUseH246Profile);
        switchUseVP8Encoder = rootView.findViewById(R.id.switchUseVP8Encoder);
        switchUseAudioStereo = rootView.findViewById(R.id.switchUseAudioStereo);
        switchUseAutoGainControl = rootView.findViewById(R.id.switchUseAutoGainControl);
        switchUseAudioEchoCancellation = rootView.findViewById(R.id.switchUseAudioEchoCancellation);
        switchUseAudioHighPassFilter = rootView.findViewById(R.id.switchUseAudioHighPassFilter);
        switchUseAudioNoiseSuppression = rootView.findViewById(R.id.switchUseAudioNoiseSuppression);
        switchMirrorLocalView = rootView.findViewById(R.id.switchMirrorLocalView);
        switchReportVideoResStable = rootView.findViewById(R.id.switchReportVideoResStable);
        switchReportVideoResWhenChanged = rootView.findViewById(R.id.switchReportVideoResWhenChanged);
        switchUseTurnServer = rootView.findViewById(R.id.switchUseTurnServer);
        switchUseSturnServer = rootView.findViewById(R.id.switchUseSturnServer);
        switchUseHostServer = rootView.findViewById(R.id.switchUseHostServer);
        switchAllowIceRestart = rootView.findViewById(R.id.switchAllowIceRestart);
        switchUseMultiTracksUP = rootView.findViewById(R.id.switchUseMultiTracksUP);

        editVideoResStableNo = rootView.findViewById(R.id.editNumCheckStable);
        editVideoResWaitNo = rootView.findViewById(R.id.editNumWaitS);
        editMaxAudioBitrate = rootView.findViewById(R.id.editMaxAudioBitrate);
        editMaxVideoBitrate = rootView.findViewById(R.id.editMaxVideoBitrate);
        editMaxDataBitrate = rootView.findViewById(R.id.editMaxDataBitrate);
        editMaxAudioRoomPeer = rootView.findViewById(R.id.editMaxPeersOfAudio);
        editMaxVideoRoomPeer = rootView.findViewById(R.id.editMaxPeersOfVideo);
        editMaxNoneMediaRoomPeer = rootView.findViewById(R.id.editMaxPeersOfNoAudioNoVideo);
        editReconnectAttempts = rootView.findViewById(R.id.editReconnectAttempt);
        editReconnectDelay = rootView.findViewById(R.id.editReconnectDelay);
        editEncryptedSecret = rootView.findViewById(R.id.edit_encrypted_secret);
    }

    private void setActionBar() {
        actionBar = ((SettingActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setTitle("Settings");

        setHasOptionsMenu(true);
    }

    private void initControls() {
        txtRoomSettingName.setOnClickListener(this);
        txtKeySettingName.setOnClickListener(this);
        rdGroupAudioOutput.setOnClickListener(this);
        rdGroupVideoOutput.setOnClickListener(this);
        rdGroupVideoDevice.setOnClickListener(this);
        rdGroupCameraResolution.setOnClickListener(this);
        rdGroupScreenResolution.setOnClickListener(this);
        audioHeadset.setOnClickListener(this);
        audioSpeaker.setOnClickListener(this);
        videoHeadset.setOnClickListener(this);
        videoSpeaker.setOnClickListener(this);
        cameraNone.setOnClickListener(this);
        cameraCustom.setOnClickListener(this);
        cameraFront.setOnClickListener(this);
        cameraBack.setOnClickListener(this);
        screenDevice.setOnClickListener(this);
        rdVideoResVGA.setOnClickListener(this);
        rdVideoResHDR.setOnClickListener(this);
        rdVideoResFHD.setOnClickListener(this);
        rdVideoResLarge.setOnClickListener(this);
        rdVideoResMedium.setOnClickListener(this);
        rdVideoResSmall.setOnClickListener(this);

        rdAudioVideoSend.setOnClickListener(this);
        rdAudioSendOnly.setOnClickListener(this);
        rdVideoSendOnly.setOnClickListener(this);
        rdNoneAudioVideoSend.setOnClickListener(this);

        rdAudioVideoRec.setOnClickListener(this);
        rdAudioRecOnly.setOnClickListener(this);
        rdVideoRecOnly.setOnClickListener(this);
        rdNoneAudioVideoRec.setOnClickListener(this);

        rdAudioCodecOPUS.setOnClickListener(this);
        rdAudioCodecISAC.setOnClickListener(this);

        rdSocketPolling.setOnClickListener(this);
        rdSocketWeb.setOnClickListener(this);

        rdNetworkUDP.setOnClickListener(this);
        rdNetworkTCP.setOnClickListener(this);

        rdRoomSizeSX.setOnClickListener(this);
        rdRoomSizeS.setOnClickListener(this);
        rdRoomSizeM.setOnClickListener(this);
        rdRoomSizeL.setOnClickListener(this);

        switchHasDataTransfer.setOnCheckedChangeListener(this);
        switchHasDataTransfer.setOnCheckedChangeListener(this);
        switchHasPeerMessage.setOnCheckedChangeListener(this);
        switchHasFileTransfer.setOnCheckedChangeListener(this);

        switchUseHWAcceleration.setOnCheckedChangeListener(this);
        switchUseH246Profile.setOnCheckedChangeListener(this);
        switchUseVP8Encoder.setOnCheckedChangeListener(this);
        switchUseAudioStereo.setOnCheckedChangeListener(this);
        switchUseAutoGainControl.setOnCheckedChangeListener(this);
        switchUseAudioEchoCancellation.setOnCheckedChangeListener(this);
        switchUseAudioHighPassFilter.setOnCheckedChangeListener(this);
        switchUseAudioNoiseSuppression.setOnCheckedChangeListener(this);
        switchMirrorLocalView.setOnCheckedChangeListener(this);
        switchReportVideoResStable.setOnCheckedChangeListener(this);
        switchReportVideoResWhenChanged.setOnCheckedChangeListener(this);
        switchUseTurnServer.setOnCheckedChangeListener(this);
        switchUseSturnServer.setOnCheckedChangeListener(this);
        switchUseHostServer.setOnCheckedChangeListener(this);
        switchAllowIceRestart.setOnCheckedChangeListener(this);
        switchUseMultiTracksUP.setOnCheckedChangeListener(this);

        rdAudioVideoSend.setChecked(true);
        rdAudioVideoRec.setChecked(true);
        rdAudioCodecOPUS.setChecked(true);
        rdSocketWeb.setChecked(true);
        rdNetworkUDP.setChecked(true);
        rdRoomSizeS.setChecked(true);

        switchHasDataTransfer.setOnClickListener(this);

        editVideoResStableNo.setOnFocusChangeListener(this);
        editVideoResWaitNo.setOnFocusChangeListener(this);
        editMaxAudioBitrate.setOnFocusChangeListener(this);
        editMaxVideoBitrate.setOnFocusChangeListener(this);
        editMaxDataBitrate.setOnFocusChangeListener(this);
        editMaxAudioRoomPeer.setOnFocusChangeListener(this);
        editMaxVideoRoomPeer.setOnFocusChangeListener(this);
        editMaxNoneMediaRoomPeer.setOnFocusChangeListener(this);
        editReconnectAttempts.setOnFocusChangeListener(this);
        editReconnectDelay.setOnFocusChangeListener(this);
        editEncryptedSecret.setOnFocusChangeListener(this);
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switchHasdataTransfer:
                mPresenter.onProcessChangeHasDataTransferState(isChecked);
                break;
            case R.id.switchHasPeerMessaging:
                mPresenter.onProcessChangeHasPeerMessagingState(isChecked);
                break;
            case R.id.switchHasFileTransfer:
                mPresenter.onProcessChangeHasFileTransferState(isChecked);
                break;
            case R.id.switchUseHWAcc:
                mPresenter.onProcessChangeUseHWAccState(isChecked);
                break;
            case R.id.switchUseH246Profile:
                mPresenter.onProcessChangeUseH246ProfileState(isChecked);
                break;
            case R.id.switchUseVP8Encoder:
                mPresenter.onProcessChangeUseVP8EncoderState(isChecked);
                break;
            case R.id.switchUseAudioStereo:
                mPresenter.onProcessChangeUseAudioStereoState(isChecked);
                break;
            case R.id.switchUseAutoGainControl:
                mPresenter.onProcessChangeUseAutoGainControlState(isChecked);
                break;
            case R.id.switchUseAudioEchoCancellation:
                mPresenter.onProcessChangeUseAudioEchoCancellationState(isChecked);
                break;
            case R.id.switchUseAudioHighPassFilter:
                mPresenter.onProcessChangeUseAudioHighPassFilterrState(isChecked);
                break;
            case R.id.switchUseAudioNoiseSuppression:
                mPresenter.onProcessChangeUseAudioNoiseSuppressionState(isChecked);
                break;
            case R.id.switchMirrorLocalView:
                mPresenter.onProcessChangeMirrorLocalViewState(isChecked);
                break;
            case R.id.switchReportVideoResStable:
                mPresenter.onProcessChangeReportVideoResStableState(isChecked);
                break;
            case R.id.switchReportVideoResWhenChanged:
                mPresenter.onProcessChangeReportVideoResWhenChangedState(isChecked);
                break;
            case R.id.switchUseTurnServer:
                mPresenter.onProcessChangeUseTurnServerState(isChecked);
                break;
            case R.id.switchUseSturnServer:
                mPresenter.onProcessChangeUseSturnServerState(isChecked);
                break;
            case R.id.switchUseHostServer:
                mPresenter.onProcessChangeUseHostServerState(isChecked);
                break;
            case R.id.switchAllowIceRestart:
                mPresenter.onProcessChangeAllowIceRestartState(isChecked);
                break;
            case R.id.switchUseMultiTracksUP:
                mPresenter.onProcessChangeUseMultiTracksUPState(isChecked);
                break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.edit_encrypted_secret:
                mPresenter.onProcessEncrytedSecretValue(editEncryptedSecret.getText().toString());
                break;
            case R.id.editNumCheckStable:
                mPresenter.onProcessNumCheckStableValue(editVideoResStableNo.getText().toString());
                break;
            case R.id.editNumWaitS:
                mPresenter.onProcessNumWaitSValue(editVideoResWaitNo.getText().toString());
                break;
            case R.id.editMaxAudioBitrate:
                mPresenter.onProcessMaxAudioBitrateValue(editMaxAudioBitrate.getText().toString());
                break;
            case R.id.editMaxVideoBitrate:
                mPresenter.onProcessMaxVideoBitrateValue(editMaxVideoBitrate.getText().toString());
                break;
            case R.id.editMaxDataBitrate:
                mPresenter.onProcessMaxDataBitrateValue(editMaxDataBitrate.getText().toString());
                break;
            case R.id.editMaxPeersOfAudio:
                mPresenter.onProcessMaxPeersOfAudioValue(editMaxAudioRoomPeer.getText().toString());
                break;
            case R.id.editMaxPeersOfVideo:
                mPresenter.onProcessMaxPeersOfVideoValue(editMaxVideoRoomPeer.getText().toString());
                break;
            case R.id.editMaxPeersOfNoAudioNoVideo:
                mPresenter.onProcessMaxPeersOfNoAudioNoVideValue(editMaxNoneMediaRoomPeer.getText().toString());
                break;
            case R.id.editReconnectAttempt:
                mPresenter.onProcessReconnectAttemptValue(editReconnectAttempts.getText().toString());
                break;
            case R.id.editReconnectDelay:
                mPresenter.onProcessReconnectDelayValue(editReconnectDelay.getText().toString());
                break;
        }
    }
}
