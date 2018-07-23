package sg.com.temasys.skylink.sdk.sampleapp.videocall;

import android.content.Context;
import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.data.service.VideoCallService;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class VideoCallPresenter implements VideoCallContract.Presenter {

    public VideoCallContract.View mVideoCallView;
    private VideoCallService mVideoCallService;

    public VideoCallPresenter(VideoCallContract.View videoCallView, Context context) {
        this.mVideoCallView = videoCallView;
        mVideoCallService = new VideoCallService(context);

        this.mVideoCallView.setPresenter(this);
        this.mVideoCallService.setPresenter(this);
    }

    @Override
    public void onConnectUIChangePresenterHandler() {
        mVideoCallView.onConnectUIChangeViewHandler();
    }

    @Override
    public void onDisconnectUIChangePresenterHandler() {
        mVideoCallView.onDisconnectUIChangeViewHandler();
    }

    @Override
    public void addSelfViewPresenterHandler(SurfaceViewRenderer videoView) {
        mVideoCallView.addSelfViewPresenterHandler(videoView);
    }

    @Override
    public void noteInputVideoResolutionsPresenterHandler(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
        mVideoCallView.noteInputVideoResolutionsViewHandler(width, height, fps, captureFormat);
    }

    @Override
    public void setUiResTvStatsReceivedPresenterHandler(int width, int height, int fps) {
        mVideoCallView.setUiResTvStatsReceivedViewHandler(width, height, fps);
    }

    @Override
    public void setUiResTvStatsSentPresenterHandler(int width, int height, int fps) {
        mVideoCallView.setUiResTvStatsSentViewHandler(width, height, fps);
    }

    @Override
    public void addRemoteViewPresenterHandler() {
        mVideoCallView.addRemoteViewViewHandler();
    }

    @Override
    public void onRemotePeerLeaveUIChangePresenterHandler() {
        mVideoCallView.onRemotePeerLeaveUIChangeViewHandler();
    }

    @Override
    public Fragment getFragmentPresenterHandler() {
        return mVideoCallView.getFragmentViewHandler();
    }

    @Override
    public void disconnectFromRoomPresenterHandler() {
        mVideoCallService.disconnectFromRoomServiceHandler();
    }

    @Override
    public void connectToRoomPresenterHandler(String roomName) {
        mVideoCallService.connectToRoomServiceHandler(roomName);
    }

    @Override
    public boolean isConnectingOrConnectedPresenterHandler() {
        return mVideoCallService.isConnectingOrConnectedServiceHandler();
    }

    @Override
    public boolean toggleCameraPresenterHandler() {
        return mVideoCallService.toggleCameraServiceHandler();
    }

    @Override
    public boolean toggleCameraPresenterHandler(boolean isToggle) {
        return mVideoCallService.toggleCameraServiceHandler(isToggle);
    }

    @Override
    public void muteLocalAudioPresenterHandler(boolean audioMuted) {
        mVideoCallService.muteLocalAudioServiceHandler(audioMuted);
    }

    @Override
    public void muteLocalVideoPresenterHandler(boolean videoMuted) {
        mVideoCallService.muteLocalVideoServiceHandler(videoMuted);
    }

    @Override
    public SkylinkCaptureFormat[] getCaptureFormatsPresenterHandler(SkylinkConfig.VideoDevice videoDevice) {
        return mVideoCallService.getCaptureFormatsServiceHandler(videoDevice);
    }

    @Override
    public String getCaptureFormatsStringPresenterHandler(SkylinkCaptureFormat[] captureFormats) {
        return mVideoCallService.getCaptureFormatsStringServiceHandler(captureFormats);
    }

    @Override
    public String getPeerIdPresenterHandler(int index) {
        return mVideoCallService.getPeerIdServiceHandler(index);
    }

    @Override
    public SkylinkConfig.VideoDevice getCurrentVideoDevicePresenterHandler() {
        return mVideoCallService.getCurrentVideoDeviceServiceHandler();
    }

    @Override
    public String getCurrentCameraNamePresenterHandler() {
        return mVideoCallService.getCurrentCameraNameServiceHandler();
    }

    @Override
    public void setInputVideoResolutionPresenterHandler(int width, int height, int fps){
        mVideoCallService.setInputVideoResolutionServiceHandler(width, height, fps);
    }

    @Override
    public SurfaceViewRenderer getVideoViewPresenterHandler(String remotePeerId){
        return mVideoCallService.getVideoViewServiceHandler(remotePeerId);
    }

    @Override
    public String getRoomPeerIdNickPresenterHandler() {
        return mVideoCallService.getRoomPeerIdNickServiceHandler();
    }

    @Override
    public void getVideoResolutionsPresenterHandler(String peerIdPresenterHandler) {
        mVideoCallService.getVideoResolutionsServiceHandler(peerIdPresenterHandler);
    }

    @Override
    public void switchCameraPresenterHandler() {
        mVideoCallService.switchCameraServiceHandler();
    }

    @Override
    public int getSeekBarIndexDimPresenterHandler(SkylinkCaptureFormat[] captureFormats, int width, int height) {
        return mVideoCallService.getSeekBarIndexDimServiceHandler(captureFormats, width, height);
    }

    @Override
    public int getSeekBarIndexFpsPresenterHandler(SkylinkCaptureFormat format, int fps) {
        return mVideoCallService.getSeekBarIndexFpsServiceHandler(format, fps);
    }

    @Override
    public SkylinkCaptureFormat getSeekBarValueDimPresenterHandler(int progress, SkylinkCaptureFormat[] captureFormats) {
        return mVideoCallService.getSeekBarValueDimServiceHandler(progress, captureFormats);
    }

    @Override
    public int getSeekBarValueFpsPresenterHandler(int progress, SkylinkCaptureFormat captureFormatSel) {
        return mVideoCallService.getSeekBarValueFpsServiceHandler(progress, captureFormatSel);
    }
}
