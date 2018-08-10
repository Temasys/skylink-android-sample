package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.support.v4.app.Fragment;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.PermRequesterInfo;

/**
 * Created by muoi.pham on 20/07/18.
 */
public interface AudioCallContract {
    interface View extends BaseView<Presenter> {

        /**
         * Get instance of the fragment for processing permission
         */
        Fragment onGetFragmentViewHandler();

        /**
         * Set the room details information on UI.
         */
        void onUpdateUIViewHandler(String roomDetails);
    }

    interface Presenter extends BasePresenter {

        void onRequestPermissionsResultPresenterHandler(int requestCode, String[] permissions, int[] grantResults, String tag);

        void onPermissionRequiredPresenterHandler(PermRequesterInfo infor);
    }

    interface Service extends BaseService<Presenter> {


    }
}

