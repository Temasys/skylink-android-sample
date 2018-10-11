package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.app.AlertDialog;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * A simple {@link Fragment} subclass.
 */
public class VideoCallFragment extends Fragment implements VideoCallContract.View {

    private final String TAG = VideoCallFragment.class.getName();

    private VideoCallContract.Presenter mPresenter;

    private Context mContext;

    private LinearLayout linearLayout, ll_video_res_input, ll_video_res_sent, ll_video_res_receive, ll_video_res_info;
    private FloatingActionButton btnDisconnect;
    private FloatingActionButton btnAudioMute;
    private FloatingActionButton btnVideoMute;
    private FloatingActionButton btnCameraToggle;
    private FloatingActionButton btnSpeaker;
    private TextView tvRoomName;
    private TextView tvInput;
    private TextView tvResInput;
    private TextView tvSent;
    private TextView tvResSent;
    private TextView tvRecv;
    private TextView tvResRecv;
    private SeekBar seekBarResDim;
    private SeekBar seekBarResFps;
    private TextView tvResDim;
    private TextView tvResFps;
    private SeekBar.OnSeekBarChangeListener seekBarChangeListenerResDim;
    private SeekBar.OnSeekBarChangeListener seekBarChangeListenerResFps;

    public static VideoCallFragment newInstance() {
        return new VideoCallFragment();
    }

    @Override
    public void setPresenter(VideoCallContract.Presenter presenter) {
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
        // Allow volume to be controlled using volume keys
        ((VideoCallActivity) mContext).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[SA][Video][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_video_call, container, false);

        getControlWidgets(rootView);

        setActionBar();

        initComponents(rootView);

        requestViewLayout();

        btnSpeaker.setOnClickListener(v -> {
            mPresenter.onViewRequestChangeAudioOutput();
        });

        btnAudioMute.setOnClickListener(v -> {
            mPresenter.onViewRequestChangeAudioState();
        });

        btnVideoMute.setOnClickListener(v -> {
            mPresenter.onViewRequestChangeVideoState();
        });

        btnCameraToggle.setOnClickListener(v -> {
            mPresenter.onViewRequestChangeCameraState();
        });

        btnDisconnect.setOnClickListener(v -> {

            mPresenter.onViewRequestDisconnectFromRoom();

            toastLog(TAG, mContext, "Clicked Disconnect!");

            onPresenterRequestDisconnectUIChange();
        });

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
        if (!((VideoCallActivity) mContext).isChangingConfigurations()) {
            mPresenter.onViewRequestExit();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == ORIENTATION_PORTRAIT) {
            changeFloatingButtons(false);
        } else {
            changeFloatingButtons(true);
        }
    }

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
     * Change certain UI elements once connected to room or when Peer(s) join or leave.
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

    @Override
    public void onPresenterRequestUpdateUiResInput(VideoResolution videoInput) {
        setUiResTvStats(videoInput, tvResInput);
    }

    @Override
    public void onPresenterRequestUpdateUiResSent(VideoResolution videoSent) {
        setUiResTvStats(videoSent, tvResSent);
    }

    @Override
    public void onPresenterRequestUpdateUiResReceive(VideoResolution videoReceive) {
        setUiResTvStats(videoReceive, tvResRecv);
    }

    @Override
    public boolean onPresenterRequestUpdateUiResDimInfo(int width, int height) {
        return setUiResTvDim(width, height);
    }

    @Override
    public void onPresenterRequestUpdateUiResFpsInfo(int fps) {
        setUiResTvFps(fps);
    }

