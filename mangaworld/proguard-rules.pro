# ============================================================
# Kotlin
# ============================================================
#-keep class kotlin.Metadata { *; }
#-keepclassmembers class **$WhenMappings { *; }
#-keepclassmembers class kotlin.Lazy { *; }
#-dontwarn kotlin.**

# ============================================================
# Kotlin Coroutines
# ============================================================
#-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
#-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
#-keepclassmembernames class kotlinx.** { volatile <fields>; }
#-dontwarn kotlinx.coroutines.**

-keepclassmembers class androidx.core.content.ContextCompat {
    public static java.util.concurrent.Executor getMainExecutor(android.content.Context);
}

-keep class com.programmersbox.source_utilities.NetworkHelper { *; }
-keep class com.programmersbox.source_utilities.** { *; }

# ============================================================
# Kotlinx Serialization
# ============================================================
#-keepattributes *Annotation*, InnerClasses
#-dontnote kotlinx.serialization.AnnotationsKt
#-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
#-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
#-keep,includedescriptorclasses class com.programmersbox.**$$serializer { *; }
#-keepclassmembers class com.programmersbox.** {
#    *** Companion;
#}
#-keepclasseswithmembers class com.programmersbox.** {
#    kotlinx.serialization.KSerializer serializer(...);
#}
#-keep @kotlinx.serialization.Serializable class * { *; }

# Keep Kotlin standard library methods that extensions might rely on
#-keep class kotlin.** { *; }
#-keep class kotlin.jvm.internal.** { *; }
#-keepclassmembers class kotlin.** { *; }

# Keep kotlinx.serialization for extensions
#-keep class kotlinx.serialization.** { *; }
#-keepclassmembers class kotlinx.serialization.** { *; }

# Keep your extension loader and the interfaces/classes it reflects on
-keep class com.programmersbox.kmpextensionloader.** { *; }

# Keep everything in programmersbox, EXCEPT classes inside the showcase package
#-keep class !com.programmersbox.showcase.**, com.programmersbox.** { *; }

# ============================================================
# Compose / Compose Multiplatform
# ============================================================
#-keep class androidx.compose.** { *; }
#-keepclassmembers class * {
#    @androidx.compose.runtime.Composable *;
#}
#-keepclassmembers class * {
#    @androidx.compose.ui.tooling.preview.Preview *;
#}

#-dontwarn androidx.compose.**

# ============================================================
# Koin
# ============================================================
-keep class org.koin.** { *; }
-keepnames class * {
    @org.koin.core.annotation.* *;
}
-dontwarn org.koin.**

# ============================================================
# Room 3 (androidx.room3 artifact and package)
# ============================================================
#-keep @androidx.room3.Entity class * { *; }
#-keep @androidx.room3.Database class * { *; }
#-keep @androidx.room3.Dao class * { *; }
#-keepclassmembers class * extends androidx.room3.RoomDatabase { *; }
#-dontwarn androidx.room3.**
#-dontwarn androidx.room.**

# ============================================================
# Extension / source-loading contract (kmpmodels)
# Mihon-style: external plugins implement these at runtime, so they
# must survive shrinking/obfuscation with their original names.
# ============================================================
-keep interface com.programmersbox.kmpmodels.** { *; }
-keep class com.programmersbox.kmpmodels.** { *; }
-keep class * implements com.programmersbox.kmpmodels.KmpApiService { *; }
-dontwarn com.programmersbox.kmpmodels.**
-keep class com.programmersbox.models.** { *; }

# ============================================================
# Firebase
# ============================================================
#-keep class com.google.firebase.** { *; }
#-keep class com.google.android.gms.** { *; }
#-dontwarn com.google.firebase.**
#-dontwarn com.google.android.gms.**

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
#-keep class dev.chrisbanes.haze.** { *; }
#-dontwarn dev.chrisbanes.haze.**

