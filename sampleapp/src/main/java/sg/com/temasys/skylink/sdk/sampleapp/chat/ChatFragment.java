package sg.com.temasys.skylink.sdk.sampleapp.chat;

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

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.ChatListAdapter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar;
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomImageButton;

/**
 * A simple {@link CustomActionBar} subclass.
 * This class is responsible for display UI and get user interaction
 */
public class ChatFragment extends CustomActionBar implements ChatContract.View, View.OnClickListener,
        View.OnLongClickListener {

    private final String TAG = ChatFragment.class.getName();

    // presenter instance to implement app logic
    private ChatContract.Presenter presenter;

    // view widgets
    private Button btnSendServerMessage;
    private Button btnSendP2PMessage;
    private Button btnMsgFormatString, btnMsgFormatJson, btnMsgFormatArray;
    private ListView listViewChats;
    private BaseAdapter adapter;
    private EditText editChatMessage;
    private ImageButton btnSend;
    private CustomImageButton btnMsgEncryptSecret;
    private EditText editMsgEncryptionSecret;
    private View dividerEncryptionSecret;
    private boolean showMessageEncryptionSecret = false;

    public static ChatFragment newInstance() {
        return new ChatFragment();
    }

    @Override
    public void setPresenter(ChatContract.Presenter presenter) {
        this.presenter = presenter;
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

        return rootView;
    }

    @Override
    public void onClick(View view) {
        //Defining a click event listener for the buttons in the layout
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
                processSelectPeer(4);
                break;
            case R.id.btnRemotePeer5:
                changeRemotePeerUI(5, true);
                processSelectPeer(5);
                break;
            case R.id.btnRemotePeer6:
                changeRemotePeerUI(6, true);
                processSelectPeer(6);
                break;
            case R.id.btnRemotePeer7:
                changeRemotePeerUI(7, true);
                processSelectPeer(7);
                break;
            case R.id.btnServerMsg:
                processSelectMessageType(ChatPresenter.MESSAGE_TYPE.TYPE_SERVER);
                break;
            case R.id.btnP2PMsg:
                processSelectMessageType(ChatPresenter.MESSAGE_TYPE.TYPE_P2P);
                break;
            case R.id.btnSendMsg:
                processSendMessage();
                break;
            case R.id.msgFormatString:
                processSelectMessageFormat(ChatPresenter.MESSAGE_FORMAT.FORMAT_STRING);
                break;
            case R.id.msgFormatJson:
                processSelectMessageFormat(ChatPresenter.MESSAGE_FORMAT.FORMAT_JSON_OBJECT);
                break;
            case R.id.msgFormatArray:
                processSelectMessageFormat(ChatPresenter.MESSAGE_FORMAT.FORMAT_JSON_ARRAY);
                break;
            case R.id.btn_show_hide_encryption_secret:
                changeUIMessageEncryptionSecret();
                break;
        }
    }

    private void processSelectMessageFormat(ChatPresenter.MESSAGE_FORMAT formatMsg) {
        presenter.processSelectMessageFormat(formatMsg);

        switch (formatMsg) {
            case FORMAT_STRING:
                btnMsgFormatString.setSelected(true);
                btnMsgFormatJson.setSelected(false);
                btnMsgFormatArray.setSelected(false);
                btnMsgFormatString.setBackgroundResource(R.drawable.button_format_msg_selected);
                btnMsgFormatJson.setBackgroundResource(R.drawable.button_format_msg);
                btnMsgFormatArray.setBackgroundResource(R.drawable.button_format_msg);
                break;
            case FORMAT_JSON_OBJECT:
                btnMsgFormatJson.setSelected(true);
                btnMsgFormatString.setSelected(false);
                btnMsgFormatArray.setSelected(false);
                btnMsgFormatJson.setBackgroundResource(R.drawable.button_format_msg_selected);
                btnMsgFormatString.setBackgroundResource(R.drawable.button_format_msg);
                btnMsgFormatArray.setBackgroundResource(R.drawable.button_format_msg);
                break;
            case FORMAT_JSON_ARRAY:
                btnMsgFormatArray.setSelected(true);
                btnMsgFormatString.setSelected(false);
                btnMsgFormatJson.setSelected(false);
                btnMsgFormatArray.setBackgroundResource(R.drawable.button_format_msg_selected);
                btnMsgFormatString.setBackgroundResource(R.drawable.button_format_msg);
                btnMsgFormatJson.setBackgroundResource(R.drawable.button_format_msg);
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

    private void changeUIMessageEncryptionSecret() {
        this.showMessageEncryptionSecret = !this.showMessageEncryptionSecret;
        setUIEncryptionSecret(this.showMessageEncryptionSecret);
    }

    @Override
    public void onDetach() {

        super.onDetach();

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        // in case of changing screen orientation, do not close the connection
        if (!((ChatActivity) context).isChangingConfigurations()) {
            // Inform the presenter to implement closing the connection
            presenter.processExit();
        }
    }

    //----------------------------------------------------------------------------------------------
    // Methods called from the Presenter layer to update UI
    //----------------------------------------------------------------------------------------------

    /**
     * Notify the chat collection to update the new message
     */
    @Override
    public void updateUIChatCollection(boolean isLocalMessage) {
        //notify adapter and update list view selection
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            if (adapter != null) {
                listViewChats.setSelection(adapter.getCount() - 1);

                if (isLocalMessage)
                    updateUIClearMessageInput();
            }
        }
    }

    /**
     * Update GUI into connected state when connected to room
     * Show room id and local peer button and display local avatar by the first character of the local username
     *
     * @param roomId the id of the connected room that generated by SDK
     */
    @Override
    public void updateUIConnected(String roomId) {
        updateRoomInfo(roomId);
        updateUILocalPeer(Config.USER_NAME_CHAT);
    }

    /**
     * Update UI into disconnected state
     */
    @Override
    public void updateUIDisconnected() {
//        if(context == null)
//            return;
//
//        updateRoomInfo(getResources().getString(R.string.guide_room_id));
//
//        btnLocalPeer.setVisibility(GONE);
    }

    /**
     * Update information about new remote peer joining the room at a specific index
     *
     * @param newPeer remote peer joining the room
     * @param index   specific index
     */
    @Override
    public void updateUIRemotePeerConnected(SkylinkPeer newPeer, int index) {
        updateUiRemotePeerJoin(newPeer, index);
    }

    /**
     * Update information about remote peer left the room
     * Re-fill the peers list in order to display correct order of peers in room
     *
     * @param peersList
     */
    @Override
    public void updateUIRemotePeerDisconnected(List<SkylinkPeer> peersList) {
        processFillPeers(peersList);
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------

    /**
     * Get the view widgets by id from the layout
     */
    private void getControlWidgets(View rootView) {
        listViewChats = rootView.findViewById(R.id.lv_messages);
        btnSendServerMessage = rootView.findViewById(R.id.btnServerMsg);
        btnSendP2PMessage = rootView.findViewById(R.id.btnP2PMsg);
        editChatMessage = rootView.findViewById(R.id.editMsg);
        btnSend = rootView.findViewById(R.id.btnSendMsg);
        btnMsgFormatString = rootView.findViewById(R.id.msgFormatString);
        btnMsgFormatJson = rootView.findViewById(R.id.msgFormatJson);
        btnMsgFormatArray = rootView.findViewById(R.id.msgFormatArray);
        btnMsgEncryptSecret = rootView.findViewById(R.id.btn_show_hide_encryption_secret);
        dividerEncryptionSecret = rootView.findViewById(R.id.divider_encryption_secret);
        editMsgEncryptionSecret = rootView.findViewById(R.id.edit_message_encryption_secret);
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private void setActionBar() {
        ActionBar actionBar = ((ChatActivity) getActivity()).getSupportActionBar();
        super.setActionBar(actionBar);
    }

    /**
     * Init the view widgets for the fragment
     */
    private void initControls() {
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
        btnSendServerMessage.setOnClickListener(this);
        btnSendP2PMessage.setOnClickListener(this);
        btnSend.setOnClickListener(this);

        btnMsgFormatString.setOnClickListener(this);
        btnMsgFormatJson.setOnClickListener(this);
        btnMsgFormatArray.setOnClickListener(this);
        btnMsgEncryptSecret.setOnClickListener(this);

        btnLocalPeer.setOnLongClickListener(this);
        btnRemotePeer1.setOnLongClickListener(this);
        btnRemotePeer2.setOnLongClickListener(this);
        btnRemotePeer3.setOnLongClickListener(this);
        btnRemotePeer4.setOnLongClickListener(this);
        btnRemotePeer5.setOnLongClickListener(this);
        btnRemotePeer6.setOnLongClickListener(this);
        btnRemotePeer7.setOnLongClickListener(this);

        // init setting value for room name in action bar
        txtRoomName.setText(Config.ROOM_NAME_CHAT);

        //Defining the ArrayAdapter to set items to ListView
        adapter = new ChatListAdapter(context, R.layout.list_item_remote, presenter.processGetChatCollection());

        //Setting the adapter to the ListView
        listViewChats.setAdapter(adapter);

        // Selected default message type
        btnSendServerMessage.setSelected(true);
        btnSendServerMessage.setBackgroundResource(R.drawable.button_message_type_selected);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            btnSendServerMessage.setTextColor(context.getColor(R.color.color_white));
        }

        // default sending option is sending message to all peers in the room in String format
        btnMsgFormatString.setSelected(true);
        btnMsgFormatString.setBackgroundResource(R.drawable.button_format_msg_selected);

        // default is no encryption secret feature, will show the encryption secret UI when getting info from presenter
        setUIEncryptionSecret(false);
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
        presenter.processSelectMessageType(message_type);

        // update selected state of buttons
        if (message_type == ChatPresenter.MESSAGE_TYPE.TYPE_SERVER) {
            btnSendServerMessage.setSelected(true);
            btnSendServerMessage.setBackgroundResource(R.drawable.button_message_type_selected);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnSendServerMessage.setTextColor(context.getColor(R.color.color_white));
            }
            btnSendP2PMessage.setSelected(false);
            btnSendP2PMessage.setBackgroundResource(R.drawable.button_message_type);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnSendP2PMessage.setTextColor(context.getColor(R.color.color_black));
            }
        } else if (message_type == ChatPresenter.MESSAGE_TYPE.TYPE_P2P) {
            btnSendServerMessage.setSelected(false);
            btnSendServerMessage.setBackgroundResource(R.drawable.button_message_type);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnSendServerMessage.setTextColor(context.getColor(R.color.color_black));
            }
            btnSendP2PMessage.setSelected(true);
            btnSendP2PMessage.setBackgroundResource(R.drawable.button_message_type_selected);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnSendP2PMessage.setTextColor(context.getColor(R.color.color_white));
            }
        }
    }

    /**
     * request info to display from presenter
     * try to connect to room if not connected
     * try to update UI if connected to room
     */
    private void requestViewLayout() {
        if (presenter != null) {
            presenter.processConnectedLayout();
        }
    }

    /**
     * process sending message
     */
    private void processSendMessage() {
        // get the input text from the user
        String message = editChatMessage.getText().toString();

        String encryptedSecret = null;
        if (this.showMessageEncryptionSecret) {
            encryptedSecret = editMsgEncryptionSecret.getText().toString().trim();
        }

        if (message != null && message.length() > 0) {
            // delegate to the presenter to implement sending message
            presenter.processSendMessage(message, encryptedSecret);
        }
    }

    /**
     * process select the peer button in action bar in specific index
     * when the user click into the peer button.
     */
    private void processSelectPeer(int index) {
        // inform the presenter layer about the selection
        presenter.processSelectRemotePeer(index);

        // check for select remote peer or un select it
        boolean unSelected = presenter.processGetCurrentSelectedPeer() == 0 ? true : false;

        // update the UI of the peer buttons
        updateUISelectRemotePeer(index, unSelected);
    }

    /**
     * Display the dialog of peer info including peer username and peer id
     * when the user long click into the peer button in action bar
     */
    private void displayPeerInfo(int index) {
        SkylinkPeer peer = presenter.processGetPeerByIndex(index);
        if (index == 0) {
            processDisplayLocalPeer(peer);
        } else {
            processDisplayRemotePeer(peer);
        }
    }

    /**
     * process exit the demo when people press on back button in the menu
     */
    private void processReturn() {
        presenter.processExit();
        processBack();
    }

    /**
     * Clear the input from edit text
     */
    private void updateUIClearMessageInput() {
        editChatMessage.setText("");
    }

    /**
     * Display or hide the encryption secret UI
     */
    private void setUIEncryptionSecret(boolean isVisible) {
        if (isVisible) {
            dividerEncryptionSecret.setVisibility(View.VISIBLE);
            editMsgEncryptionSecret.setVisibility(View.VISIBLE);
            editMsgEncryptionSecret.requestFocus();
        } else {
            dividerEncryptionSecret.setVisibility(View.GONE);
            editMsgEncryptionSecret.setVisibility(View.GONE);
            editChatMessage.requestFocus();
        }
    }
}
