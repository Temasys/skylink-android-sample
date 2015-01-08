package sg.com.temasys.skylink.sdk.sample;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.ipaulpro.afilechooser.utils.FileUtils;
import com.warting.bubbles.DiscussArrayAdapter;
import com.warting.bubbles.OneComment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Date;

import sg.com.temasys.skylink.sdk.sample.R;
import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection.SkyLinkConfig;

public class RoomViewActivity extends Activity implements
        SkyLinkConnection.LifeCycleDelegate,
        SkyLinkConnection.RemotePeerDelegate, SkyLinkConnection.MediaDelegate,
        SkyLinkConnection.MessagesDelegate,
        SkyLinkConnection.FileTransferDelegate {

    final static private String TAG = "RoomViewActivity";

    final static private String BUNDLE_CONNECTION_CONFIG = "sg.com.temasys.skylink.sdk.sample.RoomViewActivity.connectionConfig";
    final static private String BUNDLE_CONTROL_PANEL_WEIGHT = "sg.com.temasys.skylink.sdk.sample.RoomViewActivity.controlPanelWeight";
    final static private String BUNDLE_IS_CONNECTED = "sg.com.temasys.skylink.sdk.sample.RoomViewActivity.isConnected";
    final static private String BUNDLE_IS_RUNNING = "sg.com.temasys.skylink.sdk.sample.RoomViewActivity.isRunning";

    final static public String EXTRA_DISPLAY_NAME = "sg.com.temasys.skylink.sdk.sample.RoomViewActiivty.displayName";
    final static public String EXTRA_ROOM_NAME = "sg.com.temasys.skylink.sdk.sample.RoomViewActivity.roomName";

    private boolean mActiveFileUICancelled = false;
    private boolean mIsAlreadyConnected = false;
    private boolean mIsExplicitlyTerminated = false;
    private boolean mIsRunning = false;
    private float mCpWeight = 0.25f;
    private SkyLinkConfig mConnectionConfig;
    private SkyLinkConnection mConnection;
    private String mCancelMessage;
    private String mDisplayName;
    private String mRoomName;

    // Audio
    private HeadSetReceiver mHeadSetReceiver;
    private IntentFilter headSetFilter;
    private MediaPlayer mMediaPlayer;

    private class HeadSetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.d(TAG, "Headset: Unplugged");
                        break;
                    case 1:
                        Log.d(TAG, "Headset: Plugged");
                        break;
                    default:
                        Log.d(TAG, "Headset: Error determining state!");
                }
                // Reset audio path
                setAudioPath();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsAlreadyConnected = savedInstanceState.getBoolean(
                    BUNDLE_IS_CONNECTED, false);
            mConnectionConfig = (SkyLinkConfig) savedInstanceState
                    .get(BUNDLE_CONNECTION_CONFIG);
            mCpWeight = savedInstanceState
                    .getFloat(BUNDLE_CONTROL_PANEL_WEIGHT);
            mIsRunning = savedInstanceState.getBoolean(BUNDLE_IS_RUNNING);
        }
        mDisplayName = getIntent().getStringExtra(EXTRA_DISPLAY_NAME);
        mRoomName = getIntent().getStringExtra(EXTRA_ROOM_NAME);

		/*AudioManager audioManager = ((AudioManager) getSystemService(android.content.Context.AUDIO_SERVICE));

		@SuppressWarnings("deprecation")
		boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
		audioManager.setMode(isWiredHeadsetOn ? AudioManager.MODE_IN_CALL
				: AudioManager.MODE_IN_COMMUNICATION);
		audioManager.setSpeakerphoneOn(!isWiredHeadsetOn);*/


        if (!mIsAlreadyConnected) {
            SkyLinkConfig config = new SkyLinkConnection.SkyLinkConfig();
            config.setHasAudio(true);
            config.setHasVideo(true);
            config.setHasPeerMessaging(true);
            config.setHasFileTransfer(true);
            config.setTimeout(60);
            // config.setTimeout( 11 );
            mConnection = new SkyLinkConnection(getString(R.string.app_key),
                    getString(R.string.app_secret), config, this);
            mConnectionConfig = config;
            RoomManager.getInstance(mConnection);
        } else {
            // New Activity has been generated, so reset the delegates to the
            // new activity.
            mConnection = RoomManager.get().getConnection();
            mConnection.resetContext(this);
        }

        mConnection.setFileTransferDelegate(this);
        mConnection.setLifeCycleDelegate(this);
        mConnection.setMediaDelegate(this);
        mConnection.setMessagesDelegate(this);
        mConnection.setRemotePeerDelegate(this);

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = layoutInflater.inflate(R.layout.activity_room_view,
                null);
        View view = rootView.findViewById(R.id.control_panel_container);
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        layoutParams.weight = mCpWeight;
        view.setLayoutParams(layoutParams);

        setContentView(rootView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            if (!mIsAlreadyConnected) {
                String userData = mDisplayName;
                mConnection.connectToRoom(mRoomName, userData, new Date(), 200);
            }
        } catch (JSONException e) {
            Log.w(TAG, e.getLocalizedMessage(), e);
        } catch (SignatureException e) {
            Log.w(TAG, e.getLocalizedMessage(), e);
        } catch (IOException e) {
            Log.w(TAG, e.getLocalizedMessage(), e);
        }

        // Prepare to set audio path
        mHeadSetReceiver = new HeadSetReceiver();
        headSetFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsRunning = true;
        if (mConnection != null)
            mConnection.onResume();
        if (RoomManager.get().isSplitChanged()) {
            setVideoUIFromRoomManager();
            RoomManager.get().setSplitChanged(false);
        }
        // Get ready to set audio path when headset state changes.
        registerReceiver(mHeadSetReceiver, headSetFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsRunning = false;
        if (mConnection != null)
            mConnection.onPause();
        // Do not set audio path when app is not forefront.
        unregisterReceiver(mHeadSetReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_IS_CONNECTED, mIsAlreadyConnected);
        outState.putSerializable(BUNDLE_CONNECTION_CONFIG, mConnectionConfig);
        outState.putFloat(BUNDLE_CONTROL_PANEL_WEIGHT, mCpWeight);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Utility.REQUEST_CODE_PICK_FILE:
                // If the file selection was successful
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        Log.i(TAG, "Uri = " + uri.toString());
                        try {
                            // Get the file path from the URI
                            final String path = FileUtils.getPath(this, uri);
                            final String fileName = new File(path).getName();
                            mConnection.sendFileTransferRequest(RoomManager.get()
                                    .getFileTransferPeerId(), fileName, path);
                        } catch (Exception e) {
                            Log.e("FileSelectorTestActivity", "File select error",
                                    e);
                        }
                    }
                }
                break;
            case Utility.REQUEST_CODE_PICK_DIR: {
                if (data != null) {
                    String newDir = data
                            .getStringExtra(sg.com.temasys.skylink.sdk.sample.FileBrowserActivity.returnDirectoryParameter);
                    String peerId = data
                            .getStringExtra(FileBrowserActivity.EXTRA_PEER_ID);
                    String fileName = data
                            .getStringExtra(FileBrowserActivity.EXTRA_FILE_NAME);
                    String cancelSave = data
                            .getStringExtra(FileBrowserActivity.EXTRA_CANCEL_SAVE);
                    if (resultCode == RESULT_OK) {
                        if (cancelSave != null
                                && cancelSave
                                .equals(FileBrowserActivity.EXTRA_TIMEOUT)) {
                            // File save was cancelled explicitly or due to timeout
                            // at FilePermissionAlert stage.
                        } else {
                            mConnection.acceptFileTransferRequest(peerId, true,
                                    newDir + File.separator + fileName);
                        }
                    } else {
                        // We cancelled file save ourselves.
                        Utility.showShortToast(this,
                                R.string.message_on_decline_save, fileName,
                                RoomManager.get().getDisplayName(peerId));
                        mConnection.acceptFileTransferRequest(peerId, false,
                                fileName);
                    }// END } else {//if(resultCode == this.RESULT_OK) {
                } else {
                    // File save was cancelled explicitly or due to timeout at File
                    // explorer UI stage.
                }
            }
            break;
        }

        // Handle file save that was cancelled explicitly or due to timeout at
        // File save UI.
        if (mActiveFileUICancelled) {
            // Inform user
            AlertFragment.newInstance(mCancelMessage).show(
                    RoomViewActivity.this.getFragmentManager(), TAG);
            // Reset state
            mActiveFileUICancelled = false;
        }

        // Reset FileBrowserActivity reference in RoomManager
        RoomManager.get().setFileBrowserActivity(null);
        // Reset File explorer state and process next request.
        RoomManager.get().setFileUIActive(false);
        processFileRequest();
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager
                .findFragmentById(R.id.split_container);
        if (RoomFragment.class.isAssignableFrom(fragment.getClass())) {
            super.onBackPressed();
            mIsExplicitlyTerminated = true;
            RoomManager.get().destroy();
            finish();
        } else {
            // Resume the video UI from its last state.
            setVideoUIFromRoomManager();
        }
    }

    // -------------------------------------------------------------------------------------------------
