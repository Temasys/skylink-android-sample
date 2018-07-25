package sg.com.temasys.skylink.sdk.sampleapp.filetransfer;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Pair;
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
import java.util.ArrayList;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

import sg.com.temasys.skylink.sdk.sampleapp.MultiPartyFragment;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.MultiPeersInfo;
import sg.com.temasys.skylink.sdk.sampleapp.utils.PermissionUtils;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

/**
 * A simple {@link MultiPartyFragment} subclass.
 */
public class FileTransferFragment extends MultiPartyFragment implements FileTransferContract.View{

    private final String TAG = FileTransferFragment.class.getName();

    private static FileTransferContract.Presenter mPresenter;

    private PermissionUtils permissionUtils;

    // Constants for configuration change
    private final String BUNDLE_PEERS_JOINED = "PEERS_JOINED";
    private TextView tvRoomDetails;
    private EditText etSenderFilePath;
    private TextView tvFileTransferDetails;
    private ImageView ivFilePreview;
    private Button sendFilePrivate;
    private Button sendFileGroup;
    private final String FILENAME_PRIVATE = "FileTransferPrivate.png";
    private final String FILENAME_GROUP = "FileTransferGroup.png";

    private MultiPeersInfo multiFileTransferPeersInfo;

    public FileTransferFragment() {
        // Required empty public constructor
    }

    public static FileTransferFragment newInstance() {
        return new FileTransferFragment();
    }

    @Override
    public void setPresenter(FileTransferContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_file_transfer, container, false);

        getControlWidgets(rootView);

        setActionBar();

        initComponents();

        // Manual selection of file to send is now enabled.
        // After selecting Peer(s) to send to, click on file path (etSenderFilePath)
        // and enter desired file path.
        etSenderFilePath.setOnClickListener(v -> {
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
        });

        // Check if it was an orientation change
        if (savedInstanceState != null) {
            // Resume previous permission request, if any.
            permissionUtils.permQResume(getContext(), this);

            if (mPresenter.isConnectingOrConnectedPresenterHandler()) {
                // Set states
                multiFileTransferPeersInfo = (MultiPeersInfo) savedInstanceState.getSerializable(BUNDLE_PEERS_JOINED);

                // [MultiParty]
                // Populate peerList
                if(multiFileTransferPeersInfo != null) {
                    popPeerList(multiFileTransferPeersInfo.getPeerIdList());
                    // Set the appropriate UI if already connected.
                    onConnectUIChange();
                }
            }
        } else {
            // This is the start of this sample, reset permission request states.
            permissionUtils.permQReset();

            // Just set room details
            boolean isPeerJoined = multiFileTransferPeersInfo == null ? false : multiFileTransferPeersInfo.isPeerJoined();
            mPresenter.setRoomDetailsPresenterHandler(isPeerJoined);
        }

        // Try to connect to room if not yet connected.
        if (!mPresenter.isConnectingOrConnectedPresenterHandler()) {
            connectToRoomViewHandler();
        }

