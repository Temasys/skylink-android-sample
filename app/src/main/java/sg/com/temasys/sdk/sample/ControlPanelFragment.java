package sg.com.temasys.sdk.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.temasys.skylink.sample.R;
import com.warting.bubbles.HelloBubblesActivity;

import java.util.Map;

public class ControlPanelFragment extends RoomFragment {

    final static private String BUNDLE_AUDIO_MUTE = "tools.skylink.sample.ControlPanelFragment.audioMute";
    final static private String BUNDLE_VIDEO_MUTE = "tools.skylink.sample.ControlPanelFragment.videoMute";

    private boolean mIsAudioMuted = false;
    private boolean mIsVideoMuted = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsAudioMuted = savedInstanceState.getBoolean(BUNDLE_AUDIO_MUTE);
            mIsVideoMuted = savedInstanceState.getBoolean(BUNDLE_VIDEO_MUTE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_control_panel, container, false);

        ViewGroup parent = (ViewGroup) rootView.findViewById(R.id.self_video);
        View child = RoomManager.get().getSelfVideo().getVideoView();
        addVideoView(parent, child, null);

        ImageButton audioButton = (ImageButton) rootView
                .findViewById(R.id.audio_imageButton);
        audioButton.setImageResource(mIsAudioMuted ? R.drawable.disable_audio
                : R.drawable.enable_audio);
        audioButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                mIsAudioMuted = !mIsAudioMuted;
                ((ImageButton) view)
                        .setImageResource(mIsAudioMuted ? R.drawable.disable_audio
                                : R.drawable.enable_audio);
                RoomManager.get().getConnection().muteAudio(mIsAudioMuted);
            }

        });

        ImageButton videoButton = (ImageButton) rootView
                .findViewById(R.id.video_imageButton);
        videoButton.setImageResource(mIsVideoMuted ? R.drawable.disable_camera
                : R.drawable.enable_camera);
        videoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                mIsVideoMuted = !mIsVideoMuted;
                ((ImageButton) view)
                        .setImageResource(mIsVideoMuted ? R.drawable.disable_camera
                                : R.drawable.enable_camera);
                RoomManager.get().getConnection().muteVideo(mIsVideoMuted);
            }

        });

        ImageButton chatButton = (ImageButton) rootView
                .findViewById(R.id.chat_button);
        chatButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // Start group chat.
                // ((RoomViewActivity) getActivity()).setChat(null, false);

                // Clear all the group new chats.
                Map<String, TextView> newChatGroupPool = RoomManager.get()
                        .getNewChatGroupPool();
                for (String peerId : newChatGroupPool.keySet()) {
                    // Remove from video UI.
                    newChatGroupPool.get(peerId).setText("");
                    // Remove from Chat content as well.
                    ChatContent.newChatGroupMsgList.put(peerId, "");
                }

                RoomManager.get().setChatPeerId(null);

                Intent intent = new Intent(getActivity(),
                        HelloBubblesActivity.class);
                getActivity().startActivity(intent);
                // Check if saveFileDialogue is in progress.
        /*if( RoomManager.get().getFeOn() ) {
          String operation = parentActivity.getFeFragment().getOperation();
          if( operation.equals( "SAVE" ) ) {
            // Disable chat while save saveFileDialogue in progress.
            return;
          }
        }*/
            }
        });

        ImageButton fileButton = (ImageButton) rootView
                .findViewById(R.id.file_button);
        fileButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // Start group file transfer.
        /*((RoomViewActivity) getActivity()).setFileExplorer( 
          FileExplorerFragment.Ops.SEND, null, false, "" );*/

                // Set File explorer state.
                RoomManager.get().setFileActive(true);

                RoomManager.get().setFileTransferPeerId(null);
                Utility.showChooser(getActivity(), null, false);
            }

        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_AUDIO_MUTE, mIsAudioMuted);
        outState.putBoolean(BUNDLE_VIDEO_MUTE, mIsVideoMuted);
    }

}
