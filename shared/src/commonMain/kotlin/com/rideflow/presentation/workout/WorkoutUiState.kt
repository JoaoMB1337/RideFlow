package com.rideflow.presentation.workout

import com.rideflow.domain.model.SensorConnectionState

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
    val bikeConnection: SensorConnectionState = SensorConnectionState.Disconnected
)

sealed class WorkoutIntent {
    object StartWorkout : WorkoutIntent()
    object StopWorkout : WorkoutIntent()
    data class AdjustResistance(val level: Int) : WorkoutIntent()
}
