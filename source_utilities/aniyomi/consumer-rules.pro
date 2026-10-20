# Aniyomi extension APKs are loaded at runtime and bind to these classes by name, so R8 in the
# consuming app must not strip, rename or reshape them.
-keep class eu.kanade.tachiyomi.** { *; }
-keep class tachiyomi.** { *; }
-keep class mihon.** { *; }
-keep class app.cash.quickjs.** { *; }
-keep class uy.kohesive.injekt.** { *; }
-keep class fi.iki.elonen.** { *; }
-dontwarn rx.**
-dontwarn fi.iki.elonen.**

# If the consuming app enables minify, extensions also bind to these runtime libraries by name.
# Only their public API needs to survive; allowoptimization lets R8 still optimize bodies.
-keep,allowoptimization class rx.** { public protected *; }
-keep,allowoptimization class org.jsoup.** { public protected *; }
-keep,allowoptimization class okhttp3.** { public protected *; }
-keep,allowoptimization class kotlinx.serialization.** { public protected *; }
-keep,allowoptimization class !kotlin.reflect.jvm.internal.**,kotlin.** { public protected *; }
-keep,allowoptimization class kotlinx.coroutines.** { public protected *; }
-keep,allowoptimization class okio.** { public protected *; }
-keep,allowoptimization class androidx.preference.** { public protected *; }
