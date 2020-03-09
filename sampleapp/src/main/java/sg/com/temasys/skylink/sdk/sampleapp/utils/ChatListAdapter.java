package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.chat.ChatActivity;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.MessageModel;

public class ChatListAdapter extends ArrayAdapter<MessageModel> {

    private Context context;
    private List<MessageModel> chatCollection;

    private MessageRowType viewType = MessageRowType.CHAT_METADATA;
    private ImageButton btnGetMsgHistory;

    public enum MessageRowType {
        CHAT_METADATA,
        CHAT_METADATA_MSG_HISTORY,
        CHAT_LOCAL_GRP_SIG,
        CHAT_LOCAL_GRP_P2P,
        CHAT_LOCAL_PTE_SIG,
        CHAT_LOCAL_PTE_P2P,
        CHAT_REMOTE_GRP_SIG_HISTORY,
        CHAT_REMOTE_GRP_SIG,
        CHAT_REMOTE_GRP_P2P,
        CHAT_REMOTE_PTE_SIG,
        CHAT_REMOTE_PTE_P2P
    }

    public ChatListAdapter(@NonNull Context context, int resource, @NonNull List<MessageModel> objects) {
        super(context, resource, objects);
        this.context = context;
        chatCollection = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;

        // get the correct message content the the position from the collection
        MessageModel chatListRow = chatCollection.get(position);

        switch (chatListRow.getMessageRowType()) {
            case CHAT_METADATA:
                listItem = LayoutInflater.from(context).inflate(R.layout.list_item_metadata, parent, false);
                processMetadataRow(chatListRow, listItem);
                break;
            case CHAT_METADATA_MSG_HISTORY:
                listItem = LayoutInflater.from(context).inflate(R.layout.list_item_metadata_msg_history, parent, false);
                processMetadataMsgHisRow(chatListRow, listItem);
                break;
            case CHAT_LOCAL_GRP_SIG:
            case CHAT_LOCAL_GRP_P2P:
            case CHAT_LOCAL_PTE_SIG:
            case CHAT_LOCAL_PTE_P2P:
                listItem = LayoutInflater.from(context).inflate(R.layout.list_item_local_chat, parent, false);
                processLocalMessageRow(chatListRow.getMessageRowType(), listItem, chatListRow);
                break;
            case CHAT_REMOTE_GRP_SIG:
            case CHAT_REMOTE_GRP_P2P:
            case CHAT_REMOTE_PTE_SIG:
            case CHAT_REMOTE_PTE_P2P:
                listItem = LayoutInflater.from(context).inflate(R.layout.list_item_remote, parent, false);
                processRemoteMessageRow(chatListRow.getMessageRowType(), listItem, chatListRow);
                break;
            case CHAT_REMOTE_GRP_SIG_HISTORY:
                listItem = LayoutInflater.from(context).inflate(R.layout.list_item_remote, parent, false);
                processMessageHistoryRow(chatListRow.getMessageRowType(), listItem, chatListRow);
                break;
        }

        return listItem;
    }

    private void processMessageHistoryRow(MessageRowType messageRowType, View listItem, MessageModel chatListRow) {
        viewType = messageRowType;

        // get the view controls from the list view
        RelativeLayout layoutRow = listItem.findViewById(R.id.ll_row_remote);
        Button buttonUseAvatar = listItem.findViewById(R.id.btnChatUser);
        TextView txtUserName = listItem.findViewById(R.id.txtRemotePeerUser);
        TextView txtDateTime = listItem.findViewById(R.id.txtRemoteTimeStamp);
        TextView txtRemoteMessageContent = listItem.findViewById(R.id.txtRemoteChatContent);

        String chatContent = "";

        switch (messageRowType) {
            case CHAT_REMOTE_GRP_SIG:
                layoutRow.setBackground(context.getResources().getDrawable(R.drawable.background_ripple_chat_group_sig));
                chatContent = "[GRP][SIG] ";
                break;
            case CHAT_REMOTE_GRP_P2P:
                layoutRow.setBackground(context.getResources().getDrawable(R.drawable.background_ripple_chat_group_p2p));
                chatContent = "[GRP][P2P] ";
                break;
            case CHAT_REMOTE_PTE_SIG:
                layoutRow.setBackground(context.getResources().getDrawable(R.drawable.background_ripple_chat_pte_sig));
                chatContent = "[PTE][SIG] ";
                break;
            case CHAT_REMOTE_PTE_P2P:
                layoutRow.setBackground(context.getResources().getDrawable(R.drawable.background_ripple_chat_pte_p2p));
                chatContent = "[PTE][P2P] ";
                break;
        }

        String userName = chatListRow.getPeerUserName();
        buttonUseAvatar.setText(userName.substring(0, 1));
        txtUserName.setText(userName);
        txtDateTime.setText(chatListRow.getTimeStamp());
        txtRemoteMessageContent.setText(chatContent + chatListRow.getMessageContent());
    }

