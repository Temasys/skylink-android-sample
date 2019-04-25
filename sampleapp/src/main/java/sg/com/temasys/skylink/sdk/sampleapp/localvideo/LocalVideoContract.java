package sg.com.temasys.skylink.sdk.sampleapp.localvideo;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface LocalVideoContract {
    interface View {

        /**
         * set connection between view and presenter
         */
        void setLocalPresenter(LocalVideoContract.Presenter presenter);

    }

    interface Presenter {

        /**
         * switch camera between front and back
         */
        void onViewRequestSwitchCamera();

        /**
         * get current video resolution info
         */
        void onViewRequestGetVideoResolutions();

    }

    interface Service extends BaseService<Presenter> {


    }
}

