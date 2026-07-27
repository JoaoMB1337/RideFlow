package com.rideflow.domain.usecase

import com.rideflow.domain.model.Train
import com.rideflow.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow

class GetWorkoutHistoryUseCase(
    private val repository: WorkoutRepository
) {
    operator fun invoke(): Flow<List<Train>> {
        return repository.getAllTrains()
    }
}
