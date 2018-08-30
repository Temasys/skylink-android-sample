package sg.com.temasys.skylink.sdk.sampleapp.chat;

import android.support.v7.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.MultiPartyFragment;
import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * A simple {@link MultiPartyFragment} subclass.
 */
public class ChatFragment extends MultiPartyFragment implements ChatContract.View {

    private final String TAG = ChatFragment.class.getName();

    private ChatContract.Presenter mPresenter;

    private Button btnSendServerMessage;
    private Button btnSendP2PMessage;
    private ListView listViewChats;
    private TextView tvRoomDetails;
    private BaseAdapter adapter;
    private EditText editChatMessage;

    public static ChatFragment newInstance() {
        return new ChatFragment();
    }

    @Override
    public void setPresenter(ChatContract.Presenter presenter) {
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
        Log.e("Set1", Config.ROOM_NAME_CHAT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[SA][Chat][onCreateView] ");

        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        getControlWidgets(rootView);

        setActionBar();

        initControls();

        requestViewLayout();

        /** Defining a click event listener for the button "Send Server Message" */
        btnSendServerMessage.setOnClickListener(v -> {

            processSendMessage(true);

        });

        /** Defining a click event listener for the button "Send Private Message" */
        btnSendP2PMessage.setOnClickListener(v -> {

            processSendMessage(false);

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
            mPresenter.onViewExit();
        }
    }

    //----------------------------------------------------------------------------------------------
    // View Listeners to update GUI from presenter
    //----------------------------------------------------------------------------------------------

    @Override
    public void onRefreshListView(){

        //refresh adapter and listview selection
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            if (listViewChats != null) {
                listViewChats.setSelection(adapter.getCount() - 1);
            }
        }
    }

    @Override
    public void onClearEditText(){
        editChatMessage.setText("");
    }

    @Override
    public void onAddPeerRadioBtn(SkylinkPeer newPeer) {
        addPeerRadioBtn(newPeer);
    }

    @Override
    public void onRemovePeerRadioBtn(String remotePeerId) {
        removePeerRadioBtn(remotePeerId);
    }

    @Override
    public void onFillPeerRadioBtn(List<SkylinkPeer> peersList) {
        fillPeerRadioBtn(peersList);
    }

    @Override
    public void onUpdateRoomDetails(String roomDetails) {
        tvRoomDetails.setText(roomDetails);
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

        editChatMessage = (EditText) rootView.findViewById(R.id.chatMessage);
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

        /** Defining the ArrayAdapter to set items to ListView */
        adapter = new ArrayAdapter<String>(context, R.layout.list_item, mPresenter.onGetChatMessageCollection());

        /** Setting the adapter to the ListView */
        listViewChats.setAdapter(adapter);
    }

    /**
     * request info to display from presenter
     * try to connect to room if not connected
     * try to update UI if connected to room
     */
    private void requestViewLayout(){
        if(mPresenter != null){
            mPresenter.onViewLayoutRequested();
        }
    }

    private void processSendMessage(boolean isSentToServer){

        // Pass null for remotePeerId to send message to all users in the room
        String remotePeerId = getPeerIdSelectedWithWarning();
        String message = editChatMessage.getText().toString();

        // Sends message using the signalling server or P2P directly
        if(isSentToServer) {
            mPresenter.onSendServerMessage(remotePeerId, message);
        } else{
            mPresenter.onSendP2PMessage(remotePeerId, message);
        }
    }

}
