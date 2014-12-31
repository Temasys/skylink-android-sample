package com.temasys.skylink.sample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class FilePermissionAlertFragment extends DialogFragment {

    final static private String BUNDLE_MESSAGE = "com.temasys.skylink.sample.FilePermissionAlertFragment.message";
    final static private String BUNDLE_PEER_ID = "com.temasys.skylink.sample.FilePermissionAlertFragment.peerId";
    final static private String BUNDLE_FILE_NAME = "com.temasys.skylink.sample.FilePermissionAlertFragment.fileName";

    private String mMessage;
    private String mPeerId;
    private String mFileName;

    public FilePermissionAlertFragment() {

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Set reference in RoomManager
        RoomManager.get().setFileAlertFragment(this);

        if (savedInstanceState != null) {
            // For automatic recreation.
            mMessage = savedInstanceState.getString(BUNDLE_MESSAGE);
            mPeerId = savedInstanceState.getString(BUNDLE_PEER_ID);
            mFileName = savedInstanceState.getString(BUNDLE_FILE_NAME);
        } else {
            // For manual creation.
            mMessage = getArguments().getString(BUNDLE_MESSAGE);
            mPeerId = getArguments().getString(BUNDLE_PEER_ID);
            mFileName = getArguments().getString(BUNDLE_FILE_NAME);
        }

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setMessage(mMessage)
                .setPositiveButton(
                        getString(R.string.label_file_request_button_pos),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent fileExploreIntent = new Intent(
                                        com.temasys.skylink.sample.FileBrowserActivity.INTENT_ACTION_SELECT_DIR,
                                        null,
                                        getActivity(),
                                        com.temasys.skylink.sample.FileBrowserActivity.class);
                                fileExploreIntent.putExtra(
                                        FileBrowserActivity.EXTRA_PEER_ID,
                                        mPeerId);
                                fileExploreIntent.putExtra(
                                        FileBrowserActivity.EXTRA_FILE_NAME,
                                        mFileName);
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
                            public void onClick(DialogInterface dialog, int which) {
                                declineFileShare();
                            }
                        })
                .create();
        return alertDialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        declineFileShare();
        dialog.cancel();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_MESSAGE, mMessage);
        outState.putString(BUNDLE_PEER_ID, mPeerId);
        outState.putString(BUNDLE_FILE_NAME, mFileName);
    }

    public static FilePermissionAlertFragment newInstance(String message,
                                                          String peerId, String fileName) {
        FilePermissionAlertFragment fragment = new FilePermissionAlertFragment();
        Bundle args = new Bundle();
        args.putString(BUNDLE_MESSAGE, message);
        args.putString(BUNDLE_PEER_ID, peerId);
        args.putString(BUNDLE_FILE_NAME, fileName);
        fragment.setArguments(args);
        return fragment;
    }

    // Get Set methods
    public String getPeerId() {
        return mPeerId;
    }

    public String getFileName() {
        return mFileName;
    }

    // To clear this fragment and FileBrowserActivity when file transfer is cancelled
    // explicitly by Peer or due to timeout.
    public void saveTimeout(String message) {
        Intent fileExploreIntent = new Intent(
                com.temasys.skylink.sample.FileBrowserActivity.INTENT_ACTION_CANCEL_SAVE,
                null,
                getActivity(),
                com.temasys.skylink.sample.FileBrowserActivity.class);
        fileExploreIntent.putExtra(FileBrowserActivity.EXTRA_PEER_ID, mPeerId);
        fileExploreIntent.putExtra(FileBrowserActivity.EXTRA_FILE_NAME, mFileName);
        fileExploreIntent.putExtra(FileBrowserActivity.EXTRA_CANCEL_MESSAGE, message);
        getActivity().startActivityForResult(
                fileExploreIntent,
                Utility.REQUEST_CODE_PICK_DIR);
        FilePermissionAlertFragment.this.dismiss();
    }

    // Decline file share request and do not proceed file explorer UI.
    private void declineFileShare() {
        RoomManager.get().getConnection()
                .acceptFileTransferRequest(mPeerId, false, mFileName);
        RoomManager.get().setFileAlertFragment(null);
        // Reset File explorer state and process next request.
        RoomManager.get().setFileActive(false);
        ((RoomViewActivity) getActivity()).processFileRequest();
    }

}
