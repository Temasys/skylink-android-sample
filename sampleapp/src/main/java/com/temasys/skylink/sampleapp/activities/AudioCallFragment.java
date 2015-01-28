package com.temasys.skylink.sampleapp.activities;

import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.temasys.skylink.sampleapp.R;

import org.json.JSONException;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Date;

import sg.com.temasys.skylink.sdk.config.SkyLinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.rtc.SkyLinkConnection;

/**
 * This class is used to demonstrate the AudioCall between two clients in WebRTC
 * Created by lavanyasudharsanam on 20/1/15.
 */
public class AudioCallFragment extends Fragment implements LifeCycleListener, MediaListener {
    private static final String TAG = AudioCallFragment.class.getCanonicalName();
    LinearLayout parentFragment;
    private SkyLinkConnection skyLinkConnection;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_audio_call, container, false);
        parentFragment = (LinearLayout) rootView.findViewById(R.id.ll_audio_call);

        Button btnAudioCall = (Button) rootView.findViewById(R.id.btn_audio_call);
        btnAudioCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    skyLinkConnection.connectToRoom(Constants.ROOM_NAME, Constants
                            .MY_USER_NAME, new Date(), Constants.DURATION);
                } catch (SignatureException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        skyLinkConnection = SkyLinkConnection.getInstance();
        skyLinkConnection.init(getString(R.string.app_key),
                getString(R.string.app_secret), getSkylinkConfig(),
                this.getActivity().getApplicationContext());

        Log.d(TAG, " lo " + this.getActivity());
        skyLinkConnection.setLifeCycleListener(this);
        skyLinkConnection.setMediaListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        skyLinkConnection.disconnectFromRoom();
        skyLinkConnection.setLifeCycleListener(null);
        skyLinkConnection.setMediaListener(null);
        skyLinkConnection.setRemotePeerListener(null);
    }

    private SkyLinkConfig getSkylinkConfig() {
        SkyLinkConfig config = new SkyLinkConfig();
        config.setAudioVideoSendConfig(SkyLinkConfig.AudioVideoConfig.AUDIO_ONLY);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        return config;
    }

    /***
     * Lifecycle Listener
     */

    /**
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        if (isSuccess) {
            Log.d(TAG, "Skylink Connected");
        } else {
            Log.d(TAG, "Skylink Failed");
        }
    }

    @Override
    public void onWarning(String message) {
        Log.d(TAG, message + "warning");
    }

    @Override
    public void onDisconnect(String message) {
        Log.d(TAG, message + " disconnected");
    }

    @Override
    public void onReceiveLog(String message) {
        Log.d(TAG, message + "onReceiveLog");
    }

    @Override
    public void onLocalMediaCapture(GLSurfaceView glSurfaceView, Point point) {
        Log.d(TAG, "onLocalMediaCapture");
    }

    @Override
    public void onVideoSizeChange(GLSurfaceView glSurfaceView, Point point) {
        Log.d(TAG, point.toString() + "got size");
    }

    @Override
    public void onRemotePeerAudioToggle(String s, boolean b) {
        Log.d(TAG, "onRemotePeerAudioToggle");
    }

    @Override
    public void onRemotePeerVideoToggle(String s, boolean b) {
        Log.d(TAG, "onRemotePeerVideoToggle");
    }

    @Override
    public void onRemotePeerMediaReceive(String s, GLSurfaceView glSurfaceView, Point point) {
        Log.d(TAG, "onRemotePeerVideoToggle");
    }
}
