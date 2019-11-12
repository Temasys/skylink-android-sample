package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.SAMPLE_DATA_NAME;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for display UI and get user interaction
 */

public class DataTransferFragment extends CustomActionBar implements DataTransferContract.View,
        View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = DataTransferFragment.class.getName();

    // The request code for file browser
    private static final int CHOOSE_DATA_REQUEST_CODE = 11;

    // presenter instance to implement app logic
    private DataTransferContract.Presenter presenter;

    // sample data in byte array to be transfered
    private byte[] dataSample;

    // view widgets
    private TextView txtDataPathInfo;
    private EditText editDataPath;
    private TextView txtDataSize;
    private ImageButton btnChooseData;
    private ImageButton btnSendData;
    private LinearLayout dataPreviewContainer;
    private ImageView imgPreview;

    public static DataTransferFragment newInstance() {
        return new DataTransferFragment();
    }

    @Override
    public void setPresenter(DataTransferContract.Presenter presenter) {
        this.presenter = presenter;
    }

    //----------------------------------------------------------------------------------------------
    // Fragment life cycle methods
    //----------------------------------------------------------------------------------------------

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "[SA][DataTransfer][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_data_transfer, container, false);

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
    public void onClick(View view) {
        //Defining a click event listener for the buttons in the action bar.
        switch (view.getId()) {
            case R.id.btnBack:
                processReturn();
                break;
            case R.id.btnLocalPeer:
                changeLocalPeerUI(true);
                processSelectPeer(0);
                break;
            case R.id.btnRemotePeer1:
                changeRemotePeerUI(1, true);
                processSelectPeer(1);
                break;
            case R.id.btnRemotePeer2:
                changeRemotePeerUI(2, true);
                processSelectPeer(2);
                break;
            case R.id.btnRemotePeer3:
                changeRemotePeerUI(3, true);
                processSelectPeer(3);
                break;
            case R.id.btnRemotePeer4:
                changeRemotePeerUI(4, true);
                displayPeerInfo(4);
                break;
            case R.id.btnRemotePeer5:
                changeRemotePeerUI(5, true);
                displayPeerInfo(5);
                break;
            case R.id.btnRemotePeer6:
                changeRemotePeerUI(6, true);
                displayPeerInfo(6);
                break;
            case R.id.btnRemotePeer7:
                changeRemotePeerUI(7, true);
                displayPeerInfo(7);
                break;
            case R.id.btnDataChoose:
                requestPermission();
                break;
            case R.id.btnDataSend:
                processSendData();
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        //Defining a long click event listener for the peer buttons in the action bar.
        switch (view.getId()) {
            case R.id.btnLocalPeer:
                changeLocalPeerUI(true);
                displayPeerInfo(0);
                break;
            case R.id.btnRemotePeer1:
                changeRemotePeerUI(1, true);
                displayPeerInfo(1);
                break;
            case R.id.btnRemotePeer2:
                changeRemotePeerUI(2, true);
                displayPeerInfo(2);
                break;
            case R.id.btnRemotePeer3:
                changeRemotePeerUI(3, true);
                displayPeerInfo(3);
                break;
            case R.id.btnRemotePeer4:
                changeRemotePeerUI(4, true);
                displayPeerInfo(4);
                break;
            case R.id.btnRemotePeer5:
                changeRemotePeerUI(5, true);
                displayPeerInfo(5);
                break;
            case R.id.btnRemotePeer6:
                changeRemotePeerUI(6, true);
                displayPeerInfo(6);
                break;
            case R.id.btnRemotePeer7:
                changeRemotePeerUI(7, true);
                displayPeerInfo(7);
                break;
        }

        return true;
    }

    /**
     * Process file browser permission from the user choice
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
                presenter.onViewRequestPermissionDeny();
            }
        }
    }

    /**
     * Get the file that user choose from browser and then display on UI properly
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // CHOOSE_DATA_REQUEST_CODE.
        if (requestCode == CHOOSE_DATA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // update data info
            // check for select remote peer or un select it
            boolean unSelected = presenter.onViewRequestGetCurrentSelectedPeer() == 0 ? true : false;
            if (unSelected) {
                txtDataPathInfo.setText("Data source path to send to all peers : ");
            } else {
                int index = presenter.onViewRequestGetCurrentSelectedPeer();
                SkylinkPeer selectedPeer = presenter.onViewRequestGetPeerByIndex(index);
                txtDataPathInfo.setText("Data source path to send to " + selectedPeer.toString() + " : ");
            }

            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i(TAG, "Uri: " + uri.toString());

                // update the edit text with file path from Uri
                String filePath = Utils.getFilePath(context, uri);
                editDataPath.setText(filePath);
                // move cursor to the end of edit text
                editDataPath.setSelection(editDataPath.getText().length());
                editDataPath.setVisibility(View.VISIBLE);

                // update the file size text view
                // the array of data to be sent is come from the chosen file
                try {
                    File file = new File(filePath);
                    byte[] fileData = Utils.getDataFromFile(file);
                    if (fileData != null) {
                        txtDataSize.setText(fileData.length + "");
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    txtDataSize.setText("Undefined");
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

    @Override
    public void onDetach() {
        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        // in case of changing screen orientation, do not close the connection
        if (!((DataTransferActivity) context).isChangingConfigurations()) {
            // Inform the presenter to implement closing the connection
            presenter.onViewRequestExit();

            //reset sample data
            dataSample = null;
        }
    }

    //----------------------------------------------------------------------------------------------
    // Methods called from the Presenter to update UI
    //----------------------------------------------------------------------------------------------

    /**
     * Get the instance of the view for implementing runtime permission
     */
    @Override
    public Fragment onPresenterRequestGetFragmentInstance() {
        return this;
    }

    /**
     * Update GUI into connected state when connected to room
     * Show room id and local peer button and display local avatar by the first character of the local username
     *
     * @param roomId the id of the connected room that generated by SDK
     */
    @Override
    public void onPresenterRequestUpdateUIConnected(String roomId) {
        updateRoomInfo(roomId);
        updateUILocalPeer(Config.USER_NAME_DATA);
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
     * Update UI when received data from remote peer
     */
    @Override
    public void onPresenterRequestChangeUIReceivedData(SkylinkPeer remotePeer, byte[] data) {
        txtDataPathInfo.setText("Data received from " + remotePeer.toString() + " : ");
        editDataPath.setText("");

        // update data size text view
        txtDataSize.setText(data.length + "");

        editDataPath.setVisibility(View.GONE);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        txtDataPathInfo = rootView.findViewById(R.id.txtDataPathInfo);
        editDataPath = rootView.findViewById(R.id.editDataPath);
        txtDataSize = rootView.findViewById(R.id.txt_data_size);
        btnChooseData = rootView.findViewById(R.id.btnDataChoose);
        btnSendData = rootView.findViewById(R.id.btnDataSend);
        dataPreviewContainer = rootView.findViewById(R.id.data_preview_container);
        imgPreview = rootView.findViewById(R.id.img_data_preview);
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private void setActionBar() {
        ActionBar actionBar = ((DataTransferActivity) getActivity()).getSupportActionBar();
        super.setActionBar(actionBar);
    }

    private void initControls() {
        // init setting value for room name in action bar
        txtRoomName.setText(Config.ROOM_NAME_DATA);
        // default sending data option
        txtDataPathInfo.setText("Data source path to send to all peers : ");

        // Prepare default data for transfer and set UI.
        prepFile(Utils.getFileToTransfer(SAMPLE_DATA_NAME).getAbsolutePath());

        // Copy files raw/R.raw.logo_icon to the device's file system
        Utils.createExternalStoragePrivatePicture(SAMPLE_DATA_NAME);

        // move cursor to the end of edit text
        editDataPath.requestFocus();

        // set the default data to send
        getSampleDataTranfered();
        txtDataSize.setText(dataSample.length + "");

        // set onClick and LongClick event for view widgets
        btnBack.setOnClickListener(this);
        btnLocalPeer.setOnClickListener(this);
        btnRemotePeer1.setOnClickListener(this);
        btnRemotePeer2.setOnClickListener(this);
        btnRemotePeer3.setOnClickListener(this);
        btnRemotePeer4.setOnClickListener(this);
        btnRemotePeer5.setOnClickListener(this);
        btnRemotePeer6.setOnClickListener(this);
        btnRemotePeer7.setOnClickListener(this);

        btnChooseData.setOnClickListener(this);
        btnSendData.setOnClickListener(this);

        btnLocalPeer.setOnLongClickListener(this);
        btnRemotePeer1.setOnLongClickListener(this);
        btnRemotePeer2.setOnLongClickListener(this);
        btnRemotePeer3.setOnLongClickListener(this);
        btnRemotePeer4.setOnLongClickListener(this);
        btnRemotePeer5.setOnLongClickListener(this);
        btnRemotePeer6.setOnLongClickListener(this);
        btnRemotePeer7.setOnLongClickListener(this);
        // change screen orientation in case of landscape mode
        changeScreenOrientation(context.getResources().getConfiguration().orientation);
    }


    /**
     * request info to display from presenter
     * try to connect to room if not connected
     * try to update UI if connected to room
     */
    private void requestViewLayout() {
        if (presenter != null) {
            presenter.onViewRequestConnectedLayout();
        }
    }

    /**
     * get sample data for transfer
     */
    private void getSampleDataTranfered() {
        dataSample = Utils.getDataSample();
    }

    /**
     * Prepare file that provide data array before transfer.
     * Sets file path in edit text by default
     */
    private void prepFile(String filePath) {
        editDataPath.setText(filePath);
    }

    /**
     * process select the peer button in action bar in specific index
     * when the user click into the peer button.
     */
    private void processSelectPeer(int index) {
        // inform the presenter layer about the selection
        presenter.onViewRequestSelectedRemotePeer(index);

        // check for select remote peer or un select it
        boolean unSelected = presenter.onViewRequestGetCurrentSelectedPeer() == 0 ? true : false;
        if (unSelected) {
            txtDataPathInfo.setText("Data source path to send to all peers : ");
        } else {
            SkylinkPeer selectedPeer = presenter.onViewRequestGetPeerByIndex(index);
            txtDataPathInfo.setText("Data source path to send to " + selectedPeer.toString() + " : ");
        }

        // update the UI of the peer buttons
        updateUISelectRemotePeer(index, unSelected);
    }

    /**
     * Display the dialog of peer info including peer username and peer id
     * when the user long click into the peer button in action bar
     */
    private void displayPeerInfo(int index) {
        SkylinkPeer peer = presenter.onViewRequestGetPeerByIndex(index);
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
        if (presenter.onViewRequestFilePermission()) {
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

        startActivityForResult(intent, CHOOSE_DATA_REQUEST_CODE);
    }

    /**
     * Send the data to remote peer(s)
     */
    private void processSendData() {
        // display file preview correctly to input edit text
        // in case the user manual input the file path
        String filePath = editDataPath.getText().toString();
        File file = new File(filePath);

        boolean isValidPath = false;
        if (file.isFile()) {
            // Delegate sending data to presenter layer
            try {
                presenter.onViewRequestSendData(Utils.getDataFromFile(file));
                isValidPath = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (!isValidPath) {
            toastLog(TAG, context, "Please input correct file path to get data");
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
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) dataPreviewContainer.getLayoutParams();

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.height = (int) context.getResources().getDimension(R.dimen.dp_300dp);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                imgPreview.setImageDrawable(context.getDrawable(R.drawable.ic_data_upload_150));
            }

        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.height = (int) context.getResources().getDimension(R.dimen.dp_150dp);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                imgPreview.setImageDrawable(context.getDrawable(R.drawable.ic_data_upload_70));
            }
        }

        dataPreviewContainer.setLayoutParams(params);
    }

    /**
     * process exit the demo when people press on back button in the menu
     */
    private void processReturn() {
        presenter.onViewRequestExit();
        processBack();
    }
}
