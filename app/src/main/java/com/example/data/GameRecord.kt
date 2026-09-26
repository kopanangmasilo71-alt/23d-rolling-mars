package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_records")
data class GameRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val score: Int,
    val distanceMeters: Int,
    val ballsDodged: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val characterColor: String = "Classic Blue"
)

@Dao
interface GameRecordDao {
    @Query("SELECT * FROM game_records ORDER BY score DESC LIMIT 50")
    fun getAllRecords(): Flow<List<GameRecord>>

    @Query("SELECT * FROM game_records ORDER BY score DESC LIMIT 1")
    fun getTopRecord(): Flow<GameRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: GameRecord): Long

    @Query("DELETE FROM game_records")
    suspend fun clearAllRecords()
}

@Database(entities = [GameRecord::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameRecordDao(): GameRecordDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "roll_runner.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class GameRecordRepository(private val dao: GameRecordDao) {
    val allRecords: Flow<List<GameRecord>> = dao.getAllRecords()
    val topRecord: Flow<GameRecord?> = dao.getTopRecord()

    suspend fun saveRecord(record: GameRecord): Long = dao.insertRecord(record)
    suspend fun clearRecords() = dao.clearAllRecords()
}