# ============================================================
# Navigation3
# ============================================================
#-keep class * implements androidx.navigation3.runtime.NavKey { *; }
#-keep @kotlinx.serialization.Serializable class * implements androidx.navigation3.runtime.NavKey { *; }
#-dontwarn androidx.navigation3.**

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
#-keep class com.github.jakepurple13.** { *; }
#-dontwarn com.github.jakepurple13.**

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
#-keep class io.github.jan.supabase.** { *; }
#-dontwarn io.github.jan.supabase.**

# ============================================================
# R8 missing classes (transitive deps not present on Android;
# see mangaworld/build/outputs/mapping/noFirebaseBeta/missing_rules.txt)
# ============================================================
-dontwarn io.opentelemetry.api.incubator.metrics.ExtendedDoubleHistogram
-dontwarn io.opentelemetry.api.incubator.metrics.ExtendedDoubleHistogramBuilder
-dontwarn io.opentelemetry.api.incubator.metrics.ExtendedLongCounter
-dontwarn io.opentelemetry.api.incubator.metrics.ExtendedLongCounterBuilder
-dontwarn java.awt.geom.Rectangle2D
-dontwarn javax.imageio.event.IIOWriteWarningListener

# ============================================================
# Preserve stack traces
# ============================================================
#-keepattributes SourceFile,LineNumberTable
#-renamesourcefileattribute SourceFile

-keep,allowoptimization class eu.kanade.**
-keep,allowoptimization class tachiyomi.**
-keep,allowoptimization class mihon.**

# Keep common dependencies used in extensions
-keep,allowoptimization class androidx.preference.** { public protected *; }
-keep,allowoptimization class kotlin.** { public protected *; }
-keep,allowoptimization class kotlinx.coroutines.** { public protected *; }
-keep,allowoptimization class kotlinx.serialization.** { public protected *; }
-keep,allowoptimization class kotlin.time.** { public protected *; }
-keep,allowoptimization class okhttp3.** { public protected *; }
-keep,allowoptimization class okio.** { public protected *; }
-keep,allowoptimization class org.jsoup.** { public protected *; }
-keep,allowoptimization class rx.** { public protected *; }
-keep,allowoptimization class app.cash.quickjs.** { public protected *; }
-keep,allowoptimization class uy.kohesive.injekt.** { public protected *; }
-keep,allowoptimization class com.squareup.zstd.** { public protected *; }

# From extensions-lib
-keep,allowoptimization class eu.kanade.tachiyomi.network.interceptor.RateLimitInterceptorKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.network.interceptor.SpecificHostRateLimitInterceptorKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.network.NetworkHelper { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.network.OkHttpExtensionsKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.network.RequestsKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.AppInfo { public protected *; }

-keepclassmembers class * implements java.io.Serializable {
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

##---------------Begin: proguard configuration for RxJava 1.x  ----------
-dontwarn sun.misc.**

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

-dontnote rx.internal.util.PlatformDependent
##---------------End: proguard configuration for RxJava 1.x  ----------

##---------------Begin: proguard configuration for okhttp  ----------
-keepclasseswithmembers class okhttp3.MultipartBody$Builder { *; }
##---------------End: proguard configuration for okhttp  ----------

##---------------Begin: proguard configuration for kotlinx.serialization  ----------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.** # core serialization annotations

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class eu.kanade.**$$serializer { *; }
-keepclassmembers class eu.kanade.** {
    *** Companion;
}
-keepclasseswithmembers class eu.kanade.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.** {
    <methods>;
}
##---------------End: proguard configuration for kotlinx.serialization  ----------

# XmlUtil
-keep public enum nl.adaptivity.xmlutil.EventType { *; }

# Firebase
-keep class com.google.firebase.installations.** { *; }
-keep interface com.google.firebase.installations.** { *; }

# PackageInstaller broadcast receiver — instantiated by the OS via manifest registration
#-keep class com.programmersbox.kmpuiviews.receivers.PackageInstallReceiver { public <init>(); }

-assumenosideeffects class * {
    @com.programmersbox.showcase.annotations.ShowcaseComponent <methods>;
}