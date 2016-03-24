# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/janidu/Desktop/android-sdk-macosx/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Do not obfuscate webrtc library
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**
-keep class io.socket.** { *; }
-dontwarn io.socket.**

-keep public class * {
    public *;
}

-keepclassmembers class * {
    public *;
}

-keepattributes Exceptions,InnerClasses
-keepparameternames

# Use:
# proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
# Instead of:
# proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
# In order for the following optimization to take effect.
-assumenosideeffects class android.util.Log {
# Allow Info, Warn, Error type logs to remain on release.
# However, no logging will occur unless SkylinkConfig's enableLogs is set to true (Skylink SDK >= 0.9.6).
#    public static *** e(...);
#    public static *** w(...);
#    public static *** i(...);
    public static *** d(...);
    public static *** v(...);
}
