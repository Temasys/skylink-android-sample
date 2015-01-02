package sg.com.temasys.sdk.sample;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.temasys.skylink.sample.R;

import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection.SkyLinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkyLinkException;

import java.util.Map;

public class ChatFragment extends Fragment {

    final static private String BUNDLE_TID = "tools.skylink.sample.ChatFragment.tid";
    final static private String BUNDLE_TARGET = "tools.skylink.sample.ChatFragment.target";
    final static private String BUNDLE_IS_PRIVATE = "tools.skylink.sample.ChatFragment.isPrivate";
    final static private String BUNDLE_NICKNAME = "tools.skylink.sample.ChatFragment.nickname";
    final static private String BUNDLE_CONNECTION_CONFIG = "tools.skylink.sample.ChatFragment.connectionConfig";

    // Share application wide items.
    public static final boolean DEBUG = true;

    // Parent activity, set at onAttach.
    private RoomViewActivity parentActivity;
    // Instantiate views
    // The root view of ChatFragment
    private View rootView;
    public LinearLayout chatFragmentVG;
    public TextView txtVwChatTarget;
    public TextView txtVwChatMsgs;
    public EditText edtVwUserMsg;
    public EditText edtVwUsername;
    private Button btnSendChat;

    private static String TAG = "ChatFragment";

    private String nickname;
    private String tid;
    // target is a string label for tid and used to identify chat containers.
    // If tid is null, then target is "group".
    private String target;
    private boolean isPrivate;
    private SkyLinkConnection connectionManager;
    private SkyLinkConnection.SkyLinkConfig config;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    // Set the display of chat target label correctly.
    // setTarget should only be called after UI objects are initialised in onCreate.
    // If unable to proceed (e.g. peer is not in the room, UI not initialised ), return false.
    public boolean setTarget(String tid, boolean isPrivate) {
        String nick = "";
        if (isPrivate) nick = RoomManager.get().getDisplayName(tid);
        if (nick == null) return false;

        setTid(tid);
        setIsPrivate(isPrivate);
        if (txtVwChatTarget == null) return false;

        if (!isPrivate) target = "group";
        else target = tid;
        // Set Chat target
        if (isPrivate) txtVwChatTarget.setText(
                String.format(getString(R.string.title_chat_private), nick));
        else txtVwChatTarget.setText(getString(R.string.title_chat_group));

        // Set specific chat content.
        // Clear previous text.
        txtVwChatMsgs.setText("");
        String chatHistory = ChatContent.chatMsgList.get(target);
        if (chatHistory != null) txtVwChatMsgs.append(chatHistory);
        String userMsg = ChatContent.chatUserMsgList.get(target);
        if (userMsg != null) edtVwUserMsg.setText(userMsg);
        return true;
    }

    public void setConfig(SkyLinkConnection.SkyLinkConfig config) {
        this.config = config;
    }

    public SkyLinkConnection getConnectionManager() {
        return connectionManager;
    }

    public void setConnectionManager() {
        this.connectionManager = RoomManager.get().getConnection();
    }

