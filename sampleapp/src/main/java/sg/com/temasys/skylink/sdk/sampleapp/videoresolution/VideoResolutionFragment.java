package sg.com.temasys.skylink.sdk.sampleapp.videoresolution;

import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoActivity;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomSeekBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomTriangleButton;

/**
 * A simple {@link Fragment} subclass.
 * <p>
 * This fragment is responsible to display view related to video resolutions
 * It can be attached on any activities or fragments that have video views and want to display
 * their video resolutions
 * <p>
 * Created by muoi.pham on 27/02/19.
 */
public class VideoResolutionFragment extends android.support.v4.app.Fragment implements VideoResolutionContract.View {

    private final String TAG = VideoResolutionFragment.class.getName();

    private RelativeLayout frameVideoRes;
    private CustomSeekBar seekBarWidthHeight, seekBarFps;
    private TextView txtMinWH, txtMaxWH, txtMinFps, txtMaxFps, txtInputWH, txtSentWH, txtRecceivedWH;
    private SeekBar.OnSeekBarChangeListener seekBarChangeListenerResDim, seekBarChangeListenerResFps;
    private ImageButton btnGetVideoRes, btnVideoScreen, btnVideoCamera;

    // presenter instance to implement video res logic
    private VideoResolutionContract.Presenter presenter;

    public VideoResolutionFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment
     *
     * @return A new instance of fragment VideoResolutionFragment.
     */
    public static VideoResolutionFragment newInstance() {
        VideoResolutionFragment fragment = new VideoResolutionFragment();
        return fragment;
    }

    @Override
    public void setPresenter(VideoResolutionContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public void resetResolution() {
        txtMinWH.setText("widthxheight");
        txtMaxWH.setText("widthxheight");
        txtMinFps.setText("Fps");
        txtMaxFps.setText("Fps");
        txtInputWH.setText("widthxheight");
        txtSentWH.setText("widthxheight");
        txtRecceivedWH.setText("widthxheight");

        // init seekbars
        seekBarWidthHeight.setType(CustomSeekBar.Seekbar_Type.WIDTH_HEIGHT);
        seekBarFps.setType(CustomSeekBar.Seekbar_Type.FPS);
        seekBarWidthHeight.setCurrentWidthHeight("WidthxHeight");
        seekBarFps.setCurrentWidthHeight("Fps");

        seekBarChangeListenerResDim = getSeekBarChangeListenerDim();
        seekBarWidthHeight.setOnSeekBarChangeListener(seekBarChangeListenerResDim);

        seekBarChangeListenerResFps = getSeekBarChangeListenerFps();
        seekBarFps.setOnSeekBarChangeListener(seekBarChangeListenerResFps);
    }

    //----------------------------------------------------------------------------------------------
    // Fragment life cycle methods
    //----------------------------------------------------------------------------------------------

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[SA][VideoResolution][onCreateView] ");

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_video_resolution, container, false);

        getControlWidgets(rootView);

        init();

        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    //----------------------------------------------------------------------------------------------
    // Methods called from the Presenter to update UI
    //----------------------------------------------------------------------------------------------

    /**
     * Update the value of TextView for local input video resolution
     */
    @Override
    public void onPresenterRequestUpdateUiResInput(VideoResolution videoInput) {
        setUiResTvStats(videoInput, txtInputWH);
    }

    /**
     * Update the value of TextView for local sent video resolution
     */
    @Override
    public void onPresenterRequestUpdateUiResSent(VideoResolution videoSent) {
        setUiResTvStats(videoSent, txtSentWH);
    }

    /**
     * Update the value of TextView for received remote video resolution
     */
    @Override
    public void onPresenterRequestUpdateUiResReceive(VideoResolution videoReceive) {
        setUiResTvStats(videoReceive, txtRecceivedWH);
    }

    /**
     * Update the value of TextView when changing video resolution width x height Seek bar
     */
    @Override
    public boolean onPresenterRequestUpdateUiResDimInfo(int width, int height) {
        return setUiResTvDim(width, height);
    }

    /**
     * Update the value of TextView when changing video resolution frame rate Seek bar
     */
    @Override
    public void onPresenterRequestUpdateUiResFpsInfo(int fps) {
        setUiResTvFps(fps);
    }

    /**
     * Update the max range of the width x height resolution seek bar due to current camera
     *
     * @param maxDimRange
     */
    @Override
    public void onPresenterRequestUpdateUiResRangeDimInfo(int maxDimRange, String minDimValue, String maxDimValue) {
        // update max rang on seekbar
        seekBarWidthHeight.setMaxRange(maxDimRange);
        // update max/min width x height textviews
        txtMaxWH.setText(maxDimValue);
        txtMinWH.setText(minDimValue);
    }

    /**
     * Update the max range of the frame rate resolution seek bar due to current camera
     *
     * @param maxFpsRange
     */
    @Override
    public void onPresenterRequestUpdateUiResRangeFpsInfo(int maxFpsRange, int minFpsValue, int maxFpsValue) {
        // update max rang on seekbar
        seekBarFps.setMaxRange(maxFpsRange);
        // update min/max width x height textview
        txtMinFps.setText(String.valueOf(minFpsValue));
        txtMaxFps.setText(String.valueOf(maxFpsRange));
    }

