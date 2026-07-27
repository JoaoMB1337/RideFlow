package com.rideflow.domain.usecase

import com.rideflow.domain.repository.WorkoutRepository
import kotlin.time.Clock

class StartWorkoutUseCase(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke(): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return repository.startTrain(now)
    }
}
