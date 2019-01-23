package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * A simple {@link Fragment} subclass.
 * This class is responsible for display UI and get user interaction
 */
public class VideoCallFragment extends CustomActionBar implements VideoCallContract.View, View.OnClickListener,
        PopupMenu.OnMenuItemClickListener {

    private final String TAG = VideoCallFragment.class.getName();

    // presenter instance to implement app logic
    private VideoCallContract.Presenter mPresenter;

    private LinearLayout linearLayout, ll_video_res_input, ll_video_res_sent, ll_video_res_receive, ll_video_res_info;
    private FloatingActionButton btnDisconnect, btnAudioMute, btnVideoMute, btnCameraToggle, btnSpeaker;
    private TextView tvRoomName, tvInput, tvResInput, tvSent, tvResSent, tvRecv, tvResRecv;
    private SeekBar seekBarResDim, seekBarResFps;
    private TextView tvResDim, tvResFps;
    private SeekBar.OnSeekBarChangeListener seekBarChangeListenerResDim, seekBarChangeListenerResFps;
    private Button btnLocalOption;

    public static VideoCallFragment newInstance() {
        return new VideoCallFragment();
    }

    @Override
    public void setPresenter(VideoCallContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    //----------------------------------------------------------------------------------------------
    // Fragment life cycle methods
    //----------------------------------------------------------------------------------------------

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        super.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Allow volume to be controlled using volume keys
        ((VideoCallActivity) context).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[SA][Video][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_video_call, container, false);

        // get the UI controls from layout
        getControlWidgets(rootView);

        // setup the action bar
        setActionBar();

        // init the UI controls
        initComponents(rootView);

        //request an initiative connection
        requestViewLayout();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.onViewRequestResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPresenter.onViewRequestPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        mPresenter.onViewRequestPermissionsResult(requestCode, permissions, grantResults, TAG);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (!((VideoCallActivity) context).isChangingConfigurations()) {
            mPresenter.onViewRequestExit();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Changing video custom buttons position when screen orientation changed
        if (newConfig.orientation == ORIENTATION_PORTRAIT) {
            changeFloatingButtons(false);
        } else {
            changeFloatingButtons(true);
        }
    }

    @Override
    public void onClick(View view) {
        //Defining a click event listener for the buttons in the layout
        switch (view.getId()) {
            case R.id.btnBack:
                processBack();
                break;
            case R.id.btnLocalPeer:
                displayPeerInfo(0);
                break;
            case R.id.btnRemotePeer1:
                displayPeerInfo(1);
                break;
            case R.id.btnRemotePeer2:
                displayPeerInfo(2);
                break;
            case R.id.btnRemotePeer3:
                displayPeerInfo(3);
                break;
            case R.id.toggle_speaker:
                mPresenter.onViewRequestChangeAudioOutput();
                break;
            case R.id.toggle_audio:
                mPresenter.onViewRequestChangeAudioState();
                break;
            case R.id.toggle_video:
                mPresenter.onViewRequestChangeVideoState();
                break;
            case R.id.toggle_camera:
                mPresenter.onViewRequestChangeCameraState();
                break;
            case R.id.disconnect:
                mPresenter.onViewRequestDisconnectFromRoom();
                onPresenterRequestDisconnectUIChange();
                break;
            case R.id.btnLocalPeerOption:
                onMenuOptionLocalPeer(btnLocalOption);
                break;
        }
    }

    /**
     * define the action for each menu items for local peer
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.switch_camera:
                mPresenter.onViewRequestSwitchCamera();
                break;
            case R.id.video_resolution:
                mPresenter.onViewRequestGetVideoResolutions();
                break;
            default:
                Log.e(TAG, "Unknown menu option: " + menuItem.getItemId() + "!");
                return false;
        }
        return true;
    }

    //----------------------------------------------------------------------------------------------
    // Methods called from the Presenter to update UI
    //----------------------------------------------------------------------------------------------

    /**
     * Change certain UI elements when trying to connect to room, but not connected
     */
    @Override
    public void onPresenterRequestConnectingUIChange() {
        btnAudioMute.setVisibility(GONE);
        btnVideoMute.setVisibility(GONE);
        btnCameraToggle.setVisibility(GONE);
        btnDisconnect.setVisibility(VISIBLE);
        setUiResControlsVisibility(VISIBLE);
    }

    /**
     * Change certain UI elements once connected to room
     */
    @Override
    public void onPresenterRequestConnectedUIChange() {
        btnAudioMute.setVisibility(VISIBLE);
        btnVideoMute.setVisibility(VISIBLE);
        btnCameraToggle.setVisibility(VISIBLE);
        btnDisconnect.setVisibility(VISIBLE);
        setUiResControlsVisibility(VISIBLE);
    }

    /**
     * Change certain UI elements when disconnecting from room.
     */
    @Override
    public void onPresenterRequestDisconnectUIChange() {
        View self = linearLayout.findViewWithTag("self");
        if (self != null) {
            linearLayout.removeView(self);
        }

        View peer = linearLayout.findViewWithTag("peer");
        if (peer != null) {
            linearLayout.removeView(peer);
        }

        btnAudioMute.setVisibility(GONE);
        btnVideoMute.setVisibility(GONE);
        btnCameraToggle.setVisibility(GONE);
        btnDisconnect.setVisibility(GONE);

        setUiResControlsVisibility(GONE);

        btnSpeaker.setVisibility(GONE);
        btnAudioMute.setVisibility(GONE);
        btnVideoMute.setVisibility(GONE);
        btnCameraToggle.setVisibility(GONE);
        btnDisconnect.setVisibility(GONE);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    /**
     * Update the value of TextView for local input video resolution
     */
    @Override
    public void onPresenterRequestUpdateUiResInput(VideoResolution videoInput) {
        setUiResTvStats(videoInput, tvResInput);
    }

    /**
     * Update the value of TextView for local sent video resolution
     */
    @Override
    public void onPresenterRequestUpdateUiResSent(VideoResolution videoSent) {
        setUiResTvStats(videoSent, tvResSent);
    }

    /**
     * Update the value of TextView for received remote video resolution
     */
    @Override
    public void onPresenterRequestUpdateUiResReceive(VideoResolution videoReceive) {
        setUiResTvStats(videoReceive, tvResRecv);
    }

    /**
     * Update the value of TextView when changing video resolution width x height Seek bar
     */
    @Override
    public boolean onPresenterRequestUpdateUiResDimInfo(int width, int height) {
        return setUiResTvDim(width, height);
    }

    /**
     * Update the value of TextView when changing video resolution frame rate Seek bar
     */
    @Override
    public void onPresenterRequestUpdateUiResFpsInfo(int fps) {
        setUiResTvFps(fps);
    }

    /**
     * Add or update our self VideoView into the view layout.
     *
     * @param videoView local video view
     */
    @Override
    public void onPresenterRequestAddSelfView(SurfaceViewRenderer videoView) {
        if (videoView != null) {
            // If previous self video exists,
            // Set new video to size of previous self video
            // And remove old self video.
            View self = linearLayout.findViewWithTag("self");
            if (self != null) {
                // Remove the old self video.
                linearLayout.removeView(self);
            }

            // Tag new video as self and add onClickListener.
            videoView.setTag("self");

            // If peer video exists, remove it first.
            View peer = linearLayout.findViewWithTag("peer");
            if (peer != null) {
                linearLayout.removeView(peer);
            }

            // Show new video on screen
            // Remove video from previous parent, if any.
            Utils.removeViewFromParent(videoView);

            // And new self video.
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            params.gravity = Gravity.CENTER;
            params.weight = 1;
            videoView.setLayoutParams(params);

            //alway set self video as vertical orientation
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            linearLayout.addView(videoView);

            // Return the peer video, if it was there before.
            if (peer != null) {
                linearLayout.addView(peer);
            }
        } else {
            String log = "[SA][addSelfView] Not adding self view as videoView is null!";
            Log.d(TAG, log);
        }
    }

    /**
     * Add or update remote Peer's VideoView into the view layout.
     *
     * @param remoteVideoView
     */
    @Override
    public void onPresenterRequestAddRemoteView(SurfaceViewRenderer remoteVideoView) {

        if (remoteVideoView == null)
            return;

        // Remove previous peer video if it exists
        View viewToRemove = linearLayout.findViewWithTag("peer");
        if (viewToRemove != null) {
            linearLayout.removeView(viewToRemove);
        }

        // Add new peer video
        remoteVideoView.setTag("peer");
        // Remove view from previous parent, if any.
        Utils.removeViewFromParent(remoteVideoView);
        // Add view to parent
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        params.gravity = Gravity.CENTER;
        params.weight = 1;
        remoteVideoView.setLayoutParams(params);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            linearLayout.setOrientation(LinearLayout.VERTICAL);
        } else {
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        }

        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.addView(remoteVideoView);


    }

    /**
     * Remove remote video view
     * <p>
     * Change layout orientation to Vertical when there is only 1 local video view
     */
    @Override
    public void onPresenterRequestRemoveRemotePeer() {
        View peerView = linearLayout.findViewWithTag("peer");
        linearLayout.removeView(peerView);

        //change orientation to vertical
        linearLayout.setOrientation(LinearLayout.VERTICAL);
    }

    /**
     * Get the instance of the view for implementing runtime permission
     */
    @Override
    public Fragment onPresenterRequestGetFragmentInstance() {
        return this;
    }

    /**
     * Update UI (display toast info) about remote audio state
     *
     * @param isAudioMuted remote audio state
     * @param isToast      display toast or not
     */
    @Override
    public void onPresenterRequestUpdateAudioState(boolean isAudioMuted, boolean isToast) {
        if (isAudioMuted) {
            if (isToast) {
                String log = getString(R.string.muted_audio);
                toastLog(TAG, context, log);
            }
        } else {
            if (isToast) {
                String log = getString(R.string.enabled_audio);
                toastLog(TAG, context, log);
            }
        }
    }

    /**
     * Update UI (display toast info) about remote video state
     *
     * @param isVideoMuted remote video state
     * @param isToast      display toast or not
     */
    @Override
    public void onPresenterRequestUpdateVideoState(boolean isVideoMuted, boolean isToast) {
        if (isVideoMuted) {
            if (isToast) {
                String log = getString(R.string.muted_video);
                toastLog(TAG, context, log);
            }
        } else {
            if (isToast) {
                String log = getString(R.string.enabled_video);
                toastLog(TAG, context, log);
            }
        }
    }

    /**
     * Update the max range of the width x height resolution seek bar
     *
     * @param maxDimRange
     */
    @Override
    public void onPresenterRequestUpdateUiResRangeDimInfo(int maxDimRange) {
        seekBarResDim.setMax(maxDimRange);
    }

    /**
     * Update the max range of the frame rate resolution seek bar
     *
     * @param maxFpsRange
     */
    @Override
    public void onPresenterRequestUpdateUiResRangeFpsInfo(int maxFpsRange) {
        seekBarResFps.setMax(maxFpsRange);
    }

    /**
     * Update the UI when changing width x height video resolution.
     * Update on both the seek bar and the text view
     */
    @Override
    public void onPresenterRequestUpdateResDimInfo(int index, int width, int height) {
        // Set the SeekBar
        seekBarResDim.setProgress(index);
        // Set TextView
        setUiResTvDim(width, height);
    }

    /**
     * Update the UI when changing frame rate video resolution.
     * Update on both the seek bar and the text view
     */
    @Override
    public void onPresenterRequestUpdateResFpsInfo(int index, int fps) {
        // Set the SeekBar
        seekBarResFps.setProgress(index);
        // Set TextView
        setUiResTvFps(fps);
    }

    /**
     * Update the audio output button when being requested from presenter
     * For example: when the phone connects to a bluetooth headset, the speaker is automatically off
     */
    @Override
    public void onPresenterRequestChangeAudioOuput(boolean isSpeakerOn) {
        if (isSpeakerOn) {
            btnSpeaker.setImageResource(R.drawable.ic_audio_speaker);
            String log = getString(R.string.enable_speaker);
            toastLog(TAG, context, log);

        } else {
            btnSpeaker.setImageResource(R.drawable.icon_speaker_mute);
            String log = getString(R.string.enable_headset);
            toastLog(TAG, context, log);
        }
    }

    /**
     * Update the audio button UI when changing audio state
     */
    @Override
    public void onPresenterRequestChangeAudioUI(boolean isAudioMute) {
        if (isAudioMute) {
            btnAudioMute.setImageResource(R.drawable.icon_audio_mute);

        } else {
            btnAudioMute.setImageResource(R.drawable.icon_audio_active);
        }
    }

    /**
     * Update the video button UI when changing video state
     */
    @Override
    public void onPresenterRequestChangeVideoUI(boolean isVideoMute) {
        if (isVideoMute) {
            btnVideoMute.setImageResource(R.drawable.icon_video_mute);

        } else {
            btnVideoMute.setImageResource(R.drawable.icon_video_active);
        }
    }

    /**
     * Update the camera button UI when changing camera state
     */
    @Override
    public void onPresenterRequestChangeCameraUI(boolean isCameraMute) {
        if (isCameraMute) {
            btnCameraToggle.setImageResource(R.drawable.icon_camera_mute);

        } else {
            btnCameraToggle.setImageResource(R.drawable.icon_camera_active);
        }
    }

    /**
     * Update the view layout when changing screen orientation.
     * <p>
     * Change the custom floating buttons position
     */
    @Override
    public void onPresenterRequestchangeViewLayout() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            changeFloatingButtons(false);
        } else {
            changeFloatingButtons(true);
        }
    }

    /**
     * Show local peer button and display local avatar by the first character of the local username
     */
    @Override
    public void onPresenterRequestUpdateLocalPeer(String localUserName) {
        updateUILocalPeer(localUserName);
    }

    /**
     * Update information about new remote peer joining the room at a specific index
     *
     * @param newPeer remote peer joining the room
     * @param index   specific index
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer, int index) {
        updateUiRemotePeerJoin(newPeer, index);
    }

    /**
     * Update information about remote peer left the room
     *
     * @param peersList the list of left peer(s) in the room
     */
    @Override
    public void onPresenterRequestRemotePeerLeft(List<SkylinkPeer> peersList) {
        processFillPeers(peersList);
    }

    /**
     * Update information about room id on the action bar
     *
     * @param roomId
     */
    public void onPresenterRequestUpdateRoomInfo(String roomId) {
        updateRoomInfo(roomId);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        linearLayout = rootView.findViewById(R.id.ll_video_call);
        ll_video_res_input = rootView.findViewById(R.id.ll_video_res_input);
        ll_video_res_sent = rootView.findViewById(R.id.ll_video_res_sent);
        ll_video_res_receive = rootView.findViewById(R.id.ll_video_res_receive);
        ll_video_res_info = rootView.findViewById(R.id.ll_video_res_info);
        tvRoomName = rootView.findViewById(R.id.tv_room_name);
        btnSpeaker = rootView.findViewById(R.id.toggle_speaker);
        btnAudioMute = rootView.findViewById(R.id.toggle_audio);
        btnVideoMute = rootView.findViewById(R.id.toggle_video);
        btnCameraToggle = rootView.findViewById(R.id.toggle_camera);
        btnDisconnect = rootView.findViewById(R.id.disconnect);
        tvInput = rootView.findViewById(R.id.textViewInput);
        tvResInput = rootView.findViewById(R.id.textViewResInput);
        tvSent = rootView.findViewById(R.id.textViewSent);
        tvResSent = rootView.findViewById(R.id.textViewResSent);
        tvRecv = rootView.findViewById(R.id.textViewRecv);
        tvResRecv = rootView.findViewById(R.id.textViewResRecv);
        tvResDim = rootView.findViewById(R.id.textViewDim);
        tvResFps = rootView.findViewById(R.id.textViewFps);
        btnLocalOption = rootView.findViewById(R.id.btnLocalPeerOption);
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private void setActionBar() {
        ActionBar actionBar = ((VideoCallActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.custom_action_bar);
        View customBar = actionBar.getCustomView();

        // get the view controls in custom action bar by id
        btnBack = customBar.findViewById(R.id.btnBack);
        txtRoomName = customBar.findViewById(R.id.txtRoomName);
        txtRoomId = customBar.findViewById(R.id.txtRoomId);
        btnLocalPeer = customBar.findViewById(R.id.btnLocalPeer);
        btnRemotePeer1 = customBar.findViewById(R.id.btnRemotePeer1);
        btnRemotePeer2 = customBar.findViewById(R.id.btnRemotePeer2);
        btnRemotePeer3 = customBar.findViewById(R.id.btnRemotePeer3);
    }

    private void initComponents(View rootView) {
        // set onClick event for buttons in layout
        btnBack.setOnClickListener(this);
        btnLocalPeer.setOnClickListener(this);
        btnRemotePeer1.setOnClickListener(this);
        btnRemotePeer2.setOnClickListener(this);
        btnRemotePeer3.setOnClickListener(this);
        btnLocalOption.setOnClickListener(this);
        btnSpeaker.setOnClickListener(this);
        btnAudioMute.setOnClickListener(this);
        btnVideoMute.setOnClickListener(this);
        btnCameraToggle.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);

        // init setting value for room name in action bar
        txtRoomName.setText(Config.ROOM_NAME_VIDEO);

        // Video resolution UI.
        setUiResControls(rootView);

        // Set UI elements
        setAudioBtnLabel(false, false);
        setVideoBtnLabel(false, false);

        tvRoomName.setText(Config.ROOM_NAME_VIDEO);
        //can not edit room name
        tvRoomName.setEnabled(false);

        // make floating buttons
        // changing button positions base on the screen orientation
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            changeFloatingButtons(false);
        } else {
            changeFloatingButtons(true);
        }
    }

    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListenerDim() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPresenter.onViewRequestDimProgressChanged(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mPresenter.onViewRequestDimSelected(seekBar.getProgress());
            }
        };
    }

    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListenerFps() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPresenter.onViewRequestFpsProgressChanged(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mPresenter.onViewRequestFpsSelected(seekBar.getProgress());

                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                } else {
                    linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                }
            }
        };
    }

    /**
     * Set UI Controls for Resolution related.
     */
    private void setUiResControls(View rootView) {
        seekBarResDim = (SeekBar) rootView.findViewById(R.id.seekBarDim);
        seekBarChangeListenerResDim = getSeekBarChangeListenerDim();
        seekBarResDim.setOnSeekBarChangeListener(seekBarChangeListenerResDim);
        seekBarResFps = (SeekBar) rootView.findViewById(R.id.seekBarFps);
        seekBarChangeListenerResFps = getSeekBarChangeListenerFps();
        seekBarResFps.setOnSeekBarChangeListener(seekBarChangeListenerResFps);

        // Set empty values.
        tvInput.setText("Input:");
        tvSent.setText("Sent:");
        tvRecv.setText("Recv:");
        tvResDim.setFocusable(true);
        tvResDim.setEnabled(true);
        tvResDim.setClickable(true);
        tvResDim.setFocusableInTouchMode(true);
        tvResFps.setFocusable(true);
        tvResFps.setEnabled(true);
        tvResFps.setClickable(true);
        tvResFps.setFocusableInTouchMode(true);

        setUiResTvStats(null, tvResInput);
        setUiResTvStats(null, tvResSent);
        setUiResTvStats(null, tvResRecv);

        tvInput.setFreezesText(true);
        tvResInput.setFreezesText(true);
        tvSent.setFreezesText(true);
        tvResSent.setFreezesText(true);
        tvRecv.setFreezesText(true);
        tvResRecv.setFreezesText(true);
        tvResDim.setFreezesText(true);
        tvResFps.setFreezesText(true);
    }

    private void setUiResControlsVisibility(int visibility) {
        tvInput.setVisibility(visibility);
        tvSent.setVisibility(visibility);
        tvRecv.setVisibility(visibility);

        if (tvResInput != null)
            tvResInput.setVisibility(visibility);

        if (tvResSent != null)
            tvResSent.setVisibility(visibility);

        if (tvResRecv != null)
            tvResRecv.setVisibility(visibility);

        if (tvResDim != null)
            tvResDim.setVisibility(visibility);

        if (tvResFps != null)
            tvResFps.setVisibility(visibility);

        if (seekBarResDim != null)
            seekBarResDim.setVisibility(visibility);

        if (seekBarResFps != null)
            seekBarResFps.setVisibility(visibility);
    }

    /**
     * request info to display from presenter
     * try to connect to room if not connected
     * try to update UI if connnected
     */
    private void requestViewLayout() {
        if (mPresenter != null) {
            mPresenter.onViewRequestConnectedLayout();
        }
    }

    /**
     * Set the value of TextView for local input resolution or sent resolution.
     * If any parameters are invalid, set default text.
     *
     * @param textView
     * @param videoResolution
     */
    private void setUiResTvStats(VideoResolution videoResolution, TextView textView) {
        if (textView == null)
            return;

        if (videoResolution == null || videoResolution.getWidth() <= 0 || videoResolution.getHeight() <= 0 || videoResolution.getFps() < 0) {
            textView.setText("N/A");
            return;
        }
        // Set textView to match
        String str = Utils.getResDimStr(videoResolution) + ",\n" + Utils.getResFpsStr(videoResolution);
        textView.setText(str);
    }

    /**
     * Set the value of TextView tvResDim.
     * If inputs are invalid, set default text.
     *
     * @param width
     * @param height
     * @return True if inputs are valid, false otherwise.
     */
    private boolean setUiResTvDim(int width, int height) {
        if (width <= 0 || height <= 0) {
            tvResDim.setText("N/A");
            return false;
        }
        // Set textView to match
        tvResDim.setText(Utils.getResDimStr(width, height));
        return true;
    }

    /**
     * Set the value of TextView tvResFps in frames per second.
     * If input is invalid, set default text.
     *
     * @param fps Framerate in frames per second.
     */
    private void setUiResTvFps(int fps) {
        if (fps < 0) {
            tvResFps.setText("N/A");
            return;
        }
        // Set textView to match
        tvResFps.setText(Utils.getResFpsStr(fps));
    }

    /**
     * Set the mute audio button label according to the current state of audio.
     *
     * @param doToast If true, Toast about setting audio to current state.
     */
    private void setAudioBtnLabel(boolean isAudioMuted, boolean doToast) {
        if (isAudioMuted) {
            if (doToast) {
                String log = getString(R.string.muted_audio);
                toastLog(TAG, context, log);
            }
        } else {
            if (doToast) {
                String log = getString(R.string.enabled_audio);
                toastLog(TAG, context, log);
            }
        }
    }

    /**
     * Set the mute video button label according to the current state of video.
     *
     * @param doToast If true, Toast about setting video to current state.
     */
    private void setVideoBtnLabel(boolean isVideoMuted, boolean doToast) {
        if (isVideoMuted) {
            if (doToast) {
                String log = getString(R.string.muted_video);
                toastLog(TAG, context, log);
            }
        } else {
            if (doToast) {
                String log = getString(R.string.enabled_video);
                toastLog(TAG, context, log);
            }
        }
    }

    /*
     * Changing view layout base on the screen orientation
     * */
    private void changeFloatingButtons(boolean isLandscapeMode) {
        if (!isLandscapeMode) {
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            ll_video_res_input.setOrientation(LinearLayout.VERTICAL);
            ll_video_res_sent.setOrientation(LinearLayout.VERTICAL);
            ll_video_res_receive.setOrientation(LinearLayout.VERTICAL);

            int llHeight = (int) context.getResources().getDimension(R.dimen.dp_48dp);

            RelativeLayout.LayoutParams llParams = (RelativeLayout.LayoutParams) ll_video_res_info.getLayoutParams();
            llParams.height = llHeight;
            ll_video_res_info.setLayoutParams(llParams);

            changeFloatingButtonPortrait(btnDisconnect);
            changeFloatingButtonPortrait(btnCameraToggle);
            changeFloatingButtonPortrait(btnVideoMute);
            changeFloatingButtonPortrait(btnAudioMute);
            changeFloatingButtonPortrait(btnSpeaker);

        } else {
            //only change orientation if there is peer in room
            View viewToRemove = linearLayout.findViewWithTag("peer");
            if (viewToRemove != null) {
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            } else {
                linearLayout.setOrientation(LinearLayout.VERTICAL);
            }

            ll_video_res_input.setOrientation(LinearLayout.HORIZONTAL);
            ll_video_res_sent.setOrientation(LinearLayout.HORIZONTAL);
            ll_video_res_receive.setOrientation(LinearLayout.HORIZONTAL);

            int llHeight = (int) context.getResources().getDimension(R.dimen.dp_35dp);

            RelativeLayout.LayoutParams llParams = (RelativeLayout.LayoutParams) ll_video_res_info.getLayoutParams();
            llParams.height = llHeight;
            ll_video_res_info.setLayoutParams(llParams);

            changeFloatingButtonLandscape(btnDisconnect);
            changeFloatingButtonLandscape(btnCameraToggle);
            changeFloatingButtonLandscape(btnVideoMute);
            changeFloatingButtonLandscape(btnAudioMute);
            changeFloatingButtonLandscape(btnSpeaker);

        }
    }

    /*
     * Changing floating buttons position in Portrait mode
     * All floating buttons will be located in the right of the layout
     * */
    private void changeFloatingButtonPortrait(FloatingActionButton btn) {
        int landWidth = (int) context.getResources().getDimension(R.dimen.dp_60dp);
        int margin = (int) context.getResources().getDimension(R.dimen.dp_10dp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(landWidth, landWidth);

        params.width = landWidth;
        params.height = landWidth;

        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.setMargins(0, 0, margin, margin);

        Drawable backgroundSrc = null;
        if (btn == btnDisconnect) {
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrc = context.getResources().getDrawable(R.drawable.ic_audio_call_end, null);
            }
        } else if (btn == btnCameraToggle) {
            params.addRule(RelativeLayout.ABOVE, R.id.disconnect);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrc = context.getResources().getDrawable(R.drawable.icon_camera_active, null);
            }
        } else if (btn == btnVideoMute) {
            params.addRule(RelativeLayout.ABOVE, R.id.toggle_camera);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrc = context.getResources().getDrawable(R.drawable.icon_video_active, null);
            }
        } else if (btn == btnAudioMute) {
            params.addRule(RelativeLayout.ABOVE, R.id.toggle_video);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrc = context.getResources().getDrawable(R.drawable.icon_audio_active, null);
            }
        } else if (btn == btnSpeaker) {
            params.addRule(RelativeLayout.ABOVE, R.id.toggle_audio);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrc = context.getResources().getDrawable(R.drawable.ic_audio_speaker, null);
            }
        }

        btn.setLayoutParams(params);

        if (backgroundSrc != null)
            btn.setBackgroundDrawable(backgroundSrc);
    }

    /*
     * Changing floating buttons position in Landscape mode
     * All floating buttons will be located in the left of the layout / on the local video side
     * */
    private void changeFloatingButtonLandscape(FloatingActionButton btn) {
        int landWidth = (int) context.getResources().getDimension(R.dimen.dp_45dp);
        int margin = (int) context.getResources().getDimension(R.dimen.dp_10dp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(landWidth, landWidth);

        params.width = landWidth;
        params.height = landWidth;

        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.setMargins(margin, 0, 0, margin);

        Drawable backgroundSrc = null;
        if (btn == btnDisconnect) {
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrc = context.getResources().getDrawable(R.drawable.ic_audio_call_end, null);
            }
        } else if (btn == btnCameraToggle) {
            params.addRule(RelativeLayout.ABOVE, R.id.disconnect);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrc = context.getResources().getDrawable(R.drawable.icon_camera_active, null);
            }
        } else if (btn == btnVideoMute) {
            params.addRule(RelativeLayout.ABOVE, R.id.toggle_camera);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrc = context.getResources().getDrawable(R.drawable.icon_video_active, null);
            }
        } else if (btn == btnAudioMute) {
            params.addRule(RelativeLayout.ABOVE, R.id.toggle_video);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrc = context.getResources().getDrawable(R.drawable.icon_audio_active, null);
            }
        } else if (btn == btnSpeaker) {
            params.addRule(RelativeLayout.ABOVE, R.id.toggle_audio);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrc = context.getResources().getDrawable(R.drawable.ic_audio_speaker, null);
            }
        }

        btn.setLayoutParams(params);

        if (backgroundSrc != null)
            btn.setBackgroundDrawable(backgroundSrc);
    }

    /**
     * Display the dialog of peer info including peer username and peer id
     * when the user click into the peer button in action bar
     */
    private void displayPeerInfo(int index) {
        SkylinkPeer peer = mPresenter.onViewRequestGetPeerByIndex(index);
        if (index == 0) {
            processDisplayLocalPeer(peer);
        } else {
            processDisplayRemotePeer(peer);
        }
    }

    /**
     * Display local peer menu option
     */
    private void onMenuOptionLocalPeer(View view) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.local_option_menu_video);
        popup.show();
    }
}
