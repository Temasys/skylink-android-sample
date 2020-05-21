package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkError;
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoContract;
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionContract;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkConfig.VideoDevice.CAMERA_BACK;
import static sg.com.temasys.skylink.sdk.rtc.SkylinkConfig.VideoDevice.CAMERA_FRONT;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.SCREEN_RESOLUTION_LARGE;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.SCREEN_RESOLUTION_MEDIUM;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.Config.SCREEN_RESOLUTION_SMALL;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;


/**
 * Created by muoi.pham on 20/07/18.
 * The service class is responsible for communicating with the SkylinkSDK API by using SkylinkConnection instance
 */

public class VideoService extends SkylinkCommonService implements VideoContract.Service {

    private static final String TAG = VideoService.class.getCanonicalName();

    private final int MAX_REMOTE_PEER = 1;

    public VideoService(Context context) {
        super(context);
        initializeSkylinkConnection(Constants.CONFIG_TYPE.VIDEO);
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
     * Create local video camera if it has not been started
     * if the local video camera is started:
     * - Stop or restart the local camera based on the parameter |toActive|,
     * given that the local video source is available, i.e., had been started and not removed.
     * However, if the intended state of the camera (active or stopped) is already the current
     * state, then no change will be effected.
     * Trigger callback SkylinkCallback.onError if an error occurs, for example:
     * if local video source is not available.
     */
    public void toggleVideo(boolean toActive) {
        if (skylinkConnection != null && localVideo != null) {
            if (toActive) {
                skylinkConnection.changeLocalMediaState(localVideo.getMediaId(), SkylinkMedia.MediaState.ACTIVE, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to active local video as " + contextDescription);
                    }
                });
            } else {
                skylinkConnection.changeLocalMediaState(localVideo.getMediaId(), SkylinkMedia.MediaState.STOPPED, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to stop local video as " + contextDescription);
                    }
                });
            }
        } else if (toActive) {
            createLocalVideo();
        }
    }

    /**
     * Create local video screen sharing if it has not been started
     * if the local video screen is started:
     * - Stop or restart the local screen based on the parameter |toActive|,
     * given that the local screen source is available, i.e., had been started and not removed.
     * However, if the intended state of the screen (active or stopped) is already the current
     * state, then no change will be effected.
     * Trigger callback SkylinkCallback.onError if an error occurs, for example:
     * if local screen source is not available.
     */
    public void toggleScreen(boolean toActive) {
        if (skylinkConnection != null && localScreen != null) {
            if (toActive) {
                skylinkConnection.changeLocalMediaState(localScreen.getMediaId(), SkylinkMedia.MediaState.ACTIVE, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to active screen as " + contextDescription);
                    }
                });
            } else {
                skylinkConnection.changeLocalMediaState(localScreen.getMediaId(), SkylinkMedia.MediaState.STOPPED, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to stop screen as " + contextDescription);
                    }
                });
            }
        } else
            createLocalScreen();
    }

    /**
     * Mutes the local user's audio and notifies all the peers in the room.
     *
     * @param toMuted Flag that specifies whether audio should be mute
     */
    public void muteLocalAudio(boolean toMuted) {
        if (skylinkConnection != null) {
            if (toMuted) {
                skylinkConnection.changeLocalMediaState(localAudio.getMediaId(), SkylinkMedia.MediaState.MUTED, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to mute local audio as " + contextDescription);
                    }
                });
            } else {
                skylinkConnection.changeLocalMediaState(localAudio.getMediaId(), SkylinkMedia.MediaState.ACTIVE, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to active local audio as " + contextDescription);
                    }
                });
            }
        }
    }

    /**
     * Mutes the local user's video and notifies all the peers in the room.
     * Note that black video frames (consuming bandwidth) are still being sent to remote Peer(s).
     *
     * @param toMuted Flag that specifies whether video should be mute
     */
    public void muteLocalVideo(boolean toMuted) {
        if (skylinkConnection != null) {
            if (toMuted) {
                skylinkConnection.changeLocalMediaState(localVideo.getMediaId(), SkylinkMedia.MediaState.MUTED, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to mute local video as " + contextDescription);
                    }
                });
            } else {
                skylinkConnection.changeLocalMediaState(localVideo.getMediaId(), SkylinkMedia.MediaState.ACTIVE, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to active local video as " + contextDescription);
                    }
                });
            }
        }
    }

    /**
     * Mutes the local user's screen video and notifies all the peers in the room.
     * Note that black video frames (consuming bandwidth) are still being sent to remote Peer(s).
     *
     * @param toMuted Flag that specifies whether screen video should be mute
     */
    public void muteLocalScreen(boolean toMuted) {
        if (skylinkConnection != null) {
            if (toMuted) {
                skylinkConnection.changeLocalMediaState(localScreen.getMediaId(), SkylinkMedia.MediaState.MUTED, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to mute local screen as " + contextDescription);
                    }
                });
            } else {
                skylinkConnection.changeLocalMediaState(localScreen.getMediaId(), SkylinkMedia.MediaState.ACTIVE, new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to active local screen as " + contextDescription);
                    }
                });
            }
        }
    }

    /**
     * Return the specific video view of the SkylinkMedia that mediaId is provided
     *
     * @return Video View of Peer or null if none is matched with the input id.
     */
    public SurfaceViewRenderer getVideoView(String mediaId) {
        if (skylinkConnection != null) {
            SkylinkMedia media = skylinkConnection.getSkylinkMedia(mediaId);
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

        if (skylinkConnection != null) {
            mediaObjects = skylinkConnection.getSkylinkMediaList(mediaType, peerId);
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
     * The speaker is automatically turned off when audio bluetooth or headset is connected.
     */
    public void changeSpeakerOutput(boolean isSpeakerOn) {
        if (isSpeakerOn) {
            AudioRouter.turnOnSpeaker();
        } else {
            AudioRouter.turnOffSpeaker();
        }
    }

    /**
     * Sets the specified listeners for video function
     * Video call needs to implement LifeCycleListener, RemotePeerListener, MediaListener, OsListener
     */
    @Override
    public void setSkylinkListeners() {
        if (skylinkConnection != null) {
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setRemotePeerListener(this);
            skylinkConnection.setMediaListener(this);
            skylinkConnection.setOsListener(this);
        }
    }

    /**
     * Get the config for video function
     * User can custom video config by using SkylinkConfig
     */
    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();

        // Set some common configs base on the default setting on the setting page
        Utils.skylinkConfigCommonOptions(skylinkConfig);

        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);

        skylinkConfig.setSkylinkRoomSize(SkylinkConfig.SkylinkRoomSize.EXTRA_SMALL);

        skylinkConfig.setMirrorLocalFrontCameraView(true);
        skylinkConfig.setReportVideoResolutionUntilStable(true);
        skylinkConfig.setReportVideoResolutionOnVideoChange(true);

        // just 1 to 1 video call
        skylinkConfig.setMaxRemotePeersConnected(1, SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);

        // set unsupportedHWAEC list to the skylinkConfig
        AudioRouter.unsupportedHWAECList.add("Mi A2");
        AudioRouter.unsupportedHWAECList.add("TA-1196");
        AudioRouter.unsupportedHWAECList.add("TA-1119");

        skylinkConfig.setUnsupportedAECModels(AudioRouter.unsupportedHWAECList);

        return skylinkConfig;
    }

    /**
     * Get the info of a peer in specific index
     */
    public SkylinkPeer getPeerByIndex(int index) {
        return mPeersList.get(index);
    }

    public void switchCamera() {
        skylinkConnection.switchCamera(new SkylinkCallback() {
            @Override
            public void onError(SkylinkError error, HashMap<String, Object> details) {
                String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                Log.e("SkylinkCallback", contextDescription);
                toastLog(TAG, context, "\"Unable to switch local camera as " + contextDescription);
            }
        });
    }

    public void createLocalAudio() {
        Log.d(TAG, "createLocalAudio()");
        //Start audio.
        if (skylinkConnection != null && localAudio == null) {
            skylinkConnection.createLocalMedia(SkylinkConfig.AudioDevice.MICROPHONE, "mobile's audio", new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to createLocalAudio as " + contextDescription);
                }
            });
        }
    }

    public void createLocalVideo() {
        Log.d(TAG, "createLocalVideo()");
        if (skylinkConnection != null && localVideo == null) {

            // Get default setting for videoDevice
            SkylinkConfig.VideoDevice videoDevice = Utils.getDefaultVideoDevice();

            // If user select back camera as default video device, start back camera
            // else start front camera as default
            if (videoDevice == CAMERA_BACK) {
                skylinkConnection.createLocalMedia(CAMERA_BACK, "mobile cam back", new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to createLocalVideo as " + contextDescription);
                    }
                });
            } else {
                skylinkConnection.createLocalMedia(CAMERA_FRONT, "mobile cam front", new SkylinkCallback() {
                    @Override
                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                        Log.e("SkylinkCallback", contextDescription);
                        toastLog(TAG, context, "\"Unable to createLocalVideo as " + contextDescription);
                    }
                });
            }
        }
    }

    public void createLocalScreen() {
        Log.d(TAG, "createLocalScreen()");

        //get default video resolution (widthxheight) from setting to create local screen with preferred resolution (optional)
        int width = 800, height = 1600;
        String screenResolution = Utils.getDefaultScreenResolution();
        int screenOrientation = context.getResources().getConfiguration().orientation;

        if (screenResolution.equals(SCREEN_RESOLUTION_LARGE)) {
            if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                width = Config.ScreenResolution.LARGE_PORTRAIT.getWidth();
                height = Config.ScreenResolution.LARGE_PORTRAIT.getHeight();
            } else if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                width = Config.ScreenResolution.LARGE_LANDSCAPE.getWidth();
                height = Config.ScreenResolution.LARGE_LANDSCAPE.getHeight();
            }
        } else if (screenResolution.equals(SCREEN_RESOLUTION_MEDIUM)) {
            if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                width = Config.ScreenResolution.MEDIUM_PORTRAIT.getWidth();
                height = Config.ScreenResolution.MEDIUM_PORTRAIT.getHeight();
            } else if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                width = Config.ScreenResolution.MEDIUM_LANDSCAPE.getWidth();
                height = Config.ScreenResolution.MEDIUM_LANDSCAPE.getHeight();
            }
        } else if (screenResolution.equals(SCREEN_RESOLUTION_SMALL)) {
            if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                width = Config.ScreenResolution.SMALL_PORTRAIT.getWidth();
                height = Config.ScreenResolution.SMALL_PORTRAIT.getHeight();
            } else if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                width = Config.ScreenResolution.SMALL_LANDSCAPE.getWidth();
                height = Config.ScreenResolution.SMALL_LANDSCAPE.getHeight();
            }
        }

        int defaultScreenFps = 60;
        if (skylinkConnection != null && localScreen == null) {
            SkylinkConfig.VideoDevice videoDevice = SkylinkConfig.VideoDevice.SCREEN;
            //Start screen by default video resolution in setting.
            skylinkConnection.createLocalMedia(videoDevice, "screen capture from mobile",
                    width, height, defaultScreenFps, new SkylinkCallback() {
                        @Override
                        public void onError(SkylinkError error, HashMap<String, Object> details) {
                            String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                            Log.e("SkylinkCallback", contextDescription);
                            toastLog(TAG, context, "\"Unable to createLocalScreen as " + contextDescription);
                        }
                    });
        }
    }

    public void createLocalCustomVideo() {
        Log.d(TAG, "createLocalCustomVideo()");
        // create a new custom video capturer to input for the method
        VideoCapturer customVideoCapturer = Utils.createCustomVideoCapturerFromCamera(
                CAMERA_FRONT, skylinkConnection);
        if (customVideoCapturer != null) {
            skylinkConnection.createLocalMedia(CAMERA_FRONT, "external video from mobile",
                    customVideoCapturer, -1, -1, -1, new SkylinkCallback() {
                        @Override
                        public void onError(SkylinkError error, HashMap<String, Object> details) {
                            String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                            Log.e("SkylinkCallback", contextDescription);
                            toastLog(TAG, context, "\"Unable to createLocalCustomVideo as " + contextDescription);
                        }
                    });

        }
    }

    public SkylinkMedia getLocalAudio() {
        return localAudio;
    }

    public SkylinkMedia getLocalVideo() {
        return localVideo;
    }

    public SkylinkMedia getLocalScreen() {
        return localScreen;
    }

    public void disposeLocalMedia() {
        clearInstance();
    }

    /**
     * Remove local audio object
     * Result will be informed in {@link MediaListener#onChangeLocalMedia(SkylinkMedia)}
     * with {@link SkylinkMedia.MediaState} is {@link SkylinkMedia.MediaState#UNAVAILABLE} if local audio
     * is removed successful OR {@link LifeCycleListener#onReceiveWarning(SkylinkError, HashMap)} if local audio
     * can not be removed or any error occurs
     */
    public void destroyLocalAudio() {
        if (localAudio != null) {
            skylinkConnection.destroyLocalMedia(localAudio.getMediaId(), new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to destroyLocalAudio as " + contextDescription);
                }
            });
        }
    }

    /**
     * Remove local video camera object
     * Result will be informed in {@link MediaListener#onChangeLocalMedia(SkylinkMedia)}
     * with {@link SkylinkMedia.MediaState} is {@link SkylinkMedia.MediaState#UNAVAILABLE} if local video camera
     * is removed successful OR {@link LifeCycleListener#onReceiveWarning(SkylinkError, HashMap)}  if local video camera
     * can not be removed or any error occurs
     */
    public void destroyLocalVideo() {
        if (localVideo != null) {
            skylinkConnection.destroyLocalMedia(localVideo.getMediaId(), new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to destroyLocalVideo as " + contextDescription);
                }
            });
        }
    }

    /**
     * Remove local screen object
     * Result will be informed in {@link MediaListener#onChangeLocalMedia(SkylinkMedia)}
     * with {@link SkylinkMedia.MediaState} is {@link SkylinkMedia.MediaState#UNAVAILABLE} if local screen
     * is removed successful OR {@link LifeCycleListener#onReceiveWarning(SkylinkError, HashMap)} if local screen
     * can not be removed or any error occurs
     */
    public void destroyLocalScreen() {
        if (localScreen != null) {
            skylinkConnection.destroyLocalMedia(localScreen.getMediaId(), new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to destroyLocalScreen as " + contextDescription);
                }
            });
        }
    }
}
