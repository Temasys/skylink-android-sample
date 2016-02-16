package sg.com.temasys.skylink.sdk.sampleapp;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

import java.util.ArrayList;
import java.util.ListIterator;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;

/**
 * Created by xiangrong on 16/2/16.
 */
public class MultiPartyFragment extends Fragment {

    static final String BUNDLE_PEER_ID_LIST = "peerIdList";

    // Container for PeerId and nick
    protected ArrayList<Pair<String, String>> peerList;

    // RadioGroup and RadioButtons for selection of Peer(s)
    protected RadioGroup peerRadioGroup;
    protected RadioButton peerAll;
    protected RadioButton peer1;
    protected RadioButton peer2;
    protected RadioButton peer3;
    protected RadioButton peer4;

    /***
     * Multi party methods
     */

    /**
     * Add Peer name to next available Radio button.
     * @param peerId PeerId of the Peer to add.
     * @param nick   Nickname of the Peer to add. Empty string if not available.
     * @param peerList ArrayList<Pair<String peerId, String nick>> of remote Peer info.
     * @param peerAll RadioButton for All Peers.
     * @param peerRadioGroup RadioGroup for Peer RadioButtons.
     */
    void addPeerRadioBtn(String peerId, String nick, ArrayList<Pair<String, String>> peerList,
                         RadioButton peerAll, RadioGroup peerRadioGroup) {
        // Add Peer to peerList
        Pair<String, String> peer = new Pair<>(peerId, nick);
        peerList.add(peer);

        // Add Peer to radio buttons.
        fillPeerRadioBtn(peerList, peerAll, peerRadioGroup);
    }

    /**
     * Populate radio buttons from top to bottom with PeerId and nick.
     * Unpopulated radio buttons will be invisible.
     * Ensure All Peer button is visible IFF there are Peer(s), invisible otherwise.
     * @param peerList ArrayList<Pair<String peerId, String nick>> of remote Peer info.
     * @param peerAll RadioButton for All Peers.
     * @param peerRadioGroup RadioGroup for Peer RadioButtons.
     */
    void fillPeerRadioBtn(ArrayList<Pair<String, String>> peerList,
                          RadioButton peerAll, RadioGroup peerRadioGroup) {
        // Check for initialised peerList
        if (peerList == null) {
            return;
        }
        int peerNum = getPeerNum(peerList);

        // Ensure All Peers button visibility is correct.
        if (peerNum > 0) {
            peerAll.setVisibility(View.VISIBLE);
        } else {
            peerAll.setVisibility(View.INVISIBLE);
        }

        // Populate each radio button appropriately starting after All Peers button.
        for (int i = 1; i < peerRadioGroup.getChildCount(); ++i) {
            RadioButton rb = (RadioButton) peerRadioGroup.getChildAt(i);
            // If there are no more Peers
            if (i > peerNum) {
                // Make radio button invisible
                rb.setVisibility(View.INVISIBLE);
                // Clear text and tag
                rb.setText("");
                rb.setTag("");

            } else {
                // Make radio button visible
                rb.setVisibility(View.VISIBLE);
                // Set text and tag
                Pair<String, String> peerPair = peerList.get(i - 1);
                String peerId = peerPair.first;
                String nick = peerPair.second;
                rb.setText(peerId + " (" + nick + ")");
                rb.setTag(peerId);
            }
        }

    }

    /**
     * Get a String array of PeerIds from a ArrayList<Pair<String, String>> peerList provided.
     * @param peerList
     * @return String array of PeerIds.
     */
    String[] getPeerIdList(ArrayList<Pair<String, String>> peerList) {

        int peerNum = getPeerNum(peerList);
        String[] peerIdList = new String[peerNum];
        // Populate peeIdList with PeerIds.
        for (int i = 0; i < peerNum; ++i) {
            peerIdList[i] = peerList.get(i).first;
        }
        return peerIdList;
    }

