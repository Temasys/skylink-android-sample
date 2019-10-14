package sg.com.temasys.skylink.sdk.sampleapp.service.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;

public class SkylinkPeer implements Serializable {
    private String peerId;
    private String peerName;

    // The map to keep track of media(audio, video, screen) object belongs to the peer
    private Map<String, SkylinkMedia> mediaMap;

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

    public Map<String, SkylinkMedia> getMediaMap() {
        return mediaMap;
    }

    public void setMediaMap(Map<String, SkylinkMedia> mediaMap) {
        this.mediaMap = mediaMap;
    }

    @Override
    public String toString() {
        return peerName + "(" + peerId + ")";
    }

    public void addMedia(SkylinkMedia media) {
        if (mediaMap == null) {
            mediaMap = new HashMap<String, SkylinkMedia>();
        }

        mediaMap.put(media.getMediaId(), media);
    }

    public void updateMedia(SkylinkMedia remoteMedia) {
        if (mediaMap == null) {
            mediaMap = new HashMap<String, SkylinkMedia>();
        }

        mediaMap.put(remoteMedia.getMediaId(), remoteMedia);
    }

    public SkylinkMedia removeMediaId(String mediaId) {
        if (mediaMap == null) {
            return null;
        }
        return mediaMap.remove(mediaId);
    }

    /**
     * Get the first audio object in the mediaMap
     */
    public SkylinkMedia getAudio() {
        if (mediaMap == null || mediaMap.size() == 0)
            return null;

        for (SkylinkMedia media : mediaMap.values()) {
            if (!media.isVideo())
                return media;
        }

        return null;
    }

    /**
     * Get the first video camera object in the mediaMap
     */
    public SkylinkMedia getVideo() {
        if (mediaMap == null || mediaMap.size() == 0)
            return null;

        for (SkylinkMedia media : mediaMap.values()) {
            if (media.getMediaType() == SkylinkMedia.MediaType.VIDEO_CAMERA || media.getMediaType() == SkylinkMedia.MediaType.VIDEO)
                return media;
        }

        return null;
    }

    /**
     * Get the first video screen object in the mediaMap
     */
    public SkylinkMedia getScreen() {
        if (mediaMap == null || mediaMap.size() == 0)
            return null;

        for (SkylinkMedia media : mediaMap.values()) {
            if (media.getMediaType() == SkylinkMedia.MediaType.VIDEO_SCREEN)
                return media;
        }

        return null;
    }
}