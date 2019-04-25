package sg.com.temasys.skylink.sdk.sampleapp.utils;


import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Created by muoi.pham on 14/01/19
 * A simple {@link Fragment} subclass.
 * This class is for displaying UI about peer(s) in room in action bar
 */
public class CustomActionBar extends Fragment {

    protected Context context;

    // view widgets in custom action bar
    protected ImageButton btnBack;
    protected TextView txtRoomName, txtRoomId;
    protected Button btnLocalPeer;
    protected Button btnRemotePeer1;
    protected Button btnRemotePeer2;
    protected Button btnRemotePeer3;

    public CustomActionBar() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return null;
    }

    protected void setActionBar(ActionBar actionBar) {
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
     * Show local peer button and display local avatar by the first character of the local username
     */
    protected void updateUILocalPeer(String localUserName) {
        // update the local peer button in the action bar
        btnLocalPeer.setVisibility(View.VISIBLE);
        btnLocalPeer.setText(localUserName.charAt(0) + "");
    }

    /**
     * Update information about new remote peer joining the room at a specific index
     *
     * @param newPeer remote peer joining the room
     * @param index   specific index
     */
    protected void updateUiRemotePeerJoin(SkylinkPeer newPeer, int index) {
        // Update the peer info in the index button in action bar
        // Using the first character of the peerName for peer avatar
        if (newPeer.getPeerName() == null || newPeer.getPeerName().length() == 0) {
            return;
        }

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
     * Update information about remote peer leaves the room
     * Remove peer button in the action bar
     *
     * @param index index of the peer to remove
     */
    protected void updateUiRemotePeerLeave(int index) {
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
     * Display information about list of remote peers in the room
     * The local peer is already displayed in updateUILocalPeer method
     *
     * @param peersList
     */
    protected void processFillPeers(List<SkylinkPeer> peersList) {
        // reset the peers first
        resetRemotePeers();

        // re-fill all peers, except local peer
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
     * Update information about room id on the action bar
     *
     * @param roomId
     */
    protected void updateRoomInfo(String roomId) {
        txtRoomId.setText(roomId);
    }

    /**
     * Define the action for button back in action bar
     */
    protected void processBack() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    /**
     * Display local peer info in the alert dialog when user long click to the local peer button
     */
    protected void processDisplayLocalPeer(SkylinkPeer peer) {
        // display info about local peer including username and peer id
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.peer_info_layout_local, null);
        alertDialogBuilder.setView(view);
        final AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();

        displayPeerDlg(view, peer);
    }

    /**
     * Display local peer info in the alert dialog when user long click to the remote peer button
     */
    protected void processDisplayRemotePeer(SkylinkPeer peer) {
        // display info about remote peer including username and peer id
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.peer_info_layout_remote, null);
        alertDialogBuilder.setView(view);
        final AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();

        displayPeerDlg(view, peer);
    }

    /**
     * Notify the presenter when the user select the remote peer button
     * Update UI button to selected state
     *
     * @param index the index of the selected peer button
     */
    protected void updateUISelectRemotePeer(int index, boolean unSelected) {
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
     * Display info of the peer (both local and remote) in specific index
     */
    protected void displayPeerDlg(View viewContainer, SkylinkPeer remotePeer) {
        Button btnAvatar = viewContainer.findViewById(R.id.btnPeerInfoAvatar);
        btnAvatar.setText(remotePeer.getPeerName().charAt(0) + "");

        TextView txtLocalPeerName = viewContainer.findViewById(R.id.txtPeerInfoUserName);
        txtLocalPeerName.setText(remotePeer.getPeerName());

        TextView txtPeerInfoId = viewContainer.findViewById(R.id.txtPeerInfoId);
        txtPeerInfoId.setText(remotePeer.getPeerId());
    }

    /**
     * Refresh the UI of all remote peers in room
     * by hiding all peers
     */
    private void resetRemotePeers() {
        btnRemotePeer1.setVisibility(View.GONE);
        btnRemotePeer2.setVisibility(View.GONE);
        btnRemotePeer3.setVisibility(View.GONE);
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
                btnSelectedPeer.setTextColor(context.getColor(R.color.color_black));
            }
        } else {
            btnSelectedPeer.setSelected(true);
            btnSelectedPeer.setBackgroundResource(R.drawable.button_circle_avatar_selected);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnSelectedPeer.setTextColor(context.getColor(R.color.color_white));
            }
        }
        btnUnSelectedPeer1.setSelected(false);
        btnUnSelectedPeer1.setBackgroundResource(R.drawable.button_circle_avatar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            btnUnSelectedPeer1.setTextColor(context.getColor(R.color.color_black));
        }
        btnUnSelectedPeer2.setSelected(false);
        btnUnSelectedPeer2.setBackgroundResource(R.drawable.button_circle_avatar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            btnUnSelectedPeer2.setTextColor(context.getColor(R.color.color_black));
        }
    }
}
