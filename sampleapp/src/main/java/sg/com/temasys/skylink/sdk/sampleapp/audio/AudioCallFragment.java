package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.AudioRemotePeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;


/**
 * A simple {@link Fragment} subclass.
 */
public class AudioCallFragment extends Fragment implements AudioCallContract.View {

    private final String TAG = AudioCallFragment.class.getName();

    // Constants for configuration change
    private final String BUNDLE_PEER = "REMOTE_PEER";

    private Context mContext;

    private TextView tvRoomDetails;
    private Button btnAudioCall;

    private static AudioCallContract.Presenter mPresenter;

    private AudioRemotePeer audioRemotePeer;

    private PermissionUtils permissionUtils;


    public static AudioCallFragment newInstance() {
        return new AudioCallFragment();
    }

    @Override
    public void setPresenter(AudioCallContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_audio_call, container, false);

        getControlWidgets(rootView);

        setActionBar();

        permissionUtils = new PermissionUtils(mContext);

        // Check if it was an orientation change
        if (savedInstanceState != null) {
            permissionUtils.permQResume(mContext, this);

            // Set the appropriate UI if already isConnected().
            if (mPresenter.isConnectingOrConnectedPresenterHandler()) {
                audioRemotePeer = (AudioRemotePeer) savedInstanceState.getSerializable(BUNDLE_PEER);

                // Set the appropriate UI if already isConnected().
                onConnectUIChangeViewHandler();
            } else {
                disconnectUIChangeViewHandler();
            }
        } else {
            // This is the start of this sample, reset permission request states.
            permissionUtils.permQReset();
        }

        btnAudioCall.setOnClickListener(v -> {
            connectToRoomViewHandler();
            onConnectUIChangeViewHandler();
        });
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Allow volume to be controlled using volume keys
        ((AudioCallActivity) mContext).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save states for fragment restart
        outState.putSerializable(BUNDLE_PEER, audioRemotePeer);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        disconnectFromRoomViewHandler();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        permissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, TAG);
    }

    //----------------------------------------------------------------------------------------------
    // View Listeners to update GUI from presenter
    //----------------------------------------------------------------------------------------------

    @Override
    public void onDisconnectUIChangeViewHandler() {
        disconnectUIChangeViewHandler();
    }

    @Override
    public Fragment getFragmentViewHandler() {
        return this;
    }

    @Override
    public void setRoomDetailsViewHandler(String roomDetails) {
        setTvRoomDetails(roomDetails);
    }

    @Override
    public void setAudioRemotePeerViewHandler(AudioRemotePeer audioRemotePeer) {
        this.audioRemotePeer = audioRemotePeer;
    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_room_details);
        btnAudioCall = (Button) rootView.findViewById(R.id.btn_audio_call);
    }

    private void setActionBar() {
        ActionBar actionBar = ((AudioCallActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        setHasOptionsMenu(true);
    }

    private void connectToRoomViewHandler() {
        mPresenter.connectToRoomPresenterHandler();
    }

    private void disconnectFromRoomViewHandler() {
        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        if (!((AudioCallActivity) mContext).isChangingConfigurations()) {
            mPresenter.disconnectFromRoomPresenterHandler();
        }
    }

    /**
     * Change certain UI elements once isConnected() to room or when Peer(s) join or leave.
     */
    private void onConnectUIChangeViewHandler() {
        btnAudioCall.setEnabled(false);
        setTvRoomDetails();
    }

    /**
     * Change certain UI elements when disconnecting from room.
     */
    private void disconnectUIChangeViewHandler() {
        btnAudioCall.setEnabled(true);
        setTvRoomDetails();
    }

    /**
     * Set the room details on UI.
     */
    private void setTvRoomDetails() {
        boolean isPeerJoined = audioRemotePeer == null ? false : audioRemotePeer.isPeerJoined();

        String roomDetails = mPresenter.getRoomDetailsPresenterHandler(isPeerJoined);

        tvRoomDetails.setText(roomDetails);
    }

    /**
     * Set the room details on UI.
     */
    private void setTvRoomDetails(String roomDetails) {
        tvRoomDetails.setText(roomDetails);
    }

}
