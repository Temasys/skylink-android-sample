package sg.com.temasys.skylink.sdk.sampleapp.service.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SkylinkPeer implements Serializable {
    private String peerId;
    private String peerName;

    public enum MEDIA_TYPE {
        AUDIO,
        VIDEO,
        SCREEN_SHARING
    }


    // The list to keep track of media ids to manage by the peer
    // For local peer, just have video id(s)
    // For remote peer, have both audio id(s) and video id(s)
    private Map<String, MEDIA_TYPE> mediaIds;

    public SkylinkPeer() {
    }

    public SkylinkPeer(String peerId) {
        this.peerId = peerId;
    }

    public SkylinkPeer(String peerId, String peerName) {
        this.peerId = peerId;
        this.peerName = peerName;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public String getPeerName() {
        return peerName;
    }

    public void setPeerName(String peerName) {
        this.peerName = peerName;
    }

    public Map<String, MEDIA_TYPE> getMediaIds() {
        return mediaIds;
    }

    public void setMediaIds(Map<String, MEDIA_TYPE> mediaIds) {
        this.mediaIds = mediaIds;
    }

    @Override
    public String toString() {
        return peerName + "(" + peerId + ")";
    }

    public void addMediaId(String mediaId, MEDIA_TYPE mediaType) {
        if (mediaIds == null) {
            mediaIds = new HashMap<String, MEDIA_TYPE>();
        }

        mediaIds.put(mediaId, mediaType);
    }

    public MEDIA_TYPE removeMediaId(String mediaId) {
        if(mediaIds == null){
            return null;
        }
        return mediaIds.remove(mediaId);
    }
}