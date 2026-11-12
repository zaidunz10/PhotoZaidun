# ==========================================
# GOOGLE DRIVE API & GOOGLE API CLIENT
# ==========================================

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep class com.google.api.services.drive.model.** { *; }
-keep class com.google.api.services.drive.** { *; }

-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.** { *; }

-keep class * extends com.google.api.client.json.GenericJson { *; }
-keep class * extends com.google.api.client.util.GenericData { *; }

-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}

# Ignore optional legacy library warnings in R8
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.apache.http.**
-dontwarn com.google.api.client.**
-dontwarn com.google.android.play.**


# ==========================================
# KOTLINX SERIALIZATION
# ==========================================

-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

-keep,includedescriptorclasses class **$$serializer { *; }

-keepclassmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}


# ==========================================
# SUPABASE DTO MODELS
# ==========================================

-keep class com.zaidun.carifoto.data.remote.** { *; }


# ==========================================
# REFLECTION SUPPORT
# ==========================================

-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes AnnotationDefault
