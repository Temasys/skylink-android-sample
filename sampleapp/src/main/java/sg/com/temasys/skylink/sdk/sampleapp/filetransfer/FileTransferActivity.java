package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class FileTransferActivity extends AppCompatActivity {

    private final String FILE_TRANSFER_FRAGMENT_TAG = "FILE_TRANSFER_FRAGMENT";

    // presenter instance
    private FileTransferPresenter mFileTransferPresenter;

    // view instance
    private FileTransferFragment mFileTransferFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_transfer);

        //create presenter
        mFileTransferPresenter = new FileTransferPresenter(this);

        // check previous state in case of screen rotation
        // if it is new state, then create view instance
        // otherwise reuse the view instance, just update it
        if (savedInstanceState == null) {
            mFileTransferFragment = FileTransferFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameFileTransfer, mFileTransferFragment, FILE_TRANSFER_FRAGMENT_TAG)
                    .commit();
        } else {
            mFileTransferFragment = (FileTransferFragment) getSupportFragmentManager()
                    .findFragmentByTag(FILE_TRANSFER_FRAGMENT_TAG);
        }

        //link between view and presenter
        mFileTransferPresenter.setView(mFileTransferFragment);
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
        getSupportFragmentManager().putFragment(outState, FILE_TRANSFER_FRAGMENT_TAG, mFileTransferFragment);
    }
}
