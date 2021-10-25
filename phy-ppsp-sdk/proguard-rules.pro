-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-printmapping mapping.txt
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-ignorewarnings

#保留注解:
-keepattributes *Annotation*
-keepattributes JavascriptInterface
-keepattributes Signature

-dontwarn android.webkit.WebView
-dontwarn com.alibaba.fastjson.**

-keep class com.phy.ota.sdk.PhyOTAsUtils{*;}
-keep class com.phy.ota.sdk.ble.OTAsBtLeUtils{*;}

-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.AppCompatActivity
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Fragment
-keep public class * extends android.support.v4.app.Fragment
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver


-keep interface com.phy.ota.sdk.firware.OTAsStatusCallBack{ *; }

-keep interface com.phy.ota.sdk.ble.OTACallBack{ *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

