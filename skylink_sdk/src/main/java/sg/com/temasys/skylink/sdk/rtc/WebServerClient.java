/*
 * libjingle
 * Copyright 2013, Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Negotiates signaling for chatting with apprtc.appspot.com "rooms". Uses the client<->server
 * specifics of the apprtc AppEngine webapp.
 * <p/>
 * To use: create an instance of this object (registering a message handler) and call
 * connectToRoom().  Once that's done call sendMessage() and wait for the registered handler to be
 * called with received messages.
 */
class WebServerClient implements RoomParameterServiceListener {

    private static final String TAG = WebServerClient.class.getName();
    private final IceServersObserver iceServersObserver;


    // These members are only read/written under sendQueue's lock.
    private LinkedList<String> sendQueue = new LinkedList<String>();
    private AppRTCSignalingParameters appRTCSignalingParameters;

    private SignalingMessageProcessingService signalingMessageProcessingService;

    /**
     * Callback fired once the room's signaling parameters specify the set of ICE servers to use.
     */
    public static interface IceServersObserver {
        public void onIceServers(List<PeerConnection.IceServer> iceServers);

        public void onError(String message);

        public void onShouldConnectToRoom();
    }

    public WebServerClient(IceServersObserver iceServersObserver,
                           SignalingMessageProcessingService signalingMessageProcessingService) {
        this.signalingMessageProcessingService = signalingMessageProcessingService;
        this.iceServersObserver = iceServersObserver;
    }

    /**
     * Asynchronously connect to an AppRTC room URL, e.g. https://apprtc.appspot.com/?r=NNN and
     * register message-handling callbacks on its GAE Channel.
     *
     * @throws IOException
     * @throws JSONException
     * @throws Exception
     */
    public void connectToRoom(String url) throws IOException, JSONException {
        (new RoomParameterService(this)).execute(url);
    }

    /**
     * Disconnect from the Signaling Channel.
     */
    public void disconnect() {
        signalingMessageProcessingService.disconnect();
    }

    @Override
    public void onRoomParameterSuccessful(AppRTCSignalingParameters params) {
        appRTCSignalingParameters = params;
        if (params != null) {
            Log.d(TAG, "onRoomParameterSuccessful ipSigserver" + params.getIpSigserver());
            Log.d(TAG, "onRoomParameterSuccessful portSigserver" + params.getPortSigserver());

            signalingMessageProcessingService.connect(params.getIpSigserver(),
                    params.getPortSigserver(), params.getSid(), params.getRoomId());
        }
    }

    @Override
    public void onRoomParameterError(String message) {
        WebServerClient.this.iceServersObserver.onError(message);
    }

    @Override
    public void onShouldConnectToRoom() {
        WebServerClient.this.iceServersObserver.onShouldConnectToRoom();
    }

    public void sendMessage(JSONObject dictMessage) {
        Log.d(TAG, "Send message");
        signalingMessageProcessingService.sendMessage(dictMessage);
    }

    public boolean isInitiator() {
        return appRTCSignalingParameters.isInitiator();
    }

    public MediaConstraints pcConstraints() {
        return appRTCSignalingParameters.getPcConstraints();
    }

    public MediaConstraints videoConstraints() {
        return appRTCSignalingParameters.getVideoConstraints();
    }

    public MediaConstraints audioConstraints() {
        return appRTCSignalingParameters.getAudioConstraints();
    }

    public String getAppOwner() {
        return appRTCSignalingParameters.getAppOwner();
    }

    public void setAppOwner(String appOwner) {
        this.appRTCSignalingParameters.setAppOwner(appOwner);
    }

    public String getCid() {
        return appRTCSignalingParameters.getCid();
    }

    public void setCid(String cid) {
        this.appRTCSignalingParameters.setCid(cid);
    }

    public String getDisplayName() {
        return appRTCSignalingParameters.getDisplayName();
    }

    public void setDisplayName(String displayName) {
        this.appRTCSignalingParameters.setDisplayName(displayName);
    }

    public String getLen() {
        return appRTCSignalingParameters.getLen();
    }

    public void setLen(String len) {
        this.appRTCSignalingParameters.setLen(len);
    }

    public String getRoomCred() {
        return appRTCSignalingParameters.getRoomCred();
    }

    public void setRoomCred(String roomCred) {
        this.appRTCSignalingParameters.setRoomCred(roomCred);
    }

    public String getRoomId() {
        return appRTCSignalingParameters.getRoomId();
    }

    public void setRoomId(String roomId) {
        this.appRTCSignalingParameters.setRoomId(roomId);
    }

    public String getSid() {
        return appRTCSignalingParameters.getSid();
    }

    public void setSid(String sid) {
        this.appRTCSignalingParameters.setSid(sid);
    }

    public String getStart() {
        return appRTCSignalingParameters.getStart();
    }

    public void setStart(String start) {
        this.appRTCSignalingParameters.setStart(start);
    }

    public String getTimeStamp() {
        return appRTCSignalingParameters.getTimeStamp();
    }

    public void setTimeStamp(String timeStamp) {
        this.appRTCSignalingParameters.setTimeStamp(timeStamp);
    }

    public String getTokenTempCreated() {
        return appRTCSignalingParameters.getTokenTempCreated();
    }

    public void setTokenTempCreated(String tokenTempCreated) {
        this.appRTCSignalingParameters.setTokenTempCreated(tokenTempCreated);
    }

    public String getUserCred() {
        return appRTCSignalingParameters.getUserCred();
    }

    public void setUserCred(String userCred) {
        this.appRTCSignalingParameters.setUserCred(userCred);
    }

    public String getUserId() {
        return appRTCSignalingParameters.getUserId();
    }

    public void setUserId(String userId) {
        this.appRTCSignalingParameters.setUserId(userId);
    }

}