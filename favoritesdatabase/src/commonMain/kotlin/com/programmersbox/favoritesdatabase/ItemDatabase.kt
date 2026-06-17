package com.programmersbox.favoritesdatabase

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [DbModel::class, ChapterWatched::class, NotificationItem::class, SourceOrder::class, IncognitoSource::class],
    version = 7,
    exportSchema = true,
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
        ),
    ],
)
abstract class ItemDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE `Notifications` (`id` INTEGER NOT NULL, `url` TEXT NOT NULL, `summaryText` TEXT NOT NULL, `notiTitle` TEXT NOT NULL, `notiPicture` TEXT, `source` TEXT NOT NULL, `contentTitle` TEXT NOT NULL, PRIMARY KEY(`url`))")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `IncognitoSourceTable` (`source` TEXT NOT NULL, `name` TEXT NOT NULL, `isIncognito` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`source`))")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override suspend fun migrate(connection: SQLiteConnection) {
                listOf("FavoriteItem", "ChapterWatched", "Notifications", "SourceOrder", "IncognitoSourceTable").forEach { table ->
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `supabase_id` TEXT DEFAULT ''")
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
                }
            }
        }

        fun getInstance(databaseBuilder: DatabaseBuilder): ItemDatabase = databaseBuilder
            .build<ItemDatabase>("favoriteItems.db")
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_5_6)
            .addMigrations(MIGRATION_6_7)
            .build()
    }
}