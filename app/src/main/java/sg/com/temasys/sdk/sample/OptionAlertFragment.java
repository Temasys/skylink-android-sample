package sg.com.temasys.sdk.sample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;

import com.temasys.skylink.sample.R;
import com.warting.bubbles.HelloBubblesActivity;

public class OptionAlertFragment extends DialogFragment {

    final static private String BUNDLE_PEER_ID = "tools.skylink.sample.OptionAlertFragment.peerId";
    final static private String BUNDLE_CHAT_BUTTON_STATUS = "tools.skylink.sample.OptionAlertFragment.chatButtonStatus";
    final static private String BUNDLE_FILE_BUTTON_STATUS = "tools.skylink.sample.OptionAlertFragment.fileButtonStatus";

    private String mPeerId;
    private boolean mIsChatOptionSelected = true;
    private boolean mIsFileOptionSelected = false;

    public OptionAlertFragment() {

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // For automatic recreation.
            mPeerId = savedInstanceState.getString(BUNDLE_PEER_ID);
            mIsChatOptionSelected = savedInstanceState
                    .getBoolean(BUNDLE_CHAT_BUTTON_STATUS);
            mIsFileOptionSelected = savedInstanceState
                    .getBoolean(BUNDLE_FILE_BUTTON_STATUS);
        } else {
            // For manual creation.
            mPeerId = getArguments().getString(BUNDLE_PEER_ID);
        }

        View view = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_open_alert, null);

        final RadioButton openChatButton = (RadioButton) view
                .findViewById(R.id.open_chat_radioButton);
        openChatButton.setChecked(mIsChatOptionSelected);
        openChatButton
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        // TODO Auto-generated method stub
                        mIsChatOptionSelected = isChecked;
                    }
                });

        final RadioButton sendFileButton = (RadioButton) view
                .findViewById(R.id.send_file_radioButton);
        sendFileButton.setChecked(mIsFileOptionSelected);
        sendFileButton
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        // TODO Auto-generated method stub
                        mIsFileOptionSelected = isChecked;
                    }
                });

        return new AlertDialog.Builder(getActivity())
                .setTitle(String.format(getString(R.string.title_alert_peer),
                        RoomManager.get().getDisplayName(mPeerId)))
                .setMessage(R.string.message_option_alert)
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                if (mIsChatOptionSelected) {
                                    // Start chat fragment if not already
                                    // started.
                                    // ( ( RoomViewActivity ) getActivity()).setChat( mPeerId, true );

                                    // Remove new chat from Video UI new chat
                                    // textView for this Peer
                                    // Clear for private chat
                                    TextView newChatTxtVw = RoomManager.get().getChatPrivateTextView(mPeerId);
                                    // Remove from video UI.
                                    if (newChatTxtVw != null)
                                        newChatTxtVw.setText("");
                                    // Remove from Chat content as well.
                                    ChatContent.newChatPrivateMsgList.put(mPeerId, "");
                                    // Clear for group chat
                                    newChatTxtVw = RoomManager.get().getChatGroupTextView(mPeerId);
                                    // Remove from video UI.
                                    if (newChatTxtVw != null) newChatTxtVw.setText("");
                                    // Remove from Chat content as well.
                                    ChatContent.newChatGroupMsgList.put(mPeerId, "");

                                    RoomManager.get().setChatPeerId(mPeerId);
                                    Intent intent = new Intent(getActivity(), HelloBubblesActivity.class);
                                    intent.putExtra(HelloBubblesActivity.EXTRA_PEER_ID, mPeerId);
                                    getActivity().startActivity(intent);
                                } else if (mIsFileOptionSelected) {
                                    /*( ( RoomViewActivity ) getActivity())
                    .setFileExplorer( FileExplorerFragment.Ops.SEND, mPeerId, true, "" );*/

                                    // Set File explorer state.
                                    RoomManager.get().setFileActive(true);

                                    RoomManager.get().setFileTransferPeerId(
                                            mPeerId);
                                    Utility.showChooser(getActivity(), mPeerId,
                                            true);
                                }
                                OptionAlertFragment.this.dismiss();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                OptionAlertFragment.this.dismiss();
                            }
                        }).create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_PEER_ID, mPeerId);
        outState.putBoolean(BUNDLE_CHAT_BUTTON_STATUS, mIsChatOptionSelected);
        outState.putBoolean(BUNDLE_FILE_BUTTON_STATUS, mIsFileOptionSelected);
    }

    public static OptionAlertFragment newInstance(String message) {
        OptionAlertFragment optionAlertFragment = new OptionAlertFragment();
        Bundle args = new Bundle();
        args.putString(BUNDLE_PEER_ID, message);
        optionAlertFragment.setArguments(args);
        return optionAlertFragment;
    }

}
