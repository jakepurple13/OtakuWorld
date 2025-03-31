package com.programmersbox.favoritesdatabase

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DbModel::class, ChapterWatched::class, NotificationItem::class, SourceOrder::class],
    version = 5,
    autoMigrations = [
        AutoMigration(
            from = 2,
            to = 3
        ),
        AutoMigration(
            from = 3,
            to = 4
        ),
        AutoMigration(
            from = 4,
            to = 5
        )
    ]
)
@TypeConverters(ItemConverters::class)
abstract class ItemDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `Notifications` (`id` INTEGER NOT NULL, `url` TEXT NOT NULL, `summaryText` TEXT NOT NULL, `notiTitle` TEXT NOT NULL, `notiPicture` TEXT, `source` TEXT NOT NULL, `contentTitle` TEXT NOT NULL, PRIMARY KEY(`url`))")
            }
        }

        @Volatile
        private var INSTANCE: ItemDatabase? = null

        fun getInstance(context: Context): ItemDatabase =
            INSTANCE ?: synchronized(this) { INSTANCE ?: buildDatabase(context).also { INSTANCE = it } }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, ItemDatabase::class.java, "favoriteItems.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}