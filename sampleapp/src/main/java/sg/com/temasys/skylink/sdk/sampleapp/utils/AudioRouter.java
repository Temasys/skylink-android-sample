package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;


/**
 * Simple AudioRouter that switches between the speaker phone and the headset
 */
public class AudioRouter{

    private static AudioRouter instance = null;

    private static final String TAG = AudioRouter.class.getName();
    private static BroadcastReceiver headsetBroadcastReceiver;
    private static BroadcastReceiver blueToothBroadcastReceiver;

    private static AudioManager audioManager;

    private static BluetoothAdapter bluetoothAdapter;

    //variable to set audio mode = headset at the first time
    private static boolean isFirstTimeAudioOnHeadset;
    private static boolean isFirstTimeAudioOnBlueTooth;

    private AudioRouter() {
        headsetBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                    String logTag = "[SA][headsetBroadcastReceiver][onReceive] ";
                    String log;
                    int state = intent.getIntExtra("state", -1);
                    switch (state) {
                        case 0:
                            setAudioPathOnHeadedSet(context, true);
                            log = logTag + "Headset: Unplugged";
                            Log.d(TAG, log);
                            break;
                        case 1:
                            setAudioPathOnHeadedSet(context, false);
                            log = logTag + "Headset: Plugged";
                            Log.d(TAG, log);
                            break;
                        default:
                            log = logTag + "Headset: Error determining state!";
                            Log.d(TAG, log);
                    }

                    //change variable value for next time usage of audio as normal behavior
                    if (isFirstTimeAudioOnHeadset) {
                        isFirstTimeAudioOnHeadset = false;
                    }
                }
            }
        };

        blueToothBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                    String logTag = "[SA][headsetBroadcastReceiver][onReceive] ";
                    String log;
                    int currentAudioState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
                    if (currentAudioState == BluetoothHeadset.STATE_DISCONNECTED) {
                        // Bluetooth is disconnected, do speaker on
                        setAudioPathOnBluetooth(context, true);
                        log = logTag + "Bluetooth: off";
                        Log.d(TAG, log);
                    } else if(currentAudioState == BluetoothHeadset.STATE_DISCONNECTING){
                        setAudioPathOnBluetooth(context, true);
                        log = logTag + "Bluetooth: off";
                        Log.d(TAG, log);
                    } else if(currentAudioState == BluetoothHeadset.STATE_CONNECTED){
                        setAudioPathOnBluetooth(context, false);
                        log = logTag + "Bluetooth: on";
                        Log.d(TAG, log);
                    } else if(currentAudioState == BluetoothHeadset.STATE_CONNECTING){
                        setAudioPathOnBluetooth(context, false);
                        log = logTag + "Bluetooth: on";
                        Log.d(TAG, log);
                    }
                }

                //change variable value for next time usage of audio as normal behavior
                if (isFirstTimeAudioOnBlueTooth) {
                    isFirstTimeAudioOnBlueTooth = false;
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

    /**
     * Initialize AudioRouter and
     * start routing audio to headset if plugged, else to the speaker phone
     */
    public static void startAudioRouting(Context context) {
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
                new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED));


        log = logTag + "Setting Audio Path...";
        Log.d(TAG, log);

        isFirstTimeAudioOnHeadset = true;
        isFirstTimeAudioOnBlueTooth = true;
        setAudioPathOnHeadedSet(context, false);


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
            // Must use applicationContext here and not Activity context.
            context.getApplicationContext().unregisterReceiver(headsetBroadcastReceiver);
            context.getApplicationContext().unregisterReceiver(blueToothBroadcastReceiver);
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

    /**
     * Set the audio path according to whether earphone is connected. Use ear piece if earphone is
     * connected. Use speakerphone if no earphone is connected.
     */
    private static void setAudioPathOnHeadedSet(Context context, boolean isSpeakerphoneOn) {
        String logTag = "[SA][AR][setAudioPath] ";
        String log = logTag + "Trying to set audio path...";
        Log.d(TAG, log);

        if (audioManager == null) {
            throw new RuntimeException(
                    "Attempt to set audio path before setting AudioManager");
        }

        //the audio is in headset mode at the first time
        if (isFirstTimeAudioOnHeadset) {
            audioManager.setSpeakerphoneOn(false);
            log = logTag + "Setting Speakerphone to off as the first time usage.";
        } else {
            audioManager.setSpeakerphoneOn(isSpeakerphoneOn);
            log = logTag + "Setting Speakerphone to " + isSpeakerphoneOn +
                    " as audioManager.isWiredHeadsetOn() = " + audioManager.isWiredHeadsetOn();
        }

        Log.d(TAG, log);

        log = logTag + "Setting audio path is complete.";
        Log.d(TAG, log);
    }

    private static void setAudioPathOnBluetooth(Context context, boolean isSpeakerphoneOn) {
        String logTag = "[SA][AR][setAudioPath] ";
        String log = logTag + "Trying to set audio path...";
        Log.d(TAG, log);

        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
        } else if (isFirstTimeAudioOnBlueTooth) {
            audioManager.setSpeakerphoneOn(false);
            log = logTag + "Setting Speakerphone to off as the first time usage.";
        } else {
            audioManager.setSpeakerphoneOn(isSpeakerphoneOn);
            log = logTag + "Setting Speakerphone to " + isSpeakerphoneOn +
                    " as currentAudioState = " + bluetoothAdapter.isEnabled();
        }

        Log.d(TAG, log);

        log = logTag + "Setting audio path is complete.";
        Log.d(TAG, log);
    }


    static void initializeAudioRouter(Context context) {
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
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        instance.init(audioManager, bluetoothAdapter);
        log = logTag + "Initializing audio router is complete.";
        Log.d(TAG, log);
    }
}