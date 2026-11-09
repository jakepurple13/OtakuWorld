package com.programmersbox.favoritesdatabase

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [DbModel::class, DbModelFts::class, ChapterWatched::class, NotificationItem::class, SourceOrder::class, IncognitoSource::class, SkippedUpdateSource::class],
    version = 10,
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
@ConstructedBy(FavoritesDatabaseConstructor::class)
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

        // Creates the FTS5 index and fills it from existing favorites; Room re-creates
        // the content sync triggers once migrations finish.
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `favorites_fts` USING FTS5(`title`, `description`, tokenize=`unicode61 remove_diacritics 2`, content=`FavoriteItem`, prefix=`2 3`)")
                connection.execSQL("INSERT INTO favorites_fts(favorites_fts) VALUES('rebuild')")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `SkippedUpdateSource` (`source` TEXT NOT NULL, PRIMARY KEY(`source`))")
            }
        }

        // SkippedUpdateSource now holds extension package names instead of per-language service names.
        // Old rows can't be mapped without the installed sources, so every source starts checked again.
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DELETE FROM `SkippedUpdateSource`")
            }
        }

        internal val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)

        fun getInstance(databaseBuilder: DatabaseBuilder): ItemDatabase = databaseBuilder
            .build<ItemDatabase>("favoriteItems.db")
            .addMigrations(*MIGRATIONS)
            .build()
    }
}