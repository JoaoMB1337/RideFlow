package com.rideflow.presentation.workout

import com.rideflow.domain.model.SensorConnectionState
import com.rideflow.domain.model.Train

data class WorkoutUiState(
    val isWorkoutActive: Boolean = false,
    val currentTrainId: Long? = null,
    val durationSeconds: Long = 0,
    val distance: Double = 0.0,
    val heartRate: Int? = null,
    val power: Int? = null,
    val speed: Double? = null,
    val resistance: Int = 10,
    val heartRateConnection: SensorConnectionState = SensorConnectionState.Disconnected,
    val bikeConnection: SensorConnectionState = SensorConnectionState.Disconnected,
    val heartRateDeviceName: String? = null,
    val bikeDeviceName: String? = null,
    val lastRide: Train? = null,
    val weekRides: Int = 0,
    val weekSeconds: Long = 0,
    val weekKm: Double = 0.0
)

sealed class WorkoutIntent {
    object StartWorkout : WorkoutIntent()
    object StopWorkout : WorkoutIntent()
    data class AdjustResistance(val level: Int) : WorkoutIntent()
}
