package com.temasys.skylink.sample;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection;
import com.warting.bubbles.DiscussArrayAdapter;
import com.warting.bubbles.OneComment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RoomManager {

    private static RoomManager sConnectionManager;

    private Class<?> mSplitFragmentClass;
    private Map<String, VideoInfo> mVideoPool;
    private Map<String, TextView> mNewChatPrivatePool;
    private Map<String, TextView> mNewChatGroupPool;
    private SkyLinkConnection mConnection;
    private String mMyDisplayName;
    private List<OneComment> mGroupChat;
    private Map<String, List<OneComment>> mPrivateChat;
    private DiscussArrayAdapter mChatAdapter;
    // To hold adapter on rotation, as DiscussArrayAdapter is not serializable.
    private DiscussArrayAdapter mChatAdapterTemp;
    // Target of current Chat UI. null if group chat.
    private String mChatPeerId = null;
    private boolean mSplitChanged = false;

    // File
    private String mFileTransferPeerId;
    private boolean mFileActive = false;
    private FilePermissionAlertFragment mFileAlertFragment;
    private FileBrowserActivity mFileBrowserActivity;

    // List of pending file requests.
    // String[ <request msg>, <peerId>, <filename> ]
    public ArrayList<String[]> mFileRequestList;

    private RoomManager(SkyLinkConnection connection) {
        mConnection = connection;
        mVideoPool = new HashMap<String, VideoInfo>();
        mNewChatPrivatePool = new HashMap<String, TextView>();
        mNewChatGroupPool = new HashMap<String, TextView>();
        mPrivateChat = new HashMap<String, List<OneComment>>();
        mFileRequestList = new ArrayList<String[]>();
    }

    public static RoomManager getInstance(SkyLinkConnection connection) {
        if (sConnectionManager == null) {
            sConnectionManager = new RoomManager(connection);
        }
        return sConnectionManager;
    }

    public static RoomManager get() {
        return sConnectionManager;
    }

    public Class<?> getSplitFragmentClass() {
        return mSplitFragmentClass;
    }

    public void setSplitFragmentClass(Class<?> splitFragmentClass) {
        mSplitFragmentClass = splitFragmentClass;
    }

    public SkyLinkConnection getConnection() {
        return mConnection;
    }

    public void setConnection(SkyLinkConnection connection) {
        mConnection = connection;
    }

    public String getMyDisplayName() {
        return mMyDisplayName;
    }

    public void setMyDisplayName(String displayName) {
        mMyDisplayName = displayName;
    }

    public String getFileTransferPeerId() {
        return mFileTransferPeerId;
    }

    public void setFileTransferPeerId(String fileTransferPeerId) {
        mFileTransferPeerId = fileTransferPeerId;
    }

    public DiscussArrayAdapter getChatAdapter() {
        return mChatAdapter;
    }

    public void setChatAdapter(DiscussArrayAdapter chatAdapter) {
        mChatAdapter = chatAdapter;
    }

    public DiscussArrayAdapter getChatAdapterTemp() {
        return mChatAdapterTemp;
    }

    public void setChatAdapterTemp(DiscussArrayAdapter chatAdapterTemp) {
        mChatAdapterTemp = chatAdapterTemp;
    }

    public String getChatPeerId() {
        return mChatPeerId;
    }

    public void setChatPeerId(String chatPeerId) {
        mChatPeerId = chatPeerId;
    }

    public boolean isSplitChanged() {
        return mSplitChanged;
    }

    public void setSplitChanged(boolean splitChanged) {
        mSplitChanged = splitChanged;
    }

    public boolean getFileActive() {
        return mFileActive;
    }

    public void setFileActive(boolean fileActive) {
        mFileActive = fileActive;
    }

    public FilePermissionAlertFragment getFileAlertFragment() {
        return mFileAlertFragment;
    }

    public void setFileAlertFragment(FilePermissionAlertFragment fileAlertFragment) {
        mFileAlertFragment = fileAlertFragment;
    }

    public FileBrowserActivity getFileBrowserActivity() {
        return mFileBrowserActivity;
    }

    public void setFileBrowserActivity(FileBrowserActivity fileBrowserActivity) {
        mFileBrowserActivity = fileBrowserActivity;
    }

    public void putDisplayName(String peerId, String displayName) {
        VideoInfo videoInfo = mVideoPool.get(peerId);
        if (videoInfo == null) {
            videoInfo = new VideoInfo(peerId);
            mVideoPool.put(peerId, videoInfo);
        }
        videoInfo.setDisplayName(displayName);
    }

    public void putVideo(String peerId, GLSurfaceView surfaceView) {
        VideoInfo videoInfo = mVideoPool.get(peerId);
        if (videoInfo == null) {
            videoInfo = new VideoInfo(peerId);
            mVideoPool.put(peerId, videoInfo);
        }
        videoInfo.setVideoView(surfaceView);
    }

    public void putSize(GLSurfaceView surfaceView, Point size) {
        Iterator<Map.Entry<String, VideoInfo>> iterator = mVideoPool.entrySet()
                .iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, VideoInfo> entry = iterator.next();
            VideoInfo videoInfo = entry.getValue();
            if (surfaceView == videoInfo.getVideoView()) {
                videoInfo.setSize(size);
                break;
            }
        }
    }

    public Map<String, TextView> getNewChatGroupPool() {
        return this.mNewChatGroupPool;
    }

    public void putChatPrivateTextView(String peerId, TextView txtVw) {
        mNewChatPrivatePool.put(peerId, txtVw);
    }

    public void putChatGroupTextView(String peerId, TextView txtVw) {
        mNewChatGroupPool.put(peerId, txtVw);
    }

    public void setChatAlert(
            ViewGroup rootView, VideoInfo videoInfo, int chatPrivateRid, int chatGroupRid) {

        String peerId = videoInfo.getPeerId();

        // Assign textView to Peer
        TextView newChatPrivateTxtVw = (TextView) rootView.findViewById(chatPrivateRid);
        putChatPrivateTextView(peerId, newChatPrivateTxtVw);
        newChatPrivateTxtVw.setText(ChatContent.newChatPrivateMsgList.get(peerId));

        TextView newChatGroupTxtVw = (TextView) rootView.findViewById(chatGroupRid);
        putChatGroupTextView(peerId, newChatGroupTxtVw);
        newChatGroupTxtVw.setText(ChatContent.newChatGroupMsgList.get(peerId));
        // Set background colours
        // Format: Hexadecimal AARRGGBB each value can be from 0-255 (0-F).
        // Set background colours
        // 20% white background.
        newChatPrivateTxtVw.setBackgroundColor(0X33FFFFFF);
        newChatGroupTxtVw.setBackgroundColor(0X33FFFFFF);
        // Set 100% Black text.
        newChatPrivateTxtVw.setTextColor(0XFF000000);
        newChatGroupTxtVw.setTextColor(0XFF000000);
    }

    public Context getContext() {
        return mConnection.getContext();
    }

    public String getDisplayName(String peerId) {
        VideoInfo videoInfo = mVideoPool.get(peerId);
        if (videoInfo != null)
            return videoInfo.getDisplayName();
        else
            return null;
    }

    public VideoInfo getSelfVideo() {
        Iterator<Map.Entry<String, VideoInfo>> iterator = mVideoPool.entrySet()
                .iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, VideoInfo> entry = iterator.next();
            String key = entry.getKey();
            VideoInfo videoInfo = entry.getValue();
            if (key == null)
                return videoInfo;
        }
        return null;
    }

    public List<VideoInfo> getRemoteVideoList() {
        List<VideoInfo> videoInfoList = new ArrayList<VideoInfo>();
        Iterator<Map.Entry<String, VideoInfo>> iterator = mVideoPool.entrySet()
                .iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, VideoInfo> entry = iterator.next();
            String key = entry.getKey();
            VideoInfo videoInfo = entry.getValue();
            if (key != null)
                videoInfoList.add(videoInfo);
        }
        return videoInfoList;
    }

    public Point getRemoteVideoSize(GLSurfaceView surfaceView) {
        Iterator<Map.Entry<String, VideoInfo>> iterator = mVideoPool.entrySet()
                .iterator();
        while (iterator.hasNext()) {
            VideoInfo videoInfo = iterator.next().getValue();
            if (surfaceView == videoInfo.getVideoView())
                return videoInfo.getSize();
        }
        return null;
    }

    public TextView getChatPrivateTextView(String peerId) {
        return mNewChatPrivatePool.get(peerId);
    }

    public TextView getChatGroupTextView(String peerId) {
        return mNewChatGroupPool.get(peerId);
    }

    public void removePeer(String peerId) {
        VideoInfo videoInfo = mVideoPool.get(peerId);
        ViewGroup parentView = (ViewGroup) videoInfo.getVideoView().getParent();
        if (parentView != null)
            parentView.removeView(videoInfo.getVideoView());
        mVideoPool.remove(peerId);
    }

    public void destroy() {
        if (mVideoPool != null) {
            Iterator<Map.Entry<String, VideoInfo>> iterator = mVideoPool
                    .entrySet().iterator();
            while (iterator.hasNext()) {
                View videoView = iterator.next().getValue().getVideoView();
                ViewGroup parentView = (ViewGroup) videoView.getParent();
                if (parentView != null)
                    parentView.removeView(videoView);
            }
            mVideoPool.clear();
            mVideoPool = null;
        }
        if (mConnection != null) {
            mConnection.disconnect();
            mConnection = null;
        }
        sConnectionManager = null;
    }

    public void addGroupChat(OneComment oneComment) {
        if (mGroupChat == null)
            mGroupChat = new ArrayList<OneComment>();
        mGroupChat.add(oneComment);
    }

    public List<OneComment> getGroupChat() {
        return mGroupChat;
    }

    public void addPrivateChat(String peerId, OneComment oneComment) {
        List<OneComment> privateChatList = mPrivateChat.get(peerId);
        if (privateChatList == null) {
            privateChatList = new ArrayList<OneComment>();
            mPrivateChat.put(peerId, privateChatList);
        }
        privateChatList.add(oneComment);
    }

    public List<OneComment> getPrivateChat(String peerId) {
        return mPrivateChat.get(peerId);
    }

    public class VideoInfo {

        private String peerId;
        private String mDisplayName;
        private GLSurfaceView mVideoView;
        private Point mSize;


        public VideoInfo(String peerId) {
            super();
            this.peerId = peerId;
        }

        public String getPeerId() {
            return peerId;
        }

        public String getDisplayName() {
            return mDisplayName;
        }

        public void setDisplayName(String displayName) {
            mDisplayName = displayName;
        }

        public GLSurfaceView getVideoView() {
            return mVideoView;
        }

        public void setVideoView(GLSurfaceView videoView) {
            mVideoView = videoView;
        }

        public Point getSize() {
            return mSize;
        }

        public void setSize(Point size) {
            mSize = size;
        }

    }

}
