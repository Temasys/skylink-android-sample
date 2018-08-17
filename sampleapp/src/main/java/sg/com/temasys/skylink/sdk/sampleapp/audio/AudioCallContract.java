package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.support.v4.app.Fragment;

import sg.com.temasys.skylink.sdk.rtc.UserInfo;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;

/**
 * Created by muoi.pham on 20/07/18.
 */
public interface AudioCallContract {
    interface View extends BaseView<Presenter> {

        /**
         * Get instance of the fragment for processing permission
         */
        Fragment onGetFragment();

        /**
         * Set the room details information on UI.
         */
        void onUpdateUI(String roomDetails);
    }

    interface Presenter extends BasePresenter {

        void onPermissionRequired(PermRequesterInfo info);

        void onPermissionGranted(PermRequesterInfo info);

        void onPermissionDenied(PermRequesterInfo info);

        void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag);

        /**
         * process update view when remote peer refresh the connection
         * @param log info to display
         * @param remotePeerUserInfo
         */
        void onRemotePeerConnectionRefreshed(String log, UserInfo remotePeerUserInfo);

        /**
         * process update view when remote peer has receive media info
         * @param log info to display
         * @param remotePeerUserInfo
         */
        void onRemotePeerMediaReceive(String log, UserInfo remotePeerUserInfo);

    }

    interface Service extends BaseService<Presenter> {


    }
}

