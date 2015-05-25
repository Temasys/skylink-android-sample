Skylink SDK : Getting Started with SkylinkSDK
=============================================

How to implement the SkylinkSDK in your app.

Step-by-step guide
------------------

 

1.  Add the SDK to your project. 
    1.  Getting started with SkylinkSDK using Android Studio <<< see SkylinkSDK_Android_Studio_Setup.md >>>
    2.  Getting started with SkylinkSDK using Eclipse (ADT) <<< see SkylinkSDK_Eclipse_ADT_Setup.md >>>
    3.  Note the Android SDK version required for the SkylinkSDK used <<< see Android_SDK_Version_Required.md >>>

2.  Register at [Temasys Developer
    Portal](https://developer.temasys.com.sg/) To receive your
    application key and secret. 
3.  Implement Listeners in the Class which needs to receive the events
    sent from the SDK 
    -   List of Listeners and the callbacks they provide can be found
        [here](http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/doc/index.html)

`

            public class VideoCallFragment extends Fragment implements LifeCycleListener, MediaListener, RemotePeerListener {

            .....
            /**Implementation of callbacks provided the listeners
            **/
             
            .....
            .....
            }
'

####Always implement LifeCycleListener and RemotePeerListener

   In addition to that, depending on the functionality you wish to
        achieve add the respective listener

        1. For Audio Call : Implement MediaListener
        2. For Video Call : Implement MediaListener
        3. For File Transfer : Implement FileTransferListener
        4. For Data Transfer : Implement DataTransferListener
        5. For Messaging : Implement MessageListener

####Initialize SkylinkConfig to specify what features are required from the SDK

    private SkyLinkConfig getSkylinkConfig() {
                SkyLinkConfig config = new SkyLinkConfig();
                config.setAudioVideoSendConfig(SkyLinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
                config.setHasPeerMessaging(true);
                config.setHasFileTransfer(true);
                config.setHasDataTransfer(true);
                config.setTimeout(60);
                return config;
        }

 There are four kinds of AudioVideoConfig

    01. SkyLinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO

    02. SkyLinkConfig.AudioVideoConfig.NO_AUDIO_NO_VIDEO

    03. SkyLinkConfig.AudioVideoConfig.AUDIO_ONLY

    04. SkyLinkConfig.AudioVideoConfig.VIDEO_ONLY


####Initialize SkylinkConnection object and pass the App key from the developer portal

            SkylinkConnection skylinkConnection;
            .....
            .....
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate();
                skyLinkConnection = SkyLinkConnection.getInstance();
                skyLinkConnection.init(getString(R.string.app_key), getSkylinkConfig(),
                                                        this.getActivity().getApplicationContext());
                // register respective listeners
                skyLinkConnection.setLifeCycleListener(this);
                skyLinkConnection.setMediaListener(this);
                skyLinkConnection.setRemotePeerListener(this);
                .........
                .........
           }

#### Connect to a room using Skylink SDK - using the secret key obtained from the developer portal

           // you will be connected to the room named "roomKey" using the name "userName"
           skyLinkConnection.connectToRoom("secret", "roomKey", "userName");

#### Connect to a room using Skylink SDK - using a connection string (recommended to use in production, refer to sample app for more details)

           // SkylinkConnectionString Generated with room name, appKey, secret, startTime and duration
           skyLinkConnection.connectToRoom(skylinkConnectionString, "userName");

####Verify if the connection works by logging on callback

            /***
             * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's lifecycle
             */

            /**
             * Triggered when connection is successful
             *
             * @param isSuccess
             * @param message
             */
            @Override
            public void onConnect(boolean isSuccess, String message) {
                if (isSuccess) {
                    Toast.makeText(getActivity(), "Connected to room ").show();
                } else {
                    Log.d(TAG, "Skylink Connection Failed");
                }
            }
            @Override
            public void onWarning(String message) {
                Log.d(TAG, message + "warning");
            }
            @Override
            public void onDisconnect(String message) {
                Log.d(TAG, message + " SkylinkConnection has been disconnected");
            }
            @Override
            public void onReceiveLog(String message) {
                Log.d(TAG, message + " on receive log");
            }

