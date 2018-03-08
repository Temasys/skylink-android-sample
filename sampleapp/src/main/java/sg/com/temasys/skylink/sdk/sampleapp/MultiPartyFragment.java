package sg.com.temasys.skylink.sdk.sampleapp;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.ListIterator;

import static sg.com.temasys.skylink.sdk.sampleapp.Utils.toastLog;

/**
 * Created by xiangrong on 16/2/16.
 */
public class MultiPartyFragment extends Fragment {

    static final String BUNDLE_PEER_ID_LIST = "peerIdList";
    private static final String TAG = MultiPartyFragment.class.getName();

    // Container for PeerId and nick
    // ArrayList<Pair<String peerId, String nick>> of remote Peer info.
    protected static ArrayList<Pair<String, String>> peerList;

    // RadioGroup and RadioButtons for selection of Peer(s)
    protected RadioGroup peerRadioGroup;
    protected RadioButton peerAll;
    protected RadioButton peer1;
    protected RadioButton peer2;
    protected RadioButton peer3;
    protected RadioButton peer4;
    protected Context context;

    /***
     * Multi party methods
     */

    /**
     * Add Peer name to next available Radio button.
     *
     * @param peerId PeerId of the Peer to add.
     * @param nick   Nickname of the Peer to add. Empty string if not available.
     */
    void addPeerRadioBtn(String peerId, String nick) {
        String logTag = "[SA][addPeerRadioBtn] ";
        String log = logTag + "Adding Peer \"" + peerId + "\"(" + nick + ") to peerList...";
        Log.d(TAG, log);

        // Add Peer to peerList
        Pair<String, String> peer = new Pair<>(peerId, nick);
        peerList.add(peer);

        // Add Peer to radio buttons.
        log = logTag + "Populating RadioButton UI with added Peer.";
        Log.d(TAG, log);
        fillPeerRadioBtn();
    }

    /**
     * Populate radio buttons from top to bottom with PeerId and nick.
     * Unpopulated radio buttons will be invisible.
     * Ensure All Peer button is visible IFF there are Peer(s), invisible otherwise.
     */
    void fillPeerRadioBtn() {
        // Check for initialised peerList
        if (peerList == null) {
            return;
        }
        int peerNum = getPeerNum();
        String logTag = "[SA][fillPeerRadioBtn] ";
        String log = logTag + "Populating RadioButton UI with " + peerNum + " Peer(s)...";
        Log.d(TAG, log);

        // Ensure All Peers button visibility is correct.
        if (peerNum > 0) {
            peerAll.setVisibility(View.VISIBLE);
        } else {
            peerAll.setVisibility(View.INVISIBLE);
            log = logTag + "There are no remote Peers, so there will be no RadioButtons.";
            Log.d(TAG, log);
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
                log = logTag + "RadioButton " + i + " is invisible as there are only " +
                        peerNum + " remote Peer(s).";
                Log.d(TAG, log);

            } else {
                // Make radio button visible
                rb.setVisibility(View.VISIBLE);
                // Set text and tag
                Pair<String, String> peerPair = peerList.get(i - 1);
                String peerId = peerPair.first;
                String nick = peerPair.second;
                rb.setText(peerId + " (" + nick + ")");
                rb.setTag(peerId);
                log = logTag + "RadioButton " + i + " is visible as there are " +
                        peerNum + " remote Peer(s).";
                Log.d(TAG, log);
            }
        }
    }

    /**
     * Get a String array of PeerIds from a ArrayList<Pair<String, String>> peerList provided.
     *
     * @return String array of PeerIds.
     */
    String[] getPeerIdList() {

        int peerNum = getPeerNum();
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
     */
    String getPeerIdSelected() {
        int selectedRB = peerRadioGroup.getCheckedRadioButtonId();

        // If none is selected, return empty string.
        if (selectedRB < 0) {
            return "";
        }

        // Return null of All Peer(s) was selected.
        if (selectedRB == peerAll.getId()) {
            return null;
        }

        RadioButton rb = (RadioButton) ((MainActivity) context).findViewById(selectedRB);
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
     * @return
     */
    String getPeerIdSelectedWithWarning() {
        // Do not allow button actions if there are no Peers in the room.
        if (getPeerNum() == 0) {
            String log = getString(R.string.warn_no_peer_message);
            toastLog(TAG, context, log);
            return "";
        }

        String remotePeerId = getPeerIdSelected(
        );
        // Do not allow button actions if no selection was made.
        if ("".equals(remotePeerId)) {
            String log = getString(R.string.warn_no_sel_message);
            toastLog(TAG, context, log);
            return "";
        }
        return remotePeerId;
    }

    /**
     * Get number of remote Peers in the room.
     *
     * @return Number of peer(s).
     */
    int getPeerNum() {

        int peerNum = 0;
        if (peerList == null) {
            return 0;
        }
        peerNum = peerList.size();
        return peerNum;
    }

    /**
     * Populate peerList from a list of PeerIds of remote Peers, using info from SkylinkConnection.
     *
     * @param peerIdList String Array of PeerIds of remote Peer(s) in the room.
     */
    void popPeerList(String[] peerIdList) {
        // Clear peerList
        peerList.clear();
        // Populate peerList
        for (int i = 0; i < peerIdList.length; ++i) {
            String peerId = peerIdList[i];
            String nick = Utils.getNick(peerId);
            Pair<String, String> peer = new Pair<>(peerId, nick);
            peerList.add(peer);
        }
    }

    /**
     * Remove Peer name from Radio buttons.
     *
     * @param peerId PeerId of the Peer to remove.
     */
    void removePeerRadioBtn(String peerId) {
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
        fillPeerRadioBtn();
    }
}
