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

        **Initialize SkylinkConnection**

            public class VideoCallFragment extends Fragment implements LifeCycleListener, MediaListener, RemotePeerListener {

            .....
            /**Implementation of callbacks provided the listeners 
            **/
             
            .....
            .....
            }

        Icon

        **Always implement LifeCycleListener and RemotePeerListener**

        In addition to that, depending on the functionality you wish to
        achieve add the respective listener

        1. For Audio Call : Implement MediaListener

        2. For Video Call : Implement MediaListener

        3. For File Transfer : Implement FileTransferListener

        4. For Messaging : Implement MessageListener

4.  Initialize SkylinkConfig to specify what features are required from
    the SDK

    **Initialize SkylinkConnection**

        private SkyLinkConfig getSkylinkConfig() {
                SkyLinkConfig config = new SkyLinkConfig();
                config.setAudioVideoSendConfig(SkyLinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
                config.setHasPeerMessaging(true);
                config.setHasFileTransfer(true);
                config.setTimeout(60);
                return config;
        }

    Icon

    There are four kinds of AudioVideoConfig

    01. SkyLinkConfig.AudioVideoConfig.AUDIO\_AND\_VIDEO

    02. SkyLinkConfig.AudioVideoConfig.NO\_AUDIO\_NO\_VIDEO

    03. SkyLinkConfig.AudioVideoConfig.AUDIO\_ONLY

    04. SkyLinkConfig.AudioVideoConfig.VIDEO\_ONLY

      

5.  Initialize SkylinkConnection object and pass the API key and secret
    obtained from the developer portal and also the config object from
    step 4

    **Initialize SkylinkConnection**

            SkylinkConnection skylinkConnection;
            .....
            ..... 
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate();  
                skyLinkConnection = SkyLinkConnection.getInstance();
                skyLinkConnection.init(getString(R.string.app_key),
                        getString(R.string.app_secret), getSkylinkConfig(), this.getActivity().getApplicationContext());
            //register respective listeners
                skyLinkConnection.setLifeCycleListener(this);
                skyLinkConnection.setMediaListener(this);
                skyLinkConnection.setRemotePeerListener(this);
                .........
                .........
           }

6.  Connect to a room using Skylink SDK

    **Initialize SkylinkConnection**

        try {
        //you will be connected to the room named "roomKey" using the name "userName" 
           skyLinkConnection.connectToRoom("roomKey", "userName");
         } catch (SignatureException e) {
           e.printStackTrace();
         } catch (IOException e) {
           e.printStackTrace();
         } catch (JSONException e) {
           e.printStackTrace();
         }

7.  Verify if the connection works by logging on callback

    **Initialize SkylinkConnection**

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

