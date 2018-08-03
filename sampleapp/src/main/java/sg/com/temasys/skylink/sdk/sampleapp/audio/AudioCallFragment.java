package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
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

    //this variable need to be static for configuration change
    private static AudioCallContract.Presenter mPresenter;

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

        requestViewLayout(true);

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
    public void onDetach() {
        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        // in case of changing screen orientation, do not close the connection
        if (!((AudioCallActivity) mContext).isChangingConfigurations()) {
            requestViewLayout(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        mPresenter.onRequestPermissionsResultPresenterHandler(requestCode, permissions, grantResults, TAG);
    }

    @Override
    public Fragment getFragmentViewHandler() {
        return this;
    }

    @Override
    public void setRoomDetailsViewHandler(String roomDetails) {
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
     */
    private void requestViewLayout(boolean tryToConnect){
        if(mPresenter != null){
            mPresenter.onViewLayoutRequestedPresenterHandler(tryToConnect);
        }
    }
}
