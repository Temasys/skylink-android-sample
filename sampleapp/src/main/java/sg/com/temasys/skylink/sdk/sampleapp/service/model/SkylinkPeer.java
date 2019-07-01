package sg.com.temasys.skylink.sdk.sampleapp.service.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;

public class SkylinkPeer implements Serializable {
    private String peerId;
    private String peerName;

    // The list to keep track of media(audio, video, screen) ids to manage by the peer
    private Map<String, SkylinkMedia.MediaType> mediaIds;

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

    public Map<String, SkylinkMedia.MediaType> getMediaIds() {
        return mediaIds;
    }

    public void setMediaIds(Map<String, SkylinkMedia.MediaType> mediaIds) {
        this.mediaIds = mediaIds;
    }

    @Override
    public String toString() {
        return peerName + "(" + peerId + ")";
    }

    public void addMediaId(String mediaId, SkylinkMedia.MediaType mediaType) {
        if (mediaIds == null) {
            mediaIds = new HashMap<String, SkylinkMedia.MediaType>();
        }

        mediaIds.put(mediaId, mediaType);
    }

    public SkylinkMedia.MediaType removeMediaId(String mediaId) {
        if (mediaIds == null) {
            return null;
        }
        return mediaIds.remove(mediaId);
    }
}