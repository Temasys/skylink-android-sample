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

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Connects to an App server and process the response from the App server.
 */
class AppServerClient /*extends AsyncTask<String, Void, Void>*/ implements RoomParameterListener {

    private static final String TAG = AppServerClient.class.getName();
    private AppServerClientListener appServerClientListener;
    private RoomParameterProcessor roomParameterProcessor;

    public AppServerClient(AppServerClientListener appServerClientListener,
                           RoomParameterProcessor roomParameterProcessor) {
        this.appServerClientListener = appServerClientListener;
        this.roomParameterProcessor = roomParameterProcessor;
        roomParameterProcessor.setRoomParameterListener(this);
    }

    /**
     * Get room parameters from App server required to connect to room. Connection to room will
     * trigger after obtaining room parameters.
     *
     * @param url
     * @throws IOException
     * @throws JSONException
     */
    public void connectToRoom(String url) throws IOException, JSONException {
        // Obtain parameters required to connect to room.
        // (new RoomParameterService(this)).execute(url);
        (new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... urls) {
                String url = urls[0];
//                RoomParameters roomParameters = new RoomParameters();
                Log.d(TAG, "getParametersForRoomUrl " + url);

                InputStream inputStream = null;
                String result = "";
                try {
                    // Connect to App server
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

                    // and get server response
                    inputStream = httpResponse.getEntity().getContent();

                    if (inputStream != null) {
                        result = Utils.convertInputStreamToString(inputStream);

                        // Create RoomParameter with a RoomParameterProcessor
                        roomParameterProcessor.processRoomParameters(result);
                    } else {
                        String error = "Error obtaining Room Parameters: " +
                                "App Server did not return a response!";
                        appServerClientListener.onErrorAppServer(error);
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage(), e);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Log.e(TAG, e.getLocalizedMessage(), e);
                        }
                    }
                }
                // Room Parameters will be returned via Listener.
                return null;
            }
        }).execute(url);
    }

    /**
     * RoomParameterListener implementation
     */
    @Override
    public void onRoomParameterSuccessful(final RoomParameters params) {
        Utils.runOnUiThread(new Runnable() {
            public void run() {
                if (params != null) {
                    Log.d(TAG, "onRoomParameterSuccessful ipSigserver" + params.getIpSigserver());
                    Log.d(TAG, "onRoomParameterSuccessful portSigserver" + params.getPortSigserver());
                    // Inform that Room parameters have been obtained.
                    appServerClientListener.onObtainedRoomParameters(params);
                }
            }
        });
    }

    @Override
    public void onRoomParameterError(final int error) {
        Utils.runOnUiThread(new Runnable() {
            public void run() {
                appServerClientListener.onErrorAppServer(error);
            }
        });
    }

    @Override
    public void onRoomParameterError(final String error) {
        Utils.runOnUiThread(new Runnable() {
            public void run() {
                appServerClientListener.onErrorAppServer(error);
            }
        });

    }
}

interface AppServerClientListener {
    public void onErrorAppServer(int message);

    public void onErrorAppServer(String message);

    public void onObtainedRoomParameters(RoomParameters params);
}
