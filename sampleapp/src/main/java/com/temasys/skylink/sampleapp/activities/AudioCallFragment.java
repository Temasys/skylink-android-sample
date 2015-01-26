package com.temasys.skylink.sampleapp.activities;

import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.temasys.skylink.sampleapp.R;

import sg.com.temasys.skylink.sdk.config.SkyLinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;

/**
 * This class is used to demonstrate the audicall between two clients in webrtc
 * Created by lavanyasudharsanam on 20/1/15.
 */
public class AudioCallFragment extends Fragment implements LifeCycleListener, MediaListener {
    private static final String TAG = AudioCallFragment.class.getCanonicalName();
    LinearLayout parentFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_audio_call, container, false);
        parentFragment = (LinearLayout) rootView.findViewById(R.id.ll_audio_call);
        return rootView;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        SkyLinkConnection mConnection = new SkyLinkConnection(getString(R.string.app_key),
//                getString(R.string.app_secret), getSkylinkConfig(), this.getActivity());
//
//        Log.d(TAG," lo " + this.getActivity());
//        mConnection.setLifeCycleListener(this);
//        mConnection.setMediaListener(this);
//        try {
//            mConnection.connectToRoom("room","username",new Date(),200);
//        } catch (SignatureException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

    }

    private SkyLinkConfig getSkylinkConfig() {
        SkyLinkConfig config = new SkyLinkConfig();
        config.setHasAudio(true);
        config.setHasVideo(true);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(60);
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
        // TODO Auto-generated method stub
        if (isSuccess)
            Log.d(TAG, "Skylink Connected");
        else
            Log.d(TAG, "Skylink Failed");
    }

    @Override
    public void onGetUserMedia(GLSurfaceView videoView, Point size) {
        //show media on screen
        Log.d(TAG, videoView + "received view");

    }

    @Override
    public void onWarning(String message) {
        // TODO Auto-generated method stub
        Log.d(TAG, message + "warning");

    }

    @Override
    public void onDisconnect(String message) {
        // TODO Auto-generated method stub

        Log.d(TAG, message + " disconnected");
    }

    @Override
    public void onReceiveLog(String message) {
        // TODO Auto-generated method stub
        Log.d(TAG, message + " on receive log");


    }

    /**
     * Media Listeners
     */

    @Override
    public void onVideoSize(GLSurfaceView videoView, Point size) {
        // TODO Auto-generated method stub
        Log.d(TAG, videoView + " got size");
    }

    @Override
    public void onToggleAudio(String peerId, boolean isMuted) {
        // TODO Auto-generated method stub


    }

    @Override
    public void onToggleVideo(String peerId, boolean isMuted) {
        // TODO Auto-generated method stub

    }


}
