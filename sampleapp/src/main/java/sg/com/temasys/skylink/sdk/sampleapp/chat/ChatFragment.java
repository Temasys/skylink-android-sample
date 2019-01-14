package sg.com.temasys.skylink.sdk.sampleapp.chat;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.ChatListAdapter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.MultiPartyFragment;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * A simple {@link MultiPartyFragment} subclass.
 * This class is responsible for display UI and get user interaction
 */
public class ChatFragment extends MultiPartyFragment implements ChatContract.View {

    private final String TAG = ChatFragment.class.getName();

    // presenter instance to implement app logic
    private ChatContract.Presenter mPresenter;

    // view widgets
    private Button btnSendServerMessage;
    private Button btnSendP2PMessage;
    private ListView listViewChats;
    private TextView tvRoomDetails;
    private BaseAdapter adapter;
    private EditText editChatMessage;
    private ImageButton btnBack;
    private TextView txtRoomName, txtRoomId;
    private Button btnLocalPeer;
    private Button btnRemotePeer1;
    private Button btnRemotePeer2;
    private Button btnRemotePeer3;
    private ImageButton btnSend;

    public static ChatFragment newInstance() {
        return new ChatFragment();
    }

    @Override
    public void setPresenter(ChatContract.Presenter presenter) {
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
        Log.e("[SA][Chat][onCreate]", Config.ROOM_NAME_CHAT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[SA][Chat][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        // get the UI controls from layout
        getControlWidgets(rootView);

        // setup the action bar
        setActionBar();

        // init the UI controls
        initControls();

        //request an initiative connection
        requestViewLayout();

        //Defining a click event listener for the button "<" in action bar
        btnBack.setOnClickListener(v -> {
            processBack();
        });

        //Defining a click event listener for the button local peer in action bar
        btnLocalPeer.setOnClickListener(v -> {
                    processSelectRemotePeer(0);
                }
        );

        //Defining a click event listener for the button remote peer 1 in action bar
        btnRemotePeer1.setOnClickListener(v -> {
            processSelectRemotePeer(1);
        });

        //Defining a click event listener for the button remote peer 2 in action bar
        btnRemotePeer2.setOnClickListener(v -> {
            processSelectRemotePeer(2);
        });

        //Defining a click event listener for the button remote peer 3 in action bar
        btnRemotePeer3.setOnClickListener(v -> {
            processSelectRemotePeer(3);
        });

        //Defining a long click event listener for the button local peer in action bar
        btnLocalPeer.setOnLongClickListener(v -> {
            processDisplayLocalPeer();
            return true;
        });

        //Defining a long click event listener for the button local peer 1 in action bar
        btnRemotePeer1.setOnLongClickListener(v -> {
            processDisplayRemotePeer(1);
            return true;
        });

        //Defining a long click event listener for the button local peer 2 in action bar
        btnRemotePeer2.setOnLongClickListener(v -> {
            processDisplayRemotePeer(2);
            return true;
        });

        //Defining a long click event listener for the button local peer 3 in action bar
        btnRemotePeer3.setOnLongClickListener(v -> {
            processDisplayRemotePeer(3);
            return true;
        });

        //Defining a click event listener for the button "Send Server Message"
        btnSendServerMessage.setOnClickListener(v -> {
            processSelectMessageType(ChatPresenter.MESSAGE_TYPE.TYPE_SERVER);
        });

        //Defining a click event listener for the button "Send Private Message"
        btnSendP2PMessage.setOnClickListener(v -> {
            processSelectMessageType(ChatPresenter.MESSAGE_TYPE.TYPE_P2P);
        });

        //Defining a click event listener for the button "Send"
        btnSend.setOnClickListener(v -> {
            processSendMessage();
        });

        return rootView;
    }

    @Override
    public void onDetach() {

        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        // in case of changing screen orientation, do not close the connection
        if (!((ChatActivity) context).isChangingConfigurations()) {
            // Inform the presenter to implement closing the connection
            mPresenter.onViewRequestExit();

            //clear all static variables to avoid memory leak
//            peerRadioGroup = null;
//            peerAll = null;
//            peer1 = null;
//            peer2 = null;
//            peer3 = null;
//            peer4 = null;
//
//            mPeers = null;
        }
    }

    //----------------------------------------------------------------------------------------------
    // Methods called from the Presenter to update UI
    //----------------------------------------------------------------------------------------------

    /**
     * Notify the chat collection to update the new message
     */
    @Override
    public void onPresenterRequestUpdateChatCollection() {
        //notify adapter and update listview selection
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            if (adapter != null) {
                listViewChats.setSelection(adapter.getCount() - 1);
            }
        }
    }

    /**
     * Clear the input from edit text
     */
    @Override
    public void onPresenterRequestClearInput() {
        editChatMessage.setText("");
    }

    /**
     * Show local peer button and display local avatar by the first character of the local username
     */
    @Override
    public void onPresenterRequestUpdateLocalPeer(String localUserName) {
        btnLocalPeer.setVisibility(View.VISIBLE);
        btnLocalPeer.setText(localUserName.charAt(0) + "");
    }

    /**
     * Display information about remote peer joining the room
     *
     * @param newPeer remote peer joining the room
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer) {
//        addPeerRadioBtn(newPeer);
    }

    /**
     * Update information about new remote peer joining the room at a specific index
     *
     * @param newPeer remote peer joining the room
     * @param index   specific index
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerJoin(SkylinkPeer newPeer, int index) {
        // Update the peer info in the index button in action bar
        // Using the first character of the peerName for peer avatar
        String peerAvatar = newPeer.getPeerName().charAt(0) + "";
        switch (index) {
            case 1:
                btnRemotePeer1.setVisibility(View.VISIBLE);
                btnRemotePeer1.setText(peerAvatar);
                break;
            case 2:
                btnRemotePeer2.setVisibility(View.VISIBLE);
                btnRemotePeer2.setText(peerAvatar);
                break;
            case 3:
                btnRemotePeer3.setVisibility(View.VISIBLE);
                btnRemotePeer3.setText(peerAvatar);
                break;
        }
    }

    /**
     * Display information about remote peer leaves the room
     *
     * @param remotePeerId remote peer ID leaving the room
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerLeave(String remotePeerId) {
//        removePeerRadioBtn(remotePeerId);
    }

    /**
     * Update information about remote peer leaves the room
     * Remove peer button in the action bar
     *
     * @param index index of the peer to remove
     */
    @Override
    public void onPresenterRequestChangeUiRemotePeerLeave(int index) {
        switch (index) {
            case 1:
                btnRemotePeer1.setVisibility(View.GONE);
                break;
            case 2:
                btnRemotePeer2.setVisibility(View.GONE);
                break;
            case 3:
                btnRemotePeer3.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Display information about list of peers in room
     *
     * @param peersList
     */
    @Override
    public void onPresenterRequestFillPeers(List<SkylinkPeer> peersList) {
//        fillPeerRadioBtn(peersList);

        // refresh the peers first
        refreshPeers();

        // re-fill all peers
        for (int index = 0; index < peersList.size(); index++) {
            SkylinkPeer peer = peersList.get(index);

            String peerAvatar = peer.getPeerName().charAt(0) + "";

            switch (index) {
                case 1:
                    btnRemotePeer1.setVisibility(View.VISIBLE);
                    btnRemotePeer1.setText(peerAvatar);
                    break;
                case 2:
                    btnRemotePeer2.setVisibility(View.VISIBLE);
                    btnRemotePeer2.setText(peerAvatar);
                    break;
                case 3:
                    btnRemotePeer3.setVisibility(View.VISIBLE);
                    btnRemotePeer3.setText(peerAvatar);
                    break;
            }
        }
    }

    /**
     * Display information about updated room details
     *
     * @param roomDetails
     */
    @Override
    public void onPresenterRequestUpdateUi(String roomDetails) {
//        tvRoomDetails.setText(roomDetails);
    }

    /**
     * Update information about room id on the action bar
     *
     * @param roomId
     */
    public void onPresenterRequestUpdateRoomInfo(String roomId) {
        txtRoomId.setText(roomId);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Get the view widget by id
     */
    private void getControlWidgets(View rootView) {
        listViewChats = rootView.findViewById(R.id.lv_messages);

        // The controls from MultiPartyFragment
//        peerRadioGroup = rootView.findViewById(R.id.radio_grp_peers);
//        peerAll = rootView.findViewById(R.id.radio_btn_peer_all);
//        peer1 = rootView.findViewById(R.id.radio_btn_peer1);
//        peer2 = rootView.findViewById(R.id.radio_btn_peer2);
//        peer3 = rootView.findViewById(R.id.radio_btn_peer3);
//        peer4 = rootView.findViewById(R.id.radio_btn_peer4);

        // The controls from chat layout
        btnSendServerMessage = rootView.findViewById(R.id.btnServerMsg);
        btnSendP2PMessage = rootView.findViewById(R.id.btnP2PMsg);
//        tvRoomDetails = rootView.findViewById(R.id.tv_chat_room_details);

        editChatMessage = rootView.findViewById(R.id.editMsg);

        btnSend = rootView.findViewById(R.id.btnSendMsg);
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private void setActionBar() {
        ActionBar actionBar = ((ChatActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.custom_action_bar);
        View customBar = actionBar.getCustomView();

        // get the view control in custom action bar by id
        btnBack = customBar.findViewById(R.id.btnBack);
        txtRoomName = customBar.findViewById(R.id.txtRoomName);
        txtRoomId = customBar.findViewById(R.id.txtRoomId);
        btnLocalPeer = customBar.findViewById(R.id.btnLocalPeer);
        btnRemotePeer1 = customBar.findViewById(R.id.btnRemotePeer1);
        btnRemotePeer2 = customBar.findViewById(R.id.btnRemotePeer2);
        btnRemotePeer3 = customBar.findViewById(R.id.btnRemotePeer3);
    }

    /**
     * Init the view widgets for the fragment
     */
    private void initControls() {
        txtRoomName.setText(Config.ROOM_NAME_CHAT);
        txtRoomId.setText("(Room_id) is being generated...");

        //Defining the ArrayAdapter to set items to ListView
        adapter = new ChatListAdapter(context, R.layout.list_item_remote, mPresenter.onViewRequestGetChatCollection());

        //Setting the adapter to the ListView
        listViewChats.setAdapter(adapter);

        // Selected default message type
        btnSendServerMessage.setSelected(true);
        btnSendServerMessage.setBackgroundResource(R.drawable.button_message_type_selected);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            btnSendServerMessage.setTextColor(context.getColor(R.color.white));
        }

        // default sending option is sending message to all peers in the room
    }

    /**
     * Define the action for button back in action bar
     */
    private void processBack() {
        getActivity().onBackPressed();
    }

    /**
     * Refresh the UI of all remote peers in room
     * by hiding all peers
     */
    private void refreshPeers() {
        btnRemotePeer1.setVisibility(View.GONE);
        btnRemotePeer2.setVisibility(View.GONE);
        btnRemotePeer3.setVisibility(View.GONE);
    }

    /**
     * Display local peer info in the alert dialog when user long click to the local peer button
     */
    private void processDisplayLocalPeer() {
        // display info about local peer including username and peer id
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.peer_info_layout_local, null);
        alertDialogBuilder.setView(view);
        final AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();

        displayPeerInfo(view, 0);
    }

    /**
     * Display local peer info in the alert dialog when user long click to the remote peer button
     */
    private void processDisplayRemotePeer(int peerIndex) {
        // display info about remote peer including username and peer id
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.peer_info_layout_remote, null);
        alertDialogBuilder.setView(view);
        final AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();

        displayPeerInfo(view, peerIndex);
    }

    /**
     * Notify the presenter when the user select the remote peer button
     * Update UI button to selected state
     *
     * @param index the index of the selected peer button
     */
    private void processSelectRemotePeer(int index) {
        // inform the presenter layer about the selection
        mPresenter.onViewRequestSelectedRemotePeer(index);

        boolean unSelected = mPresenter.onViewRequestGetCurrentSelectedPeer() == 0 ? true : false;

        // update selected state of the buttons
        switch (index) {
            // select all peers in group to send message to
            case 0:
                updateButtonUI(unSelected, btnRemotePeer1, btnRemotePeer2, btnRemotePeer3);
                break;
            // select the first/send/third remote peer in room to send message to
            case 1:
                updateButtonUI(unSelected, btnRemotePeer1, btnRemotePeer2, btnRemotePeer3);
                break;
            case 2:
                updateButtonUI(unSelected, btnRemotePeer2, btnRemotePeer1, btnRemotePeer3);
                break;
            case 3:
                updateButtonUI(unSelected, btnRemotePeer3, btnRemotePeer1, btnRemotePeer2);
                break;
        }
    }

    /**
     * Notify the presenter when user select the sending option: send message through signaling server
     * or directly peer-to-peer
     * Update UI button to selected state
     *
     * @param message_type the type of sending
     */
    private void processSelectMessageType(ChatPresenter.MESSAGE_TYPE message_type) {
        // inform the presenter layer about the selection
        mPresenter.onViewRequestSelectedMessageType(message_type);

        // update selected state of buttons
        if (message_type == ChatPresenter.MESSAGE_TYPE.TYPE_SERVER) {
            btnSendServerMessage.setSelected(true);
            btnSendServerMessage.setBackgroundResource(R.drawable.button_message_type_selected);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnSendServerMessage.setTextColor(context.getColor(R.color.white));
            }
            btnSendP2PMessage.setSelected(false);
            btnSendP2PMessage.setBackgroundResource(R.drawable.button_message_type);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnSendP2PMessage.setTextColor(context.getColor(R.color.black));
            }
        } else if (message_type == ChatPresenter.MESSAGE_TYPE.TYPE_P2P) {
            btnSendServerMessage.setSelected(false);
            btnSendServerMessage.setBackgroundResource(R.drawable.button_message_type);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnSendServerMessage.setTextColor(context.getColor(R.color.black));
            }
            btnSendP2PMessage.setSelected(true);
            btnSendP2PMessage.setBackgroundResource(R.drawable.button_message_type_selected);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnSendP2PMessage.setTextColor(context.getColor(R.color.white));
            }
        }
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

//    /**
//     * process sending message to server or directly
//     */
//    private void processSendMessage(boolean isSentToServer) {
//
//        // Pass null for remotePeerId to send message to all users in the room
//        String remotePeerId = getPeerIdSelectedWithWarning();
//        String message = editChatMessage.getText().toString();
//
//        // Sends message using the signalling server or P2P directly
//        // Inform the presenter to implement sending message
//        if (isSentToServer) {
//            mPresenter.onViewRequestSendServerMessage(remotePeerId, message);
//        } else {
//            mPresenter.onViewRequestSendP2PMessage(remotePeerId, message);
//        }
//    }

