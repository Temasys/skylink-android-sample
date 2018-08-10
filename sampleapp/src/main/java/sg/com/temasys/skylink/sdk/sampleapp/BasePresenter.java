package sg.com.temasys.skylink.sdk.sampleapp;

import android.graphics.Point;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.PermRequesterInfo; /**
 * Created by muoi.pham on 20/07/18.
 */

public interface BasePresenter {

    /**
     * process data to display on View
     */
    void onViewLayoutRequestedPresenterHandler();

    /**
     * process disconnect from room when view exit
     */
    void onViewExitPresenterHandler();

    /**
     * process update view when connected to Skylink SDK
     */
    void onConnectPresenterHandler(boolean isSuccessful);

    /**
     * process update view when disconnect from Skylink SDK
     */
    void onDisconnectPresenterHandler();

    /**
     * process update view when remote peer joined the room
     * @param remotePeerId
     * @param nick
     */
    void onRemotePeerJoinPresenterHandler(String remotePeerId, String nick);

    /**
     * process update view when remote peer left the room
     * @param remotePeerId
     */
    void onRemotePeerLeavePresenterHandler(String remotePeerId);

    void onRemotePeerConnectionRefreshedPresenterHandler(String log, UserInfo remotePeerUserInfo);

    void onLocalMediaCapturePresenterHandler(SurfaceViewRenderer videoView);

    void onInputVideoResolutionObtainedPresenterHandler(int width, int height, int fps, SkylinkCaptureFormat captureFormat);

    void onReceivedVideoResolutionObtainedPresenterHandler(String peerId, int width, int height, int fps);

    void onSentVideoResolutionObtainedPresenterHandler(String peerId, int width, int height, int fps);

    void onVideoSizeChangePresenterHandler(String peerId, Point size);

    void onRemotePeerMediaReceivePresenterHandler(String log, UserInfo remotePeerUserInfo);

    void onPermissionRequiredPresenterHandler(PermRequesterInfo info);

    void onPermissionGrantedPresenterHandler(String[] permissions, int infoCode);

    void onPermissionDeniedPresenterHandler(int infoCode);
}