// LifeCycleDelegate callbacks
// -------------------------------------------------------------------------------------------------
    @Override
    public void onConnect(boolean isSuccess, String message) {
        if (!isSuccess) {
            Log.d(TAG, "onConnect()::message->" + message);
            Intent intent = new Intent();
            intent.putExtra(JoinRoomActivity.EXTRA_RESULT_DISCONNECT_STATUS,
                    JoinRoomActivity.DisconnectStatus.ON_CONNECT_FALSE);
            setResult(RESULT_OK, intent);
            RoomManager.get().destroy();
            finish();
        } else {
            mIsAlreadyConnected = true;
        }
    }

    @Override
    public void onGetUserMedia(GLSurfaceView videoView, Point size) {
        RoomManager.get().putVideo(null, videoView);
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager
                .findFragmentById(R.id.split_container);
        if (fragment == null) {
            fragment = new SelfVideoFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.split_container, fragment).commit();
            RoomManager.get().setSplitFragmentClass(fragment.getClass());
        }
    }

    @Override
    public void onWarning(String message) {
        Log.d(TAG, "onWarning()::message->" + message);
    }

    @Override
    public void onDisconnect(String message) {
        Log.d(TAG, "onDisconnect()::message->" + message);
        if (!mIsExplicitlyTerminated) {
            Intent intent = new Intent();
            intent.putExtra(JoinRoomActivity.EXTRA_RESULT_DISCONNECT_STATUS,
                    JoinRoomActivity.DisconnectStatus.ON_DISCONNECT);
            setResult(RESULT_OK, intent);
            RoomManager.get().destroy();
            finish();
        }
    }

    @Override
    public void onReceiveLog(String message) {
        Log.d(TAG, "onReceiveLog()::message->" + message);
    }

    // -------------------------------------------------------------------------------------------------
