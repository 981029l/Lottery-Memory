-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations

# kotlinx.serialization
-keep,includedescriptorclasses class com.caiji.app.model.**$$serializer { *; }
-keepclassmembers class com.caiji.app.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.caiji.app.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
