package sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo;

import android.support.v4.app.Fragment;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface MultiPartyVideoCallContract {
    interface View extends BaseView<Presenter> {

        void addSelfViewPresenterHandler(SurfaceViewRenderer videoView);

        Fragment getFragmentViewHandler();

        void addRemoteViewViewHandler(String remotePeerId);

        void addRemotePeerViewHandler(String remotePeerId);

        void removeRemotePeerHandler(String remotePeerId);

        void setPeerListViewHandler(String peer);
    }

    interface Presenter extends BasePresenter {

        void disconnectFromRoomPresenterHandler();

        void connectToRoomPresenterHandler(String roomName);

        boolean isConnectingOrConnectedPresenterHandler();

        void addSelfViewPresenterHandler(SurfaceViewRenderer videoView);

        boolean toggleCameraPresenterHandler();

        boolean toggleCameraPresenterHandler(boolean isToggle);

        SurfaceViewRenderer getVideoViewPresenterHandler(String remotePeerId);

        String getRoomPeerIdNickPresenterHandler();

        void switchCameraPresenterHandler();

        Fragment getFragmentPresenterHandler();

        void addRemoteViewPresenterHandler(String remotePeerId);

        void addRemotePeerPresenterHandler(String remotePeerId);

        void removeRemotePeerPresenterHandler(String remotePeerId);

        void getInputVideoResolutionPresenterHandler();

        void refreshConnectionPresenterHandler(String peerId, boolean iceRestart);

        boolean startRecordingPresenterHandler();

        boolean stopRecordingPresenterHandler();

        boolean getTransferSpeedsPresenterHandler(String peerId, int mediaDirectionBoth, int mediaAll);

        boolean getWebrtcStatsPresenterHandler(String peerId, int mediaDirectionBoth, int mediaAll);

        String[] getPeerIdListPresenterHandler();

        void getSentVideoResolutionPresenterHandler(String peerId);

        void getReceivedVideoResolutionPresenterHandler(String peerId);

        String getRoomPeerIdNickPresenterHandler(String room_name, String peerId);

        void setPeerListPresenterHandler(String s);

        int getTotalInRoomPresenterHandler();

        int getNumRemotePeersPresenterHandler();
    }

    interface Service extends BaseService<Presenter> {


    }
}