// RemotePeerDelegate callbacks
// -------------------------------------------------------------------------------------------------
    @Override
    public void onUserData(String peerId, Object userData) {
        String oldNick = RoomManager.get().getDisplayName(peerId);
        String userDataStr = userData.toString();
        RoomManager.get().putDisplayName(peerId, userDataStr);
        Utility.showShortToast(getApplicationContext(),
                R.string.message_on_user_data, oldNick, userDataStr);
    }

    @Override
    public void onOpenDataConnection(String peerId) {
        Log.d(TAG, "onOpenDataConnection()::peerId->" + peerId);
    }

    @Override
    public void onPeerJoin(String peerId, Object userData) {
        String userDataStr = getDisplayNameFromUserData(userData);
        RoomManager.get().putDisplayName(peerId, userDataStr);
        Utility.showShortToast(getApplicationContext(),
                R.string.message_on_peer_join, userDataStr);
    }

    @Override
    public void onGetPeerMedia(String peerId, GLSurfaceView videoView,
                               Point size) {
        RoomManager.get().putVideo(peerId, videoView);

        // Set video UI either immediately or record the video UI required.
        if (mIsRunning)
            setVideoUI();
        else
            recordVideoUI();
    }

    @Override
    public void onPeerLeave(String peerId, String message) {
        Utility.showShortToast(getApplicationContext(),
                R.string.message_on_peer_leave, RoomManager.get()
                        .getDisplayName(peerId));
        RoomManager.get().removePeer(peerId);

        // Set video UI either immediately or record the video UI required.
        if (mIsRunning) {
            setVideoUI();

            // If we are currently in private chat or File UI with this Peer, or if this was the last peer,
            // return to video UI.
            // NOTE XR: Old chat and file implementations below, need to write one for new ones.
            // Check if fragment occupying split_container is a ChatFragment
            // For Chat
      /*Fragment fragment = getFragmentManager().findFragmentById(R.id.split_container);
      if (ChatFragment.class.isInstance(fragment)) {
          int totalPeerVideos = RoomManager.get().getRemoteVideoList().size();
          // Check if there are more peers
          if (totalPeerVideos > 0) {
              // and we are not having private chat with this Peer
              String chatTid = ((ChatFragment) fragment).getTid();
              if (chatTid == null || !chatTid.equals(peerId))
                  // Do nothing special.
                  return;
          }
          // Otherwise, exit from chat and go to video UI.
          setVideoUIFromRoomManager();
      }*/

            // For File
      /*if (FeFragment.class.isInstance(fragment)) {
          int totalPeerVideos = RoomManager.get().getRemoteVideoList().size();
          // Check if there are more peers
          if (totalPeerVideos > 0) {
              // and we are not having a file share process with this Peer
              String fileTid = ((FeFragment) fragment).getTid();
              if (fileTid == null || !fileTid.equals(peerId))
                  // Do nothing special.
                  return;
          }
          // Otherwise, exit from file and go to video UI.
          setVideoUIFromRoomManager();
      }*/
        } else {
            recordVideoUI();
        }
    }

    // -------------------------------------------------------------------------------------------------
