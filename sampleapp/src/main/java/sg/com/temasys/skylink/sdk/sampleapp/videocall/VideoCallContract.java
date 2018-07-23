package sg.com.temasys.skylink.sdk.sampleapp.videocall;

import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface VideoCallContract {
    interface View extends BaseView<Presenter> {

        void setRoomDetailsViewHandler(String roomDetails);

        void onConnectUIChangeViewHandler();

        void onDisconnectUIChangeViewHandler();

        void addSelfViewPresenterHandler(SurfaceViewRenderer videoView);

        void noteInputVideoResolutionsViewHandler(int width, int height, int fps, SkylinkCaptureFormat captureFormat);

        void setUiResTvStatsReceivedViewHandler(int width, int height, int fps);

        void setUiResTvStatsSentViewHandler(int width, int height, int fps);

        void addRemoteViewViewHandler();

        void onRemotePeerLeaveUIChangeViewHandler();

        Fragment getFragmentViewHandler();

        
    }

    interface Presenter extends BasePresenter {

        void disconnectFromRoomPresenterHandler();

        void connectToRoomPresenterHandler(String roomName);

        boolean isConnectingOrConnectedPresenterHandler();

        void onConnectUIChangePresenterHandler();

        void onDisconnectUIChangePresenterHandler();

        void addSelfViewPresenterHandler(SurfaceViewRenderer videoView);

        void noteInputVideoResolutionsPresenterHandler(int width, int height, int fps, SkylinkCaptureFormat captureFormat);

        void setUiResTvStatsReceivedPresenterHandler(int width, int height, int fps);

        void setUiResTvStatsSentPresenterHandler(int width, int height, int fps);

        void addRemoteViewPresenterHandler();

        void onRemotePeerLeaveUIChangePresenterHandler();

        boolean toggleCameraPresenterHandler();

        boolean toggleCameraPresenterHandler(boolean isToggle);

        void muteLocalAudioPresenterHandler(boolean audioMuted);

        void muteLocalVideoPresenterHandler(boolean videoMuted);

        SkylinkCaptureFormat[] getCaptureFormatsPresenterHandler(SkylinkConfig.VideoDevice videoDevice);

        String getCaptureFormatsStringPresenterHandler(SkylinkCaptureFormat[] captureFormats);

        String getPeerIdPresenterHandler(int index);

        SkylinkConfig.VideoDevice getCurrentVideoDevicePresenterHandler();

        String getCurrentCameraNamePresenterHandler();

        void setInputVideoResolutionPresenterHandler(int width, int height, int fps);

        SurfaceViewRenderer getVideoViewPresenterHandler(String remotePeerId);

        String getRoomPeerIdNickPresenterHandler();

        void getVideoResolutionsPresenterHandler(String peerIdPresenterHandler);

        void switchCameraPresenterHandler();

        Fragment getFragmentPresenterHandler();

        int getSeekBarIndexDimPresenterHandler(SkylinkCaptureFormat[] captureFormats, int width, int height);

        int getSeekBarIndexFpsPresenterHandler(SkylinkCaptureFormat format, int fps);

        SkylinkCaptureFormat getSeekBarValueDimPresenterHandler(int progress, SkylinkCaptureFormat[] captureFormats);

        int getSeekBarValueFpsPresenterHandler(int progress, SkylinkCaptureFormat captureFormatSel);
    }

    interface Service extends BaseService<Presenter> {


    }
}

