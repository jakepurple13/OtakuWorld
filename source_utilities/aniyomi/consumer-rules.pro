# Aniyomi extension APKs are loaded at runtime and bind to these classes by name, so R8 in the
# consuming app must not strip, rename or reshape them.
-keep class eu.kanade.tachiyomi.** { *; }
-keep class tachiyomi.** { *; }
-keep class mihon.** { *; }
-keep class app.cash.quickjs.** { *; }
-keep class uy.kohesive.injekt.** { *; }
-keep class rx.** { *; }
-keep class fi.iki.elonen.** { *; }
-keep class org.jsoup.** { *; }
-keep class okhttp3.** { *; }
-keep class kotlinx.serialization.** { *; }
-dontwarn rx.**
-dontwarn fi.iki.elonen.**

# If the consuming app enables minify, extensions also bind to these runtime libraries by name.
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class okio.** { *; }
-keep class androidx.preference.** { *; }
