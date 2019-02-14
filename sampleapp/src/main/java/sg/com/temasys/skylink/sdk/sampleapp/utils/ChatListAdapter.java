package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;

public class ChatListAdapter extends ArrayAdapter<String> {

    private Context mContext = null;
    private List<String> chatCollection = null;
    private VIEW_TYPE viewType = VIEW_TYPE.CHAT_METADATA;

    enum VIEW_TYPE {
        CHAT_METADATA,
        CHAT_LOCAL,
        CHAT_REMOTE
    }

    public ChatListAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
        mContext = context;
        chatCollection = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;

        // get the correct message content the the position from the collection
        String chatContent = chatCollection.get(position);

        // base on the content, decide it is from local or remote peer
        if (chatContent.startsWith("[Metadata]")) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item_metadata, parent, false);
            viewType = VIEW_TYPE.CHAT_METADATA;
        } else if (chatContent.startsWith("You")) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item_local, parent, false);
            viewType = VIEW_TYPE.CHAT_LOCAL;
        } else {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item_remote, parent, false);
            viewType = VIEW_TYPE.CHAT_REMOTE;
        }

        if (viewType != VIEW_TYPE.CHAT_METADATA) {
            // get the view controls from the list view
            TextView txtContent = listItem.findViewById(R.id.txtChatContent);
            TextView txtPeerId = listItem.findViewById(R.id.txtRemotePeerChat);

            // set the value for the view controls
            txtContent.setText(chatContent.split(":")[1]);

            // extract the local username and local peer id
            if (viewType == VIEW_TYPE.CHAT_LOCAL) {
                txtPeerId.setText(chatContent.split(":")[0].split("---")[1]);
            }

            // only for remote
            if (viewType == VIEW_TYPE.CHAT_REMOTE) {
                Button btnUser = listItem.findViewById(R.id.btnChatUser);

                // set the remote avatar is the first character of username
                btnUser.setText(chatContent.charAt(0) + "");

                // extract remote username and remote peer id from chatContent
                txtPeerId.setText(chatContent.split(":")[0]);
            }
        } else {
            TextView txtMetadata = listItem.findViewById(R.id.txtMetadataChat);
            txtMetadata.setText(chatContent.split("\n")[0].split(":")[1]);

            TextView txtDateTime = listItem.findViewById(R.id.txtChatDateTime);
            txtDateTime.setText(chatContent.split("\n")[1]);
        }

        return listItem;

    }
}
