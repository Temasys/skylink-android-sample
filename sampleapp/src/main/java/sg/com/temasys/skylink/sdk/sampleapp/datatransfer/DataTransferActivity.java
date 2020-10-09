package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class DataTransferActivity extends AppCompatActivity {

    private final String DATA_TRANSFER_FRAGMENT_TAG = "DATA_TRANSFER_FRAGMENT";

    // presenter instance
    private DataTransferPresenter dataTransferPresenter;

    // view instance
    private DataTransferFragment aataTransferFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_transfer);

        //create presenter
        dataTransferPresenter = new DataTransferPresenter(this);

        // check previous state in case of screen rotation
        // if it is new state, then create view instance
        // otherwise reuse the view instance and keep states
        if (savedInstanceState == null) {
            aataTransferFragment = DataTransferFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameDataTransfer, aataTransferFragment, DATA_TRANSFER_FRAGMENT_TAG)
                    .commit();
        } else {
            aataTransferFragment = (DataTransferFragment) getSupportFragmentManager()
                    .findFragmentByTag(DATA_TRANSFER_FRAGMENT_TAG);
        }

        //link between view and presenter
        dataTransferPresenter.setView(aataTransferFragment);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance when changing configuration
        getSupportFragmentManager().putFragment(outState, DATA_TRANSFER_FRAGMENT_TAG, aataTransferFragment);
    }

}
