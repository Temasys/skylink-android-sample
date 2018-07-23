package sg.com.temasys.skylink.sdk.sampleapp.data.service;

import org.webrtc.SurfaceViewRenderer;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.SkylinkException;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;

/**
 * Created by muoi.pham on 20/07/18.
 */

public abstract class SDKService {

    //Audio service sdk handler

    /**
     * Check if we are currently connected or at least in process of connecting, by checking current
     * {@link sg.com.temasys.skylink.sdk.rtc.SkylinkConnection.SkylinkState}.
     * If true, we should be in a valid state to {@link SkylinkConnection#disconnectFromRoom}.
     * If false, we should be in a valid state to {@link SkylinkConnection#connectToRoom}.
     *
     * @return True if we are currently connecting or connected,
     * and False if we are currently disconnecting or disconnected.
     */
    protected boolean isConnectingOrConnectedBaseServiceHandler(SkylinkConnection skylinkConnection) {
        SkylinkConnection.SkylinkState state = skylinkConnection.getSkylinkState();
        if (state == SkylinkConnection.SkylinkState.CONNECTING ||
                state == SkylinkConnection.SkylinkState.CONNECTED) {
            return true;
        }
        return false;
    }

    /**
     * Disconnect from room.
     */
    protected void disconnectFromRoomBaseServiceHandler(SkylinkConnection skylinkConnection) {
        skylinkConnection.disconnectFromRoom();
    }

    /**
     * Connect to room by config and username
     *
     * @param skylinkConnection current SkylinkConnection
     * @param skylinkConnectionString config string to connect to room
     * @param userName username for connect
     * @return true if success or false if not
     */
    protected boolean connectToRoomBaseServiceHandler(SkylinkConnection skylinkConnection, String skylinkConnectionString, String userName) {
        return skylinkConnection.connectToRoom(skylinkConnectionString, userName);
    }

    /**
     * Get current connected room name
     *
     * @param skylinkConnection current SkylinkConnection
     * @param defaultName default username for connect
     * @return room name
     */
    protected String getRoomNameBaseServiceHandler(SkylinkConnection skylinkConnection, String defaultName) {
        String roomId = "";
        if (skylinkConnection != null) {
            roomId = skylinkConnection.getRoomId();
        }
        return defaultName + " (" + roomId + ")";
    }

    /**
     * Get current connected user name
     *
     * @param skylinkConnection current SkylinkConnection
     * @param peerId remote peer id
     * @param defaultName default username for connect
     * @return user name
     */
    protected String getUserNameBaseServiceHandler(SkylinkConnection skylinkConnection, String peerId, String defaultName) {
        if (skylinkConnection == null) {
            return defaultName;
        }
        return getPeerIdNickBaseServiceHandler(skylinkConnection, peerId);
    }

    /**
     * Returns the PeerId followed by userData in brackets of a Peer.
     * This is often useful for displaying a Peer's identity in UI or logs.
     *
     * @param skylinkConnection current SkylinkConnection
     * @param peerId Use null or peerId for self Peer.
     * @return
     */
    protected String getPeerIdNickBaseServiceHandler(SkylinkConnection skylinkConnection, String peerId) {
        String peerIdShow = peerId;
        if (peerId == null) {
            if (skylinkConnection != null) {
                peerIdShow = skylinkConnection.getPeerId();
            }
            if (peerIdShow == null) {
                peerIdShow = "Self";
            }
        }
        final String peerIdNick = "\"" + peerIdShow + "\"(" + getUserDataStringBaseServiceHandler(skylinkConnection, peerId) + ")";
        return peerIdNick;
    }

    /**
     * Returns the PeerId followed by userData in brackets of a Peer.
     * This is often useful for displaying a Peer's identity in UI or logs.
     *
     * @param skylinkConnection current SkylinkConnection
     * @param peerId
     * @param userInfo
     * @return
     */
    protected String getPeerIdNickBaseServiceHandler(SkylinkConnection skylinkConnection, String peerId, UserInfo userInfo) {
        return peerId + " (" + getUserDataStringBaseServiceHandler(skylinkConnection, userInfo) + ")";
    }

