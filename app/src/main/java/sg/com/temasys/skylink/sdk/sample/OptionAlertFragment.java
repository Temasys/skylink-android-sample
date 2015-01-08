package sg.com.temasys.skylink.sdk.sample;

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

import com.warting.bubbles.HelloBubblesActivity;

import sg.com.temasys.skylink.sdk.sample.R;

public class OptionAlertFragment extends DialogFragment {

    final static private String BUNDLE_CHAT_BUTTON_STATUS = "sg.com.temasys.skylink.sdk.sample.OptionAlertFragment.chatButtonStatus";
    final static private String BUNDLE_FILE_BUTTON_STATUS = "sg.com.temasys.skylink.sdk.sample.OptionAlertFragment.fileButtonStatus";
    final static private String BUNDLE_PEER_ID = "sg.com.temasys.skylink.sdk.sample.OptionAlertFragment.peerId";

    private boolean mIsChatOptionSelected = true;
    private boolean mIsFileOptionSelected = false;
    private String mPeerId;

    public OptionAlertFragment() {

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mIsChatOptionSelected = savedInstanceState
                    .getBoolean(BUNDLE_CHAT_BUTTON_STATUS);
            mIsFileOptionSelected = savedInstanceState
                    .getBoolean(BUNDLE_FILE_BUTTON_STATUS);
            mPeerId = savedInstanceState.getString(BUNDLE_PEER_ID);
        } else {
            mPeerId = getArguments().getString(BUNDLE_PEER_ID);
        }

        View view = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_option_alert, null);

        RadioButton openChatButton = (RadioButton) view
                .findViewById(R.id.open_chat_radioButton);
        openChatButton.setChecked(mIsChatOptionSelected);
        openChatButton
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        mIsChatOptionSelected = isChecked;
                    }
                });

        RadioButton sendFileButton = (RadioButton) view
                .findViewById(R.id.send_file_radioButton);
        sendFileButton.setChecked(mIsFileOptionSelected);
        sendFileButton
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        mIsFileOptionSelected = isChecked;
                    }
                });

        return new AlertDialog.Builder(getActivity())
                .setTitle(
                        String.format(getString(R.string.title_alert_peer),
                                RoomManager.get().getDisplayName(mPeerId)))
                .setMessage(R.string.message_option_alert)
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                if (mIsChatOptionSelected) {
                                    // Reset private chat notification.
                                    TextView textView = RoomManager.get()
                                            .getPrivateTextView(mPeerId);
                                    if (textView != null)
                                        textView.setText("");
                                    RoomManager.get().setPrivateNotif(mPeerId,
                                            "");

                                    // Reset group chat notification.
                                    textView = RoomManager.get()
                                            .getGroupTextView(mPeerId);
                                    if (textView != null)
                                        textView.setText("");
                                    RoomManager.get()
                                            .setGroupNotif(mPeerId, "");
                                    RoomManager.get().setChatPeerId(mPeerId);

                                    // Start HelloBubblesActivity.
                                    Intent intent = new Intent(getActivity(),
                                            HelloBubblesActivity.class);
                                    intent.putExtra(
                                            HelloBubblesActivity.EXTRA_PEER_ID,
                                            mPeerId);
                                    getActivity().startActivity(intent);
                                } else if (mIsFileOptionSelected) {
                                    RoomManager.get().setFileUIActive(true);
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
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_CHAT_BUTTON_STATUS, mIsChatOptionSelected);
        outState.putBoolean(BUNDLE_FILE_BUTTON_STATUS, mIsFileOptionSelected);
        outState.putString(BUNDLE_PEER_ID, mPeerId);
    }

    public static OptionAlertFragment newInstance(String message) {
        OptionAlertFragment optionAlertFragment = new OptionAlertFragment();
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_PEER_ID, message);
        optionAlertFragment.setArguments(bundle);
        return optionAlertFragment;
    }

}