// MediaDelegate callbacks
// -------------------------------------------------------------------------------------------------
    @Override
    public void onVideoSize(GLSurfaceView videoView, Point size) {
        RoomManager manager = RoomManager.get();
        if (manager != null) {
            RoomManager.get().putSize(videoView, size);
            Utility.layoutSubviews(videoView, size);
        }
    }

    @Override
    public void onToggleAudio(String peerId, boolean isMuted) {
        int messageId = isMuted ? R.string.message_on_audio_muted
                : R.string.message_on_audio_unmuted;
        Utility.showShortToast(getApplicationContext(), messageId, RoomManager
                .get().getDisplayName(peerId));
    }

    @Override
    public void onToggleVideo(String peerId, boolean isMuted) {
        int messageId = isMuted ? R.string.message_on_video_muted
                : R.string.message_on_video_unmuted;
        Utility.showShortToast(getApplicationContext(), messageId, RoomManager
                .get().getDisplayName(peerId));
    }

    // -------------------------------------------------------------------------------------------------
// MessagesDelegate callbacks
// -------------------------------------------------------------------------------------------------
    @Override
    @Deprecated
    public void onChatMessage(String peerId, String nick, String message,
                              boolean isPrivate) {
        displayChatMessage(peerId, nick, message, isPrivate);
    }

    @Override
    public void onCustomMessage(String peerId, Object message, boolean isPrivate) {
        String nick = RoomManager.get().getDisplayName(peerId);
        displayChatMessage(peerId, nick, message, isPrivate);
    }

    @Override
    public void onPeerMessage(String peerId, Object message, boolean isPrivate) {
        String nick = RoomManager.get().getDisplayName(peerId);
        displayChatMessage(peerId, nick, message, isPrivate);
    }

    // -------------------------------------------------------------------------------------------------
