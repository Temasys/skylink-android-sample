package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;


public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER_MESSAGE_ME = 0;
    private static final int VIEW_TYPE_USER_MESSAGE_OTHER = 1;

    private Context mContext;
    private List<String> mMessageList;

    public ChatAdapter(Context context, List<String> list) {
        mContext = context;
        mMessageList = list;
    }

    void setContext(Context context) {
        mContext = context;
    }


    /**
     * Inflates the correct layout according to the View Type.
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {
            case VIEW_TYPE_USER_MESSAGE_ME:
                View myUserMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_local, parent, false);
                return new MyUserMessageHolder(myUserMsgView);
            case VIEW_TYPE_USER_MESSAGE_OTHER:
                View otherUserMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_remote, parent, false);
//                return new OtherUserMessageHolder(otherUserMsgView);
                return new MyUserMessageHolder(otherUserMsgView);

            default:
                return null;

        }
    }

    @Override
    public int getItemViewType(int position) {
        String message = mMessageList.get(position);

        // Base on the message prefix to get the correct peer
        if (message.startsWith("You :")) {
            return VIEW_TYPE_USER_MESSAGE_ME;
        }

        return VIEW_TYPE_USER_MESSAGE_OTHER;
    }

    /**
     * Binds variables in the BaseMessage to UI components in the ViewHolder.
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        String message = mMessageList.get(position);
        boolean isContinuous = false;
        boolean isNewDay = false;
        boolean isTempMessage = false;
        boolean isFailedMessage = false;
        Uri tempFileMessageUri = null;

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_USER_MESSAGE_ME:
                ((MyUserMessageHolder) holder).bind(mContext, position, message);
                break;
            case VIEW_TYPE_USER_MESSAGE_OTHER:
                ((MyUserMessageHolder) holder).bind(mContext, position, message);
                break;
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }


    private class MyUserMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, editedText, timeText, readReceiptText, dateText;
        ViewGroup urlPreviewContainer;
        TextView urlPreviewSiteNameText, urlPreviewTitleText, urlPreviewDescriptionText;
        ImageView urlPreviewMainImageView;
        View padding;

        MyUserMessageHolder(View itemView) {
            super(itemView);

//            messageText = (TextView) itemView.findViewById(R.id.text_group_chat_message);
//            editedText = (TextView) itemView.findViewById(R.id.text_group_chat_edited);
//            timeText = (TextView) itemView.findViewById(R.id.text_group_chat_time);
//            readReceiptText = (TextView) itemView.findViewById(R.id.text_group_chat_read_receipt);
//            dateText = (TextView) itemView.findViewById(R.id.text_group_chat_date);

//            urlPreviewContainer = (ViewGroup) itemView.findViewById(R.id.url_preview_container);
//            urlPreviewSiteNameText = (TextView) itemView.findViewById(R.id.text_url_preview_site_name);
//            urlPreviewTitleText = (TextView) itemView.findViewById(R.id.text_url_preview_title);
            urlPreviewDescriptionText = (TextView) itemView.findViewById(R.id.txtChatContent);
//            urlPreviewMainImageView = (ImageView) itemView.findViewById(R.id.image_url_preview_main);

            // Dynamic padding that can be hidden or shown based on whether the message is continuous.
//            padding = itemView.findViewById(R.id.view_group_chat_padding);
        }

        void bind(Context context, final int position, String content) {
            urlPreviewDescriptionText.setText(content);
        }


    }


}



