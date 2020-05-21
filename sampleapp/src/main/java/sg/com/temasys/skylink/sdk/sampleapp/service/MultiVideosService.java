package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Context;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback;
import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkError;
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent;
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.multivideos.MultiVideosContract;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.utils.AudioRouter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkConfig.VideoDevice.CAMERA_FRONT;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is responsible for communicating with SkylinkSDK
 */

public class MultiVideosService extends SkylinkCommonService implements MultiVideosContract.Service {

    private final String TAG = MultiVideosService.class.getName();

    private final int MAX_REMOTE_PEER = 3;

    public MultiVideosService(Context context) {
        super(context);
        initializeSkylinkConnection(Constants.CONFIG_TYPE.MULTI_VIDEOS);
    }

    @Override
    public void setPresenter(MultiVideosContract.Presenter presenter) {
        this.presenter = (BasePresenter) presenter;
    }

    /**
     * Sets the specified listeners for multi videos function
     * Video call needs to implement LifeCycleListener, RemotePeerListener, MediaListener, OsListener,
     * RecordingListener, StatsListener
     */
    @Override
    public void setSkylinkListeners() {
        if (skylinkConnection != null) {
            // LifeCycleListener for connect, disconnect,.. with room
            skylinkConnection.setLifeCycleListener(this);

            //RemotePeerListener for communicate with remote peer(s)
            skylinkConnection.setRemotePeerListener(this);

            // MediaListener for media using like audio, video,..
            skylinkConnection.setMediaListener(this);

            // OsListener for permission of media
            skylinkConnection.setOsListener(this);

            // RecordingListener for recording audio, video
            skylinkConnection.setRecordingListener(this);
        }
    }

    /**
     * Get the config for multi videos function
     * User can custom video config by using SkylinkConfig
     */
    @Override
    public SkylinkConfig getSkylinkConfig() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();

