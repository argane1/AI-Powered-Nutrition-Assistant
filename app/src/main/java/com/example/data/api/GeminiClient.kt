package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.db.DetectedFoodItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Models for Gemini REST API ---

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

// --- Interface for Retrofit ---
interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Parsed Meal Analysis Response ---
data class MealAnalysisResult(
    val mealName: String,
    val totalCalories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val fiberGrams: Float,
    val sugarGrams: Float,
    val sodiumMilligrams: Float,
    val confidenceScore: Float,
    val healthRating: Int, // 1 to 5
    val portionExplanation: String,
    val detectedItems: List<DetectedFoodItem>,
    val suggestedAlternatives: List<String>
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    // Helper to check if API Key is available
    fun isApiKeyAvailable(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrEmpty() && key != "MY_GEMINI_API_KEY" && key != "placeholder"
    }

    // Helper to compress Bitmap to base64
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Instantly analyzes a food photo using Gemini, falling back to local simulation if no key or error.
     */
    suspend fun analyzeFoodPhoto(bitmap: Bitmap): MealAnalysisResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasRealKey = isApiKeyAvailable()

        Log.d(TAG, "analyzeFoodPhoto: hasRealKey=$hasRealKey")

        if (!hasRealKey) {
            return simulateAnalysis("Photo Analysis")
        }

        val prompt = """
            Analyze this food photo. Estimate total calories, macronutrients, micronutrients, portions, confidence score (0.0 to 1.0), and a health rating (1 to 5 stars).
            Detect individual items in the picture. Provide a suggested set of healthier alternatives.
            
            Return a JSON object conforming strictly to this format (without markdown code fences or other text, just raw JSON):
            {
              "mealName": "Descriptive meal name",
              "totalCalories": 480,
              "proteinGrams": 25.0,
              "carbsGrams": 42.5,
              "fatGrams": 14.0,
              "fiberGrams": 5.0,
              "sugarGrams": 6.5,
              "sodiumMilligrams": 540.0,
              "confidenceScore": 0.95,
              "healthRating": 4,
              "portionExplanation": "Portion size explanation here",
              "detectedItems": [
                {
                  "name": "Grilled Salmon",
                  "calories": 250,
                  "confidence": 0.96,
                  "portion": "150g"
                },
                {
                  "name": "Brown Rice",
                  "calories": 150,
                  "confidence": 0.92,
                  "portion": "1 cup"
                }
              ],
              "suggestedAlternatives": [
                "Reduce butter used in salmon seasoning",
                "Swap brown rice with quinoa for extra protein"
              ]
            }
        """.trimIndent()

        try {
            val base64Image = bitmap.toBase64()
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt),
                            GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.2f
                )
            )

            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d(TAG, "Raw Gemini Response: $jsonText")

            if (!jsonText.isNullOrEmpty()) {
                val adapter = moshi.adapter(MealAnalysisResult::class.java)
                return adapter.fromJson(jsonText) ?: throw Exception("JSON Parsing returned null")
            } else {
                throw Exception("Empty response text from Gemini")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error, falling back to offline simulation: ${e.message}", e)
            return simulateAnalysis("Photo Analysis (Local AI Fallback)")
        }
    }

    /**
     * Estimates nutritional information from a text query/search database/recipe or barcode.
     */
    suspend fun analyzeTextQuery(query: String): MealAnalysisResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasRealKey = isApiKeyAvailable()

        Log.d(TAG, "analyzeTextQuery: query=$query, hasRealKey=$hasRealKey")

        if (!hasRealKey) {
            return simulateAnalysis(query)
        }

        val prompt = """
            Analyze this food item or meal query: '$query'. Estimate calories and nutritional facts.
            Return a JSON object conforming strictly to this format:
            {
              "mealName": "Descriptive meal name based on query",
              "totalCalories": 350,
              "proteinGrams": 15.0,
              "carbsGrams": 40.0,
              "fatGrams": 8.0,
              "fiberGrams": 3.0,
              "sugarGrams": 5.0,
              "sodiumMilligrams": 300.0,
              "confidenceScore": 0.85,
              "healthRating": 3,
              "portionExplanation": "Standard serving size estimation",
              "detectedItems": [
                {
                  "name": "Food item name",
                  "calories": 350,
                  "confidence": 0.88,
                  "portion": "1 serving"
                }
              ],
              "suggestedAlternatives": [
                "Alternative suggestion 1"
              ]
            }
        """.trimIndent()

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.2f
                )
            )

            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d(TAG, "Raw Gemini Text Response: $jsonText")

            if (!jsonText.isNullOrEmpty()) {
                val adapter = moshi.adapter(MealAnalysisResult::class.java)
                return adapter.fromJson(jsonText) ?: throw Exception("JSON Parsing returned null")
            } else {
                throw Exception("Empty response text from Gemini")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini text query error, falling back to local simulation: ${e.message}", e)
            return simulateAnalysis(query)
        }
    }

    /**
     * Premium multi-meal recipe or voice logging analyzer.
     */
    suspend fun estimateRecipeCalories(recipeText: String): MealAnalysisResult {
        return analyzeTextQuery("Recipe: $recipeText")
    }

    /**
     * AI Coach conversational responder. Takes chat messages and user daily stats.
     */
    suspend fun getAICoachResponse(
        userMessage: String,
        userProfile: String,
        todayStats: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasRealKey = isApiKeyAvailable()

        if (!hasRealKey) {
            return simulateCoachResponse(userMessage)
        }

        val systemPrompt = """
            You are 'CalorieSnap AI Coach', a highly supportive, knowledgeable, and empathetic certified sports nutritionist and health coach.
            Your response must be extremely actionable, scannable, and encouraging. Use elegant bolding, bullet points, and positive tone. Keep your responses compact and easy to read.
            
            Current User Context:
            - Profile: $userProfile
            - Today's Nutrition Progress: $todayStats
        """.trimIndent()

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = userMessage))
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7f
                )
            )

            val response = apiService.generateContent(apiKey, request)
            return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I'm having a little trouble thinking of advice right now. Let's make sure we track our water and stick to the goals!"
        } catch (e: Exception) {
            Log.e(TAG, "Coach API error: ${e.message}", e)
            return simulateCoachResponse(userMessage)
        }
    }

    // --- Offline fallbacks for simulated offline mode ---

    private fun simulateAnalysis(query: String): MealAnalysisResult {
        val cleanQuery = query.trim().lowercase()
        return when {
            cleanQuery.contains("salmon") || cleanQuery.contains("fish") -> MealAnalysisResult(
                mealName = "Grilled Salmon & Asparagus",
                totalCalories = 420,
                proteinGrams = 34.0f,
                carbsGrams = 12.0f,
                fatGrams = 22.0f,
                fiberGrams = 4.0f,
                sugarGrams = 1.5f,
                sodiumMilligrams = 380.0f,
                confidenceScore = 0.96f,
                healthRating = 5,
                portionExplanation = "1 portion (150g grilled salmon fillet with 6 spears of asparagus and lemon juice)",
                detectedItems = listOf(
                    DetectedFoodItem("Grilled Atlantic Salmon", 280, 0.98f, "150g fillet"),
                    DetectedFoodItem("Steamed Green Asparagus", 40, 0.95f, "6 spears"),
                    DetectedFoodItem("Virgin Olive Oil (Seasoning)", 100, 0.85f, "1 tbsp")
                ),
                suggestedAlternatives = listOf(
                    "Reduce seasoning oil by half to save 50 calories of fat",
                    "Add 100g of sweet potato to increase complex fiber carbohydrates"
                )
            )
            cleanQuery.contains("pizza") || cleanQuery.contains("pepperoni") -> MealAnalysisResult(
                mealName = "Pepperoni Flatbread Pizza",
                totalCalories = 680,
                proteinGrams = 28.0f,
                carbsGrams = 78.0f,
                fatGrams = 26.0f,
                fiberGrams = 2.5f,
                sugarGrams = 4.0f,
                sodiumMilligrams = 950.0f,
                confidenceScore = 0.92f,
                healthRating = 2,
                portionExplanation = "2 slices of standard 12\" pepperoni thin crust pizza",
                detectedItems = listOf(
                    DetectedFoodItem("Thin Pizza Crust Base", 320, 0.95f, "2 slices"),
                    DetectedFoodItem("Mozzarella Cheese (Whole Milk)", 180, 0.90f, "50g"),
                    DetectedFoodItem("Pork Pepperoni Slices", 120, 0.94f, "8 slices"),
                    DetectedFoodItem("Tomato Marinara Sauce", 60, 0.88f, "0.25 cup")
                ),
                suggestedAlternatives = listOf(
                    "Swap for a cauliflower base to reduce carbs and gluten",
                    "Use turkey pepperoni and low-fat mozzarella to cut saturated fats by 40%"
                )
            )
            cleanQuery.contains("salad") || cleanQuery.contains("chicken") || cleanQuery.contains("caesar") -> MealAnalysisResult(
                mealName = "Chicken Caesar Salad",
                totalCalories = 380,
                proteinGrams = 26.0f,
                carbsGrams = 15.0f,
                fatGrams = 24.0f,
                fiberGrams = 3.0f,
                sugarGrams = 2.0f,
                sodiumMilligrams = 640.0f,
                confidenceScore = 0.94f,
                healthRating = 4,
                portionExplanation = "1 large bowl (100g skinless chicken breast, chopped romaine, parmesan and dressing)",
                detectedItems = listOf(
                    DetectedFoodItem("Grilled Chicken Breast", 160, 0.98f, "100g"),
                    DetectedFoodItem("Romaine Lettuce", 15, 0.97f, "2 cups"),
                    DetectedFoodItem("Creamy Caesar Dressing", 180, 0.88f, "2 tbsp"),
                    DetectedFoodItem("Croutons & Parmesan Cheese", 25, 0.91f, "1 tbsp")
                ),
                suggestedAlternatives = listOf(
                    "Swap Caesar dressing for a yogurt-based lemon-herb vinaigrette to save 120 kcal",
                    "Add cucumber or cherry tomatoes to double the micronutrient density"
                )
            )
            cleanQuery.contains("burger") || cleanQuery.contains("fast food") -> MealAnalysisResult(
                mealName = "Classic Cheeseburger",
                totalCalories = 540,
                proteinGrams = 31.0f,
                carbsGrams = 40.0f,
                fatGrams = 27.0f,
                fiberGrams = 2.0f,
                sugarGrams = 7.0f,
                sodiumMilligrams = 850.0f,
                confidenceScore = 0.95f,
                healthRating = 2,
                portionExplanation = "1 single patty cheeseburger with sesame seed bun, lettuce, and cheddar cheese",
                detectedItems = listOf(
                    DetectedFoodItem("Beef Patty (80/20 Lean)", 240, 0.96f, "115g"),
                    DetectedFoodItem("Sesame Hamburger Bun", 150, 0.98f, "1 bun"),
                    DetectedFoodItem("Cheddar Cheese Slice", 110, 0.92f, "28g"),
                    DetectedFoodItem("Sauce & Pickles", 40, 0.85f, "1.5 tbsp")
                ),
                suggestedAlternatives = listOf(
                    "Remove the top bun (open-face) to save 75 calories of simple carbs",
                    "Swap beef for a grilled chicken breast or black bean patty to cut fats"
                )
            )
            else -> {
                // Generate a customized random yet believable response based on the query name
                val name = query.replaceFirstChar { it.uppercase() }.ifEmpty { "Mixed Bowl" }
                MealAnalysisResult(
                    mealName = "$name Bowl",
                    totalCalories = 320,
                    proteinGrams = 18.0f,
                    carbsGrams = 38.0f,
                    fatGrams = 10.0f,
                    fiberGrams = 4.5f,
                    sugarGrams = 3.0f,
                    sodiumMilligrams = 420.0f,
                    confidenceScore = 0.88f,
                    healthRating = 4,
                    portionExplanation = "1 standard serving (approx. 350g)",
                    detectedItems = listOf(
                        DetectedFoodItem(name, 220, 0.90f, "1 serving"),
                        DetectedFoodItem("Assorted Steamed Vegetables", 100, 0.85f, "150g")
                    ),
                    suggestedAlternatives = listOf(
                        "Drink a full glass of water 10 minutes before eating to increase fullness",
                        "Include a lean source of protein (such as egg white or tofu) to balance macros"
                    )
                )
            }
        }
    }

    private fun simulateCoachResponse(query: String): String {
        val cleanQuery = query.trim().lowercase()
        return when {
            cleanQuery.contains("weight") || cleanQuery.contains("lose") -> """
                Here is your personalized **CalorieSnap Coach weight loss tip**:
                
                • **Create a Consistent Deficit**: Aim for a gentle deficit of **300–500 kcal** below your maintenance level. Fast fat loss triggers muscle loss.
                • **Prioritize Protein**: Consume **1.6g to 2.2g of protein per kg** of bodyweight. This keeps you full and protects lean mass.
                • **Volume Eating**: Load up 50% of your plate with green vegetables. They are low in calories but very high in fiber and volume!
                • **Water Secret**: Drinking **500ml of water** before your main meals increases satiety and natural calorie restriction.
                
                *You are doing fantastic! Keep snapping your meals and building the habit.*
            """.trimIndent()
            cleanQuery.contains("muscle") || cleanQuery.contains("gain") -> """
                Here is your **CalorieSnap Coach muscle gain plan**:
                
                • **Mild Surplus**: Muscle synthesis requires energy. Aim for **200–300 kcal above maintenance** to gain clean muscle without excess body fat.
                • **Protein Timing**: Aim to consume **25–40g of protein** every 3–4 hours to maintain muscle protein synthesis (MPS) rates.
                • **Complex Carbs**: Fuel your heavy workouts with oats, brown rice, and bananas. Carbs store glycogen, making muscles look fuller and perform better.
                • **Recovery**: Muscle grows during rest, not during workouts! Ensure you get **7.5 to 9 hours of sleep** each night.
            """.trimIndent()
            else -> """
                Hello! I am your **CalorieSnap AI Coach**. 
                
                Here are my custom health recommendations for today:
                
                1. **Mindful Snapping**: Continue snapping photos of every meal! Research shows that simply logging what you eat increases nutritional awareness by over **80%**.
                2. **Hydration Check**: Make sure you hit your water goal today. Dehydration is frequently mistaken for sugar cravings.
                3. **Active Recovery**: Try to take a **10-minute walk** immediately after your largest meal to improve insulin sensitivity and boost digestion.
                
                *What are your primary goals for today? Let's smash them together!*
            """.trimIndent()
        }
    }
}