    /**
     * Update the UI when changing width x height video resolution.
     * Update on both the seek bar and the text view
     */
    @Override
    public void onPresenterRequestUpdateResDimInfo(int index, int width, int height) {
        // Set the SeekBar
        seekBarWidthHeight.setProgress(index);
        // Set TextView
        setUiResTvDim(width, height);
    }

    /**
     * Update the UI when changing frame rate video resolution.
     * Update on both the seek bar and the text view
     */
    @Override
    public void onPresenterRequestUpdateResFpsInfo(int index, int fps) {
        // Set the SeekBar
        seekBarFps.setProgress(index);
        // Set TextView
        setUiResTvFps(fps);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        frameVideoRes = rootView.findViewById(R.id.frameVideoRes);
        seekBarWidthHeight = rootView.findViewById(R.id.seekBarResWidthHeight);
        seekBarFps = rootView.findViewById(R.id.seekBarResFps);
        txtMinWH = rootView.findViewById(R.id.txtMinRes);
        txtMaxWH = rootView.findViewById(R.id.txtMaxRes);
        txtMinFps = rootView.findViewById(R.id.txtMinFps);
        txtMaxFps = rootView.findViewById(R.id.txtMaxFps);
        txtInputWH = rootView.findViewById(R.id.txtResInput);
        txtSentWH = rootView.findViewById(R.id.txtResSent);
        txtRecceivedWH = rootView.findViewById(R.id.txtResReceived);
        btnGetVideoRes = rootView.findViewById(R.id.btnGetVideoRes);
        btnVideoScreen = rootView.findViewById(R.id.btnVideoScreen);
        btnVideoCamera = rootView.findViewById(R.id.btnVideoCamera);
    }

    private void init() {
        if (getDirection() == CustomTriangleButton.ButtonDirection.TOP_LEFT) {
            frameVideoRes.setBackgroundResource(R.drawable.frame_layout_round_border_topleft);
        } else if (getDirection() == CustomTriangleButton.ButtonDirection.TOP_RIGHT) {
            frameVideoRes.setBackgroundResource(R.drawable.frame_layout_round_border_topright);
        }

        resetResolution();

        btnGetVideoRes.setOnClickListener(view -> presenter.onViewRequestGetVideoResolutions());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnGetVideoRes.setTooltipText("Get video resolution");
        }

        btnVideoCamera.setBackground(getResources().getDrawable(R.drawable.button_circle_trans_selected));
        btnVideoCamera.setOnClickListener(view -> {
            presenter.onViewRequestChooseVideoCamera();
            btnVideoCamera.setBackground(getResources().getDrawable(R.drawable.button_circle_trans_selected));
            btnVideoScreen.setBackground(getResources().getDrawable(R.drawable.button_circle_trans));
        });

        btnVideoScreen.setOnClickListener(view -> {
            presenter.onViewRequestChooseVideoScreen();
            btnVideoCamera.setBackground(getResources().getDrawable(R.drawable.button_circle_trans));
            btnVideoScreen.setBackground(getResources().getDrawable(R.drawable.button_circle_trans_selected));
        });
    }

    /**
     * Set the value of TextView for local input video resolution/sent video resolution/received video resolution.
     * If any parameters are invalid, set default text.
     *
     * @param textView
     * @param videoResolution
     */
    private void setUiResTvStats(VideoResolution videoResolution, TextView textView) {
        if (textView == null)
            return;

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
     * @param width  video width
     * @param height video height
     * @return True if inputs are valid, false otherwise.
     */
    private boolean setUiResTvDim(int width, int height) {
        if (width <= 0 || height <= 0) {
            seekBarWidthHeight.setCurrentWidthHeight("N/A");
            return false;
        }
        // Set textView to match
        seekBarWidthHeight.setCurrentWidthHeight(Utils.getResDimStr(width, height));
        return true;
    }

    /**
     * Set the value of TextView tvResFps.
     * If input is invalid, set default text.
     *
     * @param fps frames per second.
     * @return True if inputs are valid, false otherwise.
     */
    private boolean setUiResTvFps(int fps) {
        if (fps < 0) {
            seekBarFps.setCurrentFps("N/A");
            return false;
        }
        // Set textView to match
        seekBarFps.setCurrentFps(String.valueOf(fps));
        return true;
    }

    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListenerDim() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                presenter.onViewRequestDimProgressChanged(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                presenter.onViewRequestDimSelected(seekBar.getProgress());
            }
        };
    }

    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListenerFps() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                presenter.onViewRequestFpsProgressChanged(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                presenter.onViewRequestFpsSelected(seekBar.getProgress());
            }
        };
    }

    private CustomTriangleButton.ButtonDirection getDirection() {
        if (getActivity() instanceof VideoActivity) {
            return CustomTriangleButton.ButtonDirection.TOP_RIGHT;
        }

        return null;
    }
}
