package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by xiangrong on 16/2/16.
 */
public class MultiPartyFragment extends Fragment {

    private static final String TAG = MultiPartyFragment.class.getName();

    // RadioGroup and RadioButtons for selection of Peer(s)
    protected static RadioGroup peerRadioGroup;
    protected static RadioButton peerAll;
    protected static RadioButton peer1;
    protected static RadioButton peer2;
    protected static RadioButton peer3;
    protected static RadioButton peer4;
    protected Context context;

    //list of peers in room
    protected static List<SkylinkPeer> mPeers;


    /***
     * Multi party methods
     */

    /**
     * Add Peer name to next available Radio button.
     *
     * @param newPeer new peer to be added
     */
    public void addPeerRadioBtn(SkylinkPeer newPeer) {

        String logTag = "[SA][addPeerRadioBtn] ";
        logTag += "Adding Peer \"" + newPeer.getPeerId() + "\"(" + newPeer.getPeerName() + ") to peerList...";

        // Add Peers to radio buttons.
        logTag += "\nPopulating RadioButton UI with added Peer.";

        Log.d(TAG, logTag);

        fillPeerRadioBtn(mPeers);
    }

    /**
     * Remove Peer name from Radio buttons.
     *
     * @param peerId PeerId of the Peer to remove.
     */
    public void removePeerRadioBtn(String peerId) {

        if(mPeers == null)
            return;

        // Remove Peer from peerList
        for (SkylinkPeer sp: mPeers) {
            if(sp.getPeerId().equals(peerId)){
                mPeers.remove(sp);
                break;
            }
        }

        // Remove Peer from radio buttons and rearrange them.
        fillPeerRadioBtn(mPeers);
    }

    /**
     * Populate radio buttons from top to bottom with PeerId and nick.
     * Unpopulated radio buttons will be invisible.
     * Ensure All Peer button is visible IFF there are Peer(s), invisible otherwise.
     */
    public void fillPeerRadioBtn(List<SkylinkPeer> peersList) {

        //reset mPeersList
        this.mPeers = peersList;

        int totalPeerNum = peersList.size();

        String logTag = "[SA][fillPeerRadioBtn] ";
        logTag += "\nPopulating RadioButton UI with " + totalPeerNum + " Peer(s)...";
        Log.d(TAG, logTag);

        // Ensure All Peers button visibility is correct.
        if (totalPeerNum > 1) {
            peerAll.setVisibility(View.VISIBLE);
        } else {
            peerAll.setVisibility(View.INVISIBLE);
            logTag = "There are no remote Peers, so there will be no RadioButtons.";
            Log.d(TAG, logTag);
        }

        // Populate each radio button appropriately starting after All Peers button.
        // Not populate self peer
        for (int i = 1; i < peerRadioGroup.getChildCount(); ++i) {
            RadioButton rb = (RadioButton) peerRadioGroup.getChildAt(i);
            // If there are no more Peers
            if (i >= totalPeerNum) {
                // Make radio button invisible
                rb.setVisibility(View.INVISIBLE);
                // Clear text and tag
                rb.setText("");
                rb.setTag("");
                logTag =  "RadioButton " + i + " is invisible as there are only " +
                        totalPeerNum + " remote Peer(s).";
                Log.d(TAG, logTag);

            } else {
                // Make radio button visible
                rb.setVisibility(View.VISIBLE);
                // Set text and tag
                SkylinkPeer remotePeer = peersList.get(i);

                String peerId = remotePeer.getPeerId();
                String nick = remotePeer.getPeerName();

                rb.setText(nick+" ("+peerId+")");
                rb.setTag(peerId);
                logTag = "RadioButton " + i + " is visible as there are " +
                        totalPeerNum + " remote Peer(s).";
                Log.d(TAG, logTag);
            }
        }
    }

    /**
     * Get the PeerId currently selected to send to.
     * Null means to send to all Peer(s).
     * If none is selected, return empty string.
     *
     * @return PeerId to send to.
     */
    public String getPeerIdSelected() {
        int selectedRB = peerRadioGroup.getCheckedRadioButtonId();

        // If none is selected, return empty string.
        if (selectedRB < 0) {
            return "";
        }

        // Return null when All Peer(s) was selected.
        if (selectedRB == peerAll.getId()) {
            return null;
        }

        RadioButton rb = (RadioButton)(getActivity().findViewById(selectedRB));
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
     * @return PeerId to send to.
     */
    public String getPeerIdSelectedWithWarning() {
        // Do not allow button actions if there are no remote Peers in the room.
        if (mPeers.size() < 2) {
            String log = getString(R.string.warn_no_peer_message);
            toastLog(TAG, context, log);
            return "";
        }

        String remotePeerId = getPeerIdSelected();
        // Do not allow button actions if no selection was made.
        if ("".equals(remotePeerId)) {
            String log = getString(R.string.warn_no_sel_message);
            toastLog(TAG, context, log);
            return "";
        }
        return remotePeerId;
    }

}