    /**
     * Add or update our self VideoView into the app.
     *
     * @param videoView
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
            // Show room and self info, plus give option to
            // switch self view between different cameras (if any).
            videoView.setOnClickListener(v -> {
                String name = mPresenter.onViewRequestGetRoomPeerIdNick();

                name += "\r\nClick outside dialog to return.";
                TextView selfTV = new TextView(mContext);
                selfTV.setText(name);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    selfTV.setTextIsSelectable(true);
                }
                AlertDialog.Builder selfDialogBuilder =
                        new AlertDialog.Builder(mContext);
                selfDialogBuilder.setView(selfTV);
                // Get the available video resolutions.
                selfDialogBuilder.setPositiveButton("Video resolutions",
                        (dialog, which) -> mPresenter.onViewRequestGetVideoResolutions());
                // Switch camera if possible.
                selfDialogBuilder.setNegativeButton("Switch Camera",
                        (dialog, which) -> {
                            mPresenter.onViewRequestSwitchCamera();
                        });
                selfDialogBuilder.show();
            });

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
     * Add or update remote Peer's VideoView into the app.
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

    @Override
    public void onPresenterRequestRemoveRemotePeer() {
        View peerView = linearLayout.findViewWithTag("peer");
        linearLayout.removeView(peerView);

        //change orientation to vertical
        linearLayout.setOrientation(LinearLayout.VERTICAL);
    }

    @Override
    public Fragment onPresenterRequestGetFragmentInstance() {
        return this;
    }

    @Override
    public void onPresenterRequestUpdateAudioState(boolean isAudioMuted, boolean isToast) {
        if (isAudioMuted) {
            if (isToast) {
                String log = getString(R.string.muted_audio);
                toastLog(TAG, mContext, log);
            }
        } else {
            if (isToast) {
                String log = getString(R.string.enabled_audio);
                toastLog(TAG, mContext, log);
            }
        }
    }

    @Override
    public void onPresenterRequestUpdateVideoState(boolean isVideoMuted, boolean isToast) {
        if (isVideoMuted) {
            if (isToast) {
                String log = getString(R.string.muted_video);
                toastLog(TAG, mContext, log);
            }
        } else {
            if (isToast) {
                String log = getString(R.string.enabled_video);
                toastLog(TAG, mContext, log);
            }
        }
    }

    @Override
    public void onPresenterRequestUpdateUiResRangeDimInfo(int maxDimRange) {
        seekBarResDim.setMax(maxDimRange);
    }

    @Override
    public void onPresenterRequestUpdateUiResRangeFpsInfo(int maxFpsRange) {
        seekBarResFps.setMax(maxFpsRange);
    }

    @Override
    public void onPresenterRequestUpdateResDimInfo(int index, int width, int height) {
        // Set the SeekBar
        seekBarResDim.setProgress(index);
        // Set TextView
        setUiResTvDim(width, height);
    }

    @Override
    public void onPresenterRequestUpdateResFpsInfo(int index, int fps) {
        // Set the SeekBar
        seekBarResFps.setProgress(index);
        // Set TextView
        setUiResTvFps(fps);
    }

    @Override
    public void onPresenterRequestChangeSpeakerOuput(boolean isSpeakerOn) {
        if (isSpeakerOn) {
            btnSpeaker.setImageResource(R.drawable.ic_audio_speaker);
            String log = getString(R.string.enable_speaker);
            toastLog(TAG, mContext, log);

        } else {
            btnSpeaker.setImageResource(R.drawable.icon_speaker_mute);
            String log = getString(R.string.enable_headset);
            toastLog(TAG, mContext, log);
        }
    }

    @Override
    public void onPresenterRequestChangeAudioUI(boolean isAudioMute) {
        if (isAudioMute) {
            btnAudioMute.setImageResource(R.drawable.icon_audio_mute);

        } else {
            btnAudioMute.setImageResource(R.drawable.icon_audio_active);
        }
    }

    @Override
    public void onPresenterRequestChangeVideoUI(boolean isVideoMute) {
        if (isVideoMute) {
            btnVideoMute.setImageResource(R.drawable.icon_video_mute);

        } else {
            btnVideoMute.setImageResource(R.drawable.icon_video_active);
        }
    }

    @Override
    public void onPresenterRequestChangeCameraUI(boolean isCameraMute) {
        if (isCameraMute) {
            btnCameraToggle.setImageResource(R.drawable.icon_camera_mute);

        } else {
            btnCameraToggle.setImageResource(R.drawable.icon_camera_active);
        }
    }

    @Override
    public void onPresenterRequestchangeViewLayout() {
        //make floating buttons
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            changeFloatingButtons(false);
        } else {
            changeFloatingButtons(true);
        }
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
    }

    private void setActionBar() {
        ActionBar actionBar = ((VideoCallActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setTitle(Config.ROOM_NAME_VIDEO);
        setHasOptionsMenu(true);
    }

    private void initComponents(View rootView) {
        // Video resolution UI.
        setUiResControls(rootView);

        // Set UI elements
        setAudioBtnLabel(false, false);
        setVideoBtnLabel(false, false);

        tvRoomName.setText(Config.ROOM_NAME_VIDEO);
        //can not edit room name
        tvRoomName.setEnabled(false);

        //make floating buttons
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
            mPresenter.onViewRequestLayout();
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
                toastLog(TAG, mContext, log);
            }
        } else {
            if (doToast) {
                String log = getString(R.string.enabled_audio);
                toastLog(TAG, mContext, log);
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
                toastLog(TAG, mContext, log);
            }
        } else {
            if (doToast) {
                String log = getString(R.string.enabled_video);
                toastLog(TAG, mContext, log);
            }
        }
    }

    private void changeFloatingButtons(boolean isLandscapeMode) {
        if (!isLandscapeMode) {
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            ll_video_res_input.setOrientation(LinearLayout.VERTICAL);
            ll_video_res_sent.setOrientation(LinearLayout.VERTICAL);
            ll_video_res_receive.setOrientation(LinearLayout.VERTICAL);

            int llHeight = (int) mContext.getResources().getDimension(R.dimen.ll_video_res_height);

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

            int llHeight = (int) mContext.getResources().getDimension(R.dimen.ll_video_res_height_land);

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

    private void changeFloatingButtonPortrait(FloatingActionButton btn) {
        int landWidth = (int) mContext.getResources().getDimension(R.dimen.floating_btn_size);
        int margin = (int) mContext.getResources().getDimension(R.dimen.dp_10dp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(landWidth, landWidth);

        params.width = landWidth;
        params.height = landWidth;

        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.setMargins(0, 0, margin, margin);

        Drawable backgroundSrc = null;
        if (btn == btnDisconnect) {
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            backgroundSrc = mContext.getResources().getDrawable(R.drawable.ic_audio_call_end, null);
        } else if (btn == btnCameraToggle) {
            params.addRule(RelativeLayout.ABOVE, R.id.disconnect);
            backgroundSrc = mContext.getResources().getDrawable(R.drawable.icon_camera_active, null);
        } else if (btn == btnVideoMute) {
            params.addRule(RelativeLayout.ABOVE, R.id.toggle_camera);
            backgroundSrc = mContext.getResources().getDrawable(R.drawable.icon_video_active, null);
        } else if (btn == btnAudioMute) {
            params.addRule(RelativeLayout.ABOVE, R.id.toggle_video);
            backgroundSrc = mContext.getResources().getDrawable(R.drawable.icon_audio_active, null);
        } else if (btn == btnSpeaker) {
            params.addRule(RelativeLayout.ABOVE, R.id.toggle_audio);
            backgroundSrc = mContext.getResources().getDrawable(R.drawable.ic_audio_speaker, null);
        }

        btn.setLayoutParams(params);

        if (backgroundSrc != null)
            btn.setBackgroundDrawable(backgroundSrc);
    }

    private void changeFloatingButtonLandscape(FloatingActionButton btn) {
        int landWidth = (int) mContext.getResources().getDimension(R.dimen.floating_btn_size_land);
        int margin = (int) mContext.getResources().getDimension(R.dimen.dp_10dp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(landWidth, landWidth);

        params.width = landWidth;
        params.height = landWidth;

        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.setMargins(margin, 0, 0, margin);

        Drawable backgroundSrc = null;
        if (btn == btnDisconnect) {
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            backgroundSrc = mContext.getResources().getDrawable(R.drawable.ic_audio_call_end, null);
        } else if (btn == btnCameraToggle) {
            params.addRule(RelativeLayout.ABOVE, R.id.disconnect);
            backgroundSrc = mContext.getResources().getDrawable(R.drawable.icon_camera_active, null);
        } else if (btn == btnVideoMute) {
            params.addRule(RelativeLayout.ABOVE, R.id.toggle_camera);
            backgroundSrc = mContext.getResources().getDrawable(R.drawable.icon_video_active, null);
        } else if (btn == btnAudioMute) {
            params.addRule(RelativeLayout.ABOVE, R.id.toggle_video);
            backgroundSrc = mContext.getResources().getDrawable(R.drawable.icon_audio_active, null);
        } else if (btn == btnSpeaker) {
            params.addRule(RelativeLayout.ABOVE, R.id.toggle_audio);
            backgroundSrc = mContext.getResources().getDrawable(R.drawable.ic_audio_speaker, null);
        }

        btn.setLayoutParams(params);

        if (backgroundSrc != null)
            btn.setBackgroundDrawable(backgroundSrc);
    }

}
