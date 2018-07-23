package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideocall;

import android.content.Context;
import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.sampleapp.data.service.MultiPartyVideoCallService;

/**
 * Created by muoi.pham on 20/07/18.
 */
public class MultiPartyVideoCallPresenter implements MultiPartyVideoCallContract.Presenter {

    public MultiPartyVideoCallContract.View mVideoCallView;
    private MultiPartyVideoCallService mMultiPartyVideoCallService;

    private Context mContext;

    public MultiPartyVideoCallPresenter(MultiPartyVideoCallContract.View videoCallView, Context context) {
        this.mVideoCallView = videoCallView;
        this.mContext = context;

        mMultiPartyVideoCallService = new MultiPartyVideoCallService(mContext);

        this.mVideoCallView.setPresenter(this);
        this.mMultiPartyVideoCallService.setPresenter(this);
    }

    @Override
    public Fragment getFragmentPresenterHandler() {
        return mVideoCallView.getFragmentViewHandler();
    }

    @Override
    public void setPeerListPresenterHandler(String peer){
        mVideoCallView.setPeerListViewHandler(peer);
    }

    @Override
    public void addRemoteViewPresenterHandler(String remotePeerId){
        mVideoCallView.addRemoteViewViewHandler(remotePeerId);
    }

    @Override
    public void addRemotePeerPresenterHandler(String remotePeerId){
        mVideoCallView.addRemotePeerViewHandler(remotePeerId);
    }

    @Override
    public void removeRemotePeerPresenterHandler(String remotePeerId){
        mVideoCallView.removeRemotePeerHandler(remotePeerId);
    }

    @Override
    public void addSelfViewPresenterHandler(SurfaceViewRenderer videoView) {
        mVideoCallView.addSelfViewPresenterHandler(videoView);
    }

    @Override
    public void getInputVideoResolutionPresenterHandler(){
        mMultiPartyVideoCallService.getInputVideoResolutionServiceHandler();
    }

    @Override
    public void refreshConnectionPresenterHandler(String peerId, boolean iceRestart){
        mMultiPartyVideoCallService.refreshConnectionServiceHandler(peerId, iceRestart);
    }

    @Override
    public boolean startRecordingPresenterHandler(){
        return mMultiPartyVideoCallService.startRecordingServiceHandler();
    }

    @Override
    public boolean stopRecordingPresenterHandler(){
        return mMultiPartyVideoCallService.stopRecordingServiceHandler();
    }

    @Override
    public boolean getTransferSpeedsPresenterHandler(String peerId, int mediaDirectionBoth, int mediaAll){
        return mMultiPartyVideoCallService.getTransferSpeedsServiceHandler(peerId, mediaDirectionBoth, mediaAll);
    }

    @Override
    public boolean getWebrtcStatsPresenterHandler(String peerId, int mediaDirectionBoth, int mediaAll){
        return mMultiPartyVideoCallService.getWebrtcStatsServiceHandler(peerId, mediaDirectionBoth, mediaAll);
    }

    @Override
    public String[] getPeerIdListPresenterHandler(){
        return mMultiPartyVideoCallService.getPeerIdListServiceHandler();
    }

    @Override
    public void getSentVideoResolutionPresenterHandler(String peerId){
        mMultiPartyVideoCallService.getSentVideoResolutionServiceHandler(peerId);
    }

    @Override
    public void getReceivedVideoResolutionPresenterHandler(String peerId){
        mMultiPartyVideoCallService.getReceivedVideoResolutionServiceHandler(peerId);
    }

    @Override
    public String getRoomPeerIdNickPresenterHandler(String room_name, String peerId){
        return mMultiPartyVideoCallService.getRoomPeerIdNickServiceHandler(room_name, peerId);
    }

    @Override
    public void disconnectFromRoomPresenterHandler() {
        mMultiPartyVideoCallService.disconnectFromRoomServiceHandler();
    }

    @Override
    public void connectToRoomPresenterHandler(String roomName) {
        mMultiPartyVideoCallService.connectToRoomServiceHandler(roomName);
    }

    @Override
    public boolean isConnectingOrConnectedPresenterHandler() {
        return mMultiPartyVideoCallService.isConnectingOrConnectedServiceHandler();
    }

    @Override
    public boolean toggleCameraPresenterHandler() {
        return mMultiPartyVideoCallService.toggleCameraServiceHandler();
    }

    @Override
    public boolean toggleCameraPresenterHandler(boolean isToggle) {
        return mMultiPartyVideoCallService.toggleCameraServiceHandler(isToggle);
    }

    @Override
    public SurfaceViewRenderer getVideoViewPresenterHandler(String remotePeerId){
        return mMultiPartyVideoCallService.getVideoViewServiceHandler(remotePeerId);
    }

    @Override
    public String getRoomPeerIdNickPresenterHandler() {
        return mMultiPartyVideoCallService.getRoomPeerIdNickServiceHandler();
    }

    @Override
    public void switchCameraPresenterHandler() {
        mMultiPartyVideoCallService.switchCameraServiceHandler();
    }

    @Override
    public int getTotalInRoomPresenterHandler() {
        return mMultiPartyVideoCallService.getTotalInRoomServiceHandler();
    }

    @Override
    public int getNumRemotePeersPresenterHandler() {
        return mMultiPartyVideoCallService.getNumRemotePeersServiceHandler();
    }

}
