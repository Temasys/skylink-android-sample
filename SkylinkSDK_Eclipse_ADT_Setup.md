Skylink SDK : Getting Started (Android Developer Tools)
=======================================================

Temasys does not officially support the ADT/Eclipse development tools
with our SkylinkSDK for Android. This information is offered for
educational purposes only.

If you are using Eclipse with ADT (Android Developer Tools) plugin, you
just need to do the following to start using the SDK.

Set up the Skylink SDK for Android
==================================

Set up the Skylink SDK libraries
--------------------------------

### Download the SkyLink SDK Jar from:

http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/skylink_sdk-release.jar

### Copy the SkyLink SDK Jar to the /libs folder

The Skylink SDK Jar should now be located at:



    /libs/skylink_sdk-release.jar


Set up WebRTC native dependencies[](http://archiva.temasys.com.sg/repository/internal/sg/com/temasys/skylink/sdk/skylink_sdk_jar/0.9.0-RELEASE/skylink_sdk_jar-0.9.0-RELEASE.jar)
---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

### Download the WebRTC native dependencies

[http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/armeabi-v7a/libjingle_peerconnection_so.so](http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/armeabi-v7a/libjingle_peerconnection_so.so)

### Set up [libjingle\_peerconnection\_so.so](http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/armeabi-v7a/libjingle_peerconnection_so.so) in armeabi-v7a

#### Make an architecture specific directory inside libs folder:

    /libs/armeabi-v7a

#### Copy [libjingle\_peerconnection\_so.so](http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/armeabi-v7a/libjingle_peerconnection_so.so) into the new directory

WebRTC .so file is now located at:

    /libs/armeabi-v7a/libjingle_peerconnection_so.so

You are ready go!
=================

For more information on the SDK usage, please refer to the sample
application at:

http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/sample_app_adt.tar.gz
 

