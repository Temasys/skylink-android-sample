package sg.com.temasys.skylink.sdk.sampleapp.video;

import android.app.AlertDialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.VideoResolution;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * A simple {@link Fragment} subclass.
 */
public class VideoCallFragment extends Fragment implements VideoCallContract.View {

    private final String TAG = VideoCallFragment.class.getName();

    //this variable need to be static for configuration change
    private VideoCallContract.Presenter mPresenter;

    private Context mContext;

    private LinearLayout linearLayout;
    private Button disconnectButton;
    private TextView tvRoomName;
    private Button btnAudioMute;
    private Button btnVideoMute;
    private Button btnCameraToggle;
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

    public VideoCallFragment() {
        // Required empty public constructor
    }

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

        btnAudioMute.setOnClickListener(v -> {
            mPresenter.onProcessBtnAudioMutePresenterHandler();
        });

        btnVideoMute.setOnClickListener(v -> {
            mPresenter.onProcessBtnVideoMutePresenterHandler();
        });

        btnCameraToggle.setOnClickListener(v -> {
            mPresenter.onProcessBtnCameraTogglePresenterHandler();
        });

        disconnectButton.setOnClickListener(v -> {

            mPresenter.onDisconnectFromRoomPresenterHandler();

            toastLog(TAG, mContext, "Clicked Disconnect!");
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mPresenter.onViewResumePresenterHandler();
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onPause() {
        super.onPause();

        // Stop local video source only if not changing orientation
        if (!((VideoCallActivity) mContext).isChangingConfigurations()) {
            mPresenter.onViewPausePresenterHandler();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        mPresenter.onRequestPermissionsResultPresenterHandler(requestCode, permissions, grantResults, TAG);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onDetach() {
        super.onDetach();
        if (!((VideoCallActivity) mContext).isChangingConfigurations()) {
            mPresenter.onViewExitPresenterHandler();
        }
    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        linearLayout = (LinearLayout) rootView.findViewById(R.id.ll_video_call);
        tvRoomName = (TextView) rootView.findViewById(R.id.tv_room_name);

        btnAudioMute = (Button) rootView.findViewById(R.id.toggle_audio);
        btnVideoMute = (Button) rootView.findViewById(R.id.toggle_video);
        btnCameraToggle = (Button) rootView.findViewById(R.id.toggle_camera);
        disconnectButton = (Button) rootView.findViewById(R.id.disconnect);
        tvInput = (TextView) rootView.findViewById(R.id.textViewInput);
        tvResInput = (TextView) rootView.findViewById(R.id.textViewResInput);
        tvSent = (TextView) rootView.findViewById(R.id.textViewSent);
        tvResSent = (TextView) rootView.findViewById(R.id.textViewResSent);
        tvRecv = (TextView) rootView.findViewById(R.id.textViewRecv);
        tvResRecv = (TextView) rootView.findViewById(R.id.textViewResRecv);
        tvResDim = (TextView) rootView.findViewById(R.id.textViewDim);
        tvResFps = (TextView) rootView.findViewById(R.id.textViewFps);
    }

    private void setActionBar() {
        ActionBar actionBar = ((VideoCallActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

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
    }

    @NonNull
    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListenerDim() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                mPresenter.onDimProgressChangedPresenterHandler(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                mPresenter.onDimStopTrackingTouchPresenterHandler(seekBar.getProgress());
            }
        };
    }

    @NonNull
    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListenerFps() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                mPresenter.onFpsProgressChangedPresenterHandler(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mPresenter.onFpsStopTrackingTouchPresenterHandler(seekBar.getProgress());

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
        // Resolution stats UIs.
        tvInput.setVisibility(visibility);
        tvSent.setVisibility(visibility);
        tvRecv.setVisibility(visibility);
        tvResInput.setVisibility(visibility);
        tvResSent.setVisibility(visibility);
        tvResRecv.setVisibility(visibility);

        // Resolution adjustment UIs.
        tvResDim.setVisibility(visibility);
        tvResFps.setVisibility(visibility);
        seekBarResDim.setVisibility(visibility);
        seekBarResFps.setVisibility(visibility);
    }

    /**
     * request info to display from presenter
     * try to connect to room if not connected
     */
    private void requestViewLayout() {
        if (mPresenter != null) {
            mPresenter.onViewLayoutRequestedPresenterHandler();
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
            btnAudioMute.setText(getString(R.string.enable_audio));
            if (doToast) {
                String log = getString(R.string.muted_audio);
                toastLog(TAG, mContext, log);
            }
        } else {
            btnAudioMute.setText(getString(R.string.mute_audio));
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
            btnVideoMute.setText(getString(R.string.enable_video));
            if (doToast) {
                String log = getString(R.string.muted_video);
                toastLog(TAG, mContext, log);
            }
        } else {
            btnVideoMute.setText(getString(R.string.mute_video));
            if (doToast) {
                String log = getString(R.string.enabled_video);
                toastLog(TAG, mContext, log);
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // listener methods to update UI from Presenter
    //----------------------------------------------------------------------------------------------

    /**
     * Change certain UI elements when trying to connect to room, but not connected
     */
    @Override
    public void onConnectingUIChangeViewHandler() {
        btnAudioMute.setVisibility(GONE);
        btnVideoMute.setVisibility(GONE);
        btnCameraToggle.setVisibility(GONE);
        disconnectButton.setVisibility(VISIBLE);
        setUiResControlsVisibility(VISIBLE);
    }

    /**
     * Change certain UI elements once connected to room or when Peer(s) join or leave.
     */
    @Override
    public void onConnectedUIChangeViewHandler() {
        btnAudioMute.setVisibility(VISIBLE);
        btnVideoMute.setVisibility(VISIBLE);
        btnCameraToggle.setVisibility(VISIBLE);
        disconnectButton.setVisibility(VISIBLE);
        setUiResControlsVisibility(VISIBLE);
    }

    /**
     * Change certain UI elements when disconnecting from room.
     */
    @Override
    public void onDisconnectUIChangeViewHandler() {
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
        disconnectButton.setVisibility(GONE);

        setUiResControlsVisibility(GONE);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onSetUiResTvStatsInputViewHandler(VideoResolution videoInput) {
        setUiResTvStats(videoInput, tvResInput);
    }

    @Override
    public void onSetUiResTvStatsSentViewHandler(VideoResolution videoSent) {
        setUiResTvStats(videoSent, tvResSent);
    }

    @Override
    public void onSetUiResTvStatsReceiveViewHandler(VideoResolution videoReceive) {
        setUiResTvStats(videoReceive, tvResRecv);
    }

    @Override
    public boolean onSetUiResTvDimViewHandler(int width, int height) {
        return setUiResTvDim(width, height);
    }

    @Override
    public void onSetUiResTvFpsViewHandler(int fps) {
        setUiResTvFps(fps);
    }

    /**
     * Add or update our self VideoView into the app.
     *
     * @param videoView
     */
    @Override
    public void onAddSelfViewViewHandler(SurfaceViewRenderer videoView) {
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
                String name = mPresenter.onGetRoomPeerIdNickPresenterHandler();

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
                        (dialog, which) -> mPresenter.onGetVideoResolutionsPresenterHandler());
                // Switch camera if possible.
                selfDialogBuilder.setNegativeButton("Switch Camera",
                        (dialog, which) -> {
                            mPresenter.onSwitchCameraPresenterHandler();
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
            params.gravity = Gravity.CENTER_HORIZONTAL;
            videoView.setLayoutParams(params);
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
    public void onAddRemoteViewViewHandler(SurfaceViewRenderer remoteVideoView) {
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
        params.gravity = Gravity.CENTER_HORIZONTAL;
        remoteVideoView.setLayoutParams(params);
        linearLayout.addView(remoteVideoView);
    }

    @Override
    public void onRemoveRemotePeerViewHandler() {
        View peerView = linearLayout.findViewWithTag("peer");
        linearLayout.removeView(peerView);
    }

    @Override
    public Fragment onGetFragmentViewHandler() {
        return this;
    }

    @Override
    public void onSetAudioBtnLabelViewHandler(boolean isAudioMuted, boolean isToast) {
        if (isAudioMuted) {
            btnAudioMute.setText(getString(R.string.enable_audio));
            if (isToast) {
                String log = getString(R.string.muted_audio);
                toastLog(TAG, mContext, log);
            }
        } else {
            btnAudioMute.setText(getString(R.string.mute_audio));
            if (isToast) {
                String log = getString(R.string.enabled_audio);
                toastLog(TAG, mContext, log);
            }
        }
    }

    @Override
    public void onSetVideoBtnLabelViewHandler(boolean isVideoMuted, boolean isToast) {
        if (isVideoMuted) {
            btnVideoMute.setText(getString(R.string.enable_video));
            if (isToast) {
                String log = getString(R.string.muted_video);
                toastLog(TAG, mContext, log);
            }
        } else {
            btnVideoMute.setText(getString(R.string.mute_video));
            if (isToast) {
                String log = getString(R.string.enabled_video);
                toastLog(TAG, mContext, log);
            }
        }
    }

    @Override
    public void onSetUiResSeekBarRangeDimViewHandler(int maxSeekBarDimRange) {
        seekBarResDim.setMax(maxSeekBarDimRange);
    }

    @Override
    public void onSetUiResSeekBarRangeFpsViewHandler(int maxSeekBarFpsRange) {
        seekBarResFps.setMax(maxSeekBarFpsRange);
    }

    @Override
    public void onSetSeekBarResDimViewHandler(int index, int width, int height) {
        // Set the SeekBar
        seekBarResDim.setProgress(index);
        // Set TextView
        setUiResTvDim(width, height);
    }

    @Override
    public void onSetSeekBarResFpsViewHandler(int index, int fps) {
        // Set the SeekBar
        seekBarResFps.setProgress(index);
        // Set TextView
        setUiResTvFps(fps);
    }

}
