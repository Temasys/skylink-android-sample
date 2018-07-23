package sg.com.temasys.skylink.sdk.sampleapp.videocall;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.VideoLocalState;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.VideoResolution;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;


/**
 * A simple {@link Fragment} subclass.
 */
public class VideoCallFragment extends Fragment implements VideoCallContract.View {

    private final String TAG = VideoCallFragment.class.getName();

    // Constants for configuration change
    private final String BUNDLE_VIDEO_STATE = "VIDEO_STATE";

    //this variable need to be static for configuration change
    private static VideoCallContract.Presenter mPresenter;

    private PermissionUtils permissionUtils;

    // Video resolution from camera input.
    private VideoResolution videoInput;

    // Video resolution from camera sent out to Peer.
    private VideoResolution videoSent;

    // Video resolution received from Peer.
    private VideoResolution videoReceive;

    //video call local info include audio mute, video mute, camera toggle
    //this is to store information incase of changing configuration
    private VideoLocalState videoLocalState;

    // The current VideoDevice.
    private SkylinkConfig.VideoDevice currentVideoDevice = null;
    // The current camera name.
    private String currentCameraName = null;
    // The array of SkylinkCaptureFormats support by the current camera.
    private static SkylinkCaptureFormat[] captureFormats;
    // The selected SkylinkCaptureFormat on UI,
    // not necessarily the currently used SkylinkCaptureFormat.
    private static SkylinkCaptureFormat captureFormatSel = null;
    // The last selected frame rate (fps) on UI,
    // not necessarily the currently used frame rate.
    private int fpsSel = -1;

    private String ROOM_NAME;

    private String roomName;
    private Context mContext;

    // UI Controls
    private LinearLayout linearLayout;
    // Room
    private Button disconnectButton;
    private Button btnEnterRoom;
    private EditText etRoomName;
    // Media
    private Button btnAudioMute;
    private Button btnVideoMute;
    private Button btnCameraToggle;
    // Media - Resolution
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String logTag = "[SA][Video][onCreateView] ";
        String log = logTag;

        ROOM_NAME = Config.ROOM_NAME_VIDEO;

        View rootView = inflater.inflate(R.layout.fragment_video_call, container, false);

        getControlWidgets(rootView);

        setActionBar();

        initComponents(rootView);

        // Check if it was an orientation change
        if (savedInstanceState != null) {
            log += "Restarting ";
            // Resume previous permission request, if any.
            permissionUtils.permQResume(mContext, this);

            videoLocalState = (VideoLocalState) savedInstanceState.getSerializable(BUNDLE_VIDEO_STATE);

            //resume video state
            if (videoLocalState != null && mPresenter.isConnectingOrConnectedPresenterHandler()) {
                log += "in connected state.";

                if (videoLocalState.isAudioMute()) {
                    processBtnAudioMute(true);
                }

                if (videoLocalState.isVideoMute()) {
                    processBtnVideoMute(true);
                }

                // Toggle camera back to previous state if required.
                if (videoLocalState.isCameraToggle() && mPresenter.getVideoViewPresenterHandler(null) != null) {

                    processBtnCameraToggle(true);
                }

                onConnectUIChange();

                addSelfView(mPresenter.getVideoViewPresenterHandler(null));
                addRemoteView();

            } else {
                log += "in disconnecting state.";
                onDisconnectUIChange();
            }
        } else {
            log += "Starting in disconnected state.";
            // This is the start of this sample, reset permission request states.
            permissionUtils.permQReset();

            onDisconnectUIChange();
        }

        Log.d(TAG, log);

        btnEnterRoom.setOnClickListener(v -> {
            connectToRoom();
            onConnectingUIChange();
        });

        btnAudioMute.setOnClickListener(v -> {
            processBtnAudioMute(!videoLocalState.isAudioMute());
        });

        btnVideoMute.setOnClickListener(v -> {
            processBtnVideoMute(!videoLocalState.isVideoMute());
        });

        btnCameraToggle.setOnClickListener(v -> {
            processBtnCameraToggle(!videoLocalState.isCameraToggle());
        });

