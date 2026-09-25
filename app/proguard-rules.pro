# FFH VPN — R8 configuration
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# kotlinx.serialization (reflection-free, but keep generated serializers)
-keepclassmembers class com.ffh.vpn.** {
    *** Companion;
}
-keepclasseswithmembers class com.ffh.vpn.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ffh.vpn.**$$serializer { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Do not obfuscate anything that goes into generated Xray JSON (field names are
# serialized by kotlinx.serialization, which relies on the serializer classes)
-keepclassmembers class com.ffh.vpn.core.xray.** { *; }

# JNI launcher. R8 must not rename the class or the native methods.
-keep class com.ffh.vpn.core.CoreLauncher { *; }

# Remove noisy logging in release
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}
