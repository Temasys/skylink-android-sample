package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import sg.com.temasys.skylink.sdk.sampleapp.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class AudioCallFragment extends Fragment implements AudioCallContract.View {

    private final String TAG = AudioCallFragment.class.getName();

    private Context mContext;

    private TextView tvRoomDetails;

    private AudioCallContract.Presenter mPresenter;

    public static AudioCallFragment newInstance() {
        return new AudioCallFragment();
    }

    @Override
    public void setPresenter(AudioCallContract.Presenter presenter) {
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
        ((AudioCallActivity) mContext).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[SA][Audio][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_audio_call, container, false);

        getControlWidgets(rootView);

        setActionBar();

        requestViewLayout();

        return rootView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        mPresenter.onRequestPermissionsResult(requestCode, permissions, grantResults, TAG);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        // in case of changing screen orientation, do not close the connection
        if (!((AudioCallActivity) mContext).isChangingConfigurations()) {
            mPresenter.onViewExit();
        }
    }

    @Override
    public Fragment onGetFragment() {
        return this;
    }

    @Override
    public void onUpdateUI(String roomDetails) {
        tvRoomDetails.setText(roomDetails);
    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_audio_room_details);
    }

    private void setActionBar() {
        ActionBar actionBar = ((AudioCallActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        setHasOptionsMenu(true);
    }

    /**
     * request info to display from presenter
     * try to connect to room if not connected
     * try to disconnect from room if left the room
     */
    private void requestViewLayout(){
        if(mPresenter != null){
            mPresenter.onViewLayoutRequested();
        }
    }
}
