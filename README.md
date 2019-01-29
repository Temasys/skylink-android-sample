# Temasys SDK for Android - Sample Application

Sample Application(SA) is a sample application using the Temasys SDK, provide embedded real time communication in a easiest way.

In SA, there are simple demos on:

  - Audio
  - Video
  - Chat/Message
  - DataTransfer
  - FileTransfers

## How to run the sample project

### Step-by-step guide for Android Studio

1. Clone this repository
1. Import the project into Android Studio using File -> Open and selecting the project
1. Create config.xml under res/values and add values for app_key and app_secret. See [config_example.xml](https://github.com/Temasys/skylink-android-sample/blob/master/sampleapp/src/main/res/values/config_example.xml) for more details
1. Build the project
1. Run the sampleapp module

## Sample App structure
The architecture of Sample app is the way we organize the code to have a clear structure. We try to separate between the application layer and the SDK usage layer.
With the separated parts, the user can easily change each part without changing the others and extend the functionality of the application.
For example, the user can using different view components to display GUI of the application while keeping the same logics which using the SDK.

The MVP (Model - View - Presenter) architecture used in the Sample App mainly divided into three main parts: View - Presenter - Service

    + View: responsible for displaying GUI and getting user events.
    + Presenter: responsible for processing app logic and implementing callbacks sent from the SkylinkSDK
    + Service: responsible for sending requests to SkylinkSDK, using SkylinkConnection instance to communicate with the Skylink SDK, the service part also contain the models (M) of the application.

For more details in Sample app's architecture, please refer to (https://github.com/Temasys/skylink-android-sample/blob/master/SAArch.md)

# Temasys SDK for Android

## SDK documentation

For more information on the usage of the Temasys SDK for Android, please refer to the following:

 - [Temasys SDK for Android Readme](https://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/readme.md)
 - [Getting started with SkylinkSDK using Android Studio](https://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/SkylinkSDK_Android_Studio_Setup.md)
 - [Android SDK version required](https://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/Android_SDK_Version_Required.md)
 - [Temasys SDK for Android API Documentation](https://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/doc/reference/packages.html)


## Subscribe

Subscribe to release notes for the Temasys SDK for Android! Check it out at:
http://support.temasys.io/support/solutions/articles/12000012359-how-can-i-subscribe-to-release-notes-for-skylink-


## Feedback

Please do not hesitate to reach get in touch with us if you encounter any issue or if you have any feedback or suggestions on how we could improve the Temasys SDK for Android or Sample Application.
You can raise tickets on our [support portal](http://support.temasys.io/) or on github.


## Copyright and License

Copyright 2014-2017 Temasys Communications Pte Ltd
Licensed under [APACHE 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
