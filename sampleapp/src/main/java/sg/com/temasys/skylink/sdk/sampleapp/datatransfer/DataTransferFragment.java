package sg.com.temasys.skylink.sdk.sampleapp.datatransfer;


import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;

import sg.com.temasys.skylink.sdk.sampleapp.utils.MultiPartyFragment;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class DataTransferFragment extends MultiPartyFragment implements DataTransferContract.View {

    private static final String TAG = DataTransferFragment.class.getName();

    //this variable need to be static for configuration change
    private static DataTransferContract.Presenter mPresenter;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_data_transfer, container, false);

        getControlWidgets(rootView);

        setActionBar();

        initComponents();

        requestViewLayout(true);

        btnSendDataPeer.setOnClickListener(v -> {
                    // [MultiParty]
                    String remotePeerId = getPeerIdSelectedWithWarning();
                    // Do not allow button actions if there are no Peers in the room.
                    if ("".equals(remotePeerId)) {
                        return;
                    }

                    if (remotePeerId == null) {
                        // Send dataGroup to all Peer(s)
                        mPresenter.sendDataPresenterHandler(remotePeerId, dataGroup);

                    } else {
                        // Send dataPrivate to specific Peer
                        mPresenter.sendDataPresenterHandler(remotePeerId, dataPrivate);
                    }
                }
        );

        btnSendDataRoom.setOnClickListener(v -> {
                    // [MultiParty]
                    // Do not allow button actions if there are no Peers in the room.
                    if (getPeerNum() == 0) {
                        String log = getString(R.string.warn_no_peer_message);
                        toastLog(TAG, context, log);
                        return;
                    }
                    // Select All Peers RadioButton if not already selected
                    String remotePeerId = getPeerIdSelected(
                    );
                    if (remotePeerId != null) {
                        peerAll.setChecked(true);
                    }

                    // Send dataGroup to all Peers
                    mPresenter.sendDataPresenterHandler(null, dataGroup);
                }

        );

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
    public void onDetach() {
        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        // in case of changing screen orientation, do not close the connection
        if (!((DataTransferActivity) context).isChangingConfigurations()) {
            requestViewLayout(false);

            dataPrivate = null;
            dataGroup = null;
        }
    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------

    private void getControlWidgets(View rootView) {
        peerRadioGroup = (RadioGroup) rootView.findViewById(R.id.radio_grp_peers);
        peerAll = (RadioButton) rootView.findViewById(R.id.radio_btn_peer_all);
        peer1 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer1);
        peer2 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer2);
        peer3 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer3);
        peer4 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer4);

        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_data_room_details);
        transferStatus = (TextView) rootView.findViewById(R.id.txt_data_transfer_status);
        btnSendDataRoom = (Button) rootView.findViewById(R.id.btn_send_data_to_room);
        btnSendDataPeer = (Button) rootView.findViewById(R.id.btn_send_data_to_peer);
    }

    private void setActionBar() {
        ActionBar actionBar = ((DataTransferActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        setHasOptionsMenu(true);
    }

    private void initComponents() {
        // [MultiParty]
        // Initialise peerList if required.
        if (peerList == null) {
            peerList = new ArrayList<Pair<String, String>>();
        }

        // Show info about data sizes that can be transferred.
        getDataTranfered();

        transferStatus.setText(String.format(getString(R.string.data_transfer_status),
                String.valueOf(dataPrivate.length), String.valueOf(dataGroup.length)));

    }

    /**
     * Set dataGroup to contain 2 of dataPrivate.
     * Will get dataPrivate if dataGroup and dataPrivate are null.
     */
    private void getDataTranfered() {
        dataPrivate = Utils.getDataPrivate();
        dataGroup = Utils.getDataGroup();
    }

    //----------------------------------------------------------------------------------------------
    // View Listeners to update GUI from presenter
    //----------------------------------------------------------------------------------------------

    @Override
    public void fillPeerRadioBtnViewHandler() {
        fillPeerRadioBtn();
    }

    @Override
    public void clearPeerListViewHandler() {
        peerList.clear();
    }

    @Override
    public void addPeerRadioBtnViewHandler(String remotePeerId, String nick) {
        addPeerRadioBtn(remotePeerId, nick);
    }

    @Override
    public void removePeerRadioBtnViewHandler(String remotePeerId) {
        removePeerRadioBtn(remotePeerId);
    }

    /**
     * request info to display from presenter
     * try to connect to room if not connected
     * try to disconnect from room if left the room
     */
    private void requestViewLayout(boolean tryToConnect) {
        if (mPresenter != null) {
            mPresenter.onViewLayoutRequestedPresenterHandler();
        }
    }

    public void onUpdateUIViewHandler(String strRoomDetails) {
        tvRoomDetails.setText(strRoomDetails);
    }

    @Override
    public int getPeerNumViewHandler() {
        return getPeerNum();
    }

    @Override
    public int getPeerListSizeViewHandler() {
        return peerList.size();
    }

}
