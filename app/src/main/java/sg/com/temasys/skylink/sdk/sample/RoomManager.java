package sg.com.temasys.skylink.sdk.sample;

import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.warting.bubbles.DiscussArrayAdapter;
import com.warting.bubbles.OneComment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection;

public class RoomManager {

    private static RoomManager sConnectionManager;

    private boolean mIsSplitChanged = false;
    private Class<?> mSplitFragmentClass;
    private DiscussArrayAdapter mChatAdapter;
    private List<OneComment> mGroupChat;
    private Map<String, List<OneComment>> mPrivateChat;
    private Map<String, String> mGroupNotifPool;
    private Map<String, String> mPrivateNotifPool;
    private Map<String, TextView> mGroupTextViewPool;
    private Map<String, TextView> mPrivateTextViewPool;
    private Map<String, VideoInfo> mVideoPool;
    private SkyLinkConnection mConnection;

    // File Related
    private boolean mIsFileUIActive = false;
    private FileBrowserActivity mFileBrowserActivity;
    private FilePermissionAlertFragment mFileAlertFragment;
    private String mFileTransferPeerId;

    // To hold adapter on rotation, as DiscussArrayAdapter is not serializable.
    private DiscussArrayAdapter mTempChatAdapter;

    // Target of current Chat UI. null if group chat.
    private String mChatPeerId = null;

    // List of pending file requests.
    // String[ <request msg>, <peerId>, <filename> ]
    public ArrayList<String[]> mFileRequestList;

    private RoomManager(SkyLinkConnection connection) {
        mConnection = connection;

        mGroupChat = new ArrayList<OneComment>();
        mPrivateChat = new HashMap<String, List<OneComment>>();
        mGroupNotifPool = new Hashtable<String, String>();
        mPrivateNotifPool = new Hashtable<String, String>();
        mGroupTextViewPool = new HashMap<String, TextView>();
        mPrivateTextViewPool = new HashMap<String, TextView>();
        mVideoPool = new HashMap<String, VideoInfo>();

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

    public boolean isSplitChanged() {
        return mIsSplitChanged;
    }

    public void setSplitChanged(boolean splitChanged) {
        mIsSplitChanged = splitChanged;
    }

    public Class<?> getSplitFragmentClass() {
        return mSplitFragmentClass;
    }

    public void setSplitFragmentClass(Class<?> splitFragmentClass) {
        mSplitFragmentClass = splitFragmentClass;
    }

    public DiscussArrayAdapter getChatAdapter() {
        return mChatAdapter;
    }

    public void setChatAdapter(DiscussArrayAdapter chatAdapter) {
        mChatAdapter = chatAdapter;
    }

    public List<OneComment> getGroupChat() {
        return mGroupChat;
    }

    public void addGroupChat(OneComment oneComment) {
        mGroupChat.add(oneComment);
    }

    public List<OneComment> getPrivateChat(String peerId) {
        return mPrivateChat.get(peerId);
    }

    public void addPrivateChat(String peerId, OneComment oneComment) {
        List<OneComment> privateChatList = mPrivateChat.get(peerId);
        if (privateChatList == null) {
            privateChatList = new ArrayList<OneComment>();
            mPrivateChat.put(peerId, privateChatList);
        }
        privateChatList.add(oneComment);
    }

    public String getGroupNotif(String peerId) {
        return mGroupNotifPool.get(peerId);
    }

    public void setGroupNotif(String peerId, String text) {
        mGroupNotifPool.put(peerId, text);
    }

    public String getPrivateNotif(String peerId) {
        return mPrivateNotifPool.get(peerId);
    }

    public void setPrivateNotif(String peerId, String text) {
        mPrivateNotifPool.put(peerId, text);
    }

    public Map<String, TextView> getGroupTextViewPool() {
        return this.mGroupTextViewPool;
    }

    public TextView getGroupTextView(String peerId) {
        return mGroupTextViewPool.get(peerId);
    }

    public void putGroupTextView(String peerId, TextView txtVw) {
        mGroupTextViewPool.put(peerId, txtVw);
    }

    public TextView getPrivateTextView(String peerId) {
        return mPrivateTextViewPool.get(peerId);
    }

    public void putPrivateTextView(String peerId, TextView txtVw) {
        mPrivateTextViewPool.put(peerId, txtVw);
    }

    public SkyLinkConnection getConnection() {
        return mConnection;
    }

    public void setConnection(SkyLinkConnection connection) {
        mConnection = connection;
    }

    public boolean isFileUIActive() {
        return mIsFileUIActive;
    }

    public void setFileUIActive(boolean fileActive) {
        mIsFileUIActive = fileActive;
    }

    public FileBrowserActivity getFileBrowserActivity() {
        return mFileBrowserActivity;
    }

    public void setFileBrowserActivity(FileBrowserActivity fileBrowserActivity) {
        mFileBrowserActivity = fileBrowserActivity;
    }

    public FilePermissionAlertFragment getFileAlertFragment() {
        return mFileAlertFragment;
    }

    public void setFileAlertFragment(
            FilePermissionAlertFragment fileAlertFragment) {
        mFileAlertFragment = fileAlertFragment;
    }

    public String getFileTransferPeerId() {
        return mFileTransferPeerId;
    }

    public void setFileTransferPeerId(String fileTransferPeerId) {
        mFileTransferPeerId = fileTransferPeerId;
    }

    public DiscussArrayAdapter getTempChatAdapter() {
        return mTempChatAdapter;
    }

    public void setTempChatAdapter(DiscussArrayAdapter chatAdapterTemp) {
        mTempChatAdapter = chatAdapterTemp;
    }

    public String getChatPeerId() {
        return mChatPeerId;
    }

    public void setChatPeerId(String chatPeerId) {
        mChatPeerId = chatPeerId;
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

    public void removePeer(String peerId) {
        VideoInfo videoInfo = mVideoPool.get(peerId);
        if (videoInfo != null) {
            ViewGroup parentView = (ViewGroup) videoInfo.getVideoView()
                    .getParent();
            if (parentView != null)
                parentView.removeView(videoInfo.getVideoView());
        }
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
            mConnection.disconnectFromRoom();
            mConnection = null;
        }
        sConnectionManager = null;
    }

    public class VideoInfo {

        private GLSurfaceView mVideoView;
        private Point mSize;
        private String mDisplayName;
        private String mPeerId;

        public VideoInfo(String peerId) {
            super();
            this.mPeerId = peerId;
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

        public String getDisplayName() {
            return mDisplayName;
        }

        public void setDisplayName(String displayName) {
            mDisplayName = displayName;
        }

        public void setSize(Point size) {
            mSize = size;
        }

        public String getPeerId() {
            return mPeerId;
        }

    }

}