// FileTransferDelegate callbacks
// -------------------------------------------------------------------------------------------------
    @Override
    public void onRequest(String peerId, String fileName, boolean isPrivate) {
        String nick = RoomManager.get().getDisplayName(peerId);
        String strShareTemp = String.format(
                getString(R.string.message_file_request_group), nick, fileName);
        if (isPrivate)
            strShareTemp = String.format(
                    getString(R.string.message_file_request_private), nick,
                    fileName);
        // Put request into queue at last position.
        String[] fileRequest = {strShareTemp, peerId, fileName};
        RoomManager.get().mFileRequestList.add(fileRequest);
        // Process request list.
        processFileRequest();
    }

    @Override
    public void onPermission(String peerId, String fileName, boolean isPermitted) {
        String nick = RoomManager.get().getDisplayName(peerId);
        if (isPermitted) {
            String msg = String
                    .format(getString(R.string.message_permission_true), nick,
                            fileName);
            AlertFragment.newInstance(msg).show(
                    RoomViewActivity.this.getFragmentManager(), TAG);
        } else {
            String msg = String.format(
                    getString(R.string.message_permission_false), nick,
                    fileName);
            AlertFragment.newInstance(msg).show(
                    RoomViewActivity.this.getFragmentManager(), TAG);
        }
    }

    @Override
    public void onDrop(String peerId, String fileName, String message,
                       boolean isExplicit) {
        finishActivity(Utility.REQUEST_CODE_PICK_DIR);
        String msgAlert = "";
        String nick = RoomManager.get().getDisplayName(peerId);
        if (isExplicit) {
            msgAlert = String.format(getString(R.string.message_drop_true),
                    nick, fileName);
        } else {
            msgAlert = String.format(getString(R.string.message_drop_false),
                    nick, fileName, message);
        }
        // Clear UI for dropped file if any.
        clearDroppedFileUI(peerId, fileName, msgAlert);
    }

    @Override
    public void onComplete(String peerId, String fileName, boolean isSending) {
        String nick = RoomManager.get().getDisplayName(peerId);

        // Add in the last 100% in case file progress showed less than 100%
        // (a result of using chunk for estimation of file transferred).
        if (isSending) {
            Utility.showRapidShortToast(this, R.string.message_progress_send,
                    fileName, nick, "100");
        } else if (!isSending) {
            Utility.showRapidShortToast(this, R.string.message_progress_save,
                    fileName, nick, "100");
        }

        // Announce completion. Do not use showRapidShortToast so as not to
        // cancel 100% progress toast.
        if (isSending) {
            Utility.showShortToast(this, R.string.message_complete_send, nick,
                    fileName);
        } else if (!isSending) {
            Utility.showShortToast(this, R.string.message_complete_save, nick,
                    fileName);
        }
    }

    @Override
    public void onProgress(String peerId, String fileName, double percentage,
                           boolean isSending) {
        String nick = RoomManager.get().getDisplayName(peerId);
        String pctStr = Double.toString(percentage);
        if (pctStr.length() > 4)
            pctStr = pctStr.substring(0, 4);
        if (isSending) {
            Utility.showRapidShortToast(this, R.string.message_progress_send,
                    fileName, nick, pctStr);
        } else if (!isSending) {
            Utility.showRapidShortToast(this, R.string.message_progress_save,
                    fileName, nick, pctStr);
        }
    }

// -------------------------------------------------------------------------------------------------
// Public methods.
// -------------------------------------------------------------------------------------------------

    public void processFileRequest() {
        // Proceed only if not currently in file explorer UI, and there are
        // requests.
        if (RoomManager.get().isFileUIActive()
                || RoomManager.get().mFileRequestList.size() == 0)
            return;

        // Get earliest request and process it.
        String[] fileRequest = RoomManager.get().mFileRequestList.remove(0);
        // Set File explorer state.
        RoomManager.get().setFileUIActive(true);
        FilePermissionAlertFragment.newInstance(fileRequest[0], fileRequest[1],
                fileRequest[2]).show(
                RoomViewActivity.this.getFragmentManager(), TAG);
    }