    private void processRemoteMessageRow(MessageRowType messageRowType, View listItem, MessageModel chatListRow) {
        viewType = messageRowType;

        // get the view controls from the list view
        RelativeLayout layoutRow = listItem.findViewById(R.id.ll_row_remote);
        Button buttonUseAvatar = listItem.findViewById(R.id.btnChatUser);
        TextView txtUserName = listItem.findViewById(R.id.txtRemotePeerUser);
        TextView txtDateTime = listItem.findViewById(R.id.txtRemoteTimeStamp);
        TextView txtRemoteMessageContent = listItem.findViewById(R.id.txtRemoteChatContent);

        String chatContent = "";

        switch (messageRowType) {
            case CHAT_REMOTE_GRP_SIG:
                layoutRow.setBackground(context.getResources().getDrawable(R.drawable.background_ripple_chat_group_sig));
                chatContent = "[GRP][SIG] ";
                break;
            case CHAT_REMOTE_GRP_P2P:
                layoutRow.setBackground(context.getResources().getDrawable(R.drawable.background_ripple_chat_group_p2p));
                chatContent = "[GRP][P2P] ";
                break;
            case CHAT_REMOTE_PTE_SIG:
                layoutRow.setBackground(context.getResources().getDrawable(R.drawable.background_ripple_chat_pte_sig));
                chatContent = "[PTE][SIG] ";
                break;
            case CHAT_REMOTE_PTE_P2P:
                layoutRow.setBackground(context.getResources().getDrawable(R.drawable.background_ripple_chat_pte_p2p));
                chatContent = "[PTE][P2P] ";
                break;
        }

        String userName = chatListRow.getPeerUserName();
        String displayName = null;
        if (userName != null && userName.length() > 0) {
            displayName = userName.substring(0, 1);
        }
        buttonUseAvatar.setText(displayName);
        txtUserName.setText(chatListRow.getPeerUserName() + "(" + chatListRow.getPeerId() + ")");
        txtDateTime.setText(chatListRow.getTimeStamp());
        txtRemoteMessageContent.setText(chatContent + chatListRow.getMessageContent());
    }

    private void processLocalMessageRow(MessageRowType messageRowType, View listItem, MessageModel chatListRow) {
        viewType = messageRowType;

        // get the view controls from the list view
        LinearLayout layoutRow = listItem.findViewById(R.id.ll_row_local);
        TextView txtLocalUser = listItem.findViewById(R.id.txtLocalPeerUser);
        TextView txtDateTime = listItem.findViewById(R.id.txtLocalTimeStamp);
        TextView txtLocalChatContent = listItem.findViewById(R.id.txtLocalChatContent);

        String chatContent = "";

        switch (messageRowType) {
            case CHAT_LOCAL_GRP_SIG:
                layoutRow.setBackground(context.getResources().getDrawable(R.drawable.background_ripple_chat_group_sig));
                chatContent = "[GRP][SIG] ";
                break;
            case CHAT_LOCAL_GRP_P2P:
                layoutRow.setBackground(context.getResources().getDrawable(R.drawable.background_ripple_chat_group_p2p));
                chatContent = "[GRP][P2P] ";
                break;
            case CHAT_LOCAL_PTE_SIG:
                layoutRow.setBackground(context.getResources().getDrawable(R.drawable.background_ripple_chat_pte_sig));
                chatContent = "[PTE][SIG] ";
                break;
            case CHAT_LOCAL_PTE_P2P:
                layoutRow.setBackground(context.getResources().getDrawable(R.drawable.background_ripple_chat_pte_p2p));
                chatContent = "[PTE][P2P] ";
                break;
        }

        txtLocalUser.setText(chatListRow.getPeerUserName() + "(" + chatListRow.getPeerId() + ")");
        txtDateTime.setText(chatListRow.getTimeStamp());
        txtLocalChatContent.setText(chatContent + chatListRow.getMessageContent());
    }

    private void processMetadataRow(MessageModel chatListRow, View listItem) {
        viewType = MessageRowType.CHAT_METADATA;

        // get the view controls from the list view
        TextView txtMetadata = listItem.findViewById(R.id.txtMetadataChat);
        TextView txtDateTime = listItem.findViewById(R.id.txtChatDateTime);

        txtMetadata.setText(chatListRow.getPeerUserName() + "(" + chatListRow.getPeerId() + ")" + chatListRow.getMessageContent());
        txtDateTime.setText(chatListRow.getTimeStamp());
    }

    private void processMetadataMsgHisRow(MessageModel chatListRow, View listItem) {
        viewType = MessageRowType.CHAT_METADATA_MSG_HISTORY;

        // get the view controls from the list view
        btnGetMsgHistory = listItem.findViewById(R.id.btn_get_msg_history);

        btnGetMsgHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof ChatActivity) {
                    ((ChatActivity) context).onUserGetStoredMessages();
                }
            }
        });
    }
}