        disconnectButton.setOnClickListener(v -> {
            String log1 = "Clicked Disconnect!";
            toastLog(TAG, mContext, log1);
            disconnectFromRoom();
            onDisconnectUIChange();
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Allow volume to be controlled using volume keys
        ((VideoCallActivity) mContext).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Toggle camera back to previous state if required.
        if (videoLocalState.isCameraToggle()) {
            if (mPresenter.getVideoViewPresenterHandler(null) != null) {
                mPresenter.toggleCameraPresenterHandler();

                videoLocalState.setCameraToggle(false);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onPause() {
        super.onPause();

        // Stop local video source only if not changing orientation
        if (!((VideoCallActivity) mContext).isChangingConfigurations()) {
            if (mPresenter.getVideoViewPresenterHandler(null) != null) {
                // Stop local video source if it's on.
                // Record if need to toggleCamera when resuming.
                boolean toggleCamera = mPresenter.toggleCameraPresenterHandler(false);
                videoLocalState.setCameraToggle(toggleCamera);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save states for fragment restart
        outState.putSerializable(BUNDLE_VIDEO_STATE, videoLocalState);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onDetach() {
        super.onDetach();
        disconnectFromRoom();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        permissionUtils.onRequestPermissionsResultHandler(
                requestCode, permissions, grantResults, TAG);
    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        linearLayout = (LinearLayout) rootView.findViewById(R.id.ll_video_call);
        btnEnterRoom = (Button) rootView.findViewById(R.id.btn_enter_room);
        etRoomName = (EditText) rootView.findViewById(R.id.et_room_name);
        etRoomName.setText(ROOM_NAME.toString());
        btnAudioMute = (Button) rootView.findViewById(R.id.toggle_audio);
        btnVideoMute = (Button) rootView.findViewById(R.id.toggle_video);
        btnCameraToggle = (Button) rootView.findViewById(R.id.toggle_camera);
        disconnectButton = (Button) rootView.findViewById(R.id.disconnect);
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
        //init permissionUtils
        permissionUtils = new PermissionUtils(getContext());

        //init video resolutions
        videoInput = new VideoResolution();
        videoSent = new VideoResolution();
        videoReceive = new VideoResolution();

        //init video state
        videoLocalState = new VideoLocalState();

        // Video resolution UI.
        setUiResControls(rootView);

        // Set UI elements
        setAudioBtnLabel(false, false);
        setVideoBtnLabel(false, false);
    }

    /**
     * Get room name from text field (or use default if not entered),
     * and connect to that room.
     * Initializes SkylinkConnection if not initialized.
     */
    private void connectToRoom() {
        roomName = etRoomName.getText().toString();

        String log = "";
        // If roomName is not set through the UI, get the default roomName from Constants
        if (roomName == null || "".equals(roomName)) {
            roomName = ROOM_NAME;
            etRoomName.setText(roomName);
            log = "No room name provided, entering default video room \"" + roomName
                    + "\".";
        } else {
            log = "Entering video room \"" + roomName + "\".";
        }
        toastLog(TAG, mContext, log);

        mPresenter.connectToRoomPresenterHandler(roomName);
    }

    /**
     * Disconnect from room.
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private void disconnectFromRoom() {
        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already connecting/connected and not changing orientation.
        if (!((VideoCallActivity) mContext).isChangingConfigurations()) {
            mPresenter.disconnectFromRoomPresenterHandler();
        }
    }

    /**
     * Record the current local input video width, height, fps and SkylinkCaptureFormat.
     * Get the range of {@link SkylinkCaptureFormat} supported by the current camera,
     * and write them to {@link #captureFormats} if the camera has changed.
     * If the current VideoDevice is not a camera, this will set captureFormats to null.
     *
     * @param width
     * @param height
     * @param fps
     * @param captureFormat
     */
    private void noteInputVideoResolutions(int width, int height, int fps,
                                           SkylinkCaptureFormat captureFormat) {
        videoInput.setWidth(width);
        videoInput.setHeight(height);
        videoInput.setFps(fps);
        setUiResTvStats(videoInput, tvResInput);
        currentVideoDevice = mPresenter.getCurrentVideoDevicePresenterHandler();
        String previousCameraName = currentCameraName;
        currentCameraName = mPresenter.getCurrentCameraNamePresenterHandler();

        String captureFormatString = "Current capture formats have not changed.";

        // Check if a new camera in now active.
        boolean newCamera = false;
        if (currentCameraName != null) {
            if (!currentCameraName.equals(previousCameraName)) {
                newCamera = true;
            }
        } else if (previousCameraName != null) {
            if (!previousCameraName.equals(currentCameraName)) {
                newCamera = true;
            }
        }

        // Update the UI for setting new video resolution if a new camera has been obtained.
        if (newCamera) {
            // Set the range of supported SkylinkCaptureFormats.
            // Record current range of supported SkylinkCaptureFormats.
            captureFormats = mPresenter.getCaptureFormatsPresenterHandler(null);

            //get captureFormats String info
            captureFormatString = mPresenter.getCaptureFormatsStringPresenterHandler(captureFormats);
            // Try to continue to with last selected Fps if possible.
            int fpsNew = Utils.getFpsForNewCaptureFormat(fpsSel, captureFormat);

            if (captureFormat != null) {
                // Set new selected CaptureFormat and frame rate.
                captureFormatSel = captureFormat;
                fpsSel = fpsNew;
            }

            // Set UI values.
            setUiResDim(width, height, captureFormats);
            setUiResFps(fpsNew, captureFormat);
        }

        String log = "The current local video by VideoDevice " + currentVideoDevice +
                ", with camera name \"" + currentCameraName +
                "\", has width, height, fps: " + width + ", " + height + ", " + fps +
                ".\r\n" + captureFormatString;
        Log.d(TAG, log);
    }

    /**
     * Call Skylink API to set local input video resolution's width, height and frame rate.
     * Do not call Skylink API if values are invalid, or
     * the same as current local video's values.
     *
     * @param format The {@link SkylinkCaptureFormat} with the width and height to set.
     * @param fps    Frame rate to set in frames per second
     * @return True if new resolution set, false otherwise.
     */
    private boolean setInputVideoResolutions(SkylinkCaptureFormat format, int fps) {
        int width = format.getWidth();
        int height = format.getHeight();
        if (width < 0 || height < 0 || fps < 0) {
            return false;
        }

        // Set new selected CaptureFormat and frame rate.
        captureFormatSel = format;
        fpsSel = fps;

        // If already at new resolution, no need to call Skylink API.
        if (videoInput.getWidth() == width && videoInput.getHeight() == height && videoInput.getFps() == fps) {
            return true;
        }

        mPresenter.setInputVideoResolutionPresenterHandler(width, height, fps);

        return true;
    }

    /**
     * Change certain UI elements once connected to room or when Peer(s) join or leave.
     */
    private void onConnectUIChange() {
        btnEnterRoom.setVisibility(GONE);
        etRoomName.setEnabled(false);
        btnAudioMute.setVisibility(VISIBLE);
        btnVideoMute.setVisibility(VISIBLE);
        btnCameraToggle.setVisibility(VISIBLE);
        disconnectButton.setVisibility(VISIBLE);
        setUiResControlsVisibility(VISIBLE);
    }

    /**
     * Change certain UI elements when trying to connect to room.
     */
    private void onConnectingUIChange() {
        btnEnterRoom.setVisibility(GONE);
        etRoomName.setEnabled(false);
        btnAudioMute.setVisibility(GONE);
        btnVideoMute.setVisibility(GONE);
        btnCameraToggle.setVisibility(GONE);
        disconnectButton.setVisibility(VISIBLE);
        setUiResControlsVisibility(VISIBLE);
    }

    /**
     * Change certain UI elements when disconnecting from room.
     */
    private void onDisconnectUIChange() {
        View self = linearLayout.findViewWithTag("self");
        if (self != null) {
            linearLayout.removeView(self);
        }

        View peer = linearLayout.findViewWithTag("peer");
        if (peer != null) {
            linearLayout.removeView(peer);
        }

        btnEnterRoom.setVisibility(VISIBLE);
        etRoomName.setEnabled(true);
        btnAudioMute.setVisibility(GONE);
        btnVideoMute.setVisibility(GONE);
        btnCameraToggle.setVisibility(GONE);
        disconnectButton.setVisibility(GONE);

        videoInput = new VideoResolution();
        videoSent = new VideoResolution();
        videoReceive = new VideoResolution();

        setUiResControlsVisibility(GONE);
    }

    @NonNull
    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListenerDim() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Set textView to match
                setUiResTvOnSeekBarProgressDim(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                SkylinkCaptureFormat format = setUiResTvOnSeekBarProgressDim(seekBar.getProgress());
                // Check that new CaptureFormat is valid.
                if (!Utils.isCaptureFormatValid(format)) {
                    return;
                }

                int width = format.getWidth();
                int height = format.getHeight();
                int fpsNew = Utils.getFpsForNewCaptureFormat(fpsSel, format);

                // If any of the new Dim or Fps values are not valid,
                // or setting new resolution was not successful, reset UI to previous values.
                if (fpsNew < 0 || !setUiResTvDim(width, height) || !setUiResFps(fpsNew, format)
                        || setInputVideoResolutions(format, fpsNew)) {
                    setUiResDim(captureFormatSel.getWidth(), captureFormatSel.getHeight(),
                            captureFormats);
                    setUiResFps(fpsSel, captureFormatSel);
                    return;
                }
            }
        };
    }

    @NonNull
    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListenerFps() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Set textView to match
                setUiResTvOnSeekBarProgressFps(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int fpsNew = setUiResTvOnSeekBarProgressFps(seekBar.getProgress());

                // Check that new fps is valid for selected CaptureFormat.
                fpsNew = Utils.getFpsForNewCaptureFormat(fpsNew, captureFormatSel);

                // If any of new Fps or selected CaptureFormat are not valid,
                // or setting new resolution was not successful, reset Fps UI to previous values.
                if (fpsNew < 0 || setInputVideoResolutions(captureFormatSel, fpsNew)) {
                    setUiResFps(fpsSel, captureFormatSel);
                    return;
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
        tvInput = (TextView) rootView.findViewById(R.id.textViewInput);
        tvResInput = (TextView) rootView.findViewById(R.id.textViewResInput);
        tvSent = (TextView) rootView.findViewById(R.id.textViewSent);
        tvResSent = (TextView) rootView.findViewById(R.id.textViewResSent);
        tvRecv = (TextView) rootView.findViewById(R.id.textViewRecv);
        tvResRecv = (TextView) rootView.findViewById(R.id.textViewResRecv);
        tvResDim = (TextView) rootView.findViewById(R.id.textViewDim);
        tvResFps = (TextView) rootView.findViewById(R.id.textViewFps);

        // Set empty values.
        tvInput.setText("Input:");
        tvSent.setText("Sent:");
        tvRecv.setText("Recv:");
        setUiResTvStats(videoInput, tvResInput);
        setUiResTvStats(videoSent, tvResSent);
        setUiResTvStats(videoReceive, tvResRecv);
        setUiResDim(videoInput.getWidth(), videoInput.getHeight(), captureFormats);
        setUiResFps(fpsSel, captureFormatSel);
        tvResDim.setFocusable(true);
        tvResDim.setEnabled(true);
        tvResDim.setClickable(true);
        tvResDim.setFocusableInTouchMode(true);
        tvResFps.setFocusable(true);
        tvResFps.setEnabled(true);
        tvResFps.setClickable(true);
        tvResFps.setFocusableInTouchMode(true);

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
     * Set selected resolution dimensions (i.e. width and height) on the UI based on the
     * given range of {@link SkylinkCaptureFormat} array.
     * Both the SeekBar and the TextView would be set, or reset if provided values were not valid.
     * {@link #captureFormatSel} would also be set, or reset if provided values were not valid.
     *
     * @param width          The width to be set.
     * @param height         The height to be set.
     * @param captureFormats The {@link SkylinkCaptureFormat} array that provides range of seekbar.
     * @return True if provided values were valid and false otherwise.
     */
    private boolean setUiResDim(int width, int height, SkylinkCaptureFormat[] captureFormats) {
        boolean valid;
        int index = -1;
        // Set the seekbar range.
        valid = setUiResSeekBarRangeDim(captureFormats);
        // Get the captureFormat that matches for width and height.
        if (valid) {
            index = mPresenter.getSeekBarIndexDimPresenterHandler(captureFormats, width, height);
            if (index < 0) {
                valid = false;
            }
        }

        if (valid) {
            // Set the SeekBar
            seekBarResDim.setProgress(index);
            // Set TextView
            setUiResTvDim(width, height);
        } else {
            // Set the SeekBar
            seekBarResDim.setMax(0);
            // Set default string in TextView.
            setUiResTvDim(-1, -1);
        }
        return valid;
    }

    /**
     * Set selected resolution frame rate on the UI.
     * Both the SeekBar and the TextView would be set, or reset if provided values were not valid.
     * {@link #fpsSel} would be also be set, or reset if provided values were not valid.
     *
     * @param fps    The frame rate in fps (frames per second) to be set.
     * @param format A {@link SkylinkCaptureFormat} that provides the fps range to be set.
     * @return True if provided values were valid and false otherwise.
     */
    private boolean setUiResFps(int fps, SkylinkCaptureFormat format) {
        boolean valid;
        int index = -1;
        // Set the seekbar range.
        valid = setUiResSeekBarRangeFps(format);

        // Get the fps index in format.
        if (valid) {
            index = mPresenter.getSeekBarIndexFpsPresenterHandler(format, fps);
            if (index < 0) {
                valid = false;
            }
        }

        if (valid) {
            // Set the SeekBar
            seekBarResFps.setProgress(index);
            // Set TextView
            setUiResTvFps(fps);
        } else {
            // Set the SeekBar
            seekBarResFps.setMax(0);
            // Set default string in TextView.
            setUiResTvFps(-1);
        }
        return valid;
    }

    /**
     * Once new video resolution dimensions are selected on the Seekbar,
     * change the selected video resolution dimensions TextView to match.
     *
     * @param progress
     * @return {@link SkylinkCaptureFormat} selected.
     */
    private SkylinkCaptureFormat setUiResTvOnSeekBarProgressDim(int progress) {
        SkylinkCaptureFormat format = mPresenter.getSeekBarValueDimPresenterHandler(progress, captureFormats);
        if (format == null) {
            return null;
        }

        // Set textView to match
        int width = format.getWidth();
        int height = format.getHeight();
        setUiResTvDim(width, height);
        return format;
    }

    /**
     * Once a new fps is selected on the Seekbar, change the selected fps TextView to match.
     *
     * @param progress
     * @return Fps selected.
     */
    private int setUiResTvOnSeekBarProgressFps(int progress) {
        int fps = mPresenter.getSeekBarValueFpsPresenterHandler(progress, captureFormatSel);
        setUiResTvFps(fps);
        return fps;
    }

    /**
     * Set the ranges of values for seekBarResDim.
     * Set to zero if range of values invalid.
     *
     * @param captureFormats SkylinkCaptureFormat array for setting range of seekBarResDim.
     * @return True if captureFormats was valid and false otherwise.
     */
    private boolean setUiResSeekBarRangeDim(SkylinkCaptureFormat[] captureFormats) {
        // If there is no valid captureFormats set, set seekBar range to zero.
        if (!Utils.isCaptureFormatsValid(captureFormats)) {
            seekBarResDim.setMax(0);
            return false;
        }
        // Set dimension range based on size of current CaptureFormat list.
        seekBarResDim.setMax(captureFormats.length - 1);
        return true;
    }

    /**
     * Set the ranges of values for seekBarResFps.
     * Set to zero if range of values invalid.
     *
     * @return True if captureFormat was valid and false otherwise.
     */
    private boolean setUiResSeekBarRangeFps(SkylinkCaptureFormat captureFormat) {
        // If there is no valid captureFormatSel set, set seekBar range to zero.
        if (!Utils.isCaptureFormatValid(captureFormat)) {
            seekBarResFps.setMax(0);
            return false;
        } else {
            int range = captureFormat.getFpsMax() - captureFormat.getFpsMin();
            seekBarResFps.setMax(range);
            return true;
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
        if (videoResolution.getWidth() <= 0 || videoResolution.getHeight() <= 0 || videoResolution.getFps() < 0) {
            textView.setText("Width x Height, fps");
            return;
        }
        // Set textView to match
        String str = Utils.getResDimStr(videoResolution.getWidth(), videoResolution.getHeight()) + ", " + Utils.getResFpsStr(videoResolution.getFps());
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
            tvResDim.setText("Width x Height");
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
            tvResFps.setText("Framerate (fps)");
            return;
        }
        // Set textView to match
        tvResFps.setText(Utils.getResFpsStr(fps));
    }

    /**
     * Add or update our self VideoView into the app.
     *
     * @param videoView
     */
    private void addSelfView(SurfaceViewRenderer videoView) {
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
                String name = mPresenter.getRoomPeerIdNickPresenterHandler();

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
                        (dialog, which) -> mPresenter.getVideoResolutionsPresenterHandler(mPresenter.getPeerIdPresenterHandler(1)));
                // Switch camera if possible.
                selfDialogBuilder.setNegativeButton("Switch Camera",
                        (dialog, which) -> {
                            mPresenter.switchCameraPresenterHandler();
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
    private void addRemoteView() {
        SurfaceViewRenderer videoView;
        String remotePeerId = mPresenter.getPeerIdPresenterHandler(1);
        // Proceed only if the first (& only) remote Peer has joined.
        if (remotePeerId == null) {
            return;
        } else {
            videoView = mPresenter.getVideoViewPresenterHandler(remotePeerId);
        }
        if (videoView == null) {
            return;
        }

        // Remove previous peer video if it exists
        View viewToRemove = linearLayout.findViewWithTag("peer");
        if (viewToRemove != null) {
            linearLayout.removeView(viewToRemove);
        }

        // Add new peer video
        videoView.setTag("peer");
        // Remove view from previous parent, if any.
        Utils.removeViewFromParent(videoView);
        // Add view to parent
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        videoView.setLayoutParams(params);
        linearLayout.addView(videoView);
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

    // If audio is enabled, mute audio and if audio is mute, then enable it
    private void processBtnAudioMute(boolean isAudioMuted) {

        //save audioMuted for other usage
        videoLocalState.setAudioMute(isAudioMuted);

        //set mute audio to sdk
        mPresenter.muteLocalAudioPresenterHandler(isAudioMuted);

        // Set UI and Toast.
        setAudioBtnLabel(isAudioMuted, true);
    }

    // If video is enabled, mute video and if video is mute, then enable it
    private void processBtnVideoMute(boolean isVideoMuted) {

        //save audioMuted for other usage
        videoLocalState.setVideoMute(isVideoMuted);

        //set mute audio to sdk
        mPresenter.muteLocalVideoPresenterHandler(isVideoMuted);

        // Set UI and Toast.
        setVideoBtnLabel(videoLocalState.isVideoMute(), true);
    }

    // If video is enable, toggle video and if video is toggle, then enable it
    private void processBtnCameraToggle(boolean isCameraToggle) {

        //change state of camera toggle
        videoLocalState.setCameraToggle(!isCameraToggle);

        //display instruction log
        String log12 = "Toggled camera ";
        if (mPresenter.getVideoViewPresenterHandler(null) != null) {
            if (mPresenter.toggleCameraPresenterHandler()) {
                log12 += "to restarted!";
            } else {
                log12 += "to stopped!";
            }
        } else {
            log12 += "but failed as local video is not available!";
        }
        toastLog(TAG, mContext, log12);

        //this button dont need to change text
    }

    //----------------------------------------------------------------------------------------------
    // View Listeners to update GUI from presenter
    //----------------------------------------------------------------------------------------------

    @Override
    public void setRoomDetailsViewHandler(String roomDetails) {

    }

    @Override
    public void onConnectUIChangeViewHandler() {
        onConnectUIChange();
    }

    @Override
    public void onDisconnectUIChangeViewHandler() {
        onDisconnectUIChange();
    }

    @Override
    public void addSelfViewPresenterHandler(SurfaceViewRenderer videoView) {
        addSelfView(videoView);
    }

    @Override
    public void noteInputVideoResolutionsViewHandler(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        noteInputVideoResolutions(width, height, fps, captureFormat);
    }

    @Override
    public void setUiResTvStatsReceivedViewHandler(int width, int height, int fps) {
        videoReceive.setWidth(width);
        videoReceive.setHeight(height);
        videoReceive.setFps(fps);
        setUiResTvStats(videoReceive, tvResRecv);
    }

    @Override
    public void setUiResTvStatsSentViewHandler(int width, int height, int fps) {
        videoSent.setWidth(width);
        videoSent.setHeight(height);
        videoSent.setFps(fps);
        setUiResTvStats(videoSent, tvResSent);
    }

    @Override
    public void addRemoteViewViewHandler() {
        addRemoteView();
    }

    @Override
    public void onRemotePeerLeaveUIChangeViewHandler() {
        View peerView = linearLayout.findViewWithTag("peer");
        linearLayout.removeView(peerView);
    }

    @Override
    public Fragment getFragmentViewHandler() {
        return this;
    }
}