    // Default constructor.
    public ChatFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Application wide sharing.
        // appSharer.activityList.put( "chatFragment", this );
        // appRTCDemoActivity.setChatFragment( this );
        if (savedInstanceState != null) {
            this.setConfig((SkyLinkConfig) savedInstanceState
                    .getSerializable(BUNDLE_CONNECTION_CONFIG));
            tid = savedInstanceState.getString(BUNDLE_TID);
            target = savedInstanceState.getString(BUNDLE_TARGET);
            isPrivate = savedInstanceState.getBoolean(BUNDLE_IS_PRIVATE);
            nickname = savedInstanceState.getString(BUNDLE_NICKNAME);
        }
        this.setConnectionManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.chat, container, false);

        // Set Views
        chatFragmentVG = (LinearLayout) rootView.findViewById(R.id.linearLayoutChat);
        txtVwChatTarget = (TextView) rootView.findViewById(R.id.textViewChatTarget);
        txtVwChatMsgs = (TextView) rootView.findViewById(R.id.textViewChatMessages);
        edtVwUserMsg = (EditText) rootView.findViewById(R.id.editTextUserMessage);
        edtVwUsername = (EditText) rootView.findViewById(R.id.editTextUsername);
        edtVwUsername.setFocusable(false);
        btnSendChat = (Button) rootView.findViewById(R.id.buttonSendChat);
        // Chat elements
        // Set specific contents.
        setTarget(tid, isPrivate);
        // Chat display and user msg to allow scrolling.
        txtVwChatMsgs.setMovementMethod(new ScrollingMovementMethod());
        edtVwUserMsg.setMovementMethod(new ScrollingMovementMethod());
      /*// Hold chat content.
    txtVwChatMsgs.append( ChatContent.chatContents );*/
        scrollToBottom();
      /*// Set background colours
        // Format: Hexadecimal AARRGGBB each value can be from 0-255 (0-F).
        // Set background colours
          // 20% white background.
    txtVwChatMsgs.setBackgroundColor( 0X33FFFFFF );
          // Set 100% Black text.
    txtVwChatMsgs.setTextColor( 0XFF000000 );*/

        // Set View values
        edtVwUsername.setText(this.getNickname());

        // Remove new chat from Video UI new chat textView for this Peer
        TextView newChatTxtVw;
        if (isPrivate) {
            newChatTxtVw = RoomManager.get().getChatPrivateTextView(tid);
            // Remove from video UI.
            if (newChatTxtVw != null) newChatTxtVw.setText("");
            // Remove from Chat content as well.
            ChatContent.newChatPrivateMsgList.put(tid, "");
        } else {
            // Clear all the group new chats.
            Map<String, TextView> newChatGroupPool = RoomManager.get().getNewChatGroupPool();
            for (String peerId : newChatGroupPool.keySet()) {
                // Remove from video UI.
                newChatGroupPool.get(peerId).setText("");
                // Remove from Chat content as well.
                ChatContent.newChatGroupMsgList.put(peerId, "");
            }
        }


        // Set on ClickListener
        btnSendChat.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        // Hide keyboard
                        parentActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                hideKeyboard(parentActivity, edtVwUserMsg.getWindowToken());
                            }
                        });
                        // Call sendChat of parent activity.
                        String msg = edtVwUserMsg.getText().toString();
                        // [VLS-2]: Send public message via SC.
                        if (!isPrivate) {
                            connectionManager.sendCustomMessage(null, msg);
                            ChatFragment.this.addChatMsg(ChatFragment.this.nickname + ": " + msg, tid, isPrivate);
                            // else connectionManager.sendCustomMessage( tid, msg );
                            // [VLS-2]: Send private message via DC
                        } else {
                            try {
                                connectionManager.sendPeerMessage(tid, msg);
                                if (config.hasPeerMessaging())
                                    ChatFragment.this.addChatMsg(
                                            ChatFragment.this.nickname + getString(R.string.label_chat_p2p_tag)
                                                    + msg, tid, isPrivate);
                            } catch (SkyLinkException e) {
                                Log.w(TAG, e.getLocalizedMessage());
                                // A system error message will be sent at addChatMsg.
                                ChatFragment.this.addChatMsg(msg, tid, isPrivate);
                            }
                        }
                        edtVwUserMsg.setText("");
                    }
                }
        );

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        parentActivity = (RoomViewActivity) activity;
    }

    @Override
    public void onPause() {
        super.onPause();

        // Save View values
        ChatContent.chatUserMsgList.put(target, edtVwUserMsg.getText().toString());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_TID, tid);
        outState.putString(BUNDLE_TARGET, target);
        outState.putBoolean(BUNDLE_IS_PRIVATE, isPrivate);
        outState.putString(BUNDLE_NICKNAME, nickname);
        outState.putSerializable(BUNDLE_CONNECTION_CONFIG, this.config);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Method to write contents of txtVwChatMsgs from none UI thread
    public void addChatMsg(final String newChat, final String tid, boolean isPrivate) {
        final String newChatFinal;
        if (!setTarget(tid, isPrivate)) {
            newChatFinal = String.format(getString(R.string.message_chat_cannot_send), tid, newChat);
        } else newChatFinal = newChat;

        // Modification to UI must run on the UI thread.
        parentActivity.runOnUiThread(new Runnable() {
            public void run() {
                txtVwChatMsgs.append(newChatFinal + "\n");
                ChatContent.chatMsgList.put(target, txtVwChatMsgs.getText().toString());
                scrollToBottom();
            }
        });
    }

    // Method to scroll to bottom of txtVwChatMsgs
    // to display latest chat messages.
    public void scrollToBottom() {
        parentActivity.runOnUiThread(new Runnable() {
            public void run() {
                final Layout layout = txtVwChatMsgs.getLayout();
                if (layout != null) {
                    int scroll =
                            layout.getLineBottom(txtVwChatMsgs.getLineCount() - 1)
                                    - txtVwChatMsgs.getScrollY()
                                    - txtVwChatMsgs.getHeight();
                    if (scroll > 0) txtVwChatMsgs.scrollBy(0, scroll);
                }
            }
        });
    }

    // Hide keyboard
    private void hideKeyboard(Context c, IBinder windowToken) {
        InputMethodManager mgr = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(windowToken, 0);
    }
}
