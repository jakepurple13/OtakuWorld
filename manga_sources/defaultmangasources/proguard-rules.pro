# Kotlin
-keep class kotlin.Metadata { *; }

# KotlinX Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembernames class io.ktor.** { *; }
-dontwarn io.ktor.**

# JSoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
