package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;

/**
 * Simple AudioRouter that switches between the speaker phone and the headset
 */
public class AudioRouter {

    private static AudioRouter instance = null;

    private static final String TAG = AudioRouter.class.getName();
    private static BroadcastReceiver headsetBroadcastReceiver;
    private static BroadcastReceiver blueToothBroadcastReceiver;
    private static BroadcastReceiver blueToothAudioBroadcastReceiver;

    private static AudioManager audioManager;

    private static BluetoothAdapter bluetoothAdapter;

    private static BasePresenter presenter;

    private static boolean isSpeakerOn;

    private static Constants.CONFIG_TYPE callType;

    // list of device models that are unsupported hardware acoustic echo cancellation
    public static Set<String> unsupportedHWAECList = new HashSet<String>();

    private AudioRouter() {
        headsetBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {

                    int state = intent.getIntExtra("state", -1);
                    switch (state) {
                        case 0:
                            processHeadsetPlug(false);
                            break;
                        case 1:
                            processHeadsetPlug(true);
                            break;
                        default:
                            String log = "[SA][headsetBroadcastReceiver][onReceive] Headset: Error determining state!";
                            Log.d(TAG, log);
                    }
                }
            }
        };

        blueToothBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    String logTag = "[SA][headsetBroadcastReceiver][onReceive] ";
                    String log;
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                        // Bluetooth is disconnected, do speaker on
                        if (isSpeakerOn) {
                            setAudioPathOnBluetooth(true);

                            presenter.processAudioOutputChanged(true);
                        }
                        log = logTag + "Bluetooth: off";
                        Log.d(TAG, log);
                    }
                }
            }

        };

        blueToothAudioBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {

                    int currentAudioState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);

                    if (currentAudioState == BluetoothHeadset.STATE_DISCONNECTED) {
                        processHeadsetBluetooth(false);
                    } else if (currentAudioState == BluetoothHeadset.STATE_DISCONNECTING) {
                        processHeadsetBluetooth(false);
                    } else if (currentAudioState == BluetoothHeadset.STATE_CONNECTED) {
                        processHeadsetBluetooth(true);
                    } else if (currentAudioState == BluetoothHeadset.STATE_CONNECTING) {
                        processHeadsetBluetooth(true);
                    }
                }
            }
        };
    }


    /**
     * Gets an instance of the AudioRouter
     *
     * @return
     */
    public static synchronized AudioRouter getInstance() {
        if (instance == null) {
            instance = new AudioRouter();
        }
        return instance;
    }

    /**
     * Initialize the Audio router
     *
     * @param audioManager
     * @param bluetoothAdapter
     */
    public void init(AudioManager audioManager, BluetoothAdapter bluetoothAdapter) {
        this.audioManager = audioManager;
        this.bluetoothAdapter = bluetoothAdapter;
    }

    public static void setPresenter(BasePresenter presenter) {
        AudioRouter.presenter = presenter;
    }

    /**
     * Initialize AudioRouter and
     * start routing audio to headset if plugged, else to the speaker phone
     */
    public static void startAudioRouting(Context context, Constants.CONFIG_TYPE typeCall) {
        String logTag = "[SA][AR][startAudioRouting] ";
        String log = logTag + "Trying to start audio routing...";
        Log.d(TAG, log);
        if (context == null) {
            log = logTag + "Failed as provided context does not exist!";
            Log.d(TAG, log);
            return;
        }
        Context appContext = context.getApplicationContext();
        if (appContext == null) {
            log = logTag + "Failed as could not get application context from provided context!";
            Log.d(TAG, log);
            return;
        }

        log = logTag + "Initializing Audio Router...";
        Log.d(TAG, log);
        initializeAudioRouter(context);

        log = logTag + "Registering receiver...";
        Log.d(TAG, log);
        // Must use applicationContext here and not Activity context.
        appContext.registerReceiver(headsetBroadcastReceiver,
                new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        appContext.registerReceiver(blueToothBroadcastReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        appContext.registerReceiver(blueToothAudioBroadcastReceiver,
                new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED));


        log = logTag + "Setting Audio Path...";
        Log.d(TAG, log);

        //use default setting for initialize
        callType = typeCall;

        log = logTag + "Starting audio routing is complete.";
        Log.d(TAG, log);
    }

    /**
     * Stop routing audio
     */
    public static void stopAudioRouting(Context context) {
        String logTag = "[SA][AR][stopAudioRouting] ";
        String log = logTag + "Trying to stop audio routing...";
        Log.d(TAG, log);
        if (instance == null) {
            log = logTag + "Not stopping as AudioRouter does not exist.";
            Log.d(TAG, log);
            return;
        }
        if (context == null) {
            log = logTag + "Failed as provided context does not exist!";
            Log.d(TAG, log);
            return;
        }
        Context appContext = context.getApplicationContext();
        if (appContext == null) {
            log = logTag + "Failed as could not get application context from provided context!";
            Log.d(TAG, log);
            return;
        }

        try {
            //reset default audio output
            applyDefaultAudioSetting();

            isSpeakerOn = false;
            bluetoothAdapter = null;
            presenter = null;
            audioManager = null;

            // Must use applicationContext here and not Activity context.
            context.getApplicationContext().unregisterReceiver(headsetBroadcastReceiver);
            context.getApplicationContext().unregisterReceiver(blueToothBroadcastReceiver);
            context.getApplicationContext().unregisterReceiver(blueToothAudioBroadcastReceiver);
            log = logTag + "Unregister receivers.";
            // Catch potential exception:
            // java.lang.IllegalArgumentException: Receiver not registered
        } catch (java.lang.IllegalArgumentException e) {
            log = logTag + "Unable to unregister receiver due to: " + e.getMessage();
        }
        Log.d(TAG, log);

        instance = null;
        log = logTag + "Audio Router instance removed. Stop audio is complete.";
        Log.d(TAG, log);
    }

    public static void turnOnSpeaker() {
        isSpeakerOn = true;

        changeAudioOutput(true);
    }

    public static void turnOffSpeaker() {
        isSpeakerOn = false;

        changeAudioOutput(false);
    }

    /**
     * Set the audio path according to whether earphone is connected. Use ear piece if earphone is
     * connected. Use speakerphone if no earphone is connected.
     */
    private static void setAudioPathOnHeadedSet(boolean isSpeakerphoneOn) {
        String logTag = "[SA][AR][setAudioPath] ";
        String log = logTag + "Trying to set audio path...";
        Log.d(TAG, log);

        if (audioManager == null) {
            throw new RuntimeException(
                    "Attempt to set audio path before setting AudioManager");
        }

        audioManager.setSpeakerphoneOn(isSpeakerphoneOn);
        log = logTag + "Setting Speakerphone to " + isSpeakerphoneOn +
                " as audioManager.isWiredHeadsetOn() = " + audioManager.isWiredHeadsetOn();

        Log.d(TAG, log);

        log = logTag + "Setting audio path is complete.";
        Log.d(TAG, log);
    }

    private static void setAudioPathOnBluetooth(boolean isSpeakerphoneOn) {
        String logTag = "[SA][AR][setAudioPath] ";
        String log = logTag + "Trying to set audio path...";
        Log.d(TAG, log);

        audioManager.setSpeakerphoneOn(isSpeakerphoneOn);
        log = logTag + "Setting Speakerphone to " + isSpeakerphoneOn +
                " as currentAudioState = " + bluetoothAdapter.isEnabled();

        Log.d(TAG, log);

        log = logTag + "Setting audio path is complete.";
        Log.d(TAG, log);
    }

    private static void initializeAudioRouter(Context context) {
        String logTag = "[SA][AR][initializeAudioRouter] ";
        String log = logTag + "Trying to initialize Audio Router...";
        Log.d(TAG, log);

        if (instance != null) {
            log = logTag + "Not initializing as AudioRouter already exist!";
            Log.d(TAG, log);
            return;
        }

        getInstance();

        AudioManager audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setParameters("noise_suppression=on");

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        instance.init(audioManager, bluetoothAdapter);
        log = logTag + "Initializing audio router is complete.";
        Log.d(TAG, log);
    }

    private static void changeAudioOutput(boolean isSpeakerphoneOn) {
        // check the audioManager object
        // incase of user not connected to room, but user changes the speaker state
        if (audioManager == null) {
            return;
        }

        isSpeakerOn = isSpeakerphoneOn;

        //turn on speaker if no have plug headset or bluetooth headset
        if (!isSpeakerphoneOn) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.abandonAudioFocus(null);
        } else {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN);
        }

        audioManager.setSpeakerphoneOn(isSpeakerOn);
        presenter.processAudioOutputChanged(isSpeakerOn);
    }

    private static void applyDefaultAudioSetting() {
        if (callType.equals(Constants.CONFIG_TYPE.AUDIO)) {
            isSpeakerOn = Utils.isDefaultSpeakerSettingForAudio();
        } else if (callType.equals(Constants.CONFIG_TYPE.VIDEO) || callType.equals(Constants.CONFIG_TYPE.MULTI_VIDEOS)) {
            isSpeakerOn = Utils.isDefaultSpeakerSettingForVideo();
        }

        changeAudioOutput(isSpeakerOn);
    }

    private void processHeadsetPlug(boolean isHeadsetPlug) {
        String logTag = "[SA][headsetBroadcastReceiver][onReceive] ";
        String log;

        isSpeakerOn = !isHeadsetPlug;

        if (isHeadsetPlug) {
            log = logTag + "Headset: Plugged";
        } else {
            log = logTag + "Headset: Unplugged";
        }

        setAudioPathOnHeadedSet(isSpeakerOn);

        presenter.processAudioOutputChanged(isSpeakerOn);

        Log.d(TAG, log);
    }

    private void processHeadsetBluetooth(boolean isConnected) {
        String logTag = "[SA][headsetBroadcastReceiver][onReceive] ";
        String log;

        isSpeakerOn = !isConnected;

        if (isConnected) {
            log = logTag + "Bluetooth: on";
            Log.d(TAG, log);

        } else {
            log = logTag + "Bluetooth: off";
            Log.d(TAG, log);
        }

        setAudioPathOnBluetooth(isSpeakerOn);

        presenter.processAudioOutputChanged(isSpeakerOn);
    }
}