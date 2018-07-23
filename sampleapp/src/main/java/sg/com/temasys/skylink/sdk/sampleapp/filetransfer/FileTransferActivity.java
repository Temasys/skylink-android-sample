package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class FileTransferActivity extends AppCompatActivity {

    private final String FILE_TRANSFER_FRAGMENT_TAG = "FILE_TRANSFER_FRAGMENT";

    private FileTransferPresenter mFileTransferPresenter;
    private FileTransferFragment mFileTransferFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_transfer);

        //check previous state in case of screen rotation
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
        mFileTransferPresenter = new FileTransferPresenter(mFileTransferFragment, this);
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
        getSupportFragmentManager().putFragment(outState, FILE_TRANSFER_FRAGMENT_TAG, mFileTransferFragment);
    }
}
