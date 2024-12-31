package com.programmersbox.uiviews.utils.blurhash

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [BlurHashItem::class],
    version = 1,
)
abstract class BlurHashDatabase : RoomDatabase() {

    abstract fun blurDao(): BlurHashDao

    companion object {

        @Volatile
        private var INSTANCE: BlurHashDatabase? = null

        fun getInstance(context: Context): BlurHashDatabase =
            INSTANCE ?: synchronized(this) { INSTANCE ?: buildDatabase(context).also { INSTANCE = it } }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, BlurHashDatabase::class.java, "blurhash.db")
                .build()
    }
}

@Dao
interface BlurHashDao {

    @Query("SELECT * FROM BlurHashItem")
    fun getAllHashes(): Flow<List<BlurHashItem>>

    @Query("SELECT * FROM BlurHashItem WHERE url=:url")
    fun getHash(url: String?): Flow<BlurHashItem?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHash(model: BlurHashItem)

    @Delete
    suspend fun deleteHash(model: BlurHashItem)
}

@Entity(tableName = "BlurHashItem")
data class BlurHashItem(
    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "blur_hash")
    val blurHash: String,
)