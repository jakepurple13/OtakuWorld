# ============================================================
# Kotlin
# ============================================================
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { *; }
-dontwarn kotlin.**

# ============================================================
# Kotlin Coroutines
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ============================================================
# Kotlin Serialization
# ============================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.programmersbox.**$$serializer { *; }
-keepclassmembers class com.programmersbox.** {
    *** Companion;
}
-keepclasseswithmembers class com.programmersbox.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class * { *; }

# ============================================================
# Compose / Compose Multiplatform
# ============================================================
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**

# ============================================================
# Koin
# ============================================================
-keep class org.koin.** { *; }
-keepnames class * {
    @org.koin.core.annotation.* *;
}
-dontwarn org.koin.**

# ============================================================
# Room (KMP / androidx.room3)
# ============================================================
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# ============================================================
# Firebase
# ============================================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ============================================================
# Ktor
# ============================================================
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ============================================================
# OkHttp
# ============================================================
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ============================================================
# Glide
# ============================================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**

# ============================================================
# Kamel (KMP image loading)
# ============================================================
-keep class media.kamel.** { *; }
-dontwarn media.kamel.**

# ============================================================
# Haze (glassmorphism)
# ============================================================
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# ============================================================
# Navigation3
# ============================================================
-keep class * implements androidx.navigation3.runtime.NavKey { *; }
-keep @kotlinx.serialization.Serializable class * implements androidx.navigation3.runtime.NavKey { *; }
-dontwarn androidx.navigation3.**

# ============================================================
# DataStore / Protobuf
# ============================================================
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class * extends com.google.protobuf.MessageLite { *; }
-dontwarn com.google.protobuf.**

# ============================================================
# AboutLibraries
# ============================================================
-keep class com.mikepenz.aboutlibraries.** { *; }
-dontwarn com.mikepenz.aboutlibraries.**

# ============================================================
# Iconics
# ============================================================
-keep class com.mikepenz.iconics.** { *; }
-dontwarn com.mikepenz.iconics.**

# ============================================================
# jakepurple13 HelpfulTools
# ============================================================
-keep class com.github.jakepurple13.** { *; }
-dontwarn com.github.jakepurple13.**

# ============================================================
# AnimeWorld-specific: Gson (used directly in animeworld)
# ============================================================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-dontwarn com.google.gson.**

# ============================================================
# AnimeWorld-specific: ExoPlayer / Media3
# ============================================================
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# ============================================================
# AnimeWorld-specific: Cast SDK
# ============================================================
-keep class com.google.android.gms.cast.** { *; }
-keep class com.google.android.gms.cast.framework.** { *; }
-dontwarn com.google.android.gms.cast.**

# ============================================================
# AnimeWorld-specific: AutoBindings
# ============================================================
-keep class io.github.kaustubhpatange.autobindings.** { *; }
-dontwarn io.github.kaustubhpatange.autobindings.**

# ============================================================
# Supabase
# ============================================================
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ============================================================
# Preserve stack traces
# ============================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