// -------------------------------------------------------------------------------------------------
// Private methods
// -------------------------------------------------------------------------------------------------

    // Add new chat to Video UI new chat textView
    private void addChatAlert(String peerId, String msgChat, boolean isPrivate) {
        // Current chat UI target (if any)
        String chatPeerId = RoomManager.get().getChatPeerId();
        boolean chatUI = true;
        if (RoomManager.get().getChatAdapter() == null)
            chatUI = false;
        TextView newChatTxtVw;
        if (isPrivate) {
            // Do not add chat alert if already in targeted chat UI
            if (chatPeerId != null && chatPeerId.equals(peerId))
                return;

            newChatTxtVw = RoomManager.get().getPrivateTextView(peerId);
            RoomManager.get().setPrivateNotif(peerId, msgChat);
        } else {
            // Do not add chat alert if already in targeted chat UI
            if (chatPeerId == null && chatUI || chatPeerId != null
                    && chatPeerId.equals(peerId))
                return;

            newChatTxtVw = RoomManager.get().getGroupTextView(peerId);
            RoomManager.get().setGroupNotif(peerId, msgChat);
        }
        if (newChatTxtVw != null)
            newChatTxtVw.setText(msgChat);
    }

    // Add message to targeted chat UI if it is currently up, else add to chat
    // record.
    private void addChatMsg(String peerId, String nick, String message,
                            boolean isPrivate) {
        String msgChat;
        OneComment groupInPrivate = null;
        OneComment comment = null;

        // Create formatted chat message.
        if (!isPrivate) {
            groupInPrivate = new OneComment(true, "[GRP] " + message);
            msgChat = nick + ": " + message;
        } else {
            msgChat = message;
        }
        comment = new OneComment(true, msgChat);

        // Add to chat record.
        if (!isPrivate) {
            RoomManager.get().addGroupChat(comment);
            RoomManager.get().addPrivateChat(peerId, groupInPrivate);
        } else {
            RoomManager.get().addPrivateChat(peerId, comment);
        }

        // If targeted Chat UI is up, try to add to chat UI.
        DiscussArrayAdapter chatAdapter = RoomManager.get().getChatAdapter();
        if (chatAdapter != null) {
            String chatPeerId = RoomManager.get().getChatPeerId();
            // For incoming group chat
            if (!isPrivate) {
                if (chatPeerId == null) {
                    // Add group chat to group chat UI
                    chatAdapter.add(comment);
                } else if (chatPeerId.equals(peerId)) {
                    // Add group chat to targeted private chat UI
                    chatAdapter.add(groupInPrivate);
                }
                // For incoming private chat
                // If current Chat UI is targetted private Chat UI
            } else if (chatPeerId != null && chatPeerId.equals(peerId)) {
                // Add private chat to targeted private Chat UI
                chatAdapter.add(comment);
            }
        }
    }

    private void clearDroppedFileUI(String peerId, String fileName,
                                    String message) {
        // Check if FilePermissionAlertFragment exists
        FilePermissionAlertFragment fragment = RoomManager.get()
                .getFileAlertFragment();
        if (fragment != null) {
            // If it is the dropped transaction, dismmiss this fragment
            if (fragment.getPeerId().equals(peerId)
                    && fragment.getFileName().equals(fileName)) {
                fragment.saveTimeout(message);
                mCancelMessage = message;
                mActiveFileUICancelled = true;
            }
            // FileBrowserActivity will be handled from fileAlertFragment.
        } else {
            // Check if FileBrowserActivity exists
            FileBrowserActivity fe = RoomManager.get().getFileBrowserActivity();
            if (fe != null) {
                // If it the dropped transaction, dismmiss this file explorer
                // activity
                if (fe.getPeerId().equals(peerId)
                        && fe.getFileName().equals(fileName)) {
                    fe.saveTimeout(message);
                    mCancelMessage = message;
                    mActiveFileUICancelled = true;
                }
            }
        }

        // If current file transfer UI had been cleared
        if (!mActiveFileUICancelled
                && !clearDroppedPendingRequest(peerId, fileName)) {
            // The file dropped was not waiting for our permission, i.e.,
            // it could be an on going transfer, or one initiated by us.
            // Simply alert the user.
            AlertFragment.newInstance(message).show(
                    RoomViewActivity.this.getFragmentManager(), TAG);
        }
    }

    // Check for pending request and remove them silently.
    // The earliest request that matches peerId and fileName will be removed.
    // Returns true if a request was removed and false otherwise.
    private boolean clearDroppedPendingRequest(String peerId, String fileName) {
        String[] fileRequest = null;
        ArrayList<String[]> fileRequestList = RoomManager.get().mFileRequestList;
        int pending = fileRequestList.size();
        if (pending == 0)
            return false;

        // Find earliest request in queue that matches.
        for (int i = 0; i < pending; ++i) {
            String[] request = fileRequestList.get(i);
            if (request[1].equals(peerId) && request[2].equals(fileName)) {
                fileRequest = request;
                break;
            }
        }

        // If found, remove it silently.
        if (fileRequest != null) {
            fileRequestList.remove(fileRequest);
            return true;
        } else
            return false;
    }

    private void displayChatMessage(String peerId, String nick, Object message,
                                    boolean isPrivate) {
        String msgAlert = nick + ": ";
        if (isPrivate)
            msgAlert += "[P2P] ";
        msgAlert += message.toString();
        // Add new chat to Video UI new chat textView
        addChatAlert(peerId, msgAlert, isPrivate);
        // Add message to chat UI if it is currently up, else add to chat
        // container.
        addChatMsg(peerId, nick, message.toString(), isPrivate);
    }

    private String getDisplayNameFromUserData(Object userData) {
        String displayName = "";
        if (JSONObject.class.isAssignableFrom(userData.getClass())) {
            try {
                displayName = ((JSONObject) userData).getString("displayName");
            } catch (JSONException e) {
                Log.w(TAG, e.getLocalizedMessage(), e);
            }
        } else {
            displayName = (String) userData;
        }
        return displayName;
    }

    private void recordVideoUI() {
        int totalPeerVideos = RoomManager.get().getRemoteVideoList().size();
        switch (totalPeerVideos) {
            case 0:
                RoomManager.get().setSplitFragmentClass(SelfVideoFragment.class);
                break;
            case 1:
                RoomManager.get().setSplitFragmentClass(Split1Fragment.class);
                break;
            case 2:
                RoomManager.get().setSplitFragmentClass(Split2Fragment.class);
                break;
            case 3:
                RoomManager.get().setSplitFragmentClass(Split3Fragment.class);
                break;
            case 4:
                RoomManager.get().setSplitFragmentClass(Split4Fragment.class);
                break;
            default:
                break;
        }
        RoomManager.get().setSplitChanged(true);
    }

    // Set the audio path according to whether earphone is connected.
    // Use ear piece if earphone is connected.
    // Use speakerphone if no earphone is connected.
    private void setAudioPath() {
        AudioManager audioManager =
                ((AudioManager) getSystemService(android.content.Context.AUDIO_SERVICE));
        boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
        mMediaPlayer = new MediaPlayer();
        if (isWiredHeadsetOn) {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            audioManager.setSpeakerphoneOn(false);
        } else {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            audioManager.setSpeakerphoneOn(true);
        }
    }

    // Set Control Panel contents if remote peers are present.
    // Set refresh to true if new control fragment is desired (even when one exists).
    private void setControlPanel(boolean refresh) {
        int totalPeerVideos = RoomManager.get().getRemoteVideoList().size();
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager
                .findFragmentById(R.id.control_panel_container);
        if (totalPeerVideos == 0) {
            if (fragment != null)
                fragmentManager.beginTransaction().remove(fragment).commit();
            return;
        } else {
            // If existing and no need to refresh, we are done.
            if (fragment != null && !refresh)
                return;

            // Add new or refresh control panel.
            fragment = new ControlPanelFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.control_panel_container, fragment).commit();
        }
    }

    private void setVideoUI() {
        int totalPeerVideos = RoomManager.get().getRemoteVideoList().size();
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragmentOld = fragmentManager
                .findFragmentById(R.id.split_container);
        Fragment fragment = fragmentManager
                .findFragmentById(R.id.split_container);
        switch (totalPeerVideos) {
            case 0: {
                fragment = new SelfVideoFragment();
                // Remove control panel, if exists.
                setControlPanel(false);
                // Set audio and video to none muted.
                RoomManager.get().getConnection().muteAudio(false);
                RoomManager.get().getConnection().muteVideo(false);
            }
            break;
            case 1: {
                fragment = new Split1Fragment();
                // Add or refresh control panel.
                setControlPanel(false);
            }
            break;
            case 2: {
                fragment = new Split2Fragment();
            }
            break;
            case 3: {
                fragment = new Split3Fragment();
            }
            break;
            case 4: {
                fragment = new Split4Fragment();
            }
            break;
            default:
                break;
        }

        // If this is first start, or currently in Video mode,
        // directly change the video fragment used.
        if (fragmentOld == null
                || RoomFragment.class.isAssignableFrom(fragmentOld.getClass())) {
            // Replace previous video fragment with current video fragment.
            fragmentManager.beginTransaction()
                    .replace(R.id.split_container, fragment).commit();
        }

        // Record the type of video UI that should be present.
        RoomManager.get().setSplitFragmentClass(fragment.getClass());
    }

    // Create the video UI state recorded in RoomManager.
    private void setVideoUIFromRoomManager() {
        try {
            // Show control panel
            View view = (ViewGroup) findViewById(R.id.control_panel_container);
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            layoutParams.weight = mCpWeight = 0.25f;
            view.setLayoutParams(layoutParams);
            // Refresh Control Panel contents if peer(s) exist.
            setControlPanel(true);
            // Show Video UI
            Fragment fragment = (Fragment) RoomManager.get()
                    .getSplitFragmentClass().newInstance();
            getFragmentManager().beginTransaction()
                    .replace(R.id.split_container, fragment).commit();
        } catch (InstantiationException e) {
            Log.w(TAG, e.getLocalizedMessage(), e);
        } catch (IllegalAccessException e) {
            Log.w(TAG, e.getLocalizedMessage(), e);
        }
    }

}
