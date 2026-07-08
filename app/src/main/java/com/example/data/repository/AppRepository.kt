package com.example.data.repository

import com.example.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AppRepository(private val database: AppDatabase) {

    private val mealDao = database.mealRecordDao()
    private val waterDao = database.waterLogDao()
    private val weightDao = database.weightLogDao()
    private val profileDao = database.userProfileDao()

    // --- MEALS ---
    fun getAllMeals(): Flow<List<MealRecord>> = mealDao.getAllMealRecords().flowOn(Dispatchers.IO)

    fun getMealsBetween(start: Long, end: Long): Flow<List<MealRecord>> = 
        mealDao.getMealRecordsBetween(start, end).flowOn(Dispatchers.IO)

    suspend fun insertMeal(record: MealRecord): Long = withContext(Dispatchers.IO) {
        mealDao.insertMealRecord(record)
    }

    suspend fun updateMeal(record: MealRecord) = withContext(Dispatchers.IO) {
        mealDao.updateMealRecord(record)
    }

    suspend fun deleteMeal(record: MealRecord) = withContext(Dispatchers.IO) {
        mealDao.deleteMealRecord(record)
    }

    // --- WATER ---
    fun getWaterLogsBetween(start: Long, end: Long): Flow<List<WaterLog>> =
        waterDao.getWaterLogsBetween(start, end).flowOn(Dispatchers.IO)

    suspend fun insertWaterLog(log: WaterLog): Long = withContext(Dispatchers.IO) {
        waterDao.insertWaterLog(log)
    }

    suspend fun deleteWaterLog(log: WaterLog) = withContext(Dispatchers.IO) {
        waterDao.deleteWaterLog(log)
    }

    // --- WEIGHT ---
    fun getAllWeightLogs(): Flow<List<WeightLog>> = weightDao.getAllWeightLogs().flowOn(Dispatchers.IO)

    suspend fun insertWeightLog(log: WeightLog): Long = withContext(Dispatchers.IO) {
        weightDao.insertWeightLog(log)
    }

    suspend fun deleteWeightLog(log: WeightLog) = withContext(Dispatchers.IO) {
        weightDao.deleteWeightLog(log)
    }

    // --- PROFILE ---
    fun getUserProfile(): Flow<UserProfile?> = profileDao.getUserProfile().flowOn(Dispatchers.IO)

    suspend fun ensureProfileExists() = withContext(Dispatchers.IO) {
        val currentProfile = profileDao.getUserProfileSync()
        if (currentProfile == null) {
            profileDao.insertUserProfile(UserProfile())
        }
    }

    suspend fun updateProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        profileDao.updateUserProfile(profile)
    }
}
