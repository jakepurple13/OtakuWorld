# ============================================================
# Kotlin
# ============================================================
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { *; }
-keepclassmembers class kotlin.Lazy { *; }
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
-keepclassmembers class * {
    @androidx.compose.ui.tooling.preview.Preview *;
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
-dontwarn io.ktor.utils.io.**

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
# Batik/XMLGraphics (transitive via kamel-decoder-svg-batik — Java AWT not on Android)
# ============================================================
-dontwarn org.apache.batik.**
-dontwarn org.apache.xmlgraphics.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**

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
-keep class * extends com.google.protobuf.MessageLiteOrBuilder { *; }
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
# MangaWorld-specific: Piasy BigImageViewer
# ============================================================
-keep class com.github.piasy.** { *; }
-dontwarn com.github.piasy.**

# ============================================================
# MangaWorld-specific: SubsamplingScaleImageView
# ============================================================
-keep class com.davemorrissey.labs.** { *; }
-dontwarn com.davemorrissey.labs.**

# ============================================================
# MangaWorld-specific: pagecurl
# ============================================================
-keep class io.github.oleksandrbalan.pagecurl.** { *; }
-dontwarn io.github.oleksandrbalan.pagecurl.**

# ============================================================
# MangaWorld-specific: panpf zoomimage
# ============================================================
-keep class com.github.panpf.zoomimage.** { *; }
-dontwarn com.github.panpf.zoomimage.**

# ============================================================
# MangaWorld-specific: telephoto
# ============================================================
-keep class me.saket.telephoto.** { *; }
-dontwarn me.saket.telephoto.**

# ============================================================
# MangaWorld-specific: Zipline / Duktape
# ============================================================
-keep class com.squareup.duktape.** { *; }
-keep class app.cash.zipline.** { *; }
-dontwarn com.squareup.duktape.**
-dontwarn app.cash.zipline.**

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
