# CloudStream .cs3 plugins are loaded at runtime and bind to these classes by name, so R8 in the
# consuming app must not strip, rename or reshape them.
-keep class com.lagradost.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.jsoup.** { *; }
-keep class okhttp3.** { *; }
-keep class androidx.appcompat.** { *; }
-keep class androidx.fragment.** { *; }
-keep class com.google.android.material.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn com.fasterxml.jackson.**
-keep class com.google.gson.** { *; }
