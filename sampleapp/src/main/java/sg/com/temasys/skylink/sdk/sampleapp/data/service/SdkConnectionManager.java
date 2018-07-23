package sg.com.temasys.skylink.sdk.sampleapp.data.service;

import android.content.Context;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.sampleapp.ConfigFragment.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class SdkConnectionManager {

    private final String TAG = SdkConnectionManager.class.getName();

    private Context context;

    public static SkylinkConnection currentSkylinkConnection = null;

    public SdkConnectionManager(Context context){
        this.context = context;
    }

    //---------------------------------SkylinkConnection for AudioCall------------------------------

    private SkylinkConfig getSkylinkConfigForAudioCall() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // AudioVideo config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);
        skylinkConfig.setHasPeerMessaging(true);
        skylinkConfig.setHasFileTransfer(true);

        // Allow only 1 remote Peer to join.
        skylinkConfig.setMaxPeers(1); // Default is 4 remote Peers.

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    public SkylinkConnection initializeSkylinkConnectionForAudioCall() {
        currentSkylinkConnection = SkylinkConnection.getInstance();
        currentSkylinkConnection.init(Config.getAppKey(), getSkylinkConfigForAudioCall(),
                context.getApplicationContext());
        return currentSkylinkConnection;
    }

    //---------------------------------SkylinkConnection for Chat-----------------------------------

    private SkylinkConfig getSkylinkConfigForChat() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // Chat config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setHasPeerMessaging(true);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    public SkylinkConnection initializeSkylinkConnectionForChat() {
        currentSkylinkConnection = SkylinkConnection.getInstance();
        currentSkylinkConnection.init(Config.getAppKey(), getSkylinkConfigForChat(),
                context.getApplicationContext());
        return currentSkylinkConnection;
    }

    //---------------------------------SkylinkConnection for DataTransfer-----------------------------------

    private SkylinkConfig getSkylinkConfigForDataTransfer() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // DataTransfer config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setHasDataTransfer(true);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    public SkylinkConnection initializeSkylinkConnectionForDataTransfer() {
        currentSkylinkConnection = SkylinkConnection.getInstance();
        currentSkylinkConnection.init(Config.getAppKey(), getSkylinkConfigForDataTransfer(),
                context.getApplicationContext());
        return currentSkylinkConnection;
    }

    //---------------------------------SkylinkConnection for FileTransfer---------------------------

    private SkylinkConfig getSkylinkConfigForFileTransfer() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // FileTransfer config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO);
        skylinkConfig.setHasFileTransfer(true);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    public SkylinkConnection initializeSkylinkConnectionForFileTransfer() {
        currentSkylinkConnection = SkylinkConnection.getInstance();
        currentSkylinkConnection.init(Config.getAppKey(), getSkylinkConfigForFileTransfer(),
                context.getApplicationContext());
        return currentSkylinkConnection;
    }

    //---------------------------------SkylinkConnection for MultiPartyVideoCall--------------------

    private SkylinkConfig getSkylinkConfigForMultiPartyVideoCall() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // MultiPartyVideoCall config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setHasPeerMessaging(true);
        skylinkConfig.setHasFileTransfer(true);
        skylinkConfig.setMirrorLocalView(true);

        // Allow only 3 remote Peers to join, due to current UI design.
        skylinkConfig.setMaxPeers(3);

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    public SkylinkConnection initializeSkylinkConnectionForMultiPartyVideoCall() {
        currentSkylinkConnection = SkylinkConnection.getInstance();
        currentSkylinkConnection.init(Config.getAppKey(), getSkylinkConfigForMultiPartyVideoCall(),
                context.getApplicationContext());
        return currentSkylinkConnection;
    }

    //---------------------------------SkylinkConnection for VideoCall------------------------------

    private SkylinkConfig getSkylinkConfigForVideoCall() {
        SkylinkConfig skylinkConfig = new SkylinkConfig();
        // VideoCall config options can be:
        // NO_AUDIO_NO_VIDEO | AUDIO_ONLY | VIDEO_ONLY | AUDIO_AND_VIDEO
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        skylinkConfig.setHasPeerMessaging(true);
        skylinkConfig.setHasFileTransfer(true);
        skylinkConfig.setMirrorLocalView(true);
        skylinkConfig.setReportVideoResolutionOnChange(true);

        // Allow only 1 remote Peer to join.
        skylinkConfig.setMaxPeers(1); // Default is 4 remote Peers.

        // Set some common configs.
        Utils.skylinkConfigCommonOptions(skylinkConfig);
        return skylinkConfig;
    }

    public SkylinkConnection initializeSkylinkConnectionForVideoCall() {
        currentSkylinkConnection = SkylinkConnection.getInstance();
        currentSkylinkConnection.init(Config.getAppKey(), getSkylinkConfigForVideoCall(),
                context.getApplicationContext());
        return currentSkylinkConnection;
    }

    //---------------------------------get current SkylinkConnection--------------------------------

    //static method for the other to access currentSkylinkConnection
    public static SkylinkConnection getCurrentSkylinkConnection() {
        return currentSkylinkConnection;
    }
}
