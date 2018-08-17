package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;

import org.webrtc.SurfaceViewRenderer;

import java.util.Arrays;

import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoLocalState;
import sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo.MultiPartyVideoCallContract;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class MultiPartyVideoService extends SDKService implements MultiPartyVideoCallContract.Service{

    private final String TAG = MultiPartyVideoService.class.getName();

    private static VideoLocalState videoLocalState = new VideoLocalState();

    public MultiPartyVideoService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(MultiPartyVideoCallContract.Presenter presenter) {
        this.mMultiVideoPresenter = presenter;
    }

    @Override
    public void setTypeCall() {
        mTypeCall = Constants.CONFIG_TYPE.MULTI_PARTY_VIDEO;
    }

    public boolean isCameraToggle() {
        return videoLocalState.isCameraToggle();
    }

    public void setCamToggle(boolean isCamToggle) {
        videoLocalState.setCameraToggle(isCamToggle);
    }

    public boolean toggleCamera() {
        if (mSkylinkConnection != null)
            return mSkylinkConnection.toggleCamera();
        return false;
    }

    public boolean toggleCamera(boolean isToggle) {
        if (mSkylinkConnection != null)
            return mSkylinkConnection.toggleCamera(isToggle);
        return false;
    }

    public SurfaceViewRenderer getVideoView(String remotePeerId) {
        if (mSkylinkConnection != null)
            return mSkylinkConnection.getVideoView(remotePeerId);

        return null;
    }

    public void switchCamera() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.switchCamera();
        }
    }

     /* Get list of remote peer id in room using SkylinkConnection API.
     *
     * @param index 0 for self Peer, 1 onwards for remote Peer(s).
     * @return list of peerId or null if not available.
     */
    public String[] getPeerIdList() {

        if (mSkylinkConnection == null) {
            return null;
        }
        String[] peerIdList = mSkylinkConnection.getPeerIdList();

        return peerIdList;
    }

    public void getInputVideoResolution() {

        if(mSkylinkConnection != null){
            mSkylinkConnection.getInputVideoResolution();
        }
    }

    public boolean startRecording() {
        if(mSkylinkConnection != null) {
            boolean success = mSkylinkConnection.startRecording();
            String log = "[SRS][SA] startRecording=" + success +
                    ", isRecording=" + mSkylinkConnection.isRecording() + ".";
            toastLog(TAG, mContext, log);
            return success;
        }

        return false;
    }

    public boolean stopRecording() {
        if(mSkylinkConnection != null) {
            boolean success = mSkylinkConnection.stopRecording();
            String log = "[SRS][SA] stopRecording=" + success +
                    ", isRecording=" + mSkylinkConnection.isRecording() + ".";
            toastLog(TAG, mContext, log);
            return success;
            }

        return false;
    }

    public void getSentVideoResolution(int peerIndex) {

        if(mSkylinkConnection != null){
            mSkylinkConnection.getSentVideoResolution(mPeersList.get(peerIndex).getPeerId());
        }
    }

    public void getReceivedVideoResolution(int peerIndex) {

        if(mSkylinkConnection != null){
            mSkylinkConnection.getReceivedVideoResolution(mPeersList.get(peerIndex).getPeerId());
        }
    }

    public void getWebrtcStats(String peerId, int mediaDirection, int mediaType) {

        if(mSkylinkConnection != null){
            mSkylinkConnection.getWebrtcStats(peerId, mediaDirection, mediaType);
        }
    }

    public void getTransferSpeeds(int peerIndex, int mediaDirection, int mediaType) {

        String peerId = mPeersList.get(peerIndex).getPeerId();

        if(peerId == null)
            return;

        if(mSkylinkConnection != null){
            mSkylinkConnection.getTransferSpeeds(peerId, mediaDirection, mediaType);
        }
    }

    public void refreshConnection(int peerIndex, boolean iceRestart) {

        if(mSkylinkConnection == null)
            return;

        String peerStr = "All peers ";

        //list of peers that are failed for refreshing
        String[] failedPeers = null;

        if(peerIndex == -1){
            failedPeers = mSkylinkConnection.refreshConnection(null, iceRestart);
        } else {
            SkylinkPeer peer = mPeersList.get(peerIndex);
            failedPeers = mSkylinkConnection.refreshConnection(peer.getPeerId(), iceRestart);
            peerStr = "Peer " + peer.getPeerName();
        }

        String log = "Refreshing connection for " + peerStr;
        if (iceRestart) {
            log += " with ICE restart.";
        } else {
            log += ".";
        }
        toastLog(TAG, mContext, log);

        // Log errors if any.
        if (failedPeers != null) {
            log = "Unable to refresh ";
            if ("".equals(failedPeers[0])) {
                log += "as there is no Peer in the room!";
            } else {
                log += "for Peer(s): " + Arrays.toString(failedPeers) + "!";
            }
            toastLog(TAG, mContext, log);
        }
    }

    public int getTotalInRoom() {

        if(mPeersList != null)
            return mPeersList.size();

        return 0;
    }

    public String getPeerIdByIndex(int peerIndex) {
        return mPeersList.get(peerIndex).getPeerId();
    }

    public SurfaceViewRenderer getVideoViewByIndex(int peerIndex) {
        if(mSkylinkConnection != null && peerIndex < mPeersList.size()){
            return mSkylinkConnection.getVideoView(mPeersList.get(peerIndex).getPeerId());
        }

        return null;
    }

    public int getPeerIndexByPeerId(String peerId) {
        for(int i=0; i<mPeersList.size(); i++){
            SkylinkPeer peer = mPeersList.get(i);

            if(peer.getPeerId().equals(peerId)){
                return i;
            }
        }

        return -1;
    }
}
