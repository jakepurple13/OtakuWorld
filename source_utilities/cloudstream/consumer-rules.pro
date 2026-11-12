# CloudStream .cs3 plugins are loaded at runtime and bind to these classes by name, so R8 in the
# consuming app must not strip, rename or reshape them.
-keep class com.lagradost.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.jsoup.** { *; }
-keep class okhttp3.** { *; }
# Plugins subclass and call the public UI API (settings fragments, dialogs); only that surface needs
# to survive, and allowoptimization lets R8 still optimize these shared UI libraries.
-keep,allowoptimization class androidx.appcompat.** { public protected *; }
-keep,allowoptimization class androidx.fragment.** { public protected *; }
-keep,allowoptimization class com.google.android.material.** { public protected *; }
-dontwarn org.mozilla.javascript.**
-dontwarn com.fasterxml.jackson.**
-keep class com.google.gson.** { *; }
