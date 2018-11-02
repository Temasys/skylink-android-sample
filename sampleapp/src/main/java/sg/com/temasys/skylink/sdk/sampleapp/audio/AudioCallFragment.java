package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class AudioCallFragment extends Fragment implements AudioCallContract.View {

    private final String TAG = AudioCallFragment.class.getName();

    private Context mContext;

    private LinearLayout llTool;

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

        requestViewLayout();

        // Defining a click event listener for the button "Audio Speaker"
        btnAudioSpeaker.setOnClickListener(view -> mPresenter.onViewRequestChangeAudioOuput());

        // Defining a click event listener for the button "End call"
        btnAudioEnd.setOnClickListener(view -> processEndAudio());

        return rootView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        mPresenter.onViewRequestPermissionsResult(requestCode, permissions, grantResults, TAG);
    }

    @Override
    public void onStop() {
        super.onStop();

        mPresenter.onViewRequestStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        mPresenter.onViewRequestResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        // in case of changing screen orientation, do not close the connection
        if (!((AudioCallActivity) mContext).isChangingConfigurations()) {
            mPresenter.onViewRequestExit();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //when changing configuration, do change layout view to fit with screen
        changeLayout(newConfig.orientation);
    }

    @Override
    public Fragment onPresenterRequestGetFragmentInstance() {
        return this;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPresenterRequestUpdateUI(String roomDetails, boolean isPeerJoined, boolean isSpeakerOn) {

        //change room detail info
        tvRoomDetails.setText(roomDetails);

        //change img animation
        AnimationDrawable frameAnimation = null;
        if (isPeerJoined) {
            AnimationDrawable backgroundSrc = (AnimationDrawable) mContext.getResources().getDrawable(R.drawable.img_blink);
            if (backgroundSrc != null) {
                img.setImageDrawable(backgroundSrc);

                frameAnimation = (AnimationDrawable) img.getDrawable();
                if (frameAnimation != null)
                    frameAnimation.start();
            }
        } else {
            if (frameAnimation != null)
                frameAnimation.stop();

            Drawable backgroundSrc = null;
            if (isSpeakerOn)
                backgroundSrc = mContext.getResources().getDrawable(R.drawable.speaker_image, null);
            else
                backgroundSrc = mContext.getResources().getDrawable(R.drawable.headset_image, null);

            if (backgroundSrc != null)
                img.setImageDrawable(backgroundSrc);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPresenterRequestChangeAudioOutput(boolean isPeerJoined, boolean isSpeakerOn) {

        //change the button background and icon
        Drawable backgroundSrcBtn = null;
        Drawable backgroundSrcImg = null;

        if (isSpeakerOn) {
            btnAudioSpeaker.setBackground(mContext.getResources().getDrawable(R.drawable.button_circle_press));
            backgroundSrcBtn = mContext.getResources().getDrawable(R.drawable.ic_audio_speaker, null);
            backgroundSrcImg = mContext.getResources().getDrawable(R.drawable.speaker_image, null);
        } else {
            btnAudioSpeaker.setBackground(mContext.getResources().getDrawable(R.drawable.button_circle));
            backgroundSrcBtn = mContext.getResources().getDrawable(R.drawable.icon_speaker_mute, null);
            backgroundSrcImg = mContext.getResources().getDrawable(R.drawable.headset_image, null);
        }

        if (backgroundSrcBtn != null)
            btnAudioSpeaker.setImageDrawable(backgroundSrcBtn);

        if (backgroundSrcImg != null && !isPeerJoined)
            img.setImageDrawable(backgroundSrcImg);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        llTool = rootView.findViewById(R.id.ll_tool);
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

    /**
     * request info to display from presenter
     * try to connect to room if not connected
     * try to update UI if connected to room
     */
    private void requestViewLayout() {
        if (mPresenter != null) {
            mPresenter.onViewRequestConnectedLayout();
        }

        //changing layout to fit with screen
        changeLayout(getResources().getConfiguration().orientation);
    }

    private void processEndAudio() {

        //end connection
        mPresenter.onViewRequestExit();

        //close UI
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void changeLayout(int orientation) {

        LinearLayout.LayoutParams llParams = (LinearLayout.LayoutParams) llTool.getLayoutParams();
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            llParams.bottomMargin = (int) mContext.getResources().getDimension(R.dimen.dp_50dp);
        } else {
            llParams.bottomMargin = (int) mContext.getResources().getDimension(R.dimen.dp_5dp);
        }

        llTool.setLayoutParams(llParams);

    }
}
