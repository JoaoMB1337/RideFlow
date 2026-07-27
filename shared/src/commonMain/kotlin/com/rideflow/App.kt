package com.rideflow

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.rideflow.presentation.history.HistoryScreen
import com.rideflow.presentation.history.detail.TrainDetailScreen
import com.rideflow.presentation.navigation.Screen
import com.rideflow.presentation.settings.SettingsScreen
import com.rideflow.presentation.workout.WorkoutScreen

// Modern Dark Sport Palette
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E676), // Electric Lime
    secondary = Color(0xFF2979FF), // Bright Blue
    tertiary = Color(0xFFFF3D00), // Vibrant Orange
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun App() {
    MaterialTheme(colorScheme = DarkColorScheme) {
        val navController = rememberNavController()
        var isWorkoutActive by remember { mutableStateOf(false) }
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                bottomBar = {
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route
                    
                    if (!isWorkoutActive) {
                        NavigationBar(
                            tonalElevation = 0.dp,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = "Ride") },
                                label = { Text("Ride") },
                                selected = currentRoute == Screen.Workout.route,
                                onClick = { 
                                    navController.navigate(Screen.Workout.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                                label = { Text("History") },
                                selected = currentRoute == Screen.History.route,
                                onClick = { 
                                    navController.navigate(Screen.History.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                selected = currentRoute == Screen.Settings.route,
                                onClick = { 
                                    navController.navigate(Screen.Settings.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Workout.route,
                    modifier = Modifier.padding(padding),
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    composable(Screen.Workout.route) {
                        WorkoutScreen(
                            onWorkoutActiveChanged = { isWorkoutActive = it }
                        )
                    }
                    composable(Screen.History.route) {
                        HistoryScreen(
                            onNavigateToDetail = { trainId ->
                                navController.navigate(Screen.TrainDetail.createRoute(trainId))
                            }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen()
                    }
                    composable(
                        route = Screen.TrainDetail.route,
                        arguments = listOf(navArgument("trainId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val trainId = backStackEntry.arguments?.read { getLong("trainId") } ?: 0L
                        TrainDetailScreen(
                            trainId = trainId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
