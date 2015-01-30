Skylink SDK : Getting Started (Android Studio)
==============================================

If you are using Android Studio, you just need to do the following to
start using the SDK.

Set up the Skylink SDK for Android
==================================

Set up build.gradle to download and use Skylink SDK
---------------------------------------------------

Add the following to your app's build.gradle:

    repositories {
        maven {
            url = 'http://archiva.temasys.com.sg/repository/internal'
        }
    }
    dependencies {
        compile(group: 'sg.com.temasys.skylink.sdk',
                name: 'skylink_sdk',
                version: '0.9.0-RELEASE',
                ext: 'aar')
    }

You are ready go!
=================

For more information on the SDK usage, please refer to the sample
application at:

http://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/sample_app.tar.gz

=================================

``

 

