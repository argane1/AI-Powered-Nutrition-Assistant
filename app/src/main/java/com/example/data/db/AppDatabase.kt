package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface MealRecordDao {
    @Query("SELECT * FROM meal_records ORDER BY timestamp DESC")
    fun getAllMealRecords(): Flow<List<MealRecord>>

    @Query("SELECT * FROM meal_records WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getMealRecordsBetween(start: Long, end: Long): Flow<List<MealRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealRecord(record: MealRecord): Long

    @Update
    suspend fun updateMealRecord(record: MealRecord)

    @Delete
    suspend fun deleteMealRecord(record: MealRecord)
}

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getWaterLogsBetween(start: Long, end: Long): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLog): Long

    @Delete
    suspend fun deleteWaterLog(log: WaterLog)
}

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC")
    fun getAllWeightLogs(): Flow<List<WeightLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: WeightLog): Long

    @Delete
    suspend fun deleteWeightLog(log: WeightLog)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileSync(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)
}

@Database(
    entities = [MealRecord::class, WaterLog::class, WeightLog::class, UserProfile::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealRecordDao(): MealRecordDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "caloriesnap_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