    /**
     * Returns the userData of a Peer as a String.
     * If there is no userData, returns the empty string, "".
     *
     * @param skylinkConnection current SkylinkConnection
     * @param peerId The PeerId for which to search. Use null for self (local Peer).
     * @return
     */
    protected String getUserDataStringBaseServiceHandler(SkylinkConnection skylinkConnection, String peerId) {
        String userDataString = "";

        if (skylinkConnection != null) {
            Object userDataObject = skylinkConnection.getUserData(peerId);
            if (userDataObject != null) {
                userDataString = userDataObject.toString();
            }
        }
        return userDataString;
    }

    /**
     * Returns the userData of a Peer as a String.
     * If there is no userData, returns the empty string, "".
     *
     * @param skylinkConnection current SkylinkConnection
     * @param userInfo The Peer's UserInfo for which to search. Use null for self (local Peer).
     * @return
     */
    protected String getUserDataStringBaseServiceHandler(SkylinkConnection skylinkConnection, UserInfo userInfo) {
        if (userInfo == null) {
            userInfo = skylinkConnection.getUserInfo(null);
        }
        if (userInfo == null) {
            return "";
        }

        Object userDataObject = userInfo.getUserData();
        String userDataString = "";
        if (userDataObject != null) {
            userDataString = userDataObject.toString();
        }
        return userDataString;
    }

    //Chat service sdk handler

    /**
     * Sends message using the signalling server
     * Pass null for remotePeerId to send message to all users in the room
     *
     * @param skylinkConnection current SkylinkConnection
     * @param remotePeerId the target peer to send
     * @param message content
     * @return
     */
    protected void sendServerMessageBaseServiceHandler(SkylinkConnection skylinkConnection, String remotePeerId, String message) {
        skylinkConnection.sendServerMessage(remotePeerId, message);
    }

    /**
     * Sends message using a DataChannel.
     * Pass null for remotePeerId to send message to all users in the room
     *
     * @param skylinkConnection current SkylinkConnection
     * @param remotePeerId the target peer to send
     * @param message content
     * @return
     */
    protected void sendP2PMessageBaseServiceHandler(SkylinkConnection skylinkConnection, String remotePeerId, String message) throws SkylinkException {
        skylinkConnection.sendP2PMessage(remotePeerId, message);

    }

    /**
     * Get a String array of PeerIds from a ArrayList<Pair<String, String>> peerList provided.
     *
     * @param skylinkConnection current SkylinkConnection
     * @return String array of PeerIds.
     */
    protected String[] getPeerIdListBaseServiceHandler(SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null)
            return skylinkConnection.getPeerIdList();

