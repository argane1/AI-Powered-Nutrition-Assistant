package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.api.MealAnalysisResult
import com.example.data.db.DetectedFoodItem
import com.example.data.db.MealRecord
import com.example.data.db.UserProfile
import com.example.data.db.WaterLog
import com.example.data.db.WeightLog
import com.example.ui.ChatMessage
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- Custom Scandinavian Glassmorphism Card Modifier ---
fun Modifier.glassCard(
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp
) = this
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.70f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = borderWidth,
        color = Color(0xFFE2E8F0).copy(alpha = 0.7f),
        shape = RoundedCornerShape(cornerRadius)
    )

// --- Shared Utility Views ---

@Composable
fun SectionHeader(title: String, subtitle: String? = null, actionText: String? = null, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onActionClick)
                    .padding(vertical = 4.dp)
            )
        }
    }
}

// --- 🏠 SCREEN 1: HOME ---

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    profile: UserProfile,
    meals: List<MealRecord>,
    waterLogs: List<WaterLog>,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val totalCalories = meals.sumOf { it.calories }
    val totalProtein = meals.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs = meals.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat = meals.sumOf { it.fat.toDouble() }.toFloat()

    val calorieGoal = profile.dailyCalorieGoal
    val caloriesRemaining = (calorieGoal - totalCalories).coerceAtLeast(0)
    val overCalorieLimit = totalCalories > calorieGoal

    val totalWater = waterLogs.sumOf { it.amountMl }
    val waterGoal = profile.waterGoalMl

    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Brand Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val dateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()).uppercase()
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669), // Emerald 600
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "CalorieSnap",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B), // slate-800
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hej, ${profile.name}! Let's make today healthy.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Small glass profile avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1FAE5)) // Soft Emerald background
                        .clickable { onNavigate("profile") }
                        .testTag("avatar_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.name.firstOrNull()?.toString() ?: "U",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF064E3B), // Dark Emerald text
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Calorie Goal Progress Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .border(1.dp, Color(0xFFECFDF5).copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                    .shadow(4.dp, RoundedCornerShape(32.dp))
                    .testTag("calorie_goal_card"),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: stats
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DIARY BUDGET",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$totalCalories",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (overCalorieLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = " / $calorieGoal kcal",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (overCalorieLimit) "Over goal by ${totalCalories - calorieGoal} kcal" else "$caloriesRemaining kcal remaining",
                            fontSize = 14.sp,
                            color = if (overCalorieLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Right: Beautiful Circular Progress Indicator
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val progressFraction = (totalCalories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f)
                        val animatedProgress by animateFloatAsState(
                            targetValue = progressFraction,
                            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                        )

                        Canvas(modifier = Modifier.size(100.dp)) {
                            // Background track
                            drawCircle(
                                color = Color(0xFFE2E8F0),
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Foreground progress
                            drawArc(
                                color = if (overCalorieLimit) Color(0xFFEF4444) else Color(0xFF10B981),
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Fuel",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Macros Breakdown Cards (Row)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Protein (Orange Container)
                MacroBarCard(
                    modifier = Modifier.weight(1f),
                    label = "PROTEIN",
                    value = "${totalProtein.toInt()}g",
                    progress = totalProtein / (profile.dailyCalorieGoal * 0.25f / 4f), // standard goal 25% protein
                    color = Color(0xFFF97316)
                )

                // Carbs (Blue Container)
                MacroBarCard(
                    modifier = Modifier.weight(1f),
                    label = "CARBS",
                    value = "${totalCarbs.toInt()}g",
                    progress = totalCarbs / (profile.dailyCalorieGoal * 0.50f / 4f), // standard goal 50% carbs
                    color = Color(0xFF3B82F6)
                )

                // Fats (Purple Container)
                MacroBarCard(
                    modifier = Modifier.weight(1f),
                    label = "FAT",
                    value = "${totalFat.toInt()}g",
                    progress = totalFat / (profile.dailyCalorieGoal * 0.25f / 9f), // standard goal 25% fats
                    color = Color(0xFFA855F7)
                )
            }
        }

        // Dynamic AI Coach Insight Card (Professional Polish Feature)
        item {
            val proteinGoal = (profile.dailyCalorieGoal * 0.25f / 4f)
            val diffProtein = (proteinGoal - totalProtein).toInt()
            val insightTitle = if (diffProtein > 0) {
                "You're ${diffProtein}g short of your protein goal today."
            } else {
                "Excellent! You've crushed your protein target today."
            }
            val insightSubtitle = if (diffProtein > 0) {
                "Try Greek yogurt, almonds, or baked lemon cod with your next meal."
            } else {
                "Meeting your protein target keeps your metabolism strong and preserves lean muscle."
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .border(1.dp, Color(0xFF047857).copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .testTag("ai_coach_insight_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF059669)) // Emerald 600
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Decorative circular accent
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 30.dp, y = 30.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.3f), CircleShape)
                    )

                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Recommend,
                                    contentDescription = "AI Coach Insight",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "AI COACH INSIGHT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = insightTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = insightSubtitle,
                            fontSize = 12.sp,
                            color = Color(0xFFD1FAE5), // soft emerald-100
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Interactive Water Intake Tracker Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("water_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Water,
                                contentDescription = "Water Track",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "WATER TRACKER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "$totalWater / $waterGoal ml",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Water indicator cup/animation
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { viewModel.addWater(250) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFEFF6FF),
                                    contentColor = Color(0xFF1D4ED8)
                                )
                            ) {
                                Text("+250ml", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { viewModel.addWater(500) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFEFF6FF),
                                    contentColor = Color(0xFF1D4ED8)
                                )
                            ) {
                                Text("+500ml", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // Simple progress bar
                    LinearProgressIndicator(
                        progress = { (totalWater.toFloat() / waterGoal.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = Color(0xFF3B82F6),
                        trackColor = Color(0xFFEFF6FF)
                    )
                }
            }
        }

        // Intermittent Fasting Card
        item {
            val fasting = viewModel.fastingState.collectAsState().value
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("fasting_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Timelapse,
                                contentDescription = "Fasting Tracker",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "INTERMITTENT FASTING",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (fasting.isFasting) "Currently Fasting (${fasting.durationHours}h)" else "Fast Timer Offline",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Action button
                        Button(
                            onClick = {
                                if (fasting.isFasting) viewModel.stopFasting() else viewModel.startFasting(16)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (fasting.isFasting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = if (fasting.isFasting) "End Fast" else "Start 16:8",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (fasting.isFasting) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Calculate time left
                        val diffMillis = System.currentTimeMillis() - fasting.startTime
                        val hoursDiff = diffMillis / (1000 * 60 * 60f)
                        val progress = (hoursDiff / fasting.durationHours).coerceIn(0f, 1f)
                        val remainingSecs = (fasting.durationHours * 3600L - (diffMillis / 1000)).coerceAtLeast(0L)
                        val rHours = remainingSecs / 3600
                        val rMins = (remainingSecs % 3600) / 60

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Time remaining: ${rHours}h ${rMins}m (${(progress * 100).toInt()}% complete)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }

        // Today's Food Log / Diary
        item {
            SectionHeader(
                title = "Daily Meal Diary",
                subtitle = "${meals.size} logged today",
                actionText = if (meals.isNotEmpty()) "Add Custom" else null,
                onActionClick = { onNavigate("scan") }
            )
        }

        if (meals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .glassCard()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            imageVector = Icons.Rounded.AddAPhoto,
                            contentDescription = "Scan meals",
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 12.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        )
                        Text(
                            text = "Your food diary is empty",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Snap a photo of your breakfast, lunch, or dinner to instantly log it with AI!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(meals) { meal ->
                MealDiaryRow(meal = meal, onDelete = { viewModel.deleteMeal(meal) })
            }
        }

        // AI Meal Suggestions Section
        item {
            SectionHeader(
                title = "Personalized AI Suggestions",
                subtitle = "Tailored to your goal: ${profile.fitnessGoal}"
            )
        }

        item {
            val suggestions = listOf(
                Pair("Spinach Avocado Shake", "Lean fiber and clean plant-based healthy fats. Excellent morning starter."),
                Pair("Egg White Asparagus Salad", "High protein, ultra low carbohydrate density. Best for maintaining a calorie deficit."),
                Pair("Baked Lemon Cod with Broccoli", "Very low fats, packed with high grade pure lean protein.")
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(suggestions) { item ->
                    Card(
                        modifier = Modifier
                            .width(220.dp)
                            .clickable {
                                viewModel.analyzeMealText(item.first)
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Recommend,
                                    contentDescription = "AI Suggestion",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = item.first,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.second,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Spacer to prevent overlap with floating navigation bar
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun MacroBarCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    progress: Float,
    color: Color
) {
    Card(
        modifier = modifier
            .border(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .shadow(1.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color, // Matched category color (Orange, Blue, Purple)
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
fun MealDiaryRow(meal: MealRecord, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image or icon placeholder
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (meal.imageUrl != null) {
                        AsyncImage(
                            model = meal.imageUrl,
                            contentDescription = meal.mealName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Fastfood,
                            contentDescription = "Food icon",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = meal.mealName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "P: ${meal.protein.toInt()}g  C: ${meal.carbs.toInt()}g  F: ${meal.fat.toInt()}g",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right block: Calories + delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${meal.calories}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "kcal",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete meal log",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// --- 📷 SCREEN 2: SCAN MEAL ---

@Composable
fun ScanScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchInput by remember { mutableStateOf("") }
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    // Setup Compose Image Picker launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.analyzeMealPhoto(bitmap)
                }
            } catch (e: Exception) {
                Log.e("ScanScreen", "Gallery photo decode error: ${e.message}")
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.analyzeMealPhoto(bitmap)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isAnalyzing) {
            // Full screen loading indicator
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.9f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "CalorieSnap AI is working...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Detecting ingredients & estimating calories...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Scan Food with AI",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Take a photo, search manually, or paste a recipe",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Camera Lens Viewfinder Mock Placeholder
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black)
                            .testTag("camera_viewfinder"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Soft Scandinavian food background decorative illustration
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.scandinavian_food_hero),
                            contentDescription = "Scandinavian wellness background decoration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.6f)
                        )

                        // Camera guidelines overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val strokeW = 3.dp.toPx()
                            val cornerSize = 30.dp.toPx()

                            // Draw corners to simulate camera focus bounds
                            val p1 = Path().apply {
                                moveTo(20.dp.toPx() + cornerSize, 20.dp.toPx())
                                lineTo(20.dp.toPx(), 20.dp.toPx())
                                lineTo(20.dp.toPx(), 20.dp.toPx() + cornerSize)
                            }
                            drawPath(p1, Color.White, style = Stroke(strokeW))

                            val p2 = Path().apply {
                                moveTo(w - 20.dp.toPx() - cornerSize, 20.dp.toPx())
                                lineTo(w - 20.dp.toPx(), 20.dp.toPx())
                                lineTo(w - 20.dp.toPx(), 20.dp.toPx() + cornerSize)
                            }
                            drawPath(p2, Color.White, style = Stroke(strokeW))

                            val p3 = Path().apply {
                                moveTo(20.dp.toPx() + cornerSize, h - 20.dp.toPx())
                                lineTo(20.dp.toPx(), h - 20.dp.toPx())
                                lineTo(20.dp.toPx(), h - 20.dp.toPx() - cornerSize)
                            }
                            drawPath(p3, Color.White, style = Stroke(strokeW))

                            val p4 = Path().apply {
                                moveTo(w - 20.dp.toPx() - cornerSize, h - 20.dp.toPx())
                                lineTo(w - 20.dp.toPx(), h - 20.dp.toPx())
                                lineTo(w - 20.dp.toPx(), h - 20.dp.toPx() - cornerSize)
                            }
                            drawPath(p4, Color.White, style = Stroke(strokeW))
                        }

                        // Camera triggers
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FloatingActionButton(
                                onClick = { cameraLauncher.launch(null) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(64.dp)
                                    .testTag("capture_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Camera,
                                    contentDescription = "Capture live food",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "TAP TO INSTANTLY DETECT MEAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Gallery Upload Button
                item {
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .height(54.dp)
                            .testTag("upload_gallery_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Upload from gallery")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Meal Photo from Gallery", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Manual Search Section
                item {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        SectionHeader(title = "Manual Food Search", subtitle = "Or simulate scanning by typing any food")

                        TextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .testTag("food_search_input"),
                            placeholder = { Text("Search pizza, salmon salad, matcha, banana...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                            trailingIcon = {
                                if (searchInput.isNotEmpty()) {
                                    IconButton(onClick = { searchInput = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchInput.trim().isNotEmpty()) {
                                        keyboardController?.hide()
                                        viewModel.analyzeMealText(searchInput)
                                    }
                                }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        Button(
                            onClick = {
                                if (searchInput.trim().isNotEmpty()) {
                                    keyboardController?.hide()
                                    viewModel.analyzeMealText(searchInput)
                                }
                            },
                            enabled = searchInput.trim().isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .height(46.dp)
                                .testTag("search_submit_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Estimate Calories & Macros", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Barcode scanner simulation / Custom search list
                item {
                    SectionHeader(title = "Barcode Scanner & Quick Search", subtitle = "Common foods for one-tap tracking")
                }

                item {
                    val quickFoods = listOf("Pepperoni Pizza", "Avocado Toast", "Chicken Caesar Salad", "Cheeseburger")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickFoods) { item ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.analyzeMealText(item) },
                                label = { Text(item, fontSize = 13.sp) },
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Fastfood, contentDescription = item, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }

                // Recipe Calorie Estimator Block
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader(
                        title = "Recipe Estimator",
                        subtitle = "Paste a complete recipe to estimate total nutritions"
                    )

                    var recipeInput by remember { mutableStateOf("") }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = recipeInput,
                                onValueChange = { recipeInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .testTag("recipe_input_box"),
                                placeholder = { Text("e.g. 2 eggs, 50g feta cheese, 1 slice sourdough bread...", fontSize = 13.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (recipeInput.trim().isNotEmpty()) {
                                        viewModel.analyzeMealText(recipeInput)
                                    }
                                },
                                enabled = recipeInput.trim().isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Analyze Recipe", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 🍽 SCREEN 3: FOOD DETAILS / ANALYSIS RESULT ---

@Composable
fun FoodDetailsScreen(viewModel: MainViewModel) {
    val analysis = viewModel.activeAnalysis.collectAsState().value ?: return
    val activePhoto = viewModel.activeMealPhoto.collectAsState().value
    val multiplier by viewModel.portionMultiplier.collectAsState()

    // Locally adjusted state numbers
    val scaledCalories = (analysis.totalCalories * multiplier).toInt()
    val scaledProtein = analysis.proteinGrams * multiplier
    val scaledCarbs = analysis.carbsGrams * multiplier
    val scaledFat = analysis.fatGrams * multiplier
    val scaledFiber = analysis.fiberGrams * multiplier
    val scaledSugar = analysis.sugarGrams * multiplier
    val scaledSodium = analysis.sodiumMilligrams * multiplier

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Food Header Banner with Image
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                if (activePhoto != null) {
                    Image(
                        bitmap = activePhoto.asImageBitmap(),
                        contentDescription = analysis.mealName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback to our stunning generated Scandinavian Food illustration!
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.scandinavian_food_hero),
                        contentDescription = "Healthy food background logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Dark subtle gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )

                // Back Button overlay
                IconButton(
                    onClick = { viewModel.discardActiveMeal() },
                    modifier = Modifier
                        .padding(top = 36.dp, start = 16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Food Title Overlay at bottom of banner
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text("AI CONFIDENCE ${(analysis.confidenceScore * 100).toInt()}%", modifier = Modifier.padding(4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Row {
                            repeat(5) { index ->
                                Icon(
                                    imageVector = if (index < analysis.healthRating) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "Health score rating star",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = analysis.mealName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Portions Adjustment Slider Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .testTag("portion_slider_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PORTION ADJUSTMENT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = analysis.portionExplanation,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${"%.2f".format(multiplier)}x",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = multiplier,
                        onValueChange = { viewModel.updatePortionMultiplier(it) },
                        valueRange = 0.25f..3.0f,
                        steps = 11,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.25x (Snack)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("1.0x (Regular)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text("3.0x (Giant)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Estimated Energy & Macronutrients
        item {
            SectionHeader(title = "Nutrition Facts Summary", subtitle = "Scaled for ${"%.2f".format(multiplier)}x portion")
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Calories Card
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ENERGY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("$scaledCalories", fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text("kcal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Macro distributions right-hand cards column
                Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroCompactRow(label = "Protein", value = "${"%.1f".format(scaledProtein)}g", color = Color(0xFF10B981))
                    MacroCompactRow(label = "Carbs", value = "${"%.1f".format(scaledCarbs)}g", color = Color(0xFFF59E0B))
                    MacroCompactRow(label = "Fat", value = "${"%.1f".format(scaledFat)}g", color = Color(0xFFEF4444))
                }
            }
        }

        // Micronutrients breakdown card
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ADDITIONAL NUTRIENTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dietary Fiber", fontSize = 14.sp)
                        Text("${"%.1f".format(scaledFiber)} g", fontWeight = FontWeight.Bold)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Sugar", fontSize = 14.sp)
                        Text("${"%.1f".format(scaledSugar)} g", fontWeight = FontWeight.Bold)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sodium", fontSize = 14.sp)
                        Text("${scaledSodium.toInt()} mg", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Detected Food Items (Detailed Breakdown)
        item {
            SectionHeader(title = "AI Ingredient Audit", subtitle = "Breakdown of individual foods detected")
        }

        if (analysis.detectedItems.isEmpty()) {
            item {
                Text(
                    text = "No detailed sub-ingredients identified.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        } else {
            items(analysis.detectedItems) { item ->
                IngredientRow(item = item, multiplier = multiplier)
            }
        }

        // AI Alternatives Card
        item {
            SectionHeader(title = "Healthier Alternatives", subtitle = "Proactive tips suggested by AI Coach")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    analysis.suggestedAlternatives.forEach { alt ->
                        Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Alt suggestion",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = alt,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons: Save & Discard
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.saveActiveMealToDiary() },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(54.dp)
                        .testTag("save_meal_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save to Food Diary", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.discardActiveMeal() },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("discard_meal_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun MacroCompactRow(label: String, value: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun IngredientRow(item: DetectedFoodItem, multiplier: Float) {
    val scaledCals = (item.calories * multiplier).toInt()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Serving size: ${item.portion}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$scaledCals kcal", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${(item.confidence * 100).toInt()}% match", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- 📊 SCREEN 4: PROGRESS ---

@Composable
fun ProgressScreen(viewModel: MainViewModel, profile: UserProfile) {
    val weightHistory = viewModel.weightLogs.collectAsState().value
    var weightInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf(profile.heightCm.toString()) }

    var calculatedBmi by remember { mutableStateOf(0f) }
    var bmiCategory by remember { mutableStateOf("") }

    // On launch or profile weight updates, trigger BMI update
    LaunchedEffect(profile.weightKg, profile.heightCm) {
        val hMeters = profile.heightCm / 100f
        if (hMeters > 0) {
            calculatedBmi = profile.weightKg / (hMeters * hMeters)
            bmiCategory = when {
                calculatedBmi < 18.5f -> "Underweight"
                calculatedBmi < 25.0f -> "Healthy Weight"
                calculatedBmi < 30.0f -> "Overweight"
                else -> "Obese"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Health Progress Analytics", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = "View long-term weight, BMI, and nutritional trends", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Custom Canvas Chart
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "WEEKLY CALORIE PROGRESSION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Stunning Compose custom-drawn chart representing daily calorie intake
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            val cals = listOf(1850f, 2100f, 1720f, 2300f, 1950f, 2200f, 1800f)
                            val goal = profile.dailyCalorieGoal.toFloat()

                            days.forEachIndexed { i, day ->
                                val heightFrac = (cals[i] / 2500f).coerceIn(0.1f, 1.0f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .fillMaxHeight(heightFrac)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (cals[i] > goal) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = day, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Weight Track Form Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("weight_log_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "WEIGHT LOGGER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("weight_input_field"),
                            placeholder = { Text("72.5") },
                            suffix = { Text("kg") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val wFloat = weightInput.toFloatOrNull()
                                if (wFloat != null) {
                                    viewModel.addWeight(wFloat)
                                    weightInput = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Text("Log Weight")
                        }
                    }
                }
            }
        }

        // BMI Calculator & Meter Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("bmi_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "BODY MASS INDEX (BMI) STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Your Current BMI",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%.1f".format(calculatedBmi),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Status pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when (bmiCategory) {
                                        "Healthy Weight" -> Color(0xFFD1FAE5)
                                        "Overweight" -> Color(0xFFFEF3C7)
                                        else -> Color(0xFFFEE2E2)
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = bmiCategory,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (bmiCategory) {
                                    "Healthy Weight" -> Color(0xFF065F46)
                                    "Overweight" -> Color(0xFF92400E)
                                    else -> Color(0xFF991B1B)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Minimal gauge bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF60A5FA), // Underweight blue
                                        Color(0xFF34D399), // Healthy green
                                        Color(0xFFFBBF24), // Overweight yellow
                                        Color(0xFFF87171)  // Obese red
                                    )
                                )
                            )
                    )
                }
            }
        }

        // Historic Weight Progress Logs
        item {
            SectionHeader(title = "Historic Weight Logs", subtitle = "${weightHistory.size} total entries")
        }

        if (weightHistory.isEmpty()) {
            item {
                Text(
                    text = "No weight entries logged yet. Input weight above to start tracking!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        } else {
            items(weightHistory) { log ->
                WeightLogItemRow(log = log, onDelete = { viewModel.deleteWeight(log) })
            }
        }
    }
}

@Composable
fun WeightLogItemRow(log: WeightLog, onDelete: () -> Unit) {
    val dateString = remember(log.timestamp) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${log.weightKg} kg", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(dateString, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Delete weight entry",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// --- 🤖 SCREEN 5: AI COACH ---

@Composable
fun CoachScreen(viewModel: MainViewModel) {
    val messages = viewModel.chatMessages.collectAsState().value
    val isTyping by viewModel.isCoachTyping.collectAsState()
    var inputMessage by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size, isTyping) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "CalorieSnap AI Coach", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(text = "Ask any diet, fitness, or meal planning question", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(onClick = { viewModel.clearCoachHistory() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Chat history", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Scrollable Chat area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            messages.forEach { msg ->
                ChatBubble(message = msg)
            }

            if (isTyping) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🤖", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.widthIn(max = 260.dp)
                    ) {
                        Text(
                            text = "CalorieSnap Coach is typing...",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Quick Suggestion templates list
        val quickChats = listOf(
            "How can I lose fat safely?",
            "Suggest a 3-day high-protein plan",
            "What should I eat post-workout?"
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quickChats) { text ->
                ElevatedCard(
                    modifier = Modifier.clickable {
                        viewModel.sendCoachMessage(text)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Chat input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 100.dp), // Space for bottom nav
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("coach_input"),
                placeholder = { Text("Ask Coach about calories, workouts...", fontSize = 14.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputMessage.trim().isNotEmpty()) {
                            viewModel.sendCoachMessage(inputMessage)
                            inputMessage = ""
                            keyboardController?.hide()
                        }
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            FloatingActionButton(
                onClick = {
                    if (inputMessage.trim().isNotEmpty()) {
                        viewModel.sendCoachMessage(inputMessage)
                        inputMessage = ""
                        keyboardController?.hide()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("coach_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send message", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                modifier = Modifier.padding(14.dp),
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 18.sp)
            }
        }
    }
}

// --- 👤 SCREEN 6: PROFILE ---

@Composable
fun ProfileScreen(viewModel: MainViewModel, profile: UserProfile) {
    var editName by remember { mutableStateOf(profile.name) }
    var editAge by remember { mutableStateOf(profile.age.toString()) }
    var editHeight by remember { mutableStateOf(profile.heightCm.toString()) }
    var editWeight by remember { mutableStateOf(profile.weightKg.toString()) }
    var editCalorieGoal by remember { mutableStateOf(profile.dailyCalorieGoal.toString()) }
    var editWaterGoal by remember { mutableStateOf(profile.waterGoalMl.toString()) }

    var selectedGoal by remember { mutableStateOf(profile.fitnessGoal) }
    var selectedActivity by remember { mutableStateOf(profile.activityLevel) }
    var selectedGender by remember { mutableStateOf(profile.gender) }
    var selectedDietary by remember { mutableStateOf(profile.dietaryPreference) }
    var editAllergies by remember { mutableStateOf(profile.allergies) }

    // Toggle states for Premium mock sync integrations
    var appleSyncEnabled by remember { mutableStateOf(false) }
    var googleFitSyncEnabled by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 140.dp)
        ) {
            // Header Title
            item {
                Column(modifier = Modifier.padding(vertical = 20.dp)) {
                    Text(text = "Premium User Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Customize your body configurations and AI targets", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Core Profile Form Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "PERSONAL INFORMATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Name
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Age, Height, Weight Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = editAge,
                                onValueChange = { editAge = it },
                                label = { Text("Age") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_age_field"),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editHeight,
                                onValueChange = { editHeight = it },
                                label = { Text("Height (cm)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_height_field"),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editWeight,
                                onValueChange = { editWeight = it },
                                label = { Text("Weight (kg)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_weight_field"),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Targets Goal Setup Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = editCalorieGoal,
                                onValueChange = { editCalorieGoal = it },
                                label = { Text("Calorie Target") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_calorie_goal_field"),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editWaterGoal,
                                onValueChange = { editWaterGoal = it },
                                label = { Text("Water Target (ml)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_water_goal_field"),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Gender Field Option Selector
                        Text("Biological Gender", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Male", "Female", "Non-binary").forEach { gen ->
                                FilterChip(
                                    selected = selectedGender == gen,
                                    onClick = { selectedGender = gen },
                                    label = { Text(gen) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Fitness Targets Card Selector
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "AI FITNESS & DIETARY PREFERENCES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Goals
                        Text("Fitness Goal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Lose Weight", "Maintain Weight", "Gain Muscle").forEach { goal ->
                                FilterChip(
                                    selected = selectedGoal == goal,
                                    onClick = { selectedGoal = goal },
                                    label = { Text(goal, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Dietary preferences
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Dietary Style", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("None", "Vegetarian", "Vegan", "Keto").forEach { style ->
                                FilterChip(
                                    selected = selectedDietary == style,
                                    onClick = { selectedDietary = style },
                                    label = { Text(style, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Allergies manual input
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editAllergies,
                            onValueChange = { editAllergies = it },
                            label = { Text("Allergies / Restrictions") },
                            placeholder = { Text("Nuts, Dairy, Gluten...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_allergies_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Sync Health Integrations (Toggles)
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "HEALTH APP SYNCHRONIZATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Apple Health Sync
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sync Apple Health", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Auto import calories & active workout metrics", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = appleSyncEnabled,
                                onCheckedChange = { appleSyncEnabled = it }
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Google Fit Sync
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sync Google Fit / Wearables", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Auto export daily water and dietary logging details", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = googleFitSyncEnabled,
                                onCheckedChange = { googleFitSyncEnabled = it }
                            )
                        }
                    }
                }
            }

            // Save settings action button
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        val ageVal = editAge.toIntOrNull() ?: profile.age
                        val heightVal = editHeight.toFloatOrNull() ?: profile.heightCm
                        val weightVal = editWeight.toFloatOrNull() ?: profile.weightKg
                        val calorieGoalVal = editCalorieGoal.toIntOrNull() ?: profile.dailyCalorieGoal
                        val waterGoalVal = editWaterGoal.toIntOrNull() ?: profile.waterGoalMl

                        viewModel.updateProfile(
                            name = editName,
                            age = ageVal,
                            heightCm = heightVal,
                            weightKg = weightVal,
                            gender = selectedGender,
                            activityLevel = selectedActivity,
                            fitnessGoal = selectedGoal,
                            dietaryPreference = selectedDietary,
                            allergies = editAllergies,
                            dailyCalorieGoal = calorieGoalVal,
                            waterGoalMl = waterGoalVal
                        )

                        scope.launch {
                            snackbarHostState.showSnackbar("CalorieSnap profile updated successfully!")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_profile_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save Configuration Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        )
    }
}
