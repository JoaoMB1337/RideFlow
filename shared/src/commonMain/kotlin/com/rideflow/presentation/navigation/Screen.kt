package com.rideflow.presentation.navigation

sealed class Screen(val route: String) {
    object Workout : Screen("workout")
    object History : Screen("history")
    object Settings : Screen("settings")
    object TrainDetail : Screen("train_detail/{trainId}") {
        fun createRoute(trainId: Long) = "train_detail/$trainId"
    }
    object Devices : Screen("devices")
}
