# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Stack traces — preserve line numbers in crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# KotlinX Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep,includedescriptorclasses class com.programmersbox.**$$serializer { *; }
-keepclassmembers class com.programmersbox.** { *** Companion; }
-keepclasseswithmembers class com.programmersbox.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Firebase / Crashlytics
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.** { *; }
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# Koin
-keep class org.koin.** { *; }
-keepclassmembers class * { @org.koin.core.annotation.* *; }

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembernames class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
