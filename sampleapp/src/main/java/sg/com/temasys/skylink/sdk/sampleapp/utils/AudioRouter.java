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
import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioCallContract;
import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioCallPresenter;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoCallContract;


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

    private static AudioCallContract.Presenter mAudioPresenter;
    private static VideoCallContract.Presenter mVideoPresenter;

    //variable to set audio mode = headset at the first time
    private static boolean isFirstTimeAudioOnHeadset;
    private static boolean isFirstTimeAudioOnBlueTooth;
    private static boolean isAudioOnSpeaker, isPlugHeadset, isBluetoothHeadset;

    private static Constants.CONFIG_TYPE mCallType;

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

                    //change variable value for next time usage of audio as normal behavior
                    if (isFirstTimeAudioOnHeadset) {
                        isFirstTimeAudioOnHeadset = false;
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
                        isPlugHeadset = false;
                        isBluetoothHeadset = false;
                        // Bluetooth is disconnected, do speaker on
                        if (isAudioOnSpeaker) {
                            setAudioPathOnBluetooth(true);

                            if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO))
                                mAudioPresenter.onAudioChangedToSpeaker(true);
                            else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO))
                                mVideoPresenter.onAudioChangedToSpeaker(true);
                        }
                        log = logTag + "Bluetooth: off";
                        Log.d(TAG, log);
                    }
                }

                //change variable value for next time usage of audio as normal behavior
                if (isFirstTimeAudioOnBlueTooth) {
                    isFirstTimeAudioOnBlueTooth = false;
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
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        appContext.registerReceiver(blueToothAudioBroadcastReceiver,
                new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED));


        log = logTag + "Setting Audio Path...";
        Log.d(TAG, log);

        //use default setting for initialize
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
            isAudioOnSpeaker = false;
            isPlugHeadset = false;
            isBluetoothHeadset = false;

            applyDefaultAudioSetting();

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

        //check default setting before apply change
        boolean defaultSpeakerOn = false;
        if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO))
            defaultSpeakerOn = Utils.getDefaultAudioSpeaker();
        else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO))
            defaultSpeakerOn = Utils.getDefaultVideoSpeaker();

        //the audio is in headset mode at the first time
        if (isFirstTimeAudioOnHeadset) {
            audioManager.setSpeakerphoneOn(defaultSpeakerOn);
            log = logTag + "Setting Speakerphone to " + defaultSpeakerOn + " as default setting";
        } else {
            audioManager.setSpeakerphoneOn(isSpeakerphoneOn);
            log = logTag + "Setting Speakerphone to " + isSpeakerphoneOn +
                    " as audioManager.isWiredHeadsetOn() = " + audioManager.isWiredHeadsetOn();
        }

        Log.d(TAG, log);

        log = logTag + "Setting audio path is complete.";
        Log.d(TAG, log);
    }

    private static void setAudioPathOnBluetooth(boolean isSpeakerphoneOn) {
        String logTag = "[SA][AR][setAudioPath] ";
        String log = logTag + "Trying to set audio path...";
        Log.d(TAG, log);

        //check default setting before apply change
        boolean defaultSpeakerOn = false;
        if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO))
            defaultSpeakerOn = Utils.getDefaultAudioSpeaker();
        else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO))
            defaultSpeakerOn = Utils.getDefaultVideoSpeaker();

        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
        } else if (isFirstTimeAudioOnBlueTooth) {
            audioManager.setSpeakerphoneOn(defaultSpeakerOn);
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

    public static void changeAudioOutput(Context context, boolean isSpeakerphoneOn) {

        //in case of connected to wired headset before open the app
        if (audioManager.isWiredHeadsetOn() && !isPlugHeadset) {
            isPlugHeadset = true;
        }

        //in case of connected to bluetooth before open the app
        if (isBluetoothHeadsetConnected() && !isBluetoothHeadset) {
            isBluetoothHeadset = true;
        }

        //turn on speaker if no have plug headset or bluetooth headset
        if (isSpeakerphoneOn && !isPlugHeadset && !isBluetoothHeadset && audioManager != null) {
            isAudioOnSpeaker = true;
            audioManager.setSpeakerphoneOn(true);
            if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO))
                mAudioPresenter.onAudioChangedToSpeaker(true);
            else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO))
                mVideoPresenter.onAudioChangedToSpeaker(true);
        } else if (isSpeakerphoneOn && isPlugHeadset) {
            isAudioOnSpeaker = false;
            audioManager.setSpeakerphoneOn(false);
            if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO))
                mAudioPresenter.onAudioChangedToSpeaker(false);
            else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO))
                mVideoPresenter.onAudioChangedToSpeaker(false);
            Utils.toastLogLong(TAG, context, "Can not turn speaker on because headset is plugged");
        } else if (isSpeakerphoneOn && isBluetoothHeadset) {
            isAudioOnSpeaker = false;
            audioManager.setSpeakerphoneOn(false);
            if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO))
                mAudioPresenter.onAudioChangedToSpeaker(false);
            else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO))
                mVideoPresenter.onAudioChangedToSpeaker(false);
            Utils.toastLogLong(TAG, context, "Can not turn speaker on because bluetooth headset is connected");
        } else {
            isAudioOnSpeaker = false;
            audioManager.setSpeakerphoneOn(false);
            if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO))
                mAudioPresenter.onAudioChangedToSpeaker(false);
            else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO))
                mVideoPresenter.onAudioChangedToSpeaker(false);
        }
    }

    public static void setPresenter(BasePresenter mPresenter) {
        if (mPresenter instanceof AudioCallPresenter) {
            mAudioPresenter = (AudioCallContract.Presenter) mPresenter;
        } else if (mPresenter instanceof VideoCallContract.Presenter) {
            mVideoPresenter = (VideoCallContract.Presenter) mPresenter;
        }
    }

    public static boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
    }

    public static void setCallType(Constants.CONFIG_TYPE callType) {
        mCallType = callType;
    }

    public static void applyDefaultAudioSetting() {

        if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO)) {

            boolean isAudioSpeaker = Utils.getDefaultAudioSpeaker();

            //default audio is headset
            if (!isAudioSpeaker) {
                isFirstTimeAudioOnHeadset = true;
                isFirstTimeAudioOnBlueTooth = true;
                setAudioPathOnHeadedSet(false);
            } else {
                isFirstTimeAudioOnHeadset = false;
                isFirstTimeAudioOnBlueTooth = false;
                setAudioPathOnHeadedSet(true);
            }
        } else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO)) {

            boolean isVideoSpeaker = Utils.getDefaultVideoSpeaker();

            //default audio is headset
            if (!isVideoSpeaker) {
                isFirstTimeAudioOnHeadset = true;
                isFirstTimeAudioOnBlueTooth = true;
                setAudioPathOnHeadedSet(false);
            } else {
                isFirstTimeAudioOnHeadset = false;
                isFirstTimeAudioOnBlueTooth = false;
                setAudioPathOnHeadedSet(true);
            }
        }
    }

    private void processHeadsetPlug(boolean isHeadsetPlug) {
        String logTag = "[SA][headsetBroadcastReceiver][onReceive] ";
        String log;

        if (isHeadsetPlug) {
            isPlugHeadset = true;
            isBluetoothHeadset = false;
            //set speaker off
            setAudioPathOnHeadedSet(false);

            if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO))
                mAudioPresenter.onAudioChangedToSpeaker(false);
            else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO))
                mVideoPresenter.onAudioChangedToSpeaker(false);
            log = logTag + "Headset: Plugged";

        } else {
            isPlugHeadset = false;
            isBluetoothHeadset = false;
            //only set speaker on when user select turn on speaker button
            if (isAudioOnSpeaker) {
                setAudioPathOnHeadedSet(true);
                if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO))
                    mAudioPresenter.onAudioChangedToSpeaker(true);
                else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO))
                    mVideoPresenter.onAudioChangedToSpeaker(true);
            }
            log = logTag + "Headset: Unplugged";
        }

        Log.d(TAG, log);
    }

    private void processHeadsetBluetooth(boolean isConnected) {
        String logTag = "[SA][headsetBroadcastReceiver][onReceive] ";
        String log;
        if (isConnected) {

            isPlugHeadset = false;
            isBluetoothHeadset = true;
            setAudioPathOnBluetooth(false);
            if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO))
                mAudioPresenter.onAudioChangedToSpeaker(false);
            else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO))
                mVideoPresenter.onAudioChangedToSpeaker(false);

            log = logTag + "Bluetooth: on";
            Log.d(TAG, log);

        } else {

            isPlugHeadset = false;
            isBluetoothHeadset = false;
            // Bluetooth is disconnected, do speaker on
            // only turn on speaker when user select turn on speaker button
            if (isAudioOnSpeaker) {
                setAudioPathOnBluetooth(true);

                if (mCallType.equals(Constants.CONFIG_TYPE.AUDIO))
                    mAudioPresenter.onAudioChangedToSpeaker(true);
                else if (mCallType.equals(Constants.CONFIG_TYPE.VIDEO))
                    mVideoPresenter.onAudioChangedToSpeaker(true);
            }

            log = logTag + "Bluetooth: off";
            Log.d(TAG, log);

        }
    }
}