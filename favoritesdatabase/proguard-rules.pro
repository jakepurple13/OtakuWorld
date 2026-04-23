# Kotlin
-keep class kotlin.Metadata { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
