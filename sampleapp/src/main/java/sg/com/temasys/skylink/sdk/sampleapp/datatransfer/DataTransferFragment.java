package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;


import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.MultiPartyFragment;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for display UI and get user interaction
 */

public class DataTransferFragment extends MultiPartyFragment implements DataTransferContract.View {

    private static final String TAG = DataTransferFragment.class.getName();

    // presenter instance to implement app logic
    private DataTransferContract.Presenter mPresenter;

    // sample data in byte array to be transfered
    private byte[] dataPrivate;
    private byte[] dataGroup;

    private TextView tvRoomDetails;
    private TextView transferStatus;
    private Button btnSendDataRoom;
    private Button btnSendDataPeer;

    public static DataTransferFragment newInstance() {
        return new DataTransferFragment();
    }

    @Override
    public void setPresenter(DataTransferContract.Presenter presenter) {
        this.mPresenter = presenter;
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

        // Defining a click event listener for the button "SEND DATA"
        btnSendDataPeer.setOnClickListener(v -> {
            processSendDataToPeer();
        });

        // Defining a click event listener for the button "SEND DATA [GROUP]"
        btnSendDataRoom.setOnClickListener(v -> {
            processSendDataToGroup();
        });

        return rootView;
    }

    @Override
    public void onDetach() {

        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        // in case of changing screen orientation, do not close the connection
        if (!((DataTransferActivity) context).isChangingConfigurations()) {
            // Inform the presenter to implement closing the connection
            mPresenter.onViewRequestExit();

            //clear all static variables to avoid memory leak
            peerRadioGroup = null;
            peerAll = null;
            peer1 = null;
            peer2 = null;
            peer3 = null;
            peer4 = null;

            mPeers = null;

            //reset sample data
            dataPrivate = null;
            dataGroup = null;
        }
    }

    //----------------------------------------------------------------------------------------------
    // Methods called from the Presenter to update UI
    //----------------------------------------------------------------------------------------------

    /**
     * Display information about list of peers in room
     *
     * @param peersList
     */
    @Override
    public void onPresenterRequestFillPeers(List<SkylinkPeer> peersList) {
        fillPeerRadioBtn(peersList);
    }

    /**
     * Display information about remote peer joining the room
     *
     * @param skylinkPeer remote peer joining the room
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer skylinkPeer) {
        addPeerRadioBtn(skylinkPeer);
    }

    /**
     * Display information about remote peer leaves the room
     *
     * @param remotePeerId remote peer ID leaving the room
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerLeave(String remotePeerId) {
        removePeerRadioBtn(remotePeerId);
    }

    /**
     * Display information about updated room details
     *
     * @param roomDetails
     */
    @Override
    public void onPresenterRequestUpdateUi(String roomDetails) {
        tvRoomDetails.setText(roomDetails);
    }

    /**
     * Get the selected peer id by the user
     */
    @Override
    public String onPresenterRequestGetPeerIdSelected() {
        return getPeerIdSelected();
    }

    /**
     * Check/Uncheck the All peers option
     *
     * @param isSelected the checked state
     */
    @Override
    public void onPresenterRequestSetPeerAllSelected(boolean isSelected) {
        peerAll.setChecked(isSelected);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        // These controls are from MultiPartyFragment
        peerRadioGroup = rootView.findViewById(R.id.radio_grp_peers);
        peerAll = rootView.findViewById(R.id.radio_btn_peer_all);
        peer1 = rootView.findViewById(R.id.radio_btn_peer1);
        peer2 = rootView.findViewById(R.id.radio_btn_peer2);
        peer3 = rootView.findViewById(R.id.radio_btn_peer3);
        peer4 = rootView.findViewById(R.id.radio_btn_peer4);

        // These controls are from data transfer layout
        tvRoomDetails = rootView.findViewById(R.id.tv_data_room_details);
        transferStatus = rootView.findViewById(R.id.txt_data_transfer_status);
        btnSendDataRoom = rootView.findViewById(R.id.btn_send_data_to_room);
        btnSendDataPeer = rootView.findViewById(R.id.btn_send_data_to_peer);
    }

    private void setActionBar() {
        ActionBar actionBar = ((DataTransferActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        setHasOptionsMenu(true);
    }

    private void initControls() {

        // Show info about data sizes that can be transferred.
        getDataTranfered();

        transferStatus.setText("\n" + String.format(getString(R.string.data_transfer_status),
                String.valueOf(dataPrivate.length), String.valueOf(dataGroup.length)));

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
     * get sample data for transfer
     */
    private void getDataTranfered() {
        dataPrivate = Utils.getDataPrivate();
        dataGroup = Utils.getDataGroup();
    }

    /**
     * send data to specific peer with peer id
     * or to all peers in room with null id
     */
    private void processSendDataToPeer() {
        String remotePeerId = getPeerIdSelectedWithWarning();
        // Do not allow button actions if there are no Peers in the room.
        if ("".equals(remotePeerId)) {
            return;
        }

        if (remotePeerId == null) {
            // Send dataGroup to all Peer(s)
            mPresenter.onViewRequestSendData(remotePeerId, dataGroup);

        } else {
            // Send dataPrivate to specific Peer
            mPresenter.onViewRequestSendData(remotePeerId, dataPrivate);
        }
    }

    /*
    * send data to all peers
    * */
    private void processSendDataToGroup() {

        // Inform presenter to implement sending data to all peers
        mPresenter.onViewRequestSendData(null, dataGroup);
    }
}
