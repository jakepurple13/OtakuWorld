# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# PackageInstaller broadcast receiver — instantiated by the OS via manifest registration
-keep class com.programmersbox.kmpuiviews.receivers.PackageInstallReceiver { public <init>(); }

# ============================================================
# Extension / source-loading contract (kmpmodels)
# External plugins implement these at runtime, so they must survive
# shrinking/obfuscation with their original names.
# ============================================================
-keep interface com.programmersbox.kmpmodels.** { *; }
-keep class com.programmersbox.kmpmodels.** { *; }
-keep class * implements com.programmersbox.kmpmodels.KmpApiService { *; }
-dontwarn com.programmersbox.kmpmodels.**
-keep class com.programmersbox.models.** { *; }
-keep class com.programmersbox.kmpextensionloader.** { *; }
-keep class com.programmersbox.source_utilities.** { *; }

-keepclassmembers class androidx.core.content.ContextCompat {
    public static java.util.concurrent.Executor getMainExecutor(android.content.Context);
}

# ============================================================
# Koin
# ============================================================
-keep class org.koin.** { *; }
-keepnames class * {
    @org.koin.core.annotation.* *;
}
-dontwarn org.koin.**

# ============================================================
# Ktor
# ============================================================
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

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
# DataStore / Protobuf
# ============================================================
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.protobuf.**

# ============================================================
# AboutLibraries / Iconics
# ============================================================
-keep class com.mikepenz.aboutlibraries.** { *; }
-dontwarn com.mikepenz.aboutlibraries.**
-keep class com.mikepenz.iconics.** { *; }
-dontwarn com.mikepenz.iconics.**

# ============================================================
# kotlinx.serialization
# ============================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-assumenosideeffects class * {
    @com.programmersbox.showcase.annotations.ShowcaseComponent <methods>;
}

# ============================================================
# R8 missing classes (transitive deps not present on Android;
# see animeworld/build/outputs/mapping/noFirebaseRelease/missing_rules.txt)
# ============================================================
-dontwarn io.opentelemetry.api.incubator.metrics.ExtendedDoubleHistogram
-dontwarn io.opentelemetry.api.incubator.metrics.ExtendedDoubleHistogramBuilder
-dontwarn io.opentelemetry.api.incubator.metrics.ExtendedLongCounter
-dontwarn io.opentelemetry.api.incubator.metrics.ExtendedLongCounterBuilder
-dontwarn java.awt.geom.Rectangle2D
-dontwarn javax.imageio.event.IIOWriteWarningListener
