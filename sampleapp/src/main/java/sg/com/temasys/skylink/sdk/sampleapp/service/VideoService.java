package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import java.util.ArrayList;
import java.util.List;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkError;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoContract;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionContract;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkConfig.VideoDevice.CAMERA_BACK;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkConfig.VideoDevice.CAMERA_FRONT;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_FHD;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_HDR;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.VIDEO_RESOLUTION_VGA;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;


/**
 * Created by muoi.pham on 20/07/18.
 * The service class is responsible for communicating with the SkylinkSDK API by using SkylinkConnection instance
 */

public class VideoService extends SkylinkCommonService implements VideoContract.Service {

    private static final String TAG = VideoService.class.getCanonicalName();

    public VideoService(Context context) {
        super(context);
    }

    @Override
    public void setPresenter(VideoContract.Presenter presenter) {
        this.presenter = (BasePresenter) presenter;
    }

    @Override
    public void setResPresenter(VideoResolutionContract.Presenter videoResPresenter) {
        this.videoResPresenter = (BasePresenter) videoResPresenter;
    }

    /**
     * Stop or restart the local camera based on the parameter |isToggle|,
     * given that the local video source is available, i.e., had been started and not removed.
     * However, if the intended state of the camera (started or stopped) is already the current
     * state, then no change will be effected.
     * Trigger LifeCycleListener.onWarning if an error occurs, for example:
     * if local video source is not available.
     */
    public void toggleVideo(boolean isRestart) {
        if (mSkylinkConnection != null && localVideoId != null) {
            if (isRestart) {
                final boolean[] success = {true};
                mSkylinkConnection.changeLocalMediaState(localVideoId, SkylinkMedia.MediaState.ACTIVE, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                        success[0] = false;
                    }
                });
                if (!success[0]) {
                    String error = "Unable to ACTIVE video camerq!";
                    toastLog(TAG, context, error);
                }
            } else {
                final boolean[] success = {true};
                mSkylinkConnection.changeLocalMediaState(localVideoId, SkylinkMedia.MediaState.STOPPED, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                        success[0] = false;
                    }
                });
                if (!success[0]) {
                    String error = "Unable to stop video camera!";
                    toastLog(TAG, context, error);
                }
            }
        } else if (isRestart) {
            createLocalVideo();
        }
    }

    public void toggleScreen(boolean start) {
        if (mSkylinkConnection != null && localScreenSharingId != null) {
            if (start) {
                final boolean[] success = {true};
                mSkylinkConnection.changeLocalMediaState(localScreenSharingId, SkylinkMedia.MediaState.ACTIVE, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                        success[0] = false;
                    }
                });
                if (!success[0]) {
                    String error = "Unable to ACTIVE screen!";
                    toastLog(TAG, context, error);
                }
            } else {
                final boolean[] success = {true};
                mSkylinkConnection.changeLocalMediaState(localScreenSharingId, SkylinkMedia.MediaState.STOPPED, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                        success[0] = false;
                    }
                });
                if (!success[0]) {
                    String error = "Unable to stop screen!";
                    toastLog(TAG, context, error);
                }
            }
        } else
            createLocalScreen();
    }

    /**
     * Mutes the local user's audio and notifies all the peers in the room.
     *
     * @param audioMuted Flag that specifies whether audio should be mute
     */
    public void muteLocalAudio(boolean audioMuted) {
        if (mSkylinkConnection != null) {
            if (audioMuted) {
                final boolean[] success = {true};
                mSkylinkConnection.changeLocalMediaState(localAudioId, SkylinkMedia.MediaState.MUTED, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                        success[0] = false;
                    }
                });
                if (!success[0]) {
                    String error = "Unable to muteLocalAudio!";
                    toastLog(TAG, context, error);
                }
            } else {
                final boolean[] success = {true};
                mSkylinkConnection.changeLocalMediaState(localAudioId, SkylinkMedia.MediaState.ACTIVE, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                        success[0] = false;
                    }
                });
                if (!success[0]) {
                    String error = "Unable to ACTIVE LocalAudio!";
                    toastLog(TAG, context, error);
                }
            }
        }
    }

    /**
     * Mutes the local user's video and notifies all the peers in the room.
     * Note that black video frames (consuming bandwidth) are still being sent to remote Peer(s).
     *
     * @param videoMuted Flag that specifies whether video should be mute
     */
    public void muteLocalVideo(boolean videoMuted) {
        if (mSkylinkConnection != null) {
            if (videoMuted) {
                final boolean[] success = {true};
                mSkylinkConnection.changeLocalMediaState(localVideoId, SkylinkMedia.MediaState.MUTED, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                        success[0] = false;
                    }
                });
                if (!success[0]) {
                    String error = "Unable to muteLocalVideo!";
                    toastLog(TAG, context, error);
                }
            } else {
                final boolean[] success = {true};
                mSkylinkConnection.changeLocalMediaState(localVideoId, SkylinkMedia.MediaState.ACTIVE, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                        success[0] = false;
                    }
                });
                if (!success[0]) {
                    String error = "Unable to ACTIVE LocalVideo!";
                    toastLog(TAG, context, error);
                }
            }
        }
    }

    /**
     * Mutes the local user's screen video and notifies all the peers in the room.
     * Note that black video frames (consuming bandwidth) are still being sent to remote Peer(s).
     *
     * @param screenMuted Flag that specifies whether screen video should be mute
     */
    public void muteLocalScreen(boolean screenMuted) {
        if (mSkylinkConnection != null) {
            if (screenMuted) {
                final boolean[] success = {true};
                mSkylinkConnection.changeLocalMediaState(localScreenSharingId, SkylinkMedia.MediaState.MUTED, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                        success[0] = false;
                    }
                });
                if (!success[0]) {
                    String error = "Unable to muteLocalScreen!";
                    toastLog(TAG, context, error);
                }
            } else {
                final boolean[] success = {true};
                mSkylinkConnection.changeLocalMediaState(localScreenSharingId, SkylinkMedia.MediaState.ACTIVE, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                        success[0] = false;
                    }
                });
                if (!success[0]) {
                    String error = "Unable to ACTIVE LocalScreen!";
                    toastLog(TAG, context, error);
                }
            }
        }
    }

    /**
     * Return the specific video view of the SkylinkMedia that mediaId is provided
     *
     * @return Video View of Peer or null if none is matched with the input id.
     */
    public SurfaceViewRenderer getVideoView(String mediaId) {
        if (mSkylinkConnection != null) {
            SkylinkMedia media = mSkylinkConnection.getSkylinkMedia(mediaId);
            if (media != null) {
                return media.getVideoView();
            }
        }

        return null;
    }

    /**
     * Return the list of video view from a list of SkylinkMedia objects that can get from SDK by the peer id and media type
     *
     * @param peerId id of the peer that the media belongs
     *               if null, consider as self peer
     * @return lis of video views that comes with peer id and media type
     */
    public List<SurfaceViewRenderer> getVideoViews(String peerId, SkylinkMedia.MediaType mediaType) {
        List<SkylinkMedia> mediaObjects = null;
        List<SurfaceViewRenderer> videoViews = new ArrayList<SurfaceViewRenderer>();

        if (mSkylinkConnection != null) {
            mediaObjects = mSkylinkConnection.getSkylinkMediaList(mediaType, peerId);
            if (mediaObjects == null || mediaObjects.size() == 0) {
                return null;
            }
        }

        for (SkylinkMedia media : mediaObjects) {
            if (media.getMediaState() != SkylinkMedia.MediaState.UNAVAILABLE) {
                videoViews.add(media.getVideoView());
            }
        }

        return videoViews;
    }

    /**
     * Change the speaker output to on/off
     * The speaker is automatically turned off when audio bluetooth is connected.
     */
    public void changeSpeakerOutput(boolean isSpeakerOn) {
        AudioRouter.changeAudioOutput(context, isSpeakerOn);
    }

    /**
     * Sets the specified listeners for video function
     * Video call needs to implement LifeCycleListener, RemotePeerListener, MediaListener, OsListener
     */
    @Override
    public void setSkylinkListeners() {
        if (mSkylinkConnection != null) {
            mSkylinkConnection.setLifeCycleListener(this);
            mSkylinkConnection.setRemotePeerListener(this);
            mSkylinkConnection.setMediaListener(this);
            mSkylinkConnection.setOsListener(this);
        }
    }

    /**
     * Get the config for video function
     * User can custom video config by using SkylinkConfig
     */
    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // VideoCall config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setP2PMessaging(true);
        skylinkConfig.setFileTransfer(true);
        skylinkConfig.setMirrorLocalFrontCameraView(true);
        skylinkConfig.setReportVideoResolutionUntilStable(true);

        // Allow only 1 remote Peer to join.
        skylinkConfig.setMaxRemotePeersConnected(1, SkylinkConfig.RoomMediaType.VIDEO); // Default is 4 remote Peers.

        // Set the room size
        skylinkConfig.setSkylinkRoomSize(SkylinkConfig.SkylinkRoomSize.SMALL);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);

        //Set default video resolution setting
        String videoResolution = Utils.getDefaultVideoResolution();
        if (videoResolution.equals(VIDEO_RESOLUTION_VGA)) {
            skylinkConfig.setDefaultVideoWidth(SkylinkConfig.VIDEO_WIDTH_VGA);
            skylinkConfig.setDefaultVideoHeight(SkylinkConfig.VIDEO_HEIGHT_VGA);
        } else if (videoResolution.equals(VIDEO_RESOLUTION_HDR)) {
            skylinkConfig.setDefaultVideoWidth(SkylinkConfig.VIDEO_WIDTH_HDR);
            skylinkConfig.setDefaultVideoHeight(SkylinkConfig.VIDEO_HEIGHT_HDR);
        } else if (videoResolution.equals(VIDEO_RESOLUTION_FHD)) {
            skylinkConfig.setDefaultVideoWidth(SkylinkConfig.VIDEO_WIDTH_FHD);
            skylinkConfig.setDefaultVideoHeight(SkylinkConfig.VIDEO_HEIGHT_FHD);
        }

        // set enable multitrack to false to interop with JS-SDK
        // skylinkConfig.setEnableMultitrack(false);

        return skylinkConfig;
    }

    /**
     * Get the info of a peer in specific index
     */
    public SkylinkPeer getPeerByIndex(int index) {
        return mPeersList.get(index);
    }

    public void switchCamera() {
        final boolean[] success = {true};
        mSkylinkConnection.switchCamera(new SkylinkCallback() {
            @Override
            public void onError(SkylinkError error, String contextDescription) {
                Log.e("SkylinkCallback", contextDescription);
                success[0] = false;
            }
        });
        if (!success[0]) {
            String error = "Unable to switchCamera!";
            toastLog(TAG, context, error);
        }
    }

    public void createLocalAudio() {
        if (mSkylinkConnection == null) {
            initializeSkylinkConnection(Constants.CONFIG_TYPE.VIDEO);
        }

        //Start audio.
        if (mSkylinkConnection != null) {
            final boolean[] success = {true};
            mSkylinkConnection.createLocalMedia(SkylinkConfig.AudioDevice.MICROPHONE, "mobile's audio", new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, String contextDescription) {
                    Log.e("SkylinkCallback", contextDescription);
                    success[0] = false;
                }
            });
            if (!success[0]) {
                String error = "Unable to createLocalAudio!";
                toastLog(TAG, context, error);
            }
        }
    }

    public void createLocalVideo() {
        if (mSkylinkConnection == null) {
            initializeSkylinkConnection(Constants.CONFIG_TYPE.VIDEO);
        }

        //Start audio.
        if (mSkylinkConnection != null) {

            // Get default setting for videoDevice
            SkylinkConfig.VideoDevice videoDevice = Utils.getDefaultVideoDevice();

            // If user select back camera as default video device, start back camera
            // else start front camera as default
            if (videoDevice == CAMERA_BACK) {
                final boolean[] success = {true};
                mSkylinkConnection.createLocalMedia(CAMERA_BACK, "mobile cam back", new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                        success[0] = false;
                    }
                });
                if (!success[0]) {
                    String error = "Unable to createLocalVideo from CAMERA_BACK!";
                    toastLog(TAG, context, error);
                }
            } else {
                final boolean[] success = {true};
                mSkylinkConnection.createLocalMedia(CAMERA_FRONT, "mobile cam front", new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, String contextDescription) {
                        Log.e("SkylinkCallback", contextDescription);
                        success[0] = false;
                    }
                });
                if (!success[0]) {
                    String error = "Unable to createLocalVideo from CAMERA_FRONT!";
                    toastLog(TAG, context, error);
                }
            }
        }
    }

    public void createLocalScreen() {
        if (mSkylinkConnection == null) {
            initializeSkylinkConnection(Constants.CONFIG_TYPE.VIDEO);
        }

        //Start audio.
        if (mSkylinkConnection != null) {
            SkylinkConfig.VideoDevice videoDevice = SkylinkConfig.VideoDevice.SCREEN;
            //Start video.
            final boolean[] success = {true};
            mSkylinkConnection.createLocalMedia(videoDevice, "screen capture from mobile", new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, String contextDescription) {
                    Log.e("SkylinkCallback", contextDescription);
                    success[0] = false;
                }
            });
            if (!success[0]) {
                String error = "Unable to createLocalScreen!";
                toastLog(TAG, context, error);
            }
        }
    }

    public void createLocalCustomVideo() {
        // create a new custom video capturer to input for the method
        VideoCapturer customVideoCapturer = Utils.createCustomVideoCapturerFromCamera(
                CAMERA_FRONT, mSkylinkConnection);
        if (customVideoCapturer != null) {
            final boolean[] success = {true};
            mSkylinkConnection.createLocalMedia(CAMERA_FRONT, "external video from mobile",
                    customVideoCapturer, -1, -1, -1, new SkylinkCallback() {
                        @Override
                        public void onError(SkylinkError error, String contextDescription) {
                            Log.e("SkylinkCallback", contextDescription);
                            success[0] = false;
                        }
                    });
            if (!success[0]) {
                String error = "Unable to createLocalCustomVideo!";
                toastLog(TAG, context, error);
            }

        }
    }

    public void disposeLocalMedia() {
        clearInstance();
    }

    public String getLocalVideoId() {
        return localVideoId;
    }

    /**
     * Remove local audio object
     * Result will be informed in {@link MediaListener#onChangeLocalMedia(SkylinkMedia)}
     * with {@link SkylinkMedia.MediaState} is {@link SkylinkMedia.MediaState#UNAVAILABLE} if local audio
     * is removed successful OR {@link LifeCycleListener#onReceiveWarning(int, String)} if local audio
     * can not be removed or any error occurs
     */
    public void removeLocalAudio() {
        if (localAudioId != null) {
            final boolean[] success = {true};
            mSkylinkConnection.removeLocalMedia(localAudioId, new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, String contextDescription) {
                    Log.e("SkylinkCallback", contextDescription);
                    success[0] = false;
                }
            });
            if (!success[0]) {
                String error = "Unable to removeLocalAudio!";
                toastLog(TAG, context, error);
            }
        }
    }

    /**
     * Remove local video camera object
     * Result will be informed in {@link MediaListener#onChangeLocalMedia(SkylinkMedia)}
     * with {@link SkylinkMedia.MediaState} is {@link SkylinkMedia.MediaState#UNAVAILABLE} if local video camera
     * is removed successful OR {@link LifeCycleListener#onReceiveWarning(int, String)}  if local video camera
     * can not be removed or any error occurs
     */
    public void removeLocalVideo() {
        if (localVideoId != null) {
            final boolean[] success = {true};
            mSkylinkConnection.removeLocalMedia(localVideoId, new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, String contextDescription) {
                    Log.e("SkylinkCallback", contextDescription);
                    success[0] = false;
                }
            });
            if (!success[0]) {
                String error = "Unable to removeLocalVideo!";
                toastLog(TAG, context, error);
            }
        }
    }

    /**
     * Remove local screen object
     * Result will be informed in {@link MediaListener#onChangeLocalMedia(SkylinkMedia)}
     * with {@link SkylinkMedia.MediaState} is {@link SkylinkMedia.MediaState#UNAVAILABLE} if local screen
     * is removed successful OR {@link LifeCycleListener#onReceiveWarning(int, String)} if local screen
     * can not be removed or any error occurs
     */
    public void removeLocalScreen() {
        if (localScreenSharingId != null) {
            final boolean[] success = {true};

            mSkylinkConnection.removeLocalMedia(localScreenSharingId, new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, String contextDescription) {
                    Log.e("SkylinkCallback", contextDescription);
                    success[0] = false;
                }
            });
            if (!success[0]) {
                String error = "Unable to removeLocalScreen!";
                toastLog(TAG, context, error);
            }
        }
    }
}
