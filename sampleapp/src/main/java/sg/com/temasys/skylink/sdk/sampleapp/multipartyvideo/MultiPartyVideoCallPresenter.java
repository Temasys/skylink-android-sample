package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.content.Context;
import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.sampleapp.data.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.data.service.MultiPartyVideoService;

/**
 * Created by muoi.pham on 20/07/18.
 */
public abstract class MultiPartyVideoCallPresenter implements MultiPartyVideoCallContract.Presenter {

    public MultiPartyVideoCallContract.View mVideoCallView;
    private MultiPartyVideoService mMultiPartyVideoCallService;

    private Context mContext;

    public MultiPartyVideoCallPresenter(MultiPartyVideoCallContract.View videoCallView, Context context) {
        this.mVideoCallView = videoCallView;
        this.mContext = context;

        mMultiPartyVideoCallService = new MultiPartyVideoService(mContext);

        this.mVideoCallView.setPresenter(this);
//        this.mMultiPartyVideoCallService.setPresenter(this);
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
//        mMultiPartyVideoCallService.getInputVideoResolutionServiceHandler();
    }

    @Override
    public void refreshConnectionPresenterHandler(String peerId, boolean iceRestart){
//        mMultiPartyVideoCallService.refreshConnectionServiceHandler(peerId, iceRestart);
    }

    @Override
    public boolean startRecordingPresenterHandler(){
//        return mMultiPartyVideoCallService.startRecordingServiceHandler();
        return false;
    }

    @Override
    public boolean stopRecordingPresenterHandler(){
//        return mMultiPartyVideoCallService.stopRecordingServiceHandler();
        return false;
    }

    @Override
    public boolean getTransferSpeedsPresenterHandler(String peerId, int mediaDirectionBoth, int mediaAll){
//        return mMultiPartyVideoCallService.getTransferSpeedsServiceHandler(peerId, mediaDirectionBoth, mediaAll);
        return false;
    }

    @Override
    public boolean getWebrtcStatsPresenterHandler(String peerId, int mediaDirectionBoth, int mediaAll){
//        return mMultiPartyVideoCallService.getWebrtcStatsServiceHandler(peerId, mediaDirectionBoth, mediaAll);
        return false;
    }

    @Override
    public String[] getPeerIdListPresenterHandler(){
//        return mMultiPartyVideoCallService.getPeerIdListServiceHandler();
        return null;
    }

    @Override
    public void getSentVideoResolutionPresenterHandler(String peerId){
//        mMultiPartyVideoCallService.getSentVideoResolutionServiceHandler(peerId);
    }

    @Override
    public void getReceivedVideoResolutionPresenterHandler(String peerId){
//        mMultiPartyVideoCallService.getReceivedVideoResolutionServiceHandler(peerId);
    }

    @Override
    public String getRoomPeerIdNickPresenterHandler(String room_name, String peerId){
//        return mMultiPartyVideoCallService.getRoomPeerIdNickServiceHandler(room_name, peerId);
        return null;
    }

    @Override
    public void disconnectFromRoomPresenterHandler() {
//        mMultiPartyVideoCallService.disconnectFromRoomServiceHandler();
    }

    @Override
    public void connectToRoomPresenterHandler(String roomName) {
//        mMultiPartyVideoCallService.connectToRoomServiceHandler(roomName);
    }

    @Override
    public boolean isConnectingOrConnectedPresenterHandler() {
//        return mMultiPartyVideoCallService.isConnectingOrConnectedServiceHandler();
        return false;
    }

    @Override
    public boolean toggleCameraPresenterHandler() {
//        return mMultiPartyVideoCallService.toggleCameraServiceHandler();
        return false;
    }

    @Override
    public boolean toggleCameraPresenterHandler(boolean isToggle) {
//        return mMultiPartyVideoCallService.toggleCameraServiceHandler(isToggle);
        return false;
    }

    @Override
    public SurfaceViewRenderer getVideoViewPresenterHandler(String remotePeerId){
//        return mMultiPartyVideoCallService.getVideoViewServiceHandler(remotePeerId);
        return null;
    }

    @Override
    public String getRoomPeerIdNickPresenterHandler() {
//        return mMultiPartyVideoCallService.getRoomPeerIdNickServiceHandler();
        return null;
    }

    @Override
    public void switchCameraPresenterHandler() {
//        mMultiPartyVideoCallService.switchCameraServiceHandler();
    }

    @Override
    public int getTotalInRoomPresenterHandler() {
//        return mMultiPartyVideoCallService.getTotalInRoomServiceHandler();
        return 0;
    }

    @Override
    public int getNumRemotePeersPresenterHandler() {
//        return mMultiPartyVideoCallService.getNumRemotePeersServiceHandler();
        return 0;
    }

    @Override
    public void onViewLayoutRequestedPresenterHandler() {

    }

    @Override
    public void onViewExitPresenterHandler() {

    }

    public void onConnectPresenterHandler() {

    }

    @Override
    public void onDisconnectPresenterHandler() {

    }

//    @Override
    public void onRemotePeerJoinPresenterHandler(String remotePeerId, String nick) {

    }

    @Override
    public void onRemotePeerLeavePresenterHandler(String remotePeerId) {

    }

//    @Override
    public void onPermissionRequiredPresenterHandler(PermRequesterInfo info) {

    }
}
