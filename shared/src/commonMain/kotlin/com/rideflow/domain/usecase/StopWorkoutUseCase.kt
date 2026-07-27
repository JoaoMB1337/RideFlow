package com.rideflow.domain.usecase

import com.rideflow.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.first

class StopWorkoutUseCase(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke(trainId: Long, duration: Long) {
        val samples = repository.getTrainingDataForTrain(trainId).first()
        
        if (samples.isEmpty()) return

        val avgHeartRate = samples.mapNotNull { it.heartRate }.average().toInt()
        val avgPower = samples.mapNotNull { it.power }.average().toInt()
        val maxHeartRate = samples.mapNotNull { it.heartRate }.maxOrNull() ?: 0
        val maxPower = samples.mapNotNull { it.power }.maxOrNull() ?: 0

        val train = repository.getTrainById(trainId)?.copy(
            duration = duration,
            avgHeartRate = avgHeartRate,
            avgPower = avgPower,
            maxHeartRate = maxHeartRate,
            maxPower = maxPower
        )

        train?.let { repository.updateTrainSummary(it) }
    }
}