        return null;
    }

    /**
     * Get peerId of a Peer using SkylinkConnection API.
     *
     * @param skylinkConnection current SkylinkConnection
     * @return Desired peerId or null if not available.
     */
    protected String getPeerIdBaseServiceHandler(SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            return skylinkConnection.getPeerId();
        }

        return null;
    }

    /**
     * Get UserInfo of a Peer using SkylinkConnection API.
     *
     * @param skylinkConnection current SkylinkConnection
     * @param userId id of the user
     * @return Desired UserInfo or null if not available.
     */
    protected UserInfo getUserInfoBaseServiceHandler(SkylinkConnection skylinkConnection, String userId) {
        if (skylinkConnection != null) {
            return skylinkConnection.getUserInfo(userId);
        }

        return null;

    }

    //Data service sdk handler

    /**
     * Send data to remote peers
     *
     * @param skylinkConnection current SkylinkConnection
     * @param remotePeerId from group or private
     * @param data to be sent
     * @exception SkylinkException
     * @return
     */
    protected void sendDataBaseServiceHandler(SkylinkConnection skylinkConnection, String remotePeerId, byte[] data) throws SkylinkException {
        if (skylinkConnection != null) {
            skylinkConnection.sendData(remotePeerId, data);
        }
    }

    //File service sdk handler

    /**
     * Send file to remote peers
     *
     * @param skylinkConnection current SkylinkConnection
     * @param remotePeerId from group or private
     * @param fileName
     * @param filePath
     * @exception SkylinkException
     * @return
     */
    protected void sendFileTransferPermissionRequestBaseServiceHandler(SkylinkConnection skylinkConnection, String remotePeerId,
                                                                       String fileName, String filePath) throws SkylinkException {
        if (skylinkConnection != null) {
            skylinkConnection.sendFileTransferPermissionRequest(remotePeerId, fileName, filePath);
        }

    }

    //Video service sdk handler

    /**
     * get Video View from remote peer
     *
     * @param skylinkConnection current SkylinkConnection
     * @param peerId remote peer id
     * @return SurfaceViewRenderer
     */
    protected SurfaceViewRenderer getVideoViewBaseServiceHandler(SkylinkConnection skylinkConnection, String peerId) {
        if (skylinkConnection != null) {
            return skylinkConnection.getVideoView(peerId);
        }

        return null;
    }

    /**
     * toggle camera
     *
     * @param skylinkConnection current SkylinkConnection
     * @return true if toggle camera success, false if not
     */
    protected boolean toggleCameraBaseServiceHandler(SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            return skylinkConnection.toggleCamera();
        }

        return false;
    }

    /**
     * toggle camera
     *
     * @param skylinkConnection current SkylinkConnection
     * @param isToggle option for toggle camera
     * @return true if toggle camera success, false if not
     */
    protected boolean toggleCameraBaseServiceHandler(SkylinkConnection skylinkConnection, boolean isToggle) {
        if (skylinkConnection != null) {
            return skylinkConnection.toggleCamera(isToggle);
        }

        return false;
    }

    /**
     * mute audio
     *
     * @param skylinkConnection current SkylinkConnection
     * @param audioMuted option for mute audio
     * @return
     */
    protected void muteLocalAudioBaseServiceHandler(SkylinkConnection skylinkConnection, boolean audioMuted) {
        if (skylinkConnection != null) {
            skylinkConnection.muteLocalAudio(audioMuted);
        }
    }

    /**
     * mute video
     *
     * @param skylinkConnection current SkylinkConnection
     * @param videoMuted option for mute audio
     * @return
     */
    protected void muteLocalVideoBaseServiceHandler(SkylinkConnection skylinkConnection, boolean videoMuted) {
        if (skylinkConnection != null) {
            skylinkConnection.muteLocalVideo(videoMuted);
        }
    }

    /**
     * Get capture format from current device
     *
     * @param skylinkConnection current SkylinkConnection
     * @param videoDevice current video device for capturing
     * @return SkylinkCaptureFormat[]
     */
    protected SkylinkCaptureFormat[] getCaptureFormatsBaseServiceHandler(SkylinkConnection skylinkConnection, SkylinkConfig.VideoDevice videoDevice) {
        if (skylinkConnection != null) {
            return skylinkConnection.getCaptureFormats(videoDevice);
        }

        return null;
    }

    /**
     * Get capture format from current device
     *
     * @param skylinkConnection current SkylinkConnection
     * @return SkylinkCaptureFormat[]
     */
    protected SkylinkCaptureFormat getCaptureFormatsBaseServiceHandler(SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            return skylinkConnection.getCaptureFormat();
        }

        return null;
    }

    /**
     * Get current camera name from current device
     *
     * @param skylinkConnection current SkylinkConnection
     * @return camera name
     */
    protected String getCurrentCameraNameBaseServiceHandler(SkylinkConnection skylinkConnection) {

        if (skylinkConnection != null) {
            return skylinkConnection.getCurrentCameraName();
        }

        return null;
    }

    /**
     * Get current video device
     *
     * @param skylinkConnection current SkylinkConnection
     * @return current video device
     */
    protected SkylinkConfig.VideoDevice getCurrentVideoDeviceBaseServiceHandler(SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            return skylinkConnection.getCurrentVideoDevice();
        }

        return null;

    }

    /**
     * set input video resolution from local capture
     *
     * @param skylinkConnection current SkylinkConnection
     * @param width video width
     * @param height video height
     * @param fps video fps
     * @return
     */
    protected void setInputVideoResolutionBaseServiceHandler(SkylinkConnection skylinkConnection, int width, int height, int fps) {
        if (skylinkConnection != null) {
            skylinkConnection.setInputVideoResolution(width, height, fps);
        }
    }

    /**
     * Get current room id
     *
     * @param skylinkConnection current SkylinkConnection
     * @return current room id
     */
    protected String getRoomIdBaseServiceHandler(SkylinkConnection skylinkConnection) {
        return skylinkConnection.getRoomId();
    }

    /**
     * Get current input video resolution
     *
     * @param skylinkConnection current SkylinkConnection
     * @return
     */
    protected void getInputVideoResolutionBaseServiceHandler(SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            skylinkConnection.getInputVideoResolution();
        }
    }

    /**
     * Get sent video resolution from local to remote peers
     *
     * @param skylinkConnection current SkylinkConnection
     * @param peerId of remote peer
     * @return
     */
    protected void getSentVideoResolutionBaseServiceHandler(SkylinkConnection skylinkConnection, String peerId) {
        if (skylinkConnection != null) {
            skylinkConnection.getSentVideoResolution(peerId);
        }
    }

    /**
     * Get video resolution receive from remote peer
     *
     * @param skylinkConnection current SkylinkConnection
     * @param peerId of remote peer
     * @return
     */
    protected void getReceivedVideoResolutionBaseServiceHandler(SkylinkConnection skylinkConnection, String peerId) {
        if (skylinkConnection != null) {
            skylinkConnection.getReceivedVideoResolution(peerId);
        }
    }

    /**
     * switch local camera
     *
     * @param skylinkConnection current SkylinkConnection
     * @return
     */
    protected void switchCameraBaseServiceHandler(SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            skylinkConnection.switchCamera();
        }
    }

    //Multi Party Video service sdk handler

    /**
     * refresh connection between peers
     *
     * @param skylinkConnection current SkylinkConnection
     * @param peerId of remote peer
     * @param iceRestart
     * @return String[]
     */
    protected String[] refreshConnectionBaseServiceHandler(SkylinkConnection skylinkConnection, String peerId, boolean iceRestart) {
        if (skylinkConnection != null) {
            return skylinkConnection.refreshConnection(peerId, iceRestart);
        }
        return null;
    }

    /**
     * start recording the multi video call
     *
     * @param skylinkConnection current SkylinkConnection
     * @return true if success or false if not
     */
    protected boolean startRecordingBaseServiceHandler(SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            return skylinkConnection.startRecording();
        }

        return false;
    }

    /**
     * check current recording status
     *
     * @param skylinkConnection current SkylinkConnection
     * @return true if recording or false if not
     */
    protected boolean isRecordingBaseServiceHandler(SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            return skylinkConnection.isRecording();
        }
        return false;
    }

    /**
     * stop recording the multi video call
     *
     * @param skylinkConnection current SkylinkConnection
     * @return true if success or false if not
     */
    protected boolean stopRecordingBaseServiceHandler(SkylinkConnection skylinkConnection) {
        if (skylinkConnection != null) {
            return skylinkConnection.stopRecording();
        }
        return false;
    }

    /**
     * get transfered speed in multi video call
     *
     * @param skylinkConnection current SkylinkConnection
     * @param peerId of remote peer
     * @param mediaDirectionBoth
     * @param mediaAll
     * @return true if success or false if not
     */
    protected boolean getTransferSpeedsBaseServiceHandler(SkylinkConnection skylinkConnection, String peerId, int mediaDirectionBoth, int mediaAll) {
        if (skylinkConnection != null) {
            return skylinkConnection.getTransferSpeeds(peerId, mediaDirectionBoth, mediaAll);
        }

        return false;
    }

    /**
     * get stats of webRTC from current multi video call
     *
     * @param skylinkConnection current SkylinkConnection
     * @param peerId of remote peer
     * @param mediaDirectionBoth
     * @param mediaAll
     * @return true if success or false if not
     */
    protected boolean getWebrtcStatsBaseServiceHandler(SkylinkConnection skylinkConnection, String peerId, int mediaDirectionBoth, int mediaAll) {
        if (skylinkConnection != null) {
            return skylinkConnection.getWebrtcStats(peerId, mediaDirectionBoth, mediaAll);
        }

        return false;
    }

    /**
     * get stats of webRTC from current multi video call
     *
     * @param skylinkConnection current SkylinkConnection
     * @param peerId of remote peer
     * @param filePath
     * @param isPrivate is send private or group
     * @exception SkylinkException
     * @return
     */
    protected void sendFileTransferPermissionResponseBaseServiceHandler(SkylinkConnection skylinkConnection, String peerId, String filePath, boolean isPrivate) throws SkylinkException {
        if(skylinkConnection != null){
            skylinkConnection.sendFileTransferPermissionResponse(peerId, filePath, isPrivate);
        }
    }

}