    /**
     * process sending message
     */
    private void processSendMessage() {
        // get the input text from the user
        String message = editChatMessage.getText().toString();

        // delegate to the presenter to implement sending message
        mPresenter.onViewRequestSendMessage(message);
    }

    /**
     * Display info of the peer (both local and remote) in specific index
     */
    private void displayPeerInfo(View viewContainer, int index) {
        SkylinkPeer remotePeer = mPresenter.onViewRequestGetPeerByIndex(index);

        Button btnAvatar = viewContainer.findViewById(R.id.btnPeerInfoAvatar);
        btnAvatar.setText(remotePeer.getPeerName().charAt(0) + "");

        TextView txtLocalPeerName = viewContainer.findViewById(R.id.txtPeerInfoUserName);
        txtLocalPeerName.setText(remotePeer.getPeerName());

        TextView txtPeerInfoId = viewContainer.findViewById(R.id.txtPeerInfoId);
        txtPeerInfoId.setText(remotePeer.getPeerId());
    }

    /**
     * Update UI of peer button in custom action bar
     * There is default 4 peers in room in mobile SA, including local peer.
     *
     * @param unSelectAll        option that user no select any peer button
     * @param btnSelectedPeer    the selected peer button, there is only one peer can be selected to send message directly P2P
     * @param btnUnSelectedPeer1 the un selected peer button to change UI to un selected state
     * @param btnUnSelectedPeer2 the un selected peer button to change UI to un selected state
     */
    private void updateButtonUI(boolean unSelectAll, Button btnSelectedPeer, Button btnUnSelectedPeer1, Button btnUnSelectedPeer2) {
        if (unSelectAll) {
            btnSelectedPeer.setSelected(false);
            btnSelectedPeer.setBackgroundResource(R.drawable.button_circle_avatar);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnSelectedPeer.setTextColor(context.getColor(R.color.black));
            }
        } else {
            btnSelectedPeer.setSelected(true);
            btnSelectedPeer.setBackgroundResource(R.drawable.button_circle_avatar_selected);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnSelectedPeer.setTextColor(context.getColor(R.color.white));
            }
        }
        btnUnSelectedPeer1.setSelected(false);
        btnUnSelectedPeer1.setBackgroundResource(R.drawable.button_circle_avatar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            btnUnSelectedPeer1.setTextColor(context.getColor(R.color.black));
        }
        btnUnSelectedPeer2.setSelected(false);
        btnUnSelectedPeer2.setBackgroundResource(R.drawable.button_circle_avatar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            btnUnSelectedPeer2.setTextColor(context.getColor(R.color.black));
        }
    }
}
