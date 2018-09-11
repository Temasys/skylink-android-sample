package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.MultiPartyFragment;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

/**
 * A simple {@link MultiPartyFragment} subclass.
 */
public class FileTransferFragment extends MultiPartyFragment implements FileTransferContract.View {

    private final String TAG = FileTransferFragment.class.getName();

    private final String FILENAME_PRIVATE = "FileTransferPrivate.png";
    private final String FILENAME_GROUP = "FileTransferGroup.png";

    private FileTransferContract.Presenter mPresenter;

    //static variables for update UI when changing configuration
    //because we use different layout for landscape mode
    private static TextView tvRoomDetails;
    private static EditText etSenderFilePath;
    private static TextView tvFileTransferDetails;
    private static ImageView ivFilePreview;

    private Button sendFilePrivate;
    private Button sendFileGroup;

    public static FileTransferFragment newInstance() {
        return new FileTransferFragment();
    }

    @Override
    public void setPresenter(FileTransferContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "[SA][FileTransfer][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_file_transfer, container, false);

        getControlWidgets(rootView);

        setActionBar();

        initControls();

        requestViewLayout();

        // Defining a click event listener for the edit text File path
        etSenderFilePath.setOnClickListener(v -> {

            processSetFilePath();
        });

        // Defining a click event listener for the radio group
        peerRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {

            processSelectPeer(checkedId);

        });

        // Defining a click event listener for the button SEND FILE
        sendFilePrivate.setOnClickListener(v -> {

            processSendFilePrivate();

        });

        // Defining a click event listener for the button SEND FILE [GROUP]
        sendFileGroup.setOnClickListener(v -> {

            processSendFileGroup();

        });

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

            //clear all static variables to avoid memory leak
            peerRadioGroup = null;
            peerAll = null;
            peer1 = null;
            peer2 = null;
            peer3 = null;
            peer4 = null;

            mPeers = null;

