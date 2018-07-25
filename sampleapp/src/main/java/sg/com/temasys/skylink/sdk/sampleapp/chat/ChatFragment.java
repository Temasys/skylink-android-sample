package sg.com.temasys.skylink.sdk.sampleapp.chat;


import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.MultiPartyFragment;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.MultiPeersInfo;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends MultiPartyFragment
        implements ChatContract.View {

    private final String TAG = ChatFragment.class.getName();

    //this variable need to be static for configuration change
    private static ChatContract.Presenter mPresenter;

    // Constants for configuration change
    private String BUNDLE_PEER_JOINED = "PEER_JOINED";

    private static List<String> chatMessageCollection;

    private Button btnSendServerMessage;
    private Button btnSendP2PMessage;
    private ListView listViewChats;
    private TextView tvRoomDetails;
    private BaseAdapter adapter;

    private MultiPeersInfo multiChatPeersInfo;

    public ChatFragment() {
        // Required empty public constructor
    }

    public static ChatFragment newInstance() {
        return new ChatFragment();
    }

    @Override
    public void setPresenter(ChatContract.Presenter presenter) {
        this.mPresenter = presenter;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        getControlWidgets(rootView);

        setActionBar();

        initControls();

        // Check if it was an orientation change
        if (savedInstanceState != null) {

            if (mPresenter.isConnectingOrConnectedPresenterHandler()) {
                // Set states
                multiChatPeersInfo = (MultiPeersInfo) savedInstanceState.getSerializable(BUNDLE_PEER_JOINED);

                if(multiChatPeersInfo != null) {

                    //save isPeerJoined into service
                    mPresenter.saveIsPeerJoinPresenterHandler(multiChatPeersInfo.isPeerJoined());

                    // [MultiParty]
                    // Populate peerList
                    popPeerList(multiChatPeersInfo.getPeerIdList());
                    // Set the appropriate UI if already connected.
                    onConnectUIChangeViewHandler();
                }
            }
        } else {
            // Just set room details
            setRoomDetailsViewHandler();
        }

        // Try to connect to room if not yet connected.
        if (!mPresenter.isConnectingOrConnectedPresenterHandler()) {
            connectToRoomViewHandler();
        }

        /** Defining a click event listener for the button "Send Server Message" */
        btnSendServerMessage.setOnClickListener(v -> {
            // [MultiParty]
            boolean isPrivate = true;
            String remotePeerId = getPeerIdSelectedWithWarning();
            // Do not allow button actions if there are no Peers in the room.
            if ("".equals(remotePeerId)) {
                return;
            } else if (remotePeerId == null) {
                isPrivate = false;
            }

            //Add chat message to the listview
            String message = addSelfMessageToListViewViewHandler(isPrivate, false);

            // Sends message using the signalling server
            // Pass null for remotePeerId to send message to all users in the room
            mPresenter.sendServerMessagePresenterHandler(remotePeerId, message);

        });

        /** Defining a click event listener for the button "Send Private Message" */
        btnSendP2PMessage.setOnClickListener(v -> {
            // [MultiParty]
            boolean isPrivate = true;
            String remotePeerId = getPeerIdSelectedWithWarning();
            // Do not allow button actions if there are no Peers in the room.
            if ("".equals(remotePeerId)) {
                return;
            } else if (remotePeerId == null) {
                isPrivate = false;
            }

            //Add chat message to the listview
            String message = addSelfMessageToListViewViewHandler(isPrivate, true);

            mPresenter.sendP2PMessagePresenterHandler(remotePeerId, message);
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("Set1", Config.ROOM_NAME_CHAT);
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
        if(multiChatPeersInfo != null) {
            multiChatPeersInfo.setPeerIdList(getPeerIdList());
            outState.putSerializable(BUNDLE_PEER_JOINED, multiChatPeersInfo);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        listViewChats = null;

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already connected and not changing orientation.

        if (!((ChatActivity) context).isChangingConfigurations()) {
            mPresenter.disconnectFromRoomPresenterHandler();
        }

    }

    //----------------------------------------------------------------------------------------------
    // View Listeners to update GUI from presenter
    //----------------------------------------------------------------------------------------------

    @Override
    public void clearPeerListViewHandler() {
        peerList.clear();
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
    public int getPeerListSizeViewHandler() {
        return peerList.size();
    }

    @Override
    public void listViewRefreshViewHandler() {
        listViewRefresh();
    }

    @Override
    public void setRoomDetailsViewHandler(String roomDetails) {
        tvRoomDetails.setText(roomDetails);
    }

    @Override
    public void clearChatMessageCollectionViewHandler() {
        chatMessageCollection.clear();
    }

    @Override
    public void addToChatMessageCollectionViewHandler(String chatStr) {
        chatMessageCollection.add(chatStr);
    }

    @Override
    public void fillPeerRadioBtnViewHandler() {
        fillPeerRadioBtn();
    }

    @Override
    public void setMultiChatPeersInfoViewHandler(boolean isPeerJoined) {
        if(multiChatPeersInfo == null){
            multiChatPeersInfo = new MultiPeersInfo();
        }

        multiChatPeersInfo.setPeerJoined(isPeerJoined);

    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------


    private void getControlWidgets(View rootView){
        listViewChats = (ListView) rootView.findViewById(R.id.lv_messages);

        // [MultiParty]
        peerRadioGroup = (RadioGroup) rootView.findViewById(R.id.radio_grp_peers);
        peerAll = (RadioButton) rootView.findViewById(R.id.radio_btn_peer_all);
        peer1 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer1);
        peer2 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer2);
        peer3 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer3);
        peer4 = (RadioButton) rootView.findViewById(R.id.radio_btn_peer4);

        btnSendServerMessage = (Button) rootView.findViewById(R.id.btn_send_server_message);
        btnSendP2PMessage = (Button) rootView.findViewById(R.id.btn_send_p2p_message);
        tvRoomDetails = (TextView) rootView.findViewById(R.id.tv_chat_room_details);
    }

    private void setActionBar(){
        ActionBar actionBar = ((ChatActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        setHasOptionsMenu(true);
    }

    private void initControls(){
        if (chatMessageCollection == null) {
            chatMessageCollection = new ArrayList();
        }

        /** Defining the ArrayAdapter to set items to ListView */
        adapter = new ArrayAdapter<String>(context, R.layout.list_item, chatMessageCollection);
        /** Setting the adapter to the ListView */
        listViewChats.setAdapter(adapter);

        // [MultiParty]
        // Initialise peerList if required.
        if (peerList == null) {
            peerList = new ArrayList<Pair<String, String>>();
        }
    }

    private void connectToRoomViewHandler() {
        mPresenter.connectToRoomPresenterHandler();
    }

    /**
     * Change certain UI elements once connected to room or when Peer(s) join or leave.
     */
    private void onConnectUIChangeViewHandler() {
        listViewRefresh();
        setRoomDetailsViewHandler();

        // [MultiParty]
        fillPeerRadioBtn();
    }

    /**
     * Set the room details on UI.
     */
    private void setRoomDetailsViewHandler() {

        boolean isPeerJoined = multiChatPeersInfo == null ? false : multiChatPeersInfo.isPeerJoined();

        mPresenter.setRoomDetailsPresenterHandler(isPeerJoined);
    }

    /**
     * Retrieves self message written in edit text and adds it to the chatlistview.
     * Will refresh listView.
     *
     * @param isPrivateMessage
     * @param isP2P
     * @return message that was added to the listview
     */
    private String addSelfMessageToListViewViewHandler(boolean isPrivateMessage, boolean isP2P) {
        EditText editChatMessage = (EditText) ((ChatActivity) context).findViewById(R.id.chatMessage);

        String prefix = "You : ";
        prefix += isPrivateMessage ? "[PTE]" : "[GRP]";
        prefix += isP2P ? "[P2P] " : "[SIG] ";
        String message = editChatMessage.getText().toString();
        chatMessageCollection.add(prefix + message);
        editChatMessage.setText("");
        listViewRefresh();
        return message;
    }

    private void listViewRefresh() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            if (listViewChats != null) {
                listViewChats.setSelection(adapter.getCount() - 1);
            }
        }
    }

}