        // Set some common configs base on the default setting on the setting page
        Utils.skylinkConfigCommonOptions(skylinkConfig);

        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);

        skylinkConfig.setSkylinkRoomSize(SkylinkConfig.SkylinkRoomSize.SMALL);

        skylinkConfig.setMirrorLocalFrontCameraView(true);

        // just allow 3 remote peers join the room as the UI supported maximum 3 remote peers
        skylinkConfig.setMaxRemotePeersConnected(MAX_REMOTE_PEER, SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);

        // set unsupportedHWAEC list to the skylinkConfig
        AudioRouter.unsupportedHWAECList.add("Mi A2");
        AudioRouter.unsupportedHWAECList.add("TA-1196");
        AudioRouter.unsupportedHWAECList.add("TA-1119");

        skylinkConfig.setUnsupportedAECModels(AudioRouter.unsupportedHWAECList);

        return skylinkConfig;
    }

    /**
     * Call this method to switch between available camera.
     * On successful operation, camera switched to will be delivered via callback at
     * {@link LifeCycleListener#onReceiveInfo(sg.com.temasys.skylink.sdk.rtc.SkylinkInfo, HashMap)}
     * with possible {@link sg.com.temasys.skylink.sdk.rtc.SkylinkInfo}:
     * -- {@link sg.com.temasys.skylink.sdk.rtc.SkylinkInfo#CAM_OPEN_FRONT}.
     * -- {@link sg.com.temasys.skylink.sdk.rtc.SkylinkInfo#CAM_OPEN_NON_FRONT}.
     */
    public void switchCamera() {
        if (skylinkConnection != null) {
            skylinkConnection.switchCamera(new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to switchCamera as " + contextDescription);
                }
            });
        }
    }

    public void createLocalAudio() {
        //Start audio.
        if (skylinkConnection != null) {
            skylinkConnection.createLocalMedia(SkylinkConfig.AudioDevice.MICROPHONE, null, null);
        }
    }

    public void createLocalVideo() {
        // start custom camera if default video device setting is custom device
        if (Utils.isDefaultCustomVideoDeviceSetting()) {
            createLocalCustomVideo();
            return;
        }

        //Start audio.
        if (skylinkConnection != null) {

            // Get default setting for videoDevice
            SkylinkConfig.VideoDevice videoDevice = Utils.getDefaultVideoDevice();

            // If user select back camera as default video device, start back camera
            // else start front camera as default
            if (videoDevice == SkylinkConfig.VideoDevice.CAMERA_BACK) {
                skylinkConnection.createLocalMedia(SkylinkConfig.VideoDevice.CAMERA_BACK, null, null);
            } else {
                skylinkConnection.createLocalMedia(SkylinkConfig.VideoDevice.CAMERA_FRONT, null, null);
            }
        }
    }

    public void toggleVideo(boolean isRestart) {
        if (skylinkConnection != null && localVideo != null) {
            if (isRestart) {
                skylinkConnection.changeLocalMediaState(localVideo.getMediaId(), SkylinkMedia.MediaState.ACTIVE, null);
            } else {
                skylinkConnection.changeLocalMediaState(localVideo.getMediaId(), SkylinkMedia.MediaState.STOPPED, null);
            }
        }
    }

    public void createLocalScreen() {
        if (skylinkConnection != null) {

            SkylinkConfig.VideoDevice videoDevice = SkylinkConfig.VideoDevice.SCREEN;
            //Start video.
            skylinkConnection.createLocalMedia(videoDevice, null, null);
        }
    }

    public void createLocalCustomVideo() {
        // create a new custom video capturer to input for the method
        VideoCapturer customVideoCapturer = Utils.createCustomVideoCapturerFromCamera(
                CAMERA_FRONT, skylinkConnection);
        if (customVideoCapturer != null) {
            skylinkConnection.createLocalMedia(CAMERA_FRONT, null, customVideoCapturer, -1, -1, -1, null);
        }
    }

    public void toggleScreen(boolean restart) {
        if (skylinkConnection != null && localScreen != null)
            if (restart) {
                skylinkConnection.changeLocalMediaState(localScreen.getMediaId(), SkylinkMedia.MediaState.ACTIVE, null);
            } else {
                skylinkConnection.changeLocalMediaState(localScreen.getMediaId(), SkylinkMedia.MediaState.STOPPED, null);
            }
        else
            createLocalScreen();
    }

    /**
     * Start recording with conditions:
     * - We must be using Skylink Media Relay (SMR key)
     * - Recording should not be already started.
     * - We should not have just tried to start recording.
     * Actual start of recording will be notified via relevant callback on
     * {@link BasePresenter#processRecordingStarted(Context, boolean)}
     */
    public void startRecording() {
        if (skylinkConnection != null) {
            skylinkConnection.startRecording(new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to startRecording as " + contextDescription);
                }
            });
        }
    }

    /**
     * Stop recording with conditions:
     * - We must be already be recording.
     * - We should not have just tried to stop recording.
     * Actual stop of recording will be notified via relevant callback on
     * {@link BasePresenter#processRecordingStopped(Context, boolean)}
     */
    public void stopRecording() {
        if (skylinkConnection != null) {
            skylinkConnection.stopRecording(new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to stopRecording as " + contextDescription);
                }
            });
        }
    }

    /**
     * Get the current resolution of the input video being captured by the local camera
     * and the SkylinkCaptureFormat used.
     * If resolution is available, it will be returned in
     * {@link BasePresenter#processInputVideoResolutionObtained(int, int, int, SkylinkCaptureFormat)}.
     * Note:
     * - Resolution may not always be available, e.g. if no video is captured.
     * - This might be different from the resolution of the video actually sent to Peers as
     * SkylinkSDK may adjust resolution dynamically to try to match its bandwidth criteria.
     */
    public void getInputVideoResolution() {
        if (skylinkConnection != null && localVideo != null) {
            skylinkConnection.getInputVideoResolution(localVideo.getMediaId(), new SkylinkCallback.InputVideoResolution() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to getInputVideoResolution as " + contextDescription);
                }

                @Override
                public void onObtainInputVideoResolution(int width, int height, int fps, SkylinkCaptureFormat captureFormat) {
                    obtainInputVideoResolution(width, height, fps, captureFormat, localVideo.getMediaType());
                }
            });
        }
    }

    /**
     * Get the current resolution of the video being sent to a specific Peer.
     * If resolution is available, it will be returned in
     * {@link BasePresenter#processSentVideoResolutionObtained(String, SkylinkMedia.MediaType mediaType, int, int, int)}
     *
     * @param peerIndex Index of the remote Peer in frame from whom we want to get sent video resolution.
     *                  Use -1 to get sent video resolutions of all connected remote Peers.
     * @param mediaType Type of the SkylinkMedia video object that to get video resolution
     */
    public void getSentVideoResolution(int peerIndex, SkylinkMedia.MediaType mediaType) {
        String mediaId = getProperLocalMediaId(mediaType);

        if (skylinkConnection != null) {
            if (peerIndex != -1 && mediaId != null) {
                // get sent video res to remote peer
                String remotePeerId = mPeersList.get(peerIndex).getPeerId();
                skylinkConnection.getSentVideoResolution(remotePeerId, mediaId,
                        new SkylinkCallback.SentVideoResolution() {
                            @Override
                            public void onError(SkylinkError error, HashMap<String, Object> details) {
                                String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                                Log.e("SkylinkCallback", contextDescription);
                                toastLog(TAG, context, "\"Unable to getSentVideoResolution as " + contextDescription);
                            }

                            @Override
                            public void onObtainSentVideoResolution(int width, int height, int fps) {
                                obtainSentVideoResolution(width, height, fps, mediaType, remotePeerId);
                            }
                        });
            }
        }
    }

    /**
     * Get the current resolution of the video received from a specific Peer's index.
     * If resolution is available, it will be returned in
     * {@link BasePresenter#processReceivedVideoResolutionObtained(String, SkylinkMedia.MediaType, int, int, int)}
     *
     * @param peerIndex Index of the remote Peer in frame from whom we want to get received video resolution.
     *                  Use -1 to get received video resolutions of all connected remote Peers.
     * @param mediaType type of the SkylinkMedia video object to get received video resolution
     */
    public void getReceivedVideoResolution(int peerIndex, SkylinkMedia.MediaType mediaType) {
        // we also can get media id from remote peer id
        String remotePeerId = mPeersList.get(peerIndex).getPeerId();

        List<SkylinkMedia> remoteSkylinkMediaList = skylinkConnection.getSkylinkMediaList(mediaType, remotePeerId);

        if (remoteSkylinkMediaList == null || remoteSkylinkMediaList.size() == 0) {
            return;
        }

        // TODO @Muoi need to update when SDK finished get stats by specific media track
        // currently the SDK is just able to get full stats for receiving track, no all mediaTypes or
        // media tracks will get the same stats
        SkylinkMedia remoteMedia = remoteSkylinkMediaList.get(0);

        if (remoteMedia != null && remoteMedia.getMediaState() != SkylinkMedia.MediaState.UNAVAILABLE) {
            String mediaId = remoteMedia.getMediaId();
            skylinkConnection.getReceivedVideoResolution(mediaId, new SkylinkCallback.ReceivedVideoResolution() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to getReceivedVideoResolution as " + contextDescription);
                }

                @Override
                public void onObtainReceivedVideoResolution(int width, int height, int fps) {
                    obtainReceivedVideoResolution(width, height, fps, remoteMedia.getMediaType(), remotePeerId);
                }
            });
        }
    }

    /**
     * Request for full WebRTC statistics of the specified remote peer by peer index
     * Results will be reported via
     * {@link BasePresenter#processWebrtcStatsReceived(HashMap)}
     *
     * @param peerIndex Index of the remote Peer in frame for which we are getting transfer speed on.
     */
    public void getWebrtcStats(int peerIndex) {
        String peerId = mPeersList.get(peerIndex).getPeerId();

        if (peerId == null)
            return;

        // get sending stats from local media to remote peer
        Map<String, SkylinkMedia> localMediaMap = mPeersList.get(0).getMediaMap();
        if (localMediaMap != null && localMediaMap.size() > 0) {
            for (String mediaId : localMediaMap.keySet()) {
                skylinkConnection.getSentWebRtcStats(mediaId, peerId,
                        new SkylinkCallback.WebRtcStats() {
                            @Override
                            public void onError(SkylinkError error, HashMap<String, Object> details) {
                                String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                                Log.e("SkylinkCallback", contextDescription);
                                toastLog(TAG, context, "\"Unable to getSentWebRtcStats as " + contextDescription);
                            }

                            @Override
                            public void onReceiveWebRtcStats(HashMap<String, String> stats) {
                                presenter.processWebrtcStatsReceived(stats);
                            }
                        });
            }
        }

        // get receiving web rtc stats from remote media of remote peer
        Map<String, SkylinkMedia> mediaMap = mPeersList.get(peerIndex).getMediaMap();
        if (mediaMap == null || mediaMap.size() == 0)
            return;

        for (String mediaId : mediaMap.keySet()) {
            skylinkConnection.getReceivedWebRtcStats(mediaId,
                    new SkylinkCallback.WebRtcStats() {
                        @Override
                        public void onError(SkylinkError error, HashMap<String, Object> details) {
                            String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                            Log.e("SkylinkCallback", contextDescription);
                            toastLog(TAG, context, "\"Unable to getReceivedWebRtcStats as " + contextDescription);
                        }

                        @Override
                        public void onReceiveWebRtcStats(HashMap<String, String> stats) {
                            presenter.processWebrtcStatsReceived(stats);
                        }
                    });
        }
    }

    /**
     * Request for the instantaneous transfer speed(s) of media stream(s), at the moment of request.
     * Results will be reported via
     * {@link BasePresenter#processTransferSpeedReceived(double, String, boolean, Context)}
     *
     * @param peerIndex  Index of the remote Peer in frame for which we are getting transfer speed on.
     * @param mediaType  type of the media object to get resolution
     * @param forSending The flag to distinguish getting from sending/uploading or from receiving/downloading
     */
    public void getTransferSpeeds(int peerIndex, SkylinkMedia.MediaType mediaType, boolean forSending) {
        String peerId = mPeersList.get(peerIndex).getPeerId();
        String peerName = mPeersList.get(peerIndex).getPeerName() + "(" + peerId + ")";

        if (peerId == null)
            return;

        // get sending stats from local media to remote peer
        if (forSending) {
            Map<String, SkylinkMedia> localMediaMap = mPeersList.get(0).getMediaMap();
            if (localMediaMap != null && localMediaMap.size() > 0) {
                for (String mediaId : localMediaMap.keySet()) {
                    if (localMediaMap.get(mediaId).getMediaType() == mediaType) {
                        skylinkConnection.getSentTransferSpeed(mediaId, peerId,
                                new SkylinkCallback.TransferSpeed() {
                                    @Override
                                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                                        Log.e("SkylinkCallback", contextDescription);
                                        toastLog(TAG, context, "\"Unable to getSentTransferSpeed for sending as " + contextDescription);
                                    }

                                    @Override
                                    public void onReceiveTransferSpeed(double transferSpeed) {
                                        presenter.processTransferSpeedReceived(transferSpeed, peerName, true, context);
                                    }
                                });
                    }
                }
            }
        } else {
            Map<String, SkylinkMedia> mediaMap = mPeersList.get(peerIndex).getMediaMap();
            if (mediaMap == null || mediaMap.size() == 0)
                return;
            if (mediaMap != null && mediaMap.size() > 0) {
                for (String mediaId : mediaMap.keySet()) {
                    if (mediaMap.get(mediaId).getMediaType() == mediaType) {
                        skylinkConnection.getReceivedTransferSpeed(mediaId,
                                new SkylinkCallback.TransferSpeed() {
                                    @Override
                                    public void onError(SkylinkError error, HashMap<String, Object> details) {
                                        String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                                        Log.e("SkylinkCallback", contextDescription);
                                        toastLog(TAG, context, "\"Unable to getReceivedTransferSpeed for receiving as " + contextDescription);
                                    }

                                    @Override
                                    public void onReceiveTransferSpeed(double transferSpeed) {
                                        presenter.processTransferSpeedReceived(transferSpeed, peerName, false, context);
                                    }
                                });
                    }
                }
            }
        }
    }

    /**
     * Refreshes a connection with a specific peer or all peer(s) connections
     * Able to indicate preference for ICE restart.
     *
     * @param peerIndex  Index of the remote peer in frame to whom we will refresh connection. Use -1 if
     *                   refresh is to be done with all our remote peers in the room.
     * @param iceRestart Specify if ICE restart should be performed. ICE restart is recommended
     *                   if network conditions had changed, for e.g. a different network is used.
     */
    public void refreshConnection(int peerIndex, boolean iceRestart) {

        if (skylinkConnection == null)
            return;

        String peerStr = "All peers ";

        //list of peers that are failed for refreshing
        String[] failedPeers = new String[MAX_REMOTE_PEER];

        if (peerIndex == -1) {
            skylinkConnection.refreshConnection(null, iceRestart, new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    failedPeers[0] = (String) details.get(SkylinkEvent.REMOTE_PEER_ID);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to refreshConnection with remote peer " + failedPeers[0] + " as " + contextDescription);
                }
            });

        } else {
            SkylinkPeer peer = mPeersList.get(peerIndex);

            skylinkConnection.refreshConnection(peer.getPeerId(), iceRestart, new SkylinkCallback() {
                @Override
                public void onError(SkylinkError error, HashMap<String, Object> details) {
                    String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                    failedPeers[0] = (String) details.get(SkylinkEvent.REMOTE_PEER_ID);
                    Log.e("SkylinkCallback", contextDescription);
                    toastLog(TAG, context, "\"Unable to refreshConnection with remote peer " + failedPeers[0] + " as " + contextDescription);
                }
            });

            peerStr = "Peer " + peer.getPeerName();
        }

        String log = "Refreshing connection for " + peerStr;
        if (iceRestart) {
            log += " with ICE restart.";
        } else {
            log += ".";
        }
        toastLog(TAG, context, log);
    }

    /**
     * Return the list of video view of Peer whose PeerId was provided.
     * If peerId is null, local video view list will be returned.
     *
     * @param mediaId id of the Media which videoView belongs to.
     * @return video view of the SkylinkMedia object
     */
    public SurfaceViewRenderer getVideoViewById(String mediaId) {
        if (skylinkConnection == null || mediaId == null) {
            return null;
        }

        SkylinkMedia localMedia = skylinkConnection.getSkylinkMedia(mediaId);

        if (localMedia != null) {
            return localMedia.getVideoView();
        }

        return null;
    }

    /**
     * Return list of the video views of Peer whose peerIndex was provided.
     * If peerIndex is -1, local video views will be returned.
     * Return null if:
     * - peerIndex is not in peerList.
     * - No video view exists for given PeerIndex.
     *
     * @param peerIndex index of the Peer whose videoView to be returned.
     * @return List of Video View of Peer or null if none present.
     */
    public List<SurfaceViewRenderer> getVideoViewByIndex(int peerIndex) {
        List<SkylinkMedia> mediaList = null;
        List<SurfaceViewRenderer> videoViews = null;

        // for local video view
        if (peerIndex == -1) {
            mediaList = skylinkConnection.getSkylinkMediaList(SkylinkMedia.MediaType.VIDEO, null);
        } else {
            if (peerIndex < mPeersList.size()) {
                SkylinkPeer skylinkPeer = mPeersList.get(peerIndex);
                mediaList = skylinkConnection.getSkylinkMediaList(SkylinkMedia.MediaType.VIDEO, skylinkPeer.getPeerId());
            }
        }

        if (mediaList == null || mediaList.size() == 0) {
            return null;
        }

        videoViews = new ArrayList<SurfaceViewRenderer>();

        for (SkylinkMedia media : mediaList) {
            SurfaceViewRenderer videoView = media.getVideoView();
            if (videoView != null) {
                videoViews.add(videoView);
            }
        }

        return videoViews;
    }

    /**
     * Get total number of peers in room including local peer
     *
     * @return number of peer(s) or 0 if nothing in room.
     */
    public int getTotalInRoom() {
        if (mPeersList != null)
            return mPeersList.size();

        return 0;
    }

    /**
     * Get list of remote peer id in room using SkylinkConnection API.
     *
     * @return list of peerId or null if not available.
     */
    public String[] getPeerIdList() {
        if (skylinkConnection != null) {
            return skylinkConnection.getPeerIdList();
        }

        return null;
    }

    /**
     * Get index of the peer whose peerId is provided
     * Return -1 if:
     * - No peer exists for given id.
     *
     * @return index in frame of the peer
     */
    public int getPeerIndexByPeerId(String peerId) {
        for (int i = 0; i < mPeersList.size(); i++) {
            SkylinkPeer peer = mPeersList.get(i);

            if (peer.getPeerId().equals(peerId)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Get id of the peer in specific index
     * Return null if:
     * - peerIndex is not in peerList.
     * - No peer exists for given index.
     *
     * @return id of peer
     */
    public String getPeerIdByIndex(int peerIndex) {
        if (skylinkConnection != null && peerIndex < mPeersList.size()) {
            return mPeersList.get(peerIndex).getPeerId();
        }

        return null;
    }

    /**
     * Get the info of a peer in specific index
     */
    public SkylinkPeer getPeerByIndex(int index) {
        return mPeersList.get(index);
    }

    public void disposeLocalMedia() {
        clearInstance();
    }
}
