package sg.com.temasys.skylink.sdk.sampleapp.localvideo;

import android.content.Context;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.LocalVideoService;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for implementing video logic.
 */

public class LocalVideoPresenter extends BasePresenter implements LocalVideoContract.Presenter {

    private final String TAG = LocalVideoPresenter.class.getName();

    private Context context;

    // The view instance
    public LocalVideoContract.View videoCallView;

    // The service instance
    private LocalVideoService localVideoService;

    public LocalVideoPresenter(Context context) {
        this.context = context;
        this.localVideoService = new LocalVideoService(context);
        this.localVideoService.setPresenter(this);
    }

    public void setView(LocalVideoContract.View view) {
        videoCallView = view;
        videoCallView.setLocalPresenter(this);
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for view to call
    // These methods are responsible for processing requests from view
    //----------------------------------------------------------------------------------------------

    @Override
    public void onViewRequestSwitchCamera() {
        localVideoService.switchCamera();
    }

    @Override
    public void onViewRequestGetVideoResolutions() {
        // get the remote peer id
        String peerId = localVideoService.getPeerId(1);
        localVideoService.getVideoResolutions(peerId);
    }
}
