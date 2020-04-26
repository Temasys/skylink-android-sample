package sg.com.temasys.skylink.sdk.sampleapp;

/**
 * Created by lavanyasudharsanam on 20/1/15.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
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

import java.util.Date;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.OsListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.Errors;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static sg.com.temasys.skylink.sdk.sampleapp.MainActivity.ARG_SECTION_NUMBER;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.getNumRemotePeers;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.isConnectingOrConnected;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.permQReset;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.permQResume;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.Utils.toastLogLong;

/**
 * This class is used to demonstrate the VideoCall between two clients in WebRTC
 */
public class VideoCallFragment extends Fragment
        implements LifeCycleListener, MediaListener, OsListener, RemotePeerListener {

    private String ROOM_NAME;
    private String MY_USER_NAME;

    private static final String TAG = VideoCallFragment.class.getCanonicalName();

    // Video resolution from camera input.
    public static int widthInput = -1;
    public static int heightInput = -1;
    public static int fpsInput = -1; // Frame rate in frames per second (fps).

    // Video resolution from camera sent out to Peer.
    public static int widthSent = -1;
    public static int heightSent = -1;
    public static int fpsSent = -1; // Frame rate in frames per second (fps).

    // Video resolution received from Peer.
    public static int widthRecv = -1;
    public static int heightRecv = -1;
    public static int fpsRecv = -1; // Frame rate in frames per second (fps).

    // The current VideoDevice.
    private static SkylinkConfig.VideoDevice currentVideoDevice = null;
    // The current camera name.
    private static String currentCameraName = null;
    // The array of SkylinkCaptureFormats support by the current camera.
    private static SkylinkCaptureFormat[] captureFormats;
    // The selected SkylinkCaptureFormat on UI,
    // not necessarily the currently used SkylinkCaptureFormat.
    private static SkylinkCaptureFormat captureFormatSel = null;
    // The last selected frame rate (fps) on UI,
    // not necessarily the currently used frame rate.
    private static int fpsSel = -1;

    // Constants for configuration change
    private static final String BUNDLE_CONNECTING = "connecting";
    private static final String BUNDLE_AUDIO_MUTED = "audioMuted";
    private static final String BUNDLE_VIDEO_MUTED = "videoMuted";

    private static SkylinkConnection skylinkConnection;
    private static SkylinkConfig skylinkConfig;
    // Indicates if camera should be toggled after returning to app.
    // Generally, it should match whether it was toggled when moving away from app.
    // For e.g., if camera was already off, then it would not be toggled when moving away from app,
    // So toggleCamera would be set to false at onPause(), and at onCreateView,
    // it would not be toggled.
    private static boolean toggleCamera;

    private boolean connecting = false;
    private String roomName;
    private boolean audioMuted;
    private boolean videoMuted;

    private Context context;
    private View rootView;

    // UI Controls
    private LinearLayout linearLayout;
    // Room
    private Button disconnectButton;
    private Button btnEnterRoom;
    private EditText etRoomName;
    // Media
    private Button toggleAudioButton;
    private Button toggleVideoButton;
    private Button toggleCameraButton;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String logTag = "[SA][Video][onCreateView] ";
        String log = logTag;

        ROOM_NAME = Config.ROOM_NAME_VIDEO;
        MY_USER_NAME = Config.USER_NAME_VIDEO;

        rootView = inflater.inflate(R.layout.fragment_video_call, container, false);
        linearLayout = (LinearLayout) rootView.findViewById(R.id.ll_video_call);
        btnEnterRoom = (Button) rootView.findViewById(R.id.btn_enter_room);
        etRoomName = (EditText) rootView.findViewById(R.id.et_room_name);
        etRoomName.setText(ROOM_NAME.toString());
        toggleAudioButton = (Button) rootView.findViewById(R.id.toggle_audio);
        toggleVideoButton = (Button) rootView.findViewById(R.id.toggle_video);
        toggleCameraButton = (Button) rootView.findViewById(R.id.toggle_camera);
        disconnectButton = (Button) rootView.findViewById(R.id.disconnect);
        // Video resolution UI.
        setUiResControls();

        // Check if it was an orientation change
        if (savedInstanceState != null) {
            log += "Restarting ";
            // Resume previous permission request, if any.
            permQResume(context, this, skylinkConnection);

            // Toggle camera back to previous state if required.
            if (toggleCamera) {
                if (getVideoView(null) != null) {
                    skylinkConnection.toggleCamera();
                    toggleCamera = false;
                }
            }

            connecting = savedInstanceState.getBoolean(BUNDLE_CONNECTING);
            // Set the appropriate UI if already connected.
            if (isConnectingOrConnected()) {
                log += "in connected state.";
                // Set listeners to receive callbacks when events are triggered
                setListeners();
                audioMuted = savedInstanceState.getBoolean(BUNDLE_AUDIO_MUTED);
                videoMuted = savedInstanceState.getBoolean(BUNDLE_VIDEO_MUTED);
                // Set the appropriate UI if already connected.
                onConnectUIChange();
                addSelfView(getVideoView(null));
                addRemoteView();
            } else if (connecting) {
                log += "in connecting state.";
                // Set listeners to receive callbacks when events are triggered
                setListeners();
                onConnectingUIChange();
                addSelfView(getVideoView(null));
            } else {
                log += "in disconnecting state.";
                onDisconnectUIChange();
            }
        } else {
            log += "Starting in disconnected state.";
            // This is the start of this sample, reset permission request states.
            permQReset();

            // Set toggleCamera back to default state.
            toggleCamera = false;

            onDisconnectUIChange();
        }

        Log.d(TAG, log);

        // Set UI elements
        setAudioBtnLabel(false);
        setVideoBtnLabel(false);

        btnEnterRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToRoom();
                onConnectingUIChange();
            }
        });

        toggleAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If audio is enabled, mute audio and if audio is enabled, mute it
                audioMuted = !audioMuted;
                skylinkConnection.muteLocalAudio(audioMuted);

                // Set UI and Toast.
                setAudioBtnLabel(true);
            }
        });

        toggleVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If video is enabled, mute video and if video is enabled, mute it
                videoMuted = !videoMuted;
                skylinkConnection.muteLocalVideo(videoMuted);

                // Set UI and Toast.
                setVideoBtnLabel(true);
            }
        });

        toggleCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String log = "Toggled camera ";
                if (getVideoView(null) != null) {
                    if (skylinkConnection.toggleCamera()) {
                        log += "to restarted!";
                    } else {
                        log += "to stopped!";
                    }
                } else {
                    log += "but failed as local video is not available!";
                }
                toastLog(TAG, context, log);
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onClick(View v) {
                String log = "Clicked Disconnect!";
                toastLog(TAG, context, log);
                disconnectFromRoom();
                onDisconnectUIChange();
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Allow volume to be controlled using volume keys
        ((MainActivity) context).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //update actionbar title
        ((MainActivity) context).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
        this.context = context;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Toggle camera back to previous state if required.
        if (toggleCamera) {
            if (getVideoView(null) != null) {
                skylinkConnection.toggleCamera();
                toggleCamera = false;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onPause() {
        super.onPause();

        // Stop local video source only if not changing orientation
        if (!((MainActivity) context).isChangingConfigurations()) {
            if (getVideoView(null) != null) {
                // Stop local video source if it's on.
                // Record if need to toggleCamera when resuming.
                toggleCamera = skylinkConnection.toggleCamera(false);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save states for fragment restart
        outState.putBoolean(BUNDLE_CONNECTING, connecting);
        outState.putBoolean(BUNDLE_AUDIO_MUTED, audioMuted);
        outState.putBoolean(BUNDLE_VIDEO_MUTED, videoMuted);
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
        Utils.onRequestPermissionsResultHandler(
                requestCode, permissions, grantResults, TAG, skylinkConnection);
    }

    //----------------------------------------------------------------------------------------------
    // Skylink helper methods
    //----------------------------------------------------------------------------------------------

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
        toastLog(TAG, context, log);

        // Initialize the skylink connection
        initializeSkylinkConnection();

        // Create the Skylink connection string.
        // In production, the connection string should be generated by an external entity
        // (such as a secure App server that has the Skylink App Key secret), and sent to the App.
        // This is to avoid keeping the App Key secret within the application, for better security.
        String skylinkConnectionString = Utils.getSkylinkConnectionString(
                roomName, new Date(), SkylinkConnection.DEFAULT_DURATION);

        // The skylinkConnectionString should not be logged in production,
        // as it contains potentially sensitive information like the Skylink App Key ID.

        boolean connectFailed;
        connectFailed = !skylinkConnection.connectToRoom(skylinkConnectionString, MY_USER_NAME);
        if (connectFailed) {
            log = "[SA][Video][connectToRoom] Unable to connect to room!";
            toastLog(TAG, context, log);
            return;
        }

        connecting = true;

        // Initialize and use the Audio router to switch between headphone and headset
        AudioRouter.startAudioRouting(context);
    }

    /**
     * Disconnect from room.
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private void disconnectFromRoom() {
        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already connecting/connected and not changing orientation.
        if (!((MainActivity) context).isChangingConfigurations() && skylinkConnection != null
                && isConnectingOrConnected()) {
            if (skylinkConnection.disconnectFromRoom()) {
                connecting = false;
            }
            AudioRouter.stopAudioRouting(context);
        }
    }

    /**
     * Set the range of supported SkylinkCaptureFormats.
     *
     * @return A string representing the current CaptureFormat and supported CaptureFormats, if any.
     */
    private String setCaptureFormats() {
        String strFormat = "No CaptureFormat currently registered.";
        String strFormats = "No CaptureFormats currently registered.";

        // Record current range of supported SkylinkCaptureFormats.
        if (skylinkConnection != null) {
            captureFormats = skylinkConnection.getCaptureFormats(null);
        }
        if (isCaptureFormatsValid(captureFormats)) {
            strFormats = captureFormatsToString(captureFormats);
        }

        // Get the current CaptureFormat, if there is one.
        SkylinkCaptureFormat captureFormat = skylinkConnection.getCaptureFormat();
        if (captureFormat != null) {
            strFormat = captureFormat.toString();
        }

        String captureFormatString = "Current capture format: " + strFormat + ".\r\n" +
                "Supported capture formats: " + strFormats + ".";
        return captureFormatString;
    }

    @NonNull
    /**
     * Return a description of each CaptureFormat in the Array of CaptureFormats provided.
     */
    private String captureFormatsToString(SkylinkCaptureFormat[] captureFormats) {
        if (!isCaptureFormatsValid(captureFormats)) {
            return null;
        }
        String formats = "";
        for (SkylinkCaptureFormat supportedFormat : captureFormats) {
            formats += "\r\n" + supportedFormat.toString();
        }
        return formats;
    }

    /**
     * Set listeners to receive callbacks when events are triggered.
     * SkylinkConnection instance must not be null or listeners cannot be set.
     * Do not set before {@link SkylinkConnection#init} as that will remove all existing Listeners.
     *
     * @return false if listeners could not be set.
     */
    private boolean setListeners() {
        String logTag = "[SA][Video][setLtn] ";
        String log = "";
        if (skylinkConnection != null) {
            log = logTag + "Setting Listeners...";
            Log.d(TAG, log);
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setMediaListener(this);
            skylinkConnection.setOsListener(this);
            skylinkConnection.setRemotePeerListener(this);
            log = logTag + "Done.";
            Log.d(TAG, log);
            return true;
        } else {
            log = logTag + "Unable to set Listeners as skylinkConnection is not available!";
            Log.d(TAG, log);
            return false;
        }
    }

    /**
     * Get peerId of a Peer using SkylinkConnection API.
     *
     * @param index 0 for self Peer, 1 onwards for remote Peer(s).
     * @return Desired peerId or null if not available.
     */
    private String getPeerId(int index) {
        if (skylinkConnection == null) {
            return null;
        }
        String[] peerIdList = skylinkConnection.getPeerIdList();
        // Ensure index does not exceed range of possible indices on peerIdList.
        if (index > peerIdList.length - 1 || index < 0) {
            return null;
        }
        return peerIdList[index];
    }

    private SkylinkConfig getSkylinkConfig() {
        if (skylinkConfig != null) {
            return skylinkConfig;
        }

        skylinkConfig = new SkylinkConfig();
        // AudioVideo config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setHasPeerMessaging(true);
        skylinkConfig.setHasFileTransfer(true);
        skylinkConfig.setMirrorLocalView(true);
        skylinkConfig.setReportVideoResolutionOnChange(true);

        // Allow only 1 remote Peer to join.
        skylinkConfig.setMaxPeers(1); // Default is 4 remote Peers.

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    private void initializeSkylinkConnection() {
        skylinkConnection = SkylinkConnection.getInstance();
        //the app_key and app_secret is obtained from the temasys developer console.
        skylinkConnection.init(Config.getAppKey(), getSkylinkConfig(),
                context.getApplicationContext());
        // Set listeners to receive callbacks when events are triggered
        setListeners();
    }

    /**
     * Call Skylink APIs to get all Video resolutions available, including:
     * Input, sent and received.
     */
    private void getVideoResolutions() {
        skylinkConnection.getInputVideoResolution();
        String remotePeerId = getPeerId(1);
        if (remotePeerId != null) {
            skylinkConnection.getSentVideoResolution(remotePeerId);
            skylinkConnection.getReceivedVideoResolution(remotePeerId);
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
        setUiResTvStats(width, height, fps, tvResInput);
        this.widthInput = width;
        this.heightInput = height;
        this.fpsInput = fps;
        currentVideoDevice = skylinkConnection.getCurrentVideoDevice();
        String previousCameraName = currentCameraName;
        currentCameraName = skylinkConnection.getCurrentCameraName();

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
            captureFormatString = setCaptureFormats();
            // Try to continue to with last selected Fps if possible.
            int fpsNew = getFpsForNewCaptureFormat(fpsSel, captureFormat);

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
        if (skylinkConnection == null) {
            return false;
        }
        int width = format.getWidth();
        int height = format.getHeight();
        if (width < 0 || height < 0 || fps < 0) {
            return false;
        }

        // Set new selected CaptureFormat and frame rate.
        captureFormatSel = format;
        fpsSel = fps;

        // If already at new resolution, no need to call Skylink API.
        if (widthInput == width && heightInput == height && fpsInput == fps) {
            return true;
        }

        skylinkConnection.setInputVideoResolution(width, height, fps);
        return true;
    }

    /**
     * Get Video View of a given Peer using SkylinkConnection API.
     *
     * @param peerId null for self Peer.
     * @return Desired Video View or null if not present.
     */
    private SurfaceViewRenderer getVideoView(String peerId) {
        if (skylinkConnection == null) {
            return null;
        }
        return skylinkConnection.getVideoView(peerId);
    }

    //----------------------------------------------------------------------------------------------
    // UI helper methods
    //----------------------------------------------------------------------------------------------

    /**
     * Change certain UI elements once connected to room or when Peer(s) join or leave.
     */
    private void onConnectUIChange() {
        btnEnterRoom.setVisibility(GONE);
        etRoomName.setEnabled(false);
        toggleAudioButton.setVisibility(VISIBLE);
        toggleVideoButton.setVisibility(VISIBLE);
        toggleCameraButton.setVisibility(VISIBLE);
        disconnectButton.setVisibility(VISIBLE);
        setUiResControlsVisibility(VISIBLE);
    }

    /**
     * Change certain UI elements when trying to connect to room.
     */
    private void onConnectingUIChange() {
        btnEnterRoom.setVisibility(GONE);
        etRoomName.setEnabled(false);
        toggleAudioButton.setVisibility(GONE);
        toggleVideoButton.setVisibility(GONE);
        toggleCameraButton.setVisibility(GONE);
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
        toggleAudioButton.setVisibility(GONE);
        toggleVideoButton.setVisibility(GONE);
        toggleCameraButton.setVisibility(GONE);
        disconnectButton.setVisibility(GONE);

        widthInput = -1;
        heightInput = -1;
        fpsInput = -1;
        widthSent = -1;
        heightSent = -1;
        fpsSent = -1;
        widthRecv = -1;
        heightRecv = -1;
        fpsRecv = -1;
        setUiResControlsVisibility(GONE);
    }

    /**
     * Checks if given {@link SkylinkCaptureFormat SkylinkCaptureFormat[]} is valid for using.
     * To be valid, it cannot be null or empty.
     *
     * @param captureFormats
     * @return
     */
    private boolean isCaptureFormatsValid(SkylinkCaptureFormat[] captureFormats) {
        if (captureFormats == null || captureFormats.length == 0) {
            return false;
        }
        return true;
    }

    /**
     * Checks if given {@link SkylinkCaptureFormat} is valid for using.
     * To be valid it cannot be null, and the fps range cannot be negative.
     *
     * @param format
     * @return True if valid and false if not.
     */
    private boolean isCaptureFormatValid(SkylinkCaptureFormat format) {
        if (format == null) {
            return false;
        }

        // Check fps range based on min and max fps of this CaptureFormat.
        int range = format.getFpsMax() - format.getFpsMin();
        if (range < 0 || format.getFpsMin() < 0) {
            return false;
        }
        return true;
    }

    /**
     * Get the frame rate (fps) that should be selected, for a given {@link SkylinkCaptureFormat}.
     * Use the given fps if supported by given CaptureFormat, else use the new max fps.
     *
     * @param fps    Frame rate that should be selected if possible.
     * @param format {@link SkylinkCaptureFormat} that defines the possible frame rate range.
     * @return The appropriate fps, or a negative number if the given CaptureFormat is invalid.
     */
    private int getFpsForNewCaptureFormat(int fps, SkylinkCaptureFormat format) {

        // Check if given CaptureFormat is valid.
        if (!isCaptureFormatValid(format)) {
            return -1;
        }

        int fpsMinNew = format.getFpsMin();
        int fpsMaxNew = format.getFpsMax();

        // Set new fps UI max value if the current one is out of the new range.
        if (fps < fpsMinNew || fps > fpsMaxNew) {
            return fpsMaxNew;
        }
        return fps;
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
                if (!isCaptureFormatValid(format)) {
                    return;
                }

                int width = format.getWidth();
                int height = format.getHeight();
                int fpsNew = getFpsForNewCaptureFormat(fpsSel, format);

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
                fpsNew = getFpsForNewCaptureFormat(fpsNew, captureFormatSel);

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
     * Return the index of the given SkylinkCaptureFormat from within the list of
     * current supported SkylinkCaptureFormat[].
     *
     * @param captureFormats
     * @param width
     * @param height
     * @return Negative number if index could not be found, or if captureFormats is invalid.
     */
    int getSeekBarIndexDim(SkylinkCaptureFormat[] captureFormats, int width, int height) {
        if (!isCaptureFormatsValid(captureFormats)) {
            return -1;
        }

        int index = -1;
        SkylinkCaptureFormat format;
        int length = captureFormats.length;
        for (int i = 0; i < length; ++i) {
            format = captureFormats[i];
            if (width == format.getWidth() && height == format.getHeight()) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Return the index of the given fps from within the range of current supported fps.
     *
     * @param captureFormat
     * @param fps
     * @return Negative number if index could not be found, or if captureFormat is invalid.
     */
    int getSeekBarIndexFps(SkylinkCaptureFormat captureFormat, int fps) {
        if (!isCaptureFormatValid(captureFormat)) {
            return -1;
        }

        int fpsMin = captureFormat.getFpsMin();
        // fps must be within captureFormat's fps range.
        if (fps < fpsMin || fps > captureFormat.getFpsMax()) {
            return -1;
        }

        int index = fps - fpsMin;
        return index;
    }

    /**
     * Get {@link SkylinkCaptureFormat} selected by new seekBarResDim progress.
     * If no valid {@link SkylinkCaptureFormat} can be returned, return null.
     *
     * @param progress
     * @param captureFormats The {@link SkylinkCaptureFormat} array
     *                       from which to select a {@link SkylinkCaptureFormat}.
     * @return {@link SkylinkCaptureFormat} indicated by the seekBarResDim progress.
     */
    private SkylinkCaptureFormat getSeekBarValueDim(int progress,
                                                    SkylinkCaptureFormat[] captureFormats) {
        if (!isCaptureFormatsValid(captureFormats) || progress >= captureFormats.length) {
            return null;
        }
        SkylinkCaptureFormat format = captureFormats[progress];
        return format;
    }

    /**
     * Get the fps indicated by the progress value on the SeekBar.
     * Return a negative number if no value is available.
     *
     * @param progress The SeekBar progress.
     * @param format   A {@link SkylinkCaptureFormat} that represents fps range to choose from.
     * @return
     */
    private int getSeekBarValueFps(int progress, SkylinkCaptureFormat format) {
        if (!isCaptureFormatValid(format)) {
            return -1;
        }
        int fpsMin = format.getFpsMin();
        int fps = fpsMin + progress;
        // Fps value must be within format's fps range.
        if (fps < fpsMin || fps > format.getFpsMax()) {
            return -1;
        }
        return fps;
    }

    /**
     * Generate a string to display a set of video resolution dimensions (i.e. width and height).
     *
     * @param width
     * @param height
     * @return
     */
    @NonNull
    private String getResDimStr(int width, int height) {
        return width + " x " + height;
    }

    /**
     * Generate a string to display frame rate in fps.
     *
     * @param fps Framerate in frames per seconds.
     * @return
     */
    @NonNull
    private String getResFpsStr(int fps) {
        return fps + " fps";
    }

    /**
     * Set UI Controls for Resolution related.
     */
    private void setUiResControls() {
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
        setUiResTvStats(widthInput, heightInput, fpsInput, tvResInput);
        setUiResTvStats(widthSent, heightSent, fpsSent, tvResSent);
        setUiResTvStats(widthRecv, heightRecv, fpsRecv, tvResRecv);
        setUiResDim(widthInput, heightInput, captureFormats);
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
            index = getSeekBarIndexDim(captureFormats, width, height);
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
            index = getSeekBarIndexFps(format, fps);
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
        SkylinkCaptureFormat format = getSeekBarValueDim(progress, captureFormats);
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
        int fps = getSeekBarValueFps(progress, captureFormatSel);
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
        if (!isCaptureFormatsValid(captureFormats)) {
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
        if (!isCaptureFormatValid(captureFormat)) {
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
     * @param width
     * @param height
     */
    private void setUiResTvStats(int width, int height, int fps, TextView textView) {
        if (width <= 0 || height <= 0 || fps < 0) {
            textView.setText("Width x Height, fps");
            return;
        }
        // Set textView to match
        String str = getResDimStr(width, height) + ", " + getResFpsStr(fps);
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
        tvResDim.setText(getResDimStr(width, height));
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
        tvResFps.setText(getResFpsStr(fps));
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
            videoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (skylinkConnection != null) {
                        String name = Utils.getRoomPeerIdNick(skylinkConnection, ROOM_NAME,
                                skylinkConnection.getPeerId());
                        name += "\r\nClick outside dialog to return.";
                        TextView selfTV = new TextView(context);
                        selfTV.setText(name);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            selfTV.setTextIsSelectable(true);
                        }
                        AlertDialog.Builder selfDialogBuilder =
                                new AlertDialog.Builder(context);
                        selfDialogBuilder.setView(selfTV);
                        // Get the available video resolutions.
                        selfDialogBuilder.setPositiveButton("Video resolutions",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // getVideoResolutions();
                                        Utils.getVideoResolutions(getPeerId(1));
                                    }
                                });
                        // Switch camera if possible.
                        selfDialogBuilder.setNegativeButton("Switch Camera",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        skylinkConnection.switchCamera();
                                    }
                                });
                        selfDialogBuilder.show();
                    }
                }
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
        String remotePeerId = getPeerId(1);
        // Proceed only if the first (& only) remote Peer has joined.
        if (remotePeerId == null) {
            return;
        } else {
            videoView = getVideoView(remotePeerId);
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
    private void setAudioBtnLabel(boolean doToast) {
        if (audioMuted) {
            toggleAudioButton.setText(getString(R.string.enable_audio));
            if (doToast) {
                String log = getString(R.string.muted_audio);
                toastLog(TAG, context, log);
            }
        } else {
            toggleAudioButton.setText(getString(R.string.mute_audio));
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
    private void setVideoBtnLabel(boolean doToast) {
        if (videoMuted) {
            toggleVideoButton.setText(getString(R.string.enable_video));
            if (doToast) {
                String log = getString(R.string.muted_video);
                toastLog(TAG, context, log);
            }
        } else {
            toggleVideoButton.setText(getString(R.string.mute_video));
            if (doToast) {
                String log = getString(R.string.enabled_video);
                toastLog(TAG, context, log);
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Skylink Listeners
    //----------------------------------------------------------------------------------------------

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's
     * lifecycle
     */

    /**
     * Triggered when connection is successful
     *
     * @param isSuccessful
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccessful, String message) {
        if (isSuccessful) {
            connecting = false;
            onConnectUIChange();

            // start audio routing and turn on speaker
            AudioRouter.startAudioRouting(context.getApplicationContext());
            AudioRouter.turnOnSpeaker();

            String log = "[SA][Video][onConnect] Connected to room " + roomName + " (" +
                    skylinkConnection.getRoomId() +
                    ") as " + skylinkConnection.getPeerId() + " (" + MY_USER_NAME + ").";
            toastLogLong(TAG, context, log);
        } else {
            connecting = false;
            String log = "Skylink failed to connect!\nReason : " + message;
            toastLogLong(TAG, context, log);
            onDisconnectUIChange();
        }
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        onDisconnectUIChange();

        // turn off speaker to the normal state and stop audio routing
        AudioRouter.turnOffSpeaker();
        AudioRouter.stopAudioRouting(context.getApplicationContext());

        connecting = false;
        String log = "[onDisconnect] ";
        if (errorCode == Errors.DISCONNECT_FROM_ROOM) {
            log += "We have successfully disconnected from the room.";
        } else if (errorCode == Errors.DISCONNECT_UNEXPECTED_ERROR) {
            log += "WARNING! We have been unexpectedly disconnected from the room!";
        }
        log += " Server message: " + message;
        toastLogLong(TAG, context, log);
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        String log = "[SA] Peer " + remotePeerId + " changed Room locked status to "
                + lockStatus + ".";
        toastLog(TAG, context, log);
    }

    @Override
    public void onReceiveLog(int infoCode, String message) {
        Utils.handleSkylinkReceiveLog(infoCode, message, context, TAG);
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Utils.handleSkylinkWarning(errorCode, message, context, TAG);
    }

    /**
     * Media Listeners Callbacks - triggered when receiving changes to Media Stream from the
     * remote peer
     */

    /**
     * Triggered after the user's local media is captured.
     *
     * @param videoView
     */
    @Override
    public void onLocalMediaCapture(SurfaceViewRenderer videoView) {
        String log = "[SA][onLocalMediaCapture] ";
        if (videoView == null) {
            log += "VideoView is null!";
            Log.d(TAG, log);
            addSelfView(getVideoView(null));
        } else {
            log += "Adding VideoView as selfView.";
            Log.d(TAG, log);
            addSelfView(videoView);
        }
    }

    @Override
    public void onInputVideoResolutionObtained(int width, int height, int fps,
                                               SkylinkCaptureFormat captureFormat) {
        noteInputVideoResolutions(width, height, fps, captureFormat);

        String log = "[SA][VideoResInput] The current video input has width x height, fps: " +
                width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }

    @Override
    public void onReceivedVideoResolutionObtained(String peerId, int width, int height, int fps) {
        widthRecv = width;
        heightRecv = height;
        fpsRecv = fps;

        setUiResTvStats(width, height, fps, tvResRecv);

        String log = "[SA][VideoResRecv] The current video received from Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }

    @Override
    public void onSentVideoResolutionObtained(String peerId, int width, int height, int fps) {
        widthSent = width;
        heightSent = height;
        fpsSent = fps;

        setUiResTvStats(width, height, fps, tvResSent);

        String log = "[SA][VideoResSent] The current video sent to Peer " + peerId +
                " has width x height, fps: " + width + " x " + height + ", " + fps + " fps.\r\n";
        Log.d(TAG, log);
    }

    @Override
    public void onVideoSizeChange(String peerId, Point size) {
        String peer = "Peer " + peerId;
        // If peerId is null, this call is for our local video.
        if (peerId == null) {
            peer = "We've";
        }
        Log.d(TAG, peer + " got video size changed to: " + size.toString() + ".");
    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, SurfaceViewRenderer videoView) {
        addRemoteView();
        String log = "Received new ";
        if (videoView != null) {
            log += "Video ";
        } else {
            log += "Audio ";
        }
        log += "from Peer " + Utils.getPeerIdNick(remotePeerId) + ".\r\n";

        UserInfo remotePeerUserInfo = skylinkConnection.getUserInfo(remotePeerId);
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        Log.d(TAG, log);
    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        String log = "Peer " + Utils.getPeerIdNick(remotePeerId) +
                " Audio mute status via:\r\nCallback: " + isMuted + ".";

        // It is also possible to get the mute status via the UserInfo.
        UserInfo userInfo = skylinkConnection.getUserInfo(remotePeerId);
        if (userInfo != null) {
            log += "\r\nUserInfo: " + userInfo.isAudioMuted() + ".";
        }
        toastLog(TAG, context, log);
    }

    @Override
    public void onRemotePeerVideoToggle(String remotePeerId, boolean isMuted) {
        String log = "Peer " + Utils.getPeerIdNick(remotePeerId) +
                " Video mute status via:\r\nCallback: " + isMuted + ".";

        // It is also possible to get the mute status via the UserInfo.
        UserInfo userInfo = skylinkConnection.getUserInfo(remotePeerId);
        if (userInfo != null) {
            log += "\r\nUserInfo: " + userInfo.isVideoMuted() + ".";
        }
        toastLog(TAG, context, log);
    }

    /**
     * OsListener Callbacks - triggered by Android OS related events.
     */
    @Override
    public void onPermissionRequired(
            final String[] permissions, final int requestCode, final int infoCode) {
        Utils.onPermissionRequiredHandler(
                permissions, requestCode, infoCode, TAG, context, this, skylinkConnection);
    }

    @Override
    public void onPermissionGranted(String[] permissions, int requestCode, int infoCode) {
        Utils.onPermissionGrantedHandler(permissions, infoCode, TAG);
    }

    @Override
    public void onPermissionDenied(String[] permissions, int requestCode, int infoCode) {
        Utils.onPermissionDeniedHandler(infoCode, context, TAG);
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        String log = "Your Peer " + Utils.getPeerIdNick(remotePeerId) + " connected.";
        toastLog(TAG, context, log);
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message, UserInfo userInfo) {
        View peerView = linearLayout.findViewWithTag("peer");
        linearLayout.removeView(peerView);

        int numRemotePeers = getNumRemotePeers();
        String log = "Your Peer " + Utils.getPeerIdNick(remotePeerId, userInfo) + " left: " +
                message + ". " + numRemotePeers + " remote Peer(s) left in the room.";
        toastLog(TAG, context, log);
    }

    @Override
    public void onRemotePeerConnectionRefreshed(String remotePeerId, Object userData, boolean hasDataChannel, boolean wasIceRestarted) {
        String peer = "Skylink Media Relay server";
        if (remotePeerId != null) {
            peer = "Peer " + Utils.getPeerIdNick(remotePeerId);
        }
        String log = "Your connection with " + peer + " has just been refreshed";
        if (wasIceRestarted) {
            log += ", with ICE restarted.";
        } else {
            log += ".\r\n";
        }

        UserInfo remotePeerUserInfo = skylinkConnection.getUserInfo(remotePeerId);
        log += "isAudioStereo:" + remotePeerUserInfo.isAudioStereo() + ".\r\n" +
                "video height:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video width:" + remotePeerUserInfo.getVideoHeight() + ".\r\n" +
                "video frameRate:" + remotePeerUserInfo.getVideoFps() + ".";
        toastLog(TAG, context, log);
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        // If Peer has no userData, use an empty string for nick.
        String nick = "";
        if (userData != null) {
            nick = userData.toString();
        }
        String log = "[SA][onRemotePeerUserDataReceive] Peer " + Utils.getPeerIdNick(remotePeerId) +
                ":\n" + nick;
        toastLog(TAG, context, log);
    }

    @Override
    public void onOpenDataConnection(String peerId) {
        Log.d(TAG, "onOpenDataConnection");
    }

}