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

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.VideoResButton;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * A simple {@link CustomActionBar} subclass.
 * This class is responsible for display UI and get user interaction
 */
public class VideoCallFragment extends CustomActionBar implements VideoCallContract.View,
        View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private final String TAG = VideoCallFragment.class.getName();

    // view widgets
    private LinearLayout linearLayout;
    private FloatingActionButton btnDisconnect, btnAudioMute, btnVideoMute, btnCameraMute, btnSpeaker;
    private Button btnLocalOption;
    private VideoResButton btnVideoResolution;

    // presenter instance to implement video call logic
    private VideoCallContract.Presenter mPresenter;

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
        mPresenter.onViewRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // only exit/disconnect from room when it is chosen by user
        // not changing configuration
        if (!((VideoCallActivity) context).isChangingConfigurations()) {
            mPresenter.onViewRequestExit();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Changing video custom buttons position to have a better view when screen orientation changed
        if (newConfig.orientation == ORIENTATION_PORTRAIT) {
            changeFloatingButtons(false);
        } else {
            changeFloatingButtons(true);
        }
    }

    @Override
    public void onClick(View view) {
        //Defining a click event actions for the buttons
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
            case R.id.btnVideoResolution:
                onMenuOptionVideoResolution(true);
                break;
            case R.id.ll_video_call:
                onMenuOptionVideoResolution(false);
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
    // Methods invoked from the Presenter to update UI
    //----------------------------------------------------------------------------------------------

    /**
     * Update UI into connected state when connected to room
     * Show room id and local peer button and display local avatar by the first character of the local username
     *
     * @param roomId the id of the connected room that generated by SDK
     */
    @Override
    public void onPresenterRequestUpdateUIConnected(String roomId) {
        updateRoomInfo(roomId);
        // update the local peer avatar with the user name configured in default setting
        updateUILocalPeer(Config.USER_NAME_VIDEO);
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
     * Re-fill the peers list in order to display correct order of peers in room
     *
     * @param peersList the list of left peer(s) in the room
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerLeft(List<SkylinkPeer> peersList) {
        processFillPeers(peersList);
    }

    /**
     * Change certain UI elements when trying to connect to room, but not connected
     */
    @Override
    public void onPresenterRequestConnectingUIChange() {
        btnAudioMute.setVisibility(GONE);
        btnVideoMute.setVisibility(GONE);
        btnCameraMute.setVisibility(GONE);
        btnDisconnect.setVisibility(VISIBLE);
    }

    /**
     * Change certain UI elements once connected to room
     */
    @Override
    public void onPresenterRequestConnectedUIChange() {
        btnAudioMute.setVisibility(VISIBLE);
        btnVideoMute.setVisibility(VISIBLE);
        btnCameraMute.setVisibility(VISIBLE);
        btnDisconnect.setVisibility(VISIBLE);
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
        btnCameraMute.setVisibility(GONE);
        btnDisconnect.setVisibility(GONE);

        btnSpeaker.setVisibility(GONE);
        btnAudioMute.setVisibility(GONE);
        btnVideoMute.setVisibility(GONE);
        btnCameraMute.setVisibility(GONE);
        btnDisconnect.setVisibility(GONE);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    /**
     * Get the instance of the view for implementing runtime permission
     */
    @Override
    public Fragment onPresenterRequestGetFragmentInstance() {
        return this;
    }

    /**
     * Add or update our self VideoView into the view layout when local peer connected to room and
     * local video view is ready
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
     * Add or update remote Peer's VideoView into the view layout when remote peer joined the room
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

        // Add new peer video and add tag
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
     * Remove remote video view when remote peer left or local peer exit the room
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
     * Update the audio output/speaker button when being requested from presenter
     * For example: when the phone connects to a bluetooth headset, the speaker is automatically off
     */
    @Override
    public void onPresenterRequestChangeAudioOuput(boolean isSpeakerOn) {
        if (isSpeakerOn) {
            btnSpeaker.setImageResource(R.drawable.ic_audio_speaker);
        } else {
            btnSpeaker.setImageResource(R.drawable.icon_speaker_mute);
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
            btnCameraMute.setImageResource(R.drawable.icon_camera_mute);

        } else {
            btnCameraMute.setImageResource(R.drawable.icon_camera_active);
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

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        linearLayout = rootView.findViewById(R.id.ll_video_call);
        btnSpeaker = rootView.findViewById(R.id.toggle_speaker);
        btnAudioMute = rootView.findViewById(R.id.toggle_audio);
        btnVideoMute = rootView.findViewById(R.id.toggle_video);
        btnCameraMute = rootView.findViewById(R.id.toggle_camera);
        btnDisconnect = rootView.findViewById(R.id.disconnect);
        btnLocalOption = rootView.findViewById(R.id.btnLocalPeerOption);
        btnVideoResolution = rootView.findViewById(R.id.btnVideoResolution);
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private void setActionBar() {
        ActionBar actionBar = ((VideoCallActivity) getActivity()).getSupportActionBar();
        super.setActionBar(actionBar);
    }

    /**
     * Init value for view components
     */
    private void initComponents(View rootView) {
        // set onClick event for buttons in layout
        btnBack.setOnClickListener(this);
        btnLocalPeer.setOnClickListener(this);
        btnRemotePeer1.setOnClickListener(this);
        btnRemotePeer2.setOnClickListener(this);
        btnRemotePeer3.setOnClickListener(this);
        btnLocalOption.setOnClickListener(this);
        btnVideoResolution.setOnClickListener(this);
        btnSpeaker.setOnClickListener(this);
        btnAudioMute.setOnClickListener(this);
        btnVideoMute.setOnClickListener(this);
        btnCameraMute.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);
        linearLayout.setOnClickListener(this);

        // init setting value for room name in action bar
        txtRoomName.setText(Config.ROOM_NAME_VIDEO);

        // Set init audio/video state
        setAudioBtnLabel(false, false);
        setVideoBtnLabel(false, false);

        // make floating buttons
        // changing button positions base on the screen orientation
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            changeFloatingButtons(false);
        } else {
            changeFloatingButtons(true);
        }
    }

    /**
     * request info to display on view from presenter
     * try to connect to room if not connected
     */
    private void requestViewLayout() {
        if (mPresenter != null) {
            mPresenter.onViewRequestConnectedLayout();
        }
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

    /**
     * Changing view layout base on the screen orientation
     */
    private void changeFloatingButtons(boolean isLandscapeMode) {
        if (!isLandscapeMode) {
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            changeFloatingButtonPortrait(btnDisconnect);
            changeFloatingButtonPortrait(btnCameraMute);
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

            changeFloatingButtonLandscape(btnDisconnect);
            changeFloatingButtonLandscape(btnCameraMute);
            changeFloatingButtonLandscape(btnVideoMute);
            changeFloatingButtonLandscape(btnAudioMute);
            changeFloatingButtonLandscape(btnSpeaker);

        }
    }

    /**
     * Changing floating buttons position in Portrait mode
     * All floating buttons will be located in the right of the layout
     */
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
        } else if (btn == btnCameraMute) {
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
     * Changing floating buttons position in Landscape mode
     * All floating buttons will be located in the left of the layout / on the local video side
     */
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
        } else if (btn == btnCameraMute) {
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

    /**
     * Handle click/unclick event of btnVideoResolution
     * if user clicks on this button, show the video resolution dialog
     * and hide the dialog when it is unclicked
     *
     * @param isFromButton flag to check the click event comes from button clicked or not
     *                     if from the button, we will show/hide the resolution fragment normally
     *                     if not from the button like user click outside the video resolution fragment
     *                     to hide the video resolution, only hide the video res fragment
     */
    private void onMenuOptionVideoResolution(boolean isFromButton) {
        // change the button background
        VideoResButton.ButtonState state = btnVideoResolution.getState();
        if (state == VideoResButton.ButtonState.NORMAL && isFromButton) {
            btnVideoResolution.setState(VideoResButton.ButtonState.CLICKED);
            // Show the video resolution fragment
            ((VideoCallActivity) getActivity()).onShowHideVideoResFragment(true);
        } else if (state == VideoResButton.ButtonState.CLICKED) {
            btnVideoResolution.setState(VideoResButton.ButtonState.NORMAL);
            // Hide the video resolution fragment
            ((VideoCallActivity) getActivity()).onShowHideVideoResFragment(false);
        }
    }
}
