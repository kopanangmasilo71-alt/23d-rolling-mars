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
    val characterColor: String = "Classic Blue",
    val difficultyMode: String = "Standard",
    val maxCombo: Int = 1,
    val sectorReached: String = "Sector 1"
)

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val totalPoints: Int = 800, // Starting welcome balance to encourage immediate unlocking
    val equippedCharacterId: String = "vanguard",
    val unlockedCharacterIds: String = "vanguard" // Comma-separated IDs: "vanguard,titan"
)

data class CareerStats(
    val totalScore: Long = 0L,
    val totalDistance: Long = 0L,
    val totalDodged: Long = 0L,
    val maxComboEver: Int = 1,
    val totalGamesPlayed: Int = 0
)

@Dao
interface GameRecordDao {
    @Query("SELECT * FROM game_records ORDER BY score DESC LIMIT 50")
    fun getAllRecords(): Flow<List<GameRecord>>

    @Query("SELECT * FROM game_records ORDER BY score DESC LIMIT 10")
    fun getTop10Records(): Flow<List<GameRecord>>

    @Query("SELECT * FROM game_records ORDER BY score DESC LIMIT 1")
    fun getTopRecord(): Flow<GameRecord?>

    @Query("SELECT COALESCE(SUM(score), 0) as totalScore, COALESCE(SUM(distanceMeters), 0) as totalDistance, COALESCE(SUM(ballsDodged), 0) as totalDodged, COALESCE(MAX(maxCombo), 1) as maxComboEver, COUNT(*) as totalGamesPlayed FROM game_records")
    fun getCareerStats(): Flow<CareerStats>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: GameRecord): Long

    @Query("DELETE FROM game_records")
    suspend fun clearAllRecords()
}

@Dao
interface PlayerProfileDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun getProfile(): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileSync(): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: PlayerProfile)
}

@Database(entities = [GameRecord::class, PlayerProfile::class], version = 3, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameRecordDao(): GameRecordDao
    abstract fun playerProfileDao(): PlayerProfileDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "roll_runner.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class GameRecordRepository(
    private val dao: GameRecordDao,
    private val profileDao: PlayerProfileDao
) {
    val allRecords: Flow<List<GameRecord>> = dao.getAllRecords()
    val top10Records: Flow<List<GameRecord>> = dao.getTop10Records()
    val topRecord: Flow<GameRecord?> = dao.getTopRecord()
    val careerStats: Flow<CareerStats> = dao.getCareerStats()
    val playerProfile: Flow<PlayerProfile?> = profileDao.getProfile()

    suspend fun getOrCreateProfile(): PlayerProfile {
        val existing = profileDao.getProfileSync()
        if (existing != null) return existing
        val defaultProfile = PlayerProfile()
        profileDao.saveProfile(defaultProfile)
        return defaultProfile
    }

    suspend fun addPoints(amount: Int) {
        val profile = getOrCreateProfile()
        val newPoints = (profile.totalPoints + amount).coerceAtLeast(0)
        profileDao.saveProfile(profile.copy(totalPoints = newPoints))
    }

    suspend fun unlockCharacter(characterId: String, cost: Int): Boolean {
        val profile = getOrCreateProfile()
        val unlockedList = profile.unlockedCharacterIds.split(",").map { it.trim().lowercase() }.toMutableSet()
        val lowerId = characterId.lowercase()
        if (unlockedList.contains(lowerId)) {
            // Already owned, just equip!
            profileDao.saveProfile(profile.copy(equippedCharacterId = lowerId))
            return true
        }
        if (profile.totalPoints < cost) {
            return false // Not enough points
        }
        unlockedList.add(lowerId)
        val newPoints = profile.totalPoints - cost
        profileDao.saveProfile(
            profile.copy(
                totalPoints = newPoints,
                equippedCharacterId = lowerId,
                unlockedCharacterIds = unlockedList.joinToString(",")
            )
        )
        return true
    }

    suspend fun equipCharacter(characterId: String) {
        val profile = getOrCreateProfile()
        profileDao.saveProfile(profile.copy(equippedCharacterId = characterId.lowercase()))
    }

    suspend fun saveRecord(record: GameRecord): Long = dao.insertRecord(record)
    suspend fun clearRecords() = dao.clearAllRecords()
}
