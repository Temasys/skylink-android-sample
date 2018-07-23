package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class DataTransferActivity extends AppCompatActivity {

    private final String DATA_TRANSFER_FRAGMENT_TAG = "DATA_TRANSFER_FRAGMENT";

    private DataTransferPresenter mDataTransferPresenter;
    private DataTransferFragment mDataTransferFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_transfer);

        //check previous state in case of screen rotation
        if (savedInstanceState == null) {
            mDataTransferFragment = DataTransferFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameDataTransfer, mDataTransferFragment, DATA_TRANSFER_FRAGMENT_TAG)
                    .commit();
        } else {
            mDataTransferFragment = (DataTransferFragment) getSupportFragmentManager()
                    .findFragmentByTag(DATA_TRANSFER_FRAGMENT_TAG);
        }

        //link between view and presenter
        mDataTransferPresenter = new DataTransferPresenter(mDataTransferFragment, this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance
        getSupportFragmentManager().putFragment(outState, DATA_TRANSFER_FRAGMENT_TAG, mDataTransferFragment);
    }



}
