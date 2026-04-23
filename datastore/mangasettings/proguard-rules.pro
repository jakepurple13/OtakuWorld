# Kotlin
-keep class kotlin.Metadata { *; }

# DataStore / Protobuf
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.protobuf.**