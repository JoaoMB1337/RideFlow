package com.rideflow

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.rideflow.presentation.history.HistoryScreen
import com.rideflow.presentation.history.detail.TrainDetailScreen
import com.rideflow.presentation.navigation.Screen
import com.rideflow.presentation.settings.SettingsScreen
import com.rideflow.presentation.theme.RideFlowTheme
import com.rideflow.presentation.workout.WorkoutScreen

private data class TopLevelTab(val screen: Screen, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TopLevelTab(Screen.Workout, "Ride", Icons.AutoMirrored.Filled.DirectionsBike),
    TopLevelTab(Screen.History, "History", Icons.Filled.History),
    TopLevelTab(Screen.Settings, "Settings", Icons.Filled.Settings),
)

@Composable
fun App() {
    RideFlowTheme {
        val navController = rememberNavController()
        var isWorkoutActive by remember { mutableStateOf(false) }

        Scaffold(
            bottomBar = {
                // Durante o treino escondemos a navegação: menos toques acidentais e mais espaço para as métricas.
                if (!isWorkoutActive) {
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route
                    // O detalhe de um treino pertence ao separador "History".
                    val selectedRoute = if (currentRoute == Screen.TrainDetail.route) Screen.History.route else currentRoute

                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                                selected = selectedRoute == tab.screen.route,
                                onClick = {
                                    navController.navigate(tab.screen.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Workout.route,
                // consumeWindowInsets: os Scaffolds/TopAppBars de cada ecrã não voltam a aplicar
                // os insets que este Scaffold já tratou (evita margens duplicadas).
                modifier = Modifier.padding(padding).consumeWindowInsets(padding),
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