            tvRoomDetails = null;
            etSenderFilePath = null;
            tvFileTransferDetails = null;
            ivFilePreview = null;
        }
    }

    @Override
    public Fragment onPresenterRequestGetFragmentInstance() {
        return this;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        mPresenter.onViewRequestPermissionsResult(requestCode, permissions, grantResults, TAG);
    }

    @Override
    public void onPresenterRequestFillPeers(List<SkylinkPeer> peersList) {
        fillPeerRadioBtn(peersList);
    }

    @Override
    public void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer skylinkPeer) {
        addPeerRadioBtn(skylinkPeer);
    }

    @Override
    public void onPresenterRequestChangeUiRemotePeerLeave(String remotePeerId) {
        removePeerRadioBtn(remotePeerId);
    }

    @Override
    public String onPresenterRequestGetPeerIdSelected() {
        return getPeerIdSelected();
    }

    @Override
    public void onPresenterRequestSetPeerAllSelected(boolean isSelected) {
        peerAll.setChecked(isSelected);
    }

    @Override
    public void onPresenterRequestDisplayFilePreview(Uri imgUri) {
        if(ivFilePreview != null)
            ivFilePreview.setImageURI(imgUri);
    }

    @Override
    public void onPresenterRequestDisplayFileReveicedInfo(String info){
        if(tvFileTransferDetails != null)
            tvFileTransferDetails.setText(info);
    }

    @Override
    public void onPresenterRequestUpdateUi(String roomDetails) {
        if(tvRoomDetails != null)
            tvRoomDetails.setText(roomDetails);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        // [MultiParty]
        peerRadioGroup = rootView.findViewById(R.id.radio_grp_peers);
        peerAll = rootView.findViewById(R.id.radio_btn_peer_all);
        peer1 = rootView.findViewById(R.id.radio_btn_peer1);
        peer2 = rootView.findViewById(R.id.radio_btn_peer2);
        peer3 = rootView.findViewById(R.id.radio_btn_peer3);
        peer4 = rootView.findViewById(R.id.radio_btn_peer4);

        tvRoomDetails = rootView.findViewById(R.id.tv_file_room_details);
        etSenderFilePath = rootView.findViewById(R.id.et_file_path);
        ivFilePreview = rootView.findViewById(R.id.iv_file_preview);
        tvFileTransferDetails = rootView.findViewById(R.id.tv_file_transfer_details);

        sendFilePrivate = rootView.findViewById(R.id.btn_send_file_pte);
        sendFileGroup = rootView.findViewById(R.id.btn_send_file_grp);
    }

    private void setActionBar() {
        ActionBar actionBar = ((FileTransferActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        setHasOptionsMenu(true);
    }

    private void initControls() {

        // Prepare default file for transfer and set UI.
        prepFile(Utils.getFileToTransfer(FILENAME_PRIVATE).getAbsolutePath());

        // Copy files raw/R.raw.icon and raw/R.raw.icon_group to the device's file system
        Utils.createExternalStoragePrivatePicture(FILENAME_PRIVATE, FILENAME_GROUP);
    }

    /**
     * request info to display from presenter
     * try to connect to room if not connected
     * try to update UI if connected to room
     */
    private void requestViewLayout() {
        if (mPresenter != null) {
            mPresenter.onViewRequestLayout();
        }
    }

    /**
     * Prepare file before transfer. Sets file path in edit text. Sets file preview.
     */
    private void prepFile(String filePath) {
        //show preview of file to transfer
        if(ivFilePreview != null)
            ivFilePreview.setImageURI(Uri.parse(filePath));

        if(etSenderFilePath != null)
            etSenderFilePath.setText(filePath);
    }


    /** Manual selection of file to send is now enabled.
     * After selecting Peer(s) to send to, click on file path (etSenderFilePath)
     * and enter desired file path.
     */
    private void processSetFilePath() {
        // Create Dialog to change file path.
        // File path will be validated to be a file before change is accepted.
        AlertDialog.Builder changePathDialogBuilder =
                new AlertDialog.Builder(getContext());
        changePathDialogBuilder.setTitle("Set the path to the file to be transferred.");

        // Create EditText for file path in Dialog.
        final EditText filePathEdtTxt = new EditText(getContext());
        filePathEdtTxt.setText(etSenderFilePath.getText());
        filePathEdtTxt.setMovementMethod(LinkMovementMethod.getInstance());
        changePathDialogBuilder.setView(filePathEdtTxt);

        // Create a Positive button but this will be overridden later.
        changePathDialogBuilder.setPositiveButton("Ok",
                (dialog, which) -> {
                    // Do nothing here as this will be overridden later.
                });

        // Negative button to cancel setting of file path.
        changePathDialogBuilder.setNegativeButton("Cancel", null);
        final AlertDialog changePathDialog = changePathDialogBuilder.create();
        changePathDialog.show();

        // Override the handler to prevent changePathDialog from auto closing
        // after clicking button.
        // Note: Has to be after show() is called.
        changePathDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                v1 -> {
                    // Close changePathDialog only if file exists at path provided.
                    final String filePathNew = filePathEdtTxt.getText().toString();

                    File file = new File(filePathNew);
                    if (file.isFile()) {
                        // Set file to be transferred to this path.
                        prepFile(filePathNew);
                        changePathDialog.dismiss();
                    } else {
                        // Else changePathDialog stays open.
                        String log = "[SA] File does not exist at newly provided path. "
                                + "Please make corrections or cancel operation.";
                        toastLog(TAG, context, log);
                        Log.e(TAG, log);
                    }
                });
    }

    // Set file to send based on selected Peer.
    private void processSelectPeer(int checkedId){

        if (checkedId == R.id.radio_btn_peer_all) {
            // Prepare group file for transfer.
            prepFile(Utils.getFileToTransfer(FILENAME_GROUP).getAbsolutePath());
        } else {
            // Prepare private file for transfer.
            prepFile(Utils.getFileToTransfer(FILENAME_PRIVATE).getAbsolutePath());
        }
    }

    // Send file to specific Peer.
    private void processSendFilePrivate(){
        // [MultiParty]
        String remotePeerId = getPeerIdSelectedWithWarning();
        // Do not allow button actions if there are no Peers in the room.
        if ("".equals(remotePeerId)) {
            return;
        }

        String filePath = etSenderFilePath.getText().toString();

        mPresenter.onViewRequestSendFile(remotePeerId, filePath);
    }

    // Send file to all Peers in room, i.e. via public (AKA group) message.
    private void processSendFileGroup(){

        // Prepare group file for transfer.
        prepFile(Utils.getFileToTransfer(FILENAME_GROUP).getAbsolutePath());

        String filePath = etSenderFilePath.getText().toString();

        mPresenter.onViewRequestSendFile(null, filePath);

    }
}
