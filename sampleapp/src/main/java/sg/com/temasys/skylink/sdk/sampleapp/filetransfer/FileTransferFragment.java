package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CurvedTextView;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.SAMPLE_FILE_NAME;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * This class is responsible for display UI and get user interaction
 */
public class FileTransferFragment extends CustomActionBar implements FileTransferContract.View,
        View.OnClickListener, View.OnLongClickListener {

    private final String TAG = FileTransferFragment.class.getName();

    // The request code for file browser
    private static final int CHOOSE_FILE_REQUEST_CODE = 10;

    // presenter instance to implement app logic
    private FileTransferContract.Presenter mPresenter;

    // view widgets
    private TextView txtFilePathInfo;
    private EditText editFilePath;
    private ImageView imgView;
    private ImageButton btnChooseFile;
    private ImageButton btnSendFile;
    private ProgressBar spinnerBackground;
    private ProgressBar spinnerForeground;
    private CurvedTextView txtProgressInfo;
    private TextView txtPercentage;
    private LinearLayout filePreviewContainer;

    public static FileTransferFragment newInstance() {
        return new FileTransferFragment();
    }

    @Override
    public void setPresenter(FileTransferContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    //----------------------------------------------------------------------------------------------
    // Fragment life cycle methods
    //----------------------------------------------------------------------------------------------

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        super.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "[SA][FileTransfer][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_file_transfer, container, false);

        // get the UI controls from layout
        getControlWidgets(rootView);

        // setup the action bar
        setActionBar();

        // init the UI controls
        initControls();

        //request an initiative connection
        requestViewLayout();

        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        // in case of changing screen orientation, do not close the connection
        if (!((FileTransferActivity) context).isChangingConfigurations()) {
            //disconnect from room
            mPresenter.onViewRequestExit();
        }
    }

    @Override
    public void onClick(View view) {
        //Defining a click event listener for the buttons in the action bar.
        switch (view.getId()) {
            case R.id.btnBack:
                processBack();
                break;
            case R.id.btnLocalPeer:
                processSelectPeer(0);
                break;
            case R.id.btnRemotePeer1:
                processSelectPeer(1);
                break;
            case R.id.btnRemotePeer2:
                processSelectPeer(2);
                break;
            case R.id.btnRemotePeer3:
                processSelectPeer(3);
                break;
            case R.id.btnChoose:
                requestPermission();
                break;
            case R.id.btnSend:
                processSendFile();
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        //Defining a long click event listener for the peer buttons in the action bar.
        switch (view.getId()) {
            case R.id.btnLocalPeer:
                displayPeerInfo(0);
                break;
            case R.id.btnRemotePeer1:
                displayPeerInfo(1);
                break;
            case R.id.btnRemotePeer2:
                displayPeerInfo(2);
                break;
            case R.id.btnRemotePeer3:
                displayPeerInfo(3);
                break;
        }

        return true;
    }

    /**
     * Process permission result from user choice
     * There are 2 permission types: from app and from SDK
     * if permission comes from SDK, the app should use the SDK request code
     * if permission comes from the app, the app need to implement itself
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // permission results from app
        if (requestCode == PermissionUtils.APP_PERMISSIONS_READ_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // show the file picker for user to choose file
                showFilePicker();
            } else {
                // permission denied, warning the user
                mPresenter.onViewRequestPermissionDeny();
            }
        } else { // permission results from SDK
            mPresenter.onViewRequestPermissionsResult(requestCode, permissions, grantResults, TAG);
        }
    }

    /**
     * Get the file that user choose from browser and then display on UI properly
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // CHOOSE_FILE_REQUEST_CODE.
        if (requestCode == CHOOSE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // update file info
            // check for select remote peer or un select it
            boolean unSelected = mPresenter.onViewRequestGetCurrentSelectedPeer() == 0 ? true : false;
            if (unSelected) {
                txtFilePathInfo.setText("File path to send to all peers : ");
            } else {
                int index = mPresenter.onViewRequestGetCurrentSelectedPeer();
                SkylinkPeer selectedPeer = mPresenter.onViewRequestGetPeerByIndex(index);
                txtFilePathInfo.setText("File path to send to " + selectedPeer.toString() + " : ");
            }

            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i(TAG, "Uri: " + uri.toString());

                // update the edit text with file path from Uri
                String filePath = Utils.getFilePath(context, uri);
                editFilePath.setText(filePath);
                // move cursor to the end of edit text
                editFilePath.setSelection(editFilePath.getText().length());

                // update the image preview
                if (Utils.isImageFile(filePath)) {
                    imgView.setImageURI(uri);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        imgView.setImageDrawable(context.getDrawable(R.drawable.ic_file_common));
                    }
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // change screen orientation in case of portrait/landscape mode
        changeScreenOrientation(newConfig.orientation);

        super.onConfigurationChanged(newConfig);
    }

    //----------------------------------------------------------------------------------------------
    // Methods called from the Presenter to update UI on view
    //----------------------------------------------------------------------------------------------

    /**
     * Get the instance of the view for implementing runtime permission
     */
    @Override
    public Fragment onPresenterRequestGetFragmentInstance() {
        return this;
    }

    /**
     * Update information about room id on the action bar when connected to room
     *
     * @param roomId room ID generated by SDK
     */
    public void onPresenterRequestUpdateRoomInfo(String roomId) {
        // display roomId
        updateRoomInfo(roomId);
    }

    /**
     * Show local peer button and display local avatar
     * Peer avatar will be the first character of the peer username
     */
    @Override
    public void onPresenterRequestUpdateLocalPeer(String localUserName) {
        btnLocalPeer.setVisibility(View.VISIBLE);
        btnLocalPeer.setText(localUserName.charAt(0) + "");
    }

    /**
     * Update information when new remote peer joining the room at a specific index
     *
     * @param newPeer remote peer joining the room
     * @param index   specific index
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer, int index) {
        updateUiRemotePeerJoin(newPeer, index);
    }

    /**
     * Update information when remote peer left the room
     * We need to re-fill all peers to display correctly peers order
     *
     * @param peersList list of left peers in room
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerLeft(List<SkylinkPeer> peersList) {
        processFillPeers(peersList);
    }

    /**
     * Update the file preview when the user manual input file path to send
     *
     * @param filePath the path of file to be sent
     */
    @Override
    public void onPresenterRequestDisplayFilePreview(String filePath) {
        updateFilePreview(filePath);
    }

    /**
     * Update UI when complete sending file to remote peer(s)
     */
    @Override
    public void onPresenterRequestFileSent() {
        showHideProgressBar(false);
        String log = "Your file has been sent.";
        toastLog(TAG, context, log);
    }

    /**
     * Update UI when complete receiving file from remote peer
     */
    @Override
    public void onPresenterRequestFileReceived(SkylinkPeer remotePeer, String filePath) {
        // Update edit text file path
        txtFilePathInfo.setText("File path received from " + remotePeer.toString() + " : ");
        editFilePath.setText(filePath);
        // move cursor to the end of edit text
        editFilePath.setSelection(editFilePath.getText().length());

        // update the file preview according to the received file
        updateFilePreview(filePath);

        // hide the progress bar
        showHideProgressBar(false);

        String log = "You has received a file.";
        toastLog(TAG, context, log);
    }

    /**
     * Update UI while sending file to remote peer(s)
     *
     * @param percentage the sending file progress
     */
    @Override
    public void onPresenterRequestFileSendProgress(int percentage) {
        showHideProgressBar(true);
        spinnerForeground.setProgress(percentage);
        txtProgressInfo.setText("Sending");
        txtPercentage.setText(percentage + "%");
    }

    /**
     * Update UI while receiving file from remote peer.
     *
     * @param percentage the receiving file progress
     */
    @Override
    public void onPresenterRequestFileReceiveProgress(int percentage) {
        showHideProgressBar(true);
        spinnerForeground.setProgress(percentage);
        txtProgressInfo.setText("Receiving");
        txtPercentage.setText(percentage + "%");
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Get the view widgets from layout
     */
    private void getControlWidgets(View rootView) {
        txtFilePathInfo = rootView.findViewById(R.id.txtFilePathInfo);
        editFilePath = rootView.findViewById(R.id.editFilePath);
        imgView = rootView.findViewById(R.id.img_preview);
        btnChooseFile = rootView.findViewById(R.id.btnChoose);
        btnSendFile = rootView.findViewById(R.id.btnSend);
        spinnerBackground = rootView.findViewById(R.id.progressBarSendingBackground);
        spinnerForeground = rootView.findViewById(R.id.progressBarSendingForeGround);
        txtProgressInfo = rootView.findViewById(R.id.progress_info);
        txtPercentage = rootView.findViewById(R.id.txtPercentage);

        filePreviewContainer = rootView.findViewById(R.id.file_preview_container);
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private void setActionBar() {
        ActionBar actionBar = ((FileTransferActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.custom_action_bar);
        View customBar = actionBar.getCustomView();

        // get the view controls in custom action bar by id
        btnBack = customBar.findViewById(R.id.btnBack);
        txtRoomName = customBar.findViewById(R.id.txtRoomName);
        txtRoomId = customBar.findViewById(R.id.txtRoomId);
        btnLocalPeer = customBar.findViewById(R.id.btnLocalPeer);
        btnRemotePeer1 = customBar.findViewById(R.id.btnRemotePeer1);
        btnRemotePeer2 = customBar.findViewById(R.id.btnRemotePeer2);
        btnRemotePeer3 = customBar.findViewById(R.id.btnRemotePeer3);
    }

    /**
     * Setup the init value for the view controls
     */
    private void initControls() {
        // init setting value for room name in action bar
        txtRoomName.setText(Config.ROOM_NAME_FILE);
        // default sending file option
        txtFilePathInfo.setText("File path to send to all peers : ");

        // Prepare default file for transfer and set UI.
        prepFile(Utils.getFileToTransfer(SAMPLE_FILE_NAME).getAbsolutePath());

        // Copy files raw/R.raw.logo_icon to the device's file system
        Utils.createExternalStoragePrivatePicture(SAMPLE_FILE_NAME);

        // move cursor to the end of edit text
        editFilePath.requestFocus();

        // set onClick and LongClick event for view widgets
        btnBack.setOnClickListener(this);
        btnLocalPeer.setOnClickListener(this);
        btnRemotePeer1.setOnClickListener(this);
        btnRemotePeer2.setOnClickListener(this);
        btnRemotePeer3.setOnClickListener(this);
        btnChooseFile.setOnClickListener(this);
        btnSendFile.setOnClickListener(this);

        btnLocalPeer.setOnLongClickListener(this);
        btnRemotePeer1.setOnLongClickListener(this);
        btnRemotePeer2.setOnLongClickListener(this);
        btnRemotePeer3.setOnLongClickListener(this);

        // change screen orientation in case of landscape mode
        changeScreenOrientation(context.getResources().getConfiguration().orientation);
    }

    /**
     * request info to display from presenter
     * try to connect to room if not connected
     * try to update UI if connected to room
     */
    private void requestViewLayout() {
        if (mPresenter != null) {
            mPresenter.onViewRequestConnectedLayout();
        }
    }

    /**
     * Prepare file before transfer.
     * Sets file path in edit text by default
     */
    private void prepFile(String filePath) {
        editFilePath.setText(filePath);
    }

    /**
     * process select the peer button in action bar in specific index
     * when the user click into the peer button.
     */
    private void processSelectPeer(int index) {
        // inform the presenter layer about the selection
        mPresenter.onViewRequestSelectedRemotePeer(index);

        // check for select remote peer or un select it
        boolean unSelected = mPresenter.onViewRequestGetCurrentSelectedPeer() == 0 ? true : false;
        if (unSelected) {
            txtFilePathInfo.setText("File path to send to all peers : ");
        } else {
            SkylinkPeer selectedPeer = mPresenter.onViewRequestGetPeerByIndex(index);
            txtFilePathInfo.setText("File path to send to " + selectedPeer.toString() + " : ");
        }

        // update the UI of the peer buttons
        updateUISelectRemotePeer(index, unSelected);
    }

    /**
     * Display the dialog of peer info including peer username and peer id
     * when the user long click into the peer button in action bar
     */
    private void displayPeerInfo(int index) {
        SkylinkPeer peer = mPresenter.onViewRequestGetPeerByIndex(index);
        if (index == 0) {
            processDisplayLocalPeer(peer);
        } else {
            processDisplayRemotePeer(peer);
        }
    }

    /**
     * request permission for file browser
     */
    private void requestPermission() {
        if (mPresenter.onViewRequestFilePermission()) {
            // Permission has already been granted
            showFilePicker();
        }
    }

    /**
     * Show the file browser for user to choose
     */
    private void showFilePicker() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");

        startActivityForResult(intent, CHOOSE_FILE_REQUEST_CODE);
    }

    /**
     * Send the file to remote peer(s)
     */
    private void processSendFile() {
        // display file preview correctly to input edit text
        // in case the user manual input the file path
        String filePath = editFilePath.getText().toString();
        File file = new File(filePath);

        if (file.isFile()) {
            // Delegate sending file to presenter layer
            mPresenter.onViewRequestSendFile(file);

            // Update image preview in case of user manual input the file path
            updateFilePreview(file.getAbsolutePath());
        } else {
            toastLog(TAG, context, "Please input correct file path");
        }
    }

    /**
     * show/hide the progress bar when sending/receiving process
     */
    private void showHideProgressBar(boolean isShow) {
        if (isShow) {
            spinnerBackground.setVisibility(View.VISIBLE);
            spinnerForeground.setVisibility(View.VISIBLE);
            txtProgressInfo.setVisibility(View.VISIBLE);
            txtPercentage.setVisibility(View.VISIBLE);
        } else {
            spinnerBackground.setVisibility(View.GONE);
            spinnerForeground.setVisibility(View.GONE);
            txtProgressInfo.setVisibility(View.GONE);
            txtPercentage.setVisibility(View.GONE);
        }
    }

    /**
     * Update the file preview when user choose file from browser /manual input file path /
     * receive file from remote peer
     */
    private void updateFilePreview(String filePath) {
        // Update image preview
        File file = new File(filePath);
        if (file.isFile()) {
            // update the image preview
            if (Utils.isImageFile(filePath)) {
                Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                imgView.setImageBitmap(myBitmap);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    imgView.setImageDrawable(context.getDrawable(R.drawable.ic_file_common));
                }
            }
        }
    }

    /**
     * change screen layout to fit with screen width and height
     * in case of changing screen configuration
     *
     * @param orientation the screen orientation
     */
    private void changeScreenOrientation(int orientation) {
        // change the image preview height to fit with the portrait/landscape screen size
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) filePreviewContainer.getLayoutParams();

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.height = (int) context.getResources().getDimension(R.dimen.dp_300dp);
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.height = (int) context.getResources().getDimension(R.dimen.dp_150dp);
        }

        filePreviewContainer.setLayoutParams(params);
    }
}
