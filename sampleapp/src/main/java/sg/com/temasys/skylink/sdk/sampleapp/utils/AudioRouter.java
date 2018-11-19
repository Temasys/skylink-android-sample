package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

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

    private static BasePresenter mPresenter;

    private static boolean isAudioOnSpeaker;

    private static Constants.CONFIG_TYPE mCallType;

    private AudioRouter() {
        headsetBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {

                    int state = intent.getIntExtra("state", -1);
                    switch (state) {
                        case 0:
//                            processHeadsetPlug(false);
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
                        if (isAudioOnSpeaker) {
                            setAudioPathOnBluetooth(true);

                            mPresenter.onServiceRequestAudioOutputChanged(true);
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
//                        processHeadsetBluetooth(false);
                    } else if (currentAudioState == BluetoothHeadset.STATE_DISCONNECTING) {
//                        processHeadsetBluetooth(false);
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
        mCallType = typeCall;
        applyDefaultAudioSetting();

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

            isAudioOnSpeaker = false;
            bluetoothAdapter = null;
            mPresenter = null;
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

    public static void changeAudioOutput(Context context, boolean isSpeakerphoneOn) {

        //turn on speaker if no have plug headset or bluetooth headset
        if (!isSpeakerphoneOn) {
            isAudioOnSpeaker = false;
            audioManager.setSpeakerphoneOn(false);
            mPresenter.onServiceRequestAudioOutputChanged(false);

        } else {
            isAudioOnSpeaker = true;
            audioManager.setSpeakerphoneOn(true);
            mPresenter.onServiceRequestAudioOutputChanged(true);
        }
    }

    public static void setPresenter(BasePresenter presenter) {
        mPresenter = presenter;
    }

    public static boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
    }

    public static void applyDefaultAudioSetting() {

        if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO)) {

            boolean isAudioSpeaker = Utils.getDefaultSpeakerAudio();

            //default audio is headset
            if (!isAudioSpeaker) {
                setAudioPathOnHeadedSet(false);
            } else {
                setAudioPathOnHeadedSet(true);
            }
        } else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO) || mCallType.equals(Constants.CONFIG_TYPE.MULTI_PARTY_VIDEO)) {

            boolean isVideoSpeaker = Utils.getDefaultSpeakerVideo();

            //default audio is headset
            if (!isVideoSpeaker) {
                setAudioPathOnHeadedSet(false);
            } else {
                setAudioPathOnHeadedSet(true);
            }
        }
    }

    private void processHeadsetPlug(boolean isHeadsetPlug) {
        String logTag = "[SA][headsetBroadcastReceiver][onReceive] ";
        String log;

        if (isHeadsetPlug) {
            //set speaker off
            setAudioPathOnHeadedSet(false);

            mPresenter.onServiceRequestAudioOutputChanged(false);
            log = logTag + "Headset: Plugged";

        } else {
            //only set speaker on when user select turn on speaker button
            if (isAudioOnSpeaker) {
                setAudioPathOnHeadedSet(true);
                mPresenter.onServiceRequestAudioOutputChanged(true);
            }
            log = logTag + "Headset: Unplugged";
        }

        Log.d(TAG, log);
    }

    private void processHeadsetBluetooth(boolean isConnected) {
        String logTag = "[SA][headsetBroadcastReceiver][onReceive] ";
        String log;
        if (isConnected) {
            setAudioPathOnBluetooth(false);

            mPresenter.onServiceRequestAudioOutputChanged(false);

            log = logTag + "Bluetooth: on";
            Log.d(TAG, log);

        } else {
            // Bluetooth is disconnected, do speaker on
            // only turn on speaker when user select turn on speaker button
            if (isAudioOnSpeaker) {
                setAudioPathOnBluetooth(true);

                mPresenter.onServiceRequestAudioOutputChanged(true);
            }

            log = logTag + "Bluetooth: off";
            Log.d(TAG, log);
        }
    }
}