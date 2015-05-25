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
    private final SkylinkConnectionService skylinkConnectionService;
    private final IceServersObserver iceServersObserver;


    // These members are only read/written under sendQueue's lock.
    private LinkedList<String> sendQueue = new LinkedList<String>();


    private SignalingMessageProcessingService signalingMessageProcessingService;

    /**
     * Callback fired once the room's signaling parameters specify the set of ICE servers to use.
     */
    public static interface IceServersObserver {
        public void onIceServers(List<PeerConnection.IceServer> iceServers);

        public void onError(String message);

        public void onShouldConnectToRoom();
    }

    public WebServerClient(SkylinkConnectionService skylinkConnectionService, IceServersObserver iceServersObserver) {
        this.skylinkConnectionService = skylinkConnectionService;
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
        // Obtain parameters required to connect to room.
        (new RoomParameterService(this)).execute(url);
    }

    @Override
    public void onRoomParameterSuccessful(AppRTCSignalingParameters params) {
        if (params != null) {
            Log.d(TAG, "onRoomParameterSuccessful ipSigserver" + params.getIpSigserver());
            Log.d(TAG, "onRoomParameterSuccessful portSigserver" + params.getPortSigserver());
            // Inform that Room parameters have been obtained.
            skylinkConnectionService.obtainedRoomParameters(params);
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


}