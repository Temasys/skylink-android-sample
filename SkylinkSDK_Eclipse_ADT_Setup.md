##Skylink SDK : Getting Started (Android Developer Tools)

Temasys does not officially support the ADT/Eclipse development tools
with our SkylinkSDK for Android. This information is offered for
educational purposes only.

If you are using Eclipse with ADT (Android Developer Tools) plugin, you
just need to do the following to start using the SDK.

###Set up the Skylink SDK for Android


#### 1. [Download ](http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/skylink_sdk-release.jar) the SkyLink SDK Jar

#### 2. Copy the SkyLink SDK Jar to the /libs folder

The Skylink SDK Jar should now be located at:

    /libs/skylink_sdk-release.jar


###Set up WebRTC native dependencies

##### 1.1 [Download armeabi-v7a](http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/armeabi-v7a/libjingle_peerconnection_so.so)
##### 1.2 [Download x86](http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/x86/libjingle_peerconnection_so.so)
##### 1.3 [Download x86-64](http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/x86-64/libjingle_peerconnection_so.so)


#### 2. Set up libjingle\_peerconnection\_so.so in relevant architecture folders

#### Make architecture specific directories inside libs folder:

    /libs/armeabi-v7a
    /libs/x86
    /libs/x86-64

#### 3. Copy the relevant libjingle\_peerconnection\_so.so files on to the proper directories

WebRTC .so (armeabi-v7a) file is now located at:

    /libs/armeabi-v7a/libjingle_peerconnection_so.so

WebRTC .so (x86) file is now located at:

    /libs/x86/libjingle_peerconnection_so.so

WebRTC .so (x86-64) file is now located at:

    /libs/x86-64/libjingle_peerconnection_so.so

###You are ready go!

For more information on the SDK usage, please refer to the [simple demo application](http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/sample_app_adt.tar.gz)
application. You will need to follow the same procedure described above to add the Skylink SDK for Android to the sample application. The sample application also requires [v7 appcompat](https://developer.android.com/tools/support-library/setup.html) as a dependency.