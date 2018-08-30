package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class AudioCallFragment extends Fragment implements AudioCallContract.View {

    private final String TAG = AudioCallFragment.class.getName();

    private Context mContext;

    private LinearLayout ll_tool;

    private TextView tvRoomDetails;

    private ImageButton btnAudioSpeaker, btnAudioEnd;

    private ImageView img;

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

        initControls();

        requestViewLayout();

        btnAudioSpeaker.setOnClickListener(view -> processChangeAudioToSpeaker());

        btnAudioEnd.setOnClickListener(view -> processEndAudio());

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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        LinearLayout.LayoutParams llParams = (LinearLayout.LayoutParams) ll_tool.getLayoutParams();
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            llParams.bottomMargin = (int) mContext.getResources().getDimension(R.dimen.dp_50dp);
        } else {
            llParams.bottomMargin = (int) mContext.getResources().getDimension(R.dimen.dp_5dp);
        }

        ll_tool.setLayoutParams(llParams);

    }

    @Override
    public Fragment onGetFragment() {
        return this;
    }

    @Override
    public void onUpdateUI(String roomDetails, boolean isPeerJoined) {
        tvRoomDetails.setText(roomDetails);

        AnimationDrawable frameAnimation = (AnimationDrawable) img.getDrawable();
        if (isPeerJoined) {
            frameAnimation.start();
        } else {
            frameAnimation.stop();
        }
    }

    @Override
    public void onChangeBtnAudioSpeakerUI(boolean isSpeakerOn) {
        //change the button background and icon
        if (isSpeakerOn) {
            btnAudioSpeaker.setBackground(mContext.getResources().getDrawable(R.drawable.button_circle_press));
            Drawable backgroundSrc = mContext.getResources().getDrawable(R.drawable.ic_audio_speaker, null);
            btnAudioSpeaker.setImageDrawable(backgroundSrc);
        } else {
            btnAudioSpeaker.setBackground(mContext.getResources().getDrawable(R.drawable.button_circle));
            Drawable backgroundSrc = mContext.getResources().getDrawable(R.drawable.icon_speaker_mute, null);
            btnAudioSpeaker.setImageDrawable(backgroundSrc);
        }
    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        ll_tool = rootView.findViewById(R.id.ll_tool);
        tvRoomDetails = rootView.findViewById(R.id.tv_audio_room_details);
        btnAudioSpeaker = rootView.findViewById(R.id.btnAudioSpeaker);
        btnAudioEnd = rootView.findViewById(R.id.btnAudioEnd);
        img = rootView.findViewById(R.id.img);
    }

    private void setActionBar() {
        ActionBar actionBar = ((AudioCallActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        setHasOptionsMenu(true);
    }

    private void initControls() {
    }

    /**
     * request info to display from presenter
     * try to connect to room if not connected
     */
    private void requestViewLayout() {
        if (mPresenter != null) {
            mPresenter.onViewLayoutRequested();
        }

        LinearLayout.LayoutParams llParams = (LinearLayout.LayoutParams) ll_tool.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            llParams.bottomMargin = (int) mContext.getResources().getDimension(R.dimen.dp_50dp);
        } else {
            llParams.bottomMargin = (int) mContext.getResources().getDimension(R.dimen.dp_5dp);
        }

        ll_tool.setLayoutParams(llParams);
    }

    private void processChangeAudioToSpeaker() {
        //change audio output : speaker or headset
        mPresenter.onChangeAudioToSpeaker();
    }

    private void processEndAudio() {
        mPresenter.onViewExit();

        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
