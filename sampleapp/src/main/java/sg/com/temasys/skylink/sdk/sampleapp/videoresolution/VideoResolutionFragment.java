package sg.com.temasys.skylink.sdk.sampleapp.videoresolution;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomSeekBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomTriangleButton;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoActivity;

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
    private RelativeLayout layoutWHMinMaxCam, layoutWHMinMaxScreen, layoutFpsMinMaxCam, layoutFpsMinMaxScreen;
    private LinearLayout layoutWHCam, layoutWHScreen;
    private CustomSeekBar seekBarWidthHeightCam, seekBarFpsCam, seekBarWidthHeightScreen, seekBarFpsScreen;
    private TextView txtMinWHCam, txtMaxWHCam, txtMinFpsCam, txtMaxFpsCam, txtInputWHCam, txtSentWHCam, txtRecceivedWHCam;
    private TextView txtMinWHScreen, txtMaxWHScreen, txtMinFpsScreen, txtMaxFpsScreen, txtInputWHScreen, txtSentWHScreen, txtRecceivedWHScreen;
    private SeekBar.OnSeekBarChangeListener seekBarChangeListenerResWHCam, seekBarChangeListenerResFpsCam;
    private SeekBar.OnSeekBarChangeListener seekBarChangeListenerResWHScreen, seekBarChangeListenerResFpsScreen;
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

    //----------------------------------------------------------------------------------------------
    // Fragment life cycle methods
    //----------------------------------------------------------------------------------------------

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

    //----------------------------------------------------------------------------------------------
    // Methods called from the Presenter to update UI
    //----------------------------------------------------------------------------------------------

    @Override
    public void updateUIChangeMediaType(SkylinkMedia.MediaType mediaType) {
        if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            seekBarWidthHeightScreen.setVisibility(View.GONE);
            seekBarFpsScreen.setVisibility(View.GONE);
            layoutWHScreen.setVisibility(View.GONE);
            layoutWHMinMaxScreen.setVisibility(View.GONE);
            layoutFpsMinMaxScreen.setVisibility(View.GONE);

            seekBarWidthHeightCam.setVisibility(View.VISIBLE);
            seekBarFpsCam.setVisibility(View.VISIBLE);
            layoutWHCam.setVisibility(View.VISIBLE);
            layoutWHMinMaxCam.setVisibility(View.VISIBLE);
            layoutFpsMinMaxCam.setVisibility(View.VISIBLE);

            btnVideoCamera.setBackground(getResources().getDrawable(R.drawable.button_circle_trans_selected));
            btnVideoScreen.setBackground(getResources().getDrawable(R.drawable.button_circle_trans));

        } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            seekBarWidthHeightCam.setVisibility(View.GONE);
            seekBarFpsCam.setVisibility(View.GONE);
            layoutWHCam.setVisibility(View.GONE);
            layoutWHMinMaxCam.setVisibility(View.GONE);
            layoutFpsMinMaxCam.setVisibility(View.GONE);

            seekBarWidthHeightScreen.setVisibility(View.VISIBLE);
            seekBarFpsScreen.setVisibility(View.VISIBLE);
            layoutWHScreen.setVisibility(View.VISIBLE);
            layoutWHMinMaxScreen.setVisibility(View.VISIBLE);
            layoutFpsMinMaxScreen.setVisibility(View.VISIBLE);

            btnVideoScreen.setBackground(getResources().getDrawable(R.drawable.button_circle_trans_selected));
            btnVideoCamera.setBackground(getResources().getDrawable(R.drawable.button_circle_trans));
        }
    }

    // For camera video resolution UI

    @Override
    public void updateUIOnCameraInputWHValue(int maxWHRange, String minWHValue, String maxWHValue, String currentWHValue, int currentIndex) {
        // update seekbar
        seekBarWidthHeightCam.setMaxRange(maxWHRange);
        if (currentIndex >= 0) {
            seekBarWidthHeightCam.setProgress(currentIndex);
            seekBarWidthHeightCam.setCurrentWidthHeight(currentWHValue);
        } else {
            seekBarWidthHeightCam.setProgress(0);
            seekBarWidthHeightCam.setCurrentWidthHeight("N/A");
        }

        // update textview
        txtMinWHCam.setText(minWHValue);
        txtMaxWHCam.setText(maxWHValue);
    }

    @Override
    public void updateUIOnCameraInputFpsValue(String maxFps, String minFps, String fps) {
        // update seekbar on progress and max range
        seekBarFpsCam.setProgress(Integer.valueOf(fps));
        seekBarFpsCam.setCurrentFps(fps);
        seekBarFpsCam.setMaxRange(Integer.valueOf(maxFps));

        // update textview
        txtMinFpsCam.setText(minFps);
        txtMaxFpsCam.setText(maxFps);
    }

    @Override
    public void updateUIOnCameraInputFpsValue(String maxFps, String minFps) {
        // update seekbar, only on max range
        seekBarFpsCam.setMaxRange(Integer.valueOf(maxFps));

        // update textview
        txtMinFpsCam.setText(minFps);
        txtMaxFpsCam.setText(maxFps);
    }

    @Override
    public void updateUIOnCameraInputValue(String inputValue) {
        txtInputWHCam.setText(inputValue);
    }

    @Override
    public void updateUIOnCameraReceivedValue(int width, int height, int fps) {
        String recValue = "N/A";
        if (width > 0 && height > 0 && fps > 0) {
            recValue = width + "x" + height + ",\n" + fps + " Fps";
        }

        txtRecceivedWHCam.setText(recValue);
    }

    @Override
    public void updateUIOnCameraSentValue(int width, int height, int fps) {
        String sentValue = "N/A";
        if (width > 0 && height > 0 && fps > 0) {
            sentValue = width + "x" + height + ",\n" + fps + " Fps";
        }
        txtSentWHCam.setText(sentValue);
    }

    @Override
    public void updateUIOnCameraInputWHProgressValue(String valueWH) {
        seekBarWidthHeightCam.setCurrentWidthHeight(valueWH);
    }

    @Override
    public void updateUIOnCameraInputFpsProgressValue(String valueFps) {
        seekBarFpsCam.setCurrentFps(valueFps);
    }

    // For screen video resolution UI

    @Override
    public void updateUIOnScreenInputWHValue(int maxWHRange, String minWHValue, String maxWHValue, String currentWHValue, int currentIndex) {
        // update seekbar
        seekBarWidthHeightScreen.setMaxRange(maxWHRange);
        if (currentIndex >= 0) {
            seekBarWidthHeightScreen.setProgress(currentIndex);
            seekBarWidthHeightScreen.setCurrentWidthHeight(currentWHValue);
        } else {
            seekBarWidthHeightScreen.setProgress(0);
            seekBarWidthHeightScreen.setCurrentWidthHeight("N/A");
        }

        // update textview
        txtMinWHScreen.setText(minWHValue);
        txtMaxWHScreen.setText(maxWHValue);
    }

    @Override
    public void updateUIOnScreenInputWHProgressValue(String valueWH) {
        seekBarWidthHeightScreen.setCurrentWidthHeight(valueWH);
    }

    @Override
    public void updateUIOnScreenInputFpsValue(String maxFps, String minFps, String fps) {
        // update seekbar
        seekBarFpsScreen.setProgress(Integer.valueOf(fps) >= 0 ? Integer.valueOf(fps) : 0);
        seekBarFpsScreen.setCurrentFps(Integer.valueOf(fps) >= 0 ? fps : "N/A");
        seekBarFpsScreen.setMaxRange(Integer.valueOf(maxFps));

        // update textview
        txtMinFpsScreen.setText(minFps);
        txtMaxFpsScreen.setText(maxFps);
    }

    @Override
    public void updateUIOnScreenInputFpsProgressValue(String valueFps) {
        seekBarFpsScreen.setCurrentFps(valueFps);
    }

    @Override
    public void updateUIOnScreenInputValue(String inputValue) {
        txtInputWHScreen.setText(inputValue);
    }

    @Override
    public void updateUIOnScreenReceivedValue(int width, int height, int fps) {
        String recValue = "N/A";
        if (width > 0 && height > 0 && fps > 0) {
            recValue = width + "x" + height + ",\n" + fps + " Fps";
        }

        txtRecceivedWHScreen.setText(recValue);
    }

    @Override
    public void updateUIOnScreenSentValue(int width, int height, int fps) {
        String sentValue = "N/A";
        if (width > 0 && height > 0 && fps > 0) {
            sentValue = width + "x" + height + ",\n" + fps + " Fps";
        }
        txtSentWHScreen.setText(sentValue);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        frameVideoRes = rootView.findViewById(R.id.frameVideoRes);
        seekBarWidthHeightCam = rootView.findViewById(R.id.seekBarResWidthHeightCamera);
        seekBarFpsCam = rootView.findViewById(R.id.seekBarResFpsCamera);
        txtMinWHCam = rootView.findViewById(R.id.txtMinRes_camera);
        txtMaxWHCam = rootView.findViewById(R.id.txtMaxRes_camera);
        txtMinFpsCam = rootView.findViewById(R.id.txtMinFps_camera);
        txtMaxFpsCam = rootView.findViewById(R.id.txtMaxFps_camera);
        txtInputWHCam = rootView.findViewById(R.id.txtResInput_cam);
        txtSentWHCam = rootView.findViewById(R.id.txtResSent_cam);
        txtRecceivedWHCam = rootView.findViewById(R.id.txtResReceived_cam);
        btnGetVideoRes = rootView.findViewById(R.id.btnGetVideoRes);
        btnVideoScreen = rootView.findViewById(R.id.btnVideoScreen);
        btnVideoCamera = rootView.findViewById(R.id.btnVideoCamera);
        layoutWHCam = rootView.findViewById(R.id.layout_res_widthHeight_cam);
        layoutWHMinMaxCam = rootView.findViewById(R.id.ll_txtDimRes_camera);
        layoutFpsMinMaxCam = rootView.findViewById(R.id.ll_txtFpsRes_camera);

        seekBarWidthHeightScreen = rootView.findViewById(R.id.seekBarResWidthHeightScreen);
        seekBarFpsScreen = rootView.findViewById(R.id.seekBarResFpsScreen);
        txtMinWHScreen = rootView.findViewById(R.id.txtMinRes_screen);
        txtMaxWHScreen = rootView.findViewById(R.id.txtMaxRes_screen);
        txtMinFpsScreen = rootView.findViewById(R.id.txtMinFps_screen);
        txtMaxFpsScreen = rootView.findViewById(R.id.txtMaxFps_screen);
        txtInputWHScreen = rootView.findViewById(R.id.txtResInput_screen);
        txtSentWHScreen = rootView.findViewById(R.id.txtResSent_screen);
        txtRecceivedWHScreen = rootView.findViewById(R.id.txtResReceived_screen);
        layoutWHScreen = rootView.findViewById(R.id.layout_res_widthHeight_screen);
        layoutWHMinMaxScreen = rootView.findViewById(R.id.ll_txtDimRes_screen);
        layoutFpsMinMaxScreen = rootView.findViewById(R.id.ll_txtFpsRes_screen);
    }

    private void init() {
        if (getDirection() == CustomTriangleButton.ButtonDirection.TOP_LEFT) {
            frameVideoRes.setBackgroundResource(R.drawable.frame_layout_round_border_topleft);
        } else if (getDirection() == CustomTriangleButton.ButtonDirection.TOP_RIGHT) {
            frameVideoRes.setBackgroundResource(R.drawable.frame_layout_round_border_topright);
        }

        resetResolution();

        btnGetVideoRes.setOnClickListener(view -> presenter.processGetVideoResolutions());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnGetVideoRes.setTooltipText("Get video resolution");
        }

        btnVideoCamera.setOnClickListener(view -> {
            updateUIChangeMediaType(SkylinkMedia.MediaType.VIDEO_CAMERA);
            presenter.processChooseVideoCamera();
            btnVideoCamera.setBackground(getResources().getDrawable(R.drawable.button_circle_trans_selected));
            btnVideoScreen.setBackground(getResources().getDrawable(R.drawable.button_circle_trans));
        });

        btnVideoScreen.setOnClickListener(view -> {
            updateUIChangeMediaType(SkylinkMedia.MediaType.VIDEO_SCREEN);
            presenter.processChooseVideoScreen();
            btnVideoCamera.setBackground(getResources().getDrawable(R.drawable.button_circle_trans));
            btnVideoScreen.setBackground(getResources().getDrawable(R.drawable.button_circle_trans_selected));
        });
    }

    public void resetResolution() {
        txtMinWHCam.setText("widthxheight");
        txtMaxWHCam.setText("widthxheight");
        txtMinFpsCam.setText("Fps");
        txtMaxFpsCam.setText("Fps");
        txtInputWHCam.setText("widthxheight");
        txtSentWHCam.setText("widthxheight");
        txtRecceivedWHCam.setText("widthxheight");

        txtMinWHScreen.setText("widthxheight");
        txtMaxWHScreen.setText("widthxheight");
        txtMinFpsScreen.setText("Fps");
        txtMaxFpsScreen.setText("Fps");
        txtInputWHScreen.setText("widthxheight");
        txtSentWHScreen.setText("widthxheight");
        txtRecceivedWHScreen.setText("widthxheight");

        // init seekbars
        seekBarWidthHeightCam.setType(CustomSeekBar.Seekbar_Type.WIDTH_HEIGHT);
        seekBarFpsCam.setType(CustomSeekBar.Seekbar_Type.FPS);
        seekBarWidthHeightCam.setCurrentWidthHeight("WidthxHeight");
        seekBarFpsCam.setCurrentWidthHeight("Fps");

        seekBarWidthHeightScreen.setType(CustomSeekBar.Seekbar_Type.WIDTH_HEIGHT);
        seekBarFpsScreen.setType(CustomSeekBar.Seekbar_Type.FPS);
        seekBarWidthHeightScreen.setCurrentWidthHeight("WidthxHeight");
        seekBarFpsScreen.setCurrentWidthHeight("Fps");

        seekBarChangeListenerResWHCam = getSeekBarChangeListenerWHCam();
        seekBarWidthHeightCam.setOnSeekBarChangeListener(seekBarChangeListenerResWHCam);

        seekBarChangeListenerResFpsCam = getSeekBarChangeListenerFpsCam();
        seekBarFpsCam.setOnSeekBarChangeListener(seekBarChangeListenerResFpsCam);

        seekBarChangeListenerResWHScreen = getSeekBarChangeListenerWHScreen();
        seekBarWidthHeightScreen.setOnSeekBarChangeListener(seekBarChangeListenerResWHScreen);

        seekBarChangeListenerResFpsScreen = getSeekBarChangeListenerFpsScreen();
        seekBarFpsScreen.setOnSeekBarChangeListener(seekBarChangeListenerResFpsScreen);
    }

    private CustomTriangleButton.ButtonDirection getDirection() {
        if (getActivity() instanceof VideoActivity) {
            return CustomTriangleButton.ButtonDirection.TOP_RIGHT;
        }

        return null;
    }

    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListenerWHCam() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                presenter.processWHProgressChangedCamera(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                presenter.processWHSelectedCamera(seekBar.getProgress());
            }
        };
    }

    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListenerFpsCam() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                presenter.processFpsProgressChangedCamera(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                presenter.processFpsSelectedCamera(seekBar.getProgress());
            }
        };
    }

    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListenerWHScreen() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                presenter.processWHProgressChangedScreen(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                presenter.processWHSelectedScreen(seekBar.getProgress());
            }
        };
    }

    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListenerFpsScreen() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                presenter.processFpsProgressChangedScreen(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                presenter.processFpsSelectedScreen(seekBar.getProgress());
            }
        };
    }
}
