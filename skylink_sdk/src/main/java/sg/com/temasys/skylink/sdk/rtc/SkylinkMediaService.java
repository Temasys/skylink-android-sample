package sg.com.temasys.skylink.sdk.rtc;

/**
 * Created by xiangrong on 25/5/15.
 */
public class SkylinkMediaService {
    private static final String TAG = SkylinkMediaService.class.getName();
    private SkylinkConnection skylinkConnection;
    private SkylinkConnectionService skylinkConnectionService;

    public SkylinkMediaService(SkylinkConnection skylinkConnection,
                               SkylinkConnectionService skylinkConnectionService) {
        this.skylinkConnection = skylinkConnection;
        this.skylinkConnectionService = skylinkConnectionService;
    }

    /**
     * Mutes the local user's audio and notifies all the peers in the room.
     *
     * @param isMuted Flag that specifies whether audio should be mute
     */
    void muteLocalAudio(boolean isMuted) {
        org.webrtc.AudioTrack localAudioTrack = skylinkConnection.getLocalAudioTrack();

        if (skylinkConnection.getMyConfig().hasAudioSend() &&
                (localAudioTrack.enabled() == isMuted)) {

            localAudioTrack.setEnabled(!isMuted);
            // Inform Peers
            skylinkConnectionService.sendMuteAudio(isMuted);
        }
    }

    /**
     * Mutes the local user's video and notifies all the peers in the room.
     *
     * @param isMuted Flag that specifies whether video should be mute
     */
    void muteLocalVideo(boolean isMuted) {

        org.webrtc.VideoTrack localVideoTrack = skylinkConnection.getLocalVideoTrack();
        if (skylinkConnection.getMyConfig().hasVideoSend() &&
                (localVideoTrack.enabled() == isMuted)) {

            localVideoTrack.setEnabled(!isMuted);
            // Inform Peers
            skylinkConnectionService.sendMuteVideo(isMuted);
        }
    }

}
