# Kotlin
-keep class kotlin.Metadata { *; }

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# KotlinX Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep,includedescriptorclasses class com.programmersbox.**$$serializer { *; }
-keepclassmembers class com.programmersbox.** { *** Companion; }
-keepclasseswithmembers class com.programmersbox.** {
    kotlinx.serialization.KSerializer serializer(...);
}