    /**
     * Get the PeerId currently selected to send to.
     * Null means to send to all Peer(s).
     * If none is selected, return empty string.
     *
     * @return PeerId to send to.
     * @param peerRadioGroup RadioGroup for Peer RadioButtons.
     * @param radio_btn_peer_all Id of All Peers RadioButton.
     * @param parentActivity Parent Activity of Fragment.
     */
    String getPeerIdSelected(RadioGroup peerRadioGroup,
                             int radio_btn_peer_all, Activity parentActivity) {
        int selectedRB = peerRadioGroup.getCheckedRadioButtonId();

        // If none is selected, return empty string.
        if (selectedRB < 0) {
            return "";
        }

        // Return null of All Peer(s) was selected.
        if (selectedRB == radio_btn_peer_all) {
            return null;
        }

        RadioButton rb = (RadioButton) parentActivity.findViewById(selectedRB);
        String peerId = rb.getTag().toString();
        return peerId;
    }

    /**
     * Get the PeerId currently selected to send to.
     * Null means to send to all Peer(s).
     * Toast warning and return empty string if:
     * - There are no Peers, or
     * - Peer(s) are present but none selected.
     *
     * @param peerList
     * @param peerRadioGroup
     * @param radio_btn_peer_all
     * @param parentActivity
     * @return
     */
    String getPeerIdSelectedWithWarning(ArrayList<Pair<String, String>> peerList,
                                        RadioGroup peerRadioGroup,
                                        int radio_btn_peer_all, Activity parentActivity){
        // Do not allow button actions if there are no Peers in the room.
        if (getPeerNum(peerList) == 0) {
            Toast.makeText(parentActivity,
                    getString(R.string.warn_no_peer_message),
                    Toast.LENGTH_SHORT).show();
            return "";
        }

        String remotePeerId = getPeerIdSelected(peerRadioGroup, radio_btn_peer_all,
                parentActivity);
        // Do not allow button actions if no selection was made.
        if ("".equals(remotePeerId)) {
            Toast.makeText(parentActivity,
                    getString(R.string.warn_no_sel_message),
                    Toast.LENGTH_SHORT).show();
            return "";
        }
        return remotePeerId;
    }

    /**
     * Get number of remote Peers in the room.
     *
     * @return Number of peer(s).
     * @param peerList ArrayList<Pair<String peerId, String nick>> of remote Peer info.
     */
    int getPeerNum(ArrayList<Pair<String, String>> peerList) {

        int peerNum = 0;
        if (peerList == null) {
            return 0;
        }
        peerNum = peerList.size();
        return peerNum;
    }

    /**
     * Populate peerList from a list of PeerIds of remote Peers, using info from SkylinkConnection.
     * @param peerList ArrayList<Pair<String peerId, String nick>> of remote Peer info to populate.
     * @param peerIdList String Array of PeerIds of remote Peer(s) in the room.
     * @param skylinkConnection SkylinkConnection instance serving this Sample.
     */
    void popPeerList(ArrayList<Pair<String, String>> peerList,
                     String[] peerIdList, SkylinkConnection skylinkConnection) {
        // Clear peerList
        peerList.clear();
        // Populate peerList
        for (int i = 0; i < peerIdList.length; ++i) {
            String peerId = peerIdList[i];
            String nick = skylinkConnection.getUserData(peerId).toString();
            Pair<String, String> peer = new Pair<>(peerId, nick);
            peerList.add(peer);
        }
    }

    /**
     * Remove Peer name from Radio buttons.
     * @param peerId PeerId of the Peer to remove.
     * @param peerList ArrayList<Pair<String peerId, String nick>> of remote Peer info.
     * @param peerAll RadioButton for All Peers.
     * @param peerRadioGroup RadioGroup for Peer RadioButtons.
     */
    void removePeerRadioBtn(String peerId, ArrayList<Pair<String, String>> peerList, RadioButton peerAll, RadioGroup peerRadioGroup) {
        // Remove Peer from peerList
        ListIterator peerIter = peerList.listIterator();
        while (peerIter.hasNext()) {
            Pair<String, String> peer = (Pair<String, String>) peerIter.next();
            if (peer.first.equals(peerId)) {
                peerList.remove(peer);
                break;
            }
        }

        // Remove Peer from radio buttons and rearrange them.
        fillPeerRadioBtn(peerList, peerAll, peerRadioGroup);
    }
}
