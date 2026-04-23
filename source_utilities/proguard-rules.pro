# Kotlin
-keep class kotlin.Metadata { *; }

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembernames class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# JSoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
