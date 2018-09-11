package sg.com.temasys.skylink.sdk.sampleapp.audio;

import android.support.v4.app.Fragment;

import sg.com.temasys.skylink.sdk.sampleapp.BaseService;
import sg.com.temasys.skylink.sdk.sampleapp.BaseView;

/**
 * Created by muoi.pham on 20/07/18.
 */
public interface AudioCallContract {


    interface View extends BaseView<Presenter> {

        /**
         * Get instance of the fragment for processing permission
         */
        Fragment onPresenterRequestGetFragmentInstance();

        /**
         * Update UI details when changing state
         */
        void onPresenterRequestUpdateUI(String roomDetails, boolean isPeerJoined, boolean isSpeakerOn);

        void onPresenterRequestChangeBtnAudioSpeaker(boolean isPeerJoined, boolean isSpeakerOn);
    }


    interface Presenter {

        /**
         * process data to display on view
         */
        void onViewRequestLayout();

        /**
         * process permission result
         */
        void onViewRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, String tag);

        /**
         * process change audio output between headset and speaker
         */
        void onViewRequestChangeAudioOuput();

        /**
         * process change state when view paused
         */
        void onViewRequestStop();

        /**
         * process change state when view resumed
         */
        void onViewRequestResume();

        /**
         * process change state when view exit/closed
         */
        void onViewRequestExit();

    }

    interface Service extends BaseService<Presenter> {

    }
}

