package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "meal_records")
data class MealRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mealName: String,
    val imageUrl: String? = null,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val sugar: Float,
    val sodium: Float,
    val timestamp: Long,
    val portionMultiplier: Float = 1.0f,
    val confidenceScore: Float = 0.90f,
    val healthRating: Int = 4, // 1 to 5 stars
    val portionExplanation: String = "",
    val itemsJson: String = "[]", // JSON string of DetectedFoodItem
    val alternativesJson: String = "[]", // JSON string of alternatives list
    val isFavorite: Boolean = false
)

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amountMl: Int,
    val timestamp: Long
)

@Entity(tableName = "weight_logs")
data class WeightLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weightKg: Float,
    val timestamp: Long
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Single row constraint
    val name: String = "Alex",
    val age: Int = 28,
    val heightCm: Float = 175.0f,
    val weightKg: Float = 72.0f,
    val gender: String = "Non-binary",
    val activityLevel: String = "Moderate", // Sedentary, Light, Moderate, Active, Very Active
    val fitnessGoal: String = "Maintain Weight", // Lose Weight, Maintain Weight, Gain Muscle
    val dietaryPreference: String = "None",
    val allergies: String = "None",
    val dailyCalorieGoal: Int = 2000,
    val waterGoalMl: Int = 2500
)

data class DetectedFoodItem(
    val name: String,
    val calories: Int,
    val confidence: Float,
    val portion: String
)

class AppConverters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, DetectedFoodItem::class.java)
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val itemAdapter = moshi.adapter<List<DetectedFoodItem>>(listType)
    private val stringAdapter = moshi.adapter<List<String>>(stringListType)

    @TypeConverter
    fun fromDetectedItems(list: List<DetectedFoodItem>?): String {
        return itemAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun toDetectedItems(json: String?): List<DetectedFoodItem> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            itemAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return stringAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            stringAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
