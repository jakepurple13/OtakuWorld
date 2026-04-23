# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}