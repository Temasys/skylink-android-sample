package sg.com.temasys.skylink.sdk.sample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class FilePermissionAlertFragment extends DialogFragment {

    final static private String BUNDLE_FILE_NAME = "sg.com.temasys.skylink.sdk.sample.FilePermissionAlertFragment.fileName";
    final static private String BUNDLE_MESSAGE = "sg.com.temasys.skylink.sdk.sample.FilePermissionAlertFragment.message";
    final static private String BUNDLE_PEER_ID = "sg.com.temasys.skylink.sdk.sample.FilePermissionAlertFragment.peerId";

    private String mFileName;
    private String mMessage;
    private String mPeerId;

    public FilePermissionAlertFragment() {

    }

    public String getFileName() {
        return mFileName;
    }

    public String getPeerId() {
        return mPeerId;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        RoomManager.get().setFileAlertFragment(this);

        if (savedInstanceState != null) {
            mFileName = savedInstanceState.getString(BUNDLE_FILE_NAME);
            mMessage = savedInstanceState.getString(BUNDLE_MESSAGE);
            mPeerId = savedInstanceState.getString(BUNDLE_PEER_ID);
        } else {
            mFileName = getArguments().getString(BUNDLE_FILE_NAME);
            mMessage = getArguments().getString(BUNDLE_MESSAGE);
            mPeerId = getArguments().getString(BUNDLE_PEER_ID);
        }

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setMessage(mMessage)
                .setPositiveButton(
                        getString(R.string.label_file_request_button_pos),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                Intent fileExploreIntent = new Intent(
                                        sg.com.temasys.skylink.sdk.sample.FileBrowserActivity.INTENT_ACTION_SELECT_DIR,
                                        null,
                                        getActivity(),
                                        sg.com.temasys.skylink.sdk.sample.FileBrowserActivity.class);
                                fileExploreIntent.putExtra(
                                        FileBrowserActivity.EXTRA_FILE_NAME,
                                        mFileName);
                                fileExploreIntent.putExtra(
                                        FileBrowserActivity.EXTRA_PEER_ID,
                                        mPeerId);
                                getActivity().startActivityForResult(
                                        fileExploreIntent,
                                        Utility.REQUEST_CODE_PICK_DIR);

                                FilePermissionAlertFragment.this.dismiss();
                            }
                        })
                .setNegativeButton(
                        getString(R.string.label_file_request_button_neg),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                declineFileShare();
                            }
                        }).create();
        return alertDialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_FILE_NAME, mFileName);
        outState.putString(BUNDLE_MESSAGE, mMessage);
        outState.putString(BUNDLE_PEER_ID, mPeerId);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        declineFileShare();
        dialog.cancel();
    }

    public static FilePermissionAlertFragment newInstance(String message,
                                                          String peerId, String fileName) {
        FilePermissionAlertFragment fragment = new FilePermissionAlertFragment();
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_FILE_NAME, fileName);
        bundle.putString(BUNDLE_MESSAGE, message);
        bundle.putString(BUNDLE_PEER_ID, peerId);
        fragment.setArguments(bundle);
        return fragment;
    }

    // To clear this fragment and FileBrowserActivity when file transfer is
    // cancelled explicitly by Peer or due to timeout.
    public void saveTimeout(String message) {
        Intent fileExploreIntent = new Intent(
                sg.com.temasys.skylink.sdk.sample.FileBrowserActivity.INTENT_ACTION_CANCEL_SAVE,
                null, getActivity(),
                sg.com.temasys.skylink.sdk.sample.FileBrowserActivity.class);
        fileExploreIntent.putExtra(FileBrowserActivity.EXTRA_CANCEL_MESSAGE,
                message);
        fileExploreIntent.putExtra(FileBrowserActivity.EXTRA_FILE_NAME,
                mFileName);
        fileExploreIntent.putExtra(FileBrowserActivity.EXTRA_PEER_ID, mPeerId);
        getActivity().startActivityForResult(fileExploreIntent,
                Utility.REQUEST_CODE_PICK_DIR);

        FilePermissionAlertFragment.this.dismiss();
    }

    // Decline file share request and do not proceed file explorer UI.
    private void declineFileShare() {
        RoomManager.get().getConnection()
                .rejectFileTransferRequest(mPeerId, mFileName);
        RoomManager.get().setFileAlertFragment(null);
        RoomManager.get().setFileUIActive(false);

        ((RoomViewActivity) getActivity()).processFileRequest();
    }

}
