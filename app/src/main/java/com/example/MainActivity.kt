package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.CalorieSnapTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalorieSnapTheme {
                val currentTab by viewModel.currentTab.collectAsState()
                val profile by viewModel.userProfile.collectAsState()
                val todayMeals by viewModel.todayMeals.collectAsState()
                val todayWaterLogs by viewModel.todayWaterLogs.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        CalorieSnapBottomBar(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.navigateTo(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding() / 2) // keep space for floating bar
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_transition"
                        ) { tab ->
                            when (tab) {
                                "home" -> HomeScreen(
                                    viewModel = viewModel,
                                    profile = profile,
                                    meals = todayMeals,
                                    waterLogs = todayWaterLogs,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                                "scan" -> ScanScreen(viewModel = viewModel)
                                "food_details" -> FoodDetailsScreen(viewModel = viewModel)
                                "progress" -> ProgressScreen(viewModel = viewModel, profile = profile)
                                "coach" -> CoachScreen(viewModel = viewModel)
                                "profile" -> ProfileScreen(viewModel = viewModel, profile = profile)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalorieSnapBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    // Elegant floating Scandinavian-style bottom nav pill
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag("floating_bottom_bar"),
        contentAlignment = Alignment.Center
    ) {
        NavigationBar(
            containerColor = Color.White.copy(alpha = 0.95f),
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp)) // Shadow applied first
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.9f), RoundedCornerShape(24.dp)),
            tonalElevation = 0.dp
        ) {
            val tabs = listOf(
                Triple("home", Icons.Rounded.Home, "Home"),
                Triple("scan", Icons.Rounded.AddAPhoto, "Scan"),
                Triple("coach", Icons.Rounded.Chat, "Coach"),
                Triple("progress", Icons.Rounded.BarChart, "Progress"),
                Triple("profile", Icons.Rounded.Person, "Profile")
            )

            tabs.forEach { (tabId, icon, label) ->
                val isSelected = currentTab == tabId
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(tabId) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}
