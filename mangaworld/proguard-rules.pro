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
# Kotlinx Serialization
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
# Room 3 (androidx.room3 artifact, package androidx.room)
# ============================================================
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# ============================================================
# Extension / source-loading contract (kmpmodels)
# Mihon-style: external plugins implement these at runtime, so they
# must survive shrinking/obfuscation with their original names.
# ============================================================
-keep interface com.programmersbox.kmpmodels.** { *; }
-keep class com.programmersbox.kmpmodels.** { *; }
-keep class * implements com.programmersbox.kmpmodels.KmpApiService { *; }
-dontwarn com.programmersbox.kmpmodels.**

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
# Piasy BigImageViewer
# ============================================================
-keep class com.github.piasy.** { *; }
-dontwarn com.github.piasy.**

# ============================================================
# SubsamplingScaleImageView
# ============================================================
-keep class com.davemorrissey.labs.** { *; }
-dontwarn com.davemorrissey.labs.**

# ============================================================
# pagecurl
# ============================================================
-keep class io.github.oleksandrbalan.pagecurl.** { *; }
-dontwarn io.github.oleksandrbalan.pagecurl.**

# ============================================================
# panpf zoomimage
# ============================================================
-keep class io.github.panpf.zoomimage.** { *; }
-dontwarn io.github.panpf.zoomimage.**

# ============================================================
# telephoto
# ============================================================
-keep class me.saket.telephoto.** { *; }
-dontwarn me.saket.telephoto.**

# ============================================================
# Zipline / Duktape (JS engine used by source plugins)
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
