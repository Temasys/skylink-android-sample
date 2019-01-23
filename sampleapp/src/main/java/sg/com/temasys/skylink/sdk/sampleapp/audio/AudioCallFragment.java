package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;

/**
 * A simple {@link CustomActionBar} subclass.
 * This class is responsible for display UI and get user interaction
 */
public class AudioCallFragment extends CustomActionBar implements AudioCallContract.View, View.OnClickListener {

    private final String TAG = AudioCallFragment.class.getName();

    // The view widgets
    private LinearLayout llTool;
    private ImageButton btnAudioSpeaker, btnAudioEnd;
    private ImageView img;

    // presenter instance to implement app logic
    private AudioCallContract.Presenter mPresenter;

    public static AudioCallFragment newInstance() {
        return new AudioCallFragment();
    }

    @Override
    public void setPresenter(AudioCallContract.Presenter presenter) {
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
        ((AudioCallActivity) context).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[SA][Audio][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_audio_call, container, false);

        // get the UI controls from layout
        getControlWidgets(rootView);

        // setup the action bar
        setActionBar();

        // init values for view controls
        initControls();

        //request an initiative connection
        requestViewLayout();

        return rootView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        // delegate presenter to implement the permission results
        mPresenter.onViewRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //when changing configuration, do change layout view to fit with screen
        changeLayout(newConfig.orientation);
    }

    @Override
    public void onClick(View view) {
        //Defining a click event listener for the buttons in the action bar.
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
            case R.id.btnAudioSpeaker:
                mPresenter.onViewRequestChangeAudioOuput();
                break;
            case R.id.btnAudioEnd:
                processEndAudio();
                break;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        // in case of changing screen orientation, do not close the connection
        if (!((AudioCallActivity) context).isChangingConfigurations()) {
            mPresenter.onViewRequestExit();
        }
    }

    //----------------------------------------------------------------------------------------------
    // Methods called from the Presenter to update UI
    //----------------------------------------------------------------------------------------------

    /**
     * Get the instance of the view for implementing runtime permission
     */
    @Override
    public Fragment onPresenterRequestGetFragmentInstance() {
        return this;
    }

    /**
     * Update information about room id on the action bar
     *
     * @param roomId id of the room that is generated by the SDK when connected to room
     */
    public void onPresenterRequestUpdateRoomInfo(String roomId) {
        updateRoomInfo(roomId);
    }

    /**
     * Show local peer button and display local avatar by the first character of the local username
     *
     * @param localUserName the default local username for audio in setting
     */
    @Override
    public void onPresenterRequestUpdateUIConnected(String localUserName) {
        // update the local peer button in the action bar
        updateUILocalPeer(localUserName);

        // update the image to show local peer in the room
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            img.setImageDrawable(context.getDrawable(R.drawable.ic_local_32));
        }
    }

    /**
     * Update information about new remote peer joining the room at a specific index
     *
     * @param newPeer remote peer joining the room
     * @param index   specific index
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer, int index) {
        //add new remote peer button in the action bar
        updateUiRemotePeerJoin(newPeer, index);

        // update the image to show peers in the room
        // there are 2 peers in room (including local peer) according to the config
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            img.setImageDrawable(context.getDrawable(R.drawable.ic_peers_32));
        }
    }

    /**
     * Display information about remote peer left from the room
     * refill the left peer(s) in the buttons to make sure the peer displayed in correct order
     *
     * @param peersList the list of left peer(s) in the room
     */
    @Override
    public void onPresenterRequestChangeUIRemotePeerLeft(List<SkylinkPeer> peersList) {
        // re fill the peers buttons in the action bar to show the peer correctly order
        processFillPeers(peersList);

        // update the image to show only local peer left in the room
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            img.setImageDrawable(context.getDrawable(R.drawable.ic_local_32));
        }
    }

    /**
     * Update button UI when audio output state changed
     *
     * @param isSpeakerOn the state of speaker {on/off}
     */
    @Override
    public void onPresenterRequestChangeAudioOutput(boolean isSpeakerOn) {

        //change the button background and icon
        Drawable backgroundSrcBtn = null;

        if (isSpeakerOn) {
            btnAudioSpeaker.setBackground(context.getResources().getDrawable(R.drawable.button_circle_press));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrcBtn = context.getResources().getDrawable(R.drawable.ic_audio_speaker, null);
            }
        } else {
            btnAudioSpeaker.setBackground(context.getResources().getDrawable(R.drawable.button_circle));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrcBtn = context.getResources().getDrawable(R.drawable.icon_speaker_mute, null);
            }
        }

        if (backgroundSrcBtn != null)
            btnAudioSpeaker.setImageDrawable(backgroundSrcBtn);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Get the widget controls from layout
     */
    private void getControlWidgets(View rootView) {
        llTool = rootView.findViewById(R.id.ll_tool);
        btnAudioSpeaker = rootView.findViewById(R.id.btnAudioSpeaker);
        btnAudioEnd = rootView.findViewById(R.id.btnAudioEnd);
        img = rootView.findViewById(R.id.img);
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private void setActionBar() {
        ActionBar actionBar = ((AudioCallActivity) getActivity()).getSupportActionBar();
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

    /**
     * Init the view widgets for the fragment
     */
    private void initControls() {
        // init setting value for room name in action bar
        txtRoomName.setText(Config.ROOM_NAME_AUDIO);

        // set onClick event for buttons in layout
        btnBack.setOnClickListener(this);
        btnLocalPeer.setOnClickListener(this);
        btnRemotePeer1.setOnClickListener(this);
        btnRemotePeer2.setOnClickListener(this);
        btnRemotePeer3.setOnClickListener(this);
        btnAudioSpeaker.setOnClickListener(this);
        btnAudioEnd.setOnClickListener(this);

        // show the connecting animation
        AnimationDrawable backgroundSrc = (AnimationDrawable) context.getResources().getDrawable(R.drawable.img_blink);
        if (backgroundSrc != null) {
            img.setImageDrawable(backgroundSrc);

            AnimationDrawable frameAnimation = (AnimationDrawable) img.getDrawable();
            if (frameAnimation != null)
                frameAnimation.start();
        }
    }

    /**
     * request init connection at the first time
     * try to connect to room if not connected
     */
    private void requestViewLayout() {
        if (mPresenter != null) {
            mPresenter.onViewRequestConnectedLayout();
        }

        //changing layout to fit with screen
        changeLayout(getResources().getConfiguration().orientation);
    }

    /**
     * changing the view layout when changing screen orientation
     *
     * @param orientation portrait or landscape mode
     */
    private void changeLayout(int orientation) {
        // Setting different bottom margins to different screen to have a better UI
        LinearLayout.LayoutParams llParams = (LinearLayout.LayoutParams) llTool.getLayoutParams();
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            llParams.bottomMargin = (int) context.getResources().getDimension(R.dimen.dp_50dp);
        } else {
            llParams.bottomMargin = (int) context.getResources().getDimension(R.dimen.dp_5dp);
        }

        llTool.setLayoutParams(llParams);
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
     * process closing the connection and activity when ending the audio call
     */
    private void processEndAudio() {

        // Inform the presenter to implement closing the connection
        mPresenter.onViewRequestExit();

        //close UI
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
