package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.api.MealAnalysisResult
import com.example.data.db.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class ChatMessage(
    val id: String,
    val text: String,
    val sender: String, // "user" or "coach"
    val timestamp: Long = System.currentTimeMillis()
)

data class FastingState(
    val isFasting: Boolean = false,
    val durationHours: Int = 16,
    val startTime: Long = 0L
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database)

    // Current screen navigation state
    private val _currentTab = MutableStateFlow("home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // --- State Streams ---
    val userProfile: StateFlow<UserProfile> = repository.getUserProfile()
        .filterNotNull<UserProfile>()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    // Today's meal records
    private val _todayMeals = MutableStateFlow<List<MealRecord>>(emptyList())
    val todayMeals: StateFlow<List<MealRecord>> = _todayMeals.asStateFlow()

    // Today's water log entries
    private val _todayWaterLogs = MutableStateFlow<List<WaterLog>>(emptyList())
    val todayWaterLogs: StateFlow<List<WaterLog>> = _todayWaterLogs.asStateFlow()

    // All weight records
    val weightLogs: StateFlow<List<WeightLog>> = repository.getAllWeightLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All meal history for history logs
    val allMeals: StateFlow<List<MealRecord>> = repository.getAllMeals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- AI Feature States ---
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _activeAnalysis = MutableStateFlow<MealAnalysisResult?>(null)
    val activeAnalysis: StateFlow<MealAnalysisResult?> = _activeAnalysis.asStateFlow()

    private val _activeMealPhoto = MutableStateFlow<Bitmap?>(null)
    val activeMealPhoto: StateFlow<Bitmap?> = _activeMealPhoto.asStateFlow()

    // Sliding scale portion adjustment (multiplier 0.25x to 3.0x)
    private val _portionMultiplier = MutableStateFlow(1.0f)
    val portionMultiplier: StateFlow<Float> = _portionMultiplier.asStateFlow()

    // --- AI Coach States ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                text = "Hello! I am your CalorieSnap AI Coach. Scan a meal or ask me any nutrition and fitness questions to get personalized, scannable advice. How can I help you smash your goals today?",
                sender = "coach"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isCoachTyping = MutableStateFlow(false)
    val isCoachTyping: StateFlow<Boolean> = _isCoachTyping.asStateFlow()

    // --- Fasting States ---
    private val _fastingState = MutableStateFlow(FastingState())
    val fastingState: StateFlow<FastingState> = _fastingState.asStateFlow()

    init {
        viewModelScope.launch {
            // Ensure a profile exists
            repository.ensureProfileExists()
            // Listen for changes in today's data
            observeTodayData()
        }
    }

    fun navigateTo(tab: String) {
        _currentTab.value = tab
    }

    private fun observeTodayData() {
        viewModelScope.launch {
            val (startOfDay, endOfDay) = getTodayRange()

            // Observe today's meals
            repository.getMealsBetween(startOfDay, endOfDay).collect { meals ->
                _todayMeals.value = meals
            }
        }
        viewModelScope.launch {
            val (startOfDay, endOfDay) = getTodayRange()

            // Observe today's water
            repository.getWaterLogsBetween(startOfDay, endOfDay).collect { water ->
                _todayWaterLogs.value = water
            }
        }
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }

    // --- MEALS ACTION ---
    fun analyzeMealPhoto(bitmap: Bitmap) {
        _isAnalyzing.value = true
        _activeMealPhoto.value = bitmap
        _portionMultiplier.value = 1.0f
        viewModelScope.launch {
            try {
                val result = GeminiClient.analyzeFoodPhoto(bitmap)
                _activeAnalysis.value = result
                _currentTab.value = "food_details"
            } catch (e: Exception) {
                Log.e("MainViewModel", "analyzeMealPhoto error: ${e.message}")
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun analyzeMealText(query: String) {
        _isAnalyzing.value = true
        _activeMealPhoto.value = null
        _portionMultiplier.value = 1.0f
        viewModelScope.launch {
            try {
                val result = GeminiClient.analyzeTextQuery(query)
                _activeAnalysis.value = result
                _currentTab.value = "food_details"
            } catch (e: Exception) {
                Log.e("MainViewModel", "analyzeMealText error: ${e.message}")
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun updatePortionMultiplier(multiplier: Float) {
        _portionMultiplier.value = multiplier
    }

    fun saveActiveMealToDiary(imagePath: String? = null) {
        val analysis = _activeAnalysis.value ?: return
        val multiplier = _portionMultiplier.value

        viewModelScope.launch {
            val converters = AppConverters()
            val record = MealRecord(
                mealName = analysis.mealName,
                imageUrl = imagePath,
                calories = (analysis.totalCalories * multiplier).toInt(),
                protein = analysis.proteinGrams * multiplier,
                carbs = analysis.carbsGrams * multiplier,
                fat = analysis.fatGrams * multiplier,
                fiber = analysis.fiberGrams * multiplier,
                sugar = analysis.sugarGrams * multiplier,
                sodium = analysis.sodiumMilligrams * multiplier,
                timestamp = System.currentTimeMillis(),
                portionMultiplier = multiplier,
                confidenceScore = analysis.confidenceScore,
                healthRating = analysis.healthRating,
                portionExplanation = analysis.portionExplanation,
                itemsJson = converters.fromDetectedItems(analysis.detectedItems),
                alternativesJson = converters.fromStringList(analysis.suggestedAlternatives)
            )

            repository.insertMeal(record)
            _activeAnalysis.value = null
            _activeMealPhoto.value = null
            _currentTab.value = "home"
        }
    }

    fun discardActiveMeal() {
        _activeAnalysis.value = null
        _activeMealPhoto.value = null
        _currentTab.value = "home"
    }

    fun deleteMeal(record: MealRecord) {
        viewModelScope.launch {
            repository.deleteMeal(record)
        }
    }

    fun toggleMealFavorite(record: MealRecord) {
        viewModelScope.launch {
            val updated = record.copy(isFavorite = !record.isFavorite)
            repository.updateMeal(updated)
        }
    }

    // --- WATER ACTIONS ---
    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            repository.insertWaterLog(WaterLog(amountMl = amountMl, timestamp = System.currentTimeMillis()))
        }
    }

    fun deleteWaterLog(log: WaterLog) {
        viewModelScope.launch {
            repository.deleteWaterLog(log)
        }
    }

    // --- WEIGHT ACTIONS ---
    fun addWeight(weightKg: Float) {
        viewModelScope.launch {
            repository.insertWeightLog(WeightLog(weightKg = weightKg, timestamp = System.currentTimeMillis()))
            // Also update the weight inside the current profile
            val currentProfile = userProfile.value
            repository.updateProfile(currentProfile.copy(weightKg = weightKg))
        }
    }

    fun deleteWeight(log: WeightLog) {
        viewModelScope.launch {
            repository.deleteWeightLog(log)
        }
    }

    // --- PROFILE ACTIONS ---
    fun updateProfile(
        name: String,
        age: Int,
        heightCm: Float,
        weightKg: Float,
        gender: String,
        activityLevel: String,
        fitnessGoal: String,
        dietaryPreference: String,
        allergies: String,
        dailyCalorieGoal: Int,
        waterGoalMl: Int
    ) {
        viewModelScope.launch {
            val updated = UserProfile(
                id = 1,
                name = name,
                age = age,
                heightCm = heightCm,
                weightKg = weightKg,
                gender = gender,
                activityLevel = activityLevel,
                fitnessGoal = fitnessGoal,
                dietaryPreference = dietaryPreference,
                allergies = allergies,
                dailyCalorieGoal = dailyCalorieGoal,
                waterGoalMl = waterGoalMl
            )
            repository.updateProfile(updated)
        }
    }

    // --- AI COACH ACTIONS ---
    fun sendCoachMessage(userText: String) {
        if (userText.trim().isEmpty()) return

        val userMsg = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            text = userText,
            sender = "user"
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isCoachTyping.value = true

        viewModelScope.launch {
            try {
                val profile = userProfile.value
                val profileString = "Name: ${profile.name}, Age: ${profile.age}, Weight: ${profile.weightKg}kg, Height: ${profile.heightCm}cm, Goal: ${profile.fitnessGoal}, Dietary: ${profile.dietaryPreference}, Allergies: ${profile.allergies}"

                val meals = _todayMeals.value
                val water = _todayWaterLogs.value.sumOf { it.amountMl }
                val totalCals = meals.sumOf { it.calories }
                val totalProtein = meals.sumOf { it.protein.toDouble() }
                val totalCarbs = meals.sumOf { it.carbs.toDouble() }
                val totalFat = meals.sumOf { it.fat.toDouble() }
                val todayStats = "Calories Consumed: $totalCals kcal, Protein: ${totalProtein.toInt()}g, Carbs: ${totalCarbs.toInt()}g, Fat: ${totalFat.toInt()}g, Water Drunk: $water ml / ${profile.waterGoalMl} ml"

                val coachText = GeminiClient.getAICoachResponse(userText, profileString, todayStats)

                val coachMsg = ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    text = coachText,
                    sender = "coach"
                )
                _chatMessages.value = _chatMessages.value + coachMsg
            } catch (e: Exception) {
                Log.e("MainViewModel", "sendCoachMessage error: ${e.message}")
            } finally {
                _isCoachTyping.value = false
            }
        }
    }

    fun clearCoachHistory() {
        _chatMessages.value = listOf(
            ChatMessage(
                id = "welcome",
                text = "Hello! I am your CalorieSnap AI Coach. Ask me anything to get custom fitness and nutrition advice!",
                sender = "coach"
            )
        )
    }

    // --- FASTING ACTIONS ---
    fun startFasting(hours: Int) {
        _fastingState.value = FastingState(
            isFasting = true,
            durationHours = hours,
            startTime = System.currentTimeMillis()
        )
    }

    fun stopFasting() {
        _fastingState.value = FastingState(isFasting = false)
    }
}