        // Set file to send based on selected Peer.
        peerRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_btn_peer_all) {
                // Prepare group file for transfer.
                prepFile(Utils.getFileToTransfer(FILENAME_GROUP).getAbsolutePath());
            } else {
                // Prepare private file for transfer.
                prepFile(Utils.getFileToTransfer(FILENAME_PRIVATE).getAbsolutePath());
            }
        });

        // Send file to specific Peer.
        sendFilePrivate.setOnClickListener(v -> {
            // [MultiParty]
            String remotePeerId = getPeerIdSelectedWithWarning();
            // Do not allow button actions if there are no Peers in the room.
            if ("".equals(remotePeerId)) {
                return;
            }

            sendFileViewHandler(remotePeerId);

        });

        // Send file to all Peers in room, i.e. via public (AKA group) message.
        sendFileGroup.setOnClickListener(v -> {
            // [MultiParty]
            // Do not allow button actions if there are no Peers in the room.
            if (getPeerNum() == 0) {
                String log = getString(R.string.warn_no_peer_message);
                toastLog(TAG, context, log);
                return;
            }
            // Select All Peers RadioButton if not already selected
            String remotePeerId = getPeerIdSelected();
            if (remotePeerId != null) {
                peerAll.setChecked(true);
                // Prepare group file for transfer.
                prepFile(Utils.getFileToTransfer(FILENAME_GROUP).getAbsolutePath());
            }
            sendFileViewHandler(null);
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save states for fragment restart
        if(multiFileTransferPeersInfo != null) {
            multiFileTransferPeersInfo.setPeerIdList(getPeerIdList());
            outState.putSerializable(BUNDLE_PEERS_JOINED, multiFileTransferPeersInfo);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        if (!((FileTransferActivity) context).isChangingConfigurations()) {
            mPresenter.disconnectFromRoomPresenterHandler();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        permissionUtils.onRequestPermissionsResultHandler(requestCode, permissions, grantResults, TAG);
    }

    //----------------------------------------------------------------------------------------------
    // View Listeners to update GUI from presenter
    //----------------------------------------------------------------------------------------------

    @Override
    public void setRoomDetailsViewHandler(String roomDetails) {
        tvRoomDetails.setText(roomDetails);
    }

    @Override
    public void fillPeerRadioBtnViewHandler() {
        fillPeerRadioBtn();
    }

    @Override
    public void clearPeerListViewHandler() {
        peerList.clear();
    }

    @Override
    public void onFileReceiveCompleteViewHandler(String msg) {
        tvFileTransferDetails.setText(msg);
    }

    @Override
    public void addPeerRadioBtnViewHandler(String remotePeerId, String nick) {
        addPeerRadioBtn(remotePeerId, nick);
    }

    @Override
    public int getPeerNumViewHandler() {
        return getPeerNum();
    }

    @Override
    public void removePeerRadioBtnViewHandler(String remotePeerId) {
        removePeerRadioBtn(remotePeerId);
    }

    @Override
    public int getPeerlistSizeViewHandler() {
        return peerList.size();
    }

    @Override
    public android.support.v4.app.Fragment getFragmentViewHandler() {
        return this;
    }

    @Override
    public void setIsPeerJoinedViewHandler(boolean isPeerJoined) {
        if(multiFileTransferPeersInfo == null){
            multiFileTransferPeersInfo = new MultiPeersInfo();
        }

        multiFileTransferPeersInfo.setPeerJoined(isPeerJoined);
    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView){
        // [MultiParty]
        peerRadioGroup = (RadioGroup) rootView.findViewById(R.id.radio_grp_peers);
        peerAll = (RadioButton) rootView.findViewById(R.id.radio_btn_peer_all);
        peer1 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer1);
        peer2 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer2);
        peer3 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer3);
        peer4 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer4);

        sendFilePrivate = (Button) rootView.findViewById(R.id.btn_send_file_pte);
        sendFileGroup = (Button) rootView.findViewById(R.id.btn_send_file_grp);
        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_file_room_details);
        etSenderFilePath = (EditText) rootView.findViewById(R.id.et_file_path);
        ivFilePreview = (ImageView) rootView.findViewById(R.id.iv_file_preview);
        tvFileTransferDetails = (TextView) rootView.findViewById(R.id.tv_file_transfer_details);
    }

    private void setActionBar(){
        ActionBar actionBar = ((FileTransferActivity)getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        setHasOptionsMenu(true);
    }

    private void initComponents(){
        permissionUtils = new PermissionUtils(getContext());

        // Prepare default file for transfer and set UI.
        prepFile(Utils.getFileToTransfer(FILENAME_PRIVATE).getAbsolutePath());

        // [MultiParty]
        // Initialise peerList if required.
        if (peerList == null) {
            peerList = new ArrayList<Pair<String, String>>();
        }

        // Copy files raw/R.raw.icon and raw/R.raw.icon_group to the device's file system
        Utils.createExternalStoragePrivatePicture(FILENAME_PRIVATE, FILENAME_GROUP);
    }

    private void connectToRoomViewHandler() {
        mPresenter.connectToRoomPresenterHandler();
    }

    /**
     * Prepare file before transfer. Sets file path in edit text. Sets file preview.
     *
     * @param filePath
     */
    private void prepFile(String filePath) {
        //show preview of file to transfer
        ivFilePreview.setImageURI(Uri.parse(filePath));
        etSenderFilePath.setText(filePath);
    }

    /**
     * Sends a file to a Peer or all Peers in room.
     *
     * @param remotePeerId Peer to send to. Use null to send to all in room.
     */
    private void sendFileViewHandler(String remotePeerId) {
        // Check if valid file
        String filePath = etSenderFilePath.getText().toString();
        File file = new File(filePath);
        if (file.isFile()) {
            ivFilePreview.setImageURI(Uri.parse(filePath));
        } else {
            String log = "Please enter a valid filename";
            toastLog(TAG, context, log);
            return;
        }

        // Send request to peer requesting permission for file transfer
        mPresenter.sendFilePresenterHandler(remotePeerId, filePath);
    }

    /**
     * Change certain UI elements once isConnected() to room or when Peer(s) join or leave.
     */
    private void onConnectUIChange() {
        // [MultiParty]
        mPresenter.setRoomDetailsPresenterHandler(multiFileTransferPeersInfo.isPeerJoined());
        fillPeerRadioBtn();
    }

}
