# ProGuard rules for Screen Translator

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep native JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---------- Online AI Translation (Retrofit + Gson + OkHttp) ----------
# R8 must not rename/remove the online API models & service interface.
# Gson serializes/deserializes these classes via reflection, and Retrofit
# resolves the endpoint methods via reflection too.
-keep class com.rocat.translator.online.** { *; }

# Attributes needed by Retrofit/Gson reflection
-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions, Annotation
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Retrofit keeps the suspend/Call machinery
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep methods annotated with Retrofit HTTP annotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp / Okio / Retrofit warnings
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
